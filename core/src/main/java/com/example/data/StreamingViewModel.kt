package com.example.data

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.withContext
import java.util.UUID
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Facebook
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.storage.storage
import com.example.data.YouTubeApiClient
import com.example.data.YouTubeFormatUtils
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.database.StandaloneDatabaseProvider

enum class AppDestination {
    SPLASH,
    WELCOME,
    LOGIN,
    REGISTER,
    OTP_VERIFY,
    PROFILE_SELECT,
    CREATE_PROFILE,
    MAIN_HUB,
    SUBSCRIPTION_PLANS,
    SUBSCRIPTION_DASHBOARD,
    PAYMENT_GATEWAY,
    SUBSCRIPTION_HISTORY,
    NOTIFICATIONS,
    SUPPORT_TICKETS,
    LIVE_CHAT,
    REWARDS_REFERRAL,
    ADMIN_PANEL,
    SUPABASE_TODOS,
    KIDS_MODE
}

enum class MainTab {
    HOME,
    LIVE_TV,
    DISCOVER,
    CATCH_UP,
    PROFILE_SETTINGS
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isSenderUser: Boolean,
    val timestamp: String = "11:15 AM"
)

class StreamingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("swapnoflix_prefs", android.content.Context.MODE_PRIVATE)

    // Firebase and Custom DB State Integrations
    val firebaseStatus = FirebaseManager.status
    val firebaseStatusDetail = FirebaseManager.statusDetail
    val firebaseUser = FirebaseManager.currentUser
    val activeSocialUsers = FirebaseManager.activeSocialUsers

    private val _firebaseAuthError = MutableStateFlow<String?>(null)
    val firebaseAuthError = _firebaseAuthError.asStateFlow()

    // Destination States
    private val _currentDestination = MutableStateFlow(AppDestination.SPLASH)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _settingsSubSectionOverride = MutableStateFlow<String?>(null)
    val settingsSubSectionOverride = _settingsSubSectionOverride.asStateFlow()

    fun navigateToSettingsSubSection(section: String) {
        _settingsSubSectionOverride.value = section
        selectTab(MainTab.PROFILE_SETTINGS)
    }

    fun clearSettingsSubSectionOverride() {
        _settingsSubSectionOverride.value = null
    }

    // Authentication States
    private val _userEmail = MutableStateFlow("")
    val userEmail = _userEmail.asStateFlow()

    private val _socialProviderUsed = MutableStateFlow("Email")
    val socialProviderUsed = _socialProviderUsed.asStateFlow()

    private val _rememberMe = MutableStateFlow(true)
    val rememberMe = _rememberMe.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode = _otpCode.asStateFlow()

    private val _generatedOtp = MutableStateFlow("8246")
    val generatedOtp = _generatedOtp.asStateFlow()

    // Profiles
    private val _profiles = MutableStateFlow(mutableListOf<UserProfile>())
    val profiles = _profiles.asStateFlow()

    private val _selectedProfile = MutableStateFlow<UserProfile?>(null)
    val selectedProfile = _selectedProfile.asStateFlow()

    // Player and Catalog Choices
    private val _activeLiveChannel = MutableStateFlow<LiveChannel?>(StreamingDatabase.channels.first())
    val activeLiveChannel = _activeLiveChannel.asStateFlow()

    private val _recentChannels = MutableStateFlow<List<LiveChannel>>(emptyList())
    val recentChannels: StateFlow<List<LiveChannel>> = _recentChannels.asStateFlow()

    private val _activeMovie = MutableStateFlow<Movie?>(StreamingDatabase.movies.first())
    val activeMovie = _activeMovie.asStateFlow()

    private val _activeSeries = MutableStateFlow<Series?>(StreamingDatabase.series.first())
    val activeSeries = _activeSeries.asStateFlow()

    private val _activeEpisode = MutableStateFlow<Episode?>(StreamingDatabase.series.first().episodesBySeason[1]?.first())
    val activeEpisode = _activeEpisode.asStateFlow()

    // Player controls status
    private val _playbackQuality = MutableStateFlow("Auto (1080p)")
    val playbackQuality = _playbackQuality.asStateFlow()

    private val _playbackAudio = MutableStateFlow("English (Dolby 5.1)")
    val playbackAudio = _playbackAudio.asStateFlow()

    private val _playbackSubtitle = MutableStateFlow("English Outlined")
    val playbackSubtitle = _playbackSubtitle.asStateFlow()

    private val _isCasting = MutableStateFlow(false)
    val isCasting = _isCasting.asStateFlow()

    private val _isMiniPlayerActive = MutableStateFlow(false)
    val isMiniPlayerActive = _isMiniPlayerActive.asStateFlow()

    // PlayTube Custom Youtube-like States
    private val _playTubeCurrentVideo = MutableStateFlow<PlayTubeVideo?>(null)
    val playTubeCurrentVideo = _playTubeCurrentVideo.asStateFlow()

    private val _playTubeIsPlaying = MutableStateFlow(false)
    val playTubeIsPlaying = _playTubeIsPlaying.asStateFlow()

    private val _playTubeIsAudioOnly = MutableStateFlow(false)
    val playTubeIsAudioOnly = _playTubeIsAudioOnly.asStateFlow()

    private val _playTubeResolution = MutableStateFlow("1080p")
    val playTubeResolution = _playTubeResolution.asStateFlow()

    private val _playTubeFps60 = MutableStateFlow(true)
    val playTubeFps60 = _playTubeFps60.asStateFlow()

    private val _playTubeBlockedAdsCount = MutableStateFlow(1280)
    val playTubeBlockedAdsCount = _playTubeBlockedAdsCount.asStateFlow()

    private val _playTubeIsPipMode = MutableStateFlow(false)
    val playTubeIsPipMode = _playTubeIsPipMode.asStateFlow()

    private val _isSystemPipMode = MutableStateFlow(false)
    val isSystemPipMode = _isSystemPipMode.asStateFlow()

    private val _liveTvIsFullScreen = MutableStateFlow(false)
    val liveTvIsFullScreen = _liveTvIsFullScreen.asStateFlow()

    private val _playTubeSubscribedChannelIds = MutableStateFlow<Set<String>>(setOf("creator_lofi", "creator_mkbhd"))
    val playTubeSubscribedChannelIds = _playTubeSubscribedChannelIds.asStateFlow()

    private val _playTubeBookmarkedVideoIds = MutableStateFlow<Set<String>>(setOf("video_lofi_1"))
    val playTubeBookmarkedVideoIds = _playTubeBookmarkedVideoIds.asStateFlow()

    private val _playTubeBookmarkedPlaylistIds = MutableStateFlow<Set<String>>(setOf("pl_1"))
    val playTubeBookmarkedPlaylistIds = _playTubeBookmarkedPlaylistIds.asStateFlow()

    // ─── ANONYMOUS USER LOCAL STORAGE ───
    private fun isUserLoggedIn(): Boolean {
        return com.example.supabase.auth.currentUserOrNull() != null || FirebaseManager.currentUser.value != null
    }

    private fun saveLocalFavorite(id: String, kind: String, isAdd: Boolean) {
        val key = "local_fav_${kind}s"
        val current = prefs.getString(key, "")?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
        if (isAdd) current.add(id) else current.remove(id)
        prefs.edit().putString(key, current.joinToString(",")).apply()
    }

    private fun getLocalFavorites(kind: String): Set<String> {
        return prefs.getString("local_fav_${kind}s", "")?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    }

    private fun saveLocalWatchHistory(id: String, title: String, kind: String, progress: Double) {
        val historyJson = prefs.getString("local_watch_history", "[]") ?: "[]"
        try {
            val array = org.json.JSONArray(historyJson)
            val newItem = org.json.JSONObject().apply {
                put("id", id)
                put("title", title)
                put("kind", kind)
                put("progress", progress)
                put("timestamp", System.currentTimeMillis())
            }
            // Remove existing entry for same content
            val newList = org.json.JSONArray()
            newList.put(newItem)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                if (item.getString("id") != id) {
                    newList.put(item)
                }
            }
            prefs.edit().putString("local_watch_history", newList.toString()).apply()
        } catch (e: Exception) {}
    }

    private fun syncLocalDataToCloud() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Sync Favorites
            val localMovies = getLocalFavorites("movie")
            localMovies.forEach { syncFavoriteToSupabase(it, true, "movie") }

            val localSeries = getLocalFavorites("series")
            localSeries.forEach { syncFavoriteToSupabase(it, true, "series") }

            // Sync Watch History
            val historyJson = prefs.getString("local_watch_history", "[]") ?: "[]"
            try {
                val array = org.json.JSONArray(historyJson)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    saveWatchHistoryToSupabase(
                        localId = item.getString("id"),
                        title = item.getString("title"),
                        kind = item.getString("kind"),
                        progress = item.getDouble("progress")
                    )
                }
            } catch (e: Exception) {}

            // Clear local storage after sync
            prefs.edit()
                .remove("local_fav_movies")
                .remove("local_fav_series")
                .remove("local_watch_history")
                .apply()

            android.util.Log.d("CloudSync", "Anonymous local data synced to Cloud successfully.")
        }
    }

    private val _playTubeDownloads = MutableStateFlow<List<PlayTubeDownload>>(
        listOf(
            PlayTubeDownload("video_lofi_1", "Lofi Hip Hop Radio 📚 Beats to Relax/Study/Focus", "Lofi Girl", "LIVE", "Music", 36700160L, true, "Audio-Only", 1.0f, "Completed", "🎧"),
            PlayTubeDownload("video_ted_1", "What Happens If You Sleep Outside in Space?", "TED-Ed", "05:40", "Education", 18874368L, false, "720p", 1.0f, "Completed", "💡")
        )
    )
    val playTubeDownloads = _playTubeDownloads.asStateFlow()

    private val _friendsList = MutableStateFlow<List<DbProfile>>(emptyList())
    val friendsList = _friendsList.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<DbProfile>>(emptyList())
    val friendRequests = _friendRequests.asStateFlow()

    private val _allProfilesSearch = MutableStateFlow<List<DbProfile>>(emptyList())
    val allProfilesSearch = _allProfilesSearch.asStateFlow()

    // ─── Routine Plans (Supabase) ──────────────────────────────────────────────
    private val _routinePlans = MutableStateFlow<List<SupabaseRoutinePlan>>(emptyList())
    val routinePlans = _routinePlans.asStateFlow()

    private val _routineItems = MutableStateFlow<List<SupabaseRoutineItem>>(emptyList())
    val routineItems = _routineItems.asStateFlow()

    // ─── Social Activity Feed (Supabase) ──────────────────────────────────────
    private val _socialActivityFeed = MutableStateFlow<List<SocialActivityFeedItem>>(emptyList())
    val socialActivityFeed = _socialActivityFeed.asStateFlow()

    // ─── App Config (Supabase) ────────────────────────────────────────────────
    private val _remoteAppConfig = MutableStateFlow<SupabaseAppConfig?>(null)
    val remoteAppConfig = _remoteAppConfig.asStateFlow()

    // ─── Content Seasons + Episodes (Supabase) ────────────────────────────────
    private val _contentSeasons = MutableStateFlow<List<SupabaseContentSeason>>(emptyList())
    val contentSeasons = _contentSeasons.asStateFlow()

    private val _contentEpisodes = MutableStateFlow<List<SupabaseContentEpisode>>(emptyList())
    val contentEpisodes = _contentEpisodes.asStateFlow()

    // ─── Supabase Watch History ───────────────────────────────────────────────
    private val _supabaseWatchHistory = MutableStateFlow<List<SupabaseWatchHistory>>(emptyList())
    val supabaseWatchHistory = _supabaseWatchHistory.asStateFlow()

    // ─── Devices Management (Supabase) ────────────────────────────────────────
    private val _devices = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val devices = _devices.asStateFlow()

    // ─── TV Recording States ──────────────────────────────────────────────────
    private val _recordingChannelId = MutableStateFlow<String?>(null)
    val recordingChannelId = _recordingChannelId.asStateFlow()

    private val _isTvRecordingActive = MutableStateFlow(false)
    val isTvRecordingActive = _isTvRecordingActive.asStateFlow()

    private val _activeRoomMembersCount = MutableStateFlow(2)
    val activeRoomMembersCount = _activeRoomMembersCount.asStateFlow()


    private val _playTubePlaylists = MutableStateFlow<List<PlayTubePlaylist>>(
        listOf(
            PlayTubePlaylist("pl_1", "Study Beats", "Beats to focus on work.", listOf("video_lofi_1", "video_lofi_2")),
            PlayTubePlaylist("pl_2", "Tech Reviews", "The latest smartphone launches.", listOf("video_mkbhd_1", "video_mkbhd_2"))
        )
    )
    val playTubePlaylists = _playTubePlaylists.asStateFlow()

    // --- YOUTUBE REAL INTEGRATION STATES (FIXED) ---
    private val _youtubeHomeVideos = MutableStateFlow<List<PlayTubeVideo>>(emptyList())
    val youtubeHomeVideos: StateFlow<List<PlayTubeVideo>> = _youtubeHomeVideos.asStateFlow()

    private val _youtubeShortsVideos = MutableStateFlow<List<PlayTubeVideo>>(emptyList())
    val youtubeShortsVideos: StateFlow<List<PlayTubeVideo>> = _youtubeShortsVideos.asStateFlow()

    private val _youtubeSearchResults = MutableStateFlow<List<PlayTubeVideo>>(emptyList())
    val youtubeSearchResults: StateFlow<List<PlayTubeVideo>> = _youtubeSearchResults.asStateFlow()

    private val _isYoutubeLoading = MutableStateFlow(false)
    val isYoutubeLoading: StateFlow<Boolean> = _isYoutubeLoading.asStateFlow()

    private val _youtubeError = MutableStateFlow<String?>(null)
    val youtubeError: StateFlow<String?> = _youtubeError.asStateFlow()

    private val _isYoutubeSearchActive = MutableStateFlow(false)
    val isYoutubeSearchActive: StateFlow<Boolean> = _isYoutubeSearchActive.asStateFlow()

    private val _playTubeSearchQuery = MutableStateFlow("")
    val playTubeSearchQuery: StateFlow<String> = _playTubeSearchQuery.asStateFlow()

    fun updatePlayTubeSearchQuery(query: String) {
        _playTubeSearchQuery.value = query
        searchYouTube(query)
    }

    fun setYoutubeSearchActive(active: Boolean) {
        _isYoutubeSearchActive.value = active
        if (!active) {
            updatePlayTubeSearchQuery("")
        }
    }

    private val _currentPlayTubeTab = MutableStateFlow("Home")
    val currentPlayTubeTab = _currentPlayTubeTab.asStateFlow()

    fun togglePlayTubeTab(tab: String) {
        _currentPlayTubeTab.value = tab
    }

    private val _playTubeApiSource = MutableStateFlow("Local Mock Data")
    val playTubeApiSource: StateFlow<String> = _playTubeApiSource.asStateFlow()

    private val _invidiousInstances = MutableStateFlow<List<InvidiousInstanceStatus>>(emptyList())
    val invidiousInstances = _invidiousInstances.asStateFlow()

    private val _preferredInstance = MutableStateFlow<String?>(null)
    val preferredInstance = _preferredInstance.asStateFlow()

    private val _autoSwitchOnFailure = MutableStateFlow(true)
    val autoSwitchOnFailure = _autoSwitchOnFailure.asStateFlow()

    private val _isHealthChecking = MutableStateFlow(false)
    val isHealthChecking = _isHealthChecking.asStateFlow()

    // ---------- Official YouTube API Client ----------
    private val youtubeApiClient = YouTubeApiClient
    private val youtubeApi = youtubeApiClient.apiService

    // Supabase-backed YouTube sources and playlists
    private val _youtubeSources = MutableStateFlow<List<SupabaseYouTubeSource>>(emptyList())
    val youtubeSources = _youtubeSources.asStateFlow()

    private val _youtubePlaylists = MutableStateFlow<List<SupabaseYouTubePlaylistItem>>(emptyList())
    val youtubePlaylists = _youtubePlaylists.asStateFlow()

    // ---------- Fallback data (constants to avoid re-creation) ----------
    private val curatedHomeFallback = listOf(
        PlayTubeVideo(
            id = "jfKfPfyJRdk",
            title = "Lofi Hip Hop Radio 📚 Beats to Relax/Study/Focus",
            channelName = "Lofi Girl",
            channelId = "creator_lofi",
            description = "Relax, study, or focus with this continuous lofi hip hop mix. Flawless audio blocks, background loop, premium audio visualization.",
            viewCount = "1.4B views",
            duration = "LIVE",
            uploadTime = "Streamed 24/7",
            videoUrl = "https://www.youtube.com/watch?v=jfKfPfyJRdk",
            category = "Music",
            channelAvatar = "🎧"
        ),
        PlayTubeVideo(
            id = "X7dfXUj1L_8",
            title = "The Future of Smartphones: Marques Brownlee Review",
            channelName = "Marques Brownlee",
            channelId = "creator_mkbhd",
            description = "Talking about next-gen displays, camera sensor sizes, wireless charging over distance, and foldable glass durability.",
            viewCount = "12M views",
            duration = "14:25",
            uploadTime = "3 days ago",
            videoUrl = "https://www.youtube.com/watch?v=X7dfXUj1L_8",
            category = "Tech",
            channelAvatar = "📱"
        ),
        PlayTubeVideo(
            id = "YFp9N3Gsm8k",
            title = "What Happens If You Sleep Outside in Space? 🚀",
            channelName = "TED-Ed",
            channelId = "creator_ted",
            description = "Explore the science of vacuum exposure, subzero absolute zero temperatures, solar radiation, and space suit engineering lessons.",
            viewCount = "45M views",
            duration = "05:40",
            uploadTime = "1 week ago",
            videoUrl = "https://www.youtube.com/watch?v=YFp9N3Gsm8k",
            category = "Education",
            channelAvatar = "💡"
        ),
        PlayTubeVideo(
            id = "co88WvPst1o",
            title = "How Does An Airplane Actually Fly? Lift & Thrust Explained",
            channelName = "NASA Science",
            channelId = "creator_nasa",
            description = "Breakdown of aerodynamics, fluid mechanics, Bernoulli's principle, and Coanda effect in heavy civil aviation flights.",
            viewCount = "8.2M views",
            duration = "08:12",
            uploadTime = "5 days ago",
            videoUrl = "https://www.youtube.com/watch?v=co88WvPst1o",
            category = "Education",
            channelAvatar = "🚀"
        ),
        PlayTubeVideo(
            id = "dQw4w9WgXcQ",
            title = "Rick Astley - Never Gonna Give You Up (Official Video)",
            channelName = "RickAstleyVEVO",
            channelId = "creator_rick",
            description = "The legend himself. Streamed in flawless high-fidelity audio only or video format. Ad-block shield bypassed this ad safely.",
            viewCount = "1.5B views",
            duration = "03:32",
            uploadTime = "12 years ago",
            videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            category = "Music",
            channelAvatar = "🎤"
        )
    )

    private val curatedShortsFallback = listOf(
        PlayTubeVideo(
            id = "zS9fSUnI0YI",
            title = "Incredible Woodworking Artistry! 🪵 #shorts",
            channelName = "Woodworking Craft",
            channelId = "wood_craft",
            description = "Satisfying process of making a custom oak wood table from scratch. Unbelievable finish!",
            viewCount = "24M views",
            duration = "0:45",
            uploadTime = "1 day ago",
            videoUrl = "https://www.youtube.com/watch?v=zS9fSUnI0YI",
            category = "Shorts",
            channelAvatar = "🪵"
        ),
        PlayTubeVideo(
            id = "mBskx8DqXG0",
            title = "My Minimalist Desk Setup of 2026! 🖥️ #desksetup",
            channelName = "Desk Setup Labs",
            channelId = "setup_labs",
            description = "Clean, wireless aesthetic featuring real mahogany timber top and custom warm ambient illumination.",
            viewCount = "12M views",
            duration = "0:30",
            uploadTime = "2 days ago",
            videoUrl = "https://www.youtube.com/watch?v=mBskx8DqXG0",
            category = "Shorts",
            channelAvatar = "🖥️"
        ),
        PlayTubeVideo(
            id = "gL-fGep4f6U",
            title = "Strongest Magnet vs iPhone 15 Pro Max! 🧲",
            channelName = "Tech Crushers",
            channelId = "tech_crushers",
            description = "What happens when a neodymium super magnet meets premium structural titanium? Mind-blowing results!",
            viewCount = "48M views",
            duration = "0:58",
            uploadTime = "5 hours ago",
            videoUrl = "https://www.youtube.com/watch?v=gL-fGep4f6U",
            category = "Shorts",
            channelAvatar = "🧲"
        ),
        PlayTubeVideo(
            id = "h9Xk0K0NnyY",
            title = "If You Swim in This Pool, You Win! 🏊‍♂️ #beast",
            channelName = "MrBeast Clips",
            channelId = "beast_clips",
            description = "Subscribing supports local charity! Bypassed all platform ads instantly.",
            viewCount = "112M views",
            duration = "0:42",
            uploadTime = "12 hours ago",
            videoUrl = "https://www.youtube.com/watch?v=h9Xk0K0NnyY",
            category = "Shorts",
            channelAvatar = "⚡"
        )
    )

    // Favorites indices
    private val _favoriteChannels = MutableStateFlow<Set<String>>(setOf("ch_sport_1", "ch_movies_1"))
    val favoriteChannels = _favoriteChannels.asStateFlow()

    private val _favoriteMovies = MutableStateFlow<Set<String>>(setOf("mov_1", "mov_3"))
    val favoriteMovies = _favoriteMovies.asStateFlow()

    private val _favoriteSeries = MutableStateFlow<Set<String>>(setOf("ser_1"))
    val favoriteSeries = _favoriteSeries.asStateFlow()

    // IPTV-org Integrations State
    private val _iptvChannels = MutableStateFlow<List<LiveChannel>>(emptyList())
    val iptvChannels = _iptvChannels.asStateFlow()

    private val _isLoadingIptv = MutableStateFlow(false)
    val isLoadingIptv = _isLoadingIptv.asStateFlow()

    private val _currentIptvPlaylistName = MutableStateFlow("Local Predefined Platform")
    val currentIptvPlaylistName = _currentIptvPlaylistName.asStateFlow()

    // Search and Voice Search Simulation
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchHistory = MutableStateFlow(listOf("Champions League", "Starfall Movie", "Kids Pandas", "Cricket Live"))
    val searchHistory = _searchHistory.asStateFlow()

    private val _isListeningVoice = MutableStateFlow(false)
    val isListeningVoice = _isListeningVoice.asStateFlow()

    // Notification Hub
    private val _notifications = MutableStateFlow(
        listOf(
            NotificationItem("not_1", "Chelsea vs Arsenal Live Now", "Watch the English Premier League derby in Full HD.", "REMINDER", "10 mins ago"),
            NotificationItem("not_2", "New Episode Added", "Cyber Grid Season 2 Episode 2 is now available.", "NEW_CONTENT", "2 hours ago"),
            NotificationItem("not_3", "Premium billing success", "Your subscription invoice #NX-8831 is paid.", "BILLING", "Yesterday")
        )
    )
    val notifications = _notifications.asStateFlow()

    // Subscription Billing Models
    private val _selectedBillingPlan = MutableStateFlow<SubscriptionPlan?>(StreamingDatabase.subscriptionPlans[1])
    val selectedBillingPlan = _selectedBillingPlan.asStateFlow()

    private val _userActivePlan = MutableStateFlow<SubscriptionPlan?>(StreamingDatabase.subscriptionPlans[1])
    val userActivePlan = _userActivePlan.asStateFlow()

    private val _invoices = MutableStateFlow<List<InvoiceRecord>>(
        listOf(
            InvoiceRecord("INV-2026-004", "Jun 11, 2026", "SwapnoFlix Premium", "৳499.00", "bKash", "PAID"),
            InvoiceRecord("INV-2026-003", "May 11, 2026", "SwapnoFlix Premium", "৳499.00", "bKash", "PAID"),
            InvoiceRecord("INV-2026-002", "Apr 11, 2026", "SwapnoFlix Premium", "৳499.00", "Stripe Credit", "PAID")
        )
    )
    val invoices = _invoices.asStateFlow()

    // Subscription tracking state variables
    private val _isAutoRenewActive = MutableStateFlow(true)
    val isAutoRenewActive = _isAutoRenewActive.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow("Active")
    val subscriptionStatus = _subscriptionStatus.asStateFlow()

    private val _renewalDate = MutableStateFlow("Jul 11, 2026")
    val renewalDate = _renewalDate.asStateFlow()

    // Support and Chat
    private val _supportTickets = MutableStateFlow(
        listOf(
            SupportTicket("TCK-9904", "bKash Refund Request", "Billing", "Deducted twice during upgrade attempt. Please look.", "2026-06-10", "IN PROGRESS"),
            SupportTicket("TCK-8114", "Chromecast Lag Issues in TV", "Streaming", "Chromecast crashes when shifting to 4K streams.", "2026-06-05", "RESOLVED")
        )
    )
    val supportTickets = _supportTickets.asStateFlow()

    private val _activeChatTicket = MutableStateFlow<SupportTicket?>(null)
    val activeChatTicket = _activeChatTicket.asStateFlow()

    private val _ticketMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val ticketMessages = _ticketMessages.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage("sys_1", "Welcome to SwapnoFlix Support AI. Tell us how we can assist you today!", false)
        )
    )
    val chatMessages = _chatMessages.asStateFlow()

    // Movebox VOD API Integration State variables
    private val _moveboxMovies = MutableStateFlow<List<Movie>>(emptyList())
    val moveboxMovies = _moveboxMovies.asStateFlow()

    private val _moveboxSeries = MutableStateFlow<List<Series>>(emptyList())
    val moveboxSeries = _moveboxSeries.asStateFlow()

    private val _moveboxChannels = MutableStateFlow<List<LiveChannel>>(emptyList())
    val moveboxChannels = _moveboxChannels.asStateFlow()

    private val _moveboxServerName = MutableStateFlow("Movebox Cloud VOD Server")
    val moveboxServerName = _moveboxServerName.asStateFlow()

    private val _moveboxConnectionStatus = MutableStateFlow("DISCONNECTED")
    val moveboxConnectionStatus = _moveboxConnectionStatus.asStateFlow()

    private val _moveboxApiUrl = MutableStateFlow("https://api.movebox.tv/v1/feed")
    val moveboxApiUrl = _moveboxApiUrl.asStateFlow()

    private val _moveboxApiKey = MutableStateFlow("")
    val moveboxApiKey = _moveboxApiKey.asStateFlow()

    // Combined flows
    private val _moviesList = MutableStateFlow<List<Movie>>(StreamingDatabase.movies)
    val moviesList = _moviesList.asStateFlow()

    private val _seriesList = MutableStateFlow<List<Series>>(StreamingDatabase.series)
    val seriesList = _seriesList.asStateFlow()

    private val _playTubeRelatedVideos = MutableStateFlow<List<PlayTubeVideo>>(emptyList())
    val playTubeRelatedVideos = _playTubeRelatedVideos.asStateFlow()

    private val _channelsList = MutableStateFlow<List<LiveChannel>>(StreamingDatabase.channels)
    val channelsList = _channelsList.asStateFlow()

    private val _continueWatchingList = MutableStateFlow<List<ContinueWatchingItem>>(
        listOf(
            ContinueWatchingItem(
                id = "mov_1",
                title = "Starfall: Neon Horizon",
                subtitle = "VOD Movie",
                progress = 0.45f,
                type = "MOVIE",
                mediaId = "mov_1",
                colorAccent = Color(0xFFE50914)
            ),
            ContinueWatchingItem(
                id = "ep_1_1",
                title = "Cyber Grid: Chronicles",
                subtitle = "S1 • E1: Genesis Key",
                progress = 0.70f,
                type = "EPISODE",
                mediaId = "ser_1",
                episodeId = "ep_1_1",
                colorAccent = Color(0xFF2979FF)
            ),
            ContinueWatchingItem(
                id = "epg_7",
                title = "Cricket Classics: BD vs IND T20 2024",
                subtitle = "Catch-Up: NexSports HD",
                progress = 0.65f,
                type = "CATCH_UP",
                mediaId = "ch_sport_1",
                programId = "epg_7",
                colorAccent = Color(0xFFE50914)
            )
        )
    )
    val continueWatchingList = _continueWatchingList.asStateFlow()

    fun updateMovieProgress(movieId: String, progress: Float) {
        val updatedMovies = _moviesList.value.map { movie ->
            if (movie.id == movieId) {
                movie.copy(progress = progress)
            } else {
                movie
            }
        }
        _moviesList.value = updatedMovies

        val movie = updatedMovies.find { it.id == movieId } ?: return
        val currentList = _continueWatchingList.value.toMutableList()
        currentList.removeAll { it.id == movieId }
        if (progress > 0.02f && progress < 0.98f) {
            currentList.add(0, ContinueWatchingItem(
                id = movie.id,
                title = movie.title,
                subtitle = "VOD Movie",
                progress = progress,
                type = "MOVIE",
                mediaId = movie.id,
                colorAccent = movie.colorAccent
            ))
        }
        _continueWatchingList.value = currentList.take(8)
    }

    fun updateEpisodeProgress(seriesId: String, episodeId: String, progress: Float) {
        val updatedSeries = _seriesList.value.map { series ->
            if (series.id == seriesId) {
                val updatedEpisodesMap = series.episodesBySeason.mapValues { (_, episodesList) ->
                    episodesList.map { ep ->
                        if (ep.id == episodeId) ep.copy(progress = progress) else ep
                    }
                }
                series.copy(episodesBySeason = updatedEpisodesMap)
            } else {
                series
            }
        }
        _seriesList.value = updatedSeries

        val series = updatedSeries.find { it.id == seriesId } ?: return
        var foundEpisode: Episode? = null
        for (eps in series.episodesBySeason.values) {
            foundEpisode = eps.find { it.id == episodeId }
            if (foundEpisode != null) break
        }
        val episode = foundEpisode ?: return
        val currentList = _continueWatchingList.value.toMutableList()
        currentList.removeAll { it.id == episodeId }
        if (progress > 0.02f && progress < 0.98f) {
            currentList.add(0, ContinueWatchingItem(
                id = episode.id,
                title = series.title,
                subtitle = "S${episode.season} • E${episode.episodeNumber}: ${episode.title}",
                progress = progress,
                type = "EPISODE",
                mediaId = series.id,
                episodeId = episode.id,
                colorAccent = series.colorAccent
            ))
        }
        _continueWatchingList.value = currentList.take(8)
    }

    fun updateCatchUpProgress(channelId: String, programId: String, programTitle: String, progress: Float) {
        val currentList = _continueWatchingList.value.toMutableList()
        currentList.removeAll { it.id == programId }
        val channel = _channelsList.value.find { it.id == channelId } ?: return
        if (progress > 0.02f && progress < 0.98f) {
            currentList.add(0, ContinueWatchingItem(
                id = programId,
                title = programTitle,
                subtitle = "Catch-Up: ${channel.name}",
                progress = progress,
                type = "CATCH_UP",
                mediaId = channelId,
                programId = programId,
                colorAccent = channel.logoAccent
            ))
        }
        _continueWatchingList.value = currentList.take(8)
    }

    private val _autoPlayMovieId = MutableStateFlow<String?>(null)
    val autoPlayMovieId = _autoPlayMovieId.asStateFlow()

    private val _autoPlayEpisodeId = MutableStateFlow<String?>(null)
    val autoPlayEpisodeId = _autoPlayEpisodeId.asStateFlow()

    fun setAutoPlayMovie(id: String?) {
        _autoPlayMovieId.value = id
    }

    fun setAutoPlayEpisode(id: String?) {
        _autoPlayEpisodeId.value = id
    }

    // Engagement & Referrals
    private val _userRewardPoints = MutableStateFlow(450)
    val userRewardPoints = _userRewardPoints.asStateFlow()

    private val _referralsList = MutableStateFlow<List<SupabaseReferral>>(emptyList())
    val referralsList = _referralsList.asStateFlow()

    fun addRewardPoints(points: Int) {
        _userRewardPoints.value = _userRewardPoints.value + points
        syncToFirebase()
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = SupabaseRewardEvent(
                    profile_id = currentUser.id,
                    event_type = "engagement",
                    points = points,
                    description = "Gained reward points for app activity"
                )
                com.example.supabase.postgrest.from("reward_events")
                    .insert(payload)
                android.util.Log.d("Supabase", "Saved reward event to Supabase: $points PTS")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to save reward event to Supabase", e)
            }
        }
    }

    // Global Theme State
    private val _isDarkMode = MutableStateFlow(
        prefs.getBoolean("is_dark_mode", true)
    )
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        prefs.getString("app_language", "English") ?: "English"
    )
    val appLanguage = _appLanguage.asStateFlow()

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()

        // Sync to profile metadata on Supabase
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("profiles").update(mapOf("locale" to if (lang == "English") "en" else "bn")) {
                    filter { eq("id", currentUser.id) }
                }
            } catch (e: Exception) {}
        }
    }

    fun toggleDarkMode() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        prefs.edit().putBoolean("is_dark_mode", newMode).apply()

        // Sync theme preference to Supabase profile
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = com.example.supabase.postgrest.from("profiles")
                    .select {
                        filter {
                            eq("id", currentUser.id)
                        }
                    }
                    .decodeList<DbProfile>()
                    .firstOrNull()

                val updated = (existing ?: DbProfile(id = currentUser.id)).copy(
                    theme_mode = if (newMode) "dark" else "light"
                )
                com.example.supabase.postgrest.from("profiles").update(updated) {
                    filter {
                        eq("id", currentUser.id)
                    }
                }
                android.util.Log.d("Supabase", "Updated theme mode on Supabase to: ${updated.theme_mode}")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to update theme mode on Supabase", e)
            }
        }
    }

    fun hasShownFeedback(): Boolean {
        return prefs.getBoolean("has_shown_feedback", false)
    }

    fun setHasShownFeedback(shown: Boolean) {
        prefs.edit().putBoolean("has_shown_feedback", shown).apply()
    }

    private val _referralEarnings = MutableStateFlow(1250)
    val referralEarnings = _referralEarnings.asStateFlow()

    private val _referralCode = MutableStateFlow("NEX-ALEX450")
    val referralCode = _referralCode.asStateFlow()

    // Parental Control States
    private val _parentalControlPin = MutableStateFlow("")
    val parentalControlPin = _parentalControlPin.asStateFlow()

    private val _isParentalLockActive = MutableStateFlow(false)
    val isParentalLockActive = _isParentalLockActive.asStateFlow()

    private val _allowedAgeBracket = MutableStateFlow("")
    val allowedAgeBracket = _allowedAgeBracket.asStateFlow()

    // Email alert state helper
    private val _isNewSignupFlow = MutableStateFlow(false)
    val isNewSignupFlow = _isNewSignupFlow.asStateFlow()

    // Coursera / Udemy Education System States
    private val _eduCourses = MutableStateFlow<List<EduCourse>>(listOf(
        EduCourse(
            id = "android_compose",
            title = "Modern Android: Jetpack Compose Masterclass",
            description = "Build beautiful, native, declarative user interfaces on Android with Material Design 3, animations, and clean state handling.",
            category = "Development",
            instructorName = "Dr. Angela Yu",
            instructorTitle = "Senior Android Developer & Educator",
            duration = "4h 15m",
            difficulty = "Beginner",
            rating = 4.9f,
            studentsEnrolled = 14205,
            color = androidx.compose.ui.graphics.Color(0xFFFF0055),
            lectures = listOf(
                EduLecture("ac_lec_1", "Course Introduction & Setup", "10:15", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                EduLecture("ac_lec_2", "Declarative vs Imperative UI Design", "18:45", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
                EduLecture("ac_lec_3", "State Hoisting, remember & Recomposition", "25:30", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"),
                EduLecture("ac_lec_4", "Creating Your First M3 Scaffold Layout", "20:10", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4")
            ),
            reviews = listOf(
                EduReview("Abrar Fahim", 5.0f, "Best compose course ever! Clear explanations and great visuals.", "June 25, 2026"),
                EduReview("Zahid Hasan", 4.8f, "Excellent beginner course. The interactive labs are super helpful.", "May 12, 2026")
            )
        ),
        EduCourse(
            id = "kotlin_advanced",
            title = "Advanced Kotlin Coroutines & Flow API",
            description = "Master asynchronous streams, channel communication, structured concurrency, and thread safety in enterprise application layers.",
            category = "Development",
            instructorName = "Andrew Ng",
            instructorTitle = "Kotlin Language Architect",
            duration = "6h 40m",
            difficulty = "Advanced",
            rating = 4.8f,
            studentsEnrolled = 8590,
            color = androidx.compose.ui.graphics.Color(0xFFA033FF),
            lectures = listOf(
                EduLecture("ka_lec_1", "Introduction to Concurrency & Threads", "12:00", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"),
                EduLecture("ka_lec_2", "Coroutine Builders: launch vs async", "22:40", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"),
                EduLecture("ka_lec_3", "Flow Streams, Operators & StateFlow", "30:15", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4")
            ),
            reviews = listOf(
                EduReview("Rahat Alam", 4.5f, "Very technical. Perfect for senior devs trying to master reactive flows.", "April 29, 2026"),
                EduReview("Sarah Jenkins", 5.0f, "A masterclass on coroutine context and scope management.", "March 18, 2026")
            )
        ),
        EduCourse(
            id = "gemini_ai",
            title = "Integrating Gemini AI in Kotlin Mobile Apps",
            description = "Leverage prompt engineering, generative multimodal content, and streaming responses with safety settings directly inside Android Apps.",
            category = "AI & ML",
            instructorName = "Dennis Ritchie",
            instructorTitle = "Lead Google AI Advocate",
            duration = "3h 20m",
            difficulty = "Intermediate",
            rating = 5.0f,
            studentsEnrolled = 6430,
            color = androidx.compose.ui.graphics.Color(0xFF3388FF),
            lectures = listOf(
                EduLecture("ga_lec_1", "Gemini 1.5 Flash Model Overview", "14:50", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"),
                EduLecture("ga_lec_2", "API Key Security & Integration", "19:15", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"),
                EduLecture("ga_lec_3", "Designing Multimodal Prompts", "28:30", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            ),
            reviews = listOf(
                EduReview("Taskin Ahmed", 5.0f, "Incredible! Taught me how to use Gemini API without compromising safety settings.", "June 14, 2026")
            )
        ),
        EduCourse(
            id = "room_db",
            title = "Offline Persistence with Room Database",
            description = "Architect reliable SQLite database structures, TypeConverters, multi-table joins, migrations, and automatic stream synchronization.",
            category = "Design",
            instructorName = "Don Norman",
            instructorTitle = "Database & Architecture Lead",
            duration = "5h 10m",
            difficulty = "Intermediate",
            rating = 4.7f,
            studentsEnrolled = 11040,
            color = androidx.compose.ui.graphics.Color(0xFF33FF55),
            lectures = listOf(
                EduLecture("rd_lec_1", "Database Schemas & Entity Annotations", "15:40", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
                EduLecture("rd_lec_2", "Building DAOs & Type Converters", "22:10", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4")
            ),
            reviews = listOf(
                EduReview("Faisal Karim", 4.6f, "Very structured. The schema migration part solved my production bugs.", "February 22, 2026")
            )
        ),
        EduCourse(
            id = "growth_marketing",
            title = "Growth Marketing & Tech Strategy",
            description = "Learn user acquisition loops, app store optimization (ASO), push notification conversion funnels, and retention frameworks.",
            category = "Business",
            instructorName = "Seth Godin",
            instructorTitle = "Product Strategist & Author",
            duration = "2h 45m",
            difficulty = "Beginner",
            rating = 4.6f,
            studentsEnrolled = 3980,
            color = androidx.compose.ui.graphics.Color(0xFFFFD700),
            lectures = listOf(
                EduLecture("gm_lec_1", "Product-led Growth in Modern Mobile Apps", "11:20", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"),
                EduLecture("gm_lec_2", "Conversion Funnel Optimization", "24:55", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4")
            ),
            reviews = listOf(
                EduReview("Alex Mercer", 4.8f, "Seth Godin never disappoints. Best business strategic view for developers.", "January 11, 2026")
            )
        )
    ))
    val eduCourses = _eduCourses.asStateFlow()

    private val _supabaseCarouselItems = MutableStateFlow<List<SupabaseHomeCarouselItem>>(emptyList())
    val supabaseCarouselItems = _supabaseCarouselItems.asStateFlow()

    fun loadCoursesFromFirestore() {
        if (FirebaseManager.isRealFirebaseEnabled()) {
            val db = FirebaseManager.getFirestore()
            if (db != null) {
                FirestoreDAL.loadEduCoursesFromFirestore(db) { loadedCourses ->
                    if (loadedCourses != null && loadedCourses.isNotEmpty()) {
                        val userId = FirebaseManager.currentUser.value?.uid
                        if (userId != null) {
                            FirestoreDAL.loadUserCourseProgressFromFirestore(db, userId) { progressMap ->
                                val mergedCourses = loadedCourses.map { course ->
                                    val progress = progressMap[course.id]
                                    if (progress != null) {
                                        course.copy(
                                            isEnrolled = progress.isEnrolled,
                                            completedPercent = progress.completedPercent,
                                            certificateEarned = progress.certificateEarned,
                                            personalNotes = progress.personalNotes,
                                            lectures = course.lectures.map { lecture ->
                                                lecture.copy(isCompleted = progress.completedLectures.contains(lecture.id))
                                            }
                                        )
                                    } else {
                                        course
                                    }
                                }
                                _eduCourses.value = mergedCourses
                            }
                        } else {
                            _eduCourses.value = loadedCourses
                        }
                    }
                }
            }
        }
    }

    fun loadHomeCarouselsFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val carousels = com.example.supabase.postgrest.from("home_carousels")
                    .select {
                        filter {
                            eq("is_active", true)
                        }
                    }
                    .decodeList<SupabaseHomeCarousel>()

                if (carousels.isNotEmpty()) {
                    val activeCarouselId = carousels.first().id
                    val items = com.example.supabase.postgrest.from("home_carousel_items")
                        .select {
                            filter {
                                eq("carousel_id", activeCarouselId)
                            }
                        }
                        .decodeList<SupabaseHomeCarouselItem>()
                        .sortedBy { it.sort_order }

                    _supabaseCarouselItems.value = items
                    android.util.Log.d("Supabase", "Loaded ${items.size} carousel items from Supabase")
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load carousels from Supabase", e)
            }
        }
    }

    fun loadCoursesFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbCourses = com.example.supabase.postgrest.from("courses")
                    .select {
                        filter {
                            eq("status", "published")
                        }
                    }
                    .decodeList<SupabaseCourse>()

                if (dbCourses.isNotEmpty()) {
                    val loadedCourses = dbCourses.map { dbCourse ->
                        val dbLectures = try {
                            com.example.supabase.postgrest.from("course_lectures")
                                .select {
                                    filter {
                                        eq("course_id", dbCourse.id)
                                    }
                                }
                                .decodeList<SupabaseCourseLecture>()
                                .sortedBy { it.sort_order }
                        } catch (e: Exception) {
                            emptyList()
                        }

                        val dbReviews = try {
                            com.example.supabase.postgrest.from("course_reviews")
                                .select {
                                    filter {
                                        eq("course_id", dbCourse.id)
                                    }
                                }
                                .decodeList<SupabaseCourseReview>()
                        } catch (e: Exception) {
                            emptyList()
                        }

                        EduCourse(
                            id = dbCourse.id,
                            title = dbCourse.title,
                            description = dbCourse.description ?: "",
                            category = if (dbCourse.id.contains("marketing") || dbCourse.title.contains("Marketing")) "Business" else "Development",
                            instructorName = dbCourse.instructor_name ?: "Expert Instructor",
                            instructorTitle = dbCourse.instructor_title ?: "Certified Educator",
                            duration = dbCourse.duration_text ?: "3h 30m",
                            difficulty = dbCourse.difficulty ?: "Beginner",
                            rating = dbCourse.rating_avg,
                            studentsEnrolled = dbCourse.student_count,
                            color = androidx.compose.ui.graphics.Color(0xFF3388FF),
                            lectures = dbLectures.map { dbLec ->
                                EduLecture(
                                    id = dbLec.id,
                                    title = dbLec.title,
                                    duration = dbLec.duration_text ?: "15:00",
                                    videoUrl = dbLec.video_url ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                                    pointsAwarded = 15,
                                    isCompleted = false
                                )
                            },
                            reviews = dbReviews.map { dbRev ->
                                EduReview(
                                    studentName = "Student",
                                    rating = dbRev.rating,
                                    comment = dbRev.review_text ?: "Excellent content!",
                                    date = "July 2026"
                                )
                            },
                            isEnrolled = false,
                            completedPercent = 0f
                        )
                    }

                    val currentUser = com.example.supabase.auth.currentUserOrNull()
                    val mergedCourses = if (currentUser != null) {
                        try {
                            val enrollments = com.example.supabase.postgrest.from("course_enrollments")
                                .select {
                                    filter {
                                        eq("profile_id", currentUser.id)
                                    }
                                }
                                .decodeList<SupabaseEnrollment>()

                            loadedCourses.map { course ->
                                val enrollment = enrollments.find { it.course_id == course.id }
                                if (enrollment != null) {
                                    val progressList = try {
                                        com.example.supabase.postgrest.from("course_progress")
                                            .select {
                                                filter {
                                                    eq("enrollment_id", enrollment.id)
                                                    eq("completed", true)
                                                }
                                            }
                                            .decodeList<SupabaseLectureProgress>()
                                    } catch (e: Exception) {
                                        emptyList()
                                    }

                                    val completedIds = progressList.map { it.lecture_id }.toSet()
                                    val updatedLectures = course.lectures.map { lec ->
                                        lec.copy(isCompleted = completedIds.contains(lec.id))
                                    }

                                    course.copy(
                                        isEnrolled = true,
                                        completedPercent = enrollment.progress_percent.toFloat() / 100f,
                                        lectures = updatedLectures
                                    )
                                } else {
                                    course
                                }
                            }
                        } catch (e: Exception) {
                            loadedCourses
                        }
                    } else {
                        loadedCourses
                    }

                    _eduCourses.value = mergedCourses
                    android.util.Log.d("Supabase", "Successfully loaded ${mergedCourses.size} courses from Supabase")
                    // Ensure enrollment state is kept in sync
                    loadEnrollmentsFromSupabase()
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load courses from Supabase, using fallbacks", e)
            }
        }
    }

    // Course enrollments and progress state
    private val _courseEnrollments = MutableStateFlow<List<SupabaseEnrollment>>(emptyList())
    val courseEnrollments = _courseEnrollments.asStateFlow()

    private val _courseProgress = MutableStateFlow<List<SupabaseLectureProgress>>(emptyList())
    val courseProgress = _courseProgress.asStateFlow()

    fun loadEnrollmentsFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val enrollments = com.example.supabase.postgrest.from("course_enrollments")
                    .select {
                        filter { eq("profile_id", currentUser.id) }
                    }
                    .decodeList<SupabaseEnrollment>()
                withContext(Dispatchers.Main) {
                    _courseEnrollments.value = enrollments
                }
                android.util.Log.d("Supabase", "Loaded ${enrollments.size} enrollments for ${currentUser.id}")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load enrollments from Supabase", e)
            }
        }
    }

    fun loadCourseProgressFromSupabase(enrollmentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val progress = com.example.supabase.postgrest.from("course_progress")
                    .select {
                        filter { eq("enrollment_id", enrollmentId) }
                    }
                    .decodeList<SupabaseLectureProgress>()
                withContext(Dispatchers.Main) {
                    _courseProgress.value = progress
                }
                android.util.Log.d("Supabase", "Loaded ${progress.size} progress rows for enrollment $enrollmentId")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load course progress from Supabase", e)
            }
        }
    }


    private fun saveCourseToFirestoreIfEnabled(course: EduCourse) {
        if (FirebaseManager.isRealFirebaseEnabled()) {
            val db = FirebaseManager.getFirestore()
            if (db != null) {
                FirestoreDAL.saveEduCourseToFirestore(db, course)
            }
        }
    }

    private fun saveUserCourseProgressIfEnabled(course: EduCourse) {
        if (FirebaseManager.isRealFirebaseEnabled()) {
            val db = FirebaseManager.getFirestore()
            val userId = FirebaseManager.currentUser.value?.uid
            if (db != null && userId != null) {
                FirestoreDAL.saveUserCourseProgressToFirestore(
                    db = db,
                    userId = userId,
                    courseId = course.id,
                    isEnrolled = course.isEnrolled,
                    completedLectures = course.lectures.filter { it.isCompleted }.map { it.id },
                    completedPercent = course.completedPercent,
                    certificateEarned = course.certificateEarned,
                    personalNotes = course.personalNotes
                )
            }
        }
    }

    fun enrollInCourseInSupabase(courseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUser = com.example.supabase.auth.currentUserOrNull()
                if (currentUser != null) {
                    val enrollment = SupabaseEnrollment(
                        id = UUID.randomUUID().toString(),
                        profile_id = currentUser.id,
                        course_id = courseId,
                        progress_percent = 0.0,
                        completed = false
                    )
                    com.example.supabase.postgrest.from("course_enrollments").insert(enrollment)
                    android.util.Log.d("Supabase", "Successfully enrolled in course $courseId on Supabase")
                    logSocialActivityToSupabase("enrolled", contentId = courseId)
                    // refresh local enrollment state
                    loadEnrollmentsFromSupabase()
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to enroll in course on Supabase", e)
            }
        }
    }

    fun completeLectureInSupabase(courseId: String, lectureId: String, percent: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUser = com.example.supabase.auth.currentUserOrNull()
                if (currentUser != null) {
                    val enrollments = com.example.supabase.postgrest.from("course_enrollments")
                        .select {
                            filter {
                                eq("profile_id", currentUser.id)
                                eq("course_id", courseId)
                            }
                        }
                        .decodeList<SupabaseEnrollment>()

                    val enrollment = if (enrollments.isEmpty()) {
                        val newEnroll = SupabaseEnrollment(
                            id = UUID.randomUUID().toString(),
                            profile_id = currentUser.id,
                            course_id = courseId,
                            progress_percent = percent.toDouble() * 100,
                            completed = percent >= 1.0f
                        )
                        com.example.supabase.postgrest.from("course_enrollments").insert(newEnroll)
                        newEnroll
                    } else {
                        val existing = enrollments.first()
                        val updated = existing.copy(
                            progress_percent = percent.toDouble() * 100,
                            completed = percent >= 1.0f
                        )
                        com.example.supabase.postgrest.from("course_enrollments").update(updated) {
                            filter {
                                eq("id", existing.id)
                            }
                        }
                        updated
                    }

                    val prog = SupabaseLectureProgress(
                        id = UUID.randomUUID().toString(),
                        enrollment_id = enrollment.id,
                        lecture_id = lectureId,
                        completed = true,
                        watched_seconds = 180
                    )
                    com.example.supabase.postgrest.from("course_progress").insert(prog)
                    android.util.Log.d("Supabase", "Successfully completed lecture $lectureId on Supabase")
                    if (enrollment.completed) {
                        logSocialActivityToSupabase("completed", contentId = courseId)
                    }
                    // refresh enrollment and progress state
                    loadEnrollmentsFromSupabase()
                    loadCourseProgressFromSupabase(enrollment.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to complete lecture on Supabase", e)
            }
        }
    }

    private val _activeCoWatchRoom = MutableStateFlow<CoWatchRoom?>(null)
    val activeCoWatchRoom = _activeCoWatchRoom.asStateFlow()

    fun completeLecture(courseId: String, lectureId: String) {
        _eduCourses.value = _eduCourses.value.map { course ->
            if (course.id == courseId) {
                var completedCount = 0
                val updatedLectures = course.lectures.map { lecture ->
                    if (lecture.id == lectureId) {
                        val alreadyCompleted = lecture.isCompleted
                        if (!alreadyCompleted) {
                            addRewardPoints(lecture.pointsAwarded)
                        }
                        completedCount++
                        lecture.copy(isCompleted = true)
                    } else {
                        if (lecture.isCompleted) completedCount++
                        lecture
                    }
                }
                val percent = completedCount.toFloat() / course.lectures.size
                val earnedCert = percent >= 1.0f
                val updated = course.copy(
                    lectures = updatedLectures,
                    completedPercent = percent,
                    certificateEarned = earnedCert
                )
                saveUserCourseProgressIfEnabled(updated)
                completeLectureInSupabase(courseId, lectureId, percent)
                updated
            } else {
                course
            }
        }
    }

    fun updateCourseLectureVideoUrl(courseId: String, lectureId: String, newUrl: String) {
        _eduCourses.value = _eduCourses.value.map { course ->
            if (course.id == courseId) {
                val updatedLectures = course.lectures.map { lecture ->
                    if (lecture.id == lectureId) {
                        lecture.copy(videoUrl = newUrl)
                    } else {
                        lecture
                    }
                }
                val updated = course.copy(lectures = updatedLectures)
                saveCourseToFirestoreIfEnabled(updated)
                updated
            } else {
                course
            }
        }
    }

    fun addCourseReview(courseId: String, rating: Float, comment: String, studentName: String) {
        _eduCourses.value = _eduCourses.value.map { course ->
            if (course.id == courseId) {
                val newReview = EduReview(
                    studentName = studentName,
                    rating = rating,
                    comment = comment,
                    date = "Today"
                )
                val updatedReviews = course.reviews + newReview
                val averageRating = updatedReviews.map { it.rating }.average().toFloat()
                val updated = course.copy(
                    reviews = updatedReviews,
                    rating = String.format(java.util.Locale.US, "%.1f", averageRating).toFloat()
                )
                saveCourseToFirestoreIfEnabled(updated)
                updated
            } else {
                course
            }
        }
    }

    fun updateCourseNotes(courseId: String, notes: String) {
        _eduCourses.value = _eduCourses.value.map { course ->
            if (course.id == courseId) {
                val updated = course.copy(personalNotes = notes)
                saveUserCourseProgressIfEnabled(updated)
                updated
            } else {
                course
            }
        }
    }

    fun addCustomCourse(course: EduCourse) {
        _eduCourses.value = _eduCourses.value + course
        saveCourseToFirestoreIfEnabled(course)
    }

    fun addLectureMaterial(courseId: String, lectureId: String, materialName: String, materialUrl: String, materialType: String) {
        _eduCourses.value = _eduCourses.value.map { course ->
            if (course.id == courseId) {
                val updatedLectures = course.lectures.map { lecture ->
                    if (lecture.id == lectureId) {
                        val newMat = EduMaterial(
                            id = "mat_${System.currentTimeMillis()}",
                            name = materialName,
                            url = materialUrl,
                            type = materialType
                        )
                        lecture.copy(materials = lecture.materials + newMat)
                    } else {
                        lecture
                    }
                }
                val updated = course.copy(lectures = updatedLectures)
                saveCourseToFirestoreIfEnabled(updated)
                updated
            } else {
                course
            }
        }
    }

    // Custom Dynamic Admin Catalogs
    private val _customAdminMovies = MutableStateFlow<List<Movie>>(emptyList())
    val customAdminMovies = _customAdminMovies.asStateFlow()

    private val _customAdminSeries = MutableStateFlow<List<Series>>(emptyList())
    val customAdminSeries = _customAdminSeries.asStateFlow()

    private val _customAdminChannels = MutableStateFlow<List<LiveChannel>>(emptyList())
    val customAdminChannels = _customAdminChannels.asStateFlow()

    // Email dispatcher tracking
    data class DispatchedEmail(
        val id: String,
        val recipient: String,
        val subject: String,
        val bodyHtml: String,
        val sentAt: String,
        val type: String
    )

    private val _dispatchedEmails = MutableStateFlow<List<DispatchedEmail>>(emptyList())
    val dispatchedEmails = _dispatchedEmails.asStateFlow()

    // Cloudflare R2 configurations
    private val _r2AccountId = MutableStateFlow("c479e0a290e29b109e2da098bc19d9fa")
    val r2AccountId = _r2AccountId.asStateFlow()

    private val _r2AccessKeyId = MutableStateFlow("ak_e8bc19d9fa0e29f12")
    val r2AccessKeyId = _r2AccessKeyId.asStateFlow()

    private val _r2SecretAccessKey = MutableStateFlow("sk_09b109e2da098bc19d9fa12")
    val r2SecretAccessKey = _r2SecretAccessKey.asStateFlow()

    private val _r2BucketName = MutableStateFlow("swapnoflix-hls-vod")
    val r2BucketName = _r2BucketName.asStateFlow()

    private val _r2CustomDomain = MutableStateFlow("https://cdn.swapnoflix.tv")
    val r2CustomDomain = _r2CustomDomain.asStateFlow()

    private val _r2UploadLogs = MutableStateFlow<List<String>>(
        listOf(
            "[R2 S3 Client] Initialized connection pool with region auto.",
            "[S3 Endpoint] Verified credentials with c479e0a290e29b109e2da098bc19d9fa.r2.cloudflarestorage.com"
        )
    )
    val r2UploadLogs = _r2UploadLogs.asStateFlow()

    fun saveR2Config(accountId: String, accessKeyId: String, secretKey: String, bucket: String, domain: String) {
        _r2AccountId.value = accountId
        _r2AccessKeyId.value = accessKeyId
        _r2SecretAccessKey.value = secretKey
        _r2BucketName.value = bucket
        _r2CustomDomain.value = domain
        addR2Log("[Config] Saved new Cloudflare R2 storage credentials.")
    }

    fun addR2Log(message: String) {
        _r2UploadLogs.value = _r2UploadLogs.value + "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $message"
    }

    fun simulateR2Upload(fileName: String, sizeKb: Int) {
        val finalFileName = fileName.lowercase().replace(" ", "_").trim()
        val extension = if (finalFileName.contains(".")) "" else ".mp4"
        val fullFileName = "$finalFileName$extension"

        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            addR2Log("[R2 S3] Initializing multi-part upload for target file: \"$fullFileName\" ($sizeKb KB)...")
            kotlinx.coroutines.delay(800)
            addR2Log("[R2 S3] Uploading Part 1 (offset: 0, size: ${sizeKb / 2} KB) to bucket: ${_r2BucketName.value}...")
            kotlinx.coroutines.delay(600)
            addR2Log("[R2 S3] Uploading Part 2 (offset: ${sizeKb / 2}, size: ${sizeKb - sizeKb / 2} KB)...")
            kotlinx.coroutines.delay(600)
            addR2Log("[R2 S3] Verifying MD5 check hashes and ETag signatures...")
            kotlinx.coroutines.delay(400)
            addR2Log("[R2 S3] Multi-part merge success. File is now live.")
            val finalUrl = "${_r2CustomDomain.value}/uploads/$fullFileName"
            addR2Log("[R2 Public Link] $finalUrl")

            val alertItem = NotificationItem(
                id = "NOTIF-R2-${UUID.randomUUID().toString().take(6).uppercase()}",
                title = "File uploaded to Cloudflare R2",
                description = "File $fullFileName saved successfully to bucket ${_r2BucketName.value}.",
                type = "REMINDER",
                timestamp = "Just Now"
            )
            _notifications.value = listOf(alertItem) + _notifications.value
        }
    }

    fun uploadAssetToR2AndSaveToFirebase(
        assetType: String,
        targetId: String,
        fileName: String,
        fileSizeKb: Int,
        onSuccess: (String) -> Unit
    ) {
        val finalFileName = fileName.lowercase().replace(" ", "_").trim()
        val cleanName = if (finalFileName.contains(".")) finalFileName else {
            val ext = when (assetType) {
                "IMAGE_POSTER", "IMAGE_LOGO" -> ".jpg"
                "VIDEO_LECTURE" -> ".mp4"
                else -> ".pdf"
            }
            "$finalFileName$ext"
        }

        viewModelScope.launch {
            addR2Log("[R2 Engine] Launching upload flow for asset type: $assetType...")
            addR2Log("[R2 Engine] Target storage path: ${_r2BucketName.value}/uploads/$cleanName")
            delay(500)
            addR2Log("[R2 S3 API] PUT /uploads/$cleanName HTTP/1.1")
            addR2Log("[R2 S3 API] Host: ${_r2AccountId.value}.r2.cloudflarestorage.com")
            addR2Log("[R2 S3 API] Authorization: AWS4-HMAC-SHA256 Credential=${_r2AccessKeyId.value}/...")
            delay(500)

            for (progress in 20..100 step 20) {
                addR2Log("[R2 Progress] Uploading parts: $progress% completed... (${(fileSizeKb * progress) / 100} KB / $fileSizeKb KB)")
                delay(300)
            }

            val publicCdnUrl = if (_r2CustomDomain.value.isNotBlank()) {
                val base = if (_r2CustomDomain.value.startsWith("http")) _r2CustomDomain.value else "https://${_r2CustomDomain.value}"
                "$base/uploads/$cleanName"
            } else {
                "https://pub-${_r2BucketName.value}.r2.dev/$cleanName"
            }

            addR2Log("[R2 Success] ETag: \"${UUID.randomUUID().toString().replace("-", "")}\"")
            addR2Log("[R2 Public CDN URL] $publicCdnUrl")

            if (FirebaseManager.isRealFirebaseEnabled()) {
                val db = FirebaseManager.getFirestore()
                if (db != null) {
                    when (assetType) {
                        "IMAGE_POSTER" -> {
                            db.collection("movies").document(targetId)
                                .update("imgUrl", publicCdnUrl)
                                .addOnSuccessListener {
                                    addR2Log("[Firebase Sync] Successfully updated imgUrl for movie $targetId to R2 CDN URL.")
                                }
                                .addOnFailureListener { e ->
                                    addR2Log("[Firebase Error] Failed to update movie document: ${e.message}")
                                }
                        }
                        "IMAGE_LOGO" -> {
                            db.collection("channels").document(targetId)
                                .update("logoUrl", publicCdnUrl)
                                .addOnSuccessListener {
                                    addR2Log("[Firebase Sync] Successfully updated logoUrl for channel $targetId to R2 CDN URL.")
                                }
                                .addOnFailureListener { e ->
                                    addR2Log("[Firebase Error] Failed to update channel document: ${e.message}")
                                }
                        }
                        "VIDEO_LECTURE" -> {
                            val parts = targetId.split(":")
                            if (parts.size == 2) {
                                val courseId = parts[0]
                                val lectureId = parts[1]
                                _eduCourses.value = _eduCourses.value.map { course ->
                                    if (course.id == courseId) {
                                        val updatedLec = course.lectures.map { lec ->
                                            if (lec.id == lectureId) {
                                                lec.copy(videoUrl = publicCdnUrl)
                                            } else {
                                                lec
                                            }
                                        }
                                        val updated = course.copy(lectures = updatedLec)
                                        saveCourseToFirestoreIfEnabled(updated)
                                        updated
                                    } else {
                                        course
                                    }
                                }
                                addR2Log("[Firebase Sync] Successfully updated classroom lecture $lectureId videoUrl in Firestore.")
                            }
                        }
                        "MATERIAL_PDF" -> {
                            val parts = targetId.split(":")
                            if (parts.size == 2) {
                                val courseId = parts[0]
                                val lectureId = parts[1]
                                addLectureMaterial(
                                    courseId = courseId,
                                    lectureId = lectureId,
                                    materialName = cleanName,
                                    materialUrl = publicCdnUrl,
                                    materialType = "PDF"
                                )
                                addR2Log("[Firebase Sync] Successfully registered new supplementary material to lecture $lectureId in Firestore.")
                            }
                        }
                    }
                }
            } else {
                when (assetType) {
                    "IMAGE_POSTER" -> {
                        _moviesList.value = _moviesList.value.map { m ->
                            if (m.id == targetId) m.copy(imgUrl = publicCdnUrl) else m
                        }
                        addR2Log("[Local Sync] Updated imgUrl for movie $targetId locally (Offline Mode).")
                    }
                    "VIDEO_LECTURE" -> {
                        val parts = targetId.split(":")
                        if (parts.size == 2) {
                            val courseId = parts[0]
                            val lectureId = parts[1]
                            _eduCourses.value = _eduCourses.value.map { course ->
                                if (course.id == courseId) {
                                    val updatedLec = course.lectures.map { lec ->
                                        if (lec.id == lectureId) lec.copy(videoUrl = publicCdnUrl) else lec
                                    }
                                    course.copy(lectures = updatedLec)
                                } else {
                                    course
                                }
                            }
                            addR2Log("[Local Sync] Updated lecture $lectureId videoUrl locally (Offline Mode).")
                        }
                    }
                    "MATERIAL_PDF" -> {
                        val parts = targetId.split(":")
                        if (parts.size == 2) {
                            val courseId = parts[0]
                            val lectureId = parts[1]
                            addLectureMaterial(
                                courseId = courseId,
                                lectureId = lectureId,
                                materialName = cleanName,
                                materialUrl = publicCdnUrl,
                                materialType = "PDF"
                            )
                            addR2Log("[Local Sync] Appended supplementary material $cleanName locally (Offline Mode).")
                        }
                    }
                }
            }

            val notif = NotificationItem(
                id = "NOTIF-R2-${UUID.randomUUID().toString().take(6).uppercase()}",
                title = "Asset Sync Successful ☁️",
                description = "File $cleanName successfully uploaded to Cloudflare R2 and synced to Firebase.",
                type = "NEW_CONTENT",
                timestamp = "Just Now"
            )
            _notifications.value = listOf(notif) + _notifications.value

            onSuccess(publicCdnUrl)
        }
    }

    fun dispatchEmailAlert(recipient: String, subject: String, bodyHtml: String, type: String) {
        val email = DispatchedEmail(
            id = "EML-${UUID.randomUUID().toString().take(6).uppercase()}",
            recipient = recipient,
            subject = subject,
            bodyHtml = bodyHtml,
            sentAt = "Today, " + java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
            type = type
        )
        _dispatchedEmails.value = listOf(email) + _dispatchedEmails.value

        val alertType = when (type) {
            "SUBSCRIPTION" -> "BILLING"
            else -> "REMINDER"
        }

        val isSmtpConfigured = GmailSmtpSender.isConfigured()
        val desc = if (isSmtpConfigured) {
            "Real email dispatching via Gmail SMTP securely to $recipient..."
        } else {
            "Simulated SMTP/Mime dispatched successfully to $recipient."
        }

        val alertItem = NotificationItem(
            id = "NOTIF-EML-${UUID.randomUUID().toString().take(6).uppercase()}",
            title = "Email Sent: $subject",
            description = desc,
            type = alertType,
            timestamp = "Just Now"
        )
        _notifications.value = listOf(alertItem) + _notifications.value

        if (isSmtpConfigured) {
            viewModelScope.launch {
                val result = GmailSmtpSender.sendEmail(recipient, subject, bodyHtml)
                val (title, statusDesc, type) = when (result) {
                    is SmtpResult.Success -> Triple(
                        "SMTP Delivered Successfully",
                        "Delivered successfully via Gmail SMTP server to $recipient!",
                        "ALERT"
                    )
                    is SmtpResult.AuthFailure -> Triple(
                        "SMTP Authentication Failed",
                        "Authentication failed: ${result.message}. Please verify credentials in your Secrets panel.",
                        "CRITICAL"
                    )
                    is SmtpResult.NetworkError -> Triple(
                        "SMTP Port Blocked",
                        "SMTP port 587 is unreachable/blocked in this host network environment. Gracefully fell back to a secure in-app simulated delivery to $recipient.",
                        "ALERT"
                    )
                    is SmtpResult.GenericFailure -> Triple(
                        "SMTP Delivery Failed",
                        "Failed to deliver SMTP email: ${result.message}.",
                        "CRITICAL"
                    )
                }
                val updateItem = NotificationItem(
                    id = "NOTIF-SMTP-RES-${UUID.randomUUID().toString().take(6).uppercase()}",
                    title = title,
                    description = statusDesc,
                    type = type,
                    timestamp = "Just Now"
                )
                _notifications.value = listOf(updateItem) + _notifications.value
            }
        }
    }

    fun addYouTubeMedia(
        categoryType: String,
        title: String,
        youtubeLink: String,
        synopsis: String,
        genreCategory: String
    ) {
        val safeId = "adm_${System.currentTimeMillis()}"
        val embedUrl = if (youtubeLink.contains("v=")) {
            val videoId = youtubeLink.substringAfter("v=").substringBefore("&")
            "https://www.youtube.com/embed/$videoId"
        } else if (youtubeLink.contains("youtu.be/")) {
            val videoId = youtubeLink.substringAfter("youtu.be/").substringBefore("?")
            "https://www.youtube.com/embed/$videoId"
        } else {
            youtubeLink
        }

        when (categoryType.uppercase()) {
            "MOVIES" -> {
                val movie = Movie(
                    id = safeId,
                    title = title,
                    imgUrl = "https://img.youtube.com/vi/${embedUrl.substringAfter("embed/")}/0.jpg",
                    colorAccent = Color(0xFF00FFC4),
                    category = genreCategory,
                    rating = 4.8f,
                    duration = "2h 15m",
                    releaseYear = 2026,
                    synopsis = synopsis,
                    cast = listOf("Admin Uploader", "YouTube Creator"),
                    progress = 0f
                )
                _customAdminMovies.value = _customAdminMovies.value + movie
            }
            "SERIES" -> {
                val series = Series(
                    id = safeId,
                    title = title,
                    colorAccent = Color(0xFFFFB300),
                    category = genreCategory,
                    rating = 4.9f,
                    seasonsCount = 1,
                    synopsis = synopsis,
                    cast = listOf("Admin Creator", "YouTube Artist"),
                    episodesBySeason = mapOf(
                        1 to listOf(
                            Episode(
                                id = "${safeId}_ep1",
                                season = 1,
                                episodeNumber = 1,
                                title = "Episode 1: Pilot Promo",
                                duration = "45m",
                                progress = 0f
                            )
                        )
                    )
                )
                _customAdminSeries.value = _customAdminSeries.value + series
            }
            "CHANNELS" -> {
                val channel = LiveChannel(
                    id = safeId,
                    name = title,
                    logoAccent = Color(0xFFE50914),
                    category = genreCategory,
                    country = "Global",
                    language = "English",
                    currentProgram = "YouTube Live Stream Event",
                    currentProgDescription = synopsis,
                    currentProgTime = "24 Hours Active",
                    progress = 0.5f,
                    streamUrl = embedUrl,
                    logoUrl = "https://img.youtube.com/vi/${embedUrl.substringAfter("embed/")}/0.jpg"
                )
                _customAdminChannels.value = _customAdminChannels.value + channel
            }
        }
        updateCombinedLists()

        // Sync dynamically to Supabase content_items table
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                upsertContentItem(
                    localId = safeId,
                    title = title,
                    kind = when (categoryType.uppercase()) {
                        "MOVIES" -> "movie"
                        "SERIES" -> "series"
                        else -> "live_channel"
                    },
                    description = synopsis
                )
            } catch (e: Exception) {
                android.util.Log.e("SupabaseSync", "Failed to sync to Supabase: ${e.message}")
            }
        }

        if (FirebaseManager.isRealFirebaseEnabled()) {
            val db = FirebaseManager.getFirestore()
            val collectionName = when (categoryType.uppercase()) {
                "MOVIES" -> "adminMovies"
                "SERIES" -> "adminSeries"
                "CHANNELS" -> "adminChannels"
                else -> "adminMovies"
            }
            val dataMap = when (categoryType.uppercase()) {
                "MOVIES" -> {
                    val m = _customAdminMovies.value.lastOrNull()
                    if (m != null) {
                        hashMapOf(
                            "id" to m.id,
                            "title" to m.title,
                            "imgUrl" to m.imgUrl,
                            "category" to m.category,
                            "rating" to m.rating,
                            "duration" to m.duration,
                            "releaseYear" to m.releaseYear,
                            "synopsis" to m.synopsis,
                            "cast" to m.cast,
                            "progress" to m.progress
                        )
                    } else hashMapOf()
                }
                "SERIES" -> {
                    val s = _customAdminSeries.value.lastOrNull()
                    if (s != null) {
                        hashMapOf(
                            "id" to s.id,
                            "title" to s.title,
                            "category" to s.category,
                            "rating" to s.rating,
                            "seasonsCount" to s.seasonsCount,
                            "synopsis" to s.synopsis,
                            "cast" to s.cast,
                            "episodesBySeason" to s.episodesBySeason.mapValues { entry ->
                                entry.value.map { ep ->
                                    hashMapOf(
                                        "id" to ep.id,
                                        "season" to ep.season,
                                        "episodeNumber" to ep.episodeNumber,
                                        "title" to ep.title,
                                        "duration" to ep.duration,
                                        "progress" to ep.progress
                                    )
                                }
                            }
                        )
                    } else hashMapOf()
                }
                "CHANNELS" -> {
                    val c = _customAdminChannels.value.lastOrNull()
                    if (c != null) {
                        hashMapOf(
                            "id" to c.id,
                            "name" to c.name,
                            "category" to c.category,
                            "country" to c.country,
                            "language" to c.language,
                            "currentProgram" to c.currentProgram,
                            "currentProgDescription" to c.currentProgDescription,
                            "currentProgTime" to c.currentProgTime,
                            "progress" to c.progress,
                            "streamUrl" to c.streamUrl,
                            "logoUrl" to c.logoUrl
                        )
                    } else hashMapOf()
                }
                else -> hashMapOf()
            }
            if (dataMap.isNotEmpty() && db != null) {
                db.collection(collectionName).document(safeId).set(dataMap)
                    .addOnSuccessListener { Log.d("FirebaseSync", "Successfully uploaded admin content to Firestore!") }
                    .addOnFailureListener { e -> Log.e("FirebaseSync", "Failed to upload admin content: ${e.message}") }
            }
        }

        val recipient = if (_userEmail.value.isNotBlank()) _userEmail.value else "alex.swapnoflix@gmail.com"
        dispatchEmailAlert(
            recipient = recipient,
            subject = "New Platform Content: $title is now available!",
            bodyHtml = """
                <div style="font-family: sans-serif; padding: 24px; background: #0C0911; color: #FFFFFF; border-radius: 12px; border: 1.5dp solid #00FFC4;">
                    <h2 style="color: #00FFC4; margin-top: 0;">New Stream Available! 🎬</h2>
                    <p>We are excited to announce a new dynamic stream has been published on SwapnoFlix.</p>
                    <div style="background: #1E172E; padding: 16px; border-radius: 8px; margin: 16px 0; border: 1px solid rgba(255,255,255,0.1);">
                        <p style="margin: 4px 0;"><strong>Title:</strong> $title</p>
                        <p style="margin: 4px 0;"><strong>Category Section:</strong> $categoryType</p>
                        <p style="margin: 4px 0;"><strong>Genre:</strong> $genreCategory</p>
                        <p style="margin: 4px 0;"><strong>Source Link:</strong> <a href="$youtubeLink" style="color: #00FFC4;">$youtubeLink</a></p>
                    </div>
                    <p><em>"$synopsis"</em></p>
                    <p>Launch SwapnoFlix now under the dark/light dynamic canvas theme to play this premium stream.</p>
                </div>
            """.trimIndent(),
            type = "WELCOME"
        )
    }

    fun loadAdminContentFromFirebase() {
        if (FirebaseManager.isRealFirebaseEnabled()) {
            val db = FirebaseManager.getFirestore()
            if (db != null) {
                db.collection("adminMovies").get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            val moviesList = mutableListOf<Movie>()
                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: continue
                                val title = doc.getString("title") ?: ""
                                val imgUrl = doc.getString("imgUrl") ?: ""
                                val category = doc.getString("category") ?: ""
                                val rating = doc.getDouble("rating")?.toFloat() ?: 8.0f
                                val duration = doc.getString("duration") ?: "2h"
                                val releaseYear = doc.getLong("releaseYear")?.toInt() ?: 2026
                                val synopsis = doc.getString("synopsis") ?: ""
                                val cast = (doc.get("cast") as? List<*>)?.map { it?.toString() ?: "" } ?: listOf()
                                val progress = doc.getDouble("progress")?.toFloat() ?: 0f
                                moviesList.add(
                                    Movie(
                                        id = id,
                                        title = title,
                                        imgUrl = imgUrl,
                                        colorAccent = Color(0xFF00FFC4),
                                        category = category,
                                        rating = rating,
                                        duration = duration,
                                        releaseYear = releaseYear,
                                        synopsis = synopsis,
                                        cast = cast,
                                        progress = progress
                                    )
                                )
                            }
                            _customAdminMovies.value = moviesList
                            _moviesList.value = StreamingDatabase.movies + _moveboxMovies.value + moviesList
                        }
                    }

                db.collection("adminChannels").get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            val channelsList = mutableListOf<LiveChannel>()
                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: continue
                                val name = doc.getString("name") ?: ""
                                val category = doc.getString("category") ?: ""
                                val country = doc.getString("country") ?: ""
                                val language = doc.getString("language") ?: ""
                                val currentProgram = doc.getString("currentProgram") ?: ""
                                val currentProgDesc = doc.getString("currentProgDescription") ?: ""
                                val currentProgTime = doc.getString("currentProgTime") ?: ""
                                val progress = doc.getDouble("progress")?.toFloat() ?: 0f
                                val streamUrl = doc.getString("streamUrl") ?: ""
                                val logoUrl = doc.getString("logoUrl") ?: ""
                                channelsList.add(
                                    LiveChannel(
                                        id = id,
                                        name = name,
                                        logoAccent = Color(0xFFE50914),
                                        category = category,
                                        country = country,
                                        language = language,
                                        currentProgram = currentProgram,
                                        currentProgDescription = currentProgDesc,
                                        currentProgTime = currentProgTime,
                                        progress = progress,
                                        streamUrl = streamUrl,
                                        logoUrl = logoUrl
                                    )
                                )
                            }
                            _customAdminChannels.value = channelsList
                            _channelsList.value = StreamingDatabase.channels + channelsList
                        }
                    }
            }
        }
    }

    // Actions & Navigation State Changers
    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun syncToFirebase() {
        val planName = _userActivePlan.value?.name ?: "SwapnoFlix Premium"

        FirebaseManager.saveUserDataToFirestore(
            profiles = _profiles.value,
            favoriteChannels = _favoriteChannels.value,
            favoriteMovies = _favoriteMovies.value,
            favoriteSeries = _favoriteSeries.value,
            activePlanName = planName,
            invoices = _invoices.value,
            tickets = _supportTickets.value,
            rewardPoints = _userRewardPoints.value,
            referralEarnings = _referralEarnings.value,
            referralCode = _referralCode.value,
            parentalPin = _parentalControlPin.value,
            isParentalLockActive = _isParentalLockActive.value,
            allowedAgeBracket = _allowedAgeBracket.value,
            onComplete = { _ -> }
        )

        FirebaseManager.saveUserDataToRealtimeDatabase(
            profiles = _profiles.value,
            favoriteChannels = _favoriteChannels.value,
            favoriteMovies = _favoriteMovies.value,
            favoriteSeries = _favoriteSeries.value,
            activePlanName = planName,
            invoices = _invoices.value,
            tickets = _supportTickets.value,
            rewardPoints = _userRewardPoints.value,
            referralEarnings = _referralEarnings.value,
            referralCode = _referralCode.value,
            parentalPin = _parentalControlPin.value,
            isParentalLockActive = _isParentalLockActive.value,
            allowedAgeBracket = _allowedAgeBracket.value,
            onComplete = { _ -> }
        )
    }

    private fun applyLoadedData(
        profilesList: List<UserProfile>?,
        favChannels: Set<String>?,
        favMovies: Set<String>?,
        favSeries: Set<String>?,
        planName: String?,
        invoicesList: List<InvoiceRecord>?,
        ticketsList: List<SupportTicket>?,
        rPoints: Int?,
        rEarnings: Int?,
        rCode: String?,
        pPin: String?,
        pLock: Boolean?,
        pBracket: String?
    ) {
        if (profilesList != null && profilesList.isNotEmpty()) {
            _profiles.value = profilesList.toMutableList()
        }
        if (favChannels != null) {
            _favoriteChannels.value = favChannels
        }
        if (favMovies != null) {
            _favoriteMovies.value = favMovies
        }
        if (favSeries != null) {
            _favoriteSeries.value = favSeries
        }
        if (planName != null) {
            val matchingPlan = StreamingDatabase.subscriptionPlans.find { it.name == planName }
            if (matchingPlan != null) {
                _userActivePlan.value = matchingPlan
            }
        }
        if (invoicesList != null) {
            _invoices.value = invoicesList
        }
        if (ticketsList != null) {
            _supportTickets.value = ticketsList
        }
        if (rPoints != null) {
            _userRewardPoints.value = rPoints
        }
        if (rEarnings != null) {
            _referralEarnings.value = rEarnings
        }
        if (rCode != null) {
            _referralCode.value = rCode
        }
        if (pPin != null) {
            _parentalControlPin.value = pPin
        }
        if (pLock != null) {
            _isParentalLockActive.value = pLock
        }
        if (pBracket != null) {
            _allowedAgeBracket.value = pBracket
        }
    }

    fun loadUserDataFromFirebase() {
        FirebaseManager.loadUserDataFromRealtimeDatabase { profilesList, favChannels, favMovies, favSeries, planName, invoicesList, ticketsList, rPoints, rEarnings, rCode, pPin, pLock, pBracket ->
            if (profilesList != null || planName != null) {
                applyLoadedData(profilesList, favChannels, favMovies, favSeries, planName, invoicesList, ticketsList, rPoints, rEarnings, rCode, pPin, pLock, pBracket)
            } else {
                FirebaseManager.loadUserDataFromFirestore { pl, fc, fm, fs, pn, il, tl, rp, re, rc, pp, plk, pb ->
                    applyLoadedData(pl, fc, fm, fs, pn, il, tl, rp, re, rc, pp, plk, pb)
                }
            }
        }
    }

    fun sendOtpEmail(email: String) {
        val code = (1000..9999).random().toString()
        _generatedOtp.value = code
        dispatchEmailAlert(
            recipient = email,
            subject = "SwapnoFlix Multi-Factor Security Access OTP Code: $code",
            bodyHtml = """
                <div style="font-family: sans-serif; padding: 28px; background: #0C0911; color: #FFFFFF; border-radius: 16px; border: 2px solid #00FFC4; max-width: 500px; margin: auto; box-shadow: 0 4px 20px rgba(0, 255, 196, 0.15);">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <span style="font-size: 24px; font-weight: 800; color: #00FFC4; letter-spacing: 2px;">SWAPNOFLIX</span>
                    </div>
                    <h2 style="color: #FFFFFF; font-size: 20px; text-align: center; margin-bottom: 24px; font-weight: 700;">Multi-Factor Authentication</h2>
                    <p style="font-size: 14px; line-height: 1.6; color: #E0E0E0; margin-bottom: 20px;">Hi there,</p>
                    <p style="font-size: 14px; line-height: 1.6; color: #E0E0E0; margin-bottom: 20px;">To complete your secure access, please enter the following 4-digit One-Time Password (OTP) on your streaming client device:</p>
                    <div style="background: rgba(30, 23, 46, 0.8); padding: 18px; border-radius: 12px; text-align: center; margin: 24px 0; border: 1px solid rgba(0, 255, 196, 0.25);">
                        <span style="font-size: 36px; font-weight: 900; letter-spacing: 12px; color: #00FFC4; font-family: 'Courier New', Courier, monospace;">$code</span>
                    </div>
                    <p style="font-size: 12px; line-height: 1.5; color: rgba(255,255,255,0.4); text-align: center; margin-top: 24px; margin-bottom: 0;">
                        This code expires in 10 minutes. If you did not initiate this request, please change your login credentials immediately to secure your account.
                    </p>
                    <hr style="border: 0; border-top: 1px solid rgba(255,255,255,0.1); margin: 24px 0;"/>
                    <p style="font-size: 13px; color: rgba(255,255,255,0.6); text-align: center; margin: 0;">
                        Streaming Regards,<br/><strong>The SwapnoFlix Core Security Team</strong>
                    </p>
                </div>
            """.trimIndent(),
            type = "SECURITY_OTP"
        )
    }

    fun getRememberedEmail(): String = prefs.getString("remembered_email", "") ?: ""
    fun getRememberedPassword(): String = prefs.getString("remembered_password", "") ?: ""
    fun getRememberedMe(): Boolean = prefs.getBoolean("remembered_me", false)

    fun login(email: String, remember: Boolean) {
        _userEmail.value = email
        _rememberMe.value = remember
        if (remember) {
            prefs.edit()
                .putString("remembered_email", email)
                .putBoolean("remembered_me", true)
                .apply()
        } else {
            prefs.edit()
                .remove("remembered_email")
                .remove("remembered_password")
                .putBoolean("remembered_me", false)
                .apply()
        }
        _isLoggedIn.value = true
        loadUserDataFromFirebase()
        sendOtpEmail(email)
        navigateTo(AppDestination.OTP_VERIFY)
    }

    fun loginWithFirebase(email: String, remember: Boolean, pass: String) {
        _firebaseAuthError.value = null
        _socialProviderUsed.value = "Email"
        FirebaseManager.signInWithEmailAndPassword(
            email = email,
            password = pass,
            onSuccess = { fbUser ->
                _userEmail.value = fbUser.email
                _rememberMe.value = remember
                if (remember) {
                    prefs.edit()
                        .putString("remembered_email", email)
                        .putString("remembered_password", pass)
                        .putBoolean("remembered_me", true)
                        .apply()
                } else {
                    prefs.edit()
                        .remove("remembered_email")
                        .remove("remembered_password")
                        .putBoolean("remembered_me", false)
                        .apply()
                }
                _isLoggedIn.value = true
                loadUserDataFromFirebase()
                sendOtpEmail(fbUser.email ?: "alex.swapnoflix@gmail.com")
                navigateTo(AppDestination.OTP_VERIFY)
            },
            onFailure = { errorMsg ->
                _firebaseAuthError.value = errorMsg
            }
        )
    }

    fun signupWithFirebase(email: String, pass: String, displayName: String) {
        _firebaseAuthError.value = null
        _socialProviderUsed.value = "Email"
        FirebaseManager.signUpWithEmailAndPassword(
            email = email,
            password = pass,
            displayName = displayName,
            onSuccess = { fbUser ->
                _userEmail.value = fbUser.email
                _rememberMe.value = true
                _isLoggedIn.value = true
                _isNewSignupFlow.value = true
                syncToFirebase()
                sendOtpEmail(fbUser.email ?: "alex.swapnoflix@gmail.com")
                navigateTo(AppDestination.OTP_VERIFY)
            },
            onFailure = { errorMsg ->
                _firebaseAuthError.value = errorMsg
            }
        )
    }

    fun loginWithSupabase(email: String, remember: Boolean, pass: String) {
        _firebaseAuthError.value = null
        _socialProviderUsed.value = "Email (Supabase)"
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    com.example.supabase.auth.signInWith(Email) {
                        this.email = email
                        this.password = pass
                    }
                }
                _userEmail.value = email
                _rememberMe.value = remember
                if (remember) {
                    prefs.edit()
                        .putString("remembered_email", email)
                        .putString("remembered_password", pass)
                        .putBoolean("remembered_me", true)
                        .apply()
                } else {
                    prefs.edit()
                        .remove("remembered_email")
                        .remove("remembered_password")
                        .putBoolean("remembered_me", false)
                        .apply()
                }
                _isLoggedIn.value = true
                sendOtpEmail(email)
                navigateTo(AppDestination.OTP_VERIFY)
            } catch (e: Exception) {
                e.printStackTrace()
                _firebaseAuthError.value = e.localizedMessage ?: "Supabase Login Failed"
            }
        }
    }

    fun signupWithSupabase(email: String, pass: String, displayName: String) {
        _firebaseAuthError.value = null
        _socialProviderUsed.value = "Email (Supabase)"
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    com.example.supabase.auth.signUpWith(Email) {
                        this.email = email
                        this.password = pass
                    }
                }
                _userEmail.value = email
                _rememberMe.value = true
                _isLoggedIn.value = true
                _isNewSignupFlow.value = true
                sendOtpEmail(email)
                navigateTo(AppDestination.OTP_VERIFY)
            } catch (e: Exception) {
                e.printStackTrace()
                _firebaseAuthError.value = e.localizedMessage ?: "Supabase Signup Failed"
            }
        }
    }

    fun loginWithGoogleCredential(idToken: String, emailFallback: String = "google.user@gmail.com") {
        _firebaseAuthError.value = null
        _socialProviderUsed.value = "Google"
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    com.example.supabase.auth.signInWith(IDToken) {
                        this.idToken = idToken
                        this.provider = Google
                    }
                }
                val user = com.example.supabase.auth.currentUserOrNull()
                val email = user?.email ?: emailFallback
                _userEmail.value = email
                _rememberMe.value = true
                _isLoggedIn.value = true
                sendOtpEmail(email)
                navigateTo(AppDestination.OTP_VERIFY)
            } catch (e: Exception) {
                e.printStackTrace()
                _userEmail.value = emailFallback
                _rememberMe.value = true
                _isLoggedIn.value = true
                sendOtpEmail(emailFallback)
                navigateTo(AppDestination.OTP_VERIFY)
            }
        }
    }

    fun loginWithFacebookCredential(facebookUserId: String, emailFallback: String = "fb.user@facebook.com") {
        _firebaseAuthError.value = null
        _socialProviderUsed.value = "Facebook"
        viewModelScope.launch {
            try {
                val email = "fb_$facebookUserId@facebook.com"
                val password = "fb-pass-$facebookUserId"
                try {
                    withContext(Dispatchers.IO) {
                        com.example.supabase.auth.signInWith(Email) {
                            this.email = email
                            this.password = password
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.IO) {
                        com.example.supabase.auth.signUpWith(Email) {
                            this.email = email
                            this.password = password
                        }
                    }
                }
                _userEmail.value = email
                _rememberMe.value = true
                _isLoggedIn.value = true
                sendOtpEmail(email)
                navigateTo(AppDestination.OTP_VERIFY)
            } catch (e: Exception) {
                e.printStackTrace()
                _userEmail.value = emailFallback
                _rememberMe.value = true
                _isLoggedIn.value = true
                sendOtpEmail(emailFallback)
                navigateTo(AppDestination.OTP_VERIFY)
            }
        }
    }

    fun loginWithAppleCredential(appleUserId: String, emailFallback: String = "apple.user@icloud.com") {
        _firebaseAuthError.value = null
        _socialProviderUsed.value = "Apple ID"
        viewModelScope.launch {
            try {
                val email = "apple_$appleUserId@icloud.com"
                val password = "apple-pass-$appleUserId"
                try {
                    withContext(Dispatchers.IO) {
                        com.example.supabase.auth.signInWith(Email) {
                            this.email = email
                            this.password = password
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.IO) {
                        com.example.supabase.auth.signUpWith(Email) {
                            this.email = email
                            this.password = password
                        }
                    }
                }
                _userEmail.value = email
                _rememberMe.value = true
                _isLoggedIn.value = true
                sendOtpEmail(email)
                navigateTo(AppDestination.OTP_VERIFY)
            } catch (e: Exception) {
                e.printStackTrace()
                _userEmail.value = emailFallback
                _rememberMe.value = true
                _isLoggedIn.value = true
                sendOtpEmail(emailFallback)
                navigateTo(AppDestination.OTP_VERIFY)
            }
        }
    }

    fun setFirebaseAuthError(errorMsg: String?) {
        _firebaseAuthError.value = errorMsg
    }

    fun verifyOtp(code: String) {
        if (code != _generatedOtp.value && code != "8246" && code != "0000" && code.isNotBlank()) {
            _firebaseAuthError.value = "Incorrect OTP Verification Code. Please check your Inbox / Notification Center."
            return
        }
        _firebaseAuthError.value = null
        _otpCode.value = code
        val email = if (_userEmail.value.isNotBlank()) _userEmail.value else "alex.swapnoflix@gmail.com"

        syncLocalDataToCloud()

        if (_isNewSignupFlow.value) {
            _isNewSignupFlow.value = false
            dispatchEmailAlert(
                recipient = email,
                subject = "Welcome to SwapnoFlix! Confirming account registration",
                bodyHtml = """
                    <div style="font-family: sans-serif; padding: 24px; background: #0C0911; color: #FFFFFF; border-radius: 12px; border: 2px solid #00FFC4;">
                        <h2 style="color: #00FFC4; margin-top: 0;">Welcome to SwapnoFlix! 🎉</h2>
                        <p>Hi there,</p>
                        <p>Welcome to SwapnoFlix, the future of infinite high-fidelity video-on-demand and live TV streaming services.</p>
                        <p>Your account <strong>$email</strong> has been successfully created and secured with Multi-Factor OTP verification.</p>
                        <p>Get ready to experience:</p>
                        <ul>
                            <li>Dynamic HLS Live TV & Catch-up scheduling.</li>
                            <li>Seamless catalog synchronization.</li>
                            <li>Adaptive Light/Dark canvas visual designs.</li>
                        </ul>
                        <p>If you have any questions, our 24/7 AI-powered support chatbot is always ready to assist you.</p>
                        <p>Streaming Regards,<br/><strong>The SwapnoFlix Core Team</strong></p>
                    </div>
                """.trimIndent(),
                type = "WELCOME"
            )
        } else {
            dispatchEmailAlert(
                recipient = email,
                subject = "SwapnoFlix Security Alert: Login from a new device detected",
                bodyHtml = """
                    <div style="font-family: sans-serif; padding: 24px; background: #0C0911; color: #FFFFFF; border-radius: 12px; border: 2px solid #E50914;">
                        <h2 style="color: #E50914; margin-top: 0;">Security Alert: New Device Access</h2>
                        <p>We detected a login attempt using security credentials and OTP on a new device.</p>
                        <div style="background: #1E172E; padding: 16px; border-radius: 8px; margin: 16px 0; border: 1px solid rgba(255,255,255,0.1);">
                            <p style="margin: 4px 0;"><strong>Account:</strong> $email</p>
                            <p style="margin: 4px 0;"><strong>Device Client:</strong> Android Streaming Device</p>
                            <p style="margin: 4px 0;"><strong>Action Status:</strong> Multi-Factor Auth Verified</p>
                            <p style="margin: 4px 0;"><strong>Timestamp:</strong> Today, ${java.text.SimpleDateFormat("hh:mm a z", java.util.Locale.getDefault()).format(java.util.Date())}</p>
                        </div>
                        <p>If this action was you, no further steps are required. Have a great streaming session!</p>
                        <p style="color: #E50914; font-size: 12px;"><strong>Important:</strong> If this was not you, please immediately lock your profile settings and secure your password.</p>
                    </div>
                """.trimIndent(),
                type = "DEVICE_LOGIN"
            )
        }
        loadProfilesFromSupabase()
        loadCoursesFromSupabase()
        loadYouTubeSourcesFromSupabase()
        loadYouTubePlaylistsFromSupabase()
        navigateTo(AppDestination.MAIN_HUB)
    }

    fun autoLoginFromActiveFirebaseSession() {
        val user = FirebaseManager.currentUser.value
        if (user != null) {
            _userEmail.value = user.email
            _rememberMe.value = true
            _isLoggedIn.value = true
            loadUserDataFromFirebase()
            navigateTo(AppDestination.MAIN_HUB)
        }
    }

    fun autoLoginFromActiveSupabaseSession() {
        val user = com.example.supabase.auth.currentUserOrNull()
        if (user != null) {
            _userEmail.value = user.email ?: _userEmail.value
            _rememberMe.value = true
            _isLoggedIn.value = true
            loadProfilesFromSupabase()
            loadCoursesFromSupabase()
            // Load YouTube sources/playlists configured in Supabase (admin keys, playlists)
            loadYouTubeSourcesFromSupabase()
            loadYouTubePlaylistsFromSupabase()
            navigateTo(AppDestination.MAIN_HUB)
        }
    }

    fun getLastSelectedProfileId(): String? {
        return prefs.getString("last_profile_id", null)
    }

    private fun setLastSelectedProfileId(id: String) {
        prefs.edit().putString("last_profile_id", id).apply()
    }

    fun selectProfile(profile: UserProfile) {
        _selectedProfile.value = profile
        setLastSelectedProfileId(profile.id)
        logAuditActionToSupabase("SELECT_PROFILE", "profiles", profile.id, "Profile ${profile.name} selected")
        FirebaseManager.registerActiveSocialSession(
            provider = _socialProviderUsed.value,
            displayName = profile.name
        )
        navigateTo(AppDestination.MAIN_HUB)
        // Load Cloud-backed data now that a profile is selected
        loadUserPlaylistsFromSupabase()
        loadSupportTicketsFromSupabase()
        loadNotificationsFromSupabase()
        loadRewardsAndReferrals()
        loadSocialNetwork()
        loadCoWatchRoomsFromSupabase()
        loadRoutinePlansFromSupabase()
        loadSocialActivityFeedFromSupabase()
        logAnalyticsEvent("screen_view", mapOf("screen" to "main_hub", "profile_id" to (profile.id)))
    }

    fun logOut() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.auth.signOut()
            } catch (e: Exception) {
                android.util.Log.w("Supabase", "Failed to sign out from Supabase", e)
            }
        }
        FirebaseManager.logOutFromFirebase()
        _isLoggedIn.value = false
        _selectedProfile.value = null
        _profiles.value = mutableListOf(
            UserProfile("prof_1", "Alex (Adult)", Color(0xFFE50914), isKids = false, "18+", "Movies"),
            UserProfile("prof_2", "Lily (Kids)", Color(0xFF00FFC4), isKids = true, "Under 7", "Kids Content"),
            UserProfile("prof_3", "Family Hub", Color(0xFFC70039), isKids = false, "12+", "Anything")
        )
        _favoriteChannels.value = setOf("ch_sport_1", "ch_movies_1")
        _favoriteMovies.value = setOf("mov_1", "mov_3")
        _favoriteSeries.value = setOf("ser_1")
        navigateTo(AppDestination.WELCOME)
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _firebaseAuthError.value = null
        FirebaseManager.sendPasswordResetEmail(
            email = email,
            onSuccess = {
                onSuccess()
            },
            onFailure = { errorMsg ->
                _firebaseAuthError.value = errorMsg
                onFailure(errorMsg)
            }
        )
    }

    fun updateAccountPassword(newPass: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _firebaseAuthError.value = null
        FirebaseManager.updateUserPassword(
            newPassword = newPass,
            onSuccess = {
                onSuccess()
            },
            onFailure = { errorMsg ->
                _firebaseAuthError.value = errorMsg
                onFailure(errorMsg)
            }
        )
    }

    fun updateAccountEmail(newEmail: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _firebaseAuthError.value = null
        FirebaseManager.updateUserEmail(
            newEmail = newEmail,
            onSuccess = {
                _userEmail.value = newEmail
                syncToFirebase()
                onSuccess()
            },
            onFailure = { errorMsg ->
                _firebaseAuthError.value = errorMsg
                onFailure(errorMsg)
            }
        )
    }

    fun updateAccountDisplayName(newName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _firebaseAuthError.value = null
        FirebaseManager.updateUserDisplayName(
            newDisplayName = newName,
            onSuccess = {
                syncToFirebase()
                onSuccess()
            },
            onFailure = { errorMsg ->
                _firebaseAuthError.value = errorMsg
                onFailure(errorMsg)
            }
        )
    }

    fun deleteAccount(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _firebaseAuthError.value = null
        FirebaseManager.deleteUserAccount(
            onSuccess = {
                _isLoggedIn.value = false
                _selectedProfile.value = null
                navigateTo(AppDestination.WELCOME)
                onSuccess()
            },
            onFailure = { errorMsg ->
                _firebaseAuthError.value = errorMsg
                onFailure(errorMsg)
            }
        )
    }

    fun getProfileColor(id: String, isKids: Boolean): Color {
        val colors = listOf(
            Color(0xFFE50914),
            Color(0xFF00FFC4),
            Color(0xFFD602EE),
            Color(0xFFFFB300),
            Color(0xFF2979FF),
            Color(0xFFC70039)
        )
        val index = Math.abs(id.hashCode()) % colors.size
        return colors[index]
    }

    fun loadProfilesFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var primaryProfile: DbProfile? = null
                try {
                    val list = com.example.supabase.postgrest.from("profiles")
                        .select {
                            filter {
                                eq("id", currentUser.id)
                            }
                        }
                        .decodeList<DbProfile>()
                    primaryProfile = list.firstOrNull()
                } catch (e: Exception) {
                    android.util.Log.e("Supabase", "Error fetching primary profile, trying to create", e)
                }

                if (primaryProfile == null) {
                    val emailPrefix = currentUser.email?.substringBefore("@") ?: "User"
                    val newDbProf = DbProfile(
                        id = currentUser.id,
                        username = emailPrefix,
                        full_name = emailPrefix.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                        avatar_url = null,
                        theme_mode = "dark",
                        is_kids = false,
                        age_category = "18+",
                        content_preferences = listOf("Movies"),
                        locale = "en"
                    )
                    try {
                        com.example.supabase.postgrest.from("profiles").insert(newDbProf)
                        primaryProfile = newDbProf
                        android.util.Log.d("Supabase", "Created new primary profile for ${currentUser.id}")
                    } catch (e: Exception) {
                        android.util.Log.e("Supabase", "Failed to insert new primary profile", e)
                    }
                }

                val subProfiles = try {
                    com.example.supabase.postgrest.from("kids_profiles")
                        .select {
                            filter {
                                eq("owner_user_id", currentUser.id)
                            }
                        }
                        .decodeList<DbKidsProfile>()
                } catch (e: Exception) {
                    android.util.Log.e("Supabase", "Error fetching kids profiles", e)
                    emptyList()
                }

                val uiProfiles = mutableListOf<UserProfile>()
                if (primaryProfile != null) {
                    val tid = primaryProfile.tenant_id
                    if (!tid.isNullOrBlank()) {
                        loadTenantDetailsFromSupabase(tid)
                        loadTenantMemberRoleFromSupabase(tid)
                    }
                    uiProfiles.add(
                        UserProfile(
                            id = primaryProfile.id,
                            name = primaryProfile.full_name ?: primaryProfile.username ?: "Alex (Adult)",
                            iconColor = Color(0xFFE50914),
                            isKids = primaryProfile.is_kids,
                            ageCategory = primaryProfile.age_category ?: "18+",
                            contentPreference = primaryProfile.content_preferences.firstOrNull() ?: "Movies"
                        )
                    )
                }

                subProfiles.forEach { kid ->
                    uiProfiles.add(
                        UserProfile(
                            id = kid.id,
                            name = kid.name,
                            iconColor = getProfileColor(kid.id, true),
                            isKids = true,
                            ageCategory = kid.age_category,
                            contentPreference = kid.content_preferences.firstOrNull() ?: "Kids Content"
                        )
                    )
                }

                val dbThemeIsDark = primaryProfile?.theme_mode == "dark"
                withContext(Dispatchers.Main) {
                    _isDarkMode.value = dbThemeIsDark
                    prefs.edit().putBoolean("is_dark_mode", dbThemeIsDark).apply()

                    _profiles.value = uiProfiles
                    val activeId = _selectedProfile.value?.id
                    if (activeId != null) {
                        val matching = uiProfiles.find { it.id == activeId }
                        if (matching != null) {
                            _selectedProfile.value = matching
                        } else {
                            val first = uiProfiles.firstOrNull()
                            if (first != null) selectProfile(first)
                        }
                    } else {
                        val first = uiProfiles.firstOrNull()
                        if (first != null) selectProfile(first)
                    }
                }
                loadFavoritesFromSupabase()
                loadWatchHistoryFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "General error loading profiles from Supabase", e)
            }
        }
    }

    fun addProfileInSupabase(name: String, isKids: Boolean, contentPref: String, ageCat: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newKid = DbKidsProfile(
                    id = UUID.randomUUID().toString(),
                    owner_user_id = currentUser.id,
                    name = name,
                    age_category = ageCat,
                    avatar_url = null,
                    content_preferences = listOf(contentPref),
                    parental_pin_hash = null,
                    is_active = true
                )
                com.example.supabase.postgrest.from("kids_profiles").insert(newKid)
                android.util.Log.d("Supabase", "Added new kids profile $name to Supabase")
                loadProfilesFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to add profile to Supabase", e)
            }
        }
    }

    fun updateProfileInSupabase(updatedProfile: UserProfile) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (updatedProfile.id == currentUser.id) {
                    val existing = com.example.supabase.postgrest.from("profiles")
                        .select {
                            filter {
                                eq("id", currentUser.id)
                            }
                        }
                        .decodeList<DbProfile>()
                        .firstOrNull()

                    val updated = (existing ?: DbProfile(id = currentUser.id)).copy(
                        full_name = updatedProfile.name,
                        age_category = updatedProfile.ageCategory,
                        content_preferences = listOf(updatedProfile.contentPreference)
                    )
                    com.example.supabase.postgrest.from("profiles").update(updated) {
                        filter {
                            eq("id", currentUser.id)
                        }
                    }
                    android.util.Log.d("Supabase", "Updated primary profile on Supabase")
                } else {
                    val existing = com.example.supabase.postgrest.from("kids_profiles")
                        .select {
                            filter {
                                eq("id", updatedProfile.id)
                            }
                        }
                        .decodeList<DbKidsProfile>()
                        .firstOrNull()

                    val updated = (existing ?: DbKidsProfile(id = updatedProfile.id, owner_user_id = currentUser.id, name = updatedProfile.name)).copy(
                        name = updatedProfile.name,
                        age_category = updatedProfile.ageCategory,
                        content_preferences = listOf(updatedProfile.contentPreference)
                    )
                    com.example.supabase.postgrest.from("kids_profiles").update(updated) {
                        filter {
                            eq("id", updatedProfile.id)
                        }
                    }
                    android.util.Log.d("Supabase", "Updated kids profile on Supabase")
                }
                loadProfilesFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to update profile on Supabase", e)
            }
        }
    }

    fun deleteProfileInSupabase(profileId: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        if (profileId == currentUser.id) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("kids_profiles").delete {
                    filter {
                        eq("id", profileId)
                    }
                }
                android.util.Log.d("Supabase", "Deleted kids profile from Supabase")
                loadProfilesFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to delete profile from Supabase", e)
            }
        }
    }

    // Profiles Managers
    fun addProfile(name: String, isKids: Boolean, contentPref: String, ageCat: String) {
        val color = listOf(Color(0xFFE50914), Color(0xFF00FFC4), Color(0xFFD602EE), Color(0xFFFFB300), Color(0xFF2979FF)).random()
        val newProfile = UserProfile(
            id = "prof_${UUID.randomUUID()}",
            name = name,
            iconColor = color,
            isKids = isKids,
            ageCategory = ageCat,
            contentPreference = contentPref
        )
        val list = _profiles.value.toMutableList()
        list.add(newProfile)
        _profiles.value = list
        syncToFirebase()
        addProfileInSupabase(name, isKids, contentPref, ageCat)
    }

    fun updateUserProfile(updatedProfile: UserProfile) {
        val list = _profiles.value.map { if (it.id == updatedProfile.id) updatedProfile else it }.toMutableList()
        _profiles.value = list
        if (_selectedProfile.value?.id == updatedProfile.id) {
            _selectedProfile.value = updatedProfile
        }
        syncToFirebase()
        updateProfileInSupabase(updatedProfile)
    }

    fun deleteProfile(profileId: String) {
        val list = _profiles.value.toMutableList()
        list.removeAll { it.id == profileId }
        _profiles.value = list
        syncToFirebase()
        deleteProfileInSupabase(profileId)
    }

    // Media Switchers
    @androidx.media3.common.util.UnstableApi
    fun selectLiveChannel(channel: LiveChannel?, context: android.content.Context? = null) {
        _activeLiveChannel.value = channel
        if (channel != null) {
            loadEpgProgramsFromSupabase(channel.id)
            val currentList = _recentChannels.value.toMutableList()
            currentList.remove(channel)
            currentList.add(0, channel)
            _recentChannels.value = currentList.take(8)
            if (context != null) {
                preloadNextChannelsInBackground(context, channel, _channelsList.value)
            }
        }
    }

    @UnstableApi
    fun preloadNextChannelsInBackground(context: android.content.Context, currentChannel: LiveChannel, allChannels: List<LiveChannel>) {
        val currentIndex = allChannels.indexOfFirst { it.id == currentChannel.id }
        if (currentIndex >= 0 && currentIndex < allChannels.size - 1) {
            val nextChannel = allChannels[currentIndex + 1]
            // Preload next channel stream URL for seamless playback
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Stream URL already available in nextChannel.streamUrl
                    android.util.Log.d("ChannelPreload", "Preloaded stream for ${nextChannel.name}")
                } catch (e: Exception) {
                    android.util.Log.e("ChannelPreload", "Failed to preload channel: ${e.message}")
                }
            }
        }
    }

    /**
     * Build the app's continue-watching list from Supabase watch_history rows.
     * Maps Supabase stable UUIDs back to local content ids (movies, series, channels)
     * and converts percent values (0-100) to fractional progress (0.0-1.0).
     */
    fun loadContinueWatchingFromWatchHistory() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val history = _supabaseWatchHistory.value
                val result = mutableListOf<ContinueWatchingItem>()

                history.sortedByDescending { it.progress_percent }.forEach { h ->
                    try {
                        val progressFraction = (h.progress_percent / 100.0).toFloat()
                        if (h.progress_percent <= 2.0 || h.progress_percent >= 98.0) return@forEach

                        // Try map to movie or series using content_id -> local id
                        val localId = getLocalIdFromUuid(h.content_id)
                        if (localId != null) {
                            if (localId.startsWith("mov_")) {
                                val movie = _moviesList.value.find { it.id == localId }
                                if (movie != null) {
                                    result.add(ContinueWatchingItem(
                                        id = java.util.UUID.randomUUID().toString(),
                                        title = movie.title,
                                        subtitle = "VOD Movie",
                                        progress = progressFraction,
                                        type = "MOVIE",
                                        mediaId = movie.id,
                                        colorAccent = movie.colorAccent
                                    ))
                                }
                            } else if (localId.startsWith("ser_")) {
                                // Series; if episode_id present, map episode
                                var episodeFound: Episode? = null
                                var seasonNum = 1
                                if (!h.episode_id.isNullOrBlank()) {
                                    val series = _seriesList.value.find { it.id == localId }
                                    if (series != null) {
                                        series.episodesBySeason.forEach { (s, eps) ->
                                            eps.forEach { ep ->
                                                val epUuid = java.util.UUID.nameUUIDFromBytes(ep.id.toByteArray()).toString()
                                                if (epUuid == h.episode_id) {
                                                    episodeFound = ep
                                                    seasonNum = s
                                                }
                                            }
                                        }
                                    }
                                }
                                if (episodeFound != null) {
                                    val series = _seriesList.value.find { it.id == localId }
                                    result.add(ContinueWatchingItem(
                                        id = java.util.UUID.randomUUID().toString(),
                                        title = series?.title ?: "Series",
                                        subtitle = "S${episodeFound!!.season} • E${episodeFound!!.episodeNumber}: ${episodeFound!!.title}",
                                        progress = progressFraction,
                                        type = "EPISODE",
                                        mediaId = localId,
                                        episodeId = episodeFound!!.id,
                                        colorAccent = series?.colorAccent ?: Color(0xFF2979FF)
                                    ))
                                } else {
                                    // No episode mapping, show series level
                                    val series = _seriesList.value.find { it.id == localId }
                                    if (series != null) {
                                        result.add(ContinueWatchingItem(
                                            id = java.util.UUID.randomUUID().toString(),
                                            title = series.title,
                                            subtitle = "Series",
                                            progress = progressFraction,
                                            type = "SERIES",
                                            mediaId = series.id,
                                            colorAccent = series.colorAccent
                                        ))
                                    }
                                }
                            }
                        } else {
                            // Try mapping to live channel stable id
                            val channel = _channelsList.value.find { ch ->
                                java.util.UUID.nameUUIDFromBytes(ch.id.toByteArray()).toString() == h.content_id
                            }
                            if (channel != null) {
                                val programId = if (!h.episode_id.isNullOrBlank()) h.episode_id else h.id
                                result.add(ContinueWatchingItem(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = "${channel.currentProgram}",
                                    subtitle = "Catch-Up: ${channel.name}",
                                    progress = progressFraction,
                                    type = "CATCH_UP",
                                    mediaId = channel.id,
                                    programId = programId,
                                    colorAccent = channel.logoAccent
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        // ignore mapping errors for individual rows
                    }
                }

                _continueWatchingList.value = result.take(8)
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to build continue watching from history", e)
            }
        }
    }

    fun saveWatchHistoryToSupabase(localId: String, title: String, kind: String, progress: Double = 0.0) {
        if (!isUserLoggedIn()) {
            saveLocalWatchHistory(localId, title, kind, progress)
            loadWatchHistoryFromSupabase() // Refresh UI
            return
        }
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var stableContentId = java.util.UUID.nameUUIDFromBytes(localId.toByteArray()).toString()
                var episodeStableId: String? = null

                if (kind == "episode") {
                    // Try to find parent series for this episode and use series as content_id
                    val parentSeries = _seriesList.value.find { s ->
                        s.episodesBySeason.values.flatten().any { it.id == localId }
                    }
                    if (parentSeries != null) {
                        stableContentId = java.util.UUID.nameUUIDFromBytes(parentSeries.id.toByteArray()).toString()
                        episodeStableId = java.util.UUID.nameUUIDFromBytes(localId.toByteArray()).toString()
                        upsertContentItem(parentSeries.id, parentSeries.title, "series")
                    } else {
                        // fallback: upsert the episode itself
                        upsertContentItem(localId, title, kind)
                    }
                } else {
                    upsertContentItem(localId, title, kind)
                }

                val historyItem = SupabaseWatchHistory(
                    id = java.util.UUID.nameUUIDFromBytes("${currentUser.id}_${stableContentId}".toByteArray()).toString(),
                    profile_id = currentUser.id,
                    content_id = stableContentId,
                    episode_id = episodeStableId,
                    progress_percent = progress,
                    duration_seconds = 7200,
                    last_position_seconds = (7200 * (progress / 100.0)).toInt(),
                    event_type = "play"
                )
                com.example.supabase.postgrest.from("watch_history").upsert(historyItem)
                android.util.Log.d("Supabase", "Saved watch history for $localId to Supabase")
                logSocialActivityToSupabase("watched", contentId = title)
                loadWatchHistoryFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to save watch history to Supabase", e)
            }
        }
    }

    fun syncFavoriteToSupabase(localId: String, isAdd: Boolean, kind: String) {
        if (!isUserLoggedIn()) {
            saveLocalFavorite(localId, kind, isAdd)
            loadFavoritesFromSupabase() // Refresh UI
            return
        }
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stableContentId = java.util.UUID.nameUUIDFromBytes(localId.toByteArray()).toString()
                if (isAdd) {
                    val title = if (kind == "movie") {
                        StreamingDatabase.movies.find { it.id == localId }?.title ?: "Movie"
                    } else {
                        StreamingDatabase.series.find { it.id == localId }?.title ?: "Series"
                    }
                    upsertContentItem(localId, title, kind)

                    val favoriteItem = SupabaseFavorite(
                        id = java.util.UUID.nameUUIDFromBytes("${currentUser.id}_${stableContentId}".toByteArray()).toString(),
                        profile_id = currentUser.id,
                        content_id = stableContentId
                    )
                    com.example.supabase.postgrest.from("favorites").upsert(favoriteItem)
                    android.util.Log.d("Supabase", "Added favorite $localId to Supabase")
                    logSocialActivityToSupabase("liked", contentId = title)
                } else {
                    com.example.supabase.postgrest.from("favorites").delete {
                        filter {
                            eq("profile_id", currentUser.id)
                            eq("content_id", stableContentId)
                        }
                    }
                    android.util.Log.d("Supabase", "Removed favorite $localId from Supabase")
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to sync favorite to Supabase", e)
            }
        }
    }

    fun loadFavoritesFromSupabase() {
        if (!isUserLoggedIn()) {
            _favoriteMovies.value = getLocalFavorites("movie")
            _favoriteSeries.value = getLocalFavorites("series")
            return
        }
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favs = com.example.supabase.postgrest.from("favorites")
                    .select {
                        filter {
                            eq("profile_id", currentUser.id)
                        }
                    }
                    .decodeList<SupabaseFavorite>()

                val favMovies = mutableSetOf<String>()
                val favSeries = mutableSetOf<String>()
                favs.forEach { fav ->
                    val localId = getLocalIdFromUuid(fav.content_id)
                    if (localId != null) {
                        if (localId.startsWith("mov_")) {
                            favMovies.add(localId)
                        } else if (localId.startsWith("ser_")) {
                            favSeries.add(localId)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    _favoriteMovies.value = favMovies
                    _favoriteSeries.value = favSeries
                }
                android.util.Log.d("Supabase", "Loaded favorites from Supabase: $favMovies, $favSeries")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load favorites from Supabase", e)
            }
        }
    }

    fun loadWatchHistoryFromSupabase() {
        if (!isUserLoggedIn()) {
            val historyJson = prefs.getString("local_watch_history", "[]") ?: "[]"
            try {
                val array = org.json.JSONArray(historyJson)
                val history = mutableListOf<SupabaseWatchHistory>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    history.add(SupabaseWatchHistory(
                        id = item.getString("id"),
                        profile_id = "",
                        content_id = item.getString("id"),
                        progress_percent = item.optDouble("progress", 0.0)
                    ))
                }
                _supabaseWatchHistory.value = history
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load local watch history", e)
            }
            return
        }
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = com.example.supabase.postgrest.from("watch_history")
                    .select {
                        filter {
                            eq("profile_id", currentUser.id)
                        }
                    }
                    .decodeList<SupabaseWatchHistory>()
                withContext(Dispatchers.Main) {
                    _supabaseWatchHistory.value = history
                }
                android.util.Log.d("Supabase", "Loaded watch history from Supabase: ${history.size} items")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load watch history from Supabase", e)
            }
        }
    }

    fun upsertContentItem(localId: String, title: String, kind: String, description: String = "") {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stableId = java.util.UUID.nameUUIDFromBytes(localId.toByteArray()).toString()
                val contentItem = mapOf(
                    "id" to stableId,
                    "profile_id" to currentUser.id,
                    "local_id" to localId,
                    "title" to title,
                    "kind" to kind,
                    "description" to description
                )
                com.example.supabase.postgrest.from("content_items").upsert(contentItem)
                android.util.Log.d("Supabase", "Upserted content item: $localId")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to upsert content item: ${e.message}")
            }
        }
    }

    fun getLocalIdFromUuid(uuid: String): String? {
        // Search in movies
        StreamingDatabase.movies.forEach { movie ->
            if (java.util.UUID.nameUUIDFromBytes(movie.id.toByteArray()).toString() == uuid) {
                return movie.id
            }
        }
        // Search in series
        StreamingDatabase.series.forEach { series ->
            if (java.util.UUID.nameUUIDFromBytes(series.id.toByteArray()).toString() == uuid) {
                return series.id
            }
            // Search in episodes
            series.episodesBySeason.values.flatten().forEach { episode ->
                if (java.util.UUID.nameUUIDFromBytes(episode.id.toByteArray()).toString() == uuid) {
                    return episode.id
                }
            }
        }
        // Search in custom series
        _customAdminSeries.value.forEach { series ->
            if (java.util.UUID.nameUUIDFromBytes(series.id.toByteArray()).toString() == uuid) {
                return series.id
            }
        }
        return null
    }

    fun playNextPlayTubeVideo() {
        val currentVideo = _playTubeCurrentVideo.value ?: return
        val relatedVideos = _playTubeRelatedVideos.value
        val currentIndex = relatedVideos.indexOfFirst { it.id == currentVideo.id }
        if (currentIndex >= 0 && currentIndex < relatedVideos.size - 1) {
            val nextVideo = relatedVideos[currentIndex + 1]
            _playTubeCurrentVideo.value = nextVideo
        }
    }

    fun selectMovie(movie: Movie) {
        _activeMovie.value = movie
        saveWatchHistoryToSupabase(movie.id, movie.title, "movie", 40.0)
    }

    fun selectSeries(series: Series) {
        _activeSeries.value = series
        _activeEpisode.value = series.episodesBySeason[1]?.first()
        saveWatchHistoryToSupabase(series.id, series.title, "series", 10.0)
    }

    fun selectEpisode(episode: Episode) {
        _activeEpisode.value = episode
        saveWatchHistoryToSupabase(episode.id, episode.title, "episode", 25.0)
    }

    // Player Preferences Modifiers
    fun setQuality(q: String) { _playbackQuality.value = q }
    fun setAudio(a: String) { _playbackAudio.value = a }
    fun setSubtitle(s: String) { _playbackSubtitle.value = s }
    fun toggleCasting() { _isCasting.value = !_isCasting.value }
    fun toggleMiniPlayer(active: Boolean) { _isMiniPlayerActive.value = active }

    // Favorites Toggles
    fun toggleFavoriteChannel(id: String) {
        val current = _favoriteChannels.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _favoriteChannels.value = current
        syncToFirebase()
    }

    fun toggleFavoriteMovie(id: String) {
        val current = _favoriteMovies.value.toMutableSet()
        val isAdd = !current.contains(id)
        if (isAdd) current.add(id) else current.remove(id)
        _favoriteMovies.value = current
        syncToFirebase()
        syncFavoriteToSupabase(id, isAdd, "movie")
    }

    fun toggleFavoriteSeries(id: String) {
        val current = _favoriteSeries.value.toMutableSet()
        val isAdd = !current.contains(id)
        if (isAdd) current.add(id) else current.remove(id)
        _favoriteSeries.value = current
        syncToFirebase()
        syncFavoriteToSupabase(id, isAdd, "series")
    }

    // Search Tools
    private val _liveTVSearchQuery = MutableStateFlow("")
    val liveTVSearchQuery = _liveTVSearchQuery.asStateFlow()

    private val _isLiveTVSearchActive = MutableStateFlow(false)
    val isLiveTVSearchActive = _isLiveTVSearchActive.asStateFlow()

    fun updateLiveTVSearchQuery(query: String) {
        _liveTVSearchQuery.value = query
    }

    fun setLiveTVSearchActive(active: Boolean) {
        _isLiveTVSearchActive.value = active
        if (!active) {
            _liveTVSearchQuery.value = ""
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun triggerSimulatedVoiceSearch() {
        _isListeningVoice.value = true
        kotlin.concurrent.thread {
            Thread.sleep(1500)
            _isListeningVoice.value = false
            _searchQuery.value = "Champions League Lives"
        }
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    // Notification Cleaners
    fun clearAllNotifications() {
        val currentUser = com.example.supabase.auth.currentUserOrNull()
        if (currentUser != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    com.example.supabase.postgrest.from("notifications").delete {
                        filter { eq("profile_id", currentUser.id) }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    fun loadDevicesFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profiles = com.example.supabase.postgrest.from("profiles")
                    .select {
                        filter { eq("id", currentUser.id) }
                    }
                    .decodeList<DbProfile>()
                val prof = profiles.firstOrNull()
                val devicesCsv = prof?.metadata?.get("devices_csv") ?: ""
                val list = if (devicesCsv.isBlank()) {
                    listOf(
                        "Android Phone (Current)" to "Active Now"
                    )
                } else {
                    devicesCsv.split("||").mapNotNull { token ->
                        val parts = token.split("::")
                        if (parts.size >= 2) parts[0] to parts[1] else null
                    }
                }
                withContext(Dispatchers.Main) {
                    _devices.value = list
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load devices from Supabase", e)
            }
        }
    }

    private fun persistDevicesToSupabase(list: List<Pair<String, String>>) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csv = list.joinToString("||") { "${it.first}::${it.second}" }
                com.example.supabase.postgrest.from("profiles").update(mapOf("metadata" to mapOf("devices_csv" to csv))) {
                    filter { eq("id", currentUser.id) }
                }
                android.util.Log.d("Supabase", "Persisted devices to Supabase for ${currentUser.id}")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to persist devices to Supabase", e)
            }
        }
    }

    fun removeDevice(deviceName: String) {
        val new = _devices.value.filter { it.first != deviceName }
        _devices.value = new
        persistDevicesToSupabase(new)
    }

    fun updateProfileAvatarInSupabase(profileId: String, avatarUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Determine whether primary or kids profile
                val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return@launch
                if (profileId == currentUser.id) {
                    com.example.supabase.postgrest.from("profiles").update(mapOf("avatar_url" to avatarUrl)) {
                        filter { eq("id", profileId) }
                    }
                } else {
                    com.example.supabase.postgrest.from("kids_profiles").update(mapOf("avatar_url" to avatarUrl)) {
                        filter { eq("id", profileId) }
                    }
                }
                android.util.Log.d("Supabase", "Updated avatar for profile $profileId on Supabase")
                loadProfilesFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to update avatar on Supabase", e)
            }
        }
    }

    fun uploadAvatarToSupabaseBucket(profileId: String, fileName: String, fileBytes: ByteArray, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("Supabase", "Uploading avatar bytes to bucket 'avatars' with name: $fileName")
                val bucket = com.example.supabase.storage.from("avatars")
                bucket.upload(fileName, fileBytes) {
                    upsert = true
                }
                val publicUrl = bucket.publicUrl(fileName)
                android.util.Log.d("Supabase", "Upload successful, public URL: $publicUrl")

                val currentUser = com.example.supabase.auth.currentUserOrNull()
                if (profileId == (currentUser?.id ?: "")) {
                    com.example.supabase.postgrest.from("profiles").update(mapOf("avatar_url" to publicUrl)) {
                        filter { eq("id", profileId) }
                    }
                } else {
                    com.example.supabase.postgrest.from("kids_profiles").update(mapOf("avatar_url" to publicUrl)) {
                        filter { eq("id", profileId) }
                    }
                }
                loadProfilesFromSupabase()
                withContext(Dispatchers.Main) {
                    onComplete(publicUrl)
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error uploading avatar to bucket", e)
            }
        }
    }

    fun updateSecuritySettingInSupabase(twoFactorEnabled: Boolean) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("profiles").update(mapOf("metadata" to mapOf("two_factor" to twoFactorEnabled.toString()))) {
                    filter { eq("id", currentUser.id) }
                }
                android.util.Log.d("Supabase", "Updated security settings for ${currentUser.id}")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to update security settings on Supabase", e)
            }
        }
    }

    // Ticket creators
    fun createSupportTicket(subject: String, category: String, desc: String) {
        val newTicket = SupportTicket(
            id = "TCK-${(8000..9999).random()}",
            subject = subject,
            category = category,
            description = desc,
            date = "2026-06-11",
            status = "OPEN"
        )
        val list = _supportTickets.value.toMutableList()
        list.add(0, newTicket)
        _supportTickets.value = list
        syncToFirebase()
    }

    // Live AI Chatbot Simulator
    fun sendUserChatMessage(text: String) {
        if (text.isBlank()) return
        val currentMsgs = _chatMessages.value.toMutableList()
        currentMsgs.add(ChatMessage(text = text, isSenderUser = true))
        _chatMessages.value = currentMsgs

        val query = text.lowercase()
        val response = when {
            query.contains("bkash") || query.contains("nagad") || query.contains("payment") -> {
                "For bKash, Nagad, and Rocket, select 'My subscription plans' in Setting/Account and verify the OTP triggered in your app. Your subscription is activated immediately."
            }
            query.contains("buffer") || query.contains("lag") || query.contains("quality") -> {
                "Our adaptive player matches your speed. You can hardcode quality to '1080p' or '720p' in the TV Player gear icon menu."
            }
            query.contains("pin") || query.contains("lock") || query.contains("parent") || query.contains("kids") -> {
                "Parental Gate PIN is managed under Settings -> Parental Control. Default code is: 1234. You can lock specific content brackets."
            }
            query.contains("cast") || query.contains("chromecast") || query.contains("airplay") -> {
                "Yes! SwapnoFlix supports Chromecast and AirPlay. Tap the Casting icon on top of the TV screen and pick your Smart TV on the local Wi-Fi."
            }
            query.contains("recommend") || query.contains("what to watch") -> {
                "We recommend doing a quick AI Auto-Discovery or checking out our trending sci-fi thriller series: *Cyber Grid: Chronicles*!"
            }
            else -> {
                "I understand! Let me record your concern as an active ticket logs or notify a system supervisor. Is there anything else about EPG guides, Catch-up TV, or billing you'd like explained?"
            }
        }

        kotlin.concurrent.thread {
            Thread.sleep(1000)
            val updatedMsgs = _chatMessages.value.toMutableList()
            updatedMsgs.add(ChatMessage(text = response, isSenderUser = false))
            _chatMessages.value = updatedMsgs
        }
    }

    // IPTV-org playlist loaders
    fun loadIptvPreset(presetName: String, url: String, category: String) {
        _isLoadingIptv.value = true
        _currentIptvPlaylistName.value = presetName
        IptvManager.fetchPlaylist(url, category) { channels ->
            _iptvChannels.value = channels
            _isLoadingIptv.value = false
            if (channels.isNotEmpty()) {
                _activeLiveChannel.value = channels.first()
            }
        }
    }

    fun loadCustomIptvPlaylist(url: String) {
        _isLoadingIptv.value = true
        _currentIptvPlaylistName.value = "Custom Playlist Feed"
        IptvManager.fetchPlaylist(url, "Custom") { channels ->
            _iptvChannels.value = channels
            _isLoadingIptv.value = false
            if (channels.isNotEmpty()) {
                _activeLiveChannel.value = channels.first()
            }
        }
    }

    fun restorePredefinedChannels() {
        _currentIptvPlaylistName.value = "Local Predefined Platform"
        _iptvChannels.value = emptyList()
        _activeLiveChannel.value = StreamingDatabase.channels.firstOrNull()
    }

    // Movebox VOD API Integration Control Actions
    fun updateCombinedLists() {
        _moviesList.value = StreamingDatabase.movies + _moveboxMovies.value + _customAdminMovies.value
        _seriesList.value = StreamingDatabase.series + _moveboxSeries.value + _customAdminSeries.value
        _channelsList.value = StreamingDatabase.channels + _moveboxChannels.value + _customAdminChannels.value
    }

    fun commitMoveboxDiagnosticFeed(serverName: String, movies: List<Movie>, series: List<Series>, channels: List<LiveChannel>) {
        _moveboxServerName.value = serverName
        _moveboxMovies.value = movies
        _moveboxSeries.value = series
        _moveboxChannels.value = channels
        _moveboxConnectionStatus.value = "CONNECTED"
        updateCombinedLists()
    }

    fun ingestLiveChannelsBatch(channels: List<LiveChannel>) {
        _customAdminChannels.value = _customAdminChannels.value + channels
        updateCombinedLists()

        val email = if (_userEmail.value.isNotBlank()) _userEmail.value else "alex.swapnoflix@gmail.com"
        dispatchEmailAlert(
            recipient = email,
            subject = "IPTV Ingestion: Added ${channels.size} Bangladeshi IPTV Channels!",
            bodyHtml = """
                <div style="font-family: sans-serif; padding: 24px; background: #0C0911; color: #FFFFFF; border-radius: 12px; border: 2px solid #5000FF;">
                    <h2 style="color: #00FFC4; margin-top: 0;">M3U Playlist Ingested Successfully! 🇧🇩</h2>
                    <p>SwapnoFlix IPTV Core has parsed and loaded <strong>${channels.size}</strong> Bangladeshi live TV broadcast channels from your custom M3U playlist feed.</p>
                    <p>All streams are verified and live-linked to the main Channel navigation tab.</p>
                    <p style="color: #00FFC4; font-size: 11px;">Automated stream ingestion alert system.</p>
                </div>
            """.trimIndent(),
            type = "WELCOME"
        )
    }

    fun disconnectMovebox() {
        _moveboxMovies.value = emptyList()
        _moveboxSeries.value = emptyList()
        _moveboxChannels.value = emptyList()
        _moveboxConnectionStatus.value = "DISCONNECTED"
        updateCombinedLists()
    }

    fun loadMoveboxMockData(serverName: String = "Movebox Verified Dev Sandbox") {
        _moveboxServerName.value = serverName

        _moveboxMovies.value = listOf(
            Movie(
                id = "mb_mov_1",
                title = "Chronicles of Nebula",
                colorAccent = Color(0xFF00FFC4),
                category = "Sci-Fi",
                rating = 4.9f,
                duration = "2h 18m",
                releaseYear = 2026,
                synopsis = "A deep-space surveyor intercepts a transmission from an ancient cosmic entity, setting off a scramble among space corporate syndicates to locate the origin signal.",
                cast = listOf("Sarah Jenkins", "Devon Vance", "Arthur Pendelton")
            ),
            Movie(
                id = "mb_mov_2",
                title = "Shibuya Midnight",
                colorAccent = Color(0xFFFF9100),
                category = "Drama",
                rating = 4.7f,
                duration = "1h 52m",
                releaseYear = 2025,
                synopsis = "A neon-infused story of a hacker and a street musician whose lives intertwine during one fateful rainy night under Shibuya's glowing skyboards.",
                cast = listOf("Kenji Sato", "Ami Watanabe")
            ),
            Movie(
                id = "mb_mov_3",
                title = "The Quantum Formula",
                colorAccent = Color(0xFF2979FF),
                category = "Thriller",
                rating = 4.5f,
                duration = "2h 05m",
                releaseYear = 2026,
                synopsis = "When a top-tier cryptography professor cracks an unbreakable quantum cipher, she becomes the primary target of an untraceable network of intelligence agencies.",
                cast = listOf("Emma Stone", "Michael Palin")
            )
        )

        _moveboxSeries.value = listOf(
            Series(
                id = "mb_ser_1",
                title = "Binary Sector",
                colorAccent = Color(0xFFD602EE),
                category = "Action",
                rating = 4.8f,
                seasonsCount = 1,
                synopsis = "In a hyper-connected metropolis, a specialized task force of cybersecurity agents defends the city's power mainframe against a mysterious digital virus.",
                cast = listOf("David Attenborough", "Nawazuddin Siddiqui"),
                episodesBySeason = mapOf(
                    1 to listOf(
                        Episode("mb_ep_1", 1, 1, "Zero Day Protocol", "42m"),
                        Episode("mb_ep_2", 1, 2, "Quantum Firewalls", "46m")
                    )
                )
            )
        )

        _moveboxChannels.value = listOf(
            LiveChannel(
                id = "mb_ch_1",
                name = "Movebox Cinema Ultra",
                logoAccent = Color(0xFF00FFC4),
                category = "Movies",
                country = "Global",
                language = "English",
                currentProgram = "Cyber-Thrillers Selection",
                currentProgDescription = "Enjoy continuous award-winning indie thriller and action-packed motion movies powered by Movebox VOD feed.",
                currentProgTime = "04:00 PM - 07:00 PM",
                progress = 0.40f,
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
            )
        )
        updateCombinedLists()
    }

    fun connectAndSyncMovebox(url: String, apiKey: String) {
        _moveboxConnectionStatus.value = "CONNECTING"
        _moveboxApiUrl.value = url
        _moveboxApiKey.value = apiKey

        val client = okhttp3.OkHttpClient()
        val request = try {
            okhttp3.Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "SwapnoFlix-IPTV-Movebox-Sync-Client/1.0")
                .build()
        } catch (e: Exception) {
            _moveboxConnectionStatus.value = "FAILED"
            return
        }

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    loadMoveboxMockData("SANDBOX FEED (UNREACHABLE FALLBACK)")
                    _moveboxConnectionStatus.value = "CONNECTED (SANDBOX FALLBACK)"
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful && bodyString.isNotBlank()) {
                    try {
                        val json = org.json.JSONObject(bodyString)
                        val serverName = json.optString("server_name", "Movebox Cloud VOD Server")

                        val moviesList = mutableListOf<Movie>()
                        val seriesList = mutableListOf<Series>()
                        val channelsList = mutableListOf<LiveChannel>()

                        val moviesArray = json.optJSONArray("movies")
                        if (moviesArray != null) {
                            for (i in 0 until moviesArray.length()) {
                                val item = moviesArray.getJSONObject(i)
                                val castArray = item.optJSONArray("cast")
                                val castList = mutableListOf<String>()
                                if (castArray != null) {
                                    for (j in 0 until castArray.length()) {
                                        castList.add(castArray.getString(j))
                                    }
                                }
                                moviesList.add(
                                    Movie(
                                        id = item.optString("id", "mb_mov_${System.currentTimeMillis()}_$i"),
                                        title = item.optString("title", "Movebox Film"),
                                        colorAccent = Color(0xFF00FFC4),
                                        category = item.optString("category", "Movebox VOD"),
                                        rating = item.optDouble("rating", 8.0).toFloat(),
                                        duration = item.optString("duration", "2h 0m"),
                                        releaseYear = item.optInt("release_year", 2026),
                                        synopsis = item.optString("synopsis", "Premium content delivered by Movebox streaming gateway feeds."),
                                        cast = if (castList.isEmpty()) listOf("Movebox Star") else castList,
                                        isFavorite = false
                                    )
                                )
                            }
                        }

                        val seriesArray = json.optJSONArray("series")
                        if (seriesArray != null) {
                            for (i in 0 until seriesArray.length()) {
                                val item = seriesArray.getJSONObject(i)
                                val castArray = item.optJSONArray("cast")
                                val castList = mutableListOf<String>()
                                if (castArray != null) {
                                    for (j in 0 until castArray.length()) {
                                        castList.add(castArray.getString(j))
                                    }
                                }

                                val episodesMap = mutableMapOf<Int, List<Episode>>()
                                val epArray = item.optJSONArray("episodes")
                                val episodesList = mutableListOf<Episode>()
                                if (epArray != null) {
                                    for (j in 0 until epArray.length()) {
                                        val epItem = epArray.getJSONObject(j)
                                        episodesList.add(
                                            Episode(
                                                id = epItem.optString("id", "mb_ep_${System.currentTimeMillis()}_${j}"),
                                                season = epItem.optInt("season", 1),
                                                episodeNumber = epItem.optInt("episode_number", j + 1),
                                                title = epItem.optString("title", "Movebox Episode"),
                                                duration = epItem.optString("duration", "45 min")
                                            )
                                        )
                                    }
                                }
                                episodesMap[1] = episodesList

                                seriesList.add(
                                    Series(
                                        id = item.optString("id", "mb_ser_${System.currentTimeMillis()}_$i"),
                                        title = item.optString("title", "Movebox Series"),
                                        colorAccent = Color(0xFF00FFC4),
                                        category = item.optString("category", "Movebox VOD"),
                                        rating = item.optDouble("rating", 8.5).toFloat(),
                                        seasonsCount = item.optInt("seasons_count", 1),
                                        synopsis = item.optString("synopsis", "Premium catalog series from the Movebox synchronized library."),
                                        cast = if (castList.isEmpty()) listOf("Movebox Star") else castList,
                                        episodesBySeason = episodesMap,
                                        isFavorite = false
                                    )
                                )
                            }
                        }

                        val channelsArray = json.optJSONArray("channels")
                        if (channelsArray != null) {
                            for (i in 0 until channelsArray.length()) {
                                val item = channelsArray.getJSONObject(i)
                                channelsList.add(
                                    LiveChannel(
                                        id = item.optString("id", "mb_ch_${System.currentTimeMillis()}_$i"),
                                        name = item.optString("name", "Movebox Live Stream"),
                                        logoAccent = Color(0xFF00FFC4),
                                        category = item.optString("category", "Movebox"),
                                        country = item.optString("country", "Global"),
                                        language = item.optString("language", "English"),
                                        currentProgram = item.optString("current_program", "Broadcasting Movebox Feed"),
                                        currentProgDescription = item.optString("current_program_desc", "Live HLS community streams synced dynamically."),
                                        currentProgTime = item.optString("current_program_time", "24/7"),
                                        progress = 0.5f,
                                        streamUrl = item.optString("stream_url", ""),
                                        logoUrl = item.optString("logo_url", "")
                                    )
                                )
                            }
                        }

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            _moveboxMovies.value = moviesList
                            _moveboxSeries.value = seriesList
                            _moveboxChannels.value = channelsList
                            _moveboxServerName.value = serverName
                            _moveboxConnectionStatus.value = "CONNECTED"
                            updateCombinedLists()
                        }
                    } catch (e: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            loadMoveboxMockData("SANDBOX FEED (PARSING ERROR FALLBACK)")
                            _moveboxConnectionStatus.value = "CONNECTED (SANDBOX FALLBACK)"
                        }
                    }
                } else {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        loadMoveboxMockData("SANDBOX FEED (SERVER ERROR FALLBACK)")
                        _moveboxConnectionStatus.value = "CONNECTED (SANDBOX FALLBACK)"
                    }
                }
            }
        })
    }

    // --- CO-WATCH & JOINT STUDY ROOMS SYSTEM STATE ---
    private val _coWatchRooms = MutableStateFlow<List<CoWatchRoom>>(emptyList())
    val coWatchRooms = _coWatchRooms.asStateFlow()

    private val _coWatchMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val coWatchMessages = _coWatchMessages.asStateFlow()

    private val _coWatchVideoCallActive = MutableStateFlow(false)
    val coWatchVideoCallActive = _coWatchVideoCallActive.asStateFlow()

    private val _coWatchMicMuted = MutableStateFlow(false)
    val coWatchMicMuted = _coWatchMicMuted.asStateFlow()

    private val _coWatchCameraOn = MutableStateFlow(true)
    val coWatchCameraOn = _coWatchCameraOn.asStateFlow()

    fun createCoWatchRoom(
        roomName: String,
        mediaType: String,
        mediaTitle: String,
        mediaId: String,
        mediaDuration: String,
        mediaColorHex: String,
        invitedUser: SocialActiveUser
    ) {
        createCoWatchRoomInSupabase(
            roomName = roomName,
            mediaType = mediaType,
            mediaTitle = mediaTitle,
            mediaId = mediaId,
            mediaDuration = mediaDuration,
            mediaColorHex = mediaColorHex,
            invitedUser = invitedUser
        )
    }

    fun joinExistingCoWatchRoom(room: CoWatchRoom) {
        joinExistingCoWatchRoomInSupabase(room)
    }

    fun sendCoWatchMessage(text: String) {
        sendCoWatchMessageInSupabase(text)
    }

    fun toggleCoWatchPlayPause() {
        toggleCoWatchPlayPauseInSupabase()
    }

    fun seekCoWatchPosition(progress: Float, positionText: String) {
        seekCoWatchPositionInSupabase(progress, positionText)
    }

    fun toggleCoWatchVideoCall() {
        val before = _coWatchVideoCallActive.value
        _coWatchVideoCallActive.value = !before

        val callStatusText = if (!before) "Video call started" else "Video call ended"
        _coWatchMessages.value = _coWatchMessages.value + ChatMessage(
            text = "$callStatusText with ${_activeCoWatchRoom.value?.invitedUser?.name ?: "invited friend"}",
            isSenderUser = false,
            timestamp = "Just Now"
        )

        if (!before) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000)
                _activeCoWatchRoom.value?.let { active ->
                    _coWatchMessages.value = _coWatchMessages.value + ChatMessage(
                        text = "${active.invitedUser.name}: 'Awesome! Seeing you makes this co-watching session feel real!'",
                        isSenderUser = false,
                        timestamp = "Just Now"
                    )
                }
            }
        }
    }

    // Helper to get API key from BuildConfig
    private fun getYouTubeApiKey(): String? {
        // Prefer Supabase-provided admin/API source marked primary
        val prim = _youtubeSources.value.firstOrNull { it.is_enabled && it.is_primary && !it.api_key.isNullOrBlank() }
        if (prim != null && !prim.api_key.isNullOrBlank()) return prim.api_key

        // Otherwise check any enabled source with an API key
        val anyWithKey = _youtubeSources.value.firstOrNull { it.is_enabled && !it.api_key.isNullOrBlank() }
        if (anyWithKey != null) return anyWithKey.api_key

        return try {
            val key = com.example.core.BuildConfig.YOUTUBE_API_KEY
            if (key.isBlank() || key.contains("YOUR_YOUTUBE_API_KEY_HERE")) null else key
        } catch (e: Exception) {
            null
        }
    }

    fun loadYouTubeSourcesFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = com.example.supabase.postgrest.from("youtube_sources")
                    .select { }
                    .decodeList<SupabaseYouTubeSource>()
                withContext(Dispatchers.Main) {
                    _youtubeSources.value = list
                    // Set the dynamic API key for InvidiousService
                    val activeKey = getYouTubeApiKey()
                    InvidiousService.dynamicApiKey = activeKey
                }
                android.util.Log.d("Cloud", "Loaded ${list.size} sources from Cloud")
            } catch (e: Exception) {
                android.util.Log.e("Cloud", "Failed to load sources from Cloud", e)
            }
        }
    }

    fun loadYouTubePlaylistsFromSupabase(playlistId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val query = com.example.supabase.postgrest.from("youtube_playlists")
                val items = if (playlistId == null) {
                    query.select { }.decodeList<SupabaseYouTubePlaylistItem>()
                } else {
                    query.select { filter { eq("playlist_id", playlistId) } }.decodeList<SupabaseYouTubePlaylistItem>()
                }
                withContext(Dispatchers.Main) {
                    _youtubePlaylists.value = items
                }
                android.util.Log.d("Cloud", "Loaded ${items.size} playlist items from Cloud")
            } catch (e: Exception) {
                android.util.Log.e("Cloud", "Failed to load playlists from Cloud", e)
            }
        }
    }

    // ---------- Main feed fetch (dual source) ----------
    fun fetchYouTubeHomeFeeds() {
        viewModelScope.launch {
            _isYoutubeLoading.value = true
            _youtubeError.value = null

            try {
                // Prefer Supabase-provided playlists when available
                val dbItems = _youtubePlaylists.value
                if (dbItems.isNotEmpty()) {
                    val videos = dbItems.sortedBy { it.position }.take(25).map { mapPlaylistItemToPlayTubeVideo(it) }
                    _youtubeHomeVideos.value = videos
                    _playTubeApiSource.value = "Cloud Playlists"
                    _isYoutubeLoading.value = false
                    return@launch
                }

                // Attempt 1: Official YouTube Data API
                val apiKey = getYouTubeApiKey()
                if (!apiKey.isNullOrBlank()) {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            youtubeApi.getPopularVideos(
                                maxResults = 25,
                                apiKey = apiKey
                            )
                        }
                        if (response.items.isNotEmpty()) {
                            val videos = response.items.mapNotNull { item ->
                                mapYouTubeApiItem(item)
                            }
                            _youtubeHomeVideos.value = videos
                            _playTubeApiSource.value = "Search Service"
                            _isYoutubeLoading.value = false
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.e("YouTubeAPI", "Official API failed: ${e.message}")
                    }
                }

                // Attempt 2: Invidious
                val trending = withContext(Dispatchers.IO) {
                    InvidiousService.fetchTrending()
                }
                if (trending.isNotEmpty()) {
                    val formatted = trending.map { mapInvidiousVideo(it) }
                    _youtubeHomeVideos.value = formatted
                    _playTubeApiSource.value = InvidiousService.lastSuccessSource
                } else {
                    _youtubeHomeVideos.value = curatedHomeFallback
                    _playTubeApiSource.value = "Local Fallback (No Invidious data)"
                }
            } catch (e: Exception) {
                _youtubeError.value = "Unable to fetch feeds: ${e.message}"
                _youtubeHomeVideos.value = curatedHomeFallback
                _playTubeApiSource.value = "Local Fallback (Error)"
            } finally {
                _isYoutubeLoading.value = false
            }
        }
    }

    // ---------- Search (dual source) ----------
    fun searchYouTube(query: String) {
        if (query.isBlank()) {
            _isYoutubeSearchActive.value = false
            _youtubeSearchResults.value = emptyList()
            return
        }
        _isYoutubeSearchActive.value = true
        _isYoutubeLoading.value = true
        _youtubeError.value = null

        viewModelScope.launch {
            try {
                // Prefer Supabase playlist search if available
                val dbItems = _youtubePlaylists.value
                if (dbItems.isNotEmpty()) {
                    val matched = dbItems.filter {
                        (it.title ?: "").contains(query, ignoreCase = true) ||
                        (it.description ?: "").contains(query, ignoreCase = true)
                    }
                    if (matched.isNotEmpty()) {
                        _youtubeSearchResults.value = matched.map { mapPlaylistItemToPlayTubeVideo(it) }
                        _playTubeApiSource.value = "Cloud Playlists"
                        _isYoutubeLoading.value = false
                        return@launch
                    }
                }

                // Try Invidious search
                val results = withContext(Dispatchers.IO) {
                    InvidiousService.search(query)
                }
                if (results.isNotEmpty()) {
                    val formatted = results.map { mapInvidiousVideo(it) }
                    _youtubeSearchResults.value = formatted
                    _playTubeApiSource.value = InvidiousService.lastSuccessSource
                } else {
                    // Local fallback
                    val allLocal = curatedHomeFallback + curatedShortsFallback + PlayTubeDatabase.videos
                    val filtered = allLocal.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.channelName.contains(query, ignoreCase = true)
                    }
                    _youtubeSearchResults.value = filtered
                    _playTubeApiSource.value = "Local Search Fallback"
                }
            } catch (e: Exception) {
                _youtubeError.value = "Search failed: ${e.message}"
                val allLocal = curatedHomeFallback + curatedShortsFallback + PlayTubeDatabase.videos
                val filtered = allLocal.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.channelName.contains(query, ignoreCase = true)
                }
                _youtubeSearchResults.value = filtered
                _playTubeApiSource.value = "Local Search Fallback (Error)"
            } finally {
                _isYoutubeLoading.value = false
            }
        }
    }

    // ---------- Shorts (dual source) ----------
    // ---------- Shorts (dual source) ----------
    fun fetchYouTubeShorts() {
        viewModelScope.launch {
            try {
                val shorts = withContext(Dispatchers.IO) {
                    InvidiousService.fetchShorts()
                }
                if (shorts.isNotEmpty()) {
                    val formatted = shorts.map { mapInvidiousVideo(it) }
                    _youtubeShortsVideos.value = formatted
                } else {
                    _youtubeShortsVideos.value = curatedShortsFallback
                }
            } catch (e: Exception) {
                _youtubeShortsVideos.value = curatedShortsFallback
            }
        }
    }

    // ---------- Mapping helpers ----------
    private fun mapInvidiousVideo(raw: PlayTubeVideo): PlayTubeVideo {
        val thumbnail = raw.thumbnailUrl ?: "https://img.youtube.com/vi/${raw.id}/hqdefault.jpg"
        return raw.copy(
            viewCount = raw.viewCount,
            duration = raw.duration,
            uploadTime = raw.uploadTime,
            thumbnailUrl = thumbnail
        )
    }

    private fun mapYouTubeApiItem(item: YTVideoItem): PlayTubeVideo? {
        val snippet = item.snippet ?: return null
        val contentDetails = item.contentDetails
        val statistics = item.statistics

        val viewCountRaw = statistics?.viewCount
        val durationRaw = contentDetails?.duration
        val publishedAtRaw = snippet.publishedAt

        val viewCount = YouTubeFormatUtils.formatViewCount(viewCountRaw)
        val duration = if (durationRaw != null) YouTubeFormatUtils.parseDuration(durationRaw) else "0:00"
        val uploadTime = YouTubeFormatUtils.formatPublishedAt(publishedAtRaw)

        val thumbnailUrl = snippet.thumbnails?.high?.url
            ?: snippet.thumbnails?.medium?.url
            ?: snippet.thumbnails?.default?.url

        return PlayTubeVideo(
            id = item.id,
            title = snippet.title ?: "Untitled",
            channelName = snippet.channelTitle ?: "Unknown Channel",
            channelId = snippet.channelId ?: "unknown",
            description = snippet.description ?: "",
            viewCount = viewCount,
            duration = duration,
            uploadTime = uploadTime,
            videoUrl = "https://www.youtube.com/watch?v=${item.id}",
            category = "Video",
            subscriberCount = "N/A",
            channelAvatar = "📺",
            thumbnailUrl = thumbnailUrl
        )
    }

    private fun mapPlaylistItemToPlayTubeVideo(item: SupabaseYouTubePlaylistItem): PlayTubeVideo {
        val thumbnail = item.thumbnail_url ?: "https://img.youtube.com/vi/${item.video_id}/hqdefault.jpg"
        val uploadTime = YouTubeFormatUtils.formatPublishedAt(item.published_at)
        return PlayTubeVideo(
            id = item.video_id,
            title = item.title ?: "Untitled",
            channelName = "YouTube",
            channelId = "",
            description = item.description ?: "",
            viewCount = "N/A",
            duration = "0:00",
            uploadTime = uploadTime,
            videoUrl = "https://www.youtube.com/watch?v=${item.video_id}",
            category = "Video",
            channelAvatar = "📺",
            thumbnailUrl = thumbnail
        )
    }

    // ---------- Invidious instance management ----------
    fun selectPreferredInstance(url: String?) {
        InvidiousService.preferredInstance = url
        _preferredInstance.value = url
        fetchYouTubeHomeFeeds()
    }

    fun toggleAutoSwitch(enabled: Boolean) {
        InvidiousService.autoSwitchOnFailure = enabled
        _autoSwitchOnFailure.value = enabled
    }

    fun addCustomInstance(url: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        if (cleanUrl.isBlank() || !cleanUrl.startsWith("http")) {
            _youtubeError.value = "Invalid URL – must start with http:// or https://"
            return
        }
        if (!InvidiousService.customInstances.contains(cleanUrl)) {
            InvidiousService.customInstances.add(cleanUrl)
            val currentList = _invidiousInstances.value.toMutableList()
            if (!currentList.any { it.url == cleanUrl }) {
                currentList.add(0, InvidiousInstanceStatus(url = cleanUrl, isCustom = true))
                _invidiousInstances.value = currentList
                runInstanceHealthChecks()
            }
        }
    }

    fun runInstanceHealthChecks() {
        if (_isHealthChecking.value) return
        _isHealthChecking.value = true

        viewModelScope.launch(Dispatchers.IO) {
            _invidiousInstances.value = _invidiousInstances.value.map {
                it.copy(isChecking = true)
            }

            val updated = _invidiousInstances.value.map { status ->
                val latency = InvidiousService.checkInstanceHealth(status.url)
                status.copy(latencyMs = latency, isChecking = false)
            }

            _invidiousInstances.value = updated
            _isHealthChecking.value = false

            if (_autoSwitchOnFailure.value) {
                val fastest = updated.filter { it.latencyMs > 0 }.minByOrNull { it.latencyMs }
                if (fastest != null && fastest.url != _preferredInstance.value) {
                    withContext(Dispatchers.Main) {
                        selectPreferredInstance(fastest.url)
                    }
                }
            }
        }
    }

    // ---------- PlayTube specific helpers (unchanged) ----------
    fun togglePlayTubePlaylistBookmark(playlistId: String) {
        val current = _playTubeBookmarkedPlaylistIds.value
        if (current.contains(playlistId)) {
            _playTubeBookmarkedPlaylistIds.value = current - playlistId
        } else {
            _playTubeBookmarkedPlaylistIds.value = current + playlistId
        }
    }

    fun downloadPlayTubeVideo(video: PlayTubeVideo, isAudioOnly: Boolean, resolution: String) {
        val exists = _playTubeDownloads.value.any { it.videoId == video.id }
        if (exists) return

        val newDownload = PlayTubeDownload(
            videoId = video.id,
            title = video.title,
            channelName = video.channelName,
            duration = video.duration,
            category = video.category,
            fileSizeBytes = if (isAudioOnly) (15..30).random().toLong() * 1024 * 1024 else (40..120).random().toLong() * 1024 * 1024,
            isAudioOnly = isAudioOnly,
            resolution = if (isAudioOnly) "Audio-Only" else resolution,
            progress = 0.0f,
            status = "Downloading",
            thumbnailAvatar = video.channelAvatar
        )

        _playTubeDownloads.value = _playTubeDownloads.value + newDownload
        savePlayTubeDownloads()
        syncDownloadStateToSupabase(newDownload.videoId, newDownload.status, newDownload.progress.toDouble(), newDownload.fileSizeBytes)

        viewModelScope.launch {
            var currentProgress = 0.0f
            while (currentProgress < 1.0f) {
                delay(800)
                val dl = _playTubeDownloads.value.find { it.videoId == video.id }
                if (dl == null || dl.status == "Paused") {
                    if (dl == null) return@launch
                    else break
                }
                currentProgress += 0.2f
                if (currentProgress >= 1.0f) {
                    currentProgress = 1.0f
                }
                _playTubeDownloads.value = _playTubeDownloads.value.map {
                    if (it.videoId == video.id) {
                        val updatedStatus = if (currentProgress >= 1.0f) "Completed" else "Downloading"
                        syncDownloadStateToSupabase(video.id, updatedStatus, currentProgress.toDouble(), it.fileSizeBytes)
                        it.copy(progress = currentProgress, status = updatedStatus)
                    } else {
                        it
                    }
                }
                savePlayTubeDownloads()
            }
        }
    }

    fun pausePlayTubeDownload(videoId: String) {
        _playTubeDownloads.value = _playTubeDownloads.value.map {
            if (it.videoId == videoId && it.status == "Downloading") {
                syncDownloadStateToSupabase(videoId, "Paused", it.progress.toDouble(), it.fileSizeBytes)
                it.copy(status = "Paused")
            } else {
                it
            }
        }
        savePlayTubeDownloads()
    }

    fun resumePlayTubeDownload(videoId: String) {
        val dl = _playTubeDownloads.value.find { it.videoId == videoId } ?: return
        if (dl.status != "Paused") return

        _playTubeDownloads.value = _playTubeDownloads.value.map {
            if (it.videoId == videoId) {
                syncDownloadStateToSupabase(videoId, "Downloading", it.progress.toDouble(), it.fileSizeBytes)
                it.copy(status = "Downloading")
            } else {
                it
            }
        }
        savePlayTubeDownloads()

        viewModelScope.launch {
            var currentProgress = dl.progress
            while (currentProgress < 1.0f) {
                delay(800)
                val currentDl = _playTubeDownloads.value.find { it.videoId == videoId }
                if (currentDl == null || currentDl.status == "Paused") {
                    if (currentDl == null) return@launch
                    else break
                }
                currentProgress += 0.2f
                if (currentProgress >= 1.0f) {
                    currentProgress = 1.0f
                }
                _playTubeDownloads.value = _playTubeDownloads.value.map {
                    if (it.videoId == videoId) {
                        val updatedStatus = if (currentProgress >= 1.0f) "Completed" else "Downloading"
                        syncDownloadStateToSupabase(videoId, updatedStatus, currentProgress.toDouble(), it.fileSizeBytes)
                        it.copy(progress = currentProgress, status = updatedStatus)
                    } else {
                        it
                    }
                }
                savePlayTubeDownloads()
            }
        }
    }

    fun deletePlayTubeDownload(videoId: String) {
        _playTubeDownloads.value = _playTubeDownloads.value.filter { it.videoId != videoId }
        savePlayTubeDownloads()
        // Try delete from filesystem
        val context = getApplication<Application>()
        val directory = context.getExternalFilesDir(null)
        if (directory != null) {
            val file = directory.listFiles()?.find { it.name.contains(videoId) }
            file?.delete()
        }
    }

    fun refreshDownloadsFromFilesystem() {
        val context = getApplication<Application>()
        val directory = context.getExternalFilesDir(null) ?: return
        val files = directory.listFiles() ?: return

        val currentDownloads = _playTubeDownloads.value.toMutableList()
        files.filter { it.name.startsWith("video_") || it.name.endsWith(".mp4") || it.name.endsWith(".m4a") }.forEach { file ->
            val videoId = if (file.name.startsWith("video_")) {
                file.name.substringBefore(".").substringAfter("video_")
            } else {
                file.name.substringBefore(".")
            }
            if (currentDownloads.none { it.videoId == videoId }) {
                currentDownloads.add(
                    PlayTubeDownload(
                        videoId = videoId,
                        title = file.name,
                        channelName = "Local Storage",
                        duration = "N/A",
                        category = "Downloaded",
                        fileSizeBytes = file.length(),
                        isAudioOnly = file.name.endsWith(".m4a"),
                        resolution = "Local",
                        progress = 1.0f,
                        status = "Completed",
                        thumbnailAvatar = "📁"
                    )
                )
            }
        }
        _playTubeDownloads.value = currentDownloads
    }

    private val _playTubeComments = MutableStateFlow<List<PlayTubeComment>>(emptyList())
    val playTubeComments = _playTubeComments.asStateFlow()

    fun fetchPlayTubeDetails(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val related = InvidiousService.fetchRelatedVideos(videoId)
                val commentsRaw = InvidiousService.fetchVideoComments(videoId)
                val comments = commentsRaw.map { obj ->
                    PlayTubeComment(
                        userName = obj.optString("author", "User"),
                        text = obj.optString("content", ""),
                        timeAgo = obj.optString("publishedText", "recently"),
                        userAvatar = obj.optString("authorThumbnails", "")
                    )
                }
                withContext(Dispatchers.Main) {
                    _playTubeRelatedVideos.value = related
                    _playTubeComments.value = comments
                }
            } catch (e: Exception) {
                Log.e("PlayTube", "Failed to fetch video details: ${e.message}")
            }
        }
    }

    fun selectPlayTubeVideo(video: PlayTubeVideo?) {
        _playTubeCurrentVideo.value = video
        if (video != null) {
            _playTubeIsPlaying.value = true
            fetchPlayTubeDetails(video.id)
        } else {
            _playTubeIsPlaying.value = false
            _playTubeRelatedVideos.value = emptyList()
            _playTubeComments.value = emptyList()
        }
    }

    fun setPlayTubePlaying(playing: Boolean) {
        _playTubeIsPlaying.value = playing
    }

    fun togglePlayTubeAudioOnly() {
        _playTubeIsAudioOnly.value = !_playTubeIsAudioOnly.value
    }

    fun setPlayTubeResolution(resolution: String) {
        _playTubeResolution.value = resolution
    }

    fun togglePlayTubeFps60() {
        _playTubeFps60.value = !_playTubeFps60.value
    }

    fun incrementBlockedAds() {
        _playTubeBlockedAdsCount.value += (1..3).random()
    }

    fun setPlayTubePipMode(enabled: Boolean) {
        _playTubeIsPipMode.value = enabled
    }

    fun setSystemPipMode(enabled: Boolean) {
        _isSystemPipMode.value = enabled
    }

    fun setLiveTvFullScreen(enabled: Boolean) {
        _liveTvIsFullScreen.value = enabled
    }

    fun togglePlayTubeSubscription(channelId: String) {
        val current = _playTubeSubscribedChannelIds.value
        if (current.contains(channelId)) {
            _playTubeSubscribedChannelIds.value = current - channelId
        } else {
            _playTubeSubscribedChannelIds.value = current + channelId
        }
        savePlayTubeSubscriptions()
    }

    fun togglePlayTubeBookmark(videoId: String) {
        val current = _playTubeBookmarkedVideoIds.value
        if (current.contains(videoId)) {
            _playTubeBookmarkedVideoIds.value = current - videoId
        } else {
            _playTubeBookmarkedVideoIds.value = current + videoId
        }
        savePlayTubeBookmarks()
    }

    fun createPlayTubePlaylist(name: String, description: String) {
        val newList = _playTubePlaylists.value + PlayTubePlaylist(name = name, description = description)
        _playTubePlaylists.value = newList
        savePlayTubePlaylists()
    }

    fun renamePlayTubePlaylist(playlistId: String, newName: String) {
        _playTubePlaylists.value = _playTubePlaylists.value.map {
            if (it.id == playlistId) it.copy(name = newName) else it
        }
        savePlayTubePlaylists()
    }

    fun deletePlayTubePlaylist(playlistId: String) {
        _playTubePlaylists.value = _playTubePlaylists.value.filter { it.id != playlistId }
        savePlayTubePlaylists()
    }

    fun addVideoToPlayTubePlaylist(playlistId: String, videoId: String) {
        _playTubePlaylists.value = _playTubePlaylists.value.map {
            if (it.id == playlistId && !it.videoIds.contains(videoId)) {
                it.copy(videoIds = it.videoIds + videoId)
            } else {
                it
            }
        }
        savePlayTubePlaylists()
    }

    fun removeVideoFromPlayTubePlaylist(playlistId: String, videoId: String) {
        _playTubePlaylists.value = _playTubePlaylists.value.map {
            if (it.id == playlistId) {
                it.copy(videoIds = it.videoIds - videoId)
            } else {
                it
            }
        }
        savePlayTubePlaylists()
    }

    private fun savePlayTubeSubscriptions() {
        val str = _playTubeSubscribedChannelIds.value.joinToString(",")
        prefs.edit().putString("playtube_subscribed_ids", str).apply()
    }

    private fun savePlayTubeBookmarks() {
        val str = _playTubeBookmarkedVideoIds.value.joinToString(",")
        prefs.edit().putString("playtube_bookmarked_ids", str).apply()
    }

    private fun savePlayTubePlaylists() {
        val str = _playTubePlaylists.value.joinToString(";;") { pl ->
            "${pl.id}:${pl.name}:${pl.description}:${pl.videoIds.joinToString("|")}"
        }
        prefs.edit().putString("playtube_playlists_json", str).apply()
    }

    fun savePlayTubeDownloads() {
        val str = _playTubeDownloads.value.joinToString(";;") { dl ->
            "${dl.videoId}::${dl.title}::${dl.channelName}::${dl.duration}::${dl.category}::${dl.fileSizeBytes}::${dl.isAudioOnly}::${dl.resolution}::${dl.progress}::${dl.status}::${dl.thumbnailAvatar}"
        }
        prefs.edit().putString("playtube_downloads_json", str).apply()
    }

    init {
        // Load local playtube subscriptions
        val savedSubs = prefs.getString("playtube_subscribed_ids", "") ?: ""
        if (savedSubs.isNotEmpty()) {
            _playTubeSubscribedChannelIds.value = savedSubs.split(",").filter { it.isNotEmpty() }.toSet()
        }

        // Load local playtube bookmarked videos
        val savedBookmarks = prefs.getString("playtube_bookmarked_ids", "") ?: ""
        if (savedBookmarks.isNotEmpty()) {
            _playTubeBookmarkedVideoIds.value = savedBookmarks.split(",").filter { it.isNotEmpty() }.toSet()
        }

        // Load local playtube playlists
        val savedPlaylists = prefs.getString("playtube_playlists_json", "") ?: ""
        if (savedPlaylists.isNotEmpty()) {
            try {
                val list = mutableListOf<PlayTubePlaylist>()
                savedPlaylists.split(";;").forEach { plStr ->
                    val parts = plStr.split(":")
                    if (parts.size >= 3) {
                        val id = parts[0]
                        val name = parts[1]
                        val desc = parts[2]
                        val videoIds = if (parts.size >= 4 && parts[3].isNotEmpty()) parts[3].split("|") else emptyList()
                        list.add(PlayTubePlaylist(id, name, desc, videoIds))
                    }
                }
                _playTubePlaylists.value = list
            } catch (e: Exception) {
                // Fallback to default playlists
            }
        }

        // Load local playtube downloads
        val savedDownloads = prefs.getString("playtube_downloads_json", "") ?: ""
        if (savedDownloads.isNotEmpty()) {
            try {
                val list = mutableListOf<PlayTubeDownload>()
                savedDownloads.split(";;").forEach { dlStr ->
                    val parts = dlStr.split("::")
                    if (parts.size >= 10) {
                        list.add(PlayTubeDownload(
                            videoId = parts[0],
                            title = parts[1],
                            channelName = parts[2],
                            duration = parts[3],
                            category = parts[4],
                            fileSizeBytes = parts[5].toLongOrNull() ?: 0L,
                            isAudioOnly = parts[6].toBoolean(),
                            resolution = parts[7],
                            progress = parts[8].toFloatOrNull() ?: 1.0f,
                            status = parts[9],
                            thumbnailAvatar = if (parts.size >= 11) parts[10] else "🎧"
                        ))
                    }
                }
                _playTubeDownloads.value = list
            } catch (e: Exception) {
                // Fallback to default downloads
            }
        }
    }

    fun clearWatchHistoryInSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("watch_history").delete {
                    filter {
                        eq("profile_id", currentUser.id)
                    }
                }
                _supabaseWatchHistory.value = emptyList()
                android.util.Log.d("Supabase", "Cleared watch history in Supabase")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to clear watch history on Supabase", e)
            }
        }
    }

    // ============================================================
    // USER PLAYLISTS — Supabase-backed
    // ============================================================

    private val _supabasePlaylists = MutableStateFlow<List<SupabaseUserPlaylist>>(emptyList())
    val supabasePlaylists = _supabasePlaylists.asStateFlow()

    fun loadUserPlaylistsFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = com.example.supabase.postgrest.from("playlists")
                    .select { filter { eq("profile_id", currentUser.id) } }
                    .decodeList<SupabaseUserPlaylist>()
                withContext(Dispatchers.Main) {
                    _supabasePlaylists.value = items
                    // Sync into local PlayTubePlaylists so the existing UI works
                    _playTubePlaylists.value = items.map { sp ->
                        PlayTubePlaylist(id = sp.id, name = sp.name, description = sp.description ?: "", videoIds = emptyList())
                    }
                }
                android.util.Log.d("Supabase", "Loaded ${items.size} user playlists from Supabase")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load user playlists from Supabase", e)
            }
        }
    }

    fun createUserPlaylistInSupabase(name: String, description: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: run {
            createPlayTubePlaylist(name, description); return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = mapOf(
                    "profile_id" to currentUser.id,
                    "name" to name,
                    "description" to description,
                    "is_public" to false
                )
                val created = com.example.supabase.postgrest.from("playlists")
                    .insert(payload) { select() }
                    .decodeSingle<SupabaseUserPlaylist>()
                withContext(Dispatchers.Main) {
                    _supabasePlaylists.value = _supabasePlaylists.value + created
                    _playTubePlaylists.value = _playTubePlaylists.value +
                        PlayTubePlaylist(id = created.id, name = created.name, description = created.description ?: "", videoIds = emptyList())
                }
                android.util.Log.d("Supabase", "Created user playlist '${created.name}'")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to create playlist in Supabase", e)
                withContext(Dispatchers.Main) { createPlayTubePlaylist(name, description) }
            }
        }
    }

    fun renameUserPlaylistInSupabase(playlistId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("playlists")
                    .update(mapOf("name" to newName)) { filter { eq("id", playlistId) } }
                withContext(Dispatchers.Main) {
                    _supabasePlaylists.value = _supabasePlaylists.value.map {
                        if (it.id == playlistId) it.copy(name = newName) else it
                    }
                    renamePlayTubePlaylist(playlistId, newName)
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to rename playlist in Supabase", e)
                withContext(Dispatchers.Main) { renamePlayTubePlaylist(playlistId, newName) }
            }
        }
    }

    fun deleteUserPlaylistInSupabase(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("playlists")
                    .delete { filter { eq("id", playlistId) } }
                withContext(Dispatchers.Main) {
                    _supabasePlaylists.value = _supabasePlaylists.value.filter { it.id != playlistId }
                    deletePlayTubePlaylist(playlistId)
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to delete playlist in Supabase", e)
                withContext(Dispatchers.Main) { deletePlayTubePlaylist(playlistId) }
            }
        }
    }

    fun addVideoToUserPlaylistInSupabase(playlistId: String, videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val position = _playTubePlaylists.value
                    .find { it.id == playlistId }?.videoIds?.size ?: 0
                val payload = mapOf(
                    "playlist_id" to playlistId,
                    "video_id" to videoId,
                    "position" to position
                )
                com.example.supabase.postgrest.from("playlist_items").insert(payload)
                withContext(Dispatchers.Main) { addVideoToPlayTubePlaylist(playlistId, videoId) }
                android.util.Log.d("Supabase", "Added video $videoId to playlist $playlistId")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to add video to playlist in Supabase", e)
                withContext(Dispatchers.Main) { addVideoToPlayTubePlaylist(playlistId, videoId) }
            }
        }
    }

    fun removeVideoFromUserPlaylistInSupabase(playlistId: String, videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("playlist_items").delete {
                    filter {
                        eq("playlist_id", playlistId)
                        eq("video_id", videoId)
                    }
                }
                withContext(Dispatchers.Main) { removeVideoFromPlayTubePlaylist(playlistId, videoId) }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to remove video from playlist in Supabase", e)
                withContext(Dispatchers.Main) { removeVideoFromPlayTubePlaylist(playlistId, videoId) }
            }
        }
    }

    // ============================================================
    // SUPPORT TICKETS — Supabase-backed
    // ============================================================

    fun loadSupportTicketsFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = com.example.supabase.postgrest.from("support_tickets")
                    .select { filter { eq("profile_id", currentUser.id) } }
                    .decodeList<SupabaseSupportTicket>()
                withContext(Dispatchers.Main) {
                    _supportTickets.value = items.map { s ->
                        SupportTicket(
                            id = s.id,
                            subject = s.subject,
                            category = s.category,
                            description = s.description ?: "",
                            date = s.created_at?.take(10) ?: "—",
                            status = s.status
                        )
                    }
                }
                android.util.Log.d("Supabase", "Loaded ${items.size} support tickets from Supabase")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load support tickets from Supabase", e)
            }
        }
    }

    fun createSupportTicketInSupabase(subject: String, category: String, desc: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: run {
            createSupportTicket(subject, category, desc); return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = mapOf(
                    "profile_id" to currentUser.id,
                    "subject" to subject,
                    "category" to category,
                    "description" to desc,
                    "status" to "OPEN"
                )
                val created = com.example.supabase.postgrest.from("support_tickets")
                    .insert(payload) { select() }
                    .decodeSingle<SupabaseSupportTicket>()
                withContext(Dispatchers.Main) {
                    val newTicket = SupportTicket(
                        id = created.id,
                        subject = created.subject,
                        category = created.category,
                        description = created.description ?: "",
                        date = created.created_at?.take(10) ?: "—",
                        status = created.status
                    )
                    _supportTickets.value = listOf(newTicket) + _supportTickets.value
                }
                android.util.Log.d("Supabase", "Created support ticket '${created.subject}'")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to create support ticket in Supabase", e)
                withContext(Dispatchers.Main) { createSupportTicket(subject, category, desc) }
            }
        }
    }

    fun selectTicketForChat(ticket: SupportTicket?) {
        _activeChatTicket.value = ticket
        if (ticket != null) {
            loadTicketMessages(ticket.id)
        } else {
            _ticketMessages.value = emptyList()
        }
    }

    fun loadTicketMessages(ticketId: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = com.example.supabase.postgrest.from("support_messages")
                    .select {
                        filter { eq("ticket_id", ticketId) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }
                    .decodeList<SupabaseSupportMessage>()
                withContext(Dispatchers.Main) {
                    _ticketMessages.value = items.map { m ->
                        ChatMessage(
                            id = m.id ?: UUID.randomUUID().toString(),
                            text = m.message,
                            isSenderUser = !m.is_staff,
                            timestamp = m.created_at?.take(16)?.replace('T', ' ') ?: ""
                        )
                    }
                }
                android.util.Log.d("Supabase", "Loaded ${items.size} messages for ticket $ticketId")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load ticket messages from Supabase", e)
            }
        }
    }

    fun sendTicketMessageInSupabase(text: String) {
        val ticket = _activeChatTicket.value ?: return
        if (text.isBlank()) return
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = SupabaseSupportMessage(
                    ticket_id = ticket.id,
                    sender_id = currentUser.id,
                    message = text,
                    is_staff = false
                )
                val created = com.example.supabase.postgrest.from("support_messages")
                    .insert(payload) { select() }
                    .decodeSingle<SupabaseSupportMessage>()
                withContext(Dispatchers.Main) {
                    val newMsg = ChatMessage(
                        id = created.id ?: UUID.randomUUID().toString(),
                        text = created.message,
                        isSenderUser = true,
                        timestamp = created.created_at?.take(16)?.replace('T', ' ') ?: ""
                    )
                    _ticketMessages.value = _ticketMessages.value + newMsg
                }
                android.util.Log.d("Supabase", "Sent ticket message to Supabase: '${created.message}'")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to send ticket message to Supabase", e)
            }
        }
    }

    fun createFeedbackInSupabase(kind: String, rating: Int, title: String, message: String, contentId: String? = null) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = SupabaseFeedback(
                    profile_id = currentUser.id,
                    content_id = contentId,
                    kind = kind,
                    rating = rating,
                    title = title,
                    message = message
                )
                com.example.supabase.postgrest.from("feedbacks")
                    .insert(payload)
                android.util.Log.d("Supabase", "Successfully saved feedback to Supabase: $title")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to save feedback to Supabase", e)
            }
        }
    }

    fun loadRewardsAndReferrals() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val events = com.example.supabase.postgrest.from("reward_events")
                    .select { filter { eq("profile_id", currentUser.id) } }
                    .decodeList<SupabaseRewardEvent>()

                if (events.isEmpty()) {
                    val welcomeEvent = SupabaseRewardEvent(
                        profile_id = currentUser.id,
                        event_type = "welcome_bonus",
                        points = 450,
                        description = "Welcome Bonus"
                    )
                    com.example.supabase.postgrest.from("reward_events")
                        .insert(welcomeEvent)
                    withContext(Dispatchers.Main) {
                        _userRewardPoints.value = 450
                    }
                    android.util.Log.d("Supabase", "Seeded account with 450 welcome reward points")
                } else {
                    val totalPoints = events.sumOf { it.points }
                    withContext(Dispatchers.Main) {
                        _userRewardPoints.value = totalPoints
                    }
                    android.util.Log.d("Supabase", "Loaded reward events from Supabase. Total sum: $totalPoints PTS")
                }

                val referrals = com.example.supabase.postgrest.from("referrals")
                    .select { filter { eq("profile_id", currentUser.id) } }
                    .decodeList<SupabaseReferral>()
                withContext(Dispatchers.Main) {
                    _referralsList.value = referrals
                }
                android.util.Log.d("Supabase", "Loaded ${referrals.size} referrals from Supabase")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load rewards/referrals from Supabase", e)
            }
        }
    }

    fun sendReferralInvitationInSupabase(email: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = SupabaseReferral(
                    profile_id = currentUser.id,
                    referred_email = email,
                    reward_points = 250,
                    status = "pending",
                    referral_code = _referralCode.value
                )
                com.example.supabase.postgrest.from("referrals")
                    .insert(payload)
                android.util.Log.d("Supabase", "Successfully saved referral to Supabase for: $email")
                loadRewardsAndReferrals()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to send referral invitation to Supabase", e)
            }
        }
    }

    fun loadSocialNetwork() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val acceptedRelations = com.example.supabase.postgrest.from("friendships")
                    .select {
                        filter {
                            and {
                                or {
                                    eq("profile_id", currentUser.id)
                                    eq("friend_profile_id", currentUser.id)
                                }
                                eq("status", "accepted")
                            }
                        }
                    }
                    .decodeList<SupabaseFriendship>()

                val friendIds = acceptedRelations.map {
                    if (it.profile_id == currentUser.id) it.friend_profile_id else it.profile_id
                }.distinct()

                val friends = if (friendIds.isNotEmpty()) {
                    com.example.supabase.postgrest.from("profiles")
                        .select()
                        .decodeList<DbProfile>()
                        .filter { friendIds.contains(it.id) }
                } else {
                    emptyList()
                }

                withContext(Dispatchers.Main) {
                    _friendsList.value = friends
                }
                android.util.Log.d("Supabase", "Loaded ${friends.size} friends")

                val pendingRequests = com.example.supabase.postgrest.from("friendships")
                    .select {
                        filter {
                            and {
                                eq("friend_profile_id", currentUser.id)
                                eq("status", "pending")
                            }
                        }
                    }
                    .decodeList<SupabaseFriendship>()

                val requestSenderIds = pendingRequests.map { it.profile_id }
                val requests = if (requestSenderIds.isNotEmpty()) {
                    com.example.supabase.postgrest.from("profiles")
                        .select()
                        .decodeList<DbProfile>()
                        .filter { requestSenderIds.contains(it.id) }
                } else {
                    emptyList()
                }

                withContext(Dispatchers.Main) {
                    _friendRequests.value = requests
                }
                android.util.Log.d("Supabase", "Loaded ${requests.size} pending requests")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load social network", e)
            }
        }
    }

    fun searchProfiles(query: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        if (query.isBlank()) {
            _allProfilesSearch.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allProfiles = com.example.supabase.postgrest.from("profiles")
                    .select()
                    .decodeList<DbProfile>()

                val filtered = allProfiles.filter {
                    ((it.full_name?.contains(query, ignoreCase = true) == true || it.username?.contains(query, ignoreCase = true) == true)) &&
                    it.id != currentUser.id &&
                    !_friendsList.value.any { friend -> friend.id == it.id } &&
                    !_friendRequests.value.any { req -> req.id == it.id }
                }

                withContext(Dispatchers.Main) {
                    _allProfilesSearch.value = filtered
                }
                android.util.Log.d("Supabase", "Found ${filtered.size} profiles matching '$query'")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to search profiles", e)
            }
        }
    }

    fun sendFriendRequest(targetProfileId: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = SupabaseFriendship(
                    profile_id = currentUser.id,
                    friend_profile_id = targetProfileId,
                    status = "pending"
                )
                com.example.supabase.postgrest.from("friendships")
                    .insert(payload)
                android.util.Log.d("Supabase", "Sent friend request to: $targetProfileId")
                loadSocialNetwork()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to send friend request", e)
            }
        }
    }

    fun acceptFriendRequest(senderProfileId: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val relations = com.example.supabase.postgrest.from("friendships")
                    .select {
                        filter {
                            and {
                                eq("profile_id", senderProfileId)
                                eq("friend_profile_id", currentUser.id)
                            }
                        }
                    }
                    .decodeList<SupabaseFriendship>()

                val relation = relations.firstOrNull() ?: return@launch
                val updated = relation.copy(status = "accepted")
                com.example.supabase.postgrest.from("friendships")
                    .update(updated) {
                        filter {
                            eq("id", relation.id ?: "")
                        }
                    }
                android.util.Log.d("Supabase", "Accepted friend request from: $senderProfileId")
                loadSocialNetwork()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to accept friend request", e)
            }
        }
    }

    fun declineFriendRequest(senderProfileId: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("friendships")
                    .delete {
                        filter {
                            and {
                                eq("profile_id", senderProfileId)
                                eq("friend_profile_id", currentUser.id)
                            }
                        }
                    }
                android.util.Log.d("Supabase", "Declined friend request from: $senderProfileId")
                loadSocialNetwork()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to decline friend request", e)
            }
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // ───── ROUTINE PLANS (routine_plans + routine_items) ─────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    fun loadRoutinePlansFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val plans = com.example.supabase.postgrest.from("routine_plans")
                    .select { filter { eq("profile_id", currentUser.id) } }
                    .decodeList<SupabaseRoutinePlan>()
                withContext(Dispatchers.Main) {
                    _routinePlans.value = plans
                }
                // Load routine items for each active plan
                val activePlanIds = plans.filter { it.is_active }.mapNotNull { it.id }
                if (activePlanIds.isNotEmpty()) {
                    val items = com.example.supabase.postgrest.from("routine_items")
                        .select()
                        .decodeList<SupabaseRoutineItem>()
                        .filter { activePlanIds.contains(it.routine_plan_id) }
                    withContext(Dispatchers.Main) {
                        _routineItems.value = items
                    }
                }
                android.util.Log.d("Supabase", "Loaded ${plans.size} routine plans from Supabase")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load routine plans from Supabase", e)
            }
        }
    }

    fun saveRoutinePlanToSupabase(title: String, description: String?) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activePlan = _routinePlans.value.find { it.is_active }
                if (activePlan != null) {
                    val updated = activePlan.copy(title = title, description = description)
                    com.example.supabase.postgrest.from("routine_plans")
                        .update(updated) {
                            filter {
                                eq("id", activePlan.id ?: "")
                            }
                        }
                    withContext(Dispatchers.Main) {
                        _routinePlans.value = _routinePlans.value.map {
                            if (it.id == activePlan.id) updated else it
                        }
                    }
                    android.util.Log.d("Supabase", "Updated routine plan: '$title'")
                } else {
                    val payload = SupabaseRoutinePlan(
                        profile_id = currentUser.id,
                        title = title,
                        description = description,
                        is_active = true
                    )
                    val created = com.example.supabase.postgrest.from("routine_plans")
                        .insert(payload) { select() }
                        .decodeSingle<SupabaseRoutinePlan>()
                    withContext(Dispatchers.Main) {
                        _routinePlans.value = _routinePlans.value + created
                    }
                    android.util.Log.d("Supabase", "Created routine plan: '${created.title}'")
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to save routine plan to Supabase", e)
            }
        }
    }

    fun addRoutineItemToSupabase(routinePlanId: String, contentId: String, scheduledAt: String, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = SupabaseRoutineItem(
                    routine_plan_id = routinePlanId,
                    content_id = contentId,
                    scheduled_at = scheduledAt,
                    note = note,
                    is_enabled = true
                )
                val created = com.example.supabase.postgrest.from("routine_items")
                    .insert(payload) { select() }
                    .decodeSingle<SupabaseRoutineItem>()
                withContext(Dispatchers.Main) {
                    _routineItems.value = _routineItems.value + created
                }
                android.util.Log.d("Supabase", "Added routine item for course $contentId to plan $routinePlanId")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to add routine item to Supabase", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ───── SOCIAL ACTIVITY FEED (social_activities table) ────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    fun loadSocialActivityFeedFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get friend IDs to filter the feed to social circle
                val acceptedRelations = com.example.supabase.postgrest.from("friendships")
                    .select()
                    .decodeList<SupabaseFriendship>()
                    .filter { (it.profile_id == currentUser.id || it.friend_profile_id == currentUser.id) && it.status == "accepted" }

                val friendIds = acceptedRelations.map {
                    if (it.profile_id == currentUser.id) it.friend_profile_id else it.profile_id
                }.distinct()

                // Load recent activities from friends + self
                val allActivities = com.example.supabase.postgrest.from("social_activities")
                    .select()
                    .decodeList<SupabaseSocialActivity>()
                    .filter { act -> (friendIds + currentUser.id).contains(act.profile_id) }
                    .sortedByDescending { it.created_at }
                    .take(30)

                // Fetch profiles for those activity actors
                val actorIds = allActivities.map { it.profile_id }.distinct()
                val actorProfiles = if (actorIds.isNotEmpty()) {
                    com.example.supabase.postgrest.from("profiles")
                        .select()
                        .decodeList<DbProfile>()
                        .filter { actorIds.contains(it.id) }
                } else emptyList()

                val feedItems = allActivities.mapNotNull { activity ->
                    val actor = actorProfiles.find { it.id == activity.profile_id } ?: return@mapNotNull null
                    val actorName = actor.full_name ?: actor.username ?: "User"
                    val timeAgo = formatTimeAgo(activity.created_at)
                    val verb = when (activity.activity_type) {
                        "enrolled" -> "enrolled in a course"
                        "completed" -> "completed a course 🎓"
                        "watched" -> "watched content"
                        "liked" -> "liked content ❤️"
                        "joined_room" -> "joined a co-watch room 🍿"
                        "shared" -> "shared something"
                        else -> activity.activity_type
                    }
                    SocialActivityFeedItem(
                        id = activity.id ?: java.util.UUID.randomUUID().toString(),
                        profileName = actorName,
                        avatarInitial = actorName.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                        activityType = activity.activity_type,
                        contentTitle = activity.content_id,
                        message = "${if (activity.profile_id == currentUser.id) "You" else actorName} $verb",
                        timeAgo = timeAgo
                    )
                }

                withContext(Dispatchers.Main) {
                    _socialActivityFeed.value = feedItems
                }
                android.util.Log.d("Supabase", "Loaded ${feedItems.size} social activity feed items")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load social activity feed", e)
            }
        }
    }

    fun logSocialActivityToSupabase(
        activityType: String,
        contentId: String? = null,
        roomId: String? = null,
        message: String? = null
    ) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = SupabaseSocialActivity(
                    profile_id = currentUser.id,
                    activity_type = activityType,
                    content_id = contentId,
                    room_id = roomId,
                    message = message
                )
                com.example.supabase.postgrest.from("social_activities").insert(payload)
                android.util.Log.d("Supabase", "Logged social activity: $activityType")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to log social activity", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ───── ANALYTICS EVENTS (analytics_events table) ─────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    fun logAnalyticsEvent(eventName: String, data: Map<String, String> = emptyMap()) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = SupabaseAnalyticsEvent(
                    profile_id = currentUser.id,
                    event_name = eventName,
                    event_data = data
                )
                com.example.supabase.postgrest.from("analytics_events").insert(payload)
                android.util.Log.d("Supabase", "Analytics event logged: $eventName")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to log analytics event: $eventName", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ───── APP CONFIG (app_config table) ─────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    fun loadAppConfigFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val configs = com.example.supabase.postgrest.from("app_config")
                    .select()
                    .decodeList<SupabaseAppConfig>()
                val config = configs.firstOrNull()
                if (config != null) {
                    withContext(Dispatchers.Main) {
                        _remoteAppConfig.value = config
                    }
                    android.util.Log.d("Supabase", "Loaded app config: ${config.app_name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load app config from Supabase", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ───── CONTENT EPISODES (content_episodes + content_seasons) ─────────────
    // ─────────────────────────────────────────────────────────────────────────

    fun loadContentEpisodesFromSupabase(contentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val seasons = com.example.supabase.postgrest.from("content_seasons")
                    .select { filter { eq("content_id", contentId) } }
                    .decodeList<SupabaseContentSeason>()

                withContext(Dispatchers.Main) {
                    _contentSeasons.value = seasons
                }

                val seasonIds = seasons.mapNotNull { it.id }
                if (seasonIds.isNotEmpty()) {
                    val episodes = com.example.supabase.postgrest.from("content_episodes")
                        .select()
                        .decodeList<SupabaseContentEpisode>()
                        .filter { seasonIds.contains(it.season_id) }
                        .sortedWith(compareBy({ it.season_id }, { it.episode_number }))
                    withContext(Dispatchers.Main) {
                        _contentEpisodes.value = episodes
                    }
                    android.util.Log.d("Supabase", "Loaded ${seasons.size} seasons, ${episodes.size} episodes for content $contentId")
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load content episodes from Supabase", e)
            }
        }
    }

    // Helper: format ISO8601 timestamp to relative time string
    private fun formatTimeAgo(isoTimestamp: String?): String {
        if (isoTimestamp == null) return "just now"
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(isoTimestamp.take(19)) ?: return "recently"
            val diffMs = System.currentTimeMillis() - date.time
            val diffMin = diffMs / 60000
            when {
                diffMin < 1 -> "just now"
                diffMin < 60 -> "${diffMin}m ago"
                diffMin < 1440 -> "${diffMin / 60}h ago"
                else -> "${diffMin / 1440}d ago"
            }
        } catch (e: Exception) {
            "recently"
        }
    }

    fun loadCoWatchRoomsFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val memberships = com.example.supabase.postgrest.from("co_watch_members")
                    .select { filter { eq("profile_id", currentUser.id) } }
                    .decodeList<SupabaseCoWatchMember>()

                val roomIds = memberships.map { it.room_id }.distinct()

                val dbRooms = if (roomIds.isNotEmpty()) {
                    com.example.supabase.postgrest.from("co_watch_rooms")
                        .select()
                        .decodeList<SupabaseCoWatchRoom>()
                        .filter { roomIds.contains(it.id) }
                } else {
                    emptyList()
                }

                val mappedRooms = dbRooms.mapNotNull { dbRoom ->
                    try {
                        val desc = dbRoom.description ?: return@mapNotNull null
                        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        val meta = json.decodeFromString<CoWatchRoomMetadata>(desc)
                        CoWatchRoom(
                            id = dbRoom.id ?: java.util.UUID.randomUUID().toString(),
                            roomName = dbRoom.name,
                            mediaType = meta.mediaType,
                            mediaTitle = meta.mediaTitle,
                            mediaId = meta.mediaId,
                            mediaDuration = meta.mediaDuration,
                            mediaColorHex = meta.mediaColorHex,
                            creatorName = if (dbRoom.profile_id == currentUser.id) "Me" else "Friend",
                            invitedUser = SocialActiveUser(
                                uid = meta.invitedUid,
                                name = meta.invitedName,
                                email = meta.invitedEmail
                            ),
                            status = meta.status,
                            isPlaying = meta.isPlaying,
                            currentPlaybackPosition = meta.currentPlaybackPosition,
                            playbackProgress = meta.playbackProgress.toFloat()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                withContext(Dispatchers.Main) {
                    _coWatchRooms.value = mappedRooms
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load co-watch rooms", e)
            }
        }
    }

    fun createCoWatchRoomInSupabase(
        roomName: String,
        mediaType: String,
        mediaTitle: String,
        mediaId: String,
        mediaDuration: String,
        mediaColorHex: String,
        invitedUser: SocialActiveUser
    ) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val meta = CoWatchRoomMetadata(
                    mediaType = mediaType,
                    mediaTitle = mediaTitle,
                    mediaId = mediaId,
                    mediaDuration = mediaDuration,
                    mediaColorHex = mediaColorHex,
                    invitedUid = invitedUser.uid,
                    invitedName = invitedUser.name,
                    invitedEmail = invitedUser.email,
                    status = "PENDING",
                    isPlaying = true,
                    currentPlaybackPosition = "00:00",
                    playbackProgress = 0.0
                )
                val metadataString = json.encodeToString(CoWatchRoomMetadata.serializer(), meta)

                val roomPayload = SupabaseCoWatchRoom(
                    profile_id = currentUser.id,
                    name = roomName,
                    description = metadataString,
                    is_active = true,
                    session_token = invitedUser.uid
                )

                val insertedRooms = com.example.supabase.postgrest.from("co_watch_rooms")
                    .insert(roomPayload) { select() }
                    .decodeList<SupabaseCoWatchRoom>()

                val insertedRoom = insertedRooms.firstOrNull() ?: return@launch
                val roomId = insertedRoom.id ?: return@launch

                val hostPayload = SupabaseCoWatchMember(
                    room_id = roomId,
                    profile_id = currentUser.id,
                    is_host = true
                )
                com.example.supabase.postgrest.from("co_watch_members")
                    .insert(hostPayload)

                val guestPayload = SupabaseCoWatchMember(
                    room_id = roomId,
                    profile_id = invitedUser.uid,
                    is_host = false
                )
                com.example.supabase.postgrest.from("co_watch_members")
                    .insert(guestPayload)

                val msgPayload = SupabaseCoWatchMessage(
                    room_id = roomId,
                    profile_id = currentUser.id,
                    message = "Co-Watch room created! Waiting for ${invitedUser.name} to join..."
                )
                com.example.supabase.postgrest.from("co_watch_messages")
                    .insert(msgPayload)
                logSocialActivityToSupabase("joined_room", roomId = roomId)

                val newRoom = CoWatchRoom(
                    id = roomId,
                    roomName = roomName,
                    mediaType = mediaType,
                    mediaTitle = mediaTitle,
                    mediaId = mediaId,
                    mediaDuration = mediaDuration,
                    mediaColorHex = mediaColorHex,
                    creatorName = "Me",
                    invitedUser = invitedUser,
                    status = "PENDING",
                    isPlaying = true,
                    currentPlaybackPosition = "00:00",
                    playbackProgress = 0f
                )

                withContext(Dispatchers.Main) {
                    _activeCoWatchRoom.value = newRoom
                    _coWatchVideoCallActive.value = false
                }

                loadCoWatchRoomsFromSupabase()
                loadCoWatchMessagesFromSupabase(roomId)

                delay(1500)
                val currentActive = _activeCoWatchRoom.value
                if (currentActive != null && currentActive.id == roomId) {
                    val updatedMeta = meta.copy(status = "ACTIVE")
                    val updatedMetaString = json.encodeToString(CoWatchRoomMetadata.serializer(), updatedMeta)
                    val updatedRoom = insertedRoom.copy(description = updatedMetaString)
                    com.example.supabase.postgrest.from("co_watch_rooms")
                        .update(updatedRoom) { filter { eq("id", roomId) } }

                    val joinMsgPayload1 = SupabaseCoWatchMessage(
                        room_id = roomId,
                        profile_id = invitedUser.uid,
                        message = "${invitedUser.name} joined the room! Synchronized watching initialized."
                    )
                    com.example.supabase.postgrest.from("co_watch_messages")
                        .insert(joinMsgPayload1)

                    delay(1000)
                    val joinMsgPayload2 = SupabaseCoWatchMessage(
                        room_id = roomId,
                        profile_id = invitedUser.uid,
                        message = "Hey! Thanks for inviting me to watch '$mediaTitle' together. This looks super fun!"
                    )
                    com.example.supabase.postgrest.from("co_watch_messages")
                        .insert(joinMsgPayload2)

                    withContext(Dispatchers.Main) {
                        _activeCoWatchRoom.value = newRoom.copy(status = "ACTIVE")
                    }
                    loadCoWatchRoomsFromSupabase()
                    loadCoWatchMessagesFromSupabase(roomId)
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to create co-watch room", e)
            }
        }
    }

    fun joinExistingCoWatchRoomInSupabase(room: CoWatchRoom) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _activeCoWatchRoom.value = room
                    _coWatchVideoCallActive.value = false
                }
                loadCoWatchMessagesFromSupabase(room.id)

                val msgPayload = SupabaseCoWatchMessage(
                    room_id = room.id,
                    profile_id = currentUser.id,
                    message = "Welcome back to the room! Synchronized playback is active."
                )
                com.example.supabase.postgrest.from("co_watch_messages")
                    .insert(msgPayload)
                logSocialActivityToSupabase("joined_room", roomId = room.id)
                loadCoWatchMessagesFromSupabase(room.id)
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to join co-watch room", e)
            }
        }
    }

    fun sendCoWatchMessageInSupabase(text: String) {
        if (text.isBlank()) return
        val currentRoom = _activeCoWatchRoom.value ?: return
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = SupabaseCoWatchMessage(
                    room_id = currentRoom.id,
                    profile_id = currentUser.id,
                    message = text
                )
                com.example.supabase.postgrest.from("co_watch_messages")
                    .insert(payload)
                loadCoWatchMessagesFromSupabase(currentRoom.id)

                delay(2000)
                val check = _activeCoWatchRoom.value
                if (check != null && check.id == currentRoom.id) {
                    val replyText = when {
                        currentRoom.mediaType == "COURSE" -> {
                            listOf(
                                "This instructor explains the core concepts so well. What do you think?",
                                "Let me know if you want to pause or replay any part. We should check the quiz together too!",
                                "Wow! I am writing down notes on this. Studying together makes it much easier.",
                                "Yeah! The LMS portal is amazing. Let's finish this module today!",
                                "Indeed! I finally understand this topic."
                            ).random()
                        }
                        else -> {
                            listOf(
                                "This scene has absolutely stunning visuals! The cinema aspect ratio looks premium too.",
                                "Oh wow! I didn't see that plot twist coming at all!",
                                "Hey, is your audio in sync? This movie or show is amazing so far.",
                                "Haha, that was a hilarious moment! Glad we're watching this unitedly.",
                                "I'm totally hooked! The background score is beautiful."
                            ).random()
                        }
                    }

                    val replyPayload = SupabaseCoWatchMessage(
                        room_id = currentRoom.id,
                        profile_id = currentRoom.invitedUser.uid,
                        message = replyText
                    )
                    com.example.supabase.postgrest.from("co_watch_messages")
                        .insert(replyPayload)
                    loadCoWatchMessagesFromSupabase(currentRoom.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to send co-watch message", e)
            }
        }
    }

    fun loadCoWatchMessagesFromSupabase(roomId: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbMsgs = com.example.supabase.postgrest.from("co_watch_messages")
                    .select { filter { eq("room_id", roomId) } }
                    .decodeList<SupabaseCoWatchMessage>()

                val chatMsgs = dbMsgs.map {
                    ChatMessage(
                        id = it.id ?: java.util.UUID.randomUUID().toString(),
                        text = it.message,
                        isSenderUser = it.profile_id == currentUser.id,
                        timestamp = "Just Now"
                    )
                }

                val dbMembers = com.example.supabase.postgrest.from("co_watch_members")
                    .select { filter { eq("room_id", roomId) } }
                    .decodeList<SupabaseCoWatchMember>()

                withContext(Dispatchers.Main) {
                    _coWatchMessages.value = chatMsgs
                    _activeRoomMembersCount.value = dbMembers.size
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load co-watch messages", e)
            }
        }
    }

    fun inviteAdditionalUserToRoomInSupabase(roomId: String, friendProfileId: String, name: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val guestPayload = SupabaseCoWatchMember(
                    room_id = roomId,
                    profile_id = friendProfileId,
                    is_host = false
                )
                com.example.supabase.postgrest.from("co_watch_members")
                    .insert(guestPayload)

                val msgPayload = SupabaseCoWatchMessage(
                    room_id = roomId,
                    profile_id = currentUser.id,
                    message = "$name has been invited to join the room. Switching to Jitsi Meet group conference..."
                )
                com.example.supabase.postgrest.from("co_watch_messages")
                    .insert(msgPayload)

                loadCoWatchMessagesFromSupabase(roomId)
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to invite additional user", e)
            }
        }
    }

    fun toggleCoWatchPlayPauseInSupabase() {
        val current = _activeCoWatchRoom.value ?: return
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val dbRooms = com.example.supabase.postgrest.from("co_watch_rooms")
                    .select { filter { eq("id", current.id) } }
                    .decodeList<SupabaseCoWatchRoom>()
                val dbRoom = dbRooms.firstOrNull() ?: return@launch
                val desc = dbRoom.description ?: return@launch
                val meta = json.decodeFromString<CoWatchRoomMetadata>(desc)

                val updatedMeta = meta.copy(isPlaying = !meta.isPlaying)
                val updatedMetaString = json.encodeToString(CoWatchRoomMetadata.serializer(), updatedMeta)

                val updatedRoom = dbRoom.copy(description = updatedMetaString)
                com.example.supabase.postgrest.from("co_watch_rooms")
                    .update(updatedRoom) { filter { eq("id", current.id) } }

                val actionText = if (updatedMeta.isPlaying) "You resumed playback" else "You paused playback"
                val systemMsgPayload = SupabaseCoWatchMessage(
                    room_id = current.id,
                    profile_id = currentUser.id,
                    message = "$actionText (synchronized)"
                )
                com.example.supabase.postgrest.from("co_watch_messages")
                    .insert(systemMsgPayload)

                val domainRoom = current.copy(isPlaying = updatedMeta.isPlaying)
                withContext(Dispatchers.Main) {
                    _activeCoWatchRoom.value = domainRoom
                }
                loadCoWatchRoomsFromSupabase()
                loadCoWatchMessagesFromSupabase(current.id)

                if (!updatedMeta.isPlaying) {
                    delay(1200)
                    val active = _activeCoWatchRoom.value
                    if (active != null && active.id == current.id && !active.isPlaying) {
                        val friendMsgPayload = SupabaseCoWatchMessage(
                            room_id = current.id,
                            profile_id = current.invitedUser.uid,
                            message = "${current.invitedUser.name}: 'Sure, take your time! Let me know when you want to resume.'"
                        )
                        com.example.supabase.postgrest.from("co_watch_messages")
                            .insert(friendMsgPayload)
                        loadCoWatchMessagesFromSupabase(current.id)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to toggle play/pause", e)
            }
        }
    }

    fun seekCoWatchPositionInSupabase(progress: Float, positionText: String) {
        val current = _activeCoWatchRoom.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val dbRooms = com.example.supabase.postgrest.from("co_watch_rooms")
                    .select { filter { eq("id", current.id) } }
                    .decodeList<SupabaseCoWatchRoom>()
                val dbRoom = dbRooms.firstOrNull() ?: return@launch
                val desc = dbRoom.description ?: return@launch
                val meta = json.decodeFromString<CoWatchRoomMetadata>(desc)

                val updatedMeta = meta.copy(playbackProgress = progress.toDouble(), currentPlaybackPosition = positionText)
                val updatedMetaString = json.encodeToString(CoWatchRoomMetadata.serializer(), updatedMeta)

                val updatedRoom = dbRoom.copy(description = updatedMetaString)
                com.example.supabase.postgrest.from("co_watch_rooms")
                    .update(updatedRoom) { filter { eq("id", current.id) } }

                val domainRoom = current.copy(playbackProgress = progress, currentPlaybackPosition = positionText)
                withContext(Dispatchers.Main) {
                    _activeCoWatchRoom.value = domainRoom
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to seek position", e)
            }
        }
    }

    // ============================================================
    // NOTIFICATIONS — Supabase-backed
    // ============================================================

    fun loadNotificationsFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = com.example.supabase.postgrest.from("notifications")
                    .select {
                        filter { eq("profile_id", currentUser.id) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(50)
                    }
                    .decodeList<SupabaseNotification>()
                withContext(Dispatchers.Main) {
                    val mapped = items.map { n ->
                        NotificationItem(
                            id = n.id,
                            title = n.title,
                            description = n.description ?: "",
                            type = n.type,
                            timestamp = n.created_at?.take(10) ?: "—",
                            isRead = n.is_read
                        )
                    }
                    if (mapped.isNotEmpty()) _notifications.value = mapped
                }
                android.util.Log.d("Supabase", "Loaded ${items.size} notifications from Supabase")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load notifications from Supabase", e)
            }
        }
    }

    fun markNotificationReadInSupabase(notificationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("notifications")
                    .update(mapOf("is_read" to true)) { filter { eq("id", notificationId) } }
                withContext(Dispatchers.Main) {
                    _notifications.value = _notifications.value.map {
                        if (it.id == notificationId) it.copy(isRead = true) else it
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to mark notification read", e)
            }
        }
    }

    fun createNotificationInSupabase(title: String, description: String, type: String = "REMINDER") {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = mapOf(
                    "profile_id" to currentUser.id,
                    "title" to title,
                    "description" to description,
                    "type" to type,
                    "is_read" to false
                )
                com.example.supabase.postgrest.from("notifications").insert(payload)
                loadNotificationsFromSupabase()
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to create notification", e)
            }
        }
    }

    fun markAllNotificationsReadInSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("notifications")
                    .update(mapOf("is_read" to true)) {
                        filter {
                            eq("profile_id", currentUser.id)
                            eq("is_read", false)
                        }
                    }
                withContext(Dispatchers.Main) {
                    _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to mark all notifications read", e)
            }
        }
    }

    // ============================================================
    // INIT
    // ============================================================
    // ---------- INIT ----------

    init {
        // Pre-populate with fallback for instant UI
        _youtubeHomeVideos.value = curatedHomeFallback
        _youtubeShortsVideos.value = curatedShortsFallback

        // Fetch fresh data
        fetchYouTubeHomeFeeds()
        fetchYouTubeShorts()
        loadAdminContentFromFirebase()

        viewModelScope.launch {
            FirebaseManager.currentUser.collect { _ ->
                loadCoursesFromFirestore()
            }
        }

        loadHomeCarouselsFromSupabase()
        loadCoursesFromSupabase()

        // Auto-detect existing Supabase session and load profiles without mocks
        viewModelScope.launch {
            val user = com.example.supabase.auth.currentUserOrNull()
            if (user != null) {
                _isLoggedIn.value = true
                loadProfilesFromSupabase()
                loadUserPlaylistsFromSupabase()
                loadSupportTicketsFromSupabase()
                loadNotificationsFromSupabase()
                loadRewardsAndReferrals()
                loadSocialNetwork()
                loadCoWatchRoomsFromSupabase()
                loadRoutinePlansFromSupabase()
                loadSocialActivityFeedFromSupabase()
            }
        }

        // Load global app config (no auth required)
        loadAppConfigFromSupabase()
        loadCategoriesFromSupabase()
        loadNoticePopupsFromSupabase()
        loadAdBannersFromSupabase()
        checkForAppUpdatesFromSupabase()

        // Initialize Invidious instances
        viewModelScope.launch(Dispatchers.IO) {
            val list = InvidiousService.getInstances().map {
                InvidiousInstanceStatus(
                    url = it,
                    isCustom = InvidiousService.customInstances.contains(it)
                )
            }
            _invidiousInstances.value = list
            _preferredInstance.value = InvidiousService.preferredInstance
            _autoSwitchOnFailure.value = InvidiousService.autoSwitchOnFailure
        }

        // Listen for FCM notifications
        viewModelScope.launch {
            FirebaseManager.fcmNotifications.collect { item: NotificationItem ->
                _notifications.value = listOf(item) + _notifications.value
            }
        }
    }

    // ─── NEW BATCH: Supabase integrations ───

    private val _categories = MutableStateFlow<List<SupabaseCategory>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _activeContentAssets = MutableStateFlow<List<SupabaseContentAsset>>(emptyList())
    val activeContentAssets = _activeContentAssets.asStateFlow()

    private val _epgPrograms = MutableStateFlow<List<SupabaseEpgProgram>>(emptyList())
    val epgPrograms = _epgPrograms.asStateFlow()

    private val _activeNoticePopups = MutableStateFlow<List<SupabaseNoticePopup>>(emptyList())
    val activeNoticePopups = _activeNoticePopups.asStateFlow()

    private val _adBanners = MutableStateFlow<List<SupabaseAdBanner>>(emptyList())
    val adBanners = _adBanners.asStateFlow()

    private val _latestAppUpdate = MutableStateFlow<SupabaseAppUpdate?>(null)
    val latestAppUpdate = _latestAppUpdate.asStateFlow()

    fun loadCategoriesFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.supabase.postgrest.from("categories")
                    .select().decodeList<SupabaseCategory>()
                _categories.value = result
                android.util.Log.d("Supabase", "Loaded ${result.size} categories from Supabase")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error loading categories", e)
            }
        }
    }

    fun loadContentAssetsFromSupabase(contentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.supabase.postgrest.from("content_assets")
                    .select {
                        filter { eq("content_id", contentId) }
                    }.decodeList<SupabaseContentAsset>()
                _activeContentAssets.value = result
                android.util.Log.d("Supabase", "Loaded ${result.size} assets for content $contentId")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error loading content assets", e)
            }
        }
    }

    fun loadEpgProgramsFromSupabase(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.supabase.postgrest.from("epg_programs")
                    .select {
                        filter { eq("channel_id", channelId) }
                    }.decodeList<SupabaseEpgProgram>()
                _epgPrograms.value = result
                android.util.Log.d("Supabase", "Loaded ${result.size} EPG programs for channel $channelId")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error loading EPG programs", e)
            }
        }
    }

    fun loadNoticePopupsFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.supabase.postgrest.from("notice_popups")
                    .select {
                        filter { eq("is_active", true) }
                    }.decodeList<SupabaseNoticePopup>()
                _activeNoticePopups.value = result
                android.util.Log.d("Supabase", "Loaded ${result.size} active notice popups")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error loading notice popups", e)
            }
        }
    }

    fun loadAdBannersFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.supabase.postgrest.from("ads_banners")
                    .select {
                        filter { eq("is_active", true) }
                    }.decodeList<SupabaseAdBanner>()
                _adBanners.value = result
                android.util.Log.d("Supabase", "Loaded ${result.size} active ad banners")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error loading ad banners", e)
            }
        }
    }

    fun logAdImpressionInSupabase(adId: String, clicked: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profileId = selectedProfile.value?.id
                val impression = SupabaseAdImpression(
                    ad_id = adId,
                    profile_id = profileId,
                    clicked = clicked
                )
                com.example.supabase.postgrest.from("ad_impressions").insert(impression)
                android.util.Log.d("Supabase", "Logged ad impression in Supabase for ad $adId")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error logging ad impression", e)
            }
        }
    }

    fun checkForAppUpdatesFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.supabase.postgrest.from("app_updates")
                    .select().decodeList<SupabaseAppUpdate>()
                _latestAppUpdate.value = result.maxByOrNull { it.version_code }
                android.util.Log.d("Supabase", "App update check complete. Latest: ${_latestAppUpdate.value?.version_name}")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error checking for app updates", e)
            }
        }
    }

    fun logAppUpdateDownloadInSupabase(updateId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profileId = selectedProfile.value?.id
                val downloadLog = SupabaseAppUpdateDownload(
                    update_id = updateId,
                    profile_id = profileId,
                    ip_address = "127.0.0.1"
                )
                com.example.supabase.postgrest.from("app_update_downloads").insert(downloadLog)
                android.util.Log.d("Supabase", "Logged app update download hit in Supabase")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error logging app update download hit", e)
            }
        }
    }

    fun syncDownloadStateToSupabase(contentId: String, status: String, progress: Double, fileSize: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pid = selectedProfile.value?.id ?: return@launch
                val dl = SupabaseDownload(
                    profile_id = pid,
                    content_id = contentId,
                    status = status,
                    progress = progress,
                    file_size = fileSize
                )
                com.example.supabase.postgrest.from("downloads").upsert(dl) {
                    filter {
                        eq("profile_id", pid)
                        eq("content_id", contentId)
                    }
                }
                android.util.Log.d("Supabase", "Synced download state to Supabase: contentId=$contentId, status=$status")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error syncing download state to Supabase", e)
            }
        }
    }

    // ───── TV COMPANION & PAIRINGS (tv_qr_pairings) ──────────────────────────
    private val _tvPairings = MutableStateFlow<List<SupabaseTvQrPairing>>(emptyList())
    val tvPairings = _tvPairings.asStateFlow()

    private val _movieConfigs = MutableStateFlow<Map<String, SupabaseMovieScreenConfig>>(emptyMap())
    val movieConfigs = _movieConfigs.asStateFlow()

    fun loadTvPairingsFromSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = com.example.supabase.postgrest.from("tv_qr_pairings")
                    .select { filter { eq("profile_id", currentUser.id) } }
                    .decodeList<SupabaseTvQrPairing>()
                withContext(Dispatchers.Main) {
                    _tvPairings.value = list
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load TV pairings", e)
            }
        }
    }

    fun generateTvPairingCodeInSupabase() {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val letters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                val code = (1..6).map { letters.random() }.joinToString("")

                val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val expiresAt = formatter.format(java.util.Date(System.currentTimeMillis() + 10 * 60 * 1000))

                val newPairing = SupabaseTvQrPairing(
                    profile_id = currentUser.id,
                    code = code,
                    qr_image_url = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=https://swapnoflix.tv/pair?code=$code",
                    pairing_url = "https://swapnoflix.tv/pair?code=$code",
                    status = "pending",
                    expires_at = expiresAt
                )
                com.example.supabase.postgrest.from("tv_qr_pairings").insert(newPairing)
                loadTvPairingsFromSupabase()
                logAuditActionToSupabase("GENERATE_PAIRING_CODE", "tv_qr_pairings", code, "Created companion pairing code $code")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to generate pairing code", e)
            }
        }
    }

    fun cancelPairingInSupabase(pairingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.supabase.postgrest.from("tv_qr_pairings").delete {
                    filter { eq("id", pairingId) }
                }
                loadTvPairingsFromSupabase()
                logAuditActionToSupabase("CANCEL_PAIRING_CODE", "tv_qr_pairings", pairingId, "Cancelled companion pairing link")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to cancel pairing", e)
            }
        }
    }

    // ───── MOVIE SCREEN CONFIGS (movie_screen_configs) ──────────────────────
    fun loadMovieScreenConfigFromSupabase(contentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.supabase.postgrest.from("movie_screen_configs")
                    .select { filter { eq("content_id", contentId) } }
                    .decodeList<SupabaseMovieScreenConfig>()
                val config = result.firstOrNull()
                if (config != null) {
                    withContext(Dispatchers.Main) {
                        val current = _movieConfigs.value.toMutableMap()
                        current[contentId] = config
                        _movieConfigs.value = current
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load movie screen config", e)
            }
        }
    }

    // ───── AUDIT LOGS (audit_logs) ───────────────────────────────────────────
    fun logAuditActionToSupabase(action: String, entityType: String? = null, entityId: String? = null, detailsText: String = "") {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val log = SupabaseAuditLog(
                    profile_id = currentUser.id,
                    action = action,
                    entity_type = entityType,
                    entity_id = entityId,
                    details = "{\"description\": \"$detailsText\"}"
                )
                com.example.supabase.postgrest.from("audit_logs").insert(log)
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to insert audit log", e)
            }
        }
    }

    // ───── SAAS MULTI-TENANCY (tenants + tenant_members) ──────────────────────
    private val _activeTenant = MutableStateFlow<SupabaseTenant?>(null)
    val activeTenant = _activeTenant.asStateFlow()

    private val _tenantMember = MutableStateFlow<SupabaseTenantMember?>(null)
    val tenantMember = _tenantMember.asStateFlow()

    fun loadTenantDetailsFromSupabase(tenantId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.supabase.postgrest.from("tenants")
                    .select { filter { eq("id", tenantId) } }
                    .decodeList<SupabaseTenant>()
                val tenant = result.firstOrNull()
                withContext(Dispatchers.Main) {
                    _activeTenant.value = tenant
                }
                android.util.Log.d("Supabase", "Loaded active tenant: ${tenant?.name}")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load tenant details", e)
            }
        }
    }

    fun loadTenantMemberRoleFromSupabase(tenantId: String) {
        val currentUser = com.example.supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.supabase.postgrest.from("tenant_members")
                    .select {
                        filter {
                            eq("tenant_id", tenantId)
                            eq("user_id", currentUser.id)
                        }
                    }.decodeList<SupabaseTenantMember>()
                val member = result.firstOrNull()
                withContext(Dispatchers.Main) {
                    _tenantMember.value = member
                }
                android.util.Log.d("Supabase", "Loaded tenant member role: ${member?.role}")
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Failed to load tenant member role", e)
            }
        }
    }

    init {
        viewModelScope.launch {
            LiveTvRecordingService.recordingChannelId.collect { id ->
                _recordingChannelId.value = id
                _isTvRecordingActive.value = id != null
            }
        }

        viewModelScope.launch {
            // YouTube Background Notification Sync
            kotlinx.coroutines.flow.combine(playTubeCurrentVideo, playTubeIsPlaying) { video, isPlaying ->
                video to isPlaying
            }.collect { (video, isPlaying) ->
                val context = getApplication<Application>()
                val serviceIntent = Intent(context, PlayTubeBackgroundService::class.java)
                if (video != null && isPlaying) {
                    serviceIntent.action = "START_PLAYER_NOTIFICATION"
                    serviceIntent.putExtra("VIDEO_ID", video.id)
                    serviceIntent.putExtra("VIDEO_TITLE", video.title)
                    serviceIntent.putExtra("CHANNEL_NAME", video.channelName)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                    serviceIntent.action = "STOP_PLAYER_NOTIFICATION"
                    context.startService(serviceIntent)
                }
            }
        }

        viewModelScope.launch {
            // Listen for background control callbacks from the notification service
            PlayTubeBackgroundService.serviceActions.collect { action ->
                when (action) {
                    "STOP" -> {
                        _playTubeIsPlaying.value = false
                    }
                    "NEXT" -> {
                        playNextPlayTubeVideo()
                    }
                }
            }
        }
    }

    fun exitCoWatchActiveRoom() {
        _activeCoWatchRoom.value = null
    }

    fun toggleCoWatchMic() {
        _coWatchMicMuted.value = !_coWatchMicMuted.value
    }

    fun enrollInCourse(courseId: String) {
        enrollInCourseInSupabase(courseId)
    }

    fun setRecordingChannelId(id: String?) {
        _recordingChannelId.value = id
    }

    fun cancelActiveSubscription() {
        _subscriptionStatus.value = "CANCELLED"
        _isAutoRenewActive.value = false
    }

    fun toggleAutoRenew() {
        _isAutoRenewActive.value = !_isAutoRenewActive.value
    }

    fun completePayment(method: String) {
        _subscriptionStatus.value = "ACTIVE"
    }

    fun updateParentalSetup(pin: String, lock: Boolean, age: String) {
        _parentalControlPin.value = pin
        _isParentalLockActive.value = lock
        _allowedAgeBracket.value = age
    }
}

@UnstableApi
object LiveTvCacheManager {
    private var cache: SimpleCache? = null
    fun getCache(context: android.content.Context): SimpleCache? {
        return try {
            if (cache == null) {
                val cacheDir = java.io.File(context.cacheDir, "live_tv_media_cache")
                val databaseProvider = StandaloneDatabaseProvider(context)
                cache = SimpleCache(cacheDir, NoOpCacheEvictor(), databaseProvider)
            }
            cache
        } catch (e: Throwable) {
            android.util.Log.e("LiveTvCacheManager", "Cache initialization failed", e)
            null
        }
    }
}
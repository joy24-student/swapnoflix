package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

fun filterAndMapChannels(
    channels: List<LiveChannel>,
    searchQuery: String,
    category: String,
    country: String,
    language: String
): List<LiveChannel> {
    return channels.filter { ch ->
        (category == "All" || ch.category.equals(category, ignoreCase = true)) &&
                (country == "All Countries" || ch.country.equals(country, ignoreCase = true)) &&
                (language == "All Languages" || ch.language.equals(language, ignoreCase = true)) &&
                (searchQuery.isEmpty() || ch.name.lowercase().contains(searchQuery.lowercase()))
    }
}

private data class FeaturedProgram(
    val title: String,
    val channelId: String,
    val time: String,
    val imageUrl: String,
    val category: String
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LiveTVHomeScreen(viewModel: StreamingViewModel) {
    val activeChannel by viewModel.activeLiveChannel.collectAsState()
    val favoritedList by viewModel.favoriteChannels.collectAsState()
    val isMiniPlayerActive by viewModel.isMiniPlayerActive.collectAsState()
    val allChannelsList by viewModel.channelsList.collectAsState()
    val liveTVSearchQuery by viewModel.liveTVSearchQuery.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedCountry by remember { mutableStateOf("All Countries") }
    var selectedLanguage by remember { mutableStateOf("All Languages") }
    
    var showFilters by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var detailChannelToShow by remember { mutableStateOf<LiveChannel?>(null) }
    
    val featuredPrograms = remember {
        listOf(
            FeaturedProgram("Grand Football Championship Live", "1", "18:00 - 20:30", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800", "Sports"),
            FeaturedProgram("Global Breaking News Special", "2", "20:30 - 21:00", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800", "News"),
            FeaturedProgram("Premiere: The Midnight Galaxy", "3", "21:00 - 23:30", "https://images.unsplash.com/photo-1538370965046-79c0d6907d47?w=800", "Movies")
        )
    }
    var currentFeaturedIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            currentFeaturedIndex = (currentFeaturedIndex + 1) % featuredPrograms.size
        }
    }

    val appColors = LocalAppColors.current

    val filteredChannels = filterAndMapChannels(
        allChannelsList,
        liveTVSearchQuery,
        selectedCategory,
        selectedCountry,
        selectedLanguage
    )

    val favoriteChannels = allChannelsList.filter { favoritedList.contains(it.id) }

    if (activeChannel != null && !isMiniPlayerActive) {
        PremiumImmersivePlayerScreen(
            channel = activeChannel!!,
            viewModel = viewModel,
            allChannels = allChannelsList,
            isFavorited = favoritedList.contains(activeChannel!!.id),
            onBack = { viewModel.selectLiveChannel(null) }
        )
    } else {
        Scaffold(
            containerColor = appColors.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(appColors.background)
                            .padding(bottom = 4.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(listOf("All", "Sports", "News", "Movies", "Kids", "Entertainment")) { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontWeight = FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = appColors.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = appColors.cardBg,
                                        labelColor = appColors.textSecondary
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    val activeProg = featuredPrograms[currentFeaturedIndex]
                    val context = LocalContext.current
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        AnimatedContent(
                            targetState = activeProg,
                            transitionSpec = {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            },
                            label = "featured_program_slider",
                            modifier = Modifier.fillMaxWidth()
                        ) { prog ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clickable {
                                        val target = allChannelsList.find { it.id == prog.channelId }
                                        if (target != null) {
                                            viewModel.selectLiveChannel(target, context)
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = prog.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                )
                                            )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color.Red, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "FEATURED ${prog.category.uppercase()}",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Text(
                                            text = prog.title,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Schedule,
                                                contentDescription = null,
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = prog.time,
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        featuredPrograms.forEachIndexed { index, _ ->
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        if (index == currentFeaturedIndex) Color(0xFF00FFC4)
                                                        else Color.White.copy(alpha = 0.4f),
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Favorite Channels Row (Requirement 5)
                if (favoriteChannels.isNotEmpty()) {
                    item {
                        CategoryChannelRow("Favorite Channels", favoriteChannels, viewModel, favoritedList)
                    }
                }

                if (filteredChannels.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No channels match your filters.", color = appColors.textMuted)
                        }
                    }
                } else {
                    item {
                        CategoryChannelRow("Recently Viewed", filteredChannels.take(5), viewModel, favoritedList)
                    }
                    item {
                        CategoryChannelRow("Trending Now", filteredChannels.reversed().take(5), viewModel, favoritedList)
                    }
                    
                    item {
                        Text(
                            text = "All Broadcasters",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    items(filteredChannels.chunked(4)) { rowChannels ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowChannels.forEach { ch ->
                                val context = LocalContext.current
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.selectLiveChannel(ch, context) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .fillMaxWidth()
                                            .clip(CircleShape)
                                            .background(ch.logoAccent.copy(alpha = 0.15f))
                                            .border(BorderStroke(2.dp, ch.logoAccent), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (ch.logoUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = ch.logoUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Text(
                                                ch.name.take(2).uppercase(),
                                                color = ch.logoAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .offset(y = 2.dp)
                                                .background(Color.Red, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("LIVE", color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = ch.name,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textPrimary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            repeat(4 - rowChannels.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    detailChannelToShow?.let { ch ->
        ChannelDetailsBottomSheet(
            channel = ch,
            onDismiss = { detailChannelToShow = null },
            onLaunchPlayer = { 
                viewModel.selectLiveChannel(ch)
                detailChannelToShow = null
            },
            viewModel = viewModel
        )
    }
}

fun enterPipMode(context: android.content.Context) {
    var currentContext = context
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                currentContext.enterPictureInPictureMode(params)
            }
            break
        }
        currentContext = currentContext.baseContext
    }
}

@Composable
fun BufferingLogoLoader(channel: LiveChannel) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        CircularProgressIndicator(
            color = Color(0xFFE50914),
            strokeWidth = 3.dp,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(channel.logoAccent.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, Color(0xFFE50914), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logoUrl.isNotEmpty()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = channel.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PremiumImmersivePlayerScreen(
    channel: LiveChannel,
    viewModel: StreamingViewModel,
    allChannels: List<LiveChannel>,
    isFavorited: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var volumeLevel by remember { mutableStateOf(0.85f) }
    var isMultiScreen by remember { mutableStateOf(false) }
    var activeAudioSource by remember { mutableStateOf(1) } // 1 = Main, 2 = Secondary
    var timelineProgress by remember { mutableStateOf(0.0f) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var currentPos by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(100000L) }
    var optionSheet by remember { mutableStateOf<PlayerOptionSheet?>(null) }
    var isUsingSimulation by remember { mutableStateOf(false) }
    var hasReadyPlayed by remember { mutableStateOf(false) }
    var showEpgGuideModal by remember { mutableStateOf(false) }
    val epgPrograms by viewModel.epgPrograms.collectAsState()
    val epgReminders = remember { mutableStateMapOf<String, Boolean>() }
    val appColors = LocalAppColors.current
    val favoritedList by viewModel.favoriteChannels.collectAsState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var recordingNameInput by remember { mutableStateOf("") }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Notification permission granted. Tap REC again to start.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Notification permission is required to display background DVR progress.", Toast.LENGTH_LONG).show()
            }
        }
    )

    val playbackAudio by viewModel.playbackAudio.collectAsState()
    val playbackSubtitle by viewModel.playbackSubtitle.collectAsState()

    val aspectModes = listOf(
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom",
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch"
    )
    var currentAspectIndex by remember { mutableStateOf(0) }
    val currentAspectMode = aspectModes[currentAspectIndex].first

    val isLiveTvFS by viewModel.liveTvIsFullScreen.collectAsState()
    val isPipState by viewModel.playTubeIsPipMode.collectAsState()
    val isPlayerFullScreen = isLiveTvFS || isPipState

    // 1. Bufferless/Smooth Playback LoadControl Configuration (Requirement 1)
    val exoPlayer = remember {
        try {
            ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                volume = volumeLevel
            }
        } catch (e: Throwable) { 
            android.util.Log.e("LiveTVPlayer", "ExoPlayer init failed", e)
            null 
        }
    }
    
    // If ExoPlayer failed to create, fall back to simulation
    LaunchedEffect(exoPlayer) {
        if (exoPlayer == null) isUsingSimulation = true
    }

    // 2. Multiscreen Secondary Player (Requirement 3)
    val secondaryExoPlayer = remember {
        try {
            val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(4000, 10000, 1000, 1500)
                .build()
            ExoPlayer.Builder(context).setLoadControl(loadControl).build().apply { 
                playWhenReady = true
                volume = 0f 
            }
        } catch (e: Throwable) { 
            null 
        }
    }

    DisposableEffect(secondaryExoPlayer) {
        onDispose { secondaryExoPlayer?.release() }
    }

    // Load one fixed channel only once when entering multiscreen mode. (Requirement 3)
    // "two different screen channel can not change , just one screen can always control other show just one channel all time"
    val defaultSecondaryChannel = remember { allChannels.firstOrNull { it.id != channel.id } ?: channel }
    var secondaryChannel by remember { mutableStateOf(defaultSecondaryChannel) }
    var activeControlScreen by remember { mutableStateOf(1) } // 1 = Main, 2 = Secondary

    LaunchedEffect(isMultiScreen, secondaryChannel) {
        if (isMultiScreen && secondaryExoPlayer != null) {
            try {
                val streamUrlToUse = if (secondaryChannel.streamUrl.isNotEmpty()) secondaryChannel.streamUrl 
                                     else "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                secondaryExoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrlToUse)))
                secondaryExoPlayer.prepare()
                secondaryExoPlayer.playWhenReady = true
            } catch (e: Throwable) {
                android.util.Log.e("LiveTVPlayer", "Error loading secondary channel", e)
            }
        }
    }

    // Main playlist preloading effect
    LaunchedEffect(channel.id, exoPlayer) {
        if (exoPlayer != null) {
            try {
                isUsingSimulation = false
                playbackError = null
                isBuffering = true
                hasReadyPlayed = false

                val streamUrlToUse = if (channel.streamUrl.isNotEmpty()) channel.streamUrl
                                     else "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                val uri = Uri.parse(streamUrlToUse)

                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(channel.id)

                val lowerUrl = streamUrlToUse.lowercase()
                if (lowerUrl.contains(".m3u8")) {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                } else if (lowerUrl.contains(".mpd")) {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
                } else if (lowerUrl.contains(".mp4")) {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP4)
                }
                
                exoPlayer.setMediaItem(mediaItemBuilder.build())
                exoPlayer.prepare()
                exoPlayer.playWhenReady = isPlaying
            } catch (e: Throwable) {
                android.util.Log.e("LiveTVPlayer", "Error loading channel: ${e.localizedMessage}", e)
                isUsingSimulation = true
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) { 
                    hasReadyPlayed = true
                    playbackError = null 
                }
            }
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) { 
                playbackError = e.localizedMessage
                isBuffering = false 
                isUsingSimulation = true
            }
        }
        try {
            exoPlayer?.addListener(listener)
        } catch (e: Throwable) {}
        onDispose {
            try {
                exoPlayer?.removeListener(listener)
                exoPlayer?.release()
            } catch (e: Throwable) {}
            viewModel.setLiveTvFullScreen(false)
        }
    }

    LaunchedEffect(isPlaying) {
        try {
            if (isPlaying) exoPlayer?.play() else exoPlayer?.pause()
        } catch (e: Throwable) {}
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
            showControls = false
        }
    }

    LaunchedEffect(volumeLevel, activeAudioSource, isMultiScreen) {
        try {
            if (isMultiScreen) {
                if (activeAudioSource == 1) {
                    exoPlayer?.volume = volumeLevel
                    secondaryExoPlayer?.volume = 0f
                } else {
                    exoPlayer?.volume = 0f
                    secondaryExoPlayer?.volume = volumeLevel
                }
            } else {
                exoPlayer?.volume = volumeLevel
                secondaryExoPlayer?.volume = 0f
            }
        } catch (e: Throwable) {}
    }

    Column(modifier = Modifier.fillMaxSize().background(appColors.background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .then(
                    if (isPlayerFullScreen) Modifier.weight(1f)
                    else Modifier.height(245.dp)
                )
        ) {
            if (isMultiScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AndroidView(
                            factory = { ctx ->
                                CustomVideoPlayerView(ctx).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { view ->
                                view.player = exoPlayer
                                view.resizeMode = currentAspectMode
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isBuffering) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                                BufferingLogoLoader(channel)
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.DarkGray)) {
                        if (secondaryExoPlayer != null) {
                            AndroidView(
                                factory = { ctx ->
                                    CustomVideoPlayerView(ctx).apply {
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                update = { view ->
                                    view.player = secondaryExoPlayer
                                    view.resizeMode = currentAspectMode
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Preview", color = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            } else {
                if (exoPlayer != null && !isUsingSimulation) {
                    AndroidView(
                        factory = { ctx ->
                            CustomVideoPlayerView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            view.player = exoPlayer
                            view.resizeMode = currentAspectMode
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // Initial or buffer loading icon with Channel Logo (Requirement 7)
                    if (isBuffering) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                            BufferingLogoLoader(channel)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        BufferingLogoLoader(channel)
                    }
                }
            }

            if (showControls) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showControls = false }
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }

                    // ───── DVR BACKGROUND RECORDING BUTTON ─────
                    val currentRecordingId by viewModel.recordingChannelId.collectAsState()
                    val isRecordingThis = currentRecordingId == channel.id
                    val isRecordingOther = currentRecordingId != null && !isRecordingThis
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(
                                1.dp, 
                                when {
                                    isRecordingThis -> Color.Red
                                    isRecordingOther -> Color(0xFFFF9800)
                                    else -> Color.White.copy(alpha = 0.2f)
                                }, 
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val recordIntent = Intent(context, com.example.data.LiveTvRecordingService::class.java)
                                if (isRecordingThis) {
                                    // Trigger rename dialog
                                    recordingNameInput = "Record_${channel.name.replace(" ", "_")}_${System.currentTimeMillis()}"
                                    showRenameDialog = true
                                } else {
                                    val isPermissionGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    } else {
                                        true
                                    }
                                    
                                    if (!isPermissionGranted) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        if (isRecordingOther) {
                                            // Stop previous recording first
                                            val stopIntent = Intent(context, com.example.data.LiveTvRecordingService::class.java).apply {
                                                action = "STOP_RECORDING"
                                            }
                                            context.startService(stopIntent)
                                            Toast.makeText(context, "Switching background recording...", Toast.LENGTH_SHORT).show()
                                        }
                                        
                                        // Start recording new
                                        recordIntent.action = "START_RECORDING"
                                        recordIntent.putExtra("STREAM_URL", channel.streamUrl)
                                        recordIntent.putExtra("CHANNEL_NAME", channel.name)
                                        recordIntent.putExtra("CHANNEL_ID", channel.id)
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            context.startForegroundService(recordIntent)
                                        } else {
                                            context.startService(recordIntent)
                                        }
                                        viewModel.logAuditActionToSupabase("DVR_START", "channels", channel.id, "Started background DVR recording for ${channel.name}")
                                        Toast.makeText(context, "Background recording started for ${channel.name}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isRecordingThis -> Color.Red
                                            isRecordingOther -> Color(0xFFFF9800)
                                            else -> Color.Gray
                                        }
                                    )
                            )
                            Text(
                                text = when {
                                    isRecordingThis -> "REC"
                                    isRecordingOther -> "BUSY"
                                    else -> "DVR"
                                },
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Channel Transition
                        IconButton(onClick = {
                            val activeCh = if (activeControlScreen == 1) channel else secondaryChannel
                            val currentIndex = allChannels.indexOfFirst { it.id == activeCh.id }
                            if (currentIndex != -1) {
                                val targetIndex = (currentIndex - 1 + allChannels.size) % allChannels.size
                                val targetChannel = allChannels[targetIndex]
                                if (activeControlScreen == 1) {
                                    viewModel.selectLiveChannel(targetChannel)
                                } else {
                                    secondaryChannel = targetChannel
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous Channel",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play/Pause
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Next Channel Transition
                        IconButton(onClick = {
                            val activeCh = if (activeControlScreen == 1) channel else secondaryChannel
                            val currentIndex = allChannels.indexOfFirst { it.id == activeCh.id }
                            if (currentIndex != -1) {
                                val targetIndex = (currentIndex + 1) % allChannels.size
                                val targetChannel = allChannels[targetIndex]
                                if (activeControlScreen == 1) {
                                    viewModel.selectLiveChannel(targetChannel)
                                } else {
                                    secondaryChannel = targetChannel
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next Channel",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(bottom = 6.dp, start = 12.dp, end = 12.dp)
                    ) {
                        // Compact seek bar/slider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                        ) {
                            Slider(
                                value = timelineProgress,
                                onValueChange = { timelineProgress = it; currentPos = (it * totalDuration).toLong(); try { exoPlayer?.seekTo(currentPos) } catch (e: Throwable) {} },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Red,
                                    activeTrackColor = Color.Red,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatLiveTime(currentPos) + " / " + formatLiveTime(totalDuration),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Fullscreen button only
                                IconButton(
                                    onClick = { viewModel.setLiveTvFullScreen(!isLiveTvFS) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLiveTvFS) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().clickable { showControls = true })
            }
        }
        
        // Metadata below player respects White Mode/Light Theme (Requirement 9)
        if (!isPlayerFullScreen) {
            val favoriteChannels = allChannels.filter { favoritedList.contains(it.id) }
            val onChannelClickLambda = { ch: LiveChannel ->
                if (activeControlScreen == 1) {
                    viewModel.selectLiveChannel(ch)
                } else {
                    secondaryChannel = ch
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .background(appColors.background)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(channel.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text(channel.currentProgram, color = appColors.accent, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Share icon
                        IconButton(onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Watching ${channel.name} live on Swapnoflix! Stream URL: ${channel.streamUrl}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Channel"))
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = appColors.textPrimary
                            )
                        }

                        // Love/Favorite icon
                        val isFav = favoritedList.contains(channel.id)
                        IconButton(onClick = { viewModel.toggleFavoriteChannel(channel.id) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Color.Red else appColors.textPrimary
                            )
                        }
                    }
                }
                Text(channel.currentProgDescription, color = appColors.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                // Premium Control Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(appColors.cardBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentAspectIndex = (currentAspectIndex + 1) % aspectModes.size }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AspectRatio, null, tint = appColors.textPrimary, modifier = Modifier.size(20.dp))
                            Text("Aspect", fontSize = 9.sp, color = appColors.textSecondary)
                        }
                    }
                    IconButton(onClick = { enterPipMode(context) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PictureInPictureAlt, null, tint = appColors.textPrimary, modifier = Modifier.size(20.dp))
                            Text("PiP", fontSize = 9.sp, color = appColors.textSecondary)
                        }
                    }
                    IconButton(onClick = { 
                        isMultiScreen = !isMultiScreen
                        if (!isMultiScreen) activeControlScreen = 1
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Dashboard, null, tint = if (isMultiScreen) Color.Green else appColors.textPrimary, modifier = Modifier.size(20.dp))
                            Text("Split", fontSize = 9.sp, color = appColors.textSecondary)
                        }
                    }
                    IconButton(onClick = { viewModel.navigateToSettingsSubSection("CO_WATCH_ROOMS") }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Group, null, tint = appColors.textPrimary, modifier = Modifier.size(20.dp))
                            Text("CoWatch", fontSize = 9.sp, color = appColors.textSecondary)
                        }
                    }
                    IconButton(onClick = { showEpgGuideModal = true }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CalendarMonth, null, tint = appColors.textPrimary, modifier = Modifier.size(20.dp))
                            Text("Guide", fontSize = 9.sp, color = appColors.textSecondary)
                        }
                    }
                }

                if (isMultiScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(appColors.cardBg, RoundedCornerShape(12.dp))
                            .border(1.dp, appColors.border, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row 1: Control Screen Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Control Target:", color = appColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1, 2).forEach { num ->
                                    val isSelected = activeControlScreen == num
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) appColors.primary else appColors.cardBg.copy(alpha = 0.5f))
                                            .border(1.dp, if (isSelected) appColors.primary else appColors.border, RoundedCornerShape(8.dp))
                                            .clickable { activeControlScreen = num }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Screen $num", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Row 2: Audio Stream Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Audio Source:", color = appColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1, 2).forEach { num ->
                                    val isSelected = activeAudioSource == num
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF00FFC4).copy(alpha = 0.15f) else Color.Transparent)
                                            .border(1.dp, if (isSelected) Color(0xFF00FFC4) else appColors.border, RoundedCornerShape(8.dp))
                                            .clickable { activeAudioSource = num }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Screen $num", color = if (isSelected) Color(0xFF00FFC4) else appColors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Favorite Channels row below player (Requirement 5 & User adjustment)
                if (favoriteChannels.isNotEmpty()) {
                    CategoryChannelRow(
                        title = "Favorite Channels",
                        channels = favoriteChannels,
                        viewModel = viewModel,
                        favoritedList = favoritedList,
                        onChannelClick = onChannelClickLambda
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                CategoryChannelRow(
                    title = "More Channels",
                    channels = allChannels.filter { it.id != channel.id },
                    viewModel = viewModel,
                    favoritedList = favoritedList,
                    onChannelClick = onChannelClickLambda
                )
            }
        }
    }

    optionSheet?.let { sheet ->
        PlayerOptionDialog(
            sheet = sheet,
            selected = when (sheet) {
                PlayerOptionSheet.AUDIO -> playbackAudio
                PlayerOptionSheet.SUBTITLE -> playbackSubtitle
                else -> ""
            },
            onDismiss = { optionSheet = null },
            onSelect = { value ->
                when (sheet) {
                    PlayerOptionSheet.AUDIO -> viewModel.setAudio(value)
                    PlayerOptionSheet.SUBTITLE -> viewModel.setSubtitle(value)
                    else -> {}
                }
                optionSheet = null
            }
        )
    }

    if (showEpgGuideModal) {
        AlertDialog(
            onDismissRequest = { showEpgGuideModal = false },
            containerColor = Color(0xFF130F1A),
            title = {
                Text(
                    text = "EPG Broadcast Guide - ${channel.name}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val filteredProgs = epgPrograms.filter { it.channel_id == channel.id }
                    if (filteredProgs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No dynamic schedule broadcasts available.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        filteredProgs.sortedBy { it.start_time }.forEach { prog ->
                            val isReminded = epgReminders[prog.id] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E172E), RoundedCornerShape(10.dp))
                                    .border(1.dp, if (isReminded) Color(0xFF00FFC4) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prog.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = prog.description ?: "Live broadcast stream event",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    val timeStr = remember(prog.start_time, prog.end_time) {
                                        try {
                                            val startHour = prog.start_time.substringAfter('T').take(5)
                                            val endHour = prog.end_time.substringAfter('T').take(5)
                                            "$startHour - $endHour GMT"
                                        } catch (e: Exception) {
                                            "Scheduled Broadcast"
                                        }
                                    }
                                    Text(
                                        text = timeStr,
                                        color = Color(0xFF00FFC4),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                IconButton(
                                    onClick = {
                                        if (isReminded) {
                                            epgReminders[prog.id] = false
                                            Toast.makeText(context, "Reminder cancelled", Toast.LENGTH_SHORT).show()
                                        } else {
                                            epgReminders[prog.id] = true
                                            viewModel.createNotificationInSupabase(
                                                title = "EPG Program Alert: ${prog.title}",
                                                description = "${prog.title} starts soon on ${channel.name}!",
                                                type = "REMINDER"
                                            )
                                            Toast.makeText(context, "Reminder set for ${prog.title}!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isReminded) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                                        contentDescription = "Notify",
                                        tint = if (isReminded) Color(0xFF00FFC4) else Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEpgGuideModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close Guide", color = Color.White)
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF1F1B2C),
            title = {
                Text(
                    text = "Save Recording",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a custom name for your recording file:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = recordingNameInput,
                        onValueChange = { recordingNameInput = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FFC4),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFF00FFC4)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val recordIntent = Intent(context, com.example.data.LiveTvRecordingService::class.java).apply {
                            action = "STOP_RECORDING"
                            putExtra("CUSTOM_NAME", recordingNameInput.trim())
                        }
                        context.startService(recordIntent)
                        viewModel.setRecordingChannelId(null)
                        viewModel.logAuditActionToSupabase("DVR_STOP", "channels", channel.id, "Stopped and saved DVR recording as $recordingNameInput")
                        Toast.makeText(context, "Recording stopped and saved as $recordingNameInput", Toast.LENGTH_LONG).show()
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC4), contentColor = Color.Black)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

private enum class PlayerOptionSheet(
    val title: String,
    val options: List<String>
) {
    AUDIO(
        "Audio Track",
        listOf("English (Dolby 5.1)", "English Stereo", "Bangla Stereo", "Hindi Stereo")
    ),
    SUBTITLE(
        "Subtitles",
        listOf("Off", "English Outlined", "Bangla", "Hindi")
    )
}

@Composable
private fun PlayerOptionDialog(
    sheet: PlayerOptionSheet,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val appColors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(sheet.title, fontWeight = FontWeight.Bold, color = appColors.textPrimary) },
        containerColor = appColors.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sheet.options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(option) }
                            .background(if (option == selected) appColors.primary.copy(alpha = 0.18f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(option, fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal, color = appColors.textPrimary)
                        if (option == selected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = appColors.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = appColors.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CategoryChannelRow(
    title: String,
    channels: List<LiveChannel>,
    viewModel: StreamingViewModel,
    favoritedList: Set<String>,
    onChannelClick: ((LiveChannel) -> Unit)? = null
) {
    if (channels.isEmpty()) return
    val appColors = LocalAppColors.current
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val context = LocalContext.current
            channels.forEach { ch ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(88.dp)
                        .clickable {
                            if (onChannelClick != null) {
                                onChannelClick(ch)
                            } else {
                                viewModel.selectLiveChannel(ch, context)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ch.logoAccent.copy(alpha = 0.15f))
                            .border(BorderStroke(2.dp, ch.logoAccent), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (ch.logoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = ch.logoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                ch.name.take(2).uppercase(),
                                color = ch.logoAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        
                        // Live badge overlay if active/playing
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 4.dp)
                                .background(Color.Red, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("LIVE", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        ch.name,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelDetailsBottomSheet(
    channel: LiveChannel,
    onDismiss: () -> Unit,
    onLaunchPlayer: () -> Unit,
    viewModel: StreamingViewModel
) {
    val appColors = LocalAppColors.current
    val favoritedList by viewModel.favoriteChannels.collectAsState()
    val isFav = favoritedList.contains(channel.id)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            colors = CardDefaults.cardColors(containerColor = appColors.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp).clickable(enabled = false) {}) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(channel.logoAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (channel.logoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = channel.logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Text(
                                    channel.name.take(2).uppercase(),
                                    color = channel.logoAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(channel.name, fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 18.sp)
                            Text(channel.category, color = appColors.textMuted, fontSize = 12.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { viewModel.toggleFavoriteChannel(channel.id) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Color.Red else appColors.textPrimary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, null, tint = appColors.textPrimary)
                        }
                    }
                }
                Text(
                    "CURRENT PROGRAM",
                    fontWeight = FontWeight.Bold,
                    color = appColors.primary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(channel.currentProgram, fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 15.sp)
                Text(channel.currentProgDescription, color = appColors.textMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                Button(
                    onClick = onLaunchPlayer,
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .height(50.dp)
                ) {
                    Text("Watch Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun MiniPlayerOverlay(viewModel: StreamingViewModel) {
    val activeChannel by viewModel.activeLiveChannel.collectAsState()
    val isMiniOn by viewModel.isMiniPlayerActive.collectAsState()
    val appColors = LocalAppColors.current

    if (isMiniOn) {
        activeChannel?.let { ch ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { 
                        viewModel.toggleMiniPlayer(false)
                        viewModel.selectTab(MainTab.LIVE_TV) 
                    },
                colors = CardDefaults.cardColors(containerColor = appColors.cardBgSecondary),
                border = BorderStroke(1.dp, appColors.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ch.logoAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Tv, null, tint = ch.logoAccent, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(ch.name, fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 11.sp)
                            Text(ch.currentProgram, color = appColors.textMuted, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                    Row {
                        IconButton(onClick = { viewModel.toggleMiniPlayer(false) }) {
                            Icon(Icons.Filled.Close, null, tint = appColors.textPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerChannelSkeleton() {
    val appColors = LocalAppColors.current
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(appColors.cardBg, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(44.dp).background(appColors.border, RoundedCornerShape(8.dp)))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Box(modifier = Modifier.width(100.dp).height(14.dp).background(appColors.border))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(150.dp).height(10.dp).background(appColors.border))
                }
            }
        }
    }
}

private fun formatLiveTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

class CustomVideoPlayerView(context: android.content.Context) : android.widget.FrameLayout(context) {
    val surfaceView = android.view.SurfaceView(context)
    
    private val listener = object : androidx.media3.common.Player.Listener {
        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            requestLayout()
        }
    }
    
    init {
        addView(surfaceView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
    
    var resizeMode: Int = 0 // 0 = Fit, 3 = Zoom, 4 = Stretch/Fill
        set(value) {
            field = value
            requestLayout()
        }
        
    var player: androidx.media3.exoplayer.ExoPlayer? = null
        set(value) {
            field?.setVideoSurfaceView(null)
            field?.removeListener(listener)
            field = value
            value?.addListener(listener)
            value?.setVideoSurfaceView(surfaceView)
        }
        
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player?.setVideoSurfaceView(null)
        player?.removeListener(listener)
    }
        
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val playerInstance = player ?: return
        val videoSize = playerInstance.videoSize
        if (videoSize.width <= 0 || videoSize.height <= 0) return
        
        val videoWidth = videoSize.width
        val videoHeight = videoSize.height
        val videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
        
        val measuredWidth = measuredWidth
        val measuredHeight = measuredHeight
        val containerAspectRatio = measuredWidth.toFloat() / measuredHeight.toFloat()
        
        var targetWidth = measuredWidth
        var targetHeight = measuredHeight
        
        when (resizeMode) {
            0 -> { // FIT
                if (containerAspectRatio > videoAspectRatio) {
                    targetWidth = (measuredHeight * videoAspectRatio).toInt()
                } else {
                    targetHeight = (measuredWidth / videoAspectRatio).toInt()
                }
            }
            3 -> { // ZOOM
                if (containerAspectRatio > videoAspectRatio) {
                    targetHeight = (measuredWidth / videoAspectRatio).toInt()
                } else {
                    targetWidth = (measuredHeight * videoAspectRatio).toInt()
                }
            }
        }
        
        surfaceView.measure(
            MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY)
        )
    }
    
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        
        val childWidth = surfaceView.measuredWidth
        val childHeight = surfaceView.measuredHeight
        
        val childLeft = (width - childWidth) / 2
        val childTop = (height - childHeight) / 2
        
        surfaceView.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
    }
}


package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.LocalAppColors

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: StreamingViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Pre-create WebView Code Cache directories to silence Chromium opendir log errors
    try {
      val jsCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
      if (!jsCacheDir.exists()) {
        jsCacheDir.mkdirs()
      }
      val wasmCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
      if (!wasmCacheDir.exists()) {
        wasmCacheDir.mkdirs()
      }
    } catch (e: Exception) {
      // Safe fallback
    }

    FirebaseManager.initialize(applicationContext)
    
    viewModel = androidx.lifecycle.ViewModelProvider(this)[StreamingViewModel::class.java]
    handleIntent(intent)

    setContent {
      val isDarkMode by viewModel.isDarkMode.collectAsState()
      val currentDest by viewModel.currentDestination.collectAsState()

      MyApplicationTheme(darkTheme = isDarkMode) {
        val appColors = LocalAppColors.current

        Surface(
          modifier = Modifier.fillMaxSize().testTag("app_root"),
          color = appColors.background
        ) {
          AnimatedContent(
            targetState = currentDest,
            transitionSpec = {
              if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally { width -> width / 2 } + fadeIn()).togetherWith(
                  slideOutHorizontally { width -> -width / 2 } + fadeOut()
                )
              } else {
                (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(
                  slideOutHorizontally { width -> width / 2 } + fadeOut()
                )
              }
            },
            label = "dest_switcher_main"
          ) { dest ->
            when (dest) {
              AppDestination.SPLASH -> SplashScreen(viewModel)
              AppDestination.WELCOME -> WelcomeScreen(viewModel)
              AppDestination.LOGIN -> LoginScreen(viewModel)
              AppDestination.REGISTER -> RegisterScreen(viewModel)
              AppDestination.OTP_VERIFY -> OtpVerifyScreen(viewModel)
              AppDestination.PROFILE_SELECT -> ProfileSelectScreen(viewModel)
              AppDestination.CREATE_PROFILE -> CreateProfileScreen(viewModel)
              AppDestination.KIDS_MODE -> KidsModeShell(viewModel)
              else -> MainHubContainer(viewModel)
            }
          }
        }
      }
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    if (::viewModel.isInitialized) {
      val currentVideo = viewModel.playTubeCurrentVideo.value
      val currentTab = viewModel.currentTab.value
      // Enter picture in picture if user is currently watching a PlayTube video
      if (currentVideo != null && viewModel.playTubeIsPlaying.value && currentTab == MainTab.DISCOVER) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          val params = android.app.PictureInPictureParams.Builder()
            .setAspectRatio(android.util.Rational(16, 9))
            .build()
          enterPictureInPictureMode(params)
        }
      }
    }
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    if (::viewModel.isInitialized) {
      viewModel.setPlayTubePipMode(isInPictureInPictureMode)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    intent?.getStringExtra("PLAY_TUBE_VIDEO_ID")?.let { videoId ->
      if (videoId.isNotEmpty()) {
        viewModel.selectTab(MainTab.DISCOVER)
        val video = viewModel.youtubeHomeVideos.value.find { it.id == videoId }
          ?: viewModel.youtubeSearchResults.value.find { it.id == videoId }
          ?: PlayTubeVideo(
              id = videoId,
              title = "Playing from notification",
              channelName = "YouTube",
              channelId = "",
              description = "",
              viewCount = "LIVE",
              duration = "LIVE",
              uploadTime = "Now",
              videoUrl = "https://www.youtube.com/watch?v=$videoId",
              category = "Music"
          )
        viewModel.selectPlayTubeVideo(video)
        viewModel.togglePlayTubeTab("Home")
        viewModel.selectTab(MainTab.DISCOVER)
      }
    }
  }
}

@Composable
fun MainHubContainer(viewModel: StreamingViewModel) {
  val selectedTab by viewModel.currentTab.collectAsState()
  var settingsSubSection by remember { mutableStateOf("DEFAULT") }
  val settingsOverride by viewModel.settingsSubSectionOverride.collectAsState()
  LaunchedEffect(settingsOverride) {
    settingsOverride?.let {
      settingsSubSection = it
      viewModel.clearSettingsSubSectionOverride()
    }
  }
  val appColors = LocalAppColors.current

  val isPipState by viewModel.playTubeIsPipMode.collectAsState()
  val isLiveTvFullScreen by viewModel.liveTvIsFullScreen.collectAsState()
  val activeLiveChannel by viewModel.activeLiveChannel.collectAsState()

  // Detect landscape orientation
  val configuration = androidx.compose.ui.platform.LocalConfiguration.current
  val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
  val hideSystemBarsInLandscape = isLandscape && selectedTab == MainTab.LIVE_TV
  val shouldHideSystemBars = hideSystemBarsInLandscape || (selectedTab == MainTab.LIVE_TV && isLiveTvFullScreen)

  // Global search input toggles
  var isSearchingGlobally by remember { mutableStateOf(false) }
  val isLiveTVSearchActive by viewModel.isLiveTVSearchActive.collectAsState()
  val liveTVSearchQuery by viewModel.liveTVSearchQuery.collectAsState()
  val isPlayTubeSearchActive by viewModel.isYoutubeSearchActive.collectAsState()
  val playTubeSearchQuery by viewModel.playTubeSearchQuery.collectAsState()

  Scaffold(
    topBar = {
      if (!shouldHideSystemBars && selectedTab != MainTab.PROFILE_SETTINGS && selectedTab != MainTab.HOME && !(selectedTab == MainTab.LIVE_TV && activeLiveChannel != null)) {
        if (selectedTab == MainTab.LIVE_TV && isLiveTVSearchActive) {
          Row(
            modifier = Modifier
              .statusBarsPadding()
              .fillMaxWidth()
              .background(appColors.surface)
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(onClick = { viewModel.setLiveTVSearchActive(false) }) {
              Icon(Icons.Filled.ArrowBack, contentDescription = "Exit Search", tint = appColors.textPrimary)
            }
            OutlinedTextField(
              value = liveTVSearchQuery,
              onValueChange = { viewModel.updateLiveTVSearchQuery(it) },
              placeholder = { Text("Search channels by name...", color = appColors.textMuted, fontSize = 14.sp) },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = appColors.textMuted) },
              trailingIcon = {
                if (liveTVSearchQuery.isNotEmpty()) {
                  IconButton(onClick = { viewModel.updateLiveTVSearchQuery("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear text", tint = appColors.textPrimary)
                  }
                }
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = appColors.textPrimary,
                unfocusedTextColor = appColors.textPrimary,
                focusedBorderColor = appColors.primary,
                unfocusedBorderColor = appColors.border,
                cursorColor = appColors.primary
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("livetv_search_input"),
              shape = RoundedCornerShape(12.dp),
              singleLine = true
            )
          }
        } else if (selectedTab == MainTab.DISCOVER && isPlayTubeSearchActive) {
          Row(
            modifier = Modifier
              .statusBarsPadding()
              .fillMaxWidth()
              .background(appColors.surface)
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(onClick = { viewModel.setYoutubeSearchActive(false) }) {
              Icon(Icons.Filled.ArrowBack, contentDescription = "Exit Search", tint = appColors.textPrimary)
            }
            OutlinedTextField(
              value = playTubeSearchQuery,
              onValueChange = { viewModel.updatePlayTubeSearchQuery(it) },
              placeholder = { Text("Search YouTube...", color = appColors.textMuted, fontSize = 14.sp) },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = appColors.textMuted) },
              trailingIcon = {
                if (playTubeSearchQuery.isNotEmpty()) {
                  IconButton(onClick = { viewModel.updatePlayTubeSearchQuery("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear text", tint = appColors.textPrimary)
                  }
                }
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = appColors.textPrimary,
                unfocusedTextColor = appColors.textPrimary,
                focusedBorderColor = appColors.primary,
                unfocusedBorderColor = appColors.border,
                cursorColor = appColors.primary
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("playtube_search_input"),
              shape = RoundedCornerShape(12.dp),
              singleLine = true
            )
          }
        } else {
          Row(
            modifier = Modifier
              .statusBarsPadding()
              .fillMaxWidth()
              .background(appColors.surface)
              .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (selectedTab == MainTab.LIVE_TV) {
                Text(
                  text = androidx.compose.ui.text.buildAnnotatedString {
                    append("Swapno")
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFE50914)))
                    append("flix")
                    pop()
                    append(" Live TV")
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp,
                  color = appColors.textPrimary
                )
              } else if (selectedTab == MainTab.DISCOVER) {
                Text(
                  text = androidx.compose.ui.text.buildAnnotatedString {
                    append("Swapno")
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFE50914)))
                    append("flix")
                    pop()
                    append(" PlayTube")
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp,
                  color = appColors.textPrimary
                )
              } else {
                Text(
                  text = androidx.compose.ui.text.buildAnnotatedString {
                    append("Swapno")
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFE50914)))
                    append("flix")
                    pop()
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp,
                  color = appColors.textPrimary
                )
              }
            }
 
            Row {
              if (selectedTab == MainTab.LIVE_TV) {
                IconButton(onClick = { viewModel.setLiveTVSearchActive(true) }) {
                  Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search Channels",
                    tint = appColors.textPrimary
                  )
                }
              } else if (selectedTab == MainTab.DISCOVER) {
                IconButton(onClick = { viewModel.setYoutubeSearchActive(true) }) {
                  Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search YouTube",
                    tint = appColors.textPrimary
                  )
                }
              } else {
                IconButton(onClick = { isSearchingGlobally = !isSearchingGlobally }) {
                  Icon(
                    imageVector = if (isSearchingGlobally) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = if (isSearchingGlobally) appColors.accent else appColors.textPrimary
                  )
                }
              }
 
              // Notification heart (Screen 27)
              IconButton(onClick = { viewModel.navigateToSettingsSubSection("NOTIFICATIONS") }) {
               Icon(Icons.Filled.NotificationsNone, contentDescription = "Alerts", tint = appColors.textPrimary)
              }
            }
          }
        }
      }
    },
    bottomBar = {
      if (!shouldHideSystemBars) {
        // Bottom Navigation bar
        NavigationBar(
          containerColor = appColors.surface,
          contentColor = appColors.textPrimary,
          modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
          listOf(
            Triple(MainTab.HOME, "Home", Icons.Filled.Home),
            Triple(MainTab.LIVE_TV, "Live TV", Icons.Filled.LiveTv),
            Triple(MainTab.DISCOVER, "PlayTube", Icons.Filled.Subscriptions),
            Triple(MainTab.CATCH_UP, "Learn", Icons.Filled.School),
            Triple(MainTab.PROFILE_SETTINGS, "Profile", Icons.Filled.Person)
          ).forEach { (tab, label, icon) ->
            val isSel = selectedTab == tab
            NavigationBarItem(
              selected = isSel,
              onClick = {
                if (tab == MainTab.LIVE_TV) {
                  viewModel.selectLiveChannel(null)
                }
                if (tab != MainTab.DISCOVER) {
                  viewModel.setYoutubeSearchActive(false)
                }
                viewModel.selectTab(tab)
                isSearchingGlobally = false
                settingsSubSection = "DEFAULT"
              },
              icon = { Icon(icon, contentDescription = label) },
              label = { Text(label, fontSize = 11.sp) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = appColors.primary,
                unselectedIconColor = appColors.textMuted,
                selectedTextColor = appColors.primary,
                unselectedTextColor = appColors.textMuted,
                indicatorColor = appColors.surfaceVariant
              )
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(if (shouldHideSystemBars) PaddingValues(0.dp) else innerPadding)
        .background(appColors.background)
    ) {
      if (isSearchingGlobally) {
        SearchScreen(viewModel)
      } else {
        AnimatedContent(
          targetState = selectedTab,
          label = "hub_tabs_animation"
        ) { tabSpec ->
          when (tabSpec) {
            MainTab.HOME -> HomeScreen(viewModel)
            MainTab.LIVE_TV -> LiveTVHomeScreen(viewModel)
            MainTab.DISCOVER -> PlayTubeHomeScreen(viewModel)
            MainTab.CATCH_UP -> LearnScreen(viewModel)
            MainTab.PROFILE_SETTINGS -> {
              if (settingsSubSection == "DEFAULT") {
                UserProfileScreen(
                  viewModel = viewModel,
                  onSelectSection = { settingsSubSection = it }
                )
              } else {
                Box(modifier = Modifier.fillMaxSize()) {
                  Column {
                    // Custom Internal Header for back transitions
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .background(appColors.surface)
                        .padding(16.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      IconButton(onClick = { settingsSubSection = "DEFAULT" }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to Profile", tint = appColors.textPrimary)
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = settingsSubSection.replace("_", " "),
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        fontSize = 16.sp
                      )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                      when (settingsSubSection) {
                        "NOTIFICATIONS" -> NotificationsHubScreen(viewModel)
                        "BILLING_CENTER" -> UnifiedBillingCenterScreen(viewModel)
                        "SYSTEM_SETTINGS" -> SystemSettingsScreen(viewModel)
                        "SUPPORT_CENTER" -> UnifiedHelpSupportDeskScreen(viewModel)
                        "REWARDS_REFERRAL" -> RewardsReferralScreen(viewModel)
                        "CO_WATCH_ROOMS" -> CoWatchRoomScreen(viewModel)
                        else -> Box(modifier = Modifier.fillMaxSize())
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Compact floating Mini Player (Screen 14) overlays when active
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(bottom = 12.dp, end = 12.dp)
      ) {
        MiniPlayerOverlay(viewModel)
      }

      // Root-level draggable, resizable PlayTube PiP floating player
      PlayTubeFloatingPopupPlayer(viewModel)
    }
  }
}

@Composable
fun GeneralSettingsMenu(
  viewModel: StreamingViewModel,
  onSelectSection: (String) -> Unit
) {
  val activeProfile by viewModel.selectedProfile.collectAsState()
  val activePlan by viewModel.userActivePlan.collectAsState()
  val appColors = LocalAppColors.current

  Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Account & Streaming Options", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))

    // List of menu selections linking our 44 screens!
    listOf(
      Triple("ACCOUNT_SETTINGS", "Manage Secure Account Credentials", Icons.Filled.ManageAccounts),
      Triple("SUBSCRIPTION_DASHBOARD", "Subscription Tracking Dashboard", Icons.Filled.Dashboard),
      Triple("MOVEBOX_INTEGRATION", "Movebox VOD API Gateway", Icons.Filled.CloudSync),
      Triple("SUBSCRIPTION_PLANS", "Configure Subscription Packages", Icons.Filled.CreditCard),
      Triple("SUBSCRIPTION_HISTORY", "Receipt Invoice Transcripts", Icons.Filled.ReceiptLong),
      Triple("NOTIFICATIONS", "Notification Reminders Hub", Icons.Filled.NotificationsActive),
      Triple("LIVE_CHAT", "Help Desk - AI Live Chat assistant", Icons.Filled.SupportAgent),
      Triple("SUPPORT_TICKETS", "Report Anomalies - Support Tickets logs", Icons.Filled.BugReport),
      Triple("FAQ", "Frequently Asked Help FAQs", Icons.Filled.HelpCenter),
      Triple("REWARDS_REFERRAL", "Rewards Scorecards & Promo Referrals", Icons.Filled.Stars),
      Triple("CO_WATCH_ROOMS", "Co-Watch & Joint Study Rooms", Icons.Filled.Group),
      Triple("SYSTEM_SETTINGS", "System settings (Themes / Cleaner / Sessions)", Icons.Filled.Settings)
    ).forEach { (section, label, icon) ->
      Card(
        colors = CardDefaults.cardColors(containerColor = appColors.cardBgSecondary),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectSection(section) },
        shape = RoundedCornerShape(8.dp)
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = appColors.accent)
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = appColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }
          Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = appColors.textMuted.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

@Composable
fun KidsModeShell(viewModel: StreamingViewModel) {
  val appColors = LocalAppColors.current
  Box(
    modifier = Modifier
        .fillMaxSize()
        .background(appColors.background)
  ) {
    Column {
      KidsModeScreen(viewModel)
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

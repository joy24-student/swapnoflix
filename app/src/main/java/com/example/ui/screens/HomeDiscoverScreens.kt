package com.example.ui.screens

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import coil.compose.AsyncImage
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Velocity
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.theme.LocalAppColors

@Composable
fun Modifier.bounceClick(onClick: () -> Unit): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick() }
            )
        }
}

@Composable
fun HomeScreen(viewModel: StreamingViewModel) {
    val activeProfile by viewModel.selectedProfile.collectAsState()
    val allChannels = StreamingDatabase.channels
    val allMovies by viewModel.moviesList.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val appColors = LocalAppColors.current
    val statusBarHeight = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "বাংলা"

    val prefs = remember { context.getSharedPreferences("swapnoflix_prefs", android.content.Context.MODE_PRIVATE) }

    // Automatic feedback modal trigger states
    var showFeedbackModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        var nextShowTime = prefs.getLong("feedback_next_show_time", -1L)
        
        // If it's the first time ever, we set the initial timer of 3 days (Request 4)
        if (nextShowTime == -1L) {
            nextShowTime = now + 3L * 24L * 60L * 60L * 1000L // 3 days
            prefs.edit().putLong("feedback_next_show_time", nextShowTime).apply()
        }
        
        if (now >= nextShowTime) {
            delay(12000) // Trigger automatically after 12 seconds of use on the home page!
            showFeedbackModal = true
        }
    }

    // Supabase active notice popups (notice_popups table)
    val activeNotices by viewModel.activeNoticePopups.collectAsState()
    var currentNoticeIndex by remember { mutableStateOf(0) }
    
    if (activeNotices.isNotEmpty() && currentNoticeIndex < activeNotices.size) {
        val notice = activeNotices[currentNoticeIndex]
        AlertDialog(
            onDismissRequest = { currentNoticeIndex++ },
            title = {
                Text(notice.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!notice.image_url.isNullOrBlank()) {
                        AsyncImage(
                            model = notice.image_url,
                            contentDescription = "Notice Image",
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(notice.body, color = Color.LightGray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!notice.target_url.isNullOrBlank()) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(notice.target_url))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                        currentNoticeIndex++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("Learn More", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { currentNoticeIndex++ }) {
                    Text("Dismiss", color = Color.Gray)
                }
            },
            containerColor = appColors.cardBg
        )
    }

    // Supabase App OTA Updates (app_updates & app_update_downloads tables)
    val supabaseUpdate by viewModel.latestAppUpdate.collectAsState()
    var dismissedSupabaseUpdate by remember { mutableStateOf(false) }
    
    if (supabaseUpdate != null && !dismissedSupabaseUpdate) {
        val update = supabaseUpdate!!
        AlertDialog(
            onDismissRequest = {
                if (!update.is_mandatory) {
                    dismissedSupabaseUpdate = true
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = appColors.accent)
                    Text("New Update: v${update.version_name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(update.description ?: "A new stable build is available with bug fixes and channel optimizations.", color = Color.LightGray, fontSize = 12.sp)
                    if (update.is_mandatory) {
                        Text("This update is mandatory to continue using the application.", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logAppUpdateDownloadInSupabase(update.id)
                        if (!update.apk_url.isNullOrBlank()) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(update.apk_url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Cannot open download URL", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Starting download...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        if (!update.is_mandatory) {
                            dismissedSupabaseUpdate = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("Download Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!update.is_mandatory) {
                    TextButton(onClick = { dismissedSupabaseUpdate = true }) {
                        Text("Later", color = Color.Gray)
                    }
                }
            },
            containerColor = appColors.cardBg
        )
    }

    // Firebase update info collector
    val updateInfo by FirebaseManager.latestUpdate.collectAsState()

    if (updateInfo != null) {
        val update = updateInfo!!
        AlertDialog(
            onDismissRequest = {
                if (!update.forceUpdate) {
                    FirebaseManager.dismissUpdate()
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = "Update available",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "New Update Available! 🚀",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Version: v${update.version}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }
                    
                    Text(
                        text = update.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Package server: Cloudflare R2 secure mirror (~15.4 MB)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(update.downloadUrl))
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Downloading update package from secure Cloudflare R2 server...", android.widget.Toast.LENGTH_LONG).show()
                        }
                        FirebaseManager.dismissUpdate()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download & Install", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!update.forceUpdate) {
                    TextButton(
                        onClick = { FirebaseManager.dismissUpdate() }
                    ) {
                        Text("Remind Me Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    val scope = rememberCoroutineScope()
    var pullDistance by remember { mutableStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1200)
        isInitialLoading = false
    }
    var refreshProgressPage by remember { mutableStateOf(1) }

    var showStickyChips by remember { mutableStateOf(false) }
    var headerBottomY by remember { mutableStateOf(0f) }

    // Scroll state for gestures driving the collapsing header animations
    val scrollState = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val collapseRangePx = with(density) { 56.dp.toPx() }
    val collapseFraction = (scrollState.value.toFloat() / collapseRangePx).coerceIn(0f, 1f)

    var selectedCategory by remember { mutableStateOf("Featured") }
    var searchQuery by remember { mutableStateOf("") }
    var showNotificationsOverlay by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && pullDistance > 0f) {
                    val consumed = available.y
                    pullDistance = (pullDistance + consumed).coerceAtLeast(0f)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0 && scrollState.value == 0) {
                    val added = available.y * 0.5f // apply elastic resistance
                    pullDistance = (pullDistance + added).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (pullDistance > 180f) {
                    isRefreshing = true
                    isInitialLoading = true
                    scope.launch {
                        for (i in 1..5) {
                            delay(450)
                            refreshProgressPage = i
                        }
                        isRefreshing = false
                        isInitialLoading = false
                        androidx.compose.animation.core.Animatable(pullDistance).animateTo(
                            targetValue = 0f,
                            animationSpec = androidx.compose.animation.core.tween(400)
                        ) {
                            pullDistance = this.value
                        }
                    }
                } else if (pullDistance > 0f) {
                    scope.launch {
                        androidx.compose.animation.core.Animatable(pullDistance).animateTo(
                            targetValue = 0f,
                            animationSpec = androidx.compose.animation.core.tween(300)
                        ) {
                            pullDistance = this.value
                        }
                    }
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(appColors.background)) {
        // Beautiful revealed pull-to-refresh colorful panel with rotating World/Globe icon
        if (pullDistance > 0f) {
            val infiniteTransition = rememberInfiniteTransition(label = "world_pulse_spin")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "globe_rotation"
            )
            val colorAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "color_hue"
            )
            val animatedRainbowColor = remember(colorAngle) {
                Color.hsv(hue = colorAngle, saturation = 0.85f, value = 0.95f)
            }
            val calculatedPage = (1 + (pullDistance / 80f).toInt()).coerceAtMost(10)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { pullDistance.toDp() })
                    .background(Color(0xFF0C0C0E))
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(animatedRainbowColor.copy(alpha = 0.2f), Color.Transparent),
                                radius = size.width * 0.4f
                            ),
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = "Global Sync",
                        tint = animatedRainbowColor,
                        modifier = Modifier
                            .size(46.dp)
                            .graphicsLayer {
                                rotationZ = rotationAngle
                                val s = (pullDistance / 180f).coerceIn(0.6f, 1.3f)
                                scaleX = s
                                scaleY = s
                            }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRefreshing) "SYNCING CONTENT..." else "PULL DOWN TO REFRESH",
                        color = animatedRainbowColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "EXPLORING PAGE: ${if (isRefreshing) refreshProgressPage else calculatedPage}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (pullDistance / 350f).coerceIn(0f, 1f))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(animatedRainbowColor, Color.White)
                                    )
                                )
                        )
                    }
                }
            }
        }

        // Main Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.changedToUp() }) {
                                if (!isRefreshing && pullDistance > 0f) {
                                    scope.launch {
                                        androidx.compose.animation.core.Animatable(pullDistance).animateTo(
                                            targetValue = 0f,
                                            animationSpec = androidx.compose.animation.core.tween(300)
                                        ) {
                                            pullDistance = this.value
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .graphicsLayer {
                    translationY = pullDistance
                }
                .verticalScroll(scrollState)
                .padding(bottom = 90.dp)
        ) {
            // Spacer to keep elements from being covered by the sticky collapsible header
            Spacer(modifier = Modifier.height(116.dp + statusBarHeight + 12.dp))
            val shimmerBrush = rememberShimmerBrush()

            if (selectedCategory == "Featured") {
                if (isInitialLoading) {
                    ShimmerHeroBanner(shimmerBrush)

                    Spacer(modifier = Modifier.height(20.dp))

                    LiveSocialCommunityBar(viewModel)

                    Spacer(modifier = Modifier.height(20.dp))

                    ShimmerLiveChannelsRow(shimmerBrush)

                    Spacer(modifier = Modifier.height(20.dp))

                    ShimmerContinueWatchingRow(shimmerBrush)

                    Spacer(modifier = Modifier.height(20.dp))

                    CategoryChipsRow(
                        selectedCategory = selectedCategory,
                        onCategorySelected = {
                            selectedCategory = it
                            searchQuery = ""
                        },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val chipsTopY = coords.positionInRoot().y
                            showStickyChips = chipsTopY <= headerBottomY
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ShimmerMovieRowSection(shimmerBrush, "New Releases")

                    Spacer(modifier = Modifier.height(20.dp))

                    ShimmerMovieRowSection(shimmerBrush, "Top Rated")
                } else {
                    // OPPENHEIMER cinematic interactive Hero Banner
                    HeroMovieBanner(allMovies, viewModel)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Live Social Community Bar
                    LiveSocialCommunityBar(viewModel)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Live Channels Carousel
                    LiveChannelsRow(allChannels, viewModel)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Continue Watching
                    ContinueWatchingRow(viewModel, allChannels)

                    // Dynamic Ad Banners from Supabase (ads_banners & ad_impressions)
                    val adBanners by viewModel.adBanners.collectAsState()
                    if (adBanners.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Special Promotions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = appColors.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().height(96.dp)
                        ) {
                            items(adBanners) { ad ->
                                LaunchedEffect(ad.id) {
                                    viewModel.logAdImpressionInSupabase(ad.id, clicked = false)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(appColors.cardBgSecondary)
                                        .clickable {
                                            viewModel.logAdImpressionInSupabase(ad.id, clicked = true)
                                            if (!ad.target_url.isNullOrBlank()) {
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(ad.target_url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {}
                                            }
                                        }
                                ) {
                                    AsyncImage(
                                        model = ad.image_url,
                                        contentDescription = ad.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = ad.title,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Category Chips Row placed BEFORE New Releases Section
                    CategoryChipsRow(
                        selectedCategory = selectedCategory,
                        onCategorySelected = {
                            selectedCategory = it
                            searchQuery = "" // Reset search when switching categories
                        },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val chipsTopY = coords.positionInRoot().y
                            showStickyChips = chipsTopY <= headerBottomY
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // New Releases Row
                    MovieRowSection(
                        title = "New Releases",
                        movies = allMovies.filter { it.releaseYear >= 2022 && it.id != "mov_oppenheimer" },
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Top Rated Row
                    MovieRowSection(
                        title = "Top Rated",
                        movies = allMovies.sortedByDescending { it.rating },
                        viewModel = viewModel
                    )
                }
            } else {
                CategoryChipsRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = {
                        selectedCategory = it
                        searchQuery = "" // Reset search when switching categories
                    },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val chipsTopY = coords.positionInRoot().y
                        showStickyChips = chipsTopY <= headerBottomY
                    }
                )

                if (isInitialLoading) {
                    ShimmerFilteredMoviesView(shimmerBrush, selectedCategory)
                } else {
                    // Filtered Movies grid based on category and local search
                    val filteredList = allMovies.filter { movie ->
                        val matchesCategory = if (selectedCategory == "All Movies") true else movie.category.contains(selectedCategory, ignoreCase = true)
                        val matchesQuery = movie.title.contains(searchQuery, ignoreCase = true) || movie.synopsis.contains(searchQuery, ignoreCase = true)
                        matchesCategory && matchesQuery
                    }
                    
                    FilteredMoviesView(
                        category = selectedCategory,
                        movies = filteredList,
                        viewModel = viewModel
                    )
                }
            }
        }

        // Animated Sticky/Collapsible Header (Overlays at the top)
        StickyAnimatedHeader(
            collapseFraction = collapseFraction,
            showStickyChips = showStickyChips,
            searchQuery = searchQuery,
            onSearchQueryChange = { query ->
                searchQuery = query
                if (query.isNotEmpty() && selectedCategory == "Featured") {
                    selectedCategory = "All Movies"
                }
            },
            onNotificationClick = { viewModel.navigateToSettingsSubSection("NOTIFICATIONS") },
            viewModel = viewModel,
            modifier = Modifier
                .graphicsLayer {
                    translationY = pullDistance
                }
                .onGloballyPositioned { coords ->
                    headerBottomY = coords.positionInRoot().y + coords.size.height
                }
        )
        // Overlay sticky CategoryChipsRow copy when scrolled past
        if (showStickyChips) {
            val currentHeaderHeight = 116.dp + statusBarHeight - (56.dp * collapseFraction)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = currentHeaderHeight)
                    .graphicsLayer {
                        translationY = pullDistance
                    }
                    .background(appColors.background)
            ) {
                CategoryChipsRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = {
                        selectedCategory = it
                        searchQuery = ""
                    }
                )
            }
        }

        // Notification Dialog Dropdown
        val notificationsList by viewModel.notifications.collectAsState()
        if (showNotificationsOverlay) {
            NotificationsOverlay(
                notifications = notificationsList,
                onDismiss = { showNotificationsOverlay = false }
            )
        }

        // Automatic Services feedback modal (Request 4)
        if (showFeedbackModal) {
            FeedbackModal(
                onDismiss = {
                    showFeedbackModal = false
                    // "if user click leter it will pop up after one days"
                    val nextTime = System.currentTimeMillis() + 1L * 24L * 60L * 60L * 1000L // 1 day
                    prefs.edit().putLong("feedback_next_show_time", nextTime).apply()
                },
                onSubmit = { rating, comment, tags ->
                    showFeedbackModal = false
                    // "if once submitted it will pop up after 30 days"
                    val nextTime = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L // 30 days
                    prefs.edit().putLong("feedback_next_show_time", nextTime).apply()
                    viewModel.createFeedbackInSupabase(
                        kind = "app",
                        rating = rating,
                        title = "Service Feedback: $rating Stars",
                        message = "Selected feedback tags: ${tags.joinToString(", ")}. User review comment: $comment"
                    )
                    android.widget.Toast.makeText(context, "Thank you for rating us $rating stars! Feedback saved. ❤️", android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

@Composable
fun FeedbackModal(
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String, tags: List<String>) -> Unit
) {
    val appColors = LocalAppColors.current
    var selectedRating by remember { mutableStateOf(0) }
    var commentText by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    
    val tagsList = listOf("Fast Streaming", "Great UI", "Rich Movies", "Helpful Support", "No Buffering")

    GestureSlideUpModal(
        visible = true,
        onDismissRequest = onDismiss,
        title = "Enjoying Swapnoflix?"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Help us improve our service by rating your experience! Drag down this modal to close it.",
                fontSize = 13.sp,
                color = appColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )

            // Interactive star rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    val isStarred = star <= selectedRating
                    Icon(
                        imageVector = if (isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Star $star",
                        tint = if (isStarred) Color(0xFFE9C400) else appColors.textMuted,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { selectedRating = star }
                            .padding(2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "What did you like the most?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = appColors.textPrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tagsList.forEach { tag ->
                        val isSelected = selectedTags.contains(tag)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) appColors.accent.copy(alpha = 0.2f) else appColors.cardBgSecondary.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) appColors.accent else appColors.border,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) appColors.accent else appColors.textSecondary
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Write your thoughts (optional)...", color = appColors.textMuted, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    focusedBorderColor = appColors.primary,
                    unfocusedBorderColor = appColors.border,
                    focusedContainerColor = appColors.cardBgSecondary.copy(alpha = 0.3f),
                    unfocusedContainerColor = appColors.cardBgSecondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth().height(90.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Later", color = appColors.textMuted, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onSubmit(selectedRating, commentText, selectedTags.toList())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.accent),
                    enabled = selectedRating > 0,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(
                        text = "Submit Review",
                        fontWeight = FontWeight.Bold,
                        color = if (appColors.isDark) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChipsRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Featured", "All Movies", "Action", "Comedy", "Drama", "Sci-Fi")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val isSelected = selectedCategory == category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) Color(0xFFFF5359) else Color(0xFF201F21)
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class CarouselSlide(
    val title: String,
    val imageUrl: String,
    val description: String,
    val category: String,
    val rating: String,
    val onClick: () -> Unit
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HeroMovieBanner(
    allMovies: List<Movie>,
    viewModel: StreamingViewModel
) {
    if (allMovies.isEmpty()) return

    val appColors = LocalAppColors.current
    val supabaseItems by viewModel.supabaseCarouselItems.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "বাংলা"

    val slides = remember(allMovies, supabaseItems, isBn) {
        val list = if (supabaseItems.isNotEmpty()) {
            supabaseItems.map { item ->
                val matchingMovie = allMovies.find { it.id == item.content_id }
                CarouselSlide(
                    title = item.title ?: matchingMovie?.title ?: "Featured Title",
                    imageUrl = item.poster_url ?: matchingMovie?.imgUrl ?: "",
                    description = item.description ?: matchingMovie?.synopsis ?: (if (isBn) "আমাদের প্রিমিয়াম সিলেকশন উপভোগ করুন।" else "Enjoy our premium selection."),
                    category = matchingMovie?.category ?: (if (isBn) "সক্রিয়" else "Featured"),
                    rating = matchingMovie?.rating?.toString() ?: "9.5",
                    onClick = {
                        if (matchingMovie != null) {
                            viewModel.selectMovie(matchingMovie)
                            viewModel.selectTab(MainTab.DISCOVER)
                        } else if (item.content_id != null) {
                            val mov = allMovies.find { it.id == item.content_id }
                            if (mov != null) {
                                viewModel.selectMovie(mov)
                                viewModel.selectTab(MainTab.DISCOVER)
                            }
                        }
                    }
                )
            }
        } else {
            val featured = allMovies.filter { 
                it.id == "mov_oppenheimer" || it.id == "mov_dune" || it.id == "mov_batman" || it.id == "mov_avatar" || it.rating >= 7.5 
            }.take(24).ifEmpty { allMovies.take(6) }
            
            featured.map { movie ->
                CarouselSlide(
                    title = movie.title,
                    imageUrl = movie.imgUrl,
                    description = movie.synopsis,
                    category = movie.category,
                    rating = movie.rating.toString(),
                    onClick = {
                        viewModel.selectMovie(movie)
                        viewModel.selectTab(MainTab.DISCOVER)
                    }
                )
            }
        }
        list.take(4) // Ensure exactly 4 slides for 4-dot carousel
    }

    var currentSlideIndex by remember { mutableStateOf(0) }

    // Automatic transition timer for sliding effect
    LaunchedEffect(slides) {
        if (slides.size > 1) {
            while (true) {
                delay(6000)
                currentSlideIndex = (currentSlideIndex + 1) % slides.size
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val slide = slides.getOrNull(currentSlideIndex) ?: slides.firstOrNull()
        if (slide != null) {
            // Main Active Cinematic Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp) // Height matches Live TV Carousel card height
            ) {
                AnimatedContent(
                    targetState = slide,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "hero_banner_slider",
                    modifier = Modifier.fillMaxSize()
                ) { currentSlide ->
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { currentSlide.onClick() },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // High Quality Poster
                            AsyncImage(
                                model = currentSlide.imageUrl,
                                contentDescription = "${currentSlide.title} Cinematic Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Cinematic dark gradient for visual text contrast
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.4f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            // Cinematic Overlaid Details
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = currentSlide.title.uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "2026 • 2h 30m • ${currentSlide.category}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Rating",
                                            tint = Color(0xFFFFE143),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = currentSlide.rating,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(appColors.primary, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "IMDb",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Interactive Watch Now button
                                    Button(
                                        onClick = {
                                            currentSlide.onClick()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                                        shape = RoundedCornerShape(24.dp),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .bounceClick {
                                                currentSlide.onClick()
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Watch Now",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4-Dot Animation Overlay (Smooth horizontal slide animation)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Row of 4 background dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f))
                                    .clickable {
                                        currentSlideIndex = idx
                                    }
                            )
                        }
                    }
                    
                    // Sliding Active Indicator Dot
                    val activeOffset by animateDpAsState(
                        targetValue = (currentSlideIndex * (8 + 8)).dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "hero_active_dot_offset"
                    )
                    
                    Box(
                        modifier = Modifier
                            .offset(x = activeOffset)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFC4)) // Match Live TV active color
                    )
                }
            }
        }
    }
}

@Composable
fun MovieRowSection(
    title: String,
    movies: List<Movie>,
    viewModel: StreamingViewModel
) {
    val appColors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
            Text(
                text = "View All",
                fontSize = 13.sp,
                color = appColors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    viewModel.selectTab(MainTab.DISCOVER)
                }
            )
        }

        LazyRow(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
                var scale by remember { mutableStateOf(0.9f) }
                var alpha by remember { mutableStateOf(0f) }
                LaunchedEffect(movie.id) {
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    ) { value, _ ->
                        alpha = value
                        scale = 0.9f + (value * 0.1f)
                    }
                }

                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .bounceClick {
                            viewModel.selectMovie(movie)
                            viewModel.selectTab(MainTab.DISCOVER)
                        }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, appColors.border),
                        modifier = Modifier
                            .width(130.dp)
                            .aspectRatio(2f / 3f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (movie.imgUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = movie.imgUrl,
                                    contentDescription = movie.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(movie.colorAccent.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Movie,
                                        contentDescription = null,
                                        tint = appColors.textMuted,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }

                            // Star rating badge matching the HTML spec
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFE143),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                val displayRating = when (movie.id) {
                                    "mov_oppenheimer" -> "8.5"
                                    "mov_john_wick_4" -> "8.3"
                                    "mov_the_batman" -> "8.1"
                                    "mov_avatar_2" -> "7.8"
                                    "mov_top_gun_maverick" -> "8.6"
                                    "mov_shawshank" -> "9.3"
                                    "mov_inception" -> "8.8"
                                    "mov_interstellar" -> "8.6"
                                    "mov_pulp_fiction" -> "8.9"
                                    else -> String.format("%.1f", movie.rating * 2)
                                }
                                Text(
                                    text = displayRating,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = movie.title,
                        color = appColors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LiveChannelsRow(
    allChannels: List<LiveChannel>,
    viewModel: StreamingViewModel
) {
    val appColors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Channels",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
            Text(
                text = "View All",
                fontSize = 13.sp,
                color = appColors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    viewModel.selectTab(MainTab.LIVE_TV)
                }
            )
        }

        LazyRow(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(allChannels.take(12), key = { it.id }) { channel ->
                var scale by remember { mutableStateOf(0.9f) }
                var alpha by remember { mutableStateOf(0f) }
                LaunchedEffect(channel.id) {
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    ) { value, _ ->
                        alpha = value
                        scale = 0.9f + (value * 0.1f)
                    }
                }

                Column(
                    modifier = Modifier
                        .width(96.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .bounceClick {
                            viewModel.selectLiveChannel(channel)
                            viewModel.selectTab(MainTab.LIVE_TV)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(appColors.cardBg)
                            .border(
                                BorderStroke(
                                    2.dp, 
                                    Brush.sweepGradient(listOf(appColors.primary, appColors.accent, appColors.primary))
                                ), 
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (channel.logoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = channel.logoUrl,
                                contentDescription = channel.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(channel.logoAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                  Text(
                                    text = channel.name.take(2).uppercase(),
                                    color = channel.logoAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = channel.name,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = channel.category,
                        color = appColors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
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
fun ContinueWatchingRow(
    viewModel: StreamingViewModel,
    allChannels: List<LiveChannel>
) {
    val continueWatchingList by viewModel.continueWatchingList.collectAsState()
    val appColors = LocalAppColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Continue Watching",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
            Text(
                text = "View All",
                fontSize = 13.sp,
                color = Color(0xFFFF5359),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    viewModel.selectTab(MainTab.DISCOVER)
                }
            )
        }

        if (continueWatchingList.isNotEmpty()) {
            LazyRow(
                state = rememberLazyListState(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(continueWatchingList, key = { it.id }) { item ->
                    var scale by remember { mutableStateOf(0.9f) }
                    var alpha by remember { mutableStateOf(0f) }
                    LaunchedEffect(item.id) {
                        animate(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) { value, _ ->
                            alpha = value
                            scale = 0.9f + (value * 0.1f)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .width(210.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                            .clickable {
                                when (item.type) {
                                    "MOVIE" -> {
                                        viewModel.setAutoPlayMovie(item.mediaId)
                                        viewModel.selectTab(MainTab.DISCOVER)
                                    }
                                    "EPISODE" -> {
                                        viewModel.setAutoPlayEpisode(item.episodeId ?: "")
                                        viewModel.selectTab(MainTab.CATCH_UP)
                                    }
                                    "CATCH_UP" -> {
                                        val channel = allChannels.find { it.id == item.mediaId }
                                        if (channel != null) {
                                            viewModel.selectLiveChannel(channel)
                                            viewModel.selectTab(MainTab.LIVE_TV)
                                        }
                                    }
                                }
                            }
                            .testTag("continue_watching_card_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, appColors.border)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(105.dp)
                                    .background(item.colorAccent.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .background(item.colorAccent, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.type,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    color = appColors.textMuted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = { item.progress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(CircleShape),
                                        color = item.colorAccent,
                                        trackColor = appColors.border
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("${(item.progress * 100).toInt()}%", color = appColors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(appColors.cardBg, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No movies or learning lectures in progress. Discover contents below!",
                    color = appColors.textMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FilteredMoviesView(
    category: String,
    movies: List<Movie>,
    viewModel: StreamingViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "$category Results",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
        Text(
            text = "${movies.size} titles found",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (movies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No movies match this filter category or search phrase.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            movies.chunked(2).forEach { rowMovies ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowMovies.forEach { movie ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.selectMovie(movie)
                                    viewModel.selectTab(MainTab.DISCOVER)
                                }
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF130E20)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f / 3f)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (movie.imgUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = movie.imgUrl,
                                            contentDescription = movie.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(movie.colorAccent.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = movie.title.take(1),
                                                color = Color.White.copy(alpha = 0.1f),
                                                fontSize = 60.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5359),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        val displayRating = when (movie.id) {
                                            "mov_oppenheimer" -> "8.5"
                                            "mov_john_wick_4" -> "8.3"
                                            "mov_the_batman" -> "8.1"
                                            "mov_avatar_2" -> "7.8"
                                            "mov_top_gun_maverick" -> "8.6"
                                            "mov_shawshank" -> "9.3"
                                            "mov_inception" -> "8.8"
                                            "mov_interstellar" -> "8.6"
                                            "mov_pulp_fiction" -> "8.9"
                                            else -> String.format("%.1f", movie.rating * 2)
                                        }
                                        Text(
                                            text = displayRating,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = movie.category,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                    if (rowMovies.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun StickyAnimatedHeader(
    collapseFraction: Float,
    showStickyChips: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNotificationClick: () -> Unit,
    viewModel: StreamingViewModel,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "বাংলা"
    val density = androidx.compose.ui.platform.LocalDensity.current
    val statusBarHeight = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerHeight = 116.dp + statusBarHeight - (56.dp * collapseFraction)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(
                appColors.background.copy(alpha = collapseFraction)
            )
    ) {
        // Subtle decorative gradient glow behind top bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            appColors.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Header Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1 (Sticky App title, notification indicator, Subscription button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            append(if (isBn) "স্বপ্ন" else "Swapno")
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFE50914)))
                            append(if (isBn) "ফ্লিক্স" else "flix")
                            pop()
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = appColors.textPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val notifications by viewModel.notifications.collectAsState()
                    val hasUnread = notifications.any { !it.isRead }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onNotificationClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsNone,
                            contentDescription = "Alerts Hub",
                            tint = appColors.textPrimary
                        )
                        if (hasUnread) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(appColors.primary)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Subscription Button instead of Profile Icon
                    Button(
                        onClick = { viewModel.navigateToSettingsSubSection("BILLING_CENTER") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appColors.accent,
                            contentColor = if (appColors.isDark) Color.Black else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .bounceClick { viewModel.navigateToSettingsSubSection("BILLING_CENTER") },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Subscribe",
                            tint = if (appColors.isDark) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Subscribe",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Row 2 (Smooth collapsing Searchbar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp * (1f - collapseFraction))
                    .clip(androidx.compose.ui.graphics.RectangleShape)
                    .graphicsLayer {
                        alpha = (1f - collapseFraction * 3f).coerceIn(0f, 1f)
                    }
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search movies, shows, or categories...", color = appColors.textMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = appColors.textMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = appColors.textMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary,
                        focusedContainerColor = appColors.cardBg,
                        unfocusedContainerColor = appColors.cardBg,
                        focusedBorderColor = appColors.accent,
                        unfocusedBorderColor = appColors.border
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            }
        }
    }
}

@Composable
fun NotificationsOverlay(
    notifications: List<NotificationItem>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = Color(0xFFFF5359)
                )
                Text("Notifications Hub", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (notifications.isEmpty()) {
                    Text(
                        "No new notifications at this time.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                } else {
                    notifications.forEach { notif ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF201F21), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(notif.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notif.description, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notif.timestamp, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFFFF5359))
            }
        },
        containerColor = Color(0xFF131315),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DiscoverScreen(viewModel: StreamingViewModel) {
    val allMovies by viewModel.moviesList.collectAsState()
    val allSeries by viewModel.seriesList.collectAsState()
    var selectedTag by remember { mutableStateOf("All Recommended") }
    val tags = listOf("All Recommended", "Sci-Fi", "Documentary", "Kids / Animation", "Action")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Recommendation Lab",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(onClick = { viewModel.selectTab(MainTab.HOME) }) {
                Icon(Icons.Filled.MenuOpen, contentDescription = null, tint = Color(0xFF00FFC4))
            }
        }

        // Custom tag selection (Screen 25 / 26)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                val isSel = selectedTag == tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) Color(0xFFE50914) else Color(0xFF1E172E))
                        .clickable { selectedTag = tag }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tag,
                        color = if (isSel) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // AI Personalized learning block (Screen 33)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF130F1A)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00FFC4).copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF00FFC4).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, tint = Color(0xFF00FFC4))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Active AI Learning Node", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("SwapnoFlix neural algorithms map your watch habits.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You heavily favor Sci-Fi Action streams and Sports previews before 12:00 PM. We have auto-adjusted EPG prioritisation grids.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Action Recommendation results (Screen 33.1)
        Text(
            text = "AI Custom Matches",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val filteredMovies = if (selectedTag == "All Recommended") {
            allMovies
        } else {
            allMovies.filter { it.category.contains(selectedTag) }
        }

        if (filteredMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching movies found in this AI gate.", color = Color.White.copy(alpha = 0.4f))
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredMovies.forEach { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E172E))
                            .clickable {
                                viewModel.selectMovie(movie)
                                viewModel.navigateTo(AppDestination.MAIN_HUB)
                                viewModel.selectTab(MainTab.LIVE_TV) // Go watch
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(movie.colorAccent.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(movie.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text(movie.category + " • " + movie.duration, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF00FFC4).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text("98% AI Match", color = Color(0xFF00FFC4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(viewModel: StreamingViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isListeningVoice by viewModel.isListeningVoice.collectAsState()

    val allChannels by viewModel.channelsList.collectAsState()
    val allMovies by viewModel.moviesList.collectAsState()

    // Matching Results
    val results = allMovies.filter {
        it.title.lowercase().contains(searchQuery.lowercase()) ||
                it.category.lowercase().contains(searchQuery.lowercase())
    } + allChannels.filter {
        it.name.lowercase().contains(searchQuery.lowercase()) ||
                it.currentProgram.lowercase().contains(searchQuery.lowercase())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Global Smart Search",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Search Input Row (Screen 10.1 / 10.2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = null, tint = Color.White)
                        }
                    }
                },
                placeholder = { Text("Search sports, channels, movies...", color = Color.White.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE50914)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("global_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Voice Search (Screen 10.2)
            IconButton(
                onClick = { viewModel.triggerSimulatedVoiceSearch() },
                modifier = Modifier
                    .size(52.dp)
                    .background(if (isListeningVoice) Color(0xFF00FFC4) else Color(0xFF1E172E), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = if (isListeningVoice) Icons.Filled.MicNone else Icons.Filled.Mic,
                    contentDescription = "Voice Search",
                    tint = if (isListeningVoice) Color.Black else Color.White
                )
            }
        }

        // Voice Feedback Pop (Screen 10.2)
        AnimatedVisibility(visible = isListeningVoice) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF00FFC4).copy(alpha = 0.15f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFF00FFC4), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Listening actively to voice command...", color = Color(0xFF00FFC4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (searchQuery.isEmpty()) {
            // Search History Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Search History", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                TextButton(onClick = { viewModel.clearSearchHistory() }) {
                    Text("Clear All", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            // Search history tag capsules (Screen 10.3)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                searchHistory.forEach { history ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E172E))
                            .clickable { viewModel.updateSearchQuery(history) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.History, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(history, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auto Search Smart suggestions (Screen 10.4)
            Text(
                text = "Smart Suggestions",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            listOf("Action Movies 2026", "EPL Live Preview", "Cartoons", "Religious Live channels").forEach { sug ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateSearchQuery(sug) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF00FFC4), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(sug, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
        } else {
            // Results showing
            Text(
                text = "Matching Search Results (${results.size})",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sports, channels, or movies match search entry.", color = Color.White.copy(alpha = 0.4f))
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    results.forEach { result ->
                        val isChannel = result is LiveChannel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E172E))
                                .clickable {
                                    if (isChannel) {
                                        viewModel.selectLiveChannel(result as LiveChannel)
                                        viewModel.selectTab(MainTab.LIVE_TV)
                                    } else {
                                        viewModel.selectMovie(result as Movie)
                                        viewModel.navigateTo(AppDestination.MAIN_HUB)
                                        viewModel.selectTab(MainTab.LIVE_TV)
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isChannel) (result as LiveChannel).logoAccent else (result as Movie).colorAccent,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isChannel) "TV" else "VOD",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isChannel) (result as LiveChannel).name else (result as Movie).title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    if (isChannel) "Now: " + (result as LiveChannel).currentProgram else (result as Movie).category + " • " + (result as Movie).duration,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveSocialCommunityBar(viewModel: StreamingViewModel) {
    val activeUsers by viewModel.activeSocialUsers.collectAsState()
    
    if (activeUsers.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("live_community_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF00FFC4), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Live Stream Community",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${activeUsers.size} active",
                color = Color(0xFF00FFC4),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            activeUsers.forEach { user ->
                val colorParsed = try {
                    Color(android.graphics.Color.parseColor(user.avatarColorHex))
                } catch (e: Exception) {
                    Color(0xFFE50914)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .testTag("community_user_${user.uid}")
                ) {
                    Box(
                        modifier = Modifier.size(50.dp)
                    ) {
                        // User Profile Circle
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(colorParsed.copy(alpha = 0.25f))
                                .border(1.5.dp, colorParsed, CircleShape)
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Real-time online status dot
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .background(Color(0xFF00FFC4), CircleShape)
                                .border(1.5.dp, Color(0xFF0C0911), CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = user.name,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Social login provider badge
                    val badgeBg = when (user.provider) {
                        "Google" -> Color(0x334285F4)
                        "Facebook" -> Color(0x331877F2)
                        "Apple ID" -> Color(0x33FFFFFF)
                        else -> Color(0x33E50914)
                    }
                    val badgeTextCol = when (user.provider) {
                        "Google" -> Color(0xFF4285F4)
                        "Facebook" -> Color(0xFF1877F2)
                        "Apple ID" -> Color.White
                        else -> Color(0xFFE50914)
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = user.provider,
                            color = badgeTextCol,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E172E),
            Color(0xFF2D2244),
            Color(0xFF1E172E)
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun ShimmerHeroBanner(brush: Brush) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            )
        }
    }
}

@Composable
fun ShimmerLiveChannelsRow(brush: Brush) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .width(140.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun ShimmerContinueWatchingRow(brush: Brush) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .width(160.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun ShimmerMovieRowSection(brush: Brush, title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) {
                Column(modifier = Modifier.width(130.dp)) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerFilteredMoviesView(brush: Brush, category: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "$category Results",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(100.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        Spacer(modifier = Modifier.height(16.dp))
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(2) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(brush)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }
    }
}


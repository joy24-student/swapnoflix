package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.LocalAppColors
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView as NYoutubePlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun PlayTubeHomeScreen(viewModel: StreamingViewModel) {
    val activeVideo by viewModel.playTubeCurrentVideo.collectAsState()
    val isPipState by viewModel.playTubeIsPipMode.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "বাংলা"

    if (isPipState && activeVideo != null) {
        // App continues to show background feed while PiP is floating
        PlayTubeFeedLayer(viewModel)
    } else if (activeVideo != null) {
        PlayTubePlayerLayer(
            video = activeVideo!!,
            viewModel = viewModel,
            onMinimizeToPip = { viewModel.setPlayTubePipMode(true) }
        )
    } else {
        PlayTubeFeedLayer(viewModel)
    }
}

@Composable
fun PlayTubeFeedLayer(viewModel: StreamingViewModel) {
    val currentTab by viewModel.currentPlayTubeTab.collectAsState()
    val homeVideos by viewModel.youtubeHomeVideos.collectAsState()
    val shortsVideos by viewModel.youtubeShortsVideos.collectAsState()
    val searchResults by viewModel.youtubeSearchResults.collectAsState()
    val isLoading by viewModel.isYoutubeLoading.collectAsState()
    val apiSource by viewModel.playTubeApiSource.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "বাংলা"

    val appColors = LocalAppColors.current
    var activeCreatorDetail by remember { mutableStateOf<PlayTubeCreator?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        // Top Category Navigation
        ScrollableTabRow(
            selectedTabIndex = when (currentTab) {
                "Home" -> 0
                "Shorts" -> 1
                "Subscriptions" -> 2
                else -> 3
            },
            containerColor = appColors.surface,
            contentColor = appColors.primary,
            edgePadding = 16.dp,
            divider = {}
        ) {
            val tabs = if (isBn) listOf("হোম", "শর্টস", "সাবস্ক্রিপশন", "প্লেলিস্ট") else listOf("Home", "Shorts", "Subscriptions", "Playlists")
            val tabKeys = listOf("Home", "Shorts", "Subscriptions", "Playlists")
            tabKeys.forEachIndexed { index, tab ->
                Tab(
                    selected = currentTab == tab,
                    onClick = { viewModel.togglePlayTubeTab(tab) },
                    text = { Text(tabs[index], fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Sub-header for API Source info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = (if (isBn) "উৎস: " else "Source: ") + apiSource,
                color = appColors.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp, color = appColors.primary)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                "Home" -> {
                    if (isLoading && homeVideos.isEmpty()) {
                        LoadingStateView()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(homeVideos) { video ->
                                VideoFeedCard(
                                    video = video,
                                    onVideoClick = { viewModel.selectPlayTubeVideo(video) },
                                    onChannelClick = { /* Handle channel detail */ }
                                )
                            }
                        }
                    }
                }
                "Shorts" -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shortsVideos) { short ->
                            ShortsCard(video = short, onClick = { viewModel.selectPlayTubeVideo(short) })
                        }
                    }
                }
                "Subscriptions" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (isBn) "সাবস্ক্রিপশন শীঘ্রই আসছে" else "Subscriptions coming soon", color = appColors.textMuted)
                    }
                }
                "Playlists" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (isBn) "প্লেলিস্ট শীঘ্রই আসছে" else "Playlists coming soon", color = appColors.textMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun VideoFeedCard(
    video: PlayTubeVideo,
    onVideoClick: () -> Unit,
    onChannelClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVideoClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = "https://img.youtube.com/vi/${video.id}/hqdefault.jpg",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Duration Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(video.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(appColors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val isUrl = video.channelAvatar.startsWith("http")
                    if (isUrl) {
                        AsyncImage(model = video.channelAvatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(video.channelAvatar.ifEmpty { "📺" }, fontSize = 18.sp)
                    }
                }
                Column {
                    Text(video.title, color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${video.channelName} • ${video.viewCount} • ${video.uploadTime}", color = appColors.textMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ShortsCard(video: PlayTubeVideo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(280.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = "https://img.youtube.com/vi/${video.id}/maxresdefault.jpg",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(video.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(video.viewCount, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun LoadingStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFFE50914))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayTubePlayerLayer(
    video: PlayTubeVideo,
    viewModel: StreamingViewModel,
    onMinimizeToPip: () -> Unit
) {
    val appColors = LocalAppColors.current
    val relatedVideos by viewModel.playTubeRelatedVideos.collectAsState()
    val comments by viewModel.playTubeComments.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "বাংলা"

    var showCommentsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Video Player Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(Color.Black)
        ) {
            YouTubePlayerView(videoId = video.id, modifier = Modifier.fillMaxSize())
            
            // Header Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.selectPlayTubeVideo(null) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = onMinimizeToPip) {
                    Icon(Icons.Filled.PictureInPicture, contentDescription = null, tint = Color.White)
                }
            }
        }

        // Metadata
        Column(modifier = Modifier.padding(16.dp)) {
            Text(video.title, color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${video.viewCount} • ${video.uploadTime}", color = appColors.textMuted, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { /* Like */ }) { Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = appColors.textPrimary) }
                    Text(if (isBn) "লাইক" else "Like", color = appColors.textPrimary, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { viewModel.downloadPlayTubeVideo(video, false, "1080p") }) { Icon(Icons.Filled.Download, contentDescription = null, tint = appColors.textPrimary) }
                    Text(if (isBn) "ডাউনলোড" else "Download", color = appColors.textPrimary, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { /* Share */ }) { Icon(Icons.Filled.Share, contentDescription = null, tint = appColors.textPrimary) }
                    Text(if (isBn) "শেয়ার" else "Share", color = appColors.textPrimary, fontSize = 10.sp)
                }
            }
        }

        Divider(color = appColors.border, thickness = 0.5.dp)

        // Channel Info
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(appColors.surfaceVariant)) {
                AsyncImage(model = video.channelAvatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(video.channelName, color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(video.subscriberCount, color = appColors.textMuted, fontSize = 11.sp)
            }
            Button(onClick = { /* Subscribe */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                Text(if (isBn) "সাবস্ক্রাইব" else "Subscribe", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Divider(color = appColors.border, thickness = 0.5.dp)

        // Comments Preview
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { showCommentsSheet = true },
            colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(if (isBn) "মন্তব্য (${comments.size})" else "Comments (${comments.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (comments.isNotEmpty()) {
                    Text(comments.first().text, color = appColors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(if (isBn) "কোনো মন্তব্য নেই" else "No comments yet", color = appColors.textMuted, fontSize = 12.sp)
                }
            }
        }

        // Recommendations
        Text(if (isBn) "পরবর্তী ভিডিও" else "Up Next", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = appColors.textPrimary)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            relatedVideos.forEach { rec ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { viewModel.selectPlayTubeVideo(rec) },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(width = 120.dp, height = 70.dp).clip(RoundedCornerShape(8.dp))) {
                        AsyncImage(model = "https://img.youtube.com/vi/${rec.id}/hqdefault.jpg", contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Column {
                        Text(rec.title, color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${rec.channelName} • ${rec.viewCount}", color = appColors.textMuted, fontSize = 11.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showCommentsSheet) {
        ModalBottomSheet(onDismissRequest = { showCommentsSheet = false }, containerColor = appColors.cardBg) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Text(if (isBn) "মন্তব্য" else "Comments", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = appColors.textPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(comments) { comment ->
                    Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(appColors.surfaceVariant))
                        Column {
                            Text(comment.userName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = appColors.textPrimary)
                            Text(comment.text, fontSize = 13.sp, color = appColors.textPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayTubeFloatingPopupPlayer(viewModel: StreamingViewModel) {
    val currentVideo by viewModel.playTubeCurrentVideo.collectAsState()
    val isPipState by viewModel.playTubeIsPipMode.collectAsState()
    val isPlaying by viewModel.playTubeIsPlaying.collectAsState()

    if (currentVideo == null || !isPipState) return

    var pipOffset by remember { mutableStateOf(Offset(100f, 200f)) }

    Box(
        modifier = Modifier
            .offset { IntOffset(pipOffset.x.roundToInt(), pipOffset.y.roundToInt()) }
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .width(240.dp)
            .height(135.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    pipOffset += dragAmount
                }
            }
    ) {
        YouTubePlayerView(videoId = currentVideo!!.id, shouldPlay = isPlaying, showControls = false, modifier = Modifier.fillMaxSize())
        
        // Mini Controls
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            IconButton(onClick = { viewModel.setPlayTubePipMode(false) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Fullscreen, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = { viewModel.selectPlayTubeVideo(null) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun YouTubePlayerView(
    videoId: String,
    shouldPlay: Boolean = true,
    showControls: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var activePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }

    LaunchedEffect(shouldPlay) {
        if (shouldPlay) activePlayer?.play() else activePlayer?.pause()
    }

    val playerView = remember(videoId) {
        NYoutubePlayerView(context).apply {
            enableAutomaticInitialization = false
            initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    activePlayer = youTubePlayer
                    if (shouldPlay) youTubePlayer.loadVideo(videoId, 0f) else youTubePlayer.cueVideo(videoId, 0f)
                }
            }, IFramePlayerOptions.Builder().controls(if (showControls) 1 else 0).build())
        }
    }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(playerView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    AndroidView(factory = { playerView }, modifier = modifier)
}

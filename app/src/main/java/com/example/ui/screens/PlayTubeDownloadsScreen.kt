package com.example.ui.screens

import android.widget.Toast
import java.util.Locale
import com.example.ui.theme.LocalAppColors
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("unused")
fun PlayTubeDownloadsScreen(viewModel: StreamingViewModel) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    val downloads by viewModel.playTubeDownloads.collectAsState()
    val bookmarkedVideoIds by viewModel.playTubeBookmarkedVideoIds.collectAsState()
    val bookmarkedPlaylistIds by viewModel.playTubeBookmarkedPlaylistIds.collectAsState()
    val playlists by viewModel.playTubePlaylists.collectAsState()

    var activeSubTab by remember { mutableStateOf("Downloads") } // Downloads, Bookmarks, Playlists
    var isSimulatedOffline by remember { mutableStateOf(false) }

    // Calculate storage usage
    val totalSize = downloads.sumOf { it.fileSizeBytes }
    val sizeText = formatFileSize(totalSize)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .testTag("playtube_downloads_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        tint = Color(0xFF00FFC4),
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .padding(6.dp)
                    )
                    Column {
                        Text(
                            text = "Offline Library",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Saved on your device",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Simulated Offline Mode Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSimulatedOffline) Color(0xFFE50914).copy(alpha = 0.2f) else Color(0xFF1E142B))
                        .border(
                            width = 1.dp,
                            color = if (isSimulatedOffline) Color(0xFFE50914) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            isSimulatedOffline = !isSimulatedOffline
                            Toast.makeText(
                                context,
                                if (isSimulatedOffline) "Offline simulation enabled!" else "Online state restored.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isSimulatedOffline) Icons.Filled.WifiOff else Icons.Filled.Wifi,
                        contentDescription = null,
                        tint = if (isSimulatedOffline) Color(0xFFE50914) else Color(0xFF00FFC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isSimulatedOffline) "Offline" else "Online",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Connection Warning Banner
            AnimatedVisibility(visible = isSimulatedOffline) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Device is completely offline. Only downloaded files and saved playlists are accessible.",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Storage Overview Panel
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF130E1B)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.PieChart, contentDescription = null, tint = Color(0xFF00FFC4), modifier = Modifier.size(16.dp))
                            Text("Local Storage Usage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "$sizeText / 128 GB",
                            color = Color(0xFF00FFC4),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Double bar indicator (PlayTube vs Free storage)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        // Simulated used space percentage (PlayTube downloads take a tiny slice, other apps take more)
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.12f) // Other apps
                                    .background(Color.White.copy(alpha = 0.3f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.04f) // PlayTube downloads
                                    .background(Color(0xFF00FFC4))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.84f) // Free space
                                    .background(Color.Transparent)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF00FFC4), CircleShape))
                            Text("PlayTube ($sizeText)", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                            Text("Other Apps (15.4 GB)", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Text("112 GB Free", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }

            // Sub Tab Selectors (Downloads, Video Bookmarks, Playlist Bookmarks)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("Downloads", "Saved Videos", "Saved Playlists").forEach { tab ->
                    val isSel = activeSubTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSubTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tab,
                                color = if (isSel) Color.White else Color.White.copy(alpha = 0.5f),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedVisibility(visible = isSel) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(2.dp)
                                        .background(Color(0xFF00FFC4))
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            // Tab Content Window
            Box(modifier = Modifier.weight(1f)) {
                when (activeSubTab) {
                    "Downloads" -> {
                        if (downloads.isEmpty()) {
                            EmptyLibraryView(
                                imageVector = Icons.Outlined.FileDownload,
                                title = "No offline downloaded files",
                                desc = "Select any video in PlayTube feed and click the HFR toggle, audio-only mode, or customize quality to save offline."
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(downloads) { download ->
                                    DownloadFileRow(
                                        download = download,
                                        onPlay = {
                                            val matchedVideo = PlayTubeDatabase.videos.find { it.id == download.videoId }
                                            if (matchedVideo != null) {
                                                viewModel.selectPlayTubeVideo(matchedVideo)
                                                // Make sure if it's audio only, we sync the player mode
                                                if (download.isAudioOnly != viewModel.playTubeIsAudioOnly.value) {
                                                    viewModel.togglePlayTubeAudioOnly()
                                                }
                                                Toast.makeText(context, "Playing local offline stream: ${download.title}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "File source is missing", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onDelete = {
                                            viewModel.deletePlayTubeDownload(download.videoId)
                                            Toast.makeText(context, "Offline file deleted from disk", Toast.LENGTH_SHORT).show()
                                        },
                                        onPauseResume = {
                                            if (download.status == "Downloading") {
                                                viewModel.pausePlayTubeDownload(download.videoId)
                                            } else if (download.status == "Paused") {
                                                viewModel.resumePlayTubeDownload(download.videoId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "Saved Videos" -> {
                        val bookmarkedVideos = PlayTubeDatabase.videos.filter { bookmarkedVideoIds.contains(it.id) }
                        if (bookmarkedVideos.isEmpty()) {
                            EmptyLibraryView(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                title = "No bookmarked videos",
                                desc = "Keep track of learning series, ambient lofi beats, or science clips offline by bookmarking them inside the player."
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(bookmarkedVideos) { video ->
                                    BookmarkedVideoRow(
                                        video = video,
                                        onPlay = {
                                            viewModel.selectPlayTubeVideo(video)
                                            Toast.makeText(context, "Playing bookmarked video", Toast.LENGTH_SHORT).show()
                                        },
                                        onRemove = {
                                            viewModel.togglePlayTubeBookmark(video.id)
                                            Toast.makeText(context, "Removed bookmark", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "Saved Playlists" -> {
                        val savedPlaylists = playlists.filter { bookmarkedPlaylistIds.contains(it.id) }
                        if (savedPlaylists.isEmpty()) {
                            EmptyLibraryView(
                                imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                title = "No offline saved playlists",
                                desc = "Select any of your local custom playlists and bookmark them for full cached access."
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(savedPlaylists) { playlist ->
                                    BookmarkedPlaylistRow(
                                        playlist = playlist,
                                        onPlay = {
                                            val firstId = playlist.videoIds.firstOrNull()
                                            val v = PlayTubeDatabase.videos.find { it.id == firstId }
                                            if (v != null) {
                                                viewModel.selectPlayTubeVideo(v)
                                                Toast.makeText(context, "Streaming playlist: ${playlist.name}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Playlist is empty!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onRemove = {
                                            viewModel.togglePlayTubePlaylistBookmark(playlist.id)
                                            Toast.makeText(context, "Removed playlist from saved offline", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadFileRow(
    download: PlayTubeDownload,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onPauseResume: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF130E1B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail avatar placeholder with type badge overlay
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFF1E142B), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(download.thumbnailAvatar, fontSize = 24.sp)
                    
                    // Audio Only badge
                    if (download.isAudioOnly) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Headset,
                                contentDescription = null,
                                tint = Color(0xFF00FFC4),
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color.Black, CircleShape)
                                    .padding(2.dp)
                            )
                        }
                    }
                }

                // File metadata details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = download.channelName,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Type badge
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E142B), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (download.isAudioOnly) "AUDIO" else download.resolution,
                                color = Color(0xFF00FFC4),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Size badge
                        Text(
                            text = formatFileSize(download.fileSizeBytes),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )

                        // Status / Duration badge
                        Text(
                            text = if (download.status == "Completed") download.duration else download.status,
                            color = if (download.status == "Completed") Color.LightGray else Color(0xFF00FFC4),
                            fontSize = 11.sp,
                            fontWeight = if (download.status != "Completed") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Controls based on Status
                if (download.status == "Completed") {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .background(Color(0xFF00FFC4).copy(alpha = 0.15f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color(0xFF00FFC4), modifier = Modifier.size(18.dp))
                    }
                } else {
                    IconButton(
                        onClick = onPauseResume,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (download.status == "Downloading") Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Pause/Resume",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }

            // Progress Bar for downloading state
            if (download.status == "Downloading" || download.status == "Paused") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { download.progress },
                        color = Color(0xFF00FFC4),
                        trackColor = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "${(download.progress * 100).toInt()}%",
                        color = Color(0xFF00FFC4),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkedVideoRow(
    video: PlayTubeVideo,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF130E1B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E142B)),
                contentAlignment = Alignment.Center
            ) {
                val isUrl = video.channelAvatar.startsWith("http://") || video.channelAvatar.startsWith("https://") || video.channelAvatar.contains("/")
                if (isUrl) {
                    coil.compose.AsyncImage(
                        model = video.channelAvatar,
                        contentDescription = "Channel Logo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(video.channelAvatar, fontSize = 22.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.channelName,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .background(Color(0xFF00FFC4).copy(alpha = 0.1f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color(0xFF00FFC4), modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.BookmarkRemove, contentDescription = "Remove Bookmark", tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun BookmarkedPlaylistRow(
    playlist: PlayTubePlaylist,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF130E1B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF3F51B5), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = Color.White)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.videoIds.size} Offline Synced Tracks",
                    color = Color(0xFF00FFC4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .background(Color(0xFF00FFC4).copy(alpha = 0.1f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color(0xFF00FFC4), modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.BookmarkRemove, contentDescription = "Unbookmark Playlist", tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EmptyLibraryView(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = desc,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0.0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return String.format(Locale.US, "%.1f MB", mb)
}

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.delay

@Composable
fun MoviesSeriesHomeScreen(viewModel: StreamingViewModel, initialIsSeries: Boolean = false) {
    var isSeriesSection by remember(initialIsSeries) { mutableStateOf(initialIsSeries) }
    var isInitialLoading by remember { mutableStateOf(true) }
    val movieConfigs by viewModel.movieConfigs.collectAsState()
    LaunchedEffect(Unit) {
        delay(1200)
        isInitialLoading = false
    }

    // Dialog state targets
    var activeMovieDetails by remember { mutableStateOf<Movie?>(null) }
    var activeSeriesDetails by remember { mutableStateOf<Series?>(null) }

    // Playback active player overlays
    var activeMoviePlayer by remember { mutableStateOf<Movie?>(null) }
    var activeEpisodePlayer by remember { mutableStateOf<Pair<Series, Episode>?>(null) }

    val allMovies by viewModel.moviesList.collectAsState()
    val allSeries by viewModel.seriesList.collectAsState()

    val autoPlayMovieId by viewModel.autoPlayMovieId.collectAsState()
    val autoPlayEpisodeId by viewModel.autoPlayEpisodeId.collectAsState()

    LaunchedEffect(autoPlayMovieId, allMovies) {
        autoPlayMovieId?.let { id ->
            val movie = allMovies.find { it.id == id }
            if (movie != null) {
                activeMoviePlayer = movie
                viewModel.loadContentAssetsFromSupabase(movie.id)
                isSeriesSection = false
                viewModel.setAutoPlayMovie(null)
            }
        }
    }

    LaunchedEffect(autoPlayEpisodeId, allSeries) {
        autoPlayEpisodeId?.let { id ->
            for (series in allSeries) {
                for (eps in series.episodesBySeason.values) {
                    val ep = eps.find { it.id == id }
                    if (ep != null) {
                        activeEpisodePlayer = Pair(series, ep)
                        isSeriesSection = true
                        viewModel.setAutoPlayEpisode(null)
                        break
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0911))
            .testTag("vod_hub_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            // Header: Section Selector VOD (Screen 18 / Screen 21)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSeriesSection) "Premium Series Suite" else "Swapnoflix Movies VOD",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Toggle VOD / Series
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E172E))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isSeriesSection) Color(0xFFE50914) else Color.Transparent)
                            .clickable { isSeriesSection = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Movies", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSeriesSection) Color(0xFFE50914) else Color.Transparent)
                            .clickable { isSeriesSection = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Series", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            val shimmerBrush = rememberShimmerBrushVOD()

            if (!isSeriesSection) {
                if (isInitialLoading) {
                    ShimmerSpotlightHero(shimmerBrush)

                    Spacer(modifier = Modifier.height(24.dp))
                    ShimmerMovieRowSectionVOD(shimmerBrush, "Trending Action & Thrillers")

                    Spacer(modifier = Modifier.height(24.dp))
                    ShimmerMovieRowSectionVOD(shimmerBrush, "New Releases (Year 2026)")

                    Spacer(modifier = Modifier.height(24.dp))
                    ShimmerMovieRowSectionVOD(shimmerBrush, "Top Rated (4.5★+)")
                } else {
                    // Movies Section!
                    // Hero Spotlight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE50914).copy(alpha = 0.2f))
                            .clickable { activeMovieDetails = allMovies[0] },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFAB00), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("BLOCKBUSTER TRENDING", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(allMovies[0].title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 22.sp)
                            Text(allMovies[0].synopsis, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Trending Movies row (Screen 18.1)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Trending Action & Thrillers", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        allMovies.forEach { movie ->
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { activeMovieDetails = movie },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .background(movie.colorAccent.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.MovieFilter, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                                    }
                                    Text(movie.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }

                    // New Releases row (Screen 18.2)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("New Releases (Year 2026)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        allMovies.filter { it.releaseYear == 2026 }.forEach { movie ->
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { activeMovieDetails = movie },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .background(movie.colorAccent.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                                    }
                                    Text(movie.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }

                    // Top Rated (Screen 18.3)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Top Rated (4.5★+)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        allMovies.filter { it.rating >= 4.5f }.forEach { movie ->
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { activeMovieDetails = movie },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .background(movie.colorAccent.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Grade, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(36.dp))
                                    }
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(movie.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(movie.rating.toString(), color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (isInitialLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Popular Blockbuster TV Series", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    ShimmerSeriesRow(shimmerBrush)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Trending Shows this Week", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    ShimmerTrendingShows(shimmerBrush)
                } else {
                    // Series Section! (Screen 21)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Popular Blockbuster TV Series", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        allSeries.forEach { series ->
                            Card(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clickable { activeSeriesDetails = series },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(series.colorAccent.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Slideshow, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
                                    }
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(series.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1)
                                        Text("${series.seasonsCount} Seasons Broadcasted", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Trending Shows categories
                    Text("Trending Shows this Week", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        allSeries.forEach { shows ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E172E))
                                    .clickable { activeSeriesDetails = shows }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(shows.colorAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Tv, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(shows.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text("${shows.category} • ${shows.rating}★ Rating", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                                Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // Expanded Movie Details Bottom Dialog (Screen 19)
        activeMovieDetails?.let { movie ->
            LaunchedEffect(movie.id) {
                viewModel.loadMovieScreenConfigFromSupabase(movie.id)
            }
            var trailerPlaying by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { activeMovieDetails = null },
                containerColor = Color(0xFF1E172E),
                title = { Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Trailer Preview Simulator (Screen 19.1)
                        if (trailerPlaying) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("BUFFERS TRAILER STREAM (4K)...", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(movie.colorAccent.copy(alpha = 0.4f))
                                    .clickable { trailerPlaying = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Play Trailer", tint = Color.White, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Preview Official Trailer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val config = movieConfigs[movie.id]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF00FFC4).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(config?.promo_badge ?: "VOD PREMIUM", color = Color(0xFF00FFC4), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "${movie.releaseYear} • ${movie.duration} • ${movie.rating}★", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description (Screen 19.2)
                        Text("Movie Synopsis", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(movie.synopsis, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cast (Screen 19.3)
                        Text("Starring Cast", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(movie.cast.joinToString(", "), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reviews (Screen 19.4)
                        Text("User Reviews Feedback", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        movie.reviews.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(r, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                activeMovieDetails = null
                                viewModel.navigateToSettingsSubSection("CO_WATCH_ROOMS")
                            }
                        ) {
                            Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Co-Watch Together", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                activeMoviePlayer = movie
                                viewModel.loadContentAssetsFromSupabase(movie.id)
                                activeMovieDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                        ) {
                            Text("Watch VOD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            )
        }

        // Movie Player Premium Screen overlay (Screen 20)
        activeMoviePlayer?.let { movie ->
            MoviePlayerOverlayWidget(
                movie = movie,
                viewModel = viewModel,
                onDismiss = { activeMoviePlayer = null }
            )
        }

        // Expanded Series details dialog (Screen 22)
        activeSeriesDetails?.let { tvShow ->
            var activeSeasonIndex by remember { mutableStateOf(1) }

            AlertDialog(
                onDismissRequest = { activeSeriesDetails = null },
                containerColor = Color(0xFF1E172E),
                title = { Text(tvShow.title, color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(tvShow.synopsis, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic Season Selection tabs (Screen 22.1)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            (1..tvShow.seasonsCount).forEach { idx ->
                                val isSel = activeSeasonIndex == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) Color(0xFFE50914) else Color(0xFF130F1F))
                                        .clickable { activeSeasonIndex = idx }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Season $idx", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Episode lists representation with progress bar (Screen 22.2)
                        Text("Episode List Guides", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val eps = tvShow.episodesBySeason[activeSeasonIndex] ?: emptyList()
                        eps.forEach { ep ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeEpisodePlayer = Pair(tvShow, ep)
                                        activeSeriesDetails = null
                                    }
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF130F1F))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(tvShow.colorAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Ep ${ep.episodeNumber}: ${ep.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(ep.duration, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                        if (ep.progress > 0) {
                                            LinearProgressIndicator(
                                                progress = { ep.progress },
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .fillMaxWidth()
                                                    .height(2.dp),
                                                color = Color(0xFFE50914),
                                                trackColor = Color.White.copy(alpha = 0.1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Episode Player overlay (Screen 23)
        activeEpisodePlayer?.let { (series, episode) ->
            EpisodePlayerOverlayWidget(
                series = series,
                episode = episode,
                viewModel = viewModel,
                onDismiss = { activeEpisodePlayer = null }
            )
        }
    }
}

// Full VOD Player (Screen 20)
@Composable
fun MoviePlayerOverlayWidget(movie: Movie, viewModel: StreamingViewModel, onDismiss: () -> Unit) {
    var resumeAlertShown by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(if (movie.progress > 0) movie.progress else 0.15f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            if (currentProgress < 0.99f) {
                currentProgress += 0.005f
                viewModel.updateMovieProgress(movie.id, currentProgress)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Exit Player", tint = Color.White)
                }
                Text("VOD PLAYBACK ENVELOPE: ${movie.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Cast, contentDescription = null, tint = Color.White)
                }
            }

            // Stream Representation
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Movie else Icons.Filled.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("FHD 1080P • CODELINK ACTIVE", color = Color(0xFF00FFC4), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                Text("Simulating premium movie renderer. Seekbar tracks real latency.", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            }

            // Controls
            Column {
                // Resume Watching prompt (Screen 20.1)
                AnimatedVisibility(visible = resumeAlertShown) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Resume watching from 22:15?", color = Color.White, fontSize = 12.sp)
                            Row {
                                TextButton(onClick = { resumeAlertShown = false }) {
                                    Text("Start Over", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { resumeAlertShown = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC4))
                                ) {
                                    Text("Resume VOD", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Slider(
                    value = currentProgress,
                    onValueChange = {
                        currentProgress = it
                        viewModel.updateMovieProgress(movie.id, it)
                    },
                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFFE50914))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isPlaying = !isPlaying }) {
                            Icon(imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        val elapsedMinutes = (currentProgress * 134).toInt()
                        val elapsedSecs = ((currentProgress * 134 * 60) % 60).toInt()
                        Text(String.format("%02d:%02d / 2:14:00", elapsedMinutes, elapsedSecs), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }

                    // Quality Subtitle configuration options (Screen 20.2 / 20.3)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sub: EN", color = Color(0xFF00FFC4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Audio: Dual ENG 5.1", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Series Episode Player (Screen 23)
@Composable
fun EpisodePlayerOverlayWidget(series: Series, episode: Episode, viewModel: StreamingViewModel, onDismiss: () -> Unit) {
    var countdownTimer by remember { mutableStateOf(5) }
    var autoNextEnabled by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(if (episode.progress > 0) episode.progress else 0.2f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            if (currentProgress < 0.99f) {
                currentProgress += 0.01f
                viewModel.updateEpisodeProgress(series.id, episode.id, currentProgress)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (countdownTimer > 0) {
            delay(1000)
            countdownTimer--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text("TV Broadcast Episode ${episode.episodeNumber}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White)
                }
            }

            // Central core
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(series.title, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Text(episode.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))

                // Auto Next Countdown (Screen 23.1)
                if (countdownTimer > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E)),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("AUTO PLAY NEXT EPISODE", color = Color(0xFF00FFC4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Next Episode begins in: $countdownTimer seconds", color = Color.White, fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                    ) {
                        Text("Stream Next Episode Now", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Controls
            Column {
                Slider(
                    value = currentProgress,
                    onValueChange = {
                        currentProgress = it
                        viewModel.updateEpisodeProgress(series.id, episode.id, it)
                    },
                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFFE50914))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val epMinutes = (currentProgress * 45).toInt()
                    val epSecs = ((currentProgress * 45 * 60) % 60).toInt()
                    Text(String.format("%02d:%02d / 45:00", epMinutes, epSecs), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    Text("Auto Next: ON", color = Color(0xFF00FFC4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun rememberShimmerBrushVOD(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer_vod")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim_vod"
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
fun ShimmerSpotlightHero(brush: Brush) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            )
        }
    }
}

@Composable
fun ShimmerMovieRowSectionVOD(brush: Brush, title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                Column(modifier = Modifier.width(130.dp)) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(160.dp)
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
fun ShimmerSeriesRow(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier.width(150.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E))
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(brush)
                    )
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerTrendingShows(brush: Brush) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E172E))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}

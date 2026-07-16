package com.example.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object InvidiousService {
    private const val TAG = "InvidiousService"

    var lastSuccessSource: String = "Official YouTube Data API v3 (Dev Mode)"

    var dynamicApiKey: String? = null

    private fun getYouTubeApiKey(): String? {
        if (!dynamicApiKey.isNullOrBlank()) return dynamicApiKey
        val key = try {
            com.example.core.BuildConfig.YOUTUBE_API_KEY
        } catch (e: java.lang.Exception) {
            ""
        }
        return if (key.isBlank() || key.contains("YOUR_YOUTUBE_API_KEY_HERE")) null else key
    }

    private fun parseIso8601Duration(isoDuration: String): String {
        if (isoDuration.isBlank()) return "0:00"
        try {
            var duration = isoDuration.replace("PT", "")
            var hours = 0
            var minutes = 0
            var seconds = 0

            if (duration.contains("H")) {
                val parts = duration.split("H")
                hours = parts[0].toIntOrNull() ?: 0
                duration = if (parts.size > 1) parts[1] else ""
            }
            if (duration.contains("M")) {
                val parts = duration.split("M")
                minutes = parts[0].toIntOrNull() ?: 0
                duration = if (parts.size > 1) parts[1] else ""
            }
            if (duration.contains("S")) {
                val parts = duration.split("S")
                seconds = parts[0].toIntOrNull() ?: 0
            }

            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else if (minutes > 0 || seconds > 0) {
                String.format("%d:%02d", minutes, seconds)
            } else {
                "0:00"
            }
        } catch (e: java.lang.Exception) {
            return "0:00"
        }
    }

    private fun parsePublishedAt(publishedAt: String): String {
        try {
            if (publishedAt.length >= 10) {
                val datePart = publishedAt.substring(0, 10)
                return "on $datePart"
            }
        } catch (e: java.lang.Exception) {
            // ignore
        }
        return "recently"
    }

    private fun parseYouTubeApiVideoList(items: JSONArray, defaultCategory: String): List<PlayTubeVideo> {
        val list = mutableListOf<PlayTubeVideo>()
        for (i in 0 until items.length()) {
            try {
                val item = items.getJSONObject(i)
                
                // For search results, the video ID is inside an 'id' object
                val idObj = item.optJSONObject("id")
                val videoId = if (idObj != null) {
                    idObj.optString("videoId", "")
                } else {
                    item.optString("id", "")
                }
                if (videoId.isEmpty()) continue

                val snippet = item.optJSONObject("snippet") ?: continue
                val title = snippet.optString("title", "Untitled YouTube Video")
                val channelTitle = snippet.optString("channelTitle", "YouTube Creator")
                val channelId = snippet.optString("channelId", "youtube_channel")
                val description = snippet.optString("description", "No description provided.")
                val publishedAt = snippet.optString("publishedAt", "recently")
                
                val displayTime = parsePublishedAt(publishedAt)

                val stats = item.optJSONObject("statistics")
                val viewCountRaw = stats?.optLong("viewCount", 0) ?: 0L
                val viewCountStr = formatViews(viewCountRaw)

                val contentDetails = item.optJSONObject("contentDetails")
                val isoDuration = contentDetails?.optString("duration", "") ?: ""
                val durationStr = if (isoDuration.isNotEmpty()) parseIso8601Duration(isoDuration) else "0:00"

                val channelAvatarUrl = "https://ui-avatars.com/api/?name=${java.net.URLEncoder.encode(channelTitle, "UTF-8")}&background=random&color=fff"

                list.add(
                    PlayTubeVideo(
                        id = videoId,
                        title = title,
                        channelName = channelTitle,
                        channelId = channelId,
                        description = description,
                        viewCount = viewCountStr,
                        duration = durationStr,
                        uploadTime = displayTime,
                        videoUrl = "https://www.youtube.com/watch?v=$videoId",
                        category = defaultCategory,
                        channelAvatar = channelAvatarUrl,
                        subscriberCount = "Verified Creator"
                    )
                )
            } catch (e: java.lang.Exception) {
                // skip
            }
        }
        return list
    }

    private fun fetchTrendingFromYouTubeApi(apiKey: String): List<PlayTubeVideo>? {
        try {
            val url = "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics,contentDetails&chart=mostPopular&maxResults=25&regionCode=US&key=$apiKey"
            Log.d(TAG, "Fetching trending from Official YouTube API: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val obj = JSONObject(body)
                        val items = obj.optJSONArray("items")
                        if (items != null) {
                            val results = parseYouTubeApiVideoList(items, "Trending")
                            if (results.isNotEmpty()) {
                                lastSuccessSource = "Official YouTube Data API v3"
                                return results
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Official YouTube API trending failed with code ${response.code}")
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error fetching trending from YouTube API: ${e.message}")
        }
        return null
    }

    private fun searchFromYouTubeApi(query: String, apiKey: String): List<PlayTubeVideo>? {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encodedQuery&type=video&maxResults=25&key=$apiKey"
            Log.d(TAG, "Searching YouTube API: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val obj = JSONObject(body)
                        val items = obj.optJSONArray("items")
                        if (items != null) {
                            val videoIds = mutableListOf<String>()
                            for (i in 0 until items.length()) {
                                val item = items.optJSONObject(i)
                                val idObj = item?.optJSONObject("id")
                                val videoId = idObj?.optString("videoId", "") ?: ""
                                if (videoId.isNotEmpty()) {
                                    videoIds.add(videoId)
                                }
                            }
                            
                            if (videoIds.isNotEmpty()) {
                                val idsParam = videoIds.joinToString(",")
                                val detailsUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics,contentDetails&id=$idsParam&key=$apiKey"
                                val detailsRequest = Request.Builder().url(detailsUrl).build()
                                client.newCall(detailsRequest).execute().use { detailsResponse ->
                                    if (detailsResponse.isSuccessful) {
                                        val detailsBody = detailsResponse.body?.string()
                                        if (!detailsBody.isNullOrBlank()) {
                                            val detailsObj = JSONObject(detailsBody)
                                            val detailItems = detailsObj.optJSONArray("items")
                                            if (detailItems != null) {
                                                val results = parseYouTubeApiVideoList(detailItems, "Search Result")
                                                if (results.isNotEmpty()) {
                                                    lastSuccessSource = "Official YouTube Data API v3"
                                                    return results
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error in searchFromYouTubeApi: ${e.message}")
        }
        return null
    }

    private fun fetchShortsFromYouTubeApi(apiKey: String): List<PlayTubeVideo>? {
        try {
            val encodedQuery = java.net.URLEncoder.encode("shorts trending", "UTF-8")
            val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encodedQuery&type=video&videoDuration=short&maxResults=25&key=$apiKey"
            Log.d(TAG, "Fetching Shorts from YouTube API: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val obj = JSONObject(body)
                        val items = obj.optJSONArray("items")
                        if (items != null) {
                            val videoIds = mutableListOf<String>()
                            for (i in 0 until items.length()) {
                                val item = items.optJSONObject(i)
                                val idObj = item?.optJSONObject("id")
                                val videoId = idObj?.optString("videoId", "") ?: ""
                                if (videoId.isNotEmpty()) {
                                    videoIds.add(videoId)
                                }
                            }
                            
                            if (videoIds.isNotEmpty()) {
                                val idsParam = videoIds.joinToString(",")
                                val detailsUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics,contentDetails&id=$idsParam&key=$apiKey"
                                val detailsRequest = Request.Builder().url(detailsUrl).build()
                                client.newCall(detailsRequest).execute().use { detailsResponse ->
                                    if (detailsResponse.isSuccessful) {
                                        val detailsBody = detailsResponse.body?.string()
                                        if (!detailsBody.isNullOrBlank()) {
                                            val detailsObj = JSONObject(detailsBody)
                                            val detailItems = detailsObj.optJSONArray("items")
                                            if (detailItems != null) {
                                                val results = parseYouTubeApiVideoList(detailItems, "Shorts")
                                                if (results.isNotEmpty()) {
                                                    lastSuccessSource = "Official YouTube Data API v3"
                                                    return results
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error in fetchShortsFromYouTubeApi: ${e.message}")
        }
        return null
    }

    // High availability fallback public Invidious instances to loop through in case of failover
    private var invidiousInstances = listOf(
        "https://inv.zoomerville.com",
        "https://inv.nadeko.net",
        "https://yt.chocolatemoo53.com",
        "https://invidious.nerdvpn.de",
        "https://invidious.f5.si",
        "https://invidious.jing.rocks",
        "https://vid.puffyan.us",
        "https://inv.tux.pizza",
        "https://invidious.private.coffee",
        "https://invidious.projectsegfaut.im",
        "https://invidious.flokinet.to"
    )

    var preferredInstance: String? = null
    var autoSwitchOnFailure: Boolean = true
    val customInstances = java.util.concurrent.CopyOnWriteArrayList<String>()

    private var dynamicInstances: List<String> = emptyList()
    private var lastFetchTime = 0L
    private const val CACHE_DURATION = 15 * 60 * 1000L // 15 minutes

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    fun getInstances(): List<String> {
        val now = System.currentTimeMillis()
        if (dynamicInstances.isEmpty() || (now - lastFetchTime) > CACHE_DURATION) {
            fetchDynamicInstances()
        }
        val baseList = if (dynamicInstances.isNotEmpty()) {
            (customInstances + invidiousInstances + dynamicInstances).distinct()
        } else {
            (customInstances + invidiousInstances).distinct()
        }
        val preferred = preferredInstance
        return if (preferred != null) {
            (listOf(preferred) + baseList).distinct()
        } else {
            baseList
        }
    }

    fun checkInstanceHealth(instanceUrl: String): Long {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url("$instanceUrl/api/v1/trending?limit=1")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val healthClient = client.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()
                
            healthClient.newCall(request).execute().use { response ->
                return if (response.isSuccessful) {
                    System.currentTimeMillis() - startTime
                } else {
                    -2L
                }
            }
        } catch (e: Exception) {
            return -2L
        }
    }

    private fun fetchDynamicInstances() {
        try {
            Log.d(TAG, "Fetching dynamic Invidious instances...")
            val request = Request.Builder()
                .url("https://api.invidious.io/v1/instances?sort_by=type,health")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val jsonArray = JSONArray(body)
                        val newList = mutableListOf<String>()
                        for (i in 0 until jsonArray.length()) {
                            try {
                                val item = jsonArray.getJSONArray(i)
                                val details = item.getJSONObject(1)
                                val apiSupported = details.optBoolean("api", false)
                                val type = details.optString("type", "")
                                val uri = details.optString("uri", "")
                                if (apiSupported && type == "https" && uri.isNotEmpty()) {
                                    // Strip trailing slash if present to maintain URL consistency
                                    val formattedUri = if (uri.endsWith("/")) uri.substring(0, uri.length - 1) else uri
                                    newList.add(formattedUri)
                                }
                            } catch (e: Exception) {
                                // Skip malformed item
                            }
                        }
                        if (newList.isNotEmpty()) {
                            dynamicInstances = newList
                            lastFetchTime = System.currentTimeMillis()
                            Log.d(TAG, "Successfully loaded ${dynamicInstances.size} dynamic instances")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load dynamic instances, relying on static backups: ${e.message}")
        }
    }

    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.yt",
        "https://piped-api.lunar.icu",
        "https://piped-api.privacydev.net",
        "https://piped-api.mha.fi",
        "https://pipedapi.colby.solutions",
        "https://pipedapi.adminforge.de"
    )

    /**
     * Executes an HTTP request on a public Invidious instance with automatic failover.
     */
    private fun executeWithFailover(path: String): String? {
        val preferred = preferredInstance
        val autoSwitch = autoSwitchOnFailure
        
        val instancesList = if (preferred != null && !autoSwitch) {
            listOf(preferred)
        } else {
            getInstances()
        }
        var attempts = 0
        // Limit total failover attempts to 5 to avoid waiting too long if multiple instances are down
        for (instance in instancesList) {
            if (attempts >= 5) {
                Log.d(TAG, "Reached max failover limit of 5 attempts. Stopping.")
                break
            }
            val url = "$instance$path"
            Log.d(TAG, "Attempting Invidious fetch: $url")
            attempts++
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            Log.d(TAG, "Successfully fetched from $instance")
                            lastSuccessSource = "Invidious (${instance.substringAfter("://")})"
                            if (autoSwitch && preferredInstance != instance) {
                                preferredInstance = instance
                            }
                            return body
                        }
                    } else {
                        Log.w(TAG, "Failed fetch from $instance: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from $instance: ${e.message}")
            }
        }
        return null
    }


    /**
     * Executes an HTTP request on a public Piped instance with automatic failover.
     */
    private fun executePipedWithFailover(path: String): String? {
        var attempts = 0
        for (instance in PIPED_INSTANCES) {
            if (attempts >= 4) {
                Log.d(TAG, "Reached max Piped failover limit of 4 attempts. Stopping.")
                break
            }
            val url = "$instance$path"
            Log.d(TAG, "Attempting Piped fetch: $url")
            attempts++
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            Log.d(TAG, "Successfully fetched Piped from $instance")
                            lastSuccessSource = "Piped (${instance.substringAfter("://")})"
                            return body
                        }
                    } else {
                        Log.w(TAG, "Failed fetch Piped from $instance: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Piped from $instance: ${e.message}")
            }
        }
        return null
    }

    /**
     * Fetch real-time trending videos from YouTube via Invidious, failing over to Piped.
     */
    fun fetchTrending(): List<PlayTubeVideo> {
        val apiKey = getYouTubeApiKey()
        if (apiKey != null) {
            val ytResults = fetchTrendingFromYouTubeApi(apiKey)
            if (!ytResults.isNullOrEmpty()) {
                return ytResults
            }
        }

        val jsonStr = executeWithFailover("/api/v1/trending")
        if (jsonStr != null) {
            try {
                val jsonArray = JSONArray(jsonStr)
                val results = parseVideoArray(jsonArray, "Trending")
                if (results.isNotEmpty()) {
                    Log.d(TAG, "Loaded trending videos successfully from Invidious")
                    return results
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing trending Invidious JSON: ${e.message}")
            }
        }

        Log.d(TAG, "Invidious trending failed, trying Piped fallback...")
        val pipedJsonStr = executePipedWithFailover("/trending?region=US")
        if (pipedJsonStr != null) {
            try {
                val jsonArray = JSONArray(pipedJsonStr)
                val results = parsePipedVideoArray(jsonArray, "Trending")
                if (results.isNotEmpty()) {
                    Log.d(TAG, "Loaded trending videos successfully from Piped fallback")
                    return results
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing trending Piped JSON: ${e.message}")
            }
        }

        lastSuccessSource = "Official YouTube Data API v3 (Dev Mode)"
        return emptyList()
    }

    /**
     * Search YouTube videos in real-time via Invidious, failing over to Piped.
     */
    fun search(query: String): List<PlayTubeVideo> {
        val apiKey = getYouTubeApiKey()
        if (apiKey != null) {
            val ytResults = searchFromYouTubeApi(query, apiKey)
            if (!ytResults.isNullOrEmpty()) {
                return ytResults
            }
        }

        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val jsonStr = executeWithFailover("/api/v1/search?q=$encodedQuery&type=video")
        if (jsonStr != null) {
            try {
                val jsonArray = JSONArray(jsonStr)
                val results = parseVideoArray(jsonArray, "Search Result")
                if (results.isNotEmpty()) {
                    Log.d(TAG, "Search results successfully retrieved from Invidious")
                    return results
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing search Invidious JSON: ${e.message}")
            }
        }

        Log.d(TAG, "Invidious search failed, trying Piped fallback...")
        val pipedJsonStr = executePipedWithFailover("/search?q=$encodedQuery&filter=videos")
        if (pipedJsonStr != null) {
            try {
                val jsonArray = if (pipedJsonStr.trim().startsWith("{")) {
                    val jsonObj = JSONObject(pipedJsonStr)
                    jsonObj.optJSONArray("items") ?: JSONArray()
                } else {
                    JSONArray(pipedJsonStr)
                }
                val results = parsePipedVideoArray(jsonArray, "Search Result")
                if (results.isNotEmpty()) {
                    Log.d(TAG, "Search results successfully retrieved from Piped fallback")
                    return results
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing search Piped JSON: ${e.message}")
            }
        }

        lastSuccessSource = "Official YouTube Data API v3 (Dev Mode)"
        return emptyList()
    }

    /**
     * Fetch real-time YouTube Shorts via Invidious, failing over to Piped.
     */
    fun fetchShorts(): List<PlayTubeVideo> {
        val apiKey = getYouTubeApiKey()
        if (apiKey != null) {
            val ytResults = fetchShortsFromYouTubeApi(apiKey)
            if (!ytResults.isNullOrEmpty()) {
                return ytResults
            }
        }

        // Try standard broad query "shorts" which is highly optimized and widely cached
        val queriesToTry = listOf("shorts", "trending shorts")
        for (query in queriesToTry) {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val jsonStr = executeWithFailover("/api/v1/search?q=$encodedQuery&type=video")
            if (jsonStr != null) {
                try {
                    val jsonArray = JSONArray(jsonStr)
                    val parsed = parseVideoArray(jsonArray, "Shorts")
                    val results = parsed.filter { video ->
                        val seconds = parseDurationToSeconds(video.duration)
                        seconds in 1..95
                    }
                    if (results.isNotEmpty()) {
                        Log.d(TAG, "Loaded shorts successfully from Invidious with query: $query")
                        return results
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing shorts JSON for query $query: ${e.message}")
                }
            }

            Log.d(TAG, "Invidious shorts failed for $query, trying Piped fallback...")
            val pipedJsonStr = executePipedWithFailover("/search?q=$encodedQuery&filter=videos")
            if (pipedJsonStr != null) {
                try {
                    val jsonArray = if (pipedJsonStr.trim().startsWith("{")) {
                        val jsonObj = JSONObject(pipedJsonStr)
                        jsonObj.optJSONArray("items") ?: JSONArray()
                    } else {
                        JSONArray(pipedJsonStr)
                    }
                    val parsed = parsePipedVideoArray(jsonArray, "Shorts")
                    val results = parsed.filter { video ->
                        val seconds = parseDurationToSeconds(video.duration)
                        seconds in 1..100
                    }
                    if (results.isNotEmpty()) {
                        Log.d(TAG, "Loaded shorts successfully from Piped fallback with query: $query")
                        return results
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing shorts Piped JSON for query $query: ${e.message}")
                }
            }
        }

        // Resilient Fallback 1: Extract short videos from Popular Feed (doesn't require search APIs which are heavily rate-limited)
        try {
            val jsonStrPopular = executeWithFailover("/api/v1/popular")
            if (jsonStrPopular != null) {
                val jsonArray = JSONArray(jsonStrPopular)
                val parsed = parseVideoArray(jsonArray, "Shorts")
                val results = parsed.filter { video ->
                    val seconds = parseDurationToSeconds(video.duration)
                    seconds in 1..120
                }
                if (results.isNotEmpty()) {
                    Log.d(TAG, "Successfully loaded dynamic shorts from popular feed fallback")
                    return results
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchShorts popular fallback: ${e.message}")
        }

        // Resilient Fallback 2: Extract short videos from Trending Feed if search and popular failed
        try {
            val trendingVideos = fetchTrending()
            if (trendingVideos.isNotEmpty()) {
                val shortTrending = trendingVideos.filter { video ->
                    val seconds = parseDurationToSeconds(video.duration)
                    seconds in 1..120
                }
                if (shortTrending.isNotEmpty()) {
                    Log.d(TAG, "Successfully loaded dynamic shorts from trending fallback")
                    return shortTrending
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchShorts trending fallback: ${e.message}")
        }

        lastSuccessSource = "Official YouTube Data API v3 (Dev Mode)"
        return emptyList()
    }

    private fun parseVideoArray(jsonArray: JSONArray, defaultCategory: String): List<PlayTubeVideo> {
        val list = mutableListOf<PlayTubeVideo>()
        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)
                val type = obj.optString("type", "video")
                if (type != "video") continue // Only process videos

                val videoId = obj.optString("videoId")
                if (videoId.isNullOrBlank()) continue

                val title = obj.optString("title", "Untitled YouTube Video")
                val author = obj.optString("author", "YouTube Creator")
                val authorId = obj.optString("authorId", "youtube_channel")
                val description = obj.optString("description", "No description provided.")
                val viewCountRaw = obj.optLong("viewCount", 0)
                val publishedText = obj.optString("publishedText", "recently")
                val lengthSeconds = obj.optInt("lengthSeconds", 0)

                val viewCountStr = formatViews(viewCountRaw)
                val durationStr = formatDuration(lengthSeconds)

                var authorAvatar = "📺"
                val authorThumbnails = obj.optJSONArray("authorThumbnails")
                if (authorThumbnails != null && authorThumbnails.length() > 0) {
                    val firstThumbnail = authorThumbnails.optJSONObject(0)
                    if (firstThumbnail != null) {
                        var thumbUrl = firstThumbnail.optString("url", "")
                        if (thumbUrl.startsWith("//")) {
                            thumbUrl = "https:$thumbUrl"
                        } else if (thumbUrl.startsWith("/")) {
                            thumbUrl = "https://yewtu.be$thumbUrl"
                        }
                        if (thumbUrl.isNotEmpty()) {
                            authorAvatar = thumbUrl
                        }
                    }
                }

                list.add(
                    PlayTubeVideo(
                        id = videoId,
                        title = title,
                        channelName = author,
                        channelId = authorId,
                        description = description,
                        viewCount = viewCountStr,
                        duration = durationStr,
                        uploadTime = publishedText,
                        videoUrl = "https://www.youtube.com/watch?v=$videoId",
                        category = defaultCategory,
                        channelAvatar = authorAvatar,
                        subscriberCount = "Subscribed Locally"
                    )
                )
            } catch (e: Exception) {
                // Skip individual failed items
            }
        }
        return list
    }

    private fun parsePipedVideoArray(jsonArray: JSONArray, defaultCategory: String): List<PlayTubeVideo> {
        val list = mutableListOf<PlayTubeVideo>()
        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)
                val type = obj.optString("type", "")
                // In Piped, videos are typed as "stream"
                if (type != "stream" && type != "video" && type.isNotEmpty()) continue

                val url = obj.optString("url", "")
                if (url.isEmpty()) continue

                val videoId = if (url.contains("?v=")) {
                    url.substringAfter("?v=").substringBefore("&")
                } else if (url.contains("watch/")) {
                    url.substringAfter("watch/").substringBefore("?")
                } else {
                    url
                }
                if (videoId.isEmpty()) continue

                val title = obj.optString("title", "Untitled YouTube Video")
                val uploaderName = obj.optString("uploaderName", "YouTube Creator")
                val uploaderUrl = obj.optString("uploaderUrl", "")
                val uploaderId = if (uploaderUrl.isNotEmpty()) uploaderUrl.substringAfterLast("/") else "channel_id"
                val description = obj.optString("shortDescription", "No description provided.")
                val viewsRaw = obj.optLong("views", 0)
                val uploadedDate = obj.optString("uploadedDate", "recently")
                val durationSeconds = obj.optInt("duration", 0)

                val viewCountStr = formatViews(viewsRaw)
                val durationStr = formatDuration(durationSeconds)

                var authorAvatar = "📺"
                val uploaderAvatar = obj.optString("uploaderAvatar", "")
                if (uploaderAvatar.isNotEmpty()) {
                    authorAvatar = uploaderAvatar
                }

                list.add(
                    PlayTubeVideo(
                        id = videoId,
                        title = title,
                        channelName = uploaderName,
                        channelId = uploaderId,
                        description = description,
                        viewCount = viewCountStr,
                        duration = durationStr,
                        uploadTime = uploadedDate,
                        videoUrl = "https://www.youtube.com/watch?v=$videoId",
                        category = defaultCategory,
                        channelAvatar = authorAvatar,
                        subscriberCount = "Subscribed Locally"
                    )
                )
            } catch (e: Exception) {
                // Skip individual failed items
            }
        }
        return list
    }

    fun fetchRelatedVideos(videoId: String): List<PlayTubeVideo> {
        val jsonStr = executeWithFailover("/api/v1/videos/$videoId")
        if (jsonStr != null) {
            try {
                val obj = JSONObject(jsonStr)
                val related = obj.optJSONArray("recommendedVideos")
                if (related != null) {
                    return parseVideoArray(related, "Related")
                }
            } catch (e: Exception) {}
        }
        return emptyList()
    }

    fun fetchVideoComments(videoId: String): List<JSONObject> {
        val jsonStr = executeWithFailover("/api/v1/comments/$videoId")
        if (jsonStr != null) {
            try {
                val obj = JSONObject(jsonStr)
                val comments = obj.optJSONArray("comments")
                if (comments != null) {
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until comments.length()) {
                        list.add(comments.getJSONObject(i))
                    }
                    return list
                }
            } catch (e: Exception) {}
        }
        return emptyList()
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "0:00"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }

    private fun formatViews(views: Long): String {
        if (views <= 0) return "0 views"
        return when {
            views >= 1_000_000_000 -> String.format("%.1fB views", views / 1_000_000_000.0)
            views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000.0)
            views >= 1_000 -> String.format("%.1fK views", views / 1_000.0)
            else -> "$views views"
        }
    }

    private fun parseDurationToSeconds(duration: String): Int {
        if (duration == "LIVE") return 0
        val parts = duration.split(":")
        return try {
            when (parts.size) {
                1 -> parts[0].toIntOrNull() ?: 0
                2 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
                3 -> (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
}

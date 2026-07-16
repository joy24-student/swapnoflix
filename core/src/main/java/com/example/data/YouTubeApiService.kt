package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class YTSearchResponse(
    @Json(name = "items") val items: List<YTSearchItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YTSearchItem(
    @Json(name = "id") val id: YTId,
    @Json(name = "snippet") val snippet: YTSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YTId(
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "videoId") val videoId: String? = null,
    @Json(name = "channelId") val channelId: String? = null
)

@JsonClass(generateAdapter = true)
data class YTSnippet(
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "channelTitle") val channelTitle: String? = null,
    @Json(name = "channelId") val channelId: String? = null,
    @Json(name = "publishedAt") val publishedAt: String? = null,
    @Json(name = "thumbnails") val thumbnails: YTThumbnails? = null
)

@JsonClass(generateAdapter = true)
data class YTThumbnails(
    @Json(name = "medium") val medium: YTThumbnailDetails? = null,
    @Json(name = "high") val high: YTThumbnailDetails? = null,
    @Json(name = "default") val default: YTThumbnailDetails? = null
)

@JsonClass(generateAdapter = true)
data class YTThumbnailDetails(
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class YTVideoListResponse(
    @Json(name = "items") val items: List<YTVideoItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YTVideoItem(
    @Json(name = "id") val id: String,
    @Json(name = "snippet") val snippet: YTSnippet? = null,
    @Json(name = "contentDetails") val contentDetails: YTContentDetails? = null,
    @Json(name = "statistics") val statistics: YTStatistics? = null
)

@JsonClass(generateAdapter = true)
data class YTContentDetails(
    @Json(name = "duration") val duration: String? = null
)

@JsonClass(generateAdapter = true)
data class YTStatistics(
    @Json(name = "viewCount") val viewCount: String? = null
)

interface YouTubeApi {
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("maxResults") maxResults: Int = 25,
        @Query("type") type: String = "video",
        @Query("q") query: String,
        @Query("key") apiKey: String
    ): YTSearchResponse

    @GET("videos")
    suspend fun getVideosDetails(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("id") videoIds: String,
        @Query("key") apiKey: String
    ): YTVideoListResponse

    @GET("videos")
    suspend fun getPopularVideos(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("chart") chart: String = "mostPopular",
        @Query("maxResults") maxResults: Int = 25,
        @Query("key") apiKey: String
    ): YTVideoListResponse
}

object YouTubeApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val apiService: YouTubeApi = retrofit.create(YouTubeApi::class.java)

    fun parseDuration(durationStr: String?): String {
        if (durationStr == null) return "0:00"
        if (durationStr.startsWith("PT")) {
            var temp = durationStr.substring(2)
            var hours = 0
            var minutes = 0
            var seconds = 0

            if (temp.contains("H")) {
                val parts = temp.split("H")
                hours = parts[0].toIntOrNull() ?: 0
                temp = if (parts.size > 1) parts[1] else ""
            }
            if (temp.contains("M")) {
                val parts = temp.split("M")
                minutes = parts[0].toIntOrNull() ?: 0
                temp = if (parts.size > 1) parts[1] else ""
            }
            if (temp.contains("S")) {
                val parts = temp.split("S")
                seconds = parts[0].toIntOrNull() ?: 0
            }

            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
        return durationStr
    }

    fun formatViewCount(viewCountStr: String?): String {
        val count = viewCountStr?.toLongOrNull() ?: return "0 views"
        return when {
            count >= 1_000_000_000 -> String.format("%.1fB views", count / 1_000_000_000.0)
            count >= 1_000_000 -> String.format("%.1fM views", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK views", count / 1_000.0)
            else -> "$count views"
        }
    }

    fun formatPublishedAt(publishedAt: String?): String {
        if (publishedAt == null) return "recently"
        return try {
            val clean = publishedAt.substringBefore("T")
            val parts = clean.split("-")
            if (parts.size == 3) {
                "${parts[1]}/${parts[2]}/${parts[0]}"
            } else {
                clean
            }
        } catch (e: Exception) {
            "recently"
        }
    }
}

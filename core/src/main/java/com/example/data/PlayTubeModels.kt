package com.example.data

import java.util.UUID

// ---------- YouTubeFormatUtils ----------
object YouTubeFormatUtils {
    fun parseDuration(durationStr: String?): String = YouTubeApiClient.parseDuration(durationStr)
    fun formatViewCount(viewCountStr: String?): String = YouTubeApiClient.formatViewCount(viewCountStr)
    fun formatPublishedAt(publishedAt: String?): String = YouTubeApiClient.formatPublishedAt(publishedAt)
}

// ---------- Data class for PlayTubeVideo ----------
data class PlayTubeVideo(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val description: String,
    val viewCount: String,
    val duration: String,
    val uploadTime: String,
    val videoUrl: String,
    val category: String,
    val channelAvatar: String = "📺",
    val thumbnailUrl: String? = null,
    val subscriberCount: String = "N/A"
)

// ---------- Data class for PlayTubeCreator ----------
data class PlayTubeCreator(
    val id: String,
    val name: String,
    val avatar: String,
    val subscriberCount: String,
    val bio: String,
    val bannerColor: String = "#2C1E40"
)

// ---------- Data class for PlayTubePlaylist ----------
data class PlayTubePlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val videoIds: List<String> = emptyList()
)

// ---------- Data class for PlayTubeDownload ----------
data class PlayTubeDownload(
    val videoId: String,
    val title: String,
    val channelName: String,
    val duration: String,
    val category: String,
    val fileSizeBytes: Long,
    val isAudioOnly: Boolean,
    val resolution: String,
    val progress: Float,
    val status: String,
    val thumbnailAvatar: String
)

// ---------- Data class for InvidiousInstanceStatus ----------
data class InvidiousInstanceStatus(
    val url: String,
    val isCustom: Boolean = false,
    val latencyMs: Long = -1L,
    val isChecking: Boolean = false
)

// ---------- Data class for comments ----------
data class PlayTubeComment(
    val userName: String,
    val text: String,
    val timeAgo: String,
    val userAvatar: String
)

// ---------- PlayTubeDatabase ----------
object PlayTubeDatabase {
    val videos = listOf(
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
        ),
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

    val creators = listOf(
        PlayTubeCreator(
            id = "creator_lofi",
            name = "Lofi Girl",
            avatar = "🎧",
            subscriberCount = "14.3M",
            bio = "The ultimate chill study workspace soundtrack. Continuous lo-fi hip hop beats for studying, relaxing, or working offline.",
            bannerColor = "#2C1E40"
        ),
        PlayTubeCreator(
            id = "creator_mkbhd",
            name = "Marques Brownlee",
            avatar = "📱",
            subscriberCount = "18.2M",
            bio = "Tech reviews, product deep dives, and futuristic concepts discussed with premium production details.",
            bannerColor = "#FF1100"
        ),
        PlayTubeCreator(
            id = "creator_ted",
            name = "TED-Ed",
            avatar = "💡",
            subscriberCount = "19.5M",
            bio = "Beautifully animated lessons that feed your curiosity about astronomy, science, history, and physics.",
            bannerColor = "#E60000"
        ),
        PlayTubeCreator(
            id = "creator_nasa",
            name = "NASA Science",
            avatar = "🚀",
            subscriberCount = "8.5M",
            bio = "Official science payloads, space exploration highlights, and aerospace engineering milestones explained simple.",
            bannerColor = "#0B3C5D"
        ),
        PlayTubeCreator(
            id = "creator_rick",
            name = "RickAstleyVEVO",
            avatar = "🎤",
            subscriberCount = "4.2M",
            bio = "Legendary pop singer-songwriter Rick Astley. Never gonna give you up, never gonna let you down.",
            bannerColor = "#D3A15C"
        ),
        PlayTubeCreator(
            id = "wood_craft",
            name = "Woodworking Craft",
            avatar = "🪵",
            subscriberCount = "1.2M",
            bio = "Satisfying woodworking, resin crafting, and lumber refurbishing.",
            bannerColor = "#8B5A2B"
        ),
        PlayTubeCreator(
            id = "setup_labs",
            name = "Desk Setup Labs",
            avatar = "🖥️",
            subscriberCount = "950K",
            bio = "Reviewing workspaces, ergonomics, monitors, and mechanical keyboards.",
            bannerColor = "#333333"
        ),
        PlayTubeCreator(
            id = "tech_crushers",
            name = "Tech Crushers",
            avatar = "🧲",
            subscriberCount = "2.1M",
            bio = "Pushing limits of physics and engineering with extreme hardware testing.",
            bannerColor = "#4B0082"
        ),
        PlayTubeCreator(
            id = "beast_clips",
            name = "MrBeast Clips",
            avatar = "⚡",
            subscriberCount = "35M",
            bio = "High energy stunts, challenges, and philanthropies in short format.",
            bannerColor = "#00E5FF"
        )
    )
}

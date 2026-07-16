package com.example.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

// Data Models
data class LiveChannel(
    val id: String,
    val name: String,
    val logoAccent: Color,
    val category: String, // Sports, News, Kids, Movies, Music, Religious
    val country: String,
    val language: String,
    val currentProgram: String,
    val currentProgDescription: String,
    val currentProgTime: String,
    val progress: Float, // 0f to 1.0f
    val upcomingPrograms: List<String> = listOf("Morning Edition Show", "World News Special", "Prime Documentaries"),
    val cast: List<String> = listOf("Sarah Jenkins", "David Attenborough", "Michael Palin"),
    val isFavorite: Boolean = false,
    val streamUrl: String = "",
    val logoUrl: String = "",
    val isPremium: Boolean = false
)

data class Movie(
    val id: String,
    val title: String,
    val imgUrl: String = "", // Placeholders for Coil
    val colorAccent: Color,
    val category: String,
    val rating: Float,
    val duration: String,
    val releaseYear: Int,
    val synopsis: String,
    val cast: List<String>,
    val reviews: List<String> = listOf("A stunning masterpiece!", "Absolutely breathtaking visuals.", "Storyline was a bit predictable but worth the watch."),
    val progress: Float = 0f, // For "Continue Watching" simulator
    val isFavorite: Boolean = false
)

data class Series(
    val id: String,
    val title: String,
    val imgUrl: String = "",
    val colorAccent: Color,
    val category: String,
    val rating: Float,
    val seasonsCount: Int,
    val synopsis: String,
    val cast: List<String>,
    val episodesBySeason: Map<Int, List<Episode>>,
    val isFavorite: Boolean = false
)

data class Episode(
    val id: String,
    val season: Int,
    val episodeNumber: Int,
    val title: String,
    val duration: String,
    val progress: Float = 0f
)

data class ProgramEvent(
    val id: String,
    val title: String,
    val timeSlot: String,
    val channelName: String,
    val dayOffset: Int // 0 is today, -1 to -7 are past Catch Up, 1 to 6 are future EPG
)

data class ContinueWatchingItem(
    val id: String, // Can be unique id of movie, series-episode, or catch-up program
    val title: String,
    val subtitle: String, // e.g., "S1:E2" or "Global News 24/7"
    val progress: Float, // 0.0f to 1.0f
    val type: String, // "MOVIE", "EPISODE", "CATCH_UP"
    val mediaId: String, // ID of movie, series, or live channel
    val episodeId: String? = null,
    val programId: String? = null,
    val durationText: String = "",
    val colorAccent: Color = Color(0xFFE50914)
)

data class UserProfile(
    val id: String,
    val name: String,
    val iconColor: Color,
    val isKids: Boolean,
    val ageCategory: String, // "Under 7", "7-12", "Teens", "18+"
    val contentPreference: String, // "Sports", "Movies", "News", "Kids Content", "Anything"
    val avatarUrl: String? = null
)

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val type: String, // "REMINDER", "BILLING", "NEW_CONTENT"
    val timestamp: String,
    var isRead: Boolean = false
)

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val price: String,
    val period: String,
    val features: List<String>
)

data class InvoiceRecord(
    val id: String,
    val date: String,
    val planName: String,
    val amount: String,
    val paymentMethod: String,
    val status: String // "PAID", "PENDING"
)

data class SupportTicket(
    val id: String,
    val subject: String,
    val category: String,
    val description: String,
    val date: String,
    val status: String // "OPEN", "RESOLVED", "IN PROGRESS"
)

// Prepopulated static mock data database
object StreamingDatabase {
    val professionalAvatars = listOf(
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=128&h=128&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=128&h=128&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=128&h=128&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?q=80&w=128&h=128&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=128&h=128&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1552058544-f2b08422138a?q=80&w=128&h=128&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=128&h=128&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=128&h=128&auto=format&fit=crop"
    )

    val channels = CustomM3uChannels.channels + listOf(
        LiveChannel(
            id = "ch_sport_1",
            name = "SwapnoSports HD",
            logoAccent = Color(0xFFE50914),
            category = "Sports",
            country = "USA",
            language = "English",
            currentProgram = "Champions League Finals: Preview Show",
            currentProgDescription = "An exciting countdown and analytic review to the biggest trophy play-offs with legendary football guests.",
            currentProgTime = "11:00 AM - 12:30 PM",
            progress = 0.65f
        ),
        LiveChannel(
            id = "ch_sport_2",
            name = "Stellar Futbol",
            logoAccent = Color(0xFF00C853),
            category = "Sports",
            country = "UK",
            language = "English",
            currentProgram = "EPL Classic Rewind: Derby Special",
            currentProgDescription = "Reliving some of the highest-scoring soccer derbies in Premier League history.",
            currentProgTime = "10:30 AM - 1:00 PM",
            progress = 0.3f
        ),
        LiveChannel(
            id = "ch_news_1",
            name = "Global News 24/7",
            logoAccent = Color(0xFF1E88E5),
            category = "News",
            country = "Global",
            language = "English",
            currentProgram = "Pulse Daily Review: Markets Update",
            currentProgDescription = "Real-time coverage of financial flows, commodity exchanges, and emerging technology breakthroughs.",
            currentProgTime = "11:00 AM - 11:30 AM",
            progress = 0.85f
        ),
        LiveChannel(
            id = "ch_movies_1",
            name = "Action Cinema Prime",
            logoAccent = Color(0xFFFFAB00),
            category = "Movies",
            country = "USA",
            language = "English",
            currentProgram = "Shadow Over Midnight (4K Broadcast)",
            currentProgDescription = "A highly rated cybersecurity agent gets trapped inside an automated fortress.",
            currentProgTime = "9:00 AM - 11:45 AM",
            progress = 0.95f
        ),
        LiveChannel(
            id = "ch_music_1",
            name = "BeatDrop FM Video",
            logoAccent = Color(0xFFD602EE),
            category = "Music",
            country = "UK",
            language = "English",
            currentProgram = "Ultra Electro House Hour",
            currentProgDescription = "Vibrant electronic dance mixes synced with hyper-creative holographic stage visualizations.",
            currentProgTime = "11:00 AM - 12:00 PM",
            progress = 0.2f
        ),
        LiveChannel(
            id = "ch_kids_1",
            name = "Panda Adventures TV",
            logoAccent = Color(0xFF00B0FF),
            category = "Kids",
            country = "Global",
            language = "English",
            currentProgram = "Bamboo Forest Quest",
            currentProgDescription = "Little panda Pip and his friends teach counting, friendship, and basic nature science in lovely 3D.",
            currentProgTime = "10:30 AM - 11:30 AM",
            progress = 0.70f
        ),
        LiveChannel(
            id = "ch_relig_1",
            name = "Faith Horizon",
            logoAccent = Color(0xFF7CB342),
            category = "Religious",
            country = "Saudi Arabia",
            language = "Arabic / English",
            currentProgram = "Wisdom of the Ancient Sands",
            currentProgDescription = "Inspiring spiritual lessons, scriptures analysis, and values-centered living reflections.",
            currentProgTime = "11:00 AM - 12:00 PM",
            progress = 0.4f
        )
    )

    val movies = listOf(
        Movie(
            id = "mov_oppenheimer",
            title = "Oppenheimer",
            imgUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAGOTrJYCEN9YYVZSRpCB795mV1xQbOb-__vYAg0bYbRpq868EjS_b4ui3Pc-xr1u86F2p1oyltwu3Q8hd8pktGGVpY75zMXQFcVFdRSK88jkXU3hHT3kOhmM1WCeSxPB2UMVqsihge4a3z5wh6ClqSsGca3Yz9tnVk10odnePUOSWSGPQNfG1-8UrYiZPWXhlhaaL5sSmmiwQkaNLr_t-f9sd6LeFV0FUTqZBXUp9Axn5Jw_QnPDyQ7JQ6Did_bIuDzlk-qCyITEg",
            colorAccent = Color(0xFFFFB3B0),
            category = "Biography/Drama",
            rating = 4.8f,
            duration = "3h 00m",
            releaseYear = 2023,
            synopsis = "J. Robert Oppenheimer led the Manhattan Project to develop the first nuclear weapons, forever shifting history's trajectory in a fiery, golden-orange atomic glare.",
            cast = listOf("Cillian Murphy", "Emily Blunt", "Matt Damon"),
            progress = 0.55f
        ),
        Movie(
            id = "mov_john_wick_4",
            title = "John Wick: Chapter 4",
            imgUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAZ9ggJFB-K1yLdMfpAh4KmWUnhgFTYFwvbk05OKn3JqCvD9EtY49uD-S5h4lZJ3Z2c-0XLnihu2YImiucer5nA7srIvbSDxAf4X92vcwBgv5_VLBhPUz8ZdVyLCjDuwFanPX_jyRFp0msdc5hQ0nJpCrRuz6w_qquIdmkp_w3hombxy5UfbVFdUIkp8u2vE-ye4Nbu53jLk5kvNz_r3WggMhaLdL5iMPlH1k7o_7jVr3vRG4iTVmjwUCvM-xJ8RRAydDniBB879rQ",
            colorAccent = Color(0xFFFF5359),
            category = "Action",
            rating = 4.6f,
            duration = "2h 49m",
            releaseYear = 2023,
            synopsis = "John Wick uncovers a path to defeating The High Table, facing off against elite global assassins on rain-drenched Parisian streets under glowing neon light.",
            cast = listOf("Keanu Reeves", "Donnie Yen", "Bill Skarsgård"),
            progress = 0.35f
        ),
        Movie(
            id = "mov_the_batman",
            title = "The Batman",
            imgUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCEP-ymRKXfJwqqQzP5suEibomqoTc9WOCzrgLAKL8B-xqETHo6QRV5iPhcr_XnejcO0UNuB__oJldvpnxFhPS4Nx5GXmhcadb_IqUfoCfkjmnL1sAAlT0_ck67K1mcI8vbB0D0d_SVLdf37yZXg1SZg9lIJsm_9iTEP-KKqkxaIapcLn8gGNM3B7sbvHvXX58GN5as5zAxiKZTbPw49Pc9da7g0KFMnAEd8W711O5TsegodYRDFDWkaBhvDJ7KKRt9XxduN5dLDY4",
            colorAccent = Color(0xFFE9BCB9),
            category = "Action/Drama",
            rating = 4.5f,
            duration = "2h 56m",
            releaseYear = 2022,
            synopsis = "A gritty, dark silhouette of the Batman stalks the rain-drenched streets of Gotham City, solving a sadistic killer's cryptic trail of clues.",
            cast = listOf("Robert Pattinson", "Zoë Kravitz", "Paul Dano"),
            progress = 0.0f
        ),
        Movie(
            id = "mov_avatar_2",
            title = "Avatar: The Way of Water",
            imgUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAqOD_Jb64iOP3tm5oGnuKUqhING9rEbaLCZ3bRmNf-aRa9rjU8PmSA5BSP1Yt7p-r7rnFr9CThV6s0w6FFVK4IFcply0RqKM0oQDPW7y0Ek4khd8vNWt-52jRuHfXGjdk7kg3023DnWrsCH8RqHxqpcZO57biXq4DcrO0FzDpcMXT7ZmiA6QAeKGcCjGuFr5641hOnqs8JglMEN5g2pLjXbpJf88v7IY4ce2OVlofGxV5g_FOI_cTgp5z-B7kyvISSPQGqF-T8l5Y",
            colorAccent = Color(0xFF00E5FF),
            category = "Sci-Fi",
            rating = 4.4f,
            duration = "3h 12m",
            releaseYear = 2022,
            synopsis = "Jake Sully and Neytiri build a life under bioluminescent ocean waters, fighting off returning corporate invaders on the majestic moon Pandora.",
            cast = listOf("Sam Worthington", "Zoe Saldana", "Sigourney Weaver"),
            progress = 0.85f
        ),
        Movie(
            id = "mov_top_gun_maverick",
            title = "Top Gun: Maverick",
            imgUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCgBc6Et5xSx9tKfZzNzdoQB4FGR6Bpt9qae8_XHJwSVvET6STwlbxQXXn247_poEdYXZzMiYtu5zh1X3dYNDe09IBXE82WcRX9RX9F7qnb60N2Ra9uPLPImTwQn6YYCuMdqVQeDOq_FLn0Z5LNl1ta4zhoTPCcbhsw5lR6N-NNRp6tuVvI6kTGGgSH0CQBiPqht29-D3LX_GIVFfmBz_X8ScxsFUpsuOj9jdGp5nRh87aU8p3Yas5_9MBtGHRtDggz9dP7tg0PcH0",
            colorAccent = Color(0xFFFFC400),
            category = "Action",
            rating = 4.7f,
            duration = "2h 10m",
            releaseYear = 2022,
            synopsis = "After thirty years, Pete 'Maverick' Mitchell trains elite fighter pilot graduates for an impossible high-g altitude precision bombing run.",
            cast = listOf("Tom Cruise", "Miles Teller", "Jennifer Connelly"),
            progress = 0.0f
        ),
        Movie(
            id = "mov_shawshank",
            title = "The Shawshank Redemption",
            imgUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAM4kHW_zA4zl8j9UOuqFvOGIIBba1yY8GpsdRUy0CNXe1F2goQQy1tw_7TYvm4PubH4PsifP1wed7l0Wcf3M4UNA4OAswdk83e_37EeaOlnhDX6YG3LgGZ89pOF5usRMHzeXZT5oPLTizEB6bGirrkJi2f58dFUCR3D7DzmdrJhaPlkTeCUgjzduABwMnxRhweuLvpukxhCM7CaRbnsHnov-GCfnulslHsTM8n3I2YTeckDTL0xawWdBpHwGjjlVLrdlOgz2dvcM4",
            colorAccent = Color(0xFFE5E1E4),
            category = "Drama",
            rating = 4.9f,
            duration = "2h 22m",
            releaseYear = 1994,
            synopsis = "A soft-spoken banker wrongly convicted of murder navigates life over decades within Shawshank State Penitentiary, finding salvation in deep friendship.",
            cast = listOf("Tim Robbins", "Morgan Freeman", "Bob Gunton"),
            progress = 0.2f
        ),
        Movie(
            id = "mov_inception",
            title = "Inception",
            imgUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCaRRkCPcWXBZX-uuiAVeRSBYn8xGem_t_0hgKmvZl46uYrI1TlwOfT2cl-UaiWEBccawObTgNXhhqmqCy5JcPNLwWQkTpZ0xRKDn34xbNQh-1oBVF1qVJ4kqiwljFXWh8OTgNRBi6I9_MaGpF0Lx3rSexoDDAcO5kYM3ZJTzv4Ojif30fhCHetYQ4xpJ0OfIKA6F2qAwFtcY9Tl7GOcZhDkpn3y5IXQrehEQpMf_lKF13qV_Lmr7wfYB_oF3jLLEtVeE_D97aeCk0",
            colorAccent = Color(0xFF2979FF),
            category = "Sci-Fi",
            rating = 4.8f,
            duration = "2h 28m",
            releaseYear = 2010,
            synopsis = "A professional thief who extracts trade secrets through subconscious dream-sharing technology is offered a chance to have his criminal record erased.",
            cast = listOf("Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page"),
            progress = 0.0f
        ),
        Movie(
            id = "mov_interstellar",
            title = "Interstellar",
            imgUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCT7dZeSJvAlcNCcVlYU_Yliwr6E8QLXZmw49k-s-BGjw4RfD66gbygrMOZCp2NrquJtQxPSg9luSKf8Gwq08UvMKRuWPcI5VOo_ACZj0X6WHUMBKOHqqwLRrGAa2jbQSHGjHP_xizU-k8ORrSvHY73PYUZ_6HTiKv8HCFaPASBCGFjhPXgMCV9q2x8Hc7avlu8DIUd4UgRgPLbOb1DAUBbwnWqnsCETbw2e-CMb1WIDA1df0vMl_yndlcCYew_JtsunioPLMkqpbQ",
            colorAccent = Color(0xFF9E9E9E),
            category = "Sci-Fi",
            rating = 4.8f,
            duration = "2h 49m",
            releaseYear = 2014,
            synopsis = "A team of brave space explorers travel through a newly discovered wormhole near Saturn to find a viable new home planet for dying humanity.",
            cast = listOf("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain"),
            progress = 0.0f
        ),
        Movie(
            id = "mov_pulp_fiction",
            title = "Pulp Fiction",
            imgUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB8XL7KXZmwJrdtz7UwySAYNwS6LjImGrnlLjdAuepybttgQUMIKOrJkZXc3hP9r7JdgEblXxDXcH4APyKlN1SCuFPMyAT8FlGDzQYmotcmRA-baVVnJkpWlJNGM2mNNoDfjU_UNUo_Cvw4z-aBnJpYUU4VMeYgraQT4qK11J1mNXuxXah_9GHmEZJnwTPwoHeYHTW2_uckzLVf23xErj7qy4eV_DzaqMkfaEwksHaW73V_cmpC3qM58ABPDnMZH3-jSfoUEVy8MwM",
            colorAccent = Color(0xFFFFAB00),
            category = "Drama",
            rating = 4.9f,
            duration = "2h 34m",
            releaseYear = 1994,
            synopsis = "The intersecting lives of two philosophical hitmen, a boxer on the run, a gangster's glamorous wife, and diner thieves unfold in vivid color.",
            cast = listOf("John Travolta", "Uma Thurman", "Samuel L. Jackson"),
            progress = 0.0f
        )
    )

    val series = listOf(
        Series(
            id = "ser_1",
            title = "Cyber Grid: Chronicles",
            colorAccent = Color(0xFF2979FF),
            category = "Sci-Fi / Drama",
            rating = 4.9f,
            seasonsCount = 2,
            synopsis = "A multi-layered look into the cybernetic underbelly of virtual currency miners who run illicit holographic networks underneath historical city structures.",
            cast = listOf("Cillian Murphy", "Zendaya", "Pedro Pascal"),
            isFavorite = true,
            episodesBySeason = mapOf(
                1 to listOf(
                    Episode("ep_1_1", 1, 1, "The Genesis Key", "45 mins", 1.0f),
                    Episode("ep_1_2", 1, 2, "Hologram Rain", "42 mins", 0.9f),
                    Episode("ep_1_3", 1, 3, "Cryptic Junctions", "48 mins", 0.0f)
                ),
                2 to listOf(
                    Episode("ep_2_1", 2, 1, "Next-Gen Reboot", "50 mins", 0.2f),
                    Episode("ep_2_2", 2, 2, "Neural Override", "52 mins", 0.0f)
                )
            )
        ),
        Series(
            id = "ser_2",
            title = "Chef's Table: Spice Trails",
            colorAccent = Color(0xFFFF9100),
            category = "Documentary / Lifestyle",
            rating = 4.6f,
            seasonsCount = 1,
            synopsis = "Explore the vibrant histories, spices, and local market culture that make South Asian culinary systems iconic.",
            cast = listOf("Asma Khan", "Gaggan Anand"),
            episodesBySeason = mapOf(
                1 to listOf(
                    Episode("ep_s2_1_1", 1, 1, "Spices of the Bengal Delta", "38 mins", 1.0f),
                    Episode("ep_s2_1_2", 1, 2, "The Clay Pot Curry Masters", "41 mins", 0.0f)
                )
            )
        )
    )

    // EPG schedules spanning 7 days (-3 to +3 offsets)
    val epgSchedules = listOf(
        ProgramEvent("epg_1", "Sunrise Yoga Special", "06:00 AM - 07:00 AM", "SwapnoSports HD", 0),
        ProgramEvent("epg_2", "Motorsport Rally Championship", "07:30 AM - 09:30 AM", "SwapnoSports HD", 0),
        ProgramEvent("epg_3", "Champions League Preview", "11:00 AM - 12:30 PM", "SwapnoSports HD", 0),
        ProgramEvent("epg_4", "World News Today", "08:00 AM - 09:00 AM", "Global News 24/7", 0),
        ProgramEvent("epg_5", "Tech Innovations Summit", "09:30 AM - 10:45 AM", "Global News 24/7", 0),
        ProgramEvent("epg_6", "Shadow Over Midnight (4K Broadcast)", "09:00 AM - 11:45 AM", "Action Cinema Prime", 0),
        // Catch Up (Past events)
        ProgramEvent("epg_7", "Cricket Classics: BD vs IND T20 2024", "Yesterday (4:00 PM)", "SwapnoSports HD", -1),
        ProgramEvent("epg_8", "Retro Action Special: The Matrix 1", "2 Days Ago (8:00 PM)", "Action Cinema Prime", -2),
        ProgramEvent("epg_9", "Panda Forest Cooking Lesson", "3 Days Ago (9:00 AM)", "Panda Adventures TV", -3)
    )

    val subscriptionPlans = listOf(
        SubscriptionPlan(
            "sub_1", "Starter Lite", "৳249", "Month",
            listOf("1 Screen HD Access", "All Live TV Access", "Standard audio system", "Ad-supported library")
        ),
        SubscriptionPlan(
            "sub_2", "SwapnoFlix Premium", "৳499", "Month",
            listOf("2 Screens Full HD (1080p)", "Full Movies & Series suite", "Dolby Digital Atmos", "100% Ad-Free experience", "Download watch offline")
        ),
        SubscriptionPlan(
            "sub_3", "Grand Pro Ultra 4K", "৳899", "Month",
            listOf("4 Screens Ultra HD (4K HDR)", "Includes Kids Companion profile", "Exclusive live PPV events included", "Hi-Res Audio Master track")
        )
    )
}

// Coursera / Udemy Education System Data Models
data class EduMaterial(
    val id: String,
    val name: String,
    val url: String,
    val type: String // e.g. "PDF", "Cheat Sheet", "Slides", "GitHub Link"
)

data class EduLecture(
    val id: String,
    val title: String,
    val duration: String,
    val videoUrl: String, // Can be edited in Admin Panel!
    val pointsAwarded: Int = 15,
    val isCompleted: Boolean = false,
    val materials: List<EduMaterial> = emptyList()
)

data class EduReview(
    val studentName: String,
    val rating: Float,
    val comment: String,
    val date: String
)

data class EduCourse(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // Development, Design, AI & ML, Business, Marketing
    val instructorName: String,
    val instructorTitle: String,
    val duration: String,
    val difficulty: String, // Beginner, Intermediate, Advanced
    val rating: Float,
    val studentsEnrolled: Int,
    val color: Color,
    val lectures: List<EduLecture>,
    val reviews: List<EduReview>,
    val isEnrolled: Boolean = false,
    val completedPercent: Float = 0f,
    val certificateEarned: Boolean = false,
    val personalNotes: String = ""
)

data class CoWatchRoom(
    val id: String = java.util.UUID.randomUUID().toString(),
    val roomName: String,
    val mediaType: String, // "MOVIE", "TV_SHOW", "COURSE"
    val mediaTitle: String,
    val mediaId: String,
    val mediaDuration: String = "2h 15m",
    val mediaColorHex: String = "#E50914",
    val creatorName: String = "Me",
    val invitedUser: SocialActiveUser,
    val status: String = "PENDING", // "PENDING", "ACTIVE", "ENDED"
    val isPlaying: Boolean = false,
    val currentPlaybackPosition: String = "00:00",
    val playbackProgress: Float = 0f
)

@Serializable
data class SupabaseHomeCarousel(
    val id: String,
    val title: String,
    val slug: String,
    val description: String? = null,
    val display_type: String = "top_card",
    val is_active: Boolean = true
)

@Serializable
data class SupabaseHomeCarouselItem(
    val id: String,
    val carousel_id: String,
    val content_id: String? = null,
    val title: String? = null,
    val poster_url: String? = null,
    val link_url: String? = null,
    val description: String? = null,
    val sort_order: Int = 0
)

@Serializable
data class SupabaseCourse(
    val id: String,
    val title: String,
    val slug: String? = null,
    val description: String? = null,
    val instructor_name: String? = null,
    val instructor_title: String? = null,
    val duration_text: String? = null,
    val difficulty: String? = null,
    val rating_avg: Float = 4.5f,
    val student_count: Int = 1000,
    val poster_url: String? = null,
    val promo_video_url: String? = null,
    val promo_image_url: String? = null
)

@Serializable
data class SupabaseCourseLecture(
    val id: String,
    val course_id: String,
    val title: String,
    val duration_text: String? = null,
    val video_url: String? = null,
    val sort_order: Int = 0
)

@Serializable
data class SupabaseCourseReview(
    val id: String,
    val course_id: String,
    val profile_id: String? = null,
    val rating: Float = 5f,
    val review_text: String? = null
)

@Serializable
data class SupabaseEnrollment(
    val id: String,
    val profile_id: String,
    val course_id: String,
    val progress_percent: Double = 0.0,
    val completed: Boolean = false
)

@Serializable
data class SupabaseLectureProgress(
    val id: String,
    val enrollment_id: String,
    val lecture_id: String,
    val completed: Boolean = false,
    val watched_seconds: Int = 0
)

@Serializable
data class DbProfile(
    val id: String,
    val tenant_id: String? = null,
    val username: String? = null,
    val full_name: String? = null,
    val avatar_url: String? = null,
    val theme_mode: String = "dark",
    val is_kids: Boolean = false,
    val age_category: String? = null,
    val content_preferences: List<String> = emptyList(),
    val locale: String = "en"
    ,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class DbKidsProfile(
    val id: String,
    val owner_user_id: String,
    val name: String,
    val age_category: String = "7-12",
    val avatar_url: String? = null,
    val content_preferences: List<String> = emptyList(),
    val parental_pin_hash: String? = null,
    val is_active: Boolean = true
    ,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class DbContentItem(
    val id: String,
    val tenant_id: String? = null,
    val kind: String, // "movie", "series", "live_channel", "episode", "course"
    val title: String,
    val slug: String,
    val description: String? = null,
    val poster_url: String? = null,
    val banner_url: String? = null,
    val trailer_url: String? = null,
    val preview_url: String? = null,
    val genres: List<String> = emptyList(),
    val director: String? = null,
    val studio: String? = null,
    val producer: String? = null,
    val writer: String? = null,
    val language_code: String? = "en",
    val country_code: String? = "US",
    val content_rating: String? = null,
    val age_rating: String? = null,
    val release_year: Int? = null,
    val runtime_minutes: Int? = null,
    val rating_avg: Double? = 0.0,
    val rating_count: Int? = 0,
    val is_featured: Boolean = false,
    val is_trending: Boolean = false,
    val is_premium: Boolean = false,
    val is_live: Boolean = false,
    val status: String = "published",
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class SupabaseFavorite(
    val id: String,
    val profile_id: String,
    val content_id: String
)

@Serializable
data class SupabaseWatchHistory(
    val id: String,
    val profile_id: String,
    val content_id: String,
    val episode_id: String? = null,
    val progress_percent: Double = 0.0,
    val duration_seconds: Int? = null,
    val last_position_seconds: Int = 0,
    val event_type: String = "play"
)


@Serializable
data class SupabaseYouTubeSource(
    val id: String,
    val title: String,
    val playlist_id: String? = null,
    val channel_id: String? = null,
    val api_key: String? = null,
    val is_enabled: Boolean = true,
    val is_primary: Boolean = false,
    val created_by: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class SupabaseYouTubePlaylistItem(
    val id: String,
    val playlist_id: String,
    val video_id: String,
    val title: String? = null,
    val description: String? = null,
    val published_at: String? = null,
    val thumbnail_url: String? = null,
    val position: Int = 0
)

@Serializable
data class SupabasePlaylist(
    val id: String,
    val profile_id: String,
    val name: String,
    val description: String? = null,
    val is_public: Boolean = false
)

@Serializable
data class SupabasePlaylistItem(
    val id: String? = null,
    val playlist_id: String,
    val video_id: String,
    val position: Int = 0
)

// -- New Supabase-backed models --

@Serializable
data class SupabaseUserPlaylist(
    val id: String,
    val profile_id: String,
    val name: String,
    val description: String? = null,
    val is_public: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseUserPlaylistItem(
    val id: String? = null,
    val playlist_id: String,
    @kotlinx.serialization.SerialName("content_id") val video_id: String,
    @kotlinx.serialization.SerialName("sort_order") val position: Int = 0,
    val created_at: String? = null
)

@Serializable
data class SupabaseSupportTicket(
    val id: String,
    val profile_id: String? = null,
    val subject: String,
    val category: String,
    val description: String? = null,
    val status: String = "OPEN",
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseSupportMessage(
    val id: String? = null,
    val ticket_id: String,
    val sender_id: String? = null,
    val message: String,
    val is_staff: Boolean = false,
    val created_at: String? = null
)

@Serializable
data class SupabaseNotification(
    val id: String,
    val profile_id: String? = null,
    val title: String,
    val description: String? = null,
    val type: String = "GENERAL",
    val is_read: Boolean = false,
    val created_at: String? = null
)

@Serializable
data class SupabaseFeedback(
    val id: String? = null,
    val profile_id: String,
    val content_id: String? = null,
    val kind: String = "app",
    val rating: Int? = null,
    val title: String? = null,
    val message: String,
    val status: String = "new",
    val created_at: String? = null,
    val resolved_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseReferral(
    val id: String? = null,
    val profile_id: String,
    val referred_email: String,
    val reward_points: Int = 250,
    val status: String = "pending",
    val referral_code: String? = null,
    val invited_at: String? = null,
    val accepted_at: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseRewardEvent(
    val id: String? = null,
    val profile_id: String,
    val event_type: String,
    val points: Int = 0,
    val description: String? = null,
    val created_at: String? = null
)

@Serializable
data class SupabaseFriendship(
    val id: String? = null,
    val profile_id: String,
    val friend_profile_id: String,
    val status: String = "pending",
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseCoWatchRoom(
    val id: String? = null,
    val profile_id: String,
    val content_id: String? = null,
    val name: String,
    val description: String? = null,
    val is_active: Boolean = true,
    val session_token: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseCoWatchMember(
    val id: String? = null,
    val room_id: String,
    val profile_id: String,
    val joined_at: String? = null,
    val left_at: String? = null,
    val is_host: Boolean = false
)

@Serializable
data class SupabaseCoWatchMessage(
    val id: String? = null,
    val room_id: String,
    val profile_id: String,
    val message: String,
    val created_at: String? = null
)

@Serializable
data class CoWatchRoomMetadata(
    val mediaType: String,
    val mediaTitle: String,
    val mediaId: String,
    val mediaDuration: String,
    val mediaColorHex: String,
    val invitedUid: String,
    val invitedName: String,
    val invitedEmail: String,
    val status: String,
    val isPlaying: Boolean,
    val currentPlaybackPosition: String,
    val playbackProgress: Double
)

// ─── NEW: Routine Plans (routine_plans table) ─────────────────────────────────
@Serializable
data class SupabaseRoutinePlan(
    val id: String? = null,
    val profile_id: String,
    val title: String,
    val description: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

// ─── NEW: Routine Items (routine_items table) ─────────────────────────────────
@Serializable
data class SupabaseRoutineItem(
    val id: String? = null,
    val routine_plan_id: String,
    val content_id: String,
    val scheduled_at: String, // ISO8601 timestamp
    val note: String? = null,
    val is_enabled: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

// ─── NEW: Social Activities (social_activities table) ────────────────────────
@Serializable
data class SupabaseSocialActivity(
    val id: String? = null,
    val profile_id: String,
    val target_profile_id: String? = null,
    val activity_type: String, // "enrolled", "completed", "watched", "liked", "joined_room", "shared"
    val content_id: String? = null,
    val room_id: String? = null,
    val message: String? = null,
    val created_at: String? = null
)

// ─── NEW: Analytics Events (analytics_events table) ─────────────────────────
@Serializable
data class SupabaseAnalyticsEvent(
    val id: String? = null,
    val profile_id: String? = null,
    val event_name: String,
    val event_data: Map<String, String> = emptyMap(),
    val created_at: String? = null
)

// ─── NEW: Content Seasons (content_seasons table) ────────────────────────────
@Serializable
data class SupabaseContentSeason(
    val id: String,
    val content_id: String,
    val season_number: Int,
    val title: String? = null,
    val release_year: Int? = null,
    val created_at: String? = null
)

// ─── NEW: Content Episodes (content_episodes table) ──────────────────────────
@Serializable
data class SupabaseContentEpisode(
    val id: String,
    val season_id: String,
    val content_id: String,
    val episode_number: Int,
    val title: String,
    val description: String? = null,
    val runtime_minutes: Int? = null,
    val poster_url: String? = null,
    val stream_url: String? = null,
    val created_at: String? = null
)

// ─── NEW: App Config (app_config table) ─────────────────────────────────────
@Serializable
data class SupabaseAppConfig(
    val id: String? = null,
    val app_name: String = "SwapnoFlix IPTV",
    val theme: Map<String, String> = emptyMap(),
    val feature_flags: Map<String, String> = emptyMap(),
    val default_language: String = "en",
    val timezone: String = "Asia/Dhaka",
    val iptv_sync_code: String = "INITIAL_SYNC_001",
    val created_at: String? = null
)

// ─── App-layer model for social activity feed item ───────────────────────────
data class SocialActivityFeedItem(
    val id: String,
    val profileName: String,
    val avatarInitial: String,
    val activityType: String,
    val contentTitle: String?,
    val message: String,
    val timeAgo: String
)

// ─── NEW BATCH: Supabase schema integrations ─────────────────────────────────

@Serializable
data class SupabaseCategory(
    val id: String,
    val name: String,
    val description: String? = null,
    val parent_id: String? = null,
    val created_at: String? = null
)

@Serializable
data class SupabaseContentItemCategory(
    val content_id: String,
    val category_id: String,
    val created_at: String? = null
)

@Serializable
data class SupabaseContentAsset(
    val id: String,
    val content_id: String,
    val asset_type: String, // "video", "subtitle", "audio"
    val resolution: String? = null, // "1080p", "720p", "480p"
    val bit_rate: Int? = null,
    val file_url: String, // stream URL
    val mime_type: String? = null,
    val language: String? = null, // e.g. "English", "Bengali"
    val is_primary: Boolean = true,
    val created_at: String? = null
)

@Serializable
data class SupabaseEpgProgram(
    val id: String,
    val channel_id: String,
    val title: String,
    val description: String? = null,
    val start_time: String,
    val end_time: String,
    val category: String? = null,
    val created_at: String? = null
)

@Serializable
data class SupabaseNoticePopup(
    val id: String,
    val title: String,
    val body: String,
    val image_url: String? = null,
    val target_url: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null
)

@Serializable
data class SupabaseAdBanner(
    val id: String,
    val title: String,
    val image_url: String,
    val target_url: String? = null,
    val position: String? = null, // "home_hero", "home_middle", "sidebar"
    val is_active: Boolean = true,
    val created_at: String? = null
)

@Serializable
data class SupabaseAdImpression(
    val id: String? = null,
    val ad_id: String,
    val profile_id: String? = null,
    val clicked: Boolean = false,
    val created_at: String? = null
)

@Serializable
data class SupabaseAppUpdate(
    val id: String,
    val version_code: Int,
    val version_name: String,
    val description: String? = null,
    val is_mandatory: Boolean = false,
    val apk_url: String? = null,
    val created_at: String? = null
)

@Serializable
data class SupabaseAppUpdateDownload(
    val id: String? = null,
    val update_id: String,
    val profile_id: String? = null,
    val ip_address: String? = null,
    val created_at: String? = null
)

@Serializable
data class SupabaseDownload(
    val id: String? = null,
    val profile_id: String,
    val content_id: String,
    val status: String, // "queued", "downloading", "completed", "paused", "failed"
    val progress: Double = 0.0,
    val file_size: Long = 0L,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseTvQrPairing(
    val id: String? = null,
    val profile_id: String,
    val code: String,
    val qr_image_url: String? = null,
    val pairing_url: String? = null,
    val device_name: String? = null,
    val status: String = "pending", // "pending", "paired", "expired"
    val expires_at: String,
    val paired_profile_id: String? = null,
    val linked_at: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseMovieScreenConfig(
    val id: String? = null,
    val content_id: String,
    val layout: String = "default",
    val hero_image_url: String? = null,
    val background_image_url: String? = null,
    val promo_badge: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseAuditLog(
    val id: String? = null,
    val tenant_id: String? = null,
    val profile_id: String? = null,
    val action: String,
    val entity_type: String? = null,
    val entity_id: String? = null,
    val details: String = "{}",
    val created_at: String? = null
)

@Serializable
data class SupabaseTenant(
    val id: String,
    val slug: String,
    val name: String,
    val domain: String? = null,
    val settings: String = "{}",
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseTenantMember(
    val id: String? = null,
    val tenant_id: String,
    val user_id: String,
    val role: String = "user",
    val status: String = "active",
    val joined_at: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)




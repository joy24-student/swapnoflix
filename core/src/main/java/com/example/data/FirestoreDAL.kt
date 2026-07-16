package com.example.data

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

object FirestoreDAL {
    private const val TAG = "FirestoreDAL"

    /**
     * Initializes core database collections in Firestore if they do not yet exist or are empty.
     * Maps local prepopulated models directly to the requested TypeScript equivalent Firestore layouts.
     */
    fun initializeCollections(db: FirebaseFirestore) {
        Log.i(TAG, "Starting Firestore collections initialization check...")

        // 1. Initialize Subscription Plans
        db.collection("subscriptionPlans").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.i(TAG, "subscriptionPlans collection is empty, indexing seed data...")
                    for (plan in StreamingDatabase.subscriptionPlans) {
                        val planMap = hashMapOf(
                            "planId" to plan.id,
                            "name" to plan.name,
                            "price" to (plan.price.replace("৳", "").trim().toIntOrNull() ?: 499),
                            "currency" to "BDT",
                            "durationDays" to 30,
                            "deviceLimit" to 2,
                            "screenLimit" to 2,
                            "features" to plan.features,
                            "isFreeTrial" to false,
                            "isActive" to true,
                            "sortOrder" to 0
                        )
                        db.collection("subscriptionPlans").document(plan.id).set(planMap)
                    }
                } else {
                    Log.d(TAG, "subscriptionPlans collection already populated.")
                }
            }

        // 2. Initialize Channels
        db.collection("channels").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.i(TAG, "channels collection is empty, indexing seed catalog...")
                    for (ch in StreamingDatabase.channels) {
                        val colorHex = String.format("#%06X", 0xFFFFFF and ch.logoAccent.value.toLong().toInt())
                        val chMap = hashMapOf(
                            "channelId" to ch.id,
                            "name" to ch.name,
                            "logoUrl" to "channels/logos/${ch.id}.png",
                            "colorHex" to colorHex,
                            "categoryIds" to listOf(ch.category),
                            "languageCode" to ch.language,
                            "countryCode" to ch.country,
                            "source" to "direct",
                            "streamUrl" to "https://iptv-media.com/live/${ch.id}/master.m3u8",
                            "qualityOptions" to listOf("auto", "1080p", "720p", "480p"),
                            "status" to "online",
                            "isPremium" to true,
                            "isFeatured" to true,
                            "catchupEnabled" to true,
                            "catchupRetentionDays" to 7,
                            "favoriteCount" to 0,
                            "sortOrder" to 0,
                            "createdAt" to Timestamp.now(),
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("channels").document(ch.id).set(chMap)
                    }
                } else {
                    Log.d(TAG, "channels collection already populated.")
                }
            }

        // 3. Initialize Movies Catalog
        db.collection("movies").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.i(TAG, "movies collection is empty, indexing seed content...")
                    for (mov in StreamingDatabase.movies) {
                        val colorHex = String.format("#%06X", 0xFFFFFF and mov.colorAccent.value.toLong().toInt())
                        val movMap = hashMapOf(
                            "movieId" to mov.id,
                            "title" to mov.title,
                            "description" to mov.synopsis,
                            "posterUrl" to "movies/posters/${mov.id}.jpg",
                            "colorHex" to colorHex,
                            "videoSources" to listOf(
                                hashMapOf("quality" to "1080p", "url" to "movies/videos/${mov.id}/1080p/master.m3u8")
                            ),
                            "categoryIds" to listOf(mov.category),
                            "genres" to listOf(mov.category),
                            "cast" to mov.cast.map { castName -> hashMapOf("name" to castName, "role" to "Actor") },
                            "director" to "Director J.",
                            "ageRating" to "PG-13",
                            "durationMinutes" to 134,
                            "releaseDate" to Timestamp(Date(mov.releaseYear - 1900, 0, 1)),
                            "ratingAvg" to mov.rating.toDouble(),
                            "ratingCount" to 15,
                            "viewCount" to 1420,
                            "language" to "English",
                            "isFeatured" to true,
                            "isNewRelease" to true,
                            "isTopRated" to (mov.rating >= 4.5f),
                            "isPremium" to true,
                            "status" to "published",
                            "createdAt" to Timestamp.now(),
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("movies").document(mov.id).set(movMap)
                    }
                } else {
                    Log.d(TAG, "movies collection already populated.")
                }
            }

        // 4. Initialize Series Catalog
        db.collection("series").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.i(TAG, "series collection is empty, indexing episodic catalog...")
                    for (ser in StreamingDatabase.series) {
                        val colorHex = String.format("#%06X", 0xFFFFFF and ser.colorAccent.value.toLong().toInt())
                        val serMap = hashMapOf(
                            "seriesId" to ser.id,
                            "title" to ser.title,
                            "description" to ser.synopsis,
                            "posterUrl" to "series/posters/${ser.id}.jpg",
                            "colorHex" to colorHex,
                            "categoryIds" to listOf(ser.category),
                            "genres" to listOf(ser.category),
                            "cast" to ser.cast.map { castName -> hashMapOf("name" to castName, "role" to "Actor") },
                            "ageRating" to "18+",
                            "ratingAvg" to ser.rating.toDouble(),
                            "ratingCount" to 22,
                            "viewCount" to 2405,
                            "totalSeasons" to ser.seasonsCount,
                            "totalEpisodes" to 5,
                            "isFeatured" to true,
                            "isTrending" to ser.isFavorite,
                            "isPopular" to true,
                            "isPremium" to true,
                            "status" to "published",
                            "createdAt" to Timestamp.now(),
                            "updatedAt" to Timestamp.now()
                        )
                        db.collection("series").document(ser.id).set(serMap)

                        // Populate seasons and episodes
                        for ((seasonNum, episodes) in ser.episodesBySeason) {
                            val seasonId = "season_$seasonNum"
                            val seasonMap = hashMapOf(
                                "seasonNumber" to seasonNum,
                                "episodeCount" to episodes.size,
                                "releaseYear" to 2026
                            )
                            db.collection("series").document(ser.id)
                                .collection("seasons").document(seasonId).set(seasonMap)

                            // Populate Episodes sub-collection
                            for (ep in episodes) {
                                val epMap = hashMapOf(
                                    "episodeNumber" to ep.episodeNumber,
                                    "title" to ep.title,
                                    "description" to "Intense events fold in episode ${ep.episodeNumber} of season $seasonNum.",
                                    "durationMinutes" to (ep.duration.replace("mins", "").trim().toIntOrNull() ?: 45),
                                    "videoSources" to listOf(
                                        hashMapOf("quality" to "1080p", "url" to "series/${ser.id}/s${seasonNum}/e${ep.episodeNumber}/1080p/master.m3u8")
                                    ),
                                    "status" to "published"
                                )
                                db.collection("series").document(ser.id)
                                    .collection("seasons").document(seasonId)
                                    .collection("episodes").document(ep.id).set(epMap)
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "series collection already populated.")
                }
            }

        // 5. Initialize EPG TV Guide
        db.collection("epgPrograms").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.i(TAG, "epgPrograms collection is empty, seeding TV schedules...")
                    for (prog in StreamingDatabase.epgSchedules) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, prog.dayOffset)
                        
                        // Set standard hour slots
                        val isAm = prog.timeSlot.contains("AM", ignoreCase = true)
                        val slotNumber = prog.timeSlot.split(":")[0].trim().toIntOrNull() ?: 10
                        cal.set(Calendar.HOUR, slotNumber)
                        cal.set(Calendar.AM_PM, if (isAm) Calendar.AM else Calendar.PM)
                        cal.set(Calendar.MINUTE, 0)
                        val startTime = Timestamp(cal.time)
                        
                        cal.add(Calendar.HOUR, 1)
                        val endTime = Timestamp(cal.time)

                        val progMap = hashMapOf(
                            "programId" to prog.id,
                            "channelId" to when (prog.channelName) {
                                "NexSports HD" -> "ch_sport_1"
                                "Global News 24/7" -> "ch_news_1"
                                "Action Cinema Prime" -> "ch_movies_1"
                                "Panda Adventures TV" -> "ch_kids_1"
                                else -> "ch_sport_1"
                            },
                            "title" to prog.title,
                            "description" to "Excellent broadcast and review stream presenting fine entertainment.",
                            "category" to "Entertainment",
                            "startTime" to startTime,
                            "endTime" to endTime,
                            "isLive" to (prog.dayOffset == 0)
                        )
                        db.collection("epgPrograms").document(prog.id).set(progMap)
                    }
                } else {
                    Log.d(TAG, "epgPrograms collection already populated.")
                }
            }

        // 6. Initialize App Config main document
        db.collection("appConfig").document("main").get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Log.i(TAG, "appConfig/main does not exist, creating default system configurations...")
                    val configMap = hashMapOf(
                        "appName" to "SwapnoFlix IPTV",
                        "appLogoUrl" to "app/logo.png",
                        "splashScreenUrl" to "app/splash/variant_dark.png",
                        "themeColors" to hashMapOf(
                            "primary" to "#E50914",
                            "secondary" to "#1E172E",
                            "accent" to "#00FFC4",
                            "mode" to "dark"
                        ),
                        "supportedLanguages" to listOf("en", "bn"),
                        "defaultLanguage" to "en",
                        "timezone" to "Asia/Dhaka",
                        "featureToggles" to hashMapOf(
                            "liveTv" to true,
                            "movies" to true,
                            "series" to true,
                            "catchupTv" to true,
                            "aiRecommendations" to true,
                            "ads" to false,
                            "guestMode" to true
                        ),
                        "minSupportedAppVersion" to hashMapOf("android" to "1.0.0", "ios" to "1.0.0"),
                        "latestAppVersion" to hashMapOf("android" to "1.0.4", "ios" to "1.0.4"),
                        "updatedAt" to Timestamp.now(),
                        "updatedBy" to "system_init"
                    )
                    db.collection("appConfig").document("main").set(configMap)
                } else {
                    Log.d(TAG, "appConfig/main already configured.")
                }
            }

        // 7. Initialize Education Courses Catalog
        db.collection("eduCourses").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.i(TAG, "eduCourses collection is empty, indexing default LMS courses...")
                    val defaultCourses = listOf(
                        EduCourse(
                            id = "android_compose",
                            title = "Modern Android: Jetpack Compose Masterclass",
                            description = "Build beautiful, native, declarative user interfaces on Android with Material Design 3, animations, and clean state handling.",
                            category = "Development",
                            instructorName = "Dr. Angela Yu",
                            instructorTitle = "Senior Android Developer & Educator",
                            duration = "4h 15m",
                            difficulty = "Beginner",
                            rating = 4.9f,
                            studentsEnrolled = 14205,
                            color = Color(0xFFFF0055),
                            lectures = listOf(
                                EduLecture("ac_lec_1", "Course Introduction & Setup", "10:15", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                                EduLecture("ac_lec_2", "Declarative vs Imperative UI Design", "18:45", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
                                EduLecture("ac_lec_3", "State Hoisting, remember & Recomposition", "25:30", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"),
                                EduLecture("ac_lec_4", "Creating Your First M3 Scaffold Layout", "20:10", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4")
                            ),
                            reviews = listOf(
                                EduReview("Abrar Fahim", 5.0f, "Best compose course ever! Clear explanations and great visuals.", "June 25, 2026"),
                                EduReview("Zahid Hasan", 4.8f, "Excellent beginner course. The interactive labs are super helpful.", "May 12, 2026")
                            )
                        ),
                        EduCourse(
                            id = "kotlin_advanced",
                            title = "Advanced Kotlin Coroutines & Flow API",
                            description = "Master asynchronous streams, channel communication, structured concurrency, and thread safety in enterprise application layers.",
                            category = "Development",
                            instructorName = "Andrew Ng",
                            instructorTitle = "Kotlin Language Architect",
                            duration = "6h 40m",
                            difficulty = "Advanced",
                            rating = 4.8f,
                            studentsEnrolled = 8590,
                            color = Color(0xFFA033FF),
                            lectures = listOf(
                                EduLecture("ka_lec_1", "Introduction to Concurrency & Threads", "12:00", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"),
                                EduLecture("ka_lec_2", "Coroutine Builders: launch vs async", "22:40", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"),
                                EduLecture("ka_lec_3", "Flow Streams, Operators & StateFlow", "30:15", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4")
                            ),
                            reviews = listOf(
                                EduReview("Rahat Alam", 4.5f, "Very technical. Perfect for senior devs trying to master reactive flows.", "April 29, 2026"),
                                EduReview("Sarah Jenkins", 5.0f, "A masterclass on coroutine context and scope management.", "March 18, 2026")
                            )
                        ),
                        EduCourse(
                            id = "gemini_ai",
                            title = "Integrating Gemini AI in Kotlin Mobile Apps",
                            description = "Leverage prompt engineering, generative multimodal content, and streaming responses with safety settings directly inside Android Apps.",
                            category = "AI & ML",
                            instructorName = "Dennis Ritchie",
                            instructorTitle = "Lead Google AI Advocate",
                            duration = "3h 20m",
                            difficulty = "Intermediate",
                            rating = 5.0f,
                            studentsEnrolled = 6430,
                            color = Color(0xFF3388FF),
                            lectures = listOf(
                                EduLecture("ga_lec_1", "Gemini 1.5 Flash Model Overview", "14:50", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"),
                                EduLecture("ga_lec_2", "API Key Security & Integration", "19:15", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"),
                                EduLecture("ga_lec_3", "Designing Multimodal Prompts", "28:30", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                            ),
                            reviews = listOf(
                                EduReview("Taskin Ahmed", 5.0f, "Incredible! Taught me how to use Gemini API without compromising safety settings.", "June 14, 2026")
                            )
                        ),
                        EduCourse(
                            id = "room_db",
                            title = "Offline Persistence with Room Database",
                            description = "Architect reliable SQLite database structures, TypeConverters, multi-table joins, migrations, and automatic stream synchronization.",
                            category = "Design",
                            instructorName = "Don Norman",
                            instructorTitle = "Database & Architecture Lead",
                            duration = "5h 10m",
                            difficulty = "Intermediate",
                            rating = 4.7f,
                            studentsEnrolled = 11040,
                            color = Color(0xFF33FF55),
                            lectures = listOf(
                                EduLecture("rd_lec_1", "Database Schemas & Entity Annotations", "15:40", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
                                EduLecture("rd_lec_2", "Building DAOs & Type Converters", "22:10", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4")
                            ),
                            reviews = listOf(
                                EduReview("Faisal Karim", 4.6f, "Very structured. The schema migration part solved my production bugs.", "February 22, 2026")
                            )
                        ),
                        EduCourse(
                            id = "growth_marketing",
                            title = "Growth Marketing & Tech Strategy",
                            description = "Learn user acquisition loops, app store optimization (ASO), push notification conversion funnels, and retention frameworks.",
                            category = "Business",
                            instructorName = "Seth Godin",
                            instructorTitle = "Product Strategist & Author",
                            duration = "2h 45m",
                            difficulty = "Beginner",
                            rating = 4.6f,
                            studentsEnrolled = 3980,
                            color = Color(0xFFFFD700),
                            lectures = listOf(
                                EduLecture("gm_lec_1", "Product-led Growth in Modern Mobile Apps", "11:20", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"),
                                EduLecture("gm_lec_2", "Conversion Funnel Optimization", "24:55", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4")
                            ),
                            reviews = listOf(
                                EduReview("Alex Mercer", 4.8f, "Seth Godin never disappoints. Best business strategic view for developers.", "January 11, 2026")
                            )
                        )
                    )
                    for (course in defaultCourses) {
                        saveEduCourseToFirestore(db, course)
                    }
                } else {
                    Log.d(TAG, "eduCourses collection already populated.")
                }
            }
            
        Log.i(TAG, "Completed Firestore database schema and layout checks!")
    }

    fun loadEduCoursesFromFirestore(db: FirebaseFirestore, onLoaded: (List<EduCourse>?) -> Unit) {
        db.collection("eduCourses").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onLoaded(null)
                    return@addOnSuccessListener
                }
                val list = mutableListOf<EduCourse>()
                for (doc in snapshot.documents) {
                    try {
                        val id = doc.id
                        val title = doc.getString("title") ?: ""
                        val description = doc.getString("description") ?: ""
                        val category = doc.getString("category") ?: ""
                        val instructorName = doc.getString("instructorName") ?: ""
                        val instructorTitle = doc.getString("instructorTitle") ?: ""
                        val duration = doc.getString("duration") ?: ""
                        val difficulty = doc.getString("difficulty") ?: ""
                        val rating = doc.getDouble("rating")?.toFloat() ?: 5f
                        val studentsEnrolled = doc.getLong("studentsEnrolled")?.toInt() ?: 0
                        val colorHex = doc.getString("colorHex") ?: "#FF0055"
                        val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch(e: Exception) { Color(0xFFFF0055) }
                        
                        val lecturesList = (doc.get("lectures") as? List<Map<String, Any>>)?.map { lecMap ->
                            val materialsList = (lecMap["materials"] as? List<Map<String, Any>>)?.map { matMap ->
                                EduMaterial(
                                    id = matMap["id"] as? String ?: "",
                                    name = matMap["name"] as? String ?: "",
                                    url = matMap["url"] as? String ?: "",
                                    type = matMap["type"] as? String ?: "PDF"
                                )
                            } ?: emptyList()
                            
                            EduLecture(
                                id = lecMap["id"] as? String ?: "",
                                title = lecMap["title"] as? String ?: "",
                                duration = lecMap["duration"] as? String ?: "",
                                videoUrl = lecMap["videoUrl"] as? String ?: "",
                                pointsAwarded = (lecMap["pointsAwarded"] as? Long)?.toInt() ?: 15,
                                isCompleted = lecMap["isCompleted"] as? Boolean ?: false,
                                materials = materialsList
                            )
                        } ?: emptyList()

                        val reviewsList = (doc.get("reviews") as? List<Map<String, Any>>)?.map { revMap ->
                            EduReview(
                                studentName = revMap["studentName"] as? String ?: "",
                                rating = (revMap["rating"] as? Double)?.toFloat() ?: 5f,
                                comment = revMap["comment"] as? String ?: "",
                                date = revMap["date"] as? String ?: ""
                            )
                        } ?: emptyList()

                        val isEnrolled = doc.getBoolean("isEnrolled") ?: false
                        val completedPercent = doc.getDouble("completedPercent")?.toFloat() ?: 0f
                        val certificateEarned = doc.getBoolean("certificateEarned") ?: false
                        val personalNotes = doc.getString("personalNotes") ?: ""

                        list.add(
                            EduCourse(
                                id = id,
                                title = title,
                                description = description,
                                category = category,
                                instructorName = instructorName,
                                instructorTitle = instructorTitle,
                                duration = duration,
                                difficulty = difficulty,
                                rating = rating,
                                studentsEnrolled = studentsEnrolled,
                                color = color,
                                lectures = lecturesList,
                                reviews = reviewsList,
                                isEnrolled = isEnrolled,
                                completedPercent = completedPercent,
                                certificateEarned = certificateEarned,
                                personalNotes = personalNotes
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing course: ${e.message}", e)
                    }
                }
                onLoaded(list)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading courses from Firestore: ${e.message}", e)
                onLoaded(null)
            }
    }

    fun saveEduCourseToFirestore(db: FirebaseFirestore, course: EduCourse, onComplete: (Boolean) -> Unit = {}) {
        val colorHex = try {
            String.format("#%06X", 0xFFFFFF and course.color.value.toLong().toInt())
        } catch (e: Exception) {
            "#FF0055"
        }
        val courseMap = hashMapOf(
            "title" to course.title,
            "description" to course.description,
            "category" to course.category,
            "instructorName" to course.instructorName,
            "instructorTitle" to course.instructorTitle,
            "duration" to course.duration,
            "difficulty" to course.difficulty,
            "rating" to course.rating.toDouble(),
            "studentsEnrolled" to course.studentsEnrolled,
            "colorHex" to colorHex,
            "isEnrolled" to course.isEnrolled,
            "completedPercent" to course.completedPercent.toDouble(),
            "certificateEarned" to course.certificateEarned,
            "personalNotes" to course.personalNotes,
            "lectures" to course.lectures.map { lec ->
                hashMapOf(
                    "id" to lec.id,
                    "title" to lec.title,
                    "duration" to lec.duration,
                    "videoUrl" to lec.videoUrl,
                    "pointsAwarded" to lec.pointsAwarded,
                    "isCompleted" to lec.isCompleted,
                    "materials" to lec.materials.map { mat ->
                        hashMapOf(
                            "id" to mat.id,
                            "name" to mat.name,
                            "url" to mat.url,
                            "type" to mat.type
                        )
                    }
                )
            },
            "reviews" to course.reviews.map { rev ->
                hashMapOf(
                    "studentName" to rev.studentName,
                    "rating" to rev.rating.toDouble(),
                    "comment" to rev.comment,
                    "date" to rev.date
                )
            }
        )

        db.collection("eduCourses").document(course.id).set(courseMap)
            .addOnSuccessListener {
                Log.d(TAG, "Course ${course.title} saved to Firestore successfully!")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving course ${course.title} to Firestore: ${e.message}", e)
                onComplete(false)
            }
    }

    fun saveUserCourseProgressToFirestore(
        db: FirebaseFirestore,
        userId: String,
        courseId: String,
        isEnrolled: Boolean,
        completedLectures: List<String>,
        completedPercent: Float,
        certificateEarned: Boolean,
        personalNotes: String
    ) {
        val progressMap = hashMapOf(
            "isEnrolled" to isEnrolled,
            "completedLectures" to completedLectures,
            "completedPercent" to completedPercent.toDouble(),
            "certificateEarned" to certificateEarned,
            "personalNotes" to personalNotes,
            "updatedAt" to Timestamp.now()
        )
        db.collection("users").document(userId)
            .collection("courseProgress").document(courseId)
            .set(progressMap)
            .addOnSuccessListener {
                Log.d(TAG, "User progress for course $courseId saved successfully!")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving user progress for course $courseId: ${e.message}")
            }
    }

    fun loadUserCourseProgressFromFirestore(
        db: FirebaseFirestore,
        userId: String,
        onLoaded: (Map<String, UserCourseProgress>) -> Unit
    ) {
        db.collection("users").document(userId)
            .collection("courseProgress").get()
            .addOnSuccessListener { snapshot ->
                val progressMap = mutableMapOf<String, UserCourseProgress>()
                for (doc in snapshot.documents) {
                    val courseId = doc.id
                    val isEnrolled = doc.getBoolean("isEnrolled") ?: false
                    val completedLectures = (doc.get("completedLectures") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val completedPercent = doc.getDouble("completedPercent")?.toFloat() ?: 0f
                    val certificateEarned = doc.getBoolean("certificateEarned") ?: false
                    val personalNotes = doc.getString("personalNotes") ?: ""
                    
                    progressMap[courseId] = UserCourseProgress(
                        courseId = courseId,
                        isEnrolled = isEnrolled,
                        completedLectures = completedLectures,
                        completedPercent = completedPercent,
                        certificateEarned = certificateEarned,
                        personalNotes = personalNotes
                    )
                }
                onLoaded(progressMap)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading user course progress: ${e.message}")
                onLoaded(emptyMap())
            }
    }
}

data class UserCourseProgress(
    val courseId: String,
    val isEnrolled: Boolean,
    val completedLectures: List<String>,
    val completedPercent: Float,
    val certificateEarned: Boolean,
    val personalNotes: String
)


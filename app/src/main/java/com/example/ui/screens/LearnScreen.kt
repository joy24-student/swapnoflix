package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.StreamingViewModel
import com.example.data.EduCourse
import com.example.data.EduLecture
import com.example.data.EduReview
import com.example.data.EduMaterial
import com.example.ui.theme.LocalAppColors

// Define quiz questions for courses
data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val pointsAwarded: Int
)

// Helper to return high-fidelity cover image urls matching requested design
fun getCourseThumbnailUrl(courseId: String): String {
    return when (courseId) {
        "android_compose" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuCf7AV3lHuWojdcqgF3UAwmaWQs2pw_sA2E47oxb53zVw_mijXBiEOC5fymj4qWNhAOTudbvzxSo6HEXqUoyAqYg0foJ-lC9ALqoyX5OhYWW3oeH_w2b2zFJNYmuru-yf9E4AHP9fdLyjpMjPq5_kd9eITLdogBc1nhAVPljAmvkRLGr0D0TSNSgX984gSxKiUC_winjZlRaxUEXt_xduKoEgd_NAgBbJSvrpn30JrkgQ4SxSPNn21mcq6tutIENccZXkR8b6bfi2E"
        "kotlin_advanced" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDHTyj2dKZPfDHerTRosk3Fb5TGD-bWfk7G12tKMFpFKuIvJdRo_9Fg0jgeIrc3xLJ1hEbvh8Km-5PP4aQ_LcznL8qKKfycFtrtTHMPvL1yDQX-hePxPS_DbArwZYZi0ltYnZotRBftjMtwMkevog0mrG_Eka9UnyQZI3LD_ewyJgJLOyz4ggZO4WWjTMRYy9TB1x4LYjivSoFdtEWnNRR6HZqpjgW-jZ20mkc57cu01Wp1vjQoLkFbBvDaxkt6X8QQ7nwzqXnPkEw"
        "gemini_ai" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuCE4U3J9umYb4_edWxtTKT-mRwULgn0SDdNSHEOzact5MX7srFgaSOLfJa88VCO4YxGNRCO8izVAb8mrdR-F8vxIrWvwfeHgsa90OV6Wj8FLqmlPPdAlk0eXmMQ9BDNtyYS1WrqrJlH3P20Bm1DDTx516D-su9XTkkYbGlOIQ7tKw76Safug_G1wEtnTUxbLHAfnoCL41g0GEKcI7jQphhQZ_0qAykJ4my4bZesMyIw6psvQ5Bdz2ltnlhsnwa4Q90w9oMTnSiJ-X8"
        "room_db" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuD-Bg2qqA_7AqI6MoyIj-uZ0A8mFNgoaMoZlWEq-oG70Ufz56jSaab5XZ0RfV29iFxPR8BZBIlINn-Gz06BF0gioJ6AigM-JsgWfZuoUDjj1Pdk3HncZ4eW3Alfwm43ltuT4KK8t0woRkqZdBoOKyNhfzr-rhIfgRoxKcNe8zpb11lHd5o_Y2gXPSfc1NLIR0MROuEksBfUjj6ywIsQJd3wz_pLxJdVmJ4bFwBDUsE_IuU-vBdjcbLQf_fiwfF4xfOIzsXl09EjYx0"
        "growth_marketing" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuC-gilF7J-f2NwmV3zm4hmovhMSmJ1fdeexqJS-FwMuVLF9AT96-i03x7qHi6GPMMBMZ512x8s491JomkWHsxvT8HCUYT46JJEMsJKAJN8L5cFZbp4iFLqiGOa1iPw3poQReSUja0iUJI3sfL34r2AIp-vVOqzJJD834xaRyFjOL61Hf6ts74Ub9vftYD92DFvd9zey-JdOEGGUBueSLcMt-h4Vfmccj2NbBXs-CWNtcbtwDoif6a0GogCNeUEXKscXpVCnFlmnk8o"
        else -> "https://lh3.googleusercontent.com/aida-public/AB6AXuBh2VlN1NBeKj_qiG1jDK8VnvpU5rdonqep4w-T92zBbCTbe2yRF5OHfEAJR6dUeyjGde5JzxunSoFDtwcVJ6PsM6P-PTcyWfw4dBthPMYhTEq3UbAEbqwX9qjXilXmcaM058YIh4UkDFJb_IHz05fUgvPpFKWe1mjPkuRy0l99zKp1KRb2aS4QglK7q23NUhcQg8CyDPk8DVYHtRS7E7OFrlYyO3GT-LHoZLzGvw4m-cgFT5qRAapY-SHMIKf_Swos9HfOJ16M9zU"
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(viewModel: StreamingViewModel) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    val rewardPoints by viewModel.userRewardPoints.collectAsState()
    val eduCoursesList by viewModel.eduCourses.collectAsState()

    // Screen-level Main tabs: Explore (Swapnoflix LMS), My Learning, Quizzes & Docs
    var activeMainTab by remember { mutableStateOf("Explore") }
    var activeCategoryFilter by remember { mutableStateOf("All") } // All, Development, AI & ML, Design, Business
    
    // My Learning sub-tab navigation
    var myLearningSubTab by remember { mutableStateOf("In Progress") } // In Progress, Completed, Wishlist

    // Wishlist local state storage (simple runtime persistent bookmark engine)
    var wishlistCourseIds by remember { mutableStateOf(setOf<String>()) }

    // Interactivity dialogues toggles
    var selectedSyllabusCourseId by remember { mutableStateOf<String?>(null) }
    var activeClassroomCourseId by remember { mutableStateOf<String?>(null) }
    var showCertificateCourseId by remember { mutableStateOf<String?>(null) }
    var claimedCertificates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showRoutinePlannerDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // State for interactive quiz dialog
    var activeQuizCourseId by remember { mutableStateOf<String?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var answerSubmitted by remember { mutableStateOf(false) }
    var scoreAwardedInQuiz by remember { mutableStateOf(0) }

    // Local Planner state backed by Supabase
    val routinePlans by viewModel.routinePlans.collectAsState()
    val activePlan = remember(routinePlans) { routinePlans.find { it.is_active } }

    var selectedStudyDays by remember { mutableStateOf(setOf("Mon", "Wed", "Fri")) }
    var dailyTargetStudyHours by remember { mutableStateOf(2) }

    LaunchedEffect(activePlan) {
        activePlan?.let {
            val days = it.title.split(",").filter { day -> day.isNotEmpty() }.toSet()
            if (days.isNotEmpty()) {
                selectedStudyDays = days
            }
            dailyTargetStudyHours = it.description?.toIntOrNull() ?: 2
        }
    }

    // Active studies hours & stats
    val completedLessonsCount = eduCoursesList.flatMap { it.lectures }.count { it.isCompleted }
    val totalStudyHours = (completedLessonsCount * 0.4f) + 1.2f

    // Quiz questions dataset mapped to match course IDs
    val quizzes = remember {
        mapOf(
            "android_compose" to listOf(
                QuizQuestion(
                    1,
                    "What is the key advantage of Jetpack Compose over XML layouts?",
                    listOf("It is written in legacy XML templates.", "It uses declarative Kotlin code to rebuild UI dynamically.", "It doesn't support structured state management.", "It requires an emulator to compile."),
                    1,
                    25
                ),
                QuizQuestion(
                    2,
                    "Which Composable function is typically used to stack views vertically?",
                    listOf("Row", "Box", "Column", "Scaffold"),
                    2,
                    25
                )
            ),
            "kotlin_advanced" to listOf(
                QuizQuestion(
                    1,
                    "Which builder is commonly used to launch a new coroutine without blocking?",
                    listOf("runBlocking", "launch", "async", "withContext"),
                    1,
                    30
                ),
                QuizQuestion(
                    2,
                    "What does StateFlow emit to its active subscribers?",
                    listOf("Only the final processed exception.", "A list of all historical logs.", "The current state and subsequent state updates.", "Nothing unless triggered by system broadcast."),
                    2,
                    30
                )
            ),
            "gemini_ai" to listOf(
                QuizQuestion(
                    1,
                    "Which model is best suited for fast, cost-effective mobile multimodal tasks?",
                    listOf("Gemini 1.5 Flash", "Gemini 1.5 Pro", "Gemini Ultra", "Legacy GPT-3"),
                    0,
                    25
                ),
                QuizQuestion(
                    2,
                    "How should API credentials be securely managed in Android production?",
                    listOf("Hardcoded in MainActivity launcher strings.", "Saved in plain text strings.xml.", "Managed via local.properties on Git repository.", "Injected via secrets-gradle-plugin with environment variables."),
                    3,
                    35
                )
            ),
            "room_db" to listOf(
                QuizQuestion(
                    1,
                    "What annotation is required to mark a class as a SQLite database table in Room?",
                    listOf("@Database", "@Table", "@Entity", "@PrimaryKey"),
                    2,
                    25
                ),
                QuizQuestion(
                    2,
                    "What type of return value allows Room queries to automatically emit live updates?",
                    listOf("StateFlow / Flow", "String", "Completable", "Plain ArrayList"),
                    0,
                    25
                )
            )
        )
    }

    Scaffold(
        containerColor = Color(0xFF131313),
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF0F0F0F))) {
                ScrollableTabRow(
                    selectedTabIndex = when (activeMainTab) {
                        "Explore" -> 0
                        "My Learning" -> 1
                        "Quizzes & Docs" -> 2
                        else -> 0
                    },
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[when (activeMainTab) {
                                "Explore" -> 0
                                "My Learning" -> 1
                                "Quizzes & Docs" -> 2
                                else -> 0
                            }]),
                            color = Color(0xFFE50914)
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = activeMainTab == "Explore",
                        onClick = { activeMainTab = "Explore" },
                        text = { Text("Explore", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeMainTab == "My Learning",
                        onClick = { activeMainTab = "My Learning" },
                        text = { Text("My Learning", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeMainTab == "Quizzes & Docs",
                        onClick = { activeMainTab = "Quizzes & Docs" },
                        text = { Text("Quizzes & Resources", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search course...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.DarkGray
                    )
                )
            }

            when (activeMainTab) {
                "Explore" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF1E1E24), Color(0xFF2C2C35))
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { showRoutinePlannerDialog = true }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color(0xFFF59E0B).copy(alpha = 0.15f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EditCalendar,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Make Routine",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Create and customize your daily study schedule and classroom routines.",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Top Enrolled Courses", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { activeCategoryFilter = "All" }) {
                                        Text("View All", color = Color(0xFFE50914), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                val topCourses = eduCoursesList.sortedByDescending { it.studentsEnrolled }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(topCourses) { course ->
                                        Box(
                                            modifier = Modifier
                                                .width(180.dp)
                                                .clickable { selectedSyllabusCourseId = course.id }
                                        ) {
                                            Column {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(16f / 9f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.Black)
                                                ) {
                                                    AsyncImage(
                                                        model = getCourseThumbnailUrl(course.id),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    if (course.isEnrolled) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .align(Alignment.BottomCenter)
                                                                .background(Color.Black.copy(0.6f))
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            LinearProgressIndicator(
                                                                progress = course.completedPercent,
                                                                color = Color(0xFFE50914),
                                                                trackColor = Color(0xFF2C2C2E),
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(3.dp)
                                                                    .clip(CircleShape)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = course.title,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 15.sp
                                                )

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Text(
                                                    text = course.instructorName,
                                                    color = Color(0xFFA1A1AA),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF131313))
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("All", "Development", "AI & ML", "Design", "Business").forEach { category ->
                                        val isSelected = category == activeCategoryFilter
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) Color(0xFFE50914) else Color(0xFF1C1C1E))
                                                .clickable { activeCategoryFilter = category }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = category,
                                                color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Recommended For You",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }

                        val filteredCourses = eduCoursesList.filter { course ->
                            val matchCat = activeCategoryFilter == "All" || course.category == activeCategoryFilter
                            val matchSearch = searchQuery.isEmpty() ||
                                    course.title.contains(searchQuery, ignoreCase = true) ||
                                    course.instructorName.contains(searchQuery, ignoreCase = true)
                            matchCat && matchSearch
                        }

                        if (filteredCourses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No matching courses found.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(filteredCourses) { course ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                                        .clickable { selectedSyllabusCourseId = course.id }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 110.dp, height = 64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = getCourseThumbnailUrl(course.id),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(24.dp)
                                                .background(Color.Black.copy(0.4f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = course.title,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 15.sp
                                        )
                                        Text(
                                            text = course.instructorName,
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )

                                        if (course.isEnrolled) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${(course.completedPercent * 100).toInt()}% complete",
                                                    color = Color(0xFFE50914),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Lessons: ${course.lectures.count { it.isCompleted }}/${course.lectures.size}",
                                                    color = Color.Gray,
                                                    fontSize = 9.sp
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = course.completedPercent,
                                                color = Color(0xFFE50914),
                                                trackColor = Color(0xFF2C2C2E),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .padding(top = 4.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(course.difficulty, color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Text(course.duration, color = Color.Gray, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Text(
                                    text = "More Courses",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )

                                val chunkedCourses = eduCoursesList.chunked(2)
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    chunkedCourses.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowItems.forEach { course ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                                                        .clickable { selectedSyllabusCourseId = course.id }
                                                        .padding(8.dp)
                                                ) {
                                                    Column {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(16f / 9f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.Black)
                                                        ) {
                                                            AsyncImage(
                                                                model = getCourseThumbnailUrl(course.id),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            course.title,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            course.instructorName,
                                                            color = Color(0xFFA1A1AA),
                                                            fontSize = 9.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Filled.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                                                            Text(
                                                                " ${course.rating}",
                                                                color = Color(0xFFA1A1AA),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (rowItems.size < 2) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "My Learning" -> {
                    // ── TODAY'S DAY CHECK ──────────────────────────────────────
                    val todayDayShort = java.text.SimpleDateFormat("EEE", java.util.Locale.US)
                        .format(java.util.Date()) // e.g. "Mon", "Wed"
                    val isStudyDay = selectedStudyDays.contains(todayDayShort)
                    val enrolledCourses = eduCoursesList.filter { it.isEnrolled }
                    val inProgressCourses = enrolledCourses.filter { it.completedPercent < 1.0f }
                    val completedCourses = enrolledCourses.filter { it.completedPercent >= 1.0f }
                    val todayCourses = inProgressCourses.take(dailyTargetStudyHours)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {

                        // A. Stats Bar
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    Triple(enrolledCourses.size.toString(), "Enrolled", Color(0xFF6366F1)),
                                    Triple(inProgressCourses.size.toString(), "In Progress", Color(0xFFEC4899)),
                                    Triple(completedCourses.size.toString(), "Completed", Color(0xFF22C55E))
                                ).forEach { (count, label, color) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(color.copy(0.1f), RoundedCornerShape(12.dp))
                                            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(count, color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                            Text(label, color = Color(0xFFA1A1AA), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // B. TODAY'S SCHEDULE CARD
                        item {
                            if (isStudyDay && inProgressCourses.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF1E0A3C), Color(0xFF2D1155))
                                            ),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .border(1.dp, Color(0xFF7C3AED).copy(0.5f), RoundedCornerShape(18.dp))
                                        .padding(18.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    "Today's Schedule 📅",
                                                    color = Color(0xFFE9D5FF),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "${inProgressCourses.size} course(s) planned • $todayDayShort",
                                                    color = Color(0xFFA78BFA),
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF7C3AED).copy(0.2f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    "${dailyTargetStudyHours}h target",
                                                    color = Color(0xFFA78BFA),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(14.dp))
                                        todayCourses.forEach { course ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 10.dp)
                                                    .background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))
                                                    .clickable { activeClassroomCourseId = course.id }
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(Color(0xFF7C3AED).copy(0.3f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Filled.PlayArrow,
                                                        null,
                                                        tint = Color(0xFFE9D5FF),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        course.title,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        "${(course.completedPercent * 100).toInt()}% complete",
                                                        color = Color(0xFFA78BFA),
                                                        fontSize = 10.sp
                                                    )
                                                    LinearProgressIndicator(
                                                        progress = course.completedPercent,
                                                        color = Color(0xFF7C3AED),
                                                        trackColor = Color.White.copy(0.1f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(3.dp)
                                                            .padding(top = 4.dp)
                                                            .clip(CircleShape)
                                                    )
                                                }
                                                Text(
                                                    "▶ Start",
                                                    color = Color(0xFFA78BFA),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (!isStudyDay) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🌙", fontSize = 32.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Rest Day", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Today ($todayDayShort) is a rest day. Enjoy your break!",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        TextButton(onClick = { showRoutinePlannerDialog = true }) {
                                            Text("Edit Schedule →", color = Color(0xFFE50914), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // C. Sub-Tab Row: In Progress / Completed / Wishlist
                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF131313))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("In Progress", "Completed", "Wishlist").forEach { tab ->
                                    val selected = myLearningSubTab == tab
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (selected) Color(0xFFE50914) else Color(0xFF1C1C1E))
                                            .border(1.dp, if (selected) Color.Transparent else Color(0xFF2C2C2E), RoundedCornerShape(20.dp))
                                            .clickable { myLearningSubTab = tab }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(tab, color = if (selected) Color.White else Color(0xFFA1A1AA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // D. Course Cards for active sub-tab
                        val displayCourses = when (myLearningSubTab) {
                            "In Progress" -> inProgressCourses
                            "Completed" -> completedCourses
                            "Wishlist" -> eduCoursesList.filter { wishlistCourseIds.contains(it.id) }
                            else -> inProgressCourses
                        }

                        if (displayCourses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            when (myLearningSubTab) {
                                                "In Progress" -> "📚"
                                                "Completed" -> "🏆"
                                                else -> "❤️"
                                            },
                                            fontSize = 40.sp
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            when (myLearningSubTab) {
                                                "In Progress" -> "No courses in progress.\nBrowse the Explore tab to enroll!"
                                                "Completed" -> "No completed courses yet.\nKeep learning — you're getting there!"
                                                else -> "No wishlist items.\nTap ♥ on any course to save it!"
                                            },
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        if (myLearningSubTab == "In Progress") {
                                            Spacer(Modifier.height(12.dp))
                                            Button(
                                                onClick = { activeMainTab = "Explore" },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Browse Courses", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            items(displayCourses) { course ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
                                        .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(14.dp))
                                        .clickable { activeClassroomCourseId = course.id }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(width = 100.dp, height = 60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = getCourseThumbnailUrl(course.id),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (myLearningSubTab == "Completed") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF22C55E).copy(0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                            }
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            course.title,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 15.sp
                                        )
                                        Text(course.instructorName, color = Color(0xFFA1A1AA), fontSize = 10.sp)
                                        Spacer(Modifier.height(6.dp))
                                        if (myLearningSubTab != "Completed") {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "${(course.completedPercent * 100).toInt()}% complete",
                                                    color = Color(0xFFE50914),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "${course.lectures.count { it.isCompleted }}/${course.lectures.size} lessons",
                                                    color = Color.Gray,
                                                    fontSize = 9.sp
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = course.completedPercent,
                                                color = Color(0xFFE50914),
                                                trackColor = Color(0xFF2C2C2E),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(12.dp))
                                                Text("Course completed! 🎓", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    // Action button
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        if (myLearningSubTab == "Completed" && !claimedCertificates.contains(course.id)) {
                                            IconButton(onClick = { showCertificateCourseId = course.id }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Filled.WorkspacePremium, null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                                            }
                                        } else if (myLearningSubTab == "Wishlist") {
                                            IconButton(
                                                onClick = { wishlistCourseIds = wishlistCourseIds - course.id },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Filled.Favorite, null, tint = Color(0xFFE50914), modifier = Modifier.size(20.dp))
                                            }
                                        } else {
                                            Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFFE50914), modifier = Modifier.size(22.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // E. Routine Planner Summary Footer
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                                    .clickable { showRoutinePlannerDialog = true }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Study Routine", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Days: ${selectedStudyDays.joinToString(", ")} • ${dailyTargetStudyHours}h/day",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE50914).copy(0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Edit →", color = Color(0xFFE50914), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                "Quizzes & Docs" -> {
                    // QUIZZES AND DOWNLOADABLE CHEAT SHEETS
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp)
                    ) {
                        item {
                            Text(
                                text = "Active Interactive Quizzes",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Lists available quizzes
                        items(eduCoursesList) { course ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                                    .clickable {
                                        activeQuizCourseId = course.id
                                        currentQuestionIndex = 0
                                        selectedAnswerIndex = null
                                        answerSubmitted = false
                                        scoreAwardedInQuiz = 0
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(course.color.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Assignment,
                                            contentDescription = null,
                                            tint = course.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${course.title} Quiz",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Answer classwork review questions",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Start Quiz",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        item {
                            Text(
                                text = "LMS Resources & Cheat Sheets",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                        }

                        // Cheat sheets items
                        val resources = listOf(
                            "Kotlin Flow Quick Reference Guide" to "PDF • 240 KB",
                            "Jetpack Compose Standard Layout Cheat Sheet" to "Image • 1.2 MB",
                            "Room Database Annotation Cheat Sheet" to "PDF • 180 KB",
                            "Gemini Multimodal Safe API Prompt Guide" to "Markdown • 45 KB"
                        )

                        items(resources) { (resTitle, formatSize) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                                    .clickable {
                                        Toast.makeText(context, "Downloaded $resTitle to Offline cache!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(resTitle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(formatSize, color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Filled.CloudDownload,
                                    contentDescription = "Save Offline",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. COURSE SYLLABUS / UDEMY LANDING PAGE DIALOG
    val syllabusCourse = eduCoursesList.find { it.id == selectedSyllabusCourseId }
    if (syllabusCourse != null) {
        var activePreviewTab by remember(selectedSyllabusCourseId) { mutableStateOf("About") }
        val context = LocalContext.current

        Dialog(
            onDismissRequest = { selectedSyllabusCourseId = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = Color(0xFF0B0D12),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Video Header Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = getCourseThumbnailUrl(syllabusCourse.id),
                            contentDescription = "Course Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.6f)
                        )

                        // Top Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedSyllabusCourseId = null },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Share link copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (wishlistCourseIds.contains(syllabusCourse.id)) {
                                            wishlistCourseIds = wishlistCourseIds - syllabusCourse.id
                                            Toast.makeText(context, "Removed from Wishlist", Toast.LENGTH_SHORT).show()
                                        } else {
                                            wishlistCourseIds = wishlistCourseIds + syllabusCourse.id
                                            Toast.makeText(context, "Added to Wishlist", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (wishlistCourseIds.contains(syllabusCourse.id)) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Wishlist",
                                        tint = if (wishlistCourseIds.contains(syllabusCourse.id)) Color(0xFFD81E3D) else Color.White
                                    )
                                }
                            }
                        }

                        // Central Play Button
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Playing course intro trailer...", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play Intro",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Content Main Scrollable Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Course Meta
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = syllabusCourse.title,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp
                            )
                            Text(
                                text = syllabusCourse.instructorName,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Rating",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${syllabusCourse.rating}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "(${syllabusCourse.studentsEnrolled})",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = syllabusCourse.duration,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = syllabusCourse.difficulty,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Call to Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!syllabusCourse.isEnrolled) {
                                        viewModel.enrollInCourse(syllabusCourse.id)
                                        Toast.makeText(context, "Enrolled in ${syllabusCourse.title}! Good Luck!", Toast.LENGTH_LONG).show()
                                    }
                                    selectedSyllabusCourseId = null
                                    activeClassroomCourseId = syllabusCourse.id
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD81E3D)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Text(
                                    text = if (syllabusCourse.isEnrolled) "Continue Learning" else "Enroll & Start",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (wishlistCourseIds.contains(syllabusCourse.id)) {
                                        wishlistCourseIds = wishlistCourseIds - syllabusCourse.id
                                        Toast.makeText(context, "Removed from Wishlist", Toast.LENGTH_SHORT).show()
                                    } else {
                                        wishlistCourseIds = wishlistCourseIds + syllabusCourse.id
                                        Toast.makeText(context, "Added to Wishlist", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(
                                    imageVector = if (wishlistCourseIds.contains(syllabusCourse.id)) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (wishlistCourseIds.contains(syllabusCourse.id)) Color(0xFFFFD700) else Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (wishlistCourseIds.contains(syllabusCourse.id)) "Wishlisted" else "Wishlist",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Tabs Navigation Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            listOf("About", "Curriculum", "Instructor", "Reviews").forEach { tab ->
                                val isSelected = activePreviewTab == tab
                                Column(
                                    modifier = Modifier
                                        .clickable { activePreviewTab = tab }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = tab,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(2.dp)
                                            .width(48.dp)
                                            .background(if (isSelected) Color(0xFFD81E3D) else Color.Transparent)
                                    )
                                }
                            }
                        }

                        // Tab Contents
                        when (activePreviewTab) {
                            "About" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        "About this course",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = syllabusCourse.description,
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )

                                    // Tag list
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("Development", syllabusCourse.category, "Full Stack").forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF1A1C23), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    tag,
                                                    color = Color.Gray,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Features List
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        FeatureRow(
                                            icon = Icons.Default.AccessTime,
                                            title = syllabusCourse.duration,
                                            subtitle = "On-demand video"
                                        )
                                        FeatureRow(
                                            icon = Icons.Default.Laptop,
                                            title = "${syllabusCourse.lectures.size * 3 + 20}+ Lectures",
                                            subtitle = "Full lifetime access"
                                        )
                                        FeatureRow(
                                            icon = Icons.Default.TrendingUp,
                                            title = syllabusCourse.difficulty,
                                            subtitle = "Beginner to Advanced"
                                        )
                                        FeatureRow(
                                            icon = Icons.Default.Verified,
                                            title = "Certificate of completion",
                                            subtitle = "Shareable certificate"
                                        )
                                    }

                                    // Rating Summary Card
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1A1C23), RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFFD700), RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "Bestseller",
                                                    color = Color.Black,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "#1 in ${syllabusCourse.category}",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    "${syllabusCourse.rating}",
                                                    color = Color.White,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row {
                                                    repeat(5) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Star,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFFD700),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                "${syllabusCourse.studentsEnrolled} Ratings",
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                            "Curriculum" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Curriculum Preview (${syllabusCourse.lectures.size} Lessons)",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    syllabusCourse.lectures.forEachIndexed { idx, lecture ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayCircleOutline,
                                                    contentDescription = null,
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "${idx + 1}. ${lecture.title}",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                lecture.duration,
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                            "Instructor" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        "Your Instructor",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .background(syllabusCourse.color.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = syllabusCourse.instructorName.take(2).uppercase(),
                                                color = syllabusCourse.color,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                        Column {
                                            Text(
                                                syllabusCourse.instructorName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                syllabusCourse.instructorTitle,
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Angela is a lead instructor at the London App Brewery. She has helped over a million students learn to code and improve their lives by becoming a developer. Her courses are known for being fun, engaging, and extremely thorough.",
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                            "Reviews" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Student Reviews",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    listOf(
                                        Triple("Alex M.", 5, "Amazing course! Explains core concepts in-depth. Jetpack Compose parts are super helpful."),
                                        Triple("Sarah T.", 4, "Extremely structured and engaging curriculum. Highly recommend it to beginners and intermediate devs alike."),
                                        Triple("David K.", 5, "Best bootcamp on the internet. Dr. Angela's explanations are unmatched.")
                                    ).forEach { (user, stars, comment) ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    user,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Row {
                                                    repeat(stars) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Star,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFFD700),
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                comment,
                                                color = Color.Gray,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
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
    }

    // 2. FULL UDEMY VIRTUAL CLASSROOM & STREAMING VIDEO CENTER
    val classroomCourse = eduCoursesList.find { it.id == activeClassroomCourseId }
    if (classroomCourse != null) {
        LaunchedEffect(activeClassroomCourseId) {
            // load enrollments and progress for this course when opened
            viewModel.loadEnrollmentsFromSupabase()
            // find enrollment and load progress
            val enroll = viewModel.courseEnrollments.value.find { it.course_id == classroomCourse.id }
            if (enroll != null) viewModel.loadCourseProgressFromSupabase(enroll.id)
        }
        var selectedLectureIndex by remember(activeClassroomCourseId) { mutableStateOf(0) }
        var classroomTab by remember { mutableStateOf("Lessons") } // Lessons, Notes, Reviews
        var localNotesState by remember(activeClassroomCourseId) { mutableStateOf(classroomCourse.personalNotes) }
        var isVideoPlaying by remember { mutableStateOf(true) }

        // Review states
        var ratingInput by remember { mutableStateOf(5f) }
        var commentInput by remember { mutableStateOf("") }

        val activeLecture = classroomCourse.lectures.getOrNull(selectedLectureIndex)

        Dialog(onDismissRequest = { activeClassroomCourseId = null }) {
            Surface(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Close Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(classroomCourse.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { activeClassroomCourseId = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Simulated Video Player Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        if (activeLecture != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Bitrate telemetry bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).background(if (isVideoPlaying) Color(0xFF00FFC4) else Color.Red, CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isVideoPlaying) "HLS LIVE STREAM" else "PAUSED", color = Color(0xFF00FFC4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("1080p • 60fps • 4.8 Mbps", color = Color.Gray, fontSize = 9.sp)
                                }

                                // Centered Playback overlay
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { isVideoPlaying = !isVideoPlaying },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isVideoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }

                                // Mark Complete Button
                                if (activeLecture != null) {
                                    Button(
                                        onClick = {
                                            // compute percent: (current completed + 1) / total
                                            val completedCount = classroomCourse.lectures.count { it.isCompleted }
                                            val willBe = if (activeLecture.isCompleted) completedCount else completedCount + 1
                                            val percent = willBe.toFloat() / classroomCourse.lectures.size.toFloat()
                                            viewModel.completeLectureInSupabase(classroomCourse.id, activeLecture.id, percent)
                                            Toast.makeText(context, "Marked lesson complete", Toast.LENGTH_SHORT).show()
                                            // reload courses to reflect updated completion
                                            viewModel.loadCoursesFromSupabase()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .padding(bottom = 8.dp)
                                            .background(Color.Transparent),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                    ) {
                                        Text(text = if (activeLecture.isCompleted) "Completed" else "Mark Lesson Complete", color = Color.Black)
                                    }
                                }

                                // Lecture Details overlay
                                Column {
                                    Text("Lesson ${selectedLectureIndex + 1}: ${activeLecture.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("URI: ${activeLecture.videoUrl}", color = Color.LightGray, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No video loaded.", color = Color.Gray)
                            }
                        }
                    }

                    // Progress indicator
                    LinearProgressIndicator(
                        progress = classroomCourse.completedPercent,
                        color = Color(0xFFE50914),
                        trackColor = Color(0xFF2C2C2E),
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    )

                    // Navigation Tabs within Classroom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C2E)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Lessons", "Materials", "Notes", "Feedback").forEach { tab ->
                            val isSelected = tab == classroomTab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { classroomTab = tab }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Tab contents
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF131313))
                            .padding(12.dp)
                    ) {
                        when (classroomTab) {
                            "Materials" -> {
                                val currentLecture = classroomCourse.lectures.getOrNull(selectedLectureIndex)
                                val materials = currentLecture?.materials ?: emptyList()
                                if (materials.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No supplementary files uploaded for this lecture.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(materials) { mat ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        Toast.makeText(context, "Opening resource: ${mat.name}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Description,
                                                        contentDescription = null,
                                                        tint = Color(0xFF10B981)
                                                    )
                                                    Column {
                                                        Text(mat.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text("File Type: ${mat.type}", color = Color.Gray, fontSize = 9.sp)
                                                    }
                                                }
                                                Icon(Icons.Filled.CloudDownload, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            "Lessons" -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(classroomCourse.lectures.size) { idx ->
                                        val lecture = classroomCourse.lectures[idx]
                                        val isCurrent = idx == selectedLectureIndex

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrent) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                                .clickable { selectedLectureIndex = idx }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.completeLecture(classroomCourse.id, lecture.id)
                                                        Toast.makeText(context, "Lesson completed! +15 Pts Wallet!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (lecture.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                                        contentDescription = "Complete",
                                                        tint = if (lecture.isCompleted) Color(0xFF33FF55) else Color.Gray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = "${idx + 1}. ${lecture.title}",
                                                        color = if (isCurrent) Color.White else Color.LightGray,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text("Duration: ${lecture.duration}", color = Color.Gray, fontSize = 9.sp)
                                                }
                                            }

                                            if (lecture.isCompleted) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("PASSED", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // Special claims certificate block
                                    if (classroomCourse.completedPercent >= 1.0f) {
                                        item {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFFFD700).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .border(BorderStroke(1.dp, Color(0xFFFFD700)), RoundedCornerShape(8.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Congratulations!", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text("You have completed 100% curriculum of ${classroomCourse.title}!", color = Color.White, fontSize = 10.sp)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            activeClassroomCourseId = null
                                                            showCertificateCourseId = classroomCourse.id
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Claim Cert", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "Notes" -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Personal Study Notes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = localNotesState,
                                        onValueChange = { localNotesState = it },
                                        placeholder = { Text("Jot down code snippets, architectural tips or lecture summaries...", color = Color.Gray, fontSize = 11.sp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF1C1C1E),
                                            unfocusedContainerColor = Color(0xFF1C1C1E),
                                            focusedBorderColor = Color(0xFFE50914),
                                            unfocusedBorderColor = Color(0xFF2C2C2E)
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.updateCourseNotes(classroomCourse.id, localNotesState)
                                            Toast.makeText(context, "Study notes saved to Cloud Notebook!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save Changes", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            "Feedback" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Submit Course Review & Star Feedback", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(5) { index ->
                                            val starRating = index + 1
                                            IconButton(onClick = { ratingInput = starRating.toFloat() }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = if (ratingInput >= starRating) Color(0xFFFFD700) else Color.Gray,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = commentInput,
                                        onValueChange = { commentInput = it },
                                        placeholder = { Text("Review comment (e.g. Best course ever!)", color = Color.Gray, fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF1C1C1E),
                                            unfocusedContainerColor = Color(0xFF1C1C1E),
                                            focusedBorderColor = Color(0xFFE50914),
                                            unfocusedBorderColor = Color(0xFF2C2C2E)
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            if (commentInput.trim().isNotEmpty()) {
                                                viewModel.addCourseReview(classroomCourse.id, ratingInput, commentInput, "You")
                                                commentInput = ""
                                                Toast.makeText(context, "Thank you! Feedback saved and uploaded to Firestore!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Please enter a comment", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Submit Review", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. SECURE DIGITAL CERTIFICATE PREVIEW DIALOG
    val certCourse = eduCoursesList.find { it.id == showCertificateCourseId }
    if (certCourse != null) {
        Dialog(onDismissRequest = { showCertificateCourseId = null }) {
            Surface(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD700)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0F12), RoundedCornerShape(12.dp))
                            .border(BorderStroke(2.dp, Color(0xFFFFD700).copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("SWAPNOFLIX LMS ACADEMY", color = Color(0xFFE50914), fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 2.sp)
                            Text("CERTIFICATE OF COMPLETION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .size(36.dp)
                                    .background(Color(0xFFFFD700).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                            }
                            
                            Text("This is proudly awarded to", color = Color.Gray, fontSize = 9.sp)
                            Text("STUDENT CHAMPION", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("for successfully finishing 100% curriculum of", color = Color.Gray, fontSize = 9.sp)
                            Text(certCourse.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                            
                            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(top = 10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("June 2026", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    Text("DATE AWARDED", color = Color.Gray, fontSize = 7.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Angela Yu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    Text("ACADEMY DIRECTOR", color = Color.Gray, fontSize = 7.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCertificateCourseId = null },
                            border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", color = Color.White)
                        }

                        Button(
                            onClick = {
                                claimedCertificates = claimedCertificates + certCourse.id
                                Toast.makeText(context, "Certificate shared to LinkedIn and Profile Successfully!", Toast.LENGTH_LONG).show()
                                showCertificateCourseId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("Share & Export", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 4. INTERACTIVE MULTIPLE CHOICE QUIZ DIALOG
    val activeQuizCourse = eduCoursesList.find { it.id == activeQuizCourseId }
    val quizList = quizzes[activeQuizCourseId ?: ""]
    if (activeQuizCourse != null && quizList != null) {
        if (currentQuestionIndex < quizList.size) {
            val q = quizList[currentQuestionIndex]
            Dialog(onDismissRequest = { activeQuizCourseId = null }) {
                Surface(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${activeQuizCourse.title} Quiz", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            IconButton(onClick = { activeQuizCourseId = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Question index progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Question ${currentQuestionIndex + 1} of ${quizList.size}", color = Color.Gray, fontSize = 11.sp)
                            Text("+${q.pointsAwarded} pts wallet reward", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        LinearProgressIndicator(
                            progress = (currentQuestionIndex.toFloat() / quizList.size),
                            color = Color(0xFFE50914),
                            trackColor = Color(0xFF2C2C2E),
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                        )

                        Text(q.question, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        // Option Cards
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            q.options.forEachIndexed { optIndex, optionText ->
                                val isSelected = selectedAnswerIndex == optIndex
                                val isCorrect = optIndex == q.correctOptionIndex
                                val optionBg = when {
                                    answerSubmitted && isCorrect -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    answerSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    isSelected -> Color(0xFFE50914).copy(alpha = 0.1f)
                                    else -> Color.White.copy(alpha = 0.02f)
                                }
                                val optionBorder = when {
                                    answerSubmitted && isCorrect -> Color(0xFF10B981)
                                    answerSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                                    isSelected -> Color(0xFFE50914)
                                    else -> Color(0xFF2C2C2E)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(optionBg, RoundedCornerShape(12.dp))
                                        .border(1.dp, optionBorder, RoundedCornerShape(12.dp))
                                        .clickable(enabled = !answerSubmitted) { selectedAnswerIndex = optIndex }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                            .border(1.dp, if (isSelected) Color(0xFFE50914) else Color.Gray, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFE50914), CircleShape))
                                        }
                                    }
                                    Text(optionText, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Submit CTA
                        Button(
                            onClick = {
                                if (!answerSubmitted) {
                                    if (selectedAnswerIndex != null) {
                                        answerSubmitted = true
                                        if (selectedAnswerIndex == q.correctOptionIndex) {
                                            scoreAwardedInQuiz += q.pointsAwarded
                                            viewModel.addRewardPoints(q.pointsAwarded)
                                        }
                                    } else {
                                        Toast.makeText(context, "Please choose an answer", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    currentQuestionIndex++
                                    selectedAnswerIndex = null
                                    answerSubmitted = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (!answerSubmitted) "Submit Answer" else if (currentQuestionIndex + 1 < quizList.size) "Next Question" else "Finish Quiz",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // Quiz Complete Success Dialog
            Dialog(onDismissRequest = { activeQuizCourseId = null }) {
                Surface(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = "Trophy", tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                        }
                        Text("Quiz Completed!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Outstanding job! You answered all questions and earned +$scoreAwardedInQuiz points wallet bonus synced to Firebase.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { activeQuizCourseId = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Acknowledge & Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 5. TODAY'S CLASS ROUTINE MAKER DIALOG (Make Routine)
    if (showRoutinePlannerDialog) {
        Dialog(onDismissRequest = { showRoutinePlannerDialog = false }) {
            Surface(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Routine Planner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showRoutinePlannerDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Text("Plan your scheduled study times and set daily routine milestones below.", color = Color.Gray, fontSize = 11.sp)

                    // Target study days of week
                    Text("Select Study Days", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            val isSelected = selectedStudyDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFFE50914) else Color.White.copy(0.04f))
                                    .clickable {
                                        selectedStudyDays = if (isSelected) {
                                            selectedStudyDays - day
                                        } else {
                                            selectedStudyDays + day
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day.take(2), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Target study duration slider
                    Text("Daily Study Target: ${dailyTargetStudyHours}h", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Slider(
                        value = dailyTargetStudyHours.toFloat(),
                        onValueChange = { dailyTargetStudyHours = it.toInt() },
                        valueRange = 1f..6f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE50914),
                            activeTrackColor = Color(0xFFE50914),
                            inactiveTrackColor = Color(0xFF2C2C2E)
                        )
                    )

                    Button(
                        onClick = {
                            val daysStr = selectedStudyDays.joinToString(",")
                            viewModel.saveRoutinePlanToSupabase(daysStr, dailyTargetStudyHours.toString())
                            showRoutinePlannerDialog = false
                            Toast.makeText(context, "Routine updated and synced to Cloud!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Routine Plan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 6. REAL-TIME STUDENT LECTURE ALERTS NOTIFICATIONS DIALOG
    if (showNotificationsDialog) {
        Dialog(onDismissRequest = { showNotificationsDialog = false }) {
            Surface(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LMS Notifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showNotificationsDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("Course Completed!", "You have finished Kotlin advanced lectures. Claim your professional diploma now!", "1h ago"),
                            Triple("Wallet Points Earned", "Quiz reward successfully added to student ledger: +50 Pts", "5h ago"),
                            Triple("Live Class Starting", "Udemy instructor stream beginning shortly on modern Android.", "Yesterday")
                        ).forEach { (title, msg, time) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(0.02f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(time, color = Color.Gray, fontSize = 9.sp)
                                }
                                Text(msg, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }

                    Button(
                        onClick = { showNotificationsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Notifications", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF1A1C23), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun EnrolledCoursesView(
    eduCoursesList: List<EduCourse>,
    myLearningSubTab: String,
    onSubTabChange: (String) -> Unit,
    wishlistCourseIds: Set<String>,
    onWishlistChange: (Set<String>) -> Unit,
    selectedStudyDays: Set<String>,
    dailyTargetStudyHours: Int,
    onEnterClass: (String) -> Unit,
    onSelectSyllabus: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Get current day of week (e.g. Mon, Tue, Wed, Thu, Fri, Sat, Sun)
    val currentDayOfWeek = remember {
        val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale.US)
        sdf.format(java.util.Date())
    }
    
    val isStudyDay = selectedStudyDays.contains(currentDayOfWeek)
    
    LaunchedEffect(Unit) {
        if (isStudyDay) {
            Toast.makeText(context, "📚 Today is a scheduled study day! Tap a course to resume learning.", Toast.LENGTH_LONG).show()
        }
    }
    
    val subTabCourses = when (myLearningSubTab) {
        "In Progress" -> eduCoursesList.filter { it.isEnrolled && it.completedPercent < 1.0f }
        "Completed" -> eduCoursesList.filter { it.isEnrolled && it.completedPercent >= 1.0f }
        "Wishlist" -> eduCoursesList.filter { wishlistCourseIds.contains(it.id) }
        else -> emptyList()
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Sub-tabs: In Progress, Completed, Wishlist
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("In Progress", "Completed", "Wishlist").forEach { subTab ->
                    val isSelected = subTab == myLearningSubTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFE50914) else Color(0xFF1C1C1E))
                            .clickable { onSubTabChange(subTab) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = subTab,
                            color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // A. Today's Class / Daily Routine Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Today's Schedule",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                if (isStudyDay) {
                    val todaysClasses = eduCoursesList.filter { it.isEnrolled && it.completedPercent < 1.0f }.take(2)
                    if (todaysClasses.isNotEmpty()) {
                        todaysClasses.forEach { course ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { onEnterClass(course.id) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                                border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 88.dp, height = 52.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        AsyncImage(
                                            model = getCourseThumbnailUrl(course.id),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEC4899).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("TODAY CLASS", color = Color(0xFFF472B6), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(course.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Daily Target: ${dailyTargetStudyHours} Hours • Scheduled", color = Color.Gray, fontSize = 10.sp)
                                    }
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Join", tint = Color(0xFFE50914), modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("All enrolled courses are completed! Browse catalog to enroll in more.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Off Day", tint = Color(0xFFF59E0B), modifier = Modifier.size(36.dp))
                            Column {
                                Text("Rest Day", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("No routine study classes scheduled for $currentDayOfWeek.", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        
        // B. Continue Learning list
        item {
            Text(
                text = if (myLearningSubTab == "Wishlist") "Saved Bookmarks" else "Continue Learning",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        
        if (subTabCourses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.School, null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (myLearningSubTab) {
                                "In Progress" -> "No classes currently in progress."
                                "Completed" -> "Finish your active classes to receive certification."
                                else -> "Your bookmark folder is empty."
                            },
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(subTabCourses) { course ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                        .clickable {
                            if (course.isEnrolled) {
                                onEnterClass(course.id)
                            } else {
                                onSelectSyllabus(course.id)
                            }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = getCourseThumbnailUrl(course.id),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .border(1.dp, Color.White.copy(0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = course.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (wishlistCourseIds.contains(course.id)) {
                                        onWishlistChange(wishlistCourseIds - course.id)
                                    } else {
                                        onWishlistChange(wishlistCourseIds + course.id)
                                    }
                                },
                                modifier = Modifier.size(20.dp).offset(x = 6.dp, y = (-4).dp)
                            ) {
                                Icon(
                                    imageVector = if (wishlistCourseIds.contains(course.id)) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = Color(0xFFE50914),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(course.instructorName, color = Color(0xFFA1A1AA), fontSize = 10.sp)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (course.isEnrolled) {
                            LinearProgressIndicator(
                                progress = course.completedPercent,
                                color = Color(0xFFE50914),
                                trackColor = Color(0xFF2C2C2E),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${(course.completedPercent * 100).toInt()}% complete",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "Enter Class",
                                    color = Color(0xFFE50914),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "12 hours remaining • Not Enrolled",
                                color = Color.Gray,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
        
        // C. Recommended to Continue
        item {
            Text(
                text = "Recommended To Continue",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        
        val recommendedToContinue = eduCoursesList.filter { !it.isEnrolled }
        if (recommendedToContinue.isNotEmpty()) {
            items(recommendedToContinue) { course ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                        .clickable { onSelectSyllabus(course.id) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = getCourseThumbnailUrl(course.id),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            course.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(course.instructorName, color = Color(0xFFA1A1AA), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("8 hours remaining", color = Color.Gray, fontSize = 9.sp)
                            Text("Start Learning", color = Color(0xFFE50914), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        // D. Promo Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep learning, Keep growing!",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Complete your courses and achieve your professional career goals.",
                            color = Color(0xFFA1A1AA),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "Graduation Cap",
                        tint = Color(0xFFE50914),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }
    }
}


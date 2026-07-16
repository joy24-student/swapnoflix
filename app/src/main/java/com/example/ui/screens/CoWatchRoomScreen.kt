package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.screens.YouTubePlayerView
import com.example.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetActivity
import java.net.URL

fun launchJitsiConference(context: android.content.Context, roomId: String, displayName: String) {
    try {
        val options = JitsiMeetConferenceOptions.Builder()
            .setServerURL(URL("https://meet.jit.si"))
            .setRoom(roomId)
            .setSubject("SwapnoFlix Group Co-Watch")
            .setAudioMuted(false)
            .setVideoMuted(false)
            .setFeatureFlag("chat.enabled", false)
            .setFeatureFlag("invite.enabled", false)
            .build()
        JitsiMeetActivity.launch(context, options)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
data class FloatingReaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val emoji: String,
    val xOffset: Float, // Percentage 0.0 to 1.0
    val size: Float = 24f
)

@Composable
fun CoWatchRoomScreen(viewModel: StreamingViewModel) {
    val activeRoom by viewModel.activeCoWatchRoom.collectAsState()
    val roomsList by viewModel.coWatchRooms.collectAsState()
    val socialUsers by viewModel.activeSocialUsers.collectAsState()

    // Trigger loading of active social users if empty
    LaunchedEffect(Unit) {
        FirebaseManager.startListeningToActiveSocialUsers()
    }

    AnimatedContent(
        targetState = activeRoom,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "room_screen_flow"
    ) { room ->
        if (room == null) {
            CoWatchLobbyView(
                roomsList = roomsList,
                socialUsers = socialUsers,
                viewModel = viewModel
            )
        } else {
            ActiveCoWatchRoomView(
                room = room,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun CoWatchLobbyView(
    roomsList: List<CoWatchRoom>,
    socialUsers: List<SocialActiveUser>,
    viewModel: StreamingViewModel
) {
    val appColors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }

    // Forms fields
    var roomNameInput by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf("MOVIE") } // "MOVIE", "LIVE_TV", "COURSE", "YOUTUBE"
    var selectedMediaId by remember { mutableStateOf("") }
    var selectedMediaTitle by remember { mutableStateOf("") }
    var selectedMediaDuration by remember { mutableStateOf("") }
    var selectedMediaColorHex by remember { mutableStateOf("#E50914") }
    var selectedUser by remember { mutableStateOf<SocialActiveUser?>(null) }

    // Grab available assets from the databases
    val movies = StreamingDatabase.movies
    val courses by viewModel.eduCourses.collectAsState()
    val liveChannels = StreamingDatabase.channels
    val youtubeVideos by viewModel.youtubeHomeVideos.collectAsState()

    // Initialize selections
    LaunchedEffect(showCreateDialog, selectedMediaType) {
        if (showCreateDialog) {
            if (movies.isNotEmpty() && selectedMediaType == "MOVIE") {
                selectedMediaId = movies.first().id
                selectedMediaTitle = movies.first().title
                selectedMediaDuration = movies.first().duration
                selectedMediaColorHex = "#E50914"
            } else if (liveChannels.isNotEmpty() && selectedMediaType == "LIVE_TV") {
                selectedMediaId = liveChannels.first().id
                selectedMediaTitle = liveChannels.first().name
                selectedMediaDuration = "LIVE Stream"
                selectedMediaColorHex = "#00C853"
            } else if (courses.isNotEmpty() && selectedMediaType == "COURSE") {
                selectedMediaId = courses.first().id
                selectedMediaTitle = courses.first().title
                selectedMediaDuration = courses.first().duration
                selectedMediaColorHex = "#3388FF"
            } else if (youtubeVideos.isNotEmpty() && selectedMediaType == "YOUTUBE") {
                val first = youtubeVideos.first()
                selectedMediaId = first.id
                selectedMediaTitle = first.title
                selectedMediaDuration = first.duration
                selectedMediaColorHex = "#FF0000"
            }
            if (socialUsers.isNotEmpty() && selectedUser == null) {
                selectedUser = socialUsers.first()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("cowatch_lobby_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature Header Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14121F)),
            border = BorderStroke(1.dp, Color(0xFF332A55)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF281E46), Color(0xFF14121F))
                        )
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFF0055).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = null,
                        tint = Color(0xFFFF0055),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zoom-Style Co-Watch",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Invite friends to study courses, watch movies, or stream live TV in real-time with synchronized views, multi-video grids, and hand raises.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Segmented Control Tab Bar
        var activeSubTab by remember { mutableStateOf("SESSIONS") } // "SESSIONS", "FRIENDS"
        val friendsList by viewModel.friendsList.collectAsState()
        val friendRequests by viewModel.friendRequests.collectAsState()
        val allProfilesSearch by viewModel.allProfilesSearch.collectAsState()
        var searchQuery by remember { mutableStateOf("") }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F14), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("SESSIONS" to "Active Rooms", "FRIENDS" to "Social Network").forEach { (tabId, label) ->
                val isSelected = activeSubTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFFF0055) else Color.Transparent)
                        .clickable { activeSubTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (activeSubTab == "SESSIONS") {
            // Quick Invite CTA
            Button(
                onClick = {
                    roomNameInput = "SwapnoFlix Study Space"
                    showCreateDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_room_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Co-Watch Room", fontWeight = FontWeight.Bold)
            }

            // Active/Recent Rooms list
            Text(
                text = "Active & Scheduled Sessions",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (roomsList.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F13)),
                    border = BorderStroke(1.dp, Color(0xFF1A1A24)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tv,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No active joint sessions.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Create a room and invite online friends to synchronize screens.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    roomsList.forEach { room ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E12)),
                            border = BorderStroke(
                                1.dp,
                                if (room.status == "ACTIVE") Color(0xFF3388FF).copy(alpha = 0.5f) else Color(0xFF1D1D26)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.joinExistingCoWatchRoom(room)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = room.roomName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (room.status == "ACTIVE") Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = room.status,
                                                color = if (room.status == "ACTIVE") Color(0xFF4CAF50) else Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (room.mediaType) {
                                                "COURSE" -> Icons.Filled.School
                                                "LIVE_TV" -> Icons.Filled.Tv
                                                else -> Icons.Filled.Movie
                                            },
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${room.mediaType}: ${room.mediaTitle}",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = "With ${room.invitedUser.name} • ${room.mediaDuration}",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Enter Room",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Online suggested users/test fallback list
            Text(
                text = "Suggested Watch Partners",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F14)),
                border = BorderStroke(1.dp, Color(0xFF1C1C26)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    socialUsers.forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(android.graphics.Color.parseColor(user.avatarColorHex)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Column {
                                    Text(
                                        text = user.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF4CAF50), CircleShape)
                                        )
                                        Text("Online • Study Group", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    selectedUser = user
                                    roomNameInput = "Zoom study with ${user.name.split(" ").first()}"
                                    showCreateDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055).copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, Color(0xFFFF0055).copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Invite", color = Color(0xFFFF0055), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // SOCIAL NETWORK Tab content
            Text(
                text = "Search & Make Social Friends",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchProfiles(it)
                },
                placeholder = { Text("Type name or email to search...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF0055),
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (allProfilesSearch.isNotEmpty()) {
                Text("Search Results", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14121F)),
                    border = BorderStroke(1.dp, Color(0xFFFF0055).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        allProfilesSearch.forEach { profile ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text((profile.full_name ?: profile.username ?: "User"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(profile.username ?: "User Profile", color = Color.Gray, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.sendFriendRequest(profile.id)
                                        searchQuery = ""
                                        viewModel.searchProfiles("")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Add Friend", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (friendRequests.isNotEmpty()) {
                Text(
                    text = "Pending Friend Requests (${friendRequests.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14121F)),
                    border = BorderStroke(1.dp, Color(0xFFE94057).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        friendRequests.forEach { req ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text((req.full_name ?: req.username ?: "User"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(req.username ?: "", color = Color.Gray, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.acceptFriendRequest(req.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Accept", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.declineFriendRequest(req.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Ignore", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "My Friends Network",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (friendsList.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F14)),
                    border = BorderStroke(1.dp, Color(0xFF1E1E26)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No friends accepted yet. Search above to add partners!", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F14)),
                    border = BorderStroke(1.dp, Color(0xFF1E1E26)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        friendsList.forEach { friend ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF3388FF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (friend.full_name ?: friend.username ?: "U").take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Column {
                                        Text((friend.full_name ?: friend.username ?: "User"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                                            Text("Online", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }
 
                                Button(
                                    onClick = {
                                        selectedUser = SocialActiveUser(
                                            uid = friend.id,
                                            email = friend.username ?: "",
                                            name = (friend.full_name ?: friend.username ?: "User"),
                                            provider = "Cloud",
                                            isOnline = true,
                                            avatarColorHex = "#3388FF"
                                        )
                                        roomNameInput = "Study session with ${(friend.full_name ?: friend.username ?: "User").split(" ").first()}"
                                        showCreateDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Invite", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE ROOM POPUP DIALOG
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    "Setup Co-Watch Room",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            containerColor = Color(0xFF141419),
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Room Name
                    OutlinedTextField(
                        value = roomNameInput,
                        onValueChange = { roomNameInput = it },
                        label = { Text("Room Title", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF0055),
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Friend to Invite info
                    Text(
                        text = "Meeting Participant:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0912)),
                        border = BorderStroke(1.dp, Color(0xFF1E1E26))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(selectedUser?.avatarColorHex ?: "#2196F3")),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedUser?.name?.take(1)?.uppercase() ?: "A",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = selectedUser?.name ?: "No user selected",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Media Type selector
                    Text(
                        text = "Real-time Synchronized Media:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("MOVIE", "Movie", Icons.Filled.Movie),
                            Triple("LIVE_TV", "Live TV", Icons.Filled.Tv),
                            Triple("COURSE", "Course", Icons.Filled.School),
                            Triple("YOUTUBE", "YouTube", Icons.Filled.PlayArrow)
                        ).forEach { (type, label, icon) ->
                            val isSel = selectedMediaType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) Color(0xFFFF0055).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSel) Color(0xFFFF0055) else Color.White.copy(alpha = 0.08f)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedMediaType = type
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSel) Color(0xFFFF0055) else Color.LightGray,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.White else Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic selection dropdown content depending on Type
                    Text(
                        text = "Choose content to stream:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0xFF09090D), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFF1F1F2C)), RoundedCornerShape(8.dp))
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (selectedMediaType == "MOVIE") {
                                items(movies) { item ->
                                    val isSel = selectedMediaId == item.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSel) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                            .clickable {
                                                selectedMediaId = item.id
                                                selectedMediaTitle = item.title
                                                selectedMediaDuration = item.duration
                                                selectedMediaColorHex = "#E50914"
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("${item.category} • ${item.duration}", color = Color.Gray, fontSize = 10.sp)
                                        }
                                        if (isSel) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFFF0055), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            } else if (selectedMediaType == "LIVE_TV") {
                                items(liveChannels) { item ->
                                    val isSel = selectedMediaId == item.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSel) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                            .clickable {
                                                selectedMediaId = item.id
                                                selectedMediaTitle = item.name
                                                selectedMediaDuration = "LIVE Stream"
                                                selectedMediaColorHex = "#00C853"
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Live TV • ${item.category} (${item.language})", color = Color.Gray, fontSize = 10.sp)
                                        }
                                        if (isSel) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFFF0055), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            } else if (selectedMediaType == "YOUTUBE") {
                                items(youtubeVideos) { item ->
                                    val isSel = selectedMediaId == item.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSel) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                            .clickable {
                                                selectedMediaId = item.id
                                                selectedMediaTitle = item.title
                                                selectedMediaDuration = item.duration
                                                selectedMediaColorHex = "#FF0000"
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("YouTube • ${item.channelName} (${item.duration})", color = Color.Gray, fontSize = 10.sp)
                                        }
                                        if (isSel) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFFF0055), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            } else if (selectedMediaType == "COURSE") {
                                items(courses) { item ->
                                    val isSel = selectedMediaId == item.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSel) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                            .clickable {
                                                selectedMediaId = item.id
                                                selectedMediaTitle = item.title
                                                selectedMediaDuration = item.duration
                                                selectedMediaColorHex = "#3388FF"
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("${item.category} • ${item.duration}", color = Color.Gray, fontSize = 10.sp)
                                        }
                                        if (isSel) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFFF0055), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val activeUser = selectedUser
                        if (activeUser != null && selectedMediaTitle.isNotBlank()) {
                            viewModel.createCoWatchRoom(
                                roomName = if (roomNameInput.isNotBlank()) roomNameInput else "Watch Together Room",
                                mediaType = selectedMediaType,
                                mediaTitle = selectedMediaTitle,
                                mediaId = selectedMediaId,
                                mediaDuration = selectedMediaDuration,
                                mediaColorHex = selectedMediaColorHex,
                                invitedUser = activeUser
                            )
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                ) {
                    Text("Start Meeting", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}

@Composable
fun ActiveCoWatchRoomView(
    room: CoWatchRoom,
    viewModel: StreamingViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val chatMessages by viewModel.coWatchMessages.collectAsState()
    val isVideoCallActive by viewModel.coWatchVideoCallActive.collectAsState()
    val isMicMuted by viewModel.coWatchMicMuted.collectAsState()
    val isCameraOn by viewModel.coWatchCameraOn.collectAsState()
    val context = LocalContext.current

    var chatInput by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    // Advanced Zoom States
    var viewMode by remember { mutableStateOf("THEATER") } // "THEATER" or "ZOOM_GRID"
    var isHandRaised by remember { mutableStateOf(false) }
    var activeSpeaker by remember { mutableStateOf("Me") }
    var showSwapContentDialog by remember { mutableStateOf(false) }
    var showFriendInviteMenu by remember { mutableStateOf(false) }
    var transitionTriggered by remember { mutableStateOf(false) }

    val activeRoomMembersCount by viewModel.activeRoomMembersCount.collectAsState()
    val friendsList by viewModel.friendsList.collectAsState()

    LaunchedEffect(activeRoomMembersCount) {
        if (activeRoomMembersCount >= 3) {
            transitionTriggered = true
            delay(3000) // Show upgrading overlay for 3 seconds
            transitionTriggered = false
        }
    }

    // Floating reaction elements
    var floatingReactions by remember { mutableStateOf<List<FloatingReaction>>(emptyList()) }

    // Sliding slide index for COURSE
    var currentSlideIndex by remember { mutableStateOf(1) }

    // Ticking stream position timer
    var ticksSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(room.isPlaying) {
        if (room.isPlaying) {
            while (true) {
                delay(1000)
                ticksSeconds += 1
            }
        }
    }

    // Auto active speaker switcher
    LaunchedEffect(isVideoCallActive) {
        if (isVideoCallActive) {
            if (activeRoomMembersCount >= 3) {
                launchJitsiConference(context, room.id, "User")
            }
            val speakers = listOf("Me", room.invitedUser.name, "Emma Watson", "Dr. Alan Mercer")
            while (true) {
                delay(5000)
                activeSpeaker = speakers.random()
            }
        }
    }

    // typing indicators
    var typingUser by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty() && chatMessages.last().isSenderUser) {
            typingUser = room.invitedUser.name.split(" ").first()
            delay(1600)
            typingUser = null
        }
    }

    // Auto-scroll chat to bottom
    LaunchedEffect(chatMessages.size, typingUser) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Dynamic color matching
    val accentColor = when (room.mediaType) {
        "COURSE" -> Color(0xFF3388FF)
        "LIVE_TV" -> Color(0xFF00C853)
        else -> Color(0xFFFF0055)
    }

    // Formatted ticking time string
    val formattedPlaybackTime = remember(ticksSeconds) {
        val minutes = ticksSeconds / 60
        val seconds = ticksSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    // Function to inject custom emoji floating reactions
    fun shootReaction(emoji: String) {
        val randomX = (10..90).random().toFloat() / 100f
        val reaction = FloatingReaction(emoji = emoji, xOffset = randomX)
        floatingReactions = floatingReactions + reaction

        // Auto remove after animation completes
        coroutineScope.launch {
            delay(2000)
            floatingReactions = floatingReactions.filter { it.id != reaction.id }
        }

        // 60% chance peer reacts back
        if ((0..100).random() < 60) {
            coroutineScope.launch {
                delay((600..1200).random().toLong())
                val peerX = (10..90).random().toFloat() / 100f
                val peerReaction = FloatingReaction(emoji = emoji, xOffset = peerX)
                floatingReactions = floatingReactions + peerReaction
                delay(2000)
                floatingReactions = floatingReactions.filter { it.id != peerReaction.id }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070B))
            .testTag("active_room_view")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // 1. Zoom Style Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D0D12))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = { viewModel.exitCoWatchActiveRoom() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Exit Room", tint = Color.White)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = room.roomName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00C853).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(8.dp))
                                Text("AES-256", color = Color(0xFF00C853), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        text = "ID: ${room.id.take(8).uppercase()} • Connected with ${room.invitedUser.name}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            // Controls for Quick Meeting Settings
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Swap media directly
                IconButton(
                    onClick = { showSwapContentDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Change Shared Media", tint = Color.LightGray, modifier = Modifier.size(15.dp))
                }

                // Invite additional friend
                Box {
                    IconButton(
                        onClick = { showFriendInviteMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Invite Friend to Call", tint = Color.LightGray, modifier = Modifier.size(15.dp))
                    }
                    DropdownMenu(
                        expanded = showFriendInviteMenu,
                        onDismissRequest = { showFriendInviteMenu = false },
                        modifier = Modifier.background(Color(0xFF141419))
                    ) {
                        if (friendsList.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No friends available", color = Color.Gray) },
                                onClick = { showFriendInviteMenu = false }
                            )
                        } else {
                            friendsList.forEach { friend ->
                                DropdownMenuItem(
                                    text = { Text((friend.full_name ?: friend.username ?: "User"), color = Color.White) },
                                    onClick = {
                                        viewModel.inviteAdditionalUserToRoomInSupabase(room.id, friend.id, (friend.full_name ?: friend.username ?: "User"))
                                        showFriendInviteMenu = false
                                        android.widget.Toast.makeText(context, "Invited ${(friend.full_name ?: friend.username ?: "User")} to Co-Watch!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                // Copy Link Invite
                IconButton(
                    onClick = {
                        android.widget.Toast.makeText(context, "Zoom Co-Watch Invite Link Copied!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Invite Link", tint = Color.LightGray, modifier = Modifier.size(15.dp))
                }

                Button(
                    onClick = { viewModel.exitCoWatchActiveRoom() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Filled.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("End", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Layout View Selector (Theater Focus vs. Zoom Grid Focus)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF101016))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                Text(
                    text = "MODE: ${if (viewMode == "THEATER") "Theater Co-Watch" else "Interactive Grid"}",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .background(
                            if (viewMode == "THEATER") accentColor.copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            if (viewMode == "THEATER") accentColor else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { viewMode = "THEATER" }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Theater View", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (viewMode == "ZOOM_GRID") accentColor.copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            if (viewMode == "ZOOM_GRID") accentColor else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { viewMode = "ZOOM_GRID" }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Zoom Grid", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Connection Protocol Status Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (activeRoomMembersCount == 2) Color(0xFF0055FF).copy(alpha = 0.12f)
                    else Color(0xFFFF9900).copy(alpha = 0.12f)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (activeRoomMembersCount == 2) Color(0xFF0055FF).copy(alpha = 0.3f)
                        else Color(0xFFFF9900).copy(alpha = 0.3f)
                    )
                )
                .padding(vertical = 6.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (activeRoomMembersCount == 2) Color(0xFF00D2FF) else Color(0xFFFFB300),
                            CircleShape
                        )
                )
                Text(
                    text = if (activeRoomMembersCount == 2)
                        "Direct Call: WebRTC Peer-to-Peer Protocol (Low Latency • STUN Server)"
                    else
                        "Group Conference: Jitsi Meet SDK Protocol (Auto Mixed Streams)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 2. Main Area (Split screen between Media sharing, video call, and floating reactions)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .background(Color.Black)
            ) {
                // Render content depending on selected ViewMode
                if (viewMode == "THEATER") {
                    // THEATER MODE: Large Media Stream sharing, strip of camera feeds
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Media Stream View Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            MediaStreamContainer(
                                room = room,
                                accentColor = accentColor,
                                formattedPlaybackTime = formattedPlaybackTime,
                                currentSlideIndex = currentSlideIndex,
                                onSlideChange = { newIdx ->
                                    currentSlideIndex = newIdx
                                    viewModel.sendCoWatchMessage("Navigated synchronized course slides to Page $newIdx")
                                }
                            )

                            // Render Floating Reactions on top of Media Screen
                            FloatingReactionsContainer(floatingReactions)
                        }

                        // Video feeds strip under the media player (scrolling list)
                        if (isVideoCallActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF08080C))
                                    .padding(vertical = 4.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic strip of active participant video blocks
                                val allParticipants = listOf(
                                    ParticipantCallData("Me", "Me", isCameraOn, isMicMuted, isHandRaised, "#FF5722"),
                                    ParticipantCallData(room.invitedUser.name, room.invitedUser.name.split(" ").first(), true, false, false, room.invitedUser.avatarColorHex),
                                    ParticipantCallData("Emma Watson", "Emma", true, false, false, "#9C27B0"),
                                    ParticipantCallData("Dr. Alan Mercer", "Dr. Alan", true, true, false, "#3F51B5")
                                )
                                val activeFeeds = if (activeRoomMembersCount == 2) allParticipants.take(2) else allParticipants
                                activeFeeds.forEach { participant ->
                                    ParticipantStripCard(
                                        participant = participant,
                                        activeSpeaker = activeSpeaker,
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ZOOM MEETING GRID FOCUS
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 2x2 Video Call Grid taking majority space
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(6.dp)
                        ) {
                            if (activeRoomMembersCount == 2) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        FullZoomParticipantCard(
                                            name = "Me",
                                            avatarColor = "#FF5722",
                                            cameraOn = isCameraOn,
                                            micMuted = isMicMuted,
                                            handRaised = isHandRaised,
                                            activeSpeaker = activeSpeaker,
                                            accentColor = accentColor
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        FullZoomParticipantCard(
                                            name = room.invitedUser.name,
                                            avatarColor = room.invitedUser.avatarColorHex,
                                            cameraOn = true,
                                            micMuted = false,
                                            handRaised = false,
                                            activeSpeaker = activeSpeaker,
                                            accentColor = accentColor
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            FullZoomParticipantCard(
                                                name = "Me",
                                                avatarColor = "#FF5722",
                                                cameraOn = isCameraOn,
                                                micMuted = isMicMuted,
                                                handRaised = isHandRaised,
                                                activeSpeaker = activeSpeaker,
                                                accentColor = accentColor
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            FullZoomParticipantCard(
                                                name = room.invitedUser.name,
                                                avatarColor = room.invitedUser.avatarColorHex,
                                                cameraOn = true,
                                                micMuted = false,
                                                handRaised = false,
                                                activeSpeaker = activeSpeaker,
                                                accentColor = accentColor
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            FullZoomParticipantCard(
                                                name = "Emma Watson",
                                                avatarColor = "#9C27B0",
                                                cameraOn = true,
                                                micMuted = false,
                                                handRaised = false,
                                                activeSpeaker = activeSpeaker,
                                                accentColor = accentColor
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            FullZoomParticipantCard(
                                                name = "Dr. Alan Mercer",
                                                avatarColor = "#3F51B5",
                                                cameraOn = true,
                                                micMuted = true,
                                                handRaised = false,
                                                activeSpeaker = activeSpeaker,
                                                accentColor = accentColor
                                            )
                                        }
                                    }
                                }
                            }

                            // Render Floating Reactions on top of meeting grids
                            FloatingReactionsContainer(floatingReactions)

                            // Minimize shared media overlay (PIP Style Card in Zoom Call Grid)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161622).copy(alpha = 0.9f)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .size(width = 130.dp, height = 80.dp)
                                    .clickable { viewMode = "THEATER" }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = when (room.mediaType) {
                                                    "COURSE" -> Icons.Filled.School
                                                    "LIVE_TV" -> Icons.Filled.Tv
                                                    else -> Icons.Filled.Movie
                                                },
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                "SHARED SCREEN",
                                                color = accentColor,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = room.mediaTitle,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Click to enlarge",
                                            color = Color.Gray,
                                            fontSize = 7.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Media control bar & quick Zoom emoji reactions (placed right below the stream box)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F14))
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Playback Scrubber for non-Live-TV media
                    if (room.mediaType != "LIVE_TV") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formattedPlaybackTime,
                                color = Color.LightGray,
                                fontSize = 9.sp
                            )
                            LinearProgressIndicator(
                                progress = if (ticksSeconds < 3600) (ticksSeconds.toFloat() / 3600f).coerceIn(0f, 1f) else 1f,
                                color = accentColor,
                                trackColor = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                            )
                            Text(
                                text = room.mediaDuration,
                                color = Color.LightGray,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Media Seekers
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { viewModel.toggleCoWatchPlayPause() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (room.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Playback Control",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (room.mediaType != "LIVE_TV") {
                                IconButton(
                                    onClick = { ticksSeconds = (ticksSeconds - 10).coerceAtLeast(0) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Replay10, contentDescription = "Rewind", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                }

                                IconButton(
                                    onClick = { ticksSeconds += 10 },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Forward10, contentDescription = "Forward", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(Icons.Filled.Sync, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(10.dp))
                                Text("LOCKED SYNC", color = Color(0xFF00C853), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Floating reactions panel row (👏, 👍, ❤️, 😂, 🔥, ✋)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Pair("👏", "Clap"),
                                Pair("👍", "Like"),
                                Pair("❤️", "Love"),
                                Pair("😂", "Laugh"),
                                Pair("🔥", "Fire")
                            ).forEach { (emoji, label) ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                        .clickable { shootReaction(emoji) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 11.sp)
                                }
                            }

                            // Hand raise toggle
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isHandRaised) accentColor else Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        isHandRaised = !isHandRaised
                                        val statusNotice = if (isHandRaised) "raised their hand" else "lowered their hand"
                                        viewModel.sendCoWatchMessage("Me $statusNotice")
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("✋", fontSize = 10.sp)
                                    Text(
                                        if (isHandRaised) "Lower" else "Raise Hand",
                                        color = if (isHandRaised) Color.Black else Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Chat Room Message Feed Section with Video Call controls underneath
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .background(Color(0xFF0A0A0E))
            ) {
                // Header of Chat Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E0E12))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEETING CHAT & LOGS",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color(0xFFFF0055), CircleShape)
                        )
                        Text(
                            text = "LIVE SESSION ACTIVE",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Messages lazy column
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chatMessages) { msg ->
                        if (msg.isSenderUser) {
                            // Local user aligned right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(accentColor, RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = if (accentColor == Color(0xFF00C853)) Color.Black else Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = msg.timestamp,
                                        color = Color.Gray,
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        } else {
                            // System notification or Friend message aligned left
                            val isSystem = msg.text.contains("joined") || msg.text.contains("created") || msg.text.contains("playback") || msg.text.contains("started") || msg.text.contains("ended") || msg.text.contains("navigated") || msg.text.contains("hand")
                            if (isSystem) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                Color(android.graphics.Color.parseColor(room.invitedUser.avatarColorHex)),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = room.invitedUser.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = msg.text,
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = "${room.invitedUser.name.split(" ").first()} • ${msg.timestamp}",
                                            color = Color.Gray,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Realtime typing indicator slot
                if (typingUser != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$typingUser is typing...",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Chat Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F14))
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Video Call Controls (Toggle mic, camera, start call directly inside meeting)
                    IconButton(
                        onClick = { viewModel.toggleCoWatchMic() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isMicMuted) Color(0xFFE50914).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = "Mute Mic",
                            tint = if (isMicMuted) Color.Red else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleCoWatchVideoCall() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isVideoCallActive) Color(0xFF00C853).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideoCallActive) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                            contentDescription = "Video Call Grid",
                            tint = if (isVideoCallActive) Color(0xFF00C853) else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Group chat or study logs...", color = Color.Gray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("room_chat_input"),
                        shape = RoundedCornerShape(100.dp),
                        singleLine = true,
                        maxLines = 1,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )

                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                viewModel.sendCoWatchMessage(chatInput)
                                chatInput = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = accentColor
                        ),
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("room_send_chat_btn")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = if (accentColor == Color(0xFF00C853)) Color.Black else Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Jitsi Switcher Transition Overlay
        if (transitionTriggered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = false) {}, // absorb clicks
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF0055))
                    Text(
                        text = "Adding participant...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Switching from Peer-to-Peer to Jitsi Group Call Protocol",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // SWAP CONTENT SELECTOR DIALOG (CHANGE MEDIA IN REAL-TIME)
    if (showSwapContentDialog) {
        AlertDialog(
            onDismissRequest = { showSwapContentDialog = false },
            title = {
                Text(
                    "Change Screen Content",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            containerColor = Color(0xFF141419),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Instantly share another video stream, Live TV channel, or study course lecture with the active group.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFF08080C), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text("LIVE TV CHANNELS", color = Color(0xFF00C853), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                            }
                            items(StreamingDatabase.channels) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.createCoWatchRoom(
                                                roomName = room.roomName,
                                                mediaType = "LIVE_TV",
                                                mediaTitle = item.name,
                                                mediaId = item.id,
                                                mediaDuration = "LIVE Stream",
                                                mediaColorHex = "#00C853",
                                                invitedUser = room.invitedUser
                                            )
                                            ticksSeconds = 0
                                            showSwapContentDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("LIVE TV", color = Color.LightGray, fontSize = 9.sp)
                                }
                            }

                            item {
                                Text("COURSES & LECTURES", color = Color(0xFF3388FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                            }
                            items(StreamingDatabase.movies) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.createCoWatchRoom(
                                                roomName = room.roomName,
                                                mediaType = "MOVIE",
                                                mediaTitle = item.title,
                                                mediaId = item.id,
                                                mediaDuration = item.duration,
                                                mediaColorHex = "#E50914",
                                                invitedUser = room.invitedUser
                                            )
                                            ticksSeconds = 0
                                            showSwapContentDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Movie • ${item.category}", color = Color.LightGray, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSwapContentDialog = false }) {
                    Text("Close", color = Color.LightGray)
                }
            }
        )
    }
} // end Box (ActiveCoWatchRoomView)
} // end ActiveCoWatchRoomView

// Media Container Simulator Component
@Composable
fun MediaStreamContainer(
    room: CoWatchRoom,
    accentColor: Color,
    formattedPlaybackTime: String,
    currentSlideIndex: Int,
    onSlideChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (room.mediaType == "LIVE_TV") {
            // HIGH-FIDELITY LIVE TV SIMULATOR
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background visual bars simulating television transmission
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(8) { idx ->
                        val infiniteTransition = rememberInfiniteTransition(label = "bars")
                        val heightMultiplier by infiniteTransition.animateFloat(
                            initialValue = 0.15f,
                            targetValue = 0.9f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = (600 + idx * 100), easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bars_anim"
                        )

                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight(if (room.isPlaying) heightMultiplier else 0.2f)
                                .background(accentColor.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                            .border(BorderStroke(1.dp, Color.Red), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Text("LIVE BROADCAST", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Text(
                        text = room.mediaTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "1080p Stream • 5.1 Stereo Sync Active",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                // Up next ticker tapecrawler at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(vertical = 4.dp, horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CRITICAL BREAKING FEED: Continuous study group watch synchronization active.",
                            color = Color.Yellow,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "SwapnoFlix News 24/7",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Watermark
                Text(
                    text = "LIVE FEED",
                    color = Color.White.copy(alpha = 0.15f),
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        } else if (room.mediaType == "COURSE") {
            // ADVANCED ACADEMIC COURSE SLIDE DECK VIEW
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Lecture Deck Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.School, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                            Text(
                                "CLASSROOM DECK",
                                color = accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "Slide $currentSlideIndex of 5",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Main Slide Canvas
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = when (currentSlideIndex) {
                                1 -> "1. Core Principles of Machine Learning"
                                2 -> "2. Underfitting vs Overfitting Vectors"
                                3 -> "3. Designing Neural Network Layers"
                                4 -> "4. Gradient Descent Optimization"
                                else -> "5. Building End-To-End Generative Models"
                            },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when (currentSlideIndex) {
                                1 -> "• Supervised Learning relies on labeled training pairs.\n• Regression maps to continuous bounds, Classification to discrete states."
                                2 -> "• Overfitting has high variance; high accuracy on training set but poor generalization.\n• Underfitting occurs when model parameters are too basic."
                                3 -> "• Fully connected layers connect every neuron to the preceding layer.\n• Activation functions (ReLU, Sigmoid) introduce non-linearity."
                                4 -> "• Loss Function measures inaccuracy of network predictions.\n• Learning rates scale the adjustment step vectors during backprop."
                                else -> "• Integrating APIs directly into cloud frameworks.\n• Deploying specialized pipelines on local JVM nodes."
                            },
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    // Slide deck triggers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (currentSlideIndex > 1) onSlideChange(currentSlideIndex - 1) },
                            enabled = currentSlideIndex > 1,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Prev Slide", fontSize = 9.sp, color = Color.White)
                        }

                        Text(
                            text = "Study Room: '${room.mediaTitle}'",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { if (currentSlideIndex < 5) onSlideChange(currentSlideIndex + 1) },
                            enabled = currentSlideIndex < 5,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Next Slide", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }
        } else if (room.mediaType == "YOUTUBE") {
            // INTEGRATED YOUTUBE PLAYER FOR CO-WATCH
            YouTubePlayerView(
                videoId = room.mediaId,
                shouldPlay = room.isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // DRAMATIC KODACHROME CINEMATIC MOVIE SIMULATOR
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.2f), Color(0xFF020205))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
                            Text(
                                "CINEMA SYNC SCREEN",
                                color = accentColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            "Dolby Digital 7.1",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Simulated moving visualizer representing movie frames
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "SUBTITLES:",
                                color = accentColor,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "“This beautiful world contains secrets that we must study together.”",
                                color = Color.White,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Viewing: ${room.mediaTitle}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Participant Video Stream blocks
data class ParticipantCallData(
    val fullName: String,
    val nickName: String,
    val isCameraOn: Boolean,
    val isMicMuted: Boolean,
    val isHandRaised: Boolean,
    val colorHex: String
)

@Composable
fun ParticipantStripCard(
    participant: ParticipantCallData,
    activeSpeaker: String,
    accentColor: Color
) {
    val isSpeaking = activeSpeaker == participant.fullName
    val glowBorderColor = if (isSpeaking) accentColor else Color.Transparent

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
        border = BorderStroke(1.5.dp, if (isSpeaking) glowBorderColor else Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(width = 75.dp, height = 75.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (participant.isCameraOn) {
                // Render custom dynamic canvas representing video stream
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(android.graphics.Color.parseColor(participant.colorHex)).copy(alpha = 0.4f), Color.Black)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = participant.nickName,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSpeaking) {
                            Text(
                                "SPEAKING",
                                color = accentColor,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            } else {
                // Camera OFF
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(android.graphics.Color.parseColor(participant.colorHex)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = participant.nickName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Bottom Mic indicator overlays
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(vertical = 1.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = participant.nickName,
                    color = Color.LightGray,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (participant.isMicMuted) {
                    Icon(Icons.Filled.MicOff, contentDescription = null, tint = Color.Red, modifier = Modifier.size(8.dp))
                } else if (isSpeaking) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(8.dp))
                }
            }

            // Top Hand Raised indicator overlay
            if (participant.isHandRaised) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Yellow, CircleShape)
                        .size(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✋", fontSize = 8.sp)
                }
            }
        }
    }
}

// Full 2x2 Zoom Video Meeting Grid Card
@Composable
fun FullZoomParticipantCard(
    name: String,
    avatarColor: String,
    cameraOn: Boolean,
    micMuted: Boolean,
    handRaised: Boolean,
    activeSpeaker: String,
    accentColor: Color
) {
    val isSpeaking = activeSpeaker == name
    val outlineBorderColor = if (isSpeaking) accentColor else Color.White.copy(alpha = 0.1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E14)),
        border = BorderStroke(2.dp, if (isSpeaking) outlineBorderColor else Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraOn) {
                // Simulated rich video stream background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(android.graphics.Color.parseColor(avatarColor)).copy(alpha = 0.35f),
                                    Color(0xFF09090E)
                                )
                            )
                        )
                ) {
                    // Visual elements inside active video stream
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(android.graphics.Color.parseColor(avatarColor)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (isSpeaking) {
                            Text(
                                "ACTIVE SPEAKER",
                                color = accentColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            } else {
                // Camera is OFF
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF030305)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color(android.graphics.Color.parseColor(avatarColor)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Camera Off", color = Color.Gray, fontSize = 9.sp)
                    }
                }
            }

            // Mute / Speaker Indicator Bottom overlays
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSpeaking) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF00C853), CircleShape)
                        )
                    }
                }

                if (micMuted) {
                    Icon(
                        imageVector = Icons.Filled.MicOff,
                        contentDescription = "Muted",
                        tint = Color.Red,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Unmuted",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Hand raise status icon overlay
            if (handRaised) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.Yellow, CircleShape)
                        .size(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✋", fontSize = 11.sp)
                }
            }
        }
    }
}

// Floating Reactions Container drawing drifts
@Composable
fun FloatingReactionsContainer(reactions: List<FloatingReaction>) {
    Box(modifier = Modifier.fillMaxSize()) {
        reactions.forEach { reaction ->
            val infiniteTransition = rememberInfiniteTransition(label = "floating")
            val animatedY by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 0.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "floating_y"
            )

            val animatedAlpha by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "floating_alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = reaction.emoji,
                    fontSize = reaction.size.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(
                            x = (reaction.xOffset * 220).dp,
                            y = (animatedY * -240).dp
                        )
                        .scale(1.2f)
                )
            }
        }
    }
}

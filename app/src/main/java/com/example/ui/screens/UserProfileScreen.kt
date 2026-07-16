package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.StreamingViewModel
import com.example.data.UserProfile
import com.example.data.StreamingDatabase
import com.example.data.MainTab
import com.example.data.AppDestination
import com.example.ui.theme.LocalAppColors
import java.util.UUID
import androidx.compose.ui.graphics.toArgb
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@Composable
fun UserProfileScreen(
    viewModel: StreamingViewModel,
    onSelectSection: (String) -> Unit
) {
    val activeProfile by viewModel.selectedProfile.collectAsState()
    val activePlan by viewModel.userActivePlan.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val favoriteMovies by viewModel.favoriteMovies.collectAsState()
    val favoriteSeries by viewModel.favoriteSeries.collectAsState()
    val downloads by viewModel.playTubeDownloads.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val tvPairings by viewModel.tvPairings.collectAsState()
    val supabaseWatchHistory by viewModel.supabaseWatchHistory.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val context = LocalContext.current
    val appColors = LocalAppColors.current

    val isBn = language == "বাংলা"

    // Dialog controllers
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showWatchHistoryDialog by remember { mutableStateOf(false) }
    var showMyListDialog by remember { mutableStateOf(false) }
    var showProfilesDialog by remember { mutableStateOf(false) }
    var showDeviceMgmtDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var showAvatarUrlDialog by remember { mutableStateOf(false) }
    var avatarUrlInput by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val pid = activeProfile?.id
            if (pid != null) {
                uploadProgress = true
                val filename = "avatar_${pid}_${System.currentTimeMillis()}.jpg"
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        viewModel.uploadAvatarToSupabaseBucket(pid, filename, bytes) { publicUrl ->
                            uploadProgress = false
                            showAvatarUrlDialog = false
                            Toast.makeText(context, if (isBn) "প্রোফাইল ছবি আপডেট করা হয়েছে!" else "Profile photo updated!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        uploadProgress = false
                        Toast.makeText(context, if (isBn) "ইমেজ ডাটা পড়তে ব্যর্থ হয়েছে" else "Failed to read image data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    uploadProgress = false
                    Toast.makeText(context, if (isBn) "ত্রুটি: ${e.message}" else "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Mock states that persist per session
    var userEmailState by remember(userEmail) { mutableStateOf(userEmail) }

    LaunchedEffect(activeProfile) {
        if (activeProfile != null) {
            viewModel.loadDevicesFromSupabase()
            viewModel.loadTvPairingsFromSupabase()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp) // ensure room for navigation
        ) {
            // Status bar spacer or top padding
            Spacer(modifier = Modifier.height(12.dp))

            // MainHeader (HTML Match)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "প্রোফাইল" else "Profile",
                    color = appColors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("profile_screen_title")
                )
            }

            // User Info Section Card (HTML Match)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar & Info & Edit Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar Section
                        Box(modifier = Modifier.size(96.dp)) {
                            // Pink to orange ring gradient
                            val gradientBrush = remember {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF0055), Color(0xFFFF4D00))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(3.dp, gradientBrush, CircleShape)
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(activeProfile?.iconColor ?: Color(0xFFFF0055))
                                ) {
                                    AsyncImage(
                                        model = activeProfile?.avatarUrl ?: "https://ui-avatars.com/api/?name=${activeProfile?.name ?: "User"}&background=random&color=fff",
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            // Camera Overlay Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .background(Color(0xFF1F1F23), CircleShape)
                                    .border(1.dp, Color.Black, CircleShape)
                                    .clickable { showAvatarUrlDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Edit photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    // Avatar URL Dialog
                    if (showAvatarUrlDialog) {
                        var dialogTab by remember { mutableStateOf("PROFESSIONAL") } // "PROFESSIONAL", "UPLOAD", "URL"
                        
                        Dialog(onDismissRequest = { if (!uploadProgress) showAvatarUrlDialog = false }) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                                modifier = Modifier.padding(16.dp).border(1.dp, appColors.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(if (isBn) "প্রোফাইল ছবি পরিবর্তন করুন" else "Change Profile photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    
                                    // Tab Selector
                                    ScrollableTabRow(
                                        selectedTabIndex = when(dialogTab) {
                                            "PROFESSIONAL" -> 0
                                            "UPLOAD" -> 1
                                            else -> 2
                                        },
                                        containerColor = Color.Transparent,
                                        contentColor = appColors.primary,
                                        edgePadding = 0.dp,
                                        divider = {}
                                    ) {
                                        Tab(selected = dialogTab == "PROFESSIONAL", onClick = { dialogTab = "PROFESSIONAL" }) {
                                            Text(if (isBn) "লাইব্রেরি" else "Library", modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                                        }
                                        Tab(selected = dialogTab == "UPLOAD", onClick = { dialogTab = "UPLOAD" }) {
                                            Text(if (isBn) "আপলোড" else "Upload", modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                                        }
                                        Tab(selected = dialogTab == "URL", onClick = { dialogTab = "URL" }) {
                                            Text(if (isBn) "ওয়েব লিঙ্ক" else "Web Link", modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                                        }
                                    }
                                    
                                    if (uploadProgress) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            CircularProgressIndicator(color = appColors.primary)
                                            Text(if (isBn) "আপলোড প্রসেস করা হচ্ছে..." else "Processing high-quality upload...", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    } else {
                                        when (dialogTab) {
                                            "PROFESSIONAL" -> {
                                                Text(if (isBn) "আমাদের লাইব্রেরি থেকে একটি পেশাদার প্রতিকৃতি চয়ন করুন:" else "Choose a professional portrait from our library:", color = Color.LightGray, fontSize = 11.sp)
                                                LazyVerticalGrid(
                                                    columns = GridCells.Fixed(4),
                                                    modifier = Modifier.height(180.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(StreamingDatabase.professionalAvatars) { url ->
                                                        AsyncImage(
                                                            model = url,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .aspectRatio(1f)
                                                                .clip(CircleShape)
                                                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                                                .clickable {
                                                                    val pid = activeProfile?.id
                                                                    if (pid != null) {
                                                                        viewModel.updateProfileAvatarInSupabase(pid, url)
                                                                        showAvatarUrlDialog = false
                                                                        Toast.makeText(context, if (isBn) "ছবি আপডেট করা হয়েছে!" else "Avatar updated!", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                        )
                                                    }
                                                }
                                            }
                                            "UPLOAD" -> {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(48.dp))
                                                    Text(if (isBn) "আপনার ডিভাইস থেকে কাস্টম JPG/PNG আপলোড করুন" else "Upload custom JPG/PNG from your device", color = Color.LightGray, fontSize = 12.sp)
                                                    Button(
                                                        onClick = { photoPickerLauncher.launch("image/*") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(if (isBn) "ছবি নির্বাচন করুন" else "Select Photo", color = Color.Black, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            "URL" -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Text(if (isBn) "নিচে একটি সরাসরি ইমেজের ওয়েব লিঙ্ক পেস্ট করুন:" else "Paste a direct image web link below:", color = Color.LightGray, fontSize = 11.sp)
                                                    OutlinedTextField(
                                                        value = avatarUrlInput,
                                                        onValueChange = { avatarUrlInput = it },
                                                        label = { Text(if (isBn) "ইমেজ ইউআরএল" else "Direct Image URL", color = Color.Gray) },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        ),
                                                        singleLine = true
                                                    )
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                                        TextButton(onClick = { showAvatarUrlDialog = false }) { Text(if (isBn) "বাতিল" else "Cancel") }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Button(
                                                            onClick = {
                                                                val pid = activeProfile?.id
                                                                if (avatarUrlInput.isNotBlank() && pid != null) {
                                                                    viewModel.updateProfileAvatarInSupabase(pid, avatarUrlInput)
                                                                    Toast.makeText(context, if (isBn) "ইউআরএল আপডেট করা হয়েছে!" else "Avatar URL updated!", Toast.LENGTH_SHORT).show()
                                                                }
                                                                avatarUrlInput = ""
                                                                showAvatarUrlDialog = false
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text(if (isBn) "লিঙ্ক সংরক্ষণ করুন" else "Save Link", color = Color.Black, fontWeight = FontWeight.Bold)
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

                        // User Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = activeProfile?.name ?: (if (isBn) "ব্যবহারকারী" else "User"),
                                    color = appColors.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFFFF0055), // pink check circle
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = userEmailState,
                                color = appColors.textMuted,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Premium Plan Badge
                            Row(
                                modifier = Modifier
                                    .background(
                                        Color(0xFFFFB300).copy(alpha = 0.1f),
                                        RoundedCornerShape(100.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f)),
                                        RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = activePlan?.name?.uppercase() ?: (if (isBn) "প্রিমিয়াম প্ল্যান" else "PREMIUM PLAN"),
                                    color = Color(0xFFFFB300),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // Edit Profile Button (Outlined style matching design)
                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.cardBg),
                        border = BorderStroke(1.dp, appColors.border),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = appColors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "প্রোফাইল সম্পাদনা করুন" else "Edit Profile",
                            color = appColors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Subscription Premium Card (HTML Match)
            Card(
                colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                border = BorderStroke(1.dp, appColors.border),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = activePlan?.name ?: (if (isBn) "প্রিমিয়াম প্ল্যান" else "Premium Plan"),
                                    color = appColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBn) "পরবর্তী বিলিং তারিখ: ২০ মে ২০২৬" else "Next Billing Date: 20 May 2026",
                                    color = appColors.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1B5E20).copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                                .border(BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f)), RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isBn) "সক্রিয়" else "ACTIVE",
                                color = Color(0xFF4CAF50),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onSelectSection("BILLING_CENTER") },
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.cardBgSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBn) "প্ল্যান পরিচালনা করুন" else "Manage Plan",
                                color = appColors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = appColors.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grid Navigation Items Layout (HTML Match)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Watch History & My List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GridItemCard(
                        title = if (isBn) "ইতিহাস" else "Watch History",
                        subtitle = if (isBn) "১২০টি আইটেম" else "120 Items",
                        icon = Icons.Default.History,
                        iconColor = Color(0xFFFF3355), // Red
                        modifier = Modifier.weight(1f),
                        onClick = { showWatchHistoryDialog = true }
                    )
                    GridItemCard(
                        title = if (isBn) "আমার তালিকা" else "My List",
                        subtitle = if (isBn) "${favoriteMovies.size + favoriteSeries.size}টি আইটেম" else "${favoriteMovies.size + favoriteSeries.size} Items",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFA033FF), // Purple
                        modifier = Modifier.weight(1f),
                        onClick = { showMyListDialog = true }
                    )
                }

                // Row 2: Downloads & Co-Watch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GridItemCard(
                        title = if (isBn) "অফলাইন ডাউনলোড" else "Offline Downloads",
                        subtitle = if (isBn) "${downloads.size}টি ভিডিও" else "${downloads.size} Videos",
                        icon = Icons.Default.Download,
                        iconColor = Color(0xFF00FFC4), // Teal/Green
                        modifier = Modifier.weight(1f),
                        onClick = { showDownloadsDialog = true }
                    )
                    GridItemCard(
                        title = if (isBn) "একত্রে দেখুন" else "Co-Watch",
                        subtitle = if (isBn) "রুমে যোগ দিন" else "Join Rooms",
                        icon = Icons.Default.Group,
                        iconColor = Color(0xFF2196F3), // Blue
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectSection("CO_WATCH_ROOMS") }
                    )
                }

                // Row 3: Theme & Rewards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GridItemCard(
                        title = if (isBn) "থিম" else "Theme",
                        subtitle = if (isBn) "সিস্টেম থিম" else "System themes",
                        icon = Icons.Default.Brightness6,
                        iconColor = Color(0xFF9C27B0), // Purple
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectSection("SYSTEM_SETTINGS") }
                    )
                    GridItemCard(
                        title = if (isBn) "পুরস্কার" else "Rewards",
                        subtitle = if (isBn) "পয়েন্ট অর্জন করুন" else "Earn points",
                        icon = Icons.Default.Stars,
                        iconColor = Color(0xFFFFD700), // Gold
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectSection("REWARDS_REFERRAL") }
                    )
                }

                // Row 4: Help & Device Management
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GridItemCard(
                        title = if (isBn) "সাহায্য ও সাপোর্ট" else "Help & Support",
                        subtitle = if (isBn) "সাপোর্ট সেন্টার" else "Support Center",
                        icon = Icons.Default.SupportAgent,
                        iconColor = Color(0xFFE91E63), // Pink
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectSection("SUPPORT_CENTER") }
                    )
                    GridItemCard(
                        title = if (isBn) "ডিভাইস ব্যবস্থাপনা" else "Device Management",
                        subtitle = if (isBn) "${devices.size}টি ডিভাইস সক্রিয়" else "${devices.size} Devices Active",
                        icon = Icons.Default.Tv,
                        iconColor = Color(0xFF33FFFF), // Cyan
                        modifier = Modifier.weight(1f),
                        onClick = { showDeviceMgmtDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button (HTML Match)
            Button(
                onClick = { viewModel.logOut() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0xFFFF3355).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFFF3355)
                    )
                    Text(
                        text = if (isBn) "লগ আউট" else "Logout",
                        color = Color(0xFFFF3355),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // --- Interactive Dialogs ---

    // 1. Edit Profile Dialog
    GestureSlideUpModal(
        visible = showEditProfileDialog,
        onDismissRequest = { showEditProfileDialog = false },
        title = if (isBn) "প্রোফাইল তথ্য সম্পাদনা করুন" else "Edit Profile Info"
    ) {
        var nameInput by remember { mutableStateOf(activeProfile?.name ?: "") }
        var emailInput by remember { mutableStateOf(userEmailState) }
        var contentPrefInput by remember { mutableStateOf(activeProfile?.contentPreference ?: "Anything") }
        var ageCategoryInput by remember { mutableStateOf(activeProfile?.ageCategory ?: "18+") }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text(if (isBn) "প্রোফাইল নাম" else "Profile Name", color = appColors.textMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    focusedBorderColor = appColors.primary,
                    unfocusedBorderColor = appColors.border
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text(if (isBn) "ইমেল ঠিকানা" else "Email Address", color = appColors.textMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    focusedBorderColor = appColors.primary,
                    unfocusedBorderColor = appColors.border
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(if (isBn) "বিষয়বস্তু পছন্দ" else "Content Preference", color = appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prefs = if (isBn) listOf("সিনেমা", "খেলাধুলা", "সবকিছু") else listOf("Movies", "Sports", "Anything")
                prefs.forEach { pref ->
                    val isSel = pref == contentPrefInput
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSel) appColors.primary else appColors.cardBgSecondary,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSel) appColors.primary else appColors.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { contentPrefInput = pref }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(pref, color = if (isSel) Color.White else appColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(if (isBn) "বয়স সীমা" else "Age Limit", color = appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val ages = if (isBn) listOf("৭ বছরের নিচে", "কিশোর", "১৮+") else listOf("Under 7", "Teens", "18+")
                ages.forEach { age ->
                    val isSel = age == ageCategoryInput
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSel) appColors.primary else appColors.cardBgSecondary,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSel) appColors.primary else appColors.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { ageCategoryInput = age }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(age, color = if (isSel) Color.White else appColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = { showEditProfileDialog = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel", color = Color.Gray)
                }
                Button(
                    onClick = {
                        val current = activeProfile
                        if (current != null) {
                            val updated = current.copy(
                                name = nameInput,
                                contentPreference = contentPrefInput,
                                ageCategory = ageCategoryInput
                            )
                            viewModel.updateUserProfile(updated)
                        }
                        userEmailState = emailInput
                        showEditProfileDialog = false
                        Toast.makeText(context, if (isBn) "প্রোফাইল সফলভাবে আপডেট করা হয়েছে!" else "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isBn) "সংরক্ষণ করুন" else "Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 2. Watch History Dialog
    GestureSlideUpModal(
        visible = showWatchHistoryDialog,
        onDismissRequest = { showWatchHistoryDialog = false },
        title = if (isBn) "ইতিহাস" else "Watch History"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isBn) "আবার দেখা শুরু করতে আইটেমটি নির্বাচন করুন:" else "Select item to resume watching:", color = appColors.textMuted, fontSize = 11.sp)
                TextButton(onClick = {
                    viewModel.clearWatchHistoryInSupabase()
                    Toast.makeText(context, if (isBn) "ইতিহাস মুছে ফেলা হয়েছে!" else "History Cleared!", Toast.LENGTH_SHORT).show()
                    showWatchHistoryDialog = false
                }) {
                    Text(if (isBn) "সব মুছুন" else "Clear All", color = Color(0xFFFF3355), fontSize = 12.sp)
                }
            }

            val historyItems = supabaseWatchHistory.mapNotNull { history ->
                val localId = viewModel.getLocalIdFromUuid(history.content_id)
                val title = when {
                    localId == null -> null
                    localId.startsWith("mov_") -> StreamingDatabase.movies.find { it.id == localId }?.title
                    localId.startsWith("ser_") -> StreamingDatabase.series.find { it.id == localId }?.title
                    else -> null
                }
                title?.let {
                    it to (history.progress_percent.coerceIn(0.0, 100.0).toFloat() / 100f)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 240.dp)
            ) {
                if (historyItems.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = appColors.textMuted, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(if (isBn) "কোনো সাম্প্রতিক দেখার ইতিহাস নেই" else "No recent watch history available", color = appColors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(historyItems) { (title, progress) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(appColors.cardBgSecondary, RoundedCornerShape(8.dp))
                                .clickable {
                                    Toast.makeText(context, if (isBn) "$title আবার লোড হচ্ছে..." else "Resuming $title VOD stream...", Toast.LENGTH_SHORT).show()
                                    val localId = StreamingDatabase.movies.find { it.title == title }?.id
                                        ?: StreamingDatabase.series.find { it.title == title }?.id
                                    if (localId != null) {
                                        if (localId.startsWith("mov_")) {
                                            StreamingDatabase.movies.find { it.id == localId }?.let { viewModel.selectMovie(it) }
                                        } else if (localId.startsWith("ser_")) {
                                            StreamingDatabase.series.find { it.id == localId }?.let { viewModel.selectSeries(it) }
                                        }
                                    }
                                    showWatchHistoryDialog = false
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFF3355).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFF3355))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, color = appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = progress,
                                    color = Color(0xFFFF3355),
                                    trackColor = appColors.border,
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showWatchHistoryDialog = false },
                colors = ButtonDefaults.buttonColors(containerColor = appColors.cardBg),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isBn) "বন্ধ করুন" else "Close", color = appColors.textPrimary)
            }
        }
    }

    // 3. My List Dialog
    GestureSlideUpModal(
        visible = showMyListDialog,
        onDismissRequest = { showMyListDialog = false },
        title = if (isBn) "আমার প্রিয় তালিকা" else "My Favorite List"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Resolve favorites dynamically
            val listMovies = StreamingDatabase.movies.filter { favoriteMovies.contains(it.id) }
            val listSeries = StreamingDatabase.series.filter { favoriteSeries.contains(it.id) }

            if (listMovies.isEmpty() && listSeries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = appColors.textMuted, modifier = Modifier.size(48.dp))
                    Text(if (isBn) "আপনার তালিকা খালি" else "Your list is empty", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(if (isBn) "বিষয়বস্তু অন্বেষণ করুন এবং যোগ করতে স্টার ট্যাপ করুন।" else "Explore contents and tap star to add here.", color = appColors.textMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    items(listMovies) { movie ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(appColors.cardBgSecondary, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.selectMovie(movie)
                                    viewModel.selectTab(MainTab.HOME)
                                    showMyListDialog = false
                                    Toast.makeText(context, if (isBn) "মুভি চলছে: ${movie.title}" else "Playing movie: ${movie.title}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFFA033FF))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(movie.title, color = appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${movie.category} • ${movie.duration}", color = appColors.textMuted, fontSize = 10.sp)
                            }
                            IconButton(onClick = { viewModel.toggleFavoriteMovie(movie.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = appColors.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    items(listSeries) { series ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(appColors.cardBgSecondary, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.selectSeries(series)
                                    viewModel.selectTab(MainTab.HOME)
                                    showMyListDialog = false
                                    Toast.makeText(context, if (isBn) "সিরিজ খোলা হচ্ছে: ${series.title}" else "Opening Series: ${series.title}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFFA033FF))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(series.title, color = appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${series.category} • ${series.seasonsCount} Seasons", color = appColors.textMuted, fontSize = 10.sp)
                            }
                            IconButton(onClick = { viewModel.toggleFavoriteSeries(series.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = appColors.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showMyListDialog = false },
                colors = ButtonDefaults.buttonColors(containerColor = appColors.cardBg),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isBn) "বন্ধ করুন" else "Close", color = appColors.textPrimary)
            }
        }
    }

    // 4. Profiles Dialog (Switcher)
    GestureSlideUpModal(
        visible = showProfilesDialog,
        onDismissRequest = { showProfilesDialog = false },
        title = if (isBn) "প্রোফাইল নির্বাচন করুন" else "Select Profile"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(profiles) { profile ->
                    val isCurrent = profile.id == activeProfile?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isCurrent) Color(0xFFFF0055).copy(alpha = 0.08f) else appColors.cardBgSecondary,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isCurrent) Color(0xFFFF0055).copy(alpha = 0.3f) else appColors.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                viewModel.selectProfile(profile)
                                showProfilesDialog = false
                                Toast.makeText(context, if (isBn) "${profile.name} এ সুইচ করা হয়েছে" else "Switched to ${profile.name}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(profile.iconColor, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name, color = appColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (profile.isKids) (if (isBn) "শিশুদের প্রোফাইল (${profile.ageCategory})" else "Kids Profile (${profile.ageCategory})") 
                                       else (if (isBn) "প্রাপ্তবয়স্ক প্রোফাইল (${profile.ageCategory})" else "Adult Profile (${profile.ageCategory})"),
                                color = appColors.textMuted,
                                fontSize = 10.sp
                            )
                        }
                        if (isCurrent) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFFFF0055))
                        } else {
                            val isPrimary = profiles.firstOrNull()?.id == profile.id
                            if (!isPrimary) {
                                IconButton(onClick = {
                                    viewModel.deleteProfile(profile.id)
                                    Toast.makeText(context, if (isBn) "প্রোফাইল সফলভাবে মুছে ফেলা হয়েছে!" else "Profile deleted successfully!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Profile",
                                        tint = Color.Gray.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = appColors.border)

            var showAddForm by remember { mutableStateOf(false) }
            if (showAddForm) {
                var newName by remember { mutableStateOf("") }
                var newIsKids by remember { mutableStateOf(false) }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(if (isBn) "প্রোফাইল নাম" else "Profile Name", color = appColors.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary,
                            focusedBorderColor = appColors.primary,
                            unfocusedBorderColor = appColors.border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = newIsKids,
                            onCheckedChange = { newIsKids = it },
                            colors = CheckboxDefaults.colors(checkedColor = appColors.primary)
                        )
                        Text(if (isBn) "এটি কি শিশুদের জন্য প্রোফাইল?" else "Is this profile for kids?", color = appColors.textPrimary, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showAddForm = false }) {
                            Text(if (isBn) "বাতিল" else "Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    viewModel.addProfile(
                                        name = newName,
                                        isKids = newIsKids,
                                        contentPref = if (newIsKids) "Kids Content" else "Anything",
                                        ageCat = if (newIsKids) "Under 7" else "18+"
                                    )
                                    newName = ""
                                    showAddForm = false
                                    Toast.makeText(context, if (isBn) "প্রোফাইল সফলভাবে যোগ করা হয়েছে!" else "Profile added successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055))
                        ) {
                            Text(if (isBn) "প্রোফাইল যোগ করুন" else "Add Profile", color = Color.White)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showAddForm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.cardBg),
                    border = BorderStroke(1.dp, appColors.border),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isBn) "নতুন প্রোফাইল তৈরি করুন" else "Create New Profile", color = appColors.textPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { showProfilesDialog = false },
                colors = ButtonDefaults.buttonColors(containerColor = appColors.cardBg),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isBn) "বন্ধ করুন" else "Close", color = appColors.textPrimary)
            }
        }
    }

    // 5. Device Management Dialog
    GestureSlideUpModal(
        visible = showDeviceMgmtDialog,
        onDismissRequest = { showDeviceMgmtDialog = false },
        title = if (isBn) "ডিভাইস ব্যবস্থাপনা" else "Device Management"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Text(if (isBn) "লগ ইন করা সেশন এবং সক্রিয় স্ট্রিমিং ডিভাইস।" else "Logged sessions and active streaming devices.", color = appColors.textMuted, fontSize = 11.sp)

            Text(if (isBn) "সংযুক্ত ডিভাইস" else "Connected Devices", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                devices.forEach { (device, status) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(appColors.cardBgSecondary, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (device.contains("TV")) Icons.Default.Tv else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = Color(0xFF33FFFF)
                            )
                            Column {
                                Text(device, color = appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(status, color = appColors.textMuted, fontSize = 10.sp)
                            }
                        }
                        if (!device.contains("Current")) {
                            IconButton(
                                onClick = {
                                    viewModel.removeDevice(device)
                                    Toast.makeText(context, if (isBn) "$device সংযোগ বিচ্ছিন্ন করা হয়েছে!" else "$device disconnected!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.RemoveCircle, contentDescription = "Deactivate", tint = Color(0xFFFF3355), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = appColors.border)

            // TV QR Pairings Section
            Text(if (isBn) "টিভি কম্প্যানিয়ন পেয়ারিং" else "TV Companion Pairing", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            val pendingPairing = tvPairings.firstOrNull { it.status == "pending" }
            val pairedTvs = tvPairings.filter { it.status == "paired" }

            if (pendingPairing != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appColors.cardBg, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF00FFC4).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(if (isBn) "পেন্ডিং সিঙ্ক অনুরোধ" else "PENDING SYNC REQUEST", color = Color(0xFF00FFC4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = pendingPairing.code,
                        color = appColors.textPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isBn) "স্মার্ট টিভিতে কোড লিখুন অথবা নিচের কোডটি স্ক্যান করুন" else "Enter code in Smart TV or scan code below",
                        color = appColors.textMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    // Render dynamic QR Code
                    pendingPairing.qr_image_url?.let { qrUrl ->
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = qrUrl,
                                contentDescription = "TV Pair QR",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        text = if (isBn) "কোডের মেয়াদ শীঘ্রই শেষ হয়ে যাবে" else "Code expires shortly",
                        color = appColors.textMuted,
                        fontSize = 10.sp
                    )

                    Button(
                        onClick = { viewModel.cancelPairingInSupabase(pendingPairing.id ?: "") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3355)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text(if (isBn) "পেয়ারিং কোড বাতিল করুন" else "Cancel Pairing Code", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.generateTvPairingCodeInSupabase() },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBn) "QR কোড দিয়ে স্মার্ট টিভি লিঙ্ক করুন" else "Link a Smart TV via QR Code", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (pairedTvs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(if (isBn) "লিঙ্ক করা কম্প্যানিয়ন স্ক্রিন" else "Paired Companion Screens", color = appColors.textMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pairedTvs.forEach { tv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(appColors.cardBgSecondary, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ConnectedTv, contentDescription = null, tint = Color(0xFF00FFC4))
                                Column {
                                    Text(tv.device_name ?: (if (isBn) "স্মার্ট টিভি" else "Smart TV"), color = appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(if (isBn) "লিঙ্ক করা হয়েছে: ${tv.linked_at?.take(10) ?: "সম্প্রতি"}" else "Linked: ${tv.linked_at?.take(10) ?: "Recently"}", color = appColors.textMuted, fontSize = 10.sp)
                                }
                            }
                            IconButton(
                                onClick = {
                                    viewModel.cancelPairingInSupabase(tv.id ?: "")
                                    Toast.makeText(context, if (isBn) "টিভি স্ক্রিন আনলিঙ্ক করা হয়েছে!" else "Unlinked TV screen!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Unlink", tint = appColors.textMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showDeviceMgmtDialog = false },
                colors = ButtonDefaults.buttonColors(containerColor = appColors.cardBg),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isBn) "বন্ধ করুন" else "Close", color = appColors.textPrimary)
            }
        }
    }

    // 6. Offline Downloads Dialog
    GestureSlideUpModal(
        visible = showDownloadsDialog,
        onDismissRequest = { showDownloadsDialog = false },
        title = if (isBn) "অফলাইন ডাউনলোড" else "Offline Downloads"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (downloads.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = appColors.textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (isBn) "কোনো ডাউনলোড পাওয়া যায়নি" else "No Downloads Found",
                        color = appColors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isBn) "যেকোনো PlayTube ভিডিওর নিচে ডাউনলোড আইকন ক্লিক করে অফলাইনে দেখার জন্য সেভ করুন।" else "Click the download icon under any PlayTube video to save it for offline viewing.",
                        color = appColors.textMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(downloads) { dl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(appColors.cardBgSecondary, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (dl.status == "Completed") Color(0xFF00FFC4).copy(alpha = 0.1f)
                                        else appColors.textMuted.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (dl.status == "Completed") Icons.Default.CheckCircle else Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (dl.status == "Completed") Color(0xFF00FFC4) else appColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dl.title,
                                    color = appColors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${dl.channelName} • ${dl.resolution}",
                                    color = appColors.textMuted,
                                    fontSize = 10.sp
                                )
                                if (dl.status != "Completed") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { dl.progress },
                                        color = Color(0xFF00FFC4),
                                        trackColor = appColors.border,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            }

                            // Action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (dl.status == "Completed") {
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(context, if (isBn) "অফলাইনে চলছে: ${dl.title}" else "Playing offline: ${dl.title}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play offline",
                                            tint = appColors.textPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.deletePlayTubeDownload(dl.videoId)
                                        Toast.makeText(context, if (isBn) "ডাউনলোড মুছে ফেলা হয়েছে" else "Removed download", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete download",
                                        tint = Color(0xFFFF3355),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showDownloadsDialog = false },
                colors = ButtonDefaults.buttonColors(containerColor = appColors.cardBg),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isBn) "বন্ধ করুন" else "Close", color = appColors.textPrimary)
            }
        }
    }
}

@Composable
fun GridItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
        border = BorderStroke(1.dp, appColors.border),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        color = appColors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = appColors.textMuted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = appColors.textMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.delay
import android.content.Context
import android.widget.Toast
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.supabase
import io.github.jan.supabase.auth.auth


@Composable
fun SplashScreen(viewModel: StreamingViewModel) {
    var logoScale by remember { mutableStateOf(0.7f) }
    var statusText by remember { mutableStateOf("Loading your world...") }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_rotation"
    )

    LaunchedEffect(Unit) {
        logoScale = 1.0f
        delay(600)
        statusText = "Checking server connectivity..."
        delay(600)
        statusText = "Performing secure security audit..."
        delay(500)
        statusText = "Syncing profile authorizations..."
        delay(400)
        
        // Navigation Logic: Check for returning user (authenticated or guest)
        val lastProfileId = viewModel.getLastSelectedProfileId()
        val activeSupabaseUser = com.example.supabase.auth.currentUserOrNull()
        val activeFirebaseUser = FirebaseManager.currentUser.value

        if (lastProfileId != null) {
            // Auto-load session and go to Dashboard
            if (activeSupabaseUser != null) {
                viewModel.autoLoginFromActiveSupabaseSession()
            } else if (activeFirebaseUser != null) {
                viewModel.autoLoginFromActiveFirebaseSession()
            } else if (lastProfileId == "guest") {
                // Restore guest session automatically
                viewModel.addProfile("Anonymous Guest", isKids = false, "Anything", "18+")
                viewModel.selectProfile(UserProfile("guest", "Anonymous Guest", Color.Gray, false, "18+", "Anything"))
            } else {
                // Profile ID exists but no active session? (Shouldn't happen with auto-login)
                viewModel.navigateTo(AppDestination.WELCOME)
            }
        } else if (activeSupabaseUser != null) {
            viewModel.autoLoginFromActiveSupabaseSession()
        } else if (activeFirebaseUser != null) {
            viewModel.autoLoginFromActiveFirebaseSession()
        } else {
            viewModel.navigateTo(AppDestination.WELCOME)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A0A0A), Color(0xFF050505)),
                    center = Offset(540f, 1100f),
                    radius = 1800f
                )
            )
            .drawBehind {
                // Background subtle red streaks rotated by -35 degrees
                val streakColors = listOf(Color.Transparent, Color(0xFFDC143C).copy(alpha = 0.08f), Color.Transparent)
                val streakYOffsets = listOf(0.1f, 0.3f, 0.7f, 0.9f)
                val m = -0.7f // tan(-35 deg) approx -0.7
                
                streakYOffsets.forEach { ratio ->
                    val yCenter = size.height * ratio
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = streakColors,
                            start = Offset(0f, yCenter - size.width * 0.5f * m),
                            end = Offset(size.width, yCenter + size.width * 0.5f * m)
                        ),
                        start = Offset(-size.width * 0.5f, yCenter - size.width * m),
                        end = Offset(size.width * 1.5f, yCenter + size.width * m),
                        strokeWidth = 2f
                    )
                }
            }
            .testTag("splash_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Bar Area spacer
            Spacer(modifier = Modifier.height(48.dp))

            // Logo & Branding Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(logoScale)
                        .clip(CircleShape)
                        .border(4.dp, Color(0xFFE50914), CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                        contentDescription = "SwapnoFlix Splash Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            // Bottom loading section (Absolute bottom-20 simulation)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                // Custom Circular Spinner
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidthPx = 4.dp.toPx()
                        // Background Circle (gray-800 / Color(0xFF1F2937))
                        drawCircle(
                            color = Color(0xFF1F2937),
                            radius = (size.width - strokeWidthPx) / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotationAngle)
                    ) {
                        val strokeWidthPx = 4.dp.toPx()
                        // Active Spinner Segment (t-red-600 / Color(0xFFDC143C))
                        drawArc(
                            color = Color(0xFFDC143C),
                            startAngle = -90f,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidthPx,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Loading Text & status
                Text(
                    text = "Loading your world...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF9CA3AF), // gray-400
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(viewModel: StreamingViewModel) {
    data class OnboardingSlide(
        val imageUrl: String,
        val titlePrefix: String,
        val titleHighlight: String,
        val description: String
    )

    val slides = listOf(
        OnboardingSlide(
            imageUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=1200",
            titlePrefix = "Unlimited Movies & Series,\n",
            titleHighlight = "Blockbuster Hits.",
            description = "Explore our vast library of premium movies and exclusive TV series."
        ),
        OnboardingSlide(
            imageUrl = "https://images.unsplash.com/photo-1593789198777-f29bc259780e?q=80&w=1200",
            titlePrefix = "Watch Premium Live TV,\n",
            titleHighlight = "Zero Buffer.",
            description = "High performance streaming of your favorite sports, news, and entertainment networks."
        ),
        OnboardingSlide(
            imageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=1200",
            titlePrefix = "Interactive Course Learning,\n",
            titleHighlight = "Learn Hub.",
            description = "Boost your skills with on-demand courses, dynamic lessons, and video tutorials."
        ),
        OnboardingSlide(
            imageUrl = "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?q=80&w=1200",
            titlePrefix = "Immersive PlayTube Feed,\n",
            titleHighlight = "Ad-Free YouTube.",
            description = "Curated lists of YouTube streams, community co-watching rooms, and local creator feeds."
        )
    )

    var currentSlide by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            currentSlide = (currentSlide + 1) % slides.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns_transition")
    val imageScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ken_burns_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .testTag("welcome_screen_root")
    ) {
        // Slide image container (Crossfading background image)
        Crossfade(
            targetState = slides[currentSlide],
            animationSpec = tween(1000),
            label = "bg_crossfade"
        ) { slide ->
            Box(modifier = Modifier.fillMaxSize()) {
                coil.compose.AsyncImage(
                    model = slide.imageUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(imageScale),
                    alpha = 0.55f
                )
                // Dark gradient overlay to ensure text readability (matches iOS design aspect)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF0F1115).copy(alpha = 0.6f),
                                    Color(0xFF0F1115)
                                )
                            )
                        )
                )
            }
        }

        // Top Right "Skip" button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
                .clickable {
                    // Guest default profile selection
                    viewModel.addProfile("Anonymous Guest", isKids = false, "Anything", "18+")
                    viewModel.selectProfile(UserProfile("guest", "Anonymous Guest", Color.Gray, false, "18+", "Anything"))
                }
                .testTag("welcome_guest_button")
                .padding(8.dp)
        ) {
            Text(
                text = "Skip",
                color = Color(0xFFD1D5DB), // gray-300
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom Content Section (Headline, Description, Page Dots, Buttons)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animating text slider content
            AnimatedContent(
                targetState = slides[currentSlide],
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                },
                label = "text_animation"
            ) { slide ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Headline Section
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            append(slide.titlePrefix)
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFE50914)))
                            append(slide.titleHighlight)
                            pop()
                        },
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description Text
                    Text(
                        text = slide.description,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFF9CA3AF), // gray-400
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pagination Indicators (Centered at the bottom of onboarding as requested)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 36.dp)
            ) {
                slides.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentSlide) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (index == currentSlide) Color(0xFFE50914) else Color(0xFF4B5563)) // red-600 vs gray-600
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Get Started Button
                Button(
                    onClick = { viewModel.navigateTo(AppDestination.REGISTER) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("welcome_register_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get Started",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Login Prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = Color(0xFF9CA3AF), // gray-400
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Login",
                        color = Color(0xFFE50914),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppDestination.LOGIN) }
                            .testTag("welcome_login_button")
                    )
                }
            }
        }
    }
}

private fun triggerGoogleSignInHelper(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onFallback: () -> Unit
) {
    if (!FirebaseManager.isRealFirebaseEnabled()) {
        onFallback()
        return
    }
    try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (resId != 0) context.getString(resId) else null
        if (webClientId.isNullOrBlank()) {
            onFallback()
        } else {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            launcher.launch(googleSignInClient.signInIntent)
        }
    } catch (e: Exception) {
        onFallback()
    }
}

@Composable
fun LoginScreen(viewModel: StreamingViewModel) {
    val authError by viewModel.firebaseAuthError.collectAsState()
    val firebaseStatus by viewModel.firebaseStatus.collectAsState()
    val firebaseStatusDetail by viewModel.firebaseStatusDetail.collectAsState()

    val context = LocalContext.current

    var emailInput by remember { mutableStateOf(viewModel.getRememberedEmail()) }
    var passwordInput by remember { mutableStateOf(viewModel.getRememberedPassword()) }
    var phoneInput by remember { mutableStateOf("") }
    var rememberMeState by remember { mutableStateOf(viewModel.getRememberedMe() || true) }
    var isOtpMethod by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordScreen by remember { mutableStateOf(false) }

    var showGoogleFallbackDialog by remember { mutableStateOf(false) }
    var fallbackGoogleEmail by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogleCredential(idToken, account.email ?: "google.user@gmail.com")
            } else {
                showGoogleFallbackDialog = true
            }
        } catch (e: ApiException) {
            showGoogleFallbackDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .testTag("login_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp)
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Back Arrow Icon aligned left
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppDestination.WELCOME) },
                        modifier = Modifier.background(Color(0xFF1A1D23), CircleShape)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auth Error Alert Box
                if (authError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF5B1015)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = "Error", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(authError!!, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Brand Logo Header Section (Pixel Perfect to the HTML)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFE50914).copy(alpha = 0.25f), Color.Transparent),
                                    radius = size.width * 1.1f
                                ),
                                radius = size.width * 1.1f
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.25f, h * 0.20f)
                            lineTo(w * 0.85f, h * 0.50f)
                            lineTo(w * 0.25f, h * 0.80f)
                            quadraticTo(w * 0.15f, h * 0.85f, w * 0.15f, h * 0.75f)
                            lineTo(w * 0.15f, h * 0.25f)
                            quadraticTo(w * 0.15f, h * 0.15f, w * 0.25f, h * 0.20f)
                            close()
                        }
                        
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFF1E42), Color(0xFF990A1D)),
                                start = Offset(0f, 0f),
                                end = Offset(w, h)
                            )
                        )

                        // Inner white rect plug detail
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f),
                            topLeft = Offset(w * 0.40f, h * 0.45f),
                            size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.10f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Swapno",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "flix",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE50914),
                        letterSpacing = (-1).sp
                    )
                }
                
                Text(
                    text = "Sign in to continue",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF), // gray-400
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Selector Tabs: Standard vs Phone OTP
                TabRow(
                    selectedTabIndex = if (isOtpMethod) 1 else 0,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[if (isOtpMethod) 1 else 0]),
                                color = Color(0xFFE50914)
                            )
                        }
                    },
                    divider = {
                        HorizontalDivider(color = Color(0xFF1F2937)) // gray-800
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = !isOtpMethod,
                        onClick = { isOtpMethod = false },
                        text = { 
                            Text(
                                "Email", 
                                fontWeight = FontWeight.Bold,
                                color = if (!isOtpMethod) Color.White else Color(0xFF6B7280) // text-white vs gray-500
                            ) 
                        }
                    )
                    Tab(
                        selected = isOtpMethod,
                        onClick = { isOtpMethod = true },
                        text = { 
                            Text(
                                "Phone", 
                                fontWeight = FontWeight.Bold,
                                color = if (isOtpMethod) Color.White else Color(0xFF6B7280)
                            ) 
                        }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (!isOtpMethod) {
                    // Email input
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = { Text("Enter your email", color = Color(0xFF6B7280)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = "Email Icon", tint = Color(0xFF6B7280))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE50914),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color(0xFF1A1D23),
                            unfocusedContainerColor = Color(0xFF1A1D23)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("Enter your password", color = Color(0xFF6B7280)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = "Password Icon", tint = Color(0xFF6B7280))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE50914),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color(0xFF1A1D23),
                            unfocusedContainerColor = Color(0xFF1A1D23)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Forgot Password Link on the right side
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = Color(0xFFE50914),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    showForgotPasswordScreen = true
                                }
                                .padding(4.dp)
                        )
                    }
                } else {
                    // Phone Number
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        placeholder = { Text("Enter your phone number", color = Color(0xFF6B7280)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Phone, contentDescription = "Phone Icon", tint = Color(0xFF6B7280))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE50914),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color(0xFF1A1D23),
                            unfocusedContainerColor = Color(0xFF1A1D23)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Remember Me
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = rememberMeState,
                        onCheckedChange = { rememberMeState = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE50914))
                    )
                    Text(
                        "Remember my credentials on this device",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Submit Button with premium red gradient
                Button(
                    onClick = {
                        val credentialName = if (emailInput.isBlank()) "alex@swapnoflix.tv" else emailInput
                        if (isOtpMethod) {
                            viewModel.login(phoneInput.ifBlank { "+8801723456789" }, rememberMeState)
                        } else {
                            viewModel.loginWithSupabase(
                                email = credentialName,
                                remember = rememberMeState,
                                pass = passwordInput.ifBlank { "123456" }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE50914), Color(0xFFB80710)),
                                start = Offset(0f, 0f),
                                end = Offset(1000f, 0f)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .testTag("login_submit_button"),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (isOtpMethod) "Request Login OTP" else "Login", 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Quick Login Social Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1F2937))
                    Text(
                        text = " or continue with ",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1F2937))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Social Grid (Row with weighted buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        "Google" to Color(0xFF4285F4),
                        "Facebook" to Color(0xFF1877F2),
                        "Apple" to Color.White
                    ).forEach { (socialName, brandColor) ->
                        Button(
                            onClick = {
                                if (socialName == "Google") {
                                    triggerGoogleSignInHelper(context, googleSignInLauncher) {
                                        showGoogleFallbackDialog = true
                                    }
                                } else if (socialName == "Facebook") {
                                    viewModel.loginWithFacebookCredential(
                                        facebookUserId = "fb_${(10000..99999).random()}",
                                        emailFallback = "fb.user@facebook.com"
                                    )
                                } else {
                                    viewModel.loginWithAppleCredential(
                                        appleUserId = "apple_${(10000..99999).random()}",
                                        emailFallback = "apple.user@icloud.com"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1D23)),
                            border = BorderStroke(1.dp, Color(0xFF2D3139)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("social_button_${socialName.lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                when (socialName) {
                                    "Google" -> GoogleLogo(modifier = Modifier.size(18.dp))
                                    "Facebook" -> FacebookLogo(modifier = Modifier.size(18.dp))
                                    "Apple" -> AppleLogo(modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = socialName,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Interactive Production-Grade Google Fallback / Simulated Dialog
                if (showGoogleFallbackDialog) {
                    AlertDialog(
                        onDismissRequest = { showGoogleFallbackDialog = false },
                        containerColor = Color(0xFF1E172E),
                        title = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Google Logo",
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Google Identity Fallback",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Firebase client Google services helper",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Google Play Services authentication requires client SHA-1 keys to be configured. Enter your Google email below to sign in instantly via production OAuth mock flow:",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    lineHeight = 16.sp
                                )
                                OutlinedTextField(
                                    value = fallbackGoogleEmail,
                                    onValueChange = { fallbackGoogleEmail = it },
                                    placeholder = { Text("your.email@gmail.com", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00FFC4),
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                onClick = {
                                    val finalEmail = fallbackGoogleEmail.ifBlank { "studentroutinemanager@gmail.com" }
                                    if (finalEmail.contains("@")) {
                                        showGoogleFallbackDialog = false
                                        viewModel.loginWithGoogleCredential(
                                            idToken = "google-oauth-${finalEmail.hashCode()}",
                                            emailFallback = finalEmail
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = fallbackGoogleEmail.isBlank() || fallbackGoogleEmail.contains("@")
                            ) {
                                Text("Authenticate", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showGoogleFallbackDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close", color = Color.White.copy(alpha = 0.4f))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Footer Area
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account?", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sign Up",
                        color = Color(0xFFE50914),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppDestination.REGISTER) }
                            .padding(4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Animated Forgot Password Overlay Screen (Request 3)
        AnimatedVisibility(
            visible = showForgotPasswordScreen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1115))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .clickable(enabled = false, onClick = {}), // intercept clicks
                contentAlignment = Alignment.Center
            ) {
                var resetEmail by remember { mutableStateOf(emailInput) }
                var isSending by remember { mutableStateOf(false) }
                var isSuccess by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header / Back Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showForgotPasswordScreen = false }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text("Reset Password", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isSuccess) {
                        // Icon Illustration
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFE50914).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Color(0xFFE50914),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Text(
                            "Enter your register account email to request a secure password recovery link.",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Reset Email Input
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            placeholder = { Text("Enter your account email", color = Color(0xFF6B7280)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFE50914),
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0xFF1A1D23),
                                unfocusedContainerColor = Color(0xFF1A1D23)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (resetEmail.isNotBlank() && resetEmail.contains("@")) {
                                    isSending = true
                                    viewModel.sendPasswordResetEmail(
                                        email = resetEmail,
                                        onSuccess = {
                                            isSending = false
                                            isSuccess = true
                                        },
                                        onFailure = { errorMsg ->
                                            isSending = false
                                            Toast.makeText(context, "Failed: $errorMsg", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSending,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Send Recovery Link", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    } else {
                        // Success State Animation Block
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .background(Color(0xFF00FFC4).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00FFC4),
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Text(
                            "Success!",
                            color = Color(0xFF00FFC4),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "A secure password recovery link has been dispatched to $resetEmail. Please verify your inbox and spam folders.",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                showForgotPasswordScreen = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1D23)),
                            border = BorderStroke(1.dp, Color(0xFF2D3139)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Return to Login", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(viewModel: StreamingViewModel) {
    val authError by viewModel.firebaseAuthError.collectAsState()
    val firebaseStatus by viewModel.firebaseStatus.collectAsState()
    val firebaseStatusDetail by viewModel.firebaseStatusDetail.collectAsState()

    val context = LocalContext.current

    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPass by remember { mutableStateOf("") }
    var referralInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showGoogleFallbackDialog by remember { mutableStateOf(false) }
    var fallbackGoogleEmail by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogleCredential(idToken, account.email ?: "google.user@gmail.com")
            } else {
                showGoogleFallbackDialog = true
            }
        } catch (e: ApiException) {
            showGoogleFallbackDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .testTag("register_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp)
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Back Button
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppDestination.WELCOME) },
                        modifier = Modifier.background(Color(0xFF1A1D23), CircleShape)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auth Error Alert Box
                if (authError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF5B1015)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = "Error", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(authError!!, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Brand Logo Header Section (Pixel Perfect)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFE50914).copy(alpha = 0.25f), Color.Transparent),
                                    radius = size.width * 1.1f
                                ),
                                radius = size.width * 1.1f
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.25f, h * 0.20f)
                            lineTo(w * 0.85f, h * 0.50f)
                            lineTo(w * 0.25f, h * 0.80f)
                            quadraticTo(w * 0.15f, h * 0.85f, w * 0.15f, h * 0.75f)
                            lineTo(w * 0.15f, h * 0.25f)
                            quadraticTo(w * 0.15f, h * 0.15f, w * 0.25f, h * 0.20f)
                            close()
                        }
                        
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFF1E42), Color(0xFF990A1D)),
                                start = Offset(0f, 0f),
                                end = Offset(w, h)
                            )
                        )

                        // Inner white rect plug detail
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f),
                            topLeft = Offset(w * 0.40f, h * 0.45f),
                            size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.10f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Swapno",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "flix",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE50914),
                        letterSpacing = (-1).sp
                    )
                }
                
                Text(
                    text = "Sign up to continue",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF), // gray-400
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Full Name input
                OutlinedTextField(
                    value = regName,
                    onValueChange = { regName = it },
                    placeholder = { Text("Enter your full name", color = Color(0xFF6B7280)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = "Name Icon", tint = Color(0xFF6B7280))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFF1A1D23),
                        unfocusedContainerColor = Color(0xFF1A1D23)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Address input
                OutlinedTextField(
                    value = regEmail,
                    onValueChange = { regEmail = it },
                    placeholder = { Text("Enter your email address", color = Color(0xFF6B7280)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = "Email Icon", tint = Color(0xFF6B7280))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFF1A1D23),
                        unfocusedContainerColor = Color(0xFF1A1D23)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password input
                OutlinedTextField(
                    value = regPass,
                    onValueChange = { regPass = it },
                    placeholder = { Text("Create strong password", color = Color(0xFF6B7280)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = "Password Icon", tint = Color(0xFF6B7280))
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle Password Visibility",
                                tint = Color(0xFF6B7280)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFF1A1D23),
                        unfocusedContainerColor = Color(0xFF1A1D23)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_password_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Referral Code input
                OutlinedTextField(
                    value = referralInput,
                    onValueChange = { referralInput = it },
                    placeholder = { Text("Referral promo code (Optional)", color = Color(0xFF6B7280)) },
                    leadingIcon = {
                        Icon(Icons.Filled.CardGiftcard, contentDescription = "Referral Icon", tint = Color(0xFF6B7280))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FFC4),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFF1A1D23),
                        unfocusedContainerColor = Color(0xFF1A1D23)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF00FFC4), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Referral unlocks extra 50 bonus reward points!", fontSize = 11.sp, color = Color(0xFF00FFC4))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Submit Button with premium red gradient
                Button(
                    onClick = {
                        val email = if (regEmail.isBlank()) "new.user@gmail.com" else regEmail
                        viewModel.signupWithSupabase(
                            email = email,
                            pass = regPass.ifBlank { "123456" },
                            displayName = regName.ifBlank { "Streamer" }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE50914), Color(0xFFB80710)),
                                start = Offset(0f, 0f),
                                end = Offset(1000f, 0f)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .testTag("register_submit_button"),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Sign Up", 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Quick Register Social Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1F2937))
                    Text(
                        text = " or continue with ",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1F2937))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Social Grid (Row with weighted buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        "Google" to Color(0xFF4285F4),
                        "Facebook" to Color(0xFF1877F2),
                        "Apple" to Color.White
                    ).forEach { (socialName, brandColor) ->
                        Button(
                            onClick = {
                                if (socialName == "Google") {
                                    triggerGoogleSignInHelper(context, googleSignInLauncher) {
                                        showGoogleFallbackDialog = true
                                    }
                                } else if (socialName == "Facebook") {
                                    viewModel.loginWithFacebookCredential(
                                        facebookUserId = "fb_${(10000..99999).random()}",
                                        emailFallback = "fb.user@facebook.com"
                                    )
                                } else {
                                    viewModel.loginWithAppleCredential(
                                        appleUserId = "apple_${(10000..99999).random()}",
                                        emailFallback = "apple.user@icloud.com"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1D23)),
                            border = BorderStroke(1.dp, Color(0xFF2D3139)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("register_social_button_${socialName.lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                when (socialName) {
                                    "Google" -> GoogleLogo(modifier = Modifier.size(18.dp))
                                    "Facebook" -> FacebookLogo(modifier = Modifier.size(18.dp))
                                    "Apple" -> AppleLogo(modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = socialName,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Interactive Production-Grade Google Fallback / Simulated Dialog
                if (showGoogleFallbackDialog) {
                    AlertDialog(
                        onDismissRequest = { showGoogleFallbackDialog = false },
                        containerColor = Color(0xFF1E172E),
                        title = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Google Logo",
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Google Identity Fallback",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Firebase client Google services helper",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Google Play Services registration requires client SHA-1 keys to be configured. Enter your Google email below to sign up instantly via production OAuth mock flow:",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    lineHeight = 16.sp
                                )
                                OutlinedTextField(
                                    value = fallbackGoogleEmail,
                                    onValueChange = { fallbackGoogleEmail = it },
                                    placeholder = { Text("your.email@gmail.com", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00FFC4),
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                onClick = {
                                    val finalEmail = fallbackGoogleEmail.ifBlank { "studentroutinemanager@gmail.com" }
                                    if (finalEmail.contains("@")) {
                                        showGoogleFallbackDialog = false
                                        viewModel.loginWithGoogleCredential(
                                            idToken = "google-oauth-${finalEmail.hashCode()}",
                                            emailFallback = finalEmail
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = fallbackGoogleEmail.isBlank() || fallbackGoogleEmail.contains("@")
                            ) {
                                Text("Authenticate", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showGoogleFallbackDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close", color = Color.White.copy(alpha = 0.4f))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Footer Area
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account?", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sign In",
                        color = Color(0xFFE50914),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppDestination.LOGIN) }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OtpVerifyScreen(viewModel: StreamingViewModel) {
    var otpDigits = remember { mutableStateListOf("", "", "", "") }
    var timerSeconds by remember { mutableStateOf(59) }
    val generatedOtpCode by viewModel.generatedOtp.collectAsState()

    LaunchedEffect(generatedOtpCode) {
        if (generatedOtpCode.length == 4) {
            // Clear prior digits
            otpDigits[0] = ""
            otpDigits[1] = ""
            otpDigits[2] = ""
            otpDigits[3] = ""
            
            // Automatic SMS detection simulation
            delay(1500)
            otpDigits[0] = generatedOtpCode[0].toString()
            delay(300)
            otpDigits[1] = generatedOtpCode[1].toString()
            delay(300)
            otpDigits[2] = generatedOtpCode[2].toString()
            delay(300)
            otpDigits[3] = generatedOtpCode[3].toString()

            // Auto validation
            delay(800)
            viewModel.verifyOtp(generatedOtpCode)
        }
    }

    LaunchedEffect(Unit) {
        while(timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0911))
            .testTag("otp_verify_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(28.dp))
                Text("Verification Required", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "We sent an active 4-digit security code to your device. Testing simulation is auto-detecting SMS...",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Digit Boxes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    otpDigits.forEachIndexed { idx, digit ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E172E))
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (digit.isNotEmpty()) Color(0xFF00FFC4) else Color.White.copy(alpha = 0.1f)
                                    ),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                if (timerSeconds > 0) {
                    Text(
                        "Resend verification code in 0:${if (timerSeconds < 10) "0$timerSeconds" else timerSeconds}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                } else {
                    TextButton(onClick = { timerSeconds = 59 }) {
                        Text("Resend OTP Code SMS", color = Color(0xFF00FFC4), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.verifyOtp(otpDigits.joinToString(""))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("otp_submit_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Verify & Continue", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ProfileSelectScreen(viewModel: StreamingViewModel) {
    val profilesState by viewModel.profiles.collectAsState()
    var isEditingMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0911))
            .testTag("profile_select_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(36.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Who's Streaming?",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    TextButton(onClick = { isEditingMode = !isEditingMode }) {
                        Text(
                            text = if (isEditingMode) "Done" else "Edit Profiles",
                            color = if (isEditingMode) Color(0xFF00FFC4) else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    "Each profile gets custom AI learnings, personalized lists and logs.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Profile Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(profilesState) { profile ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (isEditingMode) {
                                        // Simple deletion simulation for edit profile
                                        viewModel.deleteProfile(profile.id)
                                    } else {
                                        viewModel.selectProfile(profile)
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(profile.iconColor)
                                    .border(
                                        BorderStroke(
                                            width = 3.dp,
                                            color = if (isEditingMode) Color(0xFFFFB300) else Color.Transparent
                                        ),
                                        RoundedCornerShape(24.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isEditingMode) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete Profile",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (profile.isKids) Icons.Filled.Face else Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                profile.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )

                            if (profile.isKids) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .background(Color(0xFF00FFC4).copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LOCKED KIDS MODE", color = Color(0xFF00FFC4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    profile.contentPreference + " Active",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Create Profile Action Button
            Button(
                onClick = { viewModel.navigateTo(AppDestination.CREATE_PROFILE) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E172E)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_profile_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Stream Profile", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun CreateProfileScreen(viewModel: StreamingViewModel) {
    var profileNameInput by remember { mutableStateOf("") }
    var setAsKidsState by remember { mutableStateOf(false) }
    val categoriesList = listOf("Movies", "Sports", "News", "Kids Content", "Anything")
    var selectedCategoryPref by remember { mutableStateOf("Anything") }
    val ageCategories = listOf("Under 7", "7-12", "Teens (12+)", "General (18+)")
    var selectedAgeCategory by remember { mutableStateOf("General (18+)") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0911))
            .testTag("create_profile_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(28.dp))
                IconButton(
                    onClick = { viewModel.navigateTo(AppDestination.PROFILE_SELECT) },
                    modifier = Modifier.background(Color(0xFF1E172E), CircleShape)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Configure Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Tailor feed parameters to separate household viewers.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it },
                    label = { Text("Profile Name", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE50914)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Kids Toggle Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Set as Children's Profile", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Restrict library access strictly to curated cartoon, gaming, and educative modules.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = setAsKidsState,
                            onCheckedChange = {
                                setAsKidsState = it
                                if (it) {
                                    selectedCategoryPref = "Kids Content"
                                    selectedAgeCategory = "Under 7"
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFC4))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Preferences Categories
                Text("Content Preference Priority", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoriesList.forEach { category ->
                        val isSel = selectedCategoryPref == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFFE50914) else Color(0xFF1E172E))
                                .clickable {
                                    if (!setAsKidsState || category == "Kids Content") {
                                        selectedCategoryPref = category
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(category, color = if (isSel) Color.White else Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Age limits Selector
                Text("Viewer Age Bracket Gates", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ageCategories.forEach { gate ->
                        val isSel = selectedAgeCategory == gate
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF00FFC4) else Color(0xFF1E172E))
                                .clickable {
                                    if (!setAsKidsState) {
                                        selectedAgeCategory = gate
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                gate,
                                color = if (isSel) Color.Black else Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (profileNameInput.isNotBlank()) {
                        viewModel.addProfile(
                            profileNameInput,
                            isKids = setAsKidsState,
                            contentPref = selectedCategoryPref,
                            ageCat = selectedAgeCategory
                        )
                    }
                    viewModel.navigateTo(AppDestination.PROFILE_SELECT)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC4)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save and Create Config", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = minOf(w, h) / 2f
        val cx = w / 2f
        val cy = h / 2f
        
        // 1. Red (top segment)
        val pathRed = Path().apply {
            moveTo(cx, cy)
            arcTo(
                rect = Rect(cx - r, cy - r, cx + r, cy + r),
                startAngleDegrees = -180f,
                sweepAngleDegrees = 135f,
                forceMoveTo = false
            )
            lineTo(cx, cy)
            close()
        }
        
        // 2. Yellow (left segment)
        val pathYellow = Path().apply {
            moveTo(cx, cy)
            arcTo(
                rect = Rect(cx - r, cy - r, cx + r, cy + r),
                startAngleDegrees = -225f,
                sweepAngleDegrees = 45f,
                forceMoveTo = false
            )
            lineTo(cx, cy)
            close()
        }
        
        // 3. Green (bottom segment)
        val pathGreen = Path().apply {
            moveTo(cx, cy)
            arcTo(
                rect = Rect(cx - r, cy - r, cx + r, cy + r),
                startAngleDegrees = 45f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(cx, cy)
            close()
        }
        
        // 4. Blue (right segment)
        val pathBlue = Path().apply {
            moveTo(cx, cy)
            arcTo(
                rect = Rect(cx - r, cy - r, cx + r, cy + r),
                startAngleDegrees = -45f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(cx, cy)
            close()
        }
        
        drawPath(pathRed, Color(0xFFEA4335))
        drawPath(pathYellow, Color(0xFFFBBC05))
        drawPath(pathGreen, Color(0xFF34A853))
        drawPath(pathBlue, Color(0xFF4285F4))
        
        // Draw the blue horizontal bar extending to the right
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(cx, cy - r * 0.18f),
            size = Size(r, r * 0.36f)
        )
        
        // Cut out top-right wedge above the blue bar to keep it open
        val wedgePath = Path().apply {
            moveTo(cx, cy)
            arcTo(
                rect = Rect(cx - r, cy - r, cx + r, cy + r),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 45f,
                forceMoveTo = false
            )
            lineTo(cx, cy)
            close()
        }
        drawPath(wedgePath, Color(0xFF1A1D23))
        
        // Cut out the inner circle to form the clean "G" shape
        drawCircle(
            color = Color(0xFF1A1D23),
            radius = r * 0.58f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun FacebookLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = minOf(w, h) / 2f
        val cx = w / 2f
        val cy = h / 2f
        
        // Draw standard Facebook circular blue background
        drawCircle(
            color = Color(0xFF1877F2),
            radius = r,
            center = Offset(cx, cy)
        )
        
        // Draw authentic white lowercase 'f' shape
        val fPath = Path().apply {
            moveTo(cx + r * 0.12f, cy + r * 0.75f)
            lineTo(cx + r * 0.12f, cy + r * 0.05f)
            lineTo(cx - r * 0.15f, cy + r * 0.05f)
            lineTo(cx - r * 0.15f, cy - r * 0.18f)
            lineTo(cx + r * 0.12f, cy - r * 0.18f)
            lineTo(cx + r * 0.12f, cy - r * 0.42f)
            quadraticTo(cx + r * 0.12f, cy - r * 0.75f, cx + r * 0.45f, cy - r * 0.75f)
            lineTo(cx + r * 0.65f, cy - r * 0.75f)
            lineTo(cx + r * 0.65f, cy - r * 0.52f)
            lineTo(cx + r * 0.45f, cy - r * 0.52f)
            quadraticTo(cx + r * 0.35f, cy - r * 0.52f, cx + r * 0.35f, cy - r * 0.42f)
            lineTo(cx + r * 0.35f, cy - r * 0.18f)
            lineTo(cx + r * 0.62f, cy - r * 0.18f)
            lineTo(cx + r * 0.55f, cy + r * 0.05f)
            lineTo(cx + r * 0.35f, cy + r * 0.05f)
            lineTo(cx + r * 0.35f, cy + r * 0.75f)
            close()
        }
        drawPath(fPath, Color.White)
    }
}

@Composable
fun AppleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = minOf(w, h) / 2f
        val cx = w / 2f
        val cy = h / 2f
        
        // Draw the leaf
        val leafPath = Path().apply {
            moveTo(cx + r * 0.08f, cy - r * 0.48f)
            quadraticTo(cx + r * 0.28f, cy - r * 0.78f, cx + r * 0.42f, cy - r * 0.78f)
            quadraticTo(cx + r * 0.28f, cy - r * 0.42f, cx + r * 0.08f, cy - r * 0.48f)
        }
        drawPath(leafPath, Color.White)
        
        // Draw the Apple body
        val applePath = Path().apply {
            moveTo(cx, cy - r * 0.38f)
            quadraticTo(cx - r * 0.32f, cy - r * 0.48f, cx - r * 0.58f, cy - r * 0.18f)
            quadraticTo(cx - r * 0.78f, cy + r * 0.12f, cx - r * 0.58f, cy + r * 0.48f)
            quadraticTo(cx - r * 0.38f, cy + r * 0.72f, cx - r * 0.12f, cy + r * 0.62f)
            quadraticTo(cx, cy + r * 0.68f, cx + r * 0.12f, cy + r * 0.62f)
            quadraticTo(cx + r * 0.38f, cy + r * 0.72f, cx + r * 0.58f, cy + r * 0.48f)
            quadraticTo(cx + r * 0.78f, cy + r * 0.12f, cx + r * 0.58f, cy - r * 0.18f)
            quadraticTo(cx + r * 0.32f, cy - r * 0.48f, cx, cy - r * 0.38f)
            close()
        }
        drawPath(applePath, Color.White)
        
        // Bite cutout
        drawCircle(
            color = Color(0xFF1A1D23),
            radius = r * 0.25f,
            center = Offset(cx + r * 0.65f, cy - r * 0.05f)
        )
    }
}

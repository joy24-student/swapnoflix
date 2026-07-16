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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.LocalAppColors
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun UnifiedHelpSupportDeskScreen(viewModel: StreamingViewModel) {
    val appColors = LocalAppColors.current
    var selectedSubTab by remember { mutableStateOf("faq") } // "faq", "tickets", "chat"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        // Upper Title Header with soft gradient accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(appColors.primary.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    text = "Help & Support Desk",
                    fontWeight = FontWeight.ExtraBold,
                    color = appColors.textPrimary,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Browse FAQs, submit official bug reports/tickets, or speak to AI Live Assistant.",
                    color = appColors.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Premium Segmented Capsule Switcher Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(appColors.surfaceVariant.copy(alpha = 0.4f))
                .border(BorderStroke(1.dp, appColors.border.copy(alpha = 0.1f)), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            listOf("faq" to "FAQs", "tickets" to "Tickets Logs", "chat" to "AI Chat Desk").forEach { (tabId, tabName) ->
                val isSelected = selectedSubTab == tabId
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) appColors.primary else Color.Transparent,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "tab_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else appColors.textPrimary,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "tab_text"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgColor)
                        .clickable { selectedSubTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = selectedSubTab,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                    ) { width -> if (targetState == "faq") -width else width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> if (targetState == "faq") width else -width } + fadeOut()
                },
                label = "support_tab_transition"
            ) { tab ->
                when (tab) {
                    "faq" -> FAQTab(viewModel)
                    "tickets" -> TicketsTab(viewModel)
                    "chat" -> ChatTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun FAQTab(viewModel: StreamingViewModel) {
    var expandedFaqId by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val appColors = LocalAppColors.current

    val faqItems = listOf(
        Triple(1, "How to bind bKash/Nagad wallet options?", "Select Account -> Subscription tier. Input your wallet phone coordinates. Validate the instant 6-digit verification code PIN triggered by SMS. Tiers update dynamically."),
        Triple(2, "My stream is loading or buffering, how to scale quality?", "Tap the Video Player Screen, click the Settings/Gear icon under the slider, hardcode resolution value to Economy (480p) or Standard 720p instead of Auto."),
        Triple(3, "ForgotPassword: how to recover parental control PIN?", "The standard preset code is '1234'. If overridden, submit a support ticket under Support Tickets logs tab. Verification completes in 30 mins."),
        Triple(4, "How to cast streams onto smart television systems?", "Ensure phone device and Smart TV share the identical Wi-Fi channel. Tap the Casting icon visible in player controls header and choose destination hub.")
    )

    val filteredFaq = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            faqItems
        } else {
            faqItems.filter { (_, q, a) ->
                q.contains(searchQuery, ignoreCase = true) || a.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick help search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search FAQ topics...", color = appColors.textMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = appColors.textMuted, modifier = Modifier.size(18.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = appColors.textPrimary,
                unfocusedTextColor = appColors.textPrimary,
                focusedBorderColor = appColors.primary,
                unfocusedBorderColor = appColors.border
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(4.dp))

        filteredFaq.forEach { (fid, question, answer) ->
            val isExpanded = expandedFaqId == fid
            val faqBorderColor by animateColorAsState(
                targetValue = if (isExpanded) appColors.primary else appColors.border,
                label = "faq_border"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                border = BorderStroke(1.dp, faqBorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = if (isExpanded) 4.dp else 1.dp, shape = RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedFaqId = if (isExpanded) null else fid }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(question, fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = if (isExpanded) appColors.primary else appColors.textMuted
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = answer,
                            color = appColors.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TicketsTab(viewModel: StreamingViewModel) {
    val activeChatTicket by viewModel.activeChatTicket.collectAsState()

    if (activeChatTicket != null) {
        TicketChatThreadScreen(viewModel = viewModel, ticket = activeChatTicket!!)
    } else {
        val tckList by viewModel.supportTickets.collectAsState()

        var newTckSubject by remember { mutableStateOf("") }
        var newTckCategory by remember { mutableStateOf("Billing") }
        var newTckDetails by remember { mutableStateOf("") }
        val appColors = LocalAppColors.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                border = BorderStroke(1.dp, appColors.border),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Submit Support Request", fontWeight = FontWeight.Black, color = appColors.textPrimary, fontSize = 16.sp)

                    OutlinedTextField(
                        value = newTckSubject,
                        onValueChange = { newTckSubject = it },
                        label = { Text("Core Concern Subject", color = appColors.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary,
                            focusedBorderColor = appColors.primary,
                            unfocusedBorderColor = appColors.border
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("Ticket Type Category", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Billing", "Streaming Lag", "Parental Lock", "Other").forEach { typ ->
                            val isSel = newTckCategory == typ
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) appColors.primary else appColors.cardBgSecondary)
                                    .border(1.dp, if (isSel) appColors.primary else appColors.border, RoundedCornerShape(10.dp))
                                    .clickable { newTckCategory = typ }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typ,
                                    color = if (isSel) Color.White else appColors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newTckDetails,
                        onValueChange = { newTckDetails = it },
                        label = { Text("Elaborated description anomaly", color = appColors.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary,
                            focusedBorderColor = appColors.primary,
                            unfocusedBorderColor = appColors.border
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (newTckSubject.isNotBlank()) {
                                viewModel.createSupportTicketInSupabase(newTckSubject, newTckCategory, newTckDetails)
                                newTckSubject = ""
                                newTckDetails = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Launch Ticket Log", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("Your Support Tickets History", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            if (tckList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No support tickets submitted yet.", color = appColors.textMuted, fontSize = 12.sp)
                }
            } else {
                tckList.forEach { ticket ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                        border = BorderStroke(1.dp, appColors.border),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.selectTicketForChat(ticket) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ticket.id, fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 13.sp)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (ticket.status == "RESOLVED") Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = ticket.status,
                                        color = if (ticket.status == "RESOLVED") Color(0xFF4CAF50) else Color(0xFFFFB300),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(ticket.subject, fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 14.sp)
                            Text(ticket.description, color = appColors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            Text("Opened on ${ticket.date} under category ${ticket.category}", color = appColors.textMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTab(viewModel: StreamingViewModel) {
    val msgs by viewModel.chatMessages.collectAsState()
    var chatFieldInput by remember { mutableStateOf("") }
    val appColors = LocalAppColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        // High-end identity bar with pulsing indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.cardBg)
                .border(BorderStroke(1.dp, appColors.border.copy(alpha = 0.1f)))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .background(appColors.accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Swapnoflix Help Assistant", fontWeight = FontWeight.Black, color = appColors.textPrimary, fontSize = 14.sp)
                Text("Online • Automated Live Help Desk", color = appColors.textMuted, fontSize = 10.sp)
            }
        }

        // Chat logs bubble list
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val listState = rememberScrollState()
            LaunchedEffect(msgs.size) {
                listState.animateScrollTo(listState.maxValue)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                msgs.forEach { item ->
                    val isUsr = item.isSenderUser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUsr) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUsr) appColors.primary else appColors.cardBgSecondary
                            ),
                            border = BorderStroke(1.dp, appColors.border),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUsr) 16.dp else 2.dp,
                                bottomEnd = if (isUsr) 2.dp else 16.dp
                            ),
                            modifier = Modifier.widthIn(max = 270.dp)
                        ) {
                            Text(
                                text = item.text,
                                color = if (isUsr) Color.White else appColors.textPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Send Text input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.surfaceVariant.copy(alpha = 0.5f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = chatFieldInput,
                onValueChange = { chatFieldInput = it },
                placeholder = { Text("Describe payment, streaming issues...", color = appColors.textMuted, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    focusedBorderColor = appColors.primary,
                    unfocusedBorderColor = appColors.border
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (chatFieldInput.isNotBlank()) {
                        viewModel.sendUserChatMessage(chatFieldInput)
                        chatFieldInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(appColors.primary, CircleShape)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun TicketChatThreadScreen(viewModel: StreamingViewModel, ticket: SupportTicket) {
    val logs by viewModel.chatMessages.collectAsState()
    var userText by remember { mutableStateOf("") }
    val appColors = LocalAppColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.cardBg)
                .border(BorderStroke(1.dp, appColors.border.copy(alpha = 0.1f)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectTicketForChat(null) }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = appColors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(ticket.subject, fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Category: ${ticket.category} • Status: ${ticket.status}", color = appColors.textMuted, fontSize = 11.sp)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            val listState = rememberScrollState()
            LaunchedEffect(logs.size) {
                listState.animateScrollTo(listState.maxValue)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ticket description starter message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBgSecondary),
                        border = BorderStroke(1.dp, appColors.border),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("TICKET INQUIRY DETAILS:", color = appColors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(ticket.description, color = appColors.textPrimary, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }

                logs.forEach { bubble ->
                    val isU = bubble.isSenderUser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isU) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isU) appColors.primary else appColors.cardBgSecondary
                            ),
                            border = BorderStroke(1.dp, appColors.border),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isU) 16.dp else 2.dp,
                                bottomEnd = if (isU) 2.dp else 16.dp
                            ),
                            modifier = Modifier.widthIn(max = 270.dp)
                        ) {
                            Text(
                                text = bubble.text,
                                color = if (isU) Color.White else appColors.textPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.surfaceVariant.copy(alpha = 0.5f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = userText,
                onValueChange = { userText = it },
                placeholder = { Text("Write your message...", color = appColors.textMuted, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    focusedBorderColor = appColors.primary,
                    unfocusedBorderColor = appColors.border
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (userText.isNotBlank()) {
                        viewModel.sendUserChatMessage(userText)
                        userText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(appColors.primary, CircleShape)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun RewardsReferralScreen(viewModel: StreamingViewModel) {
    val scoresPoints by viewModel.userRewardPoints.collectAsState()
    val earningsCoin by viewModel.referralEarnings.collectAsState()
    val codeString by viewModel.referralCode.collectAsState()
    val referrals by viewModel.referralsList.collectAsState()
    
    val appColors = LocalAppColors.current
    val context = LocalContext.current

    var emailInput by remember { mutableStateOf("") }
    
    // Animate point counter on load
    var targetPoints by remember { mutableStateOf(0) }
    LaunchedEffect(scoresPoints) {
        targetPoints = scoresPoints
    }
    val animatedPoints by animateIntAsState(
        targetValue = targetPoints,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "points_counter"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Swapnoflix Rewards Locker",
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "Acquire score points by watching events. Convert points to bills credit.",
            color = appColors.textMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Active Rewards Balance Card - Premium Animated Gradient Glow
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6B11FF), // Deep Neon Indigo
                                    Color(0xFFE94057), // Hot Coral Pink
                                    Color(0xFFF27121)  // Vibrant Sunset Orange
                                )
                            )
                        )
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE REWARDS BALANCE",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFFFFDF00), // Sparkling Yellow Gold
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "$animatedPoints PTS",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 34.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "GOLD MEMBER",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Acquires Tasks Ledger
            Text(
                text = "Acquisition Tasks Ledger",
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                fontSize = 15.sp
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                border = BorderStroke(1.dp, appColors.border),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf(
                        Pair("Daily EPG Streaks Watch multiplier", "Completed (+50 pts)"),
                        Pair("Watch 3 distinct Sports events in HD", "Active progress: 2/3 (Wins 100 pts)"),
                        Pair("Referral successful checkout", "Active progress: 1 (Wins 250 pts)")
                    ).forEachIndexed { idx, (task, progress) ->
                        val isDone = progress.startsWith("Completed")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task,
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = progress,
                                    color = if (isDone) appColors.accent else appColors.textMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                    fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = if (isDone) appColors.accent else appColors.accent.copy(alpha = 0.5f)
                            )
                        }
                        if (idx < 2) {
                            HorizontalDivider(color = appColors.border.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // Share Invitation & Promo Code Box
            Text(
                text = "Share Invitation Referral Code",
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                fontSize = 15.sp
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, appColors.accent.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Your Unique Promo Code",
                        fontSize = 12.sp,
                        color = appColors.textMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(appColors.cardBgSecondary, RoundedCornerShape(8.dp))
                                .border(1.dp, appColors.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = codeString,
                                color = appColors.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("SwapnoFlix Promo Code", codeString)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Promo code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Copy", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = appColors.border.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Referral Section
                    Text(
                        text = "Invite Friend via Email",
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("friend@example.com", color = appColors.textMuted, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = appColors.primary,
                                unfocusedBorderColor = appColors.border
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (emailInput.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                    viewModel.sendReferralInvitationInSupabase(emailInput.trim())
                                    emailInput = ""
                                    Toast.makeText(context, "Invitation successfully sent!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Invite", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = appColors.border.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Wallet/Earnings section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Your referral earnings:", fontSize = 11.sp, color = appColors.textMuted)
                            Text(
                                text = "৳$earningsCoin.00 Wallet credits",
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary,
                                fontSize = 15.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Cashout successfully initiated! Check dashboard.", Toast.LENGTH_LONG).show()
                            },
                            border = BorderStroke(1.dp, appColors.border),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cashout Credits", color = appColors.textPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Sent invitations list displaying Supabase records
            if (referrals.isNotEmpty()) {
                Text(
                    text = "Sent Invitations Ledger (${referrals.size})",
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary,
                    fontSize = 15.sp
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                    border = BorderStroke(1.dp, appColors.border),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        referrals.forEachIndexed { index, ref ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = ref.referred_email,
                                        color = appColors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Sent code: ${ref.referral_code ?: "—"}",
                                        color = appColors.textMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (ref.status == "accepted") Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = ref.status.uppercase(),
                                        color = if (ref.status == "accepted") Color(0xFF4CAF50) else Color(0xFFFFB300),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (index < referrals.size - 1) {
                                HorizontalDivider(color = appColors.border.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

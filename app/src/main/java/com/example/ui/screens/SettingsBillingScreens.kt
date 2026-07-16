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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.LocalAppColors
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun UnifiedBillingCenterScreen(viewModel: StreamingViewModel) {
    val appColors = LocalAppColors.current
    var selectedSubTab by remember { mutableStateOf("dashboard") } // "dashboard", "plans", "invoices"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        // Upper Title Header with Enterprise Gradient background accent
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
                    text = "Billing & Subscriptions",
                    fontWeight = FontWeight.ExtraBold,
                    color = appColors.textPrimary,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Manage your membership plans, billing cycle, and dynamic invoices.",
                    color = appColors.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Premium Segmented Capsule Switcher Row (No boring TabRow!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(appColors.surfaceVariant.copy(alpha = 0.4f))
                .border(BorderStroke(1.dp, appColors.border.copy(alpha = 0.1f)), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            listOf("dashboard" to "Dashboard", "plans" to "Change Plan", "invoices" to "Invoices").forEach { (tabId, tabName) ->
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
                    ) { width -> if (targetState == "dashboard") -width else width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> if (targetState == "dashboard") width else -width } + fadeOut()
                },
                label = "billing_tab_transition"
            ) { tab ->
                when (tab) {
                    "dashboard" -> BillingDashboardTab(viewModel)
                    "plans" -> BillingPlansTab(viewModel)
                    "invoices" -> BillingInvoicesTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun BillingDashboardTab(viewModel: StreamingViewModel) {
    val context = LocalContext.current
    val activePlan by viewModel.userActivePlan.collectAsState()
    val isAutoRenew by viewModel.isAutoRenewActive.collectAsState()
    val status by viewModel.subscriptionStatus.collectAsState()
    val renewalDate by viewModel.renewalDate.collectAsState()
    val appColors = LocalAppColors.current

    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = appColors.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Cancel Subscription?", color = appColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to cancel your ${activePlan?.name ?: "Premium Plan"}?",
                        color = appColors.textSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "If you cancel, you will lose:",
                        color = appColors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    listOf(
                        "100% Ad-Free premium live channels stream",
                        "4K Ultra HD resolution & Dolby Digital Atmos playback",
                        "Offline downloading capabilities for your profile sessions"
                    ).forEach { benefit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(benefit, color = appColors.textMuted, fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = "Your plan features remain active until your coverage expires on $renewalDate.",
                        color = appColors.accent,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelActiveSubscription()
                        Toast.makeText(context, "Subscription Cancelled Successfully", Toast.LENGTH_SHORT).show()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Confirm Cancellation", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Go Back", color = appColors.textPrimary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Plan Detail Overview Card - Professional Gradient Glow
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                appColors.primary.copy(alpha = 0.85f),
                                Color(0xFF7B1FA2) // Beautiful deep purple accent
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CURRENT SERVICE SUBSCRIPTION", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(
                                text = activePlan?.name ?: "Basic Free Package",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = status.uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("RENEWAL DATE", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(renewalDate, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("BILLING FREQUENCY", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(activePlan?.period ?: "Monthly", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Auto-Renew Switch Card - Glassmorphism UI
        Card(
            colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
            border = BorderStroke(1.dp, appColors.border),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Renewal Billing", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 14.sp)
                    Text("Auto-charge payment method on renewal date.", fontSize = 11.sp, color = appColors.textMuted)
                }
                Switch(
                    checked = isAutoRenew,
                    onCheckedChange = { viewModel.toggleAutoRenew() },
                    colors = SwitchDefaults.colors(checkedThumbColor = appColors.accent, checkedTrackColor = appColors.accent.copy(alpha = 0.4f))
                )
            }
        }

        // Active Card details card
        Card(
            colors = CardDefaults.cardColors(containerColor = appColors.surfaceVariant),
            border = BorderStroke(1.dp, appColors.border),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CreditCard,
                        contentDescription = "Card icon",
                        tint = appColors.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Stripe Secure Gateway", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Visa ending in **** 4242", color = appColors.textMuted, fontSize = 11.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .background(appColors.border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("PRIMARY", color = appColors.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cancel Button Action
        if (status == "ACTIVE") {
            Button(
                onClick = { showCancelDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Cancel Membership Subscription", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun BillingPlansTab(viewModel: StreamingViewModel) {
    val activePlan by viewModel.userActivePlan.collectAsState()
    val appColors = LocalAppColors.current

    var selectedPlanForPayment by remember { mutableStateOf<SubscriptionPlan?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Choose Subscription Package",
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            StreamingDatabase.subscriptionPlans.forEach { plan ->
                val isActive = activePlan?.id == plan.id
                // Animated elevation and scale for selection
                val borderWidth by animateDpAsState(targetValue = if (isActive) 2.dp else 1.dp, label = "border_width")

                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                    border = BorderStroke(
                        width = borderWidth,
                        color = if (isActive) appColors.accent else appColors.border
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = if (isActive) 6.dp else 2.dp, shape = RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(plan.name, fontWeight = FontWeight.Black, fontSize = 18.sp, color = appColors.textPrimary)
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .background(appColors.accent, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("ACTIVE PLAN", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(plan.price, fontSize = 34.sp, fontWeight = FontWeight.Black, color = appColors.textPrimary)
                            Text("/" + plan.period, color = appColors.textMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        plan.features.forEach { ft ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = appColors.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(ft, color = appColors.textSecondary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        val buttonGrad = Brush.horizontalGradient(
                            colors = if (isActive) listOf(appColors.surfaceVariant, appColors.surfaceVariant)
                            else listOf(appColors.primary, Color(0xFFE040FB))
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(buttonGrad)
                                .clickable(enabled = !isActive) {
                                    selectedPlanForPayment = plan
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isActive) "Active Configuration" else "Secure Upgrade Plan",
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) appColors.textPrimary else Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Secure payment overlay - High-fidelity animated modal
        selectedPlanForPayment?.let { plan ->
            var selectedMethod by remember { mutableStateOf("bKash Wallet") }
            var verifCodeSMS by remember { mutableStateOf("") }
            val context = LocalContext.current

            AlertDialog(
                onDismissRequest = { selectedPlanForPayment = null },
                containerColor = appColors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(BorderStroke(1.dp, appColors.border), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Secure Payment Gateway", color = appColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { selectedPlanForPayment = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = appColors.textPrimary)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Billing details card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = appColors.cardBgSecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Upgrade Package Target:", color = appColors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(plan.name, fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 15.sp)
                                }
                                Text(plan.price, fontWeight = FontWeight.ExtraBold, color = appColors.accent, fontSize = 18.sp)
                            }
                        }

                        // Payment Methods selectors
                        Text("Choose Payment Provider", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 13.sp)

                        listOf(
                            "bKash Wallet" to Color(0xFFE2125B),
                            "Nagad Checkout" to Color(0xFFF15923),
                            "Rocket Pay" to Color(0xFF862583),
                            "Stripe Visa" to Color(0xFF6772E5),
                            "PayPal Express" to Color(0xFF003087)
                        ).forEach { (mName, mColor) ->
                            val isS = selectedMethod == mName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isS) mColor.copy(alpha = 0.08f) else Color.Transparent)
                                    .border(
                                        BorderStroke(
                                            width = if (isS) 2.dp else 1.dp,
                                            color = if (isS) mColor else appColors.border
                                        ), RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedMethod = mName }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(mColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(mName, color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                if (isS) Icon(Icons.Filled.RadioButtonChecked, contentDescription = null, tint = mColor, modifier = Modifier.size(18.dp))
                                else Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null, tint = appColors.textMuted, modifier = Modifier.size(18.dp))
                            }
                        }

                        // OTP sms verification
                        Text("SMS OTP Pin Verification", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 13.sp)
                        OutlinedTextField(
                            value = verifCodeSMS,
                            onValueChange = { verifCodeSMS = it },
                            placeholder = { Text("Phone number or security wallet PIN", color = appColors.textMuted, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = appColors.accent,
                                unfocusedBorderColor = appColors.border
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.completePayment(selectedMethod)
                            Toast.makeText(context, "Payment Authorized via $selectedMethod!", Toast.LENGTH_LONG).show()
                            selectedPlanForPayment = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Complete Payment", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            )
        }
    }
}

@Composable
fun BillingInvoicesTab(viewModel: StreamingViewModel) {
    val records by viewModel.invoices.collectAsState()
    val appColors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Billing Invoices History", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 15.sp)

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No payment receipts or invoices generated yet.", color = appColors.textMuted, fontSize = 12.sp)
            }
        } else {
            records.forEach { record ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                    border = BorderStroke(1.dp, appColors.border),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(record.id, fontWeight = FontWeight.Black, color = appColors.textPrimary, fontSize = 14.sp)
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(record.status, color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("PLAN SPECIFICATION", color = appColors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(record.planName, color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("TOTAL PAID", color = appColors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(record.amount, color = appColors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paid on ${record.date} via ${record.paymentMethod}",
                                fontSize = 11.sp,
                                color = appColors.textMuted
                            )

                            OutlinedButton(
                                onClick = {},
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                                border = BorderStroke(1.dp, appColors.border),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = appColors.textPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("INVOICE PDF", color = appColors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Notifications (Screen 27)
@Composable
fun NotificationsHubScreen(viewModel: StreamingViewModel) {
    val items by viewModel.notifications.collectAsState()
    val appColors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Notification Alerts Centre", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
        IconButton(onClick = { viewModel.clearAllNotifications() }) {
            Icon(Icons.Filled.ClearAll, contentDescription = "Clear", tint = appColors.textPrimary)
        }
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No active notifications or alarms recorded.", color = appColors.textMuted)
        }
    } else {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (alert.type) {
                                        "BILLING" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        "REMINDER" -> Color(0xFF00B0FF).copy(alpha = 0.15f)
                                        else -> Color(0xFFE50914).copy(alpha = 0.15f)
                                    }, CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (alert.type) {
                                    "BILLING" -> Icons.Filled.CreditCard
                                    "REMINDER" -> Icons.Filled.Alarm
                                    else -> Icons.Filled.NewReleases
                                },
                                contentDescription = null,
                                tint = when (alert.type) {
                                    "BILLING" -> Color(0xFF4CAF50)
                                    "REMINDER" -> Color(0xFF00B0FF)
                                    else -> Color(0xFFE50914)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.title, fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 14.sp)
                            Text(alert.description, color = appColors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            Text(alert.timestamp, color = appColors.textMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

// Parental lock secure settings (Screen 31)
@Composable
fun ParentalControlScreen(viewModel: StreamingViewModel) {
    val lockActive by viewModel.isParentalLockActive.collectAsState()
    val checkPin by viewModel.parentalControlPin.collectAsState()
    val activeAge by viewModel.allowedAgeBracket.collectAsState()

    var editPinInput by remember { mutableStateOf(checkPin) }
    var ageOptionSelected by remember { mutableStateOf(activeAge) }
    var lockToggleChecked by remember { mutableStateOf(lockActive) }
    val appColors = LocalAppColors.current

    Spacer(modifier = Modifier.height(16.dp))
    Text("Parental Gate Locks Config", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 16.dp))
    Text("Lock restricted networks and movies behind standard security PIN.", color = appColors.textMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))

    Spacer(modifier = Modifier.height(24.dp))

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PIN Protection Status", fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("Demand code before shifting profiles.", fontSize = 11.sp, color = appColors.textMuted)
                    }
                    Switch(
                        checked = lockToggleChecked,
                        onCheckedChange = { lockToggleChecked = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = appColors.accent)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editPinInput,
                    onValueChange = { editPinInput = it },
                    label = { Text("Set 4-Digit Locking code", color = appColors.textMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary,
                        focusedBorderColor = appColors.accent,
                        unfocusedBorderColor = appColors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Allowed Content Rating Ceiling", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))

        listOf("Children Only (Under 7)", "Kids Safe (7-12)", "Teens (12+)", "General (18+)").forEach { tag ->
            val isS = ageOptionSelected == tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { ageOptionSelected = tag }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tag, color = if (isS) appColors.accent else appColors.textPrimary, fontSize = 14.sp, fontWeight = if (isS) FontWeight.Bold else FontWeight.Normal)
                if (isS) Icon(Icons.Filled.RadioButtonChecked, contentDescription = null, tint = appColors.accent)
                else Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null, tint = appColors.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.updateParentalSetup(editPinInput, lockToggleChecked, ageOptionSelected)
            },
            colors = ButtonDefaults.buttonColors(containerColor = appColors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Apply Restrictions Locks", fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

// Kids mode strictly filtered UI (Screen 32)
@Composable
fun KidsModeScreen(viewModel: StreamingViewModel) {
    val allMovies = StreamingDatabase.movies
    val appColors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("🌸 Panda Kids Lounge", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF4081))
            Text("Child-friendly layout locks out other adult rows.", fontSize = 12.sp, color = appColors.textMuted)
        }

        Button(
            onClick = { viewModel.navigateTo(AppDestination.PROFILE_SELECT) },
            colors = ButtonDefaults.buttonColors(containerColor = appColors.accent)
        ) {
            Text("Exit Kids Mode", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Carousels representing cartoons
    Text("Fun Video Collections", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        allMovies.filter { it.category.contains("Kids") }.forEach { cartoon ->
            Card(
                modifier = Modifier
                    .width(140.dp)
                    .clickable {
                        viewModel.selectMovie(cartoon)
                        viewModel.selectTab(MainTab.LIVE_TV) // stream
                    },
                colors = CardDefaults.cardColors(containerColor = appColors.cardBgSecondary)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(cartoon.colorAccent.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Face, contentDescription = null, tint = Color(0xFFFF4081), modifier = Modifier.size(44.dp))
                    }
                    Text(cartoon.title, color = appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp), maxLines = 1)
                }
            }
        }
    }
}

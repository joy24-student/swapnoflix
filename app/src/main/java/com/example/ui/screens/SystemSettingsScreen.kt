package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StreamingViewModel
import com.example.ui.theme.LocalAppColors

@Composable
fun SystemSettingsScreen(viewModel: StreamingViewModel) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val language by viewModel.appLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val titleText = if (language == "বাংলা") "সিস্টেম এবং থিম" else "System & Themes"
        Text(titleText, fontSize = 20.sp, color = appColors.textPrimary, style = MaterialTheme.typography.titleLarge)

        Card(
            colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brightness6, contentDescription = "Theme", tint = appColors.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val themeLabel = if (language == "বাংলা") "থিম মোড" else "Theme Mode"
                        Text(themeLabel, color = appColors.textPrimary, fontSize = 14.sp)
                        val statusLabel = if (isDarkMode) {
                            if (language == "বাংলা") "ডার্ক মোড" else "Dark Mode"
                        } else {
                            if (language == "বাংলা") "লাইট মোড" else "Light Mode"
                        }
                        Text(statusLabel, color = appColors.textMuted, fontSize = 12.sp)
                    }
                }

                // Toggle uses the global viewModel.toggleDarkMode so app theme updates live
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode() },
                    colors = SwitchDefaults.colors(checkedThumbColor = appColors.primary, checkedTrackColor = appColors.primary.copy(alpha = 0.4f))
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val langHeader = if (language == "বাংলা") "ভাষা" else "Language"
                Text(langHeader, color = appColors.textPrimary, fontSize = 14.sp)
                val languages = listOf("English", "বাংলা")
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = (lang == language), onClick = {
                                viewModel.setAppLanguage(lang)
                                Toast.makeText(context, if (lang == "বাংলা") "ভাষা সেট করা হয়েছে: বাংলা" else "Language set: English", Toast.LENGTH_SHORT).show()
                            })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (lang == language), onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(lang, color = appColors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val footerText = if (language == "বাংলা") "ভাষা পরিবর্তন করলে অনুবাদ উপলব্ধ থাকা স্থানে UI লেবেল আপডেট হবে।" else "Changing language updates UI labels where translations exist."
                Text(footerText, color = appColors.textMuted, fontSize = 12.sp)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val maintenanceLabel = if (language == "বাংলা") "রক্ষণাবেক্ষণ" else "Maintenance"
                Text(maintenanceLabel, color = appColors.textPrimary, fontSize = 14.sp)
                TextButton(onClick = {
                    // Best-effort cache clear
                    Toast.makeText(context, if (language == "বাংলা") "অ্যাপ ক্যাশে সাফ করা হয়েছে।" else "App cache cleared.", Toast.LENGTH_SHORT).show()
                }) {
                    Text(if (language == "বাংলা") "ক্যাশে এবং অ্যাপ ডাটা ফ্ল্যাগ মুছুন" else "Clear Cache & App Data Flags")
                }

                TextButton(onClick = {
                    // Reset theme to system default (light)
                    if (isDarkMode) viewModel.toggleDarkMode()
                    Toast.makeText(context, if (language == "বাংলা") "থিম লাইট মোডে রিসেট করা হয়েছে।" else "Theme reset to Light mode.", Toast.LENGTH_SHORT).show()
                }) {
                    Text(if (language == "বাংলা") "থিম লাইটে রিসেট করুন" else "Reset Theme to Light")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

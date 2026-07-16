package com.example.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.admin.screens.AdminPanelScreen
import com.example.data.FirebaseManager
import com.example.data.StreamingViewModel
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.MyApplicationTheme

class AdminMainActivity : ComponentActivity() {
    private lateinit var viewModel: StreamingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseManager.initialize(applicationContext)
        viewModel = ViewModelProvider(this)[StreamingViewModel::class.java]

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkMode) {
                val appColors = LocalAppColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = appColors.background
                ) {
                    AdminPanelScreen(viewModel = viewModel)
                }
            }
        }
    }
}

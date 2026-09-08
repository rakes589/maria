package com.example.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jarvis.builder.BuilderScreen
import com.example.jarvis.builder.BuilderViewModel
import com.example.jarvis.builder.ProjectFileManager
import com.example.jarvis.automation.GeminiAgentManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); val files = ProjectFileManager(this); val key = getSharedPreferences("agent", MODE_PRIVATE).getString("gemini_key", "") ?: ""; setContent { BuilderScreen(viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory { override fun <T : androidx.lifecycle.ViewModel> create(c: Class<T>): T = BuilderViewModel(files, GeminiAgentManager(key)) as T })) } }
}

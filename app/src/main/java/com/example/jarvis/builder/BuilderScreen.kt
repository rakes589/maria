package com.example.jarvis.builder

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@SuppressLint("SetJavaScriptEnabled")
@Composable fun BuilderScreen(vm: BuilderViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle(); val files = listOf("index.html", "style.css", "script.js"); var code by remember(state.selectedFile, state.files) { mutableStateOf(state.files[state.selectedFile].orEmpty()) }
    Scaffold(topBar = { TopAppBar(title = { Text("Jarvis Builder") }, actions = { Button(enabled = !state.isLoading, onClick = vm::executeAutoFix) { Text(if (state.isLoading) "Fixing…" else "Auto-Fix") }; Spacer(Modifier.width(8.dp)); Button(onClick = vm::reload) { Text("Refresh") } }) }) { pad -> Column(Modifier.fillMaxSize().padding(pad)) {
        TabRow(selectedTabIndex = files.indexOf(state.selectedFile).coerceAtLeast(0)) { files.forEachIndexed { i, file -> Tab(selected = file == state.selectedFile, onClick = { vm.select(file) }, text = { Text(file) }) } }
        OutlinedTextField(value = code, onValueChange = { code = it; vm.save(state.selectedFile, it) }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth().height(240.dp).padding(12.dp).verticalScroll(rememberScrollState()))
        Text("Live Preview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(12.dp)); Preview(state.previewVersion, Modifier.weight(1f)); state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
    } }
}

@Composable private fun Preview(version: Int, modifier: Modifier = Modifier) { var web: WebView? by remember { mutableStateOf(null) }; AndroidView(modifier = modifier.fillMaxWidth(), factory = { context -> WebView(context).apply { settings.javaScriptEnabled = true; settings.domStorageEnabled = true; webViewClient = WebViewClient(); layoutParams = ViewGroup.LayoutParams(-1, -1); web = this; loadUrl("http://127.0.0.1:8080/") } }, update = { web = it }); LaunchedEffect(version) { if (version > 0) web?.reload() }; DisposableEffect(Unit) { onDispose { web?.apply { stopLoading(); loadUrl("about:blank"); destroy() }; web = null } } }

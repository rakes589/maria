package com.example.jarvis

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.jarvis.agent.AgentState
import com.example.jarvis.agent.AgentStateManager
import com.example.jarvis.agent.AgentTaskPlanner
import com.example.jarvis.automation.RootShellHelper
import com.example.jarvis.automation.VoiceAgentService
import com.example.jarvis.builder.ProjectFileManager
import com.example.jarvis.builder.CodeDiagnostics
import com.example.jarvis.settings.requestBatteryOptimizationExemption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF090D14)
private val Panel = Color(0xFF111925)
private val Raised = Color(0xFF172333)
private val Teal = Color(0xFF36E0D0)
private val Purple = Color(0xFF9C7CFF)
private val Muted = Color(0xFF8090A5)
private val nav = listOf("Home", "Files", "Editor", "Terminal", "Preview", "Dev", "Settings")

@Composable
fun MariaApp() {
    var tab by remember { mutableIntStateOf(0) }
    val agentState by AgentStateManager.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val manager = remember(context) { ProjectFileManager(context) }
    Scaffold(containerColor = Ink, bottomBar = {
        NavigationBar(containerColor = Panel) {
            nav.forEachIndexed { i, label ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(navIcon(i), label) }, label = { Text(label, fontSize = 9.sp) })
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            MariaHeader(agentState)
            when (tab) {
                0 -> HomeScreen(agentState) { tab = it }
                1 -> FilesScreen(manager) { tab = 2 }
                2 -> EditorScreen(manager)
                3 -> TerminalScreen()
                4 -> PreviewScreen()
                5 -> DevScreen(manager)
                6 -> SettingsScreen()
            }
        }
    }
}

private fun navIcon(i: Int) = when (i) { 0 -> Icons.Default.Home; 1 -> Icons.Default.Folder; 2 -> Icons.Default.Code; 3 -> Icons.Default.Terminal; 4 -> Icons.Default.Preview; 5 -> Icons.Default.Build; else -> Icons.Default.Tune }

@Composable private fun MariaHeader(state: AgentState) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("MARIA", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp); Text("AUTONOMOUS MOBILE WORKSPACE", color = Teal, fontSize = 9.sp, letterSpacing = 2.sp) }
            val color = if (state == AgentState.ERROR) Color.Red else Teal
            Text(state.name, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.border(1.dp, color, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 7.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) { Pill("READY", Teal); Pill("ROOT", Purple); Pill("+8080", Color(0xFFFFB86B)) }
    }
}
@Composable private fun Pill(text: String, color: Color) { Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(color.copy(alpha = .12f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) }

@Composable private fun HomeScreen(state: AgentState, navigate: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var command by remember { mutableStateOf("") }
    var commandStatus by remember { mutableStateOf("") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item { CardBox { Text("COMMAND CENTER", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp); Text("Build, automate, or ask Maria", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)); OutlinedTextField(value = command, onValueChange = { command = it }, singleLine = true, placeholder = { Text("Ask Maria anything…", color = Muted) }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)); Text(commandStatus, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)); Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) { Button(onClick = { val request = command.trim(); command = ""; val plan = AgentTaskPlanner.plan(request); if (plan == null) { commandStatus = "Sent to Maria AI"; AgentStateManager.set(AgentState.THINKING) } else { scope.launch { AgentStateManager.set(AgentState.EXECUTING_ROOT); commandStatus = AgentTaskPlanner.execute(plan).fold({ "Completed: ${it.take(80)}" }, { "Failed: ${it.message}" }); AgentStateManager.set(AgentState.IDLE) } } }, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink)) { Text("SEND") }; Spacer(Modifier.width(8.dp)); OutlinedButton(onClick = { runCatching { ContextCompat.startForegroundService(context, Intent(context, VoiceAgentService::class.java)) } }) { Text("MIC") } } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { QuickCard("BUILD", "Generate project", Teal) { navigate(2) }; QuickCard("SCREEN", "Inspect device", Purple) { navigate(5) } } }
        item { CardBox { Text("ACTIVE WORKSPACE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp); Text("landing-page", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp)); Text("index.html • style.css • script.js", color = Muted, fontSize = 11.sp); Button(onClick = { navigate(4) }, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink), modifier = Modifier.padding(top = 10.dp)) { Text("OPEN PREVIEW") } } }
        item { Text("RECENT ACTIVITY", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp) }
        item { Activity("System initialized", "Agent state manager ready", Teal) }
        item { Activity("Local preview", "NanoHTTPD workspace available", Purple) }
    }
}

@Composable private fun FilesScreen(manager: ProjectFileManager, openEditor: () -> Unit) {
    val scope = rememberCoroutineScope(); var files by remember { mutableStateOf<List<String>>(emptyList()) }; var message by remember { mutableStateOf("Loading workspace…") }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { manager.ensureWorkspace(); manager.readAllFiles().getOrNull()?.keys?.toList() ?: emptyList() }.also { files = it; message = "${it.size} project files" } }
    Column(Modifier.fillMaxSize()) { SectionTitle("PROJECT FILES", "Private workspace: filesDir/projects/landing-page"); Button(onClick = openEditor, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink)) { Text("OPEN EDITOR") }; Text(message, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(files) { file -> Activity(file, "UTF-8 source file", Teal) } } }
}

@Composable private fun EditorScreen(manager: ProjectFileManager) {
    val scope = rememberCoroutineScope(); var file by remember { mutableStateOf("index.html") }; var code by remember { mutableStateOf("") }; var saved by remember { mutableStateOf(false) }; val files = listOf("index.html", "style.css", "script.js")
    LaunchedEffect(file) { code = withContext(Dispatchers.IO) { manager.readFile(file).getOrDefault("") } }
    val diagnostics = remember(file, code) { CodeDiagnostics.analyze(file, code) }
    Column(Modifier.fillMaxSize()) { SectionTitle("CODE EDITOR", "Monospaced workspace with live persistence"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { files.forEach { TextButton(onClick = { file = it }) { Text(it, color = if (it == file) Teal else Muted, fontSize = 11.sp) } } }; OutlinedTextField(value = code, onValueChange = { code = it; saved = false }, textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp), modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), label = { Text(file) }); if (diagnostics.isNotEmpty()) Text("${diagnostics.size} issue(s): ${diagnostics.first().message}", color = Color(0xFFFFB86B), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text(if (saved) "Saved" else "Unsaved", color = if (saved) Teal else Color(0xFFFFB86B), modifier = Modifier.padding(12.dp)); Button(onClick = { scope.launch { manager.writeFile(file, code); saved = true } }, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink)) { Text("SAVE") }; Spacer(Modifier.width(8.dp)); OutlinedButton(onClick = { code = code.replace(Regex("\\s+$", RegexOption.MULTILINE), ""); saved = false }) { Text("AUTO-FIX") } } }
}

@Composable private fun TerminalScreen() {
    val scope = rememberCoroutineScope(); var command by remember { mutableStateOf("") }; var output by remember { mutableStateOf("Maria root terminal\nReady.\n") }
    Column(Modifier.fillMaxSize()) { SectionTitle("ROOT TERMINAL", "Commands run through the libsu root shell"); Text(output, color = Teal, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(10.dp)).padding(12.dp).verticalScroll(rememberScrollState())); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) { OutlinedTextField(value = command, onValueChange = { command = it }, singleLine = true, modifier = Modifier.weight(1f), placeholder = { Text("input keyevent…") }); Spacer(Modifier.width(8.dp)); Button(onClick = { val c = command; command = ""; scope.launch { val r = RootShellHelper.execute(c); output += "\n$ $c\n${r.diagnostics}\n" } }, colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("RUN") } } }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable private fun PreviewScreen() { Column(Modifier.fillMaxSize()) { SectionTitle("LIVE PREVIEW", "http://127.0.0.1:8080/"); AndroidView(modifier = Modifier.fillMaxSize(), factory = { context -> WebView(context).apply { settings.javaScriptEnabled = true; settings.domStorageEnabled = true; webViewClient = WebViewClient(); loadUrl("http://127.0.0.1:8080/") } }) } }

@Composable private fun DevScreen(manager: ProjectFileManager) { val scope = rememberCoroutineScope(); var message by remember { mutableStateOf("Ready") }; Column(Modifier.fillMaxSize()) { SectionTitle("DEVELOPER TOOLS", "Agent diagnostics and advanced workflows"); Button(onClick = { scope.launch { message = manager.exportZip(java.io.File(manager.workspaceDir.parentFile, "exports/landing-page-${System.currentTimeMillis()}.zip")).fold({ "Exported: ${it.name}" }, { "Export failed: ${it.message}" }) } }, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink)) { Text("EXPORT PROJECT ZIP") }; Button(onClick = { scope.launch { message = manager.snapshot("manual").fold({ "Snapshot: ${it.name}" }, { "Snapshot failed: ${it.message}" }) } }, modifier = Modifier.padding(top = 8.dp)) { Text("CREATE SNAPSHOT") }; Text(message, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp)); listOf("Analyze current screen", "Run Auto-Fix", "Restart system monitor", "Reset local microservice").forEach { Activity(it, "Available action", Purple) } } }

@Composable private fun SettingsScreen() { val context = LocalContext.current; var capsule by remember { mutableStateOf(true) }; var assistant by remember { mutableStateOf(true) }; val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { SectionTitle("SETTINGS", "Maria runtime configuration"); Setting("Dynamic top capsule", capsule) { capsule = it }; Setting("Background assistant service", assistant) { assistant = it }; Setting("Live web search browsing", false) {}; Spacer(Modifier.height(12.dp)); Text("DEVICE ACCESS", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp); Button(onClick = { mic.launch(Manifest.permission.RECORD_AUDIO) }, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink), modifier = Modifier.padding(top = 10.dp)) { Text("ALLOW MICROPHONE") }; Button(onClick = { requestBatteryOptimizationExemption(context) }, colors = ButtonDefaults.buttonColors(containerColor = Raised), modifier = Modifier.padding(top = 8.dp)) { Text("ALLOW BACKGROUND SURVIVAL", color = Teal) }; TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("OPEN ACCESSIBILITY SETTINGS", color = Purple) }; TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }) { Text("OPEN OVERLAY SETTINGS", color = Purple) } } }

@Composable private fun SectionTitle(title: String, subtitle: String) { Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp)); Text(subtitle, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp)) }
@Composable private fun CardBox(content: @Composable ColumnScope.() -> Unit) { Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(17.dp), content = content) } }
@Composable private fun QuickCard(title: String, detail: String, color: Color, onClick: () -> Unit) { Column(Modifier.weight(1f).background(Raised, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(14.dp)) { Text(title, color = color, fontWeight = FontWeight.Bold); Text(detail, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)) } }
@Composable private fun Activity(title: String, detail: String, color: Color) { Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(7.dp).background(color, RoundedCornerShape(50))); Spacer(Modifier.width(10.dp)); Column { Text(title, color = Color.White, fontSize = 12.sp); Text(detail, color = Muted, fontSize = 10.sp) } } }
@Composable private fun Setting(label: String, value: Boolean, change: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = Color.White); Switch(checked = value, onCheckedChange = change) } }

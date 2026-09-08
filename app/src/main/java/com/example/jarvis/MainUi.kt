package com.example.jarvis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.agent.AgentState
import com.example.jarvis.agent.AgentStateManager

private val Ink = Color(0xFF090D14)
private val Panel = Color(0xFF111925)
private val PanelRaised = Color(0xFF172333)
private val Teal = Color(0xFF36E0D0)
private val Purple = Color(0xFF9C7CFF)
private val Muted = Color(0xFF8090A5)

private data class NavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val navItems = listOf(
    NavItem("Home", Icons.Default.Home), NavItem("Files", Icons.Default.Folder),
    NavItem("Editor", Icons.Default.Code), NavItem("Terminal", Icons.Default.Terminal),
    NavItem("Preview", Icons.Default.Preview), NavItem("Dev", Icons.Default.Build),
    NavItem("Settings", Icons.Default.Tune),
)

@Composable
fun MariaApp() {
    var selected by remember { mutableIntStateOf(0) }
    val state by AgentStateManager.state.collectAsStateWithLifecycle()
    Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Panel) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(selected = selected == index, onClick = { selected = index }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 9.sp) })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                MariaHeader(state)
                when (selected) {
                    0 -> HomePanel(state, onNavigate = { selected = it })
                    1 -> SimplePanel("PROJECT FILES", "index.html\nstyle.css\nscript.js", Icons.Default.Folder)
                    2 -> SimplePanel("CODE EDITOR", "Select a file to start editing", Icons.Default.Code)
                    3 -> SimplePanel("ROOT TERMINAL", "Command execution is available through the voice agent.", Icons.Default.Terminal)
                    4 -> SimplePanel("LIVE PREVIEW", "Local preview server: http://127.0.0.1:8080", Icons.Default.Preview)
                    5 -> SimplePanel("DEVELOPER TOOLS", "Screen context • Auto-Fix • Project export", Icons.Default.Build)
                    6 -> SettingsPanel()
                }
            }
        }
    }
}

@Composable
private fun MariaHeader(state: AgentState) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("MARIA", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                Text("AUTONOMOUS MOBILE WORKSPACE", color = Teal, fontSize = 9.sp, letterSpacing = 2.sp)
            }
            StatusPill(state)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricPill("READY", Teal); MetricPill("ROOT", Purple); MetricPill("+8080", Color(0xFFFFB86B))
        }
    }
}

@Composable
private fun StatusPill(state: AgentState) {
    val color = when (state) { AgentState.LISTENING -> Color(0xFFFF4F9A); AgentState.THINKING -> Color(0xFFFFA45B); AgentState.ERROR -> Color(0xFFFF5C6C); else -> Teal }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.border(1.dp, color.copy(alpha = .7f), RoundedCornerShape(50)).padding(horizontal = 11.dp, vertical = 7.dp)) {
        Box(Modifier.size(7.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(7.dp)); Text(state.name, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricPill(label: String, color: Color) { Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(color.copy(alpha = .12f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) }

@Composable
private fun HomePanel(state: AgentState, onNavigate: (Int) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("COMMAND CENTER", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp)); Text("What should I build or automate?", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp)); CommandBar()
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionCard("BUILD", "Generate a project", Icons.Default.Build, Teal) { onNavigate(2) }
                ActionCard("SCREEN", "Analyze context", Icons.Default.Preview, Purple) { onNavigate(4) }
            }
        }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("ACTIVE WORKSPACE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp)); Text("landing-page", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 17.sp)
                    Text("index.html • style.css • script.js", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp)); Button(onClick = { onNavigate(4) }, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink)) { Text("OPEN PREVIEW") }
                }
            }
        }
        item { Text("RECENT ACTIVITY", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp) }
        item { ActivityRow("System initialized", "Agent state manager ready", Teal) }
        item { ActivityRow("Builder available", "Local server on port 8080", Purple) }
    }
}

@Composable
private fun CommandBar() {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, placeholder = { Text("Ask Maria anything…", color = Muted) }, modifier = Modifier.fillMaxWidth(), trailingIcon = { TextButton(onClick = { text = "" }) { Text("MIC", color = Teal, fontSize = 10.sp) } })
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) { Column(Modifier.weight(1f).background(PanelRaised, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(14.dp)) { Icon(icon, title, tint = color, modifier = Modifier.size(22.dp)); Spacer(Modifier.height(12.dp)); Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(subtitle, color = Muted, fontSize = 10.sp) } }

@Composable
private fun ActivityRow(title: String, detail: String, color: Color) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(12.dp)) { Box(Modifier.size(7.dp).background(color, RoundedCornerShape(50))); Spacer(Modifier.width(10.dp)); Column { Text(title, color = Color.White, fontSize = 12.sp); Text(detail, color = Muted, fontSize = 10.sp) } } }

@Composable
private fun SimplePanel(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Column(Modifier.fillMaxSize().padding(top = 18.dp)) { Icon(icon, title, tint = Teal, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(12.dp)); Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text(body, color = Muted, fontFamily = FontFamily.Monospace, lineHeight = 22.sp, modifier = Modifier.background(Panel, RoundedCornerShape(14.dp)).fillMaxWidth().padding(18.dp)) } }

@Composable
private fun SettingsPanel() { var capsule by remember { mutableStateOf(true) }; var assistant by remember { mutableStateOf(true) }; Column(Modifier.fillMaxSize().padding(top = 18.dp)) { Text("SETTINGS", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(14.dp)); SettingRow("Dynamic top capsule", capsule) { capsule = it }; SettingRow("Background assistant service", assistant) { assistant = it }; SettingRow("Live web search browsing", false) {}; Spacer(Modifier.height(20.dp)); Text("DEVICE SERVICES", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp); Text("Root shell • Accessibility • Local preview", color = Teal, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) } }

@Composable
private fun SettingRow(label: String, enabled: Boolean, onChange: (Boolean) -> Unit) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) { Text(label, color = Color.White, fontSize = 14.sp); androidx.compose.material3.Switch(checked = enabled, onCheckedChange = onChange) } }

@Composable
private fun <T> StateFlow<T>.collectAsStateCompat(): androidx.compose.runtime.State<T> = androidx.compose.runtime.collectAsState(this)

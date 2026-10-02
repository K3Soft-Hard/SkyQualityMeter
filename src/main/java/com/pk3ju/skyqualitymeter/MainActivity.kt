package com.pk3ju.skyqualitymeter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pk3ju.skyqualitymeter.ui.*

class MainActivity : ComponentActivity() {
    private val viewModel: SqmViewModel by viewModels()
    private var devClicks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsState()
            
            val view = LocalView.current
            val window = window
            val insetsController = WindowCompat.getInsetsController(window, view)
            LaunchedEffect(state.isFullscreen) {
                if (state.isFullscreen) {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            val colors = if (state.isAstroRedMode) AstroRed else if (state.isDarkTheme) StandardDark else StandardLight

            if (state.activeTab == NavigationTab.WELCOME) {
                Scaffold(containerColor = colors.bg, modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                    WelcomeScreen(colors, viewModel::finishWelcome)
                }
                return@setContent
            }

            Scaffold(
                containerColor = colors.bg,
                modifier = if(state.isFullscreen) Modifier.fillMaxSize() else Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
                topBar = { TopBar(state, colors, { viewModel.setAstroRedMode(!state.isAstroRedMode) }, viewModel::startPolling, viewModel::setActiveServer) },
                bottomBar = { BottomNav(state.activeTab, state.betaCaptureUiEnabled, colors, state.textScale, viewModel::setActiveTab) }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    AnimatedContent(targetState = state.activeTab, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "tab") { tab ->
                        when (tab) {
                            NavigationTab.OBSERVATION -> ObservationScreen(state, colors, { name -> viewModel.saveCurrentObservation(name) }, viewModel::setActiveServer, viewModel::startPolling)
                            NavigationTab.RAW_DATA -> RawDataScreen(state, colors)
                            NavigationTab.CAPTURE -> CaptureScreen(
                                state = state,
                                colors = colors,
                                onModeSelect = viewModel::setCaptureMode,
                                onSetVideoInterval = viewModel::setVideoInterval,
                                onSetTimelapseInterval = viewModel::setTimelapseInterval,
                                onToggleAutoName = viewModel::setAutoNameCapture,
                                onStartRecord = viewModel::startRecording,
                                onStopRecord = viewModel::stopRecording,
                                onSave = viewModel::saveCurrentObservation
                            )
                            NavigationTab.GRAPH -> GraphScreen(state, colors, viewModel::exportTelemetryToCsv)
                            NavigationTab.RECORDS -> RecordsScreen(
                                state = state,
                                colors = colors,
                                onDelete = viewModel::removeObservation,
                                onOpenReport = viewModel::openReport,
                                onImport = viewModel::importObservation,
                                onToggleFavorite = viewModel::toggleFavorite
                            )
                            NavigationTab.SETTINGS -> SettingsScreen(
                                state = state,
                                colors = colors,
                                onAddServer = viewModel::addServer,
                                onEditServer = viewModel::editServer,
                                onRemoveServer = viewModel::removeServer,
                                onSwitchServer = viewModel::setActiveServer,
                                onUpdatePref = viewModel::updatePreferences,
                                onEnableDebug = viewModel::enableDebugMode,
                                onDevClick = { 
                                    devClicks++
                                    if (devClicks >= 3) {
                                        viewModel.updatePreferences { it[com.pk3ju.skyqualitymeter.data.SqmPreferencesManager.DEV_MODE_UNLOCKED] = true }
                                    }
                                },
                                onDisableDebug = viewModel::disableDevMode,
                                onExportAppData = viewModel::exportAppDataToFile,
                                onImportAppData = viewModel::importAppDataFromFile
                            )
                            else -> {}
                        }
                    }
                }
                
                if (state.selectedReport != null) {
                    FullscreenReportDialog(
                        obs = state.selectedReport!!,
                        colors = colors,
                        isDark = state.isDarkTheme,
                        isRed = state.isAstroRedMode,
                        onClose = { viewModel.openReport(null) },
                        onShareJson = { viewModel.shareJson(this@MainActivity, state.selectedReport!!) },
                        onExportCsv = viewModel::exportSessionToCsv,
                        onToggleFavorite = viewModel::toggleFavorite
                    )
                }
            }
        }
    }
}

data class AstroColors(val bg: Color, val surface: Color, val card: Color, val border: Color, val primaryText: Color, val secondaryText: Color, val accent: Color, val onlineGreen: Color, val offlineRed: Color)
val StandardDark = AstroColors(Color(0xFF000000), Color(0xFF0A0A0C), Color(0xFF121216), Color(0xFF1F1F24), Color(0xFFEEEEEE), Color(0xFF888894), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFEF4444))
val StandardLight = AstroColors(Color(0xFFF3F4F6), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFE5E7EB), Color(0xFF111827), Color(0xFF6B7280), Color(0xFF2563EB), Color(0xFF059669), Color(0xFFDC2626))
val AstroRed = AstroColors(Color(0xFF000000), Color(0xFF080000), Color(0xFF120000), Color(0xFF330000), Color(0xFFFF2B2B), Color(0xFFAA1111), Color(0xFFFF1A1A), Color(0xFFFF3333), Color(0xFF660000))

@Composable
fun TopBar(state: SqmUiState, colors: AstroColors, onToggleRed: () -> Unit, onRefresh: () -> Unit, onSwitchServer: (String) -> Unit) {
    val s = state.textScale
    var showServerDialog by remember { mutableStateOf(false) }
    var showWebViewDialog by remember { mutableStateOf(false) }
    val webUrl = state.activeServer?.websiteUrl?.trim().orEmpty()
    
    Row(modifier = Modifier.fillMaxWidth().background(colors.surface).padding(horizontal=12.dp, vertical=16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clickable { showServerDialog = true }
                    .background(colors.card, RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(state.activeServer?.name?.uppercase() ?: "SELECT SERVER", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (13 * s).sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch Server", tint = colors.primaryText, modifier = Modifier.size(18.dp))
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.isOnline) "ONLINE" else "OFFLINE",
                color = if (state.isOnline) colors.onlineGreen else colors.offlineRed,
                fontSize = (9 * s).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(colors.surface)
                    .border(1.dp, if(state.isOnline) colors.onlineGreen else colors.offlineRed, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            val themeName = if(state.isDarkTheme) "DARK" else "LIGHT"
            Button(
                onClick = onToggleRed,
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Text(if (state.isAstroRedMode) "WHITE ($themeName)" else "650nm RED", color = colors.accent, fontSize = (9 * s).sp, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            if (webUrl.length > 3) {
                IconButton(onClick = { showWebViewDialog = true }) {
                    Icon(Icons.Default.Public, "Website", tint = colors.accent)
                }
            }
            
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Refresh", tint = colors.secondaryText)
            }
        }
    }

    if (showServerDialog) {
        ServerPickerCenterDialog(
            servers = state.servers,
            activeServerId = state.activeServerId,
            isDebugMode = state.isDebugMode,
            colors = colors,
            onDismiss = { showServerDialog = false },
            onSelect = { id ->
                onSwitchServer(id)
                showServerDialog = false
            }
        )
    }

    if (showWebViewDialog && webUrl.length > 3) {
        val formattedUrl = if (!webUrl.startsWith("http://", ignoreCase = true) && !webUrl.startsWith("https://", ignoreCase = true)) {
            "http://$webUrl"
        } else {
            webUrl
        }
        Dialog(onDismissRequest = { showWebViewDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(modifier = Modifier.fillMaxSize().background(colors.bg).windowInsetsPadding(WindowInsets.systemBars)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(horizontal = 8.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(webUrl, color = colors.primaryText, fontFamily = FontFamily.Monospace, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        IconButton(onClick = { showWebViewDialog = false }) {
                            Icon(Icons.Default.Close, "Close", tint = colors.primaryText)
                        }
                    }
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                webViewClient = android.webkit.WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                loadUrl(formattedUrl)
                            }
                        },
                        update = { it.loadUrl(formattedUrl) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun ServerPickerCenterDialog(
    servers: List<com.pk3ju.skyqualitymeter.data.ServerProfile>,
    activeServerId: String,
    isDebugMode: Boolean,
    colors: AstroColors,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SELECT OBSERVATORY", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isDebugMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = BorderStroke(2.dp, colors.onlineGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom=8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("MOCK SERVER (DEBUG)", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Simulated Payload", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                            Text("ACTIVE", color = colors.onlineGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(servers) { server ->
                        val isCurrent = server.id == activeServerId && !isDebugMode
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isCurrent) colors.card else colors.surface),
                            border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) colors.accent else colors.border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(server.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(server.name.uppercase(), color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${server.ips.firstOrNull() ?: ""} | ${server.path}", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                                if (isCurrent) {
                                    Text("ACTIVE", color = colors.accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("CLOSE", color = colors.secondaryText, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun BottomNav(selected: NavigationTab, isBetaActive: Boolean, colors: AstroColors, scale: Float, onSelect: (NavigationTab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(colors.surface).border(androidx.compose.foundation.BorderStroke(1.dp, colors.border)).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        NavItem(Icons.Default.Visibility, "Main", selected == NavigationTab.OBSERVATION, colors, scale) { onSelect(NavigationTab.OBSERVATION) }
        NavItem(Icons.Default.Code, "Raw", selected == NavigationTab.RAW_DATA, colors, scale) { onSelect(NavigationTab.RAW_DATA) }
        if (isBetaActive) {
            NavItem(Icons.Default.Camera, "Capture", selected == NavigationTab.CAPTURE, colors, scale) { onSelect(NavigationTab.CAPTURE) }
        }
        NavItem(Icons.Default.Assessment, "Graph", selected == NavigationTab.GRAPH, colors, scale) { onSelect(NavigationTab.GRAPH) }
        NavItem(Icons.Default.Star, "Records", selected == NavigationTab.RECORDS, colors, scale) { onSelect(NavigationTab.RECORDS) }
        NavItem(Icons.Default.Settings, "Setup", selected == NavigationTab.SETTINGS, colors, scale) { onSelect(NavigationTab.SETTINGS) }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, colors: AstroColors, scale: Float, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(8.dp)) {
        Icon(icon, label, tint = if (isSelected) colors.accent else colors.secondaryText, modifier = Modifier.size(24.dp))
        Text(label, color = if (isSelected) colors.accent else colors.secondaryText, fontSize = (10 * scale).sp, fontFamily = FontFamily.Monospace)
    }
}

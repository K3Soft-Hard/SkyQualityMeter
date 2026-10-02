package com.pk3ju.skyqualitymeter.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pk3ju.skyqualitymeter.AstroColors
import com.pk3ju.skyqualitymeter.data.BortleClassification
import com.pk3ju.skyqualitymeter.data.RecordType
import com.pk3ju.skyqualitymeter.data.SavedObservation
import com.pk3ju.skyqualitymeter.data.ServerProfile
import com.pk3ju.skyqualitymeter.data.SqmPreferencesManager
import com.pk3ju.skyqualitymeter.data.TelemetryRecord
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

fun formatTime(ms: Long): String {
    if (ms == 0L) return "--:--"
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ms))
}
fun formatDateTime(ms: Long): String {
    if (ms == 0L) return "--"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ms))
}
fun formatElapsedDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

enum class RecordFilterType { ALL, FAVORITES, PHOTOS, VIDEOS, TIMELAPSES }

@Composable
fun WelcomeScreen(colors: AstroColors, onFinish: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("sqm.local") }
    var path by remember { mutableStateOf("/json") }
    var web by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("WELCOME TO SQM 10.0", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Configure your first observatory server to begin.", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Server Name", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP Address", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = path, onValueChange = { path = it }, label = { Text("Endpoint Path (e.g. /json or /data)", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = web, onValueChange = { web = it }, label = { Text("Optional Website URL", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText), modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = { onFinish(name, ip, path, web) }, colors = ButtonDefaults.buttonColors(containerColor = colors.accent), modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("START OBSERVING", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ObservationScreen(state: SqmUiState, colors: AstroColors, onSaveRecord: (String) -> Unit, onSwitchServer: (String) -> Unit, onRetry: () -> Unit) {
    if (!state.isOnline) {
        OfflineScreen(state, colors, onSwitchServer, onRetry)
        return
    }

    val rec = state.currentRecord
    var showDialog by remember { mutableStateOf(false) }
    var locName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        StarfieldBackground(state.isAstroRedMode, state.isDarkTheme)
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            
            Text(
                text = "${rec?.raw?.mode?.uppercase() ?: "WAIT"} MODE",
                color = colors.primaryText,
                fontSize = (24 * state.textScale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("UPDATE TIME: ${formatTime(state.lastUpdateTime)} | IP: ${state.currentActiveIp}", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * state.textScale).sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedContent(targetState = rec?.raw?.sqm, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "sqmFade") { sqmVal ->
                Text(
                    text = if (sqmVal != null) String.format(Locale.US, "%.2f", sqmVal) else "--.--",
                    color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (90 * state.textScale).sp, fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "MPSAS [mag/arcsec²]", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (16 * state.textScale).sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedContent(targetState = rec?.bortleClass, label = "bortleFade") { bortle ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bortleDisplay = if ((rec?.raw?.bortle ?: 0) > 0) "Class ${rec?.raw?.bortle}" else bortle?.level ?: "--"
                    val bortleDescDisplay = if ((rec?.raw?.bortleDesc?.isNotEmpty()) == true) rec.raw.bortleDesc else bortle?.title ?: "Waiting..."
                    Text(text = "Bortle: $bortleDisplay", color = colors.accent, fontFamily = FontFamily.Monospace, fontSize = (18 * state.textScale).sp, fontWeight = FontWeight.Bold)
                    Text(text = bortleDescDisplay, color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (14 * state.textScale).sp)
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = { showDialog = true }, enabled = rec != null, colors = ButtonDefaults.buttonColors(containerColor = colors.card), border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)) { Text("SAVE RECORD", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * state.textScale).sp) }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, containerColor = colors.surface,
            title = { Text("Save Record", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = { OutlinedTextField(value = locName, onValueChange = { locName = it }, label = { Text("Location Name", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText)) },
            confirmButton = { TextButton(onClick = { onSaveRecord(locName); showDialog = false; locName="" }) { Text("Save", color = colors.accent) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel", color = colors.secondaryText) } }
        )
    }
}

@Composable
fun OfflineScreen(state: SqmUiState, colors: AstroColors, onSwitchServer: (String) -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(colors.bg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Public, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(100.dp))
                Canvas(modifier = Modifier.size(120.dp)) {
                    drawLine(color = colors.offlineRed, start = Offset(size.width*0.2f, size.height*0.2f), end = Offset(size.width*0.8f, size.height*0.8f), strokeWidth = 8f, cap = StrokeCap.Round)
                    drawLine(color = colors.offlineRed, start = Offset(size.width*0.8f, size.height*0.2f), end = Offset(size.width*0.2f, size.height*0.8f), strokeWidth = 8f, cap = StrokeCap.Round)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Cant Connect", color = colors.primaryText, fontSize = 28.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Switch Sensors:", color = colors.secondaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.servers) { s ->
                    val isSelected = s.id == state.activeServerId
                    Button(
                        onClick = { onSwitchServer(s.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) colors.accent else Color.Transparent),
                        border = BorderStroke(1.dp, colors.accent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(s.name.uppercase(), color = if (isSelected) Color.White else colors.accent, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("RETRY", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RawDataScreen(state: SqmUiState, colors: AstroColors) {
    val rec = state.currentRecord
    val s = state.textScale
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current
    
    val metrics = listOf(
        Pair("SQM", "${rec?.raw?.sqm ?: "--"}"),
        Pair("Bortle", if ((rec?.raw?.bortle ?: 0) > 0) "Class ${rec?.raw?.bortle}" else "${rec?.bortleClass?.level ?: "--"}"),
        Pair("Bortle Desc", if ((rec?.raw?.bortleDesc?.isNotEmpty()) == true) rec.raw.bortleDesc else "${rec?.bortleClass?.title ?: "--"}"),
        Pair("Brightness (mcd)", "${rec?.raw?.brightnessMcd ?: "--"} mcd/m²"),
        Pair("Artif Bright (µcd)", "${rec?.raw?.artifBrightUcd ?: "--"} µcd/m²"),
        Pair("Freq (f₀)", "${rec?.raw?.frequencyHz ?: "--"} Hz"),
        Pair("Period (T)", "${rec?.raw?.periodMs ?: "--"} ms"),
        Pair("FOV", "${rec?.raw?.fovDeg ?: "--"}°"),
        Pair("Lens Trans", "${rec?.raw?.lensTrans ?: "--"}"),
        Pair("Timeout", "${rec?.raw?.timeoutS ?: "--"} s"),
        Pair("Gate Limit", "${rec?.raw?.minFreqLimit ?: "--"} Hz"),
        Pair("Mode", "${rec?.raw?.mode ?: "--"}"),
        Pair("Uptime", "${rec?.raw?.uptimeS ?: "--"} s"),
        Pair("Valid Packet", "${rec?.raw?.valid ?: "--"}"),
        Pair("Too Dark Flag", "${rec?.raw?.tooDark ?: "--"}"),
        Pair("Ping RTT", "${state.lastRttMs} ms"),
        Pair("Schaefer NELM", String.format(Locale.US, "%.2f mag", rec?.nelm ?: 0.0)),
        Pair("Luminance Flux", String.format(Locale.US, "%.4f cd/m²", rec?.luminanceCdM2 ?: 0.0))
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("DASHBOARD METRICS (ALL VALUES)", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (14 * s).sp)
        Spacer(modifier = Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(metrics) { metric ->
                Card(colors = CardDefaults.cardColors(containerColor = colors.card), border = androidx.compose.foundation.BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(metric.first, color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * s).sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(metric.second, color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (12 * s).sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), border = androidx.compose.foundation.BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RAW JSON PAYLOAD", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (12 * s).sp)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(state.currentRawJson))
                            android.widget.Toast.makeText(context, "JSON Copied", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy JSON", tint = colors.accent, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = state.currentRawJson, color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * s).sp)
            }
        }
    }
}

@Composable
fun CaptureScreen(
    state: SqmUiState,
    colors: AstroColors,
    onModeSelect: (RecordType) -> Unit,
    onSetVideoInterval: (Long) -> Unit,
    onSetTimelapseInterval: (Long) -> Unit,
    onToggleAutoName: (Boolean) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> List<TelemetryRecord>,
    onSave: (String, List<TelemetryRecord>, RecordType) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var locName by remember { mutableStateOf("") }
    var recordedFrames by remember { mutableStateOf(listOf<TelemetryRecord>()) }
    var showTimelapseDialog by remember { mutableStateOf(false) }
    var customTimelapseInput by remember { mutableStateOf(state.timelapseIntervalSec.toString()) }
    var showGraphs by remember { mutableStateOf(true) }
    var showLiveKeogram by remember { mutableStateOf(false) }
    var isNightVisionSquare by remember { mutableStateOf(false) }

    val currentSqm = state.currentRecord?.raw?.sqm ?: 22.0
    val lightness = 1f - ((currentSqm - 10.0) / 12.0).coerceIn(0.0, 1.0).toFloat()
    
    val rawBgCol = if (state.isAstroRedMode) Color(lightness, 0f, 0f) else Color(lightness, lightness, lightness)
    val bgCol = if (isNightVisionSquare) Color.Black else rawBgCol
    val contentCol = if (isNightVisionSquare) Color.White else (if(lightness > 0.6f) Color(0xFF111827) else Color.White)

    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.isRecording, state.recordingStartTimeMs) {
        if (state.isRecording) {
            while (state.isRecording) {
                elapsedMs = System.currentTimeMillis() - state.recordingStartTimeMs
                delay(100)
            }
        } else {
            elapsedMs = 0L
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgCol)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Equal 50/50 Day-Night Horizontal Bar
            Box(modifier = Modifier.fillMaxWidth().height(36.dp).background(Color.Black.copy(alpha=0.2f), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha=0.3f), RoundedCornerShape(8.dp)).padding(2.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gradient = Brush.horizontalGradient(
                        0.0f to Color(0xFFEAB308),
                        0.20f to Color(0xFFEAB308),
                        0.30f to Color(0xFF0F172A),
                        0.70f to Color(0xFF0F172A),
                        0.80f to Color(0xFFEAB308),
                        1.0f to Color(0xFFEAB308)
                    )
                    drawRect(brush = gradient, size = size)

                    val calendar = Calendar.getInstance()
                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f
                    val mappedHour = (currentHour + 12f) % 24f
                    val lineX = (mappedHour / 24f) * size.width

                    drawLine(Color.Red, Offset(lineX, 0f), Offset(lineX, size.height), strokeWidth = 4f)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TOOLS & OVERLAYS", color = contentCol, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row {
                    IconButton(onClick = { showGraphs = !showGraphs }) {
                        Icon(if(showGraphs) androidx.compose.material.icons.Icons.Default.VisibilityOff else androidx.compose.material.icons.Icons.Default.Visibility, "Toggle Graphs", tint = contentCol)
                    }
                    IconButton(onClick = { showLiveKeogram = !showLiveKeogram }) {
                        Icon(androidx.compose.material.icons.Icons.Default.ViewWeek, "Live Keogram", tint = if(showLiveKeogram) colors.accent else contentCol)
                    }
                    IconButton(onClick = { isNightVisionSquare = !isNightVisionSquare }) {
                        Icon(if(isNightVisionSquare) androidx.compose.material.icons.Icons.Default.CenterFocusStrong else androidx.compose.material.icons.Icons.Default.Fullscreen, "Night Vision Pixel", tint = if(isNightVisionSquare) colors.accent else contentCol)
                    }
                }
            }
            
            if (showGraphs) {
                Box(modifier = Modifier.fillMaxWidth().height(95.dp).background(Color.Black.copy(alpha=0.4f), RoundedCornerShape(8.dp)).padding(6.dp)) {
                    MultiLineChart(
                        data1 = state.history.map { Pair(it.timestamp, it.raw.sqm.toFloat()) }, color1 = Color(0xFF3B82F6),
                        data2 = state.history.map { Pair(it.timestamp, it.raw.frequencyHz.toFloat()) }, color2 = Color(0xFF10B981),
                        gridColor = Color.White.copy(alpha=0.2f), textColor = Color.White, scale = state.textScale * 0.9f
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (showLiveKeogram) {
                val frames = if(state.isRecording) state.liveRecordingFrames else state.history
                if (frames.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.Black.copy(alpha=0.4f), RoundedCornerShape(8.dp)).padding(4.dp)) {
                        KeogramView(history = frames, isRed = state.isAstroRedMode, modifier = Modifier.fillMaxSize())
                        Text("LIVE KEOGRAM (${frames.size} FRAMES)", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.TopStart).background(Color.Black.copy(alpha=0.5f)).padding(2.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Top area under graph: Top Left has the Auto-name toggle switch (+ pixel box stacked below it); Top Right has the metrics
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.Start) {
                    Switch(
                        checked = state.autoNameCapture,
                        onCheckedChange = { onToggleAutoName(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = contentCol,
                            checkedTrackColor = contentCol.copy(alpha = 0.3f),
                            uncheckedThumbColor = contentCol.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.Black.copy(alpha = 0.2f)
                        )
                    )

                    // Night-vision pixel box: bigger now, stacked directly under the Auto-Name switch
                    if (isNightVisionSquare) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.size(150.dp).background(rawBgCol, RoundedCornerShape(16.dp)).border(2.dp, colors.border, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            val sqmL = if(lightness > 0.5f) Color.Black else Color.White
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(String.format(Locale.US, "%.1f", currentSqm), color = sqmL, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("SQM", color = sqmL, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // Metrics always anchored top-right, whether or not the pixel box is showing
                Column(horizontalAlignment = Alignment.End) {
                    Text(String.format(Locale.US, "%.2f MPSAS", currentSqm), color = contentCol, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(String.format(Locale.US, "%.5f cd/m² (Lux)", state.currentRecord?.luminanceCdM2 ?: 0.0), color = contentCol.copy(alpha=0.8f), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    val bortleDisplay = if ((state.currentRecord?.raw?.bortle ?: 0) > 0) "Class ${state.currentRecord?.raw?.bortle}" else state.currentRecord?.bortleClass?.level ?: "--"
                    val bortleDescDisplay = if ((state.currentRecord?.raw?.bortleDesc?.isNotEmpty()) == true) state.currentRecord?.raw?.bortleDesc ?: "" else state.currentRecord?.bortleClass?.title ?: ""
                    Text(bortleDisplay, color = contentCol, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(bortleDescDisplay, color = contentCol.copy(alpha=0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            if (state.isRecording) {
                 Text("REC ${formatElapsedDuration(elapsedMs)} | Frames: ${state.recordingFramesCount}", color = Color.Red, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
            }
            
            // Secondary Interval/Speed Bar for Video and Timelapse
            AnimatedVisibility(visible = state.currentCaptureMode != RecordType.PHOTO) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.currentCaptureMode == RecordType.VIDEO) {
                        Text("SPEED:", color = contentCol.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        val speeds = listOf(100L to "0.1s", 250L to "0.25s", 500L to "0.50s", 1000L to "1s")
                        speeds.forEach { (ms, label) ->
                            val isSel = state.videoIntervalMs == ms
                            Box(
                                modifier = Modifier
                                    .background(if (isSel) contentCol else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { if (!state.isRecording) onSetVideoInterval(ms) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (isSel) bgCol else contentCol,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (state.currentCaptureMode == RecordType.TIMELAPSE) {
                        Text("INTERVAL:", color = contentCol.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(contentCol.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, contentCol.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { 
                                    if (!state.isRecording) {
                                        customTimelapseInput = state.timelapseIntervalSec.toString()
                                        showTimelapseDialog = true
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${state.timelapseIntervalSec}s (tap to edit)",
                                color = contentCol,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Mode Selector Ovals
            Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(16.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(RecordType.TIMELAPSE, RecordType.PHOTO, RecordType.VIDEO).forEach { mode ->
                    val isSelected = state.currentCaptureMode == mode
                    Box(
                        modifier = Modifier.background(if (isSelected) contentCol else Color.Transparent, RoundedCornerShape(16.dp))
                                           .clickable { if(!state.isRecording) onModeSelect(mode) }
                                           .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(mode.name, color = if(isSelected) bgCol else contentCol.copy(alpha=0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Capture Button (Always a Perfect Circle)
            Box(
                modifier = Modifier.size(80.dp).background(Color.Transparent, CircleShape).border(4.dp, contentCol, CircleShape).clickable {
                    if (state.isRecording) {
                        recordedFrames = onStopRecord()
                        if (state.autoNameCapture) {
                            val autoName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                            onSave(autoName, recordedFrames, state.currentCaptureMode)
                        } else {
                            showDialog = true
                        }
                    } else {
                        when (state.currentCaptureMode) {
                            RecordType.PHOTO -> { 
                                recordedFrames = emptyList()
                                if (state.autoNameCapture) {
                                    val autoName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                                    onSave(autoName, emptyList(), RecordType.PHOTO)
                                } else {
                                    showDialog = true 
                                }
                            }
                            RecordType.VIDEO -> { onStartRecord() }
                            RecordType.TIMELAPSE -> { onStartRecord() }
                        }
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(60.dp).background(if (state.isRecording) Color.Red else contentCol, CircleShape))
            }
        }
    }
    
    if (showTimelapseDialog) {
        AlertDialog(
            onDismissRequest = { showTimelapseDialog = false }, containerColor = colors.surface,
            title = { Text("Timelapse Interval", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = { OutlinedTextField(value = customTimelapseInput, onValueChange = { customTimelapseInput = it }, label = { Text("Seconds between frames", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText)) },
            confirmButton = { TextButton(onClick = { val s = (customTimelapseInput.toLongOrNull() ?: 5L).coerceAtLeast(1L); onSetTimelapseInterval(s); showTimelapseDialog = false }) { Text("Set", color = colors.accent) } },
            dismissButton = { TextButton(onClick = { showTimelapseDialog = false }) { Text("Cancel", color = colors.secondaryText) } }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, containerColor = colors.surface,
            title = { Text("Save ${state.currentCaptureMode.name}", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = { OutlinedTextField(value = locName, onValueChange = { locName = it }, label = { Text("Location Name", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText)) },
            confirmButton = { TextButton(onClick = { onSave(locName, recordedFrames, state.currentCaptureMode); showDialog = false; locName="" }) { Text("Save", color = colors.accent) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel", color = colors.secondaryText) } }
        )
    }
}


@Composable
fun GraphScreen(state: SqmUiState, colors: AstroColors, onExport: (Context, android.net.Uri) -> Unit) {
    val s = state.textScale
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) onExport(context, uri)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DataCard("COMBINED MULTI-AXIS CHART", colors, s) {
            Text("Left Y (Blue): SQM | Right Y (Green): Freq Hz", color = colors.secondaryText, fontSize = (9*s).sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) { 
                MultiLineChart(
                    data1 = state.history.map { Pair(it.timestamp, it.raw.sqm.toFloat()) }, color1 = colors.accent,
                    data2 = state.history.map { Pair(it.timestamp, it.raw.frequencyHz.toFloat()) }, color2 = colors.onlineGreen,
                    gridColor = colors.border, textColor = colors.secondaryText, scale = s
                ) 
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { launcher.launch("sqm_telemetry_${System.currentTimeMillis()}.csv") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) { Text("EXPORT SQM & FREQ CSV", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp) }
        }
        
        DataCard("PING (RTT) MONITOR", colors, s) {
            Text("Network Latency (ms)", color = colors.secondaryText, fontSize = (9*s).sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) { 
                SingleLineChart(
                    data = state.history.map { Pair(it.timestamp, it.rttMs.toFloat()) }, color = colors.primaryText,
                    gridColor = colors.border, textColor = colors.secondaryText, scale = s
                ) 
            }
        }
    }
}

@Composable
fun SingleLineChart(data: List<Pair<Long, Float>>, color: Color, gridColor: Color, textColor: Color, scale: Float) {
    if (data.size < 2) return
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = textColor, fontSize = (9 * scale).sp, fontFamily = FontFamily.Monospace)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val points = data.map { it.second }
        val minVal = 0f
        val maxVal = (points.maxOrNull() ?: 100f) * 1.2f
        val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal
        
        val leftPad = if(textColor == Color.Transparent) 0f else 80f * scale
        val bottomPad = if(textColor == Color.Transparent) 0f else 60f * scale
        val chartW = size.width - leftPad; val chartH = size.height - bottomPad
        val stepX = chartW / (data.size - 1)

        if (textColor != Color.Transparent) {
            for (i in 0..4) {
                val y = chartH * (i / 4f)
                drawLine(gridColor, Offset(leftPad, y), Offset(size.width, y), 1f)
                drawText(textMeasurer, String.format(Locale.US, "%.0f ms", maxVal - ((i/4f)*range)), Offset(0f, y - 20f), style = textStyle)
            }
        }
        
        val path = Path()
        data.forEachIndexed { i, pair ->
            val x = leftPad + (i * stepX)
            val y = chartH - (((pair.second - minVal) / range) * chartH)
            if (i == 0) { path.moveTo(x, y) } else { path.lineTo(x, y) }
        }
        drawPath(path, color, style = Stroke(width = 2f, cap = StrokeCap.Round))
    }
}

@Composable
fun MultiLineChart(data1: List<Pair<Long, Float>>, color1: Color, data2: List<Pair<Long, Float>>, color2: Color, gridColor: Color, textColor: Color, scale: Float) {
    if (data1.size < 2) return
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = textColor, fontSize = (9 * scale).sp, fontFamily = FontFamily.Monospace)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val p1 = data1.map { it.second }; val p2 = data2.map { it.second }
        val min1 = (p1.minOrNull() ?: 0f) * 0.98f; val max1 = (p1.maxOrNull() ?: 22f) * 1.02f
        val min2 = (p2.minOrNull() ?: 0f) * 0.98f; val max2 = (p2.maxOrNull() ?: 200000f) * 1.02f
        val range1 = if (max1 - min1 == 0f) 1f else max1 - min1
        val range2 = if (max2 - min2 == 0f) 1f else max2 - min2
        
        val leftPad = 80f * scale; val rightPad = 120f * scale; val bottomPad = 60f * scale
        val chartW = size.width - leftPad - rightPad; val chartH = size.height - bottomPad
        val stepX = chartW / (data1.size - 1)

        for (i in 0..4) {
            val y = chartH * (i / 4f)
            drawLine(gridColor, Offset(leftPad, y), Offset(size.width - rightPad, y), 1f)
            drawText(textMeasurer, String.format(Locale.US, "%.1f", max1 - ((i/4f)*range1)), Offset(0f, y - 20f), style = textStyle)
            drawText(textMeasurer, String.format(Locale.US, "%.0f", max2 - ((i/4f)*range2)), Offset(size.width - rightPad + 10f, y - 20f), style = textStyle)
        }
        
        val path1 = Path(); val path2 = Path()
        data1.forEachIndexed { i, pair ->
            val x = leftPad + (i * stepX)
            val y1 = chartH - (((pair.second - min1) / range1) * chartH)
            val y2 = chartH - (((data2[i].second - min2) / range2) * chartH)
            if (i == 0) { path1.moveTo(x, y1); path2.moveTo(x, y2) } else { path1.lineTo(x, y1); path2.lineTo(x, y2) }
        }
        drawPath(path1, color1, style = Stroke(width = 3f, cap = StrokeCap.Round))
        drawPath(path2, color2, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}

@Composable
fun RecordsScreen(
    state: SqmUiState,
    colors: AstroColors,
    onDelete: (String) -> Unit,
    onOpenReport: (SavedObservation) -> Unit,
    onImport: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit
) {
    val s = state.textScale
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonStr by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf(false) }
    var obsToDelete by remember { mutableStateOf<SavedObservation?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RecordFilterType.ALL) }
    
    val savedAvg = if (state.savedObservations.isNotEmpty()) state.savedObservations.map { it.raw.sqm }.average() else 0.0
    val savedBest = state.savedObservations.maxByOrNull { it.raw.sqm }

    val filteredObservations = remember(state.savedObservations, searchQuery, selectedFilter) {
        state.savedObservations.filter { obs ->
            val query = searchQuery.trim()
            val matchesQuery = query.isEmpty() ||
                obs.locationName.contains(query, ignoreCase = true) ||
                obs.bortleLevel.contains(query, ignoreCase = true) ||
                obs.raw.mode.contains(query, ignoreCase = true) ||
                obs.raw.bortleDesc.contains(query, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                RecordFilterType.ALL -> true
                RecordFilterType.FAVORITES -> obs.isFavorite
                RecordFilterType.PHOTOS -> obs.type == RecordType.PHOTO
                RecordFilterType.VIDEOS -> obs.type == RecordType.VIDEO
                RecordFilterType.TIMELAPSES -> obs.type == RecordType.TIMELAPSE
            }
            matchesQuery && matchesFilter
        }.sortedWith(
            compareByDescending<SavedObservation> { it.isFavorite }
                .thenByDescending { it.raw.sqm }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DataCard("SAVED RECORDS STATS", colors, s) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Darkest Saved SQM", color = colors.secondaryText, fontSize = (11 * s).sp, fontFamily = FontFamily.Monospace)
                        Text(String.format(Locale.US, "%.2f", savedBest?.raw?.sqm ?: 0.0), color = colors.primaryText, fontSize = (18 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(formatDateTime(savedBest?.timestamp ?: 0L), color = colors.accent, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Average Saved", color = colors.secondaryText, fontSize = (11 * s).sp, fontFamily = FontFamily.Monospace)
                        Text(String.format(Locale.US, "%.2f", savedAvg), color = colors.primaryText, fontSize = (18 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${state.savedObservations.size} total places", color = colors.accent, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search location, Bortle, mode...", color = colors.secondaryText, fontSize = (11 * s).sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.secondaryText) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.secondaryText)
                        }
                    }
                },
                textStyle = TextStyle(color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (13 * s).sp),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                val filters = listOf(
                    RecordFilterType.ALL to "ALL (${state.savedObservations.size})",
                    RecordFilterType.FAVORITES to "★ FAVORITES (${state.savedObservations.count { it.isFavorite }})",
                    RecordFilterType.PHOTOS to "PHOTOS (${state.savedObservations.count { it.type == RecordType.PHOTO }})",
                    RecordFilterType.VIDEOS to "VIDEOS (${state.savedObservations.count { it.type == RecordType.VIDEO }})",
                    RecordFilterType.TIMELAPSES to "TIMELAPSES (${state.savedObservations.count { it.type == RecordType.TIMELAPSE }})"
                )
                items(filters) { (filterType, title) ->
                    val isSelected = selectedFilter == filterType
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) colors.accent else colors.card, RoundedCornerShape(20.dp))
                            .border(BorderStroke(1.dp, if (isSelected) colors.accent else colors.border), RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = filterType }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else colors.secondaryText,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10 * s).sp
                        )
                    }
                }
            }
        }
        
        item {
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Text("IMPORT RECORD FROM JSON", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SAVED PLACES", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (14 * s).sp)
                Text("${filteredObservations.size} shown", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (11 * s).sp)
            }
        }
        
        if (filteredObservations.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    border = BorderStroke(1.dp, colors.border),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (state.savedObservations.isEmpty()) "No records saved yet." else "No records match search & filter.",
                            color = colors.secondaryText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (12 * s).sp
                        )
                    }
                }
            }
        }
        
        items(filteredObservations, key = { it.id }) { obs ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.card), border = androidx.compose.foundation.BorderStroke(1.dp, if (obs.isFavorite) colors.accent else colors.border),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().clickable { onOpenReport(obs) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            val icon = when(obs.type) { RecordType.VIDEO -> Icons.Default.Videocam; RecordType.TIMELAPSE -> Icons.Default.Timer; else -> Icons.Default.Image }
                            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(obs.locationName.uppercase(), color = colors.primaryText, fontSize = (14 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                val bortleDisplay = if (obs.raw.bortle > 0) "Class ${obs.raw.bortle}" else obs.bortleLevel
                                Text("${String.format(Locale.US, "%.2f", obs.raw.sqm)} MPSAS - $bortleDisplay", color = colors.accent, fontSize = (12 * s).sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onToggleFavorite(obs.id) }) {
                                Icon(
                                    if (obs.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (obs.isFavorite) Color(0xFFFACC15) else colors.secondaryText
                                )
                            }
                            IconButton(onClick = { obsToDelete = obs }) { Icon(Icons.Default.Delete, "Delete", tint = colors.offlineRed) }
                        }
                    }
                }
            }
        }
    }

    if (obsToDelete != null) {
        AlertDialog(
            onDismissRequest = { obsToDelete = null },
            containerColor = colors.surface,
            title = { Text("Confirm Deletion", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = { Text("Permanently delete '${obsToDelete!!.locationName}'?", color = colors.secondaryText, fontFamily = FontFamily.Monospace) },
            confirmButton = {
                TextButton(onClick = { onDelete(obsToDelete!!.id); obsToDelete = null }) {
                    Text("DELETE", color = colors.offlineRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { obsToDelete = null }) { Text("CANCEL", color = colors.secondaryText) }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; importError = false; importJsonStr = "" },
            containerColor = colors.surface,
            title = { Text("Import Record", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    OutlinedTextField(
                        value = importJsonStr, onValueChange = { importJsonStr = it; importError = false },
                        label = { Text("Paste JSON", color = colors.secondaryText) },
                        textStyle = TextStyle(color = colors.primaryText),
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                    if (importError) {
                        Text("Invalid JSON Format", color = colors.offlineRed, fontSize = 10.sp, modifier = Modifier.padding(top=4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onImport(importJsonStr)) {
                        showImportDialog = false; importJsonStr = ""; importError = false
                    } else {
                        importError = true
                    }
                }) { Text("IMPORT", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importError = false; importJsonStr = "" }) { Text("CANCEL", color = colors.secondaryText) }
            }
        )
    }
}

@Composable
fun FullscreenReportDialog(
    obs: SavedObservation,
    colors: AstroColors,
    isDark: Boolean,
    isRed: Boolean,
    onClose: () -> Unit,
    onShareJson: () -> Unit,
    onExportCsv: (Context, android.net.Uri, SavedObservation) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) onExportCsv(context, uri, obs)
    }
    
    var isPlaying by remember { mutableStateOf(false) }
    var frameIndex by remember { mutableFloatStateOf(0f) }
    var isImmersive by remember { mutableStateOf(false) }
    var isKeogramFullscreen by remember { mutableStateOf(false) }
    var showImmersiveChart by remember { mutableStateOf(false) }
    var showStandardChart by remember { mutableStateOf(true) }
    var timelapseFps by remember { mutableIntStateOf(15) }
    var showFpsMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(isPlaying, timelapseFps) {
        if (isPlaying && obs.history.isNotEmpty()) {
            while(isPlaying && frameIndex < obs.history.lastIndex) {
                val delayTime = if (obs.type == RecordType.TIMELAPSE) {
                    (1000L / timelapseFps).coerceIn(16L, 1000L)
                } else {
                    val currentFrame = obs.history[frameIndex.toInt()]
                    val nextFrame = obs.history[frameIndex.toInt() + 1]
                    val delta = nextFrame.timestamp - currentFrame.timestamp
                    if (delta > 0L) delta.coerceIn(10L, 5000L) else obs.frameIntervalMs.coerceIn(10L, 5000L)
                }
                delay(delayTime)
                frameIndex += 1f
            }
            if (frameIndex >= obs.history.lastIndex) {
                isPlaying = false
            }
        }
    }
    
    val currentIndex = frameIndex.toInt().coerceIn(0, maxOf(0, obs.history.lastIndex))
    val currentData = if (obs.history.isNotEmpty()) obs.history[currentIndex] else TelemetryRecord(
        timestamp = obs.timestamp,
        raw = obs.raw,
        luminanceCdM2 = obs.luminanceCdM2,
        nelm = obs.nelm,
        bortleClass = if (obs.raw.bortle > 0) BortleClassification("Class ${obs.raw.bortle}", obs.raw.bortleDesc) else BortleClassification(obs.bortleLevel, ""),
        rttMs = 0L
    )

    val bortleTitle = if (currentData.raw.bortleDesc.isNotEmpty()) currentData.raw.bortleDesc else currentData.bortleClass.title
    val bortleLabel = if (currentData.raw.bortle > 0) "Class ${currentData.raw.bortle}" else currentData.bortleClass.level

    if (isImmersive) {
        val lightness = 1f - ((currentData.raw.sqm - 10.0) / 12.0).coerceIn(0.0, 1.0).toFloat()
        val bgCol = if (isRed) Color(lightness, 0f, 0f) else Color(lightness, lightness, lightness)
        val contentCol = if(lightness > 0.6f) Color(0xFF111827) else Color.White

        Dialog(onDismissRequest = { isImmersive = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(modifier = Modifier.fillMaxSize().background(bgCol)) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (obs.history.isNotEmpty()) Arrangement.SpaceBetween else Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        if (obs.history.isNotEmpty()) {
                            IconButton(onClick = { showImmersiveChart = !showImmersiveChart }) {
                                Icon(if (showImmersiveChart) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle Chart", tint = contentCol)
                            }
                        }
                        IconButton(onClick = { isImmersive = false }) {
                            Icon(Icons.Default.FullscreenExit, "Exit Fullscreen", tint = contentCol)
                        }
                    }

                    if (showImmersiveChart && obs.history.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                            MultiLineChart(
                                data1 = obs.history.map { Pair(it.timestamp, it.raw.sqm.toFloat()) }, color1 = Color(0xFF3B82F6),
                                data2 = obs.history.map { Pair(it.timestamp, it.raw.frequencyHz.toFloat()) }, color2 = Color(0xFF10B981),
                                gridColor = Color.White.copy(alpha=0.2f), textColor = Color.White, scale = 0.9f
                            )
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val leftPad = 80f * 0.9f; val rightPad = 120f * 0.9f
                                val chartW = size.width - leftPad - rightPad
                                val stepX = chartW / maxOf(1, obs.history.size - 1)
                                val x = leftPad + (currentIndex * stepX)
                                drawLine(Color.Red, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(String.format(Locale.US, "%.2f", currentData.raw.sqm), color = contentCol, fontSize = 80.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("MPSAS | $bortleLabel", color = contentCol, fontSize = 22.sp, fontFamily = FontFamily.Monospace)
                        if (bortleTitle.isNotEmpty()) {
                            Text(bortleTitle, color = contentCol.copy(alpha=0.8f), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    if (obs.history.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                            if (obs.type == RecordType.TIMELAPSE) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("TIMELAPSE SPEED", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Box {
                                        TextButton(onClick = { showFpsMenu = true }) {
                                            Icon(Icons.Default.Speed, contentDescription = "FPS", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${timelapseFps} FPS", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        DropdownMenu(expanded = showFpsMenu, onDismissRequest = { showFpsMenu = false }, modifier = Modifier.background(colors.card)) {
                                            listOf(2, 4, 8, 15, 20, 25, 30, 60).forEach { fps ->
                                                DropdownMenuItem(
                                                    text = { Text("$fps FPS", color = if (fps == timelapseFps) colors.accent else colors.primaryText, fontFamily = FontFamily.Monospace) },
                                                    onClick = { timelapseFps = fps; showFpsMenu = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                IconButton(onClick = { frameIndex = kotlin.math.max(0f, frameIndex - 1f) }) { Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White) }
                                IconButton(onClick = { 
                                    if (!isPlaying && frameIndex >= obs.history.lastIndex) {
                                        frameIndex = 0f
                                    }
                                    isPlaying = !isPlaying 
                                }) { 
                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(32.dp)) 
                                }
                                IconButton(onClick = { frameIndex = kotlin.math.min(obs.history.lastIndex.toFloat(), frameIndex + 1f) }) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White) }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${currentIndex + 1}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Slider(
                                    value = frameIndex,
                                    onValueChange = { frameIndex = it; isPlaying = false },
                                    valueRange = 0f..maxOf(0f, obs.history.lastIndex.toFloat()),
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha=0.3f))
                                )
                                Text("${obs.history.size}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            StarfieldBackground(isRed, isDark)
            Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close", tint = colors.primaryText) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onToggleFavorite(obs.id) }) {
                            Icon(
                                if (obs.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (obs.isFavorite) Color(0xFFFACC15) else colors.secondaryText
                            )
                        }
                        IconButton(onClick = { isImmersive = true }) { Icon(Icons.Default.Fullscreen, "Fullscreen View", tint = colors.primaryText) }
                        if (obs.history.isNotEmpty()) {
                            IconButton(onClick = { showStandardChart = !showStandardChart }) { Icon(if (showStandardChart) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle Chart", tint = colors.primaryText) }
                            IconButton(onClick = { launcher.launch("sqm_session_${obs.locationName}.csv") }) { Icon(Icons.Default.FileDownload, "Export CSV", tint = colors.primaryText) }
                        }
                        IconButton(onClick = onShareJson) { Icon(Icons.Default.Share, "Share JSON", tint = colors.accent) }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${obs.locationName.uppercase()} [${obs.type.name}]", color = colors.primaryText, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    if (obs.isFavorite) {
                        Text("★ FAVORITE", color = Color(0xFFFACC15), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
                Text(formatDateTime(currentData.timestamp), color = colors.secondaryText, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                
                Spacer(modifier = Modifier.height(32.dp))
                Text(String.format(Locale.US, "%.2f", currentData.raw.sqm), color = colors.accent, fontSize = 80.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("MPSAS | $bortleLabel", color = colors.primaryText, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                if (bortleTitle.isNotEmpty()) {
                    Text(bortleTitle, color = colors.secondaryText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }

                var showKeogram by remember { mutableStateOf(false) }
                
                if (obs.history.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (showStandardChart) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("SESSION TELEMETRY TIMELINE", color = colors.secondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            TextButton(onClick = { showKeogram = !showKeogram }) {
                                Icon(Icons.Default.ViewWeek, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if(showKeogram) "HIDE KEOGRAM" else "SHOW KEOGRAM", color = colors.accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(colors.card, RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(8.dp)) {
                            MultiLineChart(
                                data1 = obs.history.map { Pair(it.timestamp, it.raw.sqm.toFloat()) }, color1 = colors.accent,
                                data2 = obs.history.map { Pair(it.timestamp, it.raw.frequencyHz.toFloat()) }, color2 = colors.onlineGreen,
                                gridColor = colors.border, textColor = colors.secondaryText, scale = 1f
                            )
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val leftPad = 80f; val rightPad = 120f
                                val chartW = size.width - leftPad - rightPad
                                val stepX = chartW / maxOf(1, obs.history.size - 1)
                                val x = leftPad + (currentIndex * stepX)
                                drawLine(Color.Red, Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    if (showKeogram && obs.history.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("SESSION KEOGRAM", color = colors.secondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            TextButton(onClick = { isKeogramFullscreen = true }) {
                                Icon(Icons.Default.Fullscreen, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("FULLSCREEN", color = colors.accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(colors.card, RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(4.dp)) {
                            KeogramView(
                                history = obs.history,
                                isRed = isRed,
                                modifier = Modifier.fillMaxSize(),
                                currentIndex = currentIndex,
                                onFrameClick = { idx -> 
                                    frameIndex = idx.toFloat()
                                    isPlaying = false 
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (isKeogramFullscreen) {
                        Dialog(onDismissRequest = { isKeogramFullscreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("SESSION KEOGRAM", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        IconButton(onClick = { isKeogramFullscreen = false }) {
                                            Icon(Icons.Default.FullscreenExit, "Exit Fullscreen", tint = Color.White)
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                        KeogramView(
                                            history = obs.history,
                                            isRed = isRed,
                                            modifier = Modifier.fillMaxSize(),
                                            currentIndex = currentIndex,
                                            onFrameClick = { idx ->
                                                frameIndex = idx.toFloat()
                                                isPlaying = false
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        "Frame ${currentIndex + 1} / ${obs.history.size} — tap the keogram to scrub",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        if (obs.type == RecordType.TIMELAPSE) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("TIMELAPSE PLAYBACK FPS", color = colors.secondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Box {
                                    TextButton(onClick = { showFpsMenu = true }) {
                                        Icon(Icons.Default.Speed, contentDescription = "FPS", tint = colors.accent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${timelapseFps} FPS", color = colors.accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    DropdownMenu(expanded = showFpsMenu, onDismissRequest = { showFpsMenu = false }, modifier = Modifier.background(colors.card)) {
                                        listOf(2, 4, 8, 15, 20, 25, 30, 60).forEach { fps ->
                                            DropdownMenuItem(
                                                text = { Text("$fps FPS", color = if (fps == timelapseFps) colors.accent else colors.primaryText, fontFamily = FontFamily.Monospace) },
                                                onClick = { timelapseFps = fps; showFpsMenu = false }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = { frameIndex = kotlin.math.max(0f, frameIndex - 1f) }) { Icon(Icons.Default.SkipPrevious, "Prev", tint = colors.primaryText) }
                            IconButton(onClick = { 
                                if (!isPlaying && frameIndex >= obs.history.lastIndex) {
                                    frameIndex = 0f
                                }
                                isPlaying = !isPlaying 
                            }) { 
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = colors.accent, modifier = Modifier.size(32.dp)) 
                            }
                            IconButton(onClick = { frameIndex = kotlin.math.min(obs.history.lastIndex.toFloat(), frameIndex + 1f) }) { Icon(Icons.Default.SkipNext, "Next", tint = colors.primaryText) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${currentIndex + 1}", color = colors.secondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Slider(
                                value = frameIndex,
                                onValueChange = { frameIndex = it; isPlaying = false },
                                valueRange = 0f..maxOf(0f, obs.history.lastIndex.toFloat()),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent, inactiveTrackColor = colors.border)
                            )
                            Text("${obs.history.size}", color = colors.secondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("FRAME TELEMETRY DUMP", color = colors.secondaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))
                
                RegisterRow("SQM Reading", String.format(Locale.US, "%.2f", currentData.raw.sqm), colors, 1f)
                RegisterRow("Bortle Level", bortleLabel, colors, 1f)
                RegisterRow("Bortle Description", bortleTitle, colors, 1f)
                RegisterRow("Brightness (mcd)", "${currentData.raw.brightnessMcd} mcd/m²", colors, 1f)
                RegisterRow("Artif Brightness (µcd)", "${currentData.raw.artifBrightUcd} µcd/m²", colors, 1f)
                RegisterRow("Frequency (f₀)", "${currentData.raw.frequencyHz} Hz", colors, 1f)
                RegisterRow("Period (T)", "${currentData.raw.periodMs} ms", colors, 1f)
                RegisterRow("Sensor Mode", currentData.raw.mode, colors, 1f)
                RegisterRow("FOV (deg)", "${currentData.raw.fovDeg}°", colors, 1f)
                RegisterRow("Lens Trans", "${currentData.raw.lensTrans}", colors, 1f)
                RegisterRow("Timeout", "${currentData.raw.timeoutS} s", colors, 1f)
                RegisterRow("Gate Timeout", "${currentData.raw.minFreqLimit} Hz", colors, 1f)
                RegisterRow("Controller Uptime", "${currentData.raw.uptimeS} s", colors, 1f)
                RegisterRow("Valid Packet", "${currentData.raw.valid}", colors, 1f)
                RegisterRow("Too Dark Flag", "${currentData.raw.tooDark}", colors, 1f)
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("PHYSICS DERIVATIONS", color = colors.secondaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))
                RegisterRow("Luminance Flux", String.format(Locale.US, "%.4f cd/m²", currentData.luminanceCdM2), colors, 1f)
                RegisterRow("Schaefer NELM", String.format(Locale.US, "%.2f mag", currentData.nelm), colors, 1f)
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: SqmUiState,
    colors: AstroColors,
    onAddServer: (String, String, String, String) -> Unit,
    onEditServer: (String, String, String, String, String) -> Unit,
    onRemoveServer: (String) -> Unit,
    onSwitchServer: (String) -> Unit,
    onUpdatePref: (suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) -> Unit,
    onEnableDebug: (String) -> Boolean,
    onDevClick: () -> Unit,
    onDisableDebug: () -> Unit,
    onExportAppData: (Context, Uri, ExportOption) -> Unit,
    onImportAppData: (Context, Uri, (Boolean, String) -> Unit) -> Unit
) {
    val s = state.textScale
    val context = LocalContext.current
    var newName by remember { mutableStateOf("") }; var newIp by remember { mutableStateOf("") }
    var newPath by remember { mutableStateOf("/json") }; var newWeb by remember { mutableStateOf("") }
    
    var serverToEdit by remember { mutableStateOf<ServerProfile?>(null) }
    var editName by remember { mutableStateOf("") }; var editIp by remember { mutableStateOf("") }
    var editPath by remember { mutableStateOf("") }; var editWeb by remember { mutableStateOf("") }

    var showDebugDialog by remember { mutableStateOf(false) }
    var debugJsonStr by remember { mutableStateOf("") }
    var debugError by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportOption by remember { mutableStateOf(ExportOption.ALL) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            onExportAppData(context, uri, exportOption)
            Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onImportAppData(context, uri) { success, msg ->
                Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DataCard("APP PREFERENCES", colors, s) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Theme", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
                Row {
                    TextButton(onClick = { onUpdatePref { it[SqmPreferencesManager.IS_DARK_THEME] = true } }) { Text("DARK", color = if(state.isDarkTheme) colors.accent else colors.secondaryText, fontSize = (12 * s).sp) }
                    TextButton(onClick = { onUpdatePref { it[SqmPreferencesManager.IS_DARK_THEME] = false } }) { Text("LIGHT", color = if(!state.isDarkTheme) colors.accent else colors.secondaryText, fontSize = (12 * s).sp) }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Fullscreen Interface", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
                Switch(checked = state.isFullscreen, onCheckedChange = { v -> onUpdatePref { it[SqmPreferencesManager.IS_FULLSCREEN] = v } }, colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha=0.3f)))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Polling Interval: ${state.pollingIntervalMs} ms", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
            Slider(value = state.pollingIntervalMs.toFloat(), onValueChange = { v -> onUpdatePref { it[SqmPreferencesManager.POLLING_INTERVAL_MS] = v.toLong() } }, valueRange = 1000f..10000f, steps = 8, colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent))
            
            Text("Global Text Scale", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
            Slider(value = state.textScale, onValueChange = { v -> onUpdatePref { it[SqmPreferencesManager.TEXT_SCALE] = v } }, valueRange = 0.7f..2.0f, colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent))
        }

        DataCard("OBSERVATORY SERVERS", colors, s) {
            Text("Add New Server", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * s).sp)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name", color = colors.secondaryText) }, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = colors.primaryText))
                OutlinedTextField(value = newIp, onValueChange = { newIp = it }, label = { Text("IP Address", color = colors.secondaryText) }, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = colors.primaryText))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newPath, onValueChange = { newPath = it }, label = { Text("Path", color = colors.secondaryText) }, modifier = Modifier.weight(1f), textStyle = TextStyle(color = colors.primaryText))
                    OutlinedTextField(value = newWeb, onValueChange = { newWeb = it }, label = { Text("Web URL", color = colors.secondaryText) }, modifier = Modifier.weight(1f), textStyle = TextStyle(color = colors.primaryText))
                }
            }
            Button(onClick = { onAddServer(newName, newIp, newPath, newWeb); newName=""; newIp=""; newWeb="" }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.surface), border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)) { Text("Add Server", color = colors.primaryText, fontSize = (12 * s).sp) }
            
            Spacer(modifier = Modifier.height(16.dp))
            state.servers.forEach { server ->
                val isActive = server.id == state.activeServerId && !state.isDebugMode
                Card(colors = CardDefaults.cardColors(containerColor = if(isActive) colors.surface else colors.card), border = androidx.compose.foundation.BorderStroke(if(isActive) 2.dp else 1.dp, if(isActive) colors.accent else colors.border), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSwitchServer(server.id) }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(server.name, color = colors.primaryText, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = (14 * s).sp)
                            Text("${server.ips.firstOrNull() ?: ""} | ${server.path}", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * s).sp)
                        }
                        Row {
                            IconButton(onClick = { 
                                serverToEdit = server
                                editName = server.name; editIp = server.ips.firstOrNull() ?: ""; editPath = server.path; editWeb = server.websiteUrl
                            }) { Icon(Icons.Default.Edit, "Edit", tint = colors.primaryText) }
                            if (state.servers.size > 1) IconButton(onClick = { onRemoveServer(server.id) }) { Icon(Icons.Default.Delete, "Delete", tint = colors.offlineRed) }
                        }
                    }
                }
            }
        }

        if (state.devModeUnlocked) {
            DataCard("DEVELOPER TOOLS", colors, s) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Beta Capture UI", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
                    Switch(checked = state.betaCaptureUiEnabled, onCheckedChange = { v -> onUpdatePref { it[SqmPreferencesManager.BETA_CAPTURE_UI] = v } }, colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha=0.3f)))
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("BACKUP & RESTORE DATA", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (11 * s).sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT FILE", color = colors.primaryText, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IMPORT FILE", color = colors.primaryText, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Use mock telemetry to test the UI without hardware.", color = colors.secondaryText, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showDebugDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(if (state.isDebugMode) "MOCK ACTIVE" else "MOCK JSON", color = if(state.isDebugMode) colors.onlineGreen else colors.primaryText, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = onDisableDebug,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.offlineRed)
                    ) {
                        Text("TURN OFF DEV MODE", color = colors.offlineRed, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "SQM V10.0",
            color = colors.secondaryText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.CenterHorizontally).clickable { onDevClick() }.padding(16.dp)
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = colors.surface,
            title = { Text("Export App Data", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select what to export to JSON file:", color = colors.secondaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportOption = ExportOption.ALL }) {
                        RadioButton(selected = exportOption == ExportOption.ALL, onClick = { exportOption = ExportOption.ALL })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All App Data (Servers + All Records)", color = colors.primaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportOption = ExportOption.RECORDS_ONLY }) {
                        RadioButton(selected = exportOption == ExportOption.RECORDS_ONLY, onClick = { exportOption = ExportOption.RECORDS_ONLY })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All Records Only (Photos, Videos, Timelapses)", color = colors.primaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportOption = ExportOption.PHOTOS_ONLY }) {
                        RadioButton(selected = exportOption == ExportOption.PHOTOS_ONLY, onClick = { exportOption = ExportOption.PHOTOS_ONLY })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Photo Records Only", color = colors.primaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    val filename = when (exportOption) {
                        ExportOption.ALL -> "sqm_backup_all_${System.currentTimeMillis()}.json"
                        ExportOption.RECORDS_ONLY -> "sqm_records_all_${System.currentTimeMillis()}.json"
                        ExportOption.PHOTOS_ONLY -> "sqm_records_photos_${System.currentTimeMillis()}.json"
                    }
                    exportLauncher.launch(filename)
                }) {
                    Text("SAVE TO FILE", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("CANCEL", color = colors.secondaryText) }
            }
        )
    }
    
    if (serverToEdit != null) {
        AlertDialog(
            onDismissRequest = { serverToEdit = null }, containerColor = colors.surface,
            title = { Text("Edit Server", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText))
                    OutlinedTextField(value = editIp, onValueChange = { editIp = it }, label = { Text("IP Address", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText))
                    OutlinedTextField(value = editPath, onValueChange = { editPath = it }, label = { Text("Path", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText))
                    OutlinedTextField(value = editWeb, onValueChange = { editWeb = it }, label = { Text("Web URL", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText))
                }
            },
            confirmButton = { TextButton(onClick = { onEditServer(serverToEdit!!.id, editName, editIp, editPath, editWeb); serverToEdit = null }) { Text("Save", color = colors.accent) } },
            dismissButton = { TextButton(onClick = { serverToEdit = null }) { Text("Cancel", color = colors.secondaryText) } }
        )
    }

    if (showDebugDialog) {
        AlertDialog(
            onDismissRequest = { showDebugDialog = false; debugError = false },
            containerColor = colors.surface,
            title = { Text("Mock Telemetry", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    OutlinedTextField(
                        value = debugJsonStr, onValueChange = { debugJsonStr = it; debugError = false },
                        label = { Text("Paste Raw Sensor JSON", color = colors.secondaryText) },
                        textStyle = TextStyle(color = colors.primaryText),
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                    if (debugError) Text("Invalid TSL237 JSON", color = colors.offlineRed, fontSize = 10.sp, modifier = Modifier.padding(top=4.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onEnableDebug(debugJsonStr)) {
                        showDebugDialog = false; debugJsonStr = ""; debugError = false
                    } else {
                        debugError = true
                    }
                }) { Text("START", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDebugDialog = false; debugError = false }) { Text("CANCEL", color = colors.secondaryText) }
            }
        )
    }
}

@Composable
fun DataCard(title: String, colors: AstroColors, scale: Float, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), border = androidx.compose.foundation.BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (12 * scale).sp)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun RegisterRow(label: String, value: String, colors: AstroColors, scale: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (11 * scale).sp)
        Text(value, color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (13 * scale).sp)
    }
}

@Composable
fun KeogramView(
    history: List<TelemetryRecord>,
    isRed: Boolean,
    modifier: Modifier = Modifier,
    currentIndex: Int = -1,
    onFrameClick: ((Int) -> Unit)? = null
) {
    if (history.isEmpty()) return
    Canvas(
        modifier = modifier.pointerInput(history.size) {
            detectTapGestures { offset ->
                if (history.isNotEmpty() && onFrameClick != null) {
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    val idx = (fraction * (history.size - 1)).toInt()
                    onFrameClick(idx)
                }
            }
        }
    ) {
        val total = history.size
        val stripeWidth = size.width / total.toFloat()
        history.forEachIndexed { i, record ->
            val sqm = record.raw.sqm
            val lightness = 1f - ((sqm - 10.0) / 12.0).coerceIn(0.0, 1.0).toFloat()
            val col = if (isRed) Color(lightness, 0f, 0f) else Color(lightness, lightness, lightness)
            drawRect(
                color = col,
                topLeft = Offset(i * stripeWidth, 0f),
                size = androidx.compose.ui.geometry.Size(stripeWidth + 0.5f, size.height)
            )
        }
        if (currentIndex in history.indices) {
            val cx = currentIndex * stripeWidth
            drawLine(Color.Red, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 3f)
        }
    }
}

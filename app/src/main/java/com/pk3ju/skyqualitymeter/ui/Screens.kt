package com.pk3ju.skyqualitymeter.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.pk3ju.skyqualitymeter.data.AstronomyFormulas
import com.pk3ju.skyqualitymeter.data.BortleClassification
import com.pk3ju.skyqualitymeter.data.RecordType
import com.pk3ju.skyqualitymeter.data.SavedObservation
import com.pk3ju.skyqualitymeter.data.ServerProfile
import com.pk3ju.skyqualitymeter.data.SqmPreferencesManager
import com.pk3ju.skyqualitymeter.data.TelemetryRecord
import com.pk3ju.skyqualitymeter.data.formatBortleValue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

enum class HapticType { LIGHT_TAP, MEDIUM_CLICK, HEAVY_PULSE, DOUBLE_PULSE }

fun triggerHaptic(context: Context, enabled: Boolean, type: HapticType = HapticType.MEDIUM_CLICK) {
    if (!enabled) return
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.LIGHT_TAP -> VibrationEffect.createOneShot(18L, 100)
                    HapticType.MEDIUM_CLICK -> VibrationEffect.createOneShot(35L, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticType.HEAVY_PULSE -> VibrationEffect.createOneShot(65L, 255)
                    HapticType.DOUBLE_PULSE -> VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 30), -1)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val duration = when (type) {
                    HapticType.LIGHT_TAP -> 18L
                    HapticType.MEDIUM_CLICK -> 35L
                    HapticType.HEAVY_PULSE -> 65L
                    HapticType.DOUBLE_PULSE -> 80L
                }
                vibrator.vibrate(duration)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "--:--:--"
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestamp))
}

fun formatDateTime(timestamp: Long): String {
    if (timestamp == 0L) return "--"
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
}

fun formatElapsedDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    val hr = (ms / (1000 * 60 * 60))
    return if (hr > 0) String.format(Locale.US, "%02d:%02d:%02d", hr, min, sec) else String.format(Locale.US, "%02d:%02d", min, sec)
}

enum class RecordFilterType { ALL, FAVORITES, PHOTOS, VIDEOS, TIMELAPSES }

@Composable
fun WelcomeScreen(colors: AstroColors, onFinish: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var ips by remember { mutableStateOf("192.168.3.129") }
    var path by remember { mutableStateOf("/json") }
    var website by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bg).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SKY QUALITY METER", color = colors.primaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("Initial System Setup", color = colors.secondaryText, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(32.dp))

        Card(colors = CardDefaults.cardColors(containerColor = colors.surface), border = BorderStroke(1.dp, colors.border), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Observatory Name", color = colors.secondaryText) }, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = colors.primaryText))
                OutlinedTextField(value = ips, onValueChange = { ips = it }, label = { Text("Sensor IP Address(es)", color = colors.secondaryText) }, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = colors.primaryText))
                OutlinedTextField(value = path, onValueChange = { path = it }, label = { Text("Telemetry Endpoint Path", color = colors.secondaryText) }, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = colors.primaryText))
                OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website URL (Optional)", color = colors.secondaryText) }, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = colors.primaryText))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onFinish(name, ips, path, website) },
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("CONNECT & INITIALIZE", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ObservationScreen(
    state: SqmUiState,
    colors: AstroColors,
    onSaveRecord: (String) -> Unit,
    onSwitchServer: (String) -> Unit,
    onRetry: () -> Unit,
    onPageChange: (Int) -> Unit = {},
    onToggleLightMeterPixel: (Boolean) -> Unit = {}
) {
    if (!state.isOnline) {
        OfflineScreen(state, colors, onSwitchServer, onRetry)
        return
    }

    val context = LocalContext.current
    val pageCount = if (state.betaLightMeterUiEnabled) 2 else 1
    val initialP = state.mainTabPageIndex.coerceIn(0, pageCount - 1)
    val pagerState = rememberPagerState(initialPage = initialP, pageCount = { pageCount })

    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
        triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        if (page == 0) {
            AstronomyObservationPage(state, colors, onSaveRecord, showSwipeHint = state.betaLightMeterUiEnabled)
        } else {
            LightMeterObservationPage(state, colors, onSaveRecord, onTogglePixel = onToggleLightMeterPixel)
        }
    }
}

@Composable
fun AstronomyObservationPage(
    state: SqmUiState,
    colors: AstroColors,
    onSaveRecord: (String) -> Unit,
    showSwipeHint: Boolean
) {
    val context = LocalContext.current
    val rec = state.currentRecord
    var showDialog by remember { mutableStateOf(false) }
    var locName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        StarfieldBackground(state.isAstroRedMode, state.isDarkTheme)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    val rawBortle = rec?.raw?.bortle ?: 0.0
                    val bortleDisplay = if (rawBortle > 0) "Class ${formatBortleValue(rawBortle, state.bortleDecimals)}" else bortle?.level ?: "--"
                    val bortleDescDisplay = if (rec?.raw?.bortleDesc?.isNotEmpty() == true) rec.raw.bortleDesc else bortle?.title ?: "Waiting..."
                    Text(text = "Bortle: $bortleDisplay", color = colors.accent, fontFamily = FontFamily.Monospace, fontSize = (18 * state.textScale).sp, fontWeight = FontWeight.Bold)
                    Text(text = bortleDescDisplay, color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (14 * state.textScale).sp)
                }
            }
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                    showDialog = true
                },
                enabled = rec != null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Text("SAVE RECORD", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * state.textScale).sp)
            }

            if (showSwipeHint) {
                Spacer(modifier = Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Swipe Down", tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SWIPE DOWN FOR LIGHT METER", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * state.textScale).sp)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, containerColor = colors.surface,
            title = { Text("Save Record", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = { OutlinedTextField(value = locName, onValueChange = { locName = it }, label = { Text("Location Name", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText)) },
            confirmButton = {
                TextButton(onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.DOUBLE_PULSE)
                    onSaveRecord(locName)
                    showDialog = false
                    locName=""
                }) { Text("Save", color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel", color = colors.secondaryText) } }
        )
    }
}

@Composable
fun LightMeterObservationPage(
    state: SqmUiState,
    colors: AstroColors,
    onSaveRecord: (String) -> Unit,
    onTogglePixel: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val rec = state.currentRecord
    var showDialog by remember { mutableStateOf(false) }
    var locName by remember { mutableStateOf("") }

    val lum = rec?.luminanceCdM2 ?: 0.0
    val lux = AstronomyFormulas.calculateLux(lum)
    val fc = AstronomyFormulas.calculateFootCandles(lux)
    val rating = AstronomyFormulas.getLightRating(lux)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF141418))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Swipe Up", tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SWIPE UP FOR ASTRONOMY SQM", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * state.textScale).sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIGHT METER",
                        color = colors.primaryText,
                        fontSize = (22 * state.textScale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                IconButton(onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                    onTogglePixel(!state.isLightMeterPixelPreview)
                }) {
                    Icon(
                        if (state.isLightMeterPixelPreview) Icons.Default.CenterFocusStrong else Icons.Default.Fullscreen,
                        contentDescription = "Gray Pixel Square",
                        tint = if (state.isLightMeterPixelPreview) colors.accent else colors.secondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "UPDATE TIME: ${formatTime(state.lastUpdateTime)} | IP: ${state.currentActiveIp}",
                color = colors.secondaryText,
                fontFamily = FontFamily.Monospace,
                fontSize = (10 * state.textScale).sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            val luxFormatted = if (lux < 0.001) String.format(Locale.US, "%.5f", lux) else if (lux < 10.0) String.format(Locale.US, "%.3f", lux) else String.format(Locale.US, "%.1f", lux)
            
            if (state.isLightMeterPixelPreview) {
                val sqmL = ((lum / 10.0).coerceIn(0.08, 0.92)).toFloat()
                val grayCol = Color(sqmL, sqmL, sqmL)
                val textCol = if (sqmL > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(grayCol, RoundedCornerShape(16.dp))
                        .border(2.dp, colors.border, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(luxFormatted, color = textCol, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("LUX", color = textCol, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("${String.format(Locale.US, "%.3f", lum)} cd/m²", color = textCol.copy(alpha=0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedContent(targetState = luxFormatted, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "luxFade") { luxVal ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = luxVal,
                        color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (68 * state.textScale).sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "LUX (lx)  |  ${String.format(Locale.US, "%.3f", fc)} fc",
                        color = colors.accent, fontFamily = FontFamily.Monospace, fontSize = (15 * state.textScale).sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = BorderStroke(1.dp, colors.accent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = rating.category.uppercase(),
                        color = colors.accent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (11 * state.textScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rating.rating,
                        color = colors.primaryText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (16 * state.textScale).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = rating.recommendation,
                        color = colors.secondaryText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (12 * state.textScale).sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SENSOR MEASUREMENTS", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * state.textScale).sp, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = colors.border)
                    RegisterRow("Luminance", "${String.format(Locale.US, "%.5f", lum)} cd/m²", colors, state.textScale)
                    RegisterRow("Illuminance", "${String.format(Locale.US, "%.4f", lux)} Lux (${String.format(Locale.US, "%.4f", fc)} fc)", colors, state.textScale)
                    RegisterRow("Frequency", "${String.format(Locale.US, "%.2f", rec?.raw?.frequencyHz ?: 0.0)} Hz", colors, state.textScale)
                    RegisterRow("Period", "${String.format(Locale.US, "%.3f", rec?.raw?.periodMs ?: 0.0)} ms", colors, state.textScale)
                    val rawBortle = rec?.raw?.bortle ?: 0.0
                    RegisterRow("Bortle Equivalent", if (rawBortle > 0) "Class ${formatBortleValue(rawBortle, state.bortleDecimals)}" else rec?.bortleClass?.level ?: "--", colors, state.textScale)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                    showDialog = true
                },
                enabled = rec != null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Text("SAVE LIGHT RECORD", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * state.textScale).sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, containerColor = colors.surface,
            title = { Text("Save Light Record", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = { OutlinedTextField(value = locName, onValueChange = { locName = it }, label = { Text("Location / Task Name", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText)) },
            confirmButton = {
                TextButton(onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.DOUBLE_PULSE)
                    onSaveRecord(locName)
                    showDialog = false
                    locName=""
                }) { Text("Save", color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel", color = colors.secondaryText) } }
        )
    }
}

@Composable
fun OfflineScreen(state: SqmUiState, colors: AstroColors, onSwitchServer: (String) -> Unit, onRetry: () -> Unit) {
    val context = LocalContext.current
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
                        onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                            onSwitchServer(s.id)
                        },
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
            onClick = {
                triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                onRetry()
            },
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
    
    val rawBortle = rec?.raw?.bortle ?: 0.0
    val bortleStr = if (rawBortle > 0) "Class ${formatBortleValue(rawBortle, state.bortleDecimals)}" else "${rec?.bortleClass?.level ?: "--"}"

    val metrics = listOf(
        Pair("SQM", "${rec?.raw?.sqm ?: "--"}"),
        Pair("Bortle", bortleStr),
        Pair("Bortle Desc", if (rec?.raw?.bortleDesc?.isNotEmpty() == true) rec.raw.bortleDesc else "${rec?.bortleClass?.title ?: "--"}"),
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
                Card(colors = CardDefaults.cardColors(containerColor = colors.card), border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(metric.first, color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * s).sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(metric.second, color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (12 * s).sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RAW JSON PAYLOAD", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (12 * s).sp)
                    IconButton(
                        onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(state.currentRawJson))
                            Toast.makeText(context, "JSON Copied", Toast.LENGTH_SHORT).show()
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
    onStartRecord: (Context) -> Unit,
    onStopRecord: (Context) -> List<TelemetryRecord>,
    onSave: (String, List<TelemetryRecord>, RecordType) -> Unit,
    onToggleGraphs: (Boolean) -> Unit = {},
    onToggleKeogram: (Boolean) -> Unit = {},
    onToggleNightVisionSquare: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var locName by remember { mutableStateOf("") }
    var recordedFrames by remember { mutableStateOf(listOf<TelemetryRecord>()) }
    var showTimelapseDlg by remember { mutableStateOf(false) }
    var customTimelapseInput by remember { mutableStateOf(state.timelapseIntervalSec.toString()) }

    val showGraphs = state.showCaptureGraphs
    val showLiveKeogram = state.showCaptureKeogram
    val isNightVisionSquare = state.isNightVisionSquare

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // UPPER SCROLLABLE CONTENT (Tools, Graphs, Keogram, & Telemetry Labels)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        IconButton(onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                            onToggleGraphs(!showGraphs)
                        }) {
                            Icon(if(showGraphs) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle Graphs", tint = contentCol)
                        }
                        IconButton(onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                            onToggleKeogram(!showLiveKeogram)
                        }) {
                            Icon(Icons.Default.ViewWeek, "Live Keogram", tint = if(showLiveKeogram) colors.accent else contentCol)
                        }
                        IconButton(onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                            onToggleNightVisionSquare(!isNightVisionSquare)
                        }) {
                            Icon(if(isNightVisionSquare) Icons.Default.CenterFocusStrong else Icons.Default.Fullscreen, "Night Vision Pixel", tint = if(isNightVisionSquare) colors.accent else contentCol)
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
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Switch(
                            checked = state.autoNameCapture,
                            onCheckedChange = {
                                triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                                onToggleAutoName(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = contentCol,
                                checkedTrackColor = contentCol.copy(alpha = 0.3f),
                                uncheckedThumbColor = contentCol.copy(alpha = 0.5f),
                                uncheckedTrackColor = Color.Black.copy(alpha = 0.2f)
                            )
                        )

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

                    Column(horizontalAlignment = Alignment.End) {
                        Text(String.format(Locale.US, "%.2f MPSAS", currentSqm), color = contentCol, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(String.format(Locale.US, "%.5f cd/m² (Lux)", state.currentRecord?.luminanceCdM2 ?: 0.0), color = contentCol.copy(alpha=0.8f), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        val rawBortle = state.currentRecord?.raw?.bortle ?: 0.0
                        val bortleDisplay = if (rawBortle > 0) "Class ${formatBortleValue(rawBortle, state.bortleDecimals)}" else state.currentRecord?.bortleClass?.level ?: "--"
                        val bortleDescDisplay = if (state.currentRecord?.raw?.bortleDesc?.isNotEmpty() == true) state.currentRecord.raw.bortleDesc else state.currentRecord?.bortleClass?.title ?: ""
                        Text(bortleDisplay, color = contentCol, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(bortleDescDisplay, color = contentCol.copy(alpha=0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // BOTTOM CONTROL SECTION: Pinned Cleanly at Bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isRecording) {
                    Text("REC ${formatElapsedDuration(elapsedMs)} | Frames: ${state.recordingFramesCount}", color = Color.Red, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                }
                
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
                                        .clickable {
                                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                                            if (!state.isRecording) onSetVideoInterval(ms)
                                        }
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
                                        triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                                        if (!state.isRecording) {
                                            customTimelapseInput = state.timelapseIntervalSec.toString()
                                            showTimelapseDlg = true
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

                Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(16.dp)).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(RecordType.TIMELAPSE, RecordType.PHOTO, RecordType.VIDEO).forEach { mode ->
                        val isSelected = state.currentCaptureMode == mode
                        Box(
                            modifier = Modifier.background(if (isSelected) contentCol else Color.Transparent, RoundedCornerShape(16.dp))
                                               .clickable {
                                                   triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                                                   if(!state.isRecording) onModeSelect(mode)
                                               }
                                               .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(mode.name, color = if(isSelected) bgCol else contentCol.copy(alpha=0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color.Transparent, CircleShape)
                        .border(4.dp, contentCol, CircleShape)
                        .clickable {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.DOUBLE_PULSE)
                            if (state.isRecording) {
                                recordedFrames = onStopRecord(context)
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
                                    RecordType.VIDEO -> { onStartRecord(context) }
                                    RecordType.TIMELAPSE -> { onStartRecord(context) }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(56.dp).background(if (state.isRecording) Color.Red else contentCol, CircleShape))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    
    if (showTimelapseDlg) {
        AlertDialog(
            onDismissRequest = { showTimelapseDlg = false }, containerColor = colors.surface,
            title = { Text("Timelapse Interval", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = { OutlinedTextField(value = customTimelapseInput, onValueChange = { customTimelapseInput = it }, label = { Text("Seconds between frames", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText)) },
            confirmButton = {
                TextButton(onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                    val s = (customTimelapseInput.toLongOrNull() ?: 5L).coerceAtLeast(1L)
                    onSetTimelapseInterval(s)
                    showTimelapseDlg = false
                }) { Text("Set", color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { showTimelapseDlg = false }) { Text("Cancel", color = colors.secondaryText) } }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, containerColor = colors.surface,
            title = { Text("Save ${state.currentCaptureMode.name}", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = { OutlinedTextField(value = locName, onValueChange = { locName = it }, label = { Text("Location Name", color = colors.secondaryText) }, textStyle = TextStyle(color = colors.primaryText)) },
            confirmButton = {
                TextButton(onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.DOUBLE_PULSE)
                    onSave(locName, recordedFrames, state.currentCaptureMode)
                    showDialog = false
                    locName=""
                }) { Text("Save", color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel", color = colors.secondaryText) } }
        )
    }
}

@Composable
fun DataScreen(state: SqmUiState, colors: AstroColors, onExportCsv: (Context, Uri) -> Unit) {
    val s = state.textScale
    val context = LocalContext.current
    val astroData = AstronomyFormulas.calculateAstroData()
    
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            onExportCsv(context, uri)
            Toast.makeText(context, "Exported telemetry to CSV", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // TOP SECTION: ASTRONOMICAL DATA
        Text("ASTRONOMICAL DATA", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (16 * s).sp)

        // Moon Phase & Illumination Main Banner Card
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.accent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(astroData.moonPhaseEmoji, fontSize = (36 * s).sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = astroData.moonPhaseName.uppercase(),
                                color = colors.primaryText,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = (18 * s).sp
                            )
                            Text(
                                text = "LUNAR PHASE",
                                color = colors.secondaryText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (11 * s).sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${astroData.moonIlluminationPct}%",
                            color = colors.accent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = (36 * s).sp
                        )
                        Text(
                            text = "ILLUMINATED",
                            color = colors.secondaryText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (10 * s).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colors.border)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DARK OBSERVING WINDOW", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
                    Text(
                        astroData.nightDurationHours,
                        color = colors.onlineGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = (15 * s).sp
                    )
                }
            }
        }

        // 2x2 Grid of Big Event Cards (Astro Dawn, Astro Dusk, Moonrise, Moonset)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AstroTimeCard(
                title = "ASTRO DAWN",
                timeStr = astroData.astroDawnTime,
                subtitle = "Morning Twilight End",
                colors = colors,
                scale = s,
                modifier = Modifier.weight(1f)
            )
            AstroTimeCard(
                title = "ASTRO DUSK",
                timeStr = astroData.astroDuskTime,
                subtitle = "Evening Twilight Start",
                colors = colors,
                scale = s,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AstroTimeCard(
                title = "MOONRISE",
                timeStr = astroData.moonriseTime,
                subtitle = "Moon Rise Time",
                colors = colors,
                scale = s,
                modifier = Modifier.weight(1f)
            )
            AstroTimeCard(
                title = "MOONSET",
                timeStr = astroData.moonsetTime,
                subtitle = "Moon Set Time",
                colors = colors,
                scale = s,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // BOTTOM SECTION: LIVE TELEMETRY GRAPHS (Positioned beneath Astro Data)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LIVE TELEMETRY", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (16 * s).sp)
            Button(
                onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                    csvLauncher.launch("sqm_telemetry_${System.currentTimeMillis()}.csv")
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("EXPORT TELEMETRY CSV", color = colors.primaryText, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
            }
        }

        DataCard("SKY QUALITY (MPSAS)", colors, s) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                SingleLineChart(
                    data = state.history.map { Pair(it.timestamp, it.raw.sqm.toFloat()) },
                    lineColor = colors.accent, gridColor = colors.border, textColor = colors.secondaryText, scale = s
                )
            }
        }

        DataCard("SENSOR FREQUENCY (Hz)", colors, s) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                SingleLineChart(
                    data = state.history.map { Pair(it.timestamp, it.raw.frequencyHz.toFloat()) },
                    lineColor = colors.onlineGreen, gridColor = colors.border, textColor = colors.secondaryText, scale = s
                )
            }
        }
    }
}

@Composable
fun AstroTimeCard(
    title: String,
    timeStr: String,
    subtitle: String,
    colors: AstroColors,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.border),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = colors.secondaryText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = (11 * scale).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = timeStr,
                color = colors.primaryText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = (32 * scale).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = colors.secondaryText,
                fontFamily = FontFamily.Monospace,
                fontSize = (10 * scale).sp
            )
        }
    }
}

@Composable
fun SingleLineChart(data: List<Pair<Long, Float>>, lineColor: Color, gridColor: Color, textColor: Color, scale: Float) {
    if (data.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxSize()) {
        val leftPad = 80f * scale
        val bottomPad = 40f * scale
        val chartW = size.width - leftPad
        val chartH = size.height - bottomPad

        val minY = data.minOf { it.second }
        val maxY = data.maxOf { it.second }.let { if (it == minY) minY + 1f else it }

        for (i in 0..3) {
            val y = chartH - (i * (chartH / 3f))
            drawLine(gridColor, Offset(leftPad, y), Offset(size.width, y), strokeWidth = 1f)
            val valY = minY + (i * ((maxY - minY) / 3f))
            drawText(textMeasurer, String.format(Locale.US, "%.1f", valY), Offset(0f, y - 10f), style = TextStyle(color = textColor, fontSize = (9 * scale).sp, fontFamily = FontFamily.Monospace))
        }

        val path = Path()
        val stepX = chartW / maxOf(1, data.size - 1)
        data.forEachIndexed { i, pair ->
            val x = leftPad + (i * stepX)
            val y = chartH - ((pair.second - minY) / (maxY - minY) * chartH)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3f * scale))
    }
}

@Composable
fun MultiLineChart(data1: List<Pair<Long, Float>>, color1: Color, data2: List<Pair<Long, Float>>, color2: Color, gridColor: Color, textColor: Color, scale: Float) {
    if (data1.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxSize()) {
        val leftPad = 80f * scale
        val rightPad = 120f * scale
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height

        val minY1 = data1.minOf { it.second }; val maxY1 = data1.maxOf { it.second }.let { if (it == minY1) minY1 + 1f else it }
        val minY2 = if (data2.isNotEmpty()) data2.minOf { it.second } else 0f
        val maxY2 = if (data2.isNotEmpty()) data2.maxOf { it.second }.let { if (it == minY2) minY2 + 1f else it } else 1f

        val stepX = chartW / maxOf(1, data1.size - 1)

        val path1 = Path()
        data1.forEachIndexed { i, pair ->
            val x = leftPad + (i * stepX)
            val y = chartH - ((pair.second - minY1) / (maxY1 - minY1) * chartH)
            if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
        }
        drawPath(path1, color1, style = Stroke(width = 2.5f * scale))

        if (data2.isNotEmpty()) {
            val path2 = Path()
            data2.forEachIndexed { i, pair ->
                val x = leftPad + (i * stepX)
                val y = chartH - ((pair.second - minY2) / (maxY2 - minY2) * chartH)
                if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
            }
            drawPath(path2, color2, style = Stroke(width = 2.5f * scale))
        }

        drawText(textMeasurer, String.format(Locale.US, "%.1f", maxY1), Offset(0f, 0f), style = TextStyle(color = color1, fontSize = (9 * scale).sp, fontFamily = FontFamily.Monospace))
        drawText(textMeasurer, String.format(Locale.US, "%.1f", minY1), Offset(0f, chartH - 15f), style = TextStyle(color = color1, fontSize = (9 * scale).sp, fontFamily = FontFamily.Monospace))

        if (data2.isNotEmpty()) {
            drawText(textMeasurer, String.format(Locale.US, "%.0f Hz", maxY2), Offset(size.width - rightPad + 10f, 0f), style = TextStyle(color = color2, fontSize = (9 * scale).sp, fontFamily = FontFamily.Monospace))
            drawText(textMeasurer, String.format(Locale.US, "%.0f Hz", minY2), Offset(size.width - rightPad + 10f, chartH - 15f), style = TextStyle(color = color2, fontSize = (9 * scale).sp, fontFamily = FontFamily.Monospace))
        }
    }
}

@Composable
fun RecordsScreen(
    state: SqmUiState,
    colors: AstroColors,
    onDelete: (String) -> Unit,
    onOpenReport: (SavedObservation) -> Unit,
    onImport: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit,
    onRenameObservation: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val s = state.textScale
    var query by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(RecordFilterType.ALL) }
    var obsToDelete by remember { mutableStateOf<SavedObservation?>(null) }
    var obsToRename by remember { mutableStateOf<SavedObservation?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val filteredObservations = state.savedObservations.filter { obs ->
        val matchesQuery = query.isBlank() ||
                obs.locationName.contains(query, ignoreCase = true) ||
                obs.bortleLevel.contains(query, ignoreCase = true) ||
                obs.raw.bortleDesc.contains(query, ignoreCase = true)

        val matchesFilter = when (filterType) {
            RecordFilterType.ALL -> true
            RecordFilterType.FAVORITES -> obs.isFavorite
            RecordFilterType.PHOTOS -> obs.type == RecordType.PHOTO
            RecordFilterType.VIDEOS -> obs.type == RecordType.VIDEO
            RecordFilterType.TIMELAPSES -> obs.type == RecordType.TIMELAPSE
        }

        matchesQuery && matchesFilter
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("OBSERVATION RECORDS (${filteredObservations.size})", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (14 * s).sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.secondaryText) },
                label = { Text("Search location, Bortle, mode...", color = colors.secondaryText, fontSize = (11 * s).sp) },
                modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = colors.primaryText)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RecordFilterType.entries.toTypedArray()) { type ->
                    val isSelected = filterType == type
                    Button(
                        onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                            filterType = type
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) colors.accent else colors.card),
                        border = BorderStroke(1.dp, colors.border),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            type.name,
                            color = if (isSelected) Color.White else colors.primaryText,
                            fontSize = (10 * s).sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        items(filteredObservations, key = { it.id }) { obs ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.card), border = BorderStroke(1.dp, if (obs.isFavorite) colors.accent else colors.border),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().clickable {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                    onOpenReport(obs)
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            val icon = when(obs.type) { RecordType.VIDEO -> Icons.Default.Videocam; RecordType.TIMELAPSE -> Icons.Default.Timer; else -> Icons.Default.Image }
                            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(obs.locationName.uppercase(), color = colors.primaryText, fontSize = (14 * s).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                val rawBortle = obs.raw.bortle
                                val bortleDisplay = if (rawBortle > 0) "Class ${formatBortleValue(rawBortle, state.bortleDecimals)}" else obs.bortleLevel
                                Text("${String.format(Locale.US, "%.2f", obs.raw.sqm)} MPSAS - $bortleDisplay", color = colors.accent, fontSize = (12 * s).sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                                renameInput = obs.locationName
                                obsToRename = obs
                            }) {
                                Icon(Icons.Default.Edit, "Rename", tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                                onToggleFavorite(obs.id)
                            }) {
                                Icon(
                                    if (obs.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (obs.isFavorite) Color(0xFFFACC15) else colors.secondaryText
                                )
                            }
                            IconButton(onClick = {
                                triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                                obsToDelete = obs
                            }) { Icon(Icons.Default.Delete, "Delete", tint = colors.offlineRed) }
                        }
                    }
                }
            }
        }
    }

    if (obsToRename != null) {
        AlertDialog(
            onDismissRequest = { obsToRename = null },
            containerColor = colors.surface,
            title = { Text("Rename Record", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New Name", color = colors.secondaryText) },
                    textStyle = TextStyle(color = colors.primaryText)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.DOUBLE_PULSE)
                    onRenameObservation(obsToRename!!.id, renameInput)
                    obsToRename = null
                }) {
                    Text("RENAME", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { obsToRename = null }) { Text("CANCEL", color = colors.secondaryText) }
            }
        )
    }

    if (obsToDelete != null) {
        AlertDialog(
            onDismissRequest = { obsToDelete = null },
            containerColor = colors.surface,
            title = { Text("Confirm Deletion", color = colors.primaryText, fontFamily = FontFamily.Monospace) },
            text = { Text("Permanently delete '${obsToDelete!!.locationName}'?", color = colors.secondaryText, fontFamily = FontFamily.Monospace) },
            confirmButton = {
                TextButton(onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.HEAVY_PULSE)
                    onDelete(obsToDelete!!.id)
                    obsToDelete = null
                }) {
                    Text("DELETE", color = colors.offlineRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { obsToDelete = null }) { Text("CANCEL", color = colors.secondaryText) }
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
    onExportCsv: (Context, Uri, SavedObservation) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val context = LocalContext.current
    var isImmersive by remember { mutableStateOf(false) }
    var frameIndex by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var playSpeedFps by remember { mutableIntStateOf(10) }
    var showFpsMenu by remember { mutableStateOf(false) }
    var showImmersiveChart by remember { mutableStateOf(true) }
    var showStandardChart by remember { mutableStateOf(true) }
    var showFullscreenKeogram by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            onExportCsv(context, uri, obs)
            Toast.makeText(context, "Exported session CSV", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isPlaying, obs.history.size, playSpeedFps) {
        if (isPlaying && obs.history.isNotEmpty()) {
            val intervalMs = (1000L / playSpeedFps).coerceAtLeast(16L)
            while (isPlaying) {
                delay(intervalMs)
                frameIndex = (frameIndex + 1f) % obs.history.size
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
    val rawBortleVal = currentData.raw.bortle
    val bortleLabel = if (rawBortleVal > 0) "Class ${formatBortleValue(rawBortleVal, -1)}" else currentData.bortleClass.level

    if (isImmersive) {
        val lightness = 1f - ((currentData.raw.sqm - 10.0) / 12.0).coerceIn(0.0, 1.0).toFloat()
        val bgCol = if (isRed) Color(lightness, 0f, 0f) else Color(lightness, lightness, lightness)
        val contentCol = if(lightness > 0.6f) Color(0xFF111827) else Color.White

        Dialog(onDismissRequest = { isImmersive = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(modifier = Modifier.fillMaxSize().background(bgCol)) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (obs.history.isNotEmpty()) Arrangement.SpaceBetween else Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        if (obs.history.isNotEmpty()) {
                            IconButton(onClick = {
                                triggerHaptic(context, true, HapticType.LIGHT_TAP)
                                showImmersiveChart = !showImmersiveChart
                            }) {
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
                                            Text("${playSpeedFps} FPS", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                        DropdownMenu(expanded = showFpsMenu, onDismissRequest = { showFpsMenu = false }) {
                                            listOf(1, 5, 10, 24, 30, 60).forEach { fps ->
                                                DropdownMenuItem(
                                                    text = { Text("$fps FPS", fontFamily = FontFamily.Monospace) },
                                                    onClick = { playSpeedFps = fps; showFpsMenu = false }
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            
                            Slider(value = frameIndex, onValueChange = { frameIndex = it; isPlaying = false }, valueRange = 0f..obs.history.lastIndex.toFloat())
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Frame ${currentIndex + 1}/${obs.history.size}", color = contentCol, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Row {
                                    IconButton(onClick = { isPlaying = !isPlaying }) {
                                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = contentCol)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            containerColor = colors.bg,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(obs.locationName.uppercase(), color = colors.primaryText, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                        Text(obs.type.name, color = colors.accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onToggleFavorite(obs.id) }) {
                            Icon(
                                if (obs.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (obs.isFavorite) Color(0xFFFACC15) else colors.secondaryText
                            )
                        }
                        IconButton(onClick = { isImmersive = true }) { Icon(Icons.Default.Fullscreen, "Fullscreen", tint = colors.primaryText) }
                        IconButton(onClick = onShareJson) { Icon(Icons.Default.Share, "Share", tint = colors.primaryText) }
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close", tint = colors.primaryText) }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RECORD DETAILS", color = colors.primaryText, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showKeogram = !showKeogram }) {
                                    Icon(Icons.Default.ViewWeek, contentDescription = "Keogram", tint = if (showKeogram) colors.accent else colors.secondaryText)
                                }
                                if (showKeogram) {
                                    IconButton(onClick = { showFullscreenKeogram = true }) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen Keogram", tint = colors.accent)
                                    }
                                }
                            }
                        }
                        
                        if (showKeogram) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(70.dp).background(colors.card, RoundedCornerShape(8.dp)).border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(4.dp)) {
                                KeogramView(history = obs.history, isRed = isRed, selectedIndex = currentIndex, onSelectIndex = { frameIndex = it.toFloat() }, modifier = Modifier.fillMaxSize())
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(colors.card, RoundedCornerShape(8.dp)).border(1.dp, colors.border, RoundedCornerShape(8.dp)).padding(8.dp)) {
                            MultiLineChart(
                                data1 = obs.history.map { Pair(it.timestamp, it.raw.sqm.toFloat()) }, color1 = colors.accent,
                                data2 = obs.history.map { Pair(it.timestamp, it.raw.frequencyHz.toFloat()) }, color2 = colors.onlineGreen,
                                gridColor = colors.border, textColor = colors.secondaryText, scale = 0.9f
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

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(colors = CardDefaults.cardColors(containerColor = colors.surface), border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (obs.type == RecordType.TIMELAPSE) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("TIMELAPSE SPEED", color = colors.secondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Box {
                                        TextButton(onClick = { showFpsMenu = true }) {
                                            Icon(Icons.Default.Speed, contentDescription = "FPS", tint = colors.accent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${playSpeedFps} FPS", color = colors.primaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                        DropdownMenu(expanded = showFpsMenu, onDismissRequest = { showFpsMenu = false }) {
                                            listOf(1, 5, 10, 24, 30, 60).forEach { fps ->
                                                DropdownMenuItem(
                                                    text = { Text("$fps FPS", fontFamily = FontFamily.Monospace) },
                                                    onClick = { playSpeedFps = fps; showFpsMenu = false }
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            
                            Slider(value = frameIndex, onValueChange = { frameIndex = it; isPlaying = false }, valueRange = 0f..obs.history.lastIndex.toFloat())
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Frame ${currentIndex + 1}/${obs.history.size}", color = colors.primaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Row {
                                    IconButton(onClick = { isPlaying = !isPlaying }) {
                                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = colors.accent)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                DataCard("REGISTER VALUES", colors, 1.0f) {
                    RegisterRow("Luminance", String.format(Locale.US, "%.5f cd/m²", currentData.luminanceCdM2), colors, 1.0f)
                    RegisterRow("NELM", String.format(Locale.US, "%.2f mag", currentData.nelm), colors, 1.0f)
                    RegisterRow("Frequency", "${currentData.raw.frequencyHz} Hz", colors, 1.0f)
                    RegisterRow("Period", "${currentData.raw.periodMs} ms", colors, 1.0f)
                    RegisterRow("Brightness", "${currentData.raw.brightnessMcd} mcd/m²", colors, 1.0f)
                    RegisterRow("Bortle Level", bortleLabel, colors, 1.0f)
                    RegisterRow("Bortle Description", bortleTitle, colors, 1.0f)
                }

                if (obs.history.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { csvLauncher.launch("${obs.locationName.lowercase().replace(" ", "_")}_session.csv") },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXPORT SESSION CSV", color = colors.primaryText, fontFamily = FontFamily.Monospace)
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showFullscreenKeogram) {
        Dialog(onDismissRequest = { showFullscreenKeogram = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.systemBars), contentAlignment = Alignment.Center) {
                KeogramView(
                    history = obs.history,
                    isRed = isRed,
                    selectedIndex = currentIndex,
                    showSelectionLine = false,
                    onSelectIndex = { frameIndex = it.toFloat() },
                    modifier = Modifier.fillMaxSize()
                )
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
    onSetBortleDecimals: (Int) -> Unit,
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
                    TextButton(onClick = {
                        triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                        onUpdatePref { it[SqmPreferencesManager.IS_DARK_THEME] = true }
                    }) { Text("DARK", color = if(state.isDarkTheme) colors.accent else colors.secondaryText, fontSize = (12 * s).sp) }
                    TextButton(onClick = {
                        triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                        onUpdatePref { it[SqmPreferencesManager.IS_DARK_THEME] = false }
                    }) { Text("LIGHT", color = if(!state.isDarkTheme) colors.accent else colors.secondaryText, fontSize = (12 * s).sp) }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Fullscreen Interface", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
                Switch(checked = state.isFullscreen, onCheckedChange = { v ->
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                    onUpdatePref { it[SqmPreferencesManager.IS_FULLSCREEN] = v }
                }, colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha=0.3f)))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Polling Interval: ${state.pollingIntervalMs} ms", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
            Slider(value = state.pollingIntervalMs.toFloat(), onValueChange = { v -> onUpdatePref { it[SqmPreferencesManager.POLLING_INTERVAL_MS] = v.toLong() } }, valueRange = 1000f..10000f, steps = 8, colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent))
            
            Text("Global Text Scale", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
            Slider(value = state.textScale, onValueChange = { v -> onUpdatePref { it[SqmPreferencesManager.TEXT_SCALE] = v } }, valueRange = 0.7f..2.0f, colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent))

            Button(
                onClick = {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                    onUpdatePref { it[SqmPreferencesManager.TEXT_SCALE] = 1.0f }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            ) {
                Text("RESET TEXT SCALE TO DEFAULT (1.0x)", color = colors.primaryText, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Bortle Decimal Precision", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val options = listOf(-1 to "MAX (JSON)", 0 to "0 DEC", 1 to "1 DEC", 2 to "2 DEC", 3 to "3 DEC")
                options.forEach { (valDec, label) ->
                    val isSel = state.bortleDecimals == valDec
                    TextButton(
                        onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                            onSetBortleDecimals(valDec)
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            label,
                            color = if (isSel) colors.accent else colors.secondaryText,
                            fontSize = (10 * s).sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
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
            Button(onClick = {
                triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                onAddServer(newName, newIp, newPath, newWeb)
                newName=""; newIp=""; newWeb=""
            }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.surface), border = BorderStroke(1.dp, colors.border)) { Text("Add Server", color = colors.primaryText, fontSize = (12 * s).sp) }
            
            Spacer(modifier = Modifier.height(16.dp))
            state.servers.forEach { server ->
                val isActive = server.id == state.activeServerId && !state.isDebugMode
                Card(colors = CardDefaults.cardColors(containerColor = if(isActive) colors.surface else colors.card), border = BorderStroke(if(isActive) 2.dp else 1.dp, if(isActive) colors.accent else colors.border), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                    onSwitchServer(server.id)
                }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(server.name, color = colors.primaryText, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = (14 * s).sp)
                            Text("${server.ips.firstOrNull() ?: ""} | ${server.path}", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (10 * s).sp)
                        }
                        Row {
                            IconButton(onClick = { 
                                triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                                serverToEdit = server
                                editName = server.name; editIp = server.ips.firstOrNull() ?: ""; editPath = server.path; editWeb = server.websiteUrl
                            }) { Icon(Icons.Default.Edit, "Edit", tint = colors.primaryText) }
                            if (state.servers.size > 1) IconButton(onClick = {
                                triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                                onRemoveServer(server.id)
                            }) { Icon(Icons.Default.Delete, "Delete", tint = colors.offlineRed) }
                        }
                    }
                }
            }
        }

        if (state.devModeUnlocked) {
            DataCard("DEVELOPER TOOLS", colors, s) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Beta Capture UI", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
                    Switch(checked = state.betaCaptureUiEnabled, onCheckedChange = { v ->
                        triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                        onUpdatePref { it[SqmPreferencesManager.BETA_CAPTURE_UI] = v }
                    }, colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha=0.3f)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Light Meter UI", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
                    Switch(checked = state.betaLightMeterUiEnabled, onCheckedChange = { v ->
                        triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                        onUpdatePref { it[SqmPreferencesManager.BETA_LIGHT_METER_UI] = v }
                    }, colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha=0.3f)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Service Capture", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
                        Text("Keep recording active when app is minimized/closed", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (9 * s).sp)
                    }
                    Switch(checked = state.backgroundRecordingEnabled, onCheckedChange = { v ->
                        triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.LIGHT_TAP)
                        onUpdatePref { it[SqmPreferencesManager.BACKGROUND_RECORDING_ENABLED] = v }
                    }, colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha=0.3f)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vibration Feedback", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * s).sp)
                        Text("Haptic feedback on clicks and recording actions", color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (9 * s).sp)
                    }
                    Switch(checked = state.vibrationFeedbackEnabled, onCheckedChange = { v ->
                        triggerHaptic(context, true, HapticType.LIGHT_TAP)
                        onUpdatePref { it[SqmPreferencesManager.VIBRATION_FEEDBACK_ENABLED] = v }
                    }, colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha=0.3f)))
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("BACKUP & RESTORE DATA", color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (11 * s).sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                            showExportDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT FILE", color = colors.primaryText, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                            importLauncher.launch(arrayOf("application/json", "text/*"))
                        },
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
                        onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                            showDebugDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(if (state.isDebugMode) "MOCK ACTIVE" else "MOCK JSON", color = if(state.isDebugMode) colors.onlineGreen else colors.primaryText, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = {
                            triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                            onDisableDebug()
                        },
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
            "SQM V12.8",
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
                        Text("All App Data (Settings + Servers + All Records)", color = colors.primaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
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
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.DOUBLE_PULSE)
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
            confirmButton = { TextButton(onClick = {
                triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
                onEditServer(serverToEdit!!.id, editName, editIp, editPath, editWeb)
                serverToEdit = null
            }) { Text("Save", color = colors.accent) } },
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
                    triggerHaptic(context, state.vibrationFeedbackEnabled, HapticType.MEDIUM_CLICK)
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
fun DataCard(title: String, colors: AstroColors, scale: Float, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = colors.surface), border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (11 * scale).sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun RegisterRow(label: String, value: String, colors: AstroColors, scale: Float) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.secondaryText, fontFamily = FontFamily.Monospace, fontSize = (12 * scale).sp)
        Text(value, color = colors.primaryText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (12 * scale).sp)
    }
}

@Composable
fun KeogramView(
    history: List<TelemetryRecord>,
    isRed: Boolean,
    modifier: Modifier = Modifier,
    selectedIndex: Int = -1,
    showSelectionLine: Boolean = true,
    onSelectIndex: ((Int) -> Unit)? = null
) {
    if (history.isEmpty()) return
    Canvas(
        modifier = modifier
            .pointerInput(history, onSelectIndex) {
                if (onSelectIndex != null) {
                    detectTapGestures { offset ->
                        val idx = ((offset.x / size.width) * history.size).toInt().coerceIn(0, history.lastIndex)
                        onSelectIndex(idx)
                    }
                }
            }
    ) {
        val colWidth = size.width / history.size.toFloat()
        history.forEachIndexed { i, record ->
            val sqm = record.raw.sqm
            val lightness = 1f - ((sqm - 10.0) / 12.0).coerceIn(0.0, 1.0).toFloat()
            val color = if (isRed) Color(lightness, 0f, 0f) else Color(lightness, lightness, lightness)
            drawRect(
                color = color,
                topLeft = Offset(i * colWidth, 0f),
                size = androidx.compose.ui.geometry.Size(colWidth + 1f, size.height)
            )
        }

        if (showSelectionLine && selectedIndex in history.indices) {
            val selX = selectedIndex * colWidth + (colWidth / 2f)
            drawLine(
                color = Color.Red,
                start = Offset(selX, 0f),
                end = Offset(selX, size.height),
                strokeWidth = 3f
            )
        }
    }
}

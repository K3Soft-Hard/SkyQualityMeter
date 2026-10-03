package com.pk3ju.skyqualitymeter.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pk3ju.skyqualitymeter.data.*
import com.pk3ju.skyqualitymeter.network.SqmNetworkClient
import com.pk3ju.skyqualitymeter.service.SqmCaptureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

enum class ExportOption { ALL, RECORDS_ONLY, PHOTOS_ONLY }

data class SqmUiState(
    val isFirstLaunch: Boolean = true,
    val servers: List<ServerProfile> = emptyList(),
    val activeServerId: String = "default",
    val textScale: Float = 1.0f,
    val isDarkTheme: Boolean = true,
    val isFullscreen: Boolean = false,
    val isAstroRedMode: Boolean = false,
    val pollingIntervalMs: Long = 3000L,
    
    val devModeUnlocked: Boolean = false,
    val betaCaptureUiEnabled: Boolean = false,
    val betaLightMeterUiEnabled: Boolean = true,
    val backgroundRecordingEnabled: Boolean = false,
    val vibrationFeedbackEnabled: Boolean = true,
    val isDebugMode: Boolean = false,
    val debugRawJson: String = "",
    val autoNameCapture: Boolean = false,
    val bortleDecimals: Int = -1, // -1 = Maximal (from JSON), 0 = Disabled, 1 = 1 dec, 2 = 2 dec, 3 = 3 dec
    
    val isOnline: Boolean = false,
    val currentActiveIp: String = "",
    val lastRttMs: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val currentRecord: TelemetryRecord? = null,
    val currentRawJson: String = "Waiting for data...",
    val history: List<TelemetryRecord> = emptyList(),
    
    val activeTab: NavigationTab = NavigationTab.OBSERVATION,
    val selectedReport: SavedObservation? = null,
    val savedObservations: List<SavedObservation> = emptyList(),
    val mainTabPageIndex: Int = 0, // 0 = Astronomy SQM, 1 = Light Meter
    val isLightMeterPixelPreview: Boolean = false,
    
    // Capture State
    val isRecording: Boolean = false,
    val currentCaptureMode: RecordType = RecordType.PHOTO,
    val recordingStartTimeMs: Long = 0L,
    val recordingFramesCount: Int = 0,
    val liveRecordingFrames: List<TelemetryRecord> = emptyList(),
    val videoIntervalMs: Long = 500L,
    val timelapseIntervalSec: Long = 5L,
    val showCaptureGraphs: Boolean = true,
    val showCaptureKeogram: Boolean = false,
    val isNightVisionSquare: Boolean = false
) {
    val activeServer: ServerProfile? get() = servers.find { it.id == activeServerId }
}

enum class NavigationTab { WELCOME, OBSERVATION, RAW_DATA, CAPTURE, DATA, RECORDS, SETTINGS }

class SqmViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = SqmPreferencesManager(application)
    private val networkClient = SqmNetworkClient()
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; encodeDefaults = true }
    private val _uiState = MutableStateFlow(SqmUiState())
    val uiState: StateFlow<SqmUiState> = _uiState.asStateFlow()
    
    private var pollingJob: Job? = null
    private var activeRecordingFrames = mutableListOf<TelemetryRecord>()

    init {
        viewModelScope.launch {
            val g1 = combine(prefs.serversFlow, prefs.activeServerIdFlow, prefs.textScaleFlow) { s, a, sc -> Triple(s, a, sc) }
            val g2 = combine(prefs.isDarkThemeFlow, prefs.isFullscreenFlow, prefs.pollingIntervalMsFlow) { d, f, p -> Triple(d, f, p) }
            val g3 = combine(prefs.devModeUnlockedFlow, prefs.betaCaptureUiFlow, prefs.betaLightMeterUiFlow, prefs.backgroundRecordingEnabledFlow, prefs.vibrationFeedbackEnabledFlow) { dev, betaCap, betaLight, bgRec, vib ->
                data class DevConfig(val dev: Boolean, val betaCap: Boolean, val betaLight: Boolean, val bgRec: Boolean, val vib: Boolean)
                DevConfig(dev, betaCap, betaLight, bgRec, vib)
            }
            val g4 = combine(prefs.bortleDecimalsFlow, prefs.showCaptureGraphsFlow, prefs.showCaptureKeogramFlow, prefs.isNightVisionSquareFlow, prefs.isLightMeterPixelPreviewFlow) { dec, graphs, keo, nv, lmPixel ->
                data class CapConfig(val dec: Int, val graphs: Boolean, val keo: Boolean, val nv: Boolean, val lmPixel: Boolean)
                CapConfig(dec, graphs, keo, nv, lmPixel)
            }
            val g5 = combine(prefs.savedObservationsFlow, prefs.isFirstLaunchFlow, prefs.autoNameCaptureFlow) { obs, firstLaunch, autoName ->
                Triple(obs, firstLaunch, autoName)
            }
            
            combine(g1, g2, g3, g4, g5) { t1, t2, t3, t4, t5 ->
                _uiState.update { 
                    it.copy(
                        servers = t1.first, activeServerId = t1.second, textScale = t1.third,
                        isDarkTheme = t2.first, isFullscreen = t2.second, pollingIntervalMs = t2.third,
                        devModeUnlocked = t3.dev, betaCaptureUiEnabled = t3.betaCap, betaLightMeterUiEnabled = t3.betaLight, backgroundRecordingEnabled = t3.bgRec, vibrationFeedbackEnabled = t3.vib,
                        autoNameCapture = t5.third,
                        bortleDecimals = t4.dec, showCaptureGraphs = t4.graphs, showCaptureKeogram = t4.keo, isNightVisionSquare = t4.nv, isLightMeterPixelPreview = t4.lmPixel,
                        savedObservations = t5.first, isFirstLaunch = t5.second,
                        activeTab = if (t5.second) NavigationTab.WELCOME else (if(it.activeTab == NavigationTab.WELCOME) NavigationTab.OBSERVATION else it.activeTab)
                    )
                }
            }.collect()
        }
        viewModelScope.launch {
            if (!_uiState.value.isFirstLaunch) startPolling()
        }
    }

    fun finishWelcome(name: String, ipsString: String, path: String, website: String) {
        val ipList = ipsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val finalIps = if (ipList.isEmpty()) listOf("192.168.3.129") else ipList
        val finalPath = if(path.startsWith("/")) path else "/$path"
        val newServer = ServerProfile(UUID.randomUUID().toString(), name.ifEmpty { "Observatory" }, finalIps, finalPath, website)
        
        viewModelScope.launch {
            prefs.saveServers(listOf(newServer))
            prefs.updatePreferences { 
                it[SqmPreferencesManager.ACTIVE_SERVER_ID] = newServer.id
                it[SqmPreferencesManager.IS_FIRST_LAUNCH] = false
            }
        }
        startPolling()
    }

    fun setAstroRedMode(enabled: Boolean) { _uiState.update { it.copy(isAstroRedMode = enabled) } }
    fun setActiveTab(tab: NavigationTab) { _uiState.update { it.copy(activeTab = tab) } }
    fun openReport(obs: SavedObservation?) { _uiState.update { it.copy(selectedReport = obs) } }

    fun updatePreferences(action: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch { prefs.updatePreferences(action) }
        if (_uiState.value.isOnline) { startPolling() }
    }

    fun setAutoNameCapture(enabled: Boolean) {
        _uiState.update { it.copy(autoNameCapture = enabled) }
        viewModelScope.launch {
            prefs.updatePreferences { it[SqmPreferencesManager.AUTO_NAME_CAPTURE] = enabled }
        }
    }

    fun setBortleDecimals(decimals: Int) {
        _uiState.update { it.copy(bortleDecimals = decimals) }
        viewModelScope.launch {
            prefs.updatePreferences { it[SqmPreferencesManager.BORTLE_DECIMALS] = decimals }
        }
    }

    fun setBetaLightMeterUi(enabled: Boolean) {
        _uiState.update { it.copy(betaLightMeterUiEnabled = enabled) }
        viewModelScope.launch {
            prefs.updatePreferences { it[SqmPreferencesManager.BETA_LIGHT_METER_UI] = enabled }
        }
    }

    fun setBackgroundRecordingEnabled(enabled: Boolean) {
        _uiState.update { it.copy(backgroundRecordingEnabled = enabled) }
        viewModelScope.launch {
            prefs.updatePreferences { it[SqmPreferencesManager.BACKGROUND_RECORDING_ENABLED] = enabled }
        }
    }

    fun setVibrationFeedbackEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationFeedbackEnabled = enabled) }
        viewModelScope.launch {
            prefs.updatePreferences { it[SqmPreferencesManager.VIBRATION_FEEDBACK_ENABLED] = enabled }
        }
    }

    fun setCaptureGraphsVisible(visible: Boolean) {
        _uiState.update { it.copy(showCaptureGraphs = visible) }
        viewModelScope.launch {
            prefs.updatePreferences { it[SqmPreferencesManager.SHOW_CAPTURE_GRAPHS] = visible }
        }
    }

    fun setCaptureKeogramVisible(visible: Boolean) {
        _uiState.update { it.copy(showCaptureKeogram = visible) }
        viewModelScope.launch {
            prefs.updatePreferences { it[SqmPreferencesManager.SHOW_CAPTURE_KEOGRAM] = visible }
        }
    }

    fun setNightVisionSquare(enabled: Boolean) {
        _uiState.update { it.copy(isNightVisionSquare = enabled) }
        viewModelScope.launch {
            prefs.updatePreferences { it[SqmPreferencesManager.IS_NIGHT_VISION_SQUARE] = enabled }
        }
    }

    fun setLightMeterPixelPreview(enabled: Boolean) {
        _uiState.update { it.copy(isLightMeterPixelPreview = enabled) }
        viewModelScope.launch {
            prefs.updatePreferences { it[SqmPreferencesManager.IS_LIGHT_METER_PIXEL_PREVIEW] = enabled }
        }
    }

    fun setMainTabPageIndex(index: Int) {
        _uiState.update { it.copy(mainTabPageIndex = index) }
    }
    
    fun disableDevMode() {
        viewModelScope.launch { 
            prefs.updatePreferences { 
                it[SqmPreferencesManager.DEV_MODE_UNLOCKED] = false 
                it[SqmPreferencesManager.BETA_CAPTURE_UI] = false
                it[SqmPreferencesManager.BETA_LIGHT_METER_UI] = false
                it[SqmPreferencesManager.BACKGROUND_RECORDING_ENABLED] = false
                it[SqmPreferencesManager.VIBRATION_FEEDBACK_ENABLED] = true
            } 
        }
        _uiState.update { it.copy(devModeUnlocked = false, betaCaptureUiEnabled = false, betaLightMeterUiEnabled = false, backgroundRecordingEnabled = false, vibrationFeedbackEnabled = true, isDebugMode = false, debugRawJson = "") }
        startPolling()
    }

    fun setActiveServer(id: String) {
        viewModelScope.launch { prefs.updatePreferences { it[SqmPreferencesManager.ACTIVE_SERVER_ID] = id } }
        _uiState.update { it.copy(isDebugMode = false, debugRawJson = "") }
        startPolling()
    }

    fun addServer(name: String, ipsStr: String, path: String, website: String) {
        val ipList = ipsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val current = _uiState.value.servers.toMutableList()
        current.add(ServerProfile(UUID.randomUUID().toString(), name.ifEmpty{"New Server"}, ipList, path, website))
        viewModelScope.launch { prefs.saveServers(current) }
    }
    
    fun editServer(id: String, name: String, ipsStr: String, path: String, website: String) {
        val ipList = ipsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val current = _uiState.value.servers.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = ServerProfile(id, name.ifEmpty{"Updated Server"}, ipList, path, website)
            viewModelScope.launch { prefs.saveServers(current) }
        }
    }

    fun removeServer(id: String) {
        val current = _uiState.value.servers.filterNot { it.id == id }
        viewModelScope.launch { prefs.saveServers(current) }
    }

    fun saveCurrentObservation(locationName: String, specificHistory: List<TelemetryRecord> = emptyList(), type: RecordType = RecordType.PHOTO) {
        val record = _uiState.value.currentRecord ?: return
        val finalName = if (locationName.isBlank()) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        } else {
            locationName
        }
        val interval = if (type == RecordType.VIDEO) _uiState.value.videoIntervalMs else (_uiState.value.timelapseIntervalSec * 1000L)
        val newObs = SavedObservation(
            id = UUID.randomUUID().toString(), locationName = finalName, timestamp = record.timestamp,
            raw = record.raw, bortleLevel = record.bortleClass.level, nelm = record.nelm, luminanceCdM2 = record.luminanceCdM2,
            type = type, history = specificHistory, frameIntervalMs = interval, isFavorite = false
        )
        val currentList = _uiState.value.savedObservations.toMutableList()
        currentList.add(0, newObs)
        viewModelScope.launch { prefs.saveObservations(currentList) }
    }

    fun renameObservation(id: String, newName: String) {
        if (newName.isBlank()) return
        val currentList = _uiState.value.savedObservations.map {
            if (it.id == id) it.copy(locationName = newName.trim()) else it
        }
        val currentSelected = _uiState.value.selectedReport
        val updatedSelected = if (currentSelected?.id == id) {
            currentSelected.copy(locationName = newName.trim())
        } else {
            currentSelected
        }
        _uiState.update { it.copy(savedObservations = currentList, selectedReport = updatedSelected) }
        viewModelScope.launch { prefs.saveObservations(currentList) }
    }

    fun toggleFavorite(id: String) {
        val currentList = _uiState.value.savedObservations.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
        val currentSelected = _uiState.value.selectedReport
        val updatedSelected = if (currentSelected?.id == id) {
            currentSelected.copy(isFavorite = !currentSelected.isFavorite)
        } else {
            currentSelected
        }
        _uiState.update { it.copy(savedObservations = currentList, selectedReport = updatedSelected) }
        viewModelScope.launch { prefs.saveObservations(currentList) }
    }

    fun removeObservation(id: String) {
        val currentList = _uiState.value.savedObservations.filterNot { it.id == id }
        viewModelScope.launch { prefs.saveObservations(currentList) }
    }

    fun importObservation(jsonStr: String): Boolean {
        return try {
            val obs = jsonParser.decodeFromString<SavedObservation>(jsonStr)
            val newObs = obs.copy(id = UUID.randomUUID().toString())
            val currentList = _uiState.value.savedObservations.toMutableList()
            currentList.add(0, newObs)
            viewModelScope.launch { prefs.saveObservations(currentList) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun enableDebugMode(jsonStr: String): Boolean {
        return try {
            jsonParser.decodeFromString<SqmTelemetryRaw>(jsonStr)
            _uiState.update { it.copy(isDebugMode = true, debugRawJson = jsonStr) }
            startPolling()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareJson(context: Context, obs: SavedObservation) {
        val jsonStr = Json { prettyPrint = true }.encodeToString(obs)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SQM Record: ${obs.locationName}")
            putExtra(Intent.EXTRA_TEXT, jsonStr)
        }
        context.startActivity(Intent.createChooser(intent, "Export JSON"))
    }
    
    fun exportSessionToCsv(context: Context, uri: Uri, obs: SavedObservation) {
        if(obs.history.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write("Timestamp,SQM,Frequency_Hz,Bortle,Bortle_Desc,Brightness_mcd,Artif_Bright_ucd,RTT_ms\n".toByteArray())
                    obs.history.forEach { r ->
                        output.write("${r.timestamp},${r.raw.sqm},${r.raw.frequencyHz},${r.raw.bortle},\"${r.raw.bortleDesc}\",${r.raw.brightnessMcd},${r.raw.artifBrightUcd},${r.rttMs}\n".toByteArray())
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun exportTelemetryToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write("Timestamp,SQM,Frequency_Hz,Bortle,Bortle_Desc,Brightness_mcd,Artif_Bright_ucd,RTT_ms\n".toByteArray())
                    _uiState.value.history.forEach { record ->
                        output.write("${record.timestamp},${record.raw.sqm},${record.raw.frequencyHz},${record.raw.bortle},\"${record.raw.bortleDesc}\",${record.raw.brightnessMcd},${record.raw.artifBrightUcd},${record.rttMs}\n".toByteArray())
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun exportAppDataToFile(context: Context, uri: Uri, option: ExportOption) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = _uiState.value
                val obsToExport = when (option) {
                    ExportOption.ALL -> state.savedObservations
                    ExportOption.RECORDS_ONLY -> state.savedObservations
                    ExportOption.PHOTOS_ONLY -> state.savedObservations.filter { it.type == RecordType.PHOTO }
                }
                val serversToExport = if (option == ExportOption.ALL) state.servers else emptyList()
                val preferencesToExport = if (option == ExportOption.ALL) {
                    AppPreferencesBackup(
                        textScale = state.textScale,
                        isDarkTheme = state.isDarkTheme,
                        isFullscreen = state.isFullscreen,
                        pollingIntervalMs = state.pollingIntervalMs,
                        bortleDecimals = state.bortleDecimals,
                        autoNameCapture = state.autoNameCapture,
                        showCaptureGraphs = state.showCaptureGraphs,
                        showCaptureKeogram = state.showCaptureKeogram,
                        isNightVisionSquare = state.isNightVisionSquare,
                        isLightMeterPixelPreview = state.isLightMeterPixelPreview,
                        devModeUnlocked = state.devModeUnlocked,
                        betaCaptureUiEnabled = state.betaCaptureUiEnabled,
                        betaLightMeterUiEnabled = state.betaLightMeterUiEnabled,
                        backgroundRecordingEnabled = state.backgroundRecordingEnabled,
                        vibrationFeedbackEnabled = state.vibrationFeedbackEnabled
                    )
                } else null

                val backup = AppDataBackup(
                    exportVersion = 2,
                    exportTimestamp = System.currentTimeMillis(),
                    exportType = option.name,
                    preferences = preferencesToExport,
                    servers = serversToExport,
                    observations = obsToExport
                )

                val jsonContent = Json { prettyPrint = true; encodeDefaults = true }.encodeToString(backup)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonContent.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importAppDataFromFile(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (jsonString.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { onResult(false, "File is empty") }
                    return@launch
                }

                var importedObservationsCount = 0
                var importedServersCount = 0

                try {
                    val backup = jsonParser.decodeFromString<AppDataBackup>(jsonString)
                    
                    backup.preferences?.let { p ->
                        prefs.updatePreferences { mutable ->
                            mutable[SqmPreferencesManager.TEXT_SCALE] = p.textScale
                            mutable[SqmPreferencesManager.IS_DARK_THEME] = p.isDarkTheme
                            mutable[SqmPreferencesManager.IS_FULLSCREEN] = p.isFullscreen
                            mutable[SqmPreferencesManager.POLLING_INTERVAL_MS] = p.pollingIntervalMs
                            mutable[SqmPreferencesManager.BORTLE_DECIMALS] = p.bortleDecimals
                            mutable[SqmPreferencesManager.AUTO_NAME_CAPTURE] = p.autoNameCapture
                            mutable[SqmPreferencesManager.SHOW_CAPTURE_GRAPHS] = p.showCaptureGraphs
                            mutable[SqmPreferencesManager.SHOW_CAPTURE_KEOGRAM] = p.showCaptureKeogram
                            mutable[SqmPreferencesManager.IS_NIGHT_VISION_SQUARE] = p.isNightVisionSquare
                            mutable[SqmPreferencesManager.IS_LIGHT_METER_PIXEL_PREVIEW] = p.isLightMeterPixelPreview
                            mutable[SqmPreferencesManager.DEV_MODE_UNLOCKED] = p.devModeUnlocked
                            mutable[SqmPreferencesManager.BETA_CAPTURE_UI] = p.betaCaptureUiEnabled
                            mutable[SqmPreferencesManager.BETA_LIGHT_METER_UI] = p.betaLightMeterUiEnabled
                            mutable[SqmPreferencesManager.BACKGROUND_RECORDING_ENABLED] = p.backgroundRecordingEnabled
                            mutable[SqmPreferencesManager.VIBRATION_FEEDBACK_ENABLED] = p.vibrationFeedbackEnabled
                        }
                    }

                    if (backup.servers.isNotEmpty()) {
                        val existingServerIds = _uiState.value.servers.map { it.id }.toSet()
                        val newServers = backup.servers.filterNot { it.id in existingServerIds }
                        val combined = _uiState.value.servers + newServers
                        prefs.saveServers(combined)
                        importedServersCount = newServers.size
                    }
                    if (backup.observations.isNotEmpty()) {
                        val existingObsIds = _uiState.value.savedObservations.map { it.id }.toSet()
                        val newObs = backup.observations.filterNot { it.id in existingObsIds }
                        val combined = newObs + _uiState.value.savedObservations
                        prefs.saveObservations(combined)
                        importedObservationsCount = newObs.size
                    }
                } catch (e: Exception) {
                    val directList = jsonParser.decodeFromString<List<SavedObservation>>(jsonString)
                    val existingObsIds = _uiState.value.savedObservations.map { it.id }.toSet()
                    val newObs = directList.filterNot { it.id in existingObsIds }
                    val combined = newObs + _uiState.value.savedObservations
                    prefs.saveObservations(combined)
                    importedObservationsCount = newObs.size
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Imported $importedObservationsCount records and $importedServersCount servers")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Import failed: ${e.localizedMessage ?: "Invalid JSON"}")
                }
            }
        }
    }
    
    fun setCaptureMode(mode: RecordType) {
        _uiState.update { it.copy(currentCaptureMode = mode) }
    }

    fun setVideoInterval(ms: Long) {
        _uiState.update { it.copy(videoIntervalMs = ms) }
    }

    fun setTimelapseInterval(sec: Long) {
        _uiState.update { it.copy(timelapseIntervalSec = sec) }
    }
    
    fun startRecording(context: Context? = null) {
        activeRecordingFrames.clear()
        val now = System.currentTimeMillis()
        _uiState.update { it.copy(isRecording = true, recordingStartTimeMs = now, recordingFramesCount = 0, liveRecordingFrames = emptyList()) }
        if (_uiState.value.backgroundRecordingEnabled) {
            val ctx = context ?: getApplication()
            SqmCaptureService.startService(ctx, _uiState.value.currentCaptureMode.name)
        }
        startPolling()
    }
    
    fun stopRecording(context: Context? = null): List<TelemetryRecord> {
        _uiState.update { it.copy(isRecording = false) }
        if (_uiState.value.backgroundRecordingEnabled) {
            val ctx = context ?: getApplication()
            SqmCaptureService.stopService(ctx)
        }
        startPolling()
        return activeRecordingFrames.toList()
    }

    private fun createTelemetryRecord(raw: SqmTelemetryRaw, rtt: Long): TelemetryRecord {
        val bortle = if (raw.bortle > 0) {
            BortleClassification("Class ${raw.bortle}", raw.bortleDesc.ifEmpty { "Class ${raw.bortle}" })
        } else {
            AstronomyFormulas.getBortleClass(raw.sqm)
        }
        val luminance = if (raw.brightnessMcd > 0.0) {
            raw.brightnessMcd / 1000.0
        } else {
            AstronomyFormulas.calculateLuminance(raw.sqm)
        }
        return TelemetryRecord(
            timestamp = System.currentTimeMillis(),
            raw = raw,
            luminanceCdM2 = luminance,
            nelm = AstronomyFormulas.calculateNELM(raw.sqm),
            bortleClass = bortle,
            rttMs = rtt
        )
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val delayTime = when {
                    _uiState.value.isRecording && _uiState.value.currentCaptureMode == RecordType.VIDEO ->
                        _uiState.value.videoIntervalMs
                    _uiState.value.isRecording && _uiState.value.currentCaptureMode == RecordType.TIMELAPSE ->
                        (_uiState.value.timelapseIntervalSec * 1000L).coerceAtLeast(500L)
                    else -> _uiState.value.pollingIntervalMs
                }

                if (_uiState.value.isDebugMode) {
                    try {
                        val raw = jsonParser.decodeFromString<SqmTelemetryRaw>(_uiState.value.debugRawJson)
                        val record = createTelemetryRecord(raw, 0L)
                        processRecordData(record)
                        
                        _uiState.update { state ->
                            state.copy(
                                isOnline = true, lastRttMs = 0L, lastUpdateTime = record.timestamp,
                                currentActiveIp = "DEBUG MOCK", currentRecord = record, currentRawJson = state.debugRawJson,
                                history = (state.history + record).takeLast(200)
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isOnline = false) }
                    }
                    delay(delayTime)
                    continue
                }

                val server = _uiState.value.activeServer
                if (server == null || server.ips.isEmpty()) { delay(2000); continue }
                
                val currentIp = server.ips.first()
                val url = "$currentIp${server.path}"
                _uiState.update { it.copy(currentActiveIp = currentIp) }

                val start = System.currentTimeMillis()
                val result = networkClient.fetchTelemetry(url)
                val rtt = System.currentTimeMillis() - start
                
                result.onSuccess { (raw, jsonString) ->
                    try {
                        val record = createTelemetryRecord(raw, rtt)
                        processRecordData(record)

                        _uiState.update { state ->
                            state.copy(
                                isOnline = true, lastRttMs = rtt, lastUpdateTime = record.timestamp,
                                currentRecord = record, currentRawJson = jsonString,
                                history = (state.history + record).takeLast(200)
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isOnline = false) }
                    }
                }.onFailure { 
                    _uiState.update { it.copy(isOnline = false) }
                }
                delay(delayTime)
            }
        }
    }
    
    private fun processRecordData(record: TelemetryRecord) {
        val state = _uiState.value
        if (state.isRecording) {
            if (state.currentCaptureMode == RecordType.VIDEO || state.currentCaptureMode == RecordType.TIMELAPSE) {
                activeRecordingFrames.add(record)
                val frames = activeRecordingFrames.size
                val elapsed = System.currentTimeMillis() - state.recordingStartTimeMs
                _uiState.update { it.copy(recordingFramesCount = frames, liveRecordingFrames = activeRecordingFrames.toList()) }
                if (state.backgroundRecordingEnabled) {
                    SqmCaptureService.updateProgress(getApplication(), elapsed, frames)
                }
            }
        }
    }
}

package com.pk3ju.skyqualitymeter.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pk3ju.skyqualitymeter.data.*
import com.pk3ju.skyqualitymeter.network.SqmNetworkClient
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
    val isDebugMode: Boolean = false,
    val debugRawJson: String = "",
    val autoNameCapture: Boolean = false,
    
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
    
    // Capture State
    val isRecording: Boolean = false,
    val currentCaptureMode: RecordType = RecordType.PHOTO,
    val recordingStartTimeMs: Long = 0L,
    val recordingFramesCount: Int = 0,
    val liveRecordingFrames: List<TelemetryRecord> = emptyList(),
    val videoIntervalMs: Long = 500L,
    val timelapseIntervalSec: Long = 5L
) {
    val activeServer: ServerProfile? get() = servers.find { it.id == activeServerId }
}

enum class NavigationTab { WELCOME, OBSERVATION, RAW_DATA, CAPTURE, GRAPH, RECORDS, SETTINGS }

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
            val g3 = combine(prefs.devModeUnlockedFlow, prefs.betaCaptureUiFlow, prefs.autoNameCaptureFlow) { dev, beta, autoName -> Triple(dev, beta, autoName) }
            val g4 = combine(prefs.savedObservationsFlow, prefs.isFirstLaunchFlow) { obs, firstLaunch -> Pair(obs, firstLaunch) }
            
            combine(g1, g2, g3, g4) { t1, t2, t3, t4 ->
                _uiState.update { 
                    it.copy(
                        servers = t1.first, activeServerId = t1.second, textScale = t1.third,
                        isDarkTheme = t2.first, isFullscreen = t2.second, pollingIntervalMs = t2.third,
                        devModeUnlocked = t3.first, betaCaptureUiEnabled = t3.second, autoNameCapture = t3.third,
                        savedObservations = t4.first, isFirstLaunch = t4.second,
                        activeTab = if (t4.second) NavigationTab.WELCOME else (if(it.activeTab == NavigationTab.WELCOME) NavigationTab.OBSERVATION else it.activeTab)
                    )
                }
            }.collect()
        }
        viewModelScope.launch { delay(800); if(!_uiState.value.isFirstLaunch) startPolling() }
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
    
    fun disableDevMode() {
        viewModelScope.launch { 
            prefs.updatePreferences { 
                it[SqmPreferencesManager.DEV_MODE_UNLOCKED] = false 
                it[SqmPreferencesManager.BETA_CAPTURE_UI] = false
            } 
        }
        _uiState.update { it.copy(devModeUnlocked = false, betaCaptureUiEnabled = false, isDebugMode = false, debugRawJson = "") }
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

                val backup = AppDataBackup(
                    exportVersion = 1,
                    exportTimestamp = System.currentTimeMillis(),
                    exportType = option.name,
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
    
    fun startRecording() {
        activeRecordingFrames.clear()
        val now = System.currentTimeMillis()
        _uiState.update { it.copy(isRecording = true, recordingStartTimeMs = now, recordingFramesCount = 0, liveRecordingFrames = emptyList()) }
        startPolling()
    }
    
    fun stopRecording(): List<TelemetryRecord> {
        _uiState.update { it.copy(isRecording = false) }
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
                _uiState.update { it.copy(recordingFramesCount = activeRecordingFrames.size, liveRecordingFrames = activeRecordingFrames.toList()) }
            }
        }
    }
}

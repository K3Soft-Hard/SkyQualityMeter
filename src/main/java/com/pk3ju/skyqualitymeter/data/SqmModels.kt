package com.pk3ju.skyqualitymeter.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.log10
import kotlin.math.pow

@Serializable
enum class RecordType { PHOTO, VIDEO, TIMELAPSE }

@Serializable
data class SqmTelemetryRaw(
    @SerialName("valid") val valid: Boolean = false,
    @SerialName("too_dark") val tooDark: Boolean = false,
    @SerialName("sqm") val sqm: Double = 0.0,
    @SerialName("bortle") val bortle: Int = 0,
    @SerialName("bortle_desc") val bortleDesc: String = "",
    @SerialName("brightness_mcd") val brightnessMcd: Double = 0.0,
    @SerialName("artif_bright_ucd") val artifBrightUcd: Double = 0.0,
    @SerialName("frequency_hz") val frequencyHz: Double = 0.0,
    @SerialName("period_ms") val periodMs: Double = 0.0,
    @SerialName("mode") val mode: String = "UNKNOWN",
    @SerialName("fov_deg") val fovDeg: Double = 0.0,
    @SerialName("lens_trans") val lensTrans: Double = 1.0,
    @SerialName("timeout_s") val timeoutS: Long = 0L,
    @SerialName("min_freq_limit") val minFreqLimit: Double = 0.017,
    @SerialName("uptime_s") val uptimeS: Long = 0L
)

@Serializable
data class BortleClassification(val level: String, val title: String)

@Serializable
data class TelemetryRecord(
    val timestamp: Long,
    val raw: SqmTelemetryRaw,
    val luminanceCdM2: Double,
    val nelm: Double,
    val bortleClass: BortleClassification,
    val rttMs: Long = 0L
)

@Serializable
data class SavedObservation(
    val id: String,
    val locationName: String,
    val timestamp: Long,
    val raw: SqmTelemetryRaw,
    val bortleLevel: String,
    val nelm: Double,
    val luminanceCdM2: Double,
    val type: RecordType = RecordType.PHOTO,
    val history: List<TelemetryRecord> = emptyList(),
    val frameIntervalMs: Long = 500L,
    val isFavorite: Boolean = false
)

@Serializable
data class ServerProfile(
    val id: String,
    val name: String,
    val ips: List<String>,
    val path: String = "/json",
    val websiteUrl: String = ""
)

@Serializable
data class AppDataBackup(
    val exportVersion: Int = 1,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val exportType: String = "ALL",
    val servers: List<ServerProfile> = emptyList(),
    val observations: List<SavedObservation> = emptyList()
)

object AstronomyFormulas {
    fun calculateLuminance(sqm: Double): Double {
        if (sqm <= 0.0 || sqm.isNaN() || sqm.isInfinite()) return 0.0
        return 10.8 * 1e4 * 10.0.pow(-0.4 * sqm)
    }

    fun calculateNELM(sqm: Double): Double {
        if (sqm <= 0.0 || sqm.isNaN() || sqm.isInfinite()) return 0.0
        val inner = 10.0.pow(4.3 - 0.2 * sqm) + 1.0
        return 7.93 - (5.0 * log10(inner))
    }

    fun getBortleClass(sqm: Double): BortleClassification {
        if (sqm.isNaN() || sqm.isInfinite()) return BortleClassification("--", "Invalid")
        return when {
            sqm >= 21.90 -> BortleClassification("Class 1", "Excellent Dark Site")
            sqm >= 21.70 -> BortleClassification("Class 2", "Truly Dark Site")
            sqm >= 21.50 -> BortleClassification("Class 3", "Rural Sky")
            sqm >= 20.50 -> BortleClassification("Class 4", "Rural / Suburban Transition")
            sqm >= 19.50 -> BortleClassification("Class 5", "Suburban Sky")
            sqm >= 18.90 -> BortleClassification("Class 6", "Bright Suburban Sky")
            sqm >= 18.40 -> BortleClassification("Class 7", "Suburban / Urban Transition")
            sqm >= 17.80 -> BortleClassification("Class 8", "City Sky")
            else -> BortleClassification("Class 9", "Inner-City Sky / Twilight")
        }
    }
}

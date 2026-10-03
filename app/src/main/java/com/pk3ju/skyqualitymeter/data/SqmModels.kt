package com.pk3ju.skyqualitymeter.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.util.Calendar
import java.util.Locale

object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            return when (element) {
                is JsonPrimitive -> {
                    element.doubleOrNull
                        ?: element.content.toDoubleOrNull()
                        ?: 0.0
                }
                else -> 0.0
            }
        }
        return try {
            decoder.decodeDouble()
        } catch (e: Exception) {
            0.0
        }
    }
}

fun formatBortleValue(rawBortle: Double, decimals: Int): String {
    if (rawBortle <= 0.0) return "--"
    return when (decimals) {
        0 -> String.format(Locale.US, "%.0f", rawBortle)
        1 -> String.format(Locale.US, "%.1f", rawBortle)
        2 -> String.format(Locale.US, "%.2f", rawBortle)
        3 -> String.format(Locale.US, "%.3f", rawBortle)
        else -> {
            if (rawBortle % 1.0 == 0.0) {
                rawBortle.toLong().toString()
            } else {
                rawBortle.toString()
            }
        }
    }
}

@Serializable
data class SqmTelemetryRaw(
    @Serializable(with = FlexibleDoubleSerializer::class) val sqm: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val frequencyHz: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val bortle: Double = 0.0,
    val bortleDesc: String = "",
    @Serializable(with = FlexibleDoubleSerializer::class) val brightnessMcd: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val artifBrightUcd: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val periodMs: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val fovDeg: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val lensTrans: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val timeoutS: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val minFreqLimit: Double = 0.0,
    val mode: String = "",
    val uptimeS: Long = 0,
    val valid: Boolean = false,
    val tooDark: Boolean = false
)

@Serializable
data class BortleClassification(
    val level: String,
    val title: String
)

@Serializable
data class TelemetryRecord(
    val timestamp: Long,
    val raw: SqmTelemetryRaw,
    val luminanceCdM2: Double,
    val nelm: Double,
    val bortleClass: BortleClassification,
    val rttMs: Long
)

enum class RecordType { PHOTO, VIDEO, TIMELAPSE }

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
    val frameIntervalMs: Long = 0L,
    val isFavorite: Boolean = false
)

@Serializable
data class ServerProfile(
    val id: String,
    val name: String,
    val ips: List<String>,
    val path: String,
    val websiteUrl: String
)

@Serializable
data class AppPreferencesBackup(
    val textScale: Float = 1.0f,
    val isDarkTheme: Boolean = true,
    val isFullscreen: Boolean = false,
    val pollingIntervalMs: Long = 3000L,
    val bortleDecimals: Int = -1,
    val autoNameCapture: Boolean = false,
    val showCaptureGraphs: Boolean = true,
    val showCaptureKeogram: Boolean = false,
    val isNightVisionSquare: Boolean = false,
    val isLightMeterPixelPreview: Boolean = false,
    val devModeUnlocked: Boolean = false,
    val betaCaptureUiEnabled: Boolean = false,
    val betaLightMeterUiEnabled: Boolean = true,
    val backgroundRecordingEnabled: Boolean = true,
    val vibrationFeedbackEnabled: Boolean = true
)

@Serializable
data class AppDataBackup(
    val exportVersion: Int = 2,
    val exportTimestamp: Long = 0L,
    val exportType: String = "ALL",
    val preferences: AppPreferencesBackup? = null,
    val servers: List<ServerProfile> = emptyList(),
    val observations: List<SavedObservation> = emptyList()
)

data class LightRating(
    val category: String,
    val rating: String,
    val recommendation: String
)

data class AstroSunMoonData(
    val astroDawnTime: String,
    val astroDuskTime: String,
    val moonriseTime: String,
    val moonsetTime: String,
    val moonIlluminationPct: Int,
    val moonPhaseName: String,
    val moonPhaseEmoji: String,
    val nightDurationHours: String
)

object AstronomyFormulas {
    fun getBortleClass(sqm: Double): BortleClassification {
        return when {
            sqm >= 21.99 -> BortleClassification("Class 1", "Excellent dark-sky site")
            sqm >= 21.89 -> BortleClassification("Class 2", "Typical truly dark site")
            sqm >= 21.69 -> BortleClassification("Class 3", "Rural sky")
            sqm >= 20.49 -> BortleClassification("Class 4", "Rural/suburban transition")
            sqm >= 19.50 -> BortleClassification("Class 5", "Suburban sky")
            sqm >= 18.95 -> BortleClassification("Class 6", "Bright suburban sky")
            sqm >= 18.38 -> BortleClassification("Class 7", "Suburban/urban transition")
            sqm >= 17.80 -> BortleClassification("Class 8", "City sky")
            else -> BortleClassification("Class 9", "Inner-city sky")
        }
    }
    fun calculateLuminance(sqm: Double): Double {
        return 108000.0 * Math.pow(10.0, -0.4 * sqm)
    }
    fun calculateNELM(sqm: Double): Double {
        return 7.93 - 5.0 * Math.log10(Math.pow(10.0, 4.316 - (sqm / 5.0)) + 1.0)
    }

    fun calculateLux(luminanceCdM2: Double): Double {
        return luminanceCdM2 * Math.PI
    }

    fun calculateFootCandles(lux: Double): Double {
        return lux / 10.76391041671
    }

    fun getLightRating(lux: Double): LightRating {
        return when {
            lux < 0.001 -> LightRating(
                "Pitch Dark",
                "Deep Night / Starlight",
                "Dark-adapted vision required. Excellent for deep sky astrophotography."
            )
            lux < 0.01 -> LightRating(
                "Moonlit Night",
                "Subdued Twilight",
                "Low night illumination. Requires dark adaptation or flashlight."
            )
            lux < 0.5 -> LightRating(
                "Night Sky / Streetlight",
                "Very Low Light",
                "Emergency lighting, outdoor pathways, night corridors."
            )
            lux < 10.0 -> LightRating(
                "Dim Ambient",
                "Dim Passage / Accent Light",
                "Stairways, hallways, soft atmospheric illumination."
            )
            lux < 50.0 -> LightRating(
                "Low Ambient",
                "Relaxing / Living Space",
                "Casual conversation, TV viewing, relaxing indoor environment."
            )
            lux < 150.0 -> LightRating(
                "Moderate Indoor",
                "Hallways & Household Area",
                "Dining, kitchen counters, casual household movement."
            )
            lux < 300.0 -> LightRating(
                "Good Indoor Light",
                "Good for Casual Reading & Work",
                "Classrooms, general office work, casual reading & desk tasks."
            )
            lux < 750.0 -> LightRating(
                "Optimal Task Light",
                "Ideal for Office & Precision Work",
                "Desk work, typing, drafting, reading fine print comfortably."
            )
            lux < 2000.0 -> LightRating(
                "High Precision",
                "Detailed Assembly & Crafting",
                "Electronics repair, inspection, art work, fine drafting."
            )
            lux < 10000.0 -> LightRating(
                "Very Bright",
                "Studio & Specialized Lighting",
                "Operating rooms, high-clarity photography, industrial inspection."
            )
            else -> LightRating(
                "Full Daylight",
                "Direct Sunlight / Outdoor",
                "Bright daylight conditions."
            )
        }
    }

    fun calculateAstroData(nowMs: Long = System.currentTimeMillis()): AstroSunMoonData {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowMs
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        
        val t = (dayOfYear - 81) * (2.0 * Math.PI / 365.0)
        val declination = 23.44 * Math.sin(t)
        
        val latRad = Math.toRadians(45.0)
        val decRad = Math.toRadians(declination)
        val cosH0 = (Math.sin(Math.toRadians(-18.0)) - Math.sin(latRad) * Math.sin(decRad)) / (Math.cos(latRad) * Math.cos(decRad))
        val h0Deg = if (cosH0 in -1.0..1.0) Math.toDegrees(Math.acos(cosH0)) else 90.0
        val halfHours = h0Deg / 15.0
        
        val solarNoonHr = 12.0
        val dawnHr = (solarNoonHr - halfHours + 24.0) % 24.0
        val duskHr = (solarNoonHr + halfHours) % 24.0
        
        fun formatHours(hr: Double): String {
            val h = hr.toInt() % 24
            val m = ((hr - h) * 60).toInt().coerceIn(0, 59)
            return String.format(Locale.US, "%02d:%02d", h, m)
        }
        
        val dawnStr = formatHours(dawnHr)
        val duskStr = formatHours(duskHr)
        
        val synodicMonth = 29.53058867
        val refNewMoonMs = 1704974220000L
        val daysSinceNew = ((nowMs - refNewMoonMs) / 86400000.0) % synodicMonth
        val moonAge = if (daysSinceNew < 0) daysSinceNew + synodicMonth else daysSinceNew
        
        val phaseAngle = (moonAge / synodicMonth) * 2.0 * Math.PI
        val illumPct = (((1.0 - Math.cos(phaseAngle)) / 2.0) * 100.0).toInt().coerceIn(0, 100)
        
        val (phaseName, emoji) = when {
            moonAge < 1.84566 -> Pair("New Moon", "🌑")
            moonAge < 5.53699 -> Pair("Waxing Crescent", "🌒")
            moonAge < 9.22831 -> Pair("First Quarter", "🌓")
            moonAge < 12.91963 -> Pair("Waxing Gibbous", "🌔")
            moonAge < 16.61096 -> Pair("Full Moon", "🌕")
            moonAge < 20.30228 -> Pair("Waning Gibbous", "🌖")
            moonAge < 23.99361 -> Pair("Last Quarter", "🌗")
            moonAge < 27.68493 -> Pair("Waning Crescent", "🌘")
            else -> Pair("New Moon", "🌑")
        }
        
        val moonriseHr = (solarNoonHr + (moonAge / synodicMonth) * 24.0 + 6.0) % 24.0
        val moonsetHr = (moonriseHr + 12.4) % 24.0
        
        val moonriseStr = formatHours(moonriseHr)
        val moonsetStr = formatHours(moonsetHr)
        
        val nightHours = (24.0 - (halfHours * 2.0)).coerceAtLeast(0.0)
        val nightDurStr = String.format(Locale.US, "%.1f hrs", nightHours)
        
        return AstroSunMoonData(
            astroDawnTime = dawnStr,
            astroDuskTime = duskStr,
            moonriseTime = moonriseStr,
            moonsetTime = moonsetStr,
            moonIlluminationPct = illumPct,
            moonPhaseName = phaseName,
            moonPhaseEmoji = emoji,
            nightDurationHours = nightDurStr
        )
    }
}

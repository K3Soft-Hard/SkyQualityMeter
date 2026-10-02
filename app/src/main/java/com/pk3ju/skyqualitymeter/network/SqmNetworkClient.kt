package com.pk3ju.skyqualitymeter.network

import com.pk3ju.skyqualitymeter.data.SqmTelemetryRaw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class SqmNetworkClient {
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(false)
        .build()

    suspend fun fetchTelemetry(endpointUrl: String): Result<Pair<SqmTelemetryRaw, String>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(if (!endpointUrl.startsWith("http")) "http://$endpointUrl" else endpointUrl)
            .header("Accept", "application/json")
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty"))
                val start = body.indexOf('{')
                val end = body.lastIndexOf('}')
                if (start == -1 || end == -1 || end <= start) return@withContext Result.failure(IOException("Malformed JSON"))
                val cleanJson = body.substring(start, end + 1)
                Result.success(Pair(jsonParser.decodeFromString<SqmTelemetryRaw>(cleanJson), cleanJson))
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}

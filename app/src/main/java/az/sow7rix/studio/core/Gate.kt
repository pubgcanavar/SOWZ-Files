package az.sow7rix.studio.core

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * @sowzfx kanal uyelik qapisi. SOWZ Tracket ile AYNI Cloudflare Worker'i kullanir
 * (ayni kanal, ayni bot) - her cagiri canli sorgudur, hec bir netice yaddasda saxlanilmir.
 * Kanaldan cixan/atilan istifadeci NOVBETI yoxlamada derhal redd olunur.
 */
object Gate {
    private const val WORKER_URL = "https://sowz-tracket-gate.orucovtaleh4499.workers.dev/check"
    private const val TIMEOUT_MS = 7000

    sealed class Result {
        data class Allowed(val status: String) : Result()
        data class Denied(val reason: String) : Result()
        data class NetworkError(val message: String) : Result()
    }

    /** Sinxron sebeke cagirisi - IO thread-den cagir. */
    fun checkMembership(telegramUserId: String): Result {
        return try {
            val url = URL("$WORKER_URL?uid=${telegramUserId.trim()}")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            conn.disconnect()
            val obj = JSONObject(body)
            if (obj.optBoolean("ok", false)) {
                Result.Allowed(obj.optString("status", "member"))
            } else {
                Result.Denied(obj.optString("reason", "not_member"))
            }
        } catch (e: Exception) {
            Result.NetworkError(e.message ?: "unknown")
        }
    }
}

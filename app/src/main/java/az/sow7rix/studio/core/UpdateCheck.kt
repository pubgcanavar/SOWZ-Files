package az.sow7rix.studio.core

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Zorunlu guncelleme kontrolu. Ayni Cloudflare Worker'in /version uc noktasini kullanir
 * (app=sow7rix parametresiyle - ayni worker birden fazla uygulamaya hizmet eder).
 * Worker tarafinda bu uc nokta henuz yoksa "server_misconfigured" / network hatasi doner,
 * o durumda cagiran taraf kullaniciyi BLOKE ETMEZ (worker hazir olana kadar).
 */
object UpdateCheck {
    private const val VERSION_URL = "https://sowz-tracket-gate.orucovtaleh4499.workers.dev/version?app=sow7rix"
    private const val TIMEOUT_MS = 7000
    const val DOWNLOAD_CHANNEL = "https://t.me/sowzfx"

    data class Info(val latestCode: Int, val downloadUrl: String)

    sealed class Result {
        data class UpdateRequired(val info: Info) : Result()
        object UpToDate : Result()
        data class Unknown(val message: String) : Result()
    }

    /** Sinxron sebeke cagirisi - IO thread-den cagir. */
    fun check(currentVersionCode: Int): Result {
        return try {
            val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            conn.disconnect()
            val obj = JSONObject(body)
            if (!obj.optBoolean("ok", false)) return Result.Unknown(obj.optString("reason", "unknown"))
            val latest = obj.optInt("latest_version_code", -1)
            val download = obj.optString("download_url", DOWNLOAD_CHANNEL)
            if (latest <= 0) return Result.Unknown("no_version_info")
            if (latest > currentVersionCode) Result.UpdateRequired(Info(latest, download))
            else Result.UpToDate
        } catch (e: Exception) {
            Result.Unknown(e.message ?: "network_error")
        }
    }
}

package az.sow7rix.studio.ui

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** Claude chat via the Cloudflare Worker; the provider API key never ships in the APK. */
class ClaudeActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("sow7rix", MODE_PRIVATE) }
    private val history = mutableListOf<Pair<String, String>>()
    private lateinit var transcript: LinearLayout
    private lateinit var transcriptScroll: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: TextView
    private var busy = false

    companion object {
        private const val CHAT_URL = "https://sowz-tracket-gate.orucovtaleh4499.workers.dev/ai/chat"
        private const val MAX_PROMPT_CHARS = 2000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        addBubble("assistant", "Salam! Mən Claude AI köməkçisiyəm. Sow7Rix Studio və video keyfiyyəti barədə kömək edə bilərəm.")
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 10, 10))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(40f), dp(20f), dp(16f))
            setBackgroundColor(Color.rgb(17, 17, 17))
        }
        header.addView(TextView(this).apply {
            text = "‹   Claude AI"
            textSize = 21f
            setTextColor(Color.rgb(255, 190, 70))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "Sow7Rix Studio • AI Assistant"
            textSize = 12f
            setTextColor(Color.rgb(165, 165, 165))
            setPadding(0, dp(5f), 0, 0)
        })
        page.addView(header)

        transcriptScroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        }
        transcript = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        transcriptScroll.addView(transcript)
        page.addView(transcriptScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(10f), dp(12f), dp(18f))
            setBackgroundColor(Color.rgb(17, 17, 17))
        }
        input = EditText(this).apply {
            hint = "Mesajını yaz..."
            setHintTextColor(Color.rgb(130, 130, 130))
            setTextColor(Color.WHITE)
            textSize = 14f
            maxLines = 4
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            background = rounded(Color.rgb(33, 33, 33), 14f, Color.rgb(65, 65, 65))
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
            }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        sendButton = TextView(this).apply {
            text = "Gönder"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(10, 10, 10))
            gravity = Gravity.CENTER
            setPadding(dp(15f), dp(13f), dp(15f), dp(13f))
            background = rounded(Color.rgb(255, 190, 70), 14f)
            setOnClickListener { sendMessage() }
        }
        composer.addView(sendButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(8f) })
        page.addView(composer)
        setContentView(page)
    }

    private fun sendMessage() {
        if (busy) return
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        if (prompt.length > MAX_PROMPT_CHARS) {
            AlertDialog.Builder(this).setMessage("Mesaj en fazla $MAX_PROMPT_CHARS karakter ola bilər.")
                .setPositiveButton("Tamam", null).show()
            return
        }
        val uid = prefs.getString("telegram_id", null).orEmpty()
        if (uid.isBlank()) {
            AlertDialog.Builder(this).setMessage("Telegram üzvlük məlumatı tapılmadı. Yenidən daxil ol.")
                .setPositiveButton("Tamam", null).show()
            return
        }
        history.add("user" to prompt)
        addBubble("user", prompt)
        input.setText("")
        busy = true
        sendButton.isEnabled = false
        sendButton.text = "..."
        val requestHistory = history.takeLast(12).toList()

        thread(name = "claude-api-request") {
            try {
                val answer = requestClaude(uid, requestHistory)
                runOnUiThread {
                    history.add("assistant" to answer)
                    addBubble("assistant", answer)
                    finishRequest()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    addBubble("assistant", userFriendlyError(e.message.orEmpty()))
                    finishRequest()
                }
            }
        }
    }

    private fun requestClaude(uid: String, messages: List<Pair<String, String>>): String {
        val payloadMessages = JSONArray()
        messages.forEach { (role, content) ->
            payloadMessages.put(JSONObject().put("role", role).put("content", content))
        }
        val body = JSONObject().put("uid", uid).put("messages", payloadMessages).toString()
        val connection = (URL(CHAT_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "{}"
            val response = JSONObject(responseBody)
            if (status !in 200..299 || !response.optBoolean("ok", false)) {
                throw IllegalStateException(response.optString("reason", "network_error"))
            }
            return response.optString("reply").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("empty_reply")
        } finally {
            connection.disconnect()
        }
    }

    private fun userFriendlyError(reason: String): String = when {
        reason.contains("ai_not_configured") ->
            "Claude hələ qurulmayıb. İdarəçi Cloudflare Worker-a ANTHROPIC_API_KEY əlavə etməlidir."
        reason.contains("not_member") || reason.contains("membership") ->
            "@sowzfx kanalına üzvlüyünü yoxla."
        reason.contains("429") || reason.contains("rate") ->
            "Sorğu limiti dolub. Bir az sonra yenidən yoxla."
        else -> "Claude-a qoşulmaq olmadı. İnterneti və Worker konfiqurasiyasını yoxla. ($reason)"
    }

    private fun addBubble(role: String, message: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (role == "user") Gravity.END else Gravity.START
            setPadding(0, dp(4f), 0, dp(4f))
        }
        val userMessage = role == "user"
        val bubble = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(if (userMessage) Color.rgb(12, 12, 12) else Color.rgb(242, 242, 242))
            setTextIsSelectable(true)
            maxWidth = dp(300f)
            setPadding(dp(14f), dp(11f), dp(14f), dp(11f))
            background = rounded(
                if (userMessage) Color.rgb(255, 190, 70) else Color.rgb(31, 31, 31),
                16f,
                if (userMessage) Color.rgb(255, 190, 70) else Color.rgb(55, 55, 55)
            )
        }
        row.addView(bubble, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        transcript.addView(row)
        transcriptScroll.post { transcriptScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun finishRequest() {
        busy = false
        sendButton.isEnabled = true
        sendButton.text = "Gönder"
    }

    private fun rounded(color: Int, radiusDp: Float, stroke: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (stroke != null) setStroke(dp(1f), stroke)
        }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}

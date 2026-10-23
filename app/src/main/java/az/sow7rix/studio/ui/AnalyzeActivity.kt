package az.sow7rix.studio.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

/**
 * TikTok Analiz: bir video linki icin baslik/yazar/kapak bilgisini gosterir.
 * TikTok'un herkese acik, girissiz oEmbed ucunu kullanir - hesap baglama,
 * cookie veya login yok, bu yuzden ban riski sifirdir.
 *   GET https://www.tiktok.com/oembed?url=<video_link>
 */
class AnalyzeActivity : Activity() {

    private lateinit var linkInput: EditText
    private lateinit var goRow: LinearLayout
    private lateinit var goLabel: TextView
    private lateinit var statusText: TextView
    private lateinit var resultCard: LinearLayout
    private lateinit var thumb: ImageView
    private lateinit var titleText: TextView
    private lateinit var authorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor(UiKit.BG)) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f), dp(56f), dp(24f), dp(40f))
        }

        root.addView(UiKit.title(this, "TikTok Analiz", 22f))
        root.addView(UiKit.spacer(this, 8f))
        root.addView(UiKit.subtitle(this, "Bir TikTok video linki yapistir, basligini/yazarini/kapagini gor"))
        root.addView(UiKit.spacer(this, 28f))

        linkInput = EditText(this).apply {
            hint = "https://www.tiktok.com/@kullanici/video/..."
            setHintTextColor(Color.parseColor(UiKit.TEXT_DIM))
            setTextColor(Color.parseColor(UiKit.TEXT))
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            background = UiKit.fieldBg(this@AnalyzeActivity)
            setPadding(dp(20f), dp(18f), dp(20f), dp(18f))
            textSize = 14f
        }
        root.addView(linkInput, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(UiKit.spacer(this, 16f))

        goLabel = TextView(this).apply {
            text = "Getir"
            setTextColor(Color.BLACK)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        goRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = UiKit.primaryButtonBg(this@AnalyzeActivity)
            isClickable = true
            isFocusable = true
            setPadding(0, dp(18f), 0, dp(18f))
            setOnClickListener { onGoClicked() }
            addView(goLabel)
        }
        root.addView(goRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        statusText = TextView(this).apply {
            setTextColor(Color.parseColor(UiKit.AMBER))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(18f), 0, 0)
        }
        root.addView(statusText)

        root.addView(UiKit.spacer(this, 20f))

        resultCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.card(this@AnalyzeActivity)
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
            visibility = View.GONE
        }
        thumb = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220f).toInt())
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        resultCard.addView(thumb)
        resultCard.addView(UiKit.spacer(this, 14f))
        titleText = TextView(this).apply {
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        resultCard.addView(titleText)
        resultCard.addView(UiKit.spacer(this, 6f))
        authorText = TextView(this).apply {
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 13f
        }
        resultCard.addView(authorText)
        root.addView(resultCard)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun onGoClicked() {
        val link = linkInput.text.toString().trim()
        if (link.isBlank() || !link.contains("tiktok.com")) {
            statusText.text = "Gecerli bir TikTok linki gir"
            return
        }
        goRow.isEnabled = false
        goLabel.text = "Getiriliyor..."
        statusText.text = ""
        resultCard.visibility = View.GONE
        thread {
            val result = fetchOembed(link)
            runOnUiThread {
                goRow.isEnabled = true
                goLabel.text = "Getir"
                when (result) {
                    is OembedResult.Success -> {
                        titleText.text = result.title
                        authorText.text = "@" + result.authorName
                        resultCard.visibility = View.VISIBLE
                        statusText.text = ""
                        if (result.thumbnailUrl != null) loadThumbnail(result.thumbnailUrl)
                    }
                    is OembedResult.Error -> {
                        statusText.text = result.message
                    }
                }
            }
        }
    }

    private fun loadThumbnail(urlStr: String) {
        thread {
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                conn.instanceFollowRedirects = true
                val bmp: Bitmap? = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                conn.disconnect()
                if (bmp != null) runOnUiThread { thumb.setImageBitmap(bmp) }
            } catch (_: Exception) {
                // Kapak yuklenemezse sessizce atla - baslik/yazar hala gosteriliyor.
            }
        }
    }

    private sealed class OembedResult {
        data class Success(val title: String, val authorName: String, val thumbnailUrl: String?) : OembedResult()
        data class Error(val message: String) : OembedResult()
    }

    private fun fetchOembed(videoUrl: String): OembedResult {
        return try {
            val encoded = URLEncoder.encode(videoUrl, "UTF-8")
            val conn = URL("https://www.tiktok.com/oembed?url=$encoded").openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            conn.disconnect()
            if (code !in 200..299) {
                return OembedResult.Error("Bu link icin bilgi alinamadi (kod $code). Linki kontrol et.")
            }
            val obj = JSONObject(body)
            OembedResult.Success(
                title = obj.optString("title", "(baslik yok)"),
                authorName = obj.optString("author_name", "bilinmiyor"),
                thumbnailUrl = obj.optString("thumbnail_url", null)
            )
        } catch (e: Exception) {
            OembedResult.Error("Baglanti hatasi: " + (e.message ?: "bilinmeyen"))
        }
    }

    private fun dp(v: Float) = UiKit.dp(this, v).toInt()
}

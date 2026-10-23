package az.sow7rix.studio.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import az.sow7rix.studio.BuildConfig
import az.sow7rix.studio.R
import kotlin.concurrent.thread
import az.sow7rix.studio.core.Gate
import az.sow7rix.studio.core.IntegrityCheck

/**
 * Ilk acilis ekrani: Telegram ID ile giris + @sowzfx kanal uyelik kontrolu.
 * TikTok / Google girisleri MainActivity menusunde, gercek OAuth akisiyla (bkz. OAuth.kt) -
 * calismalari icin senin developer konsollarindan Client ID/Secret almanis gerekiyor.
 */
class LoginActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("sow7rix", MODE_PRIVATE) }

    private lateinit var statusText: TextView
    private lateinit var idInput: EditText
    private lateinit var loginRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Imza korumasi: APK baskasi tarafindan alinip yeniden imzalanip (resign)
        // dagitilmaya calisilirsa, imza parmak izi uyusmaz ve uygulama acilmaz.
        if (!BuildConfig.DEBUG && IntegrityCheck.verify(this) is IntegrityCheck.Result.Tampered) {
            showTamperedScreen()
            return
        }

        val savedId = prefs.getString("telegram_id", null)
        if (!savedId.isNullOrBlank()) {
            showChecking()
            thread {
                val result = Gate.checkMembership(savedId)
                runOnUiThread { handleResult(result, savedId) }
            }
            return
        }

        buildLoginUi()
    }

    private fun buildLoginUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor(UiKit.BG)) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32f), dp(72f), dp(32f), dp(32f))
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(dp(96f), dp(96f))
        }
        root.addView(icon)
        root.addView(UiKit.spacer(this, 20f))
        root.addView(UiKit.title(this, "Sow7Rix Studio"))
        root.addView(UiKit.spacer(this, 10f))
        root.addView(UiKit.subtitle(this, "Devam etmek icin Telegram ID'ni gir ve @sowzfx kanalina uye ol"))
        root.addView(UiKit.spacer(this, 36f))

        idInput = EditText(this).apply {
            hint = "Telegram ID"
            setHintTextColor(Color.parseColor(UiKit.TEXT_DIM))
            setTextColor(Color.parseColor(UiKit.TEXT))
            inputType = InputType.TYPE_CLASS_NUMBER
            background = UiKit.fieldBg(this@LoginActivity)
            setPadding(dp(20f), dp(18f), dp(20f), dp(18f))
            textSize = 16f
        }
        root.addView(idInput, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(UiKit.spacer(this, 16f))

        loginRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = UiKit.primaryButtonBg(this@LoginActivity)
            isClickable = true
            isFocusable = true
            setPadding(0, dp(18f), 0, dp(18f))
            setOnClickListener { onLoginClicked() }
            addView(TextView(this@LoginActivity).apply {
                text = "Giris Yap"
                setTextColor(Color.BLACK)
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
        root.addView(loginRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        statusText = TextView(this).apply {
            setTextColor(Color.parseColor(UiKit.AMBER))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(24f), 0, 0)
        }
        root.addView(statusText)

        val channelLink = TextView(this).apply {
            text = "@sowzfx kanalina katil"
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(14f), 0, 0)
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sowzfx"))) }
        }
        root.addView(channelLink)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun onLoginClicked() {
        val id = idInput.text.toString().trim()
        if (id.isBlank()) {
            statusText.text = "Telegram ID bos olamaz"
            return
        }
        loginRow.isEnabled = false
        statusText.text = "Kontrol ediliyor..."
        thread {
            val result = Gate.checkMembership(id)
            runOnUiThread {
                loginRow.isEnabled = true
                handleResult(result, id)
            }
        }
    }

    private fun showChecking() {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(UiKit.BG))
        }
        wrap.addView(TextView(this).apply {
            text = "Kontrol ediliyor..."
            setTextColor(Color.parseColor(UiKit.TEXT))
            gravity = Gravity.CENTER
        })
        setContentView(wrap)
    }

    private fun handleResult(result: Gate.Result, id: String) {
        when (result) {
            is Gate.Result.Allowed -> {
                prefs.edit().putString("telegram_id", id).apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            is Gate.Result.Denied -> {
                buildLoginUi()
                statusText.text = "Bu ID @sowzfx kanalinda degil. Once kanala katil, sonra tekrar dene."
            }
            is Gate.Result.NetworkError -> {
                buildLoginUi()
                statusText.text = "Baglanti hatasi, tekrar dene: ${result.message}"
            }
        }
    }

    private fun showTamperedScreen() {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(UiKit.BG))
            setPadding(dp(32f), dp(32f), dp(32f), dp(32f))
        }
        wrap.addView(TextView(this).apply {
            text = "Bu APK'nin imzasi dogrulanamadi"
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        wrap.addView(UiKit.spacer(this, 10f))
        wrap.addView(TextView(this).apply {
            text = "Sow7Rix Studio sadece @sowzfx kanalinda paylasilan resmi surumle calisir. " +
                "Baska bir kaynaktan veya yeniden paketlenmis bir kopyadan geliyorsa guvenli degildir."
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 13f
            gravity = Gravity.CENTER
        })
        setContentView(wrap)
    }

    private fun dp(v: Float) = UiKit.dp(this, v).toInt()
}

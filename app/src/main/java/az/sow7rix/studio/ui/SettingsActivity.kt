package az.sow7rix.studio.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import az.sow7rix.studio.BuildConfig
import az.sow7rix.studio.core.UpdateCheck
import kotlin.concurrent.thread

/**
 * Ayarlar: kanal katilim linki, onbellek temizleme, cikis yap, manuel guncelleme kontrolu.
 */
class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f), dp(48f), dp(24f), dp(24f))
        }

        content.addView(UiKit.title(this, "Ayarlar", 22f))
        content.addView(UiKit.spacer(this, 20f))

        content.addView(UiKit.menuRowV(this, UiKit.Glyph.BELL, "@sowzfx Kanalina Katil", "Yeni surum duyurulari ve destek", "#4DD68C") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sowzfx")))
        })
        content.addView(UiKit.spacer(this, 12f))
        content.addView(UiKit.menuRowV(this, UiKit.Glyph.REFRESH, "Guncelleme Kontrol Et", "Yeni surum var mi diye simdi bak", "#5CC8FF") {
            checkUpdateNow()
        })
        content.addView(UiKit.spacer(this, 12f))
        content.addView(UiKit.menuRowV(this, UiKit.Glyph.TRASH, "Onbellegi Temizle", "Islenen gecici video dosyalarini sil", UiKit.TEXT_DIM) {
            clearCache()
        })
        content.addView(UiKit.spacer(this, 12f))
        content.addView(UiKit.menuRowV(this, UiKit.Glyph.EXIT, "Cikis Yap", "Telegram ID'ni unut, giris ekranina don", UiKit.DANGER) {
            confirmLogout()
        })

        content.addView(UiKit.spacer(this, 28f))
        content.addView(TextView(this).apply {
            text = "Sow7Rix Studio v" + BuildConfig.VERSION_NAME
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 12f
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(UiKit.bottomNav(this, activeIndex = 3) { index -> MainActivity.navigateTab(this, index) })

        setContentView(root)
    }

    private fun checkUpdateNow() {
        Toast.makeText(this, "Kontrol ediliyor...", Toast.LENGTH_SHORT).show()
        thread {
            val result = UpdateCheck.check(BuildConfig.VERSION_CODE)
            runOnUiThread {
                when (result) {
                    is UpdateCheck.Result.UpdateRequired -> AlertDialog.Builder(this)
                        .setTitle("Guncelleme var")
                        .setMessage("Yeni surum mevcut.")
                        .setPositiveButton("Indir") { _, _ ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.info.downloadUrl)))
                        }
                        .setNegativeButton("Kapat", null)
                        .show()
                    is UpdateCheck.Result.UpToDate -> Toast.makeText(this, "Guncelsin, en son surumdesin.", Toast.LENGTH_SHORT).show()
                    is UpdateCheck.Result.Unknown -> Toast.makeText(this, "Kontrol edilemedi: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun clearCache() {
        val files = cacheDir.listFiles() ?: emptyArray()
        var count = 0
        files.forEach { if (it.name.startsWith("sow7rix_")) { it.delete(); count++ } }
        Toast.makeText(this, "$count gecici dosya silindi.", Toast.LENGTH_SHORT).show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cikis yap")
            .setMessage("Telegram ID'n unutulacak, tekrar giris yapman gerekecek.")
            .setPositiveButton("Cikis Yap") { _, _ ->
                getSharedPreferences("sow7rix", MODE_PRIVATE).edit().clear().apply()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
            .setNegativeButton("Vazgec", null)
            .show()
    }

    private fun dp(v: Float) = UiKit.dp(this, v).toInt()
}

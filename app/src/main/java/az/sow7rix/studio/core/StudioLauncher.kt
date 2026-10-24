package az.sow7rix.studio.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * TikTok Studio'yu uygulama içi WebView/alt panel yerine gerçek bir harici tarayıcıda açar.
 * Önce Chrome denenir; Chrome yoksa Android'in varsayılan tarayıcısı kullanılır.
 * Google/Apple/Facebook oturum açma ekranları gömülü WebView kısıtlamalarına takılmaz.
 */
object StudioLauncher {
    const val STUDIO_URL = "https://www.tiktok.com/tiktokstudio/upload"

    fun open(ctx: Context, toolbarColorHex: String = "#0B0D12", cardColorHex: String = "#171B24") {
        val uri = Uri.parse(STUDIO_URL)
        val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage("com.android.chrome")
            if (ctx !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            // Explicit package prevents TikTok links from being captured by the TikTok app.
            ctx.startActivity(chromeIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Chrome isn't installed; continue to the system browser.
        } catch (_: Exception) {
            // Continue to the system browser on any package resolution failure.
        }

        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                if (ctx !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Tarayıcı açılamadı. Chrome'u yükleyip tekrar dene.", Toast.LENGTH_LONG).show()
        }
    }
}

package az.sow7rix.studio.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * TikTok Studio'yu uygulama içindeki WebView/Custom Tab yerine harici tarayıcıda açar.
 * Chrome ilk tercihtir; yoksa bilinen bir harici tarayıcı seçilir.
 */
object StudioLauncher {
    const val STUDIO_URL = "https://www.tiktok.com/tiktokstudio/upload"

    private val browserPriority = listOf(
        "com.android.chrome",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser"
    )

    fun open(ctx: Context, toolbarColorHex: String = "#0B0D12", cardColorHex: String = "#171B24") {
        val uri = Uri.parse(STUDIO_URL)

        // Launch Chrome directly first; this does not depend on package visibility queries.
        try {
            val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage("com.android.chrome")
                if (ctx !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(chromeIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Chrome isn't installed; try the next browser.
        } catch (_: Exception) {
            // Continue to fallback.
        }

        try {
            val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            val handlers = ctx.packageManager.queryIntentActivities(genericIntent, 0)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
            val browser = browserPriority.firstOrNull { it in handlers }
            if (browser != null) {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    setPackage(browser)
                    if (ctx !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(browserIntent)
            } else {
                Toast.makeText(ctx, "Uyğun brauzer tapılmadı. Chrome-u quraşdırıb yenidən yoxla.", Toast.LENGTH_LONG).show()
            }
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(ctx, "Chrome və ya başqa brauzer tapılmadı.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "Brauzer açıla bilmədi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

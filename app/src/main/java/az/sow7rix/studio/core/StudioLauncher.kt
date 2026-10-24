package az.sow7rix.studio.core

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

/** TikTok is shown as a browser-powered sheet over the app, without an embedded password form. */
object StudioLauncher {
    const val STUDIO_URL="https://www.tiktok.com/tiktokstudio/upload"
    const val LOGIN_URL="https://www.tiktok.com/login?lang=tr"
    const val PROFILE_URL="https://www.tiktok.com/"
    fun open(ctx:Context,toolbarColorHex:String="#080B10",cardColorHex:String="#111722")=openUrl(ctx,STUDIO_URL,toolbarColorHex,cardColorHex)
    fun openLogin(ctx:Context)=openUrl(ctx,LOGIN_URL)
    fun openProfile(ctx:Context)=openUrl(ctx,PROFILE_URL)
    fun openUrl(ctx:Context,url:String,toolbarColorHex:String="#080B10",cardColorHex:String="#111722") {
        val uri=Uri.parse(url)
        try {
            val height=(ctx.resources.displayMetrics.heightPixels*0.92f).toInt()
            val tab=CustomTabsIntent.Builder()
                .setToolbarColor(Color.parseColor(toolbarColorHex))
                .setNavigationBarColor(Color.parseColor(cardColorHex))
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .setInitialActivityHeightPx(height,CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE)
                .setToolbarCornerRadiusDp(18)
                .build()
            tab.launchUrl(ctx,uri)
            return
        } catch(_:ActivityNotFoundException) {
            // Fall back to the default browser if there is no Custom Tabs provider.
        } catch(_:Exception) {
            // Browser fallback is safer than collecting credentials in an embedded WebView.
        }
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW,uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                if(ctx !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch(_:Exception) {
            Toast.makeText(ctx,"Tarayıcı bulunamadı. Chrome'u kurup tekrar dene.",Toast.LENGTH_LONG).show()
        }
    }
}

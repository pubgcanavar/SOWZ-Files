package az.sow7rix.studio.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * TikTok Studio'nun yukleme ekranini "Partial Custom Tab" (alttan acilan sayfa /
 * bottom-sheet) olarak acar - tam ekran Chrome'a GECIS yapmak yerine, sayfa ekranin
 * UZERINE kayarak acilir, altta uygulamamizin bir parcasi hala gorunur kalir.
 *
 * Teknik olarak: hala GERCEK Chrome calisiyor (ayri bir islem olarak), sadece gorsel
 * olarak kucuk bir panel seklinde sunuluyor - bu yuzden Google/Facebook/Apple girisi
 * calisir (WebView degil, gercek tarayici - disallowed_useragent engeline takilmaz).
 * Bu extra'lar AndroidX kutuphanesi GEREKTIRMEZ - Chrome bunlari ham Intent string
 * anahtarlariyla okuyor. Desteklemeyen bir tarayici bu extra'lari sessizce yok sayip
 * normal tam ekran acar - cokme riski yok. Alt sayfa yuksekligi Chrome 107+ gerektirir.
 *
 * ONEMLI (talimat Bolum 10/11 ayrimi): bu fonksiyon SADECE TikTok Studio'nun yukleme
 * ekranini ACAR. Videoyu OTOMATIK yuklemez - kullanici orada kendi dosya secicisinden
 * Movies/Sow7Rix klasorune kaydedilmis dosyayi kendisi secer. Bu, UploadActivity ve
 * HistoryActivity gibi birden fazla ekranin AYNI, tek-dogrulanmis acilista kodunu
 * kullanmasi icin buraya (core katmanina) cikarildi - once UploadActivity icinde
 * gomuluydu, boylece kod tekrari ve iki farkli davranis riski ortadan kalkti.
 */
object StudioLauncher {

    const val STUDIO_URL = "https://www.tiktok.com/tiktokstudio/upload"

    /** Cihazda yuklu tarayicilar arasindan GERCEKTEN Custom Tabs protokolunu
     *  destekleyenleri bulur ve aralarindan Chrome'u tercih eder. Varsayilan tarayici
     *  ne olursa olsun (ornegin Edge), bottom-sheet ve dogru mobil render ancak
     *  GERCEKTEN Custom-Tabs-destekli bir tarayicida calisir - bu yuzden setPackage
     *  ile acikca yonlendiriyoruz. AndroidX gerektirmez: servis action'ini PackageManager
     *  uzerinden ham sorgu ile buluyoruz. */
    private fun resolveCustomTabsPackage(pm: PackageManager): String? {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com"))
        val browsersThatHandleLinks = try {
            pm.queryIntentActivities(webIntent, 0).map { it.activityInfo.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
        val serviceIntent = Intent("android.support.customtabs.action.CustomTabsService")
        val customTabsCapable = try {
            pm.queryIntentServices(serviceIntent, 0)
                .map { it.serviceInfo.packageName }
                .filter { it in browsersThatHandleLinks }
        } catch (_: Exception) {
            emptyList()
        }
        if (customTabsCapable.isEmpty()) return null
        val chromePriority = listOf(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary"
        )
        return chromePriority.firstOrNull { it in customTabsCapable } ?: customTabsCapable.first()
    }

    /** TikTok Studio'yu bottom-sheet Custom Tab olarak acar. toolbarColor/cardColor
     *  cagiran ekranin UiKit renkleriyle temalamak icin disaridan verilir (core katmani
     *  UI paketine bagimli olmasin diye renk burada parametre, UiKit sabiti degil). */
    fun open(ctx: Context, toolbarColorHex: String, cardColorHex: String) {
        try {
            val uri = Uri.parse(STUDIO_URL)
            val screenHeight = ctx.resources.displayMetrics.heightPixels
            val sheetHeight = (screenHeight * 0.88f).toInt()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                putExtra("android.support.customtabs.extra.TOOLBAR_COLOR", Color.parseColor(toolbarColorHex))
                putExtra("android.support.customtabs.extra.SECONDARY_TOOLBAR_COLOR", Color.parseColor(cardColorHex))
                putExtra("android.support.customtabs.extra.ENABLE_URLBAR_HIDING", true)
                putExtra("androidx.browser.customtabs.extra.INITIAL_ACTIVITY_HEIGHT_PX", sheetHeight)
                putExtra("androidx.browser.customtabs.extra.ACTIVITY_HEIGHT_RESIZE_BEHAVIOR", 1) // ADJUSTABLE
                putExtra("androidx.browser.customtabs.extra.TOOLBAR_CORNER_RADIUS_DP", 18)
                // ONEMLI: SESSION anahtari BULUNMAK ZORUNDA (bos/null bile olsa) - yoksa
                // Chrome bunu sıradan bir link sanip tam ekran acar.
                val sessionBundle = Bundle()
                sessionBundle.putBinder("android.support.customtabs.extra.SESSION", null)
                putExtras(sessionBundle)
            }
            resolveCustomTabsPackage(ctx.packageManager)?.let { intent.setPackage(it) }
            if (ctx !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(STUDIO_URL)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    if (ctx !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(ctx, "Tarayici acilamadi: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

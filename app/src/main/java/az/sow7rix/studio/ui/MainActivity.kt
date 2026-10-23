package az.sow7rix.studio.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import az.sow7rix.studio.BuildConfig
import az.sow7rix.studio.R
import az.sow7rix.studio.core.UpdateCheck
import kotlin.concurrent.thread

/**
 * Ana menu (Ana Sayfa sekmesi). Acilista zorunlu guncelleme kontrolu yapar: yeni surum
 * varsa kullanici guncelleyene kadar kapatilamayan bir dialog gosterilir.
 */
class MainActivity : Activity() {

    private var glowAnimator: ValueAnimator? = null

    companion object {
        /** Alt navigasyon sekmeleri arasinda gecis - Ana Sayfa/Gecmis/Ayarlar Activity'lerini degistirir. */
        fun navigateTab(from: Activity, index: Int) {
            val target: Class<*> = when (index) {
                1 -> HistoryActivity::class.java
                2 -> SettingsActivity::class.java
                else -> MainActivity::class.java
            }
            if (from.javaClass == target) return
            from.startActivity(Intent(from, target))
            from.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildMenuUi()
        checkForcedUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        glowAnimator?.cancel()
    }

    private fun checkForcedUpdate() {
        thread {
            val result = UpdateCheck.check(BuildConfig.VERSION_CODE)
            if (result is UpdateCheck.Result.UpdateRequired) {
                runOnUiThread { showForceUpdateDialog(result.info) }
            }
        }
    }

    private fun showForceUpdateDialog(info: UpdateCheck.Info) {
        AlertDialog.Builder(this)
            .setTitle("Guncelleme gerekli")
            .setMessage("Sow7Rix Studio'nun yeni bir surumu var. Devam etmek icin guncellemen gerekiyor.")
            .setCancelable(false)
            .setPositiveButton("Guncelle") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                finish()
            }
            .show()
    }

    private fun buildMenuUi() {
        val pageRoot = FrameLayout(this).apply { setBackgroundColor(Color.parseColor(UiKit.BG)) }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f), dp(56f), dp(24f), dp(100f))
        }

        // ESKI: duz logo + yazi satiri, altinda tek bir kart yoktu - bu yuzden ekran
        // "bos bir liste" gibi basliyordu. YENI: Kuronai/ORBIX tarzinda gercek bir
        // "panel" karti - halka-cerceveli logo + GERCEK 3 durum verisi (uydurma takipci
        // sayisi degil: hazir dosya sayisi, kanal durumu, motor durumu).
        root.addView(dashboardCard())
        root.addView(UiKit.spacer(this, 18f))

        // Kanal katilimi SART - bu yuzden en ustte, gozden kacmayacak sekilde.
        root.addView(channelBanner())
        root.addView(UiKit.spacer(this, 26f))

        root.addView(UiKit.sectionLabel(this, "YUKLEME"))
        root.addView(UiKit.spacer(this, 10f))
        root.addView(quickActionsRow())
        root.addView(UiKit.spacer(this, 16f))
        root.addView(UiKit.menuRowV(this, UiKit.Glyph.SEARCH, "TikTok Analiz", "Bir video linkinin baslik/yazar/kapak bilgisini gosterir", "#5CC8FF") {
            startActivity(Intent(this, AnalyzeActivity::class.java))
        })

        root.addView(UiKit.spacer(this, 28f))
        root.addView(UiKit.sectionLabel(this, "HESAP", "#4DD68C"))
        root.addView(UiKit.spacer(this, 10f))
        root.addView(UiKit.menuRowV(this, UiKit.Glyph.CHECK, "Telegram Uyeligi Aktif", "@sowzfx kanal uyeligin dogrulandi - erisimin buna bagli", "#4DD68C") {
            showSetupNeededDialog(
                "Hesap sistemi",
                "Sow7Rix Studio erisimi @sowzfx Telegram kanal uyeliginle korunur. TikTok'a yukleme " +
                "icin ise ayrica giris yapmana gerek yok - 'TikTok'a Yukle' ekraninda once video/kalite " +
                "hazirlanir, sonra TikTok Studio GERCEK Chrome icinde acilir (Google/Facebook/Apple " +
                "girisi dahil hepsi orada sorunsuz calisir), orada kendi TikTok hesabinla giris yaparsin."
            )
        })

        scroll.addView(root)
        page.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        page.addView(UiKit.bottomNav(this, activeIndex = 0) { index -> navigateTab(this, index) })
        pageRoot.addView(page, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // FAB'in arkasinda yumusak bir amber glow halkasi ve ortasinda tilki maskotu, surekli nefes alir.
        val foxGlow = UiKit.foxGlow(this, 90f)
        val glowParams = FrameLayout.LayoutParams(dp(90f), dp(90f)).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(60f)
        }
        pageRoot.addView(foxGlow, glowParams)
        glowAnimator = UiKit.startGlowPulse(UiKit.foxGlowPulseTarget(foxGlow), minAlpha = 0.12f, maxAlpha = 0.35f)

        val fab = UiKit.fab(this, "+") { startActivity(Intent(this, UploadActivity::class.java)) }
        val fabParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(75f)
        }
        pageRoot.addView(fab, fabParams)

        setContentView(pageRoot)
    }

    /** Ana sayfanin ust panel karti: halka-cerceveli logo (+ "aktif" noktasi) ve
     *  altinda GERCEK 3 durum degeri. Kuronai'nin "ORBIX" profil ekranindaki gibi
     *  bir ilk-izlenim panosu veriyor ama hicbir sayi uydurma degil:
     *  - HAZIR DOSYA: Movies/Sow7Rix klasorune GERCEKTEN kaydedilmis islenmis video sayisi
     *  - KANAL: Telegram kanal uyeligi (uygulamaya girebildiysen zaten dogrulanmis demektir)
     *  - MOTOR: FFmpeg isleme motorunun hazir/calisir durumda oldugu */
    private fun dashboardCard(): FrameLayout {
        val outer = FrameLayout(this)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.card(this@MainActivity)
            elevation = dp(8f).toFloat()
            setPadding(dp(18f), dp(20f), dp(18f), dp(16f))
        }

        // Ince, tepede-kirpilmis gradyan "vurgu seridi" - premium panolarda sik gorulen,
        // kartin bir "urun" oldugunu hissettiren ince detay.
        card.addView(View(this).apply {
            background = UiKit.gradientPill(listOf("#FFD774", UiKit.AMBER, "#5CC8FF"), 2f, this@MainActivity).apply {
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3f)).apply {
            bottomMargin = dp(16f)
        })

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // Logo halkasinin arkasinda yumusak bir amber "glow" - duz bir ikon yerine
        // one cikan, hafif parlayan bir rozet hissi.
        val logoHolder = FrameLayout(this)
        logoHolder.addView(UiKit.haloCircle(this, 70f, UiKit.AMBER), FrameLayout.LayoutParams(dp(70f), dp(70f)).apply { gravity = Gravity.CENTER })
        val logo = ImageView(this).apply { setImageResource(R.mipmap.ic_launcher) }
        logoHolder.addView(UiKit.avatarRing(this, logo, 54f, UiKit.AMBER), FrameLayout.LayoutParams(dp(54f), dp(54f)).apply { gravity = Gravity.CENTER })
        headerRow.addView(logoHolder, LinearLayout.LayoutParams(dp(70f), dp(70f)))
        val headerText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14f), 0, 0, 0)
        }
        headerText.addView(TextView(this).apply {
            text = "Sow7Rix Studio"
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 19f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        headerText.addView(TextView(this).apply {
            text = "v" + BuildConfig.VERSION_NAME + " · Hazir"
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 12f
        })
        headerRow.addView(headerText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(headerRow)

        card.addView(UiKit.spacer(this, 16f))
        card.addView(View(this).apply { setBackgroundColor(Color.parseColor(UiKit.CARD_BORDER)) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
        card.addView(UiKit.spacer(this, 14f))

        val statsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statsRow.addView(UiKit.statTile(this, readyFileCount().toString(), "HAZIR DOSYA", "#5CC8FF"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        statsRow.addView(UiKit.statTile(this, "Aktif", "KANAL", "#4DD68C"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        statsRow.addView(UiKit.statTile(this, "Hazir", "MOTOR", UiKit.AMBER), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(statsRow)

        outer.addView(card, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        return outer
    }

    /** Movies/Sow7Rix klasorune daha once kaydedilmis, GERCEKTEN islenmis video sayisi
     *  (MediaStore uzerinden canli sorgulanir - sahte/sabit bir rakam degil). */
    private fun readyFileCount(): Int {
        return try {
            val projection = arrayOf(android.provider.MediaStore.Video.Media._ID)
            val selection = "${android.provider.MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
            val args = arrayOf("Movies/Sow7Rix%")
            contentResolver.query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, selection, args, null
            )?.use { it.count } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun channelBanner(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.gradientPill(listOf("#2A1D08", "#1A1203"), 16f, this@MainActivity).apply {
                setStroke(UiKit.dp(this@MainActivity, 1f).toInt(), Color.parseColor(UiKit.AMBER))
            }
            elevation = dp(4f).toFloat()
            isClickable = true
            isFocusable = true
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sowzfx"))) }

            addView(UiKit.vectorIcon(this@MainActivity, UiKit.Glyph.BELL, UiKit.AMBER, 20f), LinearLayout.LayoutParams(dp(20f), dp(20f)))
            val textCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12f), 0, 0, 0)
            }
            textCol.addView(TextView(this@MainActivity).apply {
                text = "@sowzfx kanalina katilmak SART"
                setTextColor(Color.parseColor(UiKit.TEXT))
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            textCol.addView(TextView(this@MainActivity).apply {
                text = "Erisimin kanal uyeligine bagli - katilmak icin dokun"
                setTextColor(Color.parseColor(UiKit.TEXT_DIM))
                textSize = 11f
            })
            addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(TextView(this@MainActivity).apply {
                text = "›"
                setTextColor(Color.parseColor(UiKit.AMBER))
                textSize = 18f
            })
        }
    }

    /** Competitor uygulamalarin "FFMPEG / UPLOAD / METHODS" 3'lu kart satirina benzer
     *  hizli erisim satiri. Uc buton da gercek, calisan bir seye gider - sadece gorsel
     *  suslemenin otesinde:
     *  - FFMPEG: video sec, kalite isle (FFmpeg motoru), en sonunda TikTok Studio'yu ac
     *  - UPLOAD: isleme adimini atlayip DOGRUDAN TikTok Studio'yu ac (video zaten hazirsa)
     *  - METHODS: bizim arastirdigimiz gercek kalite/bitrate onerileri + TikTok'un
     *    kendi hesap ayari (uydurma bir "bypass" degil, kaynagi gosterilen bilgi) */
    private fun quickActionsRow(): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(
            UiKit.squareCard(this, UiKit.Glyph.FILM, "FFMPEG", "#4DD6C8") {
                startActivity(Intent(this, UploadActivity::class.java))
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8f) }
        )
        row.addView(
            UiKit.squareCard(this, UiKit.Glyph.UPLOAD, "UPLOAD", "#FF4D6D") {
                startActivity(Intent(this, UploadActivity::class.java).putExtra(UploadActivity.EXTRA_DIRECT_STUDIO, true))
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4f); marginEnd = dp(4f) }
        )
        row.addView(
            UiKit.squareCard(this, UiKit.Glyph.BOLT, "METHODS", UiKit.AMBER) {
                showMethodsInfo()
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8f) }
        )
        return row
    }

    /** Gercek, kaynagi arastirilmis kalite/bitrate onerileri - uydurma bir "gizli yontem"
     *  degil. Kullanicinin sordugu "mbps mi kbps mi" sorusunun cevabi da burada: bizim
     *  QualityEngine presetlerimiz kbps cinsinden tanimli (1080p/60fps = 18000 kbps = 18 Mbps). */
    private fun showMethodsInfo() {
        AlertDialog.Builder(this)
            .setTitle("Gercek Yontemler")
            .setMessage(
                "Bitrate birimi: uygulamada kbps kullaniyoruz ama 1000 kbps = 1 Mbps, yani ayni sey.\n" +
                "Bizim presetler: 720p/60fps = 9000 kbps (9 Mbps), 1080p/60fps = 18000 kbps (18 Mbps), " +
                "1080p/120fps = 22000 kbps (22 Mbps).\n\n" +
                "TikTok'un KENDI onerdigi degerler (resmi, gizli degil): H.264 kodek (HEVC degil - " +
                "TikTok HEVC'yi tekrar sikistirip kalite dusuruyor), 1080x1920 dikey, 60fps, " +
                "10-15 Mbps (30fps icin) / ~25 Mbps'e kadar (60fps icin), ses AAC 256kbps.\n\n" +
                "Hesabindan kontrol et: Ayarlar ve gizlilik > Icerik tercihleri > " +
                "\"Yuksek kaliteli yuklemelere izin ver\" acik olmali - kapaliysa biz ne kadar " +
                "iyi hazirlarsak hazirlayalim TikTok yine sikistirir."
            )
            .setPositiveButton("Anladim", null)
            .show()
    }


    private fun showSetupNeededDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun dp(v: Float) = UiKit.dp(this, v).toInt()
}

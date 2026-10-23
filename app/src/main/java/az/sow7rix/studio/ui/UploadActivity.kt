package az.sow7rix.studio.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import az.sow7rix.studio.core.QualityEngine
import java.io.File
import java.io.FileOutputStream

/**
 * ONCEKI SURUM TikTok'un web sayfasini kendi WebView'imizin icine gomuyordu. Bu artik
 * YAPILMIYOR - sebebi: Google/Facebook/Apple ile giris, guvenlik geregi gomulu bir
 * WebView icinde calismiyor ("disallowed_useragent" - Google bunu 2016'dan beri bilerek
 * engelliyor, sahte pencere uzerinden sifre calinmasin diye). Kullanici hesap secme
 * ekranini hic goremiyor, buton sessizce basa donuyordu.
 *
 * YENI AKIS:
 *   1) Video + kalite TAMAMEN bizim kendi ekranimizda secilir, FFmpeg ile islenir.
 *   2) Islenmis dosya cihazin genel Video klasorune (MediaStore - Movies/Sow7Rix)
 *      kaydedilir, boylece HERHANGI bir uygulamanin dosya secicisinden gorulebilir.
 *   3) TikTok Studio, bizim marka renklerimizle temalanmis GERCEK bir Chrome penceresi
 *      (Custom Tab) icinde acilir - bu sayede Google/Facebook/Apple girisi GERCEKTEN
 *      calisir (gercek Chrome oldugu icin Google'in guvenlik kontrolune takilmaz),
 *      kullanici orada kaydedilen dosyayi kendi dosya secicisinden secer.
 */
class UploadActivity : Activity() {

    companion object {
        /** true ise: kalite isleme adimini atla, dogrudan TikTok Studio'yu ac -
         *  kullanicinin zaten hazir bir videosu varsa, FFmpeg'e hic gerek yok. */
        const val EXTRA_DIRECT_STUDIO = "direct_studio"
    }

    private val REQUEST_PICK_VIDEO = 4201

    private lateinit var rootLayout: FrameLayout
    private lateinit var pickScreen: LinearLayout
    private lateinit var doneScreen: LinearLayout
    private lateinit var doneFileLabel: TextView
    private lateinit var overlay: LinearLayout
    private lateinit var overlayStatus: TextView
    private lateinit var overlayPercent: TextView
    private lateinit var presetCard: LinearLayout
    private lateinit var presetButtons: LinearLayout
    private lateinit var overlayGlow: FrameLayout
    private lateinit var launchSplash: LinearLayout
    private lateinit var drawer: LinearLayout
    private lateinit var drawerScrim: View
    private lateinit var drawerItems: LinearLayout

    private var overlayGlowAnimator: ValueAnimator? = null
    private var lastPickedUri: Uri? = null
    private var lastSavedUri: Uri? = null
    private var drawerOpen = false
    /** Su an calisan GERCEK FFmpeg surecinin referansi - "Iptal Et" butonu bunu
     *  QualityEngine.cancel() ile gercekten durdurur. skipReencode (kopyalama) modunda
     *  null kalir, cunku o modda iptal edilecek bir FFmpeg sureci yoktur. */
    private var currentSession: com.arthenica.ffmpegkit.FFmpegSession? = null
    private var currentInputFile: File? = null
    private var currentOutputFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootLayout = FrameLayout(this).apply { setBackgroundColor(Color.parseColor(UiKit.BG)) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(buildToolbar())
        content.addView(buildPickScreen(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        rootLayout.addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        rootLayout.addView(buildDoneScreen(), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        rootLayout.addView(buildOverlay(), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        rootLayout.addView(buildPresetCard(), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        rootLayout.addView(buildLaunchSplash(), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        buildDrawer(rootLayout)

        setContentView(rootLayout)

        if (intent?.getBooleanExtra(EXTRA_DIRECT_STUDIO, false) == true) {
            launchStudio()
        }
    }

    private fun buildToolbar(): LinearLayout {
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            setPadding(dp(16f), dp(40f), dp(16f), dp(14f))
        }
        toolbar.addView(TextView(this).apply {
            text = "‹"
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 26f
            setPadding(dp(4f), 0, dp(16f), 0)
            isClickable = true
            setOnClickListener { finish() }
        })
        toolbar.addView(TextView(this).apply {
            text = "TikTok'a Yukle"
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        toolbar.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
            setOnClickListener { openDrawer() }
            addView(UiKit.vectorIcon(this@UploadActivity, UiKit.Glyph.MENU, UiKit.AMBER, 20f))
        })
        return toolbar
    }

    /** Ilk ekran: once video secilir, sonra kalite sorulur. Competitor uygulamalarin
     *  "Yuklemek icin video secin" kutusuna benzer, sade ve net bir giris noktasi. */
    private fun buildPickScreen(): LinearLayout {
        pickScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32f), dp(32f), dp(32f), dp(32f))
        }
        val iconBadge = UiKit.iconBadgeV(this, UiKit.Glyph.UPLOAD, UiKit.AMBER, 64f)
        pickScreen.addView(iconBadge)
        pickScreen.addView(UiKit.spacer(this, 18f))
        pickScreen.addView(TextView(this).apply {
            text = "Yuklemek icin video sec"
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        pickScreen.addView(UiKit.spacer(this, 6f))
        pickScreen.addView(TextView(this).apply {
            text = "Once kalite isleyelim, sonra TikTok Studio'ya gonderelim"
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 13f
            gravity = Gravity.CENTER
        })
        pickScreen.addView(UiKit.spacer(this, 24f))

        val pickButton = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor(UiKit.AMBER))
                cornerRadius = dp(14f).toFloat()
            }
            setPadding(dp(32f), dp(16f), dp(32f), dp(16f))
            setOnClickListener { pickVideo() }
            addView(TextView(this@UploadActivity).apply {
                text = "Video Sec"
                setTextColor(Color.parseColor("#0A0A0A"))
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
        pickScreen.addView(pickButton)
        return pickScreen
    }

    /** Islem bittikten sonraki ekran: dosya nereye kaydedildi, TikTok Studio'yu ac. */
    private fun buildDoneScreen(): LinearLayout {
        doneScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(UiKit.BG))
            visibility = View.GONE
            setPadding(dp(32f), dp(32f), dp(32f), dp(32f))
        }
        doneScreen.addView(UiKit.vectorIcon(this, UiKit.Glyph.CHECK, "#4DD68C", 48f))
        doneScreen.addView(UiKit.spacer(this, 16f))
        doneScreen.addView(TextView(this).apply {
            text = "Dosya hazir"
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        doneScreen.addView(UiKit.spacer(this, 6f))
        doneFileLabel = TextView(this).apply {
            text = "Video klasorune (Movies/Sow7Rix) kaydedildi"
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 13f
            gravity = Gravity.CENTER
        }
        doneScreen.addView(doneFileLabel)
        doneScreen.addView(UiKit.spacer(this, 28f))

        val openButton = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor(UiKit.AMBER))
                cornerRadius = dp(14f).toFloat()
            }
            setPadding(dp(32f), dp(16f), dp(32f), dp(16f))
            setOnClickListener { launchStudio() }
            addView(TextView(this@UploadActivity).apply {
                text = "TikTok Studio'yu Ac"
                setTextColor(Color.parseColor("#0A0A0A"))
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
        doneScreen.addView(openButton)
        doneScreen.addView(UiKit.spacer(this, 14f))
        doneScreen.addView(TextView(this).apply {
            text = "Orada video sec'e bas, Movies/Sow7Rix klasorunden az once islenen dosyayi sec"
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 11f
            gravity = Gravity.CENTER
        })
        doneScreen.addView(UiKit.spacer(this, 18f))

        // Dogrudan paylasim - TikTok Studio'ya aktarimdan AYRI bir secenek (bkz.
        // shareDirectlyToTikTok() aciklamasi). Bu bir "ikincil" buton - ana akis
        // yine TikTok Studio uzerinden.
        val shareButton = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = UiKit.secondaryButtonBg(this@UploadActivity)
            setPadding(dp(28f), dp(14f), dp(28f), dp(14f))
            setOnClickListener { lastSavedUri?.let { shareDirectlyToTikTok(it) } }
            addView(UiKit.vectorIcon(this@UploadActivity, UiKit.Glyph.UPLOAD, UiKit.TEXT, 16f), LinearLayout.LayoutParams(dp(16f), dp(16f)).apply { marginEnd = dp(8f) })
            addView(TextView(this@UploadActivity).apply {
                text = "Dogrudan TikTok'a Paylas"
                setTextColor(Color.parseColor(UiKit.TEXT))
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
        doneScreen.addView(shareButton)
        doneScreen.addView(UiKit.spacer(this, 20f))
        doneScreen.addView(TextView(this).apply {
            text = "Baska video isle"
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 13f
            gravity = Gravity.CENTER
            isClickable = true
            setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
            setOnClickListener { showPickScreen() }
        })
        return doneScreen
    }

    /** Studio acilmadan once gosterilen kisa gecis ekrani - competitor uygulamalarin
     *  "OPENING STUDIO..." ekranina benzer, gercek Chrome acilana kadar bosluk hissi vermez. */
    private fun buildLaunchSplash(): LinearLayout {
        launchSplash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(UiKit.BG))
            visibility = View.GONE
        }
        launchSplash.addView(ProgressBar(this).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(UiKit.AMBER))
        })
        launchSplash.addView(UiKit.spacer(this, 14f))
        launchSplash.addView(TextView(this).apply {
            text = "STUDIO ACILIYOR..."
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 13f
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
        })
        return launchSplash
    }

    private fun showPickScreen() {
        doneScreen.visibility = View.GONE
        pickScreen.visibility = View.VISIBLE
    }

    /** Once Android'in modern Fotograf/Video Secici'sini (MediaStore.ACTION_PICK_IMAGES,
     *  API 33+ yerel, eski cihazlarda Play Servisleri ile geri-tasinmis) dener - bu,
     *  "Videolar/Koleksiyonlar" izgarali, "sadece sectigin dosyalara erisim" notlu modern
     *  arayuz. Cihaz/Chrome bunu desteklemiyorsa (resolveActivity bos donerse) hic
     *  denemeden eski, her zaman calisan sistem dosya secicisine duser - cokme riski yok. */
    private fun pickVideo() {
        val modernIntent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            type = "video/*"
        }
        val canUseModernPicker = modernIntent.resolveActivity(packageManager) != null
        if (canUseModernPicker) {
            try {
                startActivityForResult(modernIntent, REQUEST_PICK_VIDEO)
                return
            } catch (_: Exception) {
                // Beklenmedik sekilde acilamadiysa eski secicye duser, asagida devam eder.
            }
        }
        val legacyIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(legacyIntent, "Video sec"), REQUEST_PICK_VIDEO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_VIDEO) return
        val uri = if (resultCode == Activity.RESULT_OK) data?.data else null
        if (uri == null) return
        checkSpecThenPick(uri)
    }

    /** TikTok'un kendi resmi API spesifikasyonuna (4GB, 360-4096px, sure siniri) gore
     *  hizli bir on-kontrol - video acikca bu sinirlarin disindaysa kullaniciyi uyarir,
     *  ama yine de devam etmesine izin verir (kesin hesap siniri API'den gelir, biz
     *  sadece bariz asirilik durumunda haber veririz). */
    private fun checkSpecThenPick(uri: Uri) {
        val warnings = mutableListOf<String>()
        try {
            val sizeBytes = contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            if (sizeBytes > 4L * 1024 * 1024 * 1024) {
                warnings.add("Dosya boyutu 4GB'ı aşıyor olabilir (TikTok'un üst sınırı 4GB)")
            }
        } catch (_: Exception) { /* bilinmiyor, atla */ }

        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(this, uri)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                if (durationMs > 10 * 60 * 1000L) {
                    warnings.add("Video 10 dakikadan uzun - TikTok hesabına göre bu süre kabul edilmeyebilir")
                }
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                if ((w in 1..359) || (h in 1..359)) {
                    warnings.add("Çözünürlük çok düşük (TikTok en az 360px bekliyor)")
                }
            }
        } catch (_: Exception) { /* bilinmiyor, atla */ }

        if (warnings.isEmpty()) {
            showPresetPicker(uri)
        } else {
            AlertDialog.Builder(this)
                .setTitle("Dikkat")
                .setMessage(warnings.joinToString("\n\n") + "\n\nYine de devam edebilirsin, ama TikTok bunu reddedebilir.")
                .setPositiveButton("Devam Et") { _, _ -> showPresetPicker(uri) }
                .setNegativeButton("Vazgec", null)
                .show()
        }
    }

    /** TikTok Studio'yu bottom-sheet Custom Tab olarak acar - gercek acilis mantigi
     *  artik core/StudioLauncher.kt'de (birden fazla ekran - bu ve HistoryActivity -
     *  AYNI, tek-dogrulanmis kodu paylassin diye oraya cikarildi). Burada sadece
     *  gecis splash'ini gosterip StudioLauncher'i cagiriyoruz. */
    private fun launchStudio() {
        launchSplash.visibility = View.VISIBLE
        rootLayout.postDelayed({
            launchSplash.visibility = View.GONE
            az.sow7rix.studio.core.StudioLauncher.open(this, UiKit.BG, UiKit.CARD)
        }, 450L)
    }

    /** Dogrudan TikTok uygulamasina paylasim - TikTok Studio'ya aktarimdan FARKLI bir
     *  akis (talimat Bolum 11 ayrimi): burada TikTok'un yukleme sayfasini ACMIYORUZ,
     *  Android'in standart ACTION_SEND Sharesheet'i uzerinden dosyayi TikTok'a (varsa)
     *  veya kullanicinin sececegi herhangi bir uygulamaya "paylasiyoruz". Bu, TikTok'un
     *  kendi paylasim hedefi secenegini kullanir - bizim tarafimizdan "otomatik
     *  yuklendi" gibi bir iddia YOKTUR; kullanici TikTok tarafinda kendi akisini
     *  tamamlar (ozel/genel secimi dahil, kendi kontrolunde). */
    private fun shareDirectlyToTikTok(uri: Uri) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // Once dogrudan TikTok'un kendi paylasim hedefine gitmeyi dene (paket adi
            // TikTok'un resmi Android paketidir - com.zhiliaoapp.musically). TikTok
            // yuklu degilse veya bu paket beklenmedik sekilde calismazsa, pakete
            // ZORLAMADAN genel Android paylasim secicisine (Sharesheet) dus - boylece
            // kullanici en kotu ihtimalle kendi sececegi bir uygulamaya paylasabilir.
            val tiktokIntent = Intent(sendIntent).setPackage("com.zhiliaoapp.musically")
            if (tiktokIntent.resolveActivity(packageManager) != null) {
                startActivity(tiktokIntent)
            } else {
                startActivity(Intent.createChooser(sendIntent, "Paylas"))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Paylasim acilamadi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Kenardan acilip kapanan site ici menu: ayarlar, onbellek, kaliteyi degistir. */
    private fun buildDrawer(root: FrameLayout) {
        drawerScrim = View(this).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            visibility = View.GONE
            isClickable = true
            setOnClickListener { closeDrawer() }
        }
        root.addView(drawerScrim, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.CARD))
            setPadding(dp(20f), dp(52f), dp(20f), dp(24f))
            elevation = dp(16f).toFloat()
            translationX = dp(280f).toFloat()
        }
        val drawerHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        drawerHeader.addView(TextView(this).apply {
            text = "Menu"
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        drawerHeader.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            isClickable = true
            setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
            setOnClickListener { closeDrawer() }
            addView(UiKit.vectorIcon(this@UploadActivity, UiKit.Glyph.CLOSE, UiKit.TEXT_DIM, 16f))
        })
        drawer.addView(drawerHeader)
        drawer.addView(UiKit.spacer(this, 18f))
        drawerItems = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        drawer.addView(drawerItems)
        root.addView(drawer, FrameLayout.LayoutParams(dp(260f), FrameLayout.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.END })
    }

    private fun refreshDrawerItems() {
        drawerItems.removeAllViews()
        drawerItems.addView(UiKit.menuRowV(this, UiKit.Glyph.CHECK, "TikTok Hesap Ayarini Kontrol Et", "'Yuksek kaliteli yuklemelere izin ver' acik mi?", "#4DD68C") {
            closeDrawer()
            showHighQualityInfo()
        })
        drawerItems.addView(UiKit.spacer(this, 10f))
        if (lastPickedUri != null) {
            drawerItems.addView(UiKit.menuRowV(this, UiKit.Glyph.SLIDER, "Kaliteyi Yeniden Sec", "Ayni videoyu baska bir kalite ile tekrar isle", UiKit.AMBER) {
                closeDrawer()
                showPresetPicker(lastPickedUri!!)
            })
            drawerItems.addView(UiKit.spacer(this, 10f))
        }
        drawerItems.addView(UiKit.menuRowV(this, UiKit.Glyph.SETTINGS, "Ayarlar", "Kanal, guncelleme, cikis", "#5CC8FF") {
            closeDrawer()
            startActivity(Intent(this, SettingsActivity::class.java))
        })
        drawerItems.addView(UiKit.spacer(this, 10f))
        drawerItems.addView(UiKit.menuRowV(this, UiKit.Glyph.TRASH, "Onbellegi Temizle", "Islenen gecici video dosyalarini sil", UiKit.TEXT_DIM) {
            val files = cacheDir.listFiles() ?: emptyArray()
            var count = 0
            files.forEach { if (it.name.startsWith("sow7rix_")) { it.delete(); count++ } }
            Toast.makeText(this, "$count gecici dosya silindi.", Toast.LENGTH_SHORT).show()
            closeDrawer()
        })
    }

    /** TikTok'un kendi hesap ayarindaki, cogu kullanicinin bilmedigi gercek kalite
     *  anahtarini hatirlatir. Bu bizim app'imizin kontrol edebilecegi bir sey degil -
     *  TikTok'un sunucu tarafinda, hesaba bagli bir ayar - ama bilmeyenin videosu
     *  gereksiz yere daha fazla sikistirilir. */
    private fun showHighQualityInfo() {
        AlertDialog.Builder(this)
            .setTitle("Gercek kalite anahtari burada")
            .setMessage(
                "TikTok'un kendi hesabinda, cogu kisinin bilmedigi bir ayar var: " +
                "\"Yuksek kaliteli yuklemelere izin ver\" (bazi surumlerde \"Upload HD\").\n\n" +
                "Nerede: TikTok uygulamasi > Profil > ≡ Menu > Ayarlar ve gizlilik > " +
                "Icerik tercihleri.\n\n" +
                "Bu kapaliysa, biz videoyu ne kadar iyi hazirlarsak hazirlayalim TikTok " +
                "kendi tarafinda yine agresif sikistirir."
            )
            .setPositiveButton("Anladim", null)
            .show()
    }

    private fun openDrawer() {
        refreshDrawerItems()
        drawerScrim.visibility = View.VISIBLE
        drawerScrim.alpha = 0f
        drawerScrim.animate().alpha(1f).setDuration(200).start()
        drawer.animate().translationX(0f).setDuration(220).start()
        drawerOpen = true
    }

    private fun closeDrawer() {
        drawerScrim.animate().alpha(0f).setDuration(200).withEndAction { drawerScrim.visibility = View.GONE }.start()
        drawer.animate().translationX(dp(280f).toFloat()).setDuration(220).start()
        drawerOpen = false
    }

    override fun onBackPressed() {
        if (drawerOpen) { closeDrawer(); return }
        super.onBackPressed()
    }

    /** Kalite motoru calisirken gorunen tam ekran katman. */
    private fun buildOverlay(): LinearLayout {
        overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(UiKit.BG))
            visibility = View.GONE
            setPadding(dp(32f), dp(32f), dp(32f), dp(32f))
        }
        overlayGlow = UiKit.foxGlow(this, 70f)
        overlay.addView(overlayGlow, LinearLayout.LayoutParams(dp(70f), dp(70f)))
        overlay.addView(UiKit.spacer(this, 14f))
        overlay.addView(TextView(this).apply {
            text = "Kalite Motoru"
            setTextColor(Color.parseColor(UiKit.AMBER))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        overlay.addView(UiKit.spacer(this, 10f))
        overlayStatus = TextView(this).apply {
            text = "Video hazirlaniyor..."
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        overlay.addView(overlayStatus)
        overlay.addView(UiKit.spacer(this, 14f))
        overlayPercent = TextView(this).apply {
            text = "0%"
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        overlay.addView(overlayPercent)
        overlay.addView(UiKit.spacer(this, 24f))
        overlay.addView(TextView(this).apply {
            text = "Iptal Et"
            setTextColor(Color.parseColor(UiKit.DANGER))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(20f), dp(10f), dp(20f), dp(10f))
            isClickable = true
            isFocusable = true
            setOnClickListener { cancelProcessing() }
        })
        return overlay
    }

    /** Kullanici "Iptal Et"e bastiginda GERCEKTEN calisan FFmpeg surecini durdurur
     *  (QualityEngine.cancel -> FFmpegKit.cancel(sessionId)) - sahte bir iptal degil.
     *  skipReencode modunda currentSession zaten null'dur (o modda FFmpeg hic
     *  calismiyor, senkron dosya kopyalama Thread'i zaten kisa surede biter). */
    private fun cancelProcessing() {
        QualityEngine.cancel(currentSession)
        currentSession = null
    }

    /** Iptal edilen veya basarisiz olan bir islemden kalan GECICI giris/cikis dosyalarini
     *  siler - talimatin istedigi "gereksiz gecici dosya birikmesin" ilkesi (Bolum 12). */
    private fun cleanupTempFiles() {
        try { currentInputFile?.delete() } catch (_: Exception) { }
        try { currentOutputFile?.delete() } catch (_: Exception) { }
        currentInputFile = null
        currentOutputFile = null
    }

    /** Video secildikten hemen sonraki kalite secim karti. */
    private fun buildPresetCard(): LinearLayout {
        presetCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(UiKit.BG))
            visibility = View.GONE
            setPadding(dp(28f), dp(28f), dp(28f), dp(28f))
        }
        presetCard.addView(TextView(this).apply {
            text = "Kalite Sec"
            setTextColor(Color.parseColor(UiKit.AMBER))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        presetCard.addView(UiKit.spacer(this, 4f))
        presetCard.addView(TextView(this).apply {
            text = "TikTok Studio'ya gitmeden once videonun isleneceği kalite"
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 12f
            gravity = Gravity.CENTER
        })
        presetCard.addView(UiKit.spacer(this, 20f))
        presetButtons = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        presetCard.addView(presetButtons, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        return presetCard
    }

    private fun showPresetPicker(uri: Uri) {
        lastPickedUri = uri
        pickScreen.visibility = View.GONE
        doneScreen.visibility = View.GONE
        presetButtons.removeAllViews()
        QualityEngine.PRESETS.forEachIndexed { index, preset ->
            val caption = when {
                preset.skipReencode -> "Hic sikistirma yok, dosya oldugu gibi gider (en buyuk)"
                preset.crf > 0 -> "Cozunurluk/fps aynen kalir, dosya kucculur, goz fark etmez"
                else -> "${preset.videoBitrateKbps / 1000} Mbps hedef bitrate"
            }
            val accent = if (preset.skipReencode) "#9A96A8" else UiKit.AMBER
            val row = UiKit.menuRow(this, (index + 1).toString(), preset.label, caption, accent) {
                presetCard.visibility = View.GONE
                startProcessing(uri, preset)
            }
            presetButtons.addView(row)
            presetButtons.addView(UiKit.spacer(this, 10f))
        }
        presetCard.visibility = View.VISIBLE
    }

    private fun startProcessing(uri: Uri, preset: QualityEngine.Settings) {
        overlay.visibility = View.VISIBLE
        overlayStatus.text = "Video hazirlaniyor..."
        overlayPercent.text = "0%"
        overlayGlowAnimator?.cancel()
        overlayGlowAnimator = UiKit.startGlowPulse(UiKit.foxGlowPulseTarget(overlayGlow), minAlpha = 0.3f, maxAlpha = 1f, durationMs = 700L)

        Thread {
            try {
                val inputFile = File(cacheDir, "sow7rix_in_${System.currentTimeMillis()}.mp4")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(inputFile).use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Video okunamadi")

                var durationMs = 0L
                var srcWidth = 0
                var srcHeight = 0
                var srcFps = 0.0
                try {
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(inputFile.absolutePath)
                        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        srcWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                        srcHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                        // METADATA_KEY_CAPTURE_FRAMERATE her cihaz/kaynakta dolu gelmeyebilir -
                        // bos/parse edilemezse 0.0 kalir, bu da QualityEngine'de "bilinmiyor"
                        // olarak ele alinip optimizasyonu GUVENLI sekilde devre disi birakir.
                        srcFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toDoubleOrNull() ?: 0.0
                    }
                } catch (_: Exception) { }

                val outputFile = File(cacheDir, "sow7rix_out_${System.currentTimeMillis()}.mp4")
                currentInputFile = inputFile
                currentOutputFile = outputFile

                runOnUiThread { overlayStatus.text = preset.label + " isleniyor..." }

                val session = QualityEngine.process(
                    inputPath = inputFile.absolutePath,
                    outputPath = outputFile.absolutePath,
                    settings = preset,
                    durationMsHint = durationMs,
                    sourceWidth = srcWidth,
                    sourceHeight = srcHeight,
                    sourceFps = srcFps
                ) { progress ->
                    runOnUiThread {
                        when (progress) {
                            is QualityEngine.Progress.Running -> {
                                overlayPercent.text = "${progress.percent}%"
                            }
                            is QualityEngine.Progress.Done -> {
                                currentSession = null
                                finishProcessing(progress.outputFile)
                            }
                            is QualityEngine.Progress.Failed -> {
                                currentSession = null
                                overlayGlowAnimator?.cancel()
                                overlay.visibility = View.GONE
                                Toast.makeText(this, progress.reason, Toast.LENGTH_LONG).show()
                                cleanupTempFiles()
                                showPickScreen()
                            }
                            is QualityEngine.Progress.Cancelled -> {
                                currentSession = null
                                overlayGlowAnimator?.cancel()
                                overlay.visibility = View.GONE
                                Toast.makeText(this, "Islem iptal edildi.", Toast.LENGTH_SHORT).show()
                                // Iptal edilince cikan YARIM/bozuk ciktiyi sakla - kaynak
                                // dosya zaten her zaman korunuyordu (talimat Bolum 8/17'nin
                                // "iptal edilen islemde kaynagi koru" ilkesi).
                                cleanupTempFiles()
                                showPickScreen()
                            }
                        }
                    }
                }
                currentSession = session
            } catch (e: Exception) {
                runOnUiThread {
                    overlayGlowAnimator?.cancel()
                    overlay.visibility = View.GONE
                    Toast.makeText(this, "Isleme basarisiz: ${e.message}", Toast.LENGTH_LONG).show()
                    showPickScreen()
                }
            }
        }.start()
    }

    /** Islenmis dosyayi cihazin genel Video klasorune (MediaStore) kaydeder - boylece
     *  TikTok Studio Chrome'da acildiginda kendi dosya secicisinden gorulebilir.
     *  App'in ozel, gorunmez onbellek klasorunde birakmak islevsiz olurdu - hicbir
     *  tarayici orayi goremez. */
    private fun finishProcessing(outputFile: File) {
        Thread {
            val displayName = "Sow7Rix_${System.currentTimeMillis()}.mp4"
            val savedUri = try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Sow7Rix")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val itemUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (itemUri != null) {
                    contentResolver.openOutputStream(itemUri)?.use { out ->
                        outputFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    contentResolver.update(itemUri, values, null, null)
                }
                itemUri
            } catch (e: Exception) {
                null
            }
            outputFile.delete()
            runOnUiThread {
                overlayGlowAnimator?.cancel()
                overlay.visibility = View.GONE
                if (savedUri != null) {
                    lastSavedUri = savedUri
                    doneFileLabel.text = "Movies/Sow7Rix/$displayName olarak kaydedildi"
                    doneScreen.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Dosya kaydedilemedi, tekrar dene.", Toast.LENGTH_LONG).show()
                    showPickScreen()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayGlowAnimator?.cancel()
    }

    private fun dp(v: Float) = UiKit.dp(this, v).toInt()
}

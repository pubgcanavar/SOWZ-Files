package az.sow7rix.studio.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import az.sow7rix.studio.BuildConfig
import az.sow7rix.studio.R
import az.sow7rix.studio.core.UpdateCheck
import kotlin.concurrent.thread

/** SOWZ'a özel video çalışma alanı; yerel göstergeler cihazdan okunur. */
class MainActivity : Activity() {
    private var readyCountValue: TextView? = null
    private val entries = mutableListOf<View>()

    companion object {
        fun navigateTab(from: Activity, index: Int) {
            if (index == 4) {
                from.startActivity(Intent(from,UploadActivity::class.java))
                from.overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
                return
            }
            val target: Class<out Activity> = when(index) {
                1 -> HistoryActivity::class.java
                2 -> ProfileActivity::class.java
                3 -> SettingsActivity::class.java
                else -> MainActivity::class.java
            }
            if (from.javaClass == target) return
            from.startActivity(Intent(from,target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            from.finish()
            from.overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.parseColor(UiKit.BG)
        window.navigationBarColor=Color.parseColor(UiKit.BG)
        buildHome()
        checkUpdate()
    }

    override fun onResume() {
        super.onResume()
        readyCountValue?.text=readyFileCount().toString()
    }

    private fun buildHome() {
        entries.clear()
        val page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor(UiKit.BG)) }
        val scroll=ScrollView(this).apply { clipToPadding=false }
        val content=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(20f),dp(22f),dp(20f),dp(26f))
        }
        val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        header.addView(ImageView(this).apply { setImageResource(R.drawable.ic_launcher) },LinearLayout.LayoutParams(dp(47f),dp(47f)))
        val brand=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(11f),0,0,0) }
        brand.addView(TextView(this).apply {
            text="SOWZ STUDIO"; textSize=15f; letterSpacing=0.10f; typeface=Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(UiKit.TEXT))
        })
        brand.addView(TextView(this).apply {
            text="VIDEO WORKSPACE · "+BuildConfig.VERSION_NAME; textSize=10f; letterSpacing=0.04f
            setTextColor(Color.parseColor(UiKit.TEXT_DIM)); setPadding(0,dp(3f),0,0)
        })
        header.addView(brand,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        val gear=FrameLayout(this).apply {
            background=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(Color.parseColor("#141B25")); setStroke(dp(1f),Color.parseColor(UiKit.CARD_BORDER)) }
            contentDescription="Ayarlar"; isClickable=true; isFocusable=true
            addView(UiKit.vectorIcon(this@MainActivity,UiKit.Glyph.SETTINGS,UiKit.TEXT,22f),
                FrameLayout.LayoutParams(dp(24f),dp(24f),Gravity.CENTER))
            setOnClickListener { navigateTab(this@MainActivity,3) }
        }
        press(gear)
        header.addView(gear,LinearLayout.LayoutParams(dp(46f),dp(46f)))
        content.addView(header); entries.add(header); content.addView(UiKit.spacer(this,20f))

        val hero=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#192A36"),Color.parseColor("#10151E"),Color.parseColor("#10131A"))).apply {
                cornerRadius=dp(24f).toFloat(); setStroke(dp(1f),Color.parseColor("#2C444F"))
            }
            elevation=dp(2f).toFloat(); setPadding(dp(18f),dp(18f),dp(18f),dp(17f))
        }
        val status=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        status.addView(View(this).apply {
            background=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(Color.parseColor("#4DD68C")) }
        },LinearLayout.LayoutParams(dp(7f),dp(7f)))
        status.addView(TextView(this).apply {
            text="  WORKSPACE HAZIR"; textSize=10f; letterSpacing=0.13f
            typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#69DFA7"))
        })
        status.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
        status.addView(TextView(this).apply { text="SOWZ / 01"; textSize=9f; letterSpacing=0.12f; setTextColor(Color.parseColor("#8095A8")) })
        hero.addView(status); hero.addView(UiKit.spacer(this,18f))
        val titleRow=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        val titleCol=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        titleCol.addView(TextView(this).apply {
            text="Hazırla.\nRender et. Paylaş."; textSize=27f; setLineSpacing(dp(1f).toFloat(),1f)
            setTextColor(Color.parseColor(UiKit.TEXT)); typeface=Typeface.create("sans-serif",Typeface.BOLD)
        })
        titleCol.addView(TextView(this).apply {
            text="Tüm üretim araçların tek yerde."; textSize=12f; setTextColor(Color.parseColor(UiKit.TEXT_DIM)); setPadding(0,dp(8f),0,0)
        })
        titleRow.addView(titleCol,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        titleRow.addView(ImageView(this).apply { setImageResource(R.drawable.ic_launcher) },LinearLayout.LayoutParams(dp(74f),dp(74f)))
        hero.addView(titleRow); hero.addView(UiKit.spacer(this,18f))
        hero.addView(View(this).apply { setBackgroundColor(Color.parseColor("#2A3946")) },LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(1f)))
        hero.addView(UiKit.spacer(this,13f))
        val statRow=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        statRow.addView(statCell(readyFileCount().toString(),"RENDER","#68CFFF"),LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        statRow.addView(statCell("H.264","KODEK",UiKit.AMBER),LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        statRow.addView(statCell("AAC","SES","#B6A0FF"),LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        hero.addView(statRow)
        content.addView(hero); entries.add(hero); content.addView(UiKit.spacer(this,23f))

        content.addView(UiKit.sectionLabel(this,"ÇALIŞMA ALANI","#50D7D1")); content.addView(UiKit.spacer(this,11f))
        val render=primaryAction("Yeni render oluştur","Video seç · kaliteyi belirle · dışa aktar",UiKit.Glyph.FILM,"#50D7D1") {
            startActivity(Intent(this,UploadActivity::class.java))
        }
        content.addView(render); entries.add(render); content.addView(UiKit.spacer(this,11f))
        val toolRow=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        val cards=listOf(
            UiKit.squareCard(this,UiKit.Glyph.UPLOAD,"TIKTOK STUDIO","#FF5D7D") {
                startActivity(Intent(this,UploadActivity::class.java).putExtra(UploadActivity.EXTRA_DIRECT_STUDIO,true))
            },
            UiKit.squareCard(this,UiKit.Glyph.SEARCH,"VİDEO ANALİZ","#70BFFF") { startActivity(Intent(this,AnalyzeActivity::class.java)) },
            UiKit.squareCard(this,UiKit.Glyph.BOLT,"CLAUDE AI","#B6A0FF") { startActivity(Intent(this,ClaudeActivity::class.java)) }
        )
        cards.forEachIndexed { i,v ->
            press(v)
            toolRow.addView(v,LinearLayout.LayoutParams(0,dp(124f),1f).apply {
                when(i) { 0 -> marginEnd=dp(6f); 1 -> { marginStart=dp(3f); marginEnd=dp(3f) }; else -> marginStart=dp(6f) }
            })
        }
        content.addView(toolRow); entries.add(toolRow); content.addView(UiKit.spacer(this,24f))
        content.addView(UiKit.sectionLabel(this,"KÜTÜPHANE & HESAP",UiKit.AMBER)); content.addView(UiKit.spacer(this,10f))
        addTool(content,UiKit.Glyph.HISTORY,"Render geçmişi","Kaydedilmiş videolarını görüntüle","#50D7D1") { navigateTab(this,1) }
        addTool(content,UiKit.Glyph.FOX,"Profil ve TikTok oturumu","Giriş ve tarayıcı bağlantısını yönet","#70BFFF") { navigateTab(this,2) }
        addTool(content,UiKit.Glyph.SLIDER,"Render ayarları ve yöntemler","Hız, kalite, FPS ve çıktı boyutu",UiKit.AMBER) { showRenderInfo() }
        content.addView(UiKit.spacer(this,18f))
        content.addView(TextView(this).apply {
            text="SOWZ STUDIO · "+BuildConfig.VERSION_NAME; textSize=10f; letterSpacing=0.14f
            setTextColor(Color.parseColor(UiKit.TEXT_DIM)); gravity=Gravity.CENTER
        })
        scroll.addView(content)
        page.addView(scroll,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f))
        page.addView(UiKit.bottomNav(this,activeIndex=0) { navigateTab(this,it) })
        setContentView(page)
        entries.forEachIndexed { i,v -> animateIn(v,i*38L) }
    }

    private fun addTool(root:LinearLayout,glyph:UiKit.Glyph,title:String,caption:String,accent:String,click:()->Unit) {
        root.addView(UiKit.menuRowV(this,glyph,title,caption,accent,click))
        root.addView(UiKit.spacer(this,10f))
    }
    private fun statCell(value:String,label:String,accent:String)=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER
        addView(TextView(this@MainActivity).apply {
            text=value; textSize=16f; setTextColor(Color.parseColor(accent)); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER
            if(label=="RENDER") readyCountValue=this
        })
        addView(TextView(this@MainActivity).apply {
            text=label; textSize=9f; letterSpacing=0.12f; setTextColor(Color.parseColor(UiKit.TEXT_DIM)); gravity=Gravity.CENTER
            setPadding(0,dp(4f),0,0)
        })
    }
    private fun primaryAction(title:String,caption:String,glyph:UiKit.Glyph,accent:String,onClick:()->Unit):LinearLayout {
        val row=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL
            background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.parseColor("#183B45"),Color.parseColor("#142631"))).apply {
                cornerRadius=dp(18f).toFloat(); setStroke(dp(1f),Color.parseColor("#32616B"))
            }
            setPadding(dp(14f),dp(14f),dp(14f),dp(14f)); isClickable=true; isFocusable=true; setOnClickListener { onClick() }
        }
        row.addView(UiKit.ringIconBadge(this,glyph,accent,48f),LinearLayout.LayoutParams(dp(48f),dp(48f)).apply { marginEnd=dp(13f) })
        val words=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        words.addView(TextView(this).apply { text=title; textSize=15f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor(UiKit.TEXT)) })
        words.addView(TextView(this).apply { text=caption; textSize=11f; setTextColor(Color.parseColor(UiKit.TEXT_DIM)); setPadding(0,dp(4f),0,0) })
        row.addView(words,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        row.addView(TextView(this).apply { text="›"; textSize=28f; setTextColor(Color.parseColor(accent)) })
        press(row); return row
    }
    private fun showRenderInfo() {
        AlertDialog.Builder(this).setTitle("Render modları").setMessage(
            "ORİJİNAL DOSYA\nYeniden kodlamadan kopyalar; en hızlı seçenektir.\n\n"+
            "HIZLI RENDER\nDaha hızlı kodlama; kaynak çözünürlüğü ve FPS korunur. Dosya daha büyük olabilir.\n\n"+
            "YÜKSEK KALİTE\nDaha düşük CRF ayrıntıyı korur; süre ve dosya boyutu artabilir.\n\n"+
            "720p / 60 FPS · 9 Mbps\n1080p / 60 FPS · 18 Mbps\n1080p / 60 FPS Maks · 25 Mbps\n\n"+
            "TikTok videoyu yükleme sonrasında yeniden işleyebilir; sonuç kaynağa ve cihaza bağlıdır."
        ).setPositiveButton("Anladım",null).show()
    }
    private fun checkUpdate() {
        thread(name="sowz-update-check") {
            val result=UpdateCheck.check(BuildConfig.VERSION_CODE)
            if(result is UpdateCheck.Result.UpdateRequired) runOnUiThread {
                if(!isFinishing) AlertDialog.Builder(this).setTitle("Yeni sürüm hazır")
                    .setMessage("Sow7Rix Studio için yeni bir sürüm mevcut.")
                    .setPositiveButton("İndir") { _,_ -> startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(result.info.downloadUrl))) }
                    .setNegativeButton("Daha sonra",null).show()
            }
        }
    }
    private fun readyFileCount():Int=try {
        val projection=arrayOf(MediaStore.Video.Media._ID)
        val selection="${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val args=arrayOf("Movies/Sow7Rix%")
        contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,projection,selection,args,null)?.use { it.count } ?: 0
    } catch(_:Exception) { 0 }
    private fun animateIn(view:View,delay:Long) {
        view.alpha=0f; view.translationY=dp(10f).toFloat()
        view.postDelayed({ if(!isFinishing) view.animate().alpha(1f).translationY(0f).setDuration(260L).setInterpolator(DecelerateInterpolator()).start() },delay)
    }
    private fun press(view:View) {
        view.setOnTouchListener { v,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.88f).setDuration(75L).start()
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150L).setInterpolator(android.view.animation.OvershootInterpolator(0.6f)).start()
            }
            false
        }
    }
    private fun dp(value:Float):Int=UiKit.dp(this,value).toInt()
}

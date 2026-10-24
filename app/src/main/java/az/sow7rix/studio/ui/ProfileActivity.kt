package az.sow7rix.studio.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import az.sow7rix.studio.BuildConfig
import az.sow7rix.studio.R
import az.sow7rix.studio.core.StudioLauncher

/** TikTok oturumu kısayolları ve cihazdaki gerçek render sayısı. */
class ProfileActivity : Activity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.parseColor(UiKit.BG)
        window.navigationBarColor=Color.parseColor(UiKit.BG)
        buildUi()
    }
    private fun buildUi() {
        val page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor(UiKit.BG)) }
        val scroll=ScrollView(this).apply { clipToPadding=false }
        val content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(22f),dp(24f),dp(22f),dp(28f)) }
        val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text="‹"; textSize=32f; setTextColor(Color.parseColor(UiKit.TEXT)); setPadding(0,0,dp(15f),0)
            setOnClickListener { finish(); overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out) }
        })
        val heading=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        heading.addView(TextView(this).apply { text="Profil & hesap"; textSize=22f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor(UiKit.TEXT)) })
        heading.addView(TextView(this).apply { text="SOWZ STUDIO · "+BuildConfig.VERSION_NAME; textSize=10f; letterSpacing=0.09f; setTextColor(Color.parseColor(UiKit.TEXT_DIM)) })
        header.addView(heading); content.addView(header); content.addView(UiKit.spacer(this,23f))

        val hero=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.parseColor("#182A36"),Color.parseColor("#10151D"))).apply {
                cornerRadius=dp(24f).toFloat(); setStroke(dp(1f),Color.parseColor("#2B4852"))
            }
            setPadding(dp(20f),dp(24f),dp(20f),dp(24f))
        }
        hero.addView(ImageView(this).apply { setImageResource(R.drawable.ic_launcher); layoutParams=LinearLayout.LayoutParams(dp(104f),dp(104f)) })
        hero.addView(UiKit.spacer(this,13f))
        hero.addView(TextView(this).apply { text="Sow7Rix Studio"; textSize=23f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor(UiKit.TEXT)); gravity=Gravity.CENTER })
        hero.addView(TextView(this).apply { text="Video üretim alanın"; textSize=13f; setTextColor(Color.parseColor(UiKit.TEXT_DIM)); gravity=Gravity.CENTER; setPadding(0,dp(5f),0,dp(12f)) })
        hero.addView(TextView(this).apply {
            text="FOX WORKSPACE · "+BuildConfig.VERSION_NAME; textSize=9f; letterSpacing=0.12f
            setTextColor(Color.parseColor("#65DDD5")); background=UiKit.pill("#173A40",30f,this@ProfileActivity); setPadding(dp(13f),dp(8f),dp(13f),dp(8f))
        })
        content.addView(hero); hero.alpha=0f; hero.translationY=dp(10f).toFloat()
        hero.postDelayed({ if(!isFinishing) hero.animate().alpha(1f).translationY(0f).setDuration(260L).setInterpolator(DecelerateInterpolator()).start() },50L)
        content.addView(UiKit.spacer(this,24f)); content.addView(UiKit.sectionLabel(this,"TIKTOK OTURUMU","#65DDD5")); content.addView(UiKit.spacer(this,10f))
        val session=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=UiKit.card(this@ProfileActivity); setPadding(dp(16f),dp(16f),dp(16f),dp(16f)) }
        val sessionTitle=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        sessionTitle.addView(View(this).apply { background=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(Color.parseColor("#F2A318")) } },LinearLayout.LayoutParams(dp(8f),dp(8f)))
        sessionTitle.addView(TextView(this).apply { text="  Tarayıcı oturumu"; textSize=15f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor(UiKit.TEXT)) })
        session.addView(sessionTitle)
        session.addView(TextView(this).apply {
            text="TikTok girişi güvenli tarayıcı sekmesinde açılır. Oturum tarayıcıda kalır; SOWZ şifreni almaz veya saklamaz."
            textSize=12f; setLineSpacing(dp(3f).toFloat(),1f); setTextColor(Color.parseColor(UiKit.TEXT_DIM)); setPadding(0,dp(8f),0,dp(14f))
        })
        session.addView(actionButton("TikTok'ta oturum aç","#65DDD5") { StudioLauncher.openLogin(this) })
        session.addView(UiKit.spacer(this,9f))
        session.addView(actionButton("TikTok Studio'yu aç","#FF5D7D") { StudioLauncher.open(this) })
        session.addView(UiKit.spacer(this,9f))
        session.addView(actionButton("TikTok ana sayfası","#70BFFF") { StudioLauncher.openProfile(this) })
        content.addView(session); content.addView(UiKit.spacer(this,22f))
        content.addView(UiKit.sectionLabel(this,"CİHAZINDAKİ RENDER'LAR",UiKit.AMBER)); content.addView(UiKit.spacer(this,10f))
        val files=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; background=UiKit.card(this@ProfileActivity); setPadding(dp(16f),dp(16f),dp(16f),dp(16f))
        }
        val fileInfo=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        fileInfo.addView(TextView(this).apply { text=realFileCount().toString(); textSize=28f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor(UiKit.AMBER)) })
        fileInfo.addView(TextView(this).apply { text="Movies/Sow7Rix"; textSize=12f; setTextColor(Color.parseColor(UiKit.TEXT_DIM)) })
        files.addView(fileInfo,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        files.addView(actionButton("Geçmişi aç","#65DDD5") { MainActivity.navigateTab(this,1) },LinearLayout.LayoutParams(dp(112f),dp(45f)))
        content.addView(files); content.addView(UiKit.spacer(this,20f))
        content.addView(TextView(this).apply {
            text="TikTok takipçi ve beğeni verileri uydurulmaz. Gerçek istatistikleri uygulama içinde göstermek için TikTok Developer Login Kit, kayıtlı uygulama anahtarı, onaylı izinler ve güvenli sunucu OAuth yapılandırması gerekir."
            textSize=11f; setLineSpacing(dp(3f).toFloat(),1f); setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            background=UiKit.fieldBg(this@ProfileActivity); setPadding(dp(14f),dp(13f),dp(14f),dp(13f))
        })
        scroll.addView(content); page.addView(scroll,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f))
        page.addView(UiKit.bottomNav(this,activeIndex=2) { MainActivity.navigateTab(this,it) })
        setContentView(page)
    }
    private fun actionButton(label:String,accent:String,onClick:()->Unit):LinearLayout {
        val button=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER
            background=GradientDrawable().apply { setColor(Color.parseColor("#17232B")); cornerRadius=dp(13f).toFloat(); setStroke(dp(1f),Color.parseColor(accent)) }
            isClickable=true; isFocusable=true; setPadding(dp(12f),dp(13f),dp(12f),dp(13f)); setOnClickListener { onClick() }
            addView(TextView(this@ProfileActivity).apply { text=label; textSize=12f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor(UiKit.TEXT)); gravity=Gravity.CENTER })
        }
        button.setOnTouchListener { v,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70L).start()
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(140L).setInterpolator(android.view.animation.OvershootInterpolator()).start()
            }
            false
        }
        return button
    }
    private fun realFileCount():Int=try {
        val projection=arrayOf(MediaStore.Video.Media._ID); val selection="${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"; val args=arrayOf("Movies/Sow7Rix%")
        contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,projection,selection,args,null)?.use { it.count } ?: 0
    } catch(_:Exception) { 0 }
    private fun dp(value:Float):Int=UiKit.dp(this,value).toInt()
}

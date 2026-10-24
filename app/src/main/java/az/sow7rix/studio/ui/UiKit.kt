package az.sow7rix.studio.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Kucuk, bagimsiz bir UI yardimci seti - programatik view'leri (XML yok) tutarli,
 * "dizayn edilmis" hissettiren bir gorunume kavusturur: yuvarlatilmis koseler,
 * amber/karanlik marka paleti, basili-durum (ripple) geri bildirimi.
 */
object UiKit {
    const val BG = "#080B10"
    const val CARD = "#111722"
    const val CARD_BORDER = "#263141"
    const val AMBER = "#FFB938"
    const val AMBER_DIM = "#80551A"
    const val TEXT = "#F3F7FC"
    const val TEXT_DIM = "#9AA8BA"
    const val DANGER = "#FF4D6D"

    fun pill(colorHex: String, radiusDp: Float, ctx: Context): GradientDrawable {
        val r = dp(ctx, radiusDp)
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = r
        }
    }

    /** Kartin biraz daha acik tonu - duz renk yerine ince bir dikey gecis (gradient)
     *  icin ust durak. Tamamen DUZ (flat) renkler, ekrandaki her kutuyu ayni/sablon
     *  hissettiren en buyuk sebeplerden biriydi - gercek hicbir derinlik yoktu. */
    const val CARD_TOP = "#1B2533"

    fun card(ctx: Context): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor(CARD_TOP), Color.parseColor(CARD))
        ).apply {
            cornerRadius = dp(ctx, 18f)
            setStroke(dp(ctx, 1f).toInt(), Color.parseColor(CARD_BORDER))
        }
    }

    /** Capraz (TL->BR) gecisli, kose yuvarlatilmis bir gradyan dolgu - duz tek renk
     *  yerine "premium/parlak" bir yuzey hissi icin. Buton/FAB/rozet gibi one cikan
     *  elemanlarda kullanilir. */
    fun gradientPill(colorsHex: List<String>, radiusDp: Float, ctx: Context): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colorsHex.map { Color.parseColor(it) }.toIntArray()
        ).apply {
            cornerRadius = dp(ctx, radiusDp)
        }
    }

    /** Amber dolgu, basinca hafif koyulasan bir buton arka plani (ripple ile). */
    fun primaryButtonBg(ctx: Context): android.graphics.drawable.Drawable {
        val normal = gradientPill(listOf("#FFCE5C", AMBER, "#C9790C"), 16f, ctx)
        val pressed = pill(AMBER_DIM, 16f, ctx)
        val states = StateListDrawable()
        states.addState(intArrayOf(android.R.attr.state_pressed), pressed)
        states.addState(intArrayOf(), normal)
        return RippleDrawable(android.content.res.ColorStateList.valueOf(Color.parseColor("#33000000")), states, null)
    }

    /** Bir halkanin/rozetin arkasina konan, yari saydam, bulanik-gibi gorunen genis
     *  bir daire - "neon glow" hissi verir (gercek blur degil ama cok daha ucuz ve
     *  her cihazda ayni calisir). Birden fazla ic-ice daire ile yumusatilir. */
    fun haloCircle(ctx: Context, sizeDp: Float, colorHex: String): FrameLayout {
        val base = Color.parseColor(colorHex)
        val holder = FrameLayout(ctx)
        val rings = listOf(1f to 22, 0.72f to 40, 0.46f to 70)
        rings.forEach { (scale, alpha) ->
            val s = dp(ctx, sizeDp * scale).toInt()
            holder.addView(View(ctx).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base)))
                }
            }, FrameLayout.LayoutParams(s, s).apply { gravity = Gravity.CENTER })
        }
        return holder
    }

    /** Kart gorunumlu, ikincil bir menu/liste butonu arka plani. */
    fun secondaryButtonBg(ctx: Context): android.graphics.drawable.Drawable {
        val base = card(ctx)
        return RippleDrawable(android.content.res.ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")), base, base)
    }

    fun fieldBg(ctx: Context): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#1D1B24"))
            cornerRadius = dp(ctx, 14f)
            setStroke(dp(ctx, 1f).toInt(), Color.parseColor(CARD_BORDER))
        }
    }

    fun title(ctx: Context, text: String, sizeSp: Float = 24f): TextView = TextView(ctx).apply {
        this.text = text
        setTextColor(Color.parseColor(TEXT))
        textSize = sizeSp
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        gravity = Gravity.CENTER
    }

    fun subtitle(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        setTextColor(Color.parseColor(TEXT_DIM))
        textSize = 14f
        gravity = Gravity.CENTER
        setLineSpacing(4f, 1f)
    }

    /** Emoji yerine, renkli yuvarlatilmis kare bir "ikon rozeti" - gercek uygulama ikonu gibi durur. */
    fun iconBadge(ctx: Context, glyph: String, colorHex: String, sizeDp: Float = 44f): LinearLayout {
        return LinearLayout(ctx).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor(colorHex))
                cornerRadius = dp(ctx, sizeDp * 0.28f)
            }
            layoutParams = LinearLayout.LayoutParams(dp(ctx, sizeDp).toInt(), dp(ctx, sizeDp).toInt())
            addView(TextView(ctx).apply {
                text = glyph
                textSize = sizeDp * 0.42f
                setTextColor(Color.parseColor("#12100A"))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })
        }
    }

    /** Alt-orta kose icin buyuk, yuvarlak, golgeli bir hizli-eylem butonu (FAB).
     *  ESKI: duz tek-renk amber daire. YENI: capraz gradyan dolgu + daha yuksek
     *  elevation (daha belirgin golge) - "premium/parlak" bir dugme hissi icin. */
    fun fab(ctx: Context, glyph: String, onClick: () -> Unit): LinearLayout {
        val size = dp(ctx, 62f).toInt()
        return LinearLayout(ctx).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            elevation = dp(ctx, 14f)
            layoutParams = LinearLayout.LayoutParams(size, size)
            val normal = gradientPill(listOf("#FFD774", AMBER, "#C9790C"), 31f, ctx)
            val pressed = pill(AMBER_DIM, 31f, ctx)
            val states = StateListDrawable()
            states.addState(intArrayOf(android.R.attr.state_pressed), pressed)
            states.addState(intArrayOf(), normal)
            background = RippleDrawable(android.content.res.ColorStateList.valueOf(Color.parseColor("#33000000")), states, null)
            setOnClickListener { onClick() }
            addView(TextView(ctx).apply {
                text = glyph
                textSize = 26f
                setTextColor(Color.parseColor("#12100A"))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })
        }
    }

    /** Kucuk, yari saydam bir "marka rozeti" - bir WebView'in ustune bindirilip
     *  "Sow7Rix burada aktif" mesajini site icinde gosterir. */
    fun brandBadge(ctx: Context, text: String): LinearLayout {
        return LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC17161C"))
                cornerRadius = dp(ctx, 20f)
                setStroke(dp(ctx, 1f).toInt(), Color.parseColor(AMBER))
            }
            setPadding(dp(ctx, 14f).toInt(), dp(ctx, 8f).toInt(), dp(ctx, 14f).toInt(), dp(ctx, 8f).toInt())
            addView(TextView(ctx).apply {
                this.text = "● "
                setTextColor(Color.parseColor(AMBER))
                textSize = 11f
            })
            addView(TextView(ctx).apply {
                this.text = text
                setTextColor(Color.parseColor(TEXT))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            })
        }
    }

    /** Baslik + alt yazi + sag tarafta ok ikonu olan bir menu satiri (kart icinde), renkli ikon rozetiyle. */
    fun menuRow(ctx: Context, emoji: String, label: String, caption: String, accentColor: String = AMBER, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = secondaryButtonBg(ctx)
            isClickable = true
            isFocusable = true
            setPadding(dp(ctx, 16f).toInt(), dp(ctx, 16f).toInt(), dp(ctx, 20f).toInt(), dp(ctx, 16f).toInt())
            setOnClickListener { onClick() }
        }

        val icon = iconBadge(ctx, emoji, accentColor)
        row.addView(icon, LinearLayout.LayoutParams(dp(ctx, 44f).toInt(), dp(ctx, 44f).toInt()).apply {
            marginEnd = dp(ctx, 14f).toInt()
        })

        val textCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        textCol.addView(TextView(ctx).apply {
            text = label
            setTextColor(Color.parseColor(TEXT))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        })
        textCol.addView(TextView(ctx).apply {
            text = caption
            setTextColor(Color.parseColor(TEXT_DIM))
            textSize = 12f
            setPadding(0, dp(ctx, 2f).toInt(), 0, 0)
        })
        row.addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val chevron = TextView(ctx).apply {
            text = "›"
            setTextColor(Color.parseColor(TEXT_DIM))
            textSize = 20f
        }
        row.addView(chevron)

        return row
    }

    /** Bolum basligi: duz gri yazi yerine, yaninda kucuk gradyanli bir "vurgu cubugu"
     *  olan bir satir - premium panolarda bolumleri boylece ayirirlar, salt gri
     *  kucuk-yazi daha "varsayilan/sablon" hissi veriyordu. */
    fun sectionLabel(ctx: Context, text: String, accentColor: String = AMBER): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(View(ctx).apply {
                background = gradientPill(listOf(accentColor, "#00000000"), 2f, ctx).apply {
                    orientation = GradientDrawable.Orientation.LEFT_RIGHT
                }
            }, LinearLayout.LayoutParams(dp(ctx, 14f).toInt(), dp(ctx, 3f).toInt()).apply {
                marginEnd = dp(ctx, 8f).toInt()
            })
            addView(TextView(ctx).apply {
                this.text = text
                setTextColor(Color.parseColor(TEXT_DIM))
                textSize = 12f
                letterSpacing = 0.12f
                typeface = Typeface.DEFAULT_BOLD
            })
        }
    }

    fun spacer(ctx: Context, heightDp: Float): View = View(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, heightDp).toInt())
    }

    /** Buyuk, amber bir "glow" (parlama) halkasi - bir view'in arkasina konup pulse ile canlandirilir. */
    fun glowRing(ctx: Context, sizeDp: Float): View {
        return View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(ctx, sizeDp).toInt(), dp(ctx, sizeDp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(AMBER))
            }
        }
    }

    /** Amber glow halkasinin tam ortasinda uygulamanin maskotu (tilki) - marka kimligini
     *  tasiyan animasyonlu bir rozet. Dondurulen FrameLayout'un ic View'i (glow) pulse icin kullanilir. */
    fun foxGlow(ctx: Context, sizeDp: Float): FrameLayout {
        val holder = FrameLayout(ctx)
        val glow = glowRing(ctx, sizeDp)
        holder.addView(glow, FrameLayout.LayoutParams(dp(ctx, sizeDp).toInt(), dp(ctx, sizeDp).toInt()))
        holder.addView(vectorIcon(ctx, Glyph.FOX, "#12100A", sizeDp * 0.46f), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER })
        holder.tag = glow
        return holder
    }

    /** foxGlow(...) ile olusturulan bir rozetin icindeki pulse edilecek gercek glow view'ini dondurur. */
    fun foxGlowPulseTarget(holder: FrameLayout): View = holder.tag as View

    /** Bir view'i sonsuz, yumusak bir "nefes alma" (glow pulse) animasyonuyla canlandirir.
     *  Cagiran taraf, Activity kapanirken animator.cancel() cagirmali (sizinti olmasin diye). */
    fun startGlowPulse(view: View, minAlpha: Float = 0.35f, maxAlpha: Float = 0.9f, durationMs: Long = 1100L): ValueAnimator {
        val animator = ObjectAnimator.ofFloat(view, "alpha", minAlpha, maxAlpha).apply {
            duration = durationMs
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        animator.start()
        return animator
    }

    /** Kucuk, dolgu renkli bir "etiket/chip" (ornek: #kesfet). */
    fun chip(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        setTextColor(Color.parseColor(AMBER))
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#2A2412"))
            cornerRadius = dp(ctx, 12f)
            setStroke(dp(ctx, 1f).toInt(), Color.parseColor(AMBER_DIM))
        }
        setPadding(dp(ctx, 12f).toInt(), dp(ctx, 6f).toInt(), dp(ctx, 12f).toInt(), dp(ctx, 6f).toInt())
    }

    /** Durum noktasi: yesil (uygun) veya kirmizi (uygun degil) kucuk pill. */
    fun statusPill(ctx: Context, label: String, good: Boolean): LinearLayout {
        val color = if (good) "#2E7D4F" else "#7D2E2E"
        val dotColor = if (good) "#4DD68C" else "#FF6B6B"
        return LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(color))
                cornerRadius = dp(ctx, 10f)
            }
            setPadding(dp(ctx, 10f).toInt(), dp(ctx, 5f).toInt(), dp(ctx, 10f).toInt(), dp(ctx, 5f).toInt())
            addView(TextView(ctx).apply {
                text = "● "
                setTextColor(Color.parseColor(dotColor))
                textSize = 10f
            })
            addView(TextView(ctx).apply {
                text = label
                setTextColor(Color.parseColor(TEXT))
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
            })
        }
    }

    /**
     * Alt navigasyon cubugu: Ana Sayfa / Gecmis / Ayarlar. activeIndex vurgulanir.
     * (Eski "Kesfet" - sabit/jenerik hashtag + saat onerisi listesi - kaldirildi,
     * yerine GERCEK islenmis dosya gecmisini gosteren "Gecmis" sekmesi kondu.)
     * onTab(index) her sekmeye basildiginda cagrilir - cagiran taraf gecisi kendi yapar.
     */
    fun bottomNav(ctx: Context, activeIndex: Int, onTab: (Int) -> Unit): LinearLayout {
        val items = listOf(Triple(Glyph.HOME, "Ana Sayfa", 0), Triple(Glyph.HISTORY, "Gecmis", 1), Triple(Glyph.SETTINGS, "Ayarlar", 2))
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor(CARD))
            setPadding(0, dp(ctx, 10f).toInt(), 0, dp(ctx, 10f).toInt())
        }
        items.forEach { (glyph, label, index) ->
            val active = index == activeIndex
            val tab = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setOnClickListener { onTab(index) }
            }
            tab.addView(vectorIcon(ctx, glyph, if (active) AMBER else TEXT_DIM, 20f))
            tab.addView(TextView(ctx).apply {
                text = label
                textSize = 10f
                setTextColor(Color.parseColor(if (active) AMBER else TEXT_DIM))
                gravity = Gravity.CENTER
                setPadding(0, dp(ctx, 2f).toInt(), 0, 0)
            })
            bar.addView(tab, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        return bar
    }

    /** Emoji KULLANMAYAN, elle cizilen (vektorel) kucuk ikon seti. Android'in varsayilan
     *  renkli emoji fontuna hic basvurmaz - her zaman Path/Canvas ile cizilir, bu yuzden
     *  her cihazda ayni, markalasmis gorunur. */
    enum class Glyph { HOME, DISCOVER, SETTINGS, UPLOAD, SEARCH, CHECK, BELL, TRASH, EXIT, FOX, SLIDER, MENU, CLOSE, REFRESH, BOLT, FILM, HISTORY }

    fun vectorIcon(ctx: Context, glyph: Glyph, colorHex: String, sizeDp: Float = 22f): View {
        val sizePx = dp(ctx, sizeDp).toInt()
        return object : View(ctx) {
            init { layoutParams = LinearLayout.LayoutParams(sizePx, sizePx) }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val h = height.toFloat()
                val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor(colorHex)
                    style = Paint.Style.STROKE
                    strokeWidth = w * 0.09f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(colorHex) }

                when (glyph) {
                    Glyph.HOME -> {
                        val p = Path()
                        p.moveTo(w * 0.5f, h * 0.12f)
                        p.lineTo(w * 0.88f, h * 0.44f)
                        p.lineTo(w * 0.74f, h * 0.44f)
                        p.lineTo(w * 0.74f, h * 0.86f)
                        p.lineTo(w * 0.26f, h * 0.86f)
                        p.lineTo(w * 0.26f, h * 0.44f)
                        p.lineTo(w * 0.12f, h * 0.44f)
                        p.close()
                        canvas.drawPath(p, stroke)
                    }
                    Glyph.DISCOVER -> {
                        canvas.drawCircle(w * 0.5f, h * 0.5f, w * 0.38f, stroke)
                        val needle = Path()
                        needle.moveTo(w * 0.5f, h * 0.28f)
                        needle.lineTo(w * 0.6f, h * 0.5f)
                        needle.lineTo(w * 0.5f, h * 0.72f)
                        needle.lineTo(w * 0.4f, h * 0.5f)
                        needle.close()
                        canvas.drawPath(needle, fill)
                    }
                    Glyph.SETTINGS -> {
                        canvas.drawCircle(w * 0.5f, h * 0.5f, w * 0.17f, stroke)
                        for (i in 0 until 6) {
                            canvas.save()
                            canvas.rotate(i * 60f, w * 0.5f, h * 0.5f)
                            canvas.drawLine(w * 0.5f, h * 0.1f, w * 0.5f, h * 0.26f, stroke)
                            canvas.restore()
                        }
                    }
                    Glyph.UPLOAD -> {
                        val p = Path()
                        p.moveTo(w * 0.5f, h * 0.12f)
                        p.lineTo(w * 0.78f, h * 0.42f)
                        p.lineTo(w * 0.6f, h * 0.42f)
                        p.lineTo(w * 0.6f, h * 0.78f)
                        p.lineTo(w * 0.4f, h * 0.78f)
                        p.lineTo(w * 0.4f, h * 0.42f)
                        p.lineTo(w * 0.22f, h * 0.42f)
                        p.close()
                        canvas.drawPath(p, fill)
                        canvas.drawLine(w * 0.2f, h * 0.9f, w * 0.8f, h * 0.9f, stroke)
                    }
                    Glyph.SEARCH -> {
                        canvas.drawCircle(w * 0.42f, h * 0.42f, w * 0.26f, stroke)
                        canvas.drawLine(w * 0.62f, h * 0.62f, w * 0.86f, h * 0.86f, stroke)
                    }
                    Glyph.CHECK -> {
                        val p = Path()
                        p.moveTo(w * 0.18f, h * 0.52f)
                        p.lineTo(w * 0.42f, h * 0.76f)
                        p.lineTo(w * 0.84f, h * 0.26f)
                        canvas.drawPath(p, stroke)
                    }
                    Glyph.BELL -> {
                        val p = Path()
                        p.moveTo(w * 0.3f, h * 0.68f)
                        p.lineTo(w * 0.3f, h * 0.42f)
                        p.cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.2f, w * 0.7f, h * 0.42f)
                        p.lineTo(w * 0.7f, h * 0.68f)
                        canvas.drawPath(p, stroke)
                        canvas.drawLine(w * 0.22f, h * 0.68f, w * 0.78f, h * 0.68f, stroke)
                        canvas.drawCircle(w * 0.5f, h * 0.82f, w * 0.07f, fill)
                    }
                    Glyph.TRASH -> {
                        canvas.drawLine(w * 0.22f, h * 0.3f, w * 0.78f, h * 0.3f, stroke)
                        canvas.drawLine(w * 0.4f, h * 0.22f, w * 0.6f, h * 0.22f, stroke)
                        val p = Path()
                        p.moveTo(w * 0.28f, h * 0.32f)
                        p.lineTo(w * 0.32f, h * 0.82f)
                        p.lineTo(w * 0.68f, h * 0.82f)
                        p.lineTo(w * 0.72f, h * 0.32f)
                        canvas.drawPath(p, stroke)
                        canvas.drawLine(w * 0.42f, h * 0.42f, w * 0.44f, h * 0.72f, stroke)
                        canvas.drawLine(w * 0.58f, h * 0.42f, w * 0.56f, h * 0.72f, stroke)
                    }
                    Glyph.EXIT -> {
                        val p = Path()
                        p.moveTo(w * 0.58f, h * 0.2f)
                        p.lineTo(w * 0.24f, h * 0.2f)
                        p.lineTo(w * 0.24f, h * 0.8f)
                        p.lineTo(w * 0.58f, h * 0.8f)
                        canvas.drawPath(p, stroke)
                        canvas.drawLine(w * 0.4f, h * 0.5f, w * 0.82f, h * 0.5f, stroke)
                        val arrow = Path()
                        arrow.moveTo(w * 0.66f, h * 0.36f)
                        arrow.lineTo(w * 0.84f, h * 0.5f)
                        arrow.lineTo(w * 0.66f, h * 0.64f)
                        canvas.drawPath(arrow, stroke)
                    }
                    Glyph.FOX -> {
                        val face = Path()
                        face.moveTo(w * 0.5f, h * 0.88f)
                        face.cubicTo(w * 0.18f, h * 0.78f, w * 0.14f, h * 0.4f, w * 0.5f, h * 0.3f)
                        face.cubicTo(w * 0.86f, h * 0.4f, w * 0.82f, h * 0.78f, w * 0.5f, h * 0.88f)
                        face.close()
                        canvas.drawPath(face, fill)
                        val earL = Path()
                        earL.moveTo(w * 0.22f, h * 0.38f)
                        earL.lineTo(w * 0.12f, h * 0.08f)
                        earL.lineTo(w * 0.4f, h * 0.28f)
                        earL.close()
                        canvas.drawPath(earL, fill)
                        val earR = Path()
                        earR.moveTo(w * 0.78f, h * 0.38f)
                        earR.lineTo(w * 0.88f, h * 0.08f)
                        earR.lineTo(w * 0.6f, h * 0.28f)
                        earR.close()
                        canvas.drawPath(earR, fill)
                        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(BG) }
                        canvas.drawCircle(w * 0.38f, h * 0.56f, w * 0.045f, bg)
                        canvas.drawCircle(w * 0.62f, h * 0.56f, w * 0.045f, bg)
                        val nose = Path()
                        nose.moveTo(w * 0.44f, h * 0.68f)
                        nose.lineTo(w * 0.56f, h * 0.68f)
                        nose.lineTo(w * 0.5f, h * 0.78f)
                        nose.close()
                        canvas.drawPath(nose, bg)
                    }
                    Glyph.SLIDER -> {
                        canvas.drawLine(w * 0.16f, h * 0.5f, w * 0.84f, h * 0.5f, stroke)
                        canvas.drawCircle(w * 0.64f, h * 0.5f, w * 0.12f, fill)
                    }
                    Glyph.MENU -> {
                        canvas.drawLine(w * 0.16f, h * 0.28f, w * 0.84f, h * 0.28f, stroke)
                        canvas.drawLine(w * 0.16f, h * 0.5f, w * 0.84f, h * 0.5f, stroke)
                        canvas.drawLine(w * 0.16f, h * 0.72f, w * 0.84f, h * 0.72f, stroke)
                    }
                    Glyph.CLOSE -> {
                        canvas.drawLine(w * 0.22f, h * 0.22f, w * 0.78f, h * 0.78f, stroke)
                        canvas.drawLine(w * 0.78f, h * 0.22f, w * 0.22f, h * 0.78f, stroke)
                    }
                    Glyph.REFRESH -> {
                        val rect = android.graphics.RectF(w * 0.18f, h * 0.18f, w * 0.82f, h * 0.82f)
                        canvas.drawArc(rect, -30f, 260f, false, stroke)
                        val arrow = Path()
                        arrow.moveTo(w * 0.78f, h * 0.1f)
                        arrow.lineTo(w * 0.86f, h * 0.3f)
                        arrow.lineTo(w * 0.64f, h * 0.32f)
                        arrow.close()
                        canvas.drawPath(arrow, fill)
                    }
                    Glyph.BOLT -> {
                        val p = Path()
                        p.moveTo(w * 0.56f, h * 0.08f)
                        p.lineTo(w * 0.24f, h * 0.58f)
                        p.lineTo(w * 0.46f, h * 0.58f)
                        p.lineTo(w * 0.4f, h * 0.92f)
                        p.lineTo(w * 0.78f, h * 0.38f)
                        p.lineTo(w * 0.54f, h * 0.38f)
                        p.close()
                        canvas.drawPath(p, fill)
                    }
                    Glyph.FILM -> {
                        val rect = android.graphics.RectF(w * 0.14f, h * 0.2f, w * 0.86f, h * 0.8f)
                        canvas.drawRoundRect(rect, w * 0.06f, w * 0.06f, stroke)
                        for (i in 0 until 4) {
                            val x = w * (0.26f + i * 0.17f)
                            canvas.drawLine(x, h * 0.2f, x, h * 0.3f, stroke)
                            canvas.drawLine(x, h * 0.7f, x, h * 0.8f, stroke)
                        }
                    }
                    Glyph.HISTORY -> {
                        canvas.drawCircle(w * 0.5f, h * 0.5f, w * 0.38f, stroke)
                        canvas.drawLine(w * 0.5f, h * 0.5f, w * 0.5f, h * 0.28f, stroke)
                        canvas.drawLine(w * 0.5f, h * 0.5f, w * 0.66f, h * 0.58f, stroke)
                        val tick = Path()
                        tick.moveTo(w * 0.18f, h * 0.22f)
                        tick.arcTo(android.graphics.RectF(w * 0.08f, h * 0.08f, w * 0.34f, h * 0.34f), 200f, 100f)
                        canvas.drawPath(tick, stroke)
                    }
                }
            }
        }
    }

    /** Competitor-tarzi kare/kart buton: ortada ince cerceveli yuvarlak ikon, altinda
     *  kisa renkli etiket. Ana menude 3'lu bir satir olarak kullanilir. */
    fun squareCard(ctx: Context, glyph: Glyph, label: String, accentColor: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = card(ctx)
            elevation = dp(ctx, 5f)
            isClickable = true
            isFocusable = true
            setPadding(dp(ctx, 8f).toInt(), dp(ctx, 18f).toInt(), dp(ctx, 8f).toInt(), dp(ctx, 16f).toInt())
            setOnClickListener { onClick() }

            addView(LinearLayout(ctx).apply {
                gravity = Gravity.CENTER
                val base = Color.parseColor(accentColor)
                background = GradientDrawable().apply {
                    setColor(Color.argb(30, Color.red(base), Color.green(base), Color.blue(base)))
                    setStroke(dp(ctx, 1.5f).toInt(), base)
                    cornerRadius = dp(ctx, 28f)
                }
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 52f).toInt(), dp(ctx, 52f).toInt())
                addView(vectorIcon(ctx, glyph, accentColor, 22f))
            })
            addView(spacer(ctx, 10f))
            addView(TextView(ctx).apply {
                text = label
                setTextColor(Color.parseColor(accentColor))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                letterSpacing = 0.02f
            })
        }
    }

    /** Renkli yuvarlatilmis kare rozet + icinde vektorel ikon - emoji'nin yerini alir.
     *  (Eski, DOLU-renkli stil - artik menuRowV bunu kullanmiyor, ama geriye donuk
     *  uyumluluk icin birakildi.) */
    fun iconBadgeV(ctx: Context, glyph: Glyph, colorHex: String, sizeDp: Float = 44f): LinearLayout {
        return LinearLayout(ctx).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor(colorHex))
                cornerRadius = dp(ctx, sizeDp * 0.28f)
            }
            layoutParams = LinearLayout.LayoutParams(dp(ctx, sizeDp).toInt(), dp(ctx, sizeDp).toInt())
            addView(vectorIcon(ctx, glyph, "#12100A", sizeDp * 0.46f))
        }
    }

    /** menuRowV'nin ikon stili: eskiden HER satirda ayni "dolu renkli kare" kullaniliyordu,
     *  bu da uygulamanin her ekraninin birbirinin ayni sablonu gibi gorunmesine sebep
     *  oluyordu. Bunun yerine, squareCard'daki ("FFMPEG/UPLOAD/METHODS" kartlari) ile
     *  AYNI dilde - ince renkli cerceveli daire + hafif renkli (saydam) ic zemin -
     *  cizilir. Boylece uygulama ici her yerde TUTARLI ama ayni zamanda DAHA "tasarlanmis"
     *  (flat-template degil) bir ikon dili olur. */
    fun ringIconBadge(ctx: Context, glyph: Glyph, colorHex: String, sizeDp: Float = 44f): LinearLayout {
        val base = Color.parseColor(colorHex)
        val tintedFill = Color.argb(36, Color.red(base), Color.green(base), Color.blue(base))
        return LinearLayout(ctx).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(tintedFill)
                setStroke(dp(ctx, 1.4f).toInt(), base)
                cornerRadius = dp(ctx, sizeDp * 0.5f)
            }
            layoutParams = LinearLayout.LayoutParams(dp(ctx, sizeDp).toInt(), dp(ctx, sizeDp).toInt())
            addView(vectorIcon(ctx, glyph, colorHex, sizeDp * 0.42f))
        }
    }

    /** Bir alt-eleman (ornegin uygulama logosu) etrafina ince renkli bir halka cizer
     *  ve sag-alt kosesine kucuk bir "aktif" noktasi ekler. Kuronai/ORBIX benzeri
     *  profil-avatari gorunumu veriyor, ama noktanin anlami GERCEK: uygulama
     *  calisir/erisim aktif durumda - sahte bir "canli" rozeti degil. */
    fun avatarRing(ctx: Context, child: View, sizeDp: Float, ringColor: String = AMBER): FrameLayout {
        val holder = FrameLayout(ctx)
        val sizePx = dp(ctx, sizeDp).toInt()
        holder.addView(View(ctx).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(BG))
                setStroke(dp(ctx, 2f).toInt(), Color.parseColor(ringColor))
            }
        }, FrameLayout.LayoutParams(sizePx, sizePx))
        val inset = dp(ctx, sizeDp * 0.1f).toInt()
        holder.addView(child, FrameLayout.LayoutParams(sizePx - inset * 2, sizePx - inset * 2).apply {
            gravity = Gravity.CENTER
        })
        val dotSize = dp(ctx, sizeDp * 0.24f).toInt()
        holder.addView(View(ctx).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4DD68C"))
                setStroke(dp(ctx, 2f).toInt(), Color.parseColor(BG))
            }
        }, FrameLayout.LayoutParams(dotSize, dotSize).apply { gravity = Gravity.BOTTOM or Gravity.END })
        return holder
    }

    /** Bir "istatistik" dikey blogu: buyuk/kalin deger + altinda kucuk harf araligi
     *  verilmis etiket. Ana sayfadaki panoda GERCEK degerler icin kullanilir (uydurma
     *  takipci/begeni sayisi degil - ornegin hazir dosya sayisi, kanal durumu). */
    fun statTile(ctx: Context, value: String, label: String, accentColor: String = TEXT): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(TextView(ctx).apply {
                text = value
                setTextColor(Color.parseColor(accentColor))
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })
            addView(TextView(ctx).apply {
                text = label
                setTextColor(Color.parseColor(TEXT_DIM))
                textSize = 10.5f
                gravity = Gravity.CENTER
                letterSpacing = 0.05f
                setPadding(0, dp(ctx, 3f).toInt(), 0, 0)
            })
        }
    }

    /** menuRow'un emoji yerine vektorel ikon kullanan surumu. */
    fun menuRowV(ctx: Context, glyph: Glyph, label: String, caption: String, accentColor: String = AMBER, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = secondaryButtonBg(ctx)
            elevation = dp(ctx, 3f)
            isClickable = true
            isFocusable = true
            setPadding(dp(ctx, 16f).toInt(), dp(ctx, 16f).toInt(), dp(ctx, 20f).toInt(), dp(ctx, 16f).toInt())
            setOnClickListener { onClick() }
        }
        row.addView(ringIconBadge(ctx, glyph, accentColor), LinearLayout.LayoutParams(dp(ctx, 44f).toInt(), dp(ctx, 44f).toInt()).apply {
            marginEnd = dp(ctx, 14f).toInt()
        })
        val textCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        textCol.addView(TextView(ctx).apply {
            text = label
            setTextColor(Color.parseColor(TEXT))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        })
        textCol.addView(TextView(ctx).apply {
            text = caption
            setTextColor(Color.parseColor(TEXT_DIM))
            textSize = 12f
            setPadding(0, dp(ctx, 2f).toInt(), 0, 0)
        })
        row.addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(ctx).apply {
            text = "›"
            setTextColor(Color.parseColor(TEXT_DIM))
            textSize = 20f
        })
        return row
    }

    fun dp(ctx: Context, value: Float): Float = value * ctx.resources.displayMetrics.density
}

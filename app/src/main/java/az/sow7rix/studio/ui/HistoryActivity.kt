package az.sow7rix.studio.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import az.sow7rix.studio.core.StudioLauncher
import java.text.DateFormat
import java.util.Date

/**
 * Gecmis: eskiden burada sabit/jenerik ulke+hashtag listesi gosteren "Kesfet" ekrani
 * vardi (DiscoverActivity) - gercek TikTok verisine dayanmiyordu, sadece "aksam
 * saatleri daha iyi" gibi genel bir kural + sabit etiket listesiydi. Kaldirildi.
 *
 * Yerine GERCEK veriye dayanan bir ekran kondu: Movies/Sow7Rix klasorune daha once
 * islenmis, MediaStore'a kaydedilmis dosyalarin GERCEK listesi (canli sorgu - uydurma
 * "gecmis" degil). Her kayit icin: dosya adi, cozunurluk, boyut, tarih - hepsi
 * MediaStore'dan okunuyor. Iki gercek aksiyon var: "TikTok Studio'yu Ac" (StudioLauncher
 * uzerinden, UploadActivity ile AYNI kod) ve "Sil" (MediaStore'dan gercek silme).
 */
class HistoryActivity : Activity() {

    private lateinit var listContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
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

        content.addView(UiKit.title(this, "Gecmis", 22f))
        content.addView(UiKit.spacer(this, 6f))
        content.addView(UiKit.subtitle(this, "Movies/Sow7Rix klasorune kaydedilmis, daha once islenmis dosyalarin GERCEK listesi"))
        content.addView(UiKit.spacer(this, 20f))

        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(listContainer)

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(UiKit.bottomNav(this, activeIndex = 1) { index -> MainActivity.navigateTab(this, index) })

        setContentView(root)
    }

    private data class HistoryItem(
        val id: Long,
        val displayName: String,
        val sizeBytes: Long,
        val width: Int,
        val height: Int,
        val dateAddedSec: Long
    )

    /** Movies/Sow7Rix altindaki kayitlari CANLI MediaStore sorgusuyla ceker - sabit/
     *  cache'lenmis bir liste degil, her acilista/yenilemede guncel gercek durumu gosterir. */
    private fun queryHistory(): List<HistoryItem> {
        val items = mutableListOf<HistoryItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf("Movies/Sow7Rix%")
        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, selection, args,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    items.add(
                        HistoryItem(
                            id = cursor.getLong(idCol),
                            displayName = cursor.getString(nameCol) ?: "video.mp4",
                            sizeBytes = cursor.getLong(sizeCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            dateAddedSec = cursor.getLong(dateCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gecmis okunamadi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return items
    }

    private fun refreshList() {
        listContainer.removeAllViews()
        val items = queryHistory()
        if (items.isEmpty()) {
            listContainer.addView(TextView(this).apply {
                text = "Henuz islenmis dosya yok. Ana sayfadan video isleyince burada listelenir."
                setTextColor(Color.parseColor(UiKit.TEXT_DIM))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, dp(24f), 0, dp(24f))
            })
            return
        }
        items.forEach { item ->
            listContainer.addView(buildRow(item))
            listContainer.addView(UiKit.spacer(this, 10f))
        }
    }

    private fun buildRow(item: HistoryItem): LinearLayout {
        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.card(this@HistoryActivity)
            elevation = UiKit.dp(this@HistoryActivity, 3f)
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        }

        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(UiKit.ringIconBadge(this, UiKit.Glyph.FILM, "#4DD6C8", 40f), LinearLayout.LayoutParams(dp(40f), dp(40f)).apply { marginEnd = dp(12f) })
        val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textCol.addView(TextView(this).apply {
            text = item.displayName
            setTextColor(Color.parseColor(UiKit.TEXT))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
        })
        val sizeMb = item.sizeBytes / 1024.0 / 1024.0
        val dateText = if (item.dateAddedSec > 0) {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.dateAddedSec * 1000))
        } else "-"
        textCol.addView(TextView(this).apply {
            text = "%dx%d · %.1f MB · %s".format(item.width, item.height, sizeMb, dateText)
            setTextColor(Color.parseColor(UiKit.TEXT_DIM))
            textSize = 11.5f
            setPadding(0, dp(2f), 0, 0)
        })
        topRow.addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(topRow)
        row.addView(UiKit.spacer(this, 12f))

        val actionsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val openBtn = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = UiKit.primaryButtonBg(this@HistoryActivity)
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            setOnClickListener { StudioLauncher.open(this@HistoryActivity, UiKit.BG, UiKit.CARD) }
            addView(TextView(this@HistoryActivity).apply {
                text = "TikTok Studio'yu Ac"
                setTextColor(Color.parseColor("#0A0A0A"))
                textSize = 12.5f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
        actionsRow.addView(openBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8f) })

        val deleteBtn = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = UiKit.secondaryButtonBg(this@HistoryActivity)
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            setOnClickListener { confirmDelete(item.id, uri) }
            addView(UiKit.vectorIcon(this@HistoryActivity, UiKit.Glyph.TRASH, UiKit.DANGER, 16f))
        }
        actionsRow.addView(deleteBtn)
        row.addView(actionsRow)

        return row
    }

    /** GERCEK silme: MediaStore'dan kaldirir (dosya da birlikte silinir - MediaStore
     *  bu kaydin sahibi uygulama olarak biziz, cunku dosyayi biz yazdik). Sahte/sadece
     *  listeden gizleme degil. */
    private fun confirmDelete(id: Long, uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Dosyayi sil")
            .setMessage("Bu islenmis video kalici olarak silinecek. Emin misin?")
            .setPositiveButton("Sil") { _, _ ->
                try {
                    val deleted = contentResolver.delete(uri, null, null)
                    if (deleted > 0) {
                        Toast.makeText(this, "Silindi.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Silinemedi (dosya zaten yok olabilir).", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Silme hatasi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                refreshList()
            }
            .setNegativeButton("Vazgec", null)
            .show()
    }

    private fun dp(v: Float) = UiKit.dp(this, v).toInt()
}

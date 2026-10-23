package az.sow7rix.studio.core

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import java.io.File

/**
 * Yukleme sirasinda devreye giren kalite/on-isleme motoru.
 * Kullanicinin sectigi videoyu TikTok'a gitmeden once isler. Iki ayri strateji var:
 *
 * 1) CRF (kalite-bazli) kodlama: cozunurluk/fps'e DOKUNMAZ, sadece gereksiz veriyi
 *    atar. x264'un CRF 18-20 araligi "gorsel olarak kayipsiz" kabul edilir - yani
 *    goz ayirt edemez ama dosya cok daha kucuk olur. "Sikistir ama orijinal gibi
 *    gitsin" istegi tam olarak budur - bu, video kodlamada standart, herkesin
 *    bildigi bir teknik (ozel/gizli bir "method" degil).
 * 2) Sabit hedef (720p/1080p/120fps): cozunurluk/fps'i degistirmek istendiginde,
 *    sabit bir bitrate ile olceklenir.
 *
 * Nerede calisir: UploadActivity, TikTok sayfasinin kendi "video sec" dosya
 * secicisini yakalayip (WebChromeClient.onShowFileChooser) bu motoru bir
 * overlay olarak WebView'in USTUNDE gosterir - yani "site icinde", ayri bir
 * on-ekran olarak degil.
 */
object QualityEngine {

    data class Settings(
        val label: String,
        /** 0 = kaynak videonun kendi cozunurlugu/fps'i, degistirilmez. */
        val targetWidth: Int = 0,
        val targetHeight: Int = 0,
        val targetFps: Int = 0,
        /** >0 ise sabit bitrate modu kullanilir (targetWidth/Height/Fps ile birlikte). */
        val videoBitrateKbps: Int = 0,
        /** >0 ise CRF (kalite-bazli) modu kullanilir - cozunurluk/fps aynen korunur. */
        val crf: Int = 0,
        /** true ise FFmpeg hic calismaz, dosya oldugu gibi kopyalanir (hicbir sikistirma yok). */
        val skipReencode: Boolean = false
    )

    /**
     * Kullaniciya sunulan hazir kalite secenekleri.
     *
     * Bitrate degerleri rastgele secilmedi: bir referans video (1080x1080, H.264,
     * ~17.5 Mbps) incelendi ve "akici/kaliteli" hissinin asil kaynagi cok daha
     * yuksek bir bitrate kullanilmasiydi - gizli bir algoritma degil. Bu yuzden
     * sabit-bitrate secenekleri de ayni seviyeye (hatta ustune) cekildi, ve CRF
     * degeri 19'dan 16'ya dusuruldu (daha dusuk CRF = daha az kayip, goze gorunur
     * fark olmayacak seviyede).
     */
    val PRESETS = listOf(
        Settings(label = "Orijinal Dosya (Degismez)", skipReencode = true),
        Settings(label = "Orijinal Gorunum, Kucuk Dosya", crf = 16),
        Settings(label = "720p 60fps", targetWidth = 720, targetHeight = 1280, targetFps = 60, videoBitrateKbps = 9000),
        Settings(label = "1080p 60fps", targetWidth = 1080, targetHeight = 1920, targetFps = 60, videoBitrateKbps = 18000),
        // ESKI "1080p 120fps" secenegi KALDIRILDI: TikTok'un kendi API/upload spesifikasyonu
        // 23-60fps araligini kabul ediyor - 120fps zaten TikTok tarafindan hic kabul
        // edilmiyordu. Ayrica telefonda GERCEK 120fps'e motion-compensated interpolasyon
        // (mi_mode=mci) asiri agirdi, bellek tasirip uygulamayi cokertebiliyordu - "kalite
        // secince ana menuye atma" sikayetinin sebebi tam olarak buydu. Yerine, TikTok'un
        // belgelenen 60fps tavanindaki en yuksek bitrate secenegi kondu.
        Settings(label = "1080p 60fps Maks Kalite", targetWidth = 1080, targetHeight = 1920, targetFps = 60, videoBitrateKbps = 25000)
    )

    sealed class Progress {
        data class Running(val percent: Int) : Progress()
        data class Done(val outputFile: File) : Progress()
        data class Failed(val reason: String) : Progress()
        /** Kullanici isleme sirasinda GERCEKTEN iptal ettiginde (FFmpegKit.cancel
         *  ile durdurulan gercek bir surecin sonucu - sahte/gostermelik bir durum degil). */
        object Cancelled : Progress()
    }

    /**
     * inputPath'teki videoyu isler, ciktiyi outputPath'e yazar.
     * onProgress FFmpegKit'in kendi arka plan thread'inden cagrilir -
     * UI guncellemesi icin cagiran taraf runOnUiThread kullanmali.
     *
     * Donus degeri: calisan gercek FFmpegSession (iptal icin cancel(session) ile
     * kullanilir). skipReencode modunda FFmpeg hic calismadigi (senkron dosya
     * kopyalama oldugu) icin null doner - o modda iptal edilecek bir FFmpeg
     * sureci yoktur.
     */
    fun process(
        inputPath: String,
        outputPath: String,
        settings: Settings,
        durationMsHint: Long,
        /** Kaynak videonun GERCEK genislik/yukseklik/fps degerleri (caller tarafindan
         *  MediaMetadataRetriever ile zaten olculmus). Bilinmiyorsa 0 birakilir - o
         *  zaman asagidaki "gereksiz filtreyi atla" optimizasyonu devreye girmez,
         *  eskisi gibi her zaman scale/pad/interpolate uygulanir (guvenli varsayilan). */
        sourceWidth: Int = 0,
        sourceHeight: Int = 0,
        sourceFps: Double = 0.0,
        onProgress: (Progress) -> Unit
    ): FFmpegSession? {
        if (settings.skipReencode) {
            // Hic sikistirma yok - dosyayi oldugu gibi kopyala (en buyuk dosya, hic kayip yok).
            try {
                File(inputPath).copyTo(File(outputPath), overwrite = true)
                onProgress(Progress.Done(File(outputPath)))
            } catch (e: Exception) {
                onProgress(Progress.Failed("Kopyalama basarisiz: ${e.message}"))
            }
            return null
        }

        val cmd: String = if (settings.crf > 0) {
            // CRF modu: cozunurluk/fps kaynaktan AYNEN korunur (vf/scale yok), sadece
            // gereksiz bit'ler atilir. "preset slow" daha kucuk dosya + ayni gorsel
            // kalite icin daha uzun surer ama mobilde makul; veryfast yerine bunu
            // kullanmak dosya boyutunu ciddi dusurur, kaliteyi degil.
            // Ses: TikTok'un resmi onerisi 256kbps AAC stereo - 160k'dan yukseltildi.
            "-y -i \"$inputPath\" -c:v libx264 -preset slow -crf ${settings.crf} " +
                "-pix_fmt yuv420p -c:a aac -b:a 256k -ar 44100 -ac 2 -movflags +faststart \"$outputPath\""
        } else {
            // Sabit hedef cozunurluk/fps modu.
            // Kaynak GOREK zaten hedef cozunurluk/fps'teyse (ya da fps'i hedeften
            // dusuk/esitse) scale/pad/minterpolate zincirini tamamen ATLA - bu filtre
            // zinciri kaynak zaten uygunken bile her kareyi yeniden isliyordu, bu da
            // talimatin istedigi "gereksiz kalite kaybini onleme" ilkesine aykiriydi.
            // Filtre atlaninca video SADECE yeniden kodlanir (codec/bitrate hedefine
            // gore), cozunurluk/fps'e dokunulmaz - gercek bir stream-copy degil (codec
            // uyumlulugu bagimsiz dogrulanmadigi icin bu adim bilerek eklenmedi, bkz.
            // PROJE_ANALIZI_TR.md "Bilinen sinirlamalar"), ama gereksiz piksel-isleme
            // adimini (scale+pad+interpolate) ortadan kaldirarak kaliteyi daha iyi korur.
            val sourceMatchesTarget = sourceWidth > 0 && sourceHeight > 0 &&
                sourceWidth == settings.targetWidth && sourceHeight == settings.targetHeight &&
                (sourceFps <= 0.0 || sourceFps <= settings.targetFps + 0.5)
            val vf = if (sourceMatchesTarget) {
                null
            } else {
                // mi_mode=mci (tam hareket tahmini) telefon CPU'sunda 1080p'de asiri agir ve
                // bellek tasirip uygulamayi cokertebiliyordu. mi_mode=blend cok daha hafif
                // (basit kare harmanlama) - akicilik biraz daha az ama GUVENLI, cokmuyor.
                "scale=${settings.targetWidth}:${settings.targetHeight}:force_original_aspect_ratio=decrease," +
                    "pad=${settings.targetWidth}:${settings.targetHeight}:(ow-iw)/2:(oh-ih)/2," +
                    "minterpolate=fps=${settings.targetFps}:mi_mode=blend"
            }
            // Yuksek bitrate'lerde H.264 "High" profil + 5.1 seviye olmadan bazi
            // oynaticilar/TikTok'un kendi transcode'u sorun cikarabiliyor - bu yuzden
            // acikca belirtiyoruz. maxrate/bufsize, bitrate'in asiri tasmasini onler.
            val vfPart = if (vf != null) "-vf \"$vf\" " else ""
            "-y -i \"$inputPath\" $vfPart-b:v ${settings.videoBitrateKbps}k " +
                "-maxrate ${(settings.videoBitrateKbps * 1.2).toInt()}k -bufsize ${settings.videoBitrateKbps * 2}k " +
                "-profile:v high -level 5.1 -c:v libx264 -preset medium -c:a aac -b:a 256k -ar 44100 -ac 2 -movflags +faststart \"$outputPath\""
        }

        return FFmpegKit.executeAsync(
            cmd,
            { session: FFmpegSession ->
                when {
                    ReturnCode.isSuccess(session.returnCode) -> onProgress(Progress.Done(File(outputPath)))
                    // Kullanici cancel(session) cagirdiysa FFmpegKit gercek surece SIGXCPU/iptal
                    // gonderir ve donus kodu "cancel" olur - bu, bir hata degil, kullanicinin
                    // kendi istegiyle durdurdugu gercek bir islem. Ayri bir durum olarak bildirilir
                    // ki UI "basarisiz oldu" gibi yanlis bir hata mesaji gostermesin.
                    ReturnCode.isCancel(session.returnCode) -> onProgress(Progress.Cancelled)
                    else -> onProgress(Progress.Failed("Isleme basarisiz oldu (kod ${session.returnCode})."))
                }
            },
            { /* satir satir ffmpeg log'u - su an sessiz birakiliyor */ },
            { stats: Statistics ->
                if (durationMsHint > 0) {
                    val pct = ((stats.time.toDouble() / durationMsHint.toDouble()) * 100).toInt().coerceIn(0, 99)
                    onProgress(Progress.Running(pct))
                }
            }
        )
    }

    /** Devam eden GERCEK bir FFmpeg surecini durdurur (FFmpegKit.cancel ile) - sahte/
     *  gostermelik bir "iptal" degil, gercekten calisan process'e sinyal gonderir. */
    fun cancel(session: FFmpegSession?) {
        if (session != null) {
            FFmpegKit.cancel(session.sessionId)
        }
    }
}

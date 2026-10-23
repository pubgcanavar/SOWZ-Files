# Sow7Rix Studio - R8/ProGuard kurallari
# Amac: isim karartma (obfuscation) + kucultme ile kodu okumayi/kopyalamayi zorlastirmak,
# ayni zamanda reflection ile calisan kutuphanelerin (FFmpegKit) kirilmamasini saglamak.

# FFmpegKit, JNI/reflection ile kendi Java siniflarini cagiriyor - isimleri degismemeli.
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

# WebView JS<->Java koprusu kullanilirsa (su an kullanilmiyor ama ileride eklenebilir)
# diye @JavascriptInterface metotlarini koru.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Genel Android bilesenleri zaten AGP'nin varsayilan kurallarinda korunuyor
# (proguard-android-optimize.txt), ekstra bir sey eklemeye gerek yok.

# Hata ayiklama (crash) raporlarinda okunabilir stack trace icin satir numaralarini koru.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

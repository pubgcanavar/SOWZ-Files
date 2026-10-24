plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val keyStorePath=System.getenv("ANDROID_KEYSTORE_PATH")
val keyStorePassword=System.getenv("ANDROID_KEYSTORE_PASSWORD")
val keyAliasName=System.getenv("ANDROID_KEY_ALIAS")
val keyPasswordValue=System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseKey=!keyStorePath.isNullOrBlank() && !keyStorePassword.isNullOrBlank() &&
    !keyAliasName.isNullOrBlank() && !keyPasswordValue.isNullOrBlank() && file(keyStorePath).isFile
val signingPin=System.getenv("SIGNING_CERT_SHA256").orEmpty().filter { it.isLetterOrDigit() }.uppercase()
android {
    namespace="az.sow7rix.studio"
    compileSdk=35
    defaultConfig {
        applicationId="az.sow7rix.studio"
        minSdk=29
        targetSdk=35
        versionCode=5
        versionName="1.4.0"
        buildConfigField("String","EXPECTED_SIGNING_SHA256","\"$signingPin\"")
    }
    signingConfigs {
        if(hasReleaseKey) create("release") {
            storeFile=file(keyStorePath!!)
            storePassword=keyStorePassword
            keyAlias=keyAliasName
            keyPassword=keyPasswordValue
        }
    }
    buildTypes {
        release {
            isMinifyEnabled=true
            isShrinkResources=true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")
            signingConfig=if(hasReleaseKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility=JavaVersion.VERSION_17
        targetCompatibility=JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget="17" }
    buildFeatures { buildConfig=true }
    packaging { resources.excludes += setOf("META-INF/*.version","META-INF/DEPENDENCIES") }
    lint { abortOnError=false; checkReleaseBuilds=false }
}
dependencies {
    implementation("androidx.browser:browser:1.8.0")
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full-gpl:8.1.9")
}

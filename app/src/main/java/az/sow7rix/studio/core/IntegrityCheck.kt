package az.sow7rix.studio.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import az.sow7rix.studio.BuildConfig
import java.security.MessageDigest

/** Optional certificate pinning. The expected fingerprint comes from CI configuration. */
object IntegrityCheck {
    sealed class Result {
        object Valid : Result()
        object Tampered : Result()
        data class CheckFailed(val reason: String) : Result()
    }

    fun verify(context: Context): Result {
        return try {
            val expected = BuildConfig.EXPECTED_SIGNING_SHA256.trim().uppercase()
            if (expected.isBlank()) return Result.CheckFailed("Imza parmak izi yapilandirilmadi")

            val signatures = getSignatures(context)
            if (signatures.isEmpty()) return Result.CheckFailed("Imza bulunamadi")

            val match = signatures.any { signature ->
                val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                digest.joinToString("") { "%02X".format(it) } == expected
            }
            if (match) Result.Valid else Result.Tampered
        } catch (e: Exception) {
            Result.CheckFailed(e.message ?: "Bilinmeyen hata")
        }
    }

    @SuppressLint("PackageManagerGetSignatures")
    private fun getSignatures(context: Context): Array<Signature> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signing = info.signingInfo
            if (signing?.hasMultipleSigners() == true) signing.apkContentsSigners
            else signing?.signingCertificateHistory ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures ?: emptyArray()
        }
    }
}

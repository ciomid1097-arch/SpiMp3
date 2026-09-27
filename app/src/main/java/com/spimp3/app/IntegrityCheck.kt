package com.spimp3.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest

/**
 * Tamper guard: makes a repack-and-rebrand of this app expensive and unrewarding.
 *
 * Three cheap checks, run at process start and re-run on every resume:
 *
 *  1. **Signature pin** — on sideloaded installs the APK must carry OUR release
 *     key (the digest below). Apps installed by a store that re-signs uploads are
 *     exempt: Google Play and Myket both sign with their own key, and the package
 *     name is owner-unique on those stores, so the package check below already
 *     covers them.
 *  2. **Package pin** — the package must still be com.spimp3.app, so renaming
 *     and re-publishing a "fork" of the UI is blocked at the first gate.
 *  3. **Anti-debug** — a live debugger makes the guard fail, which blocks the
 *     easiest scriptable instrumentation path.
 *
 * On failure the app does NOT crash — crashing would only teach the attacker
 * where to look. Every screen loses its content ([enabled] gates the UI), so a
 * repacked build is an empty black window while looking merely "broken": the
 * least attractive target possible.
 *
 * After rotating the signing key, recompute [S] from the new certificate's
 * SHA-256 (apksigner verify --print-certs): split the 64 hex chars into eight
 * 4-byte chunks, byte-reverse each chunk, XOR with [K], store as Int.
 */
object IntegrityCheck {

    private const val K = 0x5A3C7E19

    // Release signing cert SHA-256 (581c4d91…0441455), obfuscated as above so
    // the digest never appears as a string constant a script could patch.
    private val S = intArrayOf(
        -881761727, -162674278, 1058329267, -1495161392,
        1522724921, -523620716, 609615857, 254294761,
    )

    // "com.spimp3.app", every char XOR 0x2A so the package never appears
    // verbatim in the constant pool.
    private val P = intArrayOf(
        73, 69, 71, 4, 89, 90, 67, 71, 90, 25, 4, 75, 90, 90,
    )

    // Installers that are allowed to bypass the signature check, because they may
    // re-sign the APK with their own key. An app that is not installed by one of
    // these still has to present the exact release certificate below.
    private val TRUSTED_INSTALLERS = setOf(
        "com.android.vending", // Google Play
        "com.myket.market",    // Myket (مایکت)
    )

    /** False once a tamper condition has been detected (recompose-observed). */
    var enabled: Boolean by mutableStateOf(true)
        private set

    fun check(context: Context) {
        if (BuildConfig.DEBUG) return // debug builds are never distributed
        if (!enabled) return
        enabled = signatureOk(context) && packageOk(context) && !debuggerAttached()
    }

    private fun signatureOk(context: Context): Boolean = runCatching {
        val pm = context.packageManager
        val pkg = context.packageName

        // Store installs may be re-signed by the store's own app-signing service
        // (Google Play App Signing does this, and Myket re-packages APKs for some
        // devices). When the app was installed by a known store, trust it.
        val installer = if (Build.VERSION.SDK_INT >= 30) {
            pm.getInstallSourceInfo(pkg).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(pkg)
        }
        if (installer in TRUSTED_INSTALLERS) return@runCatching true

        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
        }
        if (sigs == null || sigs.isEmpty()) return@runCatching false

        val sha = MessageDigest.getInstance("SHA-256").digest(sigs.first().toByteArray())
        if (sha.size != 32) return@runCatching false
        for (i in 0 until 8) {
            // Byte-reverse the i-th 4-byte chunk (mirrors how S was encoded).
            var v = 0
            for (b in 3 downTo 0) v = (v shl 8) or (sha[i * 4 + b].toInt() and 0xFF)
            if ((v xor K) != S[i]) return@runCatching false
        }
        true
    }.getOrDefault(false)

    private fun packageOk(context: Context): Boolean {
        // Decode the pinned package name at runtime only — the plaintext never
        // appears as a constant a patch script could search for.
        val expected = buildString { P.forEach { append((it xor 0x2A).toChar()) } }
        return context.packageName == expected
    }

    private fun debuggerAttached(): Boolean =
        Debug.isDebuggerConnected() || Debug.waitingForDebugger()
}

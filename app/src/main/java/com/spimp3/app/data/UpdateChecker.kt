package com.spimp3.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks GitHub Releases for a newer SpiMp3 build.
 *
 * Deliberately tiny: one HTTPS GET to the public releases endpoint, no SDKs,
 * no identifiers, nothing about the device or library in the request. Any
 * failure (offline, timeout, rate limit) silently yields null — the app keeps
 * working fully offline exactly as before.
 */
object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/ciomid1097-arch/SpiMp3/releases/latest"

    data class UpdateInfo(
        val versionCode: Long,
        val tagName: String,
        val apkUrl: String,
        val notes: String,
    )

    /** Null when offline / unreachable / unparsable — never throws. */
    suspend fun check(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(RELEASES_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "SpiMp3-update-check")
            try {
                if (conn.responseCode != 200) return@runCatching null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (json.optBoolean("draft", true) || json.optBoolean("prerelease", false)) {
                    return@runCatching null
                }
                val tag = json.optString("tag_name", "")
                val code = tag.removePrefix("v").toLongOrNull() ?: return@runCatching null
                if (code <= currentVersionCode) return@runCatching null
                val apk = json.getJSONArray("assets").let { arr ->
                    (0 until arr.length())
                        .asSequence()
                        .map { arr.getJSONObject(it) }
                        .firstOrNull { it.optString("name", "").endsWith(".apk") }
                        ?.optString("browser_download_url")
                } ?: return@runCatching null
                UpdateInfo(
                    versionCode = code,
                    tagName = tag,
                    apkUrl = apk,
                    notes = json.optString("body", "").ifBlank { "Bug fixes and improvements." },
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }
}

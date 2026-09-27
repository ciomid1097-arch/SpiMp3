package com.spimp3.app.data

/**
 * Bundled release notes, shown in the "What's new" dialog the first time a
 * given [android.os.Build.VERSION] code runs. Keyed by versionCode; add an
 * entry for every release. Fresh installs (no previous version seen) never
 * see the dialog — only real upgrades do.
 */
object WhatsNew {
    val notes: Map<Int, List<String>> = mapOf(
        2 to listOf(
            "Playlists, complete: pick or create one from any multi-selection or the song menu",
            "Rename and delete playlists from Home",
            "Queue and playlist actions now confirm with a snackbar",
            "Update notifications: SpiMp3 now tells you when a new version is out",
            "Stronger tamper protection and code obfuscation",
        ),
        3 to listOf(
            "Full playlist power: rename, delete and reorder from inside a playlist",
            "Play counts per song, visible in song info",
            "Automatic update check with a one-tap download link",
        ),
        4 to listOf(
            "Smoother update experience: the What's new note now appears exactly once per version",
            "Update banner got a dismiss button — hide it until the next app launch",
        ),
        5 to listOf(
            "Properly signed release build — the app is now published on Myket",
            "Fixed media-session crash on unknown external commands",
        ),
    )
}

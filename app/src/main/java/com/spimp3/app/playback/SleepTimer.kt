package com.spimp3.app.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Simple cancellable sleep timer. `endOfQueue` mode has no deadline; it is checked by the player listener. */
class SleepTimer(
    val endOfQueue: Boolean,
    private val fireAtElapsedRealtime: Long,
) {
    private var job: Job? = null
    @Volatile
    private var cancelled = false

    fun start(scope: CoroutineScope, onFire: () -> Unit) {
        cancelled = false
        job = scope.launch {
            val remaining = fireAtElapsedRealtime - android.os.SystemClock.elapsedRealtime()
            if (remaining > 0) delay(remaining)
            if (!cancelled) onFire()
        }
    }

    fun cancel() {
        cancelled = true
        job?.cancel()
        job = null
    }
}

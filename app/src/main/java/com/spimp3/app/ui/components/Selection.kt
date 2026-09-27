package com.spimp3.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Long-press multi-selection.
 *
 * Exposed as a CompositionLocal so that every song list in the app (home,
 * library tabs, search results, album/artist/folder detail, playlists, smart
 * lists) gets selection for free — no extra parameters threaded through a
 * dozen screen signatures.
 */
data class SelectionState(
    /** Song ids currently ticked. */
    val selected: Set<Long> = emptySet(),
    /** True once the user has long-pressed something and not yet dismissed. */
    val active: Boolean = false,
    /** Ids visible on the calling list, used by "Select all". */
    val universe: List<Long> = emptyList(),
    val onStart: (Long) -> Unit = {},
    val onToggle: (Long) -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onClear: () -> Unit = {},
    /** Called by [SelectionUniverse] so "Select all" knows the visible list. */
    val onRegister: (List<Long>) -> Unit = { },
) {
    val count: Int get() = selected.size
    val allSelected: Boolean get() = universe.isNotEmpty() && selected.containsAll(universe)
}

val LocalSelection = staticCompositionLocalOf { SelectionState() }

/**
 * Declares the ids currently on screen so the action bar's "All" button can tick
 * exactly what the user can see. Cheap and safe to call unconditionally: the
 * effect only re-runs when [ids] actually changes.
 */
@Composable
fun SelectionUniverse(ids: List<Long>) {
    val selection = LocalSelection.current
    LaunchedEffect(ids) { selection.onRegister(ids) }
}

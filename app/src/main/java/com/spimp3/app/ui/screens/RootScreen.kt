package com.spimp3.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.spimp3.app.data.SongDeleter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spimp3.app.MainViewModel
import com.spimp3.app.IntegrityCheck
import com.spimp3.app.data.Playlist
import com.spimp3.app.data.Song
import com.spimp3.app.ui.components.LocalSelection
import com.spimp3.app.ui.components.MiniPlayer
import com.spimp3.app.ui.components.PermissionGate
import com.spimp3.app.ui.components.PlaylistPickerSheet
import com.spimp3.app.ui.components.SelectionActionBar
import com.spimp3.app.ui.components.SelectionState
import com.spimp3.app.ui.components.SongMenuSheet
import com.spimp3.app.ui.theme.accentColor

object Routes {
    const val HOME = "home"
    const val NOW_PLAYING = "now_playing"
    const val ALBUM_DETAIL = "album/{albumId}"
    const val ARTIST_DETAIL = "artist/{artistId}"
    const val FOLDER_DETAIL = "folder/{encoded}"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val SMART_LIST = "list/{title}/{ids}"
    const val LEGAL = "legal/{doc}"

    fun legal(doc: LegalDoc) = "legal/${doc.name}"

    fun album(id: Long) = "album/$id"
    fun artist(id: Long) = "artist/$id"
    fun playlist(id: Long) = "playlist/$id"
    fun folder(path: String): String =
        "folder/" + android.util.Base64.encodeToString(
            path.toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP,
        )

    fun decodeFolder(encoded: String): String =
        String(android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))

    fun smartList(title: String, songIds: List<Long>): String {
        val t = android.net.Uri.encode(title)
        val ids = songIds.joinToString(",")
        return "list/$t/$ids"
    }

    const val EDIT_SONG = "edit_song/{songId}"
    fun editSong(id: Long) = "edit_song/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun RootScreen(vm: MainViewModel, debugRoute: String? = null) {
    val nav = rememberNavController()
    val player = vm.player
    val songId by player.currentSongId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val library by vm.library.collectAsState()
    val hasPermission by vm.hasPermission.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    var menuSong by remember { mutableStateOf<Song?>(null) }
    var scrollSettingsToBottom by remember { mutableStateOf(false) }
    var menuPlaylistId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val playlists by vm.playlists.collectAsState()

    // ---- "Add to playlist" picker ----
    // Opened from the multi-select bar (songs = whole selection) and from the
    // song menu (songs = the one song). The sheet lists existing playlists and
    // can create a new one with the songs already inside.
    var pickerSongs by remember { mutableStateOf<List<Long>>(emptyList()) }
    val pickerVisible = pickerSongs.isNotEmpty()
    fun openPlaylistPicker(ids: List<Long>) {
        if (ids.isNotEmpty()) pickerSongs = ids
    }
    fun closePlaylistPicker() {
        pickerSongs = emptyList()
    }
    fun addSongsToPlaylist(pl: Playlist, ids: List<Long>) {
        val already = pl.songIds.containsAll(ids) && ids.isNotEmpty()
        vm.addToPlaylist(pl, ids)
        closePlaylistPicker()
        scope.launch {
            snackbarHost.showSnackbar(
                if (already) "Already in \"${pl.name}\""
                else "Added ${ids.size} song${if (ids.size == 1) "" else "s"} to \"${pl.name}\"",
            )
        }
    }
    fun createPlaylistAndAdd(name: String, ids: List<Long>) {
        vm.createPlaylist(name.ifBlank { "New Playlist" }, ids) { pl ->
            scope.launch { snackbarHost.showSnackbar("Created \"${pl.name}\" with ${ids.size} song${if (ids.size == 1) "" else "s"}") }
        }
        closePlaylistPicker()
    }

    // Deletion always goes through the system's own consent dialog (Android 11+).
    var pendingDelete by remember { mutableStateOf<Song?>(null) }
    var pendingDeleteUris by remember { mutableStateOf<List<String>>(emptyList()) }
    val deleteConsent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val target = pendingDelete
        val n = pendingDeleteUris.size
        pendingDelete = null
        pendingDeleteUris = emptyList()
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            if (target != null && vm.player.currentSongId.value == target.id) {
                vm.player.controller?.stop()
            }
            scope.launch {
                vm.refreshPermission()
                snackbarHost.showSnackbar(
                    if (n > 1) "Deleted $n songs" else "Deleted \"${target?.title ?: "song"}\""
                )
            }
        }
    }

    val song = songId?.let { library.songById[it] }

    // ---- long-press multi-select ----
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectionUniverse by remember { mutableStateOf(emptyList<Long>()) }
    val selectionActive = selectedIds.isNotEmpty()

    fun clearSelection() {
        selectedIds = emptySet()
    }

    // System back closes an active selection before it navigates away.
    BackHandler(enabled = selectionActive) { clearSelection() }

    val selectionState = SelectionState(
        selected = selectedIds,
        active = selectionActive,
        universe = selectionUniverse,
        onStart = { id -> selectedIds = setOf(id) },
        onToggle = { id ->
            selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        },
        onSelectAll = {
            selectedIds =
                if (selectedIds.containsAll(selectionUniverse)) emptySet()
                else selectionUniverse.toSet()
        },
        onClear = ::clearSelection,
        onRegister = { ids ->
            // Only matters while a selection is running, and only if it changed.
            if (selectionUniverse != ids) selectionUniverse = ids
        },
    )

    val tabs = listOf(
        Tab(Routes.HOME, "Home", Icons.Rounded.MusicNote),
        Tab("library_songs", "Songs", Icons.Rounded.LibraryMusic),
        Tab("library_albums", "Albums", Icons.Rounded.Album),
        Tab("library_artists", "Artists", Icons.Rounded.QueueMusic),
        Tab("library_folders", "Folders", Icons.Rounded.Folder),
    )
    val showBottomBar = currentRoute in tabs.map { it.route }

    val onSongClick: (Song, List<Song>) -> Unit = { s, queueList ->
        val idx = queueList.indexOfFirst { it.id == s.id }.coerceAtLeast(0)
        player.playQueue(queueList.map { it.id }, idx)
    }

    // Debug-only test hook (ignored in release builds).
    LaunchedEffect(debugRoute, library.songs) {
        val route = debugRoute ?: return@LaunchedEffect
        when {
            route.startsWith("menu:") -> {
                menuSong = library.songById[route.removePrefix("menu:").toLongOrNull() ?: -1L]
            }
            route.startsWith("edit:") -> {
                val id = route.removePrefix("edit:").toLongOrNull() ?: -1L
                if (library.songById.containsKey(id)) nav.navigate(Routes.editSong(id))
            }
            route == "settings" -> nav.navigate(Routes.SETTINGS)
            route == "settings:bottom" -> {
                nav.navigate(Routes.SETTINGS)
                scrollSettingsToBottom = true
            }
            route.startsWith("play:") -> {
                val id = route.removePrefix("play:").toLongOrNull() ?: -1L
                if (library.songById.containsKey(id)) player.playSong(id)
            }
            route.startsWith("playnow:") -> {
                val id = route.removePrefix("playnow:").toLongOrNull() ?: -1L
                if (library.songById.containsKey(id)) {
                    player.playSong(id)
                    nav.navigate(Routes.NOW_PLAYING)
                }
            }
            route.startsWith("album:") -> {
                val id = route.removePrefix("album:").toLongOrNull() ?: -1L
                if (library.albumById.containsKey(id)) nav.navigate(Routes.album(id))
            }
            route.startsWith("artist:") -> {
                val id = route.removePrefix("artist:").toLongOrNull() ?: -1L
                if (library.artistById.containsKey(id)) nav.navigate(Routes.artist(id))
            }
            route == "songs" -> nav.navigate("library_songs")
            route.startsWith("sel:") -> {
                selectedIds = route.removePrefix("sel:").split(',')
                    .mapNotNull { it.toLongOrNull() }.toSet()
            }
            route == "albums" -> nav.navigate("library_albums")
            route == "artists" -> nav.navigate("library_artists")
            route == "folders" -> nav.navigate("library_folders")
            route == "search" -> nav.navigate(Routes.SEARCH)
            route == "nowplaying" -> nav.navigate(Routes.NOW_PLAYING)
        }
    }

    // Content gate: a tampered/repacked build renders an empty black window.
    if (!IntegrityCheck.enabled) {
        Box(Modifier.fillMaxSize())
        return
    }

    PermissionGate(
        hasPermission = hasPermission,
        loading = library.loading,
        songCount = library.songs.size,
        onRequest = {
            // The activity re-requests on resume; gate button is informational.
        },
    ) {
        CompositionLocalProvider(LocalSelection provides selectionState) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (selectionActive) {
                    val selSongs = selectedIds.mapNotNull { library.songById[it] }
                    val allFav = selSongs.isNotEmpty() && selSongs.all { it.id in favorites }
                    SelectionActionBar(
                        count = selectedIds.size,
                        allSelected = selectionState.allSelected,
                        allFavorited = allFav,
                        onDismiss = ::clearSelection,
                        onPlay = {
                            val ids = selSongs.map { it.id }
                            clearSelection()
                            if (ids.isNotEmpty()) player.playQueue(ids, 0)
                        },
                        onAddToQueue = {
                            val ids = selSongs.map { it.id }
                            clearSelection()
                            if (ids.isNotEmpty()) {
                                player.addToQueue(ids)
                                scope.launch {
                                    snackbarHost.showSnackbar(
                                        "Added ${ids.size} song${if (ids.size == 1) "" else "s"} to queue",
                                    )
                                }
                            }
                        },
                        onToggleFavorite = {
                            val ids = selSongs.map { it.id }.toSet()
                            val add = !allFav
                            clearSelection()
                            vm.setFavorites(ids, add)
                        },
                        onAddToPlaylist = {
                            val ids = selSongs.map { it.id }
                            openPlaylistPicker(ids)
                        },
                        onShare = {
                            val uris = selSongs.map { it.contentUri }
                            clearSelection()
                            if (uris.isNotEmpty()) {
                                val send = if (uris.size == 1) {
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uris.first())
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                } else {
                                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "audio/*"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                }
                                context.startActivity(Intent.createChooser(send, "Share audio"))
                            }
                        },
                        onDelete = {
                            val targets = selSongs
                            clearSelection()
                            if (targets.isEmpty()) return@SelectionActionBar
                            val sender = SongDeleter.createDeleteRequest(
                                context, targets.map { it.contentUri },
                            )
                            if (sender != null) {
                                pendingDelete = targets.first()
                                pendingDeleteUris = targets.map { it.contentUri.toString() }
                                runCatching {
                                    deleteConsent.launch(IntentSenderRequest.Builder(sender).build())
                                }.onFailure {
                                    pendingDelete = null
                                    pendingDeleteUris = emptyList()
                                    var ok = 0
                                    targets.forEach { t ->
                                        if (SongDeleter.deleteDirect(context, t.contentUri) is
                                            SongDeleter.Result.Deleted
                                        ) ok++
                                    }
                                    scope.launch {
                                        vm.refreshPermission()
                                        snackbarHost.showSnackbar("Deleted $ok songs")
                                    }
                                }
                            } else {
                                var ok = 0
                                targets.forEach { t ->
                                    if (SongDeleter.deleteDirect(context, t.contentUri) is
                                        SongDeleter.Result.Deleted
                                    ) ok++
                                }
                                scope.launch {
                                    vm.refreshPermission()
                                    snackbarHost.showSnackbar("Deleted $ok songs")
                                }
                            }
                        },
                        onSelectAll = selectionState.onSelectAll,
                    )
                } else if (showBottomBar) {
                    Column {
                        MiniPlayer(
                            player = player,
                            songTitle = song?.title,
                            songArtist = song?.artist,
                            albumId = song?.albumId,
                            onOpenNowPlaying = { nav.navigate(Routes.NOW_PLAYING) },
                        )
                        NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = {
                                        nav.navigate(tab.route) {
                                            popUpTo(Routes.HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = accentColor(),
                                        selectedTextColor = accentColor(),
                                        indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(bottom = padding.calculateBottomPadding())) {
                NavHost(
                    navController = nav,
                    startDestination = Routes.HOME,
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260))
                    },
                    exitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260))
                    },
                    popEnterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260))
                    },
                    popExitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260))
                    },
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            vm = vm,
                            onSongClick = onSongClick,
                            onOpenMenu = { menuSong = it },
                            onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                            onOpenSearch = { nav.navigate(Routes.SEARCH) },
                            onOpenAlbum = { nav.navigate(Routes.album(it)) },
                            onOpenArtist = { nav.navigate(Routes.artist(it)) },
                            onOpenFolder = { nav.navigate(Routes.folder(it)) },
                            onOpenPlaylist = { nav.navigate(Routes.playlist(it)) },
                            onShowList = { title, list ->
                                nav.navigate(Routes.smartList(title, list.map { it.id }))
                            },
                            onShuffleAll = {
                                val all = library.songs.shuffled()
                                if (all.isNotEmpty()) onSongClick(all.first(), all)
                            },
                        )
                    }
                    composable(Routes.SMART_LIST) { entry ->
                        val title = entry.arguments?.getString("title") ?: ""
                        val ids = entry.arguments?.getString("ids")
                            ?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
                        SmartListScreen(
                            title = title,
                            songIds = ids,
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onSongClick = onSongClick,
                            onOpenMenu = { menuSong = it },
                        )
                    }
                    composable("library_songs") {
                        SongsScreen(vm, onSongClick, { menuSong = it })
                    }
                    composable("library_albums") {
                        AlbumsScreen(vm) { nav.navigate(Routes.album(it)) }
                    }
                    composable("library_artists") {
                        ArtistsScreen(vm) { nav.navigate(Routes.artist(it)) }
                    }
                    composable("library_folders") {
                        FoldersScreen(vm) { nav.navigate(Routes.folder(it)) }
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onOpenLegal = { nav.navigate(Routes.legal(it)) },
                            debugScrollToBottom = scrollSettingsToBottom,
                        )
                    }
                    composable(Routes.LEGAL) { entry ->
                        LegalScreen(
                            doc = runCatching {
                                LegalDoc.valueOf(entry.arguments?.getString("doc").orEmpty())
                            }.getOrDefault(LegalDoc.PRIVACY_POLICY),
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(Routes.SEARCH) {
                        SearchScreen(
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onSongClick = onSongClick,
                            onOpenMenu = { menuSong = it },
                            onOpenAlbum = { nav.navigate(Routes.album(it)) },
                            onOpenArtist = { nav.navigate(Routes.artist(it)) },
                        )
                    }
                    composable(Routes.EDIT_SONG) { entry ->
                        val id = entry.arguments?.getString("songId")?.toLongOrNull() ?: return@composable
                        EditSongScreen(vm = vm, songId = id, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.ALBUM_DETAIL) { entry ->
                        val id = entry.arguments?.getString("albumId")?.toLongOrNull() ?: return@composable
                        AlbumDetailScreen(
                            vm = vm,
                            albumId = id,
                            onSongClick = onSongClick,
                            onOpenMenu = { menuSong = it },
                            onOpenArtist = { nav.navigate(Routes.artist(it)) },
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(Routes.ARTIST_DETAIL) { entry ->
                        val id = entry.arguments?.getString("artistId")?.toLongOrNull() ?: return@composable
                        ArtistDetailScreen(vm, id, onSongClick, { menuSong = it }, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.FOLDER_DETAIL) { entry ->
                        val encoded = entry.arguments?.getString("encoded") ?: return@composable
                        FolderDetailScreen(vm, Routes.decodeFolder(encoded), onSongClick, { menuSong = it }, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.PLAYLIST_DETAIL) { entry ->
                        val id = entry.arguments?.getString("playlistId")?.toLongOrNull() ?: return@composable
                        PlaylistDetailScreen(vm, id, onSongClick, { menuSong = it }, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.NOW_PLAYING) {
                        NowPlayingScreen(
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onSongClick = { s, list -> onSongClick(s, list) },
                            onOpenMenu = { menuSong = it },
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
        PlaylistPickerSheet(
            visible = pickerVisible,
            playlists = playlists,
            onDismiss = ::closePlaylistPicker,
            onCreateAndAdd = { name -> createPlaylistAndAdd(name, pickerSongs) },
            onAddTo = { pl -> addSongsToPlaylist(pl, pickerSongs) },
        )
        SongMenuSheet(
            song = menuSong,
            isFavorite = menuSong?.id in favorites,
            inPlaylistId = menuPlaylistId,
            onDismiss = { menuSong = null; menuPlaylistId = null },
            onPlayNext = { player.playNext(listOf(it.id)); menuSong = null },
            onAddToQueue = {
                player.addToQueue(listOf(it.id)); menuSong = null
                scope.launch { snackbarHost.showSnackbar("Added to queue") }
            },
            onAddToPlaylist = {
                openPlaylistPicker(listOf(it.id))
            },
            onToggleFavorite = { vm.toggleFavorite(it.id) },
            onShare = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, it.contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Share audio"))
                menuSong = null
            },
            onEdit = {
                menuSong = null
                menuPlaylistId = null
                nav.navigate(Routes.editSong(it.id))
            },
            onDelete = { target ->
                menuSong = null
                val sender = SongDeleter.createDeleteRequest(context, target.contentUri)
                if (sender != null) {
                    pendingDelete = target
                    runCatching { deleteConsent.launch(IntentSenderRequest.Builder(sender).build()) }
                        .onFailure {
                            pendingDelete = null
                            // Fall back to a direct delete on older devices
                            val res = SongDeleter.deleteDirect(context, target.contentUri)
                            if (res is SongDeleter.Result.Deleted) vm.refreshPermission()
                        }
                } else {
                    when (SongDeleter.deleteDirect(context, target.contentUri)) {
                        SongDeleter.Result.Deleted -> {
                            scope.launch {
                                vm.refreshPermission()
                                snackbarHost.showSnackbar("Deleted \"${target.title}\"")
                            }
                        }
                        is SongDeleter.Result.Failed ->
                            scope.launch { snackbarHost.showSnackbar("Delete not allowed on this Android version") }
                        SongDeleter.Result.Cancelled -> Unit
                    }
                }
            },
            onRemoveFromPlaylist = menuPlaylistId?.let { pid ->
                { target ->
                    vm.playlists.value.firstOrNull { it.id == pid }?.let { vm.removeFromPlaylist(it, target.id) }
                    menuSong = null
                }
            },
            onShowInfo = { },
        )

        SnackbarHost(
            snackbarHost,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp, start = 12.dp, end = 12.dp),
        )                { data -> Snackbar(snackbarData = data) }
        }
        }
    }
}

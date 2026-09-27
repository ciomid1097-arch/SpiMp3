package com.spimp3.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.spimp3.app.ui.screens.RootScreen
import com.spimp3.app.ui.theme.SpiMp3Theme
import com.spimp3.app.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.refreshPermission()
        }

    override fun onResume() {
        super.onResume()
        IntegrityCheck.check(this)
        vm.refreshPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IntegrityCheck.check(this)
        setContent {
            // Tampered/repacked builds render an empty screen instead of crashing.
            if (!IntegrityCheck.enabled) return@setContent
            val settings by vm.settings.collectAsState()
            val hasPermission by vm.hasPermission.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val mode = ThemeMode.from(settings.themeMode)

            LaunchedEffect(hasPermission) {
                if (!hasPermission) {
                    val perm = if (Build.VERSION.SDK_INT >= 33) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(perm)
                }
            }

            // Keep the status/navigation bars readable in both themes.
            val dark = when (mode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            SideEffect {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(transparent, transparent) { dark },
                    navigationBarStyle = SystemBarStyle.auto(transparent, transparent) { dark },
                )
            }

            SpiMp3Theme(accentKey = settings.accent, themeMode = mode, systemInDark = systemDark) {
                RootScreen(vm, debugRoute = debugRoute)
            }
        }
    }

    /**
     * Debug-only test hook: lets tooling open a specific screen/menu without
     * tapping, e.g. `--es spimp3_debug_route "menu:1234"`. Ignored in release.
     */
    private val debugRoute: String?
        get() = if (BuildConfig.DEBUG) intent?.getStringExtra("spimp3_debug_route") else null

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

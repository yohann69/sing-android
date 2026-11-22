package io.github.yohann69.sing

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.github.yohann69.sing.ui.ConnectScreen
import io.github.yohann69.sing.ui.ForYouScreen
import io.github.yohann69.sing.ui.NowPlayingScreen
import io.github.yohann69.sing.ui.QueueScreen
import io.github.yohann69.sing.ui.SearchScreen
import io.github.yohann69.sing.ui.SettingsScreen
import io.github.yohann69.sing.ui.SingTheme
import io.github.yohann69.sing.ui.SingerPicker
import io.github.yohann69.sing.ui.StatsScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: PadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (resources.configuration.smallestScreenWidthDp >= 600) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        handle(intent)
        if (android.os.Build.VERSION.SDK_INT >= 33) registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        setContent { SingTheme { App(vm, ::scan) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.dataString?.let(vm::joinLink)
    }

    private fun scan() {
        GmsBarcodeScanning.getClient(this).startScan()
            .addOnSuccessListener { b -> b.rawValue?.let(vm::joinLink) }
            .addOnFailureListener { vm.showToast("The scanner is not available on this device. Type the code instead.") }
    }
}

enum class Dest(val label: String) { Now("Now"), Queue("Queue"), Search("Search"), ForYou("For You"), Stats("Stats"), Settings("Settings") }

@Composable
fun App(vm: PadViewModel, scan: () -> Unit) {
    val saved by vm.saved.collectAsState()
    val toast by vm.toast.collectAsState()
    Box(Modifier.fillMaxSize()) {
        if (saved == null) ConnectScreen(vm, scan) else Main(vm)
        toast?.let {
            LaunchedEffect(it) { delay(2800); vm.clearToast() }
            Snackbar(Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(16.dp)) { Text(it) }
        }
    }
}

@Composable
fun Main(vm: PadViewModel) {
    var dest by rememberSaveable { mutableStateOf(Dest.Now) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val me by vm.singerId.collectAsState()
    val state by vm.state.collectAsState()
    val wide = currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    LaunchedEffect(state.singers, me) { if (state.singers.isNotEmpty() && state.singers.none { it.id == me }) picking = true }
    NavigationSuiteScaffold(navigationSuiteItems = {
        Dest.entries.forEach { d ->
            item(
                selected = dest == d, onClick = { dest = d }, label = { Text(d.label) },
                icon = {
                    Icon(when (d) {
                        Dest.Now -> Icons.Rounded.Mic; Dest.Queue -> Icons.Rounded.QueueMusic; Dest.Search -> Icons.Rounded.Search
                        Dest.ForYou -> Icons.Rounded.AutoAwesome; Dest.Stats -> Icons.Rounded.BarChart; Dest.Settings -> Icons.Rounded.Tune
                    }, d.label)
                },
            )
        }
    }) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (dest) {
                Dest.Now -> NowPlayingScreen(vm, wide)
                Dest.Queue -> QueueScreen(vm)
                Dest.Search -> SearchScreen(vm)
                Dest.ForYou -> ForYouScreen(vm)
                Dest.Stats -> StatsScreen(vm)
                Dest.Settings -> SettingsScreen(vm)
            }
        }
    }
    if (picking) SingerPicker(vm) { picking = false }
}

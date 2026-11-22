package io.github.yohann69.sing.ui

import io.github.yohann69.sing.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.yohann69.sing.PadViewModel
import io.github.yohann69.sing.protocol.PlaybackSettings
import io.github.yohann69.sing.protocol.SingerPrefs
import io.github.yohann69.sing.protocol.YearInKaraoke
import io.github.yohann69.sing.protocol.micPresets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinDialog(vm: PadViewModel) {
    val s by vm.pinFor.collectAsState()
    val singer = s ?: return
    var pin by remember { mutableStateOf("") }
    AlertDialog({ vm.cancelPin() }, title = { Text("PIN for ${singer.name}") },
        text = { OutlinedTextField(pin, { pin = it.filter(Char::isDigit) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)) },
        confirmButton = { Button({ vm.unlock(pin) }, enabled = pin.length >= 4) { Text(tr("Unlock")) } },
        dismissButton = { TextButton({ vm.cancelPin() }) { Text(tr("Cancel")) } })
}

private fun noteName(m: Double): String {
    val names = listOf("C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")
    val n = Math.round(m).toInt()
    return names[((n % 12) + 12) % 12] + (n / 12 - 1)
}

@Composable
fun YearCard(y: YearInKaraoke) = GlassCard(Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${y.year} in karaoke", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("${y.songs} songs · ${y.minutes} minutes · ${y.nights} nights", fontWeight = FontWeight.SemiBold)
        y.topSong?.let { Text("Most sung: $it") }
        y.topArtist?.let { Text("Favourite artist: $it") }
        if (y.bestScore != null && y.bestScoreSong != null) Text("Best score: ${y.bestScore} on ${y.bestScoreSong}")
        y.favoriteDuetPartner?.let { Text("Favourite duet partner: $it") }
    }
}

/** The current singer's own profile: privacy, sound, PIN and their year. */
@Composable
fun ProfileDialog(vm: PadViewModel, dismiss: () -> Unit) {
    val state by vm.state.collectAsState()
    val me by vm.singerId.collectAsState()
    val singer = state.singers.firstOrNull { it.id == me } ?: return dismiss()
    var p by remember { mutableStateOf(singer.prefs ?: SingerPrefs()) }
    var year by remember { mutableStateOf<YearInKaraoke?>(null) }
    var pin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(singer.id) { year = runCatching { vm.client?.singerStats(singer.id) }.getOrNull()?.year }
    fun save(n: SingerPrefs) { p = n; scope.launch { runCatching { vm.client?.updateSingerPrefs(singer.id, n) }.onFailure { vm.showToast(it.message ?: "") } } }
    Dialog(dismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Ember.background)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Row(verticalAlignment = Alignment.CenterVertically) { Text(singer.name, style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f)); IconButton(dismiss) { Icon(Icons.Rounded.Close, "Close") } } }
                year?.let { y -> item { YearCard(y) } }
                item { Toggle2(tr("Hide my score"), p.hideScore) { save(p.copy(hideScore = it)) } }
                item { Toggle2(tr("Leave me out of leaderboards"), p.hideFromLeaderboard) { save(p.copy(hideFromLeaderboard = it)) } }
                item { Toggle2(tr("Leave me out of the stats"), p.hideStats) { save(p.copy(hideStats = it)) } }
                item {
                    Text(tr("My mic effect"), Modifier.padding(top = 12.dp), color = Ember.accentSoft)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(p.micPreset == null, { save(p.copy(micPreset = null)) }, { Text(tr("Party setting")) })
                        micPresets.forEach { (k, l) -> FilterChip(p.micPreset == k, { save(p.copy(micPreset = k)) }, { Text(l) }) }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Key for my songs: ") + if (p.keyOffset == 0) "original" else "%+d".format(p.keyOffset), Modifier.weight(1f))
                        TextButton({ save(p.copy(keyOffset = (p.keyOffset - 1).coerceAtLeast(-6))) }) { Text("−") }
                        TextButton({ save(p.copy(keyOffset = (p.keyOffset + 1).coerceAtMost(6))) }) { Text("+") }
                    }
                }
                if (p.rangeLow != null && p.rangeHigh != null) item { Text("Comfortable range: ${noteName(p.rangeLow!!)} to ${noteName(p.rangeHigh!!)}") }
                item {
                    Text(tr("Profile PIN"), Modifier.padding(top = 12.dp), color = Ember.accentSoft)
                    OutlinedTextField(pin, { pin = it.filter(Char::isDigit) }, label = { Text(if (p.hasPIN) "PIN set, type a new one" else "Add a PIN") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    Row {
                        TextButton({ scope.launch { runCatching { vm.client?.setSingerPIN(singer.id, pin) }; pin = ""; p = p.copy(hasPIN = true) } }, enabled = pin.length >= 4) { Text(if (p.hasPIN) "Change PIN" else "Set PIN") }
                        if (p.hasPIN) TextButton({ scope.launch { runCatching { vm.client?.setSingerPIN(singer.id, null) }; p = p.copy(hasPIN = false) } }) { Text(tr("Remove PIN"), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Toggle2(label: String, value: Boolean, onChange: (Boolean) -> Unit) =
    ListItem({ Text(label) }, trailingContent = { Switch(value, onChange) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))

/** Mic inputs: per-mic volume, which mic each singer uses, pitch correction. */
@Composable
fun MicSection(vm: PadViewModel, p: PlaybackSettings) {
    val meters by vm.meters.collectAsState()
    val channels = meters.size.coerceAtLeast(1)
    Column {
        if (channels > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(tr("Separate volume per mic"), Modifier.weight(1f)); Switch(p.separateMics, { v -> vm.updateSettings { it.copy(playback = it.playback.copy(separateMics = v)) } }) }
            if (p.separateMics) for (c in 0 until channels.coerceAtMost(4)) {
                Text("Mic ${c + 1}: ${((p.micChannelGains.getOrElse(c) { 1.0 }) * 100).toInt()}%")
                Slider(p.micChannelGains.getOrElse(c) { 1.0 }.toFloat(), { v ->
                    vm.updateSettings { s -> s.copy(playback = s.playback.copy(micChannelGains = List(4) { i -> if (i == c) v.toDouble() else s.playback.micChannelGains.getOrElse(i) { 1.0 } })) }
                }, valueRange = 0f..1.5f)
            }
            for (slot in 0 until channels.coerceAtMost(4)) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (slot == 0) "First singer" else "Singer ${slot + 1}", Modifier.width(110.dp))
                for (c in 0 until channels.coerceAtMost(4)) FilterChip(p.micSlots.getOrElse(slot) { slot } == c, {
                    vm.updateSettings { s -> s.copy(playback = s.playback.copy(micSlots = List(4) { i -> if (i == slot) c else s.playback.micSlots.getOrElse(i) { i } })) }
                }, { Text("Mic ${c + 1}") })
            }
        }
        Text(if (p.pitchCorrection == 0.0) "Pitch correction: off" else "Pitch correction: ${(p.pitchCorrection * 100).toInt()}%")
        Slider(p.pitchCorrection.toFloat(), { v -> vm.updateSettings { it.copy(playback = it.playback.copy(pitchCorrection = v.toDouble())) } })
    }
}

/** Big synced lyrics for whoever holds this device while they sing. */
@Composable
fun TeleprompterDialog(vm: PadViewModel, dismiss: () -> Unit) {
    val state by vm.state.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(33) } }
    val pos = state.nowPlaying.estimatedPositionMs(now) / 1000.0
    Dialog(dismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize()) {
            BlobBackground(state.nowPlaying.entry?.song?.palette)
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                val l = lyrics
                val i = l?.lineIndex(pos + 0.15)
                val prev = i?.let { l.lines.getOrNull(it - 1) }
                prev?.let { Text(it.text, fontSize = 26.sp, color = Ember.text.copy(alpha = 0.4f), textAlign = TextAlign.Center) }
                Spacer(Modifier.height(18.dp))
                Text(if (l != null && i != null) karaoke(l.lines[i], pos) else androidx.compose.ui.text.buildAnnotatedString { append(state.nowPlaying.currentLine ?: "♪") },
                    style = LyricStyle.copy(fontSize = 44.sp, lineHeight = 52.sp), textAlign = TextAlign.Center)
                i?.let { l?.lines?.getOrNull(it)?.romanization }?.let { Text(it, fontSize = 22.sp, color = Ember.text.copy(alpha = 0.7f)) }
                Spacer(Modifier.height(18.dp))
                val next = i?.let { l?.lines?.getOrNull(it + 1) } ?: l?.lines?.firstOrNull()?.takeIf { i == null }
                next?.let { Text(it.text, fontSize = 28.sp, color = Ember.text.copy(alpha = 0.55f), textAlign = TextAlign.Center) }
            }
            IconButton(dismiss, Modifier.align(Alignment.TopEnd).padding(20.dp)) { Icon(Icons.Rounded.Close, "Close") }
        }
    }
}

package io.github.yohann69.sing.ui

import io.github.yohann69.sing.tr

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.yohann69.sing.PadViewModel
import io.github.yohann69.sing.protocol.ClearCacheRequest
import kotlinx.coroutines.launch

@Composable
fun RatingRow(vm: PadViewModel, songId: Long) {
    val ratings by vm.ratings.collectAsState()
    val r = ratings[songId]
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalIconToggleButton(r?.favorite == true, { vm.rate(songId) { it.copy(favorite = !it.favorite) } }) {
            Icon(if (r?.favorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = if (r?.favorite == true) Color(0xFFFF6B9A) else Ember.text)
        }
        FilledTonalIconToggleButton(r?.value == 1, { vm.rate(songId) { it.copy(value = if (it.value == 1) 0 else 1) } }) { Icon(Icons.Rounded.ThumbUp, "Like") }
        FilledTonalIconToggleButton(r?.value == -1, { vm.rate(songId) { it.copy(value = if (it.value == -1) 0 else -1) } }) { Icon(Icons.Rounded.ThumbDown, "Dislike") }
        FilledTonalIconToggleButton(r?.neverSuggest == true, { vm.rate(songId) { it.copy(neverSuggest = !it.neverSuggest) } }) { Icon(Icons.Rounded.VisibilityOff, "Never suggest") }
    }
}

private val categoryColors = mapOf(
    "stems" to Color(0xFFFF8A3D), "analysis" to Color(0xFFFFC145), "translations" to Color(0xFFFF6B9A), "artwork" to Color(0xFFB28DFF),
    "recordings" to Color(0xFFE5484D), "models" to Color(0xFF4CC9F0), "database" to Color(0xFF7BE07B), "backups" to Color(0xFF8A8A8A),
)

/** Storage, paired devices and owner settings, behind the owner PIN when the Mac has one. */
@Composable
fun OwnerDialog(vm: PadViewModel, dismiss: () -> Unit) {
    val overview by vm.owner.collectAsState()
    val locked by vm.ownerLocked.collectAsState()
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    fun size(b: Long) = Formatter.formatShortFileSize(ctx, b)
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<ClearCacheRequest?>(null) }
    LaunchedEffect(Unit) { vm.loadOwner() }
    var audit by remember { mutableStateOf(emptyList<io.github.yohann69.sing.protocol.AuditEntry>()) }
    var models by remember { mutableStateOf(emptyList<io.github.yohann69.sing.protocol.ModelInfo>()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(overview) { if (overview != null) models = runCatching { vm.client?.models() }.getOrNull() ?: emptyList() }
    LaunchedEffect(overview) { if (overview != null) audit = runCatching { vm.client?.auditLog() }.getOrNull() ?: emptyList() }
    Dialog(dismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Ember.background)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Owner"), style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                        IconButton(dismiss) { Icon(Icons.Rounded.Close, "Close") }
                    }
                }
                if (locked) {
                    item {
                        OutlinedTextField(pin, { pin = it.filter(Char::isDigit) }, label = { Text(tr("Owner PIN")) }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                        Button({ vm.unlockOwner(pin); pin = "" }, enabled = pin.length >= 4) { Text(tr("Unlock")) }
                        Text(tr("Set on the Mac in Settings, Owner."), color = Ember.text.copy(alpha = 0.6f))
                    }
                    return@LazyColumn
                }
                val o = overview ?: return@LazyColumn
                val st = o.storage
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row { Text(size(st.totalBytes), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); Text(tr("used by Sing!"), Modifier.align(Alignment.Bottom)) }
                            Text("${size(st.diskFreeBytes)} free on disk", color = if (st.lowDisk) MaterialTheme.colorScheme.error else Ember.text.copy(alpha = 0.7f))
                            st.limitBytes?.let { LinearProgressIndicator({ (st.totalBytes.toFloat() / it).coerceIn(0f, 1f) }, Modifier.fillMaxWidth()) }
                            if (st.lowDisk) Text(tr("The disk is almost full. Clear some cache below."), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                items(st.categories) { c ->
                    ListItem(
                        headlineContent = { Text(c.name) }, supportingContent = { Text("${size(c.bytes)} · ${c.items} items") },
                        leadingContent = { Box(Modifier.size(12.dp).clip(CircleShape).background(categoryColors[c.id] ?: Color.Gray)) },
                        trailingContent = { if (c.clearable && c.bytes > 0) TextButton({ confirm = ClearCacheRequest(category = c.id) }) { Text(tr("Clear")) } },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ confirm = ClearCacheRequest(notPlayedForDays = 90) }) { Text(tr("Not sung for 90 days")) }
                        OutlinedButton({ confirm = ClearCacheRequest() }) { Text(tr("Every cache")) }
                    }
                }
                item { Text(tr("Biggest songs"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 16.dp)) }
                items(st.biggestSongs.take(10)) { s ->
                    ListItem({ Text(s.title, maxLines = 1) }, supportingContent = { Text("${s.artist} · ${size(s.bytes)}", maxLines = 1) },
                        trailingContent = { IconButton({ confirm = ClearCacheRequest(songID = s.id) }) { Icon(Icons.Rounded.Delete, "Clear") } },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent))
                }
                item { Text(tr("AI models"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 16.dp)) }
                items(models) { m -> ListItem({ Text(m.name) }, supportingContent = { Text("${m.task} · ${size(m.bytes)}") }, trailingContent = { TextButton({ scope.launch { runCatching { vm.client?.removeModel(m.name) }; models = runCatching { vm.client?.models() }.getOrNull() ?: emptyList() } }) { Text(tr("Remove"), color = MaterialTheme.colorScheme.error) } }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
                item { Row { TextButton({ scope.launch { runCatching { vm.client?.reinstallModels() }; models = runCatching { vm.client?.models() }.getOrNull() ?: emptyList() } }) { Text(tr("Reinstall shipped models")) }; TextButton({ scope.launch { runCatching { vm.client?.checkCaches() }; vm.showToast("Checking every song cache") } }) { Text(tr("Check caches")) } } }
                item { Text(tr("Who can change what"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 16.dp)) }
                listOf("tv" to "TV display", "playback" to "Playback and mics", "queue" to "Queue rules", "theme" to "Theme").forEach { (k, l) ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(l, Modifier.weight(1f))
                            listOf("guests" to "Guests", "hosts" to "Hosts", "mac" to "Mac").forEach { (v, n) ->
                                androidx.compose.material3.FilterChip((state.settings.locks[k] ?: "hosts") == v, { vm.updateOwnerSettings { s -> s.copy(locks = s.locks + (k to v)) } }, { Text(n) })
                            }
                        }
                    }
                }
                item { Text(tr("Activity"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 16.dp)) }
                items(audit.take(40)) { e -> ListItem({ Text("${e.who} ${e.action}") }, supportingContent = { Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(e.at.toLong()))) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
                item { Text(tr("Paired devices"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 16.dp)) }
                items(o.pads) { p ->
                    ListItem({ Text(p.name) }, supportingContent = { Text((if (p.role.name == "host") "Host" else "Guest") + if (p.connected) " · connected" else "") },
                        trailingContent = { TextButton({ vm.unpair(p.id) }) { Text(tr("Unpair"), color = MaterialTheme.colorScheme.error) } },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent))
                }
                val s = state.settings
                item { Text(tr("Storage limits"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 16.dp)) }
                item { Stepper(tr("Cache limit"), s.storage.limitGB, if (s.storage.limitGB == 0.0) "No limit" else "${s.storage.limitGB.toInt()} GB", 5.0) { v -> vm.updateOwnerSettings { it.copy(storage = it.storage.copy(limitGB = v)) } } }
                item { Stepper(tr("Stems limit"), s.storage.stemsLimitGB, if (s.storage.stemsLimitGB == 0.0) "No limit" else "${s.storage.stemsLimitGB.toInt()} GB", 5.0) { v -> vm.updateOwnerSettings { it.copy(storage = it.storage.copy(stemsLimitGB = v)) } } }
                item { OwnerToggle(tr("Free space automatically"), s.storage.autoFree) { v -> vm.updateOwnerSettings { it.copy(storage = it.storage.copy(autoFree = v)) } } }
                item { Text(tr("Processing"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 16.dp)) }
                item { OwnerToggle(tr("Process the whole library in the background"), s.system.processLibraryInBackground) { v -> vm.updateOwnerSettings { it.copy(system = it.system.copy(processLibraryInBackground = v)) } } }
                item { OwnerToggle(tr("Pause processing while someone sings"), s.system.pauseProcessingWhileSinging) { v -> vm.updateOwnerSettings { it.copy(system = it.system.copy(pauseProcessingWhileSinging = v)) } } }
                item { Text(tr("Privacy"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 16.dp)) }
                item { OwnerToggle(tr("Family mode"), s.privacy.familyMode) { v -> vm.updateOwnerSettings { it.copy(privacy = it.privacy.copy(familyMode = v)) } } }
                item { OwnerToggle(tr("Save history and stats"), s.privacy.keepHistory) { v -> vm.updateOwnerSettings { it.copy(privacy = it.privacy.copy(keepHistory = v)) } } }
                item { OwnerToggle(tr("Guests can see stats"), s.privacy.guestsSeeStats) { v -> vm.updateOwnerSettings { it.copy(privacy = it.privacy.copy(guestsSeeStats = v)) } } }
                item { OwnerToggle(tr("Guests can request songs"), s.privacy.guestsCanRequest) { v -> vm.updateOwnerSettings { it.copy(privacy = it.privacy.copy(guestsCanRequest = v)) } } }
                item { OwnerToggle(tr("Guests can add themselves"), s.privacy.guestsCanCreateSingers) { v -> vm.updateOwnerSettings { it.copy(privacy = it.privacy.copy(guestsCanCreateSingers = v)) } } }
                item { Text(tr("Network"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 16.dp)) }
                item { Stepper(tr("Paired devices allowed"), s.network.maxPads.toDouble(), if (s.network.maxPads == 0) "No limit" else "${s.network.maxPads}", 1.0) { v -> vm.updateOwnerSettings { it.copy(network = it.network.copy(maxPads = v.toInt())) } } }
            }
        }
    }
    confirm?.let { r ->
        androidx.compose.material3.AlertDialog({ confirm = null }, title = { Text(tr("Clear this cache?")) }, text = { Text(tr("Songs are processed again the next time they are queued. Music files, history and favorites stay.")) },
            confirmButton = { Button({ vm.clearCache(r); confirm = null }) { Text(tr("Clear")) } }, dismissButton = { TextButton({ confirm = null }) { Text(tr("Cancel")) } })
    }
}

@Composable
private fun OwnerToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) =
    ListItem({ Text(label) }, trailingContent = { Switch(value, onChange) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))

@Composable
private fun Stepper(label: String, value: Double, display: String, step: Double, allowNegative: Boolean = false, onChange: (Double) -> Unit) =
    ListItem({ Text(label) }, supportingContent = { Text(display) }, trailingContent = {
        Row {
            TextButton({ onChange(if (allowNegative) value - step else (value - step).coerceAtLeast(0.0)) }) { Text("−") }
            TextButton({ onChange(value + step) }) { Text("+") }
        }
    }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))

@Composable
fun SongSettingsDialog(vm: PadViewModel, songId: Long, dismiss: () -> Unit) {
    var s by remember { mutableStateOf<io.github.yohann69.sing.protocol.SongSettings?>(null) }
    LaunchedEffect(songId) { s = runCatching { vm.client?.songSettings(songId) }.getOrNull() }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(tr("Song settings")) },
        text = {
            val cur = s
            if (cur == null) Text(tr("Loading…")) else Column {
                Stepper(tr("Lyric timing"), cur.lyricOffsetMs.toDouble(), if (cur.lyricOffsetMs == 0) "As found" else "%+d ms".format(cur.lyricOffsetMs), 100.0, allowNegative = true) { s = cur.copy(lyricOffsetMs = it.toInt()) }
                Stepper(tr("Key"), cur.keyShift.toDouble(), if (cur.keyShift == 0) "Original" else "%+d".format(cur.keyShift), 1.0, allowNegative = true) { s = cur.copy(keyShift = it.toInt().coerceIn(-6, 6)) }
                Stepper(tr("Tempo"), cur.tempo, "${(cur.tempo * 100).toInt()}%", 0.05) { s = cur.copy(tempo = it.coerceIn(0.75, 1.25)) }
                OwnerToggle(tr("Hide in family mode"), cur.hiddenInFamilyMode) { s = cur.copy(hiddenInFamilyMode = it) }
                OwnerToggle(tr("Keep stems"), cur.pinned) { s = cur.copy(pinned = it) }
            }
        },
        confirmButton = { Button({ s?.let { v -> vm.saveSongSettings(songId, v) }; dismiss() }, enabled = s != null) { Text(tr("Save")) } },
        dismissButton = { TextButton(dismiss) { Text(tr("Cancel")) } },
    )
}

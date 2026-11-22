package io.github.yohann69.sing.ui

import io.github.yohann69.sing.tr

import androidx.compose.animation.AnimatedContent
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import io.github.yohann69.sing.protocol.micPresets
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.yohann69.sing.PadViewModel
import io.github.yohann69.sing.net.Endpoint
import io.github.yohann69.sing.protocol.CatalogTrack
import io.github.yohann69.sing.protocol.LyricLine
import io.github.yohann69.sing.protocol.PlaybackState
import io.github.yohann69.sing.protocol.QueueEntry
import io.github.yohann69.sing.protocol.Readiness
import io.github.yohann69.sing.protocol.SearchResponse
import io.github.yohann69.sing.protocol.Singer
import io.github.yohann69.sing.protocol.SongSummary
import io.github.yohann69.sing.protocol.StatsResponse
import io.github.yohann69.sing.protocol.Suggestion
import io.github.yohann69.sing.protocol.TVDisplaySettings
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private val glass = Color.White.copy(alpha = 0.08f)

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = glass, contentColor = Ember.text),
    ) { Box(Modifier.padding(18.dp)) { content() } }
}

fun durationText(ms: Long) = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

@Composable
fun Artwork(vm: PadViewModel, song: SongSummary?, size: Int, radius: Int = 12) {
    val shape = RoundedCornerShape(radius.dp)
    val url = song?.takeIf { it.hasArtwork }?.let { vm.client?.artworkUrl(it.id) }
    if (url != null) {
        AsyncImage(url, null, Modifier.size(size.dp).clip(shape), contentScale = ContentScale.Crop)
    } else {
        val c = song?.palette?.map(::hexColor) ?: listOf(Ember.blobs[0], Ember.blobs[1])
        Box(Modifier.size(size.dp).clip(shape).background(Brush.linearGradient(c.take(2))))
    }
}

@Composable
fun SingerDot(s: Singer, size: Int = 12) = Box(Modifier.size(size.dp).clip(CircleShape).background(hexColor(s.colorHex)))

// MARK: Connect

@Composable
fun ConnectScreen(vm: PadViewModel, onScan: () -> Unit) {
    val found by vm.discovery.found.collectAsState()
    var target by remember { mutableStateOf<Endpoint?>(null) }
    var manual by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.discovery.start() }
    Box(Modifier.fillMaxSize()) {
        BlobBackground(null)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Spacer(Modifier.height(40.dp))
                Text(tr("Sing!"), fontSize = 64.sp, fontWeight = FontWeight.Black, color = Ember.accentSoft)
                Text(tr("Join the karaoke Mac on this Wi‑Fi"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
            }
            if (found.isEmpty()) item { GlassCard(Modifier.fillMaxWidth().width(520.dp)) { Text(tr("Looking for Sing! Macs…")) } }
            items(found) { ep ->
                GlassCard(Modifier.fillMaxWidth().clickable { target = ep }) {
                    Column { Text(ep.name, style = MaterialTheme.typography.titleMedium); Text(ep.host, style = MaterialTheme.typography.bodySmall) }
                }
            }
            item {
                Button(onScan, Modifier.fillMaxWidth()) { Icon(Icons.Rounded.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text(tr("Scan the TV code")) }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(manual, { manual = it }, Modifier.weight(1f), label = { Text(tr("Mac address")) }, singleLine = true)
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton({ manual.split(":").let { target = Endpoint(it[0], it[0], it.getOrNull(1)?.toIntOrNull() ?: 47800) } }, enabled = manual.isNotBlank()) { Text(tr("Next")) }
                }
            }
        }
    }
    target?.let { ep -> CodeDialog(ep, vm) { target = null } }
}

@Composable
fun CodeDialog(ep: Endpoint, vm: PadViewModel, dismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(tr("Enter the code")) },
        text = {
            Column {
                Text(tr("Guests: the code on the TV. Hosts: the code in the Mac's Pads screen."))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = MaterialTheme.typography.headlineMedium)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button({ busy = true; vm.pair(ep, code) { e -> busy = false; if (e == null) dismiss() else error = e } }, enabled = code.length == 6 && !busy) {
                Text("Join ${ep.name}")
            }
        },
        dismissButton = { TextButton(dismiss) { Text(tr("Cancel")) } },
    )
}

// MARK: Singer picker

@Composable
fun SingerPicker(vm: PadViewModel, dismiss: () -> Unit) {
    val state by vm.state.collectAsState()
    val me by vm.singerId.collectAsState()
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(tr("Who's singing?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.singers.forEach { s ->
                        FilterChip(me == s.id, { vm.choose(s); if (s.prefs?.hasPIN != true || vm.isHost) dismiss() }, { Text(s.name + if (s.prefs?.hasPIN == true) " 🔒" else "") }, leadingIcon = { SingerDot(s) })
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text(tr("New singer")) }, singleLine = true)
            }
        },
        confirmButton = { Button({ vm.createSinger(name.trim()) { if (it == null) dismiss() } }, enabled = name.isNotBlank()) { Text(tr("Create")) } },
        dismissButton = { TextButton(dismiss) { Text(tr("Close")) } },
    )
}

// MARK: Now playing

@Composable
fun NowPlayingScreen(vm: PadViewModel, wide: Boolean) {
    val state by vm.state.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val np = state.nowPlaying
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(33) } }
    val pos = np.estimatedPositionMs(now) / 1000.0
    Box(Modifier.fillMaxSize()) {
        BlobBackground(np.entry?.song?.palette)
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Artwork(vm, np.entry?.song, if (wide) 150 else 96, 22)
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(label(np.state).uppercase(), style = MaterialTheme.typography.labelMedium, color = Ember.text.copy(alpha = 0.7f))
                        Text(np.entry?.song?.title ?: "Nothing playing", style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(np.entry?.song?.artist ?: "Open Search to queue a song", style = MaterialTheme.typography.titleMedium, color = Ember.text.copy(alpha = 0.75f))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            np.entry?.singers?.forEach { s -> Row(verticalAlignment = Alignment.CenterVertically) { SingerDot(s); Spacer(Modifier.width(6.dp)); Text(s.name, fontWeight = FontWeight.SemiBold) } }
                        }
                        if (np.state == PlaybackState.preparing) np.entry?.song?.let { s ->
                            LinearProgressIndicator({ (state.preparing(s.id) ?: 0.0).toFloat() }, Modifier.padding(top = 8.dp).width(260.dp))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        var prompter by remember { mutableStateOf(false) }
                        if (prompter) TeleprompterDialog(vm) { prompter = false }
                        TextButton({ prompter = true }) { Text(tr("Teleprompter")) }
                        val meters by vm.meters.collectAsState()
                        if (meters.isNotEmpty() && np.state == PlaybackState.playing) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            meters.forEach { l -> Box(Modifier.width(6.dp).height(28.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.15f))) { Box(Modifier.align(Alignment.BottomCenter).width(6.dp).height((28 * (l * 1.6f).coerceIn(0.04f, 1f)).dp).background(if (l > 0.85f) Color.Red else Color.White)) } }
                        }
                    }
                    if (vm.isHost && wide) HostControls(vm, np.guideVocal)
                }
            }
            state.result?.takeIf { np.state == PlaybackState.finished }?.let { r ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.total?.toString() ?: "♪", fontSize = 56.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(r.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text(r.comment, color = Ember.text.copy(alpha = 0.8f))
                            Text(r.categories.joinToString("  ·  ") { "${it.name} ${it.value}" }, style = MaterialTheme.typography.bodySmall)
                            r.perSinger?.takeIf { it.size > 1 }?.let { parts -> Text(parts.joinToString("  ·  ") { "${it.name} ${it.count}" }, fontWeight = FontWeight.SemiBold) }
                            r.applause?.let { Text("👏 $it", fontWeight = FontWeight.SemiBold) }
                            r.recordingID?.let { id -> RecordingPlayButton(vm, id, r.shareURL) }
                        }
                    }
                }
            } ?: if (np.entry != null) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column {
                        val l = lyrics
                        val i = l?.lineIndex(pos + 0.15)
                        AnimatedContent(i, transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) }, label = "line") { idx ->
                            Text(if (l != null && idx != null) karaoke(l.lines[idx], pos) else buildAnnotatedString { append(np.currentLine ?: "♪") }, style = LyricStyle)
                        }
                        i?.let { l?.lines?.getOrNull(it)?.romanization }?.let { ro ->
                            Text(ro, style = LyricStyle.copy(fontSize = 18.sp), color = Ember.text.copy(alpha = 0.75f))
                        }
                        i?.let { l?.lines?.getOrNull(it)?.translation }?.let { tr ->
                            Text(tr, style = LyricStyle.copy(fontSize = 18.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = Ember.accentSoft)
                        }
                        val next = i?.let { l?.lines?.getOrNull(it + 1) } ?: l?.lines?.firstOrNull()?.takeIf { i == null }
                        next?.let { Text(it.text, style = LyricStyle.copy(fontSize = 18.sp), color = Ember.text.copy(alpha = 0.5f)) }
                        Spacer(Modifier.height(10.dp))
                        val dur = (np.entry.song.durationMs / 1000.0).coerceAtLeast(1.0)
                        LinearProgressIndicator({ (pos / dur).toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth(), color = Color.White, trackColor = Color.White.copy(alpha = 0.15f))
                    }
                }
            } else Unit
            if (vm.isHost && !wide) HostControls(vm, np.guideVocal)
            GlassCard(Modifier.fillMaxWidth().weight(1f)) {
                Column {
                    Text(tr("Up next"), style = MaterialTheme.typography.titleMedium)
                    if (state.queue.isEmpty()) Text(tr("Nobody is queued. Be the first!"), color = Ember.text.copy(alpha = 0.7f))
                    LazyColumn { itemsIndexed(state.queue) { i, e -> QueueRow(vm, i, e) } }
                }
            }
        }
    }
}

private fun label(s: PlaybackState) = when (s) {
    PlaybackState.idle -> "Waiting for a song"
    PlaybackState.preparing -> "Preparing"
    PlaybackState.countdown -> "Get ready"
    PlaybackState.playing -> "Now singing"
    PlaybackState.paused -> "Paused"
    PlaybackState.finished -> "Bravo!"
}

/** Sung words in white, the current word filling from left to right, upcoming words dimmed. */
fun karaoke(line: LyricLine, t: Double) = buildAnnotatedString {
    val dim = Color.White.copy(alpha = 0.38f)
    line.segments.forEach { s ->
        val p = ((t - s.start) / (s.end - s.start).coerceAtLeast(0.01)).coerceIn(0.0, 1.0).toFloat()
        val style = when {
            p >= 1f -> SpanStyle(color = Color.White)
            p <= 0f -> SpanStyle(color = dim)
            else -> SpanStyle(brush = Brush.horizontalGradient(0f to Color.White, p to Color.White, (p + 0.08f).coerceAtMost(1f) to dim, 1f to dim))
        }
        withStyle(style) { append(s.text) }
        if (s.endsWord) append(" ")
    }
}

@Composable
fun HostControls(vm: PadViewModel, guide: Double) {
    var g by remember(guide) { mutableStateOf(guide.toFloat()) }
    var mixer by remember { mutableStateOf(false) }
    val state by vm.state.collectAsState()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(300.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton({ vm.transport("restart") }, Modifier.size(52.dp)) { Icon(Icons.Rounded.SkipPrevious, "Restart") }
            FilledIconButton({ vm.transport("pause") }, Modifier.size(70.dp)) {
                Icon(if (state.nowPlaying.state == PlaybackState.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", Modifier.size(36.dp))
            }
            FilledTonalIconButton({ vm.transport("skip") }, Modifier.size(52.dp)) { Icon(Icons.Rounded.SkipNext, "Next song") }
            FilledTonalIconButton({ mixer = true }, Modifier.size(52.dp)) { Icon(Icons.Rounded.Tune, "Mixer") }
        }
        if (mixer) MixerDialog(vm) { mixer = false }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Guide"), style = MaterialTheme.typography.labelMedium)
            Slider(g, { g = it }, Modifier.weight(1f).padding(horizontal = 8.dp), onValueChangeFinished = { vm.guideVocal(g.toDouble()) })
            Text("${(g * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun QueueRow(vm: PadViewModel, i: Int, e: QueueEntry) {
    val saved by vm.saved.collectAsState()
    ListItem(
        headlineContent = { Text(e.song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(e.singers.joinToString(" & ") { it.name }.ifEmpty { e.song.artist }, maxLines = 1) },
        leadingContent = { Row(verticalAlignment = Alignment.CenterVertically) { Text("${i + 1}", Modifier.width(24.dp)); Artwork(vm, e.song, 44, 8) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                e.estimatedStart?.let { Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.toLong())), style = MaterialTheme.typography.labelMedium) }
                if (vm.isHost) {
                    IconButton({ vm.move(e.id, (i - 1).coerceAtLeast(0)) }, enabled = i > 0) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                    IconButton({ vm.move(e.id, i + 1) }) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                }
                if (vm.isHost || e.addedBy == saved?.padID) IconButton({ vm.remove(e.id) }) { Icon(Icons.Rounded.Close, "Remove") }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
fun QueueScreen(vm: PadViewModel) {
    val state by vm.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        BlobBackground(state.nowPlaying.entry?.song?.palette)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
            item { Text(tr("Queue"), style = MaterialTheme.typography.displaySmall); Spacer(Modifier.height(12.dp)) }
            if (state.queue.isEmpty()) item { Text(tr("The queue is empty. Search for a song to get the party going.")) }
            itemsIndexed(state.queue) { i, e -> QueueRow(vm, i, e); HorizontalDivider(color = Color.White.copy(alpha = 0.08f)) }
            if (state.requests.isNotEmpty()) {
                item { Spacer(Modifier.height(20.dp)); Text(tr("Requested"), style = MaterialTheme.typography.titleLarge) }
                items(state.requests) { r -> ListItem({ Text(r.track.title) }, supportingContent = { Text("${r.track.artist} · ${r.votes} ♥") }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
            }
        }
    }
}

// MARK: Search

@Composable
fun SearchScreen(vm: PadViewModel) {
    var q by remember { mutableStateOf("") }
    var ready by remember { mutableStateOf(false) }
    var duet by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(false) }
    var ask by remember { mutableStateOf(false) }
    var lineMode by remember { mutableStateOf(false) }
    var hum by remember { mutableStateOf<List<SongSummary>?>(null) }
    val me by vm.singerId.collectAsState()
    val ratings by vm.ratings.collectAsState()
    var result by remember { mutableStateOf(SearchResponse(emptyList(), 0)) }
    var song by remember { mutableStateOf<SongSummary?>(null) }
    var track by remember { mutableStateOf<CatalogTrack?>(null) }
    LaunchedEffect(q, ready, duet, favorites, ratings.size, ask, lineMode) {
        delay(250)
        if (lineMode && q.isNotBlank()) runCatching { vm.client?.searchLines(q) }.getOrNull()?.let { result = it }
        else if (ask && q.isNotBlank()) runCatching { vm.client?.ask(q) }.getOrNull()?.let { result = it }
        else if (q.isNotBlank() || favorites) runCatching { vm.client?.search(q, ready, duet, if (favorites) me else null) }.getOrNull()?.let { result = it }
        else result = SearchResponse(emptyList(), 0)
    }
    Box(Modifier.fillMaxSize()) {
        BlobBackground(null)
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Rounded.Search, null) }, trailingIcon = { HumButton(vm) { hum = it } },
                placeholder = { Text(tr("Any song or artist, or hum it")) }, singleLine = true, shape = RoundedCornerShape(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                FilterChip(ask, { ask = !ask; lineMode = false }, { Text(tr("✨ Ask")) })
                FilterChip(lineMode, { lineMode = !lineMode; ask = false }, { Text(tr("Lyric line")) })
                FilterChip(favorites, { favorites = !favorites }, { Text(tr("Favorites")) })
                FilterChip(ready, { ready = !ready }, { Text(tr("Ready now")) })
                FilterChip(duet, { duet = !duet }, { Text(tr("Duets")) })
            }
            LazyColumn(Modifier.fillMaxSize()) {
                hum?.takeIf { q.isBlank() }?.let { h ->
                    item { Text(tr("Sounds like"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                    if (h.isEmpty()) item { Text(tr("No match. Hum the chorus a bit longer.")) }
                    items(h) { s -> SongRow(vm, s, s.artist) { song = s } }
                }
                if (result.songs.isNotEmpty()) item { Text(tr("In the library"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                items(result.songs) { s -> SongRow(vm, s, s.artist) { song = s } }
                if (result.catalog.isNotEmpty()) item { Text(tr("Not in the library yet"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                items(result.catalog) { t ->
                    ListItem(
                        headlineContent = { Text(t.title, maxLines = 1) }, supportingContent = { Text(t.artist, maxLines = 1) },
                        leadingContent = { AsyncImage(t.artworkURL, null, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop) },
                        trailingContent = { Text(tr("Request"), color = Ember.accentSoft, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.clickable { track = t }, colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
    song?.let { SongDialog(vm, it) { song = null } }
    track?.let { t ->
        AlertDialog({ track = null }, title = { Text(t.title) }, text = { Text("${t.artist}\n\nThis song is not on the Mac yet. Request it and the host sees it in the queue.") },
            confirmButton = { Button({ vm.request(t); track = null }) { Text(tr("Request this song")) } }, dismissButton = { TextButton({ track = null }) { Text(tr("Cancel")) } })
    }
}

@Composable
fun SongRow(vm: PadViewModel, s: SongSummary, detail: String, onClick: () -> Unit) {
    val state by vm.state.collectAsState()
    val ratings by vm.ratings.collectAsState()
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (ratings[s.id]?.favorite == true) Icon(Icons.Rounded.Favorite, "Favorite", Modifier.padding(start = 6.dp).size(14.dp), tint = Color(0xFFFF6B9A))
            }
        },
        supportingContent = { Text(detail, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Artwork(vm, s, 52, 10) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(durationText(s.durationMs), style = MaterialTheme.typography.labelMedium)
                Text(
                    when (s.readiness) { Readiness.ready -> "Ready"; Readiness.processing -> "Preparing ${((state.preparing(s.id) ?: 0.0) * 100).toInt()}%"; else -> "Not ready" },
                    style = MaterialTheme.typography.labelSmall, color = if (s.readiness == Readiness.ready) Color(0xFF7BE07B) else Ember.text.copy(alpha = 0.6f),
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick), colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
fun SongDialog(vm: PadViewModel, s: SongSummary, dismiss: () -> Unit) {
    val state by vm.state.collectAsState()
    val me by vm.singerId.collectAsState()
    var picked by remember { mutableStateOf(setOfNotNull(me)) }
    var editing by remember { mutableStateOf(false) }
    var battle by remember { mutableStateOf(false) }
    if (editing) SongSettingsDialog(vm, s.id) { editing = false }
    AlertDialog(
        onDismissRequest = dismiss,
        icon = { Artwork(vm, s, 120, 20) },
        title = { Text(s.title) },
        text = {
            Column {
                Text("${s.artist} · ${durationText(s.durationMs)}")
                Spacer(Modifier.height(8.dp))
                RatingRow(vm, s.id)
                val wishes by vm.wishes.collectAsState()
                Row {
                    TextButton({ vm.toggleWish(s.id) }) { Text(if (s.id in wishes) "On my list ✓" else "Want to sing") }
                    if (vm.isHost) TextButton({ editing = true }) { Text(tr("Song settings")) }
                    TextButton({ vm.reportBadLyrics(s.id) }) { Text(tr("Wrong lyrics?")) }
                }
                var key by remember { mutableStateOf<io.github.yohann69.sing.protocol.SmartKey?>(null) }
                LaunchedEffect(s.id, me) { key = me?.let { m -> runCatching { vm.client?.smartKey(s.id, m) }.getOrNull() } }
                key?.takeIf { it.semitones != 0 }?.let { Text(it.reason, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(8.dp))
                Text(if (s.isDuet) "Duet: pick two singers" else "Who sings?", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.singers.forEach { x -> FilterChip(x.id in picked, { picked = if (x.id in picked) picked - x.id else picked + x.id }, { Text(x.name) }, leadingIcon = { SingerDot(x) }) }
                }
                if (picked.size >= 2) Row(verticalAlignment = Alignment.CenterVertically) { Text(tr("Battle: take turns, best score wins"), Modifier.weight(1f)); Switch(battle, { battle = it }) }
            }
        },
        confirmButton = { Button({ vm.enqueue(s, picked.toList(), false, battle); dismiss() }) { Text(tr("Add to queue")) } },
        dismissButton = { if (vm.isHost) TextButton({ vm.enqueue(s, picked.toList(), true); dismiss() }) { Text(tr("Play next")) } else TextButton(dismiss) { Text(tr("Cancel")) } },
    )
}

// MARK: For you, stats, settings

@Composable
fun ForYouScreen(vm: PadViewModel) {
    val me by vm.singerId.collectAsState()
    val state by vm.state.collectAsState()
    var items by remember { mutableStateOf(emptyList<Suggestion>()) }
    var wishes by remember { mutableStateOf(emptyList<SongSummary>()) }
    var partner by remember { mutableStateOf<Long?>(null) }
    var song by remember { mutableStateOf<SongSummary?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(me, partner) {
        val m = me
        val p = partner
        items = runCatching { if (m != null && p != null) vm.client?.duetSuggestions(m, p) else vm.client?.suggestions(m) }.getOrNull() ?: emptyList()
        wishes = m?.let { runCatching { vm.client?.wishes(it) }.getOrNull() } ?: emptyList()
    }
    Box(Modifier.fillMaxSize()) {
        BlobBackground(null)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
            item { Text(tr("For you"), style = MaterialTheme.typography.displaySmall); Spacer(Modifier.height(12.dp)) }
            if (wishes.isNotEmpty()) {
                item { Text(tr("Want to sing"), style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft) }
                items(wishes) { s -> SongRow(vm, s, "On your list") { song = s } }
                item { TextButton({ scope.launch { me?.let { m -> runCatching { vm.client?.queueNextWish(m) }.getOrNull()?.songs?.firstOrNull()?.let { vm.showToast("${it.title} is in the queue") } } } }) { Text(tr("Queue the next one")) } }
            }
            if (me != null) item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(tr("Duet with"), Modifier.align(Alignment.CenterVertically))
                    FilterChip(partner == null, { partner = null }, { Text(tr("Nobody")) })
                    state.singers.filter { it.id != me }.take(6).forEach { s -> FilterChip(partner == s.id, { partner = s.id }, { Text(s.name) }) }
                }
            }
            items(items) { s -> SongRow(vm, s.song, s.reason) { song = s.song } }
        }
    }
    song?.let { SongDialog(vm, it) { song = null } }
}

@Composable
fun StatsScreen(vm: PadViewModel) {
    var period by remember { mutableStateOf("tonight") }
    var data by remember { mutableStateOf<StatsResponse?>(null) }
    LaunchedEffect(period) { data = runCatching { vm.client?.stats(period) }.getOrNull() }
    Box(Modifier.fillMaxSize()) {
        BlobBackground(null)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(tr("Stats"), style = MaterialTheme.typography.displaySmall) }
            item {
                val live by vm.state.collectAsState()
                var recap by remember { mutableStateOf<io.github.yohann69.sing.protocol.PartyRecap?>(null) }
                LaunchedEffect(Unit) { recap = runCatching { vm.client?.recap() }.getOrNull() }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    live.leaderboard?.takeIf { it.isNotEmpty() }?.let { b -> Ranked("Tonight's top scores", b.map { it.name to it.count }) }
                    (live.recap ?: recap)?.let { r ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column {
                                Text(r.name ?: "Last party", style = MaterialTheme.typography.titleMedium)
                                Text("${r.songs} songs · ${r.minutes} minutes · ${r.singers} singers", fontWeight = FontWeight.Bold)
                                r.highlights.forEach { Text(it) }
                                r.reelURL?.let { url -> val ctx = androidx.compose.ui.platform.LocalContext.current; TextButton({ shareLink(ctx, url) }) { Text(tr("Share the night's highlights")) } }
                            }
                        }
                    }
                }
            }
            item { RecordingsSection(vm) }
            item {
                val options = listOf("tonight" to "Tonight", "year" to "This year", "allTime" to "All time")
                SingleChoiceSegmentedButtonRow {
                    options.forEachIndexed { i, (k, l) -> SegmentedButton(period == k, { period = k }, SegmentedButtonDefaults.itemShape(i, options.size)) { Text(l) } }
                }
            }
            data?.let { d ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("${d.totalPlays}" to "songs", "${d.totalMinutes}" to "minutes", "${d.topSingers.size}" to "singers").forEach { (v, l) ->
                            GlassCard(Modifier.weight(1f)) { Column { Text(v, fontSize = 30.sp, fontWeight = FontWeight.Black); Text(l) } }
                        }
                    }
                }
                item { Ranked("Top songs", d.topSongs.map { "${it.song.title} · ${it.song.artist}" to it.count }) }
                item { Ranked("Top singers", d.topSingers.map { it.name to it.count }) }
                item { Ranked("Top artists", d.topArtists.map { it.name to it.count }) }
            }
        }
    }
}

@Composable
fun Ranked(title: String, rows: List<Pair<String, Int>>) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (rows.isEmpty()) Text(tr("Nothing yet"), color = Ember.text.copy(alpha = 0.6f))
            rows.take(8).forEachIndexed { i, (n, c) ->
                Row { Text("${i + 1}", Modifier.width(24.dp)); Text(n, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text("$c", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: PadViewModel) {
    val state by vm.state.collectAsState()
    val saved by vm.saved.collectAsState()
    val connected by vm.connected.collectAsState()
    val tv = state.settings.tv
    val lang by vm.language.collectAsState()
    val languages by vm.languages.collectAsState()
    LaunchedEffect(Unit) { vm.loadLanguages() }
    var picking by remember { mutableStateOf(false) }
    var owner by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(false) }
    var profiles by remember { mutableStateOf(emptyList<io.github.yohann69.sing.protocol.SettingsProfile>()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(vm.isHost) { if (vm.isHost) profiles = runCatching { vm.client?.settingsProfiles() }.getOrNull() ?: emptyList() }
    if (profile) ProfileDialog(vm) { profile = false }
    PinDialog(vm)
    if (picking) SingerPicker(vm) { picking = false }
    if (owner) OwnerDialog(vm) { owner = false }
    fun setTv(change: (TVDisplaySettings) -> TVDisplaySettings) = vm.updateSettings { it.copy(tv = change(it.tv)) }
    LazyColumn(Modifier.fillMaxSize().background(Ember.background), contentPadding = PaddingValues(20.dp)) {
        item {
            Text(tr("Settings"), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text("Mac: ${saved?.endpoint?.name} · ${if (vm.isHost) "Host" else "Guest"} · ${if (connected) "Connected" else "Reconnecting…"}")
            TextButton({ vm.disconnect() }) { Text(tr("Disconnect from this Mac"), color = MaterialTheme.colorScheme.error) }
        }
        item { Header(tr("Me")) }
        item { ListItem({ Text(tr("Singer")) }, supportingContent = { Text(state.singers.firstOrNull { it.id == vm.singerId.value }?.name ?: "Not chosen") }, modifier = Modifier.clickable { picking = true }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
        if (vm.singerId.value != null) item { ListItem({ Text(tr("My profile")) }, supportingContent = { Text(tr("Privacy, mic effect, key, PIN, my year")) }, modifier = Modifier.clickable { profile = true }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
        item {
            val micOn by vm.phoneMicOn.collectAsState()
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.togglePhoneMic() }
            Toggle(tr("Use this phone as a mic (experimental)"), micOn) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) vm.togglePhoneMic()
                else ask.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
        item { Header(tr("My lyrics")) }
        item { LanguagePicker(tr("Translate lyrics on this device"), lang, languages) { vm.setLanguage(it) } }
        if (!vm.isHost) {
            item { Text(tr("Only host pads can change the TV display and party settings.")) }
            return@LazyColumn
        }
        item { ListItem({ Text(tr("Owner area")) }, supportingContent = { Text(tr("Storage, devices, processing, lyrics sources")) }, leadingContent = { Icon(Icons.Rounded.Lock, null) }, modifier = Modifier.clickable { owner = true }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
        if (profiles.isNotEmpty()) {
            item { Header(tr("Quick settings")) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) { profiles.forEach { p -> FilterChip(false, { scope.launch { runCatching { vm.client?.applySettingsProfile(p.name) }; vm.showToast("${p.name} settings on") } }, { Text(p.name) }) } } }
        }
        item { Header(tr("Lyrics on the TV")) }
        item { Toggle(tr("Previous line"), tv.showPreviousLine) { v -> setTv { it.copy(showPreviousLine = v) } } }
        item { Toggle(tr("Next line"), tv.showNextLine) { v -> setTv { it.copy(showNextLine = v) } } }
        item { Toggle(tr("Backing vocals"), tv.showBackingVocals) { v -> setTv { it.copy(showBackingVocals = v) } } }
        item { Toggle(tr("Singer names"), tv.showSingerNames) { v -> setTv { it.copy(showSingerNames = v) } } }
        item { Toggle(tr("Translation line (on-device AI)"), tv.showTranslation) { v -> setTv { it.copy(showTranslation = v) } } }
        if (tv.showTranslation) item { LanguagePicker(tr("Translate the TV into"), tv.translationLanguage, languages) { c -> setTv { it.copy(translationLanguage = c) } } }
        item { Toggle(tr("Countdown dots"), tv.showCountdown) { v -> setTv { it.copy(showCountdown = v) } } }
        item { Toggle(tr("Word fill on line-synced lyrics"), tv.estimatedWordWipe) { v -> setTv { it.copy(estimatedWordWipe = v) } } }
        item { SliderRow(tr("Lyric size"), tv.lyricSize, 56.0..150.0) { v -> setTv { it.copy(lyricSize = v) } } }
        item { Header(tr("Extras")) }
        item { Toggle(tr("Song info"), tv.showSongInfo) { v -> setTv { it.copy(showSongInfo = v) } } }
        item { Toggle(tr("Up next"), tv.showUpNext) { v -> setTv { it.copy(showUpNext = v) } } }
        item { Toggle(tr("Progress line"), tv.showProgress) { v -> setTv { it.copy(showProgress = v) } } }
        item { Toggle(tr("Join QR code when idle"), tv.showJoinQR) { v -> setTv { it.copy(showJoinQR = v) } } }
        item { Header(tr("Pitch and score")) }
        item { Toggle(tr("Pitch guide"), tv.showPitchGuide) { v -> setTv { it.copy(showPitchGuide = v) } } }
        item { Toggle(tr("Live score"), tv.showLiveScore) { v -> setTv { it.copy(showLiveScore = v) } } }
        item { Toggle(tr("Score reveal"), tv.showScoreReveal) { v -> setTv { it.copy(showScoreReveal = v) } } }
        item { Toggle(tr("Score details"), tv.showScoreDetails) { v -> setTv { it.copy(showScoreDetails = v) } } }
        item { Toggle(tr("Confetti"), tv.showConfetti) { v -> setTv { it.copy(showConfetti = v) } } }
        item { Toggle(tr("Tonight's leaderboard between songs"), tv.showLeaderboard) { v -> setTv { it.copy(showLeaderboard = v) } } }
        item { Toggle(tr("Just for fun: no numbers at all"), tv.justForFun) { v -> setTv { it.copy(justForFun = v) } } }
        item { Toggle(tr("Latin spelling under other scripts"), tv.showRomanization) { v -> setTv { it.copy(showRomanization = v) } } }
        item { Toggle(tr("Applause meter"), tv.showApplause) { v -> setTv { it.copy(showApplause = v) } } }
        item { Toggle(tr("Fun facts when idle"), tv.showFunStats) { v -> setTv { it.copy(showFunStats = v) } } }
        item { Toggle(tr("Host pairing code in a corner"), tv.showHostCode) { v -> setTv { it.copy(showHostCode = v) } } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                listOf("default" to "SF Pro", "rounded" to "Rounded", "serif" to "Serif", "mono" to "Mono").forEach { (k, l) -> FilterChip(tv.lyricFont == k, { setTv { it.copy(lyricFont = k) } }, { Text(l) }) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(tr("Lines around"), Modifier.align(Alignment.CenterVertically))
                (1..3).forEach { n -> FilterChip(tv.contextLines == n, { setTv { it.copy(contextLines = n) } }, { Text("$n") }) }
            }
        }
        item { SliderRow(tr("Results stay on screen (s)"), tv.resultSeconds, 7.0..30.0) { v -> setTv { it.copy(resultSeconds = v) } } }
        item { Header(tr("Playback")) }
        item { Toggle(tr("Countdown before each song"), state.settings.playback.countdownBetweenSongs) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(countdownBetweenSongs = v)) } } }
        item { Toggle(tr("Play the next song automatically"), state.settings.playback.autoAdvance) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(autoAdvance = v)) } } }
        item { Toggle(tr("Play the original when stems are not ready"), state.settings.playback.playOriginalWhenNotReady) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(playOriginalWhenNotReady = v)) } } }
        item { Toggle(tr("Singers take turns in the queue"), state.settings.queue.fairRotation) { v -> vm.updateSettings { it.copy(queue = it.queue.copy(fairRotation = v)) } } }
        item { Toggle(tr("Queue a crowd favourite when the queue runs dry"), state.settings.queue.autoFill) { v -> vm.updateSettings { it.copy(queue = it.queue.copy(autoFill = v)) } } }
        item { Toggle(tr("Record every performance"), state.settings.playback.recordPerformances) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(recordPerformances = v)) } } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(tr("Scoring"), Modifier.align(Alignment.CenterVertically))
                listOf("easy" to "Easy", "normal" to "Normal", "hard" to "Hard").forEach { (k, l) ->
                    FilterChip(state.settings.playback.difficulty == k, { vm.updateSettings { it.copy(playback = it.playback.copy(difficulty = k)) } }, { Text(l) })
                }
            }
        }
        item { Toggle(tr("Even out loudness between songs"), state.settings.playback.normalizeLoudness) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(normalizeLoudness = v)) } } }
        item { Toggle(tr("Turn mics down when feedback howls"), state.settings.playback.feedbackProtection) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(feedbackProtection = v)) } } }
        item { SliderRow(tr("Default guide vocal (%)"), state.settings.playback.defaultGuideVocal * 100, 0.0..100.0) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(defaultGuideVocal = v / 100)) } } }
        item { Header(tr("Theme")) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                listOf("ember" to "Ember", "aurora" to "Aurora", "studio" to "Studio").forEach { (k, l) ->
                    FilterChip(state.settings.theme.preset == k, { vm.updateSettings { it.copy(theme = it.theme.copy(preset = k)) } }, { Text(l) })
                }
            }
        }
    }
}

@Composable private fun Header(t: String) = Text(t, style = MaterialTheme.typography.titleMedium, color = Ember.accentSoft, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) =
    ListItem({ Text(label) }, trailingContent = { Switch(value, onChange) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))

@Composable
private fun SliderRow(label: String, value: Double, range: ClosedFloatingPointRange<Double>, onChange: (Double) -> Unit) {
    var v by remember(value) { mutableStateOf(value.toFloat()) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("$label: ${v.toInt()}")
        Slider(v, { v = it }, valueRange = range.start.toFloat()..range.endInclusive.toFloat(), onValueChangeFinished = { onChange(v.toDouble()) })
    }
}

@Composable
private fun LanguagePicker(label: String, value: String?, options: List<io.github.yohann69.sing.net.SingClient.Language>, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ListItem(
        { Text(label) },
        supportingContent = { Text(options.firstOrNull { it.code == value }?.name ?: "Off") },
        modifier = Modifier.clickable { open = true },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(label) },
        text = {
            LazyColumn(Modifier.height(420.dp)) {
                item { ListItem({ Text(tr("Off")) }, Modifier.clickable { onPick(null); open = false }) }
                items(options) { o -> ListItem({ Text(o.name) }, Modifier.clickable { onPick(o.code); open = false }) }
            }
        },
        confirmButton = { TextButton({ open = false }) { Text(tr("Close")) } },
    )
}

@Composable
fun MixerDialog(vm: PadViewModel, dismiss: () -> Unit) {
    val state by vm.state.collectAsState()
    val p = state.settings.playback
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(tr("Mixer")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MixSlider(tr("Guide vocal"), state.nowPlaying.guideVocal) { vm.guideVocal(it) }
                MixSlider(tr("Music volume"), p.musicVolume) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(musicVolume = v)) } }
                val np = state.nowPlaying
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Key ") + if (np.keyShift == 0) "original" else "%+d".format(np.keyShift), Modifier.weight(1f))
                    TextButton({ vm.setKey(np.keyShift - 1) }) { Text("−") }
                    TextButton({ vm.setKey(np.keyShift + 1) }) { Text("+") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tempo ${(np.tempo * 100).roundToInt()}%", Modifier.weight(1f))
                    TextButton({ vm.setTempo(np.tempo - 0.05) }) { Text("−") }
                    TextButton({ vm.setTempo(np.tempo + 0.05) }) { Text("+") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Lyric timing ") + if (np.lyricOffsetMs == 0) "as found" else "%+d ms".format(np.lyricOffsetMs), Modifier.weight(1f))
                    TextButton({ vm.setLyricOffset(np.lyricOffsetMs - 100) }) { Text(tr("Earlier")) }
                    TextButton({ vm.setLyricOffset(np.lyricOffsetMs + 100) }) { Text(tr("Later")) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Practice"), Modifier.weight(1f))
                    if (np.loopStartMs != null) TextButton({ vm.stopLoop() }) { Text(tr("Stop loop")) }
                    else { TextButton({ vm.loopLines(1) }) { Text(tr("Loop line")) }; TextButton({ vm.loopLines(4) }) { Text(tr("Loop 4")) } }
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Microphone"), Modifier.weight(1f))
                    Switch(p.micEnabled, { v -> vm.updateSettings { it.copy(playback = it.playback.copy(micEnabled = v)) } })
                }
                state.mic?.let { m -> Text(m.message ?: if (m.state == "running") "Live on ${m.deviceName ?: "the Mac's mic"}" else m.state, style = MaterialTheme.typography.bodySmall) }
                MixSlider(tr("Mic volume"), p.micGain) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(micGain = v)) } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    micPresets.forEach { (k, l) -> FilterChip(p.micPreset == k, { vm.updateSettings { it.copy(playback = it.playback.copy(micPreset = k)) } }, { Text(l) }) }
                }
                MixSlider(tr("Reverb"), p.micReverb) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(micReverb = v)) } }
                MixSlider(tr("Echo"), p.micEcho) { v -> vm.updateSettings { it.copy(playback = it.playback.copy(micEcho = v)) } }
                MicSection(vm, p)
            }
        },
        confirmButton = { TextButton(dismiss) { Text(tr("Done")) } },
    )
}

@Composable
private fun MixSlider(label: String, value: Double, onChange: (Double) -> Unit) {
    var v by remember(value) { mutableStateOf(value.toFloat()) }
    Column {
        Row { Text(label, Modifier.weight(1f)); Text("${(v * 100).toInt()}%") }
        Slider(v, { v = it; onChange(it.toDouble()) })
    }
}

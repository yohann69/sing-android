package io.github.yohann69.sing.ui

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.yohann69.sing.PadViewModel
import io.github.yohann69.sing.protocol.RecordingInfo
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** One MediaPlayer for the screen; tapping another recording stops the current one. */
class RecordingPlayer {
    private var player: MediaPlayer? = null
    var playingId by mutableStateOf<Long?>(null)
        private set

    fun toggle(id: Long, url: String) {
        if (playingId == id) return stop()
        stop()
        playingId = id
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            setDataSource(url)
            setOnPreparedListener { it.start() }
            setOnCompletionListener { stop() }
            setOnErrorListener { _, _, _ -> stop(); true }
            prepareAsync()
        }
    }

    fun stop() {
        player?.runCatching { stop(); release() }
        player = null
        playingId = null
    }
}

fun shareLink(ctx: android.content.Context, url: String) {
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url), "Share the recording"))
}

@Composable
fun RecordingPlayButton(vm: PadViewModel, id: Long, share: String?) {
    val player = remember { RecordingPlayer() }
    val ctx = LocalContext.current
    DisposableEffect(Unit) { onDispose { player.stop() } }
    Row {
        TextButton({ vm.client?.let { player.toggle(id, it.recordingAudioUrl(id)) } }) { Text(if (player.playingId == id) "Stop" else "Listen back") }
        share?.let { TextButton({ shareLink(ctx, it) }) { Text("Share") } }
    }
}

@Composable
fun RecordingsSection(vm: PadViewModel) {
    val me by vm.singerId.collectAsState()
    val state by vm.state.collectAsState()
    var items by remember { mutableStateOf(emptyList<RecordingInfo>()) }
    val player = remember { RecordingPlayer() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    suspend fun load() { items = runCatching { vm.client?.recordings(if (vm.isHost) null else me) }.getOrNull() ?: emptyList() }
    LaunchedEffect(me, state.result?.recordingID) { if (me != null || vm.isHost) load() }
    DisposableEffect(Unit) { onDispose { player.stop() } }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (vm.isHost) "Recordings" else "My recordings", style = MaterialTheme.typography.titleMedium)
            if (items.isEmpty()) Text("Nothing recorded yet. The host can turn recording on in the party settings.", color = Ember.text.copy(alpha = 0.6f))
            items.take(20).forEach { r ->
                ListItem(
                    headlineContent = { Text(r.songTitle, maxLines = 1) },
                    supportingContent = {
                        Text(listOfNotNull(r.singers.joinToString(" & ") { it.name }.ifEmpty { null }, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(r.recordedAt.toLong())), r.score?.let { "$it pts" }).joinToString(" · "), maxLines = 1)
                    },
                    leadingContent = {
                        FilledTonalIconButton({ vm.client?.let { player.toggle(r.id, it.recordingAudioUrl(r.id)) } }) {
                            Icon(if (player.playingId == r.id) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, "Play")
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton({ vm.client?.let { shareLink(ctx, it.shareUrl(r)) } }) { Icon(Icons.Rounded.Share, "Share") }
                            if (vm.isHost) {
                                IconButton({ scope.launch { runCatching { vm.client?.replayRecording(r.id) }.onFailure { vm.showToast(it.message ?: "") } } }) { Icon(Icons.Rounded.Tv, "Replay on the TV") }
                                IconButton({ scope.launch { runCatching { vm.client?.deleteRecording(r.id) }; load() } }) { Icon(Icons.Rounded.Delete, "Delete") }
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

package io.github.yohann69.sing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.yohann69.sing.protocol.LiveState
import io.github.yohann69.sing.protocol.PlaybackState

/** Ongoing notification: what is playing and how soon this singer is up. */
class Notifier(private val context: Context) {
    private val channel = "party"
    private var last: String? = null

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val m = context.getSystemService(NotificationManager::class.java)
            m.createNotificationChannel(NotificationChannel(channel, "Party", NotificationManager.IMPORTANCE_LOW).apply { description = "Now playing and your turn" })
        }
    }

    private val allowed get() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun update(state: LiveState, singerId: Long?, connected: Boolean) {
        if (!allowed) return
        val np = state.nowPlaying
        val song = np.entry?.song
        if (!connected || (song == null && state.queue.isEmpty())) return clear()
        val away = singerId?.let { id -> state.queue.indexOfFirst { e -> e.singers.any { it.id == id } }.takeIf { it >= 0 } }
        val title = when {
            song == null -> "Sing! party"
            np.state == PlaybackState.playing -> song.title
            np.state == PlaybackState.finished -> "Results: ${song.title}"
            else -> "Up: ${song.title}"
        }
        val text = listOfNotNull(
            song?.let { s -> np.entry?.singers?.joinToString(" & ") { it.name }?.ifEmpty { null }?.let { "$it · ${s.artist}" } ?: s.artist },
            away?.let { if (it == 0) "You're up next!" else "You're up in ${it + 1} songs" },
        ).joinToString("\n")
        val key = "$title|$text"
        if (key == last) return
        last = key
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(text.lineSequence().firstOrNull() ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(away != 0)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(open)
            .build()
        try { NotificationManagerCompat.from(context).notify(1, n) } catch (_: SecurityException) {}
    }

    fun clear() {
        last = null
        NotificationManagerCompat.from(context).cancel(1)
    }
}

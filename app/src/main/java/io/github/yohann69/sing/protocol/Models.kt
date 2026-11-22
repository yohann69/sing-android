package io.github.yohann69.sing.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Mirrors the Swift types in SingCore/Protocol; see protocol/README.md in sing-apple. */
object SingProtocol {
    const val VERSION = 1
    const val DEFAULT_PORT = 47800
    const val SERVICE_TYPE = "_sing._tcp."
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
}

@Serializable
enum class Readiness { new, queuedForProcessing, processing, ready, failed, missingFile }

@Serializable
enum class LyricsKind { plain, line, word, syllable }

@Serializable
enum class PlaybackState { idle, preparing, countdown, playing, paused, finished }

@Serializable
enum class PadRole { host, guest }

@Serializable
data class SongSummary(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long,
    val year: Int? = null,
    val genre: String? = null,
    val language: String? = null,
    val isrc: String? = null,
    val hasArtwork: Boolean = false,
    val readiness: Readiness = Readiness.new,
    val progress: Double? = null,
    val lyricsKind: LyricsKind? = null,
    val isDuet: Boolean = false,
    val playCount: Int = 0,
    val palette: List<String>? = null,
)

@Serializable
data class Singer(val id: Long, val name: String, val emoji: String? = null, val colorHex: String)

@Serializable
data class QueueEntry(
    val id: String,
    val song: SongSummary,
    val singers: List<Singer>,
    val addedBy: String? = null,
    val addedAt: Double,
    val estimatedStart: Double? = null,
)

@Serializable
data class NowPlaying(
    val entry: QueueEntry? = null,
    val state: PlaybackState = PlaybackState.idle,
    val positionMs: Long = 0,
    val positionAt: Double = 0.0,
    val guideVocal: Double = 0.0,
    val currentLine: String? = null,
    val nextLine: String? = null,
    val keyShift: Int = 0,
    val tempo: Double = 1.0,
    val lyricOffsetMs: Int = 0,
) {
    fun estimatedPositionMs(now: Long = System.currentTimeMillis()): Long =
        if (state == PlaybackState.playing) positionMs + ((now - positionAt.toLong()).coerceAtLeast(0) * tempo).toLong() else positionMs
}

@Serializable
data class TVDisplaySettings(
    val showPreviousLine: Boolean = true,
    val showNextLine: Boolean = true,
    val showBackingVocals: Boolean = true,
    val showSingerNames: Boolean = true,
    val showSongInfo: Boolean = true,
    val showUpNext: Boolean = true,
    val showProgress: Boolean = true,
    val showPitchGuide: Boolean = true,
    val showLiveScore: Boolean = true,
    val showCountdown: Boolean = true,
    val showJoinQR: Boolean = true,
    val showTranslation: Boolean = false,
    val translationLanguage: String? = null,
    val showScoreReveal: Boolean = true,
    val showScoreDetails: Boolean = true,
    val showConfetti: Boolean = true,
    val estimatedWordWipe: Boolean = true,
    val resultSeconds: Double = 10.0,
    val lyricSize: Double = 96.0,
    val backgroundSpeed: Double = 28.0,
    val background: String = "artworkBlobs",
)

@Serializable
data class PlaybackSettings(
    val defaultGuideVocal: Double = 0.0,
    val countdownBetweenSongs: Boolean = true,
    val autoAdvance: Boolean = true,
    val micEnabled: Boolean = true,
    val micGain: Double = 0.8,
    val micReverb: Double = 0.35,
    val musicVolume: Double = 0.9,
    val playOriginalWhenNotReady: Boolean = false,
    val micPreset: String = "hall",
    val micEcho: Double = 0.0,
    val feedbackProtection: Boolean = true,
    val normalizeLoudness: Boolean = true,
)

@Serializable
data class QueueSettings(val fairRotation: Boolean = true, val guestsCanQueue: Boolean = true, val guestsCanRemoveOwn: Boolean = true, val maxQueuedPerSinger: Int = 0)

val micPresets = listOf("natural" to "Natural", "studio" to "Studio", "hall" to "Concert hall", "stadium" to "Stadium", "echo" to "Echo", "radio" to "Radio")

@Serializable
data class ThemeSettings(val preset: String = "ember")

/** Settings the pad does not edit are kept as raw JSON so they round-trip untouched. */
@Serializable
data class SingSettings(
    val tv: TVDisplaySettings = TVDisplaySettings(),
    val playback: PlaybackSettings = PlaybackSettings(),
    val theme: ThemeSettings = ThemeSettings(),
    val queue: QueueSettings = QueueSettings(),
    val services: JsonElement? = null,
    val storage: StorageSettings = StorageSettings(),
    val system: SystemSettings = SystemSettings(),
    val network: NetworkSettings = NetworkSettings(),
    val privacy: PrivacySettings = PrivacySettings(),
)

@Serializable
data class PrivacySettings(val familyMode: Boolean = false, val keepHistory: Boolean = true, val guestsSeeStats: Boolean = true, val guestsCanRequest: Boolean = true, val guestsCanCreateSingers: Boolean = true)

@Serializable
data class SongSettings(val lyricOffsetMs: Int = 0, val defaultGuideVocal: Double? = null, val keyShift: Int = 0, val tempo: Double = 1.0, val hiddenInFamilyMode: Boolean = false, val pinned: Boolean = false)

@Serializable
data class StorageSettings(val limitGB: Double = 0.0, val stemsLimitGB: Double = 0.0, val lowDiskWarningGB: Double = 10.0, val autoFree: Boolean = true)

@Serializable
data class SystemSettings(val processLibraryInBackground: Boolean = true, val pauseProcessingWhileSinging: Boolean = true, val restAfterMinutes: Double = 0.0)

@Serializable
data class NetworkSettings(val bonjourName: String = "", val port: Int = 47800, val maxPads: Int = 0)

@Serializable
data class SongRating(val songID: Long, val singerID: Long, val value: Int = 0, val favorite: Boolean = false, val neverSuggest: Boolean = false)

@Serializable
data class StorageCategory(val id: String, val name: String, val bytes: Long, val items: Int, val clearable: Boolean)

@Serializable
data class SongUsage(val id: Long, val title: String, val artist: String, val bytes: Long, val lastPlayedAt: Double? = null, val pinned: Boolean = false)

@Serializable
data class StorageReport(
    val categories: List<StorageCategory>,
    val totalBytes: Long,
    val diskFreeBytes: Long,
    val diskTotalBytes: Long,
    val limitBytes: Long? = null,
    val lowDisk: Boolean = false,
    val biggestSongs: List<SongUsage> = emptyList(),
)

@Serializable
data class ClearCacheRequest(val category: String? = null, val songID: Long? = null, val notPlayedForDays: Int? = null, val dryRun: Boolean = false)

@Serializable
data class ClearCacheResult(val freedBytes: Long, val songs: Int)

@Serializable
data class PadInfo(val id: String, val name: String, val model: String? = null, val role: PadRole, val pairedAt: Double, val lastSeenAt: Double? = null, val connected: Boolean = false)

@Serializable
data class OwnerSession(val ownerToken: String, val expiresAt: Double)

@Serializable
data class OwnerOverview(val pinRequired: Boolean, val services: JsonElement? = null, val storage: StorageReport, val pads: List<PadInfo>, val processingPaused: Boolean = false)

@Serializable
data class SessionInfo(val id: Long, val name: String? = null, val startedAt: Double, val playCount: Int)

@Serializable
data class CatalogTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0,
    val artworkURL: String? = null,
    val isrc: String? = null,
    val sources: List<String> = emptyList(),
    val year: Int? = null,
)

@Serializable
data class SongRequest(val id: String, val track: CatalogTrack, val singers: List<Singer>, val requestedAt: Double, val votes: Int)

@Serializable
data class MicStatus(val state: String, val deviceName: String? = null, val level: Double = 0.0, val message: String? = null)

@Serializable
data class ScoreCategory(val name: String, val value: Int, val note: String)

@Serializable
data class RankedName(val name: String, val count: Int, val colorHex: String? = null)

@Serializable
data class PerformanceResult(
    val entryID: String,
    val songTitle: String,
    val artist: String,
    val singers: List<Singer>,
    val total: Int? = null,
    val grade: String? = null,
    val title: String,
    val comment: String,
    val categories: List<ScoreCategory> = emptyList(),
    val badges: List<String> = emptyList(),
    val leaderboard: List<RankedName> = emptyList(),
    val recordingID: Long? = null,
    val shareURL: String? = null,
)

@Serializable
data class LiveState(
    val nowPlaying: NowPlaying = NowPlaying(),
    val queue: List<QueueEntry> = emptyList(),
    val settings: SingSettings = SingSettings(),
    val singers: List<Singer> = emptyList(),
    val session: SessionInfo? = null,
    val requests: List<SongRequest> = emptyList(),
    val mic: MicStatus? = null,
    val result: PerformanceResult? = null,
    val preparingByID: Map<String, Double> = emptyMap(),
) {
    fun preparing(id: Long): Double? = preparingByID[id.toString()]
}

@Serializable
data class ServerInfo(val name: String, val protocolVersion: Int, val minimumProtocolVersion: Int, val appVersion: String, val livePort: Int? = null)

@Serializable
data class PairRequest(val code: String, val deviceName: String, val deviceModel: String, val protocolVersion: Int = SingProtocol.VERSION)

@Serializable
data class PairResponse(val token: String, val padID: String, val role: PadRole, val server: ServerInfo)

@Serializable
data class SearchResponse(val songs: List<SongSummary>, val total: Int, val catalog: List<CatalogTrack> = emptyList())

@Serializable
data class Suggestion(val song: SongSummary, val reason: String)

@Serializable
data class RankedSong(val song: SongSummary, val count: Int)

@Serializable
data class NightSummary(val id: Long, val startedAt: Double, val plays: Int, val singers: Int)

@Serializable
data class StatsResponse(
    val period: String,
    val totalPlays: Int = 0,
    val totalMinutes: Int = 0,
    val distinctSongs: Int = 0,
    val topSongs: List<RankedSong> = emptyList(),
    val topArtists: List<RankedName> = emptyList(),
    val topSingers: List<RankedName> = emptyList(),
    val mostSkipped: List<RankedSong> = emptyList(),
    val busiestNights: List<NightSummary> = emptyList(),
    val decades: List<RankedName> = emptyList(),
)

@Serializable
data class ErrorBody(val code: String, val message: String)

@Serializable
data class LyricSegment(val text: String, val start: Double, val end: Double, val endsWord: Boolean = true)

@Serializable
data class LyricLine(
    val id: Int,
    val start: Double,
    val end: Double,
    val agent: String? = null,
    val segments: List<LyricSegment>,
    val background: List<LyricSegment> = emptyList(),
    val translation: String? = null,
) {
    val text: String get() = segments.joinToString("") { it.text + if (it.endsWord) " " else "" }.trim()
}

@Serializable
data class Lyrics(val kind: LyricsKind, val source: String, val language: String? = null, val lines: List<LyricLine>) {
    fun lineIndex(t: Double): Int? {
        var found: Int? = null
        for ((i, l) in lines.withIndex()) { if (l.start <= t) found = i else break }
        return found
    }
}

/** Live messages: `{"type": "...", "data": {...}}` in both directions. */
sealed interface ServerMessage {
    data class Hello(val server: ServerInfo, val role: PadRole, val padID: String) : ServerMessage
    data class State(val state: LiveState) : ServerMessage
    data class Position(val positionMs: Long, val at: Double) : ServerMessage
    data class Progress(val songID: Long, val progress: Double, val readiness: Readiness) : ServerMessage
    data class YoureUp(val songsAway: Int, val entryID: String, val singerID: Long) : ServerMessage
    data class Toast(val message: String) : ServerMessage
    data class Error(val body: ErrorBody) : ServerMessage

    companion object {
        @Serializable private data class HelloData(val server: ServerInfo, val role: PadRole, val padID: String)
        @Serializable private data class PositionData(val positionMs: Long, val at: Double)
        @Serializable private data class ProgressData(val songID: Long, val progress: Double, val readiness: Readiness)
        @Serializable private data class YoureUpData(val songsAway: Int, val entryID: String, val singerID: Long)
        @Serializable private data class ToastData(val message: String)

        fun parse(text: String): ServerMessage? {
            val j = SingProtocol.json
            val obj = runCatching { j.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
            val data = obj["data"] ?: return null
            return runCatching {
                when (obj["type"]?.jsonPrimitive?.content) {
                    "hello" -> j.decodeFromJsonElement<HelloData>(data).let { Hello(it.server, it.role, it.padID) }
                    "state" -> State(j.decodeFromJsonElement(data))
                    "position" -> j.decodeFromJsonElement<PositionData>(data).let { Position(it.positionMs, it.at) }
                    "progress" -> j.decodeFromJsonElement<ProgressData>(data).let { Progress(it.songID, it.progress, it.readiness) }
                    "youreUp" -> j.decodeFromJsonElement<YoureUpData>(data).let { YoureUp(it.songsAway, it.entryID, it.singerID) }
                    "toast" -> Toast(j.decodeFromJsonElement<ToastData>(data).message)
                    "error" -> Error(j.decodeFromJsonElement(data))
                    else -> null
                }
            }.getOrNull()
        }
    }
}

object ClientMessage {
    private fun msg(type: String, data: JsonElement?): String =
        buildJsonObject { put("type", type); if (data != null) put("data", data) }.toString()

    private inline fun <reified T> enc(v: T) = SingProtocol.json.encodeToJsonElement(v)

    fun auth(token: String) = msg("auth", buildJsonObject { put("token", token) })
    fun enqueue(songID: Long, singerIDs: List<Long>, playNext: Boolean) =
        msg("enqueue", buildJsonObject { put("songID", songID); put("singerIDs", enc(singerIDs)); put("playNext", playNext) })
    fun remove(entryID: String) = msg("remove", buildJsonObject { put("entryID", entryID) })
    fun move(entryID: String, toIndex: Int) = msg("move", buildJsonObject { put("entryID", entryID); put("toIndex", toIndex) })
    fun transport(command: String) = msg("transport", buildJsonObject { put("command", command) })
    fun setGuideVocal(v: Double) = msg("setGuideVocal", buildJsonObject { put("value", v) })
    fun updateSettings(s: SingSettings) = msg("updateSettings", enc(s))
    fun request(track: CatalogTrack, singerIDs: List<Long>) =
        msg("request", buildJsonObject { put("track", enc(track)); put("singerIDs", enc(singerIDs)) })
    fun setKey(semitones: Int) = msg("setKey", buildJsonObject { put("semitones", semitones) })
    fun setTempo(v: Double) = msg("setTempo", buildJsonObject { put("value", v) })
    fun setLyricOffset(ms: Int) = msg("setLyricOffset", buildJsonObject { put("ms", ms) })
    fun ping() = msg("ping", null)
}

fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.content

@Serializable
data class RecordingInfo(
    val id: Long,
    val songID: Long? = null,
    val songTitle: String,
    val artist: String = "",
    val singers: List<Singer> = emptyList(),
    val recordedAt: Double,
    val durationMs: Long = 0,
    val score: Int? = null,
    val kept: Boolean = false,
    val shareToken: String = "",
)

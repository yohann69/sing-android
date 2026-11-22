package io.github.yohann69.sing

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.yohann69.sing.net.Discovery
import io.github.yohann69.sing.net.Endpoint
import io.github.yohann69.sing.net.SingClient
import io.github.yohann69.sing.protocol.CatalogTrack
import io.github.yohann69.sing.protocol.SongRating
import io.github.yohann69.sing.protocol.OwnerOverview
import io.github.yohann69.sing.protocol.ClearCacheRequest
import io.github.yohann69.sing.protocol.ClientMessage
import io.github.yohann69.sing.protocol.LiveState
import io.github.yohann69.sing.protocol.Lyrics
import io.github.yohann69.sing.protocol.PadRole
import io.github.yohann69.sing.protocol.ServerMessage
import io.github.yohann69.sing.protocol.SingSettings
import io.github.yohann69.sing.protocol.SongSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Saved(val endpoint: Endpoint, val token: String, val role: PadRole, val padID: String)

class PadViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("sing", 0)
    val discovery = Discovery(app)

    private val _saved = MutableStateFlow(loadSaved())
    val saved: StateFlow<Saved?> = _saved.asStateFlow()
    private val _state = MutableStateFlow(LiveState())
    val state: StateFlow<LiveState> = _state.asStateFlow()
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()
    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    private val _singer = MutableStateFlow(prefs.getLong("singer", -1).takeIf { it >= 0 })
    val singerId: StateFlow<Long?> = _singer.asStateFlow()

    var client: SingClient? = null
        private set
    private var live: Job? = null
    private var lyricsFor: Long? = null

    init {
        if (_saved.value != null) connect()
    }

    private fun loadSaved(): Saved? {
        val host = prefs.getString("host", null) ?: return null
        val token = prefs.getString("token", null) ?: return null
        return Saved(Endpoint(prefs.getString("name", host)!!, host, prefs.getInt("port", 47800)), token,
            PadRole.valueOf(prefs.getString("role", "guest")!!), prefs.getString("pad", "")!!)
    }

    private fun persist(s: Saved?) {
        prefs.edit().apply {
            if (s == null) clear() else {
                putString("host", s.endpoint.host); putString("name", s.endpoint.name); putInt("port", s.endpoint.port)
                putString("token", s.token); putString("role", s.role.name); putString("pad", s.padID)
            }
        }.apply()
    }

    val isHost get() = _saved.value?.role == PadRole.host

    fun showToast(t: String) { _toast.value = t }
    fun clearToast() { _toast.value = null }

    fun pair(endpoint: Endpoint, code: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        runCatching { SingClient(endpoint).pair(code, Build.MODEL ?: "Android", "android") }
            .onSuccess { r ->
                val s = Saved(endpoint.copy(name = r.server.name), r.token, r.role, r.padID)
                persist(s); _saved.value = s; connect(); onDone(null)
            }
            .onFailure { onDone(it.message ?: "Could not reach the Mac") }
    }

    fun joinLink(link: String) {
        val (ep, code) = Endpoint.parse(link) ?: return showToast("That code is not a Sing! link")
        pair(ep, code) { err -> err?.let(::showToast) }
    }

    fun connect() {
        val s = _saved.value ?: return
        live?.cancel(); client?.close()
        val c = SingClient(s.endpoint, s.token)
        client = c
        live = c.runLive(viewModelScope, onStatus = { _connected.value = it }) { m -> handle(m) }
        loadRatings()
    }

    private val _ratings = MutableStateFlow<Map<Long, SongRating>>(emptyMap())
    /** The current singer's thumbs, favorites and never-suggest marks. */
    val ratings: StateFlow<Map<Long, SongRating>> = _ratings.asStateFlow()

    fun loadRatings() = viewModelScope.launch {
        val id = _singer.value ?: return@launch run { _ratings.value = emptyMap() }
        runCatching { client?.ratings(id) }.getOrNull()?.let { list -> _ratings.value = list.associateBy { it.songID } }
    }

    fun rate(songId: Long, change: (SongRating) -> SongRating) {
        val singer = _singer.value ?: return showToast("Pick who you are first")
        val r = change(_ratings.value[songId] ?: SongRating(songId, singer))
        _ratings.update { it + (songId to r) }
        viewModelScope.launch { runCatching { client?.rate(r) } }
    }

    private val _owner = MutableStateFlow<OwnerOverview?>(null)
    val owner: StateFlow<OwnerOverview?> = _owner.asStateFlow()
    private val _ownerLocked = MutableStateFlow(false)
    val ownerLocked: StateFlow<Boolean> = _ownerLocked.asStateFlow()

    fun loadOwner() = viewModelScope.launch {
        runCatching { client?.ownerOverview() }
            .onSuccess { _owner.value = it; _ownerLocked.value = false }
            .onFailure { e -> if ((e as? io.github.yohann69.sing.net.ApiException)?.code == "owner") _ownerLocked.value = true else showToast(e.message ?: "Could not load") }
    }

    fun unlockOwner(pin: String) = viewModelScope.launch {
        runCatching { client?.unlockOwner(pin) }
            .onSuccess { s -> client?.ownerToken = s?.ownerToken; loadOwner() }
            .onFailure { showToast(it.message ?: "Wrong PIN") }
    }

    fun clearCache(r: ClearCacheRequest) = viewModelScope.launch {
        runCatching { client?.clearCache(r) }.getOrNull()?.let { showToast("Freed ${android.text.format.Formatter.formatShortFileSize(getApplication(), it.freedBytes)}") }
        loadOwner()
    }

    fun saveSongSettings(id: Long, s: io.github.yohann69.sing.protocol.SongSettings) = viewModelScope.launch { runCatching { client?.updateSongSettings(id, s) } }

    fun unpair(padID: String) = viewModelScope.launch { runCatching { client?.unpair(padID) }; loadOwner() }

    fun updateOwnerSettings(change: (SingSettings) -> SingSettings) {
        val s = change(_state.value.settings)
        _state.update { it.copy(settings = s) }
        viewModelScope.launch { runCatching { client?.updateOwnerSettings(s) } }
    }

    fun disconnect() {
        live?.cancel(); client?.close(); client = null
        persist(null); prefs.edit().putString("translate", _language.value).apply(); _saved.value = null; _state.value = LiveState(); _connected.value = false
    }

    private fun handle(m: ServerMessage) {
        when (m) {
            is ServerMessage.State -> { _state.value = m.state; refreshLyrics() }
            is ServerMessage.Position -> _state.update { it.copy(nowPlaying = it.nowPlaying.copy(positionMs = m.positionMs, positionAt = m.at)) }
            is ServerMessage.Progress -> _state.update {
                val map = it.preparingByID.toMutableMap()
                if (m.readiness.name == "ready") map.remove(m.songID.toString()) else map[m.songID.toString()] = m.progress
                it.copy(preparingByID = map)
            }
            is ServerMessage.YoureUp -> if (m.singerID == _singer.value && m.songsAway <= 2)
                showToast(if (m.songsAway == 1) "You're up next! 🎤" else "You're up in ${m.songsAway} songs")
            is ServerMessage.Toast -> showToast(m.message)
            is ServerMessage.Error -> { showToast(m.body.message); if (m.body.code == "unauthorized") disconnect() }
            is ServerMessage.Hello -> {}
        }
    }

    private fun refreshLyrics() {
        val id = _state.value.nowPlaying.entry?.song?.id
        if (id == lyricsFor) return
        lyricsFor = id; _lyrics.value = null
        if (id != null) viewModelScope.launch { _lyrics.value = runCatching { client?.lyrics(id, _language.value) }.getOrNull() }
    }

    private val _language = MutableStateFlow(prefs.getString("translate", null))
    val language: StateFlow<String?> = _language.asStateFlow()
    private val _languages = MutableStateFlow(emptyList<SingClient.Language>())
    val languages: StateFlow<List<SingClient.Language>> = _languages.asStateFlow()

    fun loadLanguages() = viewModelScope.launch {
        if (_languages.value.isEmpty()) runCatching { client?.translationLanguages() }.getOrNull()?.let { _languages.value = it }
    }

    /** This device's own translation language for the lyrics it shows. */
    fun setLanguage(code: String?) {
        _language.value = code
        prefs.edit().putString("translate", code).apply()
        lyricsFor = null
        refreshLyrics()
    }

    fun chooseSinger(id: Long) { _singer.value = id; prefs.edit().putLong("singer", id).apply(); loadRatings() }

    fun createSinger(name: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        runCatching { client!!.createSinger(name, null) }
            .onSuccess { chooseSinger(it.id); _state.update { st -> if (st.singers.any { s -> s.id == it.id }) st else st.copy(singers = st.singers + it) }; onDone(null) }
            .onFailure { onDone(it.message) }
    }

    fun enqueue(song: SongSummary, singers: List<Long>, playNext: Boolean) {
        client?.send(ClientMessage.enqueue(song.id, singers, playNext))
        showToast(if (playNext) "${song.title} plays next" else "${song.title} added to the queue")
    }

    fun request(track: CatalogTrack) {
        client?.send(ClientMessage.request(track, listOfNotNull(_singer.value)))
        showToast("Requested ${track.title}")
    }

    fun remove(entryID: String) = client?.send(ClientMessage.remove(entryID))
    fun move(entryID: String, to: Int) = client?.send(ClientMessage.move(entryID, to))
    fun transport(cmd: String) = client?.send(ClientMessage.transport(cmd))
    fun guideVocal(v: Double) = client?.send(ClientMessage.setGuideVocal(v))
    fun setKey(k: Int) = client?.send(ClientMessage.setKey(k.coerceIn(-6, 6)))
    fun setLyricOffset(ms: Int) = client?.send(ClientMessage.setLyricOffset(ms.coerceIn(-5000, 5000)))
    fun setTempo(t: Double) = client?.send(ClientMessage.setTempo(t.coerceIn(0.75, 1.25)))
    fun updateSettings(change: (SingSettings) -> SingSettings) {
        val s = change(_state.value.settings)
        _state.update { it.copy(settings = s) }
        client?.send(ClientMessage.updateSettings(s))
    }
}

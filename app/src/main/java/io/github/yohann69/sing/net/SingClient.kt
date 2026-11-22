package io.github.yohann69.sing.net

import android.content.Context
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import io.github.yohann69.sing.protocol.*
import io.github.yohann69.sing.protocol.ErrorBody
import io.github.yohann69.sing.protocol.Lyrics
import io.github.yohann69.sing.protocol.PairRequest
import io.github.yohann69.sing.protocol.PairResponse
import io.github.yohann69.sing.protocol.SearchResponse
import io.github.yohann69.sing.protocol.ServerMessage
import io.github.yohann69.sing.protocol.Singer
import io.github.yohann69.sing.protocol.SingProtocol
import io.github.yohann69.sing.protocol.StatsResponse
import io.github.yohann69.sing.protocol.Suggestion
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.math.min

data class Endpoint(val name: String, val host: String, val port: Int) {
    val base get() = "http://${if (host.contains(':')) "[$host]" else host}:$port"

    companion object {
        /** `sing://host:port?code=…` or the TV's `http://host:port/join?code=…`. */
        fun parse(link: String): Pair<Endpoint, String>? {
            val uri = runCatching { Uri.parse(link) }.getOrNull() ?: return null
            if (uri.scheme !in listOf("sing", "http", "https")) return null
            val host = uri.host ?: return null
            val code = uri.getQueryParameter("code") ?: return null
            val port = if (uri.port > 0) uri.port else SingProtocol.DEFAULT_PORT
            return Endpoint(uri.getQueryParameter("name") ?: host, host, port) to code
        }
    }
}

class ApiException(message: String, val code: String) : Exception(message)

/** HTTP calls to the Mac, plus the live WebSocket on the next port up. */
class SingClient(val endpoint: Endpoint, var token: String? = null) {
    /** From [unlockOwner], for owner-only calls when the Mac has an owner PIN. */
    var ownerToken: String? = null
    private val json = SingProtocol.json
    private val http = HttpClient(OkHttp) {
        install(WebSockets)
        install(HttpTimeout) { requestTimeoutMillis = 10_000; connectTimeoutMillis = 5_000 }
    }

    private suspend fun check(r: HttpResponse): String {
        val text = r.bodyAsText()
        if (!r.status.isSuccess()) {
            val e = runCatching { json.decodeFromString<ErrorBody>(text) }.getOrNull()
            if (r.status.value == 401) throw ApiException(e?.message ?: "This device is no longer paired.", "unauthorized")
            throw ApiException(e?.message ?: "The Mac answered with an error (${r.status.value}).", e?.code ?: "http")
        }
        return text
    }

    private suspend inline fun <reified T> get(path: String, vararg q: Pair<String, Any?>): T {
        val r = http.get(endpoint.base + path) {
            token?.let { header("Authorization", "Bearer $it") }
            ownerToken?.let { header("X-Sing-Owner", it) }
            q.forEach { (k, v) -> if (v != null) parameter(k, v) }
        }
        return json.decodeFromString(check(r))
    }

    suspend fun pair(code: String, deviceName: String, model: String): PairResponse {
        val r = http.post(endpoint.base + "/v1/pair") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PairRequest.serializer(), PairRequest(code.trim(), deviceName, model)))
        }
        return json.decodeFromString(check(r))
    }

    @Serializable private data class NewSinger(val name: String, val emoji: String?)

    suspend fun search(q: String, readyOnly: Boolean = false, duetOnly: Boolean = false, favoritesOf: Long? = null): SearchResponse =
        get("/v1/songs", "q" to q, "ready" to if (readyOnly) "1" else null, "duet" to if (duetOnly) "1" else null, "favorites" to favoritesOf)

    suspend fun humSearch(audio: ByteArray): List<SongSummary> {
        val r = http.post(endpoint.base + "/v1/hum") {
            token?.let { header("Authorization", "Bearer $it") }
            contentType(ContentType.parse("audio/mp4"))
            setBody(audio)
            timeout { requestTimeoutMillis = 30_000 }
        }
        return json.decodeFromString<SearchResponse>(check(r)).songs
    }

    suspend fun recap(): PartyRecap = get("/v1/recap")
    suspend fun songSettings(id: Long): SongSettings = get("/v1/songs/$id/settings")
    suspend fun updateSongSettings(id: Long, s: SongSettings): SongSettings = send(HttpMethod.Put, "/v1/songs/$id/settings", json.encodeToString(SongSettings.serializer(), s))
    suspend fun recordings(singer: Long?): List<RecordingInfo> = get("/v1/recordings", "singer" to singer)
    fun recordingAudioUrl(id: Long) = "${endpoint.base}/v1/recordings/$id/audio?token=${Uri.encode(token ?: "")}"
    fun shareUrl(r: RecordingInfo) = "${endpoint.base}/r/${r.shareToken}"
    suspend fun keepRecording(id: Long, keep: Boolean) { send<kotlinx.serialization.json.JsonElement>(HttpMethod.Post, "/v1/recordings/$id/${if (keep) "keep" else "unkeep"}", "{}") }
    suspend fun deleteRecording(id: Long) { send<kotlinx.serialization.json.JsonElement>(HttpMethod.Delete, "/v1/recordings/$id", "{}") }
    suspend fun replayRecording(id: Long) { send<kotlinx.serialization.json.JsonElement>(HttpMethod.Post, "/v1/recordings/$id/replay", "{}") }

    suspend fun ratings(singer: Long): List<SongRating> = get("/v1/ratings", "singer" to singer)
    suspend fun rate(r: SongRating): SongRating = send(HttpMethod.Post, "/v1/ratings", json.encodeToString(SongRating.serializer(), r))
    suspend fun unlockOwner(pin: String): OwnerSession = send(HttpMethod.Post, "/v1/owner/unlock", "{\"pin\":\"${pin.filter { it.isDigit() }}\"}")
    suspend fun ownerOverview(): OwnerOverview = get("/v1/owner")
    suspend fun clearCache(r: ClearCacheRequest): ClearCacheResult = send(HttpMethod.Post, "/v1/owner/storage/clear", json.encodeToString(ClearCacheRequest.serializer(), r))
    suspend fun updateOwnerSettings(s: SingSettings): SingSettings = send(HttpMethod.Put, "/v1/owner/settings", json.encodeToString(SingSettings.serializer(), s))
    suspend fun unpair(padID: String) { send<kotlinx.serialization.json.JsonElement>(HttpMethod.Delete, "/v1/owner/pads/$padID", "{}") }

    private suspend inline fun <reified T> send(method: HttpMethod, path: String, body: String): T {
        val r = http.request(endpoint.base + path) {
            this.method = method
            token?.let { header("Authorization", "Bearer $it") }
            ownerToken?.let { header("X-Sing-Owner", it) }
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return json.decodeFromString(check(r))
    }
    suspend fun lyrics(id: Long, translateTo: String? = null): Lyrics = get("/v1/songs/$id/lyrics", "translate" to translateTo)

    @Serializable data class Language(val code: String, val name: String)
    suspend fun translationLanguages(): List<Language> = get("/v1/translation/languages")
    suspend fun stats(period: String): StatsResponse = get("/v1/stats", "period" to period)
    suspend fun suggestions(singer: Long?): List<Suggestion> = get("/v1/suggestions", "singer" to singer)
    suspend fun createSinger(name: String, emoji: String?): Singer {
        val r = http.post(endpoint.base + "/v1/singers") {
            token?.let { header("Authorization", "Bearer $it") }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(NewSinger.serializer(), NewSinger(name, emoji)))
        }
        return json.decodeFromString(check(r))
    }

    fun artworkUrl(id: Long) = "${endpoint.base}/v1/songs/$id/artwork?token=${Uri.encode(token ?: "")}"

    private val pending = Channel<String>(Channel.BUFFERED)
    fun send(message: String) { pending.trySend(message) }

    /** Keeps the live socket open, reconnecting with backoff; every message lands in [onMessage]. */
    fun runLive(scope: CoroutineScope, onStatus: (Boolean) -> Unit, onMessage: (ServerMessage) -> Unit): Job = scope.launch {
        var attempt = 0
        val wsHost = if (endpoint.host.contains(':')) "[${endpoint.host}]" else endpoint.host
        while (isActive) {
            runCatching {
                http.webSocket("ws://$wsHost:${endpoint.port + 1}/v1/live") {
                    send(Frame.Text(ClientMessage.auth(token ?: "")))
                    attempt = 0
                    onStatus(true)
                    val sender = launch { for (m in pending) send(Frame.Text(m)) }
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) ServerMessage.parse(frame.readText())?.let(onMessage)
                        }
                    } finally {
                        sender.cancel()
                    }
                }
            }
            onStatus(false)
            attempt++
            delay(min(10_000L, 500L * (1L shl min(attempt, 5))))
        }
    }

    fun close() = http.close()
}

/** Finds Macs advertising `_sing._tcp` with Android's built-in mDNS. */
class Discovery(context: Context) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _found = MutableStateFlow<List<Endpoint>>(emptyList())
    val found: StateFlow<List<Endpoint>> = _found
    private var listener: NsdManager.DiscoveryListener? = null

    fun start() {
        if (listener != null) return
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onServiceLost(info: NsdServiceInfo) {
                _found.value = _found.value.filterNot { it.name == info.serviceName }
            }
            @Suppress("DEPRECATION")
            override fun onServiceFound(info: NsdServiceInfo) {
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(r: NsdServiceInfo) {
                        val host = r.host?.hostAddress?.substringBefore('%') ?: return
                        val ep = Endpoint(r.serviceName, host, r.port)
                        _found.value = (_found.value.filterNot { it.name == ep.name } + ep).sortedBy { it.name }
                    }
                })
            }
        }
        listener = l
        nsd.discoverServices(SingProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, l)
    }

    fun stop() {
        listener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        listener = null
    }
}

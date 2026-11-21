package io.github.yohann69.sing

import io.github.yohann69.sing.protocol.ClientMessage
import io.github.yohann69.sing.protocol.LiveState
import io.github.yohann69.sing.protocol.Lyrics
import io.github.yohann69.sing.protocol.SearchResponse
import io.github.yohann69.sing.protocol.ServerMessage
import io.github.yohann69.sing.protocol.SingProtocol
import io.github.yohann69.sing.protocol.StatsResponse
import io.github.yohann69.sing.protocol.Suggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Payloads recorded from a real Mac host, so the Kotlin types stay in step with Swift. */
class ContractTest {
    private fun fixture(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()
    private val json = SingProtocol.json

    @Test fun liveState() {
        val s = json.decodeFromString<LiveState>(fixture("state.json"))
        assertTrue(s.singers.isNotEmpty())
        val msg = ServerMessage.parse("""{"type":"state","data":${fixture("state.json")}}""")
        assertTrue(msg is ServerMessage.State)
    }

    @Test fun searchWithCatalog() {
        val r = json.decodeFromString<SearchResponse>(fixture("search.json"))
        assertTrue(r.songs.isNotEmpty())
        assertTrue(r.catalog.isNotEmpty())
    }

    @Test fun lyricsStatsSuggestions() {
        val l = json.decodeFromString<Lyrics>(fixture("lyrics.json"))
        assertTrue(l.lines.size > 10)
        assertEquals(0, l.lineIndex(l.lines[0].start + 0.01))
        json.decodeFromString<StatsResponse>(fixture("stats.json"))
        json.decodeFromString<List<Suggestion>>(fixture("suggestions.json"))
    }

    @Test fun clientMessages() {
        assertEquals("""{"type":"transport","data":{"command":"skip"}}""", ClientMessage.transport("skip"))
        assertEquals("""{"type":"enqueue","data":{"songID":3,"singerIDs":[1,2],"playNext":false}}""", ClientMessage.enqueue(3, listOf(1, 2), false))
        assertEquals("""{"type":"ping"}""", ClientMessage.ping())
    }
}

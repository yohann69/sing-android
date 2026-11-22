package io.github.yohann69.sing

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/** French UI text: English stays the key, assets/fr.json holds the translations. */
object Tr {
    @Volatile private var table: Map<String, String> = emptyMap()

    fun load(context: Context) {
        if (Locale.getDefault().language != "fr") return
        table = runCatching {
            val text = context.assets.open("fr.json").bufferedReader().use { it.readText() }
            Json.parseToJsonElement(text).jsonObject.mapValues { it.value.jsonPrimitive.content }
        }.getOrDefault(emptyMap())
    }

    fun get(s: String) = table[s] ?: s
}

fun tr(s: String): String = Tr.get(s)

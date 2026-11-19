package com.goreecloud.gifs.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Pinned OpenMoji 2026 snapshot; CC BY-SA 4.0 metadata.
 * Art remains on the original project CDN and is never re-licensed by GoreeCloud.
 */
class OpenMojiCatalog(private val context: Context) {
    private data class Entry(val item: MediaItem, val terms: String)
    private val lock = Mutex()
    private var catalog: List<Entry>? = null

    suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val entries = lock.withLock {
            catalog ?: load().also { catalog = it }
        }
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered = if (terms.isEmpty()) entries else entries.filter { entry ->
            terms.all { entry.terms.contains(it) }
        }
        filtered.take(100).map { it.item }
    }

    private fun load(): List<Entry> {
        val json = context.assets.open("openmoji.json").bufferedReader().use { it.readText() }
        val data = JSONArray(json)
        val list = ArrayList<Entry>(data.length())
        val hexcode = Regex("^[A-F0-9]+(?:-[A-F0-9]+)*$")
        for (i in 0 until data.length()) {
            val node = data.optJSONObject(i) ?: continue
            val code = node.optString("hexcode")
            if (!hexcode.matches(code)) continue
            val title = node.optString("annotation").ifBlank { code }
            val author = node.optString("openmoji_author").ifBlank { "OpenMoji contributors" }
            val base = "https://raw.githubusercontent.com/hfg-gmuend/openmoji/" +
                SNAPSHOT + "/color/"
            val item = MediaItem(
                id = "openmoji:" + code,
                title = title,
                kind = MediaKind.STICKER,
                url = base + "618x618/" + code + ".png",
                thumbnail = base + "72x72/" + code + ".png",
                credit = "OpenMoji contributors; artwork by " + author,
                sourceUrl = "https://github.com/hfg-gmuend/openmoji",
                license = "CC BY-SA 4.0"
            )
            val searchTerms = listOf(title, node.optString("tags"),
                node.optString("openmoji_tags"), node.optString("group"),
                node.optString("subgroups"), node.optString("emoji"))
                .joinToString(" ").lowercase()
            list += Entry(item, searchTerms)
        }
        return list
    }

    companion object {
        const val SNAPSHOT = "3e01de523ddefe9c5e46a7765b15efef04bf9002"
    }
}

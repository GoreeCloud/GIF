package com.goreecloud.gifs.data

import org.json.JSONObject
import java.net.URI

enum class MediaKind { GIF, STICKER }

data class MediaItem(
    val id: String,
    val title: String,
    val kind: MediaKind,
    val url: String,
    val thumbnail: String,
    val credit: String,
    val sourceUrl: String,
    val license: String,
    val localFile: String? = null
) {
    val mimeType: String get() = if (kind == MediaKind.GIF) "image/gif" else "image/png"
    val extension: String get() = if (kind == MediaKind.GIF) "gif" else "png"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("kind", kind.name)
        put("url", url); put("thumbnail", thumbnail); put("credit", credit)
        put("sourceUrl", sourceUrl); put("license", license)
        if (localFile != null) put("localFile", localFile)
    }

    companion object {
        fun fromJson(j: JSONObject): MediaItem? = runCatching {
            val id = j.getString("id")
            val kind = MediaKind.valueOf(j.getString("kind"))
            MediaItem(
                id = id,
                title = j.getString("title"),
                kind = kind,
                url = j.getString("url"),
                thumbnail = j.getString("thumbnail"),
                credit = j.optString("credit"),
                sourceUrl = j.optString("sourceUrl"),
                license = j.optString("license"),
                localFile = j.optString("localFile").takeIf {
                    it.isNotBlank() && SavedFileNames.matches(id, kind, it)
                }
            )
        }.getOrNull()
    }
}

object SourcePolicy {
    private val ip4 = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")
    fun isSafeHttps(raw: String): Boolean = try {
        val uri = URI(raw)
        val host = uri.host?.lowercase() ?: return false
        if (uri.scheme?.lowercase() != "https" || uri.rawUserInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        if (host == "localhost" || host.endsWith(".localhost") ||
            host.endsWith(".local") || host.endsWith(".internal") || host.contains(":")) return false
        if (host.matches(ip4)) {
            val parts = host.split('.').map { it.toInt() }
            if (parts.any { it > 255 }) return false
            val a = parts[0]; val b = parts[1]
            if (a == 0 || a == 10 || a == 127 || a >= 224 ||
                (a == 169 && b == 254) || (a == 172 && b in 16..31) ||
                (a == 192 && b == 168) || (a == 100 && b in 64..127) ||
                (a == 192 && b == 0) || (a == 198 && b in 18..19)) return false
        }
        host.contains('.')
    } catch (_: Exception) { false }
}

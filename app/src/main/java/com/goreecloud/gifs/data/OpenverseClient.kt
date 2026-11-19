package com.goreecloud.gifs.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenverseClient(private val client: OkHttpClient) {
    suspend fun searchGifs(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val q = query.trim().take(100)
        if (q.isEmpty()) return@withContext emptyList()
        val url = "https://api.openverse.org/v1/images/".toHttpUrl()
            .newBuilder()
            .addQueryParameter("q", q)
            .addQueryParameter("extension", "gif")
            .addQueryParameter("mature", "false")
            .addQueryParameter("page_size", "40")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "GoreeCloud-GIFs/0.1 (+https://github.com/GoreeCloud/GIF)")
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code == 429) throw IOException("Openverse rate limit reached. Try again later.")
                throw IOException("GIF search unavailable (HTTP " + response.code + ").")
            }
            response.body?.string() ?: throw IOException("Openverse returned no data.")
        }
        parseResults(body)
    }

    companion object {
        fun parseResults(body: String): List<MediaItem> {
            val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
            val items = ArrayList<MediaItem>()
            for (i in 0 until results.length()) {
                val result = results.optJSONObject(i) ?: continue
                if (result.optBoolean("mature", false)) continue
                if (!result.optString("filetype").equals("gif", ignoreCase = true)) continue
                val url = result.optString("url")
                val thumb = result.optString("thumbnail").takeIf(SourcePolicy::isSafeHttps) ?: url
                val source = result.optString("foreign_landing_url")
                val id = result.optString("id").takeIf { it.length in 8..100 } ?: continue
                if (!SourcePolicy.isSafeHttps(url) || !SourcePolicy.isSafeHttps(thumb)) continue
                val title = result.optString("title").ifBlank { "Untitled GIF" }.take(180)
                val credit = result.optString("attribution").ifBlank {
                    result.optString("creator").ifBlank { "Creator unknown" }
                }.take(1000)
                val license = listOf(
                    result.optString("license").uppercase(),
                    result.optString("license_version")
                ).filter { it.isNotBlank() }.joinToString(" ")
                items += MediaItem(
                    id = "openverse:" + id, title = title, kind = MediaKind.GIF,
                    url = url, thumbnail = thumb, credit = credit,
                    sourceUrl = source.takeIf(SourcePolicy::isSafeHttps)
                        ?: result.optString("license_url").takeIf(SourcePolicy::isSafeHttps) ?: url,
                    license = license.ifBlank { "See source for license" }
                )
            }
            return items.distinctBy { it.id }
        }
    }
}

fun mediaHttpClient() = OkHttpClient.Builder()
    .callTimeout(30, TimeUnit.SECONDS)
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .followRedirects(false)
    .followSslRedirects(false)
    .build()

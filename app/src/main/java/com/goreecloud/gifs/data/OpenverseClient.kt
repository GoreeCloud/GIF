package com.goreecloud.gifs.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GifSearchPage(
    val items: List<MediaItem>,
    val page: Int,
    val hasMore: Boolean
)

/**
 * No API key required. Openverse's anonymous endpoint permits at most 20
 * records per page (larger values return HTTP 401).
 */
class OpenverseClient(private val client: OkHttpClient) {
    suspend fun searchGifs(query: String, page: Int = 1): GifSearchPage = withContext(Dispatchers.IO) {
        val q = query.trim().take(MAX_QUERY_CHARS)
        if (q.isEmpty()) return@withContext GifSearchPage(emptyList(), 1, false)

        val requestedPage = page.coerceIn(1, MAX_PAGES)
        val request = Request.Builder()
            .url(buildSearchUrl(q, requestedPage))
            .header("Accept", "application/json")
            .header("User-Agent", "GoreeCloud-GIFs/0.1 (+https://github.com/GoreeCloud/GIF)")
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                when (response.code) {
                    429 -> throw IOException("Openverse is busy. Please try again shortly.")
                    401, 403 -> throw IOException("Openverse refused the anonymous request.")
                    else -> throw IOException("GIF search unavailable (HTTP " + response.code + ").")
                }
            }
            // Limit the response to bound memory use from the search endpoint.
            val responseBody = response.body ?: throw IOException("Openverse returned no data.")
            if (responseBody.contentLength() > MAX_JSON_BYTES) {
                throw IOException("Openverse response too large.")
            }
            responseBody.charStream().use { reader ->
                val buffer = CharArray(8192)
                val content = StringBuilder()
                while (true) {
                    val read = reader.read(buffer)
                    if (read == -1) break
                    if (content.length.toLong() + read > MAX_JSON_BYTES) {
                        throw IOException("Openverse response too large.")
                    }
                    content.append(buffer, 0, read)
                }
                content.toString()
            }
        }
        parsePage(body, requestedPage)
    }

    companion object {
        const val PAGE_SIZE = 20
        const val MAX_PAGES = 100
        const val MAX_QUERY_CHARS = 100
        private const val MAX_JSON_BYTES = 2_000_000L

        fun buildSearchUrl(query: String, page: Int = 1): HttpUrl =
            "https://api.openverse.org/v1/images/".toHttpUrl().newBuilder()
                .addQueryParameter("q", query.trim().take(MAX_QUERY_CHARS))
                .addQueryParameter("extension", "gif")
                .addQueryParameter("mature", "false")
                .addQueryParameter("page_size", PAGE_SIZE.toString())
                .addQueryParameter("page", page.coerceIn(1, MAX_PAGES).toString())
                .build()

        fun parsePage(body: String, page: Int): GifSearchPage {
            val root = JSONObject(body)
            val requestedPage = page.coerceIn(1, MAX_PAGES)
            val total = root.optInt("page_count", requestedPage).coerceAtLeast(0)
            return GifSearchPage(
                items = parseResults(body),
                page = requestedPage,
                hasMore = requestedPage < minOf(total, MAX_PAGES)
            )
        }

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

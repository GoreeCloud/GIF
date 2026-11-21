package com.goreecloud.gifs.data

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class OpenversePrivacyTest {
    @Test fun blankGifQueryNeverOpensAConnection() = runBlocking {
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor {
                calls.incrementAndGet()
                throw AssertionError("Blank GIF query must never trigger an HTTP call")
            }).build()
        val result = OpenverseClient(client).searchGifs(
            GallerySearchQuery.normalize("  ", MediaKind.GIF)
        )
        assertTrue(result.items.isEmpty())
        assertFalse(result.hasMore)
        assertEquals(0, calls.get())
    }

    @Test fun explicitlySubmittedTermUsesOneBoundedOpenverseRequest() = runBlocking {
        val requests = mutableListOf<Request>()
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            requests += chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("Synthetic JSON")
                .body("""{"results":[],"page_count":1}""".toResponseBody())
                .build()
        }).build()
        OpenverseClient(client).searchGifs(
            GallerySearchQuery.normalize("  cat reaction  ", MediaKind.GIF)
        )
        assertEquals(1, requests.size)
        assertEquals("cat reaction", requests.single().url.queryParameter("q"))
        assertEquals("https", requests.single().url.scheme)
    }
}

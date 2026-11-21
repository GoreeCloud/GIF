package com.goreecloud.gifs.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class OpenverseRetryIntegrationTest {
    private fun scripted(
        codes: List<Int>,
        retryAfter: String? = null,
        calls: AtomicInteger
    ): OkHttpClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        val index = calls.getAndIncrement()
        val code = codes[index.coerceAtMost(codes.lastIndex)]
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Synthetic fixture")
            .body("""{"page_count":1,"results":[]}""".toResponseBody())
            .apply { if (retryAfter != null) header("Retry-After", retryAfter) }
            .build()
    }).build()

    @Test fun transientErrorsRetryAtMostTwiceThenSucceed() = runBlocking {
        val calls = AtomicInteger()
        val waits = mutableListOf<Long>()
        val source = OpenverseClient(
            scripted(listOf(429, 503, 200), "1", calls)
        ) { waits += it }
        val result = source.searchGifs("cat")
        assertEquals(3, calls.get())
        assertEquals(listOf(1000L, 1000L), waits)
        assertTrue(result.items.isEmpty())
        assertEquals(1, result.page)
    }

    @Test fun permanentDenialNeverRetries() = runBlocking {
        val calls = AtomicInteger()
        val waits = mutableListOf<Long>()
        val source = OpenverseClient(scripted(listOf(403), calls = calls)) { waits += it }
        try {
            source.searchGifs("cat")
            fail("Anonymous search refusal should be actionable")
        } catch (error: IOException) {
            assertTrue(error.message.orEmpty().contains("refused"))
        }
        assertEquals(1, calls.get())
        assertTrue(waits.isEmpty())
    }

    @Test fun repeatedServerFailuresHaveStrictRequestLimit() = runBlocking {
        val calls = AtomicInteger()
        val waits = mutableListOf<Long>()
        val source = OpenverseClient(scripted(listOf(503), calls = calls)) { waits += it }
        try {
            source.searchGifs("cat")
            fail("Should stop after three total HTTP calls")
        } catch (_: IOException) {
            // Expected.
        }
        assertEquals(3, calls.get())
        assertEquals(listOf(750L, 1500L), waits)
    }

    @Test fun cancellationDuringBackoffPreventsNextRequest() = runBlocking {
        val calls = AtomicInteger()
        val source = OpenverseClient(scripted(listOf(429, 200), calls = calls)) {
            throw CancellationException("Search was superseded")
        }
        try {
            source.searchGifs("cat")
            fail("Backoff should be cancellable")
        } catch (_: CancellationException) {
            // Expected.
        }
        assertEquals(1, calls.get())
    }
}

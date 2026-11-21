package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class PreviewCachePolicyTest {
    @Test fun cacheIsAppPrivateAndBoundedTo64MiB() {
        val sandbox = Files.createTempDirectory("goreecloud-preview-cache-test").toFile()
        try {
            val cache = PreviewCachePolicy.create(sandbox)
            try {
                assertEquals(64L * 1024 * 1024, cache.maxSize)
                assertEquals(
                    sandbox.resolve("public-image-previews").canonicalPath,
                    cache.directory.toFile().canonicalPath
                )
                assertEquals(0L, cache.size)
            } finally {
                cache.shutdown()
            }
        } finally {
            sandbox.deleteRecursively()
        }
    }

    @Test fun cacheSizeDoesNotDependOnFavoriteStorage() {
        assertTrue(PreviewCachePolicy.MAX_DISK_BYTES > 0L)
        assertTrue(PreviewCachePolicy.MAX_DISK_BYTES < ImportPolicy.MAX_BYTES * 3)
    }
}

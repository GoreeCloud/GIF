package com.goreecloud.gifs.data

import coil3.disk.DiskCache
import coil3.disk.directory
import java.io.File

/**
 * Hard cap for public media preview files, separate from explicitly saved favorites.
 * All paths remain under Android's app-private, OS-evictable cache directory.
 */
object PreviewCachePolicy {
    const val MAX_DISK_BYTES = 64L * 1024 * 1024
    private const val DIRECTORY = "public-image-previews"

    fun create(cacheDir: File): DiskCache =
        DiskCache.Builder()
            .directory(File(cacheDir, DIRECTORY))
            .maxSizeBytes(MAX_DISK_BYTES)
            .build()
}

package com.goreecloud.gifs.data

import java.io.IOException

/**
 * MediaStore success requires a complete copy and a published non-pending row.
 * Pure validation is shared by scoped-storage export and host unit tests.
 */
internal object MediaStoreExportCheck {
    fun requireCopied(expectedBytes: Long, actualBytes: Long) {
        if (expectedBytes <= 0L || expectedBytes != actualBytes) {
            throw IOException("Exported image could not be fully copied.")
        }
    }

    fun requirePublished(updatedRows: Int) {
        if (updatedRows <= 0) {
            throw IOException("Photo library did not publish the exported image.")
        }
    }
}

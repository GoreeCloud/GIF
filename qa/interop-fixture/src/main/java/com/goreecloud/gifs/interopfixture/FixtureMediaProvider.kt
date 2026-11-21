package com.goreecloud.gifs.interopfixture

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Separate app's explicit, grant-scoped PNG provider.
 * No internet, storage or account permissions. Only a fixture-owned image.
 */
class FixtureMediaProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    private fun localImage(): File {
        val app = context ?: throw FileNotFoundException("Fixture context unavailable")
        val file = File(app.filesDir, "interop-source.png")
        if (!file.isFile) {
            val image = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            try {
                image.eraseColor(android.graphics.Color.rgb(45, 110, 215))
                file.outputStream().use { output ->
                    if (!image.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        throw FileNotFoundException("Could not prepare fixture PNG")
                    }
                }
            } finally {
                image.recycle()
            }
        }
        return file
    }

    private fun checkUri(uri: Uri) {
        if (uri != IMAGE_URI) throw FileNotFoundException("Unexpected fixture URI")
    }

    override fun getType(uri: Uri): String? {
        checkUri(uri)
        return "image/png"
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        checkUri(uri)
        if (mode != "r") throw FileNotFoundException("Fixture is read-only")
        return ParcelFileDescriptor.open(localImage(), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        checkUri(uri)
        val names = (projection?.toList() ?: listOf(
            OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        )).filter { it == OpenableColumns.DISPLAY_NAME || it == OpenableColumns.SIZE }
        val cursor = MatrixCursor(names.toTypedArray())
        cursor.addRow(names.map { column ->
            when (column) {
                OpenableColumns.DISPLAY_NAME -> "interop-source.png"
                OpenableColumns.SIZE -> localImage().length()
                else -> null
            }
        })
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        val IMAGE_URI: Uri =
            Uri.parse("content://com.goreecloud.gifs.interopfixture.media/interop-source.png")
    }
}

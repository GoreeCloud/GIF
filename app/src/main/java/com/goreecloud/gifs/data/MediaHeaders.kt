package com.goreecloud.gifs.data

import java.io.File
import java.io.IOException
import java.util.zip.CRC32

/**
 * Conservative GIF/PNG header checks before importing, exporting or decoding.
 * This is not a full image decoder, decompression-bomb proof or malware scan.
 */
object MediaHeaders {
    const val MAX_SIDE = 8192
    const val MAX_PIXELS = 16_000_000L
    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    )

    private fun u8(bytes: ByteArray, offset: Int): Int = bytes[offset].toInt() and 0xff

    private fun be32(bytes: ByteArray, offset: Int): Long =
        (u8(bytes, offset).toLong() shl 24) or
            (u8(bytes, offset + 1).toLong() shl 16) or
            (u8(bytes, offset + 2).toLong() shl 8) or
            u8(bytes, offset + 3).toLong()

    private fun dimensionsAllowed(width: Long, height: Long): Boolean =
        width in 1..MAX_SIDE.toLong() &&
            height in 1..MAX_SIDE.toLong() &&
            width * height <= MAX_PIXELS

    fun plausible(file: File, kind: MediaKind): Boolean {
        if (!file.isFile || file.length() !in 13..ImportPolicy.MAX_BYTES) return false
        return try {
            val header = ByteArray(33)
            var count = 0
            file.inputStream().use { stream ->
                while (count < header.size) {
                    val n = stream.read(header, count, header.size - count)
                    if (n == -1) break
                    if (n == 0) break
                    count += n
                }
            }
            when (kind) {
                MediaKind.GIF -> {
                    if (count < 13) return false
                    val signature = String(header, 0, 6, Charsets.US_ASCII)
                    if (signature !in setOf("GIF87a", "GIF89a")) return false
                    val width = u8(header, 6) + (u8(header, 7) shl 8)
                    val height = u8(header, 8) + (u8(header, 9) shl 8)
                    dimensionsAllowed(width.toLong(), height.toLong())
                }
                MediaKind.STICKER -> {
                    if (count < 33 || !header.copyOfRange(0, 8).contentEquals(pngSignature))
                        return false
                    if (be32(header, 8) != 13L ||
                        String(header, 12, 4, Charsets.US_ASCII) != "IHDR")
                        return false
                    val width = be32(header, 16)
                    val height = be32(header, 20)
                    if (!dimensionsAllowed(width, height)) return false
                    if (u8(header, 26) != 0 || u8(header, 27) != 0 ||
                        u8(header, 28) !in 0..1) return false
                    val depth = u8(header, 24)
                    val color = u8(header, 25)
                    val supported = when (color) {
                        0 -> depth in setOf(1, 2, 4, 8, 16)
                        2 -> depth == 8 || depth == 16
                        3 -> depth in setOf(1, 2, 4, 8)
                        4, 6 -> depth == 8 || depth == 16
                        else -> false
                    }
                    if (!supported) return false
                    val crc = CRC32().apply { update(header, 12, 17) }.value
                    crc == be32(header, 29)
                }
            }
        } catch (_: IOException) {
            false
        }
    }
}

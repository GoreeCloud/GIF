package com.goreecloud.gifs.data

import java.util.zip.CRC32

internal object TestMediaHeaders {
    fun gif(width: Int = 1, height: Int = 1): ByteArray =
        "GIF89a".toByteArray(Charsets.US_ASCII) + byteArrayOf(
            width.toByte(), (width shr 8).toByte(),
            height.toByte(), (height shr 8).toByte(), 0, 0, 0
        )

    fun png(width: Int = 1, height: Int = 1): ByteArray {
        val bytes = ByteArray(33)
        byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
            .copyInto(bytes)
        bytes[11] = 13
        "IHDR".toByteArray(Charsets.US_ASCII).copyInto(bytes, 12)
        fun write32(value: Int, offset: Int) {
            bytes[offset] = (value shr 24).toByte()
            bytes[offset + 1] = (value shr 16).toByte()
            bytes[offset + 2] = (value shr 8).toByte()
            bytes[offset + 3] = value.toByte()
        }
        write32(width, 16)
        write32(height, 20)
        bytes[24] = 8  // depth
        bytes[25] = 6  // RGBA
        val crc = CRC32().apply { update(bytes, 12, 17) }
        write32(crc.value.toInt(), 29)
        return bytes
    }
}

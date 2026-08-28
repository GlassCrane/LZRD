package com.glasscrane.flannery.gif

import java.io.OutputStream

/**
 * Minimal GIF89a writer: one global colour table, one full-size frame per image,
 * NETSCAPE2.0 block so it loops forever.
 */
class GifWriter(
    private val out: OutputStream,
    private val width: Int,
    private val height: Int,
    private val palette: IntArray
) {

    fun start() {
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShort(width)
        writeShort(height)
        // global colour table present, 8-bit colour resolution, 256 entries
        out.write(0xF7)
        out.write(0)   // background colour index
        out.write(0)   // pixel aspect ratio
        for (i in 0 until 256) {
            val c = if (i < palette.size) palette[i] else 0
            out.write((c shr 16) and 0xFF)
            out.write((c shr 8) and 0xFF)
            out.write(c and 0xFF)
        }
        // loop forever
        out.write(0x21); out.write(0xFF); out.write(0x0B)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(0x03); out.write(0x01)
        writeShort(0)
        out.write(0)
    }

    /** @param delayCs frame delay in hundredths of a second. */
    fun addFrame(indices: ByteArray, delayCs: Int) {
        out.write(0x21); out.write(0xF9); out.write(0x04)
        out.write(0x04)          // disposal "leave in place", no transparency
        writeShort(delayCs)
        out.write(0)             // transparent colour index (unused)
        out.write(0)

        out.write(0x2C)
        writeShort(0); writeShort(0)
        writeShort(width); writeShort(height)
        out.write(0)             // no local table, not interlaced

        lzwEncode(indices)
    }

    fun finish() {
        out.write(0x3B)
        out.flush()
    }

    private fun writeShort(v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
    }

    // ---- LZW ----

    private val block = ByteArray(255)
    private var blockLen = 0

    private fun blockByte(b: Int) {
        block[blockLen++] = b.toByte()
        if (blockLen == 255) flushBlock()
    }

    private fun flushBlock() {
        if (blockLen > 0) {
            out.write(blockLen)
            out.write(block, 0, blockLen)
            blockLen = 0
        }
    }

    private fun lzwEncode(pixels: ByteArray) {
        val minCodeSize = 8
        out.write(minCodeSize)

        val clearCode = 1 shl minCodeSize      // 256
        val eoi = clearCode + 1                // 257
        var codeSize = minCodeSize + 1         // 9
        var nextCode = eoi + 1                 // 258
        val dict = HashMap<Int, Int>(6144)

        var bitBuf = 0
        var bitCnt = 0

        fun emit(code: Int) {
            bitBuf = bitBuf or (code shl bitCnt)
            bitCnt += codeSize
            while (bitCnt >= 8) {
                blockByte(bitBuf and 0xFF)
                bitBuf = bitBuf ushr 8
                bitCnt -= 8
            }
        }

        emit(clearCode)

        if (pixels.isNotEmpty()) {
            var prefix = pixels[0].toInt() and 0xFF
            for (i in 1 until pixels.size) {
                val k = pixels[i].toInt() and 0xFF
                val key = (prefix shl 8) or k
                val found = dict[key]
                if (found != null) {
                    prefix = found
                    continue
                }
                emit(prefix)
                if (nextCode < 4096) {
                    dict[key] = nextCode
                    nextCode++
                    if (nextCode == (1 shl codeSize) && codeSize < 12) codeSize++
                } else {
                    emit(clearCode)
                    dict.clear()
                    codeSize = minCodeSize + 1
                    nextCode = eoi + 1
                }
                prefix = k
            }
            emit(prefix)
        }

        emit(eoi)
        if (bitCnt > 0) blockByte(bitBuf and 0xFF)
        flushBlock()
        out.write(0)   // end of image data
    }
}

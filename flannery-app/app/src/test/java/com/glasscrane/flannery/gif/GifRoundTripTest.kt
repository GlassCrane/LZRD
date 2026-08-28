package com.glasscrane.flannery.gif

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Encodes real frames and decodes them again with an independent decoder.
 *
 * A GIF whose container is well-formed can still carry a corrupt LZW payload —
 * that shipped once, when the encoder widened its code one step before the
 * decoder did and every code past the 512th came back garbage. Checking frame
 * counts or file headers does not catch it; only decoding the pixels does.
 */
class GifRoundTripTest {

    private val width = 96
    private val height = 96

    /** Noise uses far more dictionary entries than flat art, forcing code-width growth and clears. */
    private fun noiseFrame(seed: Int): ByteArray {
        val out = ByteArray(width * height)
        var x = seed * 2654435761L.toInt() + 12345
        for (i in out.indices) {
            x = x * 1103515245 + 12345
            out[i] = (((x ushr 16) and 0xFF) % 255).toByte()   // 0..254, 255 stays transparent
        }
        return out
    }

    private fun gradientFrame(shift: Int): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            out[y * width + x] = (((x + y + shift) / 2) % 255).toByte()
        }
        return out
    }

    private fun palette() = IntArray(256) { (it shl 16) or (it shl 8) or it }

    private fun encode(frames: List<ByteArray>, transparent: Int = -1): ByteArray {
        val bos = ByteArrayOutputStream()
        val gif = GifWriter(bos, width, height, palette())
        gif.start()
        frames.forEachIndexed { i, f ->
            gif.addFrame(f, 5, if (i == 0) -1 else transparent)
        }
        gif.finish()
        return bos.toByteArray()
    }

    @Test
    fun `flat frames survive the round trip`() {
        val frames = listOf(gradientFrame(0), gradientFrame(7), gradientFrame(19))
        val decoded = decodeGif(encode(frames))
        assertEquals(frames.size, decoded.size)
        frames.forEachIndexed { i, f -> assertArrayEquals("frame $i", f, decoded[i]) }
    }

    /** The case the shipped bug broke: enough distinct codes to pass 512 and 4096. */
    @Test
    fun `high entropy frames survive the round trip`() {
        val frames = listOf(noiseFrame(1), noiseFrame(2), noiseFrame(3))
        val decoded = decodeGif(encode(frames))
        assertEquals(frames.size, decoded.size)
        frames.forEachIndexed { i, f -> assertArrayEquals("frame $i", f, decoded[i]) }
    }

    @Test
    fun `a single pixel value still encodes`() {
        val flat = ByteArray(width * height) { 42 }
        val decoded = decodeGif(encode(listOf(flat)))
        assertArrayEquals(flat, decoded[0])
    }

    @Test
    fun `transparent pixels are preserved as their own index`() {
        val base = gradientFrame(0)
        val diffed = base.copyOf().also { for (i in it.indices step 3) it[i] = -1 }  // 255
        val decoded = decodeGif(encode(listOf(base, diffed), transparent = 255))
        assertArrayEquals(diffed, decoded[1])
    }

    @Test
    fun `stream is a valid gif89a that loops`() {
        val bytes = encode(listOf(gradientFrame(0), gradientFrame(3)))
        assertEquals("GIF89a", String(bytes, 0, 6, Charsets.US_ASCII))
        assertEquals(0x3B.toByte(), bytes.last())
        assertTrue("missing NETSCAPE loop block",
            String(bytes, Charsets.ISO_8859_1).contains("NETSCAPE2.0"))
    }

    // ---- an independent GIF reader, written to the spec rather than to the encoder ----

    /** @return each frame's palette indices, exactly as a decoder would reconstruct them. */
    private fun decodeGif(bytes: ByteArray): List<ByteArray> {
        val src = ByteArrayInputStream(bytes)
        fun u8() = src.read()
        fun u16() = u8() or (u8() shl 8)

        val header = ByteArray(6).also { src.read(it) }
        assertEquals("GIF89a", String(header, Charsets.US_ASCII))
        u16(); u16()                       // logical screen size
        val packed = u8()
        u8(); u8()                         // background index, aspect ratio
        if (packed and 0x80 != 0) src.skip((3 * (1 shl ((packed and 7) + 1))).toLong())

        fun readSubBlocks(): ByteArray {
            val out = ByteArrayOutputStream()
            while (true) {
                val n = u8()
                if (n <= 0) return out.toByteArray()
                val buf = ByteArray(n)
                var read = 0
                while (read < n) read += src.read(buf, read, n - read)
                out.write(buf)
            }
        }

        val frames = mutableListOf<ByteArray>()
        loop@ while (true) {
            when (u8()) {
                0x21 -> { u8(); readSubBlocks() }        // extension: label then data
                0x2C -> {                                // image descriptor
                    u16(); u16(); u16(); u16()
                    val lp = u8()
                    if (lp and 0x80 != 0) src.skip((3 * (1 shl ((lp and 7) + 1))).toLong())
                    val minCodeSize = u8()
                    frames += inflate(readSubBlocks(), minCodeSize)
                }
                0x3B, -1 -> break@loop                   // trailer
            }
        }
        return frames
    }

    private fun inflate(data: ByteArray, minCodeSize: Int): ByteArray {
        val clear = 1 shl minCodeSize
        val eoi = clear + 1
        val prefix = IntArray(4096)
        val suffix = IntArray(4096)
        for (i in 0 until clear) { prefix[i] = -1; suffix[i] = i }

        var codeSize = minCodeSize + 1
        var next = eoi + 1
        var bit = 0
        val out = ByteArrayOutputStream()
        val stack = IntArray(4096)
        var old = -1

        fun read(): Int {
            var v = 0
            for (b in 0 until codeSize) {
                val idx = (bit + b) ushr 3
                if (idx >= data.size) return eoi
                v = v or ((((data[idx].toInt() ushr ((bit + b) and 7)) and 1)) shl b)
            }
            bit += codeSize
            return v
        }
        fun firstOf(code: Int): Int {
            var c = code
            while (prefix[c] >= 0) c = prefix[c]
            return suffix[c]
        }

        while (true) {
            val code = read()
            if (code == eoi) break
            if (code == clear) {
                codeSize = minCodeSize + 1; next = eoi + 1; old = -1
                continue
            }
            var sp = 0
            var c = code
            if (code >= next) {                 // the KwKwK case
                if (old < 0) break
                stack[sp++] = firstOf(old)
                c = old
            }
            while (c >= clear) { stack[sp++] = suffix[c]; c = prefix[c] }
            val first = suffix[c]
            stack[sp++] = first
            while (sp > 0) out.write(stack[--sp])

            if (old >= 0 && next < 4096) {
                prefix[next] = old
                suffix[next] = first
                next++
                // decoder widens as soon as its own table fills — the encoder,
                // being one entry ahead, must wait a code longer
                if (next == (1 shl codeSize) && codeSize < 12) codeSize++
            }
            old = code
        }
        return out.toByteArray()
    }
}

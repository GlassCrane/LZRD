package com.glasscrane.flannery.gif

/**
 * Median-cut colour quantiser over a 5-5-5 histogram.
 *
 * Frames are fed in first, then [build] picks up to 256 representative colours.
 * Because the lookup only ever needs to answer for colours that actually occurred,
 * every populated histogram cell is assigned directly to the box that owns it —
 * no nearest-neighbour search is required.
 */
class Quantizer(private val maxColors: Int = 255) {

    private val hist = IntArray(1 shl 15)

    /** Palette as 0xRRGGBB, valid after [build]. */
    var palette = IntArray(0)
        private set

    /** 5-5-5 cell -> palette index, valid after [build]. */
    private var lookup = ByteArray(0)

    fun addFrame(pixels: IntArray) {
        for (px in pixels) {
            val r = (px ushr 19) and 31
            val g = (px ushr 11) and 31
            val b = (px ushr 3) and 31
            hist[(r shl 10) or (g shl 5) or b]++
        }
    }

    fun build() {
        var populated = 0
        for (c in hist) if (c > 0) populated++
        if (populated == 0) {
            palette = IntArray(256)
            lookup = ByteArray(1 shl 15)
            return
        }

        val cells = IntArray(populated)
        var n = 0
        for (i in hist.indices) if (hist[i] > 0) cells[n++] = i

        // Boxes are half-open ranges over `cells`.
        val loArr = IntArray(maxColors)
        val hiArr = IntArray(maxColors)
        loArr[0] = 0; hiArr[0] = populated
        var boxCount = 1

        while (boxCount < maxColors) {
            var best = -1
            var bestScore = 0
            for (i in 0 until boxCount) {
                if (hiArr[i] - loArr[i] < 2) continue
                val score = extent(cells, loArr[i], hiArr[i])
                if (score > bestScore) { bestScore = score; best = i }
            }
            if (best < 0 || bestScore == 0) break

            val lo = loArr[best]
            val hi = hiArr[best]
            sortRange(cells, lo, hi, widestAxis(cells, lo, hi))

            var total = 0
            for (i in lo until hi) total += hist[cells[i]]
            var acc = 0
            var split = lo + 1
            for (i in lo until hi - 1) {
                acc += hist[cells[i]]
                if (acc * 2 >= total) { split = i + 1; break }
                split = i + 1
            }
            if (split <= lo) split = lo + 1
            if (split >= hi) split = hi - 1

            hiArr[best] = split
            loArr[boxCount] = split
            hiArr[boxCount] = hi
            boxCount++
        }

        palette = IntArray(256)
        lookup = ByteArray(1 shl 15)
        for (i in 0 until boxCount) {
            var wr = 0L; var wg = 0L; var wb = 0L; var wt = 0L
            for (j in loArr[i] until hiArr[i]) {
                val cell = cells[j]
                val w = hist[cell].toLong()
                wr += (((cell ushr 10) and 31).toLong()) * w
                wg += (((cell ushr 5) and 31).toLong()) * w
                wb += ((cell and 31).toLong()) * w
                wt += w
                lookup[cell] = i.toByte()
            }
            if (wt == 0L) continue
            val r5 = (wr / wt).toInt()
            val g5 = (wg / wt).toInt()
            val b5 = (wb / wt).toInt()
            palette[i] = (expand(r5) shl 16) or (expand(g5) shl 8) or expand(b5)
        }
    }

    /** Map a frame's ARGB pixels onto palette indices. */
    fun map(pixels: IntArray, out: ByteArray) {
        for (i in pixels.indices) {
            val px = pixels[i]
            val r = (px ushr 19) and 31
            val g = (px ushr 11) and 31
            val b = (px ushr 3) and 31
            out[i] = lookup[(r shl 10) or (g shl 5) or b]
        }
    }

    /** 5 bits -> 8 bits, keeping white at 255. */
    private fun expand(v: Int) = (v shl 3) or (v ushr 2)

    private fun widestAxis(cells: IntArray, lo: Int, hi: Int): Int {
        var rMin = 31; var rMax = 0; var gMin = 31; var gMax = 0; var bMin = 31; var bMax = 0
        for (i in lo until hi) {
            val c = cells[i]
            val r = (c ushr 10) and 31
            val g = (c ushr 5) and 31
            val b = c and 31
            if (r < rMin) rMin = r; if (r > rMax) rMax = r
            if (g < gMin) gMin = g; if (g > gMax) gMax = g
            if (b < bMin) bMin = b; if (b > bMax) bMax = b
        }
        val dr = rMax - rMin
        val dg = gMax - gMin
        val db = bMax - bMin
        // green weighted up slightly: the eye reads it hardest
        return if (dg * 5 >= dr * 4 && dg * 5 >= db * 4) 1 else if (dr >= db) 0 else 2
    }

    private fun extent(cells: IntArray, lo: Int, hi: Int): Int {
        var rMin = 31; var rMax = 0; var gMin = 31; var gMax = 0; var bMin = 31; var bMax = 0
        for (i in lo until hi) {
            val c = cells[i]
            val r = (c ushr 10) and 31
            val g = (c ushr 5) and 31
            val b = c and 31
            if (r < rMin) rMin = r; if (r > rMax) rMax = r
            if (g < gMin) gMin = g; if (g > gMax) gMax = g
            if (b < bMin) bMin = b; if (b > bMax) bMax = b
        }
        return maxOf(rMax - rMin, gMax - gMin, bMax - bMin)
    }

    private fun sortRange(cells: IntArray, lo: Int, hi: Int, axis: Int) {
        val n = hi - lo
        val keyed = IntArray(n)
        for (i in 0 until n) {
            val c = cells[lo + i]
            val k = when (axis) {
                0 -> (c ushr 10) and 31
                1 -> (c ushr 5) and 31
                else -> c and 31
            }
            keyed[i] = (k shl 16) or i
        }
        java.util.Arrays.sort(keyed)
        val snapshot = IntArray(n)
        System.arraycopy(cells, lo, snapshot, 0, n)
        for (i in 0 until n) cells[lo + i] = snapshot[keyed[i] and 0xFFFF]
    }
}

package com.glasscrane.flannery.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import com.glasscrane.flannery.anim.AnimSpec
import com.glasscrane.flannery.anim.Renderer
import com.glasscrane.flannery.art.Stage
import com.glasscrane.flannery.gif.GifWriter
import com.glasscrane.flannery.gif.Quantizer
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Renders an animation to a looping GIF with a transparent background.
 *
 * GIF alpha is 1-bit: a pixel is either fully opaque or fully clear. Every
 * pixel is therefore thresholded, and the soft touches that need real alpha
 * (ground shadow, pool of light) are skipped upstream rather than clipped into
 * hard shapes here.
 *
 * Because the background is transparent, frames cannot be differenced against
 * each other — the transparent index is spent on "background", and each frame
 * must clear the last one (disposal 2) or the subject smears. The size that
 * would otherwise cost is won back by writing only each frame's dirty
 * rectangle instead of the whole canvas.
 */
object GifExporter {

    const val SIZE = 360

    /** Below this alpha a pixel is background. */
    private const val ALPHA_CUT = 128

    /** Palette slot reserved for transparency. */
    private const val TRANSPARENT = 255

    fun encode(spec: AnimSpec, dest: File, onProgress: (Float) -> Unit) {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bmp)
            val stage = Stage(canvas, SIZE.toFloat(), SIZE.toFloat())
            val px = IntArray(SIZE * SIZE)
            val quantizer = Quantizer()

            fun drawFrame(f: Int) {
                bmp.eraseColor(0)
                canvas.drawColor(0, PorterDuff.Mode.CLEAR)
                Renderer.render(stage, spec, f.toFloat() / spec.frames)
                bmp.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
            }

            for (f in 0 until spec.frames) {
                drawFrame(f)
                quantizer.addFrame(px, ALPHA_CUT)
                onProgress(0.45f * (f + 1) / spec.frames)
            }
            quantizer.build()
            onProgress(0.5f)

            val full = ByteArray(SIZE * SIZE)
            val delayCs = max(2, (100f / spec.fps).roundToInt())
            dest.parentFile?.mkdirs()

            BufferedOutputStream(FileOutputStream(dest), 1 shl 16).use { out ->
                val gif = GifWriter(out, SIZE, SIZE, quantizer.palette, TRANSPARENT)
                gif.start()
                for (f in 0 until spec.frames) {
                    drawFrame(f)
                    quantizer.map(px, full)

                    // knock out background and find what the frame actually covers
                    var minX = SIZE; var minY = SIZE; var maxX = -1; var maxY = -1
                    var i = 0
                    for (y in 0 until SIZE) {
                        for (x in 0 until SIZE) {
                            if ((px[i] ushr 24) and 0xFF < ALPHA_CUT) {
                                full[i] = TRANSPARENT.toByte()
                            } else {
                                if (x < minX) minX = x
                                if (x > maxX) maxX = x
                                if (y < minY) minY = y
                                if (y > maxY) maxY = y
                            }
                            i++
                        }
                    }
                    if (maxX < 0) { minX = 0; minY = 0; maxX = 0; maxY = 0 }

                    val fw = maxX - minX + 1
                    val fh = maxY - minY + 1
                    val sub = ByteArray(fw * fh)
                    for (y in 0 until fh) {
                        System.arraycopy(full, (minY + y) * SIZE + minX, sub, y * fw, fw)
                    }
                    gif.addFrame(sub, delayCs, TRANSPARENT, disposal = 2,
                        x = minX, y = minY, w = fw, h = fh)
                    onProgress(0.5f + 0.5f * (f + 1) / spec.frames)
                }
                gif.finish()
            }
        } finally {
            bmp.recycle()
        }
    }
}

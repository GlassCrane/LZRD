package com.glasscrane.flannery.share

import android.graphics.Bitmap
import android.graphics.Canvas
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
 * Renders an animation to a looping GIF.
 *
 * Frames are drawn twice — once to gather the colour histogram, once to encode —
 * so only a single bitmap is ever in memory. Drawing is pure vector work and cheap.
 */
object GifExporter {

    const val SIZE = 420

    fun encode(spec: AnimSpec, dest: File, onProgress: (Float) -> Unit) {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bmp)
            val stage = Stage(canvas, SIZE.toFloat(), SIZE.toFloat())
            val px = IntArray(SIZE * SIZE)
            val quantizer = Quantizer()

            for (f in 0 until spec.frames) {
                Renderer.render(stage, spec, f.toFloat() / spec.frames)
                bmp.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
                quantizer.addFrame(px)
                onProgress(0.45f * (f + 1) / spec.frames)
            }
            quantizer.build()
            onProgress(0.5f)

            val indices = ByteArray(SIZE * SIZE)
            val delayCs = max(2, (100f / spec.fps).roundToInt())
            dest.parentFile?.mkdirs()

            BufferedOutputStream(FileOutputStream(dest), 1 shl 16).use { out ->
                val gif = GifWriter(out, SIZE, SIZE, quantizer.palette)
                gif.start()
                for (f in 0 until spec.frames) {
                    Renderer.render(stage, spec, f.toFloat() / spec.frames)
                    bmp.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
                    quantizer.map(px, indices)
                    gif.addFrame(indices, delayCs)
                    onProgress(0.5f + 0.5f * (f + 1) / spec.frames)
                }
                gif.finish()
            }
        } finally {
            bmp.recycle()
        }
    }
}

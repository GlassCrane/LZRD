package com.glasscrane.flannery.ui

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.glasscrane.flannery.anim.AnimSpec
import com.glasscrane.flannery.anim.Renderer
import com.glasscrane.flannery.art.Stage

/** Plays one animation on loop, stepped to the same frames the GIF will contain. */
class FlanneryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var spec: AnimSpec? = null
    private var stage: Stage? = null
    private var startedAt = 0L

    fun setAnimation(spec: AnimSpec) {
        this.spec = spec
        startedAt = SystemClock.uptimeMillis()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val s = spec ?: return
        if (width == 0 || height == 0) return

        val st = stage?.also {
            it.canvas = canvas
            it.w = width.toFloat()
            it.h = height.toFloat()
        } ?: Stage(canvas, width.toFloat(), height.toFloat()).also { stage = it }

        if (startedAt == 0L) startedAt = SystemClock.uptimeMillis()
        val elapsed = SystemClock.uptimeMillis() - startedAt
        val frameMs = 1000L / s.fps
        val frame = ((elapsed / frameMs) % s.frames).toInt()

        Renderer.render(st, s, frame.toFloat() / s.frames)
        postInvalidateOnAnimation()
    }
}

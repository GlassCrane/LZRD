package com.glasscrane.flannery.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * A targeting-reticle frame: hairline border, mint corner brackets, and small
 * mid-edge ticks. Drawn in code so the brackets keep their size at any bounds.
 */
class HudFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private val hairline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * d
        color = 0xFF22302F.toInt()
    }
    private val bracket = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
        strokeCap = Paint.Cap.SQUARE
        color = 0xFFABD2C3.toInt()
    }

    var bracketLen = 13f * d

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val inset = 1f * d
        val b = bracketLen

        canvas.drawRect(inset, inset, w - inset, h - inset, hairline)

        val o = 1f * d   // brackets sit on the border
        // corners: two strokes each
        canvas.drawLine(o, o, o + b, o, bracket);          canvas.drawLine(o, o, o, o + b, bracket)
        canvas.drawLine(w - o - b, o, w - o, o, bracket);  canvas.drawLine(w - o, o, w - o, o + b, bracket)
        canvas.drawLine(o, h - o, o + b, h - o, bracket);  canvas.drawLine(o, h - o - b, o, h - o, bracket)
        canvas.drawLine(w - o - b, h - o, w - o, h - o, bracket)
        canvas.drawLine(w - o, h - o - b, w - o, h - o, bracket)

        // mid-edge ticks
        val t = 4f * d
        canvas.drawLine(w / 2 - t, o, w / 2 + t, o, bracket)
        canvas.drawLine(w / 2 - t, h - o, w / 2 + t, h - o, bracket)
        canvas.drawLine(o, h / 2 - t, o, h / 2 + t, bracket)
        canvas.drawLine(w - o, h / 2 - t, w - o, h / 2 + t, bracket)
    }
}

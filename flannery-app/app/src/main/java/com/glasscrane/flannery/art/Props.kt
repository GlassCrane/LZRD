package com.glasscrane.flannery.art

import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

/** Little things that orbit him: hearts, sparkles, snow, snacks. */
object Props {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    fun heart(st: Stage, x: Float, y: Float, r: Float, color: Int) {
        val p = st.path
        p.reset()
        p.moveTo(x, y + r * 0.78f)
        p.cubicTo(x - r * 1.25f, y - r * 0.10f, x - r * 0.62f, y - r * 1.02f, x, y - r * 0.32f)
        p.cubicTo(x + r * 0.62f, y - r * 1.02f, x + r * 1.25f, y - r * 0.10f, x, y + r * 0.78f)
        p.close()
        st.canvas.drawPath(p, st.fill(color))
    }

    fun star(st: Stage, x: Float, y: Float, r: Float, color: Int, points: Int = 5) {
        val p = st.path
        p.reset()
        for (i in 0 until points * 2) {
            val rad = if (i % 2 == 0) r else r * 0.44f
            val a = -1.5707964f + i * (TAU / (points * 2))
            val px = x + cos(a) * rad
            val py = y + sin(a) * rad
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        p.close()
        st.canvas.drawPath(p, st.fill(color))
    }

    /** Four-armed twinkle. */
    fun sparkle(st: Stage, x: Float, y: Float, r: Float, color: Int) {
        val p = st.path
        p.reset()
        p.moveTo(x, y - r)
        p.quadTo(x + r * 0.16f, y - r * 0.16f, x + r, y)
        p.quadTo(x + r * 0.16f, y + r * 0.16f, x, y + r)
        p.quadTo(x - r * 0.16f, y + r * 0.16f, x - r, y)
        p.quadTo(x - r * 0.16f, y - r * 0.16f, x, y - r)
        p.close()
        st.canvas.drawPath(p, st.fill(color))
    }

    fun note(st: Stage, x: Float, y: Float, r: Float, color: Int) {
        st.oval(x - r * 0.30f, y + r * 0.55f, r * 0.42f, r * 0.32f, color)
        val pt = st.stroke(color, r * 0.20f)
        st.canvas.drawLine(x + r * 0.10f, y + r * 0.55f, x + r * 0.10f, y - r * 0.85f, pt)
        st.path.reset()
        st.path.moveTo(x + r * 0.10f, y - r * 0.85f)
        st.path.quadTo(x + r * 0.85f, y - r * 0.60f, x + r * 0.62f, y - r * 0.10f)
        st.canvas.drawPath(st.path, st.stroke(color, r * 0.20f))
    }

    fun snowflake(st: Stage, x: Float, y: Float, r: Float, color: Int) {
        val pt = st.stroke(color, r * 0.22f)
        for (i in 0 until 3) {
            val a = i * (3.14159265f / 3f)
            st.canvas.drawLine(x - cos(a) * r, y - sin(a) * r, x + cos(a) * r, y + sin(a) * r, pt)
        }
    }

    fun bubble(st: Stage, x: Float, y: Float, r: Float, alpha: Float) {
        st.circle(x, y, r, withAlpha(0xFFBEEBFF.toInt(), 0.38f * alpha))
        val pt = st.stroke(withAlpha(Hue.WHITE, 0.85f * alpha), r * 0.14f)
        st.canvas.drawCircle(x, y, r, pt)
        st.circle(x - r * 0.34f, y - r * 0.36f, r * 0.18f, withAlpha(Hue.WHITE, 0.9f * alpha))
    }

    fun cloud(st: Stage, x: Float, y: Float, r: Float, color: Int) {
        st.circle(x - r * 0.62f, y + r * 0.10f, r * 0.52f, color)
        st.circle(x + r * 0.62f, y + r * 0.12f, r * 0.46f, color)
        st.circle(x - r * 0.06f, y - r * 0.22f, r * 0.68f, color)
        st.rect.set(x - r * 0.75f, y - r * 0.05f, x + r * 0.75f, y + r * 0.58f)
        st.canvas.drawRoundRect(st.rect, r * 0.30f, r * 0.30f, st.fill(color))
    }

    fun cookie(st: Stage, x: Float, y: Float, r: Float, bitten: Float) {
        st.circle(x, y, r, 0xFFD9A461.toInt())
        st.circle(x, y, r * 0.86f, 0xFFE8BB7C.toInt())
        for (i in 0 until 6) {
            val a = rnd(i * 7) * TAU
            val d = r * (0.20f + 0.50f * rnd(i * 7 + 3))
            st.circle(x + cos(a) * d, y + sin(a) * d, r * 0.14f, 0xFF6B4423.toInt())
        }
        // bite taken out of the top-right
        if (bitten > 0.01f) {
            st.circle(x + r * 0.86f, y - r * 0.50f, r * 0.62f * bitten, Hue.CREAM)
        }
    }

    fun flame(st: Stage, x: Float, y: Float, r: Float, alpha: Float) {
        val p = st.path
        p.reset()
        p.moveTo(x, y - r)
        p.quadTo(x + r * 0.72f, y - r * 0.20f, x + r * 0.34f, y + r * 0.52f)
        p.quadTo(x, y + r * 0.86f, x - r * 0.34f, y + r * 0.52f)
        p.quadTo(x - r * 0.72f, y - r * 0.20f, x, y - r)
        p.close()
        st.canvas.drawPath(p, st.fill(withAlpha(0xFFFF8A3D.toInt(), alpha)))
        p.reset()
        p.moveTo(x, y - r * 0.52f)
        p.quadTo(x + r * 0.36f, y - r * 0.05f, x + r * 0.16f, y + r * 0.44f)
        p.quadTo(x, y + r * 0.64f, x - r * 0.16f, y + r * 0.44f)
        p.quadTo(x - r * 0.36f, y - r * 0.05f, x, y - r * 0.52f)
        p.close()
        st.canvas.drawPath(p, st.fill(withAlpha(Hue.GOLD, alpha)))
    }

    fun confetti(st: Stage, x: Float, y: Float, r: Float, rotDeg: Float, color: Int) {
        val c = st.canvas
        c.save()
        c.translate(x, y)
        c.rotate(rotDeg)
        st.rect.set(-r * 0.5f, -r * 0.28f, r * 0.5f, r * 0.28f)
        c.drawRoundRect(st.rect, r * 0.12f, r * 0.12f, st.fill(color))
        c.restore()
    }

    fun partyHat(st: Stage, x: Float, y: Float, r: Float) {
        val p = st.path
        p.reset()
        p.moveTo(x, y - r * 1.55f)
        p.lineTo(x + r * 0.62f, y + r * 0.30f)
        p.lineTo(x - r * 0.62f, y + r * 0.30f)
        p.close()
        st.canvas.drawPath(p, st.fill(Hue.PINK))
        for (i in 0 until 3) {
            val f = (i + 1) / 4f
            st.circle(x - r * 0.24f + r * 0.30f * i, y - r * 1.10f + r * 1.30f * f,
                r * 0.13f, if (i % 2 == 0) Hue.GOLD else Hue.WHITE)
        }
        st.circle(x, y - r * 1.62f, r * 0.24f, Hue.GOLD)
    }

    /** A knit beanie — sits far better on a round, neckless plush than a scarf. */
    fun beanie(st: Stage, hw: Float, hh: Float) {
        val red = 0xFFD9576B.toInt()
        val redDeep = 0xFFBE4457.toInt()
        st.oval(0f, -hh * 0.94f, hw * 0.58f, hh * 0.30f, red)
        st.rect.set(-hw * 0.64f, -hh * 0.84f, hw * 0.64f, -hh * 0.62f)
        st.canvas.drawRoundRect(st.rect, hh * 0.11f, hh * 0.11f, st.fill(redDeep))
        // knit ribbing
        val pt = st.stroke(withAlpha(Hue.CREAM, 0.35f), hw * 0.018f)
        for (i in 0 until 7) {
            val x = -hw * 0.52f + hw * 0.173f * i
            st.canvas.drawLine(x, -hh * 0.82f, x, -hh * 0.64f, pt)
        }
        st.circle(0f, -hh * 1.24f, hw * 0.135f, Hue.CREAM)
    }

    fun rainbow(st: Stage, cx: Float, cy: Float, r: Float, sweep: Float, alpha: Float) {
        val bands = intArrayOf(
            0xFFFF7B7B.toInt(), 0xFFFFB86B.toInt(), 0xFFFFE066.toInt(),
            0xFF8BE08B.toInt(), 0xFF7BC4FF.toInt(), 0xFFC79BFF.toInt()
        )
        val band = r * 0.11f
        for (i in bands.indices) {
            val rr = r - i * band
            val pt = st.stroke(withAlpha(bands[i], alpha), band * 0.96f)
            pt.strokeCap = Paint.Cap.BUTT
            st.rect.set(cx - rr, cy - rr, cx + rr, cy + rr)
            st.canvas.drawArc(st.rect, 180f, 180f * clamp01(sweep), false, pt)
        }
    }

    fun zzz(st: Stage, x: Float, y: Float, size: Float, alpha: Float) {
        textPaint.textSize = size
        textPaint.color = withAlpha(Hue.INK, alpha)
        st.canvas.drawText("Z", x, y, textPaint)
    }

    fun crumb(st: Stage, x: Float, y: Float, r: Float, alpha: Float) {
        st.circle(x, y, r, withAlpha(0xFF8C5A2B.toInt(), alpha))
    }

    /** Soft radial glow used behind sparkle-y moments. */
    fun glow(st: Stage, x: Float, y: Float, r: Float, color: Int, alpha: Float) {
        var i = 6
        while (i >= 1) {
            st.circle(x, y, r * (i / 6f), withAlpha(color, alpha * 0.10f))
            i--
        }
    }

    /** A dashed motion arc, for spins and rolls. */
    fun swoosh(st: Stage, x: Float, y: Float, r: Float, from: Float, sweep: Float, alpha: Float) {
        val pt = st.stroke(withAlpha(Hue.MINT_DEEP, alpha), r * 0.09f)
        st.rect.set(x - r, y - r, x + r, y + r)
        st.canvas.drawArc(st.rect, from, sweep, false, pt)
    }

}

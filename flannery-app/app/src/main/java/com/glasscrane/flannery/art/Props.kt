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
        st.oval(0f, -hh * 0.78f, hw * 0.50f, hh * 0.26f, red)
        st.rect.set(-hw * 0.56f, -hh * 0.70f, hw * 0.56f, -hh * 0.50f)
        st.canvas.drawRoundRect(st.rect, hh * 0.11f, hh * 0.11f, st.fill(redDeep))
        // knit ribbing
        val pt = st.stroke(withAlpha(Hue.CREAM, 0.35f), hw * 0.018f)
        for (i in 0 until 7) {
            val x = -hw * 0.45f + hw * 0.150f * i
            st.canvas.drawLine(x, -hh * 0.68f, x, -hh * 0.52f, pt)
        }
        st.circle(0f, -hh * 1.04f, hw * 0.115f, Hue.CREAM)
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

    /** Outlined so it reads on light and dark grounds alike. */
    fun zzz(st: Stage, x: Float, y: Float, size: Float, alpha: Float) {
        textPaint.textSize = size
        val ring = size * 0.09f
        textPaint.color = withAlpha(Hue.CREAM, alpha)
        var i = 0
        while (i < 8) {
            val a = i / 8f * TAU
            st.canvas.drawText("Z", x + cos(a) * ring, y + sin(a) * ring, textPaint)
            i++
        }
        textPaint.color = withAlpha(Hue.INK, alpha)
        st.canvas.drawText("Z", x, y, textPaint)
    }

    fun crumb(st: Stage, x: Float, y: Float, r: Float, alpha: Float) {
        st.circle(x, y, r, withAlpha(0xFF8C5A2B.toInt(), alpha))
    }

    /** A dashed motion arc, for spins and rolls. */
    fun swoosh(st: Stage, x: Float, y: Float, r: Float, from: Float, sweep: Float, alpha: Float) {
        val pt = st.stroke(withAlpha(Hue.MINT_DEEP, alpha), r * 0.09f)
        st.rect.set(x - r, y - r, x + r, y + r)
        st.canvas.drawArc(st.rect, from, sweep, false, pt)
    }


    /** Bold shout text with a thick outline, drawn by stamping offsets so it
     *  renders the same everywhere rather than relying on stroked text. */
    fun shout(st: Stage, x: Float, y: Float, size: Float, text: String, fill: Int, outline: Int, alpha: Float) {
        textPaint.textSize = size
        val ring = size * 0.075f
        textPaint.color = withAlpha(outline, alpha)
        var i = 0
        while (i < 12) {
            val a = i / 12f * TAU
            st.canvas.drawText(text, x + cos(a) * ring, y + sin(a) * ring, textPaint)
            i++
        }
        textPaint.color = withAlpha(fill, alpha)
        st.canvas.drawText(text, x, y, textPaint)
    }

    /** Rocket exhaust: a tapering plume with a bright core. */
    fun flameJet(st: Stage, x: Float, y: Float, w: Float, len: Float, seed: Int, alpha: Float) {
        if (len <= 0f) return
        val layers = arrayOf(
            Triple(1.00f, 0xFFFF7A2F.toInt(), 0.55f),
            Triple(0.66f, 0xFFFFC24D.toInt(), 0.85f),
            Triple(0.34f, 0xFFFFF3C4.toInt(), 1.00f)
        )
        for ((scale, color, a) in layers) {
            val ww = w * scale
            val ll = len * (0.72f + 0.28f * scale)
            val flick = 1f + 0.16f * sin(seed * 0.7f + scale * 9f)
            val p = st.path
            p.reset()
            p.moveTo(x - ww, y)
            p.quadTo(x - ww * 0.5f, y + ll * 0.55f, x, y + ll * flick)
            p.quadTo(x + ww * 0.5f, y + ll * 0.55f, x + ww, y)
            p.quadTo(x, y - ww * 0.30f, x - ww, y)
            p.close()
            st.canvas.drawPath(p, st.fill(withAlpha(color, a * alpha)))
        }
        // sparks trailing off
        for (i in 0 until 7) {
            val f = rnd(seed * 13 + i)
            val sy = y + len * (0.5f + 0.8f * f)
            val sx = x + w * 1.5f * (rnd(seed * 7 + i) - 0.5f)
            st.circle(sx, sy, w * 0.10f * (1f - f), withAlpha(Hue.GOLD, alpha * (1f - f)))
        }
    }

    fun speedLine(st: Stage, x: Float, y: Float, len: Float, thick: Float, alpha: Float) {
        val pt = st.stroke(withAlpha(Hue.MINT_DEEP, alpha * 0.8f), thick)
        st.canvas.drawLine(x, y, x + len, y, pt)
    }

    fun bolt(st: Stage, x: Float, y: Float, size: Float, alpha: Float) {
        val p = st.path
        p.reset()
        p.moveTo(x + size * 0.15f, y - size)
        p.lineTo(x - size * 0.35f, y + size * 0.12f)
        p.lineTo(x + size * 0.02f, y + size * 0.10f)
        p.lineTo(x - size * 0.18f, y + size)
        p.lineTo(x + size * 0.40f, y - size * 0.15f)
        p.lineTo(x + size * 0.02f, y - size * 0.12f)
        p.close()
        st.canvas.drawPath(p, st.fill(withAlpha(0xFFFFD64A.toInt(), alpha)))
    }

    fun sunglasses(st: Stage, hw: Float, cx: Float, cy: Float, lensR: Float) {
        val bar = st.stroke(0xFF23262A.toInt(), lensR * 0.34f)
        st.canvas.drawLine(cx - lensR * 2.4f, cy, cx + lensR * 2.4f, cy, bar)
        st.oval(cx - lensR * 1.55f, cy, lensR * 1.25f, lensR * 1.05f, 0xFF23262A.toInt())
        st.oval(cx + lensR * 1.55f, cy, lensR * 1.25f, lensR * 1.05f, 0xFF23262A.toInt())
        st.oval(cx - lensR * 1.9f, cy - lensR * 0.35f, lensR * 0.40f, lensR * 0.26f,
            withAlpha(Hue.WHITE, 0.55f))
        st.oval(cx + lensR * 1.2f, cy - lensR * 0.35f, lensR * 0.40f, lensR * 0.26f,
            withAlpha(Hue.WHITE, 0.55f))
    }

    fun cake(st: Stage, x: Float, y: Float, r: Float, flicker: Float) {
        st.rect.set(x - r, y - r * 0.35f, x + r, y + r * 0.65f)
        st.canvas.drawRoundRect(st.rect, r * 0.14f, r * 0.14f, st.fill(0xFFF6E3C8.toInt()))
        st.rect.set(x - r, y - r * 0.45f, x + r, y - r * 0.05f)
        st.canvas.drawRoundRect(st.rect, r * 0.14f, r * 0.14f, st.fill(0xFFF3A9BE.toInt()))
        st.rect.set(x - r * 0.09f, y - r * 1.10f, x + r * 0.09f, y - r * 0.40f)
        st.canvas.drawRoundRect(st.rect, r * 0.09f, r * 0.09f, st.fill(0xFFFFF6E0.toInt()))
        flame(st, x, y - r * 1.28f, r * 0.26f * (0.85f + 0.15f * flicker), 1f)
    }

    fun leaf(st: Stage, x: Float, y: Float, r: Float, rotDeg: Float, color: Int) {
        val c = st.canvas
        c.save()
        c.translate(x, y)
        c.rotate(rotDeg)
        val p = st.path
        p.reset()
        p.moveTo(-r, 0f)
        p.quadTo(0f, -r * 0.72f, r, 0f)
        p.quadTo(0f, r * 0.72f, -r, 0f)
        p.close()
        c.drawPath(p, st.fill(color))
        c.restore()
    }

    fun droplet(st: Stage, x: Float, y: Float, r: Float, color: Int) {
        val p = st.path
        p.reset()
        p.moveTo(x, y - r * 1.35f)
        p.quadTo(x + r, y - r * 0.10f, x, y + r * 0.90f)
        p.quadTo(x - r, y - r * 0.10f, x, y - r * 1.35f)
        p.close()
        st.canvas.drawPath(p, st.fill(color))
    }

    fun firework(st: Stage, x: Float, y: Float, r: Float, t: Float, color: Int) {
        val a = clamp01(1f - t)
        for (i in 0 until 14) {
            val ang = i / 14f * TAU
            val d = r * ease(t)
            st.circle(x + cos(ang) * d, y + sin(ang) * d, r * 0.055f * a, withAlpha(color, a))
            st.circle(x + cos(ang) * d * 0.72f, y + sin(ang) * d * 0.72f,
                r * 0.035f * a, withAlpha(Hue.WHITE, a * 0.8f))
        }
    }

    fun thoughtBubble(st: Stage, x: Float, y: Float, r: Float, dots: Int) {
        st.circle(x - r * 0.90f, y + r * 0.95f, r * 0.13f, withAlpha(Hue.WHITE, 0.95f))
        st.circle(x - r * 0.62f, y + r * 0.66f, r * 0.20f, withAlpha(Hue.WHITE, 0.95f))
        st.circle(x, y, r, withAlpha(Hue.WHITE, 0.95f))
        st.circle(x + r * 0.62f, y - r * 0.12f, r * 0.52f, withAlpha(Hue.WHITE, 0.95f))
        for (i in 0 until 3) {
            val on = i < dots
            st.circle(x - r * 0.42f + r * 0.42f * i, y, r * 0.13f,
                withAlpha(Hue.INK, if (on) 0.75f else 0.16f))
        }
    }

    fun splash(st: Stage, x: Float, y: Float, r: Float, t: Float) {
        val a = clamp01(1f - t)
        for (i in 0 until 9) {
            val ang = -2.7f + i * 0.32f
            val d = r * (0.30f + 1.15f * ease(t))
            droplet(st, x + cos(ang) * d, y + sin(ang) * d * 0.75f, r * 0.13f * a,
                withAlpha(0xFF8FC7E8.toInt(), a))
        }
        val pt = st.stroke(withAlpha(0xFF8FC7E8.toInt(), a * 0.8f), r * 0.07f)
        st.rect.set(x - r * (0.5f + t), y - r * 0.20f, x + r * (0.5f + t), y + r * 0.20f)
        st.canvas.drawOval(st.rect, pt)
    }

    fun sun(st: Stage, x: Float, y: Float, r: Float, spin: Float) {
        val pt = st.stroke(withAlpha(0xFFFFCB4D.toInt(), 0.85f), r * 0.10f)
        for (i in 0 until 10) {
            val a = spin + i / 10f * TAU
            st.canvas.drawLine(x + cos(a) * r * 1.25f, y + sin(a) * r * 1.25f,
                x + cos(a) * r * 1.60f, y + sin(a) * r * 1.60f, pt)
        }
        st.circle(x, y, r, 0xFFFFD75E.toInt())
    }
}

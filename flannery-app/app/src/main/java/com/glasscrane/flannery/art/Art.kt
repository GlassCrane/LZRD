package com.glasscrane.flannery.art

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

const val TAU = 6.2831855f
const val DEG = 0.017453292f

/** Deterministic pseudo-random in [0,1) — every frame must be reproducible. */
fun rnd(i: Int): Float {
    var x = i * 374761393 + 668265263
    x = (x xor (x shr 13)) * 1274126177
    x = x xor (x shr 16)
    return (x and 0x7FFFFFFF).toFloat() / 2147483647f
}

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
fun clamp01(t: Float) = if (t < 0f) 0f else if (t > 1f) 1f else t

/** Smooth 0->1->0 pulse over [0,1]. */
fun pulse(t: Float) = sin(clamp01(t) * 3.14159265f)

/** Ease in/out. */
fun ease(t: Float): Float {
    val x = clamp01(t)
    return x * x * (3f - 2f * x)
}

/** Position of t inside the window [from,to]; 0 outside. */
fun window(t: Float, from: Float, to: Float): Float {
    if (t < from || t > to) return 0f
    return (t - from) / (to - from)
}

fun withAlpha(color: Int, a: Float): Int {
    val al = (clamp01(a) * 255f).toInt()
    return (color and 0x00FFFFFF) or (al shl 24)
}

object Hue {
    // Sampled from the reference drawing.
    val BODY = 0xFFB0CE9C.toInt()
    val BODY_LIGHT = 0xFFC8DEB8.toInt()
    val BODY_DEEP = 0xFF97BA82.toInt()
    val LINE = 0xFF5E7F4F.toInt()
    val FIN = 0xFFA2C48D.toInt()
    val FIN_LINE = 0xFF55764A.toInt()
    val BELLY = 0xFFE9E0C4.toInt()
    val BELLY_SHADE = 0xFFDACFAE.toInt()
    val BELLY_LINE = 0xFFA99878.toInt()
    val INK = 0xFF37302A.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
    val PINK = 0xFFDE7C90.toInt()
    val GOLD = 0xFFEBBB52.toInt()
    val SHADOW = 0xFF97A78E.toInt()
    val CREAM = 0xFFFFF8F2.toInt()

    val MINT = BODY
    val MINT_DEEP = BODY_DEEP
    val MINT_LIGHT = BODY_LIGHT
    val FRILL = FIN
    val FRILL_EDGE = FIN_LINE
    val BLUSH = 0xFFDCA69E.toInt()
}

enum class Eyes { OPEN, BLINK, HAPPY, SLEEPY, WIDE, HEART, STAR, DIZZY }
enum class Mouth { SQUIGGLE, SMILE, OPEN, WIDE_OPEN, BLEP, FROWN, TINY, YAWN }

/** Everything an animation can say about how he looks this frame. */
class Pose {
    var cx = 0f
    var cy = 0f
    var size = 0f          // full body width in px
    var rot = 0f           // degrees
    var sx = 1f            // squash / stretch
    var sy = 1f
    var eyes = Eyes.OPEN
    var mouth = Mouth.SQUIGGLE
    var gazeX = 0f         // -1..1
    var gazeY = 0f
    var blush = 0f         // 0..1
    var shadow = 1f
    /** Extra art drawn inside his local transform (hats, scarves, held props). */
    var attach: ((Stage, Float, Float) -> Unit)? = null

    fun reset(cx: Float, cy: Float, size: Float) {
        this.cx = cx; this.cy = cy; this.size = size
        rot = 0f; sx = 1f; sy = 1f
        eyes = Eyes.OPEN; mouth = Mouth.SQUIGGLE
        gazeX = 0f; gazeY = 0f; blush = 0f
        shadow = 1f; attach = null
    }
}

/** Canvas plus scratch objects, so a frame allocates almost nothing. */
class Stage(var canvas: Canvas, var w: Float, var h: Float) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val path = Path()
    val rect = RectF()
    val pose = Pose()

    /** Short side — the unit every size is expressed in. */
    val u: Float get() = min(w, h)

    fun fill(color: Int): Paint {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL; p.color = color
        return p
    }

    fun stroke(color: Int, width: Float): Paint {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE
        p.color = color; p.strokeWidth = width
        p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        return p
    }

    fun circle(x: Float, y: Float, r: Float, color: Int) {
        canvas.drawCircle(x, y, r, fill(color))
    }

    fun oval(x: Float, y: Float, rx: Float, ry: Float, color: Int) {
        rect.set(x - rx, y - ry, x + rx, y + ry)
        canvas.drawOval(rect, fill(color))
    }
}

object Flannery {

    /** Taller than wide, like the drawing. */
    private const val H_RATIO = 0.550f

    /**
     * His outline, parametrically: an egg with the fat end down — narrower
     * across the top dome, widest below the middle. Sampling it (rather than
     * using a plain ellipse) lets the fur and the silhouette share one shape.
     */
    private fun outlineX(t: Float, hw: Float) = hw * cos(t) * (1f + 0.10f * sin(t))
    private fun outlineY(t: Float, hh: Float) = hh * sin(t)

    private fun bodyPath(st: Stage, hw: Float, hh: Float) {
        val p = st.path
        p.reset()
        val steps = 96
        for (i in 0 until steps) {
            val t = i / steps.toFloat() * TAU
            val x = outlineX(t, hw)
            val y = outlineY(t, hh)
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
    }

    fun draw(st: Stage, p: Pose) {
        val c = st.canvas
        val hw = p.size * 0.5f
        val hh = p.size * H_RATIO
        val line = p.size * 0.0075f

        if (p.shadow > 0.01f) {
            st.oval(p.cx, p.cy + hh * 1.06f, hw * 0.80f * p.sx, hh * 0.09f,
                withAlpha(Hue.SHADOW, 0.22f * p.shadow))
        }

        c.save()
        c.translate(p.cx, p.cy)
        if (p.rot != 0f) c.rotate(p.rot)
        if (p.sx != 1f || p.sy != 1f) c.scale(p.sx, p.sy)

        drawFins(st, hw, hh, line)
        drawFur(st, hw, hh)

        bodyPath(st, hw, hh)
        c.drawPath(st.path, st.fill(Hue.BODY))

        c.save()
        bodyPath(st, hw, hh)
        c.clipPath(st.path)
        // light gathering on the upper dome, weight settling low
        for (k in 0 until 12) {
            val f = k / 11f
            st.oval(-hw * 0.12f, -hh * (0.52f - 0.14f * f), hw * (0.34f + 0.36f * f),
                hh * (0.20f + 0.26f * f), withAlpha(Hue.BODY_LIGHT, 0.055f))
        }
        for (k in 0 until 8) {
            val f = k / 7f
            st.oval(0f, hh * (1.04f + 0.06f * f), hw * (0.86f - 0.10f * f), hh * 0.30f,
                withAlpha(Hue.BODY_DEEP, 0.05f))
        }
        c.restore()

        bodyPath(st, hw, hh)
        c.drawPath(st.path, st.stroke(withAlpha(Hue.LINE, 0.60f), line))

        c.save()
        bodyPath(st, hw, hh)
        c.clipPath(st.path)
        drawBelly(st, hw, hh, line)
        c.restore()
        drawEyes(st, p, hw, hh)
        drawMouth(st, p, hw, hh)

        p.attach?.invoke(st, hw, hh)

        c.restore()
    }

    /**
     * A leafy fin fan on each side at eye level: four rounded lobes radiating
     * from one attachment point, outlined, sitting behind the body.
     */
    private fun drawFins(st: Stage, hw: Float, hh: Float, line: Float) {
        val c = st.canvas
        val baseDeg = -28f
        val rel = floatArrayOf(-44f, -15f, 14f, 42f)
        val lens = floatArrayOf(0.215f, 0.290f, 0.278f, 0.205f)
        val wids = floatArrayOf(0.132f, 0.158f, 0.153f, 0.126f)

        for (side in intArrayOf(-1, 1)) {
            val t = baseDeg * DEG
            val bx = outlineX(t, hw) * side * 0.94f
            val by = outlineY(t, hh) * 0.94f
            for (i in rel.indices) {
                val len = hw * lens[i]
                val wid = hw * wids[i]
                val ang = baseDeg + rel[i]
                c.save()
                c.translate(bx, by)
                c.rotate(if (side > 0) ang else 180f - ang)
                c.translate(len * 0.44f, 0f)
                leafPath(st, len, wid)
                c.drawPath(st.path, st.fill(Hue.FIN))
                c.drawPath(st.path, st.stroke(withAlpha(Hue.FIN_LINE, 0.80f), line))
                c.restore()
            }
        }
    }

    /** Rounded lobe with a soft point. */
    private fun leafPath(st: Stage, len: Float, wid: Float) {
        val p = st.path
        p.reset()
        p.moveTo(-len * 0.88f, -wid * 0.64f)
        p.quadTo(len * 0.24f, -wid * 1.02f, len * 0.88f, -wid * 0.30f)
        p.quadTo(len * 1.16f, 0f, len * 0.88f, wid * 0.30f)
        p.quadTo(len * 0.24f, wid * 1.02f, -len * 0.88f, wid * 0.64f)
        p.quadTo(-len * 1.14f, 0f, -len * 0.88f, -wid * 0.64f)
        p.close()
    }

    /** Shaggy pile following the same outline the body uses. */
    private fun drawFur(st: Stage, hw: Float, hh: Float) {
        val pt = st.stroke(Hue.BODY_LIGHT, hw * 0.021f)
        val n = 380
        for (i in 0 until n) {
            val t = i / n.toFloat() * TAU + (rnd(i) - 0.5f) * 0.03f
            val x = outlineX(t, hw)
            val y = outlineY(t, hh)
            val d = kotlin.math.sqrt(x * x + y * y)
            if (d <= 0f) continue
            val nx = x / d
            val ny = y / d
            val lean = (rnd(i * 5) - 0.5f) * 0.7f
            val ox = nx * cos(lean) - ny * sin(lean)
            val oy = nx * sin(lean) + ny * cos(lean)
            val clump = 0.42f + 0.58f * (0.5f + 0.5f * sin(t * 9f))
            val len = hw * (0.008f + 0.024f * clump * (0.45f + 0.55f * rnd(i * 3)))
            pt.color = withAlpha(if (rnd(i * 11) > 0.45f) Hue.BODY_LIGHT else Hue.BODY, 0.9f)
            st.canvas.drawLine(x * 0.97f, y * 0.97f, x + ox * len, y + oy * len, pt)
        }
    }

    /** The big cream belly: a tall oval low on the body, with a furry rim. */
    private fun drawBelly(st: Stage, hw: Float, hh: Float, line: Float) {
        val cy = hh * 0.52f
        val rx = hw * 0.70f
        val ry = hh * 0.50f

        st.oval(0f, cy, rx, ry, Hue.BELLY)
        st.oval(0f, cy + hh * 0.18f, rx * 0.92f, ry * 0.82f, withAlpha(Hue.BELLY_SHADE, 0.35f))
        st.oval(0f, cy - hh * 0.03f, rx * 0.96f, ry * 0.95f, Hue.BELLY)

        val pt = st.stroke(withAlpha(Hue.BELLY, 0.85f), hw * 0.014f)
        for (i in 0 until 150) {
            val a = i / 150f * TAU + (rnd(i) - 0.5f) * 0.05f
            val ca = cos(a)
            val sa = sin(a)
            val lean = (rnd(i * 5) - 0.5f) * 0.7f
            val len = hw * (0.008f + 0.018f * rnd(i * 7))
            pt.color = withAlpha(if (sa > 0.3f) Hue.BELLY_SHADE else Hue.BELLY, 0.88f)
            st.canvas.drawLine(
                ca * rx * 0.98f, cy + sa * ry * 0.98f,
                ca * rx + cos(a + lean) * len, cy + sa * ry + sin(a + lean) * len, pt
            )
        }
        st.rect.set(-rx, cy - ry, rx, cy + ry)
        st.canvas.drawOval(st.rect, st.stroke(withAlpha(Hue.BELLY_LINE, 0.40f), line * 0.85f))
    }

    private fun drawEyes(st: Stage, p: Pose, hw: Float, hh: Float) {
        val ex = hw * 0.375f
        val ey = -hh * 0.46f
        val r = hw * 0.082f
        val gx = p.gazeX * r * 0.34f
        val gy = p.gazeY * r * 0.34f

        for (side in intArrayOf(-1, 1)) {
            val x = side * ex + gx
            val y = ey + gy
            when (p.eyes) {
                // flat, matte, no highlight — as drawn
                Eyes.OPEN, Eyes.WIDE -> {
                    val rr = if (p.eyes == Eyes.WIDE) r * 1.28f else r
                    st.oval(x, y, rr * 0.90f, rr * 1.06f, Hue.INK)
                }
                Eyes.BLINK -> {
                    st.path.reset()
                    st.path.moveTo(x - r * 1.15f, y - r * 0.12f)
                    st.path.quadTo(x, y + r * 0.58f, x + r * 1.15f, y - r * 0.12f)
                    st.canvas.drawPath(st.path, st.stroke(Hue.INK, r * 0.40f))
                }
                Eyes.HAPPY -> {
                    st.path.reset()
                    st.path.moveTo(x - r * 1.15f, y + r * 0.38f)
                    st.path.quadTo(x, y - r * 0.86f, x + r * 1.15f, y + r * 0.38f)
                    st.canvas.drawPath(st.path, st.stroke(Hue.INK, r * 0.40f))
                }
                Eyes.SLEEPY -> {
                    st.path.reset()
                    st.path.moveTo(x - r * 1.15f, y)
                    st.path.quadTo(x, y + r * 0.76f, x + r * 1.15f, y)
                    st.canvas.drawPath(st.path, st.stroke(Hue.INK, r * 0.38f))
                }
                Eyes.HEART -> Props.heart(st, x, y, r * 1.70f, Hue.PINK)
                Eyes.STAR -> Props.star(st, x, y, r * 1.65f, Hue.GOLD)
                Eyes.DIZZY -> {
                    val pt = st.stroke(Hue.INK, r * 0.28f)
                    st.path.reset()
                    var a = 0f
                    var rad = r * 0.14f
                    st.path.moveTo(x, y)
                    while (a < TAU * 1.6f) {
                        st.path.lineTo(x + cos(a) * rad, y + sin(a) * rad)
                        a += 0.35f
                        rad += r * 0.070f
                    }
                    st.canvas.drawPath(st.path, pt)
                }
            }
        }
    }

    private fun drawMouth(st: Stage, p: Pose, hw: Float, hh: Float) {
        val my = -hh * 0.30f
        val m = hw * 0.160f
        val w = hw * 0.026f
        when (p.mouth) {
            // wide, flat, gently wavy — the line from the drawing
            Mouth.SQUIGGLE -> {
                st.path.reset()
                st.path.moveTo(-m, my - m * 0.10f)
                st.path.quadTo(-m * 0.48f, my + m * 0.34f, 0f, my + m * 0.10f)
                st.path.quadTo(m * 0.48f, my - m * 0.16f, m, my + m * 0.22f)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
            }
            Mouth.SMILE -> {
                st.path.reset()
                st.path.moveTo(-m, my - m * 0.06f)
                st.path.quadTo(0f, my + m * 0.60f, m, my - m * 0.06f)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
            }
            Mouth.TINY -> {
                st.path.reset()
                st.path.moveTo(-m * 0.40f, my)
                st.path.quadTo(0f, my + m * 0.30f, m * 0.40f, my)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
            }
            Mouth.FROWN -> {
                st.path.reset()
                st.path.moveTo(-m * 0.82f, my + m * 0.26f)
                st.path.quadTo(0f, my - m * 0.36f, m * 0.82f, my + m * 0.26f)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
            }
            Mouth.OPEN -> {
                st.oval(0f, my + m * 0.20f, m * 0.34f, m * 0.32f, Hue.INK)
                st.oval(0f, my + m * 0.32f, m * 0.20f, m * 0.14f, Hue.PINK)
            }
            Mouth.WIDE_OPEN -> {
                st.oval(0f, my + m * 0.26f, m * 0.46f, m * 0.52f, Hue.INK)
                st.oval(0f, my + m * 0.50f, m * 0.28f, m * 0.21f, Hue.PINK)
            }
            Mouth.YAWN -> {
                st.oval(0f, my + m * 0.42f, m * 0.40f, m * 0.78f, Hue.INK)
                st.oval(0f, my + m * 0.82f, m * 0.25f, m * 0.27f, Hue.PINK)
            }
            Mouth.BLEP -> {
                st.path.reset()
                st.path.moveTo(-m * 0.44f, my)
                st.path.quadTo(0f, my + m * 0.28f, m * 0.44f, my)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
                st.rect.set(-m * 0.19f, my + m * 0.06f, m * 0.19f, my + m * 0.56f)
                st.canvas.drawRoundRect(st.rect, m * 0.19f, m * 0.19f, st.fill(Hue.PINK))
            }
        }
    }
}

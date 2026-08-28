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
    // Soft sage plush with cream belly and a drawn dark-green line.
    val BODY = 0xFFB4D1A2.toInt()
    val BODY_DEEP = 0xFF9BBE88.toInt()
    val BODY_LIGHT = 0xFFC9E0BA.toInt()
    val LINE = 0xFF6C8D5A.toInt()
    val FIN = 0xFFA8CB95.toInt()
    val FIN_LINE = 0xFF5C7D4C.toInt()
    val BELLY = 0xFFEDE5C9.toInt()
    val BELLY_SHADE = 0xFFDCD2B0.toInt()
    val BELLY_LINE = 0xFFB3A987.toInt()
    val INK = 0xFF3A2F28.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
    val PINK = 0xFFE08095.toInt()
    val GOLD = 0xFFEDBE55.toInt()
    val SHADOW = 0xFF9AA891.toInt()
    val CREAM = 0xFFFFF8F2.toInt()

    // kept so props and backgrounds keep compiling
    val MINT = BODY
    val MINT_DEEP = BODY_DEEP
    val MINT_LIGHT = BODY_LIGHT
    val FRILL = FIN
    val FRILL_EDGE = FIN_LINE
    val BLUSH = 0xFFE0A9A2.toInt()
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

    // Round, a touch taller than wide.
    private const val H_RATIO = 0.510f

    fun draw(st: Stage, p: Pose) {
        val c = st.canvas
        val hw = p.size * 0.5f
        val hh = p.size * H_RATIO
        val line = p.size * 0.011f

        if (p.shadow > 0.01f) {
            st.oval(p.cx, p.cy + hh * 1.10f, hw * 0.76f * p.sx, hh * 0.10f,
                withAlpha(Hue.SHADOW, 0.20f * p.shadow))
        }

        c.save()
        c.translate(p.cx, p.cy)
        if (p.rot != 0f) c.rotate(p.rot)
        if (p.sx != 1f || p.sy != 1f) c.scale(p.sx, p.sy)

        drawFins(st, hw, hh, line)
        drawFuzz(st, hw, hh)

        // Body
        st.oval(0f, 0f, hw, hh, Hue.BODY)

        st.path.reset()
        st.rect.set(-hw, -hh, hw, hh)
        st.path.addOval(st.rect, Path.Direction.CW)

        c.save()
        c.clipPath(st.path)
        // gentle shading: lighter up top, deeper toward the base
        for (k in 0 until 10) {
            val f = k / 9f
            st.oval(-hw * 0.06f, -hh * (0.62f - 0.16f * f), hw * (0.40f + 0.34f * f),
                hh * (0.22f + 0.28f * f), withAlpha(Hue.BODY_LIGHT, 0.045f))
        }
        st.oval(0f, hh * 0.98f, hw * 0.74f, hh * 0.26f, withAlpha(Hue.BODY_DEEP, 0.28f))
        c.restore()

        // Silhouette line
        st.rect.set(-hw, -hh, hw, hh)
        c.drawOval(st.rect, st.stroke(withAlpha(Hue.LINE, 0.70f), line))

        drawBelly(st, hw, hh, line)

        drawEyes(st, p, hw, hh)
        drawMouth(st, p, hw, hh)

        p.attach?.invoke(st, hw, hh)

        c.restore()
    }

    /**
     * The crest: a run of overlapping leaf-shaped lobes down each side of the
     * head, tips sweeping upward, largest at the top — the way they sit on him.
     */
    private fun drawFins(st: Stage, hw: Float, hh: Float, line: Float) {
        val c = st.canvas
        for (side in intArrayOf(-1, 1)) {
            for (i in 0 until 6) {
                val f = i / 5f
                val aDeg = lerp(-56f, 24f, f)
                val a = aDeg * DEG
                val bulk = 1.06f - 0.34f * f
                val len = hw * 0.205f * bulk
                val wid = hw * 0.150f * bulk
                val px = cos(a) * side * hw * 0.90f
                val py = sin(a) * hh * 0.90f
                val tilt = 18f

                c.save()
                c.translate(px, py)
                c.rotate(if (side > 0) aDeg - tilt else 180f - aDeg + tilt)
                c.translate(len * 0.55f, 0f)
                leafPath(st, len, wid)
                c.drawPath(st.path, st.fill(Hue.FIN))
                c.drawPath(st.path, st.stroke(withAlpha(Hue.FIN_LINE, 0.85f), line))
                c.restore()
            }
        }
    }

    /** Rounded lobe with a soft point — a leafy fin. */
    private fun leafPath(st: Stage, len: Float, wid: Float) {
        val p = st.path
        p.reset()
        p.moveTo(-len * 0.86f, -wid * 0.66f)
        p.quadTo(len * 0.26f, -wid * 1.02f, len * 0.88f, -wid * 0.32f)
        p.quadTo(len * 1.16f, 0f, len * 0.88f, wid * 0.32f)
        p.quadTo(len * 0.26f, wid * 1.02f, -len * 0.86f, wid * 0.66f)
        p.quadTo(-len * 1.12f, 0f, -len * 0.86f, -wid * 0.66f)
        p.close()
    }

    /** Cream belly patch, inset from the edges, with a shaggy rim. */
    private fun drawBelly(st: Stage, hw: Float, hh: Float, line: Float) {
        val cy = hh * 0.40f
        val rx = hw * 0.70f
        val ry = hh * 0.52f

        st.oval(0f, cy, rx, ry, Hue.BELLY)
        st.oval(0f, cy + hh * 0.16f, rx * 0.92f, ry * 0.80f, withAlpha(Hue.BELLY_SHADE, 0.40f))
        st.oval(0f, cy - hh * 0.02f, rx * 0.96f, ry * 0.94f, Hue.BELLY)

        // fur rim: short strokes all the way round the patch
        val pt = st.stroke(withAlpha(Hue.BELLY, 0.7f), hw * 0.016f)
        for (i in 0 until 130) {
            val a = i / 130f * TAU + (rnd(i) - 0.5f) * 0.06f
            val lean = (rnd(i * 5) - 0.5f) * 0.6f
            val ca = cos(a)
            val sa = sin(a)
            val start = 0.955f + 0.035f * rnd(i * 13)
            val len = hw * (0.014f + 0.026f * rnd(i * 7))
            pt.color = withAlpha(if (sa > 0.3f) Hue.BELLY_SHADE else Hue.BELLY, 0.88f)
            st.canvas.drawLine(
                ca * rx * start, cy + sa * ry * start,
                ca * rx * start + cos(a + lean) * len, cy + sa * ry * start + sin(a + lean) * len, pt
            )
        }
        st.rect.set(-rx, cy - ry, rx, cy + ry)
        st.canvas.drawOval(st.rect, st.stroke(withAlpha(Hue.BELLY_LINE, 0.30f), line * 0.7f))
    }

    /** Shaggy pile around the silhouette — irregular, so it reads as fur not stitching. */
    private fun drawFuzz(st: Stage, hw: Float, hh: Float) {
        val pt = st.stroke(Hue.BODY_LIGHT, hw * 0.017f)
        for (i in 0 until 300) {
            val a = i / 300f * TAU + (rnd(i) - 0.5f) * 0.045f
            val lean = (rnd(i * 5) - 0.5f) * 0.55f
            val ca = cos(a)
            val sa = sin(a)
            val start = 0.955f + 0.030f * rnd(i * 13)
            val len = hw * (0.020f + 0.032f * rnd(i * 3))
            val ox = cos(a + lean)
            val oy = sin(a + lean)
            pt.color = withAlpha(if (rnd(i * 11) > 0.5f) Hue.BODY_LIGHT else Hue.BODY, 0.9f)
            st.canvas.drawLine(
                ca * hw * start, sa * hh * start,
                ca * hw * start + ox * len, sa * hh * start + oy * len, pt
            )
        }
    }

    private fun drawEyes(st: Stage, p: Pose, hw: Float, hh: Float) {
        val ex = hw * 0.415f
        val ey = -hh * 0.30f
        val r = hw * 0.108f
        val gx = p.gazeX * r * 0.30f
        val gy = p.gazeY * r * 0.30f

        for (side in intArrayOf(-1, 1)) {
            val x = side * ex + gx
            val y = ey + gy
            when (p.eyes) {
                // flat matte pupils, no highlight — as drawn
                Eyes.OPEN, Eyes.WIDE -> {
                    val rr = if (p.eyes == Eyes.WIDE) r * 1.24f else r
                    st.oval(x, y, rr * 0.92f, rr, Hue.INK)
                }
                Eyes.BLINK -> {
                    st.path.reset()
                    st.path.moveTo(x - r * 1.05f, y - r * 0.10f)
                    st.path.quadTo(x, y + r * 0.52f, x + r * 1.05f, y - r * 0.10f)
                    st.canvas.drawPath(st.path, st.stroke(Hue.INK, r * 0.34f))
                }
                Eyes.HAPPY -> {
                    st.path.reset()
                    st.path.moveTo(x - r * 1.05f, y + r * 0.34f)
                    st.path.quadTo(x, y - r * 0.80f, x + r * 1.05f, y + r * 0.34f)
                    st.canvas.drawPath(st.path, st.stroke(Hue.INK, r * 0.34f))
                }
                Eyes.SLEEPY -> {
                    st.path.reset()
                    st.path.moveTo(x - r * 1.05f, y)
                    st.path.quadTo(x, y + r * 0.70f, x + r * 1.05f, y)
                    st.canvas.drawPath(st.path, st.stroke(Hue.INK, r * 0.32f))
                }
                Eyes.HEART -> Props.heart(st, x, y, r * 1.40f, Hue.PINK)
                Eyes.STAR -> Props.star(st, x, y, r * 1.36f, Hue.GOLD)
                Eyes.DIZZY -> {
                    val pt = st.stroke(Hue.INK, r * 0.24f)
                    st.path.reset()
                    var a = 0f
                    var rad = r * 0.12f
                    st.path.moveTo(x, y)
                    while (a < TAU * 1.6f) {
                        st.path.lineTo(x + cos(a) * rad, y + sin(a) * rad)
                        a += 0.35f
                        rad += r * 0.058f
                    }
                    st.canvas.drawPath(st.path, pt)
                }
            }
        }
    }

    private fun drawMouth(st: Stage, p: Pose, hw: Float, hh: Float) {
        val my = -hh * 0.16f
        val m = hw * 0.185f
        val w = hw * 0.030f
        when (p.mouth) {
            // wide, flat, hand-drawn wave
            Mouth.SQUIGGLE -> {
                st.path.reset()
                st.path.moveTo(-m, my - m * 0.02f)
                st.path.quadTo(-m * 0.50f, my + m * 0.40f, 0f, my + m * 0.10f)
                st.path.quadTo(m * 0.50f, my - m * 0.24f, m, my + m * 0.20f)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
            }
            Mouth.SMILE -> {
                st.path.reset()
                st.path.moveTo(-m, my - m * 0.06f)
                st.path.quadTo(0f, my + m * 0.62f, m, my - m * 0.06f)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
            }
            Mouth.TINY -> {
                st.path.reset()
                st.path.moveTo(-m * 0.38f, my)
                st.path.quadTo(0f, my + m * 0.30f, m * 0.38f, my)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
            }
            Mouth.FROWN -> {
                st.path.reset()
                st.path.moveTo(-m * 0.80f, my + m * 0.26f)
                st.path.quadTo(0f, my - m * 0.36f, m * 0.80f, my + m * 0.26f)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
            }
            Mouth.OPEN -> {
                st.oval(0f, my + m * 0.18f, m * 0.36f, m * 0.34f, Hue.INK)
                st.oval(0f, my + m * 0.32f, m * 0.21f, m * 0.15f, Hue.PINK)
            }
            Mouth.WIDE_OPEN -> {
                st.oval(0f, my + m * 0.24f, m * 0.48f, m * 0.54f, Hue.INK)
                st.oval(0f, my + m * 0.50f, m * 0.29f, m * 0.22f, Hue.PINK)
            }
            Mouth.YAWN -> {
                st.oval(0f, my + m * 0.40f, m * 0.42f, m * 0.80f, Hue.INK)
                st.oval(0f, my + m * 0.82f, m * 0.26f, m * 0.28f, Hue.PINK)
            }
            Mouth.BLEP -> {
                st.path.reset()
                st.path.moveTo(-m * 0.44f, my)
                st.path.quadTo(0f, my + m * 0.28f, m * 0.44f, my)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, w))
                st.rect.set(-m * 0.20f, my + m * 0.06f, m * 0.20f, my + m * 0.58f)
                st.canvas.drawRoundRect(st.rect, m * 0.20f, m * 0.20f, st.fill(Hue.PINK))
            }
        }
    }
}

package com.glasscrane.flannery.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
fun pulse(t: Float) = sin(clamp01(t) * 3.14159265f)

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

/** Colours sampled from the artwork, so overlays sit on it invisibly. */
object Hue {
    val MINT = 0xFF9ED4C4.toInt()
    val MINT_DEEP = 0xFF6FB4A0.toInt()
    val MINT_LIGHT = 0xFFC6E9DE.toInt()
    val FRILL = 0xFF9FDCCB.toInt()
    val BELLY = 0xFFEDEDE9.toInt()
    val BELLY_SHADE = 0xFFDCDCD6.toInt()
    val INK = 0xFF2A2E2C.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
    val PINK = 0xFFE9718C.toInt()
    val GOLD = 0xFFEFBE52.toInt()
    val BLUSH = 0xFFE8A0A8.toInt()
    val SHADOW = 0xFF8FA69E.toInt()
    val CREAM = 0xFFF5F0E4.toInt()

    /** Fur immediately around each feature — used to hide it before redrawing. */
    val FUR_EYE_L = 0xFF90B3A3.toInt()
    val FUR_EYE_R = 0xFFAED5C6.toInt()
    val FUR_MOUTH = 0xFFA1C7B8.toInt()
}

enum class Eyes { OPEN, BLINK, HAPPY, SLEEPY, WIDE, HEART, STAR, DIZZY }
enum class Mouth { SQUIGGLE, SMILE, OPEN, WIDE_OPEN, BLEP, FROWN, TINY, YAWN }

/** The artwork itself. Loaded once and handed to the renderer. */
object Sprite {
    var bmp: Bitmap? = null

    // Where his face sits inside the square sprite, measured off the artwork.
    const val EYE_LX = 0.3391f
    const val EYE_LY = 0.3642f
    const val EYE_RX = 0.6529f
    const val EYE_RY = 0.3693f
    const val EYE_R = 0.040f
    const val MOUTH_X = 0.5339f
    const val MOUTH_Y = 0.4120f
    const val MOUTH_W = 0.092f
}

class Pose {
    var cx = 0f
    var cy = 0f
    var size = 0f          // drawn width of the sprite, in px
    var rot = 0f
    var sx = 1f
    var sy = 1f
    var eyes = Eyes.OPEN
    var mouth = Mouth.SQUIGGLE
    var gazeX = 0f
    var gazeY = 0f
    var blush = 0f
    var shadow = 1f
    var attach: ((Stage, Float, Float) -> Unit)? = null

    fun reset(cx: Float, cy: Float, size: Float) {
        this.cx = cx; this.cy = cy; this.size = size
        rot = 0f; sx = 1f; sy = 1f
        eyes = Eyes.OPEN; mouth = Mouth.SQUIGGLE
        gazeX = 0f; gazeY = 0f; blush = 0f
        shadow = 1f; attach = null
    }
}

class Stage(var canvas: Canvas, var w: Float, var h: Float) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    val path = Path()
    val rect = RectF()
    val pose = Pose()

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

    fun circle(x: Float, y: Float, r: Float, color: Int) =
        canvas.drawCircle(x, y, r, fill(color))

    fun oval(x: Float, y: Float, rx: Float, ry: Float, color: Int) {
        rect.set(x - rx, y - ry, x + rx, y + ry)
        canvas.drawOval(rect, fill(color))
    }
}

/**
 * Flannery is the artwork. Animations move, squash and rotate the sprite;
 * expressions are drawn over the top at the measured face positions, hiding
 * the painted feature underneath with a soft patch of its surrounding fur.
 */
object Flannery {

    fun draw(st: Stage, p: Pose) {
        val bmp = Sprite.bmp ?: return
        val c = st.canvas
        val half = p.size * 0.5f

        if (p.shadow > 0.01f) {
            st.oval(p.cx, p.cy + half * 0.96f, half * 0.62f * p.sx, half * 0.085f,
                withAlpha(Hue.SHADOW, 0.22f * p.shadow))
        }

        c.save()
        c.translate(p.cx, p.cy)
        if (p.rot != 0f) c.rotate(p.rot)
        if (p.sx != 1f || p.sy != 1f) c.scale(p.sx, p.sy)

        st.rect.set(-half, -half, half, half)
        c.drawBitmap(bmp, null, st.rect, st.bmpPaint)

        val s = p.size
        drawEyes(st, p, s)
        drawMouth(st, p, s)

        if (p.blush > 0.01f) {
            val ba = 0.30f * p.blush
            st.oval(-s * 0.30f, s * 0.02f, s * 0.075f, s * 0.045f, withAlpha(Hue.BLUSH, ba))
            st.oval(s * 0.30f, s * 0.02f, s * 0.075f, s * 0.045f, withAlpha(Hue.BLUSH, ba))
        }

        p.attach?.invoke(st, half, half)
        c.restore()
    }

    /** Local coordinate of a normalised sprite position. */
    private fun lx(n: Float, size: Float) = (n - 0.5f) * size
    private fun ly(n: Float, size: Float) = (n - 0.5f) * size

    /** Soft blob of fur that hides a painted feature before a new one is drawn. */
    private fun patch(st: Stage, x: Float, y: Float, r: Float, color: Int) {
        for (k in 6 downTo 0) {
            val f = k / 6f
            st.circle(x, y, r * (0.85f + 0.55f * f), withAlpha(color, 0.30f))
        }
        st.circle(x, y, r * 0.9f, color)
    }

    private fun drawEyes(st: Stage, p: Pose, s: Float) {
        if (p.eyes == Eyes.OPEN && p.gazeX == 0f && p.gazeY == 0f) return

        val r = Sprite.EYE_R * s
        val gx = p.gazeX * r * 0.30f
        val gy = p.gazeY * r * 0.30f
        val xs = floatArrayOf(lx(Sprite.EYE_LX, s), lx(Sprite.EYE_RX, s))
        val ys = floatArrayOf(ly(Sprite.EYE_LY, s), ly(Sprite.EYE_RY, s))
        val furs = intArrayOf(Hue.FUR_EYE_L, Hue.FUR_EYE_R)

        for (i in 0..1) {
            val bx = xs[i]
            val by = ys[i]
            val fur = furs[i]
            when (p.eyes) {
                Eyes.OPEN -> {
                    // gaze: cover the painted eye and redraw it shifted
                    patch(st, bx, by, r * 1.30f, fur)
                    st.oval(bx + gx, by + gy, r * 0.92f, r, Hue.INK)
                }
                Eyes.WIDE -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    st.oval(bx + gx, by + gy, r * 1.16f, r * 1.26f, Hue.INK)
                }
                Eyes.BLINK -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    st.path.reset()
                    st.path.moveTo(bx - r * 1.05f, by - r * 0.10f)
                    st.path.quadTo(bx, by + r * 0.62f, bx + r * 1.05f, by - r * 0.10f)
                    st.canvas.drawPath(st.path, st.stroke(Hue.INK, r * 0.34f))
                }
                Eyes.HAPPY -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    st.path.reset()
                    st.path.moveTo(bx - r * 1.05f, by + r * 0.40f)
                    st.path.quadTo(bx, by - r * 0.86f, bx + r * 1.05f, by + r * 0.40f)
                    st.canvas.drawPath(st.path, st.stroke(Hue.INK, r * 0.34f))
                }
                Eyes.SLEEPY -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    st.path.reset()
                    st.path.moveTo(bx - r * 1.05f, by)
                    st.path.quadTo(bx, by + r * 0.78f, bx + r * 1.05f, by)
                    st.canvas.drawPath(st.path, st.stroke(Hue.INK, r * 0.32f))
                }
                Eyes.HEART -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    Props.heart(st, bx, by, r * 1.55f, Hue.PINK)
                }
                Eyes.STAR -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    Props.star(st, bx, by, r * 1.55f, Hue.GOLD)
                }
                Eyes.DIZZY -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    val pt = st.stroke(Hue.INK, r * 0.26f)
                    st.path.reset()
                    var a = 0f
                    var rad = r * 0.13f
                    st.path.moveTo(bx, by)
                    while (a < TAU * 1.6f) {
                        st.path.lineTo(bx + cos(a) * rad, by + sin(a) * rad)
                        a += 0.35f
                        rad += r * 0.062f
                    }
                    st.canvas.drawPath(st.path, pt)
                }
            }
        }
    }

    private fun drawMouth(st: Stage, p: Pose, s: Float) {
        // His painted mouth already reads as a soft smile, so the resting
        // states leave the artwork alone.
        if (p.mouth == Mouth.SQUIGGLE || p.mouth == Mouth.SMILE || p.mouth == Mouth.TINY) return

        val x = lx(Sprite.MOUTH_X, s)
        val y = ly(Sprite.MOUTH_Y, s)
        val m = Sprite.MOUTH_W * s
        when (p.mouth) {
            Mouth.OPEN -> {
                patch(st, x, y, m * 0.62f, Hue.FUR_MOUTH)
                st.oval(x, y + m * 0.10f, m * 0.30f, m * 0.30f, Hue.INK)
                st.oval(x, y + m * 0.20f, m * 0.18f, m * 0.13f, Hue.PINK)
            }
            Mouth.WIDE_OPEN -> {
                patch(st, x, y, m * 0.70f, Hue.FUR_MOUTH)
                st.oval(x, y + m * 0.16f, m * 0.42f, m * 0.46f, Hue.INK)
                st.oval(x, y + m * 0.40f, m * 0.25f, m * 0.19f, Hue.PINK)
            }
            Mouth.YAWN -> {
                patch(st, x, y, m * 0.72f, Hue.FUR_MOUTH)
                st.oval(x, y + m * 0.34f, m * 0.36f, m * 0.68f, Hue.INK)
                st.oval(x, y + m * 0.72f, m * 0.22f, m * 0.24f, Hue.PINK)
            }
            Mouth.FROWN -> {
                patch(st, x, y, m * 0.66f, Hue.FUR_MOUTH)
                st.path.reset()
                st.path.moveTo(x - m * 0.40f, y + m * 0.18f)
                st.path.quadTo(x, y - m * 0.26f, x + m * 0.40f, y + m * 0.18f)
                st.canvas.drawPath(st.path, st.stroke(Hue.INK, m * 0.11f))
            }
            // tongue hangs below his own mouth — no patch needed
            Mouth.BLEP -> {
                st.rect.set(x - m * 0.15f, y + m * 0.06f, x + m * 0.15f, y + m * 0.52f)
                st.canvas.drawRoundRect(st.rect, m * 0.15f, m * 0.15f, st.fill(Hue.PINK))
            }
            else -> Unit
        }
    }
}

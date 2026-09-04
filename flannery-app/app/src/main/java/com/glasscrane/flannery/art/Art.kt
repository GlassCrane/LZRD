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
enum class Brows { NONE, ANGRY, SAD, RAISED }

/** One drawable Flannery: his bitmap plus where his face sits inside it. */
class SpriteSheet(
    val bmp: Bitmap,
    val eyeLx: Float, val eyeLy: Float,
    val eyeRx: Float, val eyeRy: Float,
    /** Eye radius as a fraction of sprite width. */
    val eyeR: Float,
    /** Fur immediately around each eye — used to hide it before redrawing. */
    val furEyeL: Int, val furEyeR: Int,
    /** The colour his own features are drawn in; overlays match it. */
    val ink: Int,
    /** Bilinear for the plush photo; nearest-neighbour keeps pixels crisp. */
    val filter: Boolean
)

/** Both versions of him, and which one is active. 8-bit is the default. */
object Sprite {
    var classic: SpriteSheet? = null
    var pixel: SpriteSheet? = null
    var pixelMode = true
    val current: SpriteSheet? get() = if (pixelMode) pixel else classic
}

class Pose {
    var cx = 0f
    var cy = 0f
    var size = 0f          // drawn width of the sprite, in px
    var rot = 0f
    var sx = 1f
    var sy = 1f
    var eyes = Eyes.OPEN
    var brows = Brows.NONE
    var gazeX = 0f
    var gazeY = 0f
    var blush = 0f
    var shadow = 1f
    var attach: ((Stage, Float, Float) -> Unit)? = null

    fun reset(cx: Float, cy: Float, size: Float) {
        this.cx = cx; this.cy = cy; this.size = size
        rot = 0f; sx = 1f; sy = 1f
        eyes = Eyes.OPEN; brows = Brows.NONE
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
        val sheet = Sprite.current ?: return
        val c = st.canvas
        val half = p.size * 0.5f

        c.save()
        c.translate(p.cx, p.cy)
        if (p.rot != 0f) c.rotate(p.rot)
        if (p.sx != 1f || p.sy != 1f) c.scale(p.sx, p.sy)

        st.rect.set(-half, -half, half, half)
        st.bmpPaint.isFilterBitmap = sheet.filter
        c.drawBitmap(sheet.bmp, null, st.rect, st.bmpPaint)

        val s = p.size
        drawEyes(st, p, sheet, s)
        drawBrows(st, p, sheet, s)

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

    /** Eyebrows are new marks on fur, so they draw straight over the artwork. */
    private fun drawBrows(st: Stage, p: Pose, sheet: SpriteSheet, s: Float) {
        if (p.brows == Brows.NONE) return
        val r = sheet.eyeR * s
        val xs = floatArrayOf(lx(sheet.eyeLx, s), lx(sheet.eyeRx, s))
        val ys = floatArrayOf(ly(sheet.eyeLy, s), ly(sheet.eyeRy, s))
        for (i in 0..1) {
            val bx = xs[i]
            val by = ys[i] - r * 1.70f
            val inner = if (i == 0) 1f else -1f
            st.path.reset()
            when (p.brows) {
                Brows.ANGRY -> {
                    st.path.moveTo(bx + inner * r * 1.25f, by + r * 0.70f)
                    st.path.quadTo(bx, by + r * 0.10f, bx - inner * r * 1.10f, by - r * 0.05f)
                }
                Brows.SAD -> {
                    st.path.moveTo(bx + inner * r * 1.20f, by - r * 0.10f)
                    st.path.quadTo(bx, by + r * 0.20f, bx - inner * r * 1.10f, by + r * 0.60f)
                }
                Brows.RAISED -> {
                    st.path.moveTo(bx - r * 1.10f, by + r * 0.30f)
                    st.path.quadTo(bx, by - r * 0.55f, bx + r * 1.10f, by + r * 0.30f)
                }
                Brows.NONE -> return
            }
            st.canvas.drawPath(st.path, st.stroke(sheet.ink, r * 0.30f))
        }
    }

    private fun drawEyes(st: Stage, p: Pose, sheet: SpriteSheet, s: Float) {
        if (p.eyes == Eyes.OPEN && p.gazeX == 0f && p.gazeY == 0f) return

        val r = sheet.eyeR * s
        val gx = p.gazeX * r * 0.30f
        val gy = p.gazeY * r * 0.30f
        val xs = floatArrayOf(lx(sheet.eyeLx, s), lx(sheet.eyeRx, s))
        val ys = floatArrayOf(ly(sheet.eyeLy, s), ly(sheet.eyeRy, s))
        val furs = intArrayOf(sheet.furEyeL, sheet.furEyeR)

        for (i in 0..1) {
            val bx = xs[i]
            val by = ys[i]
            val fur = furs[i]
            when (p.eyes) {
                Eyes.OPEN -> {
                    // gaze: cover the painted eye and redraw it shifted
                    patch(st, bx, by, r * 1.30f, fur)
                    st.oval(bx + gx, by + gy, r * 0.92f, r, sheet.ink)
                }
                Eyes.WIDE -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    st.oval(bx + gx, by + gy, r * 1.16f, r * 1.26f, sheet.ink)
                }
                Eyes.BLINK -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    st.path.reset()
                    st.path.moveTo(bx - r * 1.05f, by - r * 0.10f)
                    st.path.quadTo(bx, by + r * 0.62f, bx + r * 1.05f, by - r * 0.10f)
                    st.canvas.drawPath(st.path, st.stroke(sheet.ink, r * 0.34f))
                }
                Eyes.HAPPY -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    st.path.reset()
                    st.path.moveTo(bx - r * 1.05f, by + r * 0.40f)
                    st.path.quadTo(bx, by - r * 0.86f, bx + r * 1.05f, by + r * 0.40f)
                    st.canvas.drawPath(st.path, st.stroke(sheet.ink, r * 0.34f))
                }
                Eyes.SLEEPY -> {
                    patch(st, bx, by, r * 1.35f, fur)
                    st.path.reset()
                    st.path.moveTo(bx - r * 1.05f, by)
                    st.path.quadTo(bx, by + r * 0.78f, bx + r * 1.05f, by)
                    st.canvas.drawPath(st.path, st.stroke(sheet.ink, r * 0.32f))
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
                    val pt = st.stroke(sheet.ink, r * 0.26f)
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

}

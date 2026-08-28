package com.glasscrane.flannery.anim

import com.glasscrane.flannery.art.DEG
import com.glasscrane.flannery.art.Eyes
import com.glasscrane.flannery.art.Flannery
import com.glasscrane.flannery.art.Hue
import com.glasscrane.flannery.art.Mouth
import com.glasscrane.flannery.art.Pose
import com.glasscrane.flannery.art.Props
import com.glasscrane.flannery.art.Stage
import com.glasscrane.flannery.art.TAU
import com.glasscrane.flannery.art.clamp01
import com.glasscrane.flannery.art.ease
import com.glasscrane.flannery.art.lerp
import com.glasscrane.flannery.art.pulse
import com.glasscrane.flannery.art.rnd
import com.glasscrane.flannery.art.window
import com.glasscrane.flannery.art.withAlpha
import kotlin.math.cos
import kotlin.math.sin

class AnimSpec(
    val id: String,
    val title: String,
    val blurb: String,
    val bg: Int,
    val frames: Int,
    val fps: Int,
    val draw: (Stage, Float) -> Unit
)

/** Draws one frame of one animation, background included. Shared by the live view and the GIF encoder. */
object Renderer {
    fun render(st: Stage, spec: AnimSpec, t: Float) {
        st.canvas.drawColor(spec.bg)
        // soft pool of light he sits in
        st.oval(st.w * 0.5f, st.h * 0.58f, st.u * 0.46f, st.u * 0.44f, withAlpha(Hue.WHITE, 0.45f))
        spec.draw(st, t)
    }
}

private fun base(st: Stage): Pose {
    val p = st.pose
    p.reset(st.w * 0.5f, st.h * 0.54f, st.u * 0.50f)
    return p
}

private fun cube(x: Float) = x * x * x

object Animations {

    val all: List<AnimSpec> = listOf(

        // 1
        AnimSpec("idle", "Idle Squish", "just vibing", 0xFFFFF6EE.toInt(), 30, 20) { st, t ->
            val p = base(st)
            val b = sin(t * TAU)
            p.sy = 1f + 0.045f * b
            p.sx = 1f - 0.040f * b
            p.cy += st.u * 0.012f * b
            p.eyes = if (t > 0.44f && t < 0.52f) Eyes.BLINK else Eyes.OPEN
            p.blush = 0.35f
            Flannery.draw(st, p)
        },

        // 2
        AnimSpec("bounce", "Happy Bounce", "boing boing", 0xFFFFF1E4.toInt(), 30, 20) { st, t ->
            val p = base(st)
            val ph = (t * 2f) % 1f
            val air = sin(ph * 3.14159265f)
            val ground = cube(1f - air)
            p.cy -= st.u * 0.22f * air
            p.sx = 1f + 0.20f * ground - 0.07f * air
            p.sy = 1f - 0.20f * ground + 0.12f * air
            p.eyes = Eyes.HAPPY
            p.mouth = Mouth.SMILE
            p.blush = 0.55f
            p.shadow = 1f - 0.45f * air
            Flannery.draw(st, p)
        },

        // 3
        AnimSpec("wiggle", "Wiggle Wiggle", "happy shimmy", 0xFFFDF2F6.toInt(), 30, 20) { st, t ->
            val p = base(st)
            val s = sin(t * TAU * 2f)
            p.rot = 11f * s
            p.cx += st.u * 0.030f * s
            p.sy = 1f + 0.03f * cos(t * TAU * 2f)
            p.eyes = Eyes.HAPPY
            p.mouth = Mouth.SMILE
            p.blush = 0.6f
            Flannery.draw(st, p)
        },

        // 4
        AnimSpec("boop", "Blink & Boop", "right on the snoot", 0xFFF3F8FF.toInt(), 36, 20) { st, t ->
            val p = base(st)
            p.eyes = when {
                t > 0.08f && t < 0.15f -> Eyes.BLINK
                t > 0.21f && t < 0.28f -> Eyes.BLINK
                t > 0.60f && t < 0.72f -> Eyes.HAPPY
                else -> Eyes.OPEN
            }
            // a fingertip sparkle drops in and boops him at t = 0.60
            val drop = window(t, 0.38f, 0.60f)
            if (drop > 0f) {
                val y = lerp(st.h * 0.06f, p.cy - st.u * 0.30f, ease(drop))
                Props.sparkle(st, p.cx, y, st.u * 0.045f, Hue.GOLD)
            }
            val boop = window(t, 0.60f, 0.82f)
            if (boop > 0f) {
                val k = pulse(boop)
                p.sy = 1f - 0.13f * k
                p.sx = 1f + 0.13f * k
                p.blush = 0.35f + 0.65f * k
                for (i in 0 until 6) {
                    val a = -1.9f + i * 0.32f
                    val d = st.u * (0.14f + 0.16f * boop)
                    Props.sparkle(st, p.cx + cos(a) * d, p.cy - st.u * 0.22f + sin(a) * d,
                        st.u * 0.026f * (1f - boop), withAlpha(Hue.GOLD, 1f - boop))
                }
            } else {
                p.blush = 0.35f
            }
            Flannery.draw(st, p)
        },

        // 5
        AnimSpec("love", "Heart Eyes", "he loves you", 0xFFFFF0F3.toInt(), 30, 20) { st, t ->
            val p = base(st)
            for (i in 0 until 7) {
                val ht = (t + i / 7f) % 1f
                val x = p.cx + st.u * (0.30f * sin(ht * 3.4f + i * 1.7f) + 0.06f * (rnd(i) - 0.5f))
                val y = p.cy - st.u * (0.02f + 0.62f * ht)
                val a = clamp01(1.6f * (1f - ht)) * clamp01(ht * 6f)
                Props.heart(st, x, y, st.u * (0.030f + 0.026f * rnd(i + 20)), withAlpha(Hue.PINK, a))
            }
            val k = 1f + 0.055f * sin(t * TAU * 2f)
            p.sx = k; p.sy = k
            p.eyes = Eyes.HEART
            p.mouth = Mouth.SMILE
            p.blush = 0.9f
            Flannery.draw(st, p)
        },

        // 6
        AnimSpec("sleepy", "Sleepy Time", "shhh he's resting", 0xFFEFF1FB.toInt(), 36, 16) { st, t ->
            val p = base(st)
            val b = sin(t * TAU)
            p.sy = 1f + 0.055f * b
            p.sx = 1f - 0.045f * b
            p.rot = -7f
            p.eyes = Eyes.SLEEPY
            p.mouth = Mouth.TINY
            p.blush = 0.4f
            Flannery.draw(st, p)
            for (i in 0 until 3) {
                val zt = (t + i / 3f) % 1f
                val x = p.cx + st.u * (0.24f + 0.16f * zt)
                val y = p.cy - st.u * (0.20f + 0.40f * zt)
                Props.zzz(st, x, y, st.u * (0.07f + 0.05f * zt), clamp01(1.4f * (1f - zt)))
            }
        },

        // 7
        AnimSpec("sparkle", "Sparkle Pop", "extremely shiny", 0xFFFFFBEA.toInt(), 30, 20) { st, t ->
            val p = base(st)
            Props.glow(st, p.cx, p.cy, st.u * 0.60f, Hue.GOLD, 0.5f + 0.4f * sin(t * TAU))
            for (i in 0 until 10) {
                val phase = (t * 2f + rnd(i)) % 1f
                val a = rnd(i * 3) * TAU
                val d = st.u * (0.30f + 0.14f * rnd(i * 5))
                val s = pulse(phase)
                Props.sparkle(st, p.cx + cos(a) * d, p.cy + sin(a) * d * 0.85f,
                    st.u * 0.05f * s, withAlpha(Hue.GOLD, s))
            }
            val k = 1f + 0.04f * sin(t * TAU * 2f)
            p.sx = k; p.sy = k
            p.eyes = Eyes.HAPPY
            p.mouth = Mouth.SMILE
            p.blush = 0.5f
            Flannery.draw(st, p)
        },

        // 8 — no arms, so he greets you with a whole-body bow
        AnimSpec("hello", "Hi There", "a little bow", 0xFFF2FBF6.toInt(), 30, 20) { st, t ->
            val p = base(st)
            val nod = pulse(window(t, 0.05f, 0.35f)) + pulse(window(t, 0.40f, 0.70f))
            p.rot = -13f * nod
            p.cy += st.u * 0.030f * nod
            p.sy = 1f - 0.06f * nod
            p.sx = 1f + 0.06f * nod
            p.eyes = Eyes.HAPPY
            p.mouth = Mouth.SMILE
            val hi = window(t, 0.05f, 0.45f)
            if (hi > 0f) {
                Props.sparkle(st, p.cx + st.u * 0.30f, p.cy - st.u * (0.22f + 0.10f * hi),
                    st.u * 0.040f * pulse(hi), withAlpha(Hue.GOLD, pulse(hi)))
            }
            Flannery.draw(st, p)
        },

        // 9
        AnimSpec("snack", "Snack Time", "om nom nom", 0xFFFFF4E6.toInt(), 36, 18) { st, t ->
            val p = base(st)
            val bites = 3
            val phase = (t * bites) % 1f
            val chomp = phase < 0.35f
            p.mouth = if (chomp) Mouth.WIDE_OPEN else Mouth.SQUIGGLE
            p.eyes = if (chomp) Eyes.HAPPY else Eyes.OPEN
            p.sy = 1f - 0.035f * (if (chomp) 1f else 0f)
            p.blush = 0.5f
            val eaten = clamp01(t * 1.15f)
            p.rot = 5f * sin(t * TAU * bites)
            p.attach = { s, hw, hh ->
                Props.cookie(s, hw * 0.86f, -hh * 0.06f, hw * 0.26f, eaten)
            }
            Flannery.draw(st, p)
            for (i in 0 until 5) {
                val ct = (t * bites + rnd(i)) % 1f
                val x = p.cx + st.u * (0.06f + 0.10f * (rnd(i * 3) - 0.5f))
                val y = p.cy + st.u * (0.05f + 0.28f * ct)
                Props.crumb(st, x, y, st.u * 0.012f, clamp01(1f - ct))
            }
        },

        // 10
        AnimSpec("party", "Party Time", "confetti incoming", 0xFFFFF0F8.toInt(), 36, 20) { st, t ->
            val p = base(st)
            val ph = (t * 2f) % 1f
            val air = sin(ph * 3.14159265f)
            p.cy -= st.u * 0.14f * air
            p.sx = 1f + 0.12f * cube(1f - air)
            p.sy = 1f - 0.12f * cube(1f - air)
            p.rot = 6f * sin(t * TAU * 2f)
            p.eyes = Eyes.HAPPY
            p.mouth = Mouth.OPEN
            p.blush = 0.7f
            p.attach = { s, hw, hh -> Props.partyHat(s, hw * 0.06f, -hh * 0.78f, hw * 0.36f) }
            val colors = intArrayOf(Hue.PINK, Hue.GOLD, 0xFF7BC4FF.toInt(), 0xFF8BE08B.toInt(), 0xFFC79BFF.toInt())
            for (i in 0 until 16) {
                val ct = (t + rnd(i)) % 1f
                val x = st.w * rnd(i * 7)
                val y = -st.u * 0.10f + (st.h + st.u * 0.2f) * ct
                Props.confetti(st, x, y, st.u * 0.045f, ct * 540f + i * 37f,
                    withAlpha(colors[i % colors.size], clamp01(1.5f - ct)))
            }
            Flannery.draw(st, p)
        },

        // 11
        AnimSpec("dance", "Dance Party", "he's got moves", 0xFFF6F1FF.toInt(), 30, 20) { st, t ->
            val p = base(st)
            val s = sin(t * TAU * 2f)
            p.rot = 13f * s
            p.cx += st.u * 0.05f * s
            p.cy -= st.u * 0.03f * kotlin.math.abs(s)
            p.eyes = Eyes.HAPPY
            p.mouth = Mouth.OPEN
            p.blush = 0.6f
            for (i in 0 until 5) {
                val nt = (t + i / 5f) % 1f
                val side = if (i % 2 == 0) -1f else 1f
                val x = p.cx + side * st.u * (0.26f + 0.16f * nt)
                val y = p.cy - st.u * (0.10f + 0.48f * nt)
                Props.note(st, x, y, st.u * 0.055f,
                    withAlpha(0xFF7B6CFF.toInt(), clamp01(1.5f * (1f - nt))))
            }
            Flannery.draw(st, p)
        },

        // 12
        AnimSpec("peekaboo", "Peek-a-Boo", "there he is", 0xFFEFF7FF.toInt(), 36, 18) { st, t ->
            val p = base(st)
            val up = when {
                t < 0.22f -> ease(t / 0.22f)
                t < 0.74f -> 1f
                else -> 1f - ease((t - 0.74f) / 0.26f)
            }
            p.cy = lerp(st.h + st.u * 0.66f, st.h * 0.54f, up)
            p.sy = 1f + 0.07f * (1f - up)
            p.eyes = if (up > 0.85f) Eyes.WIDE else Eyes.OPEN
            p.mouth = if (up > 0.85f) Mouth.OPEN else Mouth.TINY
            p.blush = 0.6f * up
            p.shadow = 0f
            if (up > 0.9f) {
                val k = window(t, 0.30f, 0.55f)
                if (k > 0f) {
                    for (i in 0 until 5) {
                        val a = -2.3f + i * 0.42f
                        val d = st.u * (0.30f + 0.12f * k)
                        Props.sparkle(st, p.cx + cos(a) * d, p.cy + sin(a) * d,
                            st.u * 0.032f * (1f - k), withAlpha(Hue.GOLD, 1f - k))
                    }
                }
            }
            Flannery.draw(st, p)
        },

        // 13
        AnimSpec("roll", "Barrel Roll", "wheee", 0xFFF1F9F7.toInt(), 30, 20) { st, t ->
            val p = base(st)
            p.cx = -st.u * 0.42f + (st.w + st.u * 0.84f) * t
            p.rot = t * 720f
            p.cy += st.u * 0.02f * sin(t * TAU * 4f)
            p.eyes = Eyes.HAPPY
            p.mouth = Mouth.OPEN
            p.blush = 0.6f
            Flannery.draw(st, p)
        },

        // 14
        AnimSpec("rainbow", "Rainbow Mood", "pure serotonin", 0xFFF7FBFF.toInt(), 36, 20) { st, t ->
            val p = base(st)
            val sweep = ease(clamp01(t * 2.2f))
            val fade = if (t > 0.82f) 1f - (t - 0.82f) / 0.18f else 1f
            Props.rainbow(st, st.w * 0.5f, st.h * 0.66f, st.u * 0.37f, sweep, fade)
            p.gazeY = -0.7f
            p.eyes = Eyes.HAPPY
            p.mouth = Mouth.SMILE
            p.blush = 0.6f
            p.sy = 1f + 0.03f * sin(t * TAU)
            Flannery.draw(st, p)
            for (i in 0 until 6) {
                val s = pulse((t * 2f + rnd(i)) % 1f)
                val a = 3.6f + rnd(i * 3) * 2.2f
                Props.sparkle(st, st.w * 0.5f + cos(a) * st.u * 0.40f,
                    st.h * 0.66f + sin(a) * st.u * 0.40f,
                    st.u * 0.035f * s, withAlpha(Hue.WHITE, s))
            }
        },

        // 15
        AnimSpec("rain", "Rainy Blep", "a small sad", 0xFFEDF1F5.toInt(), 30, 20) { st, t ->
            val p = base(st)
            val cx = st.w * 0.5f
            val cy = st.h * 0.16f
            Props.cloud(st, cx, cy, st.u * 0.20f, 0xFFB9C4CE.toInt())
            for (i in 0 until 9) {
                val dt = (t * 1.6f + rnd(i)) % 1f
                val x = cx + st.u * 0.26f * (rnd(i * 5) - 0.5f) * 2f
                val y = cy + st.u * (0.12f + 0.42f * dt)
                val pt = st.stroke(withAlpha(0xFF7FA6C4.toInt(), clamp01(1.4f * (1f - dt))), st.u * 0.011f)
                st.canvas.drawLine(x, y, x, y + st.u * 0.05f, pt)
            }
            p.rot = -5f + 2f * sin(t * TAU)
            p.cy += st.u * 0.015f * sin(t * TAU)
            p.eyes = Eyes.SLEEPY
            p.mouth = if (t > 0.45f) Mouth.BLEP else Mouth.FROWN
            p.gazeY = 0.5f
            p.blush = 0.35f
            Flannery.draw(st, p)
        },

        // 16
        AnimSpec("snow", "Cozy Snow", "scarf weather", 0xFFF0F6FB.toInt(), 36, 18) { st, t ->
            val p = base(st)
            p.rot = 4f * sin(t * TAU)
            p.sy = 1f + 0.03f * sin(t * TAU)
            p.eyes = Eyes.HAPPY
            p.mouth = Mouth.SMILE
            p.blush = 0.85f
            p.attach = { s, hw, hh -> Props.beanie(s, hw, hh) }
            Flannery.draw(st, p)
            for (i in 0 until 14) {
                val ft = (t + rnd(i)) % 1f
                val x = st.w * rnd(i * 11) + st.u * 0.05f * sin(ft * TAU + i)
                val y = -st.u * 0.05f + (st.h + st.u * 0.1f) * ft
                Props.snowflake(st, x, y, st.u * (0.016f + 0.014f * rnd(i * 3)),
                    withAlpha(0xFFFFFFFF.toInt(), 0.9f))
            }
        },

        // 17
        AnimSpec("flame", "Tiny Flame", "he is a dragon actually", 0xFFFFF3EC.toInt(), 36, 20) { st, t ->
            val p = base(st)
            val inhale = window(t, 0.00f, 0.34f)
            val blow = window(t, 0.34f, 0.62f)
            if (blow > 0f) {
                p.mouth = Mouth.WIDE_OPEN
                p.eyes = Eyes.WIDE
                p.sx = 1f - 0.06f * pulse(blow)
                p.sy = 1f + 0.06f * pulse(blow)
            } else {
                p.mouth = Mouth.TINY
                p.eyes = if (inhale > 0.5f) Eyes.BLINK else Eyes.OPEN
                p.sx = 1f + 0.07f * inhale
                p.sy = 1f + 0.07f * inhale
            }
            p.blush = 0.4f + 0.5f * blow
            Flannery.draw(st, p)
            if (blow > 0f) {
                for (i in 0 until 3) {
                    val ft = clamp01(blow * 1.4f - i * 0.18f)
                    if (ft <= 0f) continue
                    val y = p.cy + st.u * 0.02f - st.u * 0.34f * ft
                    val x = p.cx + st.u * 0.05f * sin(ft * 5f + i)
                    Props.flame(st, x, y, st.u * (0.09f - 0.03f * ft) * (1f - i * 0.18f),
                        clamp01(1.3f * (1f - ft)))
                }
            }
        },

        // 18
        AnimSpec("bubbles", "Bubble Blow", "blub blub", 0xFFEFF9FF.toInt(), 36, 20) { st, t ->
            val p = base(st)
            p.mouth = Mouth.OPEN
            p.eyes = Eyes.HAPPY
            p.blush = 0.55f
            p.sy = 1f + 0.03f * sin(t * TAU)
            Flannery.draw(st, p)
            for (i in 0 until 7) {
                val bt = (t + i / 7f) % 1f
                val r = st.u * (0.022f + 0.038f * rnd(i))
                val x = p.cx + st.u * (0.05f + 0.26f * bt) + st.u * 0.05f * sin(bt * 6f + i)
                val y = p.cy + st.u * 0.02f - st.u * 0.55f * bt
                if (bt > 0.86f) {
                    val pop = (bt - 0.86f) / 0.14f
                    for (k in 0 until 6) {
                        val a = k * (TAU / 6f)
                        val d = r * (1f + 2.2f * pop)
                        st.circle(x + cos(a) * d, y + sin(a) * d, r * 0.16f * (1f - pop),
                            withAlpha(0xFF8FD8FF.toInt(), 1f - pop))
                    }
                } else {
                    Props.bubble(st, x, y, r, clamp01(bt * 8f))
                }
            }
        },

        // 19
        AnimSpec("yawn", "Big Yawn", "long day", 0xFFFDF4FF.toInt(), 36, 16) { st, t ->
            val p = base(st)
            val rise = window(t, 0.10f, 0.42f)
            val hold = window(t, 0.42f, 0.62f)
            val settle = window(t, 0.62f, 1.00f)
            when {
                hold > 0f -> {
                    p.sy = 1.16f
                    p.sx = 0.90f
                    p.cy -= st.u * 0.05f
                    p.mouth = Mouth.YAWN
                    p.eyes = Eyes.BLINK
                }
                rise > 0f -> {
                    val e = ease(rise)
                    p.sy = lerp(1f, 1.16f, e)
                    p.sx = lerp(1f, 0.90f, e)
                    p.cy -= st.u * 0.05f * e
                    p.mouth = if (rise > 0.5f) Mouth.YAWN else Mouth.OPEN
                    p.eyes = Eyes.BLINK
                }
                else -> {
                    val e = ease(settle)
                    p.sy = lerp(1.16f, 1f, e)
                    p.sx = lerp(0.90f, 1f, e)
                    p.cy -= st.u * 0.05f * (1f - e)
                    p.mouth = if (settle > 0.55f) Mouth.TINY else Mouth.OPEN
                    p.eyes = if (settle > 0.55f) Eyes.SLEEPY else Eyes.BLINK
                }
            }
            p.blush = 0.45f
            Flannery.draw(st, p)
            val za = clamp01((t - 0.55f) / 0.45f)
            if (za > 0f) {
                Props.zzz(st, p.cx + st.u * (0.26f + 0.10f * za), p.cy - st.u * (0.22f + 0.26f * za),
                    st.u * (0.07f + 0.03f * za), clamp01(1.6f * (1f - za)))
            }
        },

        // 20
        AnimSpec("stars", "Star Gazing", "make a wish", 0xFFF1F0FA.toInt(), 36, 18) { st, t ->
            val p = base(st)
            for (i in 0 until 12) {
                val s = pulse((t * 1.5f + rnd(i)) % 1f)
                val x = st.w * (0.08f + 0.84f * rnd(i * 3))
                val y = st.h * (0.05f + 0.32f * rnd(i * 7))
                Props.star(st, x, y, st.u * (0.018f + 0.022f * rnd(i * 5)) * (0.5f + 0.5f * s),
                    withAlpha(Hue.GOLD, 0.35f + 0.65f * s), 4)
            }
            // one shooting star
            val sh = window(t, 0.30f, 0.62f)
            if (sh > 0f) {
                val x = lerp(st.w * 0.18f, st.w * 0.86f, sh)
                val y = lerp(st.h * 0.10f, st.h * 0.26f, sh)
                val pt = st.stroke(withAlpha(Hue.WHITE, (1f - sh) * 0.9f), st.u * 0.012f)
                st.canvas.drawLine(x - st.u * 0.14f, y - st.u * 0.05f, x, y, pt)
                Props.star(st, x, y, st.u * 0.030f, withAlpha(Hue.GOLD, 1f - sh * 0.4f), 4)
            }
            p.gazeY = -0.85f
            p.eyes = if (t > 0.70f) Eyes.STAR else Eyes.OPEN
            p.mouth = if (t > 0.70f) Mouth.SMILE else Mouth.TINY
            p.blush = 0.4f + 0.4f * clamp01((t - 0.70f) / 0.30f)
            p.sy = 1f + 0.025f * sin(t * TAU)
            Flannery.draw(st, p)
        }
    )

    fun byId(id: String): AnimSpec = all.firstOrNull { it.id == id } ?: all[0]
}

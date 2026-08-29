package com.glasscrane.flannery.anim

import com.glasscrane.flannery.art.DEG
import com.glasscrane.flannery.art.Eyes
import com.glasscrane.flannery.art.Flannery
import com.glasscrane.flannery.art.Hue
import com.glasscrane.flannery.art.Brows
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
    val frames: Int,
    val fps: Int,
    val draw: (Stage, Float) -> Unit
)

/** Draws one frame of one animation, background included. Shared by the live view and the GIF encoder. */
/**
 * Draws one frame. There is no background layer and no soft shading: GIF alpha
 * is 1-bit, so anything semi-transparent would either vanish or harden into a
 * block. The same frames drive the on-screen preview and the exported GIF.
 */
object Renderer {
    fun render(st: Stage, spec: AnimSpec, t: Float) = spec.draw(st, t)
}

private fun base(st: Stage): Pose {
    val p = st.pose
    p.reset(st.w * 0.5f, st.h * 0.52f, st.u * 0.66f)
    return p
}

private fun cube(x: Float) = x * x * x

object Animations {

    val all: List<AnimSpec> = listOf(

        // 1
        AnimSpec("idle", "Idle Squish", "just vibing", 30, 20) { st, t ->
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
        AnimSpec("bounce", "Happy Bounce", "boing boing", 30, 20) { st, t ->
            val p = base(st)
            val ph = (t * 2f) % 1f
            val air = sin(ph * 3.14159265f)
            val ground = cube(1f - air)
            p.cy -= st.u * 0.22f * air
            p.sx = 1f + 0.20f * ground - 0.07f * air
            p.sy = 1f - 0.20f * ground + 0.12f * air
            p.eyes = Eyes.HAPPY
            p.blush = 0.55f
            p.shadow = 1f - 0.45f * air
            Flannery.draw(st, p)
        },

        // 3
        AnimSpec("wiggle", "Wiggle Wiggle", "happy shimmy", 30, 20) { st, t ->
            val p = base(st)
            val s = sin(t * TAU * 2f)
            p.rot = 11f * s
            p.cx += st.u * 0.030f * s
            p.sy = 1f + 0.03f * cos(t * TAU * 2f)
            p.eyes = Eyes.HAPPY
            p.blush = 0.6f
            Flannery.draw(st, p)
        },

        // 4
        AnimSpec("boop", "Blink & Boop", "right on the snoot", 36, 20) { st, t ->
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
        AnimSpec("love", "Heart Eyes", "he loves you", 30, 20) { st, t ->
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
            p.blush = 0.9f
            Flannery.draw(st, p)
        },

        // 6
        AnimSpec("sleepy", "Sleepy Time", "shhh he's resting", 36, 16) { st, t ->
            val p = base(st)
            val b = sin(t * TAU)
            p.sy = 1f + 0.055f * b
            p.sx = 1f - 0.045f * b
            p.rot = -7f
            p.eyes = Eyes.SLEEPY
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
        AnimSpec("sparkle", "Sparkle Pop", "extremely shiny", 30, 20) { st, t ->
            val p = base(st)
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
            p.blush = 0.5f
            Flannery.draw(st, p)
        },

        // 8 — no arms, so he greets you with a whole-body bow
        AnimSpec("hello", "Hi There", "a little bow", 30, 20) { st, t ->
            val p = base(st)
            val nod = pulse(window(t, 0.05f, 0.35f)) + pulse(window(t, 0.40f, 0.70f))
            p.rot = -13f * nod
            p.cy += st.u * 0.030f * nod
            p.sy = 1f - 0.06f * nod
            p.sx = 1f + 0.06f * nod
            p.eyes = Eyes.HAPPY
            val hi = window(t, 0.05f, 0.45f)
            if (hi > 0f) {
                Props.sparkle(st, p.cx + st.u * 0.30f, p.cy - st.u * (0.22f + 0.10f * hi),
                    st.u * 0.040f * pulse(hi), withAlpha(Hue.GOLD, pulse(hi)))
            }
            Flannery.draw(st, p)
        },

        // 9
        AnimSpec("snack", "Snack Time", "om nom nom", 36, 18) { st, t ->
            val p = base(st)
            val bites = 3
            val phase = (t * bites) % 1f
            val chomp = phase < 0.35f
            p.eyes = if (chomp) Eyes.HAPPY else Eyes.OPEN
            p.sy = 1f - 0.035f * (if (chomp) 1f else 0f)
            p.blush = 0.5f
            val eaten = clamp01(t * 1.15f)
            p.rot = 5f * sin(t * TAU * bites)
            p.attach = { s, hw, hh ->
                Props.cookie(s, hw * 0.88f, hh * 0.14f, hw * 0.24f, eaten)
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
        AnimSpec("party", "Party Time", "confetti incoming", 36, 20) { st, t ->
            val p = base(st)
            val ph = (t * 2f) % 1f
            val air = sin(ph * 3.14159265f)
            p.cy -= st.u * 0.14f * air
            p.sx = 1f + 0.12f * cube(1f - air)
            p.sy = 1f - 0.12f * cube(1f - air)
            p.rot = 6f * sin(t * TAU * 2f)
            p.eyes = Eyes.HAPPY
            p.blush = 0.7f
            p.attach = { s, hw, hh -> Props.partyHat(s, hw * 0.06f, -hh * 0.62f, hw * 0.31f) }
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
        AnimSpec("dance", "Dance Party", "he's got moves", 30, 20) { st, t ->
            val p = base(st)
            val s = sin(t * TAU * 2f)
            p.rot = 13f * s
            p.cx += st.u * 0.05f * s
            p.cy -= st.u * 0.03f * kotlin.math.abs(s)
            p.eyes = Eyes.HAPPY
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
        AnimSpec("peekaboo", "Peek-a-Boo", "there he is", 36, 18) { st, t ->
            val p = base(st)
            val up = when {
                t < 0.22f -> ease(t / 0.22f)
                t < 0.74f -> 1f
                else -> 1f - ease((t - 0.74f) / 0.26f)
            }
            p.cy = lerp(st.h + st.u * 0.66f, st.h * 0.54f, up)
            p.sy = 1f + 0.07f * (1f - up)
            p.eyes = if (up > 0.85f) Eyes.WIDE else Eyes.OPEN
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
        AnimSpec("roll", "Barrel Roll", "wheee", 30, 20) { st, t ->
            val p = base(st)
            p.cx = -st.u * 0.42f + (st.w + st.u * 0.84f) * t
            p.rot = t * 720f
            p.cy += st.u * 0.02f * sin(t * TAU * 4f)
            p.eyes = Eyes.HAPPY
            p.blush = 0.6f
            Flannery.draw(st, p)
        },

        // 14
        AnimSpec("rainbow", "Rainbow Mood", "pure serotonin", 36, 20) { st, t ->
            val p = base(st)
            val sweep = ease(clamp01(t * 2.2f))
            val fade = if (t > 0.82f) 1f - (t - 0.82f) / 0.18f else 1f
            Props.rainbow(st, st.w * 0.5f, st.h * 0.74f, st.u * 0.55f, sweep, fade)
            p.gazeY = -0.7f
            p.eyes = Eyes.HAPPY
            p.blush = 0.6f
            p.sy = 1f + 0.03f * sin(t * TAU)
            Flannery.draw(st, p)
            for (i in 0 until 6) {
                val s = pulse((t * 2f + rnd(i)) % 1f)
                val a = 3.6f + rnd(i * 3) * 2.2f
                Props.sparkle(st, st.w * 0.5f + cos(a) * st.u * 0.58f,
                    st.h * 0.74f + sin(a) * st.u * 0.58f,
                    st.u * 0.035f * s, withAlpha(Hue.WHITE, s))
            }
        },

        // 15
        AnimSpec("rain", "Rainy Blep", "a small sad", 30, 20) { st, t ->
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
            p.gazeY = 0.5f
            p.blush = 0.35f
            Flannery.draw(st, p)
        },

        // 16
        AnimSpec("snow", "Cozy Snow", "scarf weather", 36, 18) { st, t ->
            val p = base(st)
            p.rot = 4f * sin(t * TAU)
            p.sy = 1f + 0.03f * sin(t * TAU)
            p.eyes = Eyes.HAPPY
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
        AnimSpec("flame", "Tiny Flame", "he is a dragon actually", 36, 20) { st, t ->
            val p = base(st)
            val inhale = window(t, 0.00f, 0.34f)
            val blow = window(t, 0.34f, 0.62f)
            if (blow > 0f) {
                p.eyes = Eyes.WIDE
                p.sx = 1f - 0.06f * pulse(blow)
                p.sy = 1f + 0.06f * pulse(blow)
            } else {
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
        AnimSpec("bubbles", "Bubble Blow", "blub blub", 36, 20) { st, t ->
            val p = base(st)
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
        AnimSpec("yawn", "Big Yawn", "long day", 36, 16) { st, t ->
            val p = base(st)
            val rise = window(t, 0.10f, 0.42f)
            val hold = window(t, 0.42f, 0.62f)
            val settle = window(t, 0.62f, 1.00f)
            when {
                hold > 0f -> {
                    p.sy = 1.16f
                    p.sx = 0.90f
                    p.cy -= st.u * 0.05f
                    p.eyes = Eyes.BLINK
                }
                rise > 0f -> {
                    val e = ease(rise)
                    p.sy = lerp(1f, 1.16f, e)
                    p.sx = lerp(1f, 0.90f, e)
                    p.cy -= st.u * 0.05f * e
                    p.eyes = Eyes.BLINK
                }
                else -> {
                    val e = ease(settle)
                    p.sy = lerp(1.16f, 1f, e)
                    p.sx = lerp(0.90f, 1f, e)
                    p.cy -= st.u * 0.05f * (1f - e)
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
        AnimSpec("stars", "Star Gazing", "make a wish", 36, 18) { st, t ->
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
            p.blush = 0.4f + 0.4f * clamp01((t - 0.70f) / 0.30f)
            p.sy = 1f + 0.025f * sin(t * TAU)
            Flannery.draw(st, p)
        }    ,

        // ---- 21-40 ----------------------------------------------------------

        // 21
        AnimSpec("rocket", "Ratty Rocketship", "3… 2… 1…", 44, 20) { st, t ->
            val p = base(st)
            val ground = st.h * 0.52f
            val charge = window(t, 0.00f, 0.46f)
            val crouch = window(t, 0.46f, 0.56f)
            val launch = window(t, 0.56f, 1.00f)

            if (launch > 0f) {
                // eased so he leaves the pad slowly and keeps building
                val rise = launch * launch * (0.30f + 0.70f * launch)
                p.cy = ground - st.u * 1.15f * rise
                p.sy = 1f + 0.13f * launch
                p.sx = 1f - 0.08f * launch
                p.eyes = Eyes.WIDE
                p.shadow = 0f
                val jet = st.u * (0.22f + 0.58f * ease(clamp01(launch * 2.2f)))
                Props.flameJet(st, p.cx, p.cy + st.u * 0.30f, st.u * 0.155f, jet,
                    (t * 44f).toInt(), 1f)
                for (i in 0 until 8) {
                    val f = (rnd(i * 5) + launch * 1.4f) % 1f
                    st.circle(p.cx + st.u * 0.22f * (rnd(i) - 0.5f),
                        ground + st.u * (0.18f + 0.55f * f), st.u * 0.05f * (1f - f) + st.u * 0.02f,
                        withAlpha(0xFFCBD3D8.toInt(), 0.5f * (1f - f)))
                }
            } else if (crouch > 0f) {
                p.cy = ground + st.u * 0.02f * crouch
                p.sy = 1f - 0.10f * pulse(crouch)
                p.sx = 1f + 0.10f * pulse(crouch)
                p.eyes = Eyes.BLINK
                Props.flameJet(st, p.cx, p.cy + st.u * 0.30f, st.u * 0.125f, st.u * 0.16f,
                    (t * 44f).toInt(), 0.9f)
            } else {
                // shaking harder and harder on the pad
                val k = charge * charge
                p.cx += st.u * 0.020f * k * sin(t * 150f)
                p.cy = ground + st.u * 0.008f * k * sin(t * 121f)
                p.sx = 1f + 0.03f * k
                p.sy = 1f - 0.03f * k
                p.eyes = if (charge > 0.55f) Eyes.WIDE else Eyes.OPEN
                Props.flameJet(st, p.cx, p.cy + st.u * 0.30f, st.u * 0.10f * k,
                    st.u * 0.18f * k, (t * 44f).toInt(), k)
            }
            Flannery.draw(st, p)
        },

        // 22
        AnimSpec("rarf", "RARF!!!!", "small guy, big opinion", 36, 20) { st, t ->
            val p = base(st)
            val wind = window(t, 0.00f, 0.22f)
            val bark = window(t, 0.22f, 0.62f)

            if (bark > 0f) {
                val hit = pulse(bark)
                p.sx = 1f + 0.13f * hit
                p.sy = 1f - 0.11f * hit
                p.cx += st.u * 0.016f * sin(t * 150f) * hit
                p.cy += st.u * 0.020f * hit
            } else {
                p.sy = 1f + 0.07f * wind
                p.sx = 1f - 0.05f * wind
                p.cy -= st.u * 0.02f * wind
            }
            p.eyes = Eyes.WIDE
            p.brows = Brows.ANGRY
            Flannery.draw(st, p)

            if (bark > 0f) {
                val pop = ease(clamp01(bark * 3.2f))
                val fade = if (bark > 0.72f) 1f - (bark - 0.72f) / 0.28f else 1f
                // impact rays behind the shout
                for (i in 0 until 10) {
                    val a = i / 10f * TAU
                    val d0 = st.u * (0.30f + 0.06f * pop)
                    val d1 = d0 + st.u * 0.10f * pop
                    val pt = st.stroke(withAlpha(0xFFE8613F.toInt(), 0.45f * fade), st.u * 0.012f)
                    st.canvas.drawLine(st.w * 0.5f + cos(a) * d0, st.h * 0.28f + sin(a) * d0 * 0.6f,
                        st.w * 0.5f + cos(a) * d1, st.h * 0.28f + sin(a) * d1 * 0.6f, pt)
                }
                Props.shout(st, st.w * 0.5f, st.h * 0.30f, st.u * (0.105f + 0.030f * pop),
                    "RARF!!!!", 0xFFE8452A.toInt(), Hue.CREAM, fade)
            }
        },

        // 23
        AnimSpec("zoomies", "Zoomies", "he has the zoomies", 30, 22) { st, t ->
            val p = base(st)
            val sweep = sin(t * TAU)
            p.cx = st.w * 0.5f + st.u * 0.30f * sweep
            p.rot = -12f * sweep
            p.cy -= st.u * 0.02f * kotlin.math.abs(sweep)
            p.eyes = Eyes.HAPPY
            for (i in 0 until 5) {
                val dir = if (sweep > 0f) -1f else 1f
                Props.speedLine(st, p.cx + dir * st.u * (0.20f + 0.06f * i),
                    p.cy - st.u * 0.14f + st.u * 0.07f * i,
                    dir * st.u * 0.16f, st.u * 0.012f, 1f - i * 0.16f)
            }
            Flannery.draw(st, p)
        },

        // 24
        AnimSpec("hug", "Squish Hug", "squeezed", 30, 20) { st, t ->
            val p = base(st)
            val sq = pulse(window(t, 0.10f, 0.70f))
            p.sx = 1f + 0.20f * sq
            p.sy = 1f - 0.16f * sq
            p.cy += st.u * 0.03f * sq
            p.eyes = Eyes.HAPPY
            p.blush = 0.5f + 0.5f * sq
            Flannery.draw(st, p)
            for (i in 0 until 5) {
                val ht = (t + i / 5f) % 1f
                Props.heart(st, p.cx + st.u * 0.26f * sin(ht * 3.6f + i),
                    p.cy - st.u * (0.10f + 0.48f * ht), st.u * 0.036f,
                    withAlpha(Hue.PINK, clamp01(1.5f * (1f - ht)) * sq))
            }
        },

        // 25
        AnimSpec("coffee", "Coffee Buzz", "one too many", 30, 24) { st, t ->
            val p = base(st)
            p.cx += st.u * 0.010f * sin(t * 190f)
            p.cy += st.u * 0.007f * sin(t * 233f)
            p.rot = 3f * sin(t * 170f)
            p.eyes = Eyes.WIDE
            p.attach = { s, hw, _ -> Props.cookie(s, hw * 0.92f, hw * 0.10f, hw * 0.20f, 0f) }
            Flannery.draw(st, p)
            for (i in 0 until 3) {
                val st2 = (t * 1.6f + i / 3f) % 1f
                st.circle(p.cx + st.u * 0.30f + st.u * 0.03f * sin(st2 * 7f),
                    p.cy - st.u * (0.02f + 0.24f * st2), st.u * 0.022f * (1f - st2 * 0.4f),
                    withAlpha(Hue.WHITE, 0.7f * (1f - st2)))
            }
        },

        // 26
        AnimSpec("boo", "Boo!", "spooky little guy", 36, 18) { st, t ->
            val p = base(st)
            p.cy -= st.u * 0.04f * sin(t * TAU)
            p.rot = 4f * sin(t * TAU)
            p.shadow = 0f
            p.eyes = Eyes.WIDE
            Flannery.draw(st, p)
            val boo = window(t, 0.55f, 0.95f)
            if (boo > 0f) {
                Props.shout(st, st.w * 0.72f, st.h * 0.26f, st.u * 0.11f, "boo",
                    0xFFDCE8FF.toInt(), 0xFF2A3145.toInt(), clamp01(1.6f * (1f - boo)))
            }
        },

        // 27
        AnimSpec("trampoline", "Trampoline", "higher! higher!", 30, 22) { st, t ->
            val p = base(st)
            val air = sin(clamp01(t) * 3.14159265f * 2f)
            val up = kotlin.math.abs(air)
            p.cy = st.h * 0.56f - st.u * 0.34f * up
            val land = 1f - up
            p.sx = 1f + 0.22f * land * land * land
            p.sy = 1f - 0.22f * land * land * land + 0.14f * up
            p.eyes = Eyes.HAPPY
            p.shadow = 0.3f + 0.7f * land
            val pt = st.stroke(withAlpha(Hue.INK, 0.25f), st.u * 0.012f)
            val sag = st.u * 0.03f * land
            st.rect.set(st.w * 0.5f - st.u * 0.34f, st.h * 0.72f - st.u * 0.03f + sag,
                st.w * 0.5f + st.u * 0.34f, st.h * 0.72f + st.u * 0.03f + sag)
            st.canvas.drawOval(st.rect, pt)
            Flannery.draw(st, p)
        },

        // 28
        AnimSpec("snowball", "Snowball", "gathering momentum", 36, 20) { st, t ->
            val p = base(st)
            p.cx = -st.u * 0.36f + (st.w + st.u * 0.72f) * t
            p.size = st.u * (0.42f + 0.34f * t)
            p.rot = t * 540f
            p.eyes = Eyes.HAPPY
            for (i in 0 until 10) {
                val ft = (t + rnd(i)) % 1f
                Props.snowflake(st, st.w * rnd(i * 11), (st.h + st.u * 0.1f) * ft,
                    st.u * 0.016f, withAlpha(Hue.WHITE, 0.85f))
            }
            Flannery.draw(st, p)
        },

        // 29
        AnimSpec("windy", "Windy Day", "hold on", 30, 20) { st, t ->
            val p = base(st)
            val gust = 0.6f + 0.4f * sin(t * TAU * 2f)
            p.rot = -13f * gust
            p.sx = 1f + 0.05f * gust
            p.eyes = Eyes.BLINK
            Flannery.draw(st, p)
            val leaves = intArrayOf(0xFFD9A15A.toInt(), 0xFFC8763C.toInt(), 0xFFB9C46A.toInt())
            for (i in 0 until 9) {
                val lt = (t * 1.5f + rnd(i)) % 1f
                Props.leaf(st, -st.u * 0.1f + (st.w + st.u * 0.2f) * lt,
                    st.h * (0.18f + 0.6f * rnd(i * 7)) + st.u * 0.06f * sin(lt * 9f + i),
                    st.u * 0.030f, lt * 420f + i * 40f,
                    withAlpha(leaves[i % 3], 0.9f))
            }
        },

        // 30
        AnimSpec("sunbathe", "Sunbathing", "vitamin D", 36, 16) { st, t ->
            val p = base(st)
            p.cy += st.u * 0.012f * sin(t * TAU)
            p.rot = 2f * sin(t * TAU)
            p.eyes = Eyes.OPEN
            Props.sun(st, st.w * 0.80f, st.h * 0.18f, st.u * 0.09f, t * 1.6f)
            p.attach = { s, hw, hh ->
                Props.sunglasses(s, hw, 0f, -hh * 0.14f, hw * 0.11f)
            }
            Flannery.draw(st, p)
        },

        // 31
        AnimSpec("zap", "Static Shock", "bzzt", 30, 24) { st, t ->
            val p = base(st)
            val z = window(t, 0.20f, 0.55f)
            p.cx += st.u * 0.012f * sin(t * 210f) * (if (z > 0f) 1f else 0.25f)
            p.cy += st.u * 0.008f * sin(t * 260f)
            p.eyes = if (z > 0f) Eyes.DIZZY else Eyes.WIDE
            p.brows = if (z > 0f) Brows.RAISED else Brows.NONE
            Flannery.draw(st, p)
            if (z > 0f) {
                for (i in 0 until 4) {
                    val a = -2.4f + i * 0.55f
                    val d = st.u * 0.36f
                    Props.bolt(st, p.cx + cos(a) * d, p.cy + sin(a) * d,
                        st.u * 0.055f, pulse(z))
                }
            }
        },

        // 32
        AnimSpec("levitate", "Levitate", "inner peace", 36, 16) { st, t ->
            val p = base(st)
            val f = 0.5f + 0.5f * sin(t * TAU)
            p.cy -= st.u * 0.10f * f
            p.shadow = 0.25f + 0.35f * (1f - f)
            p.eyes = Eyes.SLEEPY
            val pt = st.stroke(withAlpha(0xFFB9A6FF.toInt(), 0.45f), st.u * 0.010f)
            st.rect.set(p.cx - st.u * 0.34f, p.cy + st.u * (0.30f + 0.05f * f) - st.u * 0.05f,
                p.cx + st.u * 0.34f, p.cy + st.u * (0.30f + 0.05f * f) + st.u * 0.05f)
            st.canvas.drawOval(st.rect, pt)
            Flannery.draw(st, p)
        },

        // 33
        AnimSpec("fireworks", "Fireworks", "ooooh", 44, 18) { st, t ->
            val p = base(st)
            p.gazeY = -0.8f
            p.eyes = Eyes.WIDE
            val colors = intArrayOf(0xFFFF8A9B.toInt(), 0xFFFFE066.toInt(), 0xFF8FD8FF.toInt(), 0xFFC7A6FF.toInt())
            for (i in 0 until 4) {
                val ft = (t * 1.4f + i * 0.27f) % 1f
                Props.firework(st, st.w * (0.20f + 0.20f * i), st.h * (0.16f + 0.10f * rnd(i * 5)),
                    st.u * 0.20f, ft, colors[i])
            }
            Flannery.draw(st, p)
        },

        // 34
        AnimSpec("birthday", "Birthday", "make a wish", 36, 18) { st, t ->
            val p = base(st)
            p.cy -= st.u * 0.015f * sin(t * TAU * 2f)
            p.eyes = Eyes.HAPPY
            p.blush = 0.6f
            Flannery.draw(st, p)
            Props.cake(st, st.w * 0.5f, st.h * 0.80f, st.u * 0.11f, sin(t * 22f))
            for (i in 0 until 10) {
                val ct = (t + rnd(i)) % 1f
                Props.confetti(st, st.w * rnd(i * 7), -st.u * 0.08f + (st.h + st.u * 0.16f) * ct,
                    st.u * 0.038f, ct * 460f, withAlpha(
                        intArrayOf(Hue.PINK, Hue.GOLD, 0xFF8FD8FF.toInt())[i % 3],
                        clamp01(1.4f - ct)))
            }
        },

        // 35
        AnimSpec("puddle", "Puddle Splash", "worth it", 36, 20) { st, t ->
            val p = base(st)
            val jump = window(t, 0.00f, 0.42f)
            val land = window(t, 0.42f, 1.00f)
            if (jump > 0f) {
                p.cy = st.h * 0.52f - st.u * 0.26f * pulse(jump)
                p.sy = 1f + 0.10f * pulse(jump)
                p.eyes = Eyes.HAPPY
                p.shadow = 0.4f
            } else {
                val k = 1f - ease(clamp01(land * 2.5f))
                p.cy = st.h * 0.52f
                p.sx = 1f + 0.18f * k
                p.sy = 1f - 0.18f * k
                p.eyes = Eyes.HAPPY
            }
            Flannery.draw(st, p)
            if (land > 0f) Props.splash(st, p.cx, st.h * 0.74f, st.u * 0.26f, clamp01(land * 1.8f))
        },

        // 36
        AnimSpec("thinking", "Thinking", "hmm", 36, 14) { st, t ->
            val p = base(st)
            p.rot = 6f * sin(t * TAU)
            p.gazeX = 0.6f * sin(t * TAU)
            p.gazeY = -0.4f
            p.eyes = Eyes.OPEN
            Flannery.draw(st, p)
            Props.thoughtBubble(st, st.w * 0.74f, st.h * 0.24f, st.u * 0.11f,
                ((t * 4f).toInt() % 4))
        },

        // 37
        AnimSpec("heartbeat", "Heartbeat", "ba-dum", 30, 20) { st, t ->
            val p = base(st)
            val beat = pulse(window(t, 0.00f, 0.18f)) + 0.6f * pulse(window(t, 0.22f, 0.38f))
            val k = 1f + 0.055f * beat
            p.sx = k; p.sy = k
            p.eyes = Eyes.HAPPY
            p.blush = 0.4f + 0.4f * beat
            Props.heart(st, st.w * 0.5f, st.h * 0.50f, st.u * (0.44f + 0.05f * beat),
                withAlpha(Hue.PINK, 0.14f + 0.10f * beat))
            Flannery.draw(st, p)
        },

        // 38
        AnimSpec("starstruck", "Star Struck", "seeing stars", 36, 18) { st, t ->
            val p = base(st)
            p.rot = 5f * sin(t * TAU)
            p.eyes = Eyes.DIZZY
            Flannery.draw(st, p)
            for (i in 0 until 5) {
                val a = t * TAU + i / 5f * TAU
                Props.star(st, p.cx + cos(a) * st.u * 0.26f,
                    p.cy - st.u * 0.34f + sin(a) * st.u * 0.07f,
                    st.u * 0.038f * (0.7f + 0.3f * sin(a)), Hue.GOLD)
            }
        },

        // 39
        AnimSpec("sneeze", "Sneeze", "aa-aa-CHOO", 36, 20) { st, t ->
            val p = base(st)
            val wind = window(t, 0.00f, 0.44f)
            val blast = window(t, 0.44f, 0.78f)
            if (blast > 0f) {
                val hit = pulse(blast)
                p.cy += st.u * 0.03f * hit
                p.sx = 1f + 0.14f * hit
                p.sy = 1f - 0.12f * hit
                p.rot = 8f * hit
                p.eyes = Eyes.BLINK
            } else {
                p.cy -= st.u * 0.04f * wind
                p.sy = 1f + 0.09f * wind
                p.sx = 1f - 0.06f * wind
                p.eyes = if (wind > 0.6f) Eyes.BLINK else Eyes.WIDE
                p.brows = Brows.SAD
            }
            Flannery.draw(st, p)
            if (blast > 0f) {
                val a = clamp01(1.4f * (1f - blast))
                for (i in 0 until 8) {
                    val ang = 0.7f + i * 0.16f
                    val d = st.u * (0.24f + 0.34f * ease(blast))
                    st.circle(p.cx + cos(ang) * d, p.cy + sin(ang) * d * 0.7f,
                        st.u * 0.035f * a, withAlpha(Hue.WHITE, a * 0.9f))
                }
                Props.shout(st, st.w * 0.30f, st.h * 0.24f, st.u * 0.09f, "achoo",
                    0xFF7C6BB0.toInt(), Hue.CREAM, a)
            }
        },

        // 40
        AnimSpec("disco", "Disco", "certified mover", 36, 20) { st, t ->
            val p = base(st)
            val s2 = sin(t * TAU * 2f)
            p.rot = 12f * s2
            p.cx += st.u * 0.05f * s2
            p.cy -= st.u * 0.035f * kotlin.math.abs(s2)
            p.eyes = Eyes.HAPPY
            val colors = intArrayOf(0xFFFF6FA5.toInt(), 0xFF6FD8FF.toInt(), 0xFFFFD86F.toInt(), 0xFFA98CFF.toInt())
            for (i in 0 until 8) {
                val a = t * TAU + i / 8f * TAU
                Props.star(st, st.w * 0.5f + cos(a) * st.u * 0.40f,
                    st.h * 0.44f + sin(a) * st.u * 0.30f,
                    st.u * 0.030f, colors[i % 4], 4)
            }
            Flannery.draw(st, p)
            for (i in 0 until 6) {
                val nt = (t + i / 6f) % 1f
                Props.note(st, st.w * (0.12f + 0.76f * rnd(i * 3)),
                    st.h * 0.9f - st.h * 0.8f * nt, st.u * 0.05f,
                    withAlpha(colors[i % 4], clamp01(1.4f * (1f - nt))))
            }
        }

    )

    fun byId(id: String): AnimSpec = all.firstOrNull { it.id == id } ?: all[0]

    /** The grid's sections. Every id appears exactly once; order is display order. */
    val sections: List<Pair<String, List<String>>> = listOf(
        "Quiet Hours" to listOf("idle", "hello", "sleepy", "yawn", "thinking", "snack", "coffee", "sneeze"),
        "Full of Beans" to listOf("bounce", "wiggle", "zoomies", "roll", "trampoline", "dance", "disco", "zap"),
        "Soft Feelings" to listOf("love", "hug", "heartbeat", "boop", "sparkle", "starstruck", "rarf", "boo"),
        "Weather Permitting" to listOf("rain", "snow", "windy", "sunbathe", "rainbow", "snowball", "puddle", "stars"),
        "Party Tricks" to listOf("party", "birthday", "fireworks", "rocket", "flame", "bubbles", "peekaboo", "levitate")
    )
}

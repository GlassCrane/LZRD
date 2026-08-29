# Flannery

A tiny Android app: forty looping animations of Flannery, each one tap away from
being shared anywhere as a GIF.

## What's inside

Flannery himself is the artwork — his image, lifted off its background into an
RGBA sprite (`res/drawable-nodpi/flannery.png`). Animations don't redraw him;
they move, squash and rotate that sprite. `art/Art.kt` renders a `Pose`
(position, scale, rotation, eye state, mouth state) and each animation is a
short function moving a pose over normalised time `t` in `[0,1)`.

Expressions are drawn *over* the artwork at face positions measured off the
image — blinks, happy arcs, heart and star eyes, eyebrows. Each one first hides
the painted feature under a soft patch of the fur colour sampled from around
it, so the overlay sits on him invisibly.

**His mouth is never redrawn.** There is no mouth overlay in the renderer at
all, so every animation keeps the shape from the original artwork. Animations
that would normally need a mouth — the shout, the sneeze — carry it with
motion, eyebrows and text instead.

Props (hearts, confetti, snow, bubbles, the beanie) are vector-drawn around
him in `art/Props.kt`.

| | | | |
|---|---|---|---|
| Idle Squish | Happy Bounce | Wiggle Wiggle | Blink & Boop |
| Heart Eyes | Sleepy Time | Sparkle Pop | Hi There |
| Snack Time | Party Time | Dance Party | Peek-a-Boo |
| Barrel Roll | Rainbow Mood | Rainy Blep | Cozy Snow |
| Tiny Flame | Bubble Blow | Big Yawn | Star Gazing |
| Ratty Rocketship | RARF!!!! | Zoomies | Squish Hug |
| Coffee Buzz | Boo! | Trampoline | Snowball |
| Windy Day | Sunbathing | Static Shock | Levitate |
| Fireworks | Birthday | Puddle Splash | Thinking |
| Heartbeat | Star Struck | Sneeze | Disco |

### Sharing

Tapping **Share GIF** encodes the animation on device and hands it to the
Android share sheet as `image/gif`, so it goes anywhere that accepts an image —
Messages, Discord, Instagram, wherever. **Save** drops it into
`Pictures/Flannery`.

The GIF encoder is written from scratch in `gif/`:

- `Quantizer.kt` — median-cut over a 5-5-5 histogram, building one global
  colour table for the whole animation.
- `GifWriter.kt` — GIF89a container plus a variable-width LZW coder, with a
  NETSCAPE2.0 block so it loops forever.

Frames are rendered twice (once to gather colours, once to encode) so only one
bitmap is ever in memory. Photographic fur compresses far worse than flat
colour, so pixels unchanged from the previous frame are written as a reserved
transparent index; that plus a 360px export keeps animations around 700 KB.

## Building

The APK is built by GitHub Actions on every push — grab it from the run's
**flannery-apk** artifact, then sideload it (you'll need "install unknown apps"
enabled for your browser or file manager).

Locally, with an Android SDK installed:

```sh
cd flannery-app
./gradlew assembleDebug
```

### Look

Dark papercraft. The ground is near-black under a tiled paper-fibre texture;
each animation is mounted like a photo on a light paper cutout, with a hard
offset shadow and a hair of rotation so the grid reads as pinned-up paper. The
accent is `#ABD2C3` — sampled from Flannery himself, not picked by eye.

The two grain tiles in `drawable-nodpi/` are generated noise, made seamless by
averaging wrapped shifts so they repeat without a visible join.

The launcher icon is his face: mint gradient, his eyes, and his mouth. The
proportions come off the artwork — the mouth is about half the eye spacing and
sits close under the eyes, which is what makes it read as him rather than as a
generic smiley.

### Signing

Builds are signed with `flannery.keystore`, committed alongside the source. That
is deliberate: Android refuses to install an update whose signature changed, and
CI runners have no debug keystore — they generate a random one per run, so every
build refused to update the one before it.

This is a sideload key, not a Play upload key. It is checked into a public repo,
so treat it as public: anyone holding it could build an APK that installs over
this one, which needs physical access to the phone to matter. If you would
rather it were secret, move the keystore into a GitHub Actions secret
(base64-encoded) and have the workflow write it out before building — say the
word and I'll switch it over.

Bump `versionCode` in `app/build.gradle.kts` for each release you want to
install over the last.

- **minSdk 29** (Android 10), targetSdk 34
- Kotlin, plain Android Views — no Compose, three AndroidX dependencies

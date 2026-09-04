# Flannery

A tiny Android app: forty-eight looping animations of Flannery, in plush
and 8-bit flavours,, each one tap away from
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
| Jelly Wobble | Moonwalk | Balloon Ride | Butterfly Friend |
| Cookie Rain | Love Letter | Lil Sprout | Ta-Da! |

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

GIFs export with a **transparent background**, so Flannery drops onto whatever
he is sent — no baked-in card colour. GIF alpha is one bit (a pixel is either
opaque or clear), so soft shading cannot survive it: there is no ground shadow,
pool of light, or glow anywhere in the renderer, on purpose. Anything
semi-transparent would either vanish or harden into a block.

Transparency also rules out frame differencing — the transparent index means
"background", so every frame clears the last (disposal 2) and redraws. The
size that would cost is won back by writing only each frame's dirty rectangle.
Frames are rendered twice (once to gather colours, once to encode) so only one
bitmap is ever in memory; animations land around 850 KB.

## Building

The APK is built by GitHub Actions on every push — grab it from the run's
**flannery-apk** artifact, then sideload it (you'll need "install unknown apps"
enabled for your browser or file manager).

Locally, with an Android SDK installed:

```sh
cd flannery-app
./gradlew assembleDebug
```

### Pre-rendered GIFs

The full 8-bit set ships in the repo at [`gifs/8bit/`](gifs/8bit/) —
48 transparent looping GIFs named `flannery-8bit-<id>.gif`, exactly what the
app exports. (The fuzzy set is render-on-demand only; it is ~42 MB and lives
in the app.)

### 8-bit mode

The app opens in 8-bit; a header toggle swaps every animation to the fuzzy
plush Flannery and back. His
sprite was lifted from pixel art by detecting the drawing's own 12.3px grid
and snapping to it — one sample per true pixel — then separating him from a
white-and-checkerboard background by flood-filling with his navy outline as
the barrier (the checkerboard has near-black squares, so brightness alone
cannot tell background from outline; blue-minus-red can). The result is his
exact 65x61 pixels, shipped pre-upscaled 8x nearest-neighbour so edges stay
crisp. Face positions, expression patch colours and ink are measured per
sprite; the plush and pixel Flannery hold their faces in different places.
Flat colours compress far better than fur: 8-bit GIFs land around 260 KB.

### Look

Dark HUD. Near-black ground under a tiled dot grid; each animation floats
directly on it — the GIFs are transparent, and so is the app's own display of
them — inside a targeting-reticle frame (hairline border, mint corner
brackets, mid-edge ticks) drawn in code so the brackets never stretch. Mono
type throughout, per-mood index readouts, and a slow scan line drifting over
the detail stage. The accent is `#ABD2C3`, sampled from Flannery himself.

Props that used to sit on light card grounds are outlined or recoloured so
they read on dark and light chats alike.

The launcher icon is his face: mint gradient, his eyes, and his mouth, with
proportions taken off the artwork.

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

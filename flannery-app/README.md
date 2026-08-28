# Flannery

A tiny Android app: twenty looping animations of Flannery, each one tap away from
being shared anywhere as a GIF.

## What's inside

Flannery is drawn entirely in code — no image assets. `art/Art.kt` renders him
from vector shapes given a `Pose` (position, squash, rotation, eye state, mouth
state), so every animation is a short function that moves a pose around over a
normalised time `t` in `[0,1)`. That keeps the APK tiny and every animation
crisp at any size.

| | | | |
|---|---|---|---|
| Idle Squish | Happy Bounce | Wiggle Wiggle | Blink & Boop |
| Heart Eyes | Sleepy Time | Sparkle Pop | Hi There |
| Snack Time | Party Time | Dance Party | Peek-a-Boo |
| Barrel Roll | Rainbow Mood | Rainy Blep | Cozy Snow |
| Tiny Flame | Bubble Blow | Big Yawn | Star Gazing |

### Sharing

Tapping **Share GIF** encodes the animation on device and hands it to the
Android share sheet as `image/gif`, so it goes anywhere that accepts an image —
Messages, Discord, Instagram, wherever. **Save** drops it into
`Pictures/Flannery`.

The GIF encoder is written from scratch in `gif/`:

- `Quantizer.kt` — median-cut over a 5-5-5 histogram, building one global
  256-colour table for the whole animation.
- `GifWriter.kt` — GIF89a container plus a variable-width LZW coder, with a
  NETSCAPE2.0 block so it loops forever.

Frames are rendered twice (once to gather colours, once to encode) so only one
bitmap is ever in memory. A typical animation lands around 300–400 KB.

## Building

The APK is built by GitHub Actions on every push — grab it from the run's
**flannery-apk** artifact, then sideload it (you'll need "install unknown apps"
enabled for your browser or file manager).

Locally, with an Android SDK installed:

```sh
cd flannery-app
./gradlew assembleDebug
```

Release builds are signed with the debug key so CI needs no secrets. That's fine
for sideloading; it would need a real key to go anywhere else.

- **minSdk 29** (Android 10), targetSdk 34
- Kotlin, plain Android Views — no Compose, three AndroidX dependencies

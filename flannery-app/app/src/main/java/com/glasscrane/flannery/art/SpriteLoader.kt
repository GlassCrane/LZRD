package com.glasscrane.flannery.art

import android.content.Context
import android.graphics.BitmapFactory
import com.glasscrane.flannery.R

/**
 * Decodes both versions of him once. Face positions are measured off each
 * artwork, not shared: the plush and the pixel Flannery hold their faces in
 * different places.
 */
fun ensureSprite(context: Context) {
    if (Sprite.classic != null && Sprite.pixel != null) return
    val res = context.applicationContext.resources
    val opts = BitmapFactory.Options().apply { inScaled = false }

    Sprite.classic = SpriteSheet(
        bmp = BitmapFactory.decodeResource(res, R.drawable.flannery, opts),
        eyeLx = 0.3391f, eyeLy = 0.3642f,
        eyeRx = 0.6529f, eyeRy = 0.3693f,
        eyeR = 0.040f,
        furEyeL = 0xFF90B3A3.toInt(), furEyeR = 0xFFAED5C6.toInt(),
        ink = 0xFF2A2E2C.toInt(),
        filter = true
    )
    Sprite.pixel = SpriteSheet(
        bmp = BitmapFactory.decodeResource(res, R.drawable.flannery_8bit, opts),
        eyeLx = 0.3160f, eyeLy = 0.3193f,
        eyeRx = 0.6145f, eyeRy = 0.3193f,
        eyeR = 0.041f,
        furEyeL = 0xFF8EE4D4.toInt(), furEyeR = 0xFF8EE4D4.toInt(),
        ink = 0xFF26314D.toInt(),
        filter = true   // sprite ships pre-upscaled 8x nearest, so minify stays crisp
    )
}

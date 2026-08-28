package com.glasscrane.flannery.art

import android.content.Context
import android.graphics.BitmapFactory
import com.glasscrane.flannery.R

/** Decodes the artwork once; every view and the GIF encoder share the one bitmap. */
fun ensureSprite(context: Context) {
    if (Sprite.bmp != null) return
    val opts = BitmapFactory.Options().apply { inScaled = false }
    Sprite.bmp = BitmapFactory.decodeResource(context.applicationContext.resources, R.drawable.flannery, opts)
}

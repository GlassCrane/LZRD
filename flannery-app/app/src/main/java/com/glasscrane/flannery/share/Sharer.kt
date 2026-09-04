package com.glasscrane.flannery.share

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.glasscrane.flannery.art.Sprite
import androidx.core.content.FileProvider
import java.io.File

object Sharer {

    /**
     * Bumped whenever the encoder changes, so GIFs written by an older build are
     * never reused. v1 files were produced by an encoder with a broken LZW code
     * width and would not render anywhere.
     */
    private const val ENCODER_VERSION = 4

    fun cacheFileFor(ctx: Context, id: String): File {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val suffix = "_v$ENCODER_VERSION.gif"
        dir.listFiles()?.forEach { if (!it.name.endsWith(suffix)) it.delete() }
        val mode = if (Sprite.pixelMode) "8bit" else "fuzzy"
        return File(dir, "flannery_${id}_$mode$suffix")
    }

    fun share(ctx: Context, file: File, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/gif"
            putExtra(Intent.EXTRA_STREAM, uri)
            // Some targets read the ClipData rather than the extra, and the URI
            // permission grant only follows reliably when both carry it.
            clipData = ClipData.newUri(ctx.contentResolver, "Flannery", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, chooserTitle))
    }

    /** Drops the GIF into Pictures/Flannery. Scoped storage, so no runtime permission needed. */
    fun saveToGallery(ctx: Context, file: File, displayName: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.gif")
            put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Flannery")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = ctx.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: return false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}

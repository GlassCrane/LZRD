package com.glasscrane.flannery

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.glasscrane.flannery.anim.AnimSpec
import com.glasscrane.flannery.anim.Animations
import com.glasscrane.flannery.share.GifExporter
import com.glasscrane.flannery.share.Sharer
import com.glasscrane.flannery.ui.FlanneryView
import java.io.File
import java.util.concurrent.Executors

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "anim_id"
    }

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private lateinit var spec: AnimSpec
    private lateinit var progress: ProgressBar
    private lateinit var progressLabel: TextView
    private lateinit var shareBtn: Button
    private lateinit var saveBtn: Button

    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        spec = Animations.byId(intent.getStringExtra(EXTRA_ID) ?: "idle")

        findViewById<TextView>(R.id.detailTitle).text = spec.title
        findViewById<TextView>(R.id.detailBlurb).text = spec.blurb
        findViewById<FlanneryView>(R.id.stage).setAnimation(spec)

        progress = findViewById(R.id.progress)
        progressLabel = findViewById(R.id.progressLabel)
        shareBtn = findViewById(R.id.shareBtn)
        saveBtn = findViewById(R.id.saveBtn)

        shareBtn.setOnClickListener {
            withGif { file -> Sharer.share(this, file, getString(R.string.share_chooser)) }
        }
        saveBtn.setOnClickListener {
            withGif { file ->
                io.execute {
                    val ok = Sharer.saveToGallery(this, file, "flannery_${spec.id}")
                    main.post {
                        Toast.makeText(
                            this,
                            if (ok) R.string.saved_to_gallery else R.string.save_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /** Encodes the GIF once (cached on disk) then hands the file to [action] on the main thread. */
    private fun withGif(action: (File) -> Unit) {
        if (busy) return
        val file = Sharer.cacheFileFor(this, spec.id)
        if (file.exists() && file.length() > 0) {
            action(file)
            return
        }
        busy = true
        setEncoding(true)
        io.execute {
            var failed: Exception? = null
            try {
                GifExporter.encode(spec, file) { f ->
                    main.post { progress.progress = (f * 100).toInt() }
                }
            } catch (e: Exception) {
                failed = e
                file.delete()
            }
            main.post {
                busy = false
                setEncoding(false)
                if (failed == null) action(file)
                else Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setEncoding(on: Boolean) {
        progress.visibility = if (on) View.VISIBLE else View.INVISIBLE
        progressLabel.visibility = if (on) View.VISIBLE else View.INVISIBLE
        shareBtn.isEnabled = !on
        saveBtn.isEnabled = !on
        if (on) progress.progress = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdownNow()
    }
}

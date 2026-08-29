package com.glasscrane.flannery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.glasscrane.flannery.anim.AnimSpec
import com.glasscrane.flannery.anim.Animations
import com.glasscrane.flannery.ui.FlanneryView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.countChip).text =
            getString(R.string.count_label, Animations.all.size).uppercase()

        val grid = findViewById<RecyclerView>(R.id.grid)
        grid.layoutManager = GridLayoutManager(this, 2)
        grid.adapter = AnimAdapter(Animations.all) { spec ->
            startActivity(Intent(this, DetailActivity::class.java).putExtra(DetailActivity.EXTRA_ID, spec.id))
        }
        grid.setHasFixedSize(true)
    }

    private class AnimAdapter(
        private val items: List<AnimSpec>,
        private val onPick: (AnimSpec) -> Unit
    ) : RecyclerView.Adapter<AnimAdapter.Holder>() {

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val preview: FlanneryView = view.findViewById(R.id.preview)
            val label: TextView = view.findViewById(R.id.label)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_anim, parent, false)
            // Square-ish cards, two to a row.
            val cell = parent.measuredWidth / 2
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (cell * 1.12f).toInt()
            ).apply {
                val m = (parent.resources.displayMetrics.density * 6).toInt()
                setMargins(m, m, m, m)
            }
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val spec = items[position]
            holder.preview.setAnimation(spec)
            holder.label.text = spec.title
            // a hair off square, so the grid reads as pinned-up paper
            holder.itemView.rotation = if (position % 2 == 0) -0.9f else 0.9f
            holder.itemView.setOnClickListener { onPick(spec) }
        }

        override fun getItemCount() = items.size
    }
}

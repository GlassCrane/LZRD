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

    /** The grid, flattened: a header row, then that section's cards. */
    private sealed class Row {
        class Header(val title: String, val count: Int) : Row()
        class Item(val spec: AnimSpec, val indexInSection: Int, val sectionSize: Int) : Row()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.countChip).text =
            getString(R.string.count_label, Animations.all.size).uppercase()

        val rows = buildList {
            for ((title, ids) in Animations.sections) {
                add(Row.Header(title, ids.size))
                ids.forEachIndexed { i, id -> add(Row.Item(Animations.byId(id), i, ids.size)) }
            }
        }

        val grid = findViewById<RecyclerView>(R.id.grid)
        val lm = GridLayoutManager(this, 2)
        lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) =
                if (rows[position] is Row.Header) 2 else 1
        }
        grid.layoutManager = lm
        grid.clipChildren = false
        grid.adapter = RowAdapter(rows) { spec ->
            startActivity(Intent(this, DetailActivity::class.java).putExtra(DetailActivity.EXTRA_ID, spec.id))
        }
    }

    private class RowAdapter(
        private val rows: List<Row>,
        private val onPick: (AnimSpec) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_HEADER = 0
            const val TYPE_ITEM = 1
        }

        class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.headerTitle)
            val count: TextView = view.findViewById(R.id.headerCount)
        }

        class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
            val preview: FlanneryView = view.findViewById(R.id.preview)
            val label: TextView = view.findViewById(R.id.label)
            val indexTag: TextView = view.findViewById(R.id.indexTag)
        }

        override fun getItemViewType(position: Int) =
            if (rows[position] is Row.Header) TYPE_HEADER else TYPE_ITEM

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            if (viewType == TYPE_HEADER) {
                val v = inflater.inflate(R.layout.item_header, parent, false)
                return HeaderHolder(v)
            }
            val v = inflater.inflate(R.layout.item_anim, parent, false)
            val cell = parent.measuredWidth / 2
            v.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (cell * 1.10f).toInt()
            ).apply {
                val m = (parent.resources.displayMetrics.density * 6).toInt()
                setMargins(m, m, m, m)
            }
            return ItemHolder(v)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val row = rows[position]) {
                is Row.Header -> {
                    holder as HeaderHolder
                    holder.title.text = row.title.uppercase()
                    holder.count.text = "[%02d]".format(row.count)
                }
                is Row.Item -> {
                    holder as ItemHolder
                    holder.preview.setAnimation(row.spec)
                    holder.label.text = row.spec.title.uppercase()
                    holder.indexTag.text = "%02d/%02d".format(row.indexInSection + 1, row.sectionSize)
                    holder.itemView.setOnClickListener { onPick(row.spec) }
                }
            }
        }

        override fun getItemCount() = rows.size
    }
}

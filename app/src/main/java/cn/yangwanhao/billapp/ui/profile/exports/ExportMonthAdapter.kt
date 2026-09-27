package cn.yangwanhao.billapp.ui.profile.exports

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R

class ExportMonthAdapter(
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ExportMonthAdapter.ViewHolder>() {

    private val items = mutableListOf<Int>()   // yyyyMM 列表

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<Int>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_export_month, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMonth: TextView = itemView.findViewById(R.id.tvMonth)

        @SuppressLint("DefaultLocale")
        fun bind(monthInt: Int, onItemClick: (Int) -> Unit) {
            val year = monthInt / 100
            val month = monthInt % 100
            tvMonth.text = String.format("%04d年%02d月", year, month)
            itemView.setOnClickListener { onItemClick(monthInt) }
        }
    }
}
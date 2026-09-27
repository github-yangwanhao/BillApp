package cn.yangwanhao.billapp.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.dto.DrillItem

class DrillListAdapter : RecyclerView.Adapter<DrillListAdapter.ViewHolder>() {

    private val items = mutableListOf<DrillItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<DrillItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drill, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDrillDate)
        private val tvRemark: TextView = itemView.findViewById(R.id.tvDrillRemark)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvDrillAmount)

        @SuppressLint("DefaultLocale")
        fun bind(item: DrillItem) {
            // 日期格式：mm-dd
            val month = (item.date % 10000) / 100
            val day = item.date % 100
            tvDate.text = String.format("%02d-%02d", month, day)

            tvRemark.text = item.remark.ifEmpty { "无备注" }

            tvAmount.text = "¥${String.format("%,.2f", item.amount / 100.0)}"
        }
    }
}
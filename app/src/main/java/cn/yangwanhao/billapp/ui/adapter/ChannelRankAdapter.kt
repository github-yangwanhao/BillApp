package cn.yangwanhao.billapp.ui.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.dto.ChannelSum
import cn.yangwanhao.billapp.utils.CategoryIconHelper

class ChannelRankAdapter(
    private val onItemClick: (ChannelSum) -> Unit
) : RecyclerView.Adapter<ChannelRankAdapter.ViewHolder>() {

    private val items = mutableListOf<ChannelSum>()
    private var maxTotal = 1

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<ChannelSum>) {
        items.clear()
        items.addAll(newItems)
        maxTotal = (newItems.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_rank, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], maxTotal, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivRankIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvRankName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvRankAmount)
        private val vProgressFill: View = itemView.findViewById(R.id.vProgressFill)
        private val tvPercent: TextView = itemView.findViewById(R.id.tvRankPercent)

        @SuppressLint("DefaultLocale")
        fun bind(
            item: ChannelSum,
            maxTotal: Int,
            onItemClick: (ChannelSum) -> Unit
        ) {
            val iconData = CategoryIconHelper.getChannelIcon(item.channelName)
            ivIcon.setImageResource(iconData.first)
            (ivIcon.background as? GradientDrawable)?.setColor(iconData.second)

            tvName.text = item.channelName
            tvAmount.text = "¥${String.format("%,.2f", item.total / 100.0)}"
            tvAmount.setTextColor(0xFF1C1C1E.toInt())

            val pct = (item.total.toFloat() / maxTotal * 100).toInt()
            tvPercent.text = "$pct%"

            vProgressFill.post {
                val parentWidth = (vProgressFill.parent as? View)?.width ?: 0
                vProgressFill.layoutParams = vProgressFill.layoutParams.apply {
                    width = (parentWidth * pct / 100f).toInt()
                }
            }
            (vProgressFill.background as? GradientDrawable)?.setColor(iconData.second)
                ?: run {
                    val gd = GradientDrawable().apply {
                        cornerRadius = 3f * itemView.resources.displayMetrics.density
                        setColor(iconData.second)
                    }
                    vProgressFill.background = gd
                }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
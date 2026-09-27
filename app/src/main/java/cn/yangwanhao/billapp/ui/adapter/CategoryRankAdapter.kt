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
import cn.yangwanhao.billapp.dto.CategorySum
import cn.yangwanhao.billapp.utils.CategoryIconHelper

class CategoryRankAdapter(
    private val isIncome: Boolean,
    private val onItemClick: (CategorySum) -> Unit
) : RecyclerView.Adapter<CategoryRankAdapter.ViewHolder>() {

    private val items = mutableListOf<CategorySum>()
    private var maxTotal = 1

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<CategorySum>) {
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
        holder.bind(items[position], isIncome, maxTotal, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivRankIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvRankName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvRankAmount)
        private val vProgressFill: View = itemView.findViewById(R.id.vProgressFill)
        private val tvPercent: TextView = itemView.findViewById(R.id.tvRankPercent)

        @SuppressLint("DefaultLocale", "SetTextI18n")
        fun bind(
            item: CategorySum,
            isIncome: Boolean,
            maxTotal: Int,
            onItemClick: (CategorySum) -> Unit
        ) {
            // 图标（收入/支出用不同映射）
            val iconData = if (isIncome) {
                CategoryIconHelper.getIncomeIcon(item.categoryName)
            } else {
                CategoryIconHelper.getBillListIcon(item.categoryName)
            }
            ivIcon.setImageResource(iconData.first)
            val bg = ivIcon.background as? GradientDrawable
            bg?.setColor(iconData.second)

            // 名称
            tvName.text = item.categoryName

            // 金额
            tvAmount.text = "¥${String.format("%,.2f", item.total / 100.0)}"
            tvAmount.setTextColor(if (isIncome) 0xFF2B8A3E.toInt() else 0xFF1C1C1E.toInt())

            // 进度条（相对最大值的百分比）
            val pct = (item.total.toFloat() / maxTotal * 100).toInt()
            tvPercent.text = "$pct%"

            // 动态设置进度条宽度（post 保证测量后再设置）
            vProgressFill.post {
                val parentWidth = (vProgressFill.parent as? View)?.width ?: 0
                val targetWidth = (parentWidth * pct / 100f).toInt()
                val lp = vProgressFill.layoutParams
                lp.width = targetWidth
                vProgressFill.layoutParams = lp
            }
            // 填充色与图标背景一致
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
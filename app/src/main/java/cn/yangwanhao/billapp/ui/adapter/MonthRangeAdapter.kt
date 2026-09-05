package cn.yangwanhao.billapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.databinding.ItemMonthBinding

class MonthRangeAdapter(
    private val year: Int,                      // 当前年份
    private var startYear: Int?,
    private var startMonth: Int?,
    private var endYear: Int?,
    private var endMonth: Int?,
    private val onMonthClick: (Int) -> Unit
) : RecyclerView.Adapter<MonthRangeAdapter.ViewHolder>() {

    private val months = (1..12).toList()

    class ViewHolder(val binding: ItemMonthBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMonthBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val month = months[position]
        holder.binding.tvMonth.text = "$month 月"

        // 判断当前月份是否在选中的范围内（必须年月同时匹配）
        val isStart = startYear != null && startMonth != null &&
                year == startYear && month == startMonth
        val isEnd = endYear != null && endMonth != null &&
                year == endYear && month == endMonth

        // 判断是否在区间内（包括起始和结束）
        val isInRange = startYear != null && startMonth != null &&
                endYear != null && endMonth != null &&
                isBetween(year, month, startYear!!, startMonth!!, endYear!!, endMonth!!)

        // 设置背景
        val bgRes = when {
            isStart && isEnd -> R.drawable.bg_range_start  // 只有一个月
            isStart -> R.drawable.bg_range_start
            isEnd -> R.drawable.bg_range_end
            isInRange -> R.drawable.bg_range_middle
            else -> R.drawable.bg_month_normal
        }
        holder.binding.tvMonth.setBackgroundResource(bgRes)

        // 文字颜色
        val textColor = if (isStart || isEnd || isInRange) {
            android.graphics.Color.WHITE
        } else {
            android.graphics.Color.parseColor("#333333")
        }
        holder.binding.tvMonth.setTextColor(textColor)

        holder.itemView.setOnClickListener {
            onMonthClick(month)
        }
    }

    override fun getItemCount(): Int = months.size

    /**
     * 判断某个日期是否在起止日期范围内（包含边界）
     */
    private fun isBetween(year: Int, month: Int, sy: Int, sm: Int, ey: Int, em: Int): Boolean {
        val target = year * 12 + (month - 1)
        val start = sy * 12 + (sm - 1)
        val end = ey * 12 + (em - 1)
        return target in start..end
    }

    fun updateSelection(startYear: Int?, startMonth: Int?, endYear: Int?, endMonth: Int?) {
        this.startYear = startYear
        this.startMonth = startMonth
        this.endYear = endYear
        this.endMonth = endMonth
        notifyDataSetChanged()
    }
}
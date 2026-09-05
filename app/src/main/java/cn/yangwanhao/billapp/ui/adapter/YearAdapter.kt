package cn.yangwanhao.billapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.databinding.ItemYearMonthsBinding

class YearAdapter(
    private val yearRange: IntRange,
    private var startYear: Int?,
    private var startMonth: Int?,
    private var endYear: Int?,
    private var endMonth: Int?,
    private val onMonthSelected: (year: Int, month: Int) -> Unit
) : RecyclerView.Adapter<YearAdapter.YearViewHolder>() {

    private val years = yearRange.toList()

    class YearViewHolder(val binding: ItemYearMonthsBinding) : RecyclerView.ViewHolder(binding.root) {
        var monthAdapter: MonthRangeAdapter? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearViewHolder {
        val binding = ItemYearMonthsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return YearViewHolder(binding)
    }

    override fun onBindViewHolder(holder: YearViewHolder, position: Int) {
        val year = years[position]
        holder.binding.tvYear.text = year.toString()

        if (holder.monthAdapter == null) {
            holder.monthAdapter = MonthRangeAdapter(
                year = year,
                startYear = startYear,
                startMonth = startMonth,
                endYear = endYear,
                endMonth = endMonth,
                onMonthClick = { month ->
                    onMonthSelected(year, month)
                }
            )
            holder.binding.rvMonths.layoutManager = GridLayoutManager(
                holder.itemView.context, 4
            )
            holder.binding.rvMonths.adapter = holder.monthAdapter
        } else {
            // 更新选中状态（传入当前年份的适配器）
            holder.monthAdapter?.updateSelection(startYear, startMonth, endYear, endMonth)
        }
    }

    override fun getItemCount(): Int = years.size

    fun updateSelection(startYear: Int?, startMonth: Int?, endYear: Int?, endMonth: Int?) {
        this.startYear = startYear
        this.startMonth = startMonth
        this.endYear = endYear
        this.endMonth = endMonth
        notifyDataSetChanged()
    }
}
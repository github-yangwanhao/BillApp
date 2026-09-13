package cn.yangwanhao.billapp.ui.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.utils.CategoryIconHelper
import java.util.Date

class IncomeListAdapter(
    private val onItemClick: (IncomeListItem.IncomeItem) -> Unit,
    private val onItemLongClick: (IncomeListItem.IncomeItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MONTH_HEADER = 0
        private const val TYPE_INCOME_ITEM = 1
        private const val TYPE_LOADING = 2
    }

    private val items = mutableListOf<IncomeListItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<IncomeListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is IncomeListItem.MonthHeader -> TYPE_MONTH_HEADER
            is IncomeListItem.IncomeItem -> TYPE_INCOME_ITEM
            is IncomeListItem.LoadingPlaceholder -> TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MONTH_HEADER -> {
                val view = inflater.inflate(R.layout.item_income_month_header, parent, false)
                MonthHeaderViewHolder(view)
            }
            TYPE_INCOME_ITEM -> {
                val view = inflater.inflate(R.layout.item_income, parent, false)
                IncomeItemViewHolder(view)
            }
            TYPE_LOADING -> {
                val view = inflater.inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is IncomeListItem.MonthHeader -> {
                (holder as MonthHeaderViewHolder).bind(item)
            }
            is IncomeListItem.IncomeItem -> {
                val itemHolder = holder as IncomeItemViewHolder
                itemHolder.bind(item, onItemClick, onItemLongClick)

                // 🔥 控制卡片圆角（与支出一致）
                val isFirstInMonth = position == 0 ||
                        items[position - 1] is IncomeListItem.MonthHeader
                val isLastInMonth = position == items.size - 1 ||
                        items[position + 1] is IncomeListItem.MonthHeader ||
                        items[position + 1] is IncomeListItem.LoadingPlaceholder

                itemHolder.setCardStyle(isFirstInMonth, isLastInMonth)
            }
            is IncomeListItem.LoadingPlaceholder -> {
                (holder as LoadingViewHolder).bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // ========== 数据类 ==========

    sealed class IncomeListItem {
        data class MonthHeader(
            val monthInt: Int,
            val totalAmount: Int
        ) : IncomeListItem()

        data class IncomeItem(
            val id: Long,
            val categoryName: String,
            val amount: Int,
            val postDate: Int,
            val billMonth: Int,
            val remark: String,
            val createTime: Date
        ) : IncomeListItem()

        data class LoadingPlaceholder(
            val isLoading: Boolean
        ) : IncomeListItem()
    }

    // ========== MonthHeader ViewHolder ==========

    class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMonth: TextView = itemView.findViewById(R.id.tvMonthTitle)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvMonthTotal)

        @SuppressLint("DefaultLocale")
        fun bind(header: IncomeListItem.MonthHeader) {
            val year = header.monthInt / 100
            val month = header.monthInt % 100
            tvMonth.text = String.format("%04d年%02d月", year, month)
            val yuan = header.totalAmount / 100.0
            tvTotal.text = "所属合计 ¥${String.format("%.2f", yuan)}"
        }
    }

    // ========== IncomeItem ViewHolder ==========

    class IncomeItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIncomeIcon)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvIncomeCategory)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvIncomeAmount)
        private val tvRemark: TextView = itemView.findViewById(R.id.tvIncomeRemark)
        private val tvDate: TextView = itemView.findViewById(R.id.tvIncomeDate)
        private val tvBelongMonth: TextView = itemView.findViewById(R.id.tvIncomeBelongMonth)
        private val divider: View = itemView.findViewById(R.id.incomeDivider)

        @SuppressLint("DefaultLocale")
        fun bind(
            item: IncomeListItem.IncomeItem,
            onItemClick: (IncomeListItem.IncomeItem) -> Unit,
            onItemLongClick: (IncomeListItem.IncomeItem) -> Unit
        ) {
            // 分类图标
            val iconData = CategoryIconHelper.getIncomeIcon(item.categoryName)
            ivIcon.setImageResource(iconData.first)
            val bg = ivIcon.background as? GradientDrawable
            bg?.setColor(iconData.second)

            // 分类名称
            tvCategory.text = item.categoryName

            // 金额（绿色，带+号）
            val yuan = item.amount / 100.0
            tvAmount.text = "+¥${String.format("%.2f", yuan)}"
            tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.income_green))

            // 入账日期（月-日，与支出同格式）
            val month = (item.postDate % 10000) / 100
            val day = item.postDate % 100
            tvDate.text = String.format("%02d-%02d", month, day)

            // 备注
            if (item.remark.isEmpty()) {
                tvRemark.visibility = View.GONE
            } else {
                tvRemark.visibility = View.VISIBLE
                tvRemark.text = item.remark
            }

            // 所属月份标签（跨月时显示；位置对应支出的支付方式药丸）
            val postMonth = item.postDate / 100
            if (item.billMonth == postMonth) {
                tvBelongMonth.visibility = View.GONE
            } else {
                tvBelongMonth.visibility = View.VISIBLE
                val belongYear = item.billMonth / 100
                val belongMonth = item.billMonth % 100
                tvBelongMonth.text = String.format("所属 %04d年%02d月", belongYear, belongMonth)
            }

            // 点击事件
            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        /**
         * 设置卡片样式（控制圆角和分割线）
         */
        fun setCardStyle(isFirstInMonth: Boolean, isLastInMonth: Boolean) {
            // 控制分割线：不是最后一条才显示
            divider.visibility = if (isLastInMonth) View.GONE else View.VISIBLE

            // 控制卡片背景圆角
            val bgRes = when {
                isFirstInMonth && isLastInMonth -> R.drawable.pub_bill_card_single
                isFirstInMonth -> R.drawable.pub_bill_card_top
                isLastInMonth -> R.drawable.pub_bill_card_bottom
                else -> R.drawable.pub_bill_card_middle
            }
            itemView.setBackgroundResource(bgRes)
        }
    }

    // ========== Loading ViewHolder ==========

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvText: TextView = itemView.findViewById(R.id.tv_loading_text)

        fun bind(placeholder: IncomeListItem.LoadingPlaceholder) {
            if (placeholder.isLoading) {
                progressBar.visibility = View.VISIBLE
                tvText.text = "加载中..."
            } else {
                progressBar.visibility = View.GONE
                tvText.text = "— 已加载全部 —"
            }
        }
    }
}
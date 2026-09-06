package cn.yangwanhao.billapp.ui.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.utils.CategoryIconHelper

class BillListAdapter(
    private val onItemClick: (BillItem) -> Unit,
    private val onItemLongClick: (BillItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_MONTH_HEADER = 0
        const val TYPE_BILL_ITEM = 1
        const val TYPE_LOADING = 2
    }

    private val items = mutableListOf<Any>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<Any>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is MonthHeader -> TYPE_MONTH_HEADER
            is BillItem -> TYPE_BILL_ITEM
            is LoadingPlaceholder -> TYPE_LOADING
            else -> {
                android.util.Log.e("BillListAdapter", "未知类型: ${item.javaClass.simpleName}")
                TYPE_MONTH_HEADER
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MONTH_HEADER -> {
                val view = inflater.inflate(R.layout.item_month_header, parent, false)
                MonthHeaderViewHolder(view)
            }
            TYPE_BILL_ITEM -> {
                val view = inflater.inflate(R.layout.item_bill, parent, false)
                BillItemViewHolder(view)
            }
            TYPE_LOADING -> {
                val view = inflater.inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_month_header, parent, false)
                MonthHeaderViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MonthHeader -> {
                (holder as MonthHeaderViewHolder).bind(item)
            }
            is BillItem -> {
                val billHolder = holder as BillItemViewHolder
                billHolder.bind(item, onItemClick, onItemLongClick)

                // 🔥 判断月份边界，控制卡片圆角
                val isFirstInMonth = position == 0 || items[position - 1] is MonthHeader
                val isLastInMonth = position == items.size - 1 ||
                        items[position + 1] is MonthHeader ||
                        items[position + 1] is LoadingPlaceholder

                billHolder.setCardStyle(isFirstInMonth, isLastInMonth)
            }
            is LoadingPlaceholder -> {
                (holder as LoadingViewHolder).bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // ========== 数据类 ==========

    data class MonthHeader(
        val monthInt: Int,
        val monthTotal: Int
    )

    data class BillItem(
        val id: Long,
        val categoryName: String,
        val channelName: String,
        val amount: Int,
        val isIncome: Boolean,
        val remark: String = "",
        val payDate: Int,
        val billMonth: Int,
        var isFirstInDay: Boolean = false
    )

    data class LoadingPlaceholder(val isLoading: Boolean)

    // ========== MonthHeader ViewHolder ==========
    class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvTotal: TextView = itemView.findViewById(R.id.tv_total)

        @SuppressLint("DefaultLocale", "SetTextI18n")
        fun bind(header: MonthHeader) {
            val year = header.monthInt / 100
            val month = header.monthInt % 100
            tvDate.text = String.format("%04d年%02d月", year, month)
            val yuan = header.monthTotal / 100.0
            tvTotal.text = "合计 ¥${String.format("%.2f", yuan)}"
        }
    }

    // ========== BillItem ViewHolder ==========
    class BillItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txIcon: ImageView = itemView.findViewById(R.id.txIcon)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvChannel: TextView = itemView.findViewById(R.id.tvChannel)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvRemark: TextView = itemView.findViewById(R.id.tvRemark)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val divider: View = itemView.findViewById(R.id.divider)

        @SuppressLint("DefaultLocale", "SetTextI18n")
        fun bind(
            bill: BillItem,
            onItemClick: (BillItem) -> Unit,
            onItemLongClick: (BillItem) -> Unit
        ) {
            // 分类图标
            val iconData = CategoryIconHelper.getBillListIcon(bill.categoryName)
            txIcon.setImageResource(iconData.first)
            val bg = txIcon.background as? GradientDrawable
            bg?.setColor(iconData.second)

            // 分类名
            tvCategory.text = bill.categoryName

            // 支付方式标签
            tvChannel.text = bill.channelName
            tvChannel.visibility = if (bill.channelName.isNullOrEmpty() || bill.channelName == "—") {
                View.GONE
            } else {
                View.VISIBLE
            }

            // 日期
            if (bill.payDate > 0) {
                tvDate.visibility = View.VISIBLE
                val month = (bill.payDate % 10000) / 100
                val day = bill.payDate % 100
                tvDate.text = String.format("%02d-%02d", month, day)
            } else {
                tvDate.visibility = View.GONE
            }

            // 备注
            if (bill.remark.isEmpty()) {
                tvRemark.visibility = View.GONE
            } else {
                tvRemark.visibility = View.VISIBLE
                tvRemark.text = bill.remark
            }

            // 金额
            val yuan = bill.amount / 100.0
            val amountStr = String.format("%.2f", yuan)
            if (bill.isIncome) {
                tvAmount.text = "+¥$amountStr"
                tvAmount.setTextColor(0xFF51CF66.toInt())
            } else {
                tvAmount.text = "-¥$amountStr"
                tvAmount.setTextColor(0xFFFA5252.toInt())
            }

            // 点击事件
            itemView.setOnClickListener { onItemClick(bill) }
            itemView.setOnLongClickListener {
                onItemLongClick(bill)
                true
            }
        }

        /**
         * 设置卡片样式（控制圆角和分割线）
         * @param isFirstInMonth 是否是当月的第一条账单
         * @param isLastInMonth 是否是当月的最后一条账单
         */
        fun setCardStyle(isFirstInMonth: Boolean, isLastInMonth: Boolean) {
            // 🔥 控制分割线：不是最后一条才显示
            divider.visibility = if (isLastInMonth) View.GONE else View.VISIBLE

            // 🔥 控制卡片背景圆角
            val bgRes = when {
                isFirstInMonth && isLastInMonth -> {
                    // 只有一条账单：全圆角
                    R.drawable.pub_bill_card_single
                }
                isFirstInMonth -> {
                    // 第一条：顶部圆角，底部无
                    R.drawable.pub_bill_card_top
                }
                isLastInMonth -> {
                    // 最后一条：底部圆角，顶部无
                    R.drawable.pub_bill_card_bottom
                }
                else -> {
                    // 中间：无圆角
                    R.drawable.pub_bill_card_middle
                }
            }
            itemView.setBackgroundResource(bgRes)
        }
    }

    // ========== Loading ViewHolder ==========
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvText: TextView = itemView.findViewById(R.id.tv_loading_text)

        fun bind(placeholder: LoadingPlaceholder) {
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
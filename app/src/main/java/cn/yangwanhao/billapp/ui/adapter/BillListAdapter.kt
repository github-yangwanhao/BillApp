package cn.yangwanhao.billapp.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R

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
            is MonthHeader -> (holder as MonthHeaderViewHolder).bind(item)
            is BillItem -> (holder as BillItemViewHolder).bind(item, onItemClick, onItemLongClick)
            is LoadingPlaceholder -> (holder as LoadingViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    // ========== 数据类 ==========

    data class MonthHeader(
        val monthInt: Int,
        val monthTotal: Int
    )

    /**
     * 🔥 增加 isFirstInDay 字段
     */
    data class BillItem(
        val id: Long,
        val categoryName: String,
        val channelName: String,
        val amount: Int,
        val isIncome: Boolean,
        val remark: String = "",
        val payDate: Int,
        val billMonth: Int,
        var isFirstInDay: Boolean = false  // 是否是该日期下的第一条
    )

    data class LoadingPlaceholder(val isLoading: Boolean)

    // ========== ViewHolder ==========

    class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvTotal: TextView = itemView.findViewById(R.id.tv_total)

        @SuppressLint("DefaultLocale", "SetTextI18n")
        fun bind(header: MonthHeader) {
            val year = header.monthInt / 100
            val month = header.monthInt % 100
            tvDate.text = String.format("%04d年%02d月", year, month)
            val yuan = header.monthTotal / 100.0
            tvTotal.text = "合计：¥${String.format("%.2f", yuan)}"
        }
    }

    class BillItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val vTimelineDot: View = itemView.findViewById(R.id.vTimelineDot)
        private val vTimelineLine: View = itemView.findViewById(R.id.vTimelineLine)
        private val tvPayDate: TextView = itemView.findViewById(R.id.tv_pay_date)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvChannel: TextView = itemView.findViewById(R.id.tv_channel)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvRemark: TextView = itemView.findViewById(R.id.tv_remark)

        @SuppressLint("DefaultLocale", "SetTextI18n")
        fun bind(
            bill: BillItem,
            onItemClick: (BillItem) -> Unit,
            onItemLongClick: (BillItem) -> Unit
        ) {
            // 时间轴：圆点仅在当天第一条显示
            vTimelineDot.visibility = if (bill.isFirstInDay) View.VISIBLE else View.INVISIBLE
            // 竖线始终显示（最后一条可以保留，也可以根据需求隐藏）
            vTimelineLine.visibility = View.VISIBLE

            // 日期：每条账单都显示
            if (bill.payDate > 0) {
                tvPayDate.visibility = View.VISIBLE
                val month = (bill.payDate % 10000) / 100
                val day = bill.payDate % 100
                tvPayDate.text = String.format("%02d-%02d", month, day)
            } else {
                tvPayDate.visibility = View.GONE
            }

            // 分类（纯文字）
            tvCategory.text = bill.categoryName

            // 支付渠道（纯文字）
            tvChannel.text = bill.channelName

            // 金额
            val yuan = bill.amount / 100.0
            val amountStr = String.format("%.2f", yuan)
            if (bill.isIncome) {
                tvAmount.text = "¥$amountStr"
                tvAmount.setTextColor(0xFF388E3C.toInt())
            } else {
                tvAmount.text = "¥$amountStr"
                tvAmount.setTextColor(0xFFD32F2F.toInt())
            }

            // 备注
            if (bill.remark.isEmpty()) {
                tvRemark.visibility = View.GONE
            } else {
                tvRemark.visibility = View.VISIBLE
                tvRemark.text = bill.remark
            }

            // 点击事件
            itemView.setOnClickListener { onItemClick(bill) }
            itemView.setOnLongClickListener {
                onItemLongClick(bill)
                true
            }
        }
    }

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
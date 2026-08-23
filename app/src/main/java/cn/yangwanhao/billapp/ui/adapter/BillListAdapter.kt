package cn.yangwanhao.billapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.common.DateUtil

class BillListAdapter(
    private val onItemClick: (BillItem) -> Unit,
    private val onItemLongClick: (BillItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_DATE_HEADER = 0
        const val TYPE_BILL_ITEM = 1
        const val TYPE_LOADING = 2   // 🔥 新增
    }

    private val items = mutableListOf<Any>()

    fun submitList(newItems: List<Any>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is DateHeader -> TYPE_DATE_HEADER
            is BillItem -> TYPE_BILL_ITEM
            is LoadingPlaceholder -> TYPE_LOADING   // 🔥 新增
            else -> {
                android.util.Log.e("BillListAdapter", "未知类型: ${item?.javaClass?.simpleName}")
                TYPE_DATE_HEADER
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = inflater.inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            TYPE_BILL_ITEM -> {
                val view = inflater.inflate(R.layout.item_bill, parent, false)
                BillItemViewHolder(view)
            }
            TYPE_LOADING -> {   // 🔥 新增
                val view = inflater.inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is BillItem -> (holder as BillItemViewHolder).bind(item, onItemClick, onItemLongClick)
            is LoadingPlaceholder -> (holder as LoadingViewHolder).bind(item)   // 🔥 传入 placeholder
        }
    }

    override fun getItemCount(): Int = items.size

    // ========== 数据类 ==========
    data class DateHeader(
        val dateInt: Int,
        val dayTotal: Int
    )

    data class BillItem(
        val id: Long,
        val categoryName: String,
        val channelName: String,
        val amount: Int,
        val isIncome: Boolean,
        val remark: String = "",
        val payDate: Int
    )

    // 🔥 新增：加载占位数据类
    data class LoadingPlaceholder(val isLoading: Boolean)

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

    // ========== DateHeader ViewHolder ==========
    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvDayTotal: TextView = itemView.findViewById(R.id.tv_day_total)

        fun bind(header: DateHeader) {
            tvDate.text = DateUtil.dateIntToDisplay(header.dateInt)
            val yuan = header.dayTotal / 100.0
            tvDayTotal.text = "支出: ¥${String.format("%.2f", yuan)}"
        }
    }

    // ========== BillItem ViewHolder ==========
    class BillItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvChannel: TextView = itemView.findViewById(R.id.tv_channel)
        private val tvRemark: TextView = itemView.findViewById(R.id.tv_remark)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)

        fun bind(
            bill: BillItem,
            onItemClick: (BillItem) -> Unit,
            onItemLongClick: (BillItem) -> Unit
        ) {
            tvCategory.text = bill.categoryName
            tvChannel.text = bill.channelName

            if (bill.remark.isNullOrEmpty()) {
                tvRemark.visibility = View.GONE
            } else {
                tvRemark.visibility = View.VISIBLE
                tvRemark.text = bill.remark
            }

            val yuan = bill.amount / 100.0
            val amountStr = String.format("%.2f", yuan)

            if (bill.isIncome) {
                tvAmount.text = "+¥$amountStr"
                tvAmount.setTextColor(0xFF388E3C.toInt())
            } else {
                tvAmount.text = "-¥$amountStr"
                tvAmount.setTextColor(0xFFD32F2F.toInt())
            }

            itemView.setOnClickListener { onItemClick(bill) }
            itemView.setOnLongClickListener {
                onItemLongClick(bill)
                true
            }
        }
    }
}
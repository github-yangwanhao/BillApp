package cn.yangwanhao.billapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.common.DateUtil

class BillListAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 条目类型常量
    companion object {
        const val TYPE_DATE_HEADER = 0
        const val TYPE_BILL_ITEM = 1
    }

    // 列表数据源（日期头和账单明细混排）
    private val items = mutableListOf<Any>()

    /** 设置数据并刷新列表 */
    fun submitList(newItems: List<Any>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DateHeader -> TYPE_DATE_HEADER
            is BillItem -> TYPE_BILL_ITEM
            else -> throw IllegalArgumentException("Unknown item type")
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
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is BillItem -> (holder as BillItemViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    // ========== 日期分组头的数据类 ==========
    data class DateHeader(
        val dateInt: Int,       // yyyyMMdd 格式
        val dayTotal: Int       // 当日支出合计（单位：分）
    )

    // ========== 账单明细的数据类 ==========
    data class BillItem(
        val id: Long,           // 账单ID
        val categoryName: String, // 分类名称（如"餐饮"）
        val channelName: String,  // 支付渠道名称（如"微信"）
        val amount: Int,        // 金额（单位：分）
        val isIncome: Boolean   // true=收入, false=支出
    )

    // ========== 日期头 ViewHolder ==========
    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvDayTotal: TextView = itemView.findViewById(R.id.tv_day_total)

        fun bind(header: DateHeader) {
            tvDate.text = DateUtil.dateIntToDisplay(header.dateInt)
            val yuan = header.dayTotal / 100.0
            tvDayTotal.text = "支出: ¥${String.format("%.2f", yuan)}"
        }
    }

    // ========== 账单明细 ViewHolder ==========
    class BillItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvChannel: TextView = itemView.findViewById(R.id.tv_channel)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)

        fun bind(bill: BillItem) {
            tvCategory.text = bill.categoryName
            tvChannel.text = bill.channelName

            val yuan = bill.amount / 100.0
            val amountStr = String.format("%.2f", yuan)

            if (bill.isIncome) {
                tvAmount.text = "+¥$amountStr"
                tvAmount.setTextColor(0xFF388E3C.toInt()) // 绿色
            } else {
                tvAmount.text = "-¥$amountStr"
                tvAmount.setTextColor(0xFFD32F2F.toInt()) // 红色
            }
        }
    }
}
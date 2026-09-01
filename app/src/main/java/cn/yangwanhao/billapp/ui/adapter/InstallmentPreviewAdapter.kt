package cn.yangwanhao.billapp.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.dto.InstallmentBillDto
import cn.yangwanhao.billapp.utils.InstallmentCalculator

class InstallmentPreviewAdapter(
    private val onRemarkClick: (InstallmentBillDto, Int) -> Unit
) : RecyclerView.Adapter<InstallmentPreviewAdapter.ViewHolder>() {

    private var bills: List<InstallmentBillDto> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newBills: List<InstallmentBillDto>) {
        bills = newBills
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_installment_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bills[position], position, onRemarkClick)
    }

    override fun getItemCount(): Int = bills.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIndex: TextView = itemView.findViewById(R.id.tv_installment_index)
        private val tvMonth: TextView = itemView.findViewById(R.id.tv_installment_month)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_installment_amount)
        private val tvRemark: TextView = itemView.findViewById(R.id.tv_installment_remark)

        @SuppressLint("SetTextI18n", "DefaultLocale")
        fun bind(
            bill: InstallmentBillDto,
            position: Int,
            onRemarkClick: (InstallmentBillDto, Int) -> Unit
        ) {
            tvIndex.text = "第${bill.installmentIndex}期"
            tvMonth.text = InstallmentCalculator.formatMonth(bill.billMonth)
            val yuan = bill.amount / 100.0
            tvAmount.text = "¥${String.format("%.2f", yuan)}"
            tvRemark.text = bill.remark.ifEmpty { "点击编辑备注" }

            // 点击备注可编辑
            tvRemark.setOnClickListener {
                onRemarkClick(bill, position)
            }
        }
    }
}
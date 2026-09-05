package cn.yangwanhao.billapp.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.dto.InstallmentBillDto
import cn.yangwanhao.billapp.utils.InstallmentCalculator

class InstallmentPreviewAdapter(
    private val onRemarkChanged: (position: Int, newRemark: String) -> Unit
) : RecyclerView.Adapter<InstallmentPreviewAdapter.ViewHolder>() {

    private var bills: List<InstallmentBillDto> = emptyList()
    private var editingPosition: Int = -1

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newBills: List<InstallmentBillDto>) {
        bills = newBills
        editingPosition = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_installment_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            bill = bills[position],
            position = position,
            isEditing = editingPosition == position,
            onRemarkChanged = onRemarkChanged,
            onEditStart = { pos ->
                // 如果已有编辑项，先保存当前编辑项
                if (editingPosition != -1 && editingPosition != pos) {
                    // 通知外部保存当前编辑项（由外部处理）
                }
                editingPosition = pos
                notifyDataSetChanged()
            },
            onEditEnd = {
                editingPosition = -1
                notifyDataSetChanged()
            }
        )
    }

    override fun getItemCount(): Int = bills.size

    /**
     * 外部调用：退出编辑模式（用于保存时或点击外部时）
     */
    fun exitEditing() {
        if (editingPosition != -1) {
            editingPosition = -1
            notifyDataSetChanged()
        }
    }

    /**
     * 外部调用：刷新数据（用于批量应用后更新）
     */
    fun refreshData(newBills: List<InstallmentBillDto>) {
        bills = newBills
        editingPosition = -1
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIndex: TextView = itemView.findViewById(R.id.tv_installment_index)
        private val tvMonth: TextView = itemView.findViewById(R.id.tv_installment_month)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_installment_amount)
        private val tvRemark: TextView = itemView.findViewById(R.id.tv_installment_remark)
        private val etRemark: EditText = itemView.findViewById(R.id.et_installment_remark)

        fun bind(
            bill: InstallmentBillDto,
            position: Int,
            isEditing: Boolean,
            onRemarkChanged: (Int, String) -> Unit,
            onEditStart: (Int) -> Unit,
            onEditEnd: () -> Unit
        ) {
            // === 基本信息 ===
            tvIndex.text = "第${bill.installmentIndex}期"
            tvMonth.text = InstallmentCalculator.formatMonth(bill.billMonth)
            val yuan = bill.amount / 100.0
            tvAmount.text = "¥${String.format("%.2f", yuan)}"

            // === 备注显示 / 编辑 ===
            if (isEditing) {
                // 编辑模式
                tvRemark.visibility = View.GONE
                etRemark.visibility = View.VISIBLE
                etRemark.setText(bill.remark)
                etRemark.setSelection(etRemark.text.length)
                etRemark.requestFocus()

                // 弹出键盘
                val imm = itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etRemark, InputMethodManager.SHOW_IMPLICIT)

                // 点击“完成” → 保存并退出编辑
                etRemark.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        saveRemark(position, onRemarkChanged, onEditEnd)
                        true
                    } else {
                        false
                    }
                }

                // 失去焦点 → 保存并退出编辑
                etRemark.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        saveRemark(position, onRemarkChanged, onEditEnd)
                    }
                }

            } else {
                // 显示模式
                tvRemark.visibility = View.VISIBLE
                etRemark.visibility = View.GONE
                tvRemark.text = bill.remark.ifEmpty { "点击编辑备注" }
                etRemark.clearFocus()

                // 点击备注 → 进入编辑模式
                tvRemark.setOnClickListener {
                    onEditStart(position)
                }
            }
        }

        private fun saveRemark(
            position: Int,
            onRemarkChanged: (Int, String) -> Unit,
            onEditEnd: () -> Unit
        ) {
            val newRemark = etRemark.text.toString().trim()
            if (newRemark.isNotEmpty()) {
                onRemarkChanged(position, newRemark)
            }
            // 隐藏键盘
            val imm = itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etRemark.windowToken, 0)
            onEditEnd()
        }
    }
}
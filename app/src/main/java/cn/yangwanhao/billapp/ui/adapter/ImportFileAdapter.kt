package cn.yangwanhao.billapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.ui.imports.ImportFileItem   // 新增
import cn.yangwanhao.billapp.ui.imports.ImportFileStatus // 新增

class ImportFileAdapter(
    private val onItemClick: (ImportFileItem) -> Unit
) : RecyclerView.Adapter<ImportFileAdapter.ViewHolder>() {

    private val items = mutableListOf<ImportFileItem>()

    fun submitList(list: List<ImportFileItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun updateStatus(filePath: String, status: ImportFileStatus, recordCount: Int = 0, errorMessage: String? = null) {
        val index = items.indexOfFirst { it.filePath == filePath }
        if (index != -1) {
            items[index] = items[index].copy(
                status = status,
                recordCount = recordCount,
                errorMessage = errorMessage
            )
            notifyItemChanged(index)
        }
    }

    fun clearAll() {
        items.clear()
        notifyDataSetChanged()
    }

    fun getItems(): List<ImportFileItem> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_import_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatusIcon: TextView = itemView.findViewById(R.id.tvStatusIcon)
        private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        private val tvFileInfo: TextView = itemView.findViewById(R.id.tvFileInfo)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(item: ImportFileItem, onItemClick: (ImportFileItem) -> Unit) {
            tvFileName.text = item.fileName

            val sizeKB = item.size / 1024.0
            val sizeStr = when {
                sizeKB < 1 -> "${item.size} B"
                sizeKB < 1024 -> String.format("%.1f KB", sizeKB)
                else -> String.format("%.1f MB", sizeKB / 1024)
            }

            tvFileInfo.text = when (item.status) {
                ImportFileStatus.PENDING -> sizeStr
                ImportFileStatus.SUCCESS -> "${item.recordCount} 条记录"
                ImportFileStatus.FAILED -> item.errorMessage ?: "导入失败"
                ImportFileStatus.SKIPPED -> "已导入，跳过"
            }

            when (item.status) {
                ImportFileStatus.PENDING -> {
                    tvStatusIcon.text = "⏳"
                    tvStatus.text = "待导入"
                    tvStatus.setTextColor(0xFF999999.toInt())
                }
                ImportFileStatus.SUCCESS -> {
                    tvStatusIcon.text = "✅"
                    tvStatus.text = "成功"
                    tvStatus.setTextColor(0xFF388E3C.toInt())
                }
                ImportFileStatus.FAILED -> {
                    tvStatusIcon.text = "❌"
                    tvStatus.text = "失败"
                    tvStatus.setTextColor(0xFFD32F2F.toInt())
                }
                ImportFileStatus.SKIPPED -> {
                    tvStatusIcon.text = "⏭️"
                    tvStatus.text = "跳过"
                    tvStatus.setTextColor(0xFFFF9800.toInt())
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
package cn.yangwanhao.billapp.ui.profile.exports

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.yangwanhao.billapp.databinding.FragmentExportBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportFragment : Fragment() {

    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExportViewModel by viewModels()

    // 待导出状态
    private var pendingType: String? = null
    private var pendingMonth: Int? = null

    private val saveDirLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { handleSaveDir(it) }
        } else {
            pendingType = null
            pendingMonth = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cardExportExpense.setOnClickListener { startExportExpense() }
        binding.cardExportIncome.setOnClickListener { startExportIncome() }

        setupObservers()
        viewModel.loadAvailableMonths()
    }

    private fun setupObservers() {
        viewModel.toastMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                viewModel.clearToast()
            }
        }
    }

    // ============================================================
    // 支出导出：弹月份列表 → 确认 → 选目录
    // ============================================================
    private fun startExportExpense() {
        val months = viewModel.availableMonths.value ?: emptyList()
        if (months.isEmpty()) {
            Toast.makeText(requireContext(), "暂无支出数据", Toast.LENGTH_SHORT).show()
            return
        }

        val sheet = ExportMonthPickerSheetFragment.newInstance(months.toIntArray())
        sheet.setOnMonthSelectedListener { monthInt ->
            showExportConfirm(monthInt)
        }
        sheet.show(childFragmentManager, "ExportMonthPicker")
    }

    @SuppressLint("DefaultLocale")
    private fun showExportConfirm(monthInt: Int) {
        val year = monthInt / 100
        val month = monthInt % 100
        AlertDialog.Builder(requireContext())
            .setTitle("确认导出")
            .setMessage(String.format("确定导出 %04d年%02d月 的支出数据吗？", year, month))
            .setPositiveButton("确定") { _, _ ->
                pendingType = "EXPENSE"
                pendingMonth = monthInt
                launchDirPicker()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ============================================================
    // 收入导出：直接选目录
    // ============================================================
    private fun startExportIncome() {
        pendingType = "INCOME"
        pendingMonth = null
        launchDirPicker()
    }

    private fun launchDirPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        saveDirLauncher.launch(intent)
    }

    private fun handleSaveDir(treeUri: Uri) {
        val type = pendingType ?: return
        val fileName = when (type) {
            "EXPENSE" -> {
                val ym = pendingMonth ?: return
                String.format("%04d-%02d.xlsx", ym / 100, ym % 100)
            }
            "INCOME" -> {
                val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                "收入数据-截止${dateStr}.xlsx"
            }
            else -> return
        }

        try {
            val dir = DocumentFile.fromTreeUri(requireContext(), treeUri)
            if (dir == null || !dir.canWrite()) {
                Toast.makeText(requireContext(), "无法写入该目录", Toast.LENGTH_SHORT).show()
                return
            }

            dir.findFile(fileName)?.delete()

            val file = dir.createFile(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fileName
            )
            if (file == null) {
                Toast.makeText(requireContext(), "创建文件失败", Toast.LENGTH_SHORT).show()
                return
            }

            when (type) {
                "EXPENSE" -> viewModel.exportExpense(
                    requireContext(), file.uri, pendingMonth!!, fileName
                )
                "INCOME" -> viewModel.exportIncome(
                    requireContext(), file.uri, fileName
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pendingType = null
            pendingMonth = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
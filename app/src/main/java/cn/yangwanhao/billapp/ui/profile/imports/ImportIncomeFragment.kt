package cn.yangwanhao.billapp.ui.profile.imports

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cn.yangwanhao.billapp.databinding.FragmentImportIncomeBinding
import cn.yangwanhao.billapp.ui.adapter.ImportFileAdapter
import kotlinx.coroutines.launch
import java.io.File

class ImportIncomeFragment : Fragment() {

    private var _binding: FragmentImportIncomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ImportIncomeViewModel by viewModels()
    private val adapter = ImportFileAdapter { }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { handleFileUri(it) }
        }
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { handleFolderUri(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvImportFiles.layoutManager = LinearLayoutManager(context)
        binding.rvImportFiles.adapter = adapter
        setupObservers()
        setupListeners()
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        viewModel.files.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.tvFileCount.text = "${items.size} 个文件"
            binding.tvClearAll.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
            val hasPending = items.any { it.status == ImportFileStatus.PENDING }
            binding.btnStartImport.isEnabled = hasPending && !viewModel.isImporting.value!!
            binding.btnStartImport.alpha = if (hasPending && !viewModel.isImporting.value!!) 1.0f else 0.5f
        }
        viewModel.isImporting.observe(viewLifecycleOwner) { isImporting ->
            binding.btnStartImport.isEnabled = !isImporting
            binding.btnStartImport.text = if (isImporting) "导入中..." else "开始导入"
            binding.btnSelectFile.isEnabled = !isImporting
            binding.btnSelectFolder.isEnabled = !isImporting
            binding.layoutProgress.visibility = if (isImporting) View.VISIBLE else View.GONE
            binding.btnCancelImport.visibility = if (isImporting) View.VISIBLE else View.GONE
        }
        viewModel.progress.observe(viewLifecycleOwner) {
            binding.progressBar.progress = it
            binding.tvProgressPercent.text = "$it%"
        }
        viewModel.progressText.observe(viewLifecycleOwner) {
            binding.tvProgressText.text = it
        }
        viewModel.toastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }
        viewModel.importResult.observe(viewLifecycleOwner) { result ->
            result?.let { showImportResultDialog(it) }
        }
    }

    private fun setupListeners() {
        binding.tvBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnSelectFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                ))
            }
            filePickerLauncher.launch(intent)
        }
        binding.btnSelectFolder.setOnClickListener {
            folderPickerLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
        }
        binding.tvClearAll.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("清空列表")
                .setMessage("确定要清空所有待导入文件吗？")
                .setPositiveButton("清空") { _, _ -> viewModel.clearFiles() }
                .setNegativeButton("取消", null)
                .show()
        }
        binding.btnCancelImport.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("取消导入")
                .setMessage("确定要取消当前导入吗？")
                .setPositiveButton("确定取消") { _, _ -> viewModel.cancelImport() }
                .setNegativeButton("继续导入", null)
                .show()
        }
        binding.btnStartImport.setOnClickListener {
            binding.btnStartImport.isEnabled = false
            startImport()
        }
    }

    private fun startImport() {
        val fileList = viewModel.files.value ?: emptyList()
        if (fileList.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择文件", Toast.LENGTH_SHORT).show()
            return
        }
        if (fileList.none { it.status == ImportFileStatus.PENDING }) {
            Toast.makeText(requireContext(), "没有待导入的文件", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            viewModel.startImport()
        }
    }

    private fun handleFileUri(uri: Uri) {
        try {
            val fileName = getFileNameFromUri(uri) ?: run {
                Toast.makeText(requireContext(), "无法获取文件名", Toast.LENGTH_SHORT).show()
                return
            }
            // 收入导入只检查扩展名
            if (!fileName.endsWith(".xlsx", true) && !fileName.endsWith(".xls", true)) {
                Toast.makeText(requireContext(), "请选择 Excel 文件", Toast.LENGTH_SHORT).show()
                return
            }
            val cacheFile = File(requireContext().cacheDir, fileName)
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (cacheFile.exists()) {
                viewModel.addFiles(listOf(cacheFile))
                Toast.makeText(requireContext(), "已添加：$fileName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "读取文件失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex("_display_name")
            if (nameIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.path?.substringAfterLast("/")
    }

    private fun handleFolderUri(uri: Uri) {
        try {
            val files = scanFolderForExcelFiles(uri)
            if (files.isEmpty()) {
                Toast.makeText(requireContext(), "文件夹中没有 Excel 文件", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addFiles(files)
                Toast.makeText(requireContext(), "找到 ${files.size} 个文件", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "读取文件夹失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scanFolderForExcelFiles(uri: Uri): List<File> {
        val result = mutableListOf<File>()
        DocumentFile.fromTreeUri(requireContext(), uri)?.listFiles()?.forEach { doc ->
            if (doc.isFile) {
                val name = doc.name ?: return@forEach
                if (name.endsWith(".xlsx", true) || name.endsWith(".xls", true)) {
                    try {
                        val cacheFile = File(requireContext().cacheDir, name)
                        requireContext().contentResolver.openInputStream(doc.uri)?.use { input ->
                            cacheFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (cacheFile.exists()) result.add(cacheFile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return result
    }

    private fun showImportResultDialog(result: ImportSummary) {
        val message = buildString {
            append("✅ 成功：${result.successCount} 个文件\n")
            append("❌ 失败：${result.failedCount} 个文件\n")
            append("⏭️ 跳过：${result.skippedCount} 个文件\n\n")
            result.details.forEach { detail ->
                val statusText = when (detail.status) {
                    ImportFileStatus.SUCCESS -> "✅ 成功 (${detail.recordCount}条)"
                    ImportFileStatus.FAILED -> "❌ 失败 (${detail.errorMessage ?: "未知错误"})"
                    ImportFileStatus.SKIPPED -> "⏭️ 跳过 (已导入)"
                    else -> "⏳ 待处理"
                }
                append("  ${detail.fileName}: $statusText\n")
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle("导入完成")
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> viewModel.reset() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
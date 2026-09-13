package cn.yangwanhao.billapp.ui.profile.imports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.ImportFileHisRepository
import cn.yangwanhao.billapp.repository.IncomeBillRepository
import cn.yangwanhao.billapp.service.IncomeImportResult
import cn.yangwanhao.billapp.service.IncomeImportService
import kotlinx.coroutines.launch
import java.io.File

class ImportIncomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val importFileHisRepository = ImportFileHisRepository(database.importFileHisDao())
    private val dictRepository = DictRepository(database.dictDao())
    private val incomeBillRepository = IncomeBillRepository(database.incomeBillDao())

    private val importService = IncomeImportService(
        database = database,
        importFileHisRepository = importFileHisRepository,
        dictRepository = dictRepository,
        incomeBillRepository = incomeBillRepository
    )

    private val _files = MutableLiveData<List<ImportFileItem>>(emptyList())
    val files: LiveData<List<ImportFileItem>> = _files

    private val _isImporting = MutableLiveData(false)
    val isImporting: LiveData<Boolean> = _isImporting

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

    private val _progressText = MutableLiveData("")
    val progressText: LiveData<String> = _progressText

    private val _importResult = MutableLiveData<ImportSummary?>(null)
    val importResult: LiveData<ImportSummary?> = _importResult

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private var isCancelled = false

    fun addFiles(files: List<File>) {
        // 收入导入不再依赖文件名格式，所有 .xlsx / .xls 都接受
        val validFiles = files.filter {
            it.name.endsWith(".xlsx", true) || it.name.endsWith(".xls", true)
        }

        val newItems = validFiles.map { file ->
            ImportFileItem(
                fileName = file.name,
                filePath = file.absolutePath,
                billMonth = 0,  // 收入不再从文件名推导月份
                size = file.length(),
                status = ImportFileStatus.PENDING
            )
        }

        val currentPaths = _files.value?.map { it.filePath }?.toSet() ?: emptySet()
        val filtered = newItems.filter { it.filePath !in currentPaths }
        if (filtered.isNotEmpty()) {
            _files.value = (_files.value ?: emptyList()) + filtered
        }

        if (validFiles.size < files.size) {
            _toastMessage.value = "已过滤 ${files.size - validFiles.size} 个非 Excel 文件"
        }
    }

    fun clearFiles() {
        _files.value = emptyList()
        _importResult.value = null
    }

    fun startImport() {
        if (_isImporting.value == true) {
            return
        }
        val fileList = _files.value ?: emptyList()
        if (fileList.isEmpty()) {
            _toastMessage.value = "请先选择文件"
            return
        }
        if (fileList.all { it.status != ImportFileStatus.PENDING }) {
            _toastMessage.value = "没有待导入的文件"
            return
        }

        _isImporting.value = true
        _progress.value = 0
        _importResult.value = null
        isCancelled = false

        val pending = fileList.filter { it.status == ImportFileStatus.PENDING }
        val total = pending.size

        viewModelScope.launch {
            var successCount = 0
            var failedCount = 0
            var skippedCount = 0
            val details = mutableListOf<ImportFileDetail>()

            pending.forEachIndexed { index, item ->
                if (isCancelled) return@forEachIndexed

                _progress.value = ((index.toFloat() / total) * 100).toInt()
                _progressText.value = "正在导入：${item.fileName}"

                val result = importService.importFile(File(item.filePath))

                val status = when (result) {
                    is IncomeImportResult.Success -> {
                        successCount++; ImportFileStatus.SUCCESS
                    }
                    is IncomeImportResult.AlreadyImported -> {
                        skippedCount++; ImportFileStatus.SKIPPED
                    }
                    is IncomeImportResult.Failed -> {
                        failedCount++; ImportFileStatus.FAILED
                    }
                }

                val recordCount = (result as? IncomeImportResult.Success)?.recordCount ?: 0
                val errorMessage = when (result) {
                    is IncomeImportResult.Failed -> result.reason
                    is IncomeImportResult.AlreadyImported -> "文件已导入过"
                    else -> null
                }

                details.add(
                    ImportFileDetail(
                        fileName = item.fileName,
                        status = status,
                        recordCount = recordCount,
                        errorMessage = errorMessage
                    )
                )
                updateFileStatus(item.filePath, status, recordCount, errorMessage)
            }

            _isImporting.value = false
            _progress.value = 100
            _progressText.value = "导入完成"
            _importResult.value = ImportSummary(successCount, failedCount, skippedCount, details)
            _toastMessage.value = "导入完成：成功 $successCount，失败 $failedCount，跳过 $skippedCount"
        }
    }

    fun cancelImport() {
        isCancelled = true
        _isImporting.value = false
        _progressText.value = "已取消"
    }

    private fun updateFileStatus(filePath: String, status: ImportFileStatus, recordCount: Int, errorMessage: String?) {
        val current = _files.value?.toMutableList() ?: return
        val index = current.indexOfFirst { it.filePath == filePath }
        if (index != -1) {
            current[index] = current[index].copy(
                status = status,
                recordCount = recordCount,
                errorMessage = errorMessage
            )
            _files.value = current
        }
    }

    fun reset() {
        _importResult.value = null
        _progress.value = 0
        _progressText.value = ""
        _isImporting.value = false
        isCancelled = false
    }
}
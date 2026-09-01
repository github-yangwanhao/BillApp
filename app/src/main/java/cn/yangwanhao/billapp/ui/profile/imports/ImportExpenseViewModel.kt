package cn.yangwanhao.billapp.ui.profile.imports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.ImportFileHisRepository
import cn.yangwanhao.billapp.service.ImportResult
import cn.yangwanhao.billapp.service.ImportService
import kotlinx.coroutines.launch
import java.io.File

class ImportExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val consumeBillDao = database.consumeBillDao()
    private val importFileHisDao = database.importFileHisDao()
    private val importFileHisRepository = ImportFileHisRepository(importFileHisDao)
    private val dictRepository = DictRepository(database.dictDao())
    private val consumeBillRepository = ConsumeBillRepository(consumeBillDao)

    private val importService = ImportService(
        database = database,
        importFileHisRepository = importFileHisRepository,
        dictRepository = dictRepository,
        consumeBillRepository = consumeBillRepository
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

    /**
     * 解析文件，添加到列表
     */
    fun addFiles(files: List<File>) {
        val validFiles = files.filter { file ->
            importService.parseBillMonthFromFileName(file.name) != null
        }

        val newItems = validFiles.map { file ->
            ImportFileItem(
                fileName = file.name,
                filePath = file.absolutePath,
                billMonth = importService.parseBillMonthFromFileName(file.name)!!,
                size = file.length(),
                status = ImportFileStatus.PENDING
            )
        }

        // 去重（按文件路径）
        val currentPaths = _files.value?.map { it.filePath }?.toSet() ?: emptySet()
        val filtered = newItems.filter { it.filePath !in currentPaths }

        if (filtered.isNotEmpty()) {
            val updated = (_files.value ?: emptyList()) + filtered
            _files.value = updated
        }

        if (validFiles.size < files.size) {
            _toastMessage.value = "已过滤 ${files.size - validFiles.size} 个不符合命名规则的文件"
        }
    }

    /**
     * 清空文件列表
     */
    fun clearFiles() {
        _files.value = emptyList()
        _importResult.value = null
    }

    /**
     * 开始导入
     */
    fun startImport(onConflict: suspend(Int, Int) -> Boolean) {
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
                if (isCancelled) {
                    _toastMessage.value = "导入已取消"
                    return@forEachIndexed
                }

                // 更新进度
                _progress.value = ((index.toFloat() / total) * 100).toInt()
                _progressText.value = "正在导入：${item.fileName}"

                val result = importService.importFile(
                    file = File(item.filePath),
                    onConflict = { billMonth, existingCount ->
                        onConflict(billMonth, existingCount)
                    }
                )

                val status = when (result) {
                    is ImportResult.Success -> {
                        successCount++
                        ImportFileStatus.SUCCESS
                    }
                    is ImportResult.AlreadyImported -> {
                        skippedCount++
                        ImportFileStatus.SKIPPED
                    }
                    is ImportResult.Failed -> {
                        failedCount++
                        ImportFileStatus.FAILED
                    }
                    is ImportResult.Conflict -> {
                        // 理论上不会走到这里，因为已经在 onConflict 中处理了
                        failedCount++
                        ImportFileStatus.FAILED
                    }
                }

                val recordCount = when (result) {
                    is ImportResult.Success -> result.recordCount
                    else -> 0
                }

                val errorMessage = when (result) {
                    is ImportResult.Failed -> result.reason
                    is ImportResult.AlreadyImported -> "文件已导入过"
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

                // 更新列表项状态
                updateFileStatus(item.filePath, status, recordCount, errorMessage)
            }

            // 导入完成
            _isImporting.value = false
            _progress.value = 100
            _progressText.value = "导入完成"

            _importResult.value = ImportSummary(
                successCount = successCount,
                failedCount = failedCount,
                skippedCount = skippedCount,
                details = details
            )

            _toastMessage.value = "导入完成：成功 $successCount，失败 $failedCount，跳过 $skippedCount"
        }
    }

    /**
     * 取消导入
     */
    fun cancelImport() {
        isCancelled = true
        _isImporting.value = false
        _progressText.value = "已取消"
    }

    /**
     * 更新单个文件状态
     */
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

    /**
     * 重置状态
     */
    fun reset() {
        _importResult.value = null
        _progress.value = 0
        _progressText.value = ""
        _isImporting.value = false
        isCancelled = false
    }
}

/**
 * 导入文件状态（UI 展示用）
 */
data class ImportFileItem(
    val fileName: String,
    val filePath: String,
    val billMonth: Int,
    val size: Long,
    var status: ImportFileStatus = ImportFileStatus.PENDING,
    var recordCount: Int = 0,
    var errorMessage: String? = null
)

enum class ImportFileStatus {
    PENDING, SUCCESS, FAILED, SKIPPED
}

data class ImportFileDetail(
    val fileName: String,
    val status: ImportFileStatus,
    val recordCount: Int,
    val errorMessage: String?
)

data class ImportSummary(
    val successCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val details: List<ImportFileDetail>
)
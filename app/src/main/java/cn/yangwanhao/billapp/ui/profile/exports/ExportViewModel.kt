package cn.yangwanhao.billapp.ui.profile.exports

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.service.ExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BillDatabase.getDatabase(application)
    private val exportService = ExportService(
        consumeBillDao = db.consumeBillDao(),
        incomeBillDao = db.incomeBillDao(),
        dictDao = db.dictDao()
    )
    private val consumeBillRepository = ConsumeBillRepository(db.consumeBillDao())

    private val _availableMonths = MutableLiveData<List<Int>>(emptyList())
    val availableMonths: LiveData<List<Int>> = _availableMonths

    private val _exporting = MutableLiveData(false)
    val exporting: LiveData<Boolean> = _exporting

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    fun loadAvailableMonths() {
        viewModelScope.launch {
            try {
                _availableMonths.value = consumeBillRepository.getDistinctBillMonths()
            } catch (e: Exception) {
                e.printStackTrace()
                _availableMonths.value = emptyList()
            }
        }
    }

    /**
     * 导出指定月份的支出到指定 URI
     */
    fun exportExpense(context: Context, uri: Uri, billMonth: Int, fileName: String) {
        if (_exporting.value == true) return
        _exporting.value = true
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: throw Exception("无法打开输出流")
                    output.use {
                        exportService.exportExpense(billMonth, it)
                    }
                }
                _toastMessage.postValue("已导出：$fileName（共 $count 条）")
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.postValue("导出失败：${e.message}")
            } finally {
                _exporting.value = false
            }
        }
    }

    /**
     * 导出全部收入
     */
    fun exportIncome(context: Context, uri: Uri, fileName: String) {
        if (_exporting.value == true) return
        _exporting.value = true
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: throw Exception("无法打开输出流")
                    output.use {
                        exportService.exportIncome(it)
                    }
                }
                _toastMessage.postValue("已导出：$fileName（共 $count 条）")
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.postValue("导出失败：${e.message}")
            } finally {
                _exporting.value = false
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
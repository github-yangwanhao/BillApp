package cn.yangwanhao.billapp.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.dto.DrillItem
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.StatsRepository
import kotlinx.coroutines.launch

class DrillSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BillDatabase.getDatabase(application)
    private val statsRepository = StatsRepository(
        consumeBillDao = db.consumeBillDao(),
        incomeBillDao = db.incomeBillDao(),
        dictRepository = DictRepository(db.dictDao())
    )

    private val _items = MutableLiveData<List<DrillItem>>(emptyList())
    val items: LiveData<List<DrillItem>> = _items

    fun loadDrillList(categoryId: Int, start: Int, end: Int, isIncome: Boolean) {
        viewModelScope.launch {
            try {
                val list = statsRepository.getDrillList(categoryId, start, end, isIncome)
                _items.postValue(list)
            } catch (e: Exception) {
                e.printStackTrace()
                _items.postValue(emptyList())
            }
        }
    }
}
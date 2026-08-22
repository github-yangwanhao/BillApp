package cn.yangwanhao.billapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.entity.IncomeBill
import cn.yangwanhao.billapp.repository.IncomeBillRepository
import kotlinx.coroutines.launch

class IncomeBillViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val incomeBillDao = database.incomeBillDao()
    private val dictDao = database.dictDao()
    private val repository = IncomeBillRepository(incomeBillDao, dictDao)

    private val pageSize = 20
    private var currentPage = 0
    private var isLoading = false
    private var isAllLoaded = false

    private val _bills = MutableLiveData<MutableList<IncomeBill>>(mutableListOf())
    val bills: LiveData<MutableList<IncomeBill>> = _bills

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    fun loadFirstPage() {
        currentPage = 0
        isAllLoaded = false
        _bills.value = mutableListOf()
        loadNextPage()
    }

    fun loadNextPage() {
        if (isLoading || isAllLoaded) return

        isLoading = true
        viewModelScope.launch {
            val offset = currentPage * pageSize
            val newList = repository.getBillsPaged(pageSize, offset)

            val currentList = _bills.value ?: mutableListOf()
            currentList.addAll(newList)
            _bills.value = currentList

            if (newList.size < pageSize) {
                isAllLoaded = true
                _hasMore.value = false
            }

            currentPage++
            isLoading = false
        }
    }

    fun insert(bill: IncomeBill) {
        viewModelScope.launch {
            repository.addBill(bill)
            loadFirstPage()
        }
    }

    fun delete(bill: IncomeBill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            loadFirstPage()
        }
    }
}
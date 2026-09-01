package cn.yangwanhao.billapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.entity.IncomeBill
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.IncomeBillRepository
import cn.yangwanhao.billapp.ui.adapter.BillListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IncomeBillViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val incomeBillDao = database.incomeBillDao()
    private val dictDao = database.dictDao()
    private val incomeBillRepository = IncomeBillRepository(incomeBillDao)
    private val dictRepository = DictRepository(dictDao)
    private val _isLoadingMore = MutableLiveData(false)

    private val pageSize = 20
    private var currentPage = 0
    private var isAllLoaded = false

    private val _rawBills = mutableListOf<IncomeBill>()

    private val _adapterItems = MutableLiveData<List<Any>?>(null)
    val adapterItems: LiveData<List<Any>?> = _adapterItems

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    fun loadFirstPage() {
        currentPage = 0
        isAllLoaded = false
        _rawBills.clear()
        _adapterItems.value = null
        loadNextPage()
    }

    fun loadNextPage() {
        if (isAllLoaded) return

        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val offset = currentPage * pageSize
                val newBills = incomeBillRepository.getBillsPaged(pageSize, offset)

                _rawBills.addAll(newBills)

                if (newBills.size < pageSize) {
                    isAllLoaded = true
                    _hasMore.value = false
                } else {
                    _hasMore.value = true
                }

                val items = withContext(Dispatchers.IO) {
                    convertToAdapterItems(_rawBills)
                }
                _adapterItems.postValue(items)

                currentPage++
            } catch (e: Exception) {
                e.printStackTrace()
                _adapterItems.postValue(emptyList())
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * 收入账单同样按 billMonth 分组
     */
    private suspend fun convertToAdapterItems(
        bills: List<IncomeBill>
    ): List<Any> {
        if (bills.isEmpty()) return emptyList()

        val items = mutableListOf<Any>()

        // 按 billMonth 降序，再按 postDate 降序
        val sortedBills = bills.sortedWith(
            compareByDescending<IncomeBill> { it.billMonth }
                .thenByDescending { it.postDate }
        )

        var lastMonth = 0
        var monthTotal = 0
        var monthBills = mutableListOf<BillListAdapter.BillItem>()

        for (bill in sortedBills) {
            val categoryName = dictRepository.getCategoryName(bill.categoryId)

            val billItem = BillListAdapter.BillItem(
                id = bill.id,
                categoryName = categoryName,
                channelName = "—",
                amount = bill.amount,
                isIncome = true,
                remark = "",
                payDate = bill.postDate,
                billMonth = bill.billMonth
            )

            if (bill.billMonth != lastMonth && lastMonth != 0) {
                items.add(BillListAdapter.MonthHeader(
                    monthInt = lastMonth,
                    monthTotal = monthTotal
                ))
                items.addAll(monthBills)
                monthBills = mutableListOf()
                monthTotal = 0
            }

            lastMonth = bill.billMonth
            monthTotal += bill.amount
            monthBills.add(billItem)
        }

        if (_hasMore.value == true) {
            items.add(BillListAdapter.LoadingPlaceholder(isLoading = true))
        } else if (bills.isNotEmpty()) {
            items.add(BillListAdapter.LoadingPlaceholder(isLoading = false))
        }

        return items
    }

    fun refresh() {
        loadFirstPage()
    }

    fun deleteBill(billId: Long) {
        viewModelScope.launch {
            incomeBillRepository.deleteBillById(billId)
            refresh()
        }
    }
}
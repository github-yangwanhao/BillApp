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
import cn.yangwanhao.billapp.ui.adapter.IncomeListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class IncomeBillViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val incomeBillDao = database.incomeBillDao()
    private val dictDao = database.dictDao()
    private val incomeBillRepository = IncomeBillRepository(incomeBillDao)
    private val dictRepository = DictRepository(dictDao)

    private val pageSize = 20
    private var currentPage = 0
    private var isAllLoaded = false

    private val _rawBills = mutableListOf<IncomeBill>()

    // 适配器数据
    private val _adapterItems = MutableLiveData<List<IncomeListAdapter.IncomeListItem>?>()
    val adapterItems: LiveData<List<IncomeListAdapter.IncomeListItem>?> = _adapterItems

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    // 统计信息
    private val _monthlyTotal = MutableLiveData<Int>(0)
    val monthlyTotal: LiveData<Int> = _monthlyTotal

    private val _monthlyCount = MutableLiveData<Int>(0)
    val monthlyCount: LiveData<Int> = _monthlyCount

    private val _crossMonthCount = MutableLiveData<Int>(0)
    val crossMonthCount: LiveData<Int> = _crossMonthCount

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val currentMonth: Int
        get() {
            val calendar = Calendar.getInstance()
            return calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1)
        }

    fun loadFirstPage() {
        currentPage = 0
        isAllLoaded = false
        _rawBills.clear()
        _adapterItems.value = null
        loadNextPage()
    }

    fun loadNextPage() {
        if (isAllLoaded) return

        _isLoading.value = true
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

                // 计算统计信息（基于已加载的所有数据）
                loadMonthStats(currentMonth)

                currentPage++
            } catch (e: Exception) {
                e.printStackTrace()
                _adapterItems.postValue(emptyList())
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 将 IncomeBill 列表转换为 IncomeListAdapter 所需的数据结构
     * 按 billMonth（所属月份）分组，同月内按 createTime 倒序
     */
    private suspend fun convertToAdapterItems(
        bills: List<IncomeBill>
    ): List<IncomeListAdapter.IncomeListItem> {
        if (bills.isEmpty()) return emptyList()

        val items = mutableListOf<IncomeListAdapter.IncomeListItem>()

        // 按 billMonth 降序，同月内按 createTime 降序
        val sortedBills = bills.sortedWith(
            compareByDescending<IncomeBill> { it.billMonth }
                .thenByDescending { it.createTime }
        )

        var lastMonth = 0
        var monthTotal = 0
        var monthBills = mutableListOf<IncomeListAdapter.IncomeListItem.IncomeItem>()

        for (bill in sortedBills) {
            val categoryName = dictRepository.getCategoryName(bill.categoryId)

            val item = IncomeListAdapter.IncomeListItem.IncomeItem(
                id = bill.id,
                categoryName = categoryName,
                amount = bill.amount,
                postDate = bill.postDate,
                billMonth = bill.billMonth,
                remark = bill.remark,
                createTime = bill.createTime
            )

            if (bill.billMonth != lastMonth && lastMonth != 0) {
                items.add(IncomeListAdapter.IncomeListItem.MonthHeader(lastMonth, monthTotal))
                items.addAll(monthBills)
                monthBills = mutableListOf()
                monthTotal = 0
            }

            lastMonth = bill.billMonth
            monthTotal += bill.amount
            monthBills.add(item)
        }

        // 添加最后一个月份
        if (lastMonth != 0) {
            items.add(IncomeListAdapter.IncomeListItem.MonthHeader(lastMonth, monthTotal))
            items.addAll(monthBills)
        }

        // 添加加载状态
        if (_hasMore.value == true) {
            items.add(IncomeListAdapter.IncomeListItem.LoadingPlaceholder(isLoading = true))
        } else if (bills.isNotEmpty()) {
            items.add(IncomeListAdapter.IncomeListItem.LoadingPlaceholder(isLoading = false))
        }

        return items
    }

    /**
     * 计算统计信息
     */
    /**
     * 加载当前月的统计信息（从数据库直接查询）
     */
    fun loadMonthStats(billMonth: Int) {
        viewModelScope.launch {
            try {
                val total = incomeBillRepository.getMonthTotal(billMonth)
                val count = incomeBillRepository.countByBillMonth(billMonth)
                _monthlyTotal.postValue(total)
                _monthlyCount.postValue(count)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
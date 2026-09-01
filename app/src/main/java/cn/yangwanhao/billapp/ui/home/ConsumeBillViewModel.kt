package cn.yangwanhao.billapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.entity.ConsumeBill
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.ui.adapter.BillListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConsumeBillViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val consumeBillDao = database.consumeBillDao()
    private val dictDao = database.dictDao()
    private val consumeBillRepository = ConsumeBillRepository(consumeBillDao)
    private val dictRepository = DictRepository(dictDao)
    private val _isLoadingMore = MutableLiveData(false)

    // ========== 分页相关 ==========
    private val pageSize = 20
    private var currentPage = 0
    private var isAllLoaded = false

    // ========== 原始数据 ==========
    private val _rawBills = mutableListOf<ConsumeBill>()

    // ========== 转换后的 Adapter 数据 ==========
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
                val newBills = consumeBillRepository.getBillsPaged(pageSize, offset)

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
     * 将 ConsumeBill 列表转换为 DateHeader + BillItem 混合列表
     * 🔥 核心改动：按 billMonth 分组（权责发生制）
     */
    private suspend fun convertToAdapterItems(
        bills: List<ConsumeBill>
    ): List<Any> {
        if (bills.isEmpty()) return emptyList()

        val items = mutableListOf<Any>()

        val sortedBills = bills.sortedWith(
            compareByDescending<ConsumeBill> { it.billMonth }
                .thenByDescending { it.payDate }
        )

        var lastMonth = 0
        var monthTotal = 0
        var monthBills = mutableListOf<BillListAdapter.BillItem>()

        for (bill in sortedBills) {
            val categoryName = dictRepository.getCategoryName(bill.categoryId)
            val channelName = dictRepository.getChannelName(bill.payChannelId)

            val billItem = BillListAdapter.BillItem(
                id = bill.id,
                categoryName = categoryName,
                channelName = channelName,
                amount = bill.amount,
                isIncome = false,
                remark = bill.remark,
                payDate = bill.payDate,
                billMonth = bill.billMonth,
                isFirstInDay = false  // 稍后标记
            )

            if (bill.billMonth != lastMonth && lastMonth != 0) {
                // 🔥 标记当月每天第一条
                markFirstInDay(monthBills)
                items.add(BillListAdapter.MonthHeader(lastMonth, monthTotal))
                items.addAll(monthBills)
                monthBills = mutableListOf()
                monthTotal = 0
            }

            lastMonth = bill.billMonth
            monthTotal += bill.amount
            monthBills.add(billItem)
        }

        if (lastMonth != 0) {
            markFirstInDay(monthBills)
            items.add(BillListAdapter.MonthHeader(lastMonth, monthTotal))
            items.addAll(monthBills)
        }

        if (_hasMore.value == true) {
            items.add(BillListAdapter.LoadingPlaceholder(isLoading = true))
        } else if (bills.isNotEmpty()) {
            items.add(BillListAdapter.LoadingPlaceholder(isLoading = false))
        }

        return items
    }

    /**
     * 标记每天的第一条账单
     */
    private fun markFirstInDay(bills: MutableList<BillListAdapter.BillItem>) {
        if (bills.isEmpty()) return
        var lastPayDate = 0
        for (bill in bills) {
            if (bill.payDate != lastPayDate) {
                // 新的一天
                bill.isFirstInDay = true
                lastPayDate = bill.payDate
            }
        }
    }

    fun refresh() {
        loadFirstPage()
    }

    fun deleteBill(billId: Long) {
        viewModelScope.launch {
            consumeBillRepository.deleteBillById(billId)
            refresh()
        }
    }
}
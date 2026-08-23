package cn.yangwanhao.billapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.entity.ConsumeBill
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.ui.adapter.BillListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConsumeBillViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val consumeBillDao = database.consumeBillDao()
    private val dictDao = database.dictDao()
    private val repository = ConsumeBillRepository(consumeBillDao, dictDao)
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    // ========== 分页相关 ==========
    private val pageSize = 20
    private var currentPage = 0
    private var isLoading = false
    private var isAllLoaded = false

    // ========== 原始数据（用于分页累加） ==========
    private val _rawBills = mutableListOf<ConsumeBill>()

    // ========== 转换后的 Adapter 数据 ==========
    // 🔥 初始值设为 null，区分“未加载”和“已加载为空”
    private val _adapterItems = MutableLiveData<List<Any>?>(null)
    val adapterItems: LiveData<List<Any>?> = _adapterItems

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    /**
     * 加载第一页
     */
    fun loadFirstPage() {
        currentPage = 0
        isAllLoaded = false
        _rawBills.clear()
        // 🔥 先设置为 null，表示正在加载
        _adapterItems.value = null
        loadNextPage()
    }

    fun loadNextPage() {
        // 如果正在加载 或 已全部加载，则直接返回
        if (isLoading || isAllLoaded) return

        isLoading = true
        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val offset = currentPage * pageSize
                val newBills = repository.getBillsPaged(pageSize, offset)

                _rawBills.addAll(newBills)

                // 🔥 判断是否已全部加载
                if (newBills.size < pageSize) {
                    isAllLoaded = true
                    _hasMore.value = false
                } else {
                    _hasMore.value = true
                }

                // 转换数据
                val items = withContext(Dispatchers.IO) {
                    convertToAdapterItems(_rawBills, isLoading)
                }
                // 🔥 使用 postValue 确保在主线程更新
                _adapterItems.postValue(items)

                currentPage++
            } catch (e: Exception) {
                e.printStackTrace()
                // 🔥 出错时也返回空列表
                _adapterItems.postValue(emptyList())
            } finally {
                isLoading = false
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun convertToAdapterItems(
        bills: List<ConsumeBill>,
        isLoading: Boolean
    ): List<Any> {
        if (bills.isEmpty()) return emptyList()

        val items = mutableListOf<Any>()
        var lastDate = 0
        val sortedBills = bills.sortedByDescending { it.payDate }

        for (bill in sortedBills) {
            val categoryName = repository.getCategoryName(bill.categoryId)
            val channelName = repository.getChannelName(bill.payChannelId)

            val billItem = BillListAdapter.BillItem(
                id = bill.id,
                categoryName = categoryName,
                channelName = channelName,
                amount = bill.amount,
                isIncome = false,
                remark = bill.remark,
                payDate = bill.payDate
            )

            if (bill.payDate != lastDate) {
                val dayTotal = repository.getDayTotal(bill.payDate)
                items.add(BillListAdapter.DateHeader(bill.payDate, dayTotal))
                lastDate = bill.payDate
            }
            items.add(billItem)
        }

        // 🔥 根据状态追加底部占位
        if (_hasMore.value == true) {
            // 还有更多数据 → 显示加载中
            items.add(BillListAdapter.LoadingPlaceholder(isLoading = true))
        } else if (bills.isNotEmpty()) {
            // 已全部加载完成 → 显示“已加载全部”
            items.add(BillListAdapter.LoadingPlaceholder(isLoading = false))
        }
        // 如果 bills 为空，不显示任何占位

        return items
    }

    /**
     * 刷新列表（保存/删除后调用）
     */
    fun refresh() {
        loadFirstPage()
    }

    // 新增删除方法
    fun deleteBill(billId: Long) {
        viewModelScope.launch {
            repository.deleteBillById(billId)
            refresh()
        }
    }
}
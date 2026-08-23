package cn.yangwanhao.billapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.entity.IncomeBill
import cn.yangwanhao.billapp.repository.IncomeBillRepository
import cn.yangwanhao.billapp.ui.adapter.BillListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IncomeBillViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val incomeBillDao = database.incomeBillDao()
    private val dictDao = database.dictDao()
    private val repository = IncomeBillRepository(incomeBillDao, dictDao)
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    // ========== 分页相关 ==========
    private val pageSize = 20
    private var currentPage = 0
    private var isLoading = false
    private var isAllLoaded = false

    // ========== 原始数据 ==========
    private val _rawBills = mutableListOf<IncomeBill>()

    // ========== 转换后的 Adapter 数据 ==========
    // 🔥 初始值设为 null，而不是 emptyList，方便观察者区分“未加载”和“已加载为空”
    private val _adapterItems = MutableLiveData<List<Any>?>(null)
    val adapterItems: LiveData<List<Any>?> = _adapterItems

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    fun loadFirstPage() {
        currentPage = 0
        isAllLoaded = false
        _rawBills.clear()
        // 🔥 先设置为 null，表示正在加载
        _adapterItems.value = null
        loadNextPage()
    }

    fun loadNextPage() {
        if (isLoading || isAllLoaded) return

        isLoading = true
        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val offset = currentPage * pageSize
                val newBills = repository.getBillsPaged(pageSize, offset)

                _rawBills.addAll(newBills)

                if (newBills.size < pageSize) {
                    isAllLoaded = true
                    _hasMore.value = false
                } else {
                    _hasMore.value = true
                }

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
        bills: List<IncomeBill>,
        isLoading: Boolean
    ): List<Any> {
        if (bills.isEmpty()) return emptyList()

        val items = mutableListOf<Any>()
        var lastDate = 0
        val sortedBills = bills.sortedByDescending { it.postDate }

        for (bill in sortedBills) {
            val categoryName = repository.getCategoryName(bill.categoryId)

            val billItem = BillListAdapter.BillItem(
                id = bill.id,
                categoryName = categoryName,
                channelName = "—",
                amount = bill.amount,
                isIncome = true,
                remark = "",
                payDate = bill.postDate
            )

            if (bill.postDate != lastDate) {
                items.add(BillListAdapter.DateHeader(bill.postDate, 0))
                lastDate = bill.postDate
            }
            items.add(billItem)
        }

        // 🔥 根据状态追加底部占位
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
            repository.deleteBillById(billId)
            refresh()
        }
    }
}
package cn.yangwanhao.billapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.entity.ConsumeBill
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import kotlinx.coroutines.launch

class ConsumeBillViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val consumeBillDao = database.consumeBillDao()
    private val dictDao = database.dictDao()
    private val repository = ConsumeBillRepository(consumeBillDao, dictDao)

    // ========== 分页相关 ==========

    /** 每页加载多少条 */
    private val pageSize = 20

    /** 当前已加载到第几页（从0开始） */
    private var currentPage = 0

    /** 是否正在加载中（防止重复请求） */
    private var isLoading = false

    /** 是否已经加载完所有数据 */
    private var isAllLoaded = false

    /** 已加载的所有账单数据（不断累加） */
    private val _bills = MutableLiveData<MutableList<ConsumeBill>>(mutableListOf())
    val bills: LiveData<MutableList<ConsumeBill>> = _bills

    /** 是否还有更多数据 */
    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    /**
     * 加载第一页（首次进入页面时调用）
     */
    fun loadFirstPage() {
        currentPage = 0
        isAllLoaded = false
        _bills.value = mutableListOf()
        loadNextPage()
    }

    /**
     * 加载下一页（用户滑到底部时调用）
     */
    fun loadNextPage() {
        // 如果正在加载或已全部加载完，就直接返回
        if (isLoading || isAllLoaded) return

        isLoading = true
        viewModelScope.launch {
            val offset = currentPage * pageSize
            val newList = repository.getBillsPaged(pageSize, offset)

            // 把新数据追加到已有列表后面
            val currentList = _bills.value ?: mutableListOf()
            currentList.addAll(newList)
            _bills.value = currentList

            // 如果返回的数据不足一页，说明没有更多了
            if (newList.size < pageSize) {
                isAllLoaded = true
                _hasMore.value = false
            }

            currentPage++
            isLoading = false
        }
    }

    /**
     * 新增一条支出账单后，重新从第一页开始加载
     */
    fun insert(bill: ConsumeBill) {
        viewModelScope.launch {
            repository.addBill(bill)
            // 插入成功后，重新加载第一页
            loadFirstPage()
        }
    }

    /**
     * 删除一条支出账单
     */
    fun delete(bill: ConsumeBill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            loadFirstPage()
        }
    }
}
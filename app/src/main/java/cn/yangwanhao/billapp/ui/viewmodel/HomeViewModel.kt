package cn.yangwanhao.billapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.BillApplication
import cn.yangwanhao.billapp.common.DateUtil
import cn.yangwanhao.billapp.entity.ConsumeBill
import cn.yangwanhao.billapp.entity.IncomeBill
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.IncomeBillRepository
import cn.yangwanhao.billapp.ui.adapter.BillListAdapter
import cn.yangwanhao.billapp.ui.adapter.DictSpinnerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // ========== 从 Application 获取 Repository ==========
    private val app = application as BillApplication
    private val consumeBillRepo: ConsumeBillRepository = app.consumeBillRepository
    private val incomeBillRepo: IncomeBillRepository = app.incomeBillRepository
    private val dictRepo: DictRepository = app.dictRepository

    // ========== 当前选中月份 ==========
    private val _currentMonth = MutableLiveData(DateUtil.getCurrentMonthInt())
    val currentMonth: LiveData<Int> = _currentMonth

    // ========== 月度支出合计（单位：分） ==========
    private val _monthTotal = MutableLiveData(0)
    val monthTotal: LiveData<Int> = _monthTotal

    // ========== 账单列表适配器数据 ==========
    private val _billItems = MutableLiveData<List<Any>>()
    val billItems: LiveData<List<Any>> = _billItems

    // ========== 消费分类下拉数据 ==========
    private val _consumeCategories = MutableLiveData<List<DictSpinnerAdapter.DictItem>>()
    val consumeCategories: LiveData<List<DictSpinnerAdapter.DictItem>> = _consumeCategories

    // ========== 收入分类下拉数据 ==========
    private val _incomeCategories = MutableLiveData<List<DictSpinnerAdapter.DictItem>>()
    val incomeCategories: LiveData<List<DictSpinnerAdapter.DictItem>> = _incomeCategories

    // ========== 支付渠道下拉数据 ==========
    private val _payChannels = MutableLiveData<List<DictSpinnerAdapter.DictItem>>()
    val payChannels: LiveData<List<DictSpinnerAdapter.DictItem>> = _payChannels

    // ========== 操作结果提示 ==========
    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    init {
        // 初始化时加载分类和渠道数据
        loadDictData()
        // 加载当月账单
        loadBills()
    }

    /** 切换到上一个月 */
    fun previousMonth() {
        val prev = DateUtil.getPreviousMonth(_currentMonth.value ?: return)
        _currentMonth.value = prev
        loadBills()
    }

    /** 切换到下一个月 */
    fun nextMonth() {
        val current = _currentMonth.value ?: return
        val sdf = java.text.SimpleDateFormat("yyyyMM", java.util.Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        try {
            calendar.time = sdf.parse(current.toString())!!
            calendar.add(java.util.Calendar.MONTH, 1)
            _currentMonth.value = sdf.format(calendar.time).toInt()
        } catch (e: Exception) {
            // 解析失败，不做操作
        }
        loadBills()
    }

    /** 加载当前月份的账单列表和汇总 */
    private fun loadBills() {
        val month = _currentMonth.value ?: return
        viewModelScope.launch {
            // 1. 查询消费账单
            val consumeBills = withContext(Dispatchers.IO) {
                consumeBillRepo.getBillsByMonth(month)
            }
            // 2. 查询收入账单
            val incomeBills = withContext(Dispatchers.IO) {
                incomeBillRepo.getBillsByMonth(month)
            }
            // 3. 计算月度支出合计
            val total = withContext(Dispatchers.IO) {
                consumeBillRepo.getMonthTotal(month)
            }
            _monthTotal.value = total

            // 4. 组装混排列表数据（按日期分组）
            val allItems = mutableListOf<Any>()

            // 将消费账单转为 BillItem
            val consumeItems = consumeBills.map { bill ->
                val categoryName = withContext(Dispatchers.IO) {
                    consumeBillRepo.getCategoryName(bill.categoryId)
                }
                val channelName = withContext(Dispatchers.IO) {
                    consumeBillRepo.getChannelName(bill.payChannelId)
                }
                BillListAdapter.BillItem(
                    id = bill.id,
                    categoryName = categoryName,
                    channelName = channelName,
                    amount = bill.amount,
                    isIncome = false
                )
            }

            // 将收入账单转为 BillItem
            val incomeItems = incomeBills.map { bill ->
                val categoryName = withContext(Dispatchers.IO) {
                    incomeBillRepo.getCategoryName(bill.categoryId)
                }
                BillListAdapter.BillItem(
                    id = bill.id,
                    categoryName = categoryName,
                    channelName = "",
                    amount = bill.amount,
                    isIncome = true
                )
            }

            // 合并并按日期降序排列
            val allBills = (consumeItems + incomeItems)
                .sortedByDescending {
                    if (consumeBills.any { b -> b.id == it.id }) {
                        consumeBills.first { b -> b.id == it.id }.payDate
                    } else {
                        incomeBills.first { b -> b.id == it.id }.postDate
                    }
                }

            // 按日期分组，插入日期头
            var lastDate = 0
            allBills.forEach { billItem ->
                val dateInt = if (consumeBills.any { it.id == billItem.id }) {
                    consumeBills.first { it.id == billItem.id }.payDate
                } else {
                    incomeBills.first { it.id == billItem.id }.postDate
                }

                if (dateInt != lastDate) {
                    // 计算当日支出合计
                    val dayTotal = withContext(Dispatchers.IO) {
                        consumeBillRepo.getDayTotal(dateInt)
                    }
                    allItems.add(BillListAdapter.DateHeader(dateInt, dayTotal))
                    lastDate = dateInt
                }
                allItems.add(billItem)
            }

            _billItems.value = allItems
        }
    }

    /** 加载字典数据（分类 + 支付渠道） */
    private fun loadDictData() {
        viewModelScope.launch {
            // 消费分类
            val consumeCats = withContext(Dispatchers.IO) {
                dictRepo.getConsumeCategories().map {
                    DictSpinnerAdapter.DictItem(it.id, it.dictValue)
                }
            }
            _consumeCategories.value = consumeCats

            // 收入分类
            val incomeCats = withContext(Dispatchers.IO) {
                dictRepo.getIncomeCategories().map {
                    DictSpinnerAdapter.DictItem(it.id, it.dictValue)
                }
            }
            _incomeCategories.value = incomeCats

            // 支付渠道
            val channels = withContext(Dispatchers.IO) {
                dictRepo.getPayChannels().map {
                    DictSpinnerAdapter.DictItem(it.id, it.dictValue)
                }
            }
            _payChannels.value = channels
        }
    }

    /** 新增一笔消费账单 */
    fun addConsumeBill(
        amount: Int,
        categoryId: Int,
        payChannelId: Int,
        remark: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            val dateInt = sdf.format(java.util.Date(now)).toInt()
            val monthInt = _currentMonth.value ?: DateUtil.getCurrentMonthInt()

            val bill = ConsumeBill(
                amount = amount,
                categoryId = categoryId,
                payChannelId = payChannelId,
                payDate = dateInt,
                billMonth = monthInt,
                remark = remark,
                billKind = "NORMAL",
                createTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(now))
            )

            withContext(Dispatchers.IO) {
                consumeBillRepo.addBill(bill)
            }

            _toastMessage.value = "支出已记录"
            loadBills() // 刷新列表
        }
    }

    /** 新增一笔收入账单 */
    fun addIncomeBill(
        amount: Int,
        categoryId: Int,
        remark: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            val dateInt = sdf.format(java.util.Date(now)).toInt()
            val monthInt = _currentMonth.value ?: DateUtil.getCurrentMonthInt()

            val bill = IncomeBill(
                amount = amount,
                categoryId = categoryId,
                postDate = dateInt,
                billMonth = monthInt,
                remark = remark,
                createTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(now))
            )

            withContext(Dispatchers.IO) {
                incomeBillRepo.addBill(bill)
            }

            _toastMessage.value = "收入已记录"
            loadBills() // 刷新列表
        }
    }

    /** 消费提示消息已被读取 */
    fun onToastConsumed() {
        _toastMessage.value = null
    }
}
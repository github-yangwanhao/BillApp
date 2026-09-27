package cn.yangwanhao.billapp.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.StatsRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BillDatabase.getDatabase(application)
    private val statsRepository = StatsRepository(
        consumeBillDao = database.consumeBillDao(),
        incomeBillDao = database.incomeBillDao(),
        dictRepository = DictRepository(database.dictDao())
    )

    private val _uiState = MutableLiveData(StatsUiState())
    val uiState: LiveData<StatsUiState> = _uiState

    // 各模式独立记忆的时间（yyyyMM / yyyy）
    private val monthPeriod = mutableMapOf<StatsViewMode, Int>()
    private val yearPeriod = mutableMapOf<StatsViewMode, Int>()

    // 🔥 全局月份边界（来自数据库）
    private var globalMinMonth: Int? = null
    private var globalMaxMonth: Int? = null

    init {
        // 默认当前月 / 当前年
        val now = Calendar.getInstance()
        val ym = now.get(Calendar.YEAR) * 100 + (now.get(Calendar.MONTH) + 1)
        val y = now.get(Calendar.YEAR)
        monthPeriod[StatsViewMode.MONTH] = ym
        yearPeriod[StatsViewMode.YEAR] = y

        // 启动时加载一次边界
        loadGlobalRange()
    }

    private fun loadGlobalRange() {
        viewModelScope.launch {
            try {
                val (min, max) = statsRepository.getGlobalMonthRange()
                globalMinMonth = min
                globalMaxMonth = max
                // 边界加载完成后刷新一次（用于更新箭头状态）
                refresh()
            } catch (e: Exception) {
                e.printStackTrace()
                refresh()
            }
        }
    }

    fun setViewMode(mode: StatsViewMode) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(viewMode = mode)
        refresh()
    }

    fun setCategoryTab(tab: StatsCategoryTab) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(categoryTab = tab)
        refresh()
    }

    fun prevPeriod() {
        val state = _uiState.value ?: return
        if (!state.canPrev) return       // 🔥 边界保护
        when (state.viewMode) {
            StatsViewMode.MONTH -> {
                val cur = monthPeriod[StatsViewMode.MONTH] ?: return
                monthPeriod[StatsViewMode.MONTH] = prevMonth(cur)
            }
            StatsViewMode.YEAR -> {
                val cur = yearPeriod[StatsViewMode.YEAR] ?: return
                yearPeriod[StatsViewMode.YEAR] = cur - 1
            }
            StatsViewMode.ALL -> return
        }
        refresh()
    }

    fun nextPeriod() {
        val state = _uiState.value ?: return
        if (!state.canNext) return       // 🔥 边界保护
        when (state.viewMode) {
            StatsViewMode.MONTH -> {
                val cur = monthPeriod[StatsViewMode.MONTH] ?: return
                monthPeriod[StatsViewMode.MONTH] = nextMonth(cur)
            }
            StatsViewMode.YEAR -> {
                val cur = yearPeriod[StatsViewMode.YEAR] ?: return
                yearPeriod[StatsViewMode.YEAR] = cur + 1
            }
            StatsViewMode.ALL -> return
        }
        refresh()
    }

    fun refresh() {
        val state = _uiState.value ?: return
        viewModelScope.launch {
            try {
                val (start, end) = resolveRange(state.viewMode)
                val periodText = resolvePeriodText(state.viewMode)
                val (canPrev, canNext) = resolveBounds(state.viewMode)

                val summary = statsRepository.getPeriodSummary(start, end)
                val categoryRank = statsRepository.getCategoryRank(
                    start, end, isIncome = state.categoryTab == StatsCategoryTab.INCOME
                )
                val channelRank = if (state.categoryTab == StatsCategoryTab.EXPENSE) {
                    statsRepository.getChannelRank(start, end)
                } else emptyList()

                val trend = when (state.viewMode) {
                    StatsViewMode.MONTH -> {
                        val ym = monthPeriod[StatsViewMode.MONTH] ?: return@launch
                        statsRepository.getMonthlyTrend(ym, ym, ym / 100, ym % 100)
                    }
                    StatsViewMode.YEAR -> {
                        val y = yearPeriod[StatsViewMode.YEAR] ?: return@launch
                        statsRepository.getYearlyTrend(y, y * 100 + 1, y * 100 + 12)
                    }
                    StatsViewMode.ALL -> statsRepository.getAllTrend()
                }

                _uiState.value = state.copy(
                    periodText = periodText,
                    summary = summary,
                    categoryRank = categoryRank,
                    channelRank = channelRank,
                    trendPoints = trend,
                    canPrev = canPrev,
                    canNext = canNext
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 🔥 计算当前模式下的翻页边界
     * @return Pair(canPrev, canNext)
     */
    private fun resolveBounds(mode: StatsViewMode): Pair<Boolean, Boolean> {
        if (mode == StatsViewMode.ALL) return false to false

        // 边界未加载时，不禁用（避免首次进来箭头全是灰的）
        val minM = globalMinMonth ?: return true to true
        val maxM = globalMaxMonth ?: return true to true

        return when (mode) {
            StatsViewMode.MONTH -> {
                val cur = monthPeriod[StatsViewMode.MONTH] ?: return false to false
                (cur > minM) to (cur < maxM)
            }
            StatsViewMode.YEAR -> {
                val cur = yearPeriod[StatsViewMode.YEAR] ?: return false to false
                val minY = minM / 100
                val maxY = maxM / 100
                (cur > minY) to (cur < maxY)
            }
            StatsViewMode.ALL -> false to false
        }
    }

    private fun resolveRange(mode: StatsViewMode): Pair<Int, Int> {
        return when (mode) {
            StatsViewMode.MONTH -> {
                val ym = monthPeriod[StatsViewMode.MONTH] ?: 0
                ym to ym
            }
            StatsViewMode.YEAR -> {
                val y = yearPeriod[StatsViewMode.YEAR] ?: 0
                (y * 100 + 1) to (y * 100 + 12)
            }
            StatsViewMode.ALL -> 0 to 999999
        }
    }

    private fun resolvePeriodText(mode: StatsViewMode): String {
        return when (mode) {
            StatsViewMode.MONTH -> {
                val ym = monthPeriod[StatsViewMode.MONTH] ?: 0
                String.format("%04d年%02d月", ym / 100, ym % 100)
            }
            StatsViewMode.YEAR -> {
                val y = yearPeriod[StatsViewMode.YEAR] ?: 0
                "${y}年"
            }
            StatsViewMode.ALL -> "全部"
        }
    }

    /** 供下钻使用 */
    fun getCurrentRange(): Pair<Int, Int> {
        val state = _uiState.value ?: return 0 to 999999
        return resolveRange(state.viewMode)
    }

    fun getCurrentPeriodText(): String {
        val state = _uiState.value ?: return ""
        return resolvePeriodText(state.viewMode)
    }

    private fun prevMonth(ym: Int): Int {
        var y = ym / 100
        var m = ym % 100 - 1
        if (m < 1) { m = 12; y-- }
        return y * 100 + m
    }

    private fun nextMonth(ym: Int): Int {
        var y = ym / 100
        var m = ym % 100 + 1
        if (m > 12) { m = 1; y++ }
        return y * 100 + m
    }
}
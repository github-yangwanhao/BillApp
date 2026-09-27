package cn.yangwanhao.billapp.repository

import cn.yangwanhao.billapp.dao.ConsumeBillDao
import cn.yangwanhao.billapp.dao.IncomeBillDao
import cn.yangwanhao.billapp.dto.CategorySum
import cn.yangwanhao.billapp.dto.ChannelSum
import cn.yangwanhao.billapp.dto.DrillItem
import cn.yangwanhao.billapp.dto.PeriodSummary
import cn.yangwanhao.billapp.dto.TrendPoint
import cn.yangwanhao.billapp.dto.TrendPoint.TrendKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 统计专用仓库，聚合两张账单表的查询
 */
class StatsRepository(
    private val consumeBillDao: ConsumeBillDao,
    private val incomeBillDao: IncomeBillDao,
    private val dictRepository: DictRepository
) {

    /** 时间区间的全量"最小/最大"边界，用于"全部/汇总"模式 */
    suspend fun getFullRange(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        // 这里没有专门的查询方法，直接用 0..999999 表示所有月份
        // 后续如需精确"最早/最新月份"，可在 DAO 加 MIN/MAX 查询
        0 to 999999
    }

    /** 某区间的收支汇总（收入 + 支出 + 笔数） */
    suspend fun getPeriodSummary(start: Int, end: Int): PeriodSummary =
        withContext(Dispatchers.IO) {
            val incomeTotal = incomeBillDao.sumByRange(start, end)
            val incomeCount = incomeBillDao.countByRange(start, end)
            val expenseTotal = consumeBillDao.sumByRange(start, end)
            val expenseCount = consumeBillDao.countByRange(start, end)
            PeriodSummary(
                incomeTotal = incomeTotal,
                incomeCount = incomeCount,
                expenseTotal = expenseTotal,
                expenseCount = expenseCount
            )
        }

    // ============================================================
    // 折线图：三种粒度
    // ============================================================

    /**
     * 月度折线图：按日聚合（支出走 PAY_DATE，收入走 POST_DATE）
     * @param start,end BILL_MONTH 区间（月度时 start == end）
     * @param year,month 用于生成 X 轴标签
     * @return 该月每一天的 TrendPoint，未补齐空的天
     */
    suspend fun getMonthlyTrend(
        start: Int,
        end: Int,
        year: Int,
        month: Int
    ): List<TrendPoint> = withContext(Dispatchers.IO) {
        val expenseMap = consumeBillDao.sumGroupByPostDate(start, end)
            .associate { it.period to it.total }
        val incomeMap = incomeBillDao.sumGroupByPostDate(start, end)
            .associate { it.period to it.total }

        // 该月天数
        val daysInMonth = java.util.Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        (1..daysInMonth).map { day ->
            val payDate = year * 10000 + month * 100 + day
            TrendPoint(
                label = "${day}日",
                income = incomeMap[payDate] ?: 0,
                expense = expenseMap[payDate] ?: 0,
                kind = TrendKind.DAY
            )
        }
    }

    /**
     * 年度折线图：按 BILL_MONTH 聚合
     * @param year 年（如 2026）
     * @param startMonth,endMonth 限制月份范围（当前年可能只到 9 月）
     */
    suspend fun getYearlyTrend(
        year: Int,
        startMonth: Int,
        endMonth: Int
    ): List<TrendPoint> = withContext(Dispatchers.IO) {
        val expenseMap = consumeBillDao.sumGroupByBillMonth(startMonth, endMonth)
            .associate { it.period to it.total }
        val incomeMap = incomeBillDao.sumGroupByBillMonth(startMonth, endMonth)
            .associate { it.period to it.total }

        (1..12).map { m ->
            val ym = year * 100 + m
            TrendPoint(
                label = "${m}月",
                income = incomeMap[ym] ?: 0,
                expense = expenseMap[ym] ?: 0,
                kind = TrendKind.MONTH
            )
        }
    }

    /**
     * 汇总折线图：按年聚合
     */
    suspend fun getAllTrend(): List<TrendPoint> = withContext(Dispatchers.IO) {
        val expenseMap = consumeBillDao.sumGroupByYear().associate { it.period to it.total }
        val incomeMap = incomeBillDao.sumGroupByYear().associate { it.period to it.total }

        val years = (expenseMap.keys + incomeMap.keys).sorted()
        years.map { y ->
            TrendPoint(
                label = y.toString(),
                income = incomeMap[y] ?: 0,
                expense = expenseMap[y] ?: 0,
                kind = TrendKind.YEAR
            )
        }
    }

    // ============================================================
    // 分类排行
    // ============================================================

    /**
     * 区间内按分类聚合
     * @param isIncome true=收入，false=支出
     */
    suspend fun getCategoryRank(
        start: Int,
        end: Int,
        isIncome: Boolean
    ): List<CategorySum> = withContext(Dispatchers.IO) {
        val rawList = if (isIncome) {
            incomeBillDao.sumGroupByCategory(start, end)
        } else {
            consumeBillDao.sumGroupByCategory(start, end)
        }
        // 补分类名
        rawList.map {
            CategorySum(
                categoryId = it.categoryId,
                categoryName = dictRepository.getCategoryName(it.categoryId),
                total = it.total
            )
        }
    }

    // ============================================================
    // 支付方式排行（仅支出）
    // ============================================================

    suspend fun getChannelRank(start: Int, end: Int): List<ChannelSum> =
        withContext(Dispatchers.IO) {
            consumeBillDao.sumGroupByChannel(start, end).map {
                ChannelSum(
                    channelId = it.channelId,
                    channelName = dictRepository.getChannelName(it.channelId),
                    total = it.total
                )
            }
        }

    // ============================================================
    // 下钻明细
    // ============================================================

    /**
     * 查某分类在区间内的所有明细
     */
    suspend fun getDrillList(
        categoryId: Int,
        start: Int,
        end: Int,
        isIncome: Boolean
    ): List<DrillItem> = withContext(Dispatchers.IO) {
        val rawList = if (isIncome) {
            incomeBillDao.drillByCategory(categoryId, start, end)
        } else {
            consumeBillDao.drillByCategory(categoryId, start, end)
        }
        rawList.map {
            DrillItem(
                id = it.id,
                date = it.date,
                remark = it.remark,
                amount = it.amount
            )
        }
    }

    /**
     * 🔥 获取全表的 BILL_MONTH 边界（收入+支出合并）
     * @return Pair(minMonth, maxMonth)，均可能为 null（无数据时）
     */
    suspend fun getGlobalMonthRange(): Pair<Int?, Int?> = withContext(Dispatchers.IO) {
        val consumeMin = consumeBillDao.getMinBillMonth()
        val consumeMax = consumeBillDao.getMaxBillMonth()
        val incomeMin = incomeBillDao.getMinBillMonth()
        val incomeMax = incomeBillDao.getMaxBillMonth()

        val min = listOfNotNull(consumeMin, incomeMin).minOrNull()
        val max = listOfNotNull(consumeMax, incomeMax).maxOrNull()
        min to max
    }
}
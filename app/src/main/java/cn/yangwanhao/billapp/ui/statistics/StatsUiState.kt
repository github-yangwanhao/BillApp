package cn.yangwanhao.billapp.ui.statistics

import cn.yangwanhao.billapp.dto.CategorySum
import cn.yangwanhao.billapp.dto.ChannelSum
import cn.yangwanhao.billapp.dto.PeriodSummary
import cn.yangwanhao.billapp.dto.TrendPoint

enum class StatsViewMode { MONTH, YEAR, ALL }

enum class StatsCategoryTab { EXPENSE, INCOME }

data class StatsUiState(
    val viewMode: StatsViewMode = StatsViewMode.MONTH,
    val categoryTab: StatsCategoryTab = StatsCategoryTab.EXPENSE,

    // 时间
    val periodText: String = "",

    // 卡片
    val summary: PeriodSummary = PeriodSummary(0, 0, 0, 0),

    // 折线图
    val trendPoints: List<TrendPoint> = emptyList(),

    // 排行
    val categoryRank: List<CategorySum> = emptyList(),
    val channelRank: List<ChannelSum> = emptyList(),

    // 🔥 翻页边界
    val canPrev: Boolean = false,
    val canNext: Boolean = false
)
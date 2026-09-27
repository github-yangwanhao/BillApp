package cn.yangwanhao.billapp.dto

/**
 * 某时间区间的收支汇总
 */
data class PeriodSummary(
    val incomeTotal: Int,     // 收入总额（分）
    val incomeCount: Int,     // 收入笔数
    val expenseTotal: Int,    // 支出总额（分）
    val expenseCount: Int     // 支出笔数
) {
    /** 结余（分） */
    val balance: Int get() = incomeTotal - expenseTotal

    /** 结余占收入百分比（收入为 0 时返回 null） */
    val balancePercent: Int?
        get() = if (incomeTotal == 0) null
        else Math.round(balance.toFloat() / incomeTotal * 100)
}
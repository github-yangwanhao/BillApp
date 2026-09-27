package cn.yangwanhao.billapp.dao

/**
 * Room 直接映射的聚合结果（不要在 UI 层使用）
 * period 的含义由具体查询决定：
 *  - 按 BILL_MONTH：period = yyyyMM
 *  - 按 POST_DATE：period = yyyyMMdd
 *  - 按年份：period = yyyy
 */
data class PeriodSum(
    val period: Int,
    val total: Int
)

data class CategorySumRaw(
    val categoryId: Int,
    val total: Int
)

data class ChannelSumRaw(
    val channelId: Int,
    val total: Int
)

data class DrillItemRaw(
    val id: Long,
    val date: Int,
    val remark: String,
    val amount: Int
)
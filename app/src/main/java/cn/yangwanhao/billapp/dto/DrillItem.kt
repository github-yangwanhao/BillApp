package cn.yangwanhao.billapp.dto

/**
 * 下钻明细条目
 */
data class DrillItem(
    val id: Long,
    val date: Int,          // yyyyMMdd（支出取 POST_DATE，收入取 POST_DATE）
    val remark: String,
    val amount: Int         // 分
)
package cn.yangwanhao.billapp.dto

/**
 * 分类聚合结果
 */
data class CategorySum(
    val categoryId: Int,
    val categoryName: String,   // 由 Repository 填充
    val total: Int              // 金额（分）
)
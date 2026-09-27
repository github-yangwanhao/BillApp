package cn.yangwanhao.billapp.dto

data class TrendPoint(
    val label: String,
    val income: Int,
    val expense: Int,
    val kind: TrendKind
) {
    enum class TrendKind { DAY, MONTH, YEAR }
}
package cn.yangwanhao.billapp.entity

/**
 * 分期账单临时数据类
 * @param installmentIndex 期数（从1开始）
 * @param amount 本期金额（单位：分）
 * @param billMonth 账单所属月份（yyyyMM）
 * @param remark 本期备注
 */
data class InstallmentBill(
    val installmentIndex: Int,
    val amount: Int,
    val billMonth: Int,
    var remark: String
)
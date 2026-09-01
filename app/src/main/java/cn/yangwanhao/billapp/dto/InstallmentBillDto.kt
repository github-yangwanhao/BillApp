package cn.yangwanhao.billapp.dto

/**
 * 分期账单临时数据类
 * @param installmentIndex 期数（从1开始）
 * @param amount 本期金额（单位：分）
 * @param billMonth 账单所属月份（yyyyMM）
 * @param remark 本期备注
 */
data class InstallmentBillDto(
    // 第X期
    val installmentIndex: Int,
    // 金额，单位分
    val amount: Int,
    // 账单所属月份
    val billMonth: Int,
    // 备注
    var remark: String
)
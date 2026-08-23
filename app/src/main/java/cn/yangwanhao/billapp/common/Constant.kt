package cn.yangwanhao.billapp.common

object Constant {

    // ==================== 字典类型 Key ====================
    const val DICT_KEY_CONSUME_CATEGORY = "CONSUME_CATEGORY"
    const val DICT_KEY_INCOME_CATEGORY = "INCOME_CATEGORY"
    const val DICT_KEY_PAY_CHANNEL = "PAY_CHANNEL"
    const val DICT_KEY_CONSUME_REMARK = "CONSUME_REMARK"
    const val DICT_KEY_INCOME_REMARK = "INCOME_REMARK"

    // ==================== 字典状态 ====================
    const val STATUS_ENABLE = "1"
    const val STATUS_DISABLE = "0"

    // ==================== 账单类型 ====================
    const val BILL_KIND_NORMAL = "NORMAL"    // 单笔账单
    const val BILL_KIND_INSTALLMENT = "INSTALLMENT" // 分期账单

    // ==================== 日期格式 ====================
    const val DATE_FORMAT_YYYYMMDD = "yyyyMMdd"
    const val DATE_FORMAT_YYYYMM = "yyyyMM"
}
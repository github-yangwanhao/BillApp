package cn.yangwanhao.billapp.common

object ImportConstants {

    /** 导入状态：成功 */
    const val STATUS_SUCCESS = "S"

    /** 导入状态：失败 */
    const val STATUS_FAILED = "F"

    /** 导入类型-支出 */
    const val IMPORT_TYPE_EXPENSE = "EXPENSE"

    /** 导入类型-收入 */
    const val IMPORT_TYPE_INCOME = "INCOME"

    /** 文件名匹配正则：yyyy-MM.xlsx 或 yyyy-MM.xls */
    val FILE_NAME_PATTERN = Regex("""^(\d{4}-\d{2})\.(xlsx|xls)$""")

    /** Excel 表头校验（第1行） */
    val HEADER_EXPECTED = listOf("日期", "备注", "分类", "支付方式", "金额")

    /** 🔥 收入表头 */
    val HEADER_INCOME_EXPECTED = listOf("年月", "到账日期", "金额", "类型", "备注")
}
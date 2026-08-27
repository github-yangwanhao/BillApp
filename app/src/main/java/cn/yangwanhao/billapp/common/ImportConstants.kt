package cn.yangwanhao.billapp.common

object ImportConstants {

    /** 导入状态：成功 */
    const val STATUS_SUCCESS = "S"

    /** 导入状态：失败 */
    const val STATUS_FAILED = "F"

    /** 文件名匹配正则：yyyy-MM.xlsx 或 yyyy-MM.xls */
    val FILE_NAME_PATTERN = Regex("""^(\d{4}-\d{2})\.(xlsx|xls)$""")

    /** Excel 表头校验（第1行） */
    val HEADER_EXPECTED = listOf("日期", "备注", "分类", "支付方式", "金额")
}
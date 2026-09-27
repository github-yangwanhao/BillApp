package cn.yangwanhao.billapp.service

import cn.yangwanhao.billapp.common.Constant
import cn.yangwanhao.billapp.dao.ConsumeBillDao
import cn.yangwanhao.billapp.dao.DictDao
import cn.yangwanhao.billapp.dao.IncomeBillDao
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Excel 导出服务
 *
 * 生成的 Excel 与导入模板完全对称：
 * - 支出：A~E 列数据 + G~H 列统计区（SUMIFS 公式，动态从字典生成）
 * - 收入：A~E 列数据，无统计区
 */
class ExportService(
    private val consumeBillDao: ConsumeBillDao,
    private val incomeBillDao: IncomeBillDao,
    private val dictDao: DictDao
) {

    /**
     * 导出指定月份的支出
     * @return 导出的数据条数
     */
    suspend fun exportExpense(billMonth: Int, outputStream: OutputStream): Int {
        val bills = consumeBillDao.getAllByBillMonth(billMonth)
        val categoryMap = dictDao.getByKey(Constant.DICT_KEY_CONSUME_CATEGORY).associateBy { it.id.toInt() }
        val channelMap = dictDao.getByKey(Constant.DICT_KEY_PAY_CHANNEL).associateBy { it.id.toInt() }
        val categories = dictDao.getByKey(Constant.DICT_KEY_CONSUME_CATEGORY)
        val channels = dictDao.getByKey(Constant.DICT_KEY_PAY_CHANNEL)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Sheet1")

        // ===== 表头（第 1 行） =====
        val headerRow = sheet.createRow(0)
        val headers = listOf("日期", "备注", "分类", "支付方式", "金额")
        headers.forEachIndexed { i, text ->
            headerRow.createCell(i).setCellValue(text)
        }

        // ===== 数据（从第 2 行开始） =====
        bills.forEachIndexed { idx, bill ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(formatDateForExpense(bill.payDate))
            row.createCell(1).setCellValue(bill.remark)
            row.createCell(2).setCellValue(categoryMap[bill.categoryId]?.dictValue ?: "未知分类")
            row.createCell(3).setCellValue(channelMap[bill.payChannelId]?.dictValue ?: "未知渠道")
            row.createCell(4).setCellValue(fenToYuan(bill.amount))
        }

        // ===== 统计区（G、H 列） =====
        // 第 1 行：总消费
        var statRowIndex = 0
        writeStatRow(sheet, statRowIndex, "总消费", "SUM(E2:E10001)")
        statRowIndex++

        // 支付方式消费（动态从字典）
        channels.forEach { channel ->
            val formula = "SUMIFS(E2:E10001,D2:D10001,\"${channel.dictValue}\")"
            writeStatRow(sheet, statRowIndex, "${channel.dictValue}消费", formula)
            statRowIndex++
        }

        // 空一行
        statRowIndex++

        // 分类消费（动态从字典）
        categories.forEach { category ->
            val formula = "SUMIFS(E2:E10001,C2:C10001,\"${category.dictValue}\")"
            writeStatRow(sheet, statRowIndex, "${category.dictValue}消费", formula)
            statRowIndex++
        }

        workbook.write(outputStream)
        workbook.close()
        return bills.size
    }

    /**
     * 导出全部收入
     * @return 导出的数据条数
     */
    suspend fun exportIncome(outputStream: OutputStream): Int {
        val bills = incomeBillDao.getAllForExport()
        val categoryMap = dictDao.getByKey(Constant.DICT_KEY_INCOME_CATEGORY)
            .associateBy { it.id.toInt() }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Sheet1")

        // ===== 表头 =====
        val headerRow = sheet.createRow(0)
        val headers = listOf("年月", "到账日期", "金额", "类型", "备注")
        headers.forEachIndexed { i, text ->
            headerRow.createCell(i).setCellValue(text)
        }

        // ===== 数据 =====
        bills.forEachIndexed { idx, bill ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(formatMonthForIncome(bill.billMonth))
            row.createCell(1).setCellValue(formatDateForIncome(bill.postDate))
            row.createCell(2).setCellValue(fenToYuan(bill.amount))
            row.createCell(3).setCellValue(categoryMap[bill.categoryId]?.dictValue ?: "未知类型")
            row.createCell(4).setCellValue(bill.remark)
        }

        workbook.write(outputStream)
        workbook.close()
        return bills.size
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /** 在指定行写入统计标签（G 列）和公式（H 列） */
    private fun writeStatRow(sheet: Sheet, rowIndex: Int, label: String, formula: String) {
        val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        row.createCell(6).setCellValue(label)       // G 列
        row.createCell(7).setCellFormula(formula)   // H 列
    }

    /** 分 → 元（保留 2 位小数，避免浮点误差） */
    private fun fenToYuan(fen: Int): Double {
        return BigDecimal(fen)
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /** yyyyMMdd → "yyyy-MM-dd" */
    private fun formatDateForExpense(dateInt: Int): String {
        val s = dateInt.toString().padStart(8, '0')
        return "${s.substring(0, 4)}-${s.substring(4, 6)}-${s.substring(6, 8)}"
    }

    /** yyyyMMdd → "yyyy/MM/dd" */
    private fun formatDateForIncome(dateInt: Int): String {
        val s = dateInt.toString().padStart(8, '0')
        return "${s.substring(0, 4)}/${s.substring(4, 6)}/${s.substring(6, 8)}"
    }

    /** yyyyMM → "yyyy-MM" */
    private fun formatMonthForIncome(monthInt: Int): String {
        val s = monthInt.toString().padStart(6, '0')
        return "${s.substring(0, 4)}-${s.substring(4, 6)}"
    }
}
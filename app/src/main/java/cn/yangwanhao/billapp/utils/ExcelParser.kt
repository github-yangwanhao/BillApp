package cn.yangwanhao.billapp.utils

import cn.yangwanhao.billapp.common.ImportConstants
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedExcelRow(
    val rowIndex: Int,
    val date: String,
    val remark: String,
    val category: String,
    val channel: String,
    val amountFen: Int
)

data class ExcelParseResult(
    val rows: List<ParsedExcelRow>,
    val errors: List<String>
)

object ExcelParser {

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )

    fun parse(file: File): ExcelParseResult {
        val errors = mutableListOf<String>()
        val rows = mutableListOf<ParsedExcelRow>()

        try {
            FileInputStream(file).use { fis ->
                val workbook: Workbook = when {
                    file.extension.equals("xlsx", ignoreCase = true) -> XSSFWorkbook(fis)
                    file.extension.equals("xls", ignoreCase = true) -> HSSFWorkbook(fis)
                    else -> {
                        return ExcelParseResult(emptyList(), listOf("不支持的文件格式：${file.extension}"))
                    }
                }

                val sheet = workbook.getSheetAt(0) ?: run {
                    workbook.close()
                    return ExcelParseResult(emptyList(), listOf("Excel 文件无有效 Sheet"))
                }

                // 校验表头
                val headerRow = sheet.getRow(0)
                if (headerRow == null) {
                    workbook.close()
                    return ExcelParseResult(emptyList(), listOf("文件为空或表头不存在"))
                }

                val actualHeaders = (0 until 5).map {
                    getStringValue(headerRow.getCell(it)) ?: ""
                }
                val expectedHeaders = ImportConstants.HEADER_EXPECTED
                if (actualHeaders != expectedHeaders) {
                    workbook.close()
                    return ExcelParseResult(
                        emptyList(),
                        listOf("表头不匹配，期望：$expectedHeaders，实际：$actualHeaders")
                    )
                }

                // 从第2行开始解析
                for (i in 1 until sheet.physicalNumberOfRows) {
                    val row = sheet.getRow(i) ?: continue
                    val rowNum = i + 1

                    val dateCell = row.getCell(0)
                    val amountCell = row.getCell(4)
                    if (dateCell == null || amountCell == null) continue

                    try {
                        // 日期
                        val dateStr = getStringValue(dateCell) ?: ""
                        val date = parseDate(dateStr)
                        if (date == null) {
                            errors.add("第${rowNum}行日期解析失败：$dateStr")
                            continue
                        }

                        // 备注
                        val remark = getStringValue(row.getCell(1)) ?: ""

                        // 分类
                        val category = getStringValue(row.getCell(2)) ?: ""
                        if (category.isEmpty()) {
                            errors.add("第${rowNum}行分类为空")
                            continue
                        }

                        // 支付方式
                        val channel = getStringValue(row.getCell(3)) ?: ""
                        if (channel.isEmpty()) {
                            errors.add("第${rowNum}行支付方式为空")
                            continue
                        }

                        // 金额
                        val amountYuan = getNumericValue(amountCell)
                        if (amountYuan == null || amountYuan < 0) {
                            errors.add("第${rowNum}行金额无效：${amountCell.toString()}")
                            continue
                        }
                        val amountFen = (amountYuan * 100).toInt()

                        rows.add(
                            ParsedExcelRow(
                                rowIndex = rowNum,
                                date = date,
                                remark = remark,
                                category = category,
                                channel = channel,
                                amountFen = amountFen
                            )
                        )

                    } catch (e: Exception) {
                        errors.add("第${rowNum}行解析异常：${e.message}")
                    }
                }

                workbook.close()
            }
        } catch (e: Exception) {
            return ExcelParseResult(emptyList(), listOf("文件读取失败：${e.message}"))
        }

        return if (errors.isNotEmpty()) {
            ExcelParseResult(emptyList(), errors)
        } else {
            ExcelParseResult(rows, emptyList())
        }
    }

    /**
     * 获取单元格字符串值（POI 5.2.5 兼容）
     */
    private fun getStringValue(cell: Cell?): String? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cell.dateCellValue)
                } else {
                    cell.numericCellValue.toString()
                }
            }
            CellType.BLANK -> ""
            else -> null
        }
    }

    /**
     * 获取单元格数值（POI 5.2.5 兼容）
     */
    private fun getNumericValue(cell: Cell?): Double? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull()
            else -> null
        }
    }

    private fun parseDate(str: String): String? {
        for (format in dateFormats) {
            try {
                val date = format.parse(str)
                if (date != null) {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                }
            } catch (_: Exception) {
                // 继续尝试
            }
        }
        return null
    }
}
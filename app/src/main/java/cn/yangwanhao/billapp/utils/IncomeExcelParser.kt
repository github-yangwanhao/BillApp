package cn.yangwanhao.billapp.utils

import cn.yangwanhao.billapp.common.ImportConstants
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedIncomeRow(
    val rowIndex: Int,
    val billMonth: Int,     // yyyyMM
    val postDate: Int,      // yyyyMMdd
    val amountFen: Int,
    val category: String,
    val remark: String
)

data class IncomeParseResult(
    val rows: List<ParsedIncomeRow>,
    val errors: List<String>
)

object IncomeExcelParser {

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("yyyy/M/d", Locale.getDefault())
    )

    fun parse(file: File): IncomeParseResult {
        val errors = mutableListOf<String>()
        val rows = mutableListOf<ParsedIncomeRow>()

        try {
            FileInputStream(file).use { fis ->
                val workbook: Workbook = when {
                    file.extension.equals("xlsx", ignoreCase = true) -> XSSFWorkbook(fis)
                    file.extension.equals("xls", ignoreCase = true) -> HSSFWorkbook(fis)
                    else -> return IncomeParseResult(emptyList(), listOf("不支持的文件格式：${file.extension}"))
                }

                val sheet = workbook.getSheetAt(0) ?: run {
                    workbook.close()
                    return IncomeParseResult(emptyList(), listOf("Excel 文件无有效 Sheet"))
                }

                // 校验表头
                val headerRow = sheet.getRow(0) ?: run {
                    workbook.close()
                    return IncomeParseResult(emptyList(), listOf("文件为空或表头不存在"))
                }
                val actualHeaders = (0 until 5).map { getStringValue(headerRow.getCell(it)) ?: "" }
                if (actualHeaders != ImportConstants.HEADER_INCOME_EXPECTED) {
                    workbook.close()
                    return IncomeParseResult(
                        emptyList(),
                        listOf("表头不匹配，期望：${ImportConstants.HEADER_INCOME_EXPECTED}，实际：$actualHeaders")
                    )
                }

                for (i in 1 until sheet.physicalNumberOfRows) {
                    val row = sheet.getRow(i) ?: continue
                    val rowNum = i + 1

                    val monthCell = row.getCell(0)
                    val dateCell = row.getCell(1)
                    val amountCell = row.getCell(2)
                    if (monthCell == null || dateCell == null || amountCell == null) continue

                    try {
                        // 年月：2020-06 → 202006
                        val monthStr = getStringValue(monthCell) ?: ""
                        val billMonth = parseBillMonth(monthStr)
                        if (billMonth == null) {
                            errors.add("第${rowNum}行年月解析失败：$monthStr")
                            continue
                        }

                        // 到账日期：2020/06/24 → 20200624
                        val dateStr = getDateValue(dateCell)
                        val postDate = parsePostDate(dateStr)
                        if (postDate == null) {
                            errors.add("第${rowNum}行到账日期解析失败：$dateStr")
                            continue
                        }

                        // 金额
                        val amountFen = getAmountFen(amountCell)
                        if (amountFen == null || amountFen < 0) {
                            errors.add("第${rowNum}行金额无效：${amountCell.toString()}")
                            continue
                        }

                        // 类型
                        val category = getStringValue(row.getCell(3)) ?: ""
                        if (category.isEmpty()) {
                            errors.add("第${rowNum}行类型为空")
                            continue
                        }

                        // 备注
                        val remark = getStringValue(row.getCell(4)) ?: ""

                        rows.add(
                            ParsedIncomeRow(
                                rowIndex = rowNum,
                                billMonth = billMonth,
                                postDate = postDate,
                                amountFen = amountFen,
                                category = category,
                                remark = remark
                            )
                        )
                    } catch (e: Exception) {
                        errors.add("第${rowNum}行解析异常：${e.message}")
                    }
                }

                workbook.close()
            }
        } catch (e: Exception) {
            return IncomeParseResult(emptyList(), listOf("文件读取失败：${e.message}"))
        }

        return if (errors.isNotEmpty()) IncomeParseResult(emptyList(), errors)
        else IncomeParseResult(rows, emptyList())
    }

    /** "2020-06" → 202006 */
    private fun parseBillMonth(str: String): Int? {
        val cleaned = str.trim().replace("/", "-")
        val parts = cleaned.split("-")
        if (parts.size < 2) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        if (month !in 1..12) return null
        return year * 100 + month
    }

    /** "2020/06/24" → 20200624 */
    private fun parsePostDate(str: String): Int? {
        val cleaned = str.trim().replace("-", "/")
        val parts = cleaned.split("/")
        if (parts.size < 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].substringBefore(" ").toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return year * 10000 + month * 100 + day
    }

    /** 🔥 使用 BigDecimal 精确转换为分 */
    private fun getAmountFen(cell: Cell): Int? {
        val rawValue: Double? = when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull()
            else -> null
        }
        if (rawValue == null) return null
        return BigDecimal(rawValue.toString())
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
    }

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

    /** 到账日期列，可能是日期单元格或字符串 */
    private fun getDateValue(cell: Cell): String {
        return when (cell.cellType) {
            CellType.NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(cell.dateCellValue)
                } else {
                    cell.numericCellValue.toString()
                }
            }
            CellType.STRING -> cell.stringCellValue.trim()
            else -> ""
        }
    }
}
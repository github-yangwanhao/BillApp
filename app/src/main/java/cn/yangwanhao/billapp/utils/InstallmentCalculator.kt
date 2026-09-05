package cn.yangwanhao.billapp.utils

import android.annotation.SuppressLint
import cn.yangwanhao.billapp.dto.InstallmentBillDto
import java.util.Calendar

object InstallmentCalculator {

    /**
     * 计算分期账单（按固定期数）
     * @param totalAmount 总金额（单位：分）
     * @param count 分期期数
     * @param startMonth 开始月份（yyyyMM，如 202608）
     * @param baseRemark 备注模板（可选）
     * @return InstallmentResult 包含总金额、期数、每期明细
     */
    fun calculate(
        totalAmount: Int,
        count: Int,
        startMonth: Int,
        baseRemark: String = ""
    ): InstallmentResult {
        require(count > 0) { "期数必须大于0" }
        require(totalAmount > 0) { "金额必须大于0" }

        val baseAmount = totalAmount / count
        val remainder = totalAmount - (baseAmount * count)

        val bills = mutableListOf<InstallmentBillDto>()

        for (i in 0 until count) {
            val amount = if (i == count - 1) {
                baseAmount + remainder
            } else {
                baseAmount
            }

            val billMonth = addMonths(startMonth, i)

            val remark = if (baseRemark.isNotEmpty()) {
                "$baseRemark（第${i + 1}期）"
            } else {
                "第${i + 1}期"
            }

            bills.add(
                InstallmentBillDto(
                    installmentIndex = i + 1,
                    amount = amount,
                    billMonth = billMonth,
                    remark = remark
                )
            )
        }

        return InstallmentResult(
            totalAmount = totalAmount,
            installmentCount = count,
            bills = bills
        )
    }

    /**
     * 🔥 根据起止年份/月份计算分期（新增）
     * @param totalAmount 总金额（单位：分）
     * @param startYear 起始年份
     * @param startMonth 起始月份（1-12）
     * @param endYear 结束年份
     * @param endMonth 结束月份（1-12）
     * @param baseRemark 备注模板（可选）
     * @return InstallmentResult
     */
    fun calculateByYearMonth(
        totalAmount: Int,
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int,
        baseRemark: String = ""
    ): InstallmentResult {
        require(totalAmount > 0) { "金额必须大于0" }
        require(startYear < endYear || (startYear == endYear && startMonth <= endMonth)) {
            "起始月份不能大于结束月份"
        }

        // 计算月份差
        val startMonthTotal = startYear * 12 + (startMonth - 1)
        val endMonthTotal = endYear * 12 + (endMonth - 1)
        val count = endMonthTotal - startMonthTotal + 1

        // 转换为 yyyyMM 格式的起始月份
        val startMonthInt = startYear * 100 + startMonth

        return calculate(totalAmount, count, startMonthInt, baseRemark)
    }

    /**
     * yyyyMM 格式月份加 N 个月
     */
    private fun addMonths(yearMonth: Int, months: Int): Int {
        val year = yearMonth / 100
        val month = yearMonth % 100
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, months)
        }
        val newYear = cal.get(Calendar.YEAR)
        val newMonth = cal.get(Calendar.MONTH) + 1
        return newYear * 100 + newMonth
    }

    /**
     * 将 yyyyMM 格式的月份显示为 "yyyy年MM月"
     */
    @SuppressLint("DefaultLocale")
    fun formatMonth(monthInt: Int): String {
        val year = monthInt / 100
        val month = monthInt % 100
        return String.format("%04d年%02d月", year, month)
    }
}

/**
 * 分期计算结果
 */
data class InstallmentResult(
    val totalAmount: Int,
    val installmentCount: Int,
    val bills: List<InstallmentBillDto>
)
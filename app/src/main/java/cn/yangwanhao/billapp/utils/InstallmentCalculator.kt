package cn.yangwanhao.billapp.utils

import android.annotation.SuppressLint
import cn.yangwanhao.billapp.dto.InstallmentBillDto
import java.util.Calendar

object InstallmentCalculator {

    /**
     * 计算分期账单
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

        // 每期基础金额（向下取整）
        val baseAmount = totalAmount / count
        val remainder = totalAmount - (baseAmount * count)

        val bills = mutableListOf<InstallmentBillDto>()

        for (i in 0 until count) {
            // 最后一期：补上余数
            val amount = if (i == count - 1) {
                baseAmount + remainder
            } else {
                baseAmount
            }

            val billMonth = addMonths(startMonth, i)

            // 备注自动添加期数标识
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
     * yyyyMM 格式月份加 N 个月
     */
    private fun addMonths(yearMonth: Int, months: Int): Int {
        val year = yearMonth / 100
        val month = yearMonth % 100
        // 转为 Calendar 计算
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)  // Calendar 的月份从0开始
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
package cn.yangwanhao.billapp.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtil {

    /**
     * 获取当前日期的 yyyyMMdd 格式整数（如 20260822）
     */
    fun getCurrentDateInt(): Int {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return sdf.format(Date()).toInt()
    }

    /**
     * 获取当前月份的 yyyyMM 格式整数（如 202608）
     */
    fun getCurrentMonthInt(): Int {
        val sdf = SimpleDateFormat("yyyyMM", Locale.getDefault())
        return sdf.format(Date()).toInt()
    }

    /**
     * 将 yyyyMMdd 格式的整数转为 yyyy年MM月dd日 格式的字符串
     */
    fun dateIntToDisplay(dateInt: Int): String {
        val sdfIn = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val sdfOut = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        return try {
            val date = sdfIn.parse(dateInt.toString())
            sdfOut.format(date ?: "")
        } catch (e: Exception) {
            dateInt.toString()
        }
    }

    /**
     * 将 yyyyMM 格式的整数转为 yyyy年MM月 格式的字符串
     */
    fun monthIntToDisplay(monthInt: Int): String {
        val sdfIn = SimpleDateFormat("yyyyMM", Locale.getDefault())
        val sdfOut = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        return try {
            val date = sdfIn.parse(monthInt.toString())
            sdfOut.format(date ?: "")
        } catch (e: Exception) {
            monthInt.toString()
        }
    }

    /**
     * 获取指定月份的上个月（如 202608 -> 202607）
     */
    fun getPreviousMonth(monthInt: Int): Int {
        val sdf = SimpleDateFormat("yyyyMM", Locale.getDefault())
        val calendar = Calendar.getInstance()
        try {
            calendar.time = sdf.parse(monthInt.toString())!!
            calendar.add(Calendar.MONTH, -1)
            return sdf.format(calendar.time).toInt()
        } catch (e: Exception) {
            return monthInt
        }
    }
}
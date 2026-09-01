package cn.yangwanhao.billapp.common

import java.text.SimpleDateFormat
import java.util.Locale

object DateUtil {

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

}
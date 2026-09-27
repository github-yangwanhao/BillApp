package cn.yangwanhao.billapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.yangwanhao.billapp.entity.IncomeBill

@Dao
interface IncomeBillDao {

    @Insert
    suspend fun insert(bill: IncomeBill): Long

    @Insert
    suspend fun insertAll(bills: List<IncomeBill>)

    @Query("DELETE FROM income_bill WHERE ID = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM income_bill WHERE BILL_MONTH = :billMonth")
    suspend fun deleteByBillMonth(billMonth: Int)

    @Query("SELECT * FROM income_bill ORDER BY BILL_MONTH DESC, CREATE_TIME DESC LIMIT :limit OFFSET :offset")
    suspend fun getBillsPaged(limit: Int, offset: Int): List<IncomeBill>

    /** 按所属月份求和 */
    @Query("SELECT COALESCE(SUM(AMOUNT), 0) FROM income_bill WHERE BILL_MONTH = :billMonth")
    suspend fun getMonthTotal(billMonth: Int): Int

    /** 按所属月份统计笔数 */
    @Query("SELECT COUNT(*) FROM income_bill WHERE BILL_MONTH = :billMonth")
    suspend fun countByBillMonth(billMonth: Int): Int

    /** 该月内「所属月份 ≠ 入账月份」的笔数（跨月归属） */
    @Query("SELECT COUNT(*) FROM income_bill WHERE BILL_MONTH = :billMonth AND POST_DATE / 100 != :billMonth")
    suspend fun countCrossMonthByBillMonth(billMonth: Int): Int

    // ==================== 🔥 统计相关 ====================

    /** 区间内收入总额 */
    @Query("SELECT COALESCE(SUM(AMOUNT), 0) FROM income_bill WHERE BILL_MONTH BETWEEN :start AND :end")
    suspend fun sumByRange(start: Int, end: Int): Int

    /** 区间内收入笔数 */
    @Query("SELECT COUNT(*) FROM income_bill WHERE BILL_MONTH BETWEEN :start AND :end")
    suspend fun countByRange(start: Int, end: Int): Int

    /** 区间内按 BILL_MONTH 分月聚合 */
    @Query("""
        SELECT BILL_MONTH AS period, COALESCE(SUM(AMOUNT), 0) AS total
        FROM income_bill
        WHERE BILL_MONTH BETWEEN :start AND :end
        GROUP BY BILL_MONTH
        ORDER BY BILL_MONTH
    """)
    suspend fun sumGroupByBillMonth(start: Int, end: Int): List<PeriodSum>

    /** 汇总模式：按年聚合 */
    @Query("""
        SELECT BILL_MONTH / 100 AS period, COALESCE(SUM(AMOUNT), 0) AS total
        FROM income_bill
        GROUP BY BILL_MONTH / 100
        ORDER BY period
    """)
    suspend fun sumGroupByYear(): List<PeriodSum>

    /** 区间内按分类聚合 */
    @Query("""
        SELECT CATEGORY_ID AS categoryId, COALESCE(SUM(AMOUNT), 0) AS total
        FROM income_bill
        WHERE BILL_MONTH BETWEEN :start AND :end
        GROUP BY CATEGORY_ID
        ORDER BY total DESC
    """)
    suspend fun sumGroupByCategory(start: Int, end: Int): List<CategorySumRaw>

    /** 下钻：某个分类在区间内的所有明细 */
    @Query("""
        SELECT ID AS id, POST_DATE AS date, REMARK AS remark, AMOUNT AS amount
        FROM income_bill
        WHERE BILL_MONTH BETWEEN :start AND :end AND CATEGORY_ID = :categoryId
        ORDER BY POST_DATE DESC, CREATE_TIME DESC
    """)
    suspend fun drillByCategory(categoryId: Int, start: Int, end: Int): List<DrillItemRaw>

    /** 区间内按 POST_DATE 的「日」聚合（用于月度折线图，X 轴 = 日） */
    @Query("""
        SELECT POST_DATE AS period, COALESCE(SUM(AMOUNT), 0) AS total
        FROM income_bill
        WHERE BILL_MONTH BETWEEN :start AND :end
        GROUP BY POST_DATE
        ORDER BY POST_DATE
    """)
    suspend fun sumGroupByPostDate(start: Int, end: Int): List<PeriodSum>

    /** 🔥 全表最早的 BILL_MONTH */
    @Query("SELECT MIN(BILL_MONTH) FROM income_bill")
    suspend fun getMinBillMonth(): Int?

    /** 🔥 全表最晚的 BILL_MONTH */
    @Query("SELECT MAX(BILL_MONTH) FROM income_bill")
    suspend fun getMaxBillMonth(): Int?
}
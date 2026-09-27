package cn.yangwanhao.billapp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.yangwanhao.billapp.dto.MonthSummary
import cn.yangwanhao.billapp.entity.ConsumeBill

@Dao
interface ConsumeBillDao {

    /** 插入一条消费账单 */
    @Insert
    suspend fun insert(bill: ConsumeBill): Long

    /** 批量插入（用于文件导入） */
    @Insert
    suspend fun insertAll(bills: List<ConsumeBill>)

    @Query("DELETE FROM consume_bill WHERE ID = :id")
    suspend fun deleteById(id: Long)

    /**
     * 分页查询支出账单（按日期倒序）
     * @param limit  每页取多少条
     * @param offset 跳过前面多少条（第一页传0，第二页传20，第三页传40...）
     */
    @Query("SELECT * FROM consume_bill ORDER BY bill_month DESC, pay_date DESC, create_time DESC LIMIT :limit OFFSET :offset")
    suspend fun getBillsPaged(limit: Int, offset: Int): List<ConsumeBill>

    @Query("DELETE FROM consume_bill WHERE BILL_MONTH = :billMonth")
    suspend fun deleteByBillMonth(billMonth: Int)

    @Query("SELECT COUNT(*) FROM consume_bill WHERE BILL_MONTH = :billMonth")
    suspend fun countByBillMonth(billMonth: Int): Int

    /**
     * 查询某个月份的支出汇总
     */
    @Query("SELECT COALESCE(SUM(AMOUNT), 0) as totalAmount, COUNT(*) as count FROM consume_bill WHERE BILL_MONTH = :billMonth")
    suspend fun getMonthSummary(billMonth: Int): MonthSummary

    /**
     * 查询最近 N 个有数据的月份
     */
    @Query("SELECT DISTINCT BILL_MONTH FROM consume_bill ORDER BY BILL_MONTH DESC LIMIT :limit")
    suspend fun getRecentMonths(limit: Int = 12): List<Int>

    // ==================== 🔥 统计相关 ====================

    /** 区间内支出总额 */
    @Query("SELECT COALESCE(SUM(AMOUNT), 0) FROM consume_bill WHERE BILL_MONTH BETWEEN :start AND :end")
    suspend fun sumByRange(start: Int, end: Int): Int

    /** 区间内支出笔数 */
    @Query("SELECT COUNT(*) FROM consume_bill WHERE BILL_MONTH BETWEEN :start AND :end")
    suspend fun countByRange(start: Int, end: Int): Int

    /** 区间内按 BILL_MONTH 分月聚合（用于年度折线图，X 轴 = 月） */
    @Query("""
        SELECT BILL_MONTH AS period, COALESCE(SUM(AMOUNT), 0) AS total
        FROM consume_bill
        WHERE BILL_MONTH BETWEEN :start AND :end
        GROUP BY BILL_MONTH
        ORDER BY BILL_MONTH
    """)
    suspend fun sumGroupByBillMonth(start: Int, end: Int): List<PeriodSum>

    /** 区间内按 POST_DATE 的「日」聚合（用于月度折线图，X 轴 = 日） */
    @Query("""
        SELECT PAY_DATE AS period, COALESCE(SUM(AMOUNT), 0) AS total
        FROM consume_bill
        WHERE BILL_MONTH BETWEEN :start AND :end
        GROUP BY PAY_DATE
        ORDER BY PAY_DATE
    """)
    suspend fun sumGroupByPostDate(start: Int, end: Int): List<PeriodSum>

    /** 汇总模式：按年聚合（BILL_MONTH / 100） */
    @Query("""
        SELECT BILL_MONTH / 100 AS period, COALESCE(SUM(AMOUNT), 0) AS total
        FROM consume_bill
        GROUP BY BILL_MONTH / 100
        ORDER BY period
    """)
    suspend fun sumGroupByYear(): List<PeriodSum>

    /** 区间内按分类聚合（分类排行） */
    @Query("""
        SELECT CATEGORY_ID AS categoryId, COALESCE(SUM(AMOUNT), 0) AS total
        FROM consume_bill
        WHERE BILL_MONTH BETWEEN :start AND :end
        GROUP BY CATEGORY_ID
        ORDER BY total DESC
    """)
    suspend fun sumGroupByCategory(start: Int, end: Int): List<CategorySumRaw>

    /** 区间内按支付方式聚合 */
    @Query("""
        SELECT PAY_CHANNEL_ID AS channelId, COALESCE(SUM(AMOUNT), 0) AS total
        FROM consume_bill
        WHERE BILL_MONTH BETWEEN :start AND :end
        GROUP BY PAY_CHANNEL_ID
        ORDER BY total DESC
    """)
    suspend fun sumGroupByChannel(start: Int, end: Int): List<ChannelSumRaw>

    /** 下钻：某个分类在区间内的所有明细 */
    @Query("""
        SELECT ID AS id, PAY_DATE AS date, REMARK AS remark, AMOUNT AS amount
        FROM consume_bill
        WHERE BILL_MONTH BETWEEN :start AND :end AND CATEGORY_ID = :categoryId
        ORDER BY PAY_DATE DESC, CREATE_TIME DESC
    """)
    suspend fun drillByCategory(categoryId: Int, start: Int, end: Int): List<DrillItemRaw>

    /** 🔥 全表最早的 BILL_MONTH */
    @Query("SELECT MIN(BILL_MONTH) FROM consume_bill")
    suspend fun getMinBillMonth(): Int?

    /** 🔥 全表最晚的 BILL_MONTH */
    @Query("SELECT MAX(BILL_MONTH) FROM consume_bill")
    suspend fun getMaxBillMonth(): Int?

    /** 🔥 导出：某月全部支出（不分页，按日期升序） */
    @Query("SELECT * FROM consume_bill WHERE BILL_MONTH = :billMonth ORDER BY PAY_DATE ASC, CREATE_TIME ASC")
    suspend fun getAllByBillMonth(billMonth: Int): List<ConsumeBill>

    /** 🔥 导出：所有有数据的月份，降序 */
    @Query("SELECT DISTINCT BILL_MONTH FROM consume_bill ORDER BY BILL_MONTH DESC")
    suspend fun getDistinctBillMonths(): List<Int>
}
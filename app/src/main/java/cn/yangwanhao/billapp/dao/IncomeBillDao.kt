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
}
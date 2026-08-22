package cn.yangwanhao.billapp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.yangwanhao.billapp.entity.IncomeBill

@Dao
interface IncomeBillDao {

    /** 插入一条收入账单 */
    @Insert
    fun insert(bill: IncomeBill): Long

    /** 批量插入 */
    @Insert
    fun insertAll(bills: List<IncomeBill>)

    /** 更新一条收入账单 */
    @Update
    fun update(bill: IncomeBill)

    /** 删除一条收入账单 */
    @Delete
    fun delete(bill: IncomeBill)

    /** 根据ID查询单条 */
    @Query("SELECT * FROM income_bill WHERE ID = :id")
    fun getById(id: Long): IncomeBill?

    /** 按入账月份查询，按入账日期降序排列 */
    @Query("SELECT * FROM income_bill WHERE POST_DATE / 100 = :month ORDER BY POST_DATE DESC, ID DESC")
    fun getByMonth(month: Int): List<IncomeBill>

    /** 统计某月总收入（单位：分） */
    @Query("SELECT COALESCE(SUM(AMOUNT), 0) FROM income_bill WHERE POST_DATE / 100 = :month")
    fun sumAmountByMonth(month: Int): Int

    /** 按账单所属月份查询（权责发生制维度） */
    @Query("SELECT * FROM income_bill WHERE BILL_MONTH = :billMonth ORDER BY POST_DATE DESC, ID DESC")
    fun getByBillMonth(billMonth: Int): List<IncomeBill>

    @Query("SELECT * FROM income_bill ORDER BY post_date DESC LIMIT :limit OFFSET :offset")
    suspend fun getBillsPaged(limit: Int, offset: Int): List<IncomeBill>

    @Query("SELECT COUNT(*) FROM income_bill")
    suspend fun getTotalCount(): Int
}
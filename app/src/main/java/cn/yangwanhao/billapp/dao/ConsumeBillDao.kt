package cn.yangwanhao.billapp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.yangwanhao.billapp.entity.ConsumeBill

@Dao
interface ConsumeBillDao {

    /** 插入一条消费账单 */
    @Insert
    fun insert(bill: ConsumeBill): Long

    /** 批量插入（用于文件导入） */
    @Insert
    fun insertAll(bills: List<ConsumeBill>)

    /** 更新一条消费账单 */
    @Update
    fun update(bill: ConsumeBill)

    /** 删除一条消费账单 */
    @Delete
    fun delete(bill: ConsumeBill)

    @Query("DELETE FROM consume_bill WHERE ID = :id")
    suspend fun deleteById(id: Long)

    /** 根据ID查询单条 */
    @Query("SELECT * FROM consume_bill WHERE ID = :id")
    fun getById(id: Long): ConsumeBill?

    /** 按月份查询，按支付日期降序排列（首页列表用） */
    @Query("SELECT * FROM consume_bill WHERE PAY_DATE / 100 = :month ORDER BY PAY_DATE DESC, ID DESC")
    fun getByMonth(month: Int): List<ConsumeBill>

    /** 统计某月总支出（单位：分） */
    @Query("SELECT COALESCE(SUM(AMOUNT), 0) FROM consume_bill WHERE PAY_DATE / 100 = :month")
    fun sumAmountByMonth(month: Int): Int

    /** 统计某月某日的支出合计（日期分组头显示用） */
    @Query("SELECT COALESCE(SUM(AMOUNT), 0) FROM consume_bill WHERE PAY_DATE = :dateInt")
    fun sumAmountByDate(dateInt: Int): Int

    /** 按账单所属月份查询（权责发生制维度） */
    @Query("SELECT * FROM consume_bill WHERE BILL_MONTH = :billMonth ORDER BY PAY_DATE DESC, ID DESC")
    fun getByBillMonth(billMonth: Int): List<ConsumeBill>

    /** 按导入文件ID查询（用于查看某次导入的所有账单） */
    @Query("SELECT * FROM consume_bill WHERE IMPORT_FILE_ID = :importFileId ORDER BY PAY_DATE DESC")
    fun getByImportFileId(importFileId: Int): List<ConsumeBill>

    /**
     * 分页查询支出账单（按日期倒序）
     * @param limit  每页取多少条
     * @param offset 跳过前面多少条（第一页传0，第二页传20，第三页传40...）
     */
    @Query("SELECT * FROM consume_bill ORDER BY bill_month DESC, pay_date DESC, create_time DESC LIMIT :limit OFFSET :offset")
    suspend fun getBillsPaged(limit: Int, offset: Int): List<ConsumeBill>

    /**
     * 查询支出账单总数（用来判断是否还有下一页）
     */
    @Query("SELECT COUNT(*) FROM consume_bill")
    suspend fun getTotalCount(): Int
}
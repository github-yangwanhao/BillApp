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

}
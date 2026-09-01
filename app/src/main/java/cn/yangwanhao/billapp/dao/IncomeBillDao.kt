package cn.yangwanhao.billapp.dao

import androidx.room.Dao
import androidx.room.Query
import cn.yangwanhao.billapp.entity.IncomeBill

@Dao
interface IncomeBillDao {

    @Query("DELETE FROM income_bill WHERE ID = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM income_bill ORDER BY post_date DESC LIMIT :limit OFFSET :offset")
    suspend fun getBillsPaged(limit: Int, offset: Int): List<IncomeBill>
}
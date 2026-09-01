package cn.yangwanhao.billapp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.yangwanhao.billapp.entity.Dict

@Dao
interface DictDao {

    /** 批量插入（初始化预填数据用） */
    @Insert
    fun insertAll(dicts: List<Dict>)

    /** 按字典类型Key查询，只返回启用状态的，按排序序号排列 */
    @Query("SELECT * FROM dict WHERE DICT_KEY = :dictKey AND STATUS = '1' ORDER BY SORT")
    fun getByKey(dictKey: String): List<Dict>

    /** 根据ID查询单条 */
    @Query("SELECT * FROM dict WHERE ID = :id")
    fun getById(id: Long): Dict?

    @Query("SELECT * FROM dict WHERE DICT_KEY = :dictKey AND DICT_VALUE = :dictValue AND STATUS = '1' LIMIT 1")
    suspend fun getByValue(dictKey: String, dictValue: String): Dict?
}
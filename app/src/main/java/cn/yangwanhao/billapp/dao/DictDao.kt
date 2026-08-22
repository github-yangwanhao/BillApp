package cn.yangwanhao.billapp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.yangwanhao.billapp.entity.Dict

@Dao
interface DictDao {

    /** 插入一条字典 */
    @Insert
    fun insert(dict: Dict): Long

    /** 批量插入（初始化预填数据用） */
    @Insert
    fun insertAll(dicts: List<Dict>)

    /** 更新一条字典 */
    @Update
    fun update(dict: Dict)

    /** 删除一条字典 */
    @Delete
    fun delete(dict: Dict)

    /** 查询所有字典 */
    @Query("SELECT * FROM dict ORDER BY DICT_KEY, SORT")
    fun getAll(): List<Dict>

    /** 按字典类型Key查询，只返回启用状态的，按排序序号排列 */
    @Query("SELECT * FROM dict WHERE DICT_KEY = :dictKey AND STATUS = '1' ORDER BY SORT")
    fun getByKey(dictKey: String): List<Dict>

    /** 根据ID查询单条 */
    @Query("SELECT * FROM dict WHERE ID = :id")
    fun getById(id: Long): Dict?
}
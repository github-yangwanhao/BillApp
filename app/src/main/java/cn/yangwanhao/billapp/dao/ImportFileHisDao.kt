package cn.yangwanhao.billapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.yangwanhao.billapp.entity.ImportFileHis

@Dao
interface ImportFileHisDao {

    /** 插入一条导入记录，返回自增ID */
    @Insert
    fun insert(record: ImportFileHis): Long

    /** 根据MD5查询，用于判断文件是否已导入过 */
    @Query("SELECT * FROM import_file_his WHERE FILE_MD5 = :md5 LIMIT 1")
    fun getByMd5(md5: String): ImportFileHis?

    /** 查询所有导入记录，按创建时间降序 */
    @Query("SELECT * FROM import_file_his ORDER BY CREATE_TIME DESC")
    fun getAll(): List<ImportFileHis>
}
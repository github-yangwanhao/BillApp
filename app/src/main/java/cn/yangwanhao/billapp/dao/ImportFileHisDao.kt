package cn.yangwanhao.billapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.yangwanhao.billapp.entity.ImportFileHis

@Dao
interface ImportFileHisDao {

    /** 插入一条导入记录，返回自增ID */
    @Insert
    suspend fun insert(record: ImportFileHis): Long

    /** 根据MD5查询，用于判断文件是否已导入过 */
    /** 🔥 按 MD5 + 导入类型 查询，避免支出/收入互相误判 */
    @Query("SELECT * FROM import_file_his WHERE FILE_MD5 = :md5 AND IMPORT_TYPE = :importType LIMIT 1")
    suspend fun getByMd5AndType(md5: String, importType: String): ImportFileHis?

}
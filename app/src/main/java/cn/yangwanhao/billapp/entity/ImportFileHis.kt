package cn.yangwanhao.billapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_file_his")
data class ImportFileHis(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,

    /** 导入的文件名 */
    @ColumnInfo(name = "FILE_NAME")
    val fileName: String,

    /** 导入的文件数据条数 */
    @ColumnInfo(name = "FILE_ROW")
    val fileRow: Int,

    /** 文件的 MD5 值，唯一约束防重复导入 */
    @ColumnInfo(name = "FILE_MD5")
    val fileMd5: String,

    /** 创建时间 */
    @ColumnInfo(name = "CREATE_TIME")
    val createTime: String? = null,

    /** 更新时间 */
    @ColumnInfo(name = "UPDATE_TIME")
    val updateTime: String? = null
)
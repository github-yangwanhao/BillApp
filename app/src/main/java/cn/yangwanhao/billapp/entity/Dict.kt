package cn.yangwanhao.billapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dict")
data class Dict(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,

    /** 字典类型 Key，如 CONSUME_CATEGORY / INCOME_CATEGORY / PAY_CHANNEL */
    @ColumnInfo(name = "DICT_KEY")
    val dictKey: String,

    /** 字典值，如"餐饮"、"微信" */
    @ColumnInfo(name = "DICT_VALUE")
    val dictValue: String,

    /** 排序序号 */
    @ColumnInfo(name = "SORT")
    val sort: Int,

    /** 状态标识：1-启用 / 0-禁用 */
    @ColumnInfo(name = "STATUS")
    val status: String,

    /** 创建时间 */
    @ColumnInfo(name = "CREATE_TIME")
    val createTime: String? = null,

    /** 更新时间 */
    @ColumnInfo(name = "UPDATE_TIME")
    val updateTime: String? = null
)
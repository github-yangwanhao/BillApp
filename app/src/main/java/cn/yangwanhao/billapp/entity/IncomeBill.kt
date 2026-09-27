package cn.yangwanhao.billapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "income_bill",
    indices = [
        Index(value = ["BILL_MONTH"]),
        Index(value = ["POST_DATE"]),
        Index(value = ["BILL_MONTH", "CATEGORY_ID"])
    ]
)
data class IncomeBill(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,

    /** 金额，单位：分 */
    @ColumnInfo(name = "AMOUNT")
    val amount: Int,

    /** 账单分类ID，关联 dict.ID */
    @ColumnInfo(name = "CATEGORY_ID")
    val categoryId: Int,

    /** 入账日期，yyyyMMdd 格式整数 */
    @ColumnInfo(name = "POST_DATE")
    val postDate: Int,

    /** 账单所属月份，yyyyMM 格式整数 */
    @ColumnInfo(name = "BILL_MONTH")
    val billMonth: Int,

    /** 账单备注 */
    @ColumnInfo(name = "REMARK")
    val remark: String,

    /** 创建时间 */
    @ColumnInfo(name = "CREATE_TIME")
    val createTime: Date,

    /** 更新时间 */
    @ColumnInfo(name = "UPDATE_TIME")
    val updateTime: Date
)
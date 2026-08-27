package cn.yangwanhao.billapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "consume_bill")
data class ConsumeBill(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Long = 0,

    /** 金额，单位：分 */
    @ColumnInfo(name = "AMOUNT")
    val amount: Int,

    /** 账单分类ID，关联 dict.ID */
    @ColumnInfo(name = "CATEGORY_ID")
    val categoryId: Int,

    /** 支付渠道ID，关联 dict.ID */
    @ColumnInfo(name = "PAY_CHANNEL_ID")
    val payChannelId: Int,

    /** 支付日期，yyyyMMdd 格式整数 */
    @ColumnInfo(name = "PAY_DATE")
    val payDate: Int,

    /** 账单所属月份，yyyyMM 格式整数 */
    @ColumnInfo(name = "BILL_MONTH")
    val billMonth: Int,

    /** 账单备注 */
    @ColumnInfo(name = "REMARK")
    val remark: String,

    /** 账单类型：NORMAL-单笔 / INSTALLMENT-分期 */
    @ColumnInfo(name = "BILL_KIND")
    val billKind: String,

    /** 导入文件ID，关联 import_file_his.ID */
    @ColumnInfo(name = "IMPORT_FILE_ID")
    val importFileId: Int?,

    /** 创建时间 */
    @ColumnInfo(name = "CREATE_TIME")
    val createTime: Date,

    /** 更新时间 */
    @ColumnInfo(name = "UPDATE_TIME")
    val updateTime: Date
)
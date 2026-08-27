package cn.yangwanhao.billapp.database

import android.content.Context
import android.content.SharedPreferences
import cn.yangwanhao.billapp.common.Constant
import cn.yangwanhao.billapp.entity.Dict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class DatabaseInitHelper(
    private val context: Context,
    private val db: BillDatabase
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bill_app_prefs", Context.MODE_PRIVATE)

    private fun isInitialized(): Boolean = prefs.getBoolean("is_db_initialized", false)

    private fun setInitialized() {
        prefs.edit().putBoolean("is_db_initialized", true).apply()
    }

    suspend fun initDatabaseIfNeeded() {
        if (isInitialized()) return

        withContext(Dispatchers.IO) {
            val dictDao = db.dictDao()
            val now = Date()

            // 1. 默认消费分类
            val consumeCategories = listOf("餐饮美食", "生活日用", "交通出行", "医疗保健", "服饰美容",
                "充值缴费", "电子通讯", "休闲娱乐", "住房物业", "图书教育", "酒店旅行", "人情往来", "其他")
            val consumeDicts = consumeCategories.mapIndexed { index, value ->
                Dict(
                    dictKey = Constant.DICT_KEY_CONSUME_CATEGORY,
                    dictValue = value,
                    sort = index,
                    status = Constant.STATUS_ENABLE,
                    createTime = now,
                    updateTime = now
                )
            }
            dictDao.insertAll(consumeDicts)

            // 2. 默认收入分类
            val incomeCategories = listOf("工资", "奖金", "兼职", "理财", "礼金", "其他")
            val incomeDicts = incomeCategories.mapIndexed { index, value ->
                Dict(
                    dictKey = Constant.DICT_KEY_INCOME_CATEGORY,
                    dictValue = value,
                    sort = index,
                    status = Constant.STATUS_ENABLE,
                    createTime = now,
                    updateTime = now
                )
            }
            dictDao.insertAll(incomeDicts)

            // 3. 默认支付渠道
            val payChannels = listOf("支付宝", "微信", "银行卡", "信用卡", "现金", "其他")
            val payChannelDicts = payChannels.mapIndexed { index, value ->
                Dict(
                    dictKey = Constant.DICT_KEY_PAY_CHANNEL,
                    dictValue = value,
                    sort = index,
                    status = Constant.STATUS_ENABLE,
                    createTime = now,
                    updateTime = now
                )
            }
            dictDao.insertAll(payChannelDicts)

            // 4. 默认消费备注
            val consumeRemark = listOf("午饭", "晚饭", "地铁", "早饭", "房租", "话费充值", "水费",
                "电费", "餐厅充值", "火车票")
            val consumeRemarkDicts = consumeRemark.mapIndexed { index, value ->
                Dict(
                    dictKey = Constant.DICT_KEY_CONSUME_REMARK,
                    dictValue = value,
                    sort = index,
                    status = Constant.STATUS_ENABLE,
                    createTime = now,
                    updateTime = now
                )
            }
            dictDao.insertAll(consumeRemarkDicts)

            // 5. 默认收入备注
            val incomeRemark = listOf("工资", "奖金")
            val incomeRemarkDicts = incomeRemark.mapIndexed { index, value ->
                Dict(
                    dictKey = Constant.DICT_KEY_INCOME_REMARK,
                    dictValue = value,
                    sort = index,
                    status = Constant.STATUS_ENABLE,
                    createTime = now,
                    updateTime = now
                )
            }
            dictDao.insertAll(incomeRemarkDicts)

            setInitialized()
        }
    }
}
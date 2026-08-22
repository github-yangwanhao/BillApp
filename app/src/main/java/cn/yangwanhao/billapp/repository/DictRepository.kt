package cn.yangwanhao.billapp.repository

import cn.yangwanhao.billapp.dao.DictDao
import cn.yangwanhao.billapp.entity.Dict

class DictRepository(
    private val dictDao: DictDao
) {

    /** 获取所有启用的消费分类 */
    suspend fun getConsumeCategories(): List<Dict> {
        return dictDao.getByKey("CONSUME_CATEGORY")
    }

    /** 获取所有启用的收入分类 */
    suspend fun getIncomeCategories(): List<Dict> {
        return dictDao.getByKey("INCOME_CATEGORY")
    }

    /** 获取所有启用的支付渠道 */
    suspend fun getPayChannels(): List<Dict> {
        return dictDao.getByKey("PAY_CHANNEL")
    }

    /** 根据ID获取字典项 */
    suspend fun getById(id: Long): Dict? {
        return dictDao.getById(id)
    }
}
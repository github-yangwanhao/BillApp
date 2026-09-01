package cn.yangwanhao.billapp.repository

import cn.yangwanhao.billapp.dao.DictDao
import cn.yangwanhao.billapp.entity.Dict

class DictRepository(
    private val dictDao: DictDao
) {

    /** 获取所有启用的数据字典 */
    suspend fun getAllDictValue(dictKey: String): List<Dict> {
        return dictDao.getByKey(dictKey)
    }

    /**
     * 根据字典值查询字典条目
     * @param dictKey 字典类型
     * @param dictValue 字典值
     * @return 匹配的 Dict，找不到返回 null
     */
    suspend fun getByValue(dictKey: String, dictValue: String): Dict? {
        return dictDao.getByValue(dictKey, dictValue)
    }

    /** 根据分类ID获取分类名称 */
    suspend fun getCategoryName(categoryId: Int): String {
        return dictDao.getById(categoryId.toLong())?.dictValue ?: "未知分类"
    }

    /** 根据支付渠道ID获取渠道名称 */
    suspend fun getChannelName(channelId: Int): String {
        return dictDao.getById(channelId.toLong())?.dictValue ?: "未知渠道"
    }
}
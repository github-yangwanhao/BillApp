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

    /** 根据ID获取字典项 */
    suspend fun getById(id: Long): Dict? {
        return dictDao.getById(id)
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
}
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
}
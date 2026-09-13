package cn.yangwanhao.billapp.repository

import cn.yangwanhao.billapp.dao.ImportFileHisDao
import cn.yangwanhao.billapp.entity.ImportFileHis

class ImportFileHisRepository(
    private val importFileHisDao: ImportFileHisDao
) {

    suspend fun insert(record: ImportFileHis): Long {
        return importFileHisDao.insert(record)
    }

    suspend fun getByMd5AndType(md5: String, importType: String): ImportFileHis? {
        return importFileHisDao.getByMd5AndType(md5, importType)
    }
}
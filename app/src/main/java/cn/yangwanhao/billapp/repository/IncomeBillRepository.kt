package cn.yangwanhao.billapp.repository

import cn.yangwanhao.billapp.dao.IncomeBillDao
import cn.yangwanhao.billapp.entity.IncomeBill

class IncomeBillRepository(
    private val incomeBillDao: IncomeBillDao
) {

    suspend fun deleteBillById(id: Long) {
        incomeBillDao.deleteById(id)
    }

    suspend fun getBillsPaged(limit: Int, offset: Int): List<IncomeBill> {
        return incomeBillDao.getBillsPaged(limit, offset)
    }
}
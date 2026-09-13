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

    // 新增
    suspend fun addBill(bill: IncomeBill): Long {
        return incomeBillDao.insert(bill)
    }

    // 新增方法
    suspend fun getMonthTotal(billMonth: Int): Int {
        return incomeBillDao.getMonthTotal(billMonth)
    }

    suspend fun countByBillMonth(billMonth: Int): Int {
        return incomeBillDao.countByBillMonth(billMonth)
    }
}
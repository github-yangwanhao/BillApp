package cn.yangwanhao.billapp.repository

import cn.yangwanhao.billapp.dao.IncomeBillDao
import cn.yangwanhao.billapp.entity.IncomeBill

class IncomeBillRepository(
    private val incomeBillDao: IncomeBillDao
) {
    suspend fun addBill(bill: IncomeBill): Long = incomeBillDao.insert(bill)

    suspend fun insertAll(bills: List<IncomeBill>) = incomeBillDao.insertAll(bills)

    suspend fun deleteBillById(id: Long) = incomeBillDao.deleteById(id)

    suspend fun deleteByBillMonth(billMonth: Int) = incomeBillDao.deleteByBillMonth(billMonth)

    suspend fun getBillsPaged(limit: Int, offset: Int): List<IncomeBill> =
        incomeBillDao.getBillsPaged(limit, offset)

    suspend fun getMonthTotal(billMonth: Int): Int =
        incomeBillDao.getMonthTotal(billMonth)

    suspend fun countByBillMonth(billMonth: Int): Int =
        incomeBillDao.countByBillMonth(billMonth)

    suspend fun countCrossMonthByBillMonth(billMonth: Int): Int =
        incomeBillDao.countCrossMonthByBillMonth(billMonth)
}
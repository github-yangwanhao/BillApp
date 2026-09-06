package cn.yangwanhao.billapp.repository

import cn.yangwanhao.billapp.dao.ConsumeBillDao
import cn.yangwanhao.billapp.dto.MonthSummary
import cn.yangwanhao.billapp.entity.ConsumeBill

class ConsumeBillRepository(
    private val consumeBillDao: ConsumeBillDao
) {

    /** 新增一笔消费账单 */
    suspend fun addBill(bill: ConsumeBill): Long {
        return consumeBillDao.insert(bill)
    }

    suspend fun deleteBillById(id: Long) {
        consumeBillDao.deleteById(id)
    }

    /**
     * 分页查询支出账单
     */
    suspend fun getBillsPaged(limit: Int, offset: Int): List<ConsumeBill> {
        return consumeBillDao.getBillsPaged(limit, offset)
    }

    /** 批量插入账单 */
    suspend fun insertAll(bills: List<ConsumeBill>) {
        consumeBillDao.insertAll(bills)
    }

    /**
     * 按月份删除所有支出账单
     */
    suspend fun deleteByBillMonth(billMonth: Int) {
        consumeBillDao.deleteByBillMonth(billMonth)
    }

    /**
     * 按月份查询是否有支出账单
     */
    suspend fun countByBillMonth(billMonth: Int): Int {
        return consumeBillDao.countByBillMonth(billMonth)
    }

    /**
     * 查询某个月份的支出汇总（金额 + 笔数）
     */
    suspend fun getMonthSummary(billMonth: Int): MonthSummary {
        return consumeBillDao.getMonthSummary(billMonth)
    }

    /**
     * 查询最近 N 个有数据的月份
     */
    suspend fun getRecentMonths(limit: Int = 12): List<Int> {
        return consumeBillDao.getRecentMonths(limit)
    }
}
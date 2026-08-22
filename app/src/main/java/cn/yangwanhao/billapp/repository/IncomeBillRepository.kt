package cn.yangwanhao.billapp.repository

import cn.yangwanhao.billapp.dao.IncomeBillDao
import cn.yangwanhao.billapp.dao.DictDao
import cn.yangwanhao.billapp.entity.IncomeBill

class IncomeBillRepository(
    private val incomeBillDao: IncomeBillDao,
    private val dictDao: DictDao
) {

    /** 新增一笔收入账单 */
    suspend fun addBill(bill: IncomeBill): Long {
        return incomeBillDao.insert(bill)
    }

    /** 更新一笔收入账单 */
    suspend fun updateBill(bill: IncomeBill) {
        incomeBillDao.update(bill)
    }

    /** 删除一笔收入账单 */
    suspend fun deleteBill(bill: IncomeBill) {
        incomeBillDao.delete(bill)
    }

    /** 查询某月所有收入账单 */
    suspend fun getBillsByMonth(month: Int): List<IncomeBill> {
        return incomeBillDao.getByMonth(month)
    }

    /** 统计某月总收入（单位：分） */
    suspend fun getMonthTotal(month: Int): Int {
        return incomeBillDao.sumAmountByMonth(month)
    }

    /** 根据分类ID获取分类名称 */
    suspend fun getCategoryName(categoryId: Int): String {
        return dictDao.getById(categoryId.toLong())?.dictValue ?: "未知分类"
    }
}
package cn.yangwanhao.billapp.repository

import cn.yangwanhao.billapp.dao.ConsumeBillDao
import cn.yangwanhao.billapp.dao.DictDao
import cn.yangwanhao.billapp.entity.ConsumeBill

class ConsumeBillRepository(
    private val consumeBillDao: ConsumeBillDao,
    private val dictDao: DictDao
) {

    /** 新增一笔消费账单 */
    suspend fun addBill(bill: ConsumeBill): Long {
        return consumeBillDao.insert(bill)
    }

    /** 更新一笔消费账单 */
    suspend fun updateBill(bill: ConsumeBill) {
        consumeBillDao.update(bill)
    }

    /** 删除一笔消费账单 */
    suspend fun deleteBill(bill: ConsumeBill) {
        consumeBillDao.delete(bill)
    }

    /** 查询某月所有消费账单 */
    suspend fun getBillsByMonth(month: Int): List<ConsumeBill> {
        return consumeBillDao.getByMonth(month)
    }

    /** 统计某月总支出（单位：分） */
    suspend fun getMonthTotal(month: Int): Int {
        return consumeBillDao.sumAmountByMonth(month)
    }

    /** 统计某日支出合计 */
    suspend fun getDayTotal(dateInt: Int): Int {
        return consumeBillDao.sumAmountByDate(dateInt)
    }

    /** 根据分类ID获取分类名称 */
    suspend fun getCategoryName(categoryId: Int): String {
        return dictDao.getById(categoryId.toLong())?.dictValue ?: "未知分类"
    }

    /** 根据支付渠道ID获取渠道名称 */
    suspend fun getChannelName(channelId: Int): String {
        return dictDao.getById(channelId.toLong())?.dictValue ?: "未知渠道"
    }

    /**
     * 分页查询支出账单
     */
    suspend fun getBillsPaged(limit: Int, offset: Int): List<ConsumeBill> {
        return consumeBillDao.getBillsPaged(limit, offset)
    }

    /**
     * 查询支出账单总数
     */
    suspend fun getTotalCount(): Int {
        return consumeBillDao.getTotalCount()
    }
}
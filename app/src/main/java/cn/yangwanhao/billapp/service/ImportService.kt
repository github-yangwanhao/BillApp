package cn.yangwanhao.billapp.service

import cn.yangwanhao.billapp.common.Constant
import cn.yangwanhao.billapp.common.ImportConstants
import cn.yangwanhao.billapp.dao.ConsumeBillDao
import cn.yangwanhao.billapp.dao.ImportFileHisDao
import cn.yangwanhao.billapp.entity.ConsumeBill
import cn.yangwanhao.billapp.entity.ImportFileHis
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.utils.ExcelParser
import cn.yangwanhao.billapp.utils.FileUtils
import java.io.File
import java.util.Date

/**
 * 导入结果
 */
sealed class ImportResult {
    data class Success(val recordCount: Int, val billMonth: Int) : ImportResult()
    data class Failed(val reason: String) : ImportResult()
    data class AlreadyImported(val md5: String) : ImportResult()
    data class Conflict(val billMonth: Int, val existingCount: Int) : ImportResult()
}

/**
 * 导入服务
 */
class ImportService(
    private val consumeBillDao: ConsumeBillDao,
    private val importFileHisDao: ImportFileHisDao,
    private val dictRepository: DictRepository,
    private val consumeBillRepository: ConsumeBillRepository
) {

    /**
     * 解析文件名，提取 billMonth
     * @return yyyyMM 格式的 Int，如 202608
     */
    fun parseBillMonthFromFileName(fileName: String): Int? {
        val match = ImportConstants.FILE_NAME_PATTERN.find(fileName)
        return match?.groupValues?.get(1)?.replace("-", "")?.toIntOrNull()
    }

    /**
     * 检查文件是否已导入（通过 MD5）
     */
    suspend fun checkFileImported(file: File): ImportFileHis? {
        val md5 = FileUtils.getFileMd5(file)
        return importFileHisDao.getByMd5(md5)
    }

    /**
     * 检查月份是否有账单
     */
    suspend fun checkMonthHasBills(billMonth: Int): Boolean {
        return consumeBillRepository.existsByBillMonth(billMonth)
    }

    /**
     * 执行导入
     * @param file 要导入的文件
     * @param onConflict 月份冲突回调，返回 true 表示覆盖，false 表示取消
     * @return ImportResult
     */
    suspend fun importFile(
        file: File,
        onConflict: suspend (billMonth: Int, existingCount: Int) -> Boolean
    ): ImportResult {
        // 1. 校验文件名
        val fileName = file.name
        val billMonth = parseBillMonthFromFileName(fileName)
        if (billMonth == null) {
            return ImportResult.Failed("文件名格式不正确，应为 yyyy-MM.xlsx 或 yyyy-MM.xls")
        }

        // 2. 计算 MD5，检查是否已导入
        val md5 = FileUtils.getFileMd5(file)
        val existingRecord = importFileHisDao.getByMd5(md5)
        if (existingRecord != null) {
            return ImportResult.AlreadyImported(md5)
        }

        // 3. 检查该月份是否已有账单
        val existingCount = consumeBillDao.countByBillMonth(billMonth)
        var shouldOverwrite = false
        if (existingCount > 0) {
            shouldOverwrite = onConflict(billMonth, existingCount)
            if (!shouldOverwrite) {
                return ImportResult.Failed("用户取消覆盖")
            }
        }

        // 4. 解析 Excel
        val parseResult = ExcelParser.parse(file)
        if (parseResult.errors.isNotEmpty()) {
            val errorMsg = parseResult.errors.joinToString("；")
            // 记录失败的导入历史
            saveImportHistory(fileName, md5, 0, ImportConstants.STATUS_FAILED, errorMsg)
            return ImportResult.Failed(errorMsg)
        }

        if (parseResult.rows.isEmpty()) {
            return ImportResult.Failed("文件无有效数据")
        }

        // 5. 匹配分类和支付方式
        val bills = mutableListOf<ConsumeBill>()
        val errors = mutableListOf<String>()

        for (row in parseResult.rows) {
            // 匹配分类
            val category = dictRepository.getByValue(
                Constant.DICT_KEY_CONSUME_CATEGORY, row.category
            )
            if (category == null) {
                errors.add("第${row.rowIndex}行分类匹配失败：${row.category}")
                continue
            }

            // 匹配支付方式
            val channel = dictRepository.getByValue(
                Constant.DICT_KEY_PAY_CHANNEL, row.channel
            )
            if (channel == null) {
                errors.add("第${row.rowIndex}行支付方式匹配失败：${row.channel}")
                continue
            }

            // 解析支付日期
            val payDate = row.date.replace("-", "").toIntOrNull()
            if (payDate == null) {
                errors.add("第${row.rowIndex}行日期格式无效：${row.date}")
                continue
            }

            val currentTime = Date()

            bills.add(
                ConsumeBill(
                    amount = row.amountFen,
                    categoryId = category.id.toInt(),
                    payChannelId = channel.id.toInt(),
                    payDate = payDate,
                    billMonth = billMonth,
                    remark = row.remark,
                    billKind = "NORMAL",
                    importFileId = null,
                    createTime = currentTime,
                    updateTime = currentTime
                )
            )
        }

        // 如果有匹配错误，整个文件失败
        if (errors.isNotEmpty()) {
            val errorMsg = errors.joinToString("；")
            saveImportHistory(fileName, md5, 0, ImportConstants.STATUS_FAILED, errorMsg)
            return ImportResult.Failed(errorMsg)
        }

        // 6. 执行导入（事务）
        return try {
            // 如果选择覆盖，先删除该月份所有支出账单
            if (shouldOverwrite) {
                consumeBillDao.deleteByBillMonth(billMonth)
            }

            // 批量插入
            consumeBillDao.insertAll(bills)

            // 记录成功历史
            saveImportHistory(fileName, md5, bills.size, ImportConstants.STATUS_SUCCESS, null)

            ImportResult.Success(bills.size, billMonth)

        } catch (e: Exception) {
            // 记录失败历史
            saveImportHistory(fileName, md5, 0, ImportConstants.STATUS_FAILED, e.message ?: "未知异常")
            ImportResult.Failed("数据库保存失败：${e.message}")
        }
    }

    /**
     * 保存导入历史记录
     */
    private suspend fun saveImportHistory(
        fileName: String,
        md5: String,
        recordCount: Int,
        status: String,
        errorMsg: String?
    ) {
        val now = Date()
        val record = ImportFileHis(
            fileName = fileName,
            fileRow = recordCount,
            fileMd5 = md5,
            status = status,
            createTime = now,
            updateTime = now
        )
        // 注意：ImportFileHis 没有存储错误信息的字段，但为了追溯失败原因，
        // 建议后续可通过 Log 记录，或扩展表结构增加 ERROR_MSG 字段
        // 目前保留此项
        importFileHisDao.insert(record)
    }
}
package cn.yangwanhao.billapp.service

import androidx.room.withTransaction
import cn.yangwanhao.billapp.common.Constant
import cn.yangwanhao.billapp.common.ImportConstants
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.entity.ConsumeBill
import cn.yangwanhao.billapp.entity.ImportFileHis
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.ImportFileHisRepository
import cn.yangwanhao.billapp.utils.ExcelParser
import cn.yangwanhao.billapp.utils.FileUtils
import java.io.File
import java.util.Date

sealed class ImportResult {
    data class Success(val recordCount: Int, val billMonth: Int, val importFileId: Long) : ImportResult()
    data class Failed(val reason: String) : ImportResult()
    data class AlreadyImported(val md5: String) : ImportResult()
    data class Conflict(val billMonth: Int, val existingCount: Int) : ImportResult()
}

class ImportService(
    private val database: BillDatabase,  // 🔥 注入数据库实例，用于事务
    private val importFileHisRepository: ImportFileHisRepository,
    private val dictRepository: DictRepository,
    private val consumeBillRepository: ConsumeBillRepository
) {

    fun parseBillMonthFromFileName(fileName: String): Int? {
        val match = ImportConstants.FILE_NAME_PATTERN.find(fileName)
        return match?.groupValues?.get(1)?.replace("-", "")?.toIntOrNull()
    }

    suspend fun importFile(
        file: File,
        onConflict: suspend (billMonth: Int, existingCount: Int) -> Boolean
    ): ImportResult {
        // 1. 校验文件名
        val fileName = file.name
        val billMonth = parseBillMonthFromFileName(fileName)
            ?: return ImportResult.Failed("文件名格式不正确，应为 yyyy-MM.xlsx 或 yyyy-MM.xls")

        // 2. 计算 MD5，检查是否已导入
        val md5 = FileUtils.getFileMd5(file)
        val existingRecord = importFileHisRepository.getByMd5(md5)
        if (existingRecord != null) {
            return ImportResult.AlreadyImported(md5)
        }

        // 3. 检查该月份是否已有账单
        val existingCount = consumeBillRepository.countByBillMonth(billMonth)
        var shouldOverwrite = false
        if (existingCount > 0) {
            shouldOverwrite = onConflict(billMonth, existingCount)
            if (!shouldOverwrite) {
                return ImportResult.Failed("用户取消覆盖")
            }
        }

        // 4. 解析 Excel（在事务外执行，避免事务持有时间过长）
        val parseResult = ExcelParser.parse(file)
        if (parseResult.errors.isNotEmpty()) {
            val errorMsg = parseResult.errors.joinToString("；")
            return ImportResult.Failed(errorMsg)
        }
        if (parseResult.rows.isEmpty()) {
            return ImportResult.Failed("文件无有效数据")
        }

        // 5. 匹配分类和支付方式（在事务外执行）
        val billDataList = mutableListOf<BillData>()
        val errors = mutableListOf<String>()

        for (row in parseResult.rows) {
            val category = dictRepository.getByValue(
                Constant.DICT_KEY_CONSUME_CATEGORY, row.category
            )
            if (category == null) {
                errors.add("第${row.rowIndex}行分类匹配失败：${row.category}")
                continue
            }

            val channel = dictRepository.getByValue(
                Constant.DICT_KEY_PAY_CHANNEL, row.channel
            )
            if (channel == null) {
                errors.add("第${row.rowIndex}行支付方式匹配失败：${row.channel}")
                continue
            }

            val payDate = row.date.replace("-", "").toIntOrNull()
            if (payDate == null) {
                errors.add("第${row.rowIndex}行日期格式无效：${row.date}")
                continue
            }

            billDataList.add(
                BillData(
                    amountFen = row.amountFen,
                    categoryId = category.id.toInt(),
                    channelId = channel.id.toInt(),
                    payDate = payDate,
                    remark = row.remark
                )
            )
        }

        if (errors.isNotEmpty()) {
            return ImportResult.Failed(errors.joinToString("；"))
        }

        // ========== 🔥 6. 执行事务（使用 Room 的 withTransaction） ==========
        return try {
            database.withTransaction {
                // 6.1 先插入文件记录，获得 ID
                val now = Date()
                val importRecord = ImportFileHis(
                    fileName = fileName,
                    fileRow = billDataList.size,
                    fileMd5 = md5,
                    status = ImportConstants.STATUS_SUCCESS,
                    createTime = now,
                    updateTime = now
                )
                val importFileId = importFileHisRepository.insert(importRecord)

                // 6.2 如果选择覆盖，删除该月份所有支出账单
                if (shouldOverwrite) {
                    consumeBillRepository.deleteByBillMonth(billMonth)
                }

                // 6.3 构建账单列表（带上 importFileId）
                val bills = billDataList.map { data ->
                    ConsumeBill(
                        amount = data.amountFen,
                        categoryId = data.categoryId,
                        payChannelId = data.channelId,
                        payDate = data.payDate,
                        billMonth = billMonth,
                        remark = data.remark,
                        billKind = "NORMAL",
                        importFileId = importFileId,
                        createTime = now,
                        updateTime = now
                    )
                }

                // 6.4 批量插入账单
                consumeBillRepository.insertAll(bills)

                // 6.5 如果事务执行到这一步，所有操作都成功了
                ImportResult.Success(bills.size, billMonth, importFileId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 事务回滚后，任何操作都不会持久化
            // 但注意：如果回滚成功，导入历史记录也不会存在
            ImportResult.Failed("导入失败：${e.message}")
        }
    }

    private data class BillData(
        val amountFen: Int,
        val categoryId: Int,
        val channelId: Int,
        val payDate: Int,
        val remark: String
    )
}
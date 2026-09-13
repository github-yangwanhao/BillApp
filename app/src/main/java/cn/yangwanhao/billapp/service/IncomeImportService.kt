package cn.yangwanhao.billapp.service

import androidx.room.withTransaction
import cn.yangwanhao.billapp.common.Constant
import cn.yangwanhao.billapp.common.ImportConstants
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.entity.ImportFileHis
import cn.yangwanhao.billapp.entity.IncomeBill
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.ImportFileHisRepository
import cn.yangwanhao.billapp.repository.IncomeBillRepository
import cn.yangwanhao.billapp.utils.FileUtils
import cn.yangwanhao.billapp.utils.IncomeExcelParser
import java.io.File
import java.util.Date

sealed class IncomeImportResult {
    data class Success(val recordCount: Int, val importFileId: Long) : IncomeImportResult()
    data class Failed(val reason: String) : IncomeImportResult()
    data class AlreadyImported(val md5: String) : IncomeImportResult()
}

class IncomeImportService(
    private val database: BillDatabase,
    private val importFileHisRepository: ImportFileHisRepository,
    private val dictRepository: DictRepository,
    private val incomeBillRepository: IncomeBillRepository
) {

    suspend fun importFile(file: File): IncomeImportResult {
        val fileName = file.name

        // 1. MD5 查重（限定收入类型）
        val md5 = FileUtils.getFileMd5(file)
        val existing = importFileHisRepository.getByMd5AndType(md5, ImportConstants.IMPORT_TYPE_INCOME)
        if (existing != null) {
            return IncomeImportResult.AlreadyImported(md5)
        }

        // 2. 解析 Excel
        val parseResult = IncomeExcelParser.parse(file)
        if (parseResult.errors.isNotEmpty()) {
            return IncomeImportResult.Failed(parseResult.errors.joinToString("；"))
        }
        if (parseResult.rows.isEmpty()) {
            return IncomeImportResult.Failed("文件无有效数据")
        }

        // 3. 匹配分类 + 检查冲突
        val billDataList = mutableListOf<BillData>()
        val errors = mutableListOf<String>()
        val involvedMonths = mutableSetOf<Int>()

        for (row in parseResult.rows) {
            val category = dictRepository.getByValue(
                Constant.DICT_KEY_INCOME_CATEGORY, row.category
            )
            if (category == null) {
                errors.add("第${row.rowIndex}行类型匹配失败：${row.category}")
                continue
            }

            involvedMonths.add(row.billMonth)
            billDataList.add(
                BillData(
                    amountFen = row.amountFen,
                    categoryId = category.id.toInt(),
                    postDate = row.postDate,
                    billMonth = row.billMonth,
                    remark = row.remark
                )
            )
        }

        if (errors.isNotEmpty()) {
            return IncomeImportResult.Failed(errors.joinToString("；"))
        }

        // 🔥 4. 检查每个涉及的月份是否有已存数据 → 有则整个文件失败
        for (month in involvedMonths) {
            val count = incomeBillRepository.countByBillMonth(month)
            if (count > 0) {
                val year = month / 100
                val m = month % 100
                return IncomeImportResult.Failed(
                    "${year}年${m}月已有 $count 条收入记录，不能导入该文件"
                )
            }
        }

        // 5. 事务：插入文件记录 + 批量插入收入
        return try {
            database.withTransaction {
                val now = Date()
                val importRecord = ImportFileHis(
                    fileName = fileName,
                    fileRow = billDataList.size,
                    fileMd5 = md5,
                    importType = ImportConstants.IMPORT_TYPE_INCOME,
                    status = ImportConstants.STATUS_SUCCESS,
                    createTime = now,
                    updateTime = now
                )
                val importFileId = importFileHisRepository.insert(importRecord)

                val bills = billDataList.map { data ->
                    IncomeBill(
                        amount = data.amountFen,
                        categoryId = data.categoryId,
                        postDate = data.postDate,
                        billMonth = data.billMonth,
                        remark = data.remark,
                        createTime = now,
                        updateTime = now
                    )
                }
                incomeBillRepository.insertAll(bills)

                IncomeImportResult.Success(bills.size, importFileId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            IncomeImportResult.Failed("导入失败：${e.message}")
        }
    }

    private data class BillData(
        val amountFen: Int,
        val categoryId: Int,
        val postDate: Int,
        val billMonth: Int,
        val remark: String
    )
}
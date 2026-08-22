package cn.yangwanhao.billapp

import android.app.Application
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.database.DatabaseInitHelper
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.IncomeBillRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class BillApplication : Application() {

    val database: BillDatabase by lazy { BillDatabase.getDatabase(this) }
    val initHelper: DatabaseInitHelper by lazy { DatabaseInitHelper(this, database) }

    // Repository 懒加载
    val dictRepository: DictRepository by lazy {
        DictRepository(database.dictDao())
    }
    val consumeBillRepository: ConsumeBillRepository by lazy {
        ConsumeBillRepository(database.consumeBillDao(), database.dictDao())
    }
    val incomeBillRepository: IncomeBillRepository by lazy {
        IncomeBillRepository(database.incomeBillDao(), database.dictDao())
    }

    override fun onCreate() {
        super.onCreate()

        // 在后台线程中异步检查并初始化默认字典数据
        MainScope().launch {
            initHelper.initDatabaseIfNeeded()
        }
    }
}
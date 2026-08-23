package cn.yangwanhao.billapp

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import cn.yangwanhao.billapp.database.BillDatabase
import cn.yangwanhao.billapp.database.DatabaseInitHelper
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.IncomeBillRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Locale

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

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(updateLanguage(base!!))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 配置变化时保持语言不变
        updateLanguage(baseContext)
    }

    /**
     * 强制将 Context 的语言设置为简体中文
     */
    private fun updateLanguage(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        // 简体中文
        config.setLocale(Locale.CHINA)
        // 如果希望繁体中文，用 Locale.TAIWAN
        return context.createConfigurationContext(config)
    }
}
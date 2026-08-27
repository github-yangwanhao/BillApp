package cn.yangwanhao.billapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cn.yangwanhao.billapp.dao.ConsumeBillDao
import cn.yangwanhao.billapp.dao.DictDao
import cn.yangwanhao.billapp.dao.ImportFileHisDao
import cn.yangwanhao.billapp.dao.IncomeBillDao
import cn.yangwanhao.billapp.entity.ConsumeBill
import cn.yangwanhao.billapp.entity.Dict
import cn.yangwanhao.billapp.entity.ImportFileHis
import cn.yangwanhao.billapp.entity.IncomeBill

@Database(
    entities = [
        ConsumeBill::class,
        IncomeBill::class,
        Dict::class,
        ImportFileHis::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class BillDatabase : RoomDatabase() {

    abstract fun consumeBillDao(): ConsumeBillDao
    abstract fun incomeBillDao(): IncomeBillDao
    abstract fun dictDao(): DictDao
    abstract fun importFileHisDao(): ImportFileHisDao

    companion object {
        @Volatile
        private var INSTANCE: BillDatabase? = null

        fun getDatabase(context: Context): BillDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BillDatabase::class.java,
                    "bill_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
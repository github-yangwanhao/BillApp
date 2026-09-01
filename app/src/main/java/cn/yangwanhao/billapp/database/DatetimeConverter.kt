package cn.yangwanhao.billapp.database

import androidx.room.TypeConverter
import java.util.Date

class DateTimeConverter {
    @TypeConverter
    fun timestampToDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
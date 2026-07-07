package de.pattaku.worktracker.data.db

import androidx.room.TypeConverter
import de.pattaku.worktracker.data.model.PunchKind
import java.time.Instant

/** Instant <-> Long(epochMilli), PunchKind <-> String(name). */
class Converters {
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun kindToString(value: PunchKind?): String? = value?.name

    @TypeConverter
    fun stringToKind(value: String?): PunchKind? = value?.let { PunchKind.valueOf(it) }
}

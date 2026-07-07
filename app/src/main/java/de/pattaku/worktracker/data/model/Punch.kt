package de.pattaku.worktracker.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Ein einzelnes Stempel-Ereignis. [ts] ist IMMER ein UTC-Instant — niemals lokale Zeit
 * persistieren (§13 DST). [auto] markiert vom AutoCloseReceiver erzeugte Buchungen.
 */
@Entity(tableName = "punches")
data class Punch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Instant,
    val kind: PunchKind,
    @ColumnInfo(defaultValue = "0") val auto: Boolean = false,
)

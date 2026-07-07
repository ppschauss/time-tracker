package de.pattaku.worktracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import de.pattaku.worktracker.data.model.Punch
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Tie-Break `id ASC` ist PFLICHT: BREAK_END und CLOCK_OUT eines Abend-Taps teilen sich denselben
 * [Punch.ts]. Nur die Einfüge-Reihenfolge (id) stellt die korrekte fachliche Ordnung sicher (§13).
 */
@Dao
interface PunchDao {
    @Insert
    suspend fun insert(punch: Punch): Long

    @Update
    suspend fun update(punch: Punch)

    @Delete
    suspend fun delete(punch: Punch)

    @Query("SELECT * FROM punches WHERE ts >= :from AND ts < :to ORDER BY ts ASC, id ASC")
    suspend fun between(from: Instant, to: Instant): List<Punch>

    @Query("SELECT * FROM punches WHERE ts >= :from AND ts < :to ORDER BY ts ASC, id ASC")
    fun observeBetween(from: Instant, to: Instant): Flow<List<Punch>>

    @Query("SELECT * FROM punches ORDER BY ts DESC, id DESC")
    fun observeAll(): Flow<List<Punch>>
}

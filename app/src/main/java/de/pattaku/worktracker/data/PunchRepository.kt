package de.pattaku.worktracker.data

import de.pattaku.worktracker.data.db.PunchDao
import de.pattaku.worktracker.data.model.Punch
import de.pattaku.worktracker.domain.dayBounds
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Dünne Schicht über [PunchDao]. Keine Business-Logik hier (§6). */
class PunchRepository(private val dao: PunchDao) {

    suspend fun insert(punch: Punch): Long = dao.insert(punch)

    suspend fun update(punch: Punch) = dao.update(punch)

    suspend fun delete(punch: Punch) = dao.delete(punch)

    suspend fun between(from: Instant, to: Instant): List<Punch> = dao.between(from, to)

    /** Live-Flow der Buchungen des lokalen Tages, in dem [now] liegt. */
    fun observeToday(now: Instant): Flow<List<Punch>> {
        val (from, to) = dayBounds(now)
        return dao.observeBetween(from, to)
    }

    fun observeAll(): Flow<List<Punch>> = dao.observeAll()
}

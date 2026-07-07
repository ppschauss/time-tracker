package de.pattaku.worktracker.domain

import de.pattaku.worktracker.data.db.PunchDao
import de.pattaku.worktracker.data.model.Punch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** In-Memory-DAO für Unit-Tests. Bildet die Sortier-/Tie-Break-Semantik von Room nach. */
class FakePunchDao : PunchDao {
    private val store = MutableStateFlow<List<Punch>>(emptyList())
    private var nextId = 1L

    private fun sortedAsc(list: List<Punch>) =
        list.sortedWith(compareBy({ it.ts }, { it.id }))

    override suspend fun insert(punch: Punch): Long {
        val id = nextId++
        store.value = store.value + punch.copy(id = id)
        return id
    }

    override suspend fun update(punch: Punch) {
        store.value = store.value.map { if (it.id == punch.id) punch else it }
    }

    override suspend fun delete(punch: Punch) {
        store.value = store.value.filterNot { it.id == punch.id }
    }

    override suspend fun between(from: Instant, to: Instant): List<Punch> =
        sortedAsc(store.value.filter { it.ts >= from && it.ts < to })

    override fun observeBetween(from: Instant, to: Instant): Flow<List<Punch>> =
        store.map { list -> sortedAsc(list.filter { it.ts >= from && it.ts < to }) }

    override fun observeAll(): Flow<List<Punch>> =
        store.map { list -> list.sortedWith(compareByDescending<Punch> { it.ts }.thenByDescending { it.id }) }
}

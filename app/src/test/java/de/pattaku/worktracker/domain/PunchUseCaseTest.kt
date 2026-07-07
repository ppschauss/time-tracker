package de.pattaku.worktracker.domain

import de.pattaku.worktracker.data.PunchRepository
import de.pattaku.worktracker.data.model.PunchKind
import de.pattaku.worktracker.settings.AppSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.Duration
import java.time.LocalTime
import org.junit.Test

/** Deckt §14.1–4 ab: eine fixe Clock steuert die "Tageszeit" jedes Taps. */
class PunchUseCaseTest {

    private fun setup(startTime: java.time.Instant): Triple<PunchUseCase, MutableClock, PunchRepository> {
        val dao = FakePunchDao()
        val repo = PunchRepository(dao)
        val clock = MutableClock(startTime)
        val useCase = PunchUseCase(repo, { AppSettings() }, clock)
        return Triple(useCase, clock, repo)
    }

    private suspend fun allToday(repo: PunchRepository, at: java.time.Instant) =
        repo.between(dayBounds(at).first, dayBounds(at).second)

    // §14.1
    @Test
    fun leer_tap_0800_gibt_genau_ein_clock_in_und_working() = runTest {
        val at = berlin(2026, 7, 7, LocalTime.of(8, 0))
        val (uc, _, repo) = setup(at)

        val r = uc.punch()

        assertTrue(r is PunchResult.Recorded)
        r as PunchResult.Recorded
        assertEquals(listOf(PunchKind.CLOCK_IN), r.kinds)
        val events = allToday(repo, at)
        assertEquals(1, events.size)
        assertEquals(PunchKind.CLOCK_IN, events.single().kind)
        assertEquals(WorkState.WORKING, r.summary.state)
    }

    // §14.2
    @Test
    fun tap_1200_working_gibt_break_start_dann_break_end_pause_steigt() = runTest {
        val (uc, clock, repo) = setup(berlin(2026, 7, 7, LocalTime.of(8, 0)))
        uc.punch() // CLOCK_IN 08:00

        clock.current = berlin(2026, 7, 7, LocalTime.of(12, 0))
        val r1 = uc.punch()
        assertEquals(listOf(PunchKind.BREAK_START), (r1 as PunchResult.Recorded).kinds)
        assertEquals(WorkState.ON_BREAK, r1.summary.state)

        clock.current = berlin(2026, 7, 7, LocalTime.of(12, 30))
        val r2 = uc.punch()
        assertEquals(listOf(PunchKind.BREAK_END), (r2 as PunchResult.Recorded).kinds)
        assertEquals(WorkState.WORKING, r2.summary.state)
        // Pause = 12:00–12:30 = 30 min
        assertEquals(Duration.ofMinutes(30), r2.summary.pause)
    }

    // §14.3
    @Test
    fun tap_abend_working_gibt_clock_out_dann_noop() = runTest {
        val (uc, clock, repo) = setup(berlin(2026, 7, 7, LocalTime.of(8, 0)))
        uc.punch() // CLOCK_IN 08:00

        clock.current = berlin(2026, 7, 7, LocalTime.of(17, 0)) // im Abendfenster 16:30–20:00
        val r1 = uc.punch()
        assertEquals(listOf(PunchKind.CLOCK_OUT), (r1 as PunchResult.Recorded).kinds)
        assertEquals(WorkState.DONE, r1.summary.state)

        clock.current = berlin(2026, 7, 7, LocalTime.of(17, 5))
        val r2 = uc.punch()
        assertEquals(PunchResult.NoOp, r2)
        // Keine neuen Events durch NoOp.
        assertEquals(2, allToday(repo, clock.current).size)
    }

    // §14.4
    @Test
    fun tap_abend_on_break_gibt_break_end_und_clock_out_in_reihenfolge() = runTest {
        val (uc, clock, repo) = setup(berlin(2026, 7, 7, LocalTime.of(8, 0)))
        uc.punch() // CLOCK_IN 08:00

        clock.current = berlin(2026, 7, 7, LocalTime.of(12, 0))
        uc.punch() // BREAK_START 12:00 (kein Abend)

        clock.current = berlin(2026, 7, 7, LocalTime.of(17, 0)) // Abendfenster, Zustand ON_BREAK
        val r = uc.punch() as PunchResult.Recorded
        assertEquals(listOf(PunchKind.BREAK_END, PunchKind.CLOCK_OUT), r.kinds)
        assertEquals(WorkState.DONE, r.summary.state)

        // Reihenfolge in DB korrekt (Tie-Break id ASC bei gleichem ts).
        val events = allToday(repo, clock.current)
        assertEquals(
            listOf(PunchKind.CLOCK_IN, PunchKind.BREAK_START, PunchKind.BREAK_END, PunchKind.CLOCK_OUT),
            events.map { it.kind },
        )
        // worked zählt Pause NICHT: 08:00–12:00 = 4h; Pause 12:00–17:00 = 5h.
        assertEquals(Duration.ofHours(4), r.summary.worked)
        assertEquals(Duration.ofHours(5), r.summary.pause)
    }
}

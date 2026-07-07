package de.pattaku.worktracker.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import de.pattaku.worktracker.App
import de.pattaku.worktracker.data.model.Punch
import de.pattaku.worktracker.domain.ZONE
import de.pattaku.worktracker.domain.summarize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Exportiert alle Buchungen als CSV und öffnet das Share-Sheet (§11). Datei liegt im cacheDir und
 * wird über den FileProvider geteilt — kein Storage-Permission nötig. Zeiten in Europe/Berlin.
 */
class CsvExporter(private val context: Context) {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.GERMANY)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.GERMANY)

    fun exportAndShare() {
        val container = App.container(context)
        container.appScope.launch {
            val punches = container.punchRepository.observeAll().first()
                .sortedWith(compareBy({ it.ts }, { it.id }))
            val csv = buildCsv(punches)
            val file = writeToCache(csv)
            withContext(Dispatchers.Main) { share(file) }
        }
    }

    private fun buildCsv(punches: List<Punch>): String = buildString {
        // Roh-Events
        appendLine("datum;zeit;kind;auto")
        punches.forEach { p ->
            val z = p.ts.atZone(ZONE)
            appendLine("${z.format(dateFmt)};${z.format(timeFmt)};${p.kind.name};${p.auto}")
        }

        // Aggregiert pro Tag
        appendLine()
        appendLine("datum;ein;aus;arbeit_min;pause_min")
        val today = Instant.now().atZone(ZONE).toLocalDate()
        punches.groupBy { it.ts.atZone(ZONE).toLocalDate() }
            .toSortedMap()
            .forEach { (date, dayEvents) ->
                val capAt = capFor(date, today)
                val sum = summarize(dayEvents.sortedWith(compareBy({ it.ts }, { it.id })), capAt)
                val ein = sum.firstIn?.atZone(ZONE)?.format(timeFmt) ?: ""
                val aus = sum.lastOut?.atZone(ZONE)?.format(timeFmt) ?: ""
                appendLine("${date.format(dateFmt)};$ein;$aus;${sum.worked.toMinutes()};${sum.pause.toMinutes()}")
            }
    }

    private fun capFor(date: LocalDate, today: LocalDate): Instant =
        if (date == today) Instant.now()
        else date.plusDays(1).atStartOfDay(ZONE).toInstant()

    private fun writeToCache(csv: String): File {
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val file = File(dir, "worktracker_export.csv")
        file.writeText(csv)
        return file
    }

    private fun share(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "CSV exportieren")
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(chooser)
    }
}

package de.pattaku.worktracker.trigger

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import de.pattaku.worktracker.R
import de.pattaku.worktracker.domain.DaySummary
import de.pattaku.worktracker.domain.label
import de.pattaku.worktracker.domain.toHMM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick-Settings-Tile: ein Tap = ein punch(). Zeigt Status + gearbeitete Zeit als Subtitle.
 * Endet — wie Widget und Shortcut — im selben PunchUseCase.punch() (§8).
 */
class PunchTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        Puncher.fire(this) { refresh() }
    }

    /** Liest den aktuellen Status frisch aus der DB und aktualisiert die Kachel. */
    private fun refresh() {
        val scope = de.pattaku.worktracker.App.container(this).appScope
        scope.launch {
            val status = Puncher.currentStatus(this@PunchTileService)
            withContext(Dispatchers.Main) { applyStatus(status) }
        }
    }

    private fun applyStatus(status: DaySummary) {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "${status.state.label()} · ${status.worked.toHMM()}"
        }
        tile.updateTile()
    }
}

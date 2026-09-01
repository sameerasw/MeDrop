package com.sameerasw.medrop.services.tiles

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.google.gson.Gson
import com.sameerasw.medrop.R
import com.sameerasw.medrop.data.repository.MeDropRepository
import com.sameerasw.medrop.domain.model.MeDropSettings
import com.sameerasw.medrop.ui.activities.MeDropActivity

class MeDropTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        val repo = MeDropRepository(this)
        repo.setTileAdded(true)
        updateTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        val repo = MeDropRepository(this)
        repo.setTileAdded(false)
    }

    override fun onStartListening() {
        super.onStartListening()
        val repo = MeDropRepository(this)
        repo.setTileAdded(true)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val repo = MeDropRepository(this)
        val settingsJson = repo.getMeDropSettingsJson()

        var subtitle = getString(R.string.feat_medrop_set_up)
        var state = Tile.STATE_INACTIVE

        if (settingsJson != null) {
            try {
                val settings = Gson().fromJson(settingsJson, MeDropSettings::class.java)
                if (settings.contact != null) {
                    state = Tile.STATE_ACTIVE
                    subtitle = settings.contact.displayName
                }
            } catch (_: Exception) {}
        }

        tile.state = state
        tile.label = getString(R.string.feat_medrop_title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val repo = MeDropRepository(this)
        val allowWhenLocked = repo.isMeDropAllowWhenLocked()

        val launchAction = {
            val intent = Intent(this, MeDropActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }

        if (isLocked && !allowWhenLocked) {
            unlockAndRun {
                launchAction()
            }
        } else {
            launchAction()
        }
    }
}

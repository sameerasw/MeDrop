package com.sameerasw.medrop.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.sameerasw.medrop.R
import com.sameerasw.medrop.domain.model.MeDropProfileType
import com.sameerasw.medrop.domain.model.MeDropSettings
import com.sameerasw.medrop.ui.activities.MeDropActivity

object DynamicShortcutManager {

    private const val SHORTCUT_ID_PREFIX = "shortcut_profile_"

    fun updateShortcuts(context: Context, settings: MeDropSettings?) {
        val contact = settings?.contact
        if (contact == null) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            return
        }

        val shortcuts = mutableListOf<ShortcutInfoCompat>()

        val profilesToProcess = listOf(
            Triple(
                MeDropProfileType.CONTACT,
                context.getString(R.string.feat_medrop_profile_contact),
                R.drawable.medrop_logo
            ),
            Triple(
                MeDropProfileType.PROFESSIONAL,
                context.getString(R.string.feat_medrop_profile_professional),
                R.drawable.rounded_work_24
            ),
            Triple(
                MeDropProfileType.CUSTOM,
                context.getString(R.string.feat_medrop_profile_custom),
                R.drawable.rounded_id_card_24
            )
        )

        var rank = 0
        for ((type, defaultTitle, defaultIconRes) in profilesToProcess) {
            val isProfileEnabled = when (type) {
                MeDropProfileType.CONTACT -> true
                MeDropProfileType.PROFESSIONAL -> settings.professionalProfile.enabled
                MeDropProfileType.CUSTOM -> settings.customProfile.enabled
            }

            if (!isProfileEnabled) continue

            val displayName = settings.getEffectiveDisplayName(type).ifBlank { defaultTitle }
            val photoUri = settings.getEffectivePhotoUri(type)
            val iconCompat = getProfileIcon(context, photoUri, defaultIconRes)

            val intent = Intent(context, MeDropActivity::class.java).apply {
                action = "com.sameerasw.medrop.action.SHARE"
                putExtra("extra_profile_type", type.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val shortcut = ShortcutInfoCompat.Builder(context, "$SHORTCUT_ID_PREFIX${type.name.lowercase()}")
                .setShortLabel(displayName)
                .setLongLabel(displayName)
                .setIcon(iconCompat)
                .setIntent(intent)
                .setRank(rank++)
                .build()

            shortcuts.add(shortcut)
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private fun getProfileIcon(context: Context, photoUriStr: String?, defaultIconRes: Int): IconCompat {
        if (!photoUriStr.isNullOrBlank()) {
            try {
                val bitmap = if (photoUriStr.startsWith("content://") || photoUriStr.startsWith("file://")) {
                    val uri = Uri.parse(photoUriStr)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else if (photoUriStr.length > 100) {
                    val bytes = Base64.decode(photoUriStr, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else null

                if (bitmap != null) {
                    val circular = getCircularBitmap(bitmap)
                    return IconCompat.createWithBitmap(circular)
                }
            } catch (_: Exception) {}
        }

        return IconCompat.createWithResource(context, defaultIconRes)
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        val rect = Rect(0, 0, size, size)
        val srcRect = Rect(
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            (bitmap.width + size) / 2,
            (bitmap.height + size) / 2
        )

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, srcRect, rect, paint)

        return output
    }
}

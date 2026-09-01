package com.sameerasw.medrop.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.sameerasw.medrop.R
import java.util.EnumMap
import kotlin.math.max

object QrCodeGenerator {

    fun generateQrBitmap(
        content: String,
        size: Int = 600,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        logo: Bitmap? = null,
    ): Bitmap? {
        if (content.isBlank()) return null

        val errorCorrectionLevels = listOf(ErrorCorrectionLevel.M, ErrorCorrectionLevel.L)
        val writer = QRCodeWriter()

        for (ecLevel in errorCorrectionLevels) {
            try {
                val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                    put(EncodeHintType.CHARACTER_SET, "UTF-8")
                    put(EncodeHintType.ERROR_CORRECTION, ecLevel)
                    put(EncodeHintType.MARGIN, 2)
                }

                val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
                val moduleCount = bitMatrix.width
                val moduleSize = size.toFloat() / moduleCount

                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(backgroundColor)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = foregroundColor
                    style = Paint.Style.FILL
                }

                val rect = RectF()

                for (y in 0 until moduleCount) {
                    for (x in 0 until moduleCount) {
                        if (bitMatrix.get(x, y)) {
                            val left = x * moduleSize
                            val top = y * moduleSize
                            val right = left + moduleSize
                            val bottom = top + moduleSize
                            rect.set(left, top, right, bottom)
                            canvas.drawRect(rect, paint)
                        }
                    }
                }

                if (logo != null) {
                    val logoSize = size * 0.20f
                    val logoMargin = (size - logoSize) / 2f
                    val badgeRect = RectF(logoMargin, logoMargin, logoMargin + logoSize, logoMargin + logoSize)

                    val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = backgroundColor
                        style = Paint.Style.FILL
                    }
                    val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#26000000")
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                    }
                    canvas.drawOval(badgeRect, badgeBgPaint)
                    canvas.drawOval(badgeRect, badgeStrokePaint)

                    val innerPadding = logoSize * 0.14f
                    val innerRect = RectF(
                        badgeRect.left + innerPadding,
                        badgeRect.top + innerPadding,
                        badgeRect.right - innerPadding,
                        badgeRect.bottom - innerPadding,
                    )
                    canvas.drawBitmap(logo, null, innerRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                }

                return bitmap
            } catch (_: Exception) {
                // Try next error correction level or return null
            }
        }
        return null
    }

    fun getAppLogoBitmap(context: Context): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
            val bitmap = Bitmap.createBitmap(
                max(1, drawable.intrinsicWidth),
                max(1, drawable.intrinsicHeight),
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}

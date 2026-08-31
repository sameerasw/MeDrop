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
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.sameerasw.medrop.R
import kotlin.math.max
import java.util.HashMap
import java.util.Map


object QrCodeGenerator {

    fun generateQrBitmap(
        content: String,
        size: Int =600,
        foregroundColor: Int = Color.WHITE,
        backgroundColor: Int = Color.BLACK,
        logo: Bitmap? = null,
    ): Bitmap? {
        // Standard limit for alphanumeric QR codes with low error correction is 4,296 characters
        // We use a safe limit of 2,953 to ensure reliability across all QR versions
        if (content.trim().isEmpty() || content.length > 2953) {
            return null
        }

        try {
            val writer = QRCodeWriter()
            val hints: MutableMap<EncodeHintType, Any> = HashMap()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
            hints[EncodeHintType.MARGIN] = 0
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            val moduleCount = bitMatrix.width
            val moduleSize = size.toFloat() / moduleCount

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(backgroundColor)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = foregroundColor
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 0.8f
            }

            val rect = RectF()

            for (y in 0 until moduleCount) {
                for (x in 0 until moduleCount) {
                    if (bitMatrix.get(x, y)) {
                        // We add a tiny 0.5f overlap to ensure squares touch perfectly
                        // and appear "chunkier" by eliminating anti-aliased gaps.
                        val left = x * moduleSize
                        val top = y * moduleSize
                        val right = (x + 1) * moduleSize + 0.5f
                        val bottom = (y + 1) * moduleSize + 0.5f
                        rect.set(left, top, right, bottom)
                        canvas.drawRect(rect, paint)
                    }
                }
            }

            if (logo != null) {
                val logoSize = size * 0.20f
                val logoMargin = (size - logoSize) / 2f
                val badgeRect =
                    RectF(logoMargin, logoMargin, logoMargin + logoSize, logoMargin + logoSize)

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
                canvas.drawBitmap(
                    logo,
                    null,
                    innerRect,
                    Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                )
            }

            return bitmap
        } catch (e: WriterException) {
            return null
        }
    }

    fun getAppLogoBitmap(context: Context): Bitmap? {
        return try {
            val drawable =
                ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
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

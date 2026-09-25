package com.madeby.JAI

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object LocalAvatarManager {
    private const val AVATAR_FILE_NAME = "profile_avatar.jpg"
    private const val MAX_DIMENSION = 512
    private const val COMPRESS_QUALITY = 88

    private val GOOGLE_PALETTE = intArrayOf(
        0xFFE53935.toInt(), // Red
        0xFFD81B60.toInt(), // Pink
        0xFF8E24AA.toInt(), // Purple
        0xFF5E35B1.toInt(), // Deep Purple
        0xFF3949AB.toInt(), // Indigo
        0xFF1E88E5.toInt(), // Blue
        0xFF0288D1.toInt(), // Light Blue
        0xFF00897B.toInt(), // Teal
        0xFF43A047.toInt(), // Green
        0xFF7CB342.toInt(), // Light Green
        0xFFF4511E.toInt(), // Deep Orange
        0xFFFB8C00.toInt(), // Orange
        0xFF6D4C41.toInt(), // Brown
        0xFF546E7A.toInt()  // Blue Grey
    )

    fun getGoogleAvatarColor(name: String?): Int {
        if (name.isNullOrBlank()) return GOOGLE_PALETTE[0]
        val hash = Math.abs(name.trim().lowercase().hashCode())
        return GOOGLE_PALETTE[hash % GOOGLE_PALETTE.size]
    }

    fun getLetterAvatarBitmap(name: String?, targetSizePx: Int): Bitmap {
        val letter = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
        val bgColor = getGoogleAvatarColor(name)
        val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw Google-colored Circle
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val radius = targetSizePx / 2f
        canvas.drawCircle(radius, radius, radius, bgPaint)

        // Draw Bold White Letter
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = targetSizePx * 0.48f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = textPaint.fontMetrics
        val yOffset = radius - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(letter, radius, yOffset, textPaint)

        return bitmap
    }

    fun getAvatarFile(context: Context): File {
        return File(context.filesDir, AVATAR_FILE_NAME)
    }

    fun hasCustomAvatar(context: Context): Boolean {
        val file = getAvatarFile(context)
        return file.exists() && file.length() > 0
    }

    fun deleteAvatar(context: Context): Boolean {
        val file = getAvatarFile(context)
        AuthManager.saveProfileImageUri(context, "")
        return if (file.exists()) file.delete() else false
    }

    fun saveAvatarFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                var tempStream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(tempStream, null, options)
                tempStream?.close()

                val origWidth = options.outWidth
                val origHeight = options.outHeight
                if (origWidth <= 0 || origHeight <= 0) return false

                var sampleSize = 1
                while (origWidth / sampleSize > MAX_DIMENSION * 2 || origHeight / sampleSize > MAX_DIMENSION * 2) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                tempStream = context.contentResolver.openInputStream(uri)
                val sampledBitmap = BitmapFactory.decodeStream(tempStream, null, decodeOptions)
                tempStream?.close()

                if (sampledBitmap == null) return false

                val size = Math.min(sampledBitmap.width, sampledBitmap.height)
                val xOffset = (sampledBitmap.width - size) / 2
                val yOffset = (sampledBitmap.height - size) / 2
                val squareBitmap = Bitmap.createBitmap(sampledBitmap, xOffset, yOffset, size, size)

                val finalBitmap = if (size > MAX_DIMENSION) {
                    Bitmap.createScaledBitmap(squareBitmap, MAX_DIMENSION, MAX_DIMENSION, true)
                } else {
                    squareBitmap
                }

                val targetFile = getAvatarFile(context)
                val fos = FileOutputStream(targetFile)
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, fos)
                fos.flush()
                fos.close()
                AuthManager.saveProfileImageUri(context, targetFile.absolutePath)
                markAvatarPendingUpload(context, true)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun getCircularAvatarBitmap(context: Context, targetSizePx: Int): Bitmap? {
        val file = getAvatarFile(context)
        if (file.exists() && file.length() > 0L) {
            return try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
                val output = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rect = Rect(0, 0, targetSizePx, targetSizePx)
                val rectF = RectF(rect)

                canvas.drawARGB(0, 0, 0, 0)
                canvas.drawOval(rectF, paint)

                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), rect, paint)
                output
            } catch (_: Exception) {
                null
            }
        }

        // If local file not cached yet, check memory cache for remote avatar URL
        val remoteUrl = AuthManager.getProfileImageUri(context)?.trim() ?: ""
        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            val cacheKey = "${remoteUrl}_$targetSizePx"
            remoteAvatarCache.get(cacheKey)?.let { return it }
        }

        return null
    }

    fun downloadAndSaveRemoteAvatar(context: Context, urlStr: String): Boolean {
        if (urlStr.isBlank() || !urlStr.startsWith("http")) return false
        return try {
            val url = java.net.URL(urlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            if (conn.responseCode in 200..299) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    val file = getAvatarFile(context)
                    FileOutputStream(file).use { fos ->
                        fos.write(bytes)
                        fos.flush()
                    }
                    AuthManager.saveProfileImageUri(context, file.absolutePath)
                    true
                } else false
            } else false
        } catch (_: Exception) {
            false
        }
    }

    private const val KEY_AVATAR_PENDING_UPLOAD = "avatar_pending_upload"
    private val remoteAvatarCache = android.util.LruCache<String, Bitmap>(60)

    fun isAvatarPendingUpload(context: Context): Boolean {
        return context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            .getBoolean(KEY_AVATAR_PENDING_UPLOAD, false)
    }

    fun markAvatarPendingUpload(context: Context, isPending: Boolean) {
        context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AVATAR_PENDING_UPLOAD, isPending)
            .apply()
    }

    fun resolvePresetToEmoji(presetOrUrl: String?): String {
        if (presetOrUrl.isNullOrBlank()) return ""
        val trimmed = presetOrUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:")) return trimmed
        return ""
    }

    fun getCircularBitmapFromUrl(context: Context, urlStr: String, targetSizePx: Int): Bitmap? {
        if (urlStr.isBlank()) return null
        val cacheKey = "${urlStr}_$targetSizePx"
        remoteAvatarCache.get(cacheKey)?.let { return it }

        return try {
            val url = java.net.URL(urlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "StudyTimer-Android")
            conn.setRequestProperty("Connection", "Keep-Alive")

            if (conn.responseCode in 200..299) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.isEmpty()) return null

                // First decode bounds only for optimal memory footprint
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                val originalSize = Math.max(options.outWidth, options.outHeight)
                var sampleSize = 1
                while (originalSize / (sampleSize * 2) >= targetSizePx) {
                    sampleSize *= 2
                }

                // Decode actual sampled bitmap
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

                val size = Math.min(rawBitmap.width, rawBitmap.height)
                val xOffset = (rawBitmap.width - size) / 2
                val yOffset = (rawBitmap.height - size) / 2
                val squareBitmap = Bitmap.createBitmap(rawBitmap, xOffset, yOffset, size, size)
                val scaledBitmap = if (size != targetSizePx) {
                    Bitmap.createScaledBitmap(squareBitmap, targetSizePx, targetSizePx, true)
                } else {
                    squareBitmap
                }
                val output = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rect = Rect(0, 0, targetSizePx, targetSizePx)
                val rectF = RectF(rect)
                canvas.drawARGB(0, 0, 0, 0)
                canvas.drawOval(rectF, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(scaledBitmap, Rect(0, 0, scaledBitmap.width, scaledBitmap.height), rect, paint)
                remoteAvatarCache.put(cacheKey, output)
                output
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

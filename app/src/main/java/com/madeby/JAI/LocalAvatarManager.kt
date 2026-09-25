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
        if (!file.exists() || file.length() == 0L) return null
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
        val map = mapOf(
            "avatar_default" to "🎓",
            "avatar_cat" to "🐱",
            "avatar_fox" to "🦊",
            "avatar_lion" to "🦁",
            "avatar_panda" to "🐼",
            "avatar_owl" to "🦉",
            "avatar_rocket" to "🚀",
            "avatar_fire" to "🔥",
            "avatar_star" to "⭐",
            "avatar_scholar" to "🎓",
            "avatar_books" to "📚",
            "avatar_brain" to "🧠",
            "avatar_science" to "🔬",
            "avatar_med" to "🩺",
            "avatar_coder" to "💻",
            "avatar_lightning" to "⚡",
            "avatar_artist" to "🎨",
            "avatar_lotus" to "🌸",
            "avatar_forest" to "🌲",
            "avatar_coffee" to "☕",
            "avatar_moon" to "🌙",
            "avatar_target" to "🎯",
            "avatar_diamond" to "💎",
            "avatar_champion" to "🏆",
            "avatar_crown" to "👑",
            "avatar_saturn" to "🪐",
            "avatar_gamer" to "🎮",
            "avatar_lofi" to "🎧",
            "cat" to "🐱",
            "fox" to "🦊",
            "lion" to "🦁",
            "panda" to "🐼",
            "owl" to "🦉",
            "rocket" to "🚀",
            "fire" to "🔥",
            "star" to "⭐",
            "scholar" to "🎓",
            "books" to "📚",
            "brain" to "🧠",
            "coder" to "💻",
            "lightning" to "⚡",
            "coffee" to "☕",
            "target" to "🎯",
            "diamond" to "💎",
            "champion" to "🏆",
            "crown" to "👑"
        )
        val lower = trimmed.lowercase()
        if (map.containsKey(lower)) {
            return map[lower]!!
        }
        return trimmed
    }

    fun getCircularBitmapFromUrl(context: Context, urlStr: String, targetSizePx: Int): Bitmap? {
        if (urlStr.isBlank()) return null
        val cacheKey = "${urlStr}_$targetSizePx"
        remoteAvatarCache.get(cacheKey)?.let { return it }

        return try {
            val url = java.net.URL(urlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "StudyTimer-Android")

            if (conn.responseCode in 200..299) {
                val inputStream = conn.inputStream
                val rawBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (rawBitmap != null) {
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
                } else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

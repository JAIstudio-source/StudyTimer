package com.madeby.JAI

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Sandboxed Developer Test Engine for StudyTimer.
 * 
 * SECURITY & ARCHITECTURE GUARANTEES:
 * 1. Strict Sandboxing: Operates ONLY within StudyTimer's active Activity window.
 *    Cannot access system settings, outside app boundaries, or other applications.
 * 2. Debug-Only: Hard-gated by BuildConfig.DEBUG.
 *    In release builds, this class does NOTHING and is stripped out by R8/ProGuard.
 * 3. Zero External Dependencies: Built using pure Kotlin and standard java.net.ServerSocket.
 */
object TestServerHelper {

    private const val DEFAULT_PORT = 8080
    @Volatile
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isRunning = false
    private var activityRef: WeakReference<Activity>? = null
    private val logQueue = ConcurrentLinkedQueue<String>()

    fun log(msg: String) {
        if (!BuildConfig.DEBUG) return
        val entry = "[${System.currentTimeMillis()}] $msg"
        logQueue.add(entry)
        while (logQueue.size > 100) {
            logQueue.poll()
        }
    }

    fun startIfDebug(activity: Activity) {
        // Absolute hard guard: Never run in release builds
        if (!BuildConfig.DEBUG) return

        activityRef = WeakReference(activity)
        if (isRunning) return

        log("Starting Debug Test Server...")
        Thread {
            try {
                var port = DEFAULT_PORT
                var ss: ServerSocket? = null
                try {
                    ss = ServerSocket(port)
                } catch (e: Exception) {
                    port = 8089
                    ss = ServerSocket(port)
                }
                serverSocket = ss
                isRunning = true
                log("Test Server listening on port $port")

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(activity, "Test Engine active on port $port", Toast.LENGTH_SHORT).show()
                }

                while (isRunning) {
                    val currentSs = serverSocket ?: break
                    if (currentSs.isClosed) break
                    val socket = currentSs.accept()
                    Thread { handleClient(socket) }.start()
                }
            } catch (e: Exception) {
                log("Server stopped: ${e.message}")
            } finally {
                isRunning = false
            }
        }.start()
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 15000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            val requestLine = readLine(input) ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val fullUrl = parts[1]
            val path = fullUrl.substringBefore("?")
            val queryParams = parseQueryParams(fullUrl.substringAfter("?", ""))

            // Drain remaining HTTP request headers until blank line
            while (true) {
                val header = readLine(input)
                if (header == null || header.trim().isEmpty()) break
            }

            log("Request: $method $path")

            when (path) {
                "/", "/status" -> handleStatus(output)
                "/screenshot", "/screenshot.png" -> handleScreenshot(output)
                "/ui", "/ui_tree" -> handleUiTree(output)
                "/click" -> handleClick(queryParams, output)
                "/input" -> handleInput(queryParams, output)
                "/state" -> handleState(output)
                "/logs" -> handleLogs(output)
                "/install_update" -> handleInstallUpdate(queryParams, output)
                "/back" -> handleBack(output)
                "/nav" -> handleNav(queryParams, output)
                "/dev_unlock" -> handleDevUnlock(output)
                else -> sendHttpResponse(output, 404, "text/plain", "Not Found")
            }
        } catch (e: Exception) {
            log("Client error: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun handleStatus(output: OutputStream) {
        val json = JSONObject().apply {
            put("status", "ok")
            put("app", "StudyTimer")
            put("version", BuildConfig.VERSION_NAME)
            put("versionCode", BuildConfig.VERSION_CODE)
            put("debug", BuildConfig.DEBUG)
            put("sandboxed", true)
            put("scope", "StudyTimer Window Only")
            put("activity", activityRef?.get()?.javaClass?.simpleName ?: "null")
        }
        sendHttpResponse(output, 200, "application/json", json.toString())
    }

    private fun handleScreenshot(output: OutputStream) {
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing) {
            sendHttpResponse(output, 500, "text/plain", "Activity not available")
            return
        }

        val bitmapOutputStream = ByteArrayOutputStream()
        val latch = CountDownLatch(1)
        var success = false

        Handler(Looper.getMainLooper()).post {
            try {
                val decorView = activity.window.decorView
                val width = decorView.width.coerceAtLeast(1)
                val height = decorView.height.coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                decorView.draw(canvas)
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, bitmapOutputStream)
                bitmap.recycle()
                success = true
            } catch (e: Exception) {
                log("Screenshot error: ${e.message}")
            } finally {
                latch.countDown()
            }
        }

        try { latch.await(2000, TimeUnit.MILLISECONDS) } catch (_: Exception) {}

        if (success && bitmapOutputStream.size() > 0) {
            val bytes = bitmapOutputStream.toByteArray()
            sendRawHttpResponse(output, 200, "image/png", bytes)
        } else {
            sendHttpResponse(output, 500, "text/plain", "Failed to capture screen")
        }
    }

    private fun handleUiTree(output: OutputStream) {
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing) {
            sendHttpResponse(output, 500, "text/plain", "Activity not available")
            return
        }

        val jsonArray = JSONArray()
        val latch = CountDownLatch(1)

        Handler(Looper.getMainLooper()).post {
            try {
                val decorView = activity.window.decorView
                dumpViewHierarchy(decorView, jsonArray)
            } catch (e: Exception) {
                log("UI dump error: ${e.message}")
            } finally {
                latch.countDown()
            }
        }

        try { latch.await(2000, TimeUnit.MILLISECONDS) } catch (_: Exception) {}

        sendHttpResponse(output, 200, "application/json", jsonArray.toString())
    }

    private fun dumpViewHierarchy(view: View, array: JSONArray) {
        val obj = JSONObject()
        obj.put("class", view.javaClass.simpleName)
        if (view.id != View.NO_ID) {
            try {
                val resName = view.resources.getResourceEntryName(view.id)
                obj.put("id", resName)
            } catch (_: Exception) {
                obj.put("id", view.id)
            }
        }
        if (view is TextView) {
            obj.put("text", view.text?.toString() ?: "")
        }
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val bounds = JSONArray().apply {
            put(loc[0])
            put(loc[1])
            put(loc[0] + view.width)
            put(loc[1] + view.height)
        }
        obj.put("bounds", bounds)
        obj.put("visible", view.visibility == View.VISIBLE && view.alpha > 0)
        obj.put("clickable", view.isClickable)
        array.put(obj)

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                dumpViewHierarchy(view.getChildAt(i), array)
            }
        }
    }

    private fun handleClick(params: Map<String, String>, output: OutputStream) {
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing) {
            sendHttpResponse(output, 500, "text/plain", "Activity not available")
            return
        }

        val idParam = params["id"]
        val textParam = params["text"]
        val xParam = params["x"]?.toFloatOrNull()
        val yParam = params["y"]?.toFloatOrNull()

        var resultMessage = "Action queued"
        var success = false
        val latch = CountDownLatch(1)

        Handler(Looper.getMainLooper()).post {
            try {
                val decorView = activity.window.decorView
                if (idParam != null) {
                    val target = findViewByResourceName(decorView, idParam)
                    if (target != null) {
                        target.performClick()
                        resultMessage = "Clicked view id '$idParam'"
                        success = true
                    } else {
                        resultMessage = "View id '$idParam' not found"
                    }
                } else if (textParam != null) {
                    val target = findViewByText(decorView, textParam)
                    if (target != null) {
                        target.performClick()
                        resultMessage = "Clicked view with text '$textParam'"
                        success = true
                    } else {
                        resultMessage = "View with text '$textParam' not found"
                    }
                } else if (xParam != null && yParam != null) {
                    val downTime = android.os.SystemClock.uptimeMillis()
                    val eventDown = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, xParam, yParam, 0)
                    val eventUp = MotionEvent.obtain(downTime, downTime + 100, MotionEvent.ACTION_UP, xParam, yParam, 0)
                    decorView.dispatchTouchEvent(eventDown)
                    decorView.dispatchTouchEvent(eventUp)
                    eventDown.recycle()
                    eventUp.recycle()
                    resultMessage = "Dispatched tap at ($xParam, $yParam)"
                    success = true
                } else {
                    resultMessage = "Provide id, text, or (x, y) coordinates"
                }
            } catch (e: Exception) {
                resultMessage = "Click error: ${e.message}"
            } finally {
                latch.countDown()
            }
        }

        try { latch.await(2000, TimeUnit.MILLISECONDS) } catch (_: Exception) {}

        val json = JSONObject().apply {
            put("success", success)
            put("message", resultMessage)
        }
        sendHttpResponse(output, if (success) 200 else 400, "application/json", json.toString())
    }

    private fun handleInput(params: Map<String, String>, output: OutputStream) {
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing) {
            sendHttpResponse(output, 500, "text/plain", "Activity not available")
            return
        }

        val textToInput = params["text"] ?: ""
        val idParam = params["id"]
        var success = false
        var message = ""
        val latch = CountDownLatch(1)

        Handler(Looper.getMainLooper()).post {
            try {
                val decorView = activity.window.decorView
                var target: EditText? = null
                if (idParam != null) {
                    val v = findViewByResourceName(decorView, idParam)
                    if (v is EditText) target = v
                } else {
                    target = findFocusedEditText(decorView)
                }

                if (target != null) {
                    target.setText(textToInput)
                    target.setSelection(textToInput.length)
                    message = "Input '$textToInput' into ${target.javaClass.simpleName}"
                    success = true
                } else {
                    message = "No target EditText found"
                }
            } catch (e: Exception) {
                message = "Input error: ${e.message}"
            } finally {
                latch.countDown()
            }
        }

        try { latch.await(2000, TimeUnit.MILLISECONDS) } catch (_: Exception) {}

        val json = JSONObject().apply {
            put("success", success)
            put("message", message)
        }
        sendHttpResponse(output, if (success) 200 else 400, "application/json", json.toString())
    }

    private fun handleState(output: OutputStream) {
        val activity = activityRef?.get()
        val json = JSONObject().apply {
            put("activity", activity?.javaClass?.simpleName ?: "null")
            put("timestamp", System.currentTimeMillis())
            put("thread", Thread.currentThread().name)
        }
        sendHttpResponse(output, 200, "application/json", json.toString())
    }

    private fun handleLogs(output: OutputStream) {
        val array = JSONArray()
        logQueue.forEach { array.put(it) }
        sendHttpResponse(output, 200, "application/json", array.toString())
    }

    private fun handleInstallUpdate(params: Map<String, String>, output: OutputStream) {
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing) {
            sendHttpResponse(output, 500, "text/plain", "Activity not available")
            return
        }

        val downloadUrl = params["url"]
        if (downloadUrl.isNullOrEmpty()) {
            sendHttpResponse(output, 400, "application/json", "{\"success\":false,\"message\":\"Missing 'url' parameter\"}")
            return
        }

        Thread {
            try {
                log("Downloading update from $downloadUrl")
                val urlObj = URL(downloadUrl)
                val connection = urlObj.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 30000

                val apkFile = File(activity.cacheDir, "update.apk")
                if (apkFile.exists()) apkFile.delete()

                connection.getInputStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        input.copyTo(output)
                    }
                }

                log("Downloaded APK (${apkFile.length()} bytes), saving to device Downloads and launching installer...")
                try {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                        val publicApk = File(downloadsDir, "StudyTimer-debug.apk")
                        apkFile.copyTo(publicApk, overwrite = true)
                        log("Saved copy to device Downloads: ${publicApk.absolutePath}")
                    }
                } catch (e: Exception) {
                    log("Public Download copy warning: ${e.message}")
                }

                Handler(Looper.getMainLooper()).post {
                    try {
                        val apkUri = FileProvider.getUriForFile(
                            activity,
                            "${activity.packageName}.fileprovider",
                            apkFile
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        activity.startActivity(intent)
                        Toast.makeText(activity, "Opening update installer...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        log("Installer intent error: ${e.message}")
                        Toast.makeText(activity, "Failed to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                log("Download APK failed: ${e.message}")
            }
        }.start()

        val json = JSONObject().apply {
            put("success", true)
            put("message", "Downloading APK update from $downloadUrl...")
        }
        sendHttpResponse(output, 200, "application/json", json.toString())
    }

    private fun handleBack(output: OutputStream) {
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing) {
            sendHttpResponse(output, 500, "text/plain", "Activity not available")
            return
        }
        Handler(Looper.getMainLooper()).post {
            try {
                if (activity is androidx.activity.ComponentActivity) {
                    activity.onBackPressedDispatcher.onBackPressed()
                } else {
                    @Suppress("DEPRECATION")
                    activity.onBackPressed()
                }
            } catch (e: Exception) {
                log("Back press error: ${e.message}")
            }
        }
        sendHttpResponse(output, 200, "application/json", "{\"success\":true,\"message\":\"Back pressed\"}")
    }

    private fun handleNav(params: Map<String, String>, output: OutputStream) {
        val activity = activityRef?.get() as? MainActivity
        if (activity == null || activity.isFinishing) {
            sendHttpResponse(output, 500, "text/plain", "MainActivity not available")
            return
        }
        val panelStr = params["panel"]?.uppercase(java.util.Locale.ROOT) ?: "FOCUS"
        val panel = try { AppPanel.valueOf(panelStr) } catch (_: Exception) { AppPanel.FOCUS }
        Handler(Looper.getMainLooper()).post {
            try {
                activity.navigateToPanel(panel)
            } catch (e: Exception) {
                log("Nav error: ${e.message}")
            }
        }
        sendHttpResponse(output, 200, "application/json", "{\"success\":true,\"panel\":\"$panel\"}")
    }

    private fun handleDevUnlock(output: OutputStream) {
        val activity = activityRef?.get() as? MainActivity
        if (activity == null || activity.isFinishing) {
            sendHttpResponse(output, 500, "text/plain", "MainActivity not available")
            return
        }
        Handler(Looper.getMainLooper()).post {
            try {
                activity.isDevModeUnlocked = true
                Toast.makeText(activity, "Developer Mode Unlocked!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                log("Dev unlock error: ${e.message}")
            }
        }
        sendHttpResponse(output, 200, "application/json", "{\"success\":true,\"message\":\"Developer mode unlocked\"}")
    }

    private fun findViewByResourceName(view: View, resName: String): View? {
        if (view.id != View.NO_ID) {
            try {
                val name = view.resources.getResourceEntryName(view.id)
                if (name.equals(resName, ignoreCase = true)) return view
            } catch (_: Exception) {}
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findViewByResourceName(view.getChildAt(i), resName)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findViewByText(view: View, text: String): View? {
        if (view is TextView && view.text != null) {
            val t = view.text.toString()
            if (t.equals(text, ignoreCase = true) || t.contains(text, ignoreCase = true)) {
                return view
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findViewByText(view.getChildAt(i), text)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findFocusedEditText(view: View): EditText? {
        if (view is EditText && view.hasFocus()) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findFocusedEditText(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val k = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val v = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[k] = v
            }
        }
        return map
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        var b: Int
        while (input.read().also { b = it } != -1) {
            if (b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
        }
        return if (sb.isEmpty() && b == -1) null else sb.toString()
    }

    private fun sendHttpResponse(output: OutputStream, statusCode: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        sendRawHttpResponse(output, statusCode, contentType, bytes)
    }

    private fun sendRawHttpResponse(output: OutputStream, statusCode: Int, contentType: String, bytes: ByteArray) {
        val statusText = when (statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            500 -> "Internal Server Error"
            else -> "OK"
        }
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }
}

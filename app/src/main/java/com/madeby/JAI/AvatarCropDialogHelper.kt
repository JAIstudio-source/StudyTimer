package com.madeby.JAI

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Interactive Profile Picture Cropper with fixed 1:1 Aspect Ratio.
 * Allows users to pinch-to-zoom, pan/drag, rotate, and frame their avatar precisely.
 */
class AvatarCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rawBitmap: Bitmap? = null
    private var displayBitmap: Bitmap? = null
    private val matrixTransform = Matrix()
    private val inverseMatrix = Matrix()

    private val cropRect = RectF()
    private var primaryColor: Int = Color.parseColor("#A78BFA")
    private var secondaryColor: Int = Color.parseColor("#38BDF8")

    // Gesture tracking
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isDragging = false

    private var minScale = 1f
    private var maxScale = 6f
    private var currentScale = 1f

    private val maskPath = Path()
    private val circlePath = Path()

    // Paints
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E60A0A10") // Deep AMOLED translucent backdrop
        style = Paint.Style.FILL
    }

    private val circleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2.5f)
        color = Color.WHITE
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        color = Color.argb(70, 255, 255, 255)
    }

    private val cornerAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
        strokeCap = Paint.Cap.ROUND
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val targetScale = currentScale * scaleFactor

            if (targetScale in minScale..maxScale) {
                currentScale = targetScale
                matrixTransform.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                clampMatrixBounds()
                invalidate()
            }
            return true
        }
    })

    fun setPrimaryColor(color: Int, secondary: Int) {
        this.primaryColor = color
        this.secondaryColor = secondary
        circleBorderPaint.color = Color.WHITE
        cornerAccentPaint.color = color
        invalidate()
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        this.rawBitmap = bitmap
        this.displayBitmap = bitmap
        if (width > 0 && height > 0) {
            setupInitialImageBounds()
        }
        invalidate()
    }

    fun rotate90() {
        val current = displayBitmap ?: return
        val rotMatrix = Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(current, 0, 0, current.width, current.height, rotMatrix, true)
        this.displayBitmap = rotated
        setupInitialImageBounds()
        invalidate()
    }

    fun resetTransform() {
        setupInitialImageBounds()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        // 1:1 Aspect Ratio Crop Viewport
        val cropDimension = (Math.min(w, h) * 0.82f).coerceAtLeast(dpToPx(180f))
        val cx = w / 2f
        val cy = h / 2f

        cropRect.set(
            cx - cropDimension / 2f,
            cy - cropDimension / 2f,
            cx + cropDimension / 2f,
            cy + cropDimension / 2f
        )

        setupInitialImageBounds()
    }

    private fun setupInitialImageBounds() {
        val bmp = displayBitmap ?: return
        if (cropRect.width() <= 0f || cropRect.height() <= 0f) return

        matrixTransform.reset()

        // Minimum scale must fully cover the 1:1 crop rectangle without gaps
        val scaleX = cropRect.width() / bmp.width.toFloat()
        val scaleY = cropRect.height() / bmp.height.toFloat()
        minScale = Math.max(scaleX, scaleY)
        maxScale = minScale * 6f
        currentScale = minScale

        matrixTransform.postScale(currentScale, currentScale)

        // Center bitmap within cropRect
        val scaledWidth = bmp.width * currentScale
        val scaledHeight = bmp.height * currentScale
        val dx = cropRect.left + (cropRect.width() - scaledWidth) / 2f
        val dy = cropRect.top + (cropRect.height() - scaledHeight) / 2f

        matrixTransform.postTranslate(dx, dy)
        clampMatrixBounds()
    }

    private fun clampMatrixBounds() {
        val bmp = displayBitmap ?: return
        if (cropRect.isEmpty) return

        val currentValues = FloatArray(9)
        matrixTransform.getValues(currentValues)

        val transX = currentValues[Matrix.MTRANS_X]
        val transY = currentValues[Matrix.MTRANS_Y]
        val scaleX = currentValues[Matrix.MSCALE_X]
        val scaleY = currentValues[Matrix.MSCALE_Y]

        val bmpWidth = bmp.width * scaleX
        val bmpHeight = bmp.height * scaleY

        var adjustedTransX = transX
        var adjustedTransY = transY

        // Ensure image always covers cropRect left/right
        if (bmpWidth >= cropRect.width()) {
            if (adjustedTransX > cropRect.left) {
                adjustedTransX = cropRect.left
            } else if (adjustedTransX + bmpWidth < cropRect.right) {
                adjustedTransX = cropRect.right - bmpWidth
            }
        } else {
            adjustedTransX = cropRect.left + (cropRect.width() - bmpWidth) / 2f
        }

        // Ensure image always covers cropRect top/bottom
        if (bmpHeight >= cropRect.height()) {
            if (adjustedTransY > cropRect.top) {
                adjustedTransY = cropRect.top
            } else if (adjustedTransY + bmpHeight < cropRect.bottom) {
                adjustedTransY = cropRect.bottom - bmpHeight
            }
        } else {
            adjustedTransY = cropRect.top + (cropRect.height() - bmpHeight) / 2f
        }

        currentValues[Matrix.MTRANS_X] = adjustedTransX
        currentValues[Matrix.MTRANS_Y] = adjustedTransY
        matrixTransform.setValues(currentValues)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY

                        matrixTransform.postTranslate(dx, dy)
                        clampMatrixBounds()
                        invalidate()

                        lastTouchX = x
                        lastTouchY = y
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bmp = displayBitmap
        if (bmp != null && !bmp.isRecycled) {
            canvas.drawBitmap(bmp, matrixTransform, bitmapPaint)
        }

        if (cropRect.isEmpty) return

        // 1. Draw outer darkened mask using Path clipping / Fill
        maskPath.reset()
        maskPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        
        // Circular Aperture cutout
        circlePath.reset()
        val radius = cropRect.width() / 2f
        circlePath.addCircle(cropRect.centerX(), cropRect.centerY(), radius, Path.Direction.CCW)
        maskPath.addPath(circlePath)

        canvas.drawPath(maskPath, maskPaint)

        // 2. Draw Rule-of-Thirds Grid inside circular aperture
        val thirdW = cropRect.width() / 3f
        val thirdH = cropRect.height() / 3f

        canvas.save()
        canvas.clipPath(circlePath)
        
        // Vertical grid lines
        canvas.drawLine(cropRect.left + thirdW, cropRect.top, cropRect.left + thirdW, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left + thirdW * 2f, cropRect.top, cropRect.left + thirdW * 2f, cropRect.bottom, gridPaint)

        // Horizontal grid lines
        canvas.drawLine(cropRect.left, cropRect.top + thirdH, cropRect.right, cropRect.top + thirdH, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + thirdH * 2f, cropRect.right, cropRect.top + thirdH * 2f, gridPaint)

        canvas.restore()

        // 3. Draw Circle Boundary Guide
        canvas.drawCircle(cropRect.centerX(), cropRect.centerY(), radius, circleBorderPaint)

        // 4. Draw Modern Corner Tick Marks on Crop Box
        val tickLen = dpToPx(16f)
        // Top-Left
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + tickLen, cropRect.top, cornerAccentPaint)
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + tickLen, cornerAccentPaint)
        // Top-Right
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right - tickLen, cropRect.top, cornerAccentPaint)
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + tickLen, cornerAccentPaint)
        // Bottom-Left
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + tickLen, cropRect.bottom, cornerAccentPaint)
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left, cropRect.bottom - tickLen, cornerAccentPaint)
        // Bottom-Right
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right - tickLen, cropRect.bottom, cornerAccentPaint)
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right, cropRect.bottom - tickLen, cornerAccentPaint)
    }

    /**
     * Crops and returns a 1:1 aspect ratio square Bitmap at target output dimension (e.g. 512x512).
     */
    fun cropCroppedBitmap(outputDimension: Int = 512): Bitmap? {
        val bmp = displayBitmap ?: return null
        if (cropRect.isEmpty || bmp.isRecycled) return null

        try {
            matrixTransform.invert(inverseMatrix)
            val mappedCropRect = RectF()
            inverseMatrix.mapRect(mappedCropRect, cropRect)

            val srcLeft = mappedCropRect.left.coerceIn(0f, bmp.width.toFloat()).toInt()
            val srcTop = mappedCropRect.top.coerceIn(0f, bmp.height.toFloat()).toInt()
            val srcRight = mappedCropRect.right.coerceIn(0f, bmp.width.toFloat()).toInt()
            val srcBottom = mappedCropRect.bottom.coerceIn(0f, bmp.height.toFloat()).toInt()

            val srcWidth = (srcRight - srcLeft).coerceAtLeast(1)
            val srcHeight = (srcBottom - srcTop).coerceAtLeast(1)

            val cropped = Bitmap.createBitmap(bmp, srcLeft, srcTop, srcWidth, srcHeight)
            return if (srcWidth != outputDimension || srcHeight != outputDimension) {
                Bitmap.createScaledBitmap(cropped, outputDimension, outputDimension, true)
            } else {
                cropped
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}

/**
 * Modern AMOLED Themed Dialog Helper to display the Profile Picture Crop System.
 */
object AvatarCropDialogHelper {

    fun showCropDialog(
        activity: Activity,
        imageUri: Uri,
        themeCoordinator: ThemeCoordinator,
        onCropConfirmed: (Bitmap) -> Unit
    ) {
        val dialog = Dialog(activity, R.style.AmoledPickerDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Root Container
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF09090D.toInt())
            setPadding(dp(activity, 18), dp(activity, 20), dp(activity, 18), dp(activity, 22))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Header Section
        val headerLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(activity, 12))
        }

        val titleTv = TextView(activity).apply {
            text = "Crop Profile Picture"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val subtitleTv = TextView(activity).apply {
            text = "Pinch to zoom & drag to position your avatar"
            textSize = 12.5f
            setTextColor(0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(activity, 4), 0, 0)
        }

        headerLayout.addView(titleTv)
        headerLayout.addView(subtitleTv)
        root.addView(headerLayout)

        // Center Crop View Container
        val cropViewContainer = FrameLayout(activity).apply {
            val size = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                size
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(activity, 4)
                bottomMargin = dp(activity, 12)
            }
            background = GradientDrawable().apply {
                setColor(0xFF13131A.toInt())
                cornerRadius = dp(activity, 16).toFloat()
            }
            clipToOutline = true
        }

        val cropView = AvatarCropView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPrimaryColor(themeCoordinator.primaryColor, themeCoordinator.secondaryColor)
        }
        cropViewContainer.addView(cropView)
        root.addView(cropViewContainer)

        // Controls Bar (Rotate & Reset)
        val controlsBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(activity, 16))
        }

        val rotateBtn = createPillButton(activity, "⟳ Rotate 90°", 0xFF1E1E28.toInt(), Color.WHITE) {
            cropView.rotate90()
        }

        val resetBtn = createPillButton(activity, "Reset View", 0xFF1E1E28.toInt(), 0xFFCBD5E1.toInt()) {
            cropView.resetTransform()
        }

        controlsBar.addView(rotateBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 36)).apply {
            rightMargin = dp(activity, 10)
        })
        controlsBar.addView(resetBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 36)))
        root.addView(controlsBar)

        // Bottom Action Buttons (Cancel & Set Avatar)
        val actionsLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)
        }

        val cancelBtn = Button(activity).apply {
            text = "Cancel"
            setTextColor(0xFFCBD5E1.toInt())
            textSize = 14f
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(0xFF1A1A24.toInt())
                cornerRadius = dp(activity, 12).toFloat()
            }
            setOnClickListener {
                dialog.dismiss()
            }
        }

        val confirmBtn = Button(activity).apply {
            text = "Set Avatar"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(themeCoordinator.primaryColor)
                cornerRadius = dp(activity, 12).toFloat()
            }
            setOnClickListener {
                val cropped = cropView.cropCroppedBitmap(512)
                if (cropped != null) {
                    dialog.dismiss()
                    onCropConfirmed(cropped)
                } else {
                    Toast.makeText(activity, "Failed to crop image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val cancelParams = LinearLayout.LayoutParams(0, dp(activity, 46), 1f).apply {
            rightMargin = dp(activity, 10)
        }
        val confirmParams = LinearLayout.LayoutParams(0, dp(activity, 46), 1.3f)

        actionsLayout.addView(cancelBtn, cancelParams)
        actionsLayout.addView(confirmBtn, confirmParams)
        root.addView(actionsLayout)

        dialog.setContentView(root)
        dialog.window?.let { win ->
            win.setBackgroundDrawableResource(android.R.color.transparent)
            win.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.94f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Asynchronously load & safely downsample the bitmap with EXIF orientation
        CoroutineScope(Dispatchers.IO).launch {
            val loadedBitmap = loadBitmapFromUriSafely(activity, imageUri)
            withContext(Dispatchers.Main) {
                if (loadedBitmap != null && !activity.isFinishing && !activity.isDestroyed) {
                    cropView.setImageBitmap(loadedBitmap)
                    dialog.show()
                } else {
                    Toast.makeText(activity, "Could not open selected image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createPillButton(
        context: Context,
        text: String,
        bgColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            this.text = text
            this.textSize = 12.5f
            this.setTextColor(textColor)
            this.gravity = Gravity.CENTER
            this.setPadding(dp(context, 14), 0, dp(context, 14), 0)
            this.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(context, 18).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun loadBitmapFromUriSafely(context: Context, uri: Uri, maxTargetDim: Int = 1600): Bitmap? {
        return try {
            // 1. Read EXIF Orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            } catch (_: Exception) {}

            // 2. Decode Bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            while (origWidth / (sampleSize * 2) >= maxTargetDim || origHeight / (sampleSize * 2) >= maxTargetDim) {
                sampleSize *= 2
            }

            // 3. Decode Sampled Bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 4. Apply EXIF orientation
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            if (!matrix.isIdentity) {
                Bitmap.createBitmap(
                    sampledBitmap, 0, 0,
                    sampledBitmap.width, sampledBitmap.height,
                    matrix, true
                )
            } else {
                sampledBitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}

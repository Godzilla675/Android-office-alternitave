package com.officesuite.app.ui.pdf

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * Custom ImageView that supports pinch-to-zoom and pan gestures.
 * Used for viewing PDF pages with zoom capability.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    
    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    
    private var minScale = 1f
    private var maxScale = 4f
    private var currentScale = 1f
    
    private val matrixValues = FloatArray(9)
    
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = matrix
        
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    val dx = event.x - start.x
                    val dy = event.y - start.y
                    matrix.postTranslate(dx, dy)
                    limitTranslation()
                } else if (mode == ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        val newScale = currentScale * scale
                        
                        if (newScale in minScale..maxScale) {
                            matrix.postScale(scale, scale, mid.x, mid.y)
                            currentScale = newScale
                        }
                        limitTranslation()
                    }
                }
            }
        }

        imageMatrix = matrix
        return true
    }

    private fun spacing(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(x * x + y * y)
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        if (event.pointerCount < 2) return
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun limitTranslation() {
        matrix.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val scaleX = matrixValues[Matrix.MSCALE_X]
        val scaleY = matrixValues[Matrix.MSCALE_Y]

        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth * scaleX
        val imageHeight = drawable.intrinsicHeight * scaleY
        
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        var dx = 0f
        var dy = 0f

        // Limit horizontal movement
        if (imageWidth <= viewWidth) {
            dx = (viewWidth - imageWidth) / 2 - transX
        } else {
            if (transX > 0) dx = -transX
            else if (transX + imageWidth < viewWidth) dx = viewWidth - transX - imageWidth
        }

        // Limit vertical movement
        if (imageHeight <= viewHeight) {
            dy = (viewHeight - imageHeight) / 2 - transY
        } else {
            if (transY > 0) dy = -transY
            else if (transY + imageHeight < viewHeight) dy = viewHeight - transY - imageHeight
        }

        if (dx != 0f || dy != 0f) {
            matrix.postTranslate(dx, dy)
        }
    }

    fun resetZoom() {
        matrix.reset()
        currentScale = 1f
        fitImageToView()
        imageMatrix = matrix
    }

    private fun fitImageToView() {
        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        if (imageWidth == 0f || imageHeight == 0f || viewWidth == 0f || viewHeight == 0f) return
        
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = minOf(scaleX, scaleY)
        
        val dx = (viewWidth - imageWidth * scale) / 2
        val dy = (viewHeight - imageHeight * scale) / 2
        
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        minScale = scale
        currentScale = scale
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitImageToView()
        imageMatrix = matrix
    }

    override fun setImageBitmap(bm: android.graphics.Bitmap?) {
        super.setImageBitmap(bm)
        post {
            fitImageToView()
            imageMatrix = matrix
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = currentScale * scaleFactor
            
            if (newScale in minScale..maxScale) {
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                currentScale = newScale
                limitTranslation()
                imageMatrix = matrix
            }
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (currentScale > minScale * 1.5f) {
                // Zoom out
                resetZoom()
            } else {
                // Zoom in to 2x
                val targetScale = minScale * 2f
                val scaleFactor = targetScale / currentScale
                matrix.postScale(scaleFactor, scaleFactor, e.x, e.y)
                currentScale = targetScale
                limitTranslation()
                imageMatrix = matrix
            }
            return true
        }
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}

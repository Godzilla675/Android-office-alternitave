package com.officesuite.app.ui.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Stack

/**
 * Custom view for drawing annotations and shapes on documents.
 * Supports freehand drawing, highlighter, shapes (rectangle, circle, arrow, line),
 * and text annotations with full undo/redo support.
 */
class AnnotationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Tool {
        NONE, PEN, HIGHLIGHTER, ERASER, RECTANGLE, CIRCLE, ARROW, LINE, TEXT
    }

    data class Annotation(
        val path: Path? = null,
        val shape: RectF? = null,
        val shapeType: Tool = Tool.NONE,
        val paint: Paint,
        val text: String? = null,
        val textPosition: PointF? = null,
        val startPoint: PointF? = null,
        val endPoint: PointF? = null
    )

    private val annotations = mutableListOf<Annotation>()
    private val redoStack = Stack<Annotation>()
    
    private var currentPath: Path? = null
    private var currentPaint = createDefaultPaint()
    private var currentTool = Tool.NONE
    
    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var isDrawing = false

    // Configurable properties
    var strokeColor = Color.BLACK
        set(value) {
            field = value
            updatePaint()
        }
    
    var strokeWidth = 5f
        set(value) {
            field = value
            updatePaint()
        }
    
    var highlighterAlpha = 100
    
    var textSize = 40f
    var textTypeface: Typeface = Typeface.DEFAULT

    // Listener for annotation changes
    var onAnnotationChangeListener: (() -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun createDefaultPaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            isDither = true
            color = strokeColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = this@AnnotationView.strokeWidth
        }
    }

    private fun updatePaint() {
        currentPaint = when (currentTool) {
            Tool.HIGHLIGHTER -> createHighlighterPaint()
            Tool.TEXT -> createTextPaint()
            else -> createDefaultPaint()
        }
    }

    private fun createHighlighterPaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            color = strokeColor
            alpha = highlighterAlpha
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.SQUARE
            strokeWidth = this@AnnotationView.strokeWidth * 3
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
    }

    private fun createTextPaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            color = strokeColor
            textSize = this@AnnotationView.textSize
            typeface = textTypeface
            style = Paint.Style.FILL
        }
    }

    fun setTool(tool: Tool) {
        currentTool = tool
        updatePaint()
    }

    fun getTool(): Tool = currentTool

    fun undo() {
        if (annotations.isNotEmpty()) {
            val removed = annotations.removeAt(annotations.lastIndex)
            redoStack.push(removed)
            invalidate()
            onAnnotationChangeListener?.invoke()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            annotations.add(redoStack.pop())
            invalidate()
            onAnnotationChangeListener?.invoke()
        }
    }

    fun canUndo(): Boolean = annotations.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        annotations.clear()
        redoStack.clear()
        invalidate()
        onAnnotationChangeListener?.invoke()
    }

    fun addTextAnnotation(text: String, x: Float, y: Float) {
        val paint = createTextPaint()
        annotations.add(Annotation(
            paint = Paint(paint),
            text = text,
            textPosition = PointF(x, y),
            shapeType = Tool.TEXT
        ))
        redoStack.clear()
        invalidate()
        onAnnotationChangeListener?.invoke()
    }

    fun getAnnotations(): List<Annotation> = annotations.toList()

    fun setAnnotations(newAnnotations: List<Annotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
        redoStack.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all completed annotations
        for (annotation in annotations) {
            when {
                annotation.path != null -> {
                    canvas.drawPath(annotation.path, annotation.paint)
                }
                annotation.text != null && annotation.textPosition != null -> {
                    canvas.drawText(annotation.text, annotation.textPosition.x, annotation.textPosition.y, annotation.paint)
                }
                annotation.shape != null -> {
                    drawShape(canvas, annotation.shapeType, annotation.shape, annotation.paint)
                }
                annotation.startPoint != null && annotation.endPoint != null -> {
                    drawLineOrArrow(canvas, annotation.shapeType, annotation.startPoint, annotation.endPoint, annotation.paint)
                }
            }
        }

        // Draw current drawing operation
        if (isDrawing) {
            when (currentTool) {
                Tool.PEN, Tool.HIGHLIGHTER -> {
                    currentPath?.let { canvas.drawPath(it, currentPaint) }
                }
                Tool.RECTANGLE -> {
                    val rect = createRectFromPoints(startX, startY, currentX, currentY)
                    drawShape(canvas, Tool.RECTANGLE, rect, currentPaint)
                }
                Tool.CIRCLE -> {
                    val rect = createRectFromPoints(startX, startY, currentX, currentY)
                    drawShape(canvas, Tool.CIRCLE, rect, currentPaint)
                }
                Tool.LINE -> {
                    canvas.drawLine(startX, startY, currentX, currentY, currentPaint)
                }
                Tool.ARROW -> {
                    drawArrow(canvas, startX, startY, currentX, currentY, currentPaint)
                }
                else -> {}
            }
        }
    }

    private fun createRectFromPoints(x1: Float, y1: Float, x2: Float, y2: Float): RectF {
        return RectF(
            minOf(x1, x2),
            minOf(y1, y2),
            maxOf(x1, x2),
            maxOf(y1, y2)
        )
    }

    private fun drawShape(canvas: Canvas, type: Tool, rect: RectF, paint: Paint) {
        when (type) {
            Tool.RECTANGLE -> canvas.drawRect(rect, paint)
            Tool.CIRCLE -> canvas.drawOval(rect, paint)
            else -> {}
        }
    }

    private fun drawLineOrArrow(canvas: Canvas, type: Tool, start: PointF, end: PointF, paint: Paint) {
        when (type) {
            Tool.LINE -> canvas.drawLine(start.x, start.y, end.x, end.y, paint)
            Tool.ARROW -> drawArrow(canvas, start.x, start.y, end.x, end.y, paint)
            else -> {}
        }
    }

    private fun drawArrow(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float, paint: Paint) {
        // Draw the line
        canvas.drawLine(startX, startY, endX, endY, paint)
        
        // Calculate arrowhead
        val angle = Math.atan2((endY - startY).toDouble(), (endX - startX).toDouble())
        val arrowLength = paint.strokeWidth * 4
        val arrowAngle = Math.toRadians(30.0)
        
        val x1 = endX - arrowLength * Math.cos(angle - arrowAngle)
        val y1 = endY - arrowLength * Math.sin(angle - arrowAngle)
        val x2 = endX - arrowLength * Math.cos(angle + arrowAngle)
        val y2 = endY - arrowLength * Math.sin(angle + arrowAngle)
        
        val arrowPath = Path().apply {
            moveTo(endX, endY)
            lineTo(x1.toFloat(), y1.toFloat())
            moveTo(endX, endY)
            lineTo(x2.toFloat(), y2.toFloat())
        }
        canvas.drawPath(arrowPath, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (currentTool == Tool.NONE) return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y
                currentX = x
                currentY = y
                isDrawing = true

                when (currentTool) {
                    Tool.PEN, Tool.HIGHLIGHTER -> {
                        currentPath = Path().apply {
                            moveTo(x, y)
                        }
                    }
                    Tool.ERASER -> {
                        eraseAt(x, y)
                    }
                    else -> {}
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentX = x
                currentY = y

                when (currentTool) {
                    Tool.PEN, Tool.HIGHLIGHTER -> {
                        currentPath?.quadTo(startX, startY, (x + startX) / 2, (y + startY) / 2)
                        startX = x
                        startY = y
                    }
                    Tool.ERASER -> {
                        eraseAt(x, y)
                    }
                    else -> {}
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                isDrawing = false
                
                when (currentTool) {
                    Tool.PEN, Tool.HIGHLIGHTER -> {
                        currentPath?.let { path ->
                            annotations.add(Annotation(
                                path = Path(path),
                                paint = Paint(currentPaint),
                                shapeType = currentTool
                            ))
                        }
                        currentPath = null
                    }
                    Tool.RECTANGLE, Tool.CIRCLE -> {
                        val rect = createRectFromPoints(startX, startY, x, y)
                        if (rect.width() > 5 && rect.height() > 5) {
                            annotations.add(Annotation(
                                shape = RectF(rect),
                                shapeType = currentTool,
                                paint = Paint(currentPaint)
                            ))
                        }
                    }
                    Tool.LINE, Tool.ARROW -> {
                        val distance = Math.sqrt(
                            Math.pow((x - startX).toDouble(), 2.0) +
                            Math.pow((y - startY).toDouble(), 2.0)
                        )
                        if (distance > 10) {
                            annotations.add(Annotation(
                                startPoint = PointF(startX, startY),
                                endPoint = PointF(x, y),
                                shapeType = currentTool,
                                paint = Paint(currentPaint)
                            ))
                        }
                    }
                    else -> {}
                }
                
                redoStack.clear()
                invalidate()
                onAnnotationChangeListener?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun eraseAt(x: Float, y: Float) {
        val eraserRadius = strokeWidth * 3
        val iterator = annotations.iterator()
        var changed = false
        
        while (iterator.hasNext()) {
            val annotation = iterator.next()
            if (intersectsWithEraser(annotation, x, y, eraserRadius)) {
                iterator.remove()
                changed = true
            }
        }
        
        if (changed) {
            redoStack.clear()
            onAnnotationChangeListener?.invoke()
        }
    }

    private fun intersectsWithEraser(annotation: Annotation, x: Float, y: Float, radius: Float): Boolean {
        val eraserBounds = RectF(x - radius, y - radius, x + radius, y + radius)
        
        return when {
            annotation.path != null -> {
                val pathBounds = RectF()
                annotation.path.computeBounds(pathBounds, true)
                RectF.intersects(eraserBounds, pathBounds)
            }
            annotation.shape != null -> {
                RectF.intersects(eraserBounds, annotation.shape)
            }
            annotation.textPosition != null -> {
                eraserBounds.contains(annotation.textPosition.x, annotation.textPosition.y)
            }
            annotation.startPoint != null && annotation.endPoint != null -> {
                val lineBounds = RectF(
                    minOf(annotation.startPoint.x, annotation.endPoint.x),
                    minOf(annotation.startPoint.y, annotation.endPoint.y),
                    maxOf(annotation.startPoint.x, annotation.endPoint.x),
                    maxOf(annotation.startPoint.y, annotation.endPoint.y)
                )
                RectF.intersects(eraserBounds, lineBounds)
            }
            else -> false
        }
    }

    /**
     * Export annotations to a bitmap that can be overlaid on the document
     */
    fun exportAnnotations(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        for (annotation in annotations) {
            when {
                annotation.path != null -> canvas.drawPath(annotation.path, annotation.paint)
                annotation.text != null && annotation.textPosition != null -> {
                    canvas.drawText(annotation.text, annotation.textPosition.x, annotation.textPosition.y, annotation.paint)
                }
                annotation.shape != null -> drawShape(canvas, annotation.shapeType, annotation.shape, annotation.paint)
                annotation.startPoint != null && annotation.endPoint != null -> {
                    drawLineOrArrow(canvas, annotation.shapeType, annotation.startPoint, annotation.endPoint, annotation.paint)
                }
            }
        }
        
        return bitmap
    }
}

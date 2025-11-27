package com.officesuite.app.ui.editor

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.officesuite.app.R

/**
 * Reusable editor toolbar with annotation and formatting tools.
 */
class EditorToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // Tool buttons
    private lateinit var btnPen: ImageButton
    private lateinit var btnHighlighter: ImageButton
    private lateinit var btnEraser: ImageButton
    private lateinit var btnText: ImageButton
    private lateinit var btnRectangle: ImageButton
    private lateinit var btnCircle: ImageButton
    private lateinit var btnArrow: ImageButton
    private lateinit var btnLine: ImageButton
    private lateinit var btnColor: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnStrokeWidth: ImageButton

    // State
    private var currentTool = AnnotationView.Tool.NONE
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 5f

    // Listeners
    var onToolSelectedListener: ((AnnotationView.Tool) -> Unit)? = null
    var onColorSelectedListener: ((Int) -> Unit)? = null
    var onStrokeWidthChangedListener: ((Float) -> Unit)? = null
    var onUndoClickListener: (() -> Unit)? = null
    var onRedoClickListener: (() -> Unit)? = null
    var onTextAnnotationRequestListener: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        setupViews()
    }

    private fun setupViews() {
        // Create toolbar buttons
        btnPen = createToolButton(R.drawable.ic_pen) {
            selectTool(AnnotationView.Tool.PEN)
        }
        
        btnHighlighter = createToolButton(R.drawable.ic_highlight) {
            selectTool(AnnotationView.Tool.HIGHLIGHTER)
        }
        
        btnEraser = createToolButton(R.drawable.ic_eraser) {
            selectTool(AnnotationView.Tool.ERASER)
        }
        
        btnText = createToolButton(R.drawable.ic_text_add) {
            selectTool(AnnotationView.Tool.TEXT)
            onTextAnnotationRequestListener?.invoke()
        }
        
        btnRectangle = createToolButton(R.drawable.ic_rectangle) {
            selectTool(AnnotationView.Tool.RECTANGLE)
        }
        
        btnCircle = createToolButton(R.drawable.ic_circle) {
            selectTool(AnnotationView.Tool.CIRCLE)
        }
        
        btnArrow = createToolButton(R.drawable.ic_arrow) {
            selectTool(AnnotationView.Tool.ARROW)
        }
        
        btnLine = createToolButton(R.drawable.ic_line) {
            selectTool(AnnotationView.Tool.LINE)
        }

        // Add separator
        addView(createSeparator())

        btnColor = createToolButton(R.drawable.ic_color_palette) {
            // Color picker will be handled by fragment
        }
        btnColor.setColorFilter(currentColor)

        btnStrokeWidth = createToolButton(R.drawable.ic_pen) {
            showStrokeWidthDialog()
        }

        // Add separator
        addView(createSeparator())

        btnUndo = createToolButton(R.drawable.ic_undo) {
            onUndoClickListener?.invoke()
        }
        
        btnRedo = createToolButton(R.drawable.ic_redo) {
            onRedoClickListener?.invoke()
        }

        // Add all buttons
        addView(btnPen)
        addView(btnHighlighter)
        addView(btnEraser)
        addView(btnText)
        addView(btnRectangle)
        addView(btnCircle)
        addView(btnArrow)
        addView(btnLine)
        addView(btnColor)
        addView(btnStrokeWidth)
        addView(btnUndo)
        addView(btnRedo)
    }

    private fun createToolButton(iconRes: Int, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            setImageResource(iconRes)
            setBackgroundResource(android.R.color.transparent)
            setPadding(16, 16, 16, 16)
            setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 4
                marginEnd = 4
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createSeparator(): View {
        return View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray))
            layoutParams = LayoutParams(2, LayoutParams.MATCH_PARENT).apply {
                marginStart = 8
                marginEnd = 8
            }
        }
    }

    fun selectTool(tool: AnnotationView.Tool) {
        currentTool = tool
        updateToolSelection()
        onToolSelectedListener?.invoke(tool)
    }

    private fun updateToolSelection() {
        val selectedColor = ContextCompat.getColor(context, R.color.primary)
        val normalColor = ContextCompat.getColor(context, R.color.text_secondary)

        btnPen.setColorFilter(if (currentTool == AnnotationView.Tool.PEN) selectedColor else normalColor)
        btnHighlighter.setColorFilter(if (currentTool == AnnotationView.Tool.HIGHLIGHTER) selectedColor else normalColor)
        btnEraser.setColorFilter(if (currentTool == AnnotationView.Tool.ERASER) selectedColor else normalColor)
        btnText.setColorFilter(if (currentTool == AnnotationView.Tool.TEXT) selectedColor else normalColor)
        btnRectangle.setColorFilter(if (currentTool == AnnotationView.Tool.RECTANGLE) selectedColor else normalColor)
        btnCircle.setColorFilter(if (currentTool == AnnotationView.Tool.CIRCLE) selectedColor else normalColor)
        btnArrow.setColorFilter(if (currentTool == AnnotationView.Tool.ARROW) selectedColor else normalColor)
        btnLine.setColorFilter(if (currentTool == AnnotationView.Tool.LINE) selectedColor else normalColor)
    }

    fun setColor(color: Int) {
        currentColor = color
        btnColor.setColorFilter(color)
        onColorSelectedListener?.invoke(color)
    }

    fun getColorButton(): ImageButton = btnColor

    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width
        onStrokeWidthChangedListener?.invoke(width)
    }

    private fun showStrokeWidthDialog() {
        val seekBar = SeekBar(context).apply {
            max = 30
            progress = currentStrokeWidth.toInt()
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(context)
            .setTitle("Stroke Width")
            .setView(seekBar)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val width = seekBar.progress.coerceAtLeast(1).toFloat()
                setStrokeWidth(width)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun setUndoEnabled(enabled: Boolean) {
        btnUndo.isEnabled = enabled
        btnUndo.alpha = if (enabled) 1f else 0.5f
    }

    fun setRedoEnabled(enabled: Boolean) {
        btnRedo.isEnabled = enabled
        btnRedo.alpha = if (enabled) 1f else 0.5f
    }

    fun getCurrentTool(): AnnotationView.Tool = currentTool
    fun getCurrentColor(): Int = currentColor
    fun getCurrentStrokeWidth(): Float = currentStrokeWidth
}

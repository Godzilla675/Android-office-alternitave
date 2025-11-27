package com.officesuite.app.ui.editor

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.officesuite.app.R

/**
 * Formatting toolbar for rich text editing with bold, italic, underline,
 * alignment, and font controls.
 */
class FormattingToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val toolbarLayout: LinearLayout

    // Formatting buttons
    private lateinit var btnBold: ImageButton
    private lateinit var btnItalic: ImageButton
    private lateinit var btnUnderline: ImageButton
    private lateinit var btnAlignLeft: ImageButton
    private lateinit var btnAlignCenter: ImageButton
    private lateinit var btnAlignRight: ImageButton
    private lateinit var btnTextColor: ImageButton
    private lateinit var btnHighlight: ImageButton
    private lateinit var btnFontSize: ImageButton
    private lateinit var btnBulletList: ImageButton
    private lateinit var btnNumberedList: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton

    // State
    private var isBold = false
    private var isItalic = false
    private var isUnderlined = false
    private var currentAlignment = 0 // 0 = left, 1 = center, 2 = right
    private var currentTextColor = Color.BLACK
    private var currentHighlightColor = Color.YELLOW

    // Listeners
    var onBoldClickListener: (() -> Unit)? = null
    var onItalicClickListener: (() -> Unit)? = null
    var onUnderlineClickListener: (() -> Unit)? = null
    var onAlignmentClickListener: ((Int) -> Unit)? = null
    var onTextColorClickListener: (() -> Unit)? = null
    var onHighlightClickListener: (() -> Unit)? = null
    var onFontSizeClickListener: (() -> Unit)? = null
    var onBulletListClickListener: (() -> Unit)? = null
    var onNumberedListClickListener: (() -> Unit)? = null
    var onUndoClickListener: (() -> Unit)? = null
    var onRedoClickListener: (() -> Unit)? = null

    init {
        isHorizontalScrollBarEnabled = false
        
        toolbarLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 4, 8, 4)
        }
        addView(toolbarLayout)
        
        setupViews()
    }

    private fun setupViews() {
        // Text style buttons
        btnBold = createToolButton(R.drawable.ic_bold) {
            isBold = !isBold
            updateButtonState(btnBold, isBold)
            onBoldClickListener?.invoke()
        }
        
        btnItalic = createToolButton(R.drawable.ic_italic) {
            isItalic = !isItalic
            updateButtonState(btnItalic, isItalic)
            onItalicClickListener?.invoke()
        }
        
        btnUnderline = createToolButton(R.drawable.ic_underline) {
            isUnderlined = !isUnderlined
            updateButtonState(btnUnderline, isUnderlined)
            onUnderlineClickListener?.invoke()
        }

        // Add separator
        toolbarLayout.addView(createSeparator())

        // Alignment buttons
        btnAlignLeft = createToolButton(R.drawable.ic_align_left) {
            setAlignment(0)
        }
        
        btnAlignCenter = createToolButton(R.drawable.ic_align_center) {
            setAlignment(1)
        }
        
        btnAlignRight = createToolButton(R.drawable.ic_align_right) {
            setAlignment(2)
        }

        // Add separator
        toolbarLayout.addView(createSeparator())

        // Color and font buttons
        btnTextColor = createToolButton(R.drawable.ic_color_palette) {
            onTextColorClickListener?.invoke()
        }
        btnTextColor.setColorFilter(currentTextColor)
        
        btnHighlight = createToolButton(R.drawable.ic_highlight) {
            onHighlightClickListener?.invoke()
        }
        
        btnFontSize = createToolButton(R.drawable.ic_font_size) {
            onFontSizeClickListener?.invoke()
        }

        // Add separator
        toolbarLayout.addView(createSeparator())

        // List buttons
        btnBulletList = createToolButton(R.drawable.ic_edit) { // Using edit as placeholder
            onBulletListClickListener?.invoke()
        }
        
        btnNumberedList = createToolButton(R.drawable.ic_document) { // Using document as placeholder
            onNumberedListClickListener?.invoke()
        }

        // Add separator
        toolbarLayout.addView(createSeparator())

        // Undo/Redo
        btnUndo = createToolButton(R.drawable.ic_undo) {
            onUndoClickListener?.invoke()
        }
        
        btnRedo = createToolButton(R.drawable.ic_redo) {
            onRedoClickListener?.invoke()
        }

        // Add all buttons
        toolbarLayout.addView(btnBold, 0)
        toolbarLayout.addView(btnItalic, 1)
        toolbarLayout.addView(btnUnderline, 2)
        // separator is at 3
        toolbarLayout.addView(btnAlignLeft, 4)
        toolbarLayout.addView(btnAlignCenter, 5)
        toolbarLayout.addView(btnAlignRight, 6)
        // separator is at 7
        toolbarLayout.addView(btnTextColor, 8)
        toolbarLayout.addView(btnHighlight, 9)
        toolbarLayout.addView(btnFontSize, 10)
        // separator is at 11
        toolbarLayout.addView(btnBulletList, 12)
        toolbarLayout.addView(btnNumberedList, 13)
        // separator is at 14
        toolbarLayout.addView(btnUndo)
        toolbarLayout.addView(btnRedo)

        // Initialize alignment
        updateAlignmentButtons()
    }

    private fun createToolButton(iconRes: Int, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            setImageResource(iconRes)
            setBackgroundResource(android.R.color.transparent)
            setPadding(20, 16, 20, 16)
            setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { onClick() }
        }
    }

    private fun createSeparator(): View {
        return View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray))
            layoutParams = LinearLayout.LayoutParams(2, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginStart = 12
                marginEnd = 12
            }
        }
    }

    private fun updateButtonState(button: ImageButton, isActive: Boolean) {
        val color = if (isActive) {
            ContextCompat.getColor(context, R.color.primary)
        } else {
            ContextCompat.getColor(context, R.color.text_secondary)
        }
        button.setColorFilter(color)
    }

    private fun setAlignment(alignment: Int) {
        currentAlignment = alignment
        updateAlignmentButtons()
        onAlignmentClickListener?.invoke(alignment)
    }

    private fun updateAlignmentButtons() {
        val activeColor = ContextCompat.getColor(context, R.color.primary)
        val normalColor = ContextCompat.getColor(context, R.color.text_secondary)
        
        btnAlignLeft.setColorFilter(if (currentAlignment == 0) activeColor else normalColor)
        btnAlignCenter.setColorFilter(if (currentAlignment == 1) activeColor else normalColor)
        btnAlignRight.setColorFilter(if (currentAlignment == 2) activeColor else normalColor)
    }

    fun setTextColor(color: Int) {
        currentTextColor = color
        btnTextColor.setColorFilter(color)
    }

    fun setHighlightColor(color: Int) {
        currentHighlightColor = color
    }

    fun setBoldState(bold: Boolean) {
        isBold = bold
        updateButtonState(btnBold, bold)
    }

    fun setItalicState(italic: Boolean) {
        isItalic = italic
        updateButtonState(btnItalic, italic)
    }

    fun setUnderlineState(underlined: Boolean) {
        isUnderlined = underlined
        updateButtonState(btnUnderline, underlined)
    }

    fun setUndoEnabled(enabled: Boolean) {
        btnUndo.isEnabled = enabled
        btnUndo.alpha = if (enabled) 1f else 0.5f
    }

    fun setRedoEnabled(enabled: Boolean) {
        btnRedo.isEnabled = enabled
        btnRedo.alpha = if (enabled) 1f else 0.5f
    }

    fun getTextColorButton(): ImageButton = btnTextColor
    fun getHighlightButton(): ImageButton = btnHighlight
    fun getFontSizeButton(): ImageButton = btnFontSize
}

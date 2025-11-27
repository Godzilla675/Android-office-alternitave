package com.officesuite.app.ui.editor

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatEditText
import java.util.Stack

/**
 * Rich text editor with formatting capabilities including:
 * - Bold, Italic, Underline, Strikethrough
 * - Font size and color
 * - Text alignment
 * - Bullet and numbered lists
 * - Undo/Redo support
 */
class RichTextEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    companion object {
        /** Maximum number of undo states to keep in memory */
        private const val MAX_UNDO_STACK_SIZE = 50
        /** Debounce delay in milliseconds before saving undo state */
        private const val UNDO_DEBOUNCE_DELAY_MS = 300L
    }

    data class UndoState(
        val text: CharSequence,
        val selectionStart: Int,
        val selectionEnd: Int
    )

    private val undoStack = Stack<UndoState>()
    private val redoStack = Stack<UndoState>()
    private var isUndoRedo = false
    private var lastTextLength = 0
    private val handler = Handler(Looper.getMainLooper())
    private var pendingUndoSave: Runnable? = null

    var onTextChangedListener: ((CharSequence?) -> Unit)? = null

    init {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No-op
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onTextChangedListener?.invoke(s)
            }

            override fun afterTextChanged(s: Editable?) {
                if (!isUndoRedo && s != null) {
                    val lengthDiff = kotlin.math.abs(s.length - lastTextLength)
                    if (lengthDiff > 0) {
                        // Debounce undo state saving to avoid performance issues
                        scheduleSaveUndoState()
                    }
                    lastTextLength = s.length
                }
            }
        })
    }

    private fun scheduleSaveUndoState() {
        pendingUndoSave?.let { handler.removeCallbacks(it) }
        pendingUndoSave = Runnable { saveUndoState() }
        handler.postDelayed(pendingUndoSave!!, UNDO_DEBOUNCE_DELAY_MS)
    }

    private fun saveUndoState() {
        val state = UndoState(
            SpannableStringBuilder(text),
            selectionStart,
            selectionEnd
        )
        undoStack.push(state)
        if (undoStack.size > MAX_UNDO_STACK_SIZE) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.size > 1) {
            isUndoRedo = true
            val currentState = undoStack.pop()
            redoStack.push(currentState)
            
            val previousState = undoStack.peek()
            setText(previousState.text)
            setSelection(
                previousState.selectionStart.coerceIn(0, text?.length ?: 0),
                previousState.selectionEnd.coerceIn(0, text?.length ?: 0)
            )
            isUndoRedo = false
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isUndoRedo = true
            val nextState = redoStack.pop()
            undoStack.push(nextState)
            
            setText(nextState.text)
            setSelection(
                nextState.selectionStart.coerceIn(0, text?.length ?: 0),
                nextState.selectionEnd.coerceIn(0, text?.length ?: 0)
            )
            isUndoRedo = false
        }
    }

    fun canUndo(): Boolean = undoStack.size > 1
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun toggleBold() {
        applyStyleSpan(Typeface.BOLD)
    }

    fun toggleItalic() {
        applyStyleSpan(Typeface.ITALIC)
    }

    fun toggleUnderline() {
        applySpan(UnderlineSpan())
    }

    fun toggleStrikethrough() {
        applySpan(StrikethroughSpan())
    }

    private fun applyStyleSpan(style: Int) {
        val start = selectionStart
        val end = selectionEnd
        
        if (start == end) return
        
        val spannable = text as? Spannable ?: return
        
        // Check if style is already applied
        val existingSpans = spannable.getSpans(start, end, StyleSpan::class.java)
        val hasStyle = existingSpans.any { it.style == style }
        
        if (hasStyle) {
            // Remove the style
            existingSpans.filter { it.style == style }.forEach { span ->
                val spanStart = spannable.getSpanStart(span)
                val spanEnd = spannable.getSpanEnd(span)
                spannable.removeSpan(span)
                
                // Re-apply to parts outside selection
                if (spanStart < start) {
                    spannable.setSpan(StyleSpan(style), spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (spanEnd > end) {
                    spannable.setSpan(StyleSpan(style), end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        } else {
            // Add the style
            spannable.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        saveUndoState()
    }

    private fun applySpan(span: Any) {
        val start = selectionStart
        val end = selectionEnd
        
        if (start == end) return
        
        val spannable = text as? Spannable ?: return
        
        // Check if span is already applied
        val existingSpans = spannable.getSpans(start, end, span.javaClass)
        
        if (existingSpans.isNotEmpty()) {
            // Remove existing spans
            existingSpans.forEach { existingSpan ->
                val spanStart = spannable.getSpanStart(existingSpan)
                val spanEnd = spannable.getSpanEnd(existingSpan)
                spannable.removeSpan(existingSpan)
                
                // Re-apply to parts outside selection
                if (spanStart < start) {
                    spannable.setSpan(createSpanInstance(span), spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (spanEnd > end) {
                    spannable.setSpan(createSpanInstance(span), end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        } else {
            // Add the span
            spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        saveUndoState()
    }

    private fun createSpanInstance(span: Any): Any {
        return when (span) {
            is UnderlineSpan -> UnderlineSpan()
            is StrikethroughSpan -> StrikethroughSpan()
            else -> span
        }
    }

    fun setSelectionTextColor(color: Int) {
        val start = selectionStart
        val end = selectionEnd
        
        if (start == end) return
        
        val spannable = text as? Spannable ?: return
        
        // Remove existing color spans in selection
        spannable.getSpans(start, end, ForegroundColorSpan::class.java).forEach {
            val spanStart = spannable.getSpanStart(it)
            val spanEnd = spannable.getSpanEnd(it)
            spannable.removeSpan(it)
            
            if (spanStart < start) {
                spannable.setSpan(ForegroundColorSpan(it.foregroundColor), spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (spanEnd > end) {
                spannable.setSpan(ForegroundColorSpan(it.foregroundColor), end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        
        spannable.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        saveUndoState()
    }

    fun setSelectionBackgroundColor(color: Int) {
        val start = selectionStart
        val end = selectionEnd
        
        if (start == end) return
        
        val spannable = text as? Spannable ?: return
        
        // Remove existing background spans in selection
        spannable.getSpans(start, end, BackgroundColorSpan::class.java).forEach {
            val spanStart = spannable.getSpanStart(it)
            val spanEnd = spannable.getSpanEnd(it)
            spannable.removeSpan(it)
            
            if (spanStart < start) {
                spannable.setSpan(BackgroundColorSpan(it.backgroundColor), spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (spanEnd > end) {
                spannable.setSpan(BackgroundColorSpan(it.backgroundColor), end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        
        spannable.setSpan(BackgroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        saveUndoState()
    }

    fun setFontSize(sizeSp: Float) {
        val start = selectionStart
        val end = selectionEnd
        
        if (start == end) return
        
        val spannable = text as? Spannable ?: return
        
        // Remove existing size spans in selection
        spannable.getSpans(start, end, AbsoluteSizeSpan::class.java).forEach {
            spannable.removeSpan(it)
        }
        
        val sizeInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sizeSp,
            context.resources.displayMetrics
        ).toInt()
        
        spannable.setSpan(AbsoluteSizeSpan(sizeInPx), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        saveUndoState()
    }

    fun setAlignment(alignment: Int) {
        gravity = when (alignment) {
            0 -> Gravity.START or Gravity.TOP
            1 -> Gravity.CENTER_HORIZONTAL or Gravity.TOP
            2 -> Gravity.END or Gravity.TOP
            else -> Gravity.START or Gravity.TOP
        }
    }

    fun insertBulletList() {
        val start = selectionStart
        val end = selectionEnd
        val text = text ?: return
        
        // Find line start
        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }
        
        // Insert bullet
        val bullet = "• "
        (text as Editable).insert(lineStart, bullet)
        setSelection(start + bullet.length)
        saveUndoState()
    }

    fun insertNumberedList() {
        val start = selectionStart
        val text = text ?: return
        
        // Find line start
        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }
        
        // Count existing numbered items
        var count = 1
        var pos = lineStart - 1
        while (pos > 0) {
            if (text[pos] == '\n') {
                val lineContent = getLineAt(pos + 1)
                if (lineContent.matches(Regex("^\\d+\\.\\s.*"))) {
                    count++
                } else {
                    break
                }
            }
            pos--
        }
        
        // Insert number
        val number = "$count. "
        (text as Editable).insert(lineStart, number)
        setSelection(start + number.length)
        saveUndoState()
    }

    private fun getLineAt(position: Int): String {
        val text = text ?: return ""
        var end = position
        while (end < text.length && text[end] != '\n') {
            end++
        }
        return if (position < text.length) {
            text.substring(position, end)
        } else {
            ""
        }
    }

    fun getHtmlContent(): String {
        val text = text ?: return ""
        val builder = StringBuilder()
        var i = 0
        
        while (i < text.length) {
            val c = text[i]
            val spans = (text as Spannable).getSpans(i, i + 1, Any::class.java)
            
            var openTags = ""
            var closeTags = ""
            
            for (span in spans) {
                when (span) {
                    is StyleSpan -> {
                        when (span.style) {
                            Typeface.BOLD -> {
                                openTags += "<b>"
                                closeTags = "</b>$closeTags"
                            }
                            Typeface.ITALIC -> {
                                openTags += "<i>"
                                closeTags = "</i>$closeTags"
                            }
                        }
                    }
                    is UnderlineSpan -> {
                        openTags += "<u>"
                        closeTags = "</u>$closeTags"
                    }
                    is StrikethroughSpan -> {
                        openTags += "<s>"
                        closeTags = "</s>$closeTags"
                    }
                }
            }
            
            builder.append(openTags)
            when (c) {
                '<' -> builder.append("&lt;")
                '>' -> builder.append("&gt;")
                '&' -> builder.append("&amp;")
                '\n' -> builder.append("<br>")
                else -> builder.append(c)
            }
            builder.append(closeTags)
            i++
        }
        
        return builder.toString()
    }

    fun getPlainText(): String {
        return text?.toString() ?: ""
    }

    fun isSelectionBold(): Boolean {
        return hasStyleInSelection(Typeface.BOLD)
    }

    fun isSelectionItalic(): Boolean {
        return hasStyleInSelection(Typeface.ITALIC)
    }

    fun isSelectionUnderlined(): Boolean {
        return hasSpanInSelection(UnderlineSpan::class.java)
    }

    private fun hasStyleInSelection(style: Int): Boolean {
        val start = selectionStart
        val end = selectionEnd
        if (start == end) return false
        
        val spannable = text as? Spannable ?: return false
        return spannable.getSpans(start, end, StyleSpan::class.java).any { it.style == style }
    }

    private fun <T> hasSpanInSelection(spanClass: Class<T>): Boolean {
        val start = selectionStart
        val end = selectionEnd
        if (start == end) return false
        
        val spannable = text as? Spannable ?: return false
        return spannable.getSpans(start, end, spanClass).isNotEmpty()
    }
}

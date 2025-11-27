package com.officesuite.app.ui.editor

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * Dialog for entering text annotations.
 */
class TextInputDialog : DialogFragment() {

    private var onTextEnteredListener: ((String) -> Unit)? = null

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_HINT = "hint"
        private const val ARG_INITIAL_TEXT = "initial_text"

        fun newInstance(
            title: String = "Enter Text",
            hint: String = "Type your text here",
            initialText: String = ""
        ): TextInputDialog {
            return TextInputDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_HINT, hint)
                    putString(ARG_INITIAL_TEXT, initialText)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(ARG_TITLE) ?: "Enter Text"
        val hint = arguments?.getString(ARG_HINT) ?: "Type your text here"
        val initialText = arguments?.getString(ARG_INITIAL_TEXT) ?: ""

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val editText = EditText(requireContext()).apply {
            this.hint = hint
            setText(initialText)
            if (initialText.isNotEmpty()) {
                setSelection(initialText.length)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(editText)

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = editText.text.toString()
                if (text.isNotBlank()) {
                    onTextEnteredListener?.invoke(text)
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    fun setOnTextEnteredListener(listener: (String) -> Unit): TextInputDialog {
        onTextEnteredListener = listener
        return this
    }
}

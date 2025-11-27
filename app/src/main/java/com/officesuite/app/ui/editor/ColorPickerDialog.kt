package com.officesuite.app.ui.editor

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.card.MaterialCardView
import com.officesuite.app.R

/**
 * Color picker dialog for selecting annotation and text colors.
 */
class ColorPickerDialog : DialogFragment() {

    private var onColorSelectedListener: ((Int) -> Unit)? = null
    private var selectedColor = Color.BLACK

    companion object {
        private const val ARG_SELECTED_COLOR = "selected_color"
        private const val ARG_TITLE = "title"

        val COLORS = intArrayOf(
            Color.BLACK,
            Color.WHITE,
            Color.RED,
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#673AB7"), // Deep Purple
            Color.parseColor("#3F51B5"), // Indigo
            Color.BLUE,
            Color.parseColor("#03A9F4"), // Light Blue
            Color.CYAN,
            Color.parseColor("#009688"), // Teal
            Color.GREEN,
            Color.parseColor("#8BC34A"), // Light Green
            Color.parseColor("#CDDC39"), // Lime
            Color.YELLOW,
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#FF5722"), // Deep Orange
            Color.parseColor("#795548"), // Brown
            Color.GRAY,
            Color.parseColor("#607D8B"), // Blue Gray
            Color.parseColor("#FFEB3B"), // Bright Yellow
            Color.parseColor("#00BCD4"), // Cyan variant
            Color.parseColor("#4CAF50")  // Green variant
        )

        fun newInstance(selectedColor: Int = Color.BLACK, title: String = "Select Color"): ColorPickerDialog {
            return ColorPickerDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SELECTED_COLOR, selectedColor)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedColor = it.getInt(ARG_SELECTED_COLOR, Color.BLACK)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(ARG_TITLE) ?: "Select Color"
        
        val gridLayout = GridLayout(requireContext()).apply {
            columnCount = 6
            rowCount = 4
            setPadding(32, 32, 32, 32)
        }

        COLORS.forEachIndexed { index, color ->
            val colorView = createColorView(requireContext(), color, color == selectedColor) { selectedColor ->
                onColorSelectedListener?.invoke(selectedColor)
                dismiss()
            }
            
            val params = GridLayout.LayoutParams().apply {
                width = 120
                height = 120
                setMargins(8, 8, 8, 8)
            }
            gridLayout.addView(colorView, params)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(gridLayout)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    private fun createColorView(context: Context, color: Int, isSelected: Boolean, onClick: (Int) -> Unit): View {
        val card = MaterialCardView(context).apply {
            setCardBackgroundColor(color)
            radius = 16f
            strokeWidth = if (isSelected) 4 else if (color == Color.WHITE) 1 else 0
            strokeColor = if (isSelected) Color.parseColor("#1976D2") else Color.LTGRAY
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick(color) }
        }

        if (isSelected) {
            val checkmark = ImageView(context).apply {
                setImageResource(R.drawable.ic_check)
                setColorFilter(if (color == Color.WHITE || color == Color.YELLOW) Color.BLACK else Color.WHITE)
                setPadding(20, 20, 20, 20)
            }
            card.addView(checkmark, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        return card
    }

    fun setOnColorSelectedListener(listener: (Int) -> Unit): ColorPickerDialog {
        onColorSelectedListener = listener
        return this
    }
}

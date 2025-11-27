package com.officesuite.app.ui.data

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.databinding.ItemCsvRowBinding

/**
 * Adapter for displaying CSV rows in RecyclerView.
 */
class CsvAdapter(
    private val data: List<List<String>>
) : RecyclerView.Adapter<CsvAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCsvRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position], position == 0)
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(
        private val binding: ItemCsvRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: List<String>, isHeader: Boolean) {
            // Join cells with separator for display
            binding.textRow.text = row.joinToString("  |  ")
            
            // Style header differently
            if (isHeader) {
                binding.textRow.setTypeface(null, android.graphics.Typeface.BOLD)
                binding.root.setBackgroundColor(0xFFE3F2FD.toInt()) // Light blue
            } else {
                binding.textRow.setTypeface(null, android.graphics.Typeface.NORMAL)
                binding.root.setBackgroundColor(android.graphics.Color.WHITE)
            }
        }
    }
}

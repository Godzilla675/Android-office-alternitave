package com.officesuite.app.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.R
import com.officesuite.app.data.model.DocumentType
import com.officesuite.app.data.repository.FavoriteItem
import com.officesuite.app.databinding.ItemFavoriteBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying favorite documents in RecyclerView.
 */
class FavoritesAdapter(
    private val onItemClick: (FavoriteItem) -> Unit,
    private val onRemoveClick: (FavoriteItem) -> Unit
) : ListAdapter<FavoriteItem, FavoritesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFavoriteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FavoriteItem) {
            binding.textFileName.text = item.name
            
            // Set icon based on document type
            val iconRes = when {
                item.type == DocumentType.PDF.name -> R.drawable.ic_pdf
                item.type == DocumentType.DOCX.name || item.type == DocumentType.DOC.name -> R.drawable.ic_document
                item.type == DocumentType.PPTX.name || item.type == DocumentType.PPT.name -> R.drawable.ic_presentation
                item.type == DocumentType.MARKDOWN.name -> R.drawable.ic_text
                else -> R.drawable.ic_document
            }
            binding.imageIcon.setImageResource(iconRes)
            
            // Format the date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.textDate.text = "Added ${dateFormat.format(Date(item.addedAt))}"
            
            // Click listeners
            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnRemove.setOnClickListener { onRemoveClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FavoriteItem>() {
        override fun areItemsTheSame(oldItem: FavoriteItem, newItem: FavoriteItem): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: FavoriteItem, newItem: FavoriteItem): Boolean {
            return oldItem == newItem
        }
    }
}

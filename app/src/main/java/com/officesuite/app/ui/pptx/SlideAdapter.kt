package com.officesuite.app.ui.pptx

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.databinding.ItemSlideBinding

class SlideAdapter(
    private val slides: List<Bitmap>
) : RecyclerView.Adapter<SlideAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSlideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size

    inner class ViewHolder(
        private val binding: ItemSlideBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bitmap: Bitmap) {
            binding.imageSlide.setImageBitmap(bitmap)
        }
    }
}

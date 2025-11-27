package com.officesuite.app.ui.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.databinding.ItemPdfPageBinding
import com.officesuite.app.utils.MemoryManager
import java.io.File

/**
 * Adapter for displaying PDF pages in a RecyclerView.
 * Uses MemoryManager for efficient bitmap caching and memory optimization.
 * Implements lazy loading - pages are rendered only when visible.
 */
class PdfPagesAdapter(
    private val pdfFile: File,
    private val onPageRendered: (Int, Int) -> Unit
) : RecyclerView.Adapter<PdfPagesAdapter.PageViewHolder>() {

    private var pdfRenderer: PdfRenderer? = null
    private var pageCount = 0
    private val documentKey = pdfFile.absolutePath
    private val renderedPages = mutableMapOf<Int, Bitmap>()

    init {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
            pageCount = pdfRenderer?.pageCount ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPdfPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
        onPageRendered(position, pageCount)
    }
    
    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        // Clear the ImageView to help with memory when scrolling
        holder.clearImage()
    }

    override fun getItemCount(): Int = pageCount

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    fun close() {
        renderedPages.values.forEach { it.recycle() }
        renderedPages.clear()
        pdfRenderer?.close()
        // Clear cached pages for this document
        for (i in 0 until pageCount) {
            val key = MemoryManager.createPageKey(documentKey, i)
            MemoryManager.removeBitmap(key)
        }
    }

    inner class PageViewHolder(
        private val binding: ItemPdfPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentPageIndex: Int = -1

        fun bind(pageIndex: Int) {
            // Check cache first
            val cacheKey = MemoryManager.createPageKey(documentKey, pageIndex)
            val cachedBitmap = MemoryManager.getBitmap(cacheKey)
            
            if (cachedBitmap != null) {
                // Use cached bitmap
                binding.imagePage.setImageBitmap(cachedBitmap)
                binding.textPageNumber.text = "Page ${pageIndex + 1}"
                return
            }
            
            // Render the page if not cached
            pdfRenderer?.let { renderer ->
                try {
                    // Trim cache if memory is low
                    MemoryManager.trimCacheIfNeeded()
            currentPageIndex = pageIndex
            
            pdfRenderer?.let { renderer ->
                try {
                    // Check if page is already rendered
                    val cachedBitmap = renderedPages[pageIndex]
                    if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                        binding.imagePage.setImageBitmap(cachedBitmap)
                        binding.textPageNumber.text = "Page ${pageIndex + 1}"
                        return
                    }
                    
                    val page = renderer.openPage(pageIndex)
                    
                    // Calculate dimensions for good quality
                    val scale = 2.0f
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()
                    
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    // Cache the rendered bitmap
                    MemoryManager.putBitmap(cacheKey, bitmap)
                    renderedPages[pageIndex] = bitmap
                    
                    binding.imagePage.setImageBitmap(bitmap)
                    binding.textPageNumber.text = "Page ${pageIndex + 1}"
                    
                    page.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        fun clearImage() {

        fun recycle() {
            binding.imagePage.setImageBitmap(null)
        }
    }
}

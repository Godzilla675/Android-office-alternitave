package com.officesuite.app.ui.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.databinding.ItemPdfPageBinding
import java.io.File

class PdfPagesAdapter(
    private val pdfFile: File,
    private val onPageRendered: (Int, Int) -> Unit
) : RecyclerView.Adapter<PdfPagesAdapter.PageViewHolder>() {

    private var pdfRenderer: PdfRenderer? = null
    private var pageCount = 0
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

    override fun getItemCount(): Int = pageCount

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    fun close() {
        renderedPages.values.forEach { it.recycle() }
        renderedPages.clear()
        pdfRenderer?.close()
    }

    inner class PageViewHolder(
        private val binding: ItemPdfPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentPageIndex: Int = -1

        fun bind(pageIndex: Int) {
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
                    renderedPages[pageIndex] = bitmap
                    
                    binding.imagePage.setImageBitmap(bitmap)
                    binding.textPageNumber.text = "Page ${pageIndex + 1}"
                    
                    page.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun recycle() {
            binding.imagePage.setImageBitmap(null)
        }
    }
}

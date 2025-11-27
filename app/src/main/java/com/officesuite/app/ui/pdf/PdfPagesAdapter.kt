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

    fun close() {
        pdfRenderer?.close()
    }

    inner class PageViewHolder(
        private val binding: ItemPdfPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pageIndex: Int) {
            pdfRenderer?.let { renderer ->
                try {
                    val page = renderer.openPage(pageIndex)
                    
                    // Calculate dimensions for good quality
                    val scale = 2.0f
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()
                    
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    binding.imagePage.setImageBitmap(bitmap)
                    binding.textPageNumber.text = "Page ${pageIndex + 1}"
                    
                    page.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

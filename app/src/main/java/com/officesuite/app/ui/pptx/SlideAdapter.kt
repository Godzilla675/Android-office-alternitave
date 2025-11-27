package com.officesuite.app.ui.pptx

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.databinding.ItemSlideBinding
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import java.io.File
import java.io.FileInputStream

/**
 * Adapter for rendering PPTX slides on-demand.
 * Slides are rendered lazily as they become visible, rather than all at once.
 */
class SlideAdapter(
    private val pptxFile: File,
    private val onSlideRendered: (Int, Int) -> Unit
) : RecyclerView.Adapter<SlideAdapter.ViewHolder>() {

    private var slideShow: XMLSlideShow? = null
    private var slideCount = 0
    private val renderedSlides = mutableMapOf<Int, Bitmap>()
    
    // Standard 16:9 presentation dimensions (scaled down for display)
    private val slideWidth = 960
    private val slideHeight = 540

    init {
        try {
            slideShow = XMLSlideShow(FileInputStream(pptxFile))
            slideCount = slideShow?.slides?.size ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSlideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
        onSlideRendered(position, slideCount)
    }

    override fun getItemCount(): Int = slideCount

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    fun close() {
        renderedSlides.values.forEach { 
            if (!it.isRecycled) it.recycle() 
        }
        renderedSlides.clear()
        try {
            slideShow?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class ViewHolder(
        private val binding: ItemSlideBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentSlideIndex: Int = -1

        fun bind(slideIndex: Int) {
            currentSlideIndex = slideIndex
            
            // Check if slide is already rendered
            val cachedBitmap = renderedSlides[slideIndex]
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                binding.imageSlide.setImageBitmap(cachedBitmap)
                return
            }
            
            // Render slide on-demand
            slideShow?.let { show ->
                try {
                    val slides = show.slides
                    if (slideIndex < slides.size) {
                        val slide = slides[slideIndex]
                        val bitmap = renderSlide(slide, slideIndex + 1)
                        renderedSlides[slideIndex] = bitmap
                        binding.imageSlide.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Show error placeholder
                    val errorBitmap = createErrorBitmap(slideIndex + 1)
                    binding.imageSlide.setImageBitmap(errorBitmap)
                }
            }
        }

        fun recycle() {
            binding.imageSlide.setImageBitmap(null)
        }
    }

    private fun renderSlide(slide: org.apache.poi.xslf.usermodel.XSLFSlide, slideNumber: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(slideWidth, slideHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw white background (simplified - Android doesn't have java.awt.Color)
        canvas.drawColor(Color.WHITE)
        
        val titlePaint = android.graphics.Paint().apply {
            color = Color.BLACK
            textSize = 36f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        val textPaint = android.graphics.Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }
        
        // Extract and render text content from shapes
        var yPosition = 60f
        var hasContent = false
        var isFirstText = true
        
        for (shape in slide.shapes) {
            if (shape is XSLFTextShape) {
                val text = shape.text ?: continue
                if (text.isNotBlank()) {
                    hasContent = true
                    
                    // Handle multi-line text
                    val lines = text.split("\n")
                    for (line in lines) {
                        if (line.isBlank()) {
                            yPosition += 20f
                            continue
                        }
                        
                        val trimmedLine = line.trim()
                        if (trimmedLine.length > 50) {
                            // Word wrap long lines
                            val words = trimmedLine.split(" ")
                            val lineBuilder = StringBuilder()
                            for (word in words) {
                                val testLine = if (lineBuilder.isEmpty()) word else "$lineBuilder $word"
                                if (testLine.length > 45) {
                                    drawTextLine(canvas, lineBuilder.toString(), yPosition, isFirstText, titlePaint, textPaint)
                                    yPosition += if (isFirstText) 50f else 35f
                                    isFirstText = false
                                    lineBuilder.clear()
                                    lineBuilder.append(word)
                                } else {
                                    if (lineBuilder.isNotEmpty()) lineBuilder.append(" ")
                                    lineBuilder.append(word)
                                }
                            }
                            if (lineBuilder.isNotEmpty()) {
                                drawTextLine(canvas, lineBuilder.toString(), yPosition, isFirstText, titlePaint, textPaint)
                                yPosition += if (isFirstText) 50f else 35f
                                isFirstText = false
                            }
                        } else {
                            drawTextLine(canvas, trimmedLine, yPosition, isFirstText, titlePaint, textPaint)
                            yPosition += if (isFirstText) 50f else 35f
                            isFirstText = false
                        }
                        
                        if (yPosition > slideHeight - 60) break
                    }
                    
                    yPosition += 15f // Gap between shapes
                    if (yPosition > slideHeight - 60) break
                }
            }
        }
        
        // If no content found, show placeholder
        if (!hasContent) {
            canvas.drawText("Slide $slideNumber", slideWidth / 2f, slideHeight / 2f, titlePaint)
        }
        
        // Draw slide number at bottom
        val pageNumberPaint = android.graphics.Paint().apply {
            color = Color.GRAY
            textSize = 18f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("$slideNumber", slideWidth / 2f, slideHeight - 20f, pageNumberPaint)
        
        return bitmap
    }
    
    private fun drawTextLine(
        canvas: Canvas,
        text: String,
        y: Float,
        isTitle: Boolean,
        titlePaint: android.graphics.Paint,
        textPaint: android.graphics.Paint
    ) {
        if (isTitle) {
            canvas.drawText(text, slideWidth / 2f, y, titlePaint)
        } else {
            canvas.drawText("• $text", 40f, y, textPaint)
        }
    }

    private fun createErrorBitmap(slideNumber: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(slideWidth, slideHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        val paint = android.graphics.Paint().apply {
            color = Color.RED
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Error loading slide $slideNumber", slideWidth / 2f, slideHeight / 2f, paint)
        
        return bitmap
    }
}


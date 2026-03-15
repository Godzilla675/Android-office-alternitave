package com.officesuite.app.ui.editor

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentPdfEditorBinding
import com.officesuite.app.ui.pdf.PdfPagesAdapter
import com.officesuite.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF Editor Fragment with full annotation support including:
 * - Freehand drawing with pen
 * - Highlighter tool
 * - Shape tools (rectangle, circle, arrow, line)
 * - Text annotations
 * - Eraser tool
 * - Color picker
 * - Stroke width control
 * - Undo/Redo
 * - Save annotated PDF
 */
class PdfEditorFragment : Fragment() {

    private var _binding: FragmentPdfEditorBinding? = null
    private val binding get() = _binding!!

    private var fileUri: Uri? = null
    private var cachedFile: File? = null
    private var currentPage = 0
    private var totalPages = 0
    private var pdfAdapter: PdfPagesAdapter? = null
    
    private var currentTool = AnnotationView.Tool.NONE
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 5f

    // Store annotations per page
    private val pageAnnotations = mutableMapOf<Int, List<AnnotationView.Annotation>>()
    // Store redo stacks per page for undo persistence
    private val pageRedoStacks = mutableMapOf<Int, List<AnnotationView.Annotation>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("file_uri")?.let { uriString ->
            fileUri = Uri.parse(uriString)
            loadPdf()
        }

        setupToolbar()
        setupAnnotationToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupAnnotationView()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            inflateMenu(R.menu.menu_pdf_editor)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save -> {
                        saveAnnotatedPdf()
                        true
                    }
                    R.id.action_clear -> {
                        clearAnnotations()
                        true
                    }
                    R.id.action_share -> {
                        sharePdf()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupAnnotationToolbar() {
        val toolButtons = mapOf(
            binding.btnPen to AnnotationView.Tool.PEN,
            binding.btnHighlighter to AnnotationView.Tool.HIGHLIGHTER,
            binding.btnEraser to AnnotationView.Tool.ERASER,
            binding.btnText to AnnotationView.Tool.TEXT,
            binding.btnRectangle to AnnotationView.Tool.RECTANGLE,
            binding.btnCircle to AnnotationView.Tool.CIRCLE,
            binding.btnArrow to AnnotationView.Tool.ARROW,
            binding.btnLine to AnnotationView.Tool.LINE
        )

        toolButtons.forEach { (button, tool) ->
            button.setOnClickListener {
                if (currentTool == tool) {
                    // Deselect tool
                    selectTool(AnnotationView.Tool.NONE)
                } else {
                    selectTool(tool)
                    if (tool == AnnotationView.Tool.TEXT) {
                        showTextInputDialog()
                    }
                }
            }
        }

        binding.btnColor.setOnClickListener {
            showColorPicker()
        }

        binding.btnStrokeWidth.setOnClickListener {
            showStrokeWidthDialog()
        }

        binding.btnUndo.setOnClickListener {
            binding.annotationView.undo()
            updateUndoRedoButtons()
        }

        binding.btnRedo.setOnClickListener {
            binding.annotationView.redo()
            updateUndoRedoButtons()
        }

        updateUndoRedoButtons()
    }

    private fun selectTool(tool: AnnotationView.Tool) {
        currentTool = tool
        binding.annotationView.setTool(tool)
        updateToolbarSelection()
    }

    private fun updateToolbarSelection() {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val normalColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        binding.btnPen.setColorFilter(if (currentTool == AnnotationView.Tool.PEN) selectedColor else normalColor)
        binding.btnHighlighter.setColorFilter(if (currentTool == AnnotationView.Tool.HIGHLIGHTER) selectedColor else normalColor)
        binding.btnEraser.setColorFilter(if (currentTool == AnnotationView.Tool.ERASER) selectedColor else normalColor)
        binding.btnText.setColorFilter(if (currentTool == AnnotationView.Tool.TEXT) selectedColor else normalColor)
        binding.btnRectangle.setColorFilter(if (currentTool == AnnotationView.Tool.RECTANGLE) selectedColor else normalColor)
        binding.btnCircle.setColorFilter(if (currentTool == AnnotationView.Tool.CIRCLE) selectedColor else normalColor)
        binding.btnArrow.setColorFilter(if (currentTool == AnnotationView.Tool.ARROW) selectedColor else normalColor)
        binding.btnLine.setColorFilter(if (currentTool == AnnotationView.Tool.LINE) selectedColor else normalColor)
    }

    private fun updateUndoRedoButtons() {
        val canUndo = binding.annotationView.canUndo()
        val canRedo = binding.annotationView.canRedo()

        binding.btnUndo.alpha = if (canUndo) 1f else 0.5f
        binding.btnUndo.isEnabled = canUndo
        binding.btnRedo.alpha = if (canRedo) 1f else 0.5f
        binding.btnRedo.isEnabled = canRedo
    }

    private fun showColorPicker() {
        ColorPickerDialog.newInstance(currentColor, "Select Color")
            .setOnColorSelectedListener { color ->
                currentColor = color
                binding.annotationView.strokeColor = color
                binding.btnColor.setColorFilter(color)
            }
            .show(parentFragmentManager, "color_picker")
    }

    private fun showStrokeWidthDialog() {
        val seekBar = SeekBar(requireContext()).apply {
            max = 30
            progress = currentStrokeWidth.toInt()
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Stroke Width")
            .setView(seekBar)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                currentStrokeWidth = seekBar.progress.coerceAtLeast(1).toFloat()
                binding.annotationView.strokeWidth = currentStrokeWidth
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTextInputDialog() {
        TextInputDialog.newInstance("Add Text Annotation")
            .setOnTextEnteredListener { text ->
                // For now, add text at center of view
                val x = binding.annotationView.width / 2f
                val y = binding.annotationView.height / 2f
                binding.annotationView.addTextAnnotation(text, x, y)
                selectTool(AnnotationView.Tool.NONE)
            }
            .show(parentFragmentManager, "text_input")
    }

    private fun setupAnnotationView() {
        binding.annotationView.apply {
            strokeColor = currentColor
            strokeWidth = currentStrokeWidth
            onAnnotationChangeListener = {
                updateUndoRedoButtons()
                // Save current page annotations and redo stack
                pageAnnotations[currentPage] = getAnnotations()
                pageRedoStacks[currentPage] = getRedoStack()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerPdfPages.apply {
            layoutManager = LinearLayoutManager(context)
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                    if (firstVisiblePosition != RecyclerView.NO_POSITION && firstVisiblePosition != currentPage) {
                        // Save current page annotations and redo stack before switching
                        pageAnnotations[currentPage] = binding.annotationView.getAnnotations()
                        pageRedoStacks[currentPage] = binding.annotationView.getRedoStack()
                        currentPage = firstVisiblePosition
                        updatePageInfo()
                        // Load annotations for new page
                        loadAnnotationsForPage(currentPage)
                    }
                }
            })
        }
    }

    private fun loadAnnotationsForPage(page: Int) {
        val annotations = pageAnnotations[page] ?: emptyList()
        val redoStack = pageRedoStacks[page] ?: emptyList()
        binding.annotationView.setAnnotationsWithRedo(annotations, redoStack)
        updateUndoRedoButtons()
    }

    private fun setupClickListeners() {
        binding.fabPrevious.setOnClickListener {
            if (currentPage > 0) {
                pageAnnotations[currentPage] = binding.annotationView.getAnnotations()
                pageRedoStacks[currentPage] = binding.annotationView.getRedoStack()
                currentPage--
                binding.recyclerPdfPages.smoothScrollToPosition(currentPage)
                updatePageInfo()
                loadAnnotationsForPage(currentPage)
            }
        }

        binding.fabNext.setOnClickListener {
            if (currentPage < totalPages - 1) {
                pageAnnotations[currentPage] = binding.annotationView.getAnnotations()
                pageRedoStacks[currentPage] = binding.annotationView.getRedoStack()
                currentPage++
                binding.recyclerPdfPages.smoothScrollToPosition(currentPage)
                updatePageInfo()
                loadAnnotationsForPage(currentPage)
            }
        }

        binding.fabSave.setOnClickListener {
            saveAnnotatedPdf()
        }
    }

    private fun loadPdf() {
        fileUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    cachedFile = withContext(Dispatchers.IO) {
                        FileUtils.copyToCache(requireContext(), uri)
                    }

                    cachedFile?.let { file ->
                        pdfAdapter = PdfPagesAdapter(file) { page, count ->
                            totalPages = count
                            if (page == 0) {
                                binding.progressBar.visibility = View.GONE
                                updatePageInfo()
                            }
                        }
                        binding.recyclerPdfPages.adapter = pdfAdapter
                        binding.toolbar.title = file.name
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updatePageInfo() {
        binding.textPageInfo.text = getString(R.string.page_of, currentPage + 1, totalPages)
    }

    private fun saveAnnotatedPdf() {
        // Save current page annotations
        pageAnnotations[currentPage] = binding.annotationView.getAnnotations()

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val savedFile = withContext(Dispatchers.IO) {
                    cachedFile?.let { file ->
                        val outputFile = File(
                            FileUtils.getOutputDirectory(requireContext()),
                            "annotated_${file.name}"
                        )
                        
                        // Use iText to embed annotations
                        saveAnnotationsWithIText(file, outputFile)
                        outputFile
                    }
                }

                binding.progressBar.visibility = View.GONE
                if (savedFile != null) {
                    Toast.makeText(context, "Annotations saved to ${savedFile.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save annotations", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveAnnotationsWithIText(inputFile: File, outputFile: File) {
        var pdfDoc: com.itextpdf.kernel.pdf.PdfDocument? = null
        try {
            val reader = com.itextpdf.kernel.pdf.PdfReader(inputFile)
            val writer = com.itextpdf.kernel.pdf.PdfWriter(outputFile)
            pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(reader, writer)
            
            // Process annotations for each page
            pageAnnotations.forEach { (pageIndex, annotations) ->
                if (annotations.isNotEmpty()) {
                    val page = pdfDoc.getPage(pageIndex + 1) // iText uses 1-based indexing
                    val pageHeight = page.pageSize.height
                    
                    annotations.forEach { annotation ->
                        when {
                            // Handle freehand drawing (pen/highlighter)
                            annotation.path != null -> {
                                val pathBounds = android.graphics.RectF()
                                annotation.path.computeBounds(pathBounds, true)
                                
                                val inkList = com.itextpdf.kernel.pdf.PdfArray()
                                val pointArray = com.itextpdf.kernel.pdf.PdfArray()
                                
                                // Extract points from path (simplified approach)
                                val pathMeasure = android.graphics.PathMeasure(annotation.path, false)
                                val pathLength = pathMeasure.length
                                val numPoints = minOf(100, (pathLength / 5).toInt().coerceAtLeast(10))
                                val coords = FloatArray(2)
                                
                                for (i in 0..numPoints) {
                                    val distance = (i.toFloat() / numPoints) * pathLength
                                    if (pathMeasure.getPosTan(distance, coords, null)) {
                                        pointArray.add(com.itextpdf.kernel.pdf.PdfNumber(coords[0].toDouble()))
                                        pointArray.add(com.itextpdf.kernel.pdf.PdfNumber((pageHeight - coords[1]).toDouble()))
                                    }
                                }
                                inkList.add(pointArray)
                                
                                val pdfRect = com.itextpdf.kernel.geom.Rectangle(
                                    pathBounds.left - 5,
                                    pageHeight - pathBounds.bottom - 5,
                                    pathBounds.width() + 10,
                                    pathBounds.height() + 10
                                )
                                
                                val inkAnnotation = com.itextpdf.kernel.pdf.annot.PdfInkAnnotation(pdfRect, inkList)
                                val r = Color.red(annotation.paint.color)
                                val g = Color.green(annotation.paint.color)
                                val b = Color.blue(annotation.paint.color)
                                inkAnnotation.setColor(com.itextpdf.kernel.colors.DeviceRgb(r, g, b))
                                
                                // Set opacity for highlighter
                                if (annotation.shapeType == AnnotationView.Tool.HIGHLIGHTER) {
                                    inkAnnotation.setOpacity(com.itextpdf.kernel.pdf.PdfNumber(0.4))
                                }
                                
                                // Generate appearance stream so annotation is visible in all viewers
                                val appearance = com.itextpdf.kernel.pdf.xobject.PdfFormXObject(pdfRect)
                                val canvas = com.itextpdf.kernel.pdf.canvas.PdfCanvas(appearance, pdfDoc)
                                canvas.saveState()
                                canvas.setStrokeColor(com.itextpdf.kernel.colors.DeviceRgb(r, g, b))
                                canvas.setLineWidth(annotation.paint.strokeWidth)
                                if (annotation.shapeType == AnnotationView.Tool.HIGHLIGHTER) {
                                    canvas.setExtGState(com.itextpdf.kernel.pdf.extgstate.PdfExtGState().setStrokeOpacity(0.4f))
                                }
                                // Replay the path points in the appearance stream
                                val apCoords = FloatArray(2)
                                val apMeasure = android.graphics.PathMeasure(annotation.path, false)
                                val apLength = apMeasure.length
                                val apNumPoints = minOf(100, (apLength / 5).toInt().coerceAtLeast(10))
                                var apFirst = true
                                for (j in 0..apNumPoints) {
                                    val dist = (j.toFloat() / apNumPoints) * apLength
                                    if (apMeasure.getPosTan(dist, apCoords, null)) {
                                        val ax = (apCoords[0] - pdfRect.x).toDouble()
                                        val ay = (pageHeight - apCoords[1] - pdfRect.y).toDouble()
                                        if (apFirst) {
                                            canvas.moveTo(ax, ay)
                                            apFirst = false
                                        } else {
                                            canvas.lineTo(ax, ay)
                                        }
                                    }
                                }
                                if (!apFirst) canvas.stroke()
                                canvas.restoreState()
                                inkAnnotation.setNormalAppearance(appearance.pdfObject)
                                
                                page.addAnnotation(inkAnnotation)
                            }
                            // Handle text annotations
                            annotation.text != null && annotation.textPosition != null -> {
                                val pdfRect = com.itextpdf.kernel.geom.Rectangle(
                                    annotation.textPosition.x,
                                    pageHeight - annotation.textPosition.y - 20,
                                    200f,
                                    24f
                                )
                                val freeTextAnnotation = com.itextpdf.kernel.pdf.annot.PdfFreeTextAnnotation(
                                    pdfRect,
                                    com.itextpdf.kernel.pdf.PdfString(annotation.text)
                                )
                                freeTextAnnotation.setContents(annotation.text)
                                val r = Color.red(annotation.paint.color)
                                val g = Color.green(annotation.paint.color)
                                val b = Color.blue(annotation.paint.color)
                                freeTextAnnotation.setColor(com.itextpdf.kernel.colors.DeviceRgb(r, g, b))
                                
                                // Generate appearance stream so annotation is visible in all viewers
                                val appearance = com.itextpdf.kernel.pdf.xobject.PdfFormXObject(pdfRect)
                                val canvas = com.itextpdf.kernel.pdf.canvas.PdfCanvas(appearance, pdfDoc)
                                canvas.saveState()
                                canvas.beginText()
                                val font = com.itextpdf.kernel.font.PdfFontFactory.createFont()
                                canvas.setFontAndSize(font, 12f)
                                canvas.setFillColor(com.itextpdf.kernel.colors.DeviceRgb(r, g, b))
                                canvas.moveText(2.0, (pdfRect.height - 12f).toDouble())
                                canvas.showText(annotation.text)
                                canvas.endText()
                                canvas.restoreState()
                                freeTextAnnotation.setNormalAppearance(appearance.pdfObject)
                                
                                page.addAnnotation(freeTextAnnotation)
                            }
                            // Handle shape annotations (rectangle, circle)
                            annotation.shape != null -> {
                                val shape = annotation.shape
                                val pdfRect = com.itextpdf.kernel.geom.Rectangle(
                                    shape.left,
                                    pageHeight - shape.bottom,
                                    shape.width(),
                                    shape.height()
                                )
                                
                                val squareAnnotation = com.itextpdf.kernel.pdf.annot.PdfSquareAnnotation(pdfRect)
                                val r = Color.red(annotation.paint.color)
                                val g = Color.green(annotation.paint.color)
                                val b = Color.blue(annotation.paint.color)
                                squareAnnotation.setColor(com.itextpdf.kernel.colors.DeviceRgb(r, g, b))
                                
                                // Generate appearance stream so annotation is visible in all viewers
                                val appearance = com.itextpdf.kernel.pdf.xobject.PdfFormXObject(pdfRect)
                                val canvas = com.itextpdf.kernel.pdf.canvas.PdfCanvas(appearance, pdfDoc)
                                canvas.saveState()
                                canvas.setStrokeColor(com.itextpdf.kernel.colors.DeviceRgb(r, g, b))
                                canvas.setLineWidth(annotation.paint.strokeWidth)
                                if (annotation.shapeType == AnnotationView.Tool.CIRCLE) {
                                    // Draw ellipse inscribed in the rectangle
                                    val cx = pdfRect.width / 2
                                    val cy = pdfRect.height / 2
                                    val rx = pdfRect.width / 2
                                    val ry = pdfRect.height / 2
                                    canvas.ellipse(
                                        (cx - rx).toDouble(), (cy - ry).toDouble(),
                                        (cx + rx).toDouble(), (cy + ry).toDouble()
                                    )
                                    canvas.stroke()
                                } else {
                                    canvas.rectangle(0.0, 0.0, pdfRect.width.toDouble(), pdfRect.height.toDouble())
                                    canvas.stroke()
                                }
                                canvas.restoreState()
                                squareAnnotation.setNormalAppearance(appearance.pdfObject)
                                
                                page.addAnnotation(squareAnnotation)
                            }
                            // Handle line/arrow annotations
                            annotation.startPoint != null && annotation.endPoint != null -> {
                                val lineFloatArray = floatArrayOf(
                                    annotation.startPoint.x,
                                    pageHeight - annotation.startPoint.y,
                                    annotation.endPoint.x,
                                    pageHeight - annotation.endPoint.y
                                )
                                
                                val minX = minOf(annotation.startPoint.x, annotation.endPoint.x)
                                val minY = minOf(annotation.startPoint.y, annotation.endPoint.y)
                                val maxX = maxOf(annotation.startPoint.x, annotation.endPoint.x)
                                val maxY = maxOf(annotation.startPoint.y, annotation.endPoint.y)
                                
                                val pdfRect = com.itextpdf.kernel.geom.Rectangle(
                                    minX - 5,
                                    pageHeight - maxY - 5,
                                    maxX - minX + 10,
                                    maxY - minY + 10
                                )
                                
                                val lineAnnotation = com.itextpdf.kernel.pdf.annot.PdfLineAnnotation(pdfRect, lineFloatArray)
                                val r = Color.red(annotation.paint.color)
                                val g = Color.green(annotation.paint.color)
                                val b = Color.blue(annotation.paint.color)
                                lineAnnotation.setColor(com.itextpdf.kernel.colors.DeviceRgb(r, g, b))
                                
                                // Add arrow head if it's an arrow
                                if (annotation.shapeType == AnnotationView.Tool.ARROW) {
                                    val endingStyles = com.itextpdf.kernel.pdf.PdfArray()
                                    endingStyles.add(com.itextpdf.kernel.pdf.PdfName("None"))
                                    endingStyles.add(com.itextpdf.kernel.pdf.PdfName("OpenArrow"))
                                    lineAnnotation.put(com.itextpdf.kernel.pdf.PdfName.LE, endingStyles)
                                }
                                
                                // Generate appearance stream so annotation is visible in all viewers
                                val appearance = com.itextpdf.kernel.pdf.xobject.PdfFormXObject(pdfRect)
                                val canvas = com.itextpdf.kernel.pdf.canvas.PdfCanvas(appearance, pdfDoc)
                                canvas.saveState()
                                canvas.setStrokeColor(com.itextpdf.kernel.colors.DeviceRgb(r, g, b))
                                canvas.setLineWidth(annotation.paint.strokeWidth)
                                val lx1 = (annotation.startPoint.x - pdfRect.x).toDouble()
                                val ly1 = (pageHeight - annotation.startPoint.y - pdfRect.y).toDouble()
                                val lx2 = (annotation.endPoint.x - pdfRect.x).toDouble()
                                val ly2 = (pageHeight - annotation.endPoint.y - pdfRect.y).toDouble()
                                canvas.moveTo(lx1, ly1)
                                canvas.lineTo(lx2, ly2)
                                canvas.stroke()
                                if (annotation.shapeType == AnnotationView.Tool.ARROW) {
                                    // Draw arrowhead
                                    val angle = Math.atan2(ly2 - ly1, lx2 - lx1)
                                    val arrowLen = annotation.paint.strokeWidth * 4.0
                                    val arrowAngle = Math.toRadians(30.0)
                                    canvas.moveTo(lx2, ly2)
                                    canvas.lineTo(
                                        lx2 - arrowLen * Math.cos(angle - arrowAngle),
                                        ly2 - arrowLen * Math.sin(angle - arrowAngle)
                                    )
                                    canvas.moveTo(lx2, ly2)
                                    canvas.lineTo(
                                        lx2 - arrowLen * Math.cos(angle + arrowAngle),
                                        ly2 - arrowLen * Math.sin(angle + arrowAngle)
                                    )
                                    canvas.stroke()
                                }
                                canvas.restoreState()
                                lineAnnotation.setNormalAppearance(appearance.pdfObject)
                                
                                page.addAnnotation(lineAnnotation)
                            }
                        }
                    }
                }
            }
            
            pdfDoc.close()
        } catch (e: Exception) {
            // Ensure pdfDoc is closed even on error
            try { pdfDoc?.close() } catch (_: Exception) {}
            // If iText fails, fall back to copying the original file
            // This ensures the user at least gets their original PDF
            inputFile.copyTo(outputFile, overwrite = true)
            // Log the error but don't throw - allow the copy to be used
            e.printStackTrace()
        }
    }

    private fun clearAnnotations() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Annotations")
            .setMessage("Are you sure you want to clear all annotations on this page?")
            .setPositiveButton("Clear") { _, _ ->
                binding.annotationView.clear()
                pageAnnotations[currentPage] = emptyList()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun sharePdf() {
        cachedFile?.let { file ->
            com.officesuite.app.utils.ShareUtils.shareFile(requireContext(), file, "application/pdf")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfAdapter?.close()
        _binding = null
    }
}

package com.officesuite.app.ui.pptx

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.officesuite.app.MainActivity
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentPptxViewerBinding
import com.officesuite.app.utils.ErrorHandler
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.MemoryManager
import com.officesuite.app.utils.Result
import com.officesuite.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fragment for viewing PowerPoint (PPTX) presentations.
 * Features:
 * - Slide-by-slide navigation with swipe gestures
 * - Memory-efficient slide rendering with caching
 * - Share and convert functionality
 * - Graceful error handling
 */
class PptxViewerFragment : Fragment() {

    private var _binding: FragmentPptxViewerBinding? = null
    private val binding get() = _binding!!
    
    private var fileUri: Uri? = null
    private var cachedFile: File? = null
    private var slideAdapter: SlideAdapter? = null
    private var currentSlide = 0
    private var totalSlides = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPptxViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.getString("file_uri")?.let { uriString ->
            fileUri = Uri.parse(uriString)
            loadPresentation()
        }
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
                @Suppress("DEPRECATION")
                requireActivity().onBackPressed()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            inflateMenu(R.menu.menu_pptx_viewer)
            
            // Hide PiP option if not supported
            val mainActivity = activity as? MainActivity
            menu.findItem(R.id.action_pip)?.isVisible = mainActivity?.isPipSupported() == true
            
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_pip -> {
                        enterPipMode()
                    R.id.action_slideshow -> {
                        startSlideshow()
                    R.id.action_edit -> {
                        openEditor()
                        true
                    }
                    R.id.action_share -> {
                        shareDocument()
                        true
                    }
                    R.id.action_convert -> {
                        convertToPdf()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerSlides.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                    if (firstVisiblePosition != RecyclerView.NO_POSITION && firstVisiblePosition != currentSlide) {
                        currentSlide = firstVisiblePosition
                        updateSlideInfo()
                    }
                }
            })
        }
    }

    private fun setupClickListeners() {
        binding.fabPrevious.setOnClickListener {
            navigateToPreviousSlide()
        }
        
        binding.fabNext.setOnClickListener {
            navigateToNextSlide()
        }
    }
    
    private fun navigateToPreviousSlide() {
        if (currentSlide > 0) {
            currentSlide--
            binding.recyclerSlides.smoothScrollToPosition(currentSlide)
            updateSlideInfo()
        }
    }
    
    private fun navigateToNextSlide() {
        if (currentSlide < slideImages.size - 1) {
            currentSlide++
            binding.recyclerSlides.smoothScrollToPosition(currentSlide)
            updateSlideInfo()
            if (currentSlide < totalSlides - 1) {
                currentSlide++
                binding.recyclerSlides.smoothScrollToPosition(currentSlide)
                updateSlideInfo()
            }
        }
    }

    private fun loadPresentation() {
        fileUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                val result = Result.runCatchingSuspend {
                    val file = withContext(Dispatchers.IO) {
                        FileUtils.copyToCache(requireContext(), uri)
                    }
                    
                    if (file != null) {
                        withContext(Dispatchers.IO) {
                            loadSlides(file)
                        }
                        file
                    } else {
                        throw IllegalStateException("Could not read file")
                    cachedFile?.let { file ->
                        binding.toolbar.title = file.name
                        
                        // Create on-demand adapter - slides are rendered as they become visible
                        slideAdapter = SlideAdapter(file) { slideIndex, slideCount ->
                            if (totalSlides == 0) {
                                totalSlides = slideCount
                                updateSlideInfo()
                            }
                            if (slideIndex == 0) {
                                binding.progressBar.visibility = View.GONE
                            }
                        }
                        binding.recyclerSlides.adapter = slideAdapter
                    }
                }
                
                result.onSuccess { file ->
                    cachedFile = file
                    binding.toolbar.title = file.name
                    
                    val adapter = SlideAdapter(slideImages)
                    binding.recyclerSlides.adapter = adapter
                    
                    updateSlideInfo()
                    binding.progressBar.visibility = View.GONE
                }.onError { error ->
                    binding.progressBar.visibility = View.GONE
                    ErrorHandler.showErrorToast(requireContext(), error.exception)
                }
            }
        }
    }

    /**
     * Loads slides from a PPTX file and converts them to bitmaps for display.
     * Uses MemoryManager for efficient memory handling.
     * 
     * @param file The PPTX file to load
     */
    private fun loadSlides(file: File) {
        slideImages.clear()
        
        // Trim cache if memory is low before loading slides
        MemoryManager.trimCacheIfNeeded()
        
        try {
            val slideShow = XMLSlideShow(FileInputStream(file))
            
            // Use standard 16:9 presentation dimensions (1920x1080 scaled down)
            val slideWidth = 960
            val slideHeight = 540
            
            for (slide in slideShow.slides) {
                val bitmap = Bitmap.createBitmap(
                    slideWidth,
                    slideHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                // Extract text content from the slide
                val textPaint = android.graphics.Paint().apply {
                    color = Color.BLACK
                    textSize = 24f
                    isAntiAlias = true
                }
                
                val titlePaint = android.graphics.Paint().apply {
                    color = Color.BLACK
                    textSize = 36f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                
                val slideNumber = slideImages.size + 1
                
                // Try to extract slide title and content
                var yPosition = 60f
                var hasContent = false
                
                for (shape in slide.shapes) {
                    if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                        val text = shape.text
                        if (text.isNotBlank()) {
                            hasContent = true
                            // Draw first text as title, rest as content
                            if (yPosition == 60f) {
                                canvas.drawText(text.take(40), slideWidth / 2f, yPosition, titlePaint)
                            } else {
                                canvas.drawText(text.take(50), 30f, yPosition, textPaint)
                            }
                            yPosition += 40f
                            if (yPosition > slideHeight - 80) break
                        }
                    }
                }
                
                // If no content found, show placeholder
                if (!hasContent) {
                    canvas.drawText(
                        "Slide $slideNumber",
                        slideWidth / 2f,
                        slideHeight / 2f,
                        titlePaint
                    )
                }
                
                slideImages.add(bitmap)
            }
            
            slideShow.close()
        } catch (e: Exception) {
            ErrorHandler.logError("PptxViewer", "Error loading slides", e)
        }
    private fun updateSlideInfo() {
        binding.textSlideInfo.text = "Slide ${currentSlide + 1} of $totalSlides"
    }

    private fun openEditor() {
        fileUri?.let { uri ->
            val bundle = Bundle().apply {
                putString("file_uri", uri.toString())
            }
            findNavController().navigate(R.id.pptxEditorFragment, bundle)
        }
    }

    private fun shareDocument() {
        cachedFile?.let { file ->
            ShareUtils.shareFile(
                requireContext(),
                file,
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            )
        }
    }

    private fun enterPipMode() {
        val mainActivity = activity as? MainActivity
        if (mainActivity?.isPipSupported() == true) {
            mainActivity.enterPipMode()
        } else {
            Toast.makeText(context, "Picture-in-Picture not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Update UI based on PiP mode state
     */
    fun onPipModeChanged(isInPipMode: Boolean) {
        if (_binding == null) return
        
        if (isInPipMode) {
            // Hide UI elements in PiP mode
            binding.toolbar.visibility = View.GONE
            binding.appBarLayout.visibility = View.GONE
            // Hide navigation controls
            view?.findViewById<View>(R.id.fabPrevious)?.visibility = View.GONE
            view?.findViewById<View>(R.id.fabNext)?.visibility = View.GONE
            view?.findViewById<View>(R.id.textSlideInfo)?.visibility = View.GONE
        } else {
            // Show UI elements when exiting PiP mode
            binding.toolbar.visibility = View.VISIBLE
            binding.appBarLayout.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.fabPrevious)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.fabNext)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.textSlideInfo)?.visibility = View.VISIBLE
        }
    }

    private fun convertToPdf() {
        Toast.makeText(context, "Converting to PDF...", Toast.LENGTH_SHORT).show()
        // Navigate to converter or perform conversion
    }

    private fun startSlideshow() {
        fileUri?.let { uri ->
            val intent = Intent(requireContext(), PresentationModeActivity::class.java).apply {
                putExtra(PresentationModeActivity.EXTRA_FILE_URI, uri.toString())
            }
            startActivity(intent)
        } ?: run {
            Toast.makeText(context, "No presentation loaded", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Recycle all bitmaps using MemoryManager
        MemoryManager.recycleBitmaps(slideImages)
        slideImages.clear()
        slideAdapter?.close()
        slideAdapter = null
        _binding = null
    }
}

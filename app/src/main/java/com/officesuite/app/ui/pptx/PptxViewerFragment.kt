package com.officesuite.app.ui.pptx

import android.content.Intent
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
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.MainActivity
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentPptxViewerBinding
import com.officesuite.app.utils.ErrorHandler
import com.officesuite.app.utils.FileUtils
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
            }
            inflateMenu(R.menu.menu_pptx_viewer)
            
            // Hide PiP option if not supported
            val mainActivity = activity as? MainActivity
            menu.findItem(R.id.action_pip)?.isVisible = mainActivity?.isPipSupported() == true
            
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_pip -> {
                        enterPipMode()
                        true
                    }
                    R.id.action_slideshow -> {
                        startSlideshow()
                        true
                    }
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
        if (currentSlide < totalSlides - 1) {
            currentSlide++
            binding.recyclerSlides.smoothScrollToPosition(currentSlide)
            updateSlideInfo()
        }
    }

    private fun loadPresentation() {
        fileUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                val result = Result.runCatchingSuspend {
                    withContext(Dispatchers.IO) {
                        FileUtils.copyToCache(requireContext(), uri)
                    }
                }
                
                result.onSuccess { file ->
                    if (file != null) {
                        cachedFile = file
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
                    } else {
                        binding.progressBar.visibility = View.GONE
                        ErrorHandler.showErrorToast(requireContext(), ErrorHandler.ErrorType.FILE_READ_ERROR)
                    }
                }.onError { error ->
                    binding.progressBar.visibility = View.GONE
                    ErrorHandler.showErrorToast(requireContext(), error.exception)
                }
            }
        }
    }

    private fun updateSlideInfo() {
        binding.textSlideInfo.text = getString(R.string.slide_of, currentSlide + 1, totalSlides)
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
        
        lifecycleScope.launch {
            try {
                val documentConverter = com.officesuite.app.data.repository.DocumentConverter(requireContext())
                cachedFile?.let { file ->
                    val options = com.officesuite.app.data.model.ConversionOptions(
                        sourceFormat = com.officesuite.app.data.model.DocumentType.PPTX,
                        targetFormat = com.officesuite.app.data.model.DocumentType.PDF
                    )
                    val result = documentConverter.convert(file, options)
                    
                    if (result.success && result.outputPath != null) {
                        Toast.makeText(context, "PDF saved: ${java.io.File(result.outputPath).name}", Toast.LENGTH_LONG).show()
                        ShareUtils.shareFile(
                            requireContext(),
                            java.io.File(result.outputPath),
                            "application/pdf"
                        )
                    } else {
                        Toast.makeText(context, "Conversion failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                    }
                } ?: run {
                    Toast.makeText(context, "No file loaded", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
        slideAdapter?.close()
        slideAdapter = null
        _binding = null
    }
}

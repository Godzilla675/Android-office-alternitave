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
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentPptxViewerBinding
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            inflateMenu(R.menu.menu_pptx_viewer)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
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
            if (currentSlide > 0) {
                currentSlide--
                binding.recyclerSlides.smoothScrollToPosition(currentSlide)
                updateSlideInfo()
            }
        }
        
        binding.fabNext.setOnClickListener {
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
                try {
                    cachedFile = withContext(Dispatchers.IO) {
                        FileUtils.copyToCache(requireContext(), uri)
                    }
                    
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
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load presentation: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        slideAdapter?.close()
        slideAdapter = null
        _binding = null
    }
}

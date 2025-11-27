package com.officesuite.app.ui.pptx

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentPptxViewerBinding
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.File
import java.io.FileInputStream

class PptxViewerFragment : Fragment() {

    private var _binding: FragmentPptxViewerBinding? = null
    private val binding get() = _binding!!
    
    private var fileUri: Uri? = null
    private var cachedFile: File? = null
    private var slideImages = mutableListOf<Bitmap>()
    private var currentSlide = 0

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
                requireActivity().onBackPressed()
            }
            inflateMenu(R.menu.menu_pptx_viewer)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
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
            if (currentSlide < slideImages.size - 1) {
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
                        withContext(Dispatchers.IO) {
                            loadSlides(file)
                        }
                        
                        binding.toolbar.title = file.name
                        
                        val adapter = SlideAdapter(slideImages)
                        binding.recyclerSlides.adapter = adapter
                        
                        updateSlideInfo()
                        binding.progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load presentation: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadSlides(file: File) {
        slideImages.clear()
        
        try {
            val slideShow = XMLSlideShow(FileInputStream(file))
            val dimension = slideShow.pageSize
            
            for (slide in slideShow.slides) {
                val bitmap = Bitmap.createBitmap(
                    dimension.width,
                    dimension.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                // Draw slide content (simplified - real implementation would render each shape)
                val paint = android.graphics.Paint().apply {
                    color = Color.BLACK
                    textSize = 48f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                
                val slideNumber = slideImages.size + 1
                canvas.drawText(
                    "Slide $slideNumber",
                    dimension.width / 2f,
                    dimension.height / 2f,
                    paint
                )
                
                slideImages.add(bitmap)
            }
            
            slideShow.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateSlideInfo() {
        binding.textSlideInfo.text = "Slide ${currentSlide + 1} of ${slideImages.size}"
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

    override fun onDestroyView() {
        super.onDestroyView()
        slideImages.forEach { it.recycle() }
        slideImages.clear()
        _binding = null
    }
}

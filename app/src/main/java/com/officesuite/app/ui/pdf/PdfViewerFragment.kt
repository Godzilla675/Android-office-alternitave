package com.officesuite.app.ui.pdf

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
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentPdfViewerBinding
import com.officesuite.app.ocr.OcrManager
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewerFragment : Fragment() {

    private var _binding: FragmentPdfViewerBinding? = null
    private val binding get() = _binding!!
    
    private var fileUri: Uri? = null
    private var currentPage = 0
    private var totalPages = 0
    private var cachedFile: File? = null
    private var pdfAdapter: PdfPagesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.getString("file_uri")?.let { uriString ->
            fileUri = Uri.parse(uriString)
            loadPdf()
        }
        
        setupToolbar()
        setupClickListeners()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.recyclerPdfPages.apply {
            layoutManager = LinearLayoutManager(context)
            // Add snap helper for page-by-page scrolling
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                    if (firstVisiblePosition != RecyclerView.NO_POSITION && firstVisiblePosition != currentPage) {
                        currentPage = firstVisiblePosition
                        updatePageInfo()
                    }
                }
            })
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
            inflateMenu(R.menu.menu_pdf_viewer)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_share -> {
                        shareDocument()
                        true
                    }
                    R.id.action_ocr -> {
                        runOcr()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabPrevious.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                binding.recyclerPdfPages.smoothScrollToPosition(currentPage)
                updatePageInfo()
            }
        }
        
        binding.fabNext.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                binding.recyclerPdfPages.smoothScrollToPosition(currentPage)
                updatePageInfo()
            }
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

    private fun shareDocument() {
        cachedFile?.let { file ->
            ShareUtils.shareFile(requireContext(), file, "application/pdf")
        }
    }

    private fun runOcr() {
        Toast.makeText(context, "Running OCR on current page...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val ocrManager = OcrManager()
                // For PDF, we would need to render the page to bitmap first
                // This is a simplified implementation
                Toast.makeText(context, "OCR requires image input. Please use scanner.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfAdapter?.close()
        _binding = null
    }
}

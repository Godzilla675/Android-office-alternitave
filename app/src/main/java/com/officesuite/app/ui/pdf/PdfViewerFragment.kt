package com.officesuite.app.ui.pdf

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.MainActivity
import com.officesuite.app.R
import com.officesuite.app.data.collaboration.CollaborationRepository
import com.officesuite.app.databinding.FragmentPdfViewerBinding
import com.officesuite.app.pdf.PdfTextExtractor
import com.officesuite.app.utils.ErrorHandler
import com.officesuite.app.ui.collaboration.CommentsDialogFragment
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.Result
import com.officesuite.app.utils.ShareUtils
import com.officesuite.app.utils.animateFadeOut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fragment for viewing PDF documents.
 * Features:
 * - Lazy loading with LRU caching for memory optimization
 * - Swipe gesture navigation between pages
 * - Text extraction and copy
 * - Text search within document
 * - OCR support for text extraction
 * - Share functionality
 */
class PdfViewerFragment : Fragment() {

    private var _binding: FragmentPdfViewerBinding? = null
    private val binding get() = _binding!!
    
    private var fileUri: Uri? = null
    private var currentPage = 0
    private var totalPages = 0
    private var cachedFile: File? = null
    private var pdfAdapter: PdfPagesAdapter? = null
    private var documentId: String = ""
    private lateinit var collaborationRepository: CollaborationRepository
    private lateinit var pdfTextExtractor: PdfTextExtractor

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
        
        collaborationRepository = CollaborationRepository(requireContext())
        pdfTextExtractor = PdfTextExtractor()
        
        arguments?.getString("file_uri")?.let { uriString ->
            fileUri = Uri.parse(uriString)
            documentId = uriString // Use URI as document ID
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
                findNavController().navigateUp()
            }
            inflateMenu(R.menu.menu_pdf_viewer)
            
            // Hide PiP option if not supported
            val mainActivity = activity as? MainActivity
            menu.findItem(R.id.action_pip)?.isVisible = mainActivity?.isPipSupported() == true
            
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_search -> {
                        showSearchDialog()
                        true
                    }
                    R.id.action_copy_text -> {
                        copyCurrentPageText()
                        true
                    }
                    R.id.action_pip -> {
                        enterPipMode()
                        true
                    }
                    R.id.action_comments -> {
                        showComments()
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
                    R.id.action_ocr -> {
                        runOcr()
                        true
                    }
                    R.id.action_version_history -> {
                        showVersionHistory()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabPrevious.setOnClickListener {
            navigateToPreviousPage()
        }
        
        binding.fabNext.setOnClickListener {
            navigateToNextPage()
        }
    }
    
    private fun navigateToPreviousPage() {
        if (currentPage > 0) {
            currentPage--
            binding.recyclerPdfPages.smoothScrollToPosition(currentPage)
            updatePageInfo()
        }
    }
    
    private fun navigateToNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++
            binding.recyclerPdfPages.smoothScrollToPosition(currentPage)
            updatePageInfo()
        }
    }

    private fun loadPdf() {
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
                        setupPdfAdapter(file)
                    } else {
                        showError(ErrorHandler.ErrorType.FILE_READ_ERROR)
                    }
                }.onError { error ->
                    showError(error.exception)
                }
            }
        }
    }
    
    private fun setupPdfAdapter(file: File) {
        pdfAdapter = PdfPagesAdapter(file) { page, count ->
            totalPages = count
            if (page == 0) {
                binding.progressBar.animateFadeOut()
                updatePageInfo()
            }
        }
        binding.recyclerPdfPages.adapter = pdfAdapter
        binding.toolbar.title = file.name
    }
    
    private fun showError(throwable: Throwable) {
        binding.progressBar.visibility = View.GONE
        ErrorHandler.showErrorToast(requireContext(), throwable)
    }
    
    private fun showError(errorType: ErrorHandler.ErrorType) {
        binding.progressBar.visibility = View.GONE
        ErrorHandler.showErrorToast(requireContext(), errorType)
    }

    private fun updatePageInfo() {
        binding.textPageInfo.text = getString(R.string.page_of, currentPage + 1, totalPages)
    }

    private fun showComments() {
        val dialog = CommentsDialogFragment.newInstance(
            documentId = documentId,
            pageNumber = currentPage + 1
        )
        dialog.show(parentFragmentManager, "comments_dialog")
    }
    
    private fun showVersionHistory() {
        lifecycleScope.launch {
            val versions = collaborationRepository.getVersionsForDocument(documentId)
            
            if (versions.isEmpty()) {
                Toast.makeText(context, R.string.no_version_history, Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val versionNames = versions.map { 
                "${it.name ?: "Version ${it.versionNumber}"} - ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it.createdAt))}"
            }.toTypedArray()
            
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.version_history)
                .setItems(versionNames) { _, which ->
                    val version = versions[which]
                    showVersionOptions(version)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
    
    private fun showVersionOptions(version: com.officesuite.app.data.collaboration.DocumentVersion) {
        val options = arrayOf("Restore this version", "View details")
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(version.name ?: "Version ${version.versionNumber}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> restoreVersion(version)
                    1 -> showVersionDetails(version)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun restoreVersion(version: com.officesuite.app.data.collaboration.DocumentVersion) {
        lifecycleScope.launch {
            cachedFile?.let { file ->
                val success = collaborationRepository.restoreVersion(version.id, file.absolutePath)
                if (success) {
                    Toast.makeText(context, "Version restored", Toast.LENGTH_SHORT).show()
                    loadPdf() // Reload the PDF
                } else {
                    Toast.makeText(context, "Failed to restore version", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showVersionDetails(version: com.officesuite.app.data.collaboration.DocumentVersion) {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        val details = buildString {
            appendLine("Version: ${version.versionNumber}")
            appendLine("Name: ${version.name ?: "N/A"}")
            appendLine("Created: ${dateFormat.format(java.util.Date(version.createdAt))}")
            appendLine("Author: ${version.author}")
            appendLine("Size: ${version.fileSize / 1024} KB")
            version.description?.let { appendLine("Description: $it") }
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Version Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun openEditor() {
        fileUri?.let { uri ->
            val bundle = Bundle().apply {
                putString("file_uri", uri.toString())
            }
            findNavController().navigate(R.id.pdfEditorFragment, bundle)
        }
    }

    private fun shareDocument() {
        cachedFile?.let { file ->
            ShareUtils.shareFile(requireContext(), file, "application/pdf")
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

    private fun runOcr() {
        Toast.makeText(context, "Running OCR on current page...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                // For PDF, we would need to render the page to bitmap first
                // This is a simplified implementation
                Toast.makeText(context, "OCR requires image input. Please use scanner.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                ErrorHandler.showErrorToast(requireContext(), e)
            }
        }
    }
    
    private fun showSearchDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter search text"
            setPadding(48, 32, 48, 32)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.search)
            .setView(editText)
            .setPositiveButton(R.string.search) { _, _ ->
                val query = editText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun performSearch(query: String) {
        cachedFile?.let { file ->
            lifecycleScope.launch {
                binding.progressBar.visibility = View.VISIBLE
                
                val results = pdfTextExtractor.searchText(file, query)
                
                binding.progressBar.visibility = View.GONE
                
                if (results.isEmpty()) {
                    Toast.makeText(context, "No results found for \"$query\"", Toast.LENGTH_SHORT).show()
                } else {
                    showSearchResults(query, results)
                }
            }
        }
    }
    
    private fun showSearchResults(query: String, results: List<PdfTextExtractor.SearchResult>) {
        val resultItems = results.map { result ->
            "Page ${result.pageNumber} (${result.matchCount} match${if (result.matchCount > 1) "es" else ""})\n${result.contextSnippet}"
        }.toTypedArray()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Search Results: \"$query\"")
            .setItems(resultItems) { _, which ->
                // Navigate to the selected page
                val pageIndex = results[which].pageNumber - 1
                currentPage = pageIndex
                binding.recyclerPdfPages.scrollToPosition(pageIndex)
                updatePageInfo()
            }
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun copyCurrentPageText() {
        cachedFile?.let { file ->
            lifecycleScope.launch {
                binding.progressBar.visibility = View.VISIBLE
                
                val result = pdfTextExtractor.extractTextFromPage(file, currentPage + 1)
                
                binding.progressBar.visibility = View.GONE
                
                if (result.success && !result.text.isNullOrEmpty()) {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("PDF Text", result.text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                } else if (result.text.isNullOrEmpty()) {
                    Toast.makeText(context, "No text found on this page", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to extract text: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                }
            }
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
            // Hide navigation controls in linear layout
            view?.findViewById<View>(R.id.fabPrevious)?.visibility = View.GONE
            view?.findViewById<View>(R.id.fabNext)?.visibility = View.GONE
            view?.findViewById<View>(R.id.textPageInfo)?.visibility = View.GONE
        } else {
            // Show UI elements when exiting PiP mode
            binding.toolbar.visibility = View.VISIBLE
            binding.appBarLayout.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.fabPrevious)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.fabNext)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.textPageInfo)?.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfAdapter?.close()
        _binding = null
    }
}

package com.officesuite.app.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.officesuite.app.R
import com.officesuite.app.data.model.DocumentFile
import com.officesuite.app.data.model.DocumentType
import com.officesuite.app.data.repository.PreferencesRepository
import com.officesuite.app.data.templates.TemplateRepository
import com.officesuite.app.databinding.FragmentHomeBinding
import com.officesuite.app.utils.FileUtils
import kotlinx.coroutines.launch
import com.officesuite.app.utils.NavigationUtils

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recentFilesAdapter: RecentFilesAdapter
    private lateinit var quickActionsAdapter: QuickActionsAdapter
    private lateinit var templateRepository: TemplateRepository
    private lateinit var preferencesRepository: PreferencesRepository

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleFileOpened(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        templateRepository = TemplateRepository(requireContext())
        preferencesRepository = PreferencesRepository(requireContext())
        setupUI()
        setupRecyclerViews()
        setupClickListeners()
        setupTemplateClickListeners()
        loadRecentFiles()
    }

    private fun setupUI() {
        binding.toolbar.title = getString(R.string.app_name)
    }

    private fun setupRecyclerViews() {
        // Quick Actions
        val quickActions = listOf(
            QuickAction("PDF", R.drawable.ic_pdf, DocumentType.PDF),
            QuickAction("Word", R.drawable.ic_document, DocumentType.DOCX),
            QuickAction("PowerPoint", R.drawable.ic_presentation, DocumentType.PPTX),
            QuickAction("Markdown", R.drawable.ic_text, DocumentType.MARKDOWN),
            QuickAction("All Files", R.drawable.ic_folder, null)
        )
        
        quickActionsAdapter = QuickActionsAdapter(quickActions) { action ->
            if (action.type == null) {
                openFilePicker(arrayOf("*/*"))
            } else {
                openFilePicker(arrayOf(action.type.mimeType))
            }
        }
        
        binding.recyclerQuickActions.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = quickActionsAdapter
        }

        // Recent Files
        recentFilesAdapter = RecentFilesAdapter(emptyList()) { file ->
            openDocument(file)
        }
        
        binding.recyclerRecentFiles.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentFilesAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAdd.setOnClickListener {
            showCreateNewDialog()
        }

        binding.cardOpenFile.setOnClickListener {
            openFilePicker(arrayOf("*/*"))
        }
        
        binding.textViewAllTemplates.setOnClickListener {
            showAllTemplatesDialog()
        }
    }
    
    private fun setupTemplateClickListeners() {
        binding.cardTemplateResume.setOnClickListener {
            openTemplateById("builtin_resume_classic")
        }
        
        binding.cardTemplateLetter.setOnClickListener {
            openTemplateById("builtin_letter_formal")
        }
        
        binding.cardTemplateInvoice.setOnClickListener {
            openTemplateById("builtin_invoice")
        }
        
        binding.cardTemplatePresentation.setOnClickListener {
            openTemplateById("builtin_presentation_business")
        }
    }
    
    private fun openTemplateById(templateId: String) {
        lifecycleScope.launch {
            val template = templateRepository.getTemplateById(templateId)
            if (template != null) {
                // Record usage
                templateRepository.recordTemplateUsage(templateId)
                
                // Navigate to markdown editor with template content
                val bundle = Bundle().apply {
                    putString("template_content", template.content)
                    putString("template_name", template.name)
                }
                findNavController().navigate(R.id.markdownFragment, bundle)
            } else {
                Toast.makeText(context, "Template not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAllTemplatesDialog() {
        lifecycleScope.launch {
            val templates = templateRepository.getAllTemplates()
            val templateNames = templates.map { it.name }.toTypedArray()
            
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.templates)
                .setItems(templateNames) { _, which ->
                    openTemplateById(templates[which].id)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun openFilePicker(mimeTypes: Array<String>) {
        openDocumentLauncher.launch(mimeTypes)
    }

    private fun handleFileOpened(uri: Uri) {
        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        
        val fileName = FileUtils.getFileName(requireContext(), uri)
        val docType = FileUtils.getDocumentType(requireContext(), uri)
        
        openDocument(DocumentFile(
            uri = uri,
            name = fileName,
            type = docType,
            size = FileUtils.getFileSize(requireContext(), uri),
            lastModified = System.currentTimeMillis()
        ))
    }

    private fun openDocument(file: DocumentFile) {
        // Save to recent files
        preferencesRepository.addRecentFile(
            uri = file.uri.toString(),
            name = file.name,
            type = file.type,
            size = file.size
        )
        
        NavigationUtils.navigateToViewer(this, file.uri.toString(), file.type)
        val bundle = Bundle().apply {
            putString("file_uri", file.uri.toString())
        }
        
        when (file.type) {
            DocumentType.PDF -> {
                findNavController().navigate(R.id.pdfViewerFragment, bundle)
            }
            DocumentType.DOCX, DocumentType.DOC -> {
                findNavController().navigate(R.id.docxViewerFragment, bundle)
            }
            DocumentType.PPTX, DocumentType.PPT -> {
                findNavController().navigate(R.id.pptxViewerFragment, bundle)
            }
            DocumentType.XLSX, DocumentType.XLS -> {
                findNavController().navigate(R.id.xlsxViewerFragment, bundle)
            }
            DocumentType.MARKDOWN, DocumentType.TXT -> {
                findNavController().navigate(R.id.markdownFragment, bundle)
            }
            else -> {
                Toast.makeText(context, "Unsupported file type", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadRecentFiles() {
        val recentFileItems = preferencesRepository.getRecentFiles()
        val documentFiles = recentFileItems.mapNotNull { item ->
            try {
                val uri = Uri.parse(item.uri)
                val docType = DocumentType.values().find { it.name == item.type } ?: DocumentType.UNKNOWN
                DocumentFile(
                    uri = uri,
                    name = item.name,
                    type = docType,
                    size = item.size,
                    lastModified = item.accessedAt
                )
            } catch (e: Exception) {
                null
            }
        }
        
        recentFilesAdapter.updateFiles(documentFiles)
        
        // Update empty state visibility
        if (documentFiles.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.emptyState.visibility = View.GONE
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh recent files when returning to this fragment
        loadRecentFiles()
    }

    private fun showCreateNewDialog() {
        val options = arrayOf("Markdown Document", "From Template")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Create New")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> createNewMarkdown()
                    1 -> showAllTemplatesDialog()
                }
            }
            .show()
    }

    private fun createNewMarkdown() {
        findNavController().navigate(R.id.markdownFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class QuickAction(
    val title: String,
    val iconRes: Int,
    val type: DocumentType?
)

package com.officesuite.app.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.officesuite.app.R
import com.officesuite.app.data.model.DocumentFile
import com.officesuite.app.data.model.DocumentType
import com.officesuite.app.databinding.FragmentHomeBinding
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.NavigationUtils

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recentFilesAdapter: RecentFilesAdapter
    private lateinit var quickActionsAdapter: QuickActionsAdapter

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
        setupUI()
        setupRecyclerViews()
        setupClickListeners()
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

    private fun showCreateNewDialog() {
        val options = arrayOf("Markdown Document", "Text Document")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Create New")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> createNewMarkdown()
                    1 -> createNewText()
                }
            }
            .show()
    }

    private fun createNewMarkdown() {
        findNavController().navigate(R.id.markdownFragment)
    }

    private fun createNewText() {
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

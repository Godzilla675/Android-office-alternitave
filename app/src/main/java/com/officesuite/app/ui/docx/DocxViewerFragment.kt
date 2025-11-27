package com.officesuite.app.ui.docx

import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentDocxViewerBinding
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream

class DocxViewerFragment : Fragment() {

    private var _binding: FragmentDocxViewerBinding? = null
    private val binding get() = _binding!!
    
    private var fileUri: Uri? = null
    private var cachedFile: File? = null
    private var documentText = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocxViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.getString("file_uri")?.let { uriString ->
            fileUri = Uri.parse(uriString)
            loadDocument()
        }
        
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            inflateMenu(R.menu.menu_docx_viewer)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
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
                    R.id.action_copy -> {
                        copyToClipboard()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun loadDocument() {
        fileUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                try {
                    cachedFile = withContext(Dispatchers.IO) {
                        FileUtils.copyToCache(requireContext(), uri)
                    }
                    
                    cachedFile?.let { file ->
                        val content = withContext(Dispatchers.IO) {
                            extractDocxContent(file)
                        }
                        
                        binding.toolbar.title = file.name
                        binding.textContent.text = content
                        binding.progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load document: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun extractDocxContent(file: File): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        documentText.clear()
        
        try {
            val document = XWPFDocument(FileInputStream(file))
            
            for (paragraph in document.paragraphs) {
                val text = paragraph.text
                if (text.isNotEmpty()) {
                    val start = builder.length
                    builder.append(text)
                    builder.append("\n\n")
                    documentText.append(text).append("\n\n")
                    
                    // Check for bold/italic
                    for (run in paragraph.runs) {
                        if (run.isBold) {
                            builder.setSpan(
                                StyleSpan(android.graphics.Typeface.BOLD),
                                start,
                                start + text.length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        if (run.isItalic) {
                            builder.setSpan(
                                StyleSpan(android.graphics.Typeface.ITALIC),
                                start,
                                start + text.length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                }
            }
            
            // Process tables
            for (table in document.tables) {
                for (row in table.rows) {
                    for (cell in row.tableCells) {
                        builder.append(cell.text)
                        builder.append("\t")
                        documentText.append(cell.text).append("\t")
                    }
                    builder.append("\n")
                    documentText.append("\n")
                }
                builder.append("\n")
                documentText.append("\n")
            }
            
            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
            builder.append("Error reading document: ${e.message}")
        }
        
        return builder
    }

    private fun openEditor() {
        fileUri?.let { uri ->
            val bundle = Bundle().apply {
                putString("file_uri", uri.toString())
            }
            findNavController().navigate(R.id.docxEditorFragment, bundle)
        }
    }

    private fun shareDocument() {
        cachedFile?.let { file ->
            ShareUtils.shareFile(
                requireContext(),
                file,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        }
    }

    private fun convertToPdf() {
        Toast.makeText(context, "Converting to PDF...", Toast.LENGTH_SHORT).show()
    }

    private fun copyToClipboard() {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Document", documentText.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

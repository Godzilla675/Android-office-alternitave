package com.officesuite.app.ui.converter

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.officesuite.app.R
import com.officesuite.app.data.model.ConversionOptions
import com.officesuite.app.data.model.DocumentType
import com.officesuite.app.data.repository.DocumentConverter
import com.officesuite.app.databinding.FragmentConverterBinding
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ConverterFragment : Fragment() {

    private var _binding: FragmentConverterBinding? = null
    private val binding get() = _binding!!
    
    private var selectedUri: Uri? = null
    private var sourceType: DocumentType = DocumentType.UNKNOWN
    private lateinit var documentConverter: DocumentConverter

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleFileSelected(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConverterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        documentConverter = DocumentConverter(requireContext())
        
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Setup output format spinner
        val formats = listOf("PDF", "DOCX", "PPTX", "Markdown", "Text")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, formats)
        binding.spinnerOutputFormat.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSelectFile.setOnClickListener {
            openDocumentLauncher.launch(arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/markdown",
                "text/plain"
            ))
        }

        binding.btnConvert.setOnClickListener {
            if (selectedUri != null) {
                showConversionDialog()
            } else {
                Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleFileSelected(uri: Uri) {
        selectedUri = uri
        
        val fileName = FileUtils.getFileName(requireContext(), uri)
        val fileSize = FileUtils.getFileSize(requireContext(), uri)
        sourceType = FileUtils.getDocumentType(requireContext(), uri)
        
        binding.textFileName.text = fileName
        binding.textFileInfo.text = "${FileUtils.formatFileSize(fileSize)} • ${sourceType.extension.uppercase()}"
        binding.cardFileInfo.visibility = View.VISIBLE
        binding.btnConvert.isEnabled = true
        
        updateAvailableFormats()
    }

    private fun updateAvailableFormats() {
        val availableFormats = when (sourceType) {
            DocumentType.PDF -> listOf("DOCX", "PPTX", "Text")
            DocumentType.DOCX -> listOf("PDF", "Text", "Markdown")
            DocumentType.PPTX -> listOf("PDF")
            DocumentType.MARKDOWN -> listOf("PDF", "Text")
            DocumentType.TXT -> listOf("PDF", "Markdown")
            else -> listOf("PDF")
        }
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, availableFormats)
        binding.spinnerOutputFormat.adapter = adapter
    }

    /**
     * Shows a dialog to customize the output file name and OCR options before conversion.
     */
    private fun showConversionDialog() {
        val uri = selectedUri ?: return
        val fileName = FileUtils.getFileName(requireContext(), uri)
        val baseName = fileName.substringBeforeLast('.')
        
        val targetFormatString = binding.spinnerOutputFormat.selectedItem as String
        val targetType = when (targetFormatString) {
            "PDF" -> DocumentType.PDF
            "DOCX" -> DocumentType.DOCX
            "PPTX" -> DocumentType.PPTX
            "Markdown" -> DocumentType.MARKDOWN
            "Text" -> DocumentType.TXT
            else -> DocumentType.PDF
        }
        
        // Create a custom layout for the dialog
        val dialogLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        
        // Add filename input
        val fileNameLabel = android.widget.TextView(requireContext()).apply {
            text = "Output file name:"
            textSize = 14f
        }
        dialogLayout.addView(fileNameLabel)
        
        val fileNameInput = EditText(requireContext()).apply {
            setText(baseName)
            hint = "Enter file name"
            setSelectAllOnFocus(true)
        }
        dialogLayout.addView(fileNameInput)
        
        // Add extension preview
        val extensionPreview = android.widget.TextView(requireContext()).apply {
            text = "File will be saved as: ${baseName}.${targetType.extension}"
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, 8, 0, 16)
        }
        dialogLayout.addView(extensionPreview)
        
        // Update extension preview when filename changes
        fileNameInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                extensionPreview.text = "File will be saved as: ${s.toString()}.${targetType.extension}"
            }
        })
        
        // Add OCR checkbox for PDF conversions
        var ocrEnabled = false
        if (targetType == DocumentType.PDF && (sourceType == DocumentType.PPTX || sourceType == DocumentType.DOCX)) {
            val ocrCheckbox = MaterialCheckBox(requireContext()).apply {
                text = "Enable OCR (makes text selectable)"
                isChecked = false
                setOnCheckedChangeListener { _, isChecked ->
                    ocrEnabled = isChecked
                }
            }
            dialogLayout.addView(ocrCheckbox)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Convert File")
            .setView(dialogLayout)
            .setPositiveButton("Convert") { _, _ ->
                val customName = fileNameInput.text.toString().trim()
                if (customName.isNotEmpty()) {
                    convertDocument(customName, ocrEnabled)
                } else {
                    Toast.makeText(context, "Please enter a valid file name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun convertDocument(customOutputName: String? = null, ocrEnabled: Boolean = false) {
        val uri = selectedUri ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnConvert.isEnabled = false
        
        val targetFormatString = binding.spinnerOutputFormat.selectedItem as String
        val targetType = when (targetFormatString) {
            "PDF" -> DocumentType.PDF
            "DOCX" -> DocumentType.DOCX
            "PPTX" -> DocumentType.PPTX
            "Markdown" -> DocumentType.MARKDOWN
            "Text" -> DocumentType.TXT
            else -> DocumentType.PDF
        }
        
        lifecycleScope.launch {
            try {
                val inputFile = withContext(Dispatchers.IO) {
                    FileUtils.copyToCache(requireContext(), uri)
                }
                
                if (inputFile != null) {
                    val options = ConversionOptions(
                        sourceFormat = sourceType,
                        targetFormat = targetType,
                        ocrEnabled = ocrEnabled
                    )
                    
                    val result = documentConverter.convert(inputFile, options, customOutputName)
                    
                    binding.progressBar.visibility = View.GONE
                    binding.btnConvert.isEnabled = true
                    
                    if (result.success && result.outputPath != null) {
                        showConversionResult(File(result.outputPath))
                    } else {
                        Toast.makeText(
                            context,
                            "Conversion failed: ${result.errorMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnConvert.isEnabled = true
                    Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnConvert.isEnabled = true
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showConversionResult(outputFile: File) {
        binding.cardResult.visibility = View.VISIBLE
        binding.textResultFileName.text = outputFile.name
        binding.textResultFileSize.text = FileUtils.formatFileSize(outputFile.length())
        
        binding.btnShareResult.setOnClickListener {
            val mimeType = when (outputFile.extension.lowercase()) {
                "pdf" -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "md" -> "text/markdown"
                "txt" -> "text/plain"
                else -> "*/*"
            }
            ShareUtils.shareFile(requireContext(), outputFile, mimeType)
        }
        
        binding.btnOpenResult.setOnClickListener {
            val mimeType = when (outputFile.extension.lowercase()) {
                "pdf" -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "md" -> "text/markdown"
                "txt" -> "text/plain"
                else -> "*/*"
            }
            ShareUtils.openWith(requireContext(), outputFile, mimeType)
        }
        
        Toast.makeText(context, "Conversion complete!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

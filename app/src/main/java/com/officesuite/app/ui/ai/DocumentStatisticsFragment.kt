package com.officesuite.app.ui.ai

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.officesuite.app.R
import com.officesuite.app.ai.DocumentAnalyzer
import com.officesuite.app.databinding.FragmentDocumentStatisticsBinding
import com.officesuite.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fragment for displaying document statistics and AI-powered analysis
 * Part of Phase 2 AI-Powered Document Intelligence features
 */
class DocumentStatisticsFragment : Fragment() {

    private var _binding: FragmentDocumentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var documentAnalyzer: DocumentAnalyzer
    private var fileUri: Uri? = null
    private var documentText: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        documentAnalyzer = DocumentAnalyzer(requireContext())

        arguments?.getString("file_uri")?.let { uriString ->
            fileUri = Uri.parse(uriString)
        }

        arguments?.getString("document_text")?.let { text ->
            documentText = text
        }

        setupClickListeners()
        loadAndAnalyze()
    }

    private fun setupClickListeners() {
        binding.btnGenerateSummary.setOnClickListener {
            generateSummary()
        }
    }

    private fun loadAndAnalyze() {
        binding.progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // If we have a file URI but no text, extract text
                if (documentText.isBlank() && fileUri != null) {
                    documentText = withContext(Dispatchers.IO) {
                        extractTextFromFile(fileUri!!)
                    }
                }

                // Get file name
                val fileName = fileUri?.let {
                    FileUtils.getFileName(requireContext(), it)
                } ?: "Document"
                binding.textDocumentName.text = fileName

                // Analyze document
                if (documentText.isNotBlank()) {
                    val stats = documentAnalyzer.analyzeDocument(documentText)
                    displayStatistics(stats)
                } else {
                    showError("No text content found in document")
                }

            } catch (e: Exception) {
                showError("Error analyzing document: ${e.message}")
            } finally {
                binding.progressLoading.visibility = View.GONE
            }
        }
    }

    private suspend fun extractTextFromFile(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val cachedFile = FileUtils.copyToCache(requireContext(), uri)
            if (cachedFile == null) return@withContext ""
            
            val extension = cachedFile.extension.lowercase()

            when (extension) {
                "txt", "md" -> cachedFile.readText()
                "docx" -> extractTextFromDocx(cachedFile)
                "pdf" -> extractTextFromPdf(cachedFile)
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractTextFromDocx(file: File): String {
        return try {
            val doc = org.apache.poi.xwpf.usermodel.XWPFDocument(file.inputStream())
            val text = doc.paragraphs.joinToString("\n") { it.text }
            doc.close()
            text
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractTextFromPdf(file: File): String {
        return try {
            val reader = com.itextpdf.kernel.pdf.PdfReader(file)
            val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(reader)
            val text = StringBuilder()
            
            for (i in 1..pdfDoc.numberOfPages) {
                val page = pdfDoc.getPage(i)
                val strategy = com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy()
                text.append(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page, strategy))
                text.append("\n")
            }
            
            pdfDoc.close()
            text.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun displayStatistics(stats: DocumentAnalyzer.DocumentStatistics) {
        binding.apply {
            textWordCount.text = formatNumber(stats.wordCount)
            textCharCount.text = formatNumber(stats.characterCount)
            textSentenceCount.text = formatNumber(stats.sentenceCount)
            textParagraphCount.text = formatNumber(stats.paragraphCount)

            textReadingTime.text = getString(R.string.minutes, stats.readingTimeMinutes)
            textSpeakingTime.text = getString(R.string.minutes, stats.speakingTimeMinutes)

            progressReadability.progress = stats.readabilityScore.toInt()
            textReadabilityScore.text = String.format("%.1f / 100", stats.readabilityScore)
            textReadabilityLevel.text = stats.readabilityLevel

            // Display keywords as chips
            chipGroupKeywords.removeAllViews()
            stats.topKeywords.forEach { keyword ->
                val chip = Chip(requireContext()).apply {
                    text = keyword
                    isClickable = false
                    isCheckable = false
                }
                chipGroupKeywords.addView(chip)
            }
        }
    }

    private fun formatNumber(number: Int): String {
        return String.format("%,d", number)
    }

    private fun generateSummary() {
        if (documentText.isBlank()) {
            showError("No document text to summarize")
            return
        }

        binding.progressLoading.visibility = View.VISIBLE
        binding.btnGenerateSummary.isEnabled = false

        lifecycleScope.launch {
            try {
                val summary = documentAnalyzer.summarizeDocument(documentText)
                displaySummary(summary)
            } catch (e: Exception) {
                showError("Error generating summary: ${e.message}")
            } finally {
                binding.progressLoading.visibility = View.GONE
                binding.btnGenerateSummary.isEnabled = true
            }
        }
    }

    private fun displaySummary(summary: DocumentAnalyzer.DocumentSummary) {
        binding.cardSummary.visibility = View.VISIBLE

        binding.textSummary.text = summary.summary

        // Display key points
        binding.layoutKeyPoints.removeAllViews()
        summary.keyPoints.forEach { point ->
            val textView = TextView(requireContext()).apply {
                text = "• $point"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                setPadding(0, 4, 0, 4)
            }
            binding.layoutKeyPoints.addView(textView)
        }

        // Display action items
        binding.layoutActionItems.removeAllViews()
        if (summary.actionItems.isEmpty()) {
            val textView = TextView(requireContext()).apply {
                text = "No action items detected"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                alpha = 0.6f
            }
            binding.layoutActionItems.addView(textView)
        } else {
            summary.actionItems.forEach { item ->
                val textView = TextView(requireContext()).apply {
                    text = "☐ $item"
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                    setPadding(0, 4, 0, 4)
                }
                binding.layoutActionItems.addView(textView)
            }
        }
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(fileUri: String?, documentText: String? = null): DocumentStatisticsFragment {
            return DocumentStatisticsFragment().apply {
                arguments = Bundle().apply {
                    fileUri?.let { putString("file_uri", it) }
                    documentText?.let { putString("document_text", it) }
                }
            }
        }
    }
}

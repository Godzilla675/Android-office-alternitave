package com.officesuite.app.ui.docx

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
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
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFPictureData
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
        
        setupWebView()
        setupToolbar()
        
        arguments?.getString("file_uri")?.let { uriString ->
            fileUri = Uri.parse(uriString)
            loadDocument()
        }
    }

    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        }
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
                        val htmlContent = withContext(Dispatchers.IO) {
                            convertDocxToHtml(file)
                        }
                        
                        binding.toolbar.title = file.name
                        binding.webView.loadDataWithBaseURL(
                            null,
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                        binding.progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load document: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun convertDocxToHtml(file: File): String {
        val htmlBuilder = StringBuilder()
        documentText.clear()
        
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        margin: 0;
                        padding: 16px;
                        line-height: 1.6;
                        color: #212121;
                        background-color: #ffffff;
                    }
                    p {
                        margin: 0 0 12px 0;
                    }
                    h1 { font-size: 24px; font-weight: bold; margin: 16px 0 8px 0; }
                    h2 { font-size: 20px; font-weight: bold; margin: 14px 0 8px 0; }
                    h3 { font-size: 18px; font-weight: bold; margin: 12px 0 6px 0; }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin: 12px 0;
                    }
                    th, td {
                        border: 1px solid #e0e0e0;
                        padding: 8px 12px;
                        text-align: left;
                    }
                    th {
                        background-color: #f5f5f5;
                        font-weight: 600;
                    }
                    tr:nth-child(even) {
                        background-color: #fafafa;
                    }
                    img {
                        max-width: 100%;
                        height: auto;
                        margin: 8px 0;
                    }
                    .center { text-align: center; }
                    .right { text-align: right; }
                    .justify { text-align: justify; }
                    ul, ol {
                        margin: 8px 0;
                        padding-left: 24px;
                    }
                    li {
                        margin: 4px 0;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        try {
            val document = XWPFDocument(FileInputStream(file))
            
            // Create a map of picture data for embedding
            val pictureMap = mutableMapOf<String, String>()
            for (pictureData in document.allPictures) {
                val base64 = Base64.encodeToString(pictureData.data, Base64.NO_WRAP)
                val mimeType = getMimeType(pictureData)
                pictureMap[pictureData.fileName] = "data:$mimeType;base64,$base64"
            }
            
            // Process body elements in order
            val bodyElements = document.bodyElements
            for (element in bodyElements) {
                when (element) {
                    is org.apache.poi.xwpf.usermodel.XWPFParagraph -> {
                        val paragraphHtml = processParagraph(element, pictureMap)
                        if (paragraphHtml.isNotEmpty()) {
                            htmlBuilder.append(paragraphHtml)
                        }
                    }
                    is org.apache.poi.xwpf.usermodel.XWPFTable -> {
                        htmlBuilder.append(processTable(element))
                    }
                }
            }
            
            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
            htmlBuilder.append("<p style=\"color: red;\">Error reading document: ${escapeHtml(e.message ?: "Unknown error")}</p>")
        }
        
        htmlBuilder.append("</body></html>")
        return htmlBuilder.toString()
    }

    private fun processParagraph(paragraph: org.apache.poi.xwpf.usermodel.XWPFParagraph, pictureMap: Map<String, String>): String {
        val builder = StringBuilder()
        val paragraphText = StringBuilder()
        
        // Determine heading level based on style
        val styleName = paragraph.style ?: ""
        val isHeading = styleName.lowercase().startsWith("heading")
        val headingLevel = when {
            styleName.lowercase() == "heading1" || styleName == "Title" -> 1
            styleName.lowercase() == "heading2" -> 2
            styleName.lowercase() == "heading3" -> 3
            else -> 0
        }
        
        // Get alignment class
        val alignClass = when (paragraph.alignment) {
            ParagraphAlignment.CENTER -> "center"
            ParagraphAlignment.RIGHT -> "right"
            ParagraphAlignment.BOTH, ParagraphAlignment.DISTRIBUTE -> "justify"
            else -> ""
        }
        
        // Process runs
        for (run in paragraph.runs) {
            val text = run.text() ?: ""
            if (text.isEmpty() && run.embeddedPictures.isEmpty()) continue
            
            // Handle embedded pictures in run
            for (picture in run.embeddedPictures) {
                val pictureData = picture.pictureData
                val base64 = Base64.encodeToString(pictureData.data, Base64.NO_WRAP)
                val mimeType = getMimeType(pictureData)
                builder.append("<img src=\"data:$mimeType;base64,$base64\" alt=\"embedded image\" />")
            }
            
            if (text.isNotEmpty()) {
                var styledText = escapeHtml(text)
                
                // Apply formatting
                if (run.isBold) styledText = "<strong>$styledText</strong>"
                if (run.isItalic) styledText = "<em>$styledText</em>"
                if (run.underline != UnderlinePatterns.NONE) styledText = "<u>$styledText</u>"
                if (run.isStrikeThrough) styledText = "<s>$styledText</s>"
                
                // Handle font color
                val color = run.color
                if (color != null && color != "000000") {
                    styledText = "<span style=\"color:#$color\">$styledText</span>"
                }
                
                // Handle font size
                val fontSize = run.fontSize
                if (fontSize > 0 && fontSize != 11) {
                    styledText = "<span style=\"font-size:${fontSize}pt\">$styledText</span>"
                }
                
                paragraphText.append(styledText)
                documentText.append(text)
            }
        }
        
        val content = paragraphText.toString()
        if (content.isEmpty() && builder.isEmpty()) {
            builder.append("<p>&nbsp;</p>")
        } else if (content.isNotEmpty()) {
            val tag = when (headingLevel) {
                1 -> "h1"
                2 -> "h2"
                3 -> "h3"
                else -> "p"
            }
            val classAttr = if (alignClass.isNotEmpty()) " class=\"$alignClass\"" else ""
            builder.append("<$tag$classAttr>$content</$tag>")
            documentText.append("\n")
        }
        
        return builder.toString()
    }

    private fun processTable(table: org.apache.poi.xwpf.usermodel.XWPFTable): String {
        val builder = StringBuilder()
        builder.append("<table>")
        
        var isFirstRow = true
        for (row in table.rows) {
            builder.append("<tr>")
            for (cell in row.tableCells) {
                val tag = if (isFirstRow) "th" else "td"
                val cellText = cell.text ?: ""
                builder.append("<$tag>${escapeHtml(cellText)}</$tag>")
                documentText.append(cellText).append("\t")
            }
            builder.append("</tr>")
            documentText.append("\n")
            isFirstRow = false
        }
        
        builder.append("</table>")
        return builder.toString()
    }

    private fun getMimeType(pictureData: XWPFPictureData): String {
        return when (pictureData.pictureType) {
            org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG -> "image/png"
            org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG -> "image/jpeg"
            org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_GIF -> "image/gif"
            org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_BMP -> "image/bmp"
            else -> "image/png"
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
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

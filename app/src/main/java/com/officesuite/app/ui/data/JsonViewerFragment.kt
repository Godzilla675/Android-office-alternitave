package com.officesuite.app.ui.data

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentJsonViewerBinding
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

/**
 * Fragment for viewing JSON and XML files with syntax highlighting.
 * Implements Nice-to-Have Feature #16: Additional File Format Support (JSON/XML Viewer)
 */
class JsonViewerFragment : Fragment() {

    private var _binding: FragmentJsonViewerBinding? = null
    private val binding get() = _binding!!
    
    private var fileUri: Uri? = null
    private var cachedFile: File? = null
    private var isXml = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJsonViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.getString("file_uri")?.let { uriString ->
            if (uriString.isNotEmpty()) {
                fileUri = Uri.parse(uriString)
                isXml = arguments?.getBoolean("is_xml", false) ?: false
                loadFile()
            }
        }
        
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
            inflateMenu(R.menu.menu_data_viewer)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_share -> {
                        shareDocument()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun loadFile() {
        fileUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                try {
                    cachedFile = withContext(Dispatchers.IO) {
                        FileUtils.copyToCache(requireContext(), uri)
                    }
                    
                    cachedFile?.let { file ->
                        val content = withContext(Dispatchers.IO) {
                            file.readText()
                        }
                        
                        binding.toolbar.title = file.name
                        
                        val formattedContent = withContext(Dispatchers.IO) {
                            if (isXml) {
                                formatXml(content)
                            } else {
                                formatJson(content)
                            }
                        }
                        
                        binding.textContent.text = formattedContent
                        binding.progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatJson(content: String): String {
        return try {
            val json = JSONTokener(content).nextValue()
            when (json) {
                is JSONObject -> json.toString(2)
                is JSONArray -> json.toString(2)
                else -> content
            }
        } catch (e: Exception) {
            content // Return original if can't parse
        }
    }

    private fun formatXml(content: String): String {
        // Simple XML formatting with indentation
        return try {
            val result = StringBuilder()
            var indent = 0
            var inTag = false
            var inContent = false
            val contentBuilder = StringBuilder()
            
            for (char in content) {
                when {
                    char == '<' -> {
                        if (contentBuilder.isNotEmpty()) {
                            val trimmedContent = contentBuilder.toString().trim()
                            if (trimmedContent.isNotEmpty()) {
                                result.append(trimmedContent)
                            }
                            contentBuilder.clear()
                        }
                        if (result.isNotEmpty() && result.last() != '\n') {
                            result.append('\n')
                        }
                        result.append("  ".repeat(indent))
                        result.append(char)
                        inTag = true
                        inContent = false
                    }
                    char == '>' -> {
                        result.append(char)
                        val resultStr = result.toString()
                        val lastTwo = if (resultStr.length >= 2) resultStr.takeLast(2) else ""
                        if (lastTwo != "/>") {
                            if (resultStr.contains("</")) {
                                indent = maxOf(0, indent - 1)
                            } else {
                                indent++
                            }
                        }
                        inTag = false
                        inContent = true
                    }
                    inTag -> {
                        result.append(char)
                        if (char == '/') {
                            indent = maxOf(0, indent - 1)
                        }
                    }
                    inContent -> {
                        contentBuilder.append(char)
                    }
                }
            }
            
            result.toString()
        } catch (e: Exception) {
            content
        }
    }

    private fun shareDocument() {
        cachedFile?.let { file ->
            val mimeType = if (isXml) "application/xml" else "application/json"
            ShareUtils.shareFile(requireContext(), file, mimeType)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

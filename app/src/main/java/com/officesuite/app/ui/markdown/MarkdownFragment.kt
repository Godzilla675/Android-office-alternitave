package com.officesuite.app.ui.markdown

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.officesuite.app.R
import com.officesuite.app.data.repository.PreferencesRepository
import com.officesuite.app.databinding.FragmentMarkdownBinding
import com.officesuite.app.utils.ErrorHandler
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.Result
import com.officesuite.app.utils.ShareUtils
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Fragment for editing and viewing Markdown documents.
 * Features:
 * - WYSIWYG editing with markdown shortcuts
 * - Real-time preview rendering
 * - File save/share functionality
 * - Auto-save functionality
 * - Enhanced error handling
 */
class MarkdownFragment : Fragment() {

    private var _binding: FragmentMarkdownBinding? = null
    private val binding get() = _binding!!
    
    private var fileUri: Uri? = null
    private var cachedFile: File? = null
    private var isEditMode = true
    private lateinit var markwon: Markwon
    private lateinit var preferencesRepository: PreferencesRepository
    
    // Auto-save related
    private var autoSaveJob: Job? = null
    private var hasUnsavedChanges = false
    private var lastSavedContent: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarkdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesRepository = PreferencesRepository(requireContext())
        setupMarkwon()
        setupToolbar()
        setupClickListeners()
        setupAutoSave()
        
        // Restore saved content if any
        savedInstanceState?.getString("saved_content")?.let { content ->
            binding.editContent.setText(content)
            lastSavedContent = content
        }
        
        arguments?.getString("file_uri")?.let { uriString ->
            if (uriString.isNotEmpty()) {
                fileUri = Uri.parse(uriString)
                loadFile()
            } else {
                // New file mode
                binding.toolbar.title = "New Document"
                showEditMode()
            }
        } ?: run {
            // Check for template content
            arguments?.getString("template_content")?.let { templateContent ->
                binding.editContent.setText(templateContent)
                lastSavedContent = templateContent
                arguments?.getString("template_name")?.let { templateName ->
                    binding.toolbar.title = templateName
                }
                showEditMode()
            } ?: run {
                binding.toolbar.title = "New Document"
                showEditMode()
            }
        }
    }
    
    private fun setupAutoSave() {
        binding.editContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentContent = s?.toString() ?: ""
                if (currentContent != lastSavedContent) {
                    hasUnsavedChanges = true
                    scheduleAutoSave()
                }
            }
        })
    }
    
    private fun scheduleAutoSave() {
        if (!preferencesRepository.isAutoSaveEnabled) return
        
        autoSaveJob?.cancel()
        autoSaveJob = lifecycleScope.launch {
            delay(preferencesRepository.autoSaveIntervalSeconds * 1000L)
            if (hasUnsavedChanges) {
                performAutoSave()
            }
        }
    }
    
    private suspend fun performAutoSave() {
        val content = binding.editContent.text.toString()
        if (content.isEmpty()) return
        
        try {
            if (cachedFile == null) {
                // Create auto-save file for new documents
                val timestamp = System.currentTimeMillis()
                cachedFile = File(FileUtils.getOutputDirectory(requireContext()), "autosave_$timestamp.md")
            }
            
            withContext(Dispatchers.IO) {
                FileOutputStream(cachedFile!!).use { out ->
                    out.write(content.toByteArray())
                }
            }
            
            lastSavedContent = content
            hasUnsavedChanges = false
        } catch (e: Exception) {
            // Silent fail for auto-save, don't disturb user
            e.printStackTrace()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current content for configuration changes
        _binding?.let {
            outState.putString("saved_content", it.editContent.text.toString())
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Auto-save when leaving fragment
        if (hasUnsavedChanges && preferencesRepository.isAutoSaveEnabled) {
            lifecycleScope.launch {
                performAutoSave()
            }
        }
    }

    private fun setupMarkwon() {
        markwon = Markwon.builder(requireContext())
            .usePlugin(TablePlugin.create(requireContext()))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .build()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
                @Suppress("DEPRECATION")
                requireActivity().onBackPressed()
            }
            inflateMenu(R.menu.menu_markdown)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_toggle_mode -> {
                        toggleMode()
                        true
                    }
                    R.id.action_save -> {
                        saveFile()
                        true
                    }
                    R.id.action_share -> {
                        shareDocument()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabToggle.setOnClickListener {
            toggleMode()
        }
        
        // Markdown formatting buttons
        binding.btnBold.setOnClickListener {
            insertMarkdown("**", "**")
        }
        binding.btnItalic.setOnClickListener {
            insertMarkdown("*", "*")
        }
        binding.btnHeading.setOnClickListener {
            insertMarkdown("# ", "")
        }
        binding.btnList.setOnClickListener {
            insertMarkdown("- ", "")
        }
        binding.btnLink.setOnClickListener {
            insertMarkdown("[", "](url)")
        }
        binding.btnCode.setOnClickListener {
            insertMarkdown("`", "`")
        }
    }

    private fun insertMarkdown(prefix: String, suffix: String) {
        val start = binding.editContent.selectionStart
        val end = binding.editContent.selectionEnd
        val text = binding.editContent.text
        
        if (start == end) {
            text?.insert(start, prefix + suffix)
            binding.editContent.setSelection(start + prefix.length)
        } else {
            text?.insert(end, suffix)
            text?.insert(start, prefix)
            binding.editContent.setSelection(start + prefix.length, end + prefix.length)
        }
    }

    private fun loadFile() {
        fileUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                val result = Result.runCatchingSuspend {
                    val file = withContext(Dispatchers.IO) {
                        FileUtils.copyToCache(requireContext(), uri)
                    }
                    
                    if (file != null) {
                        val content = withContext(Dispatchers.IO) {
                            file.readText()
                        }
                        Pair(file, content)
                    } else {
                        throw IllegalStateException("Could not read file")
                    }
                }
                
                result.onSuccess { (file, content) ->
                    cachedFile = file
                    lastSavedContent = content
                    binding.toolbar.title = file.name
                    binding.editContent.setText(content)
                    renderPreview(content)
                    binding.progressBar.visibility = View.GONE
                    showPreviewMode()
                }.onError { error ->
                    binding.progressBar.visibility = View.GONE
                    ErrorHandler.showErrorToast(requireContext(), error.exception)
                }
            }
        }
    }

    private fun toggleMode() {
        if (isEditMode) {
            showPreviewMode()
        } else {
            showEditMode()
        }
    }

    private fun showEditMode() {
        isEditMode = true
        binding.editContent.visibility = View.VISIBLE
        binding.scrollPreview.visibility = View.GONE
        binding.formattingToolbar.visibility = View.VISIBLE
        binding.fabToggle.setImageResource(R.drawable.ic_preview)
    }

    private fun showPreviewMode() {
        isEditMode = false
        val content = binding.editContent.text.toString()
        renderPreview(content)
        binding.editContent.visibility = View.GONE
        binding.scrollPreview.visibility = View.VISIBLE
        binding.formattingToolbar.visibility = View.GONE
        binding.fabToggle.setImageResource(R.drawable.ic_edit)
    }

    private fun renderPreview(content: String) {
        markwon.setMarkdown(binding.textPreview, content)
    }

    private fun saveFile() {
        val content = binding.editContent.text.toString()
        
        lifecycleScope.launch {
            val result = Result.runCatchingSuspend {
                if (cachedFile == null) {
                    // Create new file
                    cachedFile = File(FileUtils.getOutputDirectory(requireContext()), "document.md")
                }
                
                withContext(Dispatchers.IO) {
                    FileOutputStream(cachedFile!!).use { out ->
                        out.write(content.toByteArray())
                    }
                }
            }
            
            result.onSuccess {
                lastSavedContent = content
                hasUnsavedChanges = false
                Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
            }.onError { error ->
                ErrorHandler.showErrorToast(requireContext(), error.exception)
            }
        }
    }

    private fun shareDocument() {
        val content = binding.editContent.text.toString()
        ShareUtils.shareText(requireContext(), content, "Markdown Document")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoSaveJob?.cancel()
        _binding = null
    }
}

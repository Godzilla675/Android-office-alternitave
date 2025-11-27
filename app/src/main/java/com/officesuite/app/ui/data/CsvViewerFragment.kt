package com.officesuite.app.ui.data

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentCsvViewerBinding
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Fragment for viewing CSV files.
 * Implements Nice-to-Have Feature #16: Additional File Format Support (CSV Viewer)
 */
class CsvViewerFragment : Fragment() {

    private var _binding: FragmentCsvViewerBinding? = null
    private val binding get() = _binding!!
    
    private var fileUri: Uri? = null
    private var cachedFile: File? = null
    private var csvData: List<List<String>> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCsvViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.getString("file_uri")?.let { uriString ->
            if (uriString.isNotEmpty()) {
                fileUri = Uri.parse(uriString)
                loadCsv()
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

    private fun loadCsv() {
        fileUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                try {
                    cachedFile = withContext(Dispatchers.IO) {
                        FileUtils.copyToCache(requireContext(), uri)
                    }
                    
                    cachedFile?.let { file ->
                        csvData = withContext(Dispatchers.IO) {
                            parseCsv(file)
                        }
                        
                        binding.toolbar.title = file.name
                        displayCsv()
                        binding.progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parseCsv(file: File): List<List<String>> {
        val result = mutableListOf<List<String>>()
        
        BufferedReader(InputStreamReader(file.inputStream())).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    val cells = parseCsvLine(it)
                    result.add(cells)
                }
            }
        }
        
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentValue = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(currentValue.toString().trim())
                    currentValue = StringBuilder()
                }
                else -> currentValue.append(char)
            }
        }
        result.add(currentValue.toString().trim())
        
        return result
    }

    private fun displayCsv() {
        if (csvData.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerCsv.visibility = View.GONE
            return
        }
        
        binding.emptyState.visibility = View.GONE
        binding.recyclerCsv.visibility = View.VISIBLE
        
        val adapter = CsvAdapter(csvData)
        binding.recyclerCsv.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
        
        // Show row count
        binding.textInfo.text = "${csvData.size} rows, ${csvData.firstOrNull()?.size ?: 0} columns"
        binding.textInfo.visibility = View.VISIBLE
    }

    private fun shareDocument() {
        cachedFile?.let { file ->
            ShareUtils.shareFile(requireContext(), file, "text/csv")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

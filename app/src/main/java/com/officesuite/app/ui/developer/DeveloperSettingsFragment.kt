package com.officesuite.app.ui.developer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.officesuite.app.R
import com.officesuite.app.analytics.UsageAnalyticsManager
import com.officesuite.app.databinding.FragmentDeveloperSettingsBinding
import com.officesuite.app.developer.BetaFeature
import com.officesuite.app.developer.CommandPalette
import com.officesuite.app.developer.DebugManager
import com.officesuite.app.developer.ShortcutManager
import com.officesuite.app.performance.PerformanceMonitor
import com.officesuite.app.search.DocumentIndexingService
import com.officesuite.app.utils.MemoryManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Developer Settings Fragment.
 * Implements Technical Improvements Phase 2 - Developer & Power User Features.
 * 
 * Features:
 * - Debug mode toggle
 * - Beta features management
 * - Performance monitoring
 * - Command palette access
 * - Keyboard shortcuts management
 * - Usage analytics dashboard
 * - Document index management
 */
class DeveloperSettingsFragment : Fragment() {

    private var _binding: FragmentDeveloperSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var betaFeaturesAdapter: BetaFeaturesAdapter
    private lateinit var analyticsManager: UsageAnalyticsManager
    private lateinit var indexingService: DocumentIndexingService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeveloperSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize managers
        DebugManager.init(requireContext())
        analyticsManager = UsageAnalyticsManager.getInstance(requireContext())
        indexingService = DocumentIndexingService.getInstance(requireContext())

        setupToolbar()
        setupDebugMode()
        setupPerformance()
        setupBetaFeatures()
        setupCommandPalette()
        setupAnalytics()
        setupDocumentIndex()
        setupDebugInfo()

        // Start performance monitoring
        PerformanceMonitor.startMonitoring(requireContext())
        observePerformanceMetrics()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupDebugMode() {
        binding.switchDebugMode.isChecked = DebugManager.isDebugMode
        binding.switchDebugMode.setOnCheckedChangeListener { _, isChecked ->
            DebugManager.isDebugMode = isChecked
            Toast.makeText(
                context,
                if (isChecked) R.string.debug_mode_enabled else R.string.debug_mode_disabled,
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnExportLogs.setOnClickListener {
            exportLogs()
        }

        binding.btnClearLogs.setOnClickListener {
            DebugManager.clearLogs()
            Toast.makeText(context, R.string.logs_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPerformance() {
        binding.btnClearCache.setOnClickListener {
            MemoryManager.clearCache()
            Toast.makeText(context, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
            updateCacheStats()
        }

        updateCacheStats()
    }

    private fun updateCacheStats() {
        val stats = MemoryManager.getStats()
        binding.textCacheStats.text = "Cache: ${stats.currentSize}KB / ${stats.maxSize}KB (${stats.hitRate.toInt()}% hit rate)"
    }

    private fun observePerformanceMetrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            PerformanceMonitor.performanceMetrics.collectLatest { metrics ->
                binding.textMemoryUsage.text = "Memory: ${metrics.heapUsedMb}MB / ${metrics.heapMaxMb}MB (${(metrics.heapUsedPercent * 100).toInt()}%)"
                binding.textFps.text = "FPS: ${metrics.fps.toInt()}"
            }
        }
    }

    private fun setupBetaFeatures() {
        val features = DebugManager.getAllBetaFeatures()

        betaFeaturesAdapter = BetaFeaturesAdapter(features) { feature, isEnabled ->
            DebugManager.setBetaFeatureEnabled(feature.id, isEnabled)
            Toast.makeText(
                context,
                getString(
                    if (isEnabled) R.string.beta_feature_enabled else R.string.beta_feature_disabled,
                    feature.name
                ),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.recyclerBetaFeatures.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = betaFeaturesAdapter
        }
    }

    private fun setupCommandPalette() {
        binding.btnOpenCommandPalette.setOnClickListener {
            CommandPalette.getInstance(requireContext()).show { command ->
                Toast.makeText(context, "Command: ${command.name}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnViewShortcuts.setOnClickListener {
            showShortcutsDialog()
        }
    }

    private fun showShortcutsDialog() {
        val shortcuts = ShortcutManager.getInstance(requireContext()).getAllShortcuts()
        val shortcutManager = ShortcutManager.getInstance(requireContext())

        val shortcutTexts = shortcuts.map { shortcut ->
            "${shortcut.name}: ${shortcutManager.getShortcutText(shortcut.keyCombo)}"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.keyboard_shortcuts)
            .setItems(shortcutTexts, null)
            .setPositiveButton(R.string.done, null)
            .setNeutralButton(R.string.reset_all_shortcuts) { _, _ ->
                shortcutManager.resetAllShortcuts()
                Toast.makeText(context, "Shortcuts reset to defaults", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun setupAnalytics() {
        updateAnalyticsSummary()

        binding.btnExportAnalytics.setOnClickListener {
            val json = analyticsManager.exportAnalytics()
            // In a real app, you would save this to a file
            Toast.makeText(context, R.string.analytics_exported, Toast.LENGTH_SHORT).show()
        }

        binding.btnClearAnalytics.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_analytics)
                .setMessage("This will permanently delete all usage analytics data.")
                .setPositiveButton("Clear") { _, _ ->
                    analyticsManager.clearAllData()
                    updateAnalyticsSummary()
                    Toast.makeText(context, R.string.analytics_cleared, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun updateAnalyticsSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            analyticsManager.analytics.collectLatest { summary ->
                binding.textAnalyticsSummary.text = 
                    "Documents: ${summary.documentsViewed} | Scans: ${summary.scans} | Words: ${summary.wordsWritten}"
            }
        }
    }

    private fun setupDocumentIndex() {
        updateIndexStats()

        viewLifecycleOwner.lifecycleScope.launch {
            indexingService.indexStats.collectLatest { stats ->
                binding.textIndexStats.text = 
                    "Indexed: ${stats.totalDocuments} documents | ${stats.totalWords} words"
            }
        }

        binding.btnClearIndex.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_index)
                .setMessage("This will delete the document search index. Documents will need to be re-indexed.")
                .setPositiveButton("Clear") { _, _ ->
                    indexingService.clearIndex()
                    updateIndexStats()
                    Toast.makeText(context, "Index cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun updateIndexStats() {
        val stats = indexingService.indexStats.value
        binding.textIndexStats.text = 
            "Indexed: ${stats.totalDocuments} documents | ${stats.totalWords} words"
    }

    private fun setupDebugInfo() {
        binding.btnShowDebugInfo.setOnClickListener {
            val debugInfo = DebugManager.getDebugInfo(requireContext())
            AlertDialog.Builder(requireContext())
                .setTitle("Debug Information")
                .setMessage(debugInfo)
                .setPositiveButton(R.string.done, null)
                .setNeutralButton("Copy") { _, _ ->
                    val clipboard = requireContext().getSystemService(android.content.ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Debug Info", debugInfo))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun exportLogs() {
        try {
            val logFile = DebugManager.exportLogs(requireContext())
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                logFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, getString(R.string.export_logs)))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export logs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PerformanceMonitor.stopMonitoring()
        _binding = null
    }
}

/**
 * Adapter for beta features list.
 */
class BetaFeaturesAdapter(
    private val features: List<BetaFeature>,
    private val onToggle: (BetaFeature, Boolean) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<BetaFeaturesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val name: android.widget.TextView = view.findViewById(R.id.textFeatureName)
        val description: android.widget.TextView = view.findViewById(R.id.textFeatureDescription)
        val category: android.widget.TextView = view.findViewById(R.id.textFeatureCategory)
        val badge: android.widget.TextView = view.findViewById(R.id.badgeExperimental)
        val switch: com.google.android.material.switchmaterial.SwitchMaterial = view.findViewById(R.id.switchFeature)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_beta_feature, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feature = features[position]
        holder.name.text = feature.name
        holder.description.text = feature.description
        holder.category.text = feature.category.displayName
        holder.badge.visibility = if (feature.isExperimental) View.VISIBLE else View.GONE
        holder.switch.isChecked = feature.isEnabled
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            onToggle(feature, isChecked)
        }
    }

    override fun getItemCount() = features.size
}

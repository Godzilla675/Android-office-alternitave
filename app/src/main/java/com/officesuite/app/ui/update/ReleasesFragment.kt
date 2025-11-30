package com.officesuite.app.ui.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.BuildConfig
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentReleasesBinding
import com.officesuite.app.databinding.ItemReleaseBinding
import com.officesuite.app.utils.UpdateChecker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragment that displays a list of all app releases from GitHub.
 */
class ReleasesFragment : Fragment() {

    private var _binding: FragmentReleasesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ReleasesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReleasesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        loadReleases()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ReleasesAdapter { release ->
            openRelease(release)
        }
        binding.recyclerReleases.adapter = adapter
        binding.recyclerReleases.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun loadReleases() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val releases = UpdateChecker.getAllReleases()
                
                showLoading(false)
                
                if (releases.isEmpty()) {
                    showEmpty(true)
                } else {
                    showEmpty(false)
                    adapter.submitList(releases)
                }
            } catch (e: Exception) {
                showLoading(false)
                showEmpty(true)
                Toast.makeText(
                    requireContext(),
                    R.string.update_check_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerReleases.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun showEmpty(show: Boolean) {
        binding.emptyContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerReleases.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun openRelease(release: UpdateChecker.ReleaseInfo) {
        // Find APK asset or fall back to HTML URL
        val apkAsset = release.assets.find { it.name.endsWith(".apk") }
        val url = apkAsset?.downloadUrl ?: release.htmlUrl
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.update_check_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * RecyclerView Adapter for releases with DiffUtil for efficient updates
     */
    inner class ReleasesAdapter(
        private val onDownloadClick: (UpdateChecker.ReleaseInfo) -> Unit
    ) : RecyclerView.Adapter<ReleasesAdapter.ReleaseViewHolder>() {
        
        private var releases: List<UpdateChecker.ReleaseInfo> = emptyList()
        
        fun submitList(newReleases: List<UpdateChecker.ReleaseInfo>) {
            val diffCallback = ReleasesDiffCallback(releases, newReleases)
            val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)
            releases = newReleases
            diffResult.dispatchUpdatesTo(this)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReleaseViewHolder {
            val binding = ItemReleaseBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ReleaseViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ReleaseViewHolder, position: Int) {
            holder.bind(releases[position])
        }
        
        override fun getItemCount(): Int = releases.size
        
        inner class ReleaseViewHolder(
            private val binding: ItemReleaseBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(release: UpdateChecker.ReleaseInfo) {
                val version = release.tagName.removePrefix("v")
                
                // Version badge
                binding.chipVersion.text = "v$version"
                
                // Current version indicator
                val currentVersion = BuildConfig.VERSION_NAME
                binding.chipCurrent.visibility = if (version == currentVersion) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                
                // Date
                binding.textDate.text = formatDate(release.publishedAt)
                
                // Release name
                binding.textReleaseName.text = release.name.ifBlank { "Release $version" }
                
                // Release notes
                val notes = release.body.ifBlank { "No release notes available." }
                binding.textReleaseNotes.text = notes
                
                // Download button
                binding.btnDownload.setOnClickListener {
                    onDownloadClick(release)
                }
            }
            
            private fun formatDate(dateString: String): String {
                if (dateString.isBlank()) return ""
                
                return try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val date = inputFormat.parse(dateString)
                    date?.let { outputFormat.format(it) } ?: dateString
                } catch (e: Exception) {
                    dateString.take(10) // Fall back to showing just the date part
                }
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient RecyclerView updates
     */
    private class ReleasesDiffCallback(
        private val oldList: List<UpdateChecker.ReleaseInfo>,
        private val newList: List<UpdateChecker.ReleaseInfo>
    ) : androidx.recyclerview.widget.DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].tagName == newList[newItemPosition].tagName
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

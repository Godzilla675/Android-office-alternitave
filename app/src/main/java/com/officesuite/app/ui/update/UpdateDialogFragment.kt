package com.officesuite.app.ui.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.officesuite.app.R
import com.officesuite.app.databinding.DialogUpdateBinding
import com.officesuite.app.utils.UpdateChecker

/**
 * Bottom sheet dialog fragment that shows update information to the user.
 */
class UpdateDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogUpdateBinding? = null
    private val binding get() = _binding!!

    private var updateInfo: UpdateChecker.UpdateInfo? = null

    companion object {
        private const val ARG_CURRENT_VERSION = "current_version"
        private const val ARG_LATEST_VERSION = "latest_version"
        private const val ARG_RELEASE_NOTES = "release_notes"
        private const val ARG_DOWNLOAD_URL = "download_url"
        private const val ARG_RELEASE_NAME = "release_name"
        private const val ARG_HTML_URL = "html_url"

        fun newInstance(updateInfo: UpdateChecker.UpdateInfo): UpdateDialogFragment {
            return UpdateDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_VERSION, updateInfo.currentVersion)
                    putString(ARG_LATEST_VERSION, updateInfo.latestVersion)
                    putString(ARG_RELEASE_NOTES, updateInfo.releaseNotes)
                    putString(ARG_DOWNLOAD_URL, updateInfo.downloadUrl)
                    putString(ARG_RELEASE_NAME, updateInfo.releaseName)
                    putString(ARG_HTML_URL, updateInfo.htmlUrl)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Extract update info from arguments
        arguments?.let { args ->
            updateInfo = UpdateChecker.UpdateInfo(
                currentVersion = args.getString(ARG_CURRENT_VERSION, ""),
                latestVersion = args.getString(ARG_LATEST_VERSION, ""),
                releaseNotes = args.getString(ARG_RELEASE_NOTES, ""),
                downloadUrl = args.getString(ARG_DOWNLOAD_URL, ""),
                releaseName = args.getString(ARG_RELEASE_NAME, ""),
                htmlUrl = args.getString(ARG_HTML_URL, "")
            )
        }

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        updateInfo?.let { info ->
            // Set version info
            binding.versionInfo.text = getString(
                R.string.version_info_placeholder,
                info.currentVersion,
                info.latestVersion
            )

            // Set release notes
            val notes = info.releaseNotes.ifBlank { 
                "Bug fixes and performance improvements." 
            }
            binding.releaseNotes.text = notes
        }
    }

    private fun setupClickListeners() {
        // Update Now button - opens the download URL
        binding.btnUpdate.setOnClickListener {
            updateInfo?.let { info ->
                try {
                    Toast.makeText(
                        requireContext(),
                        R.string.downloading_update,
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to HTML URL if download URL doesn't work
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.htmlUrl))
                        startActivity(intent)
                    } catch (e2: Exception) {
                        Toast.makeText(
                            requireContext(),
                            R.string.update_check_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                dismiss()
            }
        }

        // Remind Me Later button - just dismiss the dialog
        binding.btnLater.setOnClickListener {
            dismiss()
        }

        // Skip This Version button - dismiss and remember not to show this version again
        binding.btnSkip.setOnClickListener {
            updateInfo?.let { info ->
                UpdateChecker.dismissVersion(requireContext(), info.latestVersion)
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

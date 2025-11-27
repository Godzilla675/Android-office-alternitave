package com.officesuite.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.officesuite.app.R
import com.officesuite.app.data.repository.FontSize
import com.officesuite.app.data.repository.PreferencesRepository
import com.officesuite.app.databinding.FragmentSettingsBinding

/**
 * Settings Fragment for managing app preferences.
 * Implements Nice-to-Have Feature #14: Customization & Themes
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefsRepository: PreferencesRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefsRepository = PreferencesRepository(requireContext())
        
        setupToolbar()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
            title = getString(R.string.settings)
        }
    }

    private fun loadCurrentSettings() {
        // Dark mode
        binding.switchDarkMode.isChecked = prefsRepository.isDarkMode
        
        // Reading mode
        binding.switchReadingMode.isChecked = prefsRepository.isReadingMode
        
        // Auto-save
        binding.switchAutoSave.isChecked = prefsRepository.isAutoSaveEnabled
        
        // Font size
        updateFontSizeSelection(prefsRepository.fontSize)
    }

    private fun setupListeners() {
        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefsRepository.isDarkMode = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        
        // Reading mode toggle
        binding.switchReadingMode.setOnCheckedChangeListener { _, isChecked ->
            prefsRepository.isReadingMode = isChecked
        }
        
        // Auto-save toggle
        binding.switchAutoSave.setOnCheckedChangeListener { _, isChecked ->
            prefsRepository.isAutoSaveEnabled = isChecked
        }
        
        // Font size buttons
        binding.btnFontSmall.setOnClickListener {
            setFontSize(FontSize.SMALL)
        }
        binding.btnFontMedium.setOnClickListener {
            setFontSize(FontSize.MEDIUM)
        }
        binding.btnFontLarge.setOnClickListener {
            setFontSize(FontSize.LARGE)
        }
        binding.btnFontExtraLarge.setOnClickListener {
            setFontSize(FontSize.EXTRA_LARGE)
        }
        
        // Clear recent files
        binding.btnClearRecent.setOnClickListener {
            prefsRepository.clearRecentFiles()
            android.widget.Toast.makeText(context, "Recent files cleared", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setFontSize(size: FontSize) {
        prefsRepository.fontSize = size
        updateFontSizeSelection(size)
    }

    private fun updateFontSizeSelection(selectedSize: FontSize) {
        val buttons = listOf(
            binding.btnFontSmall to FontSize.SMALL,
            binding.btnFontMedium to FontSize.MEDIUM,
            binding.btnFontLarge to FontSize.LARGE,
            binding.btnFontExtraLarge to FontSize.EXTRA_LARGE
        )
        
        buttons.forEach { (button, size) ->
            button.isSelected = size == selectedSize
            button.alpha = if (size == selectedSize) 1f else 0.6f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

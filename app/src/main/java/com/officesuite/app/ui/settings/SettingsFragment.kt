package com.officesuite.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.officesuite.app.R
import com.officesuite.app.data.repository.ColorBlindMode
import com.officesuite.app.data.repository.FontSize
import com.officesuite.app.data.repository.PreferencesRepository
import com.officesuite.app.databinding.FragmentSettingsBinding

/**
 * Settings Fragment for managing app preferences.
 * Implements Nice-to-Have Feature #14: Customization & Themes
 * Implements Medium Priority Feature #12: Accessibility Enhancements
 * Implements Medium Priority Feature #6: Enhanced Writing Tools
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
                @Suppress("DEPRECATION")
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
        
        // Accessibility settings
        binding.switchReadingRuler.isChecked = prefsRepository.isReadingRulerEnabled
        binding.switchDyslexiaFont.isChecked = prefsRepository.isDyslexiaFontEnabled
        
        // Color blind mode
        updateColorBlindModeSelection(prefsRepository.colorBlindMode)
        
        // Writing tools
        binding.switchFocusMode.isChecked = prefsRepository.isFocusModeEnabled
        binding.switchTypewriterMode.isChecked = prefsRepository.isTypewriterModeEnabled
        binding.switchShowReadingTime.isChecked = prefsRepository.showReadingTimeEstimate
        
        // Word count goal
        val wordGoal = prefsRepository.wordCountGoal
        if (wordGoal > 0) {
            binding.editWordGoal.setText(wordGoal.toString())
        }
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
        
        // Accessibility settings
        binding.switchReadingRuler.setOnCheckedChangeListener { _, isChecked ->
            prefsRepository.isReadingRulerEnabled = isChecked
        }
        
        binding.switchDyslexiaFont.setOnCheckedChangeListener { _, isChecked ->
            prefsRepository.isDyslexiaFontEnabled = isChecked
        }
        
        // Color blind mode buttons
        binding.btnColorBlindNone.setOnClickListener {
            setColorBlindMode(ColorBlindMode.NONE)
        }
        binding.btnColorBlindProtanopia.setOnClickListener {
            setColorBlindMode(ColorBlindMode.PROTANOPIA)
        }
        binding.btnColorBlindDeuteranopia.setOnClickListener {
            setColorBlindMode(ColorBlindMode.DEUTERANOPIA)
        }
        binding.btnColorBlindTritanopia.setOnClickListener {
            setColorBlindMode(ColorBlindMode.TRITANOPIA)
        }
        
        // Writing tools
        binding.switchFocusMode.setOnCheckedChangeListener { _, isChecked ->
            prefsRepository.isFocusModeEnabled = isChecked
        }
        
        binding.switchTypewriterMode.setOnCheckedChangeListener { _, isChecked ->
            prefsRepository.isTypewriterModeEnabled = isChecked
        }
        
        binding.switchShowReadingTime.setOnCheckedChangeListener { _, isChecked ->
            prefsRepository.showReadingTimeEstimate = isChecked
        }
        
        // Word count goal
        binding.btnSetWordGoal.setOnClickListener {
            val goalText = binding.editWordGoal.text.toString()
            val goal = goalText.toIntOrNull() ?: 0
            prefsRepository.wordCountGoal = goal
            android.widget.Toast.makeText(
                context, 
                if (goal > 0) "Word goal set to $goal" else "Word goal cleared", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        
        // Platform Features navigation
        binding.cardPlatformFeatures.setOnClickListener {
            findNavController().navigate(R.id.platformFeaturesFragment)
        }
        
        // Developer settings
        binding.btnDeveloperSettings.setOnClickListener {
            findNavController().navigate(R.id.developerSettingsFragment)
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

    private fun setColorBlindMode(mode: ColorBlindMode) {
        prefsRepository.colorBlindMode = mode
        updateColorBlindModeSelection(mode)
    }

    private fun updateColorBlindModeSelection(selectedMode: ColorBlindMode) {
        val buttons = listOf(
            binding.btnColorBlindNone to ColorBlindMode.NONE,
            binding.btnColorBlindProtanopia to ColorBlindMode.PROTANOPIA,
            binding.btnColorBlindDeuteranopia to ColorBlindMode.DEUTERANOPIA,
            binding.btnColorBlindTritanopia to ColorBlindMode.TRITANOPIA
        )
        
        buttons.forEach { (button, mode) ->
            button.isSelected = mode == selectedMode
            button.alpha = if (mode == selectedMode) 1f else 0.6f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

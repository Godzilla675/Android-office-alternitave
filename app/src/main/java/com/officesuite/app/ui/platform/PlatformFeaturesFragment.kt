package com.officesuite.app.ui.platform

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentPlatformFeaturesBinding
import com.officesuite.app.platform.DesktopModeManager
import com.officesuite.app.platform.DigitalWellbeingManager
import com.officesuite.app.platform.NearbyShareManager
import com.officesuite.app.platform.QuickSettingsManager
import com.officesuite.app.platform.VoiceCommandManager

/**
 * Fragment for managing Platform-Specific Features.
 * Implements Phase 2 Platform-Specific Features (Section 25 & 26):
 * - Digital Wellbeing usage tracking
 * - Quick Settings Tile configuration
 * - Voice command settings
 * - Nearby Share preferences
 * - Desktop Mode / DeX / ChromeOS support
 */
class PlatformFeaturesFragment : Fragment() {
    
    private var _binding: FragmentPlatformFeaturesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var digitalWellbeing: DigitalWellbeingManager
    private lateinit var quickSettings: QuickSettingsManager
    private lateinit var voiceCommands: VoiceCommandManager
    private lateinit var nearbyShare: NearbyShareManager
    private lateinit var desktopMode: DesktopModeManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlatformFeaturesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeManagers()
        setupToolbar()
        setupDigitalWellbeing()
        setupQuickSettings()
        setupVoiceCommands()
        setupNearbyShare()
        setupDesktopMode()
        updatePlatformInfo()
    }
    
    private fun initializeManagers() {
        digitalWellbeing = DigitalWellbeingManager(requireContext())
        quickSettings = QuickSettingsManager(requireContext())
        voiceCommands = VoiceCommandManager(requireContext())
        nearbyShare = NearbyShareManager(requireContext())
        desktopMode = DesktopModeManager(requireContext())
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            title = getString(R.string.platform_features)
        }
    }
    
    private fun setupDigitalWellbeing() {
        // Update usage display
        updateUsageDisplay()
        
        // Daily limit toggle
        binding.switchDailyLimit.isChecked = digitalWellbeing.isLimitEnabled()
        binding.switchDailyLimit.setOnCheckedChangeListener { _, isChecked ->
            digitalWellbeing.setLimitEnabled(isChecked)
            binding.layoutDailyLimitSettings.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateUsageDisplay()
        }
        binding.layoutDailyLimitSettings.visibility = 
            if (digitalWellbeing.isLimitEnabled()) View.VISIBLE else View.GONE
        
        // Daily limit slider
        binding.sliderDailyLimit.value = digitalWellbeing.getDailyLimit().toFloat().coerceIn(15f, 480f)
        binding.sliderDailyLimit.addOnChangeListener { _, value, _ ->
            digitalWellbeing.setDailyLimit(value.toInt())
            updateDailyLimitLabel(value.toInt())
        }
        updateDailyLimitLabel(digitalWellbeing.getDailyLimit())
        
        // Break reminder
        val reminderSettings = digitalWellbeing.getReminderSettings()
        binding.switchBreakReminder.isChecked = reminderSettings.breakReminderEnabled
        binding.switchBreakReminder.setOnCheckedChangeListener { _, isChecked ->
            digitalWellbeing.setReminderSettings(
                reminderSettings.copy(breakReminderEnabled = isChecked)
            )
        }
        
        // View detailed stats button
        binding.btnViewStats.setOnClickListener {
            showDetailedStats()
        }
        
        // Reset stats button
        binding.btnResetStats.setOnClickListener {
            digitalWellbeing.resetDailyStats()
            updateUsageDisplay()
            Toast.makeText(context, R.string.stats_reset, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateUsageDisplay() {
        val todayUsage = digitalWellbeing.getTodayUsage()
        val weeklyUsage = digitalWellbeing.getWeeklyUsage()
        val limit = digitalWellbeing.getDailyLimit()
        
        binding.textTodayUsage.text = digitalWellbeing.formatTime(todayUsage)
        binding.textWeeklyUsage.text = digitalWellbeing.formatTime(weeklyUsage)
        binding.textAverageUsage.text = digitalWellbeing.formatTime(weeklyUsage / 7)
        
        if (digitalWellbeing.isLimitEnabled()) {
            val percentage = digitalWellbeing.getUsagePercentage()
            binding.progressDailyUsage.progress = percentage.toInt()
            binding.textRemainingTime.text = getString(
                R.string.remaining_time, 
                digitalWellbeing.formatTime(digitalWellbeing.getRemainingTime())
            )
            binding.progressDailyUsage.visibility = View.VISIBLE
            binding.textRemainingTime.visibility = View.VISIBLE
            
            // Update color based on usage
            val color = when {
                percentage >= 90 -> R.color.error
                percentage >= 75 -> R.color.warning
                else -> R.color.colorPrimary
            }
            binding.progressDailyUsage.setIndicatorColor(
                resources.getColor(color, null)
            )
        } else {
            binding.progressDailyUsage.visibility = View.GONE
            binding.textRemainingTime.visibility = View.GONE
        }
    }
    
    private fun updateDailyLimitLabel(minutes: Int) {
        binding.textDailyLimitValue.text = digitalWellbeing.formatTime(minutes)
    }
    
    private fun showDetailedStats() {
        val stats = digitalWellbeing.getDailyStats()
        val summary = digitalWellbeing.getWeeklySummary()
        
        val message = """
            Today's Statistics:
            • Usage: ${digitalWellbeing.formatTime(stats.usageMinutes)}
            • Documents viewed: ${stats.documentsViewed}
            • Documents edited: ${stats.documentsEdited}
            • Pages read: ${stats.pagesRead}
            • Scans made: ${stats.scansMade}
            
            Weekly Summary:
            • Total usage: ${digitalWellbeing.formatTime(summary.totalMinutes)}
            • Daily average: ${digitalWellbeing.formatTime(summary.averageMinutesPerDay)}
            • Peak day: ${summary.peakDay} (${digitalWellbeing.formatTime(summary.peakUsageMinutes)})
            • Most active hour: ${formatHour(summary.mostActiveHour)}
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.usage_statistics)
            .setMessage(message)
            .setPositiveButton(R.string.done, null)
            .show()
    }
    
    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }
    
    private fun setupQuickSettings() {
        // Check availability
        if (!quickSettings.areTilesAvailable()) {
            binding.cardQuickSettings.visibility = View.GONE
            return
        }
        
        // Scanner tile toggle
        binding.switchScannerTile.isChecked = quickSettings.isTileEnabled(
            QuickSettingsManager.TileType.SCANNER
        )
        binding.switchScannerTile.setOnCheckedChangeListener { _, isChecked ->
            quickSettings.setTileEnabled(QuickSettingsManager.TileType.SCANNER, isChecked)
            if (isChecked) {
                Toast.makeText(context, R.string.tile_enabled_hint, Toast.LENGTH_LONG).show()
            }
        }
        
        // Create tile toggle  
        binding.switchCreateTile.isChecked = quickSettings.isTileEnabled(
            QuickSettingsManager.TileType.CREATE
        )
        binding.switchCreateTile.setOnCheckedChangeListener { _, isChecked ->
            quickSettings.setTileEnabled(QuickSettingsManager.TileType.CREATE, isChecked)
        }
        
        // Convert tile toggle
        binding.switchConvertTile.isChecked = quickSettings.isTileEnabled(
            QuickSettingsManager.TileType.CONVERT
        )
        binding.switchConvertTile.setOnCheckedChangeListener { _, isChecked ->
            quickSettings.setTileEnabled(QuickSettingsManager.TileType.CONVERT, isChecked)
        }
    }
    
    private fun setupVoiceCommands() {
        // Check availability
        if (!voiceCommands.isVoiceRecognitionAvailable()) {
            binding.cardVoiceCommands.visibility = View.GONE
            return
        }
        
        // Voice commands toggle
        binding.switchVoiceCommands.isChecked = voiceCommands.isVoiceEnabled()
        binding.switchVoiceCommands.setOnCheckedChangeListener { _, isChecked ->
            voiceCommands.setVoiceEnabled(isChecked)
            binding.layoutVoiceSettings.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.layoutVoiceSettings.visibility = 
            if (voiceCommands.isVoiceEnabled()) View.VISIBLE else View.GONE
        
        // Voice settings
        val settings = voiceCommands.getVoiceSettings()
        binding.switchConfirmActions.isChecked = settings.confirmBeforeAction
        binding.switchReadback.isChecked = settings.readbackEnabled
        
        binding.switchConfirmActions.setOnCheckedChangeListener { _, isChecked ->
            voiceCommands.saveVoiceSettings(
                voiceCommands.getVoiceSettings().copy(confirmBeforeAction = isChecked)
            )
        }
        
        binding.switchReadback.setOnCheckedChangeListener { _, isChecked ->
            voiceCommands.saveVoiceSettings(
                voiceCommands.getVoiceSettings().copy(readbackEnabled = isChecked)
            )
        }
        
        // Voice commands help
        binding.btnVoiceHelp.setOnClickListener {
            showVoiceCommandsHelp()
        }
    }
    
    private fun showVoiceCommandsHelp() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.voice_commands)
            .setMessage(voiceCommands.getHelpText())
            .setPositiveButton(R.string.done, null)
            .show()
    }
    
    private fun setupNearbyShare() {
        if (!nearbyShare.isNearbyShareAvailable()) {
            binding.cardNearbyShare.visibility = View.GONE
            return
        }
        
        val prefs = nearbyShare.getSharePreferences()
        
        binding.switchQuickShare.isChecked = prefs.showQuickShareOption
        binding.switchQuickShare.setOnCheckedChangeListener { _, isChecked ->
            nearbyShare.setQuickShareEnabled(isChecked)
        }
    }
    
    private fun setupDesktopMode() {
        val state = desktopMode.getDesktopModeState()
        
        // Show current mode status
        updateDesktopModeStatus(state)
        
        // Desktop layout settings
        val config = desktopMode.getDesktopLayoutConfig()
        
        binding.switchShowSidebar.isChecked = config.showSidebar
        binding.switchShowSidebar.setOnCheckedChangeListener { _, isChecked ->
            desktopMode.saveDesktopLayoutConfig(config.copy(showSidebar = isChecked))
        }
        
        binding.switchCompactToolbar.isChecked = config.useCompactToolbar
        binding.switchCompactToolbar.setOnCheckedChangeListener { _, isChecked ->
            desktopMode.saveDesktopLayoutConfig(config.copy(useCompactToolbar = isChecked))
        }
        
        binding.switchMultiWindow.isChecked = config.enableMultiWindow
        binding.switchMultiWindow.setOnCheckedChangeListener { _, isChecked ->
            desktopMode.saveDesktopLayoutConfig(config.copy(enableMultiWindow = isChecked))
        }
        
        binding.switchDragDrop.isChecked = config.enableDragAndDrop
        binding.switchDragDrop.setOnCheckedChangeListener { _, isChecked ->
            desktopMode.saveDesktopLayoutConfig(config.copy(enableDragAndDrop = isChecked))
        }
        
        // Keyboard shortcuts
        binding.btnKeyboardShortcuts.setOnClickListener {
            showKeyboardShortcuts()
        }
    }
    
    private fun updateDesktopModeStatus(state: DesktopModeManager.DesktopModeState) {
        val statusText = when {
            state.isDexMode -> getString(R.string.dex_mode_active)
            state.isChromeOS -> getString(R.string.chromeos_mode_active)
            state.hasPhysicalKeyboard -> getString(R.string.keyboard_mode_active)
            else -> getString(R.string.mobile_mode)
        }
        binding.textDesktopModeStatus.text = statusText
        
        binding.textKeyboardStatus.text = if (state.hasPhysicalKeyboard) {
            getString(R.string.keyboard_connected)
        } else {
            getString(R.string.no_keyboard)
        }
        
        binding.textMouseStatus.text = if (state.hasMouseConnected) {
            getString(R.string.mouse_connected)
        } else {
            getString(R.string.no_mouse)
        }
    }
    
    private fun showKeyboardShortcuts() {
        val shortcuts = desktopMode.getDefaultShortcuts()
        val message = shortcuts.joinToString("\n") { shortcut ->
            val modifiers = buildString {
                if ((shortcut.modifiers and android.view.KeyEvent.META_CTRL_ON) != 0) append("Ctrl+")
                if ((shortcut.modifiers and android.view.KeyEvent.META_SHIFT_ON) != 0) append("Shift+")
                if ((shortcut.modifiers and android.view.KeyEvent.META_ALT_ON) != 0) append("Alt+")
            }
            val key = android.view.KeyEvent.keyCodeToString(shortcut.keyCode)
                .removePrefix("KEYCODE_")
            "$modifiers$key → ${shortcut.description}"
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.keyboard_shortcuts)
            .setMessage(message)
            .setPositiveButton(R.string.done, null)
            .show()
    }
    
    private fun updatePlatformInfo() {
        val info = buildString {
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        }
        binding.textPlatformInfo.text = info
    }
    
    override fun onResume() {
        super.onResume()
        updateUsageDisplay()
        digitalWellbeing.startSession()
    }
    
    override fun onPause() {
        super.onPause()
        digitalWellbeing.endSession()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

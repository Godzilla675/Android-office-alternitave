package com.officesuite.app.ui.productivity

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentProductivityBinding
import com.officesuite.app.productivity.*

/**
 * Fragment for displaying productivity features
 * Implements Section 13 - Gamification & Productivity
 */
class ProductivityFragment : Fragment(), FocusTimerListener {

    private var _binding: FragmentProductivityBinding? = null
    private val binding get() = _binding!!

    private lateinit var productivityManager: ProductivityManager
    private var focusTimerService: FocusTimerService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FocusTimerService.FocusTimerBinder
            focusTimerService = binder.getService()
            focusTimerService?.setListener(this@ProductivityFragment)
            isServiceBound = true
            updateTimerUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            focusTimerService = null
            isServiceBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        productivityManager = ProductivityManager(requireContext())
        
        setupUI()
        loadData()
        bindFocusTimerService()
    }

    private fun setupUI() {
        // Focus Timer Controls
        binding.buttonStartFocus.setOnClickListener {
            startFocusSession()
        }
        
        binding.buttonPauseFocus.setOnClickListener {
            pauseResumeTimer()
        }
        
        binding.buttonStopFocus.setOnClickListener {
            stopTimer()
        }
        
        binding.buttonSkipFocus.setOnClickListener {
            skipPhase()
        }
        
        binding.cardFocusSettings.setOnClickListener {
            showFocusSettingsDialog()
        }
        
        // Reading Goals
        binding.cardReadingGoals.setOnClickListener {
            showReadingGoalsDialog()
        }
        
        // Study Mode
        binding.switchStudyMode.setOnCheckedChangeListener { _, isChecked ->
            productivityManager.setStudyModeEnabled(isChecked)
            updateStudyModeUI()
        }
        
        // Achievements
        binding.cardAchievements.setOnClickListener {
            showAchievementsDialog()
        }
    }

    private fun loadData() {
        // Load streaks
        val currentStreak = productivityManager.getCurrentStreak()
        val longestStreak = productivityManager.getLongestStreak()
        binding.textCurrentStreak.text = "$currentStreak days"
        binding.textLongestStreak.text = "Best: $longestStreak days"
        
        // Load reading progress
        val todayPages = productivityManager.getTodayPagesRead()
        val dailyGoal = productivityManager.getDailyReadingGoal()
        val progress = (todayPages.toFloat() / dailyGoal.toFloat() * 100).coerceIn(0f, 100f)
        binding.progressReading.progress = progress.toInt()
        binding.textReadingProgress.text = "$todayPages / $dailyGoal pages today"
        
        // Load weekly stats
        val weeklyStats = productivityManager.getWeeklyStats()
        val totalDocsOpened = weeklyStats.sumOf { it.documentsOpened }
        val totalDocsCreated = weeklyStats.sumOf { it.documentsCreated }
        val totalScans = weeklyStats.sumOf { it.scansCompleted }
        val totalFocusSessions = weeklyStats.sumOf { it.focusSessionsCompleted }
        
        binding.textWeeklyDocsOpened.text = totalDocsOpened.toString()
        binding.textWeeklyDocsCreated.text = totalDocsCreated.toString()
        binding.textWeeklyScans.text = totalScans.toString()
        binding.textWeeklyFocusSessions.text = totalFocusSessions.toString()
        
        // Load achievements count
        val unlockedAchievements = productivityManager.getUnlockedAchievements()
        val totalAchievements = Achievement.entries.size
        binding.textAchievementsCount.text = "${unlockedAchievements.size} / $totalAchievements"
        
        // Load study mode state
        binding.switchStudyMode.isChecked = productivityManager.isStudyModeEnabled()
        updateStudyModeUI()
        
        // Load focus timer settings
        val focusDuration = productivityManager.getFocusSessionDuration()
        binding.textFocusDuration.text = "$focusDuration min"
    }

    private fun bindFocusTimerService() {
        val intent = Intent(requireContext(), FocusTimerService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startFocusSession() {
        val duration = productivityManager.getFocusSessionDuration() * 60 * 1000L
        val intent = Intent(requireContext(), FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_DURATION, duration)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
        
        if (!isServiceBound) {
            bindFocusTimerService()
        }
    }

    private fun pauseResumeTimer() {
        val action = if (focusTimerService?.isTimerRunning() == true) {
            FocusTimerService.ACTION_PAUSE
        } else {
            FocusTimerService.ACTION_RESUME
        }
        val intent = Intent(requireContext(), FocusTimerService::class.java).apply {
            this.action = action
        }
        requireContext().startService(intent)
    }

    private fun stopTimer() {
        val intent = Intent(requireContext(), FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        }
        requireContext().startService(intent)
    }

    private fun skipPhase() {
        val intent = Intent(requireContext(), FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_SKIP
        }
        requireContext().startService(intent)
    }

    private fun updateTimerUI() {
        focusTimerService?.let { service ->
            val isRunning = service.isTimerRunning()
            val phase = service.getCurrentPhase()
            val remaining = service.getTimeRemaining()
            val total = service.getTotalTime()
            
            binding.textTimerTime.text = formatTime(remaining)
            binding.textTimerPhase.text = when (phase) {
                TimerPhase.FOCUS -> "Focus Time"
                TimerPhase.SHORT_BREAK -> "Short Break"
                TimerPhase.LONG_BREAK -> "Long Break"
            }
            
            val progress = if (total > 0) ((total - remaining).toFloat() / total * 100).toInt() else 0
            binding.progressTimer.progress = progress
            
            binding.buttonStartFocus.visibility = if (!isRunning && remaining == 0L) View.VISIBLE else View.GONE
            binding.buttonPauseFocus.visibility = if (isRunning || remaining > 0) View.VISIBLE else View.GONE
            binding.buttonPauseFocus.text = if (isRunning) "Pause" else "Resume"
            binding.buttonStopFocus.visibility = if (isRunning || remaining > 0) View.VISIBLE else View.GONE
            binding.buttonSkipFocus.visibility = if (isRunning || remaining > 0) View.VISIBLE else View.GONE
        }
    }

    private fun showFocusSettingsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_focus_settings, null
        )
        
        val focusPicker = dialogView.findViewById<NumberPicker>(R.id.pickerFocusDuration)
        val breakPicker = dialogView.findViewById<NumberPicker>(R.id.pickerBreakDuration)
        val longBreakPicker = dialogView.findViewById<NumberPicker>(R.id.pickerLongBreakDuration)
        
        focusPicker.minValue = 5
        focusPicker.maxValue = 60
        focusPicker.value = productivityManager.getFocusSessionDuration()
        
        breakPicker.minValue = 1
        breakPicker.maxValue = 15
        breakPicker.value = productivityManager.getBreakDuration()
        
        longBreakPicker.minValue = 5
        longBreakPicker.maxValue = 30
        longBreakPicker.value = productivityManager.getLongBreakDuration()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.focus_settings)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                productivityManager.setFocusSessionDuration(focusPicker.value)
                productivityManager.setBreakDuration(breakPicker.value)
                productivityManager.setLongBreakDuration(longBreakPicker.value)
                binding.textFocusDuration.text = "${focusPicker.value} min"
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showReadingGoalsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_reading_goals, null
        )
        
        val dailyPicker = dialogView.findViewById<NumberPicker>(R.id.pickerDailyGoal)
        val weeklyPicker = dialogView.findViewById<NumberPicker>(R.id.pickerWeeklyGoal)
        
        dailyPicker.minValue = 1
        dailyPicker.maxValue = 100
        dailyPicker.value = productivityManager.getDailyReadingGoal()
        
        weeklyPicker.minValue = 5
        weeklyPicker.maxValue = 500
        weeklyPicker.value = productivityManager.getWeeklyReadingGoal()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reading_goals)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                productivityManager.setDailyReadingGoal(dailyPicker.value)
                productivityManager.setWeeklyReadingGoal(weeklyPicker.value)
                loadData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAchievementsDialog() {
        val unlocked = productivityManager.getUnlockedAchievements()
        val all = Achievement.entries
        
        val message = buildString {
            appendLine("Unlocked Achievements:\n")
            all.forEach { achievement ->
                val status = if (unlocked.contains(achievement)) "✓" else "○"
                appendLine("$status ${achievement.title}")
                appendLine("   ${achievement.description}\n")
            }
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.achievements)
            .setMessage(message)
            .setPositiveButton(R.string.done, null)
            .show()
    }

    private fun updateStudyModeUI() {
        val isEnabled = productivityManager.isStudyModeEnabled()
        binding.textStudyModeStatus.text = if (isEnabled) {
            "Distracting features are blocked"
        } else {
            "All features available"
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // FocusTimerListener implementation
    override fun onTick(remainingMillis: Long, totalMillis: Long, phase: TimerPhase) {
        activity?.runOnUiThread {
            binding.textTimerTime.text = formatTime(remainingMillis)
            val progress = ((totalMillis - remainingMillis).toFloat() / totalMillis * 100).toInt()
            binding.progressTimer.progress = progress
        }
    }

    override fun onTimerStarted(phase: TimerPhase) {
        activity?.runOnUiThread {
            binding.textTimerPhase.text = when (phase) {
                TimerPhase.FOCUS -> "Focus Time"
                TimerPhase.SHORT_BREAK -> "Short Break"
                TimerPhase.LONG_BREAK -> "Long Break"
            }
            updateTimerUI()
        }
    }

    override fun onTimerPaused() {
        activity?.runOnUiThread {
            binding.buttonPauseFocus.text = "Resume"
        }
    }

    override fun onTimerResumed() {
        activity?.runOnUiThread {
            binding.buttonPauseFocus.text = "Pause"
        }
    }

    override fun onTimerStopped() {
        activity?.runOnUiThread {
            binding.textTimerTime.text = "00:00"
            binding.progressTimer.progress = 0
            binding.buttonStartFocus.visibility = View.VISIBLE
            binding.buttonPauseFocus.visibility = View.GONE
            binding.buttonStopFocus.visibility = View.GONE
            binding.buttonSkipFocus.visibility = View.GONE
        }
    }

    override fun onPhaseCompleted(completedPhase: TimerPhase, nextPhase: TimerPhase) {
        activity?.runOnUiThread {
            val message = when (completedPhase) {
                TimerPhase.FOCUS -> "Focus session complete! Time for a break."
                TimerPhase.SHORT_BREAK -> "Break over! Ready to focus again?"
                TimerPhase.LONG_BREAK -> "Long break over! Ready to focus again?"
            }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Session Complete")
                .setMessage(message)
                .setPositiveButton("Start ${nextPhase.name.replace("_", " ")}") { _, _ ->
                    val duration = when (nextPhase) {
                        TimerPhase.FOCUS -> productivityManager.getFocusSessionDuration()
                        TimerPhase.SHORT_BREAK -> productivityManager.getBreakDuration()
                        TimerPhase.LONG_BREAK -> productivityManager.getLongBreakDuration()
                    } * 60 * 1000L
                    
                    val intent = Intent(requireContext(), FocusTimerService::class.java).apply {
                        action = FocusTimerService.ACTION_START
                        putExtra(FocusTimerService.EXTRA_DURATION, duration)
                    }
                    ContextCompat.startForegroundService(requireContext(), intent)
                }
                .setNegativeButton("Stop", null)
                .show()
        }
    }

    override fun onFocusSessionCompleted(totalSessions: Int) {
        activity?.runOnUiThread {
            productivityManager.recordFocusSessionCompleted()
            loadData()
            Toast.makeText(
                requireContext(),
                "Focus session completed! Total today: ${productivityManager.getTodayFocusSessions()}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isServiceBound) {
            focusTimerService?.setListener(null)
            requireContext().unbindService(serviceConnection)
            isServiceBound = false
        }
        _binding = null
    }
}

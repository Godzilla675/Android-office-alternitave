package com.officesuite.app.ui.productivity

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.officesuite.app.R
import com.officesuite.app.productivity.FocusTimer
import com.officesuite.app.productivity.ProductivityManager

/**
 * Fragment for displaying productivity stats, focus timer, and achievements.
 * Implements Nice-to-Have Features from Phase 2:
 * - Productivity Stats (Section 13)
 * - Focus Timer (Section 13)
 * - Achievement Badges (Section 13)
 */
class ProductivityFragment : Fragment(), FocusTimer.FocusTimerListener {
    
    private lateinit var focusTimer: FocusTimer
    
    // UI Elements
    private var timerText: TextView? = null
    private var timerProgress: CircularProgressIndicator? = null
    private var sessionTypeText: TextView? = null
    private var startButton: MaterialButton? = null
    private var pauseButton: MaterialButton? = null
    private var skipButton: MaterialButton? = null
    
    private var readingProgressBar: LinearProgressIndicator? = null
    private var readingProgressText: TextView? = null
    private var writingProgressBar: LinearProgressIndicator? = null
    private var writingProgressText: TextView? = null
    
    private var streakText: TextView? = null
    private var achievementsCountText: TextView? = null
    private var documentsViewedText: TextView? = null
    private var documentsEditedText: TextView? = null
    private var totalReadingTimeText: TextView? = null
    private var totalWordsWrittenText: TextView? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Create a simple layout programmatically
        return createLayout()
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        focusTimer = FocusTimer(requireContext())
        focusTimer.setListener(this)
        
        setupUI()
        updateStats()
        updateTimerUI()
    }
    
    private fun createLayout(): View {
        val context = requireContext()
        
        val scrollView = androidx.core.widget.NestedScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(32, 32, 32, 32)
        }
        
        // Focus Timer Card
        val timerCard = createTimerCard()
        rootLayout.addView(timerCard)
        
        // Daily Goals Card
        val goalsCard = createGoalsCard()
        rootLayout.addView(goalsCard)
        
        // Stats Card
        val statsCard = createStatsCard()
        rootLayout.addView(statsCard)
        
        scrollView.addView(rootLayout)
        return scrollView
    }
    
    private fun createTimerCard(): MaterialCardView {
        val context = requireContext()
        
        return MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            radius = 16f
            cardElevation = 4f
            
            val cardContent = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                
                // Title
                addView(TextView(context).apply {
                    text = "Focus Timer"
                    textSize = 20f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setPadding(0, 0, 0, 16)
                })
                
                // Timer display
                timerText = TextView(context).apply {
                    text = "25:00"
                    textSize = 48f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(resources.getColor(R.color.primary, null))
                }
                addView(timerText)
                
                // Session type
                sessionTypeText = TextView(context).apply {
                    text = "Focus Session"
                    textSize = 14f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 8, 0, 16)
                }
                addView(sessionTypeText)
                
                // Progress
                timerProgress = CircularProgressIndicator(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                    isIndeterminate = false
                    max = 100
                    visibility = View.GONE
                }
                addView(timerProgress)
                
                // Buttons
                val buttonRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 16, 0, 0)
                    
                    startButton = MaterialButton(context).apply {
                        text = "Start"
                        setOnClickListener { onStartClicked() }
                    }
                    addView(startButton)
                    
                    pauseButton = MaterialButton(context).apply {
                        text = "Pause"
                        setOnClickListener { onPauseClicked() }
                        visibility = View.GONE
                        (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 16
                    }
                    addView(pauseButton)
                    
                    skipButton = MaterialButton(context).apply {
                        text = "Skip"
                        setOnClickListener { onSkipClicked() }
                        visibility = View.GONE
                        (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 16
                    }
                    addView(skipButton)
                }
                addView(buttonRow)
            }
            
            addView(cardContent)
        }
    }
    
    private fun createGoalsCard(): MaterialCardView {
        val context = requireContext()
        
        return MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            radius = 16f
            cardElevation = 4f
            
            val cardContent = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                
                // Title
                addView(TextView(context).apply {
                    text = "Today's Goals"
                    textSize = 20f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setPadding(0, 0, 0, 16)
                })
                
                // Reading goal
                addView(TextView(context).apply {
                    text = "Reading"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                })
                
                readingProgressBar = LinearProgressIndicator(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    max = 100
                }
                addView(readingProgressBar)
                
                readingProgressText = TextView(context).apply {
                    text = "0%"
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 4, 0, 16)
                }
                addView(readingProgressText)
                
                // Writing goal
                addView(TextView(context).apply {
                    text = "Writing"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                })
                
                writingProgressBar = LinearProgressIndicator(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    max = 100
                }
                addView(writingProgressBar)
                
                writingProgressText = TextView(context).apply {
                    text = "0%"
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 4, 0, 0)
                }
                addView(writingProgressText)
            }
            
            addView(cardContent)
        }
    }
    
    private fun createStatsCard(): MaterialCardView {
        val context = requireContext()
        
        return MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            radius = 16f
            cardElevation = 4f
            
            val cardContent = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                
                // Title
                addView(TextView(context).apply {
                    text = "Productivity Stats"
                    textSize = 20f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setPadding(0, 0, 0, 16)
                })
                
                // Streak
                streakText = TextView(context).apply {
                    text = "🔥 Writing Streak: 0 days"
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setPadding(0, 0, 0, 8)
                }
                addView(streakText)
                
                // Achievements
                achievementsCountText = TextView(context).apply {
                    text = "🏆 Achievements: 0 unlocked"
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setPadding(0, 0, 0, 8)
                }
                addView(achievementsCountText)
                
                // Documents viewed
                documentsViewedText = TextView(context).apply {
                    text = "📖 Documents Viewed: 0"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 0, 0, 8)
                }
                addView(documentsViewedText)
                
                // Documents edited
                documentsEditedText = TextView(context).apply {
                    text = "✏️ Documents Edited: 0"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 0, 0, 8)
                }
                addView(documentsEditedText)
                
                // Total reading time
                totalReadingTimeText = TextView(context).apply {
                    text = "📚 Total Reading: 0 min"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setPadding(0, 0, 0, 8)
                }
                addView(totalReadingTimeText)
                
                // Total words written
                totalWordsWrittenText = TextView(context).apply {
                    text = "📝 Total Words Written: 0"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                }
                addView(totalWordsWrittenText)
            }
            
            addView(cardContent)
        }
    }
    
    private fun setupUI() {
        // Initial UI state is handled in updateTimerUI()
    }
    
    private fun updateStats() {
        val stats = ProductivityManager.getProductivityStats()
        
        // Update progress bars
        readingProgressBar?.progress = stats.todayReadingProgress.toInt()
        readingProgressText?.text = "${stats.todayReadingProgress.toInt()}% of daily goal"
        
        writingProgressBar?.progress = stats.todayWritingProgress.toInt()
        writingProgressText?.text = "${stats.todayWritingProgress.toInt()}% of daily goal"
        
        // Update stats
        streakText?.text = "🔥 Writing Streak: ${stats.currentWritingStreak} days"
        achievementsCountText?.text = "🏆 Achievements: ${stats.achievementsUnlocked} unlocked"
        documentsViewedText?.text = "📖 Documents Viewed: ${stats.totalDocumentsViewed}"
        documentsEditedText?.text = "✏️ Documents Edited: ${stats.totalDocumentsEdited}"
        totalReadingTimeText?.text = "📚 Total Reading: ${stats.totalReadingMinutes} min"
        totalWordsWrittenText?.text = "📝 Total Words Written: ${stats.totalWordsWritten}"
    }
    
    private fun updateTimerUI() {
        val status = focusTimer.getStatus()
        
        timerText?.text = focusTimer.formatRemainingTime()
        
        sessionTypeText?.text = when (status.sessionType) {
            FocusTimer.SessionType.FOCUS -> "Focus Session"
            FocusTimer.SessionType.SHORT_BREAK -> "Short Break"
            FocusTimer.SessionType.LONG_BREAK -> "Long Break"
        }
        
        when (status.state) {
            FocusTimer.TimerState.IDLE -> {
                startButton?.visibility = View.VISIBLE
                startButton?.text = "Start"
                pauseButton?.visibility = View.GONE
                skipButton?.visibility = View.GONE
            }
            FocusTimer.TimerState.RUNNING -> {
                startButton?.visibility = View.GONE
                pauseButton?.visibility = View.VISIBLE
                pauseButton?.text = "Pause"
                skipButton?.visibility = View.VISIBLE
            }
            FocusTimer.TimerState.PAUSED -> {
                startButton?.visibility = View.VISIBLE
                startButton?.text = "Resume"
                pauseButton?.visibility = View.GONE
                skipButton?.visibility = View.VISIBLE
            }
            FocusTimer.TimerState.COMPLETED -> {
                startButton?.visibility = View.VISIBLE
                startButton?.text = "Start Next"
                pauseButton?.visibility = View.GONE
                skipButton?.visibility = View.GONE
            }
        }
    }
    
    private fun onStartClicked() {
        val status = focusTimer.getStatus()
        when (status.state) {
            FocusTimer.TimerState.IDLE, FocusTimer.TimerState.COMPLETED -> focusTimer.startFocusSession()
            FocusTimer.TimerState.PAUSED -> focusTimer.resume()
            else -> {}
        }
    }
    
    private fun onPauseClicked() {
        focusTimer.pause()
    }
    
    private fun onSkipClicked() {
        focusTimer.skip()
    }
    
    // FocusTimerListener implementation
    
    override fun onTick(remainingMillis: Long, progressPercent: Float) {
        activity?.runOnUiThread {
            timerText?.text = focusTimer.formatRemainingTime()
            timerProgress?.progress = progressPercent.toInt()
        }
    }
    
    override fun onSessionComplete(sessionType: FocusTimer.SessionType) {
        activity?.runOnUiThread {
            val message = when (sessionType) {
                FocusTimer.SessionType.FOCUS -> "Focus session complete! Time for a break."
                FocusTimer.SessionType.SHORT_BREAK -> "Break over! Ready for another focus session?"
                FocusTimer.SessionType.LONG_BREAK -> "Long break over! Ready to get back to work?"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            updateTimerUI()
            updateStats()
        }
    }
    
    override fun onStateChanged(state: FocusTimer.TimerState) {
        activity?.runOnUiThread {
            updateTimerUI()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        focusTimer.destroy()
        timerText = null
        timerProgress = null
        sessionTypeText = null
        startButton = null
        pauseButton = null
        skipButton = null
        readingProgressBar = null
        readingProgressText = null
        writingProgressBar = null
        writingProgressText = null
        streakText = null
        achievementsCountText = null
        documentsViewedText = null
        documentsEditedText = null
        totalReadingTimeText = null
        totalWordsWrittenText = null
    }
}

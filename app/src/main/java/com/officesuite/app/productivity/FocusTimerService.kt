package com.officesuite.app.productivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.officesuite.app.MainActivity
import com.officesuite.app.R

/**
 * Service for managing Focus Timer (Pomodoro) sessions
 * Implements Section 13 - Focus Timer (Pomodoro)
 */
class FocusTimerService : Service() {

    private val binder = FocusTimerBinder()
    private var timer: CountDownTimer? = null
    private var listener: FocusTimerListener? = null
    
    private var currentPhase = TimerPhase.FOCUS
    private var timeRemainingMillis: Long = 0
    private var totalTimeMillis: Long = 0
    private var completedSessions: Int = 0
    private var isRunning = false

    inner class FocusTimerBinder : Binder() {
        fun getService(): FocusTimerService = this@FocusTimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getLongExtra(EXTRA_DURATION, 25 * 60 * 1000L)
                startTimer(duration, TimerPhase.FOCUS)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
            ACTION_SKIP -> skipToNextPhase()
        }
        return START_STICKY
    }

    fun setListener(listener: FocusTimerListener?) {
        this.listener = listener
    }

    fun startTimer(durationMillis: Long, phase: TimerPhase) {
        timer?.cancel()
        currentPhase = phase
        totalTimeMillis = durationMillis
        timeRemainingMillis = durationMillis
        isRunning = true

        timer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished
                listener?.onTick(millisUntilFinished, totalTimeMillis, currentPhase)
                updateNotification()
            }

            override fun onFinish() {
                isRunning = false
                onPhaseComplete()
            }
        }.start()

        showNotification()
        listener?.onTimerStarted(currentPhase)
    }

    fun pauseTimer() {
        timer?.cancel()
        isRunning = false
        listener?.onTimerPaused()
        updateNotification()
    }

    fun resumeTimer() {
        if (timeRemainingMillis > 0) {
            isRunning = true
            timer = object : CountDownTimer(timeRemainingMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeRemainingMillis = millisUntilFinished
                    listener?.onTick(millisUntilFinished, totalTimeMillis, currentPhase)
                    updateNotification()
                }

                override fun onFinish() {
                    isRunning = false
                    onPhaseComplete()
                }
            }.start()
            listener?.onTimerResumed()
        }
    }

    fun stopTimer() {
        timer?.cancel()
        timer = null
        isRunning = false
        timeRemainingMillis = 0
        listener?.onTimerStopped()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun skipToNextPhase() {
        timer?.cancel()
        onPhaseComplete()
    }

    private fun onPhaseComplete() {
        when (currentPhase) {
            TimerPhase.FOCUS -> {
                completedSessions++
                listener?.onFocusSessionCompleted(completedSessions)
                
                // Determine if it's time for a long break
                val nextPhase = if (completedSessions % 4 == 0) {
                    TimerPhase.LONG_BREAK
                } else {
                    TimerPhase.SHORT_BREAK
                }
                listener?.onPhaseCompleted(currentPhase, nextPhase)
            }
            TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> {
                listener?.onPhaseCompleted(currentPhase, TimerPhase.FOCUS)
            }
        }
    }

    fun getCurrentPhase(): TimerPhase = currentPhase
    fun getTimeRemaining(): Long = timeRemainingMillis
    fun getTotalTime(): Long = totalTimeMillis
    fun getCompletedSessions(): Int = completedSessions
    fun isTimerRunning(): Boolean = isRunning

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows focus timer status"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, FocusTimerService::class.java).apply {
            action = if (isRunning) ACTION_PAUSE else ACTION_RESUME
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val phaseTitle = when (currentPhase) {
            TimerPhase.FOCUS -> "Focus Time"
            TimerPhase.SHORT_BREAK -> "Short Break"
            TimerPhase.LONG_BREAK -> "Long Break"
        }

        val timeText = formatTime(timeRemainingMillis)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(phaseTitle)
            .setContentText("Time remaining: $timeText")
            .setSmallIcon(R.drawable.ic_edit)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_edit,
                if (isRunning) "Pause" else "Resume",
                pausePendingIntent
            )
            .addAction(R.drawable.ic_delete, "Stop", stopPendingIntent)
            .build()
    }

    private fun formatTime(millis: Long): String {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.officesuite.app.action.START_TIMER"
        const val ACTION_PAUSE = "com.officesuite.app.action.PAUSE_TIMER"
        const val ACTION_RESUME = "com.officesuite.app.action.RESUME_TIMER"
        const val ACTION_STOP = "com.officesuite.app.action.STOP_TIMER"
        const val ACTION_SKIP = "com.officesuite.app.action.SKIP_PHASE"
        const val EXTRA_DURATION = "extra_duration"
    }
}

enum class TimerPhase {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}

interface FocusTimerListener {
    fun onTick(remainingMillis: Long, totalMillis: Long, phase: TimerPhase)
    fun onTimerStarted(phase: TimerPhase)
    fun onTimerPaused()
    fun onTimerResumed()
    fun onTimerStopped()
    fun onPhaseCompleted(completedPhase: TimerPhase, nextPhase: TimerPhase)
    fun onFocusSessionCompleted(totalSessions: Int)
}

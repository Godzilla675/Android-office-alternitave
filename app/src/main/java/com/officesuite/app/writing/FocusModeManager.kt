package com.officesuite.app.writing

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Focus Mode Manager for distraction-free writing
 * Part of Medium Priority Features Phase 2: Enhanced Writing & Editing Tools
 * 
 * Features:
 * - Full-screen immersive mode
 * - Hide system bars and navigation
 * - Dim/hide non-essential UI elements
 * - Typewriter mode (current line centered)
 * - Ambient sounds support (placeholder)
 */
class FocusModeManager(private val context: Context) {

    private var isFocusModeActive = false
    private var originalSystemUiVisibility: Int = 0
    private var hiddenViews = mutableListOf<Pair<View, Float>>()

    /**
     * Focus mode configuration
     */
    data class FocusModeConfig(
        val hideStatusBar: Boolean = true,
        val hideNavigationBar: Boolean = true,
        val dimBackground: Boolean = true,
        val backgroundDimLevel: Float = 0.15f,
        val typewriterMode: Boolean = false,
        val showWordCount: Boolean = true,
        val ambientSound: AmbientSound = AmbientSound.NONE
    )

    enum class AmbientSound {
        NONE,
        RAIN,
        FOREST,
        COFFEE_SHOP,
        OCEAN,
        FIREPLACE,
        WHITE_NOISE
    }

    /**
     * Enter focus mode with custom configuration
     */
    fun enterFocusMode(
        activity: AppCompatActivity,
        config: FocusModeConfig = FocusModeConfig(),
        vararg viewsToHide: View
    ) {
        if (isFocusModeActive) return
        
        isFocusModeActive = true

        // Store original state
        val decorView = activity.window.decorView
        @Suppress("DEPRECATION")
        originalSystemUiVisibility = decorView.systemUiVisibility

        // Enter immersive mode
        val windowInsetsController = WindowCompat.getInsetsController(activity.window, decorView)
        
        if (config.hideStatusBar) {
            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        }
        
        if (config.hideNavigationBar) {
            windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        }
        
        windowInsetsController.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Keep screen on
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide specified views with fade animation
        viewsToHide.forEach { view ->
            hiddenViews.add(Pair(view, view.alpha))
            fadeOutView(view)
        }
    }

    /**
     * Exit focus mode and restore original state
     */
    fun exitFocusMode(activity: AppCompatActivity) {
        if (!isFocusModeActive) return
        
        isFocusModeActive = false

        // Restore system UI
        val decorView = activity.window.decorView
        val windowInsetsController = WindowCompat.getInsetsController(activity.window, decorView)
        
        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())

        // Remove keep screen on flag
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Restore hidden views
        hiddenViews.forEach { (view, originalAlpha) ->
            fadeInView(view, originalAlpha)
        }
        hiddenViews.clear()
    }

    /**
     * Toggle focus mode
     */
    fun toggleFocusMode(
        activity: AppCompatActivity,
        config: FocusModeConfig = FocusModeConfig(),
        vararg viewsToHide: View
    ): Boolean {
        return if (isFocusModeActive) {
            exitFocusMode(activity)
            false
        } else {
            enterFocusMode(activity, config, *viewsToHide)
            true
        }
    }

    /**
     * Check if focus mode is currently active
     */
    fun isFocusModeActive(): Boolean = isFocusModeActive

    private fun fadeOutView(view: View) {
        ObjectAnimator.ofFloat(view, "alpha", view.alpha, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun fadeInView(view: View, targetAlpha: Float) {
        ObjectAnimator.ofFloat(view, "alpha", 0f, targetAlpha).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * Apply typewriter mode scrolling to keep current line centered
     */
    fun applyTypewriterScroll(scrollView: View, cursorY: Int) {
        val scrollHeight = scrollView.height
        val targetScrollY = cursorY - (scrollHeight / 2)
        
        ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.scrollY, targetScrollY).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * Create focus mode overlay for dimming background
     */
    @Suppress("UNUSED_PARAMETER")
    fun createDimOverlay(parent: ViewGroup, dimLevel: Float = 0.15f): View {
        return View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.argb((255 * dimLevel).toInt(), 0, 0, 0))
            isClickable = false
            isFocusable = false
        }
    }

    companion object {
        /**
         * Get recommended focus mode colors based on time of day
         */
        fun getRecommendedBackgroundColor(): Int {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            return when {
                hour < 6 -> Color.parseColor("#1a1a2e")    // Night - dark blue
                hour < 12 -> Color.parseColor("#fffef9")   // Morning - warm white
                hour < 18 -> Color.parseColor("#ffffff")   // Afternoon - pure white
                hour < 21 -> Color.parseColor("#fff8e7")   // Evening - sepia
                else -> Color.parseColor("#2d2d3a")        // Night - dark
            }
        }

        /**
         * Get recommended text color for focus mode
         */
        fun getRecommendedTextColor(): Int {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            return when {
                hour < 6 || hour >= 21 -> Color.parseColor("#b8b8c0")  // Night - soft gray
                else -> Color.parseColor("#333333")                      // Day - dark gray
            }
        }
    }
}

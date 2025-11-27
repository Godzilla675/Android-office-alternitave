package com.officesuite.app.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Utility class providing smooth animations for UI elements.
 * Implements Material Design motion principles for consistent UX.
 */
object AnimationUtils {
    
    private const val DEFAULT_DURATION = 300L
    private const val SHORT_DURATION = 200L
    private const val LONG_DURATION = 400L
    
    /**
     * Fades in a view smoothly.
     */
    fun fadeIn(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * Fades out a view smoothly.
     */
    fun fadeOut(view: View, duration: Long = DEFAULT_DURATION, gone: Boolean = true, onComplete: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = if (gone) View.GONE else View.INVISIBLE
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * Cross-fades between two views.
     */
    fun crossFade(fadeOut: View, fadeIn: View, duration: Long = DEFAULT_DURATION) {
        fadeIn.alpha = 0f
        fadeIn.visibility = View.VISIBLE
        
        fadeOut.animate()
            .alpha(0f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fadeOut.visibility = View.GONE
                }
            })
        
        fadeIn.animate()
            .alpha(1f)
            .setDuration(duration)
            .setListener(null)
    }
    
    /**
     * Slides a view in from the bottom.
     */
    fun slideInFromBottom(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.translationY = view.height.toFloat()
        view.visibility = View.VISIBLE
        view.animate()
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * Slides a view out to the bottom.
     */
    fun slideOutToBottom(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .translationY(view.height.toFloat())
            .setDuration(duration)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.translationY = 0f
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * Slides a view in from the right.
     */
    fun slideInFromRight(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.translationX = view.width.toFloat()
        view.visibility = View.VISIBLE
        view.animate()
            .translationX(0f)
            .setDuration(duration)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * Slides a view out to the left.
     */
    fun slideOutToLeft(view: View, duration: Long = DEFAULT_DURATION, onComplete: (() -> Unit)? = null) {
        view.animate()
            .translationX(-view.width.toFloat())
            .setDuration(duration)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.translationX = 0f
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * Scales a view up with a bounce effect (good for buttons).
     */
    fun scaleUp(view: View, duration: Long = SHORT_DURATION) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
        
        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha).apply {
            this.duration = duration
            interpolator = OvershootInterpolator(2f)
            start()
        }
        view.visibility = View.VISIBLE
    }
    
    /**
     * Scales a view down and fades it out.
     */
    fun scaleDown(view: View, duration: Long = SHORT_DURATION, onComplete: (() -> Unit)? = null) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.8f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.8f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)
        
        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.alpha = 1f
                    onComplete?.invoke()
                }
            })
            start()
        }
    }
    
    /**
     * Performs a subtle pulse animation (good for highlighting).
     */
    fun pulse(view: View, times: Int = 2) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 1f)
        
        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY).apply {
            duration = DEFAULT_DURATION
            repeatCount = times - 1
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    /**
     * Shakes a view horizontally (good for error feedback).
     */
    fun shake(view: View) {
        val translation = PropertyValuesHolder.ofFloat(
            View.TRANSLATION_X,
            0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f
        )
        
        ObjectAnimator.ofPropertyValuesHolder(view, translation).apply {
            duration = 400
            start()
        }
    }
    
    /**
     * Animates a progress bar or loading indicator.
     */
    fun showLoading(loadingView: View, contentView: View, duration: Long = SHORT_DURATION) {
        contentView.animate()
            .alpha(0.5f)
            .setDuration(duration)
            .start()
        
        loadingView.alpha = 0f
        loadingView.visibility = View.VISIBLE
        loadingView.animate()
            .alpha(1f)
            .setDuration(duration)
            .start()
    }
    
    /**
     * Hides loading indicator and shows content.
     */
    fun hideLoading(loadingView: View, contentView: View, duration: Long = SHORT_DURATION) {
        loadingView.animate()
            .alpha(0f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    loadingView.visibility = View.GONE
                }
            })
            .start()
        
        contentView.animate()
            .alpha(1f)
            .setDuration(duration)
            .setListener(null)
            .start()
    }
    
    /**
     * Rotates a view (good for refresh or expand/collapse icons).
     */
    fun rotate(view: View, fromDegrees: Float, toDegrees: Float, duration: Long = DEFAULT_DURATION) {
        view.animate()
            .rotation(toDegrees)
            .setDuration(duration)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }
    
    /**
     * Reveals a view with a circular reveal animation (for Android L+).
     */
    fun circularReveal(view: View, centerX: Int, centerY: Int, startRadius: Float, endRadius: Float) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val anim = android.view.ViewAnimationUtils.createCircularReveal(
                view, centerX, centerY, startRadius, endRadius
            )
            view.visibility = View.VISIBLE
            anim.duration = LONG_DURATION
            anim.start()
        } else {
            fadeIn(view)
        }
    }
    
    /**
     * Hides a view with a circular hide animation (for Android L+).
     */
    fun circularHide(view: View, centerX: Int, centerY: Int, startRadius: Float, endRadius: Float) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val anim = android.view.ViewAnimationUtils.createCircularReveal(
                view, centerX, centerY, startRadius, endRadius
            )
            anim.duration = LONG_DURATION
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }
            })
            anim.start()
        } else {
            fadeOut(view)
        }
    }
}

/**
 * Extension functions for View animations.
 */
fun View.animateFadeIn(duration: Long = 300L, onComplete: (() -> Unit)? = null) {
    AnimationUtils.fadeIn(this, duration, onComplete)
}

fun View.animateFadeOut(duration: Long = 300L, gone: Boolean = true, onComplete: (() -> Unit)? = null) {
    AnimationUtils.fadeOut(this, duration, gone, onComplete)
}

fun View.animateSlideIn(duration: Long = 300L, onComplete: (() -> Unit)? = null) {
    AnimationUtils.slideInFromBottom(this, duration, onComplete)
}

fun View.animateSlideOut(duration: Long = 300L, onComplete: (() -> Unit)? = null) {
    AnimationUtils.slideOutToBottom(this, duration, onComplete)
}

fun View.animatePulse(times: Int = 2) {
    AnimationUtils.pulse(this, times)
}

fun View.animateShake() {
    AnimationUtils.shake(this)
}

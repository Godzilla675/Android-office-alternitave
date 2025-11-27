package com.officesuite.app.utils

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Gesture handler for implementing swipe gestures in document viewers.
 * Supports horizontal and vertical swipes with configurable thresholds.
 */
class GestureHandler(
    context: Context,
    private val listener: OnGestureListener
) : GestureDetector.SimpleOnGestureListener() {
    
    interface OnGestureListener {
        fun onSwipeLeft()
        fun onSwipeRight()
        fun onSwipeUp() {}
        fun onSwipeDown() {}
        fun onDoubleTap() {}
        fun onLongPress() {}
        fun onSingleTap() {}
    }
    
    private val gestureDetector = GestureDetector(context, this)
    
    // Swipe thresholds
    private val swipeThreshold = 100
    private val swipeVelocityThreshold = 100
    
    /**
     * Touch listener that can be attached to views.
     */
    val touchListener = View.OnTouchListener { _, event ->
        gestureDetector.onTouchEvent(event)
    }
    
    /**
     * Processes a motion event.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
    
    override fun onDown(e: MotionEvent): Boolean {
        return true
    }
    
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        listener.onSingleTap()
        return true
    }
    
    override fun onDoubleTap(e: MotionEvent): Boolean {
        listener.onDoubleTap()
        return true
    }
    
    override fun onLongPress(e: MotionEvent) {
        listener.onLongPress()
    }
    
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null) return false
        
        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y
        
        // Check if horizontal swipe is more significant than vertical
        if (abs(diffX) > abs(diffY)) {
            // Horizontal swipe
            if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                if (diffX > 0) {
                    listener.onSwipeRight()
                } else {
                    listener.onSwipeLeft()
                }
                return true
            }
        } else {
            // Vertical swipe
            if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                if (diffY > 0) {
                    listener.onSwipeDown()
                } else {
                    listener.onSwipeUp()
                }
                return true
            }
        }
        
        return false
    }
    
    companion object {
        /**
         * Creates a simple gesture handler for horizontal navigation.
         */
        fun createForNavigation(
            context: Context,
            onNext: () -> Unit,
            onPrevious: () -> Unit
        ): GestureHandler {
            return GestureHandler(context, object : OnGestureListener {
                override fun onSwipeLeft() = onNext()
                override fun onSwipeRight() = onPrevious()
            })
        }
        
        /**
         * Creates a gesture handler for page navigation with zoom support.
         */
        fun createForDocumentViewer(
            context: Context,
            onNextPage: () -> Unit,
            onPreviousPage: () -> Unit,
            onDoubleTapZoom: () -> Unit = {},
            onSingleTap: () -> Unit = {}
        ): GestureHandler {
            return GestureHandler(context, object : OnGestureListener {
                override fun onSwipeLeft() = onNextPage()
                override fun onSwipeRight() = onPreviousPage()
                override fun onDoubleTap() = onDoubleTapZoom()
                override fun onSingleTap() = onSingleTap()
            })
        }
    }
}

/**
 * Extension function to add swipe navigation to a view.
 */
fun View.addSwipeNavigation(
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val gestureHandler = GestureHandler.createForNavigation(context, onNext, onPrevious)
    setOnTouchListener(gestureHandler.touchListener)
}

/**
 * Extension function to add document viewer gestures to a view.
 */
fun View.addDocumentGestures(
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onDoubleTapZoom: () -> Unit = {},
    onSingleTap: () -> Unit = {}
) {
    val gestureHandler = GestureHandler.createForDocumentViewer(
        context, onNextPage, onPreviousPage, onDoubleTapZoom, onSingleTap
    )
    setOnTouchListener(gestureHandler.touchListener)
}

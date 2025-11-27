package com.officesuite.app.ui.pptx

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.officesuite.app.databinding.ActivityPresentationModeBinding
import com.officesuite.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import java.io.File
import java.io.FileInputStream

/**
 * Full-screen presentation mode for PPTX files with timer support.
 * Implements Nice-to-Have Feature #10: Presentation Mode
 */
class PresentationModeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPresentationModeBinding
    
    private var slideImages = mutableListOf<Bitmap>()
    private var currentSlideIndex = 0
    private var timerCountDown: CountDownTimer? = null
    private var timerSeconds = 0L
    private var isTimerRunning = false
    private var elapsedSeconds = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPresentationModeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        hideSystemUI()
        setupClickListeners()
        
        intent.getStringExtra(EXTRA_FILE_URI)?.let { uriString ->
            loadPresentation(Uri.parse(uriString))
        } ?: run {
            Toast.makeText(this, "No presentation file provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    private fun setupClickListeners() {
        // Navigate to previous slide
        binding.btnPrevious.setOnClickListener {
            if (currentSlideIndex > 0) {
                currentSlideIndex--
                showCurrentSlide()
            }
        }
        
        // Navigate to next slide
        binding.btnNext.setOnClickListener {
            if (currentSlideIndex < slideImages.size - 1) {
                currentSlideIndex++
                showCurrentSlide()
            }
        }
        
        // Tap on slide area to toggle controls visibility
        binding.imageSlide.setOnClickListener {
            toggleControlsVisibility()
        }
        
        // Exit presentation mode
        binding.btnExit.setOnClickListener {
            finish()
        }
        
        // Timer toggle
        binding.btnTimer.setOnClickListener {
            toggleTimer()
        }
        
        // Reset timer
        binding.textTimer.setOnClickListener {
            resetTimer()
        }
    }

    private fun toggleControlsVisibility() {
        val visibility = if (binding.controlsContainer.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.controlsContainer.visibility = visibility
        binding.timerContainer.visibility = visibility
        binding.btnExit.visibility = visibility
    }

    private fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        isTimerRunning = true
        binding.btnTimer.text = "⏸"
        
        timerCountDown = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds++
                updateTimerDisplay()
            }
            
            override fun onFinish() {}
        }.start()
    }

    private fun pauseTimer() {
        isTimerRunning = false
        binding.btnTimer.text = "▶"
        timerCountDown?.cancel()
    }

    private fun resetTimer() {
        pauseTimer()
        elapsedSeconds = 0L
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        
        binding.textTimer.text = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun loadPresentation(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val cachedFile = withContext(Dispatchers.IO) {
                    FileUtils.copyToCache(this@PresentationModeActivity, uri)
                }
                
                cachedFile?.let { file ->
                    withContext(Dispatchers.IO) {
                        loadSlides(file)
                    }
                    
                    if (slideImages.isNotEmpty()) {
                        showCurrentSlide()
                    }
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@PresentationModeActivity, "Failed to load presentation: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadSlides(file: File) {
        slideImages.clear()
        
        try {
            val slideShow = XMLSlideShow(FileInputStream(file))
            
            // Use standard 16:9 presentation dimensions
            val slideWidth = 1920
            val slideHeight = 1080
            
            for (slide in slideShow.slides) {
                val bitmap = Bitmap.createBitmap(
                    slideWidth,
                    slideHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                val textPaint = android.graphics.Paint().apply {
                    color = Color.BLACK
                    textSize = 48f
                    isAntiAlias = true
                }
                
                val titlePaint = android.graphics.Paint().apply {
                    color = Color.BLACK
                    textSize = 72f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                
                var yPosition = 120f
                var hasContent = false
                
                for (shape in slide.shapes) {
                    if (shape is XSLFTextShape) {
                        val text = shape.text
                        if (text.isNotBlank()) {
                            hasContent = true
                            if (yPosition == 120f) {
                                canvas.drawText(text.take(60), slideWidth / 2f, yPosition, titlePaint)
                            } else {
                                canvas.drawText(text.take(80), 60f, yPosition, textPaint)
                            }
                            yPosition += 80f
                            if (yPosition > slideHeight - 160) break
                        }
                    }
                }
                
                if (!hasContent) {
                    canvas.drawText(
                        "Slide ${slideImages.size + 1}",
                        slideWidth / 2f,
                        slideHeight / 2f,
                        titlePaint
                    )
                }
                
                slideImages.add(bitmap)
            }
            
            slideShow.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showCurrentSlide() {
        if (slideImages.isEmpty()) return
        
        binding.imageSlide.setImageBitmap(slideImages[currentSlideIndex])
        binding.textSlideNumber.text = "${currentSlideIndex + 1} / ${slideImages.size}"
        
        // Update navigation button states
        binding.btnPrevious.isEnabled = currentSlideIndex > 0
        binding.btnNext.isEnabled = currentSlideIndex < slideImages.size - 1
        
        binding.btnPrevious.alpha = if (currentSlideIndex > 0) 1f else 0.3f
        binding.btnNext.alpha = if (currentSlideIndex < slideImages.size - 1) 1f else 0.3f
    }

    override fun onDestroy() {
        super.onDestroy()
        timerCountDown?.cancel()
        slideImages.forEach { it.recycle() }
        slideImages.clear()
    }

    companion object {
        const val EXTRA_FILE_URI = "file_uri"
    }
}

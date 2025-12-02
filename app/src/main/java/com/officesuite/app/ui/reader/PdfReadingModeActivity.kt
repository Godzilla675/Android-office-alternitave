package com.officesuite.app.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.officesuite.app.databinding.ActivityPdfReadingModeBinding
import com.officesuite.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-screen reading mode for PDF documents.
 * Provides a distraction-free viewing experience with proper PDF page rendering.
 */
class PdfReadingModeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfReadingModeBinding
    
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPageIndex = 0
    private var totalPages = 0
    
    private var timerCountDown: CountDownTimer? = null
    private var isTimerRunning = false
    private var elapsedSeconds = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfReadingModeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        hideSystemUI()
        setupClickListeners()
        
        intent.getStringExtra(EXTRA_FILE_URI)?.let { uriString ->
            loadPdf(Uri.parse(uriString))
        } ?: run {
            Toast.makeText(this, "No PDF file provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
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
        // Navigate to previous page
        binding.btnPrevious.setOnClickListener {
            if (currentPageIndex > 0) {
                currentPageIndex--
                showPage(currentPageIndex)
            }
        }
        
        // Navigate to next page
        binding.btnNext.setOnClickListener {
            if (currentPageIndex < totalPages - 1) {
                currentPageIndex++
                showPage(currentPageIndex)
            }
        }
        
        // Tap on page area to toggle controls visibility
        binding.imagePage.setOnClickListener {
            toggleControlsVisibility()
        }
        
        // Exit reading mode
        binding.btnExit.setOnClickListener {
            finish()
        }
        
        // Timer toggle
        binding.btnTimer.setOnClickListener {
            toggleTimer()
        }
        
        // Reset timer on text click
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

    private fun loadPdf(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val cachedFile = withContext(Dispatchers.IO) {
                    FileUtils.copyToCache(this@PdfReadingModeActivity, uri)
                }
                
                cachedFile?.let { file ->
                    openPdfRenderer(file)
                    showPage(0)
                    binding.progressBar.visibility = View.GONE
                } ?: run {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@PdfReadingModeActivity, "Failed to load PDF", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@PdfReadingModeActivity, "Failed to load PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun openPdfRenderer(file: File) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            totalPages = pdfRenderer?.pageCount ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun showPage(index: Int) {
        if (index < 0 || index >= totalPages) return
        
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    renderPage(index)
                }
                
                binding.imagePage.setImageBitmap(bitmap)
                updatePageDisplay()
            } catch (e: Exception) {
                Toast.makeText(this@PdfReadingModeActivity, "Error rendering page", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderPage(index: Int): Bitmap {
        // Close the current page if open
        currentPage?.close()
        
        // Open the new page
        currentPage = pdfRenderer?.openPage(index)
        
        val page = currentPage ?: throw IllegalStateException("Failed to open page")
        
        // Calculate scale to fit screen while maintaining aspect ratio
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        val pageWidth = page.width
        val pageHeight = page.height
        
        // Determine scale factor based on device memory and screen density
        // Use a lower scale for devices with lower memory to prevent OOM
        val activityManager = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // Use 1.5x scale for low-memory devices, 2x for others
        val scaleFactor = if (memoryInfo.lowMemory || memoryInfo.availMem < 256 * 1024 * 1024) 1.5f else 2f
        
        val scaleX = (screenWidth * scaleFactor) / pageWidth
        val scaleY = (screenHeight * scaleFactor) / pageHeight
        val scale = minOf(scaleX, scaleY)
        
        val bitmapWidth = (pageWidth * scale).toInt()
        val bitmapHeight = (pageHeight * scale).toInt()
        
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        
        // Render the page to the bitmap with white background
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        
        return bitmap
    }

    private fun updatePageDisplay() {
        binding.textPageNumber.text = "${currentPageIndex + 1} / $totalPages"
        
        // Update navigation button states
        binding.btnPrevious.isEnabled = currentPageIndex > 0
        binding.btnNext.isEnabled = currentPageIndex < totalPages - 1
        
        binding.btnPrevious.alpha = if (currentPageIndex > 0) 1f else 0.3f
        binding.btnNext.alpha = if (currentPageIndex < totalPages - 1) 1f else 0.3f
    }

    override fun onDestroy() {
        super.onDestroy()
        timerCountDown?.cancel()
        try {
            currentPage?.close()
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val EXTRA_FILE_URI = "file_uri"
    }
}

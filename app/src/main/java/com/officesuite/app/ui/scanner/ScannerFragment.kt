package com.officesuite.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.officesuite.app.R
import com.officesuite.app.data.repository.DocumentConverter
import com.officesuite.app.databinding.FragmentScannerBinding
import com.officesuite.app.ocr.OcrManager
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val scannedPages = mutableListOf<Bitmap>()
    private val ocrManager = OcrManager()
    private lateinit var documentConverter: DocumentConverter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        documentConverter = DocumentConverter(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupClickListeners()
        checkCameraPermission()
        updatePageCount()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(context, "Use case binding failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setupClickListeners() {
        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnOcr.setOnClickListener {
            if (scannedPages.isNotEmpty()) {
                runOcrOnLastPage()
            } else {
                Toast.makeText(context, "Take a photo first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSavePdf.setOnClickListener {
            if (scannedPages.isNotEmpty()) {
                saveToPdf()
            } else {
                Toast.makeText(context, "Take photos first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClear.setOnClickListener {
            scannedPages.forEach { it.recycle() }
            scannedPages.clear()
            updatePageCount()
            Toast.makeText(context, "Cleared all pages", Toast.LENGTH_SHORT).show()
        }

        binding.btnGrayscale.setOnClickListener {
            if (scannedPages.isNotEmpty()) {
                applyGrayscaleFilter()
            }
        }

        binding.btnEnhance.setOnClickListener {
            if (scannedPages.isNotEmpty()) {
                applyContrastEnhancement()
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        binding.progressBar.visibility = View.VISIBLE

        val photoFile = FileUtils.createTempFile(requireContext(), "scan_", ".jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    binding.progressBar.visibility = View.GONE
                    processImage(photoFile)
                }
            }
        )
    }

    private fun processImage(photoFile: File) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    var bmp = BitmapFactory.decodeFile(photoFile.absolutePath)
                    bmp = ImageUtils.rotateBitmapIfNeeded(bmp, photoFile.absolutePath)
                    // Apply auto border detection (simplified)
                    bmp = detectAndCropBorders(bmp)
                    bmp
                }
                
                scannedPages.add(bitmap)
                updatePageCount()
                binding.imagePreview.setImageBitmap(bitmap)
                binding.imagePreview.visibility = View.VISIBLE
                
                Toast.makeText(context, "Page captured", Toast.LENGTH_SHORT).show()
                
                photoFile.delete()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Detects and crops document borders from the captured image.
     * 
     * Note: This is a simplified implementation that applies a basic margin crop.
     * For production use, consider integrating OpenCV or ML-based edge detection
     * for more accurate document boundary detection.
     * 
     * Current limitations:
     * - Does not perform actual edge detection
     * - Only applies a simple 2% margin crop
     * - May not work well with skewed or rotated documents
     * 
     * @param bitmap The input bitmap to process
     * @return The cropped bitmap with margins removed
     */
    private fun detectAndCropBorders(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Apply a 2% margin crop as a basic border removal
        // This is a placeholder for proper edge detection
        val margin = (minOf(width, height) * 0.02).toInt()
        
        return try {
            Bitmap.createBitmap(
                bitmap,
                margin,
                margin,
                width - (margin * 2),
                height - (margin * 2)
            )
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun runOcrOnLastPage() {
        val lastPage = scannedPages.lastOrNull() ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.textOcrResult.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val result = ocrManager.extractText(lastPage)
                
                binding.progressBar.visibility = View.GONE
                
                if (result.success) {
                    binding.textOcrResult.text = result.fullText
                    binding.textOcrResult.visibility = View.VISIBLE
                    Toast.makeText(context, "Text extracted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "OCR failed: ${result.error}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "OCR error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveToPdf() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val outputFile = File(
                    FileUtils.getOutputDirectory(requireContext()),
                    "scan_${System.currentTimeMillis()}.pdf"
                )
                
                val success = withContext(Dispatchers.IO) {
                    documentConverter.createPdfFromBitmaps(scannedPages, outputFile)
                }
                
                binding.progressBar.visibility = View.GONE
                
                if (success) {
                    Toast.makeText(context, "PDF saved: ${outputFile.name}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyGrayscaleFilter() {
        if (scannedPages.isEmpty()) return
        
        val lastIndex = scannedPages.lastIndex
        val lastPage = scannedPages[lastIndex]
        
        lifecycleScope.launch {
            val filtered = withContext(Dispatchers.Default) {
                ImageUtils.applyGrayscaleFilter(lastPage)
            }
            
            scannedPages[lastIndex].recycle()
            scannedPages[lastIndex] = filtered
            binding.imagePreview.setImageBitmap(filtered)
            Toast.makeText(context, "Grayscale filter applied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyContrastEnhancement() {
        if (scannedPages.isEmpty()) return
        
        val lastIndex = scannedPages.lastIndex
        val lastPage = scannedPages[lastIndex]
        
        lifecycleScope.launch {
            val enhanced = withContext(Dispatchers.Default) {
                ImageUtils.applyContrastEnhancement(lastPage)
            }
            
            scannedPages[lastIndex].recycle()
            scannedPages[lastIndex] = enhanced
            binding.imagePreview.setImageBitmap(enhanced)
            Toast.makeText(context, "Contrast enhanced", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePageCount() {
        binding.textPageCount.text = "${scannedPages.size} pages"
    }

    override fun onPause() {
        super.onPause()
        // Clear bitmap preview to reduce memory usage when fragment is paused
        _binding?.imagePreview?.setImageBitmap(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        ocrManager.close()
        // Recycle all bitmaps to free memory
        scannedPages.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        scannedPages.clear()
        _binding = null
    }
}

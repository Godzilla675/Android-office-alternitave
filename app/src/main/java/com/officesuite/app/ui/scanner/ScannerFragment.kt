package com.officesuite.app.ui.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.officesuite.app.R
import com.officesuite.app.data.model.DocumentType
import com.officesuite.app.data.repository.DocumentConverter
import com.officesuite.app.data.repository.PreferencesRepository
import com.officesuite.app.databinding.FragmentScannerBinding
import com.officesuite.app.ocr.OcrLanguage
import com.officesuite.app.ocr.OcrManager
import com.officesuite.app.scanner.BarcodeScanner
import com.officesuite.app.scanner.BusinessCardScanner
import com.officesuite.app.scanner.IdDocumentScanner
import com.officesuite.app.scanner.ReceiptScanner
import com.officesuite.app.scanner.ScanResult
import com.officesuite.app.utils.DocumentBorderDetector
import com.officesuite.app.utils.FileUtils
import com.officesuite.app.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Scanner modes supported by the app
 */
enum class ScanMode {
    DOCUMENT,
    QR_CODE,
    BUSINESS_CARD,
    ID_DOCUMENT,
    RECEIPT
}

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private val scannedPages = mutableListOf<Bitmap>()
    private val ocrManager = OcrManager()
    private lateinit var documentConverter: DocumentConverter
    private lateinit var preferencesRepository: PreferencesRepository
    private val borderDetector = DocumentBorderDetector()
    private var autoBorderEnabled = true
    
    // Image analysis resolution for border detection
    private var analysisWidth = ANALYSIS_WIDTH
    private var analysisHeight = ANALYSIS_HEIGHT
    
    // Flag to prevent processing too many frames
    private val isProcessingFrame = AtomicBoolean(false)
    private var lastDetectedCorners: DocumentBorderDetector.DetectedCorners? = null
    
    // New scanner instances for different modes
    private val barcodeScanner = BarcodeScanner()
    private lateinit var businessCardScanner: BusinessCardScanner
    private lateinit var idDocumentScanner: IdDocumentScanner
    private lateinit var receiptScanner: ReceiptScanner
    
    // Current scan mode
    private var currentScanMode = ScanMode.DOCUMENT
    
    companion object {
        private const val ANALYSIS_WIDTH = 640
        private const val ANALYSIS_HEIGHT = 480
    }
    // Last scan result for action buttons
    private var lastScanResult: Any? = null

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
        preferencesRepository = PreferencesRepository(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Initialize specialized scanners
        businessCardScanner = BusinessCardScanner(ocrManager)
        idDocumentScanner = IdDocumentScanner(ocrManager)
        receiptScanner = ReceiptScanner(ocrManager)
        
        setupScanModeChips()
        setupClickListeners()
        checkCameraPermission()
        updatePageCount()
        updateOcrLanguageDisplay()
    }
    
    private fun setupScanModeChips() {
        binding.chipGroupScanMode.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            currentScanMode = when (checkedIds.first()) {
                R.id.chipDocument -> ScanMode.DOCUMENT
                R.id.chipQrCode -> ScanMode.QR_CODE
                R.id.chipBusinessCard -> ScanMode.BUSINESS_CARD
                R.id.chipIdDocument -> ScanMode.ID_DOCUMENT
                R.id.chipReceipt -> ScanMode.RECEIPT
                else -> ScanMode.DOCUMENT
            }
            
            // Update UI based on scan mode
            updateUiForScanMode()
        }
    }
    
    private fun updateUiForScanMode() {
        // Hide result card when switching modes
        binding.cardScanResult.visibility = View.GONE
        
        // Show/hide document-specific controls based on mode
        val isDocumentMode = currentScanMode == ScanMode.DOCUMENT
        binding.textPageCount.visibility = if (isDocumentMode) View.VISIBLE else View.GONE
        binding.textAutoBorderStatus.visibility = if (isDocumentMode) View.VISIBLE else View.GONE
        binding.btnGrayscale.visibility = if (isDocumentMode) View.VISIBLE else View.GONE
        binding.btnEnhance.visibility = if (isDocumentMode) View.VISIBLE else View.GONE
        binding.btnOcr.visibility = if (isDocumentMode) View.VISIBLE else View.GONE
        binding.btnSavePdf.visibility = if (isDocumentMode) View.VISIBLE else View.GONE
        
        // Show OCR language only for document mode
        binding.textOcrLanguage.visibility = if (isDocumentMode) View.VISIBLE else View.GONE
    }
    
    private fun updateOcrLanguageDisplay() {
        val language = ocrManager.getCurrentLanguage()
        binding.textOcrLanguage.text = "OCR: ${language.displayName.substringBefore(" ")}"
    }
    
    private fun showOcrLanguageDialog() {
        val languages = OcrLanguage.values()
        val languageNames = languages.map { it.displayName }.toTypedArray()
        val currentIndex = languages.indexOf(ocrManager.getCurrentLanguage())
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                ocrManager.setLanguage(languages[which])
                updateOcrLanguageDisplay()
                dialog.dismiss()
                Toast.makeText(context, "OCR language set to ${languages[which].displayName}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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

            // Create image analyzer for live border detection
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processFrameForBorderDetection(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(context, "Use case binding failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Process camera frames for live document border detection.
     * Detects document edges and displays overlay on camera preview.
     */
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processFrameForBorderDetection(imageProxy: ImageProxy) {
        // Skip if not in document mode or auto border is disabled
        if (currentScanMode != ScanMode.DOCUMENT || !autoBorderEnabled) {
            imageProxy.close()
            return
        }

        // Skip if already processing a frame
        if (!isProcessingFrame.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                val corners = borderDetector.detectBorders(bitmap)
                
                // Update UI on main thread
                activity?.runOnUiThread {
                    if (_binding != null && autoBorderEnabled && currentScanMode == ScanMode.DOCUMENT) {
                        if (corners != null && corners.confidence >= 0.3f) {
                            lastDetectedCorners = corners
                            binding.borderOverlay.setCorners(corners, bitmap.width, bitmap.height)
                            binding.borderOverlay.updateConfidenceColor(corners.confidence)
                        } else {
                            binding.borderOverlay.clearCorners()
                        }
                    }
                }
                
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isProcessingFrame.set(false)
            imageProxy.close()
        }
    }

    /**
     * Convert ImageProxy to Bitmap for border detection analysis.
     * Properly handles YUV_420_888 format from CameraX.
     */
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        
        // Store actual analysis dimensions
        analysisWidth = image.width
        analysisHeight = image.height
        
        return try {
            // CameraX uses YUV_420_888 format
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            // Create NV21 byte array for YuvImage
            // NV21 format: Y plane followed by interleaved VU
            val nv21 = ByteArray(ySize + uSize + vSize)

            // Copy Y plane
            yBuffer.get(nv21, 0, ySize)
            
            // For YUV_420_888, U and V planes may have pixel stride > 1
            // We need to properly interleave them for NV21
            val vRowStride = image.planes[2].rowStride
            val vPixelStride = image.planes[2].pixelStride
            
            if (vPixelStride == 2) {
                // U and V are already interleaved (common case)
                // V plane contains interleaved VU data
                vBuffer.get(nv21, ySize, vSize)
            } else {
                // Manual interleaving needed
                val chromaHeight = image.height / 2
                val chromaWidth = image.width / 2
                var vuIndex = ySize
                
                for (row in 0 until chromaHeight) {
                    for (col in 0 until chromaWidth) {
                        val vIndex = row * vRowStride + col * vPixelStride
                        val uIndex = row * image.planes[1].rowStride + col * image.planes[1].pixelStride
                        
                        if (vIndex < vSize) {
                            vBuffer.position(vIndex)
                            nv21[vuIndex++] = vBuffer.get()
                        }
                        if (uIndex < uSize) {
                            uBuffer.position(uIndex)
                            nv21[vuIndex++] = uBuffer.get()
                        }
                    }
                }
            }

            val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 50, out)
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
            binding.borderOverlay.clearCorners()
            binding.cardScanResult.visibility = View.GONE
            binding.imagePreview.visibility = View.GONE
            Toast.makeText(context, "Cleared all pages", Toast.LENGTH_SHORT).show()
        }

        binding.btnGrayscale.setOnClickListener {
            if (scannedPages.isNotEmpty()) {
                applyGrayscaleFilter()
            } else {
                Toast.makeText(context, "Take a photo first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEnhance.setOnClickListener {
            if (scannedPages.isNotEmpty()) {
                applyContrastEnhancement()
            } else {
                Toast.makeText(context, "Take a photo first", Toast.LENGTH_SHORT).show()
            }
        }

        // Toggle auto border detection
        binding.textAutoBorderStatus.setOnClickListener {
            autoBorderEnabled = !autoBorderEnabled
            updateAutoBorderStatus()
            Toast.makeText(
                context, 
                if (autoBorderEnabled) "Auto border detection enabled" else "Auto border detection disabled",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // OCR Language selection
        binding.textOcrLanguage.setOnClickListener {
            showOcrLanguageDialog()
        }
        
        // Copy result button
        binding.btnCopyResult.setOnClickListener {
            val content = binding.textScanResultContent.text.toString()
            if (content.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Scan Result", content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Action result button (for URLs, contacts, etc.)
        binding.btnActionResult.setOnClickListener {
            handleScanResultAction()
        }
    }
    
    private fun handleScanResultAction() {
        when (val result = lastScanResult) {
            is ScanResult.Url -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open URL", Toast.LENGTH_SHORT).show()
                }
            }
            is ScanResult.Phone -> {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${result.number}"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not dial number", Toast.LENGTH_SHORT).show()
                }
            }
            is ScanResult.Email -> {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${result.address}")
                        putExtra(Intent.EXTRA_SUBJECT, result.subject ?: "")
                        putExtra(Intent.EXTRA_TEXT, result.body ?: "")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open email", Toast.LENGTH_SHORT).show()
                }
            }
            is BusinessCardScanner.BusinessCardInfo -> {
                saveBusinessCardToContacts(result)
            }
        }
    }
    
    private fun saveBusinessCardToContacts(info: BusinessCardScanner.BusinessCardInfo) {
        try {
            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.NAME, info.name ?: "")
                putExtra(ContactsContract.Intents.Insert.COMPANY, info.company ?: "")
                putExtra(ContactsContract.Intents.Insert.JOB_TITLE, info.title ?: "")
                if (info.emails.isNotEmpty()) {
                    putExtra(ContactsContract.Intents.Insert.EMAIL, info.emails.first())
                }
                if (info.phones.isNotEmpty()) {
                    putExtra(ContactsContract.Intents.Insert.PHONE, info.phones.first())
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not save contact", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAutoBorderStatus() {
        binding.textAutoBorderStatus.text = if (autoBorderEnabled) "Auto Border: ON" else "Auto Border: OFF"
        // Clear overlay when auto border is disabled
        if (!autoBorderEnabled) {
            binding.borderOverlay.clearCorners()
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
                    bmp
                }
                
                // Process based on current scan mode
                when (currentScanMode) {
                    ScanMode.DOCUMENT -> processDocumentScan(bitmap, photoFile)
                    ScanMode.QR_CODE -> processQrCodeScan(bitmap, photoFile)
                    ScanMode.BUSINESS_CARD -> processBusinessCardScan(bitmap, photoFile)
                    ScanMode.ID_DOCUMENT -> processIdDocumentScan(bitmap, photoFile)
                    ScanMode.RECEIPT -> processReceiptScan(bitmap, photoFile)
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun processDocumentScan(bitmap: Bitmap, photoFile: File) {
        val processedBitmap = withContext(Dispatchers.IO) {
            if (autoBorderEnabled) {
                // Use the last detected corners from live preview if available
                val corners = lastDetectedCorners
                if (corners != null && corners.confidence >= 0.3f) {
                    // Scale corners to match the full resolution bitmap
                    // Use the actual analysis dimensions instead of hardcoded values
                    val scaleX = bitmap.width.toFloat() / analysisWidth.toFloat()
                    val scaleY = bitmap.height.toFloat() / analysisHeight.toFloat()
                    val scaledCorners = DocumentBorderDetector.DetectedCorners(
                        topLeft = android.graphics.PointF(corners.topLeft.x * scaleX, corners.topLeft.y * scaleY),
                        topRight = android.graphics.PointF(corners.topRight.x * scaleX, corners.topRight.y * scaleY),
                        bottomLeft = android.graphics.PointF(corners.bottomLeft.x * scaleX, corners.bottomLeft.y * scaleY),
                        bottomRight = android.graphics.PointF(corners.bottomRight.x * scaleX, corners.bottomRight.y * scaleY),
                        confidence = corners.confidence
                    )
                    borderDetector.cropDocument(bitmap, scaledCorners)
                } else {
                    // Fall back to detecting borders from the full resolution image
                    detectAndCropBorders(bitmap)
                }
            } else {
                bitmap
            }
        }
        
        scannedPages.add(processedBitmap)
        updatePageCount()
        binding.imagePreview.setImageBitmap(processedBitmap)
        binding.imagePreview.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        
        // Clear the detected corners after use
        lastDetectedCorners = null
        binding.borderOverlay.clearCorners()
        
        Toast.makeText(context, "Page captured", Toast.LENGTH_SHORT).show()
        photoFile.delete()
    }
    
    private suspend fun processQrCodeScan(bitmap: Bitmap, photoFile: File) {
        val result = barcodeScanner.scan(bitmap)
        binding.progressBar.visibility = View.GONE
        
        if (result.success && result.results.isNotEmpty()) {
            val firstCode = result.results.first()
            showScanResult(
                title = getString(R.string.qr_code_detected),
                content = buildString {
                    appendLine("${getString(R.string.code_type)} ${firstCode.formatName}")
                    appendLine()
                    appendLine(firstCode.displayValue ?: firstCode.rawValue ?: "")
                }
            )
            
            // Store result for action button
            lastScanResult = firstCode.scanResult
            
            // Show action button for actionable results
            when (firstCode.scanResult) {
                is ScanResult.Url -> {
                    binding.btnActionResult.text = getString(R.string.open_url)
                    binding.btnActionResult.visibility = View.VISIBLE
                }
                is ScanResult.Phone -> {
                    binding.btnActionResult.text = getString(R.string.call)
                    binding.btnActionResult.visibility = View.VISIBLE
                }
                is ScanResult.Email -> {
                    binding.btnActionResult.text = getString(R.string.email)
                    binding.btnActionResult.visibility = View.VISIBLE
                }
                else -> {
                    binding.btnActionResult.visibility = View.GONE
                }
            }
        } else {
            Toast.makeText(context, getString(R.string.no_code_found), Toast.LENGTH_SHORT).show()
        }
        
        bitmap.recycle()
        photoFile.delete()
    }
    
    private suspend fun processBusinessCardScan(bitmap: Bitmap, photoFile: File) {
        val result = businessCardScanner.scan(bitmap)
        binding.progressBar.visibility = View.GONE
        
        showScanResult(
            title = getString(R.string.extracted_contact),
            content = buildString {
                result.name?.let { appendLine("${getString(R.string.contact_name)}: $it") }
                result.title?.let { appendLine("${getString(R.string.contact_title)}: $it") }
                result.company?.let { appendLine("${getString(R.string.contact_company)}: $it") }
                if (result.emails.isNotEmpty()) {
                    appendLine("${getString(R.string.contact_email)}: ${result.emails.joinToString(", ")}")
                }
                if (result.phones.isNotEmpty()) {
                    appendLine("${getString(R.string.contact_phone)}: ${result.phones.joinToString(", ")}")
                }
                if (result.websites.isNotEmpty()) {
                    appendLine("Website: ${result.websites.joinToString(", ")}")
                }
            }
        )
        
        // Store result and show save contact button
        lastScanResult = result
        binding.btnActionResult.text = getString(R.string.save_to_contacts)
        binding.btnActionResult.visibility = View.VISIBLE
        
        bitmap.recycle()
        photoFile.delete()
    }
    
    private suspend fun processIdDocumentScan(bitmap: Bitmap, photoFile: File) {
        val result = idDocumentScanner.scan(bitmap)
        binding.progressBar.visibility = View.GONE
        
        showScanResult(
            title = getString(R.string.scan_id_document),
            content = buildString {
                appendLine("${getString(R.string.document_type)}: ${result.documentType.name}")
                result.fullName?.let { appendLine("${getString(R.string.contact_name)}: $it") }
                result.dateOfBirthString?.let { appendLine("${getString(R.string.date_of_birth)}: $it") }
                result.documentNumber?.let { appendLine("${getString(R.string.document_number)}: $it") }
                result.expirationDateString?.let { appendLine("${getString(R.string.expiration_date)}: $it") }
                result.nationality?.let { appendLine("${getString(R.string.nationality)}: $it") }
                result.sex?.let { appendLine("${getString(R.string.sex)}: $it") }
            }
        )
        
        binding.btnActionResult.visibility = View.GONE
        
        bitmap.recycle()
        photoFile.delete()
    }
    
    private suspend fun processReceiptScan(bitmap: Bitmap, photoFile: File) {
        val result = receiptScanner.scan(bitmap)
        binding.progressBar.visibility = View.GONE
        
        val currency = result.currency ?: "$"
        showScanResult(
            title = getString(R.string.scan_receipt),
            content = buildString {
                result.merchantName?.let { appendLine("${getString(R.string.merchant)}: $it") }
                result.dateString?.let { appendLine("${getString(R.string.receipt_date)}: $it") }
                appendLine()
                if (result.lineItems.isNotEmpty()) {
                    appendLine("${getString(R.string.line_items)}:")
                    result.lineItems.forEach { item ->
                        appendLine("  • ${item.description}: $currency${item.totalPrice ?: ""}")
                    }
                    appendLine()
                }
                result.subtotal?.let { appendLine("${getString(R.string.subtotal)}: $currency$it") }
                result.tax?.let { appendLine("${getString(R.string.tax)}: $currency$it") }
                result.total?.let { appendLine("${getString(R.string.total)}: $currency$it") }
                result.paymentMethod?.let { appendLine("${getString(R.string.payment_method)}: $it") }
                appendLine()
                appendLine("${getString(R.string.expense_category)}: ${result.category.name.replace("_", " ")}")
            }
        )
        
        binding.btnActionResult.visibility = View.GONE
        
        bitmap.recycle()
        photoFile.delete()
    }
    
    private fun showScanResult(title: String, content: String) {
        binding.textScanResultTitle.text = title
        binding.textScanResultContent.text = content
        binding.cardScanResult.visibility = View.VISIBLE
    }

    /**
     * Detects and crops document borders from the captured image using edge detection.
     * Uses the DocumentBorderDetector to find document edges and applies perspective
     * correction to straighten the document.
     * 
     * @param bitmap The input bitmap to process
     * @return The cropped and perspective-corrected bitmap
     */
    private fun detectAndCropBorders(bitmap: Bitmap): Bitmap {
        if (!autoBorderEnabled) {
            return bitmap
        }
        
        return try {
            // Use the border detector to find document corners
            val corners = borderDetector.detectBorders(bitmap)
            
            if (corners != null && corners.confidence >= 0.3f) {
                // Crop and apply perspective transform
                borderDetector.cropDocument(bitmap, corners)
            } else {
                // Fallback: apply a small margin crop
                val width = bitmap.width
                val height = bitmap.height
                val margin = (minOf(width, height) * 0.02).toInt()
                
                Bitmap.createBitmap(
                    bitmap,
                    margin,
                    margin,
                    width - (margin * 2),
                    height - (margin * 2)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                // Create output directory if it doesn't exist
                val outputDir = FileUtils.getOutputDirectory(requireContext())
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                
                val fileName = "scan_${System.currentTimeMillis()}.pdf"
                val outputFile = File(outputDir, fileName)
                
                // Create searchable PDF with OCR text for selectable text
                val success = withContext(Dispatchers.IO) {
                    documentConverter.createSearchablePdfWithOcr(
                        scannedPages, 
                        outputFile,
                        ocrManager
                    )
                }
                
                binding.progressBar.visibility = View.GONE
                
                if (success && outputFile.exists() && outputFile.length() > 0) {
                    // Get URI for the saved file using FileProvider
                    val fileUri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        outputFile
                    )
                    
                    // Add to recent files so it appears in the home screen
                    preferencesRepository.addRecentFile(
                        uri = fileUri.toString(),
                        name = fileName,
                        type = DocumentType.PDF,
                        size = outputFile.length()
                    )
                    
                    Toast.makeText(context, "PDF saved: ${outputFile.name}", Toast.LENGTH_LONG).show()
                    
                    // Clear scanned pages after successful save
                    scannedPages.forEach { it.recycle() }
                    scannedPages.clear()
                    updatePageCount()
                    binding.imagePreview.visibility = View.GONE
                    binding.imagePreview.setImageBitmap(null)
                } else {
                    Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun applyGrayscaleFilter() {
        if (scannedPages.isEmpty()) return
        
        val lastIndex = scannedPages.lastIndex
        val lastPage = scannedPages[lastIndex]
        
        // Show progress indicator
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val filtered = withContext(Dispatchers.Default) {
                    // Create a copy to avoid recycling issues
                    val copy = lastPage.copy(Bitmap.Config.ARGB_8888, true)
                    ImageUtils.applyGrayscaleFilter(copy)
                }
                
                // Update the scanned page with the filtered version
                val oldPage = scannedPages[lastIndex]
                scannedPages[lastIndex] = filtered
                
                // Update the preview
                binding.imagePreview.setImageBitmap(filtered)
                binding.imagePreview.visibility = View.VISIBLE
                
                // Recycle the old page after updating
                if (oldPage != filtered && !oldPage.isRecycled) {
                    oldPage.recycle()
                }
                
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Grayscale filter applied", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to apply grayscale: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyContrastEnhancement() {
        if (scannedPages.isEmpty()) return
        
        val lastIndex = scannedPages.lastIndex
        val lastPage = scannedPages[lastIndex]
        
        // Show progress indicator
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val enhanced = withContext(Dispatchers.Default) {
                    // Create a copy to avoid recycling issues
                    val copy = lastPage.copy(Bitmap.Config.ARGB_8888, true)
                    ImageUtils.applyContrastEnhancement(copy)
                }
                
                // Update the scanned page with the enhanced version
                val oldPage = scannedPages[lastIndex]
                scannedPages[lastIndex] = enhanced
                
                // Update the preview
                binding.imagePreview.setImageBitmap(enhanced)
                binding.imagePreview.visibility = View.VISIBLE
                
                // Recycle the old page after updating
                if (oldPage != enhanced && !oldPage.isRecycled) {
                    oldPage.recycle()
                }
                
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Contrast enhanced", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to apply enhancement: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
        barcodeScanner.close()
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

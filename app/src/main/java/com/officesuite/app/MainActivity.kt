package com.officesuite.app

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.officesuite.app.databinding.ActivityMainBinding
import com.officesuite.app.ui.onboarding.OnboardingManager
import com.officesuite.app.widget.QuickActionsWidget

import com.officesuite.app.widget.QuickNotesWidget

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var isInPipMode = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleReceivedUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        checkPermissions()
        handleIntent(intent)
        
        // Check if onboarding should be shown
        checkOnboarding()
    }
    
    private fun checkOnboarding() {
        if (!OnboardingManager.isOnboardingComplete(this)) {
            // Navigate to onboarding
            navController.navigate(R.id.onboardingFragment)
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNav: BottomNavigationView = binding.bottomNavigation
        bottomNav.setupWithNavController(navController)
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }

        // Request manage external storage for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    handleReceivedUri(uri)
                }
            }
            Intent.ACTION_SEND -> {
                handleShareIntent(intent)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                handleMultipleShareIntent(intent)
            }
            QuickActionsWidget.ACTION_SCAN -> {
                navigateToScanner()
            }
            QuickActionsWidget.ACTION_OPEN_FILE -> {
                openFilePicker()
            }
            QuickActionsWidget.ACTION_CREATE_NEW -> {
                navigateToCreateNew()
            }
            QuickActionsWidget.ACTION_CONVERT -> {
                navigateToConverter()
            }
            // Quick Notes Widget actions
            QuickNotesWidget.ACTION_QUICK_NOTE -> {
                navigateToCreateNew()
            }
            QuickNotesWidget.ACTION_CHECKLIST -> {
                navigateToCreateNew() // Opens markdown for now, can be extended for dedicated checklist
            }
            QuickNotesWidget.ACTION_PHOTO_NOTE -> {
                navigateToScanner()
            }
        }
    }

    private fun handleReceivedUri(uri: Uri) {
        // Take persistable permission if available
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Permission not available, continue anyway
        }
        
        val mimeType = contentResolver.getType(uri)
        when {
            mimeType?.contains("pdf") == true -> {
                navigateToViewer(uri, "pdf")
            }
            mimeType?.contains("presentation") == true || 
            mimeType?.contains("pptx") == true -> {
                navigateToViewer(uri, "pptx")
            }
            mimeType?.contains("wordprocessing") == true ||
            mimeType?.contains("docx") == true -> {
                navigateToViewer(uri, "docx")
            }
            mimeType?.contains("spreadsheet") == true ||
            mimeType?.contains("xlsx") == true -> {
                navigateToViewer(uri, "xlsx")
            }
            mimeType?.contains("markdown") == true ||
            uri.path?.endsWith(".md") == true -> {
                navigateToViewer(uri, "md")
            }
            mimeType?.contains("text") == true ||
            uri.path?.endsWith(".txt") == true -> {
                navigateToViewer(uri, "md")
            }
            mimeType?.contains("image") == true -> {
                // Handle images - navigate to scanner for OCR or processing
                navigateToScanner()
            }
            else -> {
                Toast.makeText(this, getString(R.string.error_unsupported_format), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleShareIntent(intent: Intent) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        
        uri?.let {
            Toast.makeText(this, getString(R.string.file_received), Toast.LENGTH_SHORT).show()
            handleReceivedUri(it)
        } ?: run {
            // Handle plain text share
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                navigateToMarkdownWithContent(text)
            }
        }
    }

    private fun handleMultipleShareIntent(intent: Intent) {
        val uriList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
        
        uriList?.firstOrNull()?.let { firstUri ->
            Toast.makeText(this, getString(R.string.file_received), Toast.LENGTH_SHORT).show()
            handleReceivedUri(firstUri)
        }
    }

    private fun navigateToViewer(uri: Uri, type: String) {
        val bundle = Bundle().apply {
            putString("file_uri", uri.toString())
            putString("file_type", type)
        }
        
        when (type) {
            "pdf" -> navController.navigate(R.id.pdfViewerFragment, bundle)
            "pptx" -> navController.navigate(R.id.pptxViewerFragment, bundle)
            "docx" -> navController.navigate(R.id.docxViewerFragment, bundle)
            "xlsx" -> navController.navigate(R.id.xlsxViewerFragment, bundle)
            "md" -> navController.navigate(R.id.markdownFragment, bundle)
        }
    }

    private fun navigateToScanner() {
        navController.navigate(R.id.scannerFragment)
    }

    private fun navigateToConverter() {
        navController.navigate(R.id.converterFragment)
    }

    private fun navigateToCreateNew() {
        navController.navigate(R.id.markdownFragment)
    }

    private fun navigateToMarkdownWithContent(content: String) {
        val bundle = Bundle().apply {
            putString("initial_content", content)
        }
        navController.navigate(R.id.markdownFragment, bundle)
    }

    private fun openFilePicker() {
        openDocumentLauncher.launch(arrayOf("*/*"))
    }

    /**
     * Enter Picture-in-Picture mode for document viewing
     */
    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            enterPictureInPictureMode(pipParams)
        }
    }

    /**
     * Check if PiP mode is supported
     */
    fun isPipSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && 
               packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        
        // Hide or show UI elements based on PiP mode
        binding.bottomNavigation.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
    }

    fun isInPipMode(): Boolean = isInPipMode

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

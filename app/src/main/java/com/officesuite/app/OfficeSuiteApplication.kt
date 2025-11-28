package com.officesuite.app

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.officesuite.app.productivity.ProductivityManager
import com.officesuite.app.productivity.ScanPresetsManager
import com.officesuite.app.utils.ThemeManager

class OfficeSuiteApplication : MultiDexApplication() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Apply saved theme settings
        ThemeManager.applyTheme(this)
        
        // Initialize productivity features
        ProductivityManager.init(this)
        ScanPresetsManager.init(this)
    }

    companion object {
        lateinit var instance: OfficeSuiteApplication
            private set
    }
}

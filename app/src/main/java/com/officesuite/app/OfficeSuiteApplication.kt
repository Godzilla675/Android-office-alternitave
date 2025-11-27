package com.officesuite.app

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.officesuite.app.utils.ThemeManager

class OfficeSuiteApplication : MultiDexApplication() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Apply saved theme settings
        ThemeManager.applyTheme(this)
    }

    companion object {
        lateinit var instance: OfficeSuiteApplication
            private set
    }
}

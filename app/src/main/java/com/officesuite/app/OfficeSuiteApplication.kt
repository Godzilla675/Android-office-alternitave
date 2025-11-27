package com.officesuite.app

import android.app.Application
import androidx.multidex.MultiDexApplication

class OfficeSuiteApplication : MultiDexApplication() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OfficeSuiteApplication
            private set
    }
}

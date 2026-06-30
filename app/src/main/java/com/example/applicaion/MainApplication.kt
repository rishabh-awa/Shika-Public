package com.example.applicaion

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 🚀 CRUCIAL: Initializes Python directly at app startup
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}
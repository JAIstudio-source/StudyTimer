package com.madeby.JAI

import android.app.Application

class StudyTimerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppAnalytics.init(this)
    }
}

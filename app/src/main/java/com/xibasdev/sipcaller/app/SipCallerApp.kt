package com.xibasdev.sipcaller.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SipCallerApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}

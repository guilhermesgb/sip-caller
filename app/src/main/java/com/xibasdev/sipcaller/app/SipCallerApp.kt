package com.xibasdev.sipcaller.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SipCallerApp : Application() {

    @Inject lateinit var appLifecycleObserver: SipCallerAppLifecycleObserver

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }
}

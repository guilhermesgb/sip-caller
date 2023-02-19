package com.xibasdev.sipcaller.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.xibasdev.sipcaller.app.call.processing.CallProcessorApi
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

class AppLifecycleObserver @Inject constructor(
    private val callProcessor: CallProcessorApi
) : DefaultLifecycleObserver {

    private val disposables = CompositeDisposable()

    override fun onCreate(owner: LifecycleOwner) {
        callProcessor.startProcessing()
            .onErrorComplete()
            .subscribe()
            .addTo(disposables)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        disposables.dispose()
    }
}

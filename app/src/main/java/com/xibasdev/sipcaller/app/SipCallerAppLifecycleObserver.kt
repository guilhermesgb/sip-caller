package com.xibasdev.sipcaller.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.xibasdev.sipcaller.app.model.CallProcessor
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

class SipCallerAppLifecycleObserver @Inject constructor(
    private val callProcessor: CallProcessor
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

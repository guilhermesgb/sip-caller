package com.xibasdev.sipcaller.sip.linphone

import android.util.Log
import com.xibasdev.sipcaller.sip.SipEngineApi
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import javax.inject.Inject
import org.linphone.core.Core
import org.linphone.core.CoreListener
import org.linphone.core.CoreListenerStub
import org.linphone.core.GlobalState

private const val TAG = "SipEngine"

class LinphoneSipEngine @Inject constructor(
    private val linphoneCore: LinphoneCore
) : SipEngineApi {

    override fun startEngine(): Completable {
        return Completable.create { emitter ->

            when (linphoneCore.globalState) {
                GlobalState.Startup,
                GlobalState.Configuring,
                GlobalState.On -> {
                    Log.d(TAG, "Linphone already started.")

                    emitter.onComplete()
                }
                GlobalState.Shutdown,
                GlobalState.Off,
                GlobalState.Ready -> {
                    Log.d(TAG, "Starting Linphone...")

                    doStartEngine(emitter)
                }
            }
        }
    }

    override fun processEngineSteps(): Completable {
        return Completable.fromCallable {
            Log.d(TAG, "Iterating Linphone core...")
            linphoneCore.iterate()
        }
    }

    private fun doStartEngine(emitter: CompletableEmitter) {
        val globalStateChangeListener: CoreListener = object : CoreListenerStub() {
            override fun onGlobalStateChanged(
                core: Core,
                state: GlobalState?,
                message: String
            ) {

                when (state) {
                    GlobalState.Startup,
                    GlobalState.Configuring -> Log.d(TAG, "Linphone startup in progress...")
                    GlobalState.On -> {
                        Log.d(TAG, "Linphone startup complete!")

                        linphoneCore.removeListener(this)
                        emitter.onComplete()
                    }
                    GlobalState.Ready,
                    GlobalState.Off,
                    GlobalState.Shutdown,
                    null -> {
                        val errorMessage = "Failed to start Linphone core; in state: $state!"
                        val error = IllegalStateException(errorMessage)
                        Log.e(TAG, errorMessage, error)

                        linphoneCore.removeListener(this)
                        emitter.onError(error)
                    }
                }
            }
        }

        linphoneCore.addListener(globalStateChangeListener)

        val startupCode = linphoneCore.start()

        if (startupCode != 0) {
            val errorMessage = "Failed to start Linphone core; startup code: $startupCode!"
            val error = IllegalStateException(errorMessage)
            Log.e(TAG, errorMessage, error)

            linphoneCore.removeListener(globalStateChangeListener)
            emitter.onError(error)
        }
    }
}
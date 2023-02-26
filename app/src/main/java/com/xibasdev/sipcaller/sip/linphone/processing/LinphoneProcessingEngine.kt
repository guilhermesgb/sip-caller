package com.xibasdev.sipcaller.sip.linphone.processing

import com.elvishew.xlog.Logger
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneContextApi
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineApi
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineProcessingFailed
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineStartFailedAsync
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineStartFailedSync
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import javax.inject.Inject
import javax.inject.Named
import org.linphone.core.GlobalState.Configuring
import org.linphone.core.GlobalState.Off
import org.linphone.core.GlobalState.On
import org.linphone.core.GlobalState.Ready
import org.linphone.core.GlobalState.Shutdown
import org.linphone.core.GlobalState.Startup


class LinphoneProcessingEngine @Inject constructor(
    private val linphoneContext: LinphoneContextApi,
    @Named("SipEngineLogger") private val logger: Logger
): ProcessingEngineApi {

    override fun startEngine(): Completable {
        return Completable.create { emitter ->

            when (linphoneContext.getCurrentGlobalState()) {
                Startup,
                Configuring,
                On -> {
                    logger.d("Linphone core already started.")

                    emitter.onComplete()
                }
                Shutdown,
                Off,
                Ready -> {
                    logger.d("Starting Linphone core...")

                    doStartEngine(emitter)
                }
            }
        }
    }

    override fun processEngineSteps(): Completable {
        return Completable.fromCallable {
            logger.d("Iterating Linphone core...")

            try {
                linphoneContext.iterateLinphoneCore()

            } catch (cause: Throwable) {
                val errorMessage = "Failed to iterate Linphone core!"
                val error = ProcessingEngineProcessingFailed(message = errorMessage, cause = cause)
                logger.e(errorMessage, error)

                throw error
            }
        }
    }

    private fun doStartEngine(emitter: CompletableEmitter) {
        val globalStateChangeListenerId = linphoneContext.createGlobalStateChangeListener {
                globalState, thisCoreListenerId, errorReason ->

            when (globalState) {
                Startup,
                Configuring -> {}
                On -> {
                    logger.d("Linphone startup complete!")

                    linphoneContext.disableCoreListener(thisCoreListenerId)
                    emitter.onComplete()

                    linphoneContext.updateLinphoneCoreStarted(isStarted = true)
                }
                Ready,
                Off,
                Shutdown -> {
                    val errorMessage = "Failed to start Linphone core; " +
                            "reason: $errorReason; in state: $globalState!"
                    val error = ProcessingEngineStartFailedAsync(errorMessage)
                    logger.e(errorMessage, error)

                    linphoneContext.disableCoreListener(thisCoreListenerId)
                    emitter.onError(error)
                }
            }
        }

        linphoneContext.enableCoreListener(globalStateChangeListenerId)

        val startupCode = linphoneContext.startLinphoneCore()

        if (startupCode != 0) {
            val errorMessage = "Failed to start Linphone core; startup code: $startupCode; " +
                    "in state: ${linphoneContext.getCurrentGlobalState()}!"
            val error = ProcessingEngineStartFailedSync(errorMessage)
            logger.e(errorMessage, error)

            linphoneContext.disableCoreListener(globalStateChangeListenerId)
            emitter.onError(error)
        }
    }
}

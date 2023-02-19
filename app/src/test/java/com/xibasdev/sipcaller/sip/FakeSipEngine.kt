package com.xibasdev.sipcaller.sip

import com.xibasdev.sipcaller.app.call.processing.worker.CALL_PROCESSING_RATE_MS
import io.reactivex.rxjava3.core.Completable
import javax.inject.Inject

private const val MAX_TOLERATED_PROCESSING_DELTA_MS = CALL_PROCESSING_RATE_MS * 1.1

class FakeSipEngine @Inject constructor() : SipEngineApi {

    private var isEngineStarted = false
    private var lastProcessingTimeMs = -1L

    override fun startEngine(): Completable {
        return Completable.fromCallable {

            if (isEngineStarted) {
                println("SIP Engine processing is already started.")

            } else {
                println("Started SIP Engine processing...")
                isEngineStarted = true
            }
        }
    }

    override fun processEngineSteps(): Completable {
        return Completable.create { emitter ->

            val currentTimeMs = System.currentTimeMillis()

            if (lastProcessingTimeMs == -1L) {
                lastProcessingTimeMs = currentTimeMs
            }

            val processingDeltaMs = currentTimeMs - lastProcessingTimeMs
            lastProcessingTimeMs = currentTimeMs

            if (processingDeltaMs > MAX_TOLERATED_PROCESSING_DELTA_MS) {
                println("Failing SIP Engine processing - beyond tolerated delta of " +
                        "$MAX_TOLERATED_PROCESSING_DELTA_MS ms!")

                emitter.onError(IllegalStateException("SIP Engine processing after >= " +
                        "$MAX_TOLERATED_PROCESSING_DELTA_MS ms!"))

            } else {
                println("Continuing SIP Engine processing...")

                emitter.onComplete()
            }
        }
    }
}
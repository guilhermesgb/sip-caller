package com.xibasdev.sipcaller.sip

import com.xibasdev.sipcaller.processing.worker.CALL_PROCESSING_RATE_MS
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import com.xibasdev.sipcaller.sip.registering.AccountRegistrationUpdate
import com.xibasdev.sipcaller.sip.registering.account.AccountDisplayName
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import com.xibasdev.sipcaller.sip.registering.account.AccountUsername
import com.xibasdev.sipcaller.sip.registering.account.address.AccountAddress
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

private const val MAX_TOLERATED_PROCESSING_DELTA_MS = CALL_PROCESSING_RATE_MS * 1.1

class FakeSipEngine @Inject constructor() : SipEngineApi, FakeSipEngineApi {

    private var isEngineStarted = false
    private var lastProcessingTimeMs = -1L

    private var simulateSipEngineStuckInCoreIteration = false

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

            if (simulateSipEngineStuckInCoreIteration) {
                return@create
            }

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

    override fun simulateSipEngineStuckInCoreIteration() {
        simulateSipEngineStuckInCoreIteration = true
    }

    override fun observeCallHistory(): Observable<List<CallHistoryUpdate>> {
        TODO("Not yet implemented")
    }

    override fun observeRegistrations(): Observable<AccountRegistrationUpdate> {
        TODO("Not yet implemented")
    }

    override fun createRegistration(
        displayName: AccountDisplayName,
        username: AccountUsername,
        password: AccountPassword,
        address: AccountAddress,
        expirationMs: Int
    ): Completable {
        TODO("Not yet implemented")
    }

    override fun destroyRegistration(): Completable {
        TODO("Not yet implemented")
    }
}

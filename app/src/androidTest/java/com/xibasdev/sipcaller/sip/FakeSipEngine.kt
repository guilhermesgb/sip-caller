package com.xibasdev.sipcaller.sip

import com.xibasdev.sipcaller.app.model.worker.CALL_PROCESSING_RATE_MS
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.details.CallUpdate
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.registering.AccountRegistrationUpdate
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.time.OffsetDateTime
import javax.inject.Inject

private const val MAX_TOLERATED_PROCESSING_DELTA_MS = CALL_PROCESSING_RATE_MS * 1.1

class FakeSipEngine @Inject constructor() : SipEngineApi, FakeSipEngineApi {

    private var isEngineStarted = false
    private var lastProcessingTimeMs = -1L

    private var simulateFailureWhileProcessingEngineSteps = false

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

            if (simulateFailureWhileProcessingEngineSteps) {
                emitter.onError(IllegalStateException("Fake error while processing engine steps!"))
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

    override fun observeRegistrations(): Observable<AccountRegistrationUpdate> {
        TODO("Not yet implemented")
    }

    override fun createRegistration(
        account: AccountInfo,
        password: AccountPassword,
        expirationMs: Int
    ): Completable {
        TODO("Not yet implemented")
    }

    override fun destroyRegistration(): Completable {
        TODO("Not yet implemented")
    }

    override fun observeIdentity(): Observable<IdentityUpdate> {
        TODO("Not yet implemented")
    }

    override fun setLocalIdentityProtocolInfo(protocolInfo: ProtocolInfo): Completable {
        TODO("Not yet implemented")
    }

    override fun observeCallHistory(offset: OffsetDateTime): Observable<List<CallHistoryUpdate>> {
        TODO("Not yet implemented")
    }

    override fun observeCallDetails(callId: CallId): Observable<CallUpdate> {
        TODO("Not yet implemented")
    }

    override fun sendCallInvitation(account: AccountInfo): Single<CallId> {
        TODO("Not yet implemented")
    }

    override fun cancelCallInvitation(callId: CallId): Completable {
        TODO("Not yet implemented")
    }

    override fun acceptCallInvitation(callId: CallId): Completable {
        TODO("Not yet implemented")
    }

    override fun declineCallInvitation(callId: CallId): Completable {
        TODO("Not yet implemented")
    }

    override fun terminateCallSession(callId: CallId): Completable {
        TODO("Not yet implemented")
    }

    override fun simulateFailureWhileProcessingEngineSteps() {
        simulateFailureWhileProcessingEngineSteps = true
    }

    override fun revertFailureSimulationWhileProcessingEngineSteps() {
        simulateFailureWhileProcessingEngineSteps = false
    }
}

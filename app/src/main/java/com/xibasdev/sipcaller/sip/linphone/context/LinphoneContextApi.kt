package com.xibasdev.sipcaller.sip.linphone.context

import android.view.Surface
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.features.CallFeatures
import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.TreeMap
import org.linphone.core.GlobalState

abstract class LinphoneContextApi (private val scheduler: Scheduler) {

    private val isStartedUpdates = BehaviorSubject.createDefault(false)
    private val stoppedDisposables = CompositeDisposable()
    private val wasCallFinishedByLocalParty = TreeMap<String, Boolean>()

    abstract fun getCurrentGlobalState(): GlobalState

    abstract fun createGlobalStateChangeListener(
        callback: (globalStateChange: GlobalState, errorReason: String, coreListenerId: Int) -> Unit
    ): Int

    abstract fun createAccountRegistrationStateChangeListener(
        callback: (
            callStateChange: LinphoneAccountRegistrationStateChange,
            coreListenerId: Int
        ) -> Unit
    ): Int

    abstract fun createNetworkReachableListener(
        callback: (isNetworkReachable: Boolean, coreListenerId: Int) -> Unit
    ): Int

    abstract fun createCallStateChangeListener(
        callback: (callStateChange: LinphoneCallStateChange, coreListenerId: Int) -> Unit
    ): Int

    abstract fun createCallStatsChangeListener(
        callback: (callStatsChange: LinphoneCallStatsChange, coreListenerId: Int) -> Unit
    ): Int

    abstract fun enableCoreListener(coreListenerId: Int)

    abstract fun disableCoreListener(coreListenerId: Int)

    abstract fun resolveNetworkCurrentlyReachable(): Boolean

    abstract fun startLinphoneCore(): Int

    fun updateLinphoneCoreStarted(isStarted: Boolean) {
        isStartedUpdates.onNext(isStarted)

        if (!isStarted) {
            stoppedDisposables.clear()
        }
    }

    abstract fun iterateLinphoneCore()

    fun <T : Any> doWhenLinphoneCoreStartsOrStops(
        subject: Subject<T>,
        operations: (isLinphoneCoreStarted: Boolean) -> Observable<T>
    ) {

        isStartedUpdates
            .distinctUntilChanged()
            .switchMap(operations)
            .onErrorComplete()
            .subscribeOn(scheduler)
            .subscribeBy(
                onNext = { result ->

                    subject.onNext(result)
                }
            )
            .addTo(stoppedDisposables)
    }

    abstract fun resolvePrimaryContactIpAddress(): String?

    abstract fun getPrimaryContactProtocolInfo(): ProtocolInfo?

    abstract fun setPrimaryContactProtocolInfo(protocolInfo: ProtocolInfo): Boolean

    abstract fun createAccount(
        idKey: String,
        accountInfo: AccountInfo,
        password: AccountPassword,
        expirationMs: Int
    ): Boolean

    abstract fun deactivateAccount(idKey: String): Boolean

    abstract fun destroyAccount(
        idKey: String,
        accountInfo: AccountInfo,
        password: AccountPassword
    ): Boolean

    /**
     * TODO allow specification of desired initial call parameters at this point
     */
    abstract fun sendCallInvitation(account: AccountInfo): Boolean

    abstract fun cancelCallInvitation(callId: CallId): Boolean

    abstract fun acceptCallInvitation(callId: CallId): Boolean

    /**
     * TODO allow specification of reason for declination of call invitation
     */
    abstract fun declineCallInvitation(callId: CallId): Boolean

    abstract fun terminateCallSession(callId: CallId): Boolean

    abstract fun isCurrentlyHandlingCall(): Boolean

    abstract fun enableOrDisableCallFeatures(callId: CallId, features: CallFeatures): Boolean

    abstract fun setLocalSurface(surface: Surface): Boolean

    abstract fun unsetLocalSurface(): Boolean

    abstract fun setRemoteSurface(surface: Surface): Boolean

    abstract fun unsetRemoteSurface(): Boolean

    internal fun setCallFinishedByLocalParty(callId: CallId) {
        wasCallFinishedByLocalParty[callId.value] = true
    }

    internal fun wasCallFinishedByLocalParty(callId: CallId): Single<Boolean> {
        return Single.just(wasCallFinishedByLocalParty.getOrDefault(callId.value, false))
    }
}

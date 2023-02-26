package com.xibasdev.sipcaller.sip.linphone.context

import com.xibasdev.sipcaller.sip.SipCallId
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.TreeMap
import org.linphone.core.GlobalState

abstract class LinphoneContextApi {

    private val isStartedUpdates = BehaviorSubject.createDefault(false)
    private val stoppedDisposables = CompositeDisposable()
    private val wasCallFinishedByLocalParty = TreeMap<String, Boolean>()

    abstract fun getCurrentGlobalState(): GlobalState

    abstract fun createGlobalStateChangeListener(
        callback: (globalState: GlobalState, coreListenerId: Int, errorReason: String) -> Unit
    ): Int

    abstract fun createCallStateChangeListener(
        callback: (callStateChange: LinphoneCallStateChange, coreListenerId: Int) -> Unit
    ): Int

    abstract fun enableCoreListener(coreListenerId: Int)

    abstract fun disableCoreListener(coreListenerId: Int)

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
            .subscribeBy(
                onNext = { result ->

                    subject.onNext(result)
                }
            )
            .addTo(stoppedDisposables)
    }

    fun setCallFinishedByLocalParty(callId: SipCallId) {
        wasCallFinishedByLocalParty[callId.value] = true
    }

    fun wasCallFinishedByLocalParty(callId: SipCallId): Single<Boolean> {
        return Single.just(wasCallFinishedByLocalParty.getOrDefault(callId.value, false))
    }
}

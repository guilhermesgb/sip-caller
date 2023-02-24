package com.xibasdev.sipcaller.sip.linphone

import com.xibasdev.sipcaller.sip.SipCallId
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import javax.inject.Inject

class LinphoneContext @Inject constructor() {

    private val isStartedUpdates = BehaviorSubject.createDefault(false)
    private val stoppedDisposables = CompositeDisposable()

    fun updateLinphoneStarted(isStarted: Boolean) {
        isStartedUpdates.onNext(isStarted)

        if (!isStarted) {
            stoppedDisposables.clear()
        }
    }

    fun <T : Any> updateWhileLinphoneStarted(
        subject: Subject<T>,
        operations: (Boolean) -> Observable<T>
    ) {

        isStartedUpdates
            .switchMap(operations)
            .onErrorComplete()
            .subscribeBy(
                onNext = { result ->

                    subject.onNext(result)
                }
            )
            .addTo(stoppedDisposables)
    }

    fun wasCallFinishedByLocalParty(callId: SipCallId): Single<Boolean> {
        return Single.just(true)  // TODO implement this.
    }
}

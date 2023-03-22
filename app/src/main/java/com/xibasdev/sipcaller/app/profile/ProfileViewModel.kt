package com.xibasdev.sipcaller.app.profile

import androidx.lifecycle.ViewModel
import com.xibasdev.sipcaller.app.profile.events.AcceptCallInvitationFailed
import com.xibasdev.sipcaller.app.profile.events.CallInvitationAccepted
import com.xibasdev.sipcaller.app.profile.events.CallInvitationCanceled
import com.xibasdev.sipcaller.app.profile.events.CallInvitationDeclined
import com.xibasdev.sipcaller.app.profile.events.CallInvitationSent
import com.xibasdev.sipcaller.app.profile.events.CallSessionTerminated
import com.xibasdev.sipcaller.app.profile.events.CancelCallInvitationFailed
import com.xibasdev.sipcaller.app.profile.events.DeclineCallInvitationFailed
import com.xibasdev.sipcaller.app.profile.events.ProfileScreenEvent
import com.xibasdev.sipcaller.app.profile.events.RegistrationCreateFailed
import com.xibasdev.sipcaller.app.profile.events.RegistrationCreated
import com.xibasdev.sipcaller.app.profile.events.RegistrationDestroyFailed
import com.xibasdev.sipcaller.app.profile.events.RegistrationDestroyed
import com.xibasdev.sipcaller.app.profile.events.SendCallInvitationFailed
import com.xibasdev.sipcaller.app.profile.events.TerminateCallSessionFailed
import com.xibasdev.sipcaller.app.utils.Indexed
import com.xibasdev.sipcaller.processing.AccountRegistry
import com.xibasdev.sipcaller.processing.CallDetailsObserver
import com.xibasdev.sipcaller.processing.CallHistoryObserver
import com.xibasdev.sipcaller.processing.CallStateManager
import com.xibasdev.sipcaller.processing.IdentityResolver
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.CallUpdate
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import com.xibasdev.sipcaller.sip.history.CallInProgressUpdate
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import com.xibasdev.sipcaller.sip.registering.AccountRegistrationUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountRegistry: AccountRegistry,
    private val identityResolver: IdentityResolver,
    private val callHistoryObserver: CallHistoryObserver,
    private val callDetailsObserver: CallDetailsObserver,
    private val callStateManager: CallStateManager
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val profileScreenEvents = BehaviorSubject.create<ProfileScreenEvent>()

    private val createRegistrationPipeline = PublishSubject.create<String>().apply {
        continuousDebouncer(
            onCompleteEvent = RegistrationCreated,
            onErrorEventProvider = { error ->

                RegistrationCreateFailed(error)
            },
            completableOperationProvider = { rawAccountRegistration ->

                accountRegistry.createRegistration(rawAccountRegistration)
            }
        )
    }

    private val sendCallInvitationPipeline = PublishSubject.create<String>().apply {
        continuousDebouncer(
            onCompleteEvent = CallInvitationSent,
            onErrorEventProvider = { error ->

                SendCallInvitationFailed(error)
            },
            completableOperationProvider = { rawDestinationAccount ->

                callStateManager.sendCallInvitation(rawDestinationAccount)
                    .ignoreElement()
            }
        )
    }

    fun observeRegistrations(): Observable<List<Indexed<AccountRegistrationUpdate>>> {
        return accountRegistry.observeRegistrations()
    }
    fun createRegistration(rawAccountRegistration: String) {
        createRegistrationPipeline.onNext(rawAccountRegistration)
    }

    fun destroyRegistration() {
        accountRegistry.destroyRegistration()
            .propagateResultAsEvent(RegistrationDestroyed) { error ->

                RegistrationDestroyFailed(error)
            }
    }

    fun sendCallInvitation(rawDestinationAccount: String) {
        sendCallInvitationPipeline.onNext(rawDestinationAccount)
    }

    fun cancelCallInvitation(callId: CallId) {
        callStateManager.cancelCallInvitation(callId)
            .propagateResultAsEvent(CallInvitationCanceled) { error ->

                CancelCallInvitationFailed(error)
            }
    }

    fun acceptCallInvitation(callId: CallId) {
        callStateManager.acceptCallInvitation(callId)
            .propagateResultAsEvent(CallInvitationAccepted) { error ->

                AcceptCallInvitationFailed(error)
            }
    }

    fun declineCallInvitation(callId: CallId) {
        callStateManager.declineCallInvitation(callId)
            .propagateResultAsEvent(CallInvitationDeclined) { error ->

                DeclineCallInvitationFailed(error)
            }
    }

    fun terminateCallSession(callId: CallId) {
        callStateManager.terminateCallSession(callId)
            .propagateResultAsEvent(CallSessionTerminated) { error ->

                TerminateCallSessionFailed(error)
            }
    }

    fun observeIdentity(): Observable<IdentityUpdate> {
        return identityResolver.observeIdentity()
    }

    fun observeCallHistory(offset: OffsetDateTime): Observable<List<Indexed<CallHistoryUpdate>>> {
        return callHistoryObserver.observeCallHistory(offset)
    }

    fun observeCallDetails(offset: OffsetDateTime): Observable<CallUpdate> {
        return callHistoryObserver.observeCallHistory(offset)
            .flatMap { updates ->

                Observable.fromIterable(updates)
            }
            .map { update ->

                update.value
            }
            .scan { previous, next ->

                if (previous.callId == next.callId) {
                    previous

                } else {
                    next
                }
            }
            .distinctUntilChanged()
            .flatMap { update ->

                if (update is CallInProgressUpdate) {
                    callDetailsObserver.observeCallDetails(update.callId)

                } else {
                    Observable.empty()
                }
            }
    }

    private fun <T : Any> Observable<T>.continuousDebouncer(
        onCompleteEvent: ProfileScreenEvent,
        onErrorEventProvider: (Throwable) -> ProfileScreenEvent,
        completableOperationProvider: (T) -> Completable
    ) {

        debounce(500, MILLISECONDS)
            .switchMapCompletable {

                completableOperationProvider(it)
            }
            .continuouslyPropagateResultAsEvent(onCompleteEvent) { error ->

                onErrorEventProvider(error)
            }
    }

    private fun Completable.continuouslyPropagateResultAsEvent(
        onCompleteEvent: ProfileScreenEvent,
        onErrorEventProvider: (Throwable) -> ProfileScreenEvent
    ) {

        doOnComplete {
            profileScreenEvents.onNext(onCompleteEvent)
        }.doOnError { error ->

            profileScreenEvents.onNext(onErrorEventProvider(error))
        }.onErrorResumeWith {
            continuouslyPropagateResultAsEvent(onCompleteEvent, onErrorEventProvider)
        }.subscribe()
            .addTo(disposables)
    }

    private fun Completable.propagateResultAsEvent(
        onCompleteEvent: ProfileScreenEvent,
        onErrorEventProvider: (Throwable) -> ProfileScreenEvent
    ) = subscribeBy(
        onComplete = {
            profileScreenEvents.onNext(onCompleteEvent)
        },
        onError = { error ->

            profileScreenEvents.onNext(onErrorEventProvider(error))
        }
    ).addTo(disposables)

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}

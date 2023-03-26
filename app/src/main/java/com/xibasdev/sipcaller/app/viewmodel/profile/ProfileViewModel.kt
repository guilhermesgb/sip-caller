package com.xibasdev.sipcaller.app.viewmodel.profile

import com.xibasdev.sipcaller.app.viewmodel.common.BaseViewModel
import com.xibasdev.sipcaller.app.viewmodel.profile.events.AcceptCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.profile.events.CallInvitationAccepted
import com.xibasdev.sipcaller.app.viewmodel.profile.events.CallInvitationCanceled
import com.xibasdev.sipcaller.app.viewmodel.profile.events.CallInvitationDeclined
import com.xibasdev.sipcaller.app.viewmodel.profile.events.CallInvitationSent
import com.xibasdev.sipcaller.app.viewmodel.profile.events.CallSessionTerminated
import com.xibasdev.sipcaller.app.viewmodel.profile.events.CancelCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.profile.events.DeclineCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.profile.events.RegistrationCreateFailed
import com.xibasdev.sipcaller.app.viewmodel.profile.events.RegistrationCreated
import com.xibasdev.sipcaller.app.viewmodel.profile.events.RegistrationDestroyFailed
import com.xibasdev.sipcaller.app.viewmodel.profile.events.RegistrationDestroyed
import com.xibasdev.sipcaller.app.viewmodel.profile.events.SendCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.profile.events.TerminateCallSessionFailed
import com.xibasdev.sipcaller.app.viewmodel.common.Indexed
import com.xibasdev.sipcaller.app.model.SipEngineClient
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.details.CallUpdate
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import com.xibasdev.sipcaller.sip.history.CallInProgressUpdate
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import com.xibasdev.sipcaller.sip.registering.AccountRegistrationUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sipEngineClient: SipEngineClient
) : BaseViewModel() {

    private val createRegistrationPipeline = PublishSubject.create<String>().apply {
        continuousDebouncer(
            onCompleteEvent = RegistrationCreated,
            onErrorEventProvider = { error ->

                RegistrationCreateFailed(error)
            },
            completableOperationProvider = { rawAccountRegistration ->

                sipEngineClient.createRegistration(rawAccountRegistration)
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

                sipEngineClient.sendCallInvitation(rawDestinationAccount)
                    .ignoreElement()
            }
        )
    }

    fun observeRegistrations(): Observable<List<Indexed<AccountRegistrationUpdate>>> {
        return Observable
            .zip(
                Observable.interval(500, MILLISECONDS),
                sipEngineClient.observeRegistrations()
            ) { index, update ->

                Indexed(index, update)
            }
            .scan(listOf<Indexed<AccountRegistrationUpdate>>()) { previous, next ->

                val filteredPrevious = previous
                    .drop(Integer.max(0, previous.size - 4))
                    .toTypedArray()

                listOf(*filteredPrevious, next)
            }
            .map { updates ->

                updates.reversed()
            }
    }
    fun createRegistration(rawAccountRegistration: String) {
        createRegistrationPipeline.onNext(rawAccountRegistration)
    }

    fun destroyRegistration() {
        sipEngineClient.destroyRegistration()
            .propagateResultAsEvent(RegistrationDestroyed) { error ->

                RegistrationDestroyFailed(error)
            }
    }

    fun observeIdentity(): Observable<IdentityUpdate> {
        return sipEngineClient.observeIdentity()
    }

    fun observeCallHistory(offset: OffsetDateTime): Observable<List<Indexed<CallHistoryUpdate>>> {
        return Observable
            .zip(
                Observable.interval(500, MILLISECONDS),
                sipEngineClient.observeCallHistory(offset)
                    .flatMap { updates ->

                        Observable.fromIterable(updates)
                    }
            ) { index, update ->

                Indexed(index, update)
            }
            .scan(listOf<Indexed<CallHistoryUpdate>>()) { previous, next ->

                val filteredPrevious = previous
                    .drop(Integer.max(0, previous.size - 9))
                    .toTypedArray()

                listOf(*filteredPrevious, next)
            }
            .map { updates ->

                updates.reversed()
            }
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun observeCallDetails(offset: OffsetDateTime): Observable<CallUpdate> {
        return observeCallHistory(offset)
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
                    sipEngineClient.observeCallDetails(update.callId)

                } else {
                    Observable.empty()
                }
            }
    }

    fun sendCallInvitation(rawDestinationAccount: String) {
        sendCallInvitationPipeline.onNext(rawDestinationAccount)
    }

    fun cancelCallInvitation(callId: CallId) {
        sipEngineClient.cancelCallInvitation(callId)
            .propagateResultAsEvent(CallInvitationCanceled) { error ->

                CancelCallInvitationFailed(error)
            }
    }

    fun acceptCallInvitation(callId: CallId) {
        sipEngineClient.acceptCallInvitation(callId)
            .propagateResultAsEvent(CallInvitationAccepted) { error ->

                AcceptCallInvitationFailed(error)
            }
    }

    fun declineCallInvitation(callId: CallId) {
        sipEngineClient.declineCallInvitation(callId)
            .propagateResultAsEvent(CallInvitationDeclined) { error ->

                DeclineCallInvitationFailed(error)
            }
    }

    fun terminateCallSession(callId: CallId) {
        sipEngineClient.terminateCallSession(callId)
            .propagateResultAsEvent(CallSessionTerminated) { error ->

                TerminateCallSessionFailed(error)
            }
    }
}

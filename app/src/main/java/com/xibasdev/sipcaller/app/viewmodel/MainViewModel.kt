package com.xibasdev.sipcaller.app.viewmodel

import com.xibasdev.sipcaller.app.model.SipEngineClient
import com.xibasdev.sipcaller.app.viewmodel.common.BaseViewModel
import com.xibasdev.sipcaller.app.viewmodel.common.Indexed
import com.xibasdev.sipcaller.app.viewmodel.events.account.registering.CreatingRegistration
import com.xibasdev.sipcaller.app.viewmodel.events.account.registering.RegistrationCreateFailed
import com.xibasdev.sipcaller.app.viewmodel.events.account.registering.RegistrationCreated
import com.xibasdev.sipcaller.app.viewmodel.events.account.unregistering.DestroyingRegistration
import com.xibasdev.sipcaller.app.viewmodel.events.account.unregistering.RegistrationDestroyFailed
import com.xibasdev.sipcaller.app.viewmodel.events.account.unregistering.RegistrationDestroyed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.send.CallInvitationSent
import com.xibasdev.sipcaller.app.viewmodel.events.calling.send.SendCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.send.SendingCallInvitation
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
class MainViewModel @Inject constructor(
    private val sipEngineClient: SipEngineClient
) : BaseViewModel() {

    private val createRegistrationPipeline = PublishSubject.create<String>().apply {
        debounceAndContinuouslyPropagateResultAsEvent(
            onRunningEvent = CreatingRegistration,
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
        debounceAndContinuouslyPropagateResultAsEvent(
            onRunningEvent = SendingCallInvitation,
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

                listOf(*previous.toTypedArray(), next)
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
            .propagateResultAsEvent(DestroyingRegistration, RegistrationDestroyed) { error ->

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

                listOf(*previous.toTypedArray(), next)
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
}

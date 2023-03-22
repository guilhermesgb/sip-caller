package com.xibasdev.sipcaller.sip.linphone.registering

import com.elvishew.xlog.Logger
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneAccountRegistrationStateChange
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneContextApi
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry.InternalAccountRegistrationIntent.ACTIVATE
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry.InternalAccountRegistrationIntent.DEACTIVATE
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry.InternalAccountRegistrationIntent.UNDEFINED
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry.InternalAccountRegistrationStatus.ACTIVATED
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry.InternalAccountRegistrationStatus.CREATED
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry.InternalAccountRegistrationStatus.DESTROYED
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry.InternalAccountRegistrationStatus.FAILED
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry.InternalAccountRegistrationStatus.REQUESTED
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry.InternalAccountRegistrationStatus.UPDATING
import com.xibasdev.sipcaller.sip.registering.AccountRegistrationUpdate
import com.xibasdev.sipcaller.sip.registering.AccountRegistryApi
import com.xibasdev.sipcaller.sip.registering.NoAccountRegistered
import com.xibasdev.sipcaller.sip.registering.RegisterAccountFailed
import com.xibasdev.sipcaller.sip.registering.RegisterId
import com.xibasdev.sipcaller.sip.registering.RegisteredAccount
import com.xibasdev.sipcaller.sip.registering.RegisteringAccount
import com.xibasdev.sipcaller.sip.registering.RegistryOffline
import com.xibasdev.sipcaller.sip.registering.UnregisterAccountFailed
import com.xibasdev.sipcaller.sip.registering.UnregisteredAccount
import com.xibasdev.sipcaller.sip.registering.UnregisteringAccount
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.TreeMap
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import org.linphone.core.RegistrationState.Cleared
import org.linphone.core.RegistrationState.Failed
import org.linphone.core.RegistrationState.None
import org.linphone.core.RegistrationState.Ok
import org.linphone.core.RegistrationState.Progress

@Singleton
class LinphoneAccountRegistry @Inject constructor(
    @Named("LinphoneRxScheduler") private val scheduler: Scheduler,
    private val linphoneContext: LinphoneContextApi,
    @Named("SipEngineLogger") private val logger: Logger
) : AccountRegistryApi {

    private enum class InternalAccountRegistrationIntent {
        UNDEFINED,
        ACTIVATE,
        DEACTIVATE
    }

    private enum class InternalAccountRegistrationStatus {
        REQUESTED,
        CREATED,
        UPDATING,
        ACTIVATED,
        FAILED,
        DESTROYED
    }

    private data class InternalAccountRegistration(
        val registerId: RegisterId = RegisterId(UUID.randomUUID().toString()),
        val intent: InternalAccountRegistrationIntent = UNDEFINED,
        val status: InternalAccountRegistrationStatus = DESTROYED,
        val account: AccountInfo = AccountInfo(),
        val password: AccountPassword = AccountPassword("********"),
        val expirationMs: Int = 0,
        val errorReason: String = ""
    )

    private val accountRegistrationStateChangeListenerId = linphoneContext
        .createAccountRegistrationStateChangeListener { accountRegistrationStateChange, _ ->

            with (accountRegistrationStateChange) {
                processAccountRegistrationStateChange()
            }
        }

    private val accountRegistrationUpdates = TreeMap<String, InternalAccountRegistration>()
    private val latestAccountRegistrationUpdates = BehaviorSubject
        .createDefault(InternalAccountRegistration())

    private val exposedAccountRegistrationUpdates = BehaviorSubject
        .create<AccountRegistrationUpdate>().apply {
            processAccountRegistrationHistoryUpdates(this)
        }

    override fun observeRegistrations(): Observable<AccountRegistrationUpdate> {
        return exposedAccountRegistrationUpdates
    }

    context (LinphoneAccountRegistrationStateChange)
    private fun processAccountRegistrationStateChange() {
        val status = when (state) {
            None -> CREATED
            Progress -> UPDATING
            Ok -> ACTIVATED
            Failed -> FAILED
            Cleared -> DESTROYED
        }

        accountRegistrationUpdates[idKey]?.let { previousUpdate ->

            val nextUpdate = previousUpdate.copy(
                status = status,
                errorReason = errorReason
            )
            accountRegistrationUpdates[idKey] = nextUpdate
            latestAccountRegistrationUpdates.onNext(nextUpdate)
        }
    }

    override fun createRegistration(
        account: AccountInfo,
        password: AccountPassword,
        expirationMs: Int
    ): Completable {

        val requestedRegistration = InternalAccountRegistration(
            registerId = RegisterId(UUID.randomUUID().toString()),
            intent = ACTIVATE,
            status = REQUESTED,
            account = account,
            password = password,
            expirationMs = expirationMs
        )

        return doCreateRegistration(requestedRegistration)
    }

    private fun doCreateRegistration(
        requested: InternalAccountRegistration,
    ): Completable {

        return latestAccountRegistrationUpdates
            .subscribeOn(scheduler)
            .firstElement()
            .flatMapCompletable { registration ->

                if (registration.intent == ACTIVATE) {
                    return@flatMapCompletable handleExistingRegistration(
                        registration,
                        requested
                    )
                }

                latestAccountRegistrationUpdates
                    .startWith(Single.just(requested))
                    .doOnSubscribe {
                        createAccountForRegistration(requested)
                    }
                    .filter { nextUpdate -> nextUpdate.registerId == requested.registerId &&
                            nextUpdate.intent == ACTIVATE
                    }
                    .takeUntil { nextUpdate -> nextUpdate.status == ACTIVATED }
                    .flatMapCompletable { nextUpdate ->

                        if (nextUpdate.status == FAILED) {
                            destroyRegistration()
                                .onErrorComplete()
                                .andThen(
                                    Completable.error(
                                        IllegalStateException(
                                            "Failed (async) to register using " +
                                                    "account ${nextUpdate.account}!"
                                        )
                                    )
                                )

                        } else {
                            Completable.complete()
                        }
                    }
            }
    }

    private fun handleExistingRegistration(
        latestRegistrationUpdate: InternalAccountRegistration,
        requestedRegistration: InternalAccountRegistration
    ): Completable {

        if (latestRegistrationUpdate.account == requestedRegistration.account
                && latestRegistrationUpdate.status in listOf(CREATED, UPDATING, ACTIVATED)) {

            return Completable.complete()
        }

        return destroyRegistration()
            .andThen(doCreateRegistration(requestedRegistration))
    }

    private fun createAccountForRegistration(registration: InternalAccountRegistration) {
        accountRegistrationUpdates[registration.registerId.value] = registration

        val accountCreated = linphoneContext.createAccount(
            idKey = registration.registerId.value,
            accountInfo = registration.account,
            password = registration.password,
            expirationMs = registration.expirationMs
        )

        if (!accountCreated) {
            accountRegistrationUpdates.remove(registration.registerId.value)

            throw IllegalStateException(
                "Failed (sync) to create account ${registration.account} for registration!"
            )
        }
    }

    override fun destroyRegistration(): Completable {
        return latestAccountRegistrationUpdates
            .subscribeOn(scheduler)
            .firstElement()
            .flatMapCompletable { registration ->

                if (registration.intent != ACTIVATE) {
                    return@flatMapCompletable handleExistingUnregistration(registration)

                } else if (registration.status == FAILED) {
                    return@flatMapCompletable Completable
                        .fromCallable {
                            deactivateRegistrationOfAccount(registration)
                        }.andThen(
                            Completable.fromCallable { destroyAccount(registration) }
                        )
                }

                latestAccountRegistrationUpdates
                    .startWith(Single.just(registration.copy(
                        intent = DEACTIVATE,
                        status = REQUESTED
                    )))
                    .doOnSubscribe {
                        deactivateRegistrationOfAccount(registration)
                    }
                    .filter { nextUpdate ->
                        nextUpdate.registerId == registration.registerId &&
                                nextUpdate.intent == DEACTIVATE
                    }
                    .takeUntil { nextUpdate -> nextUpdate.status == DESTROYED }
                    .flatMapCompletable { nextUpdate ->

                        if (nextUpdate.status == FAILED) {
                            Completable.error(
                                IllegalStateException(
                                    "Failed (async) to deactivate registration " +
                                            "for account ${nextUpdate.account}!"
                                )
                            )

                        } else {
                            Completable.complete()
                        }
                    }
                    .andThen(Completable.fromCallable { destroyAccount(registration) })
            }
    }

    private fun handleExistingUnregistration(
        registration: InternalAccountRegistration
    ): Completable {

        if (registration.intent == DEACTIVATE
                && registration.status in listOf(UPDATING, DESTROYED)) {

            return Completable.complete()
        }

        return latestAccountRegistrationUpdates
            .takeUntil { nextUpdate -> nextUpdate.intent != UNDEFINED }
            .ignoreElements()
    }

    private fun deactivateRegistrationOfAccount(registration: InternalAccountRegistration) {
        val accountDeactivated = linphoneContext.deactivateAccount(
            idKey = registration.registerId.value
        )

        if (accountDeactivated) {
            accountRegistrationUpdates[registration.registerId.value] = registration.copy(
                intent = DEACTIVATE
            )

        } else {
            throw IllegalStateException(
                "Failed (sync) to deactivate registration of account ${registration.account}!"
            )
        }
    }

    private fun destroyAccount(registration: InternalAccountRegistration) {
        val accountDestroyed = linphoneContext.destroyAccount(
            idKey = registration.registerId.value,
            accountInfo = registration.account,
            password = registration.password
        )

        if (accountDestroyed) {
            accountRegistrationUpdates.remove(registration.registerId.value)

        } else {
            throw IllegalStateException(
                "Failed (sync) to destroy account ${registration.account}!"
            )
        }
    }

    private fun processAccountRegistrationHistoryUpdates(
        subject: BehaviorSubject<AccountRegistrationUpdate>
    ) {

        with (linphoneContext) {
            doWhenLinphoneCoreStartsOrStops(subject) { isLinphoneCoreStarted ->

                logger.d("Account registrations observer detected Linphone core " +
                        (if (isLinphoneCoreStarted) "start!" else "stop!"))

                if (isLinphoneCoreStarted) {
                    enableCoreListener(accountRegistrationStateChangeListenerId)

                    if (shouldSignalNoAccountRegistered(subject)) {
                        subject.onNext(NoAccountRegistered)
                    }

                    latestAccountRegistrationUpdates
                        .filter { registrationUpdate -> registrationUpdate.intent != UNDEFINED }
                        .flatMap { registrationUpdate ->

                            when (registrationUpdate.status) {
                                UPDATING -> when (registrationUpdate.intent) {
                                    UNDEFINED -> Observable.empty()
                                    ACTIVATE -> Observable.just(
                                        RegisteringAccount(account = registrationUpdate.account)
                                    )
                                    DEACTIVATE -> Observable.just(
                                        UnregisteringAccount(account = registrationUpdate.account)
                                    )
                                }
                                ACTIVATED -> Observable.just(
                                    RegisteredAccount(account = registrationUpdate.account)
                                )
                                FAILED -> when (registrationUpdate.intent) {
                                    UNDEFINED -> Observable.empty()
                                    ACTIVATE -> Observable.just(
                                        RegisterAccountFailed(
                                            account = registrationUpdate.account,
                                            errorReason = registrationUpdate.errorReason
                                        ),
                                        NoAccountRegistered
                                    )
                                    DEACTIVATE -> Observable.just(
                                        UnregisterAccountFailed(
                                            account = registrationUpdate.account,
                                            errorReason = registrationUpdate.errorReason
                                        ),
                                        NoAccountRegistered
                                    )
                                }
                                DESTROYED -> Observable.just(
                                    UnregisteredAccount(account = registrationUpdate.account),
                                    NoAccountRegistered
                                )
                                else -> Observable.empty()
                            }
                        }

                } else {
                    disableCoreListener(accountRegistrationStateChangeListenerId)

                    Observable.just(RegistryOffline)
                }
            }
        }
    }

    private fun shouldSignalNoAccountRegistered(
        subject: BehaviorSubject<AccountRegistrationUpdate>
    ): Boolean {

        return subject.value == RegistryOffline
    }
}

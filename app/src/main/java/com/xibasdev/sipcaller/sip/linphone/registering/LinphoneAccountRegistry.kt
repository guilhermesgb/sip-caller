package com.xibasdev.sipcaller.sip.linphone.registering

import com.elvishew.xlog.Logger
import com.xibasdev.sipcaller.sip.SipRegisterId
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
import com.xibasdev.sipcaller.sip.registering.RegisteredAccount
import com.xibasdev.sipcaller.sip.registering.RegisteringAccount
import com.xibasdev.sipcaller.sip.registering.RegistryOffline
import com.xibasdev.sipcaller.sip.registering.UnregisterAccountFailed
import com.xibasdev.sipcaller.sip.registering.UnregisteredAccount
import com.xibasdev.sipcaller.sip.registering.UnregisteringAccount
import com.xibasdev.sipcaller.sip.registering.account.AccountDisplayName
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import com.xibasdev.sipcaller.sip.registering.account.AccountUsername
import com.xibasdev.sipcaller.sip.registering.account.address.AccountAddress
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.TreeMap
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import org.linphone.core.RegistrationState.Cleared
import org.linphone.core.RegistrationState.Failed
import org.linphone.core.RegistrationState.None
import org.linphone.core.RegistrationState.Ok
import org.linphone.core.RegistrationState.Progress

class LinphoneAccountRegistry @Inject constructor(
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
        val registerId: SipRegisterId = SipRegisterId(UUID.randomUUID().toString()),
        val intent: InternalAccountRegistrationIntent = UNDEFINED,
        val status: InternalAccountRegistrationStatus = DESTROYED,
        val account: AccountInfo = AccountInfo(),
        val password: AccountPassword = AccountPassword("********"),
        val expirationMs: Int = 0,
        val errorReason: String = ""
    )

    override fun observeRegistrations(): Observable<AccountRegistrationUpdate> {
        return exposedAccountRegistrationUpdates
    }

    private val accountRegistrationUpdates = TreeMap<String, InternalAccountRegistration>()
    private val latestAccountRegistrationUpdates = BehaviorSubject
        .createDefault(InternalAccountRegistration())

    private val accountRegistrationStateChangeListenerId = linphoneContext
        .createAccountRegistrationStateChangeListener { accountRegistrationStateChange, _ ->

            with (accountRegistrationStateChange) {
                processAccountRegistrationStateChange()
            }
        }

    private val exposedAccountRegistrationUpdates = BehaviorSubject
        .create<AccountRegistrationUpdate>().apply {
            processAccountRegistrationHistoryUpdates(this)
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
        displayName: AccountDisplayName,
        username: AccountUsername,
        password: AccountPassword,
        address: AccountAddress,
        expirationMs: Int
    ): Completable {

        val requestedAccountInfo = AccountInfo(
            displayName = displayName,
            username = username,
            address = address
        )

        val requestedRegistration = InternalAccountRegistration(
            registerId = SipRegisterId(UUID.randomUUID().toString()),
            intent = ACTIVATE,
            status = REQUESTED,
            account = requestedAccountInfo,
            password = password,
            expirationMs = expirationMs
        )

        return doCreateRegistration(requestedRegistration)
    }

    private fun doCreateRegistration(
        requestedRegistration: InternalAccountRegistration,
    ): Completable {

        return latestAccountRegistrationUpdates.firstElement().flatMapCompletable { registration ->

            if (registration.intent == ACTIVATE) {
                return@flatMapCompletable handleExistingRegistration(
                    registration,
                    requestedRegistration
                )
            }

            latestAccountRegistrationUpdates
                .startWith(Single.just(requestedRegistration))
                .doOnSubscribe {
                    createAccountForRegistration(requestedRegistration)
                }
                .filter { nextUpdate -> nextUpdate.registerId == requestedRegistration.registerId }
                .takeUntil { nextUpdate -> nextUpdate.status == ACTIVATED }
                .flatMapCompletable { nextUpdate ->

                    if (nextUpdate.status == FAILED) {
                        Completable.error(
                            IllegalStateException(
                                "Failed (async) to register using " +
                                        "account ${nextUpdate.account}!"
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
        return latestAccountRegistrationUpdates.firstElement().flatMapCompletable { registration ->

            if (registration.intent != ACTIVATE) {
                return@flatMapCompletable handleExistingUnregistration(registration)
            }

            latestAccountRegistrationUpdates
                .startWith(Single.just(registration.copy(
                    intent = DEACTIVATE,
                    status = REQUESTED
                )))
                .doOnSubscribe {
                    deactivateRegistrationOfAccount(registration)
                }
                .filter { nextUpdate -> nextUpdate.registerId == registration.registerId }
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

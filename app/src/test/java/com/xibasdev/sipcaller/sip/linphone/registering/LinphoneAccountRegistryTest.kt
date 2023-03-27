package com.xibasdev.sipcaller.sip.linphone.registering

import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog
import com.xibasdev.sipcaller.sip.linphone.LinphoneSipEngine
import com.xibasdev.sipcaller.sip.linphone.calling.details.LinphoneCallDetailsObserver
import com.xibasdev.sipcaller.sip.linphone.calling.features.LinphoneCallFeaturesManager
import com.xibasdev.sipcaller.sip.linphone.calling.state.LinphoneCallStateManager
import com.xibasdev.sipcaller.sip.linphone.context.FakeLinphoneContext
import com.xibasdev.sipcaller.sip.linphone.history.LinphoneCallHistoryObserver
import com.xibasdev.sipcaller.sip.linphone.identity.LinphoneIdentityResolver
import com.xibasdev.sipcaller.sip.linphone.processing.LinphoneProcessingEngine
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineApi
import com.xibasdev.sipcaller.sip.registering.AccountRegistryApi
import com.xibasdev.sipcaller.sip.registering.NoAccountRegistered
import com.xibasdev.sipcaller.sip.registering.RegisterAccountFailed
import com.xibasdev.sipcaller.sip.registering.RegisteredAccount
import com.xibasdev.sipcaller.sip.registering.RegisteringAccount
import com.xibasdev.sipcaller.sip.registering.RegistryOffline
import com.xibasdev.sipcaller.sip.registering.UnregisterAccountFailed
import com.xibasdev.sipcaller.sip.registering.UnregisteredAccount
import com.xibasdev.sipcaller.sip.registering.UnregisteringAccount
import com.xibasdev.sipcaller.test.Account.createFirstTestRegistration
import com.xibasdev.sipcaller.test.Account.createSecondTestRegistration
import com.xibasdev.sipcaller.test.AccountRegistryFixtures.ACCOUNT_1
import com.xibasdev.sipcaller.test.AccountRegistryFixtures.ACCOUNT_2
import com.xibasdev.sipcaller.test.Completable.prepareInForeground
import com.xibasdev.sipcaller.test.Completable.simulateAfterDelay
import com.xibasdev.sipcaller.test.Observable.prepareInForeground
import com.xibasdev.sipcaller.test.TEST_SCHEDULER
import com.xibasdev.sipcaller.test.XLogRule
import com.xibasdev.sipcaller.test.simulateWaitUpToTimeout
import io.reactivex.rxjava3.core.Observable
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LinphoneAccountRegistryTest {

    @get:Rule
    val xLogRule = XLogRule()

    private lateinit var logger: Logger
    private lateinit var clock: Clock
    private lateinit var linphoneContext: FakeLinphoneContext
    private lateinit var processingEngine: ProcessingEngineApi
    private lateinit var accountRegistry: AccountRegistryApi

    @Before
    fun setUp() {
        logger = XLog.tag("LinphoneAccountRegistryTest").build()
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

        linphoneContext = FakeLinphoneContext(TEST_SCHEDULER)

        val processingEngine = LinphoneProcessingEngine(
            linphoneContext, logger
        )
        val accountRegistry = LinphoneAccountRegistry(
            TEST_SCHEDULER, linphoneContext, logger
        )
        val identityResolver = LinphoneIdentityResolver(
            TEST_SCHEDULER, linphoneContext, accountRegistry, logger
        )
        val callHistoryObserver = LinphoneCallHistoryObserver(
            linphoneContext, identityResolver, logger, clock
        )
        val callDetailsObserver = LinphoneCallDetailsObserver(
            linphoneContext, callHistoryObserver, logger, clock
        )
        val callStateManager = LinphoneCallStateManager(
            TEST_SCHEDULER, linphoneContext, logger
        )
        val callFeaturesManager = LinphoneCallFeaturesManager(
            TEST_SCHEDULER, linphoneContext, callDetailsObserver
        )
        val sipEngine = LinphoneSipEngine(
            processingEngine, accountRegistry, identityResolver, callHistoryObserver,
            callDetailsObserver, callStateManager, callFeaturesManager
        )

        this.processingEngine = sipEngine
        this.accountRegistry = sipEngine
    }

    @Test
    fun `before engine is started, account registration updates cannot be observed`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertNoErrors()
    }

    @Test
    fun `after engine is started, signal of no registered account present can be observed`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        processingEngine.startEngine()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(2)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertNoErrors()
    }

    @Test
    fun `before engine is started, account registration stays pending`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = accountRegistry.createFirstTestRegistration()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout(60, MINUTES)

        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, RegistryOffline)

        completable.assertNotComplete()
    }

    @Test
    fun `after engine is started, account can be registered and updates can be observed`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(4)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertNoErrors()

        completable.assertComplete()
    }

    @Test
    fun `once engine is started, pending account registration is performed`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = accountRegistry
            .createFirstTestRegistration()
            .prepareInForeground()

        processingEngine.startEngine()
            .simulateAfterDelay(1, SECONDS)
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(4)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertNoErrors()

        completable.assertComplete()
    }

    @Test
    fun `multiple subsequent registrations for the same account are no-op`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable1 = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        val completable2 = accountRegistry.createFirstTestRegistration()
            .simulateAfterDelay(1, SECONDS)
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(4)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertNoErrors()

        completable1.assertComplete()
        completable2.assertComplete()
    }

    @Test
    fun `an existing account registration can be destroyed`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .andThen(accountRegistry.destroyRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(7)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(4, UnregisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(5, UnregisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(6, NoAccountRegistered)
        observable.assertNoErrors()

        completable.assertComplete()
    }

    @Test
    fun `multiple destruction commands are no-op`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .andThen(accountRegistry.destroyRegistration())
            .andThen(accountRegistry.destroyRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(7)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(4, UnregisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(5, UnregisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(6, NoAccountRegistered)
        observable.assertNoErrors()

        completable.assertComplete()
    }

    @Test
    fun `before engine is started, account destruction stays pending`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = accountRegistry.destroyRegistration()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout(60, MINUTES)

        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, RegistryOffline)

        completable.assertNotComplete()
    }

    @Test
    fun `before an existing account registration is ever done, a destruction stays pending`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.destroyRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(2)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertNoErrors()

        completable.assertNotComplete()
    }

    @Test
    fun `once engine is started, pending account destruction is performed`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = accountRegistry.createFirstTestRegistration()
            .andThen(accountRegistry.destroyRegistration())
            .prepareInForeground()

        processingEngine.startEngine()
            .simulateAfterDelay(1, SECONDS)
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout(4, SECONDS)

        observable.assertNotComplete()
        observable.assertValueCount(7)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(4, UnregisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(5, UnregisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(6, NoAccountRegistered)
        observable.assertNoErrors()

        completable.assertComplete()
    }

    @Test
    fun `once engine is started, scheduled account registrations are performed in sequence`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = accountRegistry.createFirstTestRegistration()
            .andThen(accountRegistry.createSecondTestRegistration())
            .prepareInForeground()

        processingEngine.startEngine()
            .simulateAfterDelay(1, SECONDS)
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout(4, SECONDS)

        observable.assertNotComplete()
        observable.assertValueCount(9)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(4, UnregisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(5, UnregisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(6, NoAccountRegistered)
        observable.assertValueAt(7, RegisteringAccount(account = ACCOUNT_2))
        observable.assertValueAt(8, RegisteredAccount(account = ACCOUNT_2))
        observable.assertNoErrors()

        completable.assertComplete()
    }

    @Test
    fun `after engine is started, last account registrations prevails`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .andThen(accountRegistry.createSecondTestRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout(4, SECONDS)

        observable.assertNotComplete()
        observable.assertValueCount(9)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(4, UnregisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(5, UnregisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(6, NoAccountRegistered)
        observable.assertValueAt(7, RegisteringAccount(account = ACCOUNT_2))
        observable.assertValueAt(8, RegisteredAccount(account = ACCOUNT_2))
        observable.assertNoErrors()

        completable.assertComplete()
    }

    @Test
    fun `after engine is started, last account destruction prevails`() {
        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .andThen(accountRegistry.createSecondTestRegistration())
            .andThen(accountRegistry.destroyRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout(4, SECONDS)

        observable.assertNotComplete()
        observable.assertValueCount(12)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(4, UnregisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(5, UnregisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(6, NoAccountRegistered)
        observable.assertValueAt(7, RegisteringAccount(account = ACCOUNT_2))
        observable.assertValueAt(8, RegisteredAccount(account = ACCOUNT_2))
        observable.assertValueAt(9, UnregisteringAccount(account = ACCOUNT_2))
        observable.assertValueAt(10, UnregisteredAccount(account = ACCOUNT_2))
        observable.assertValueAt(11, NoAccountRegistered)
        observable.assertNoErrors()

        completable.assertComplete()
    }

    @Test
    fun `synchronous failure to create registration is observed`() {
        linphoneContext.failSynchronouslyOnAccountCreation()

        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(2)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertNoErrors()

        completable.assertNotComplete()
        completable.assertError(IllegalStateException::class.java)
    }

    @Test
    fun `asynchronous failure to activate registration is observed`() {
        linphoneContext.failAsynchronouslyOnAccountRegistration()

        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(5)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3) { update ->

            update is RegisterAccountFailed && update.account == ACCOUNT_1
        }
        observable.assertValueAt(4, NoAccountRegistered)
        observable.assertNoErrors()

        completable.assertNotComplete()
        completable.assertError(IllegalStateException::class.java)
    }

    @Test
    fun `if stuck while registering account, no error is observed`() {
        linphoneContext.simulateStuckWhileRegisteringAccount()

        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertNoErrors()

        completable.assertNotComplete()
        completable.assertNoErrors()
    }

    @Test
    fun `synchronous failure to deactivate account is observed`() {
        linphoneContext.failSynchronouslyOnAccountDeactivation()

        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .andThen(accountRegistry.destroyRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(7)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertNoErrors()

        completable.assertNotComplete()
        completable.assertError(IllegalStateException::class.java)
    }

    @Test
    fun `asynchronous failure to unregister account is observed`() {
        linphoneContext.failAsynchronouslyOnAccountUnregistration()

        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .andThen(accountRegistry.destroyRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(7)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(4, UnregisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(5) { update ->

            update is UnregisterAccountFailed && update.account == ACCOUNT_1
        }
        observable.assertValueAt(6, NoAccountRegistered)
        observable.assertNoErrors()

        completable.assertNotComplete()
        completable.assertError(IllegalStateException::class.java)
    }

    @Test
    fun `synchronous failure to destroy registration is observed`() {
        linphoneContext.failSynchronouslyOnAccountDestruction()

        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .andThen(accountRegistry.destroyRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(7)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(4, UnregisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(5, UnregisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(6, NoAccountRegistered)
        observable.assertNoErrors()

        completable.assertNotComplete()
        completable.assertError(IllegalStateException::class.java)
    }

    @Test
    fun `if stuck while unregistering account, no error is observed`() {
        linphoneContext.simulateStuckWhileUnregisteringAccount()

        val observable = accountRegistry.observeRegistrations()
            .prepareInForeground()

        val completable = processingEngine.startEngine()
            .andThen(accountRegistry.createFirstTestRegistration())
            .andThen(accountRegistry.destroyRegistration())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(5)
        observable.assertValueAt(0, RegistryOffline)
        observable.assertValueAt(1, NoAccountRegistered)
        observable.assertValueAt(2, RegisteringAccount(account = ACCOUNT_1))
        observable.assertValueAt(3, RegisteredAccount(account = ACCOUNT_1))
        observable.assertValueAt(4, UnregisteringAccount(account = ACCOUNT_1))
        observable.assertNoErrors()

        completable.assertNotComplete()
        completable.assertNoErrors()
    }

    // TODO Test that an existing account registration can be scheduled for destruction once engine resumes
}

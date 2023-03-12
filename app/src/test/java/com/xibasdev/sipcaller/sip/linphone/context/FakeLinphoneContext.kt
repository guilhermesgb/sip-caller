package com.xibasdev.sipcaller.sip.linphone.context

import com.xibasdev.sipcaller.sip.SipCallId
import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import com.xibasdev.sipcaller.test.IdentityResolverFixtures.PRIMARY_CONTACT_IP_ADDRESS
import java.util.LinkedList
import java.util.Queue
import java.util.TreeMap
import org.linphone.core.Call.Dir.Incoming
import org.linphone.core.Call.Dir.Outgoing
import org.linphone.core.Call.State.Connected
import org.linphone.core.Call.State.End
import org.linphone.core.Call.State.IncomingReceived
import org.linphone.core.Call.State.OutgoingInit
import org.linphone.core.Call.State.OutgoingProgress
import org.linphone.core.Call.State.Released
import org.linphone.core.Call.State.StreamsRunning
import org.linphone.core.Call.Status.Aborted
import org.linphone.core.Call.Status.AcceptedElsewhere
import org.linphone.core.Call.Status.Declined
import org.linphone.core.Call.Status.Missed
import org.linphone.core.Call.Status.Success
import org.linphone.core.GlobalState
import org.linphone.core.GlobalState.Configuring
import org.linphone.core.GlobalState.Off
import org.linphone.core.GlobalState.On
import org.linphone.core.GlobalState.Ready
import org.linphone.core.GlobalState.Shutdown
import org.linphone.core.GlobalState.Startup
import org.linphone.core.RegistrationState
import org.linphone.core.RegistrationState.Cleared
import org.linphone.core.RegistrationState.Failed
import org.linphone.core.RegistrationState.None
import org.linphone.core.RegistrationState.Ok
import org.linphone.core.RegistrationState.Progress

private typealias FakeCoreListenerStatus = Pair<FakeCoreListener, Boolean>
private typealias LinphoneGlobalStateChange = Pair<GlobalState, String>

private const val LINPHONE_CORE_STARTED = 0
private const val LINPHONE_CORE_START_FAILED = -1

class FakeLinphoneContext : LinphoneContextApi(), FakeLinphoneContextApi {

    private val coreListeners = TreeMap<Int, FakeCoreListenerStatus>()
    private var currentGlobalState = Off
    private var currentIsNetworkReachable = false
    private var currentPrimaryContactIpAddress: String? = null
    private var currentPrimaryContactProtocolInfo: ProtocolInfo? = null

    init {
        val globalStateChangeListener = createGlobalStateChangeListener { globalState, _, _ ->

            currentGlobalState = globalState
        }

        enableCoreListener(globalStateChangeListener)
    }

    private val enqueuedGlobalStateChanges: Queue<LinphoneGlobalStateChange> = LinkedList()
    private val enqueuedCallStateChanges: Queue<LinphoneCallStateChange> = LinkedList()
    private val enqueuedAccountRegistrationStateChanges:
            Queue<LinphoneAccountRegistrationStateChange> = LinkedList()

    private val simulatedRegistrationProgressTargetState = TreeMap<String, RegistrationState>()

    private var failSynchronouslyOnLinphoneCoreStart: Boolean = false
    private var failAsynchronouslyOnLinphoneCoreStart: Boolean = false
    private var failSynchronouslyOnLinphoneCoreIterate: Boolean = false
    private var failSynchronouslyOnAccountCreation: Boolean = false
    private var failAsynchronouslyOnAccountRegistration: Boolean = false
    private var failSynchronouslyOnAccountDeactivation: Boolean = false
    private var failAsynchronouslyOnAccountUnregistration: Boolean = false
    private var failSynchronouslyOnAccountDestruction: Boolean = false

    private var nextSimulatedCallId: Long = 0L

    private var simulateStuckWhileRegisteringAccount: Boolean = false
    private var simulateStuckWhileUnregisteringAccount: Boolean = false

    override fun getCurrentGlobalState(): GlobalState {
        return currentGlobalState
    }

    override fun createGlobalStateChangeListener(
        callback: (globalState: GlobalState, coreListenerId: Int, errorReason: String) -> Unit
    ): Int {

        val fakeCoreListener = object : FakeCoreListener() {
            override fun onGlobalStateChange(globalState: GlobalState, errorReason: String) {
                callback(globalState, this.hashCode(), errorReason)
            }
        }

        val coreListenerId = fakeCoreListener.hashCode()
        coreListeners[coreListenerId] = Pair(fakeCoreListener, false)
        return coreListenerId
    }

    override fun createCallStateChangeListener(
        callback: (callStateChange: LinphoneCallStateChange, coreListenerId: Int) -> Unit
    ): Int {

        val fakeCoreListener = object : FakeCoreListener() {
            override fun onCallStateChange(callStateChange: LinphoneCallStateChange) {
                callback(callStateChange, this.hashCode())
            }
        }

        val coreListenerId = fakeCoreListener.hashCode()
        coreListeners[coreListenerId] = Pair(fakeCoreListener, false)
        return coreListenerId
    }

    override fun createAccountRegistrationStateChangeListener(
        callback: (
            callStateChange: LinphoneAccountRegistrationStateChange,
            coreListenerId: Int
        ) -> Unit
    ): Int {

        val fakeCoreListener = object : FakeCoreListener() {
            override fun onAccountRegistrationStateChange(
                accountRegistrationStateChange: LinphoneAccountRegistrationStateChange
            ) {
                callback(accountRegistrationStateChange, this.hashCode())
            }
        }

        val coreListenerId = fakeCoreListener.hashCode()
        coreListeners[coreListenerId] = Pair(fakeCoreListener, false)
        return coreListenerId
    }

    override fun createNetworkReachableListener(
        callback: (isNetworkReachable: Boolean, coreListenerId: Int) -> Unit
    ): Int {

        val fakeCoreListener = object : FakeCoreListener() {
            override fun onNetworkReachabilityChange(isNetworkReachable: Boolean) {
                callback(isNetworkReachable, this.hashCode())
            }
        }

        val coreListenerId = fakeCoreListener.hashCode()
        coreListeners[coreListenerId] = Pair(fakeCoreListener, false)
        return coreListenerId
    }

    override fun enableCoreListener(coreListenerId: Int) {
        if (!coreListeners.containsKey(coreListenerId)) {
            throw IllegalStateException("Core listener of ID '$$coreListenerId' not found!")
        }

        coreListeners[coreListenerId]?.let { listenerStatus ->

            coreListeners[coreListenerId] = listenerStatus.copy(second = true)
        }
    }

    override fun disableCoreListener(coreListenerId: Int) {
        if (!coreListeners.containsKey(coreListenerId)) {
            throw IllegalStateException("Core listener of ID '$$coreListenerId' not found!")
        }

        coreListeners[coreListenerId]?.let { listenerStatus ->

            coreListeners[coreListenerId] = listenerStatus.copy(second = false)
        }
    }

    override fun createAccount(
        idKey: String,
        accountInfo: AccountInfo,
        password: AccountPassword,
        expirationMs: Int
    ): Boolean {

        enqueueAccountRegistrationStateChange(
            LinphoneAccountRegistrationStateChange(
                idKey = idKey,
                state = None,
                errorReason = ""
            )
        )

        if (failAsynchronouslyOnAccountRegistration) {
            simulatedRegistrationProgressTargetState[idKey] = Failed

        } else if (simulateStuckWhileRegisteringAccount) {
            simulatedRegistrationProgressTargetState[idKey] = Progress

        } else {
            simulatedRegistrationProgressTargetState[idKey] = Ok
        }

        enqueueAccountRegistrationStateChange(
            LinphoneAccountRegistrationStateChange(
                idKey = idKey,
                state = Progress,
                errorReason = ""
            )
        )

        return !failSynchronouslyOnAccountCreation
    }

    override fun deactivateAccount(idKey: String): Boolean {
        if (failAsynchronouslyOnAccountUnregistration) {
            simulatedRegistrationProgressTargetState[idKey] = Failed

        } else if (simulateStuckWhileUnregisteringAccount) {
            simulatedRegistrationProgressTargetState[idKey] = Progress

        } else {
            simulatedRegistrationProgressTargetState[idKey] = Cleared
        }

        enqueueAccountRegistrationStateChange(
            LinphoneAccountRegistrationStateChange(
                idKey = idKey,
                state = Progress,
                errorReason = ""
            )
        )

        return !failSynchronouslyOnAccountDeactivation
    }

    override fun destroyAccount(
        idKey: String,
        accountInfo: AccountInfo,
        password: AccountPassword
    ): Boolean {
        return !failSynchronouslyOnAccountDestruction
    }

    override fun resolveNetworkCurrentlyReachable(): Boolean {
        return currentIsNetworkReachable
    }

    override fun resolvePrimaryContactIpAddress(): String? {
        return currentPrimaryContactIpAddress
    }

    override fun getPrimaryContactProtocolInfo(): ProtocolInfo? {
        return currentPrimaryContactProtocolInfo
    }

    override fun setPrimaryContactProtocolInfo(protocolInfo: ProtocolInfo): Boolean {
        currentPrimaryContactProtocolInfo = protocolInfo

        return true
    }

    override fun startLinphoneCore(): Int {
        if (failSynchronouslyOnLinphoneCoreStart) {
            return LINPHONE_CORE_START_FAILED
        }

        return when (currentGlobalState) {
            Ready,
            Shutdown,
            Off -> {
                currentIsNetworkReachable = true
                currentPrimaryContactIpAddress = PRIMARY_CONTACT_IP_ADDRESS

                enqueueGlobalStateChange(Startup)
                enqueueGlobalStateChange(Configuring)

                if (failAsynchronouslyOnLinphoneCoreStart) {
                    enqueueGlobalStateChange(Ready, "Asynchronous failure")

                } else {
                    enqueueGlobalStateChange(On)
                }

                LINPHONE_CORE_STARTED
            }
            Startup,
            Configuring,
            On -> LINPHONE_CORE_STARTED
        }
    }

    override fun iterateLinphoneCore() {
        enqueuedGlobalStateChanges.poll()?.let { engineState ->

            postGlobalStateChange(engineState)
        }

        if (enqueuedGlobalStateChanges.isEmpty() && currentGlobalState == On) {
            enqueuedCallStateChanges.poll()?.let { callStateChange ->

                postCallStateChange(callStateChange)
            }

            enqueuedAccountRegistrationStateChanges.poll()?.let {
                    linphoneAccountRegistrationStateChange ->

                postAccountRegistrationStateChange(linphoneAccountRegistrationStateChange)

                if (linphoneAccountRegistrationStateChange.state == Progress) {
                    simulatedRegistrationProgressTargetState[
                            linphoneAccountRegistrationStateChange.idKey
                    ]?.let { registrationState ->

                        if (registrationState != Progress) {
                            if (registrationState == Failed) {
                                enqueueAccountRegistrationStateChange(
                                    linphoneAccountRegistrationStateChange.copy(
                                        state = registrationState,
                                        errorReason = "Fake failure"
                                    )
                                )

                            } else {
                                enqueueAccountRegistrationStateChange(
                                    linphoneAccountRegistrationStateChange.copy(
                                        state = registrationState
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (failSynchronouslyOnLinphoneCoreIterate) {
            throw IllegalStateException(
                "Fake failure while linphone core was iterating!"
            )
        }
    }

    override fun failSynchronouslyOnLinphoneCoreStart() {
        failSynchronouslyOnLinphoneCoreStart = true
    }

    override fun failAsynchronouslyOnLinphoneCoreStart() {
        failAsynchronouslyOnLinphoneCoreStart = true
    }

    override fun failSynchronouslyOnLinphoneCoreIterate() {
        failSynchronouslyOnLinphoneCoreIterate = true
    }

    override fun failSynchronouslyOnAccountCreation() {
        failSynchronouslyOnAccountCreation = true
    }

    override fun failAsynchronouslyOnAccountRegistration() {
        failAsynchronouslyOnAccountRegistration = true
    }

    override fun failSynchronouslyOnAccountDeactivation() {
        failSynchronouslyOnAccountDeactivation = true
    }

    override fun failAsynchronouslyOnAccountUnregistration() {
        failAsynchronouslyOnAccountUnregistration = true
    }

    override fun failSynchronouslyOnAccountDestruction() {
        failSynchronouslyOnAccountDestruction = true
    }

    override fun simulateLinphoneCoreStop() {
        enqueueGlobalStateChange(Shutdown)
        enqueueGlobalStateChange(Off)
    }

    override fun simulateIncomingCallInvitationArrived() {
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = getNextSimulatedCallId(),
            direction = Incoming,
            state = IncomingReceived,
            status = Success,
            errorReason = ""
        ))
    }

    override fun simulateDeclineIncomingCallInvitation(callId: String) {
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = End,
            status = Declined,
            errorReason = ""
        ))
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = Released,
            status = Declined,
            errorReason = ""
        ))
    }

    override fun simulateAcceptIncomingCallInvitation(callId: String) {
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = Connected,
            status = Success,
            errorReason = ""
        ))
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = StreamsRunning,
            status = Success,
            errorReason = ""
        ))
    }

    override fun simulateMissIncomingCallInvitation(callId: String) {
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = End,
            status = Missed,
            errorReason = ""
        ))
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = Released,
            status = Missed,
            errorReason = ""
        ))
    }

    override fun simulateIncomingCallInvitationAcceptedElsewhere(callId: String) {
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = End,
            status = AcceptedElsewhere,
            errorReason = ""
        ))
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = Released,
            status = AcceptedElsewhere,
            errorReason = ""
        ))
    }

    override fun simulateIncomingCallCanceledByCaller(callId: String) {
        simulateIncomingCallCanceled(callId)
    }

    override fun simulateIncomingCallCanceledByCallee(callId: String) {
        setCallFinishedByLocalParty(SipCallId(callId))
        simulateIncomingCallCanceled(callId)
    }

    private fun simulateIncomingCallCanceled(callId: String) {
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = End,
            status = Aborted,
            errorReason = ""
        ))
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Incoming,
            state = Released,
            status = Aborted,
            errorReason = ""
        ))
    }

    override fun simulateOutgoingCallInvitationSent() {
        val callId = getNextSimulatedCallId()
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Outgoing,
            state = OutgoingInit,
            status = Success,
            errorReason = ""
        ))
        enqueueCallStateChange(LinphoneCallStateChange(
            callId = callId,
            direction = Outgoing,
            state = OutgoingProgress,
            status = Success,
            errorReason = ""
        ))
    }

    override fun simulateStuckWhileRegisteringAccount() {
        simulateStuckWhileRegisteringAccount = true
    }

    override fun simulateStuckWhileUnregisteringAccount() {
        simulateStuckWhileUnregisteringAccount = true
    }

    private fun enqueueGlobalStateChange(globalState: GlobalState, errorReason: String = "") {
        enqueuedGlobalStateChanges.offer(LinphoneGlobalStateChange(globalState, errorReason))
    }

    private fun postGlobalStateChange(globalStateChange: LinphoneGlobalStateChange) {
        coreListeners.values.forEach { (coreListener, isEnabled) ->

            if (isEnabled) {
                coreListener.onGlobalStateChange(globalStateChange.first, globalStateChange.second)
            }
        }
    }

    private fun enqueueCallStateChange(callStateChange: LinphoneCallStateChange) {
        enqueuedCallStateChanges.offer(callStateChange)
    }

    private fun postCallStateChange(callStateChange: LinphoneCallStateChange) {
        coreListeners.values.forEach { (coreListener, isEnabled) ->

            if (isEnabled) {
                coreListener.onCallStateChange(callStateChange)
            }
        }
    }

    private fun enqueueAccountRegistrationStateChange(
        accountRegistrationStateChange: LinphoneAccountRegistrationStateChange
    ) {

        enqueuedAccountRegistrationStateChanges.offer(accountRegistrationStateChange)
    }

    private fun postAccountRegistrationStateChange(
        accountRegistrationStateChange: LinphoneAccountRegistrationStateChange
    ) {

        coreListeners.values.forEach { (coreListener, isEnabled) ->

            if (isEnabled) {
                coreListener.onAccountRegistrationStateChange(accountRegistrationStateChange)
            }
        }
    }

    private fun getNextSimulatedCallId(): String {
        return (++nextSimulatedCallId).toString()
    }
}

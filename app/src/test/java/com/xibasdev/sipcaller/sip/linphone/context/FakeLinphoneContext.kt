package com.xibasdev.sipcaller.sip.linphone.context

import com.xibasdev.sipcaller.sip.SipCallId
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

private typealias FakeCoreListenerStatus = Pair<FakeCoreListener, Boolean>
private typealias LinphoneGlobalStateChange = Pair<GlobalState, String>

private const val LINPHONE_CORE_STARTED = 0
private const val LINPHONE_CORE_START_FAILED = -1

class FakeLinphoneContext : LinphoneContextApi(), FakeLinphoneContextApi {

    private val coreListeners = TreeMap<Int, FakeCoreListenerStatus>()
    private var currentGlobalState = Off

    init {
        val globalStateChangeListener = createGlobalStateChangeListener { globalState, _, _ ->

            currentGlobalState = globalState
        }

        enableCoreListener(globalStateChangeListener)
    }

    private val enqueuedGlobalStateChanges: Queue<LinphoneGlobalStateChange> = LinkedList()
    private val enqueuedCallStateChanges: Queue<LinphoneCallStateChange> = LinkedList()

    private var failSynchronouslyOnLinphoneCoreStart: Boolean = false
    private var failAsynchronouslyOnLinphoneCoreStart: Boolean = false
    private var failSynchronouslyOnLinphoneCoreIterate: Boolean = false

    private var nextSimulatedCallId: Long = 0L

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

    override fun startLinphoneCore(): Int {
        if (failSynchronouslyOnLinphoneCoreStart) {
            return LINPHONE_CORE_START_FAILED
        }

        return when (currentGlobalState) {
            Ready,
            Shutdown,
            Off -> {
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
            enqueuedCallStateChanges.poll()?.let { call ->

                postCallStateChange(call)
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

    private fun getNextSimulatedCallId(): String {
        return (++nextSimulatedCallId).toString()
    }
}

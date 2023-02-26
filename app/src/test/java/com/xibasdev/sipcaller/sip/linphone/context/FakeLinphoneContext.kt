package com.xibasdev.sipcaller.sip.linphone.context

import java.util.LinkedList
import java.util.Queue
import java.util.TreeMap
import org.linphone.core.GlobalState
import org.linphone.core.GlobalState.Configuring
import org.linphone.core.GlobalState.Off
import org.linphone.core.GlobalState.On
import org.linphone.core.GlobalState.Ready
import org.linphone.core.GlobalState.Shutdown
import org.linphone.core.GlobalState.Startup

private typealias FakeCoreListenerStatus = Pair<FakeCoreListener, Boolean>
private typealias GlobalStateChange = Pair<GlobalState, String>

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

    private val enqueuedGlobalStateChanges: Queue<GlobalStateChange> = LinkedList()
    private val enqueuedCallStateChanges: Queue<LinphoneCallStateChange> = LinkedList()

    private var failSynchronouslyOnLinphoneCoreStart: Boolean = false
    private var failAsynchronouslyOnLinphoneCoreStart: Boolean = false
    private var failSynchronouslyOnLinphoneCoreIterate: Boolean = false

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

    private fun enqueueGlobalStateChange(globalState: GlobalState, errorReason: String = "") {
        enqueuedGlobalStateChanges.offer(GlobalStateChange(globalState, errorReason))
    }

    private fun postGlobalStateChange(globalStateChange: GlobalStateChange) {
        coreListeners.values.forEach { (coreListener, isEnabled) ->

            if (isEnabled) {
                coreListener.onGlobalStateChange(globalStateChange.first, globalStateChange.second)
            }
        }
    }

    private fun postCallStateChange(callStateChange: LinphoneCallStateChange) {
        coreListeners.values.forEach { (coreListener, isEnabled) ->

            if (isEnabled) {
                coreListener.onCallStateChange(callStateChange)
            }
        }
    }
}

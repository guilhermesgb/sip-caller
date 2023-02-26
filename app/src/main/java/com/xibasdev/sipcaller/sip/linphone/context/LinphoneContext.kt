package com.xibasdev.sipcaller.sip.linphone.context

import android.util.SparseArray
import com.xibasdev.sipcaller.sip.linphone.di.LinphoneCore
import javax.inject.Inject
import org.linphone.core.Call
import org.linphone.core.Call.Dir.Outgoing
import org.linphone.core.Call.State
import org.linphone.core.Call.State.OutgoingInit
import org.linphone.core.Call.Status.Success
import org.linphone.core.Core
import org.linphone.core.CoreListener
import org.linphone.core.CoreListenerStub
import org.linphone.core.GlobalState
import org.linphone.core.GlobalState.Off

class LinphoneContext @Inject constructor(
    private val linphoneCore: LinphoneCore
) : LinphoneContextApi() {

    private val coreListeners = SparseArray<CoreListener>()

    override fun getCurrentGlobalState(): GlobalState {
        return linphoneCore.globalState
    }

    override fun createGlobalStateChangeListener(
        callback: (globalState: GlobalState, coreListenerId: Int, errorReason: String) -> Unit
    ): Int {

        val coreListener = object : CoreListenerStub() {
            override fun onGlobalStateChanged(
                core: Core,
                globalState: GlobalState?,
                errorReason: String
            ) {

                callback(globalState ?: Off, this.hashCode(), errorReason)
            }
        }

        val coreListenerId = coreListener.hashCode()
        coreListeners.put(coreListenerId, coreListener)
        return coreListenerId
    }

    override fun createCallStateChangeListener(
        callback: (callStateChange: LinphoneCallStateChange, coreListenerId: Int) -> Unit
    ): Int {

        val coreListener = object : CoreListenerStub() {
            override fun onCallStateChanged(
                core: Core,
                call: Call,
                callState: State?,
                errorReason: String
            ) {

                callback(
                    LinphoneCallStateChange(
                        callId = call.callLog.callId.orEmpty(),
                        direction = call.dir ?: Outgoing,
                        state = callState ?: OutgoingInit,
                        status = call.callLog.status ?: Success,
                        errorReason = errorReason
                    ),
                    this.hashCode()
                )
            }
        }

        val coreListenerId = coreListener.hashCode()
        coreListeners.put(coreListenerId, coreListener)
        return coreListenerId
    }

    override fun enableCoreListener(coreListenerId: Int) {
        if (coreListeners.indexOfKey(coreListenerId) < 0) {
            throw IllegalStateException("Core listener of ID '$$coreListenerId' not found!")
        }

        linphoneCore.addListener(coreListeners.get(coreListenerId))
    }

    override fun disableCoreListener(coreListenerId: Int) {
        if (coreListeners.indexOfKey(coreListenerId) < 0) {
            throw IllegalStateException("Core listener of ID '$$coreListenerId' not found!")
        }

        linphoneCore.removeListener(coreListeners.get(coreListenerId))
    }

    override fun startLinphoneCore(): Int {
        return linphoneCore.start()
    }

    override fun iterateLinphoneCore() {
        linphoneCore.iterate()
    }
}

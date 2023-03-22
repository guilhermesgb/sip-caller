package com.xibasdev.sipcaller.sip.history

import com.xibasdev.sipcaller.sip.calling.CallDirection
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import java.time.OffsetDateTime

/**
 * Representation of a upcoming call result observed via
 *   [com.xibasdev.sipcaller.sip.history.CallHistoryObserverApi.observeCallHistory].
 *
 * Upcoming calls are notified by the [com.xibasdev.sipcaller.sip.SipEngineApi] implementation
 *   through the [com.xibasdev.sipcaller.sip.SipEngineApi.observeCallHistory] as a consequence of
 *   its ongoing background processing (started with
 *   [com.xibasdev.sipcaller.sip.SipEngineApi.startEngine] and continuously iterated on through
 *   [com.xibasdev.sipcaller.sip.SipEngineApi.processEngineSteps]).
 */
sealed interface CallHistoryUpdate {

    val callId: CallId
    val callDirection: CallDirection
    val timestamp: OffsetDateTime
    val localAccount: AccountInfo
    val remoteAccount: AccountInfo
}

package com.xibasdev.sipcaller.sip.history

import com.xibasdev.sipcaller.sip.SipCallDirection
import com.xibasdev.sipcaller.sip.SipCallId
import java.time.OffsetDateTime

data class ConditionalCallHistoryUpdate(
    override val callId: SipCallId,
    override val callDirection: SipCallDirection,
    val updateIfCallFinishedByLocalParty: CallHistoryUpdate,
    val updateIfCallFinishedByRemoteParty: CallHistoryUpdate,
    override val timestamp: OffsetDateTime
) : CallHistoryUpdate

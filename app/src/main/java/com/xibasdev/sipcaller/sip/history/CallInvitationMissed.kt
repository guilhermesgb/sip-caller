package com.xibasdev.sipcaller.sip.history

import com.xibasdev.sipcaller.sip.SipCallDirection
import com.xibasdev.sipcaller.sip.SipCallId
import java.time.OffsetDateTime

data class CallInvitationMissed(
    override val callId: SipCallId,
    override val callDirection: SipCallDirection,
    override val timestamp: OffsetDateTime
) : CallInvitationFinishedUpdate

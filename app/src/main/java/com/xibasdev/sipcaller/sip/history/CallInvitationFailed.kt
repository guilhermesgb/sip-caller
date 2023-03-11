package com.xibasdev.sipcaller.sip.history

import com.xibasdev.sipcaller.sip.SipCallDirection
import com.xibasdev.sipcaller.sip.SipCallErrorReason
import com.xibasdev.sipcaller.sip.SipCallId
import java.time.OffsetDateTime

data class CallInvitationFailed(
    override val callId: SipCallId,
    override val callDirection: SipCallDirection,
    override val errorReason: SipCallErrorReason,
    override val timestamp: OffsetDateTime
) : CallInvitationFailedUpdate

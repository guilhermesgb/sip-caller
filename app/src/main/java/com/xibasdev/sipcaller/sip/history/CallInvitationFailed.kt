package com.xibasdev.sipcaller.sip.history

import com.xibasdev.sipcaller.sip.SipCallDirection
import com.xibasdev.sipcaller.sip.SipCallErrorReason
import com.xibasdev.sipcaller.sip.SipCallId

data class CallInvitationFailed(
    override val callId: SipCallId,
    override val callDirection: SipCallDirection,
    override val errorReason: SipCallErrorReason
) : CallInvitationFailedUpdate

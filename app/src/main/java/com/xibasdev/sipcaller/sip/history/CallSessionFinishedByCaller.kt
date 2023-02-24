package com.xibasdev.sipcaller.sip.history

import com.xibasdev.sipcaller.sip.SipCallDirection
import com.xibasdev.sipcaller.sip.SipCallId

data class CallSessionFinishedByCaller(
    override val callId: SipCallId,
    override val callDirection: SipCallDirection
) : CallSessionFinishedUpdate

package com.xibasdev.sipcaller.sip.history

import com.xibasdev.sipcaller.sip.calling.CallDirection
import com.xibasdev.sipcaller.sip.SipCallErrorReason
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import java.time.OffsetDateTime

data class CallInvitationFailed(
    override val callId: CallId,
    override val callDirection: CallDirection,
    override val errorReason: SipCallErrorReason,
    override val timestamp: OffsetDateTime,
    override val localAccount: AccountInfo,
    override val remoteAccount: AccountInfo
) : CallInvitationFailedUpdate

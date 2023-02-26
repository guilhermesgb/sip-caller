package com.xibasdev.sipcaller.sip.history

import com.xibasdev.sipcaller.sip.SipCallErrorReason

sealed interface CallFailedUpdate : CallHistoryUpdate {
    val errorReason: SipCallErrorReason
}

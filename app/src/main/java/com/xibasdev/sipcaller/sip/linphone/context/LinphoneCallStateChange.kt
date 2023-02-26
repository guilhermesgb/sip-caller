package com.xibasdev.sipcaller.sip.linphone.context

import org.linphone.core.Call.Dir
import org.linphone.core.Call.State
import org.linphone.core.Call.Status

data class LinphoneCallStateChange(
    val callId: String,
    val direction: Dir,
    val state: State,
    val status: Status,
    val errorReason: String
)

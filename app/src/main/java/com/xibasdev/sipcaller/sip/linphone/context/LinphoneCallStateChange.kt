package com.xibasdev.sipcaller.sip.linphone.context

import org.linphone.core.Call
import org.linphone.core.Call.Dir
import org.linphone.core.Call.State
import org.linphone.core.Call.Status
import org.linphone.core.Call.Status.Success

data class LinphoneCallStateChange(
    val call: Call,
    val callId: String,
    val direction: Dir,
    val state: State,
    val status: Status = Success,
    val remoteAccountAddress: LinphoneAccountAddress = LinphoneAccountAddress(),
    val audioStream: LinphoneCallStream = LinphoneCallStream(),
    val videoStream: LinphoneCallStream = LinphoneCallStream(),
    val errorReason: String = ""
)

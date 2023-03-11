package com.xibasdev.sipcaller.sip.linphone.context

import org.linphone.core.RegistrationState

data class LinphoneAccountRegistrationStateChange(
    val idKey: String,
    val state: RegistrationState,
    val errorReason: String
)

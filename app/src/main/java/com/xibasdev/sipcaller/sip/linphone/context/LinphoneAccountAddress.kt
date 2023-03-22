package com.xibasdev.sipcaller.sip.linphone.context

data class LinphoneAccountAddress(
    val displayName: String = "Remote Peer",
    val username: String = "username",
    val domain: String = "domain",
    val port: Int = 5060
)

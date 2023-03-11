package com.xibasdev.sipcaller.sip.registering.account.address.protocol

data class AccountProtocolInfo(
    val type: AccountProtocolType,
    val port: AccountProtocolPort,
    val sips: AccountSecureInfo = AccountSecureInfo(enabled = false, port = port),
    val srtp: AccountSecureInfo = AccountSecureInfo(enabled = false, port = port)
)

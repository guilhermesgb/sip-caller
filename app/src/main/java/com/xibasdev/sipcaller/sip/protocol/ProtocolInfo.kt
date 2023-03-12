package com.xibasdev.sipcaller.sip.protocol

data class ProtocolInfo(
    val type: ProtocolType,
    val port: ProtocolPort,
    val sips: SecureProtocolInfo = SecureProtocolInfo(enabled = false, port = port),
    val srtp: SecureProtocolInfo = SecureProtocolInfo(enabled = false, port = port)
)

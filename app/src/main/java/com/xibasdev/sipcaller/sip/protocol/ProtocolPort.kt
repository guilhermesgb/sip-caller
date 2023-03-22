package com.xibasdev.sipcaller.sip.protocol

/**
 * This value encodes the protocol port used in the address of a call party.
 */
sealed interface ProtocolPort {
    val value: Int
}

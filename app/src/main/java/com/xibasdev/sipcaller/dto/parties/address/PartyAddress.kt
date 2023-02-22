package com.xibasdev.sipcaller.dto.parties.address

/**
 * Representation of the address information of an arbitrary party involved in a call.
 *
 * TODO include protocol type in address information below.
 */
sealed interface PartyAddress {
    val protocolPort: ProtocolPort
}

val UNDEFINED_PROTOCOL_PORT = ProtocolPort(-1)

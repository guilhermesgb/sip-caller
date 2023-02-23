package com.xibasdev.sipcaller.dto.call.parties.address

/**
 * This value encodes the protocol port used in the address of a call party. This is mandatory, so
 *   it will in practice never be [UNDEFINED_PROTOCOL_PORT] for any observed address.
 *
 * The protocol may be specifically one of TCP or UDP, or both.
 *
 * TODO include protocol in address information. And link to the types in the doc line above.
 */
@JvmInline
value class ProtocolPort(val value: Int)

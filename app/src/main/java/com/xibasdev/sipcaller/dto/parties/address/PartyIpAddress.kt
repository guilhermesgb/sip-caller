package com.xibasdev.sipcaller.dto.parties.address

/**
 * Representation of the address information of an arbitrary party involved in a call, when that
 *   address is composed of an IP address and a protocol port.
 */
data class PartyIpAddress(
    val ipAddress: NetworkIp = LOOPBACK_IP_ADDRESS,
    override val protocolPort: ProtocolPort = UNDEFINED_PROTOCOL_PORT
) : PartyAddress

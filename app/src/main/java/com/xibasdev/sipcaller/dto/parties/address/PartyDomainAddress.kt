package com.xibasdev.sipcaller.dto.parties.address

/**
 * Representation of the address information of an arbitrary party involved in a call, when that
 *   address is composed of a network domain address and a protocol port.
 */
data class PartyDomainAddress(
    val remoteDomain: NetworkDomain = LOCALHOST_DOMAIN,
    override val protocolPort: ProtocolPort = UNDEFINED_PROTOCOL_PORT
) : PartyAddress

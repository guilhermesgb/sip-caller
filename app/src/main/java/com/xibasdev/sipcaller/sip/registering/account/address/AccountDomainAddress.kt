package com.xibasdev.sipcaller.sip.registering.account.address

import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.protocol.ProtocolType
import com.xibasdev.sipcaller.sip.protocol.RandomPort

/**
 * Representation of the address information of an arbitrary party involved in a call, when that
 *   address is composed of a network domain address and a protocol port.
 */
data class AccountDomainAddress(
    override val protocol: ProtocolInfo = ProtocolInfo(type = ProtocolType.TCP, port = RandomPort),
    val domain: AccountDomain = LOCALHOST_DOMAIN
) : AccountAddress

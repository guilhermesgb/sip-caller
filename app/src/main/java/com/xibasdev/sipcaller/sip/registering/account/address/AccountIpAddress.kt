package com.xibasdev.sipcaller.sip.registering.account.address

import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.protocol.ProtocolType.TCP
import com.xibasdev.sipcaller.sip.protocol.RandomPort

/**
 * Representation of the address information of an arbitrary party involved in a call, when that
 *   address is composed of an IP address and a protocol port.
 */
data class AccountIpAddress(
    override val protocol: ProtocolInfo = ProtocolInfo(type = TCP, port = RandomPort),
    val ip: AccountIp = LOOPBACK_IP_ADDRESS
) : AccountAddress

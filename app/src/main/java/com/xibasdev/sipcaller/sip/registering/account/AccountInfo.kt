package com.xibasdev.sipcaller.sip.registering.account

import com.xibasdev.sipcaller.sip.calling.Call
import com.xibasdev.sipcaller.sip.calling.CallDirection
import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.protocol.ProtocolType.TCP
import com.xibasdev.sipcaller.sip.protocol.RandomPort
import com.xibasdev.sipcaller.sip.registering.account.address.AccountAddress
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIpAddress
import com.xibasdev.sipcaller.sip.registering.account.address.LOOPBACK_IP_ADDRESS

/**
 * Representation of information pertaining to one of the two parties involved in a call session or
 *   invitation, such as its display name, username and address.
 */
data class AccountInfo(
    val displayName: AccountDisplayName = AccountDisplayName(""),
    val username: AccountUsername = AccountUsername(""),
    val address: AccountAddress = AccountIpAddress(
        protocol = ProtocolInfo(
            type = TCP,
            port = RandomPort
        ),
        ip = LOOPBACK_IP_ADDRESS
    )
)

context(Call)
fun AccountInfo.isCaller(): Boolean {
    return (direction == CallDirection.OUTGOING && parties.local == this)
            || (direction == CallDirection.INCOMING && parties.remote == this)
}

context(Call)
fun AccountInfo.isCallee(): Boolean {
    return (direction == CallDirection.OUTGOING && parties.remote == this)
            || (direction == CallDirection.INCOMING && parties.local == this)
}

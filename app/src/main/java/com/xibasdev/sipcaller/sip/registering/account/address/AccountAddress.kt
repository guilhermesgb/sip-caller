package com.xibasdev.sipcaller.sip.registering.account.address

import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo

/**
 * Representation of the address information of an arbitrary party involved in a call.
 */
sealed interface AccountAddress {
    val protocol: ProtocolInfo
}

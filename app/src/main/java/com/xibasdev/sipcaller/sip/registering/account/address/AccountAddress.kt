package com.xibasdev.sipcaller.sip.registering.account.address

import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo

sealed interface AccountAddress {
    val protocol: ProtocolInfo
}

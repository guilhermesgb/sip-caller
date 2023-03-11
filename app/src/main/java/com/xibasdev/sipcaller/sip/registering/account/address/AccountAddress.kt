package com.xibasdev.sipcaller.sip.registering.account.address

import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolInfo

sealed interface AccountAddress {
    val protocol: AccountProtocolInfo
}

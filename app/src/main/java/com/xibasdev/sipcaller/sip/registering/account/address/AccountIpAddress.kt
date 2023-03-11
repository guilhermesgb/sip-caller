package com.xibasdev.sipcaller.sip.registering.account.address

import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolInfo

data class AccountIpAddress(
    override val protocol: AccountProtocolInfo,
    val ip: AccountIp
) : AccountAddress

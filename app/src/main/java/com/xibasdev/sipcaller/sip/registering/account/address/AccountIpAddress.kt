package com.xibasdev.sipcaller.sip.registering.account.address

import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo

data class AccountIpAddress(
    override val protocol: ProtocolInfo,
    val ip: AccountIp
) : AccountAddress

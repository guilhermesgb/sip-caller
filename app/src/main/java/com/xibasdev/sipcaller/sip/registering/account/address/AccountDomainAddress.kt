package com.xibasdev.sipcaller.sip.registering.account.address

import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo

data class AccountDomainAddress(
    override val protocol: ProtocolInfo,
    val domain: AccountDomain
) : AccountAddress

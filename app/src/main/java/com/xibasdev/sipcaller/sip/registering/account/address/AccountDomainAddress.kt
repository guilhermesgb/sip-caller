package com.xibasdev.sipcaller.sip.registering.account.address

import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolInfo

data class AccountDomainAddress(
    override val protocol: AccountProtocolInfo,
    val domain: AccountDomain
) : AccountAddress

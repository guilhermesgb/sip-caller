package com.xibasdev.sipcaller.sip.identity

import com.xibasdev.sipcaller.sip.registering.account.address.AccountIpAddress

data class LocalIdentity(
    val address: AccountIpAddress
) : IdentityUpdate

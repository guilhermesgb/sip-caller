package com.xibasdev.sipcaller.sip.identity

import com.xibasdev.sipcaller.sip.registering.account.AccountInfo

data class RemoteIdentity(
    val account: AccountInfo
) : IdentityUpdate

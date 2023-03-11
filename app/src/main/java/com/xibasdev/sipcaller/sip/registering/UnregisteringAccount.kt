package com.xibasdev.sipcaller.sip.registering

import com.xibasdev.sipcaller.sip.registering.account.AccountInfo

data class UnregisteringAccount(
    val account: AccountInfo
) : AccountRegistrationUpdate

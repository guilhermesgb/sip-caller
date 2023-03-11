package com.xibasdev.sipcaller.sip.registering

import com.xibasdev.sipcaller.sip.registering.account.AccountInfo

data class UnregisteredAccount(
    val account: AccountInfo
) : AccountRegistrationUpdate

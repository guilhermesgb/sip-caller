package com.xibasdev.sipcaller.sip.registering

import com.xibasdev.sipcaller.sip.registering.account.AccountInfo

data class RegisteringAccount(
    val account: AccountInfo
) : AccountRegistrationUpdate

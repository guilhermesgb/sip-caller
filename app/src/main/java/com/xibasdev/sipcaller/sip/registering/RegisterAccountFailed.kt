package com.xibasdev.sipcaller.sip.registering

import com.xibasdev.sipcaller.sip.registering.account.AccountInfo

data class RegisterAccountFailed(
    val account: AccountInfo,
    val errorReason: String
) : AccountRegistrationUpdate

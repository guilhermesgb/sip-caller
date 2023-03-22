package com.xibasdev.sipcaller.sip.registering

import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface AccountRegistryApi {

    fun observeRegistrations(): Observable<AccountRegistrationUpdate>

    fun createRegistration(
        account: AccountInfo,
        password: AccountPassword,
        expirationMs: Int
    ): Completable

    fun destroyRegistration(): Completable
}

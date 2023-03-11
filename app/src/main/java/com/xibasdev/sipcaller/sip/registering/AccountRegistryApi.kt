package com.xibasdev.sipcaller.sip.registering

import com.xibasdev.sipcaller.sip.registering.account.AccountDisplayName
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import com.xibasdev.sipcaller.sip.registering.account.AccountUsername
import com.xibasdev.sipcaller.sip.registering.account.address.AccountAddress
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface AccountRegistryApi {

    fun observeRegistrations(): Observable<AccountRegistrationUpdate>

    fun createRegistration(
        displayName: AccountDisplayName,
        username: AccountUsername,
        password: AccountPassword,
        address: AccountAddress,
        expirationMs: Int
    ): Completable

    fun destroyRegistration(): Completable
}

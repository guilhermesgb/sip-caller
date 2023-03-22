package com.xibasdev.sipcaller.processing

import com.xibasdev.sipcaller.app.utils.Indexed
import com.xibasdev.sipcaller.app.utils.parseAddress
import com.xibasdev.sipcaller.app.utils.parseDisplayName
import com.xibasdev.sipcaller.app.utils.parsePassword
import com.xibasdev.sipcaller.app.utils.parseUsername
import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.sip.registering.AccountRegistrationUpdate
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import java.lang.Integer.max
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class AccountRegistry @Inject constructor(private val sipEngine: SipEngineApi) {

    fun observeRegistrations(): Observable<List<Indexed<AccountRegistrationUpdate>>> {
        return Observable
            .zip(
                Observable.interval(500, MILLISECONDS),
                sipEngine.observeRegistrations()
            ) { index, update ->

                Indexed(index, update)
            }
            .scan(listOf<Indexed<AccountRegistrationUpdate>>()) { previous, next ->

                val filteredPrevious = previous
                    .drop(max(0, previous.size - 4))
                    .toTypedArray()

                listOf(*filteredPrevious, next)
            }
            .map { updates ->

                updates.reversed()
            }
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun createRegistration(rawAccountRegistration: String): Completable {
        return sipEngine.createRegistration(
            account = AccountInfo(
                displayName = parseDisplayName(rawAccountRegistration),
                username = parseUsername(rawAccountRegistration),
                address = parseAddress(rawAccountRegistration),
            ),
            password = parsePassword(rawAccountRegistration),
            expirationMs = 3600
        )
    }

    fun destroyRegistration(): Completable {
        return sipEngine.destroyRegistration()
    }
}

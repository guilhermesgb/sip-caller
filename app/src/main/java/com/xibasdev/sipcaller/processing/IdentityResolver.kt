package com.xibasdev.sipcaller.processing

import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class IdentityResolver @Inject constructor(private val sipEngine: SipEngineApi) {

    fun observeIdentity(): Observable<IdentityUpdate> {
        return sipEngine.observeIdentity()
            .observeOn(AndroidSchedulers.mainThread())
    }
}

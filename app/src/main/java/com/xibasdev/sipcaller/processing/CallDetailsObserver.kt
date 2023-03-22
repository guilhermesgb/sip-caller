package com.xibasdev.sipcaller.processing

import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.CallUpdate
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class CallDetailsObserver @Inject constructor(private val sipEngine: SipEngineApi) {

    fun observeCallDetails(callId: CallId): Observable<CallUpdate> {
        return sipEngine.observeCallDetails(callId)
            .observeOn(AndroidSchedulers.mainThread())
    }
}

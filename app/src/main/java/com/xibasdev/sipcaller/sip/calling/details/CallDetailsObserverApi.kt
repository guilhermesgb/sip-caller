package com.xibasdev.sipcaller.sip.calling.details

import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.CallUpdate
import io.reactivex.rxjava3.core.Observable

interface CallDetailsObserverApi {

    fun observeCallDetails(callId: CallId): Observable<CallUpdate>
}

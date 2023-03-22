package com.xibasdev.sipcaller.sip.calling.state

import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface CallStateManagerApi {

    fun sendCallInvitation(account: AccountInfo): Single<CallId>

    fun cancelCallInvitation(callId: CallId): Completable

    /**
     * TODO include desired stream settings
     */
    fun acceptCallInvitation(callId: CallId): Completable

    /**
     * TODO include desired reason for declining
     */
    fun declineCallInvitation(callId: CallId): Completable

    fun terminateCallSession(callId: CallId): Completable
}

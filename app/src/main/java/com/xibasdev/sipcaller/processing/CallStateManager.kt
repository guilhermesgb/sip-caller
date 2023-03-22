package com.xibasdev.sipcaller.processing

import com.xibasdev.sipcaller.app.utils.parseAddress
import com.xibasdev.sipcaller.app.utils.parseDisplayName
import com.xibasdev.sipcaller.app.utils.parseUsername
import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class CallStateManager @Inject constructor(private val sipEngine: SipEngineApi) {

    fun sendCallInvitation(rawDestinationAddress: String): Single<CallId> {
        return sipEngine
            .sendCallInvitation(
                account = AccountInfo(
                    displayName = parseDisplayName(rawDestinationAddress),
                    username = parseUsername(rawDestinationAddress),
                    address = parseAddress(rawDestinationAddress),
                )
            )
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun cancelCallInvitation(callId: CallId): Completable {
        return sipEngine.cancelCallInvitation(callId)
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun acceptCallInvitation(callId: CallId): Completable {
        return sipEngine.acceptCallInvitation(callId)
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun declineCallInvitation(callId: CallId): Completable {
        return sipEngine.declineCallInvitation(callId)
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun terminateCallSession(callId: CallId): Completable {
        return sipEngine.terminateCallSession(callId)
            .observeOn(AndroidSchedulers.mainThread())
    }
}

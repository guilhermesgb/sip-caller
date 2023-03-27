package com.xibasdev.sipcaller.app.model

import android.view.Surface
import com.xibasdev.sipcaller.app.utils.parseAddress
import com.xibasdev.sipcaller.app.utils.parseDisplayName
import com.xibasdev.sipcaller.app.utils.parsePassword
import com.xibasdev.sipcaller.app.utils.parseUsername
import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.details.CallUpdate
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import com.xibasdev.sipcaller.sip.registering.AccountRegistrationUpdate
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.time.OffsetDateTime
import javax.inject.Inject

class SipEngineClient @Inject constructor(private val sipEngine: SipEngineApi) {

    fun observeRegistrations(): Observable<AccountRegistrationUpdate> {
        return sipEngine.observeRegistrations()
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun createRegistration(rawAccountRegistration: String): Completable {
        return sipEngine
            .createRegistration(
                account = AccountInfo(
                    displayName = parseDisplayName(rawAccountRegistration),
                    username = parseUsername(rawAccountRegistration),
                    address = parseAddress(rawAccountRegistration),
                ),
                password = parsePassword(rawAccountRegistration),
                expirationMs = 3600
            )
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun destroyRegistration(): Completable {
        return sipEngine.destroyRegistration()
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun observeIdentity(): Observable<IdentityUpdate> {
        return sipEngine.observeIdentity()
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun observeCallHistory(offset: OffsetDateTime): Observable<List<CallHistoryUpdate>> {
        return sipEngine.observeCallHistory(offset)
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun observeCallDetails(callId: CallId): Observable<CallUpdate> {
        return sipEngine.observeCallDetails(callId)
            .observeOn(AndroidSchedulers.mainThread())
    }

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

    fun setLocalCameraFeedSurface(callId: CallId, surface: Surface): Completable {
        return sipEngine.setLocalCameraFeedSurface(callId, surface)
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun unsetLocalCameraFeedSurface(callId: CallId): Completable {
        return sipEngine.unsetLocalCameraFeedSurface(callId)
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun setRemoteVideoFeedSurface(callId: CallId, surface: Surface): Completable {
        return sipEngine.setRemoteVideoFeedSurface(callId, surface)
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun unsetRemoteVideoFeedSurface(callId: CallId): Completable {
        return sipEngine.unsetRemoteVideoFeedSurface(callId)
            .observeOn(AndroidSchedulers.mainThread())
    }
}

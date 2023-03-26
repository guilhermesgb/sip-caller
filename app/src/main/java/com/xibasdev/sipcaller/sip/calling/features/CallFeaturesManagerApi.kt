package com.xibasdev.sipcaller.sip.calling.features

import android.view.Surface
import com.xibasdev.sipcaller.sip.calling.CallId
import io.reactivex.rxjava3.core.Completable

interface CallFeaturesManagerApi {

    fun setLocalCameraFeedSurface(callId: CallId, surface: Surface): Completable

    fun unsetLocalCameraFeedSurface(callId: CallId): Completable

    fun setRemoteVideoFeedSurface(callId: CallId, surface: Surface): Completable

    fun unsetRemoteVideoFeedSurface(callId: CallId): Completable
}

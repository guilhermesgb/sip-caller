package com.xibasdev.sipcaller.sip

import io.reactivex.rxjava3.core.Completable


interface SipEngineApi {

    fun startEngine(): Completable

    fun processEngineSteps(): Completable
}

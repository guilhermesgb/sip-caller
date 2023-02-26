package com.xibasdev.sipcaller.sip.linphone.context

interface FakeLinphoneContextApi {

    fun failSynchronouslyOnLinphoneCoreStart()

    fun failAsynchronouslyOnLinphoneCoreStart()

    fun failSynchronouslyOnLinphoneCoreIterate()
}

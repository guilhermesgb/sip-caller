package com.xibasdev.sipcaller.app.call.processing

import com.xibasdev.sipcaller.app.WorkManagerInitializer
import com.xibasdev.sipcaller.app.WorkManagerInitializerApi
import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.sip.linphone.LinphoneSipEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CallProcessorDependenciesModule {

    @Binds
    @Singleton
    fun bindWorkManagerInitializer(
        workManagerInitializer: WorkManagerInitializer
    ): WorkManagerInitializerApi

    @Binds
    @Singleton
    fun bindSipEngine(sipEngine: LinphoneSipEngine): SipEngineApi
}

package com.xibasdev.sipcaller.app.model.di

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
    fun bindSipEngine(sipEngine: LinphoneSipEngine): SipEngineApi
}

package com.xibasdev.sipcaller.processing.di

import com.xibasdev.sipcaller.processing.CallProcessor
import com.xibasdev.sipcaller.processing.CallProcessorApi
import com.xibasdev.sipcaller.processing.notifier.CallStateNotifier
import com.xibasdev.sipcaller.processing.notifier.CallStateNotifierApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CallProcessorModule {

    @Binds
    @Singleton
    fun bindCallStateNotifier(callStateNotifier: CallStateNotifier): CallStateNotifierApi

    @Binds
    @Singleton
    fun bindCallProcessor(callProcessor: CallProcessor): CallProcessorApi
}

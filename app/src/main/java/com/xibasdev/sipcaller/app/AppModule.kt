package com.xibasdev.sipcaller.app

import com.elvishew.xlog.LogLevel.ALL
import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = [AppModule.BindsModule::class])
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    @Named("LogLevel")
    fun provideLogLevel(): Int {
        return ALL
    }

    @Provides
    @Singleton
    @Named("SipEngineLogger")
    fun provideLinphoneLogger(loggerInitializer: LoggerInitializerApi): Logger {
        loggerInitializer.initializeLogger()

        return XLog.tag("SipEngine").build()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    interface BindsModule {

        @Binds
        @Singleton
        fun bindLoggerInitializer(
            loggerInitializer: LoggerInitializer
        ): LoggerInitializerApi

        @Binds
        @Singleton
        fun bindWorkManagerInitializer(
            workManagerInitializer: WorkManagerInitializer
        ): WorkManagerInitializerApi
    }
}

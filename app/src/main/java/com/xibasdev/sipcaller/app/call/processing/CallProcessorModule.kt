package com.xibasdev.sipcaller.app.call.processing

import androidx.work.Constraints
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
import com.xibasdev.sipcaller.app.call.processing.worker.CallProcessingWorker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = [CallProcessorModule.BindsModule::class])
@InstallIn(SingletonComponent::class)
class CallProcessorModule {

    @Provides
    @Named("CallProcessing")
    fun provideStartCallProcessingWorkRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<CallProcessingWorker>()
            .setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)
//            .setConstraints(Constraints.Builder().setRequiredNetworkType(CONNECTED).build())
            .build()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    interface BindsModule {

        @Binds
        @Singleton
        fun bindCallProcessor(callProcessor: CallProcessor): CallProcessorApi
    }
}

package com.xibasdev.sipcaller.app.call.processing.di

import androidx.work.Constraints
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import com.xibasdev.sipcaller.app.call.processing.worker.CallProcessingWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
class CallProcessingWorkerModule {

    @Provides
    @Named("CallProcessing")
    fun provideStartCallProcessingWorkRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<CallProcessingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(CONNECTED).build())
            .build()
    }
}

package com.xibasdev.sipcaller.processing.di

import androidx.work.Constraints
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import com.xibasdev.sipcaller.processing.worker.CallProcessingChecksWorker
import com.xibasdev.sipcaller.processing.worker.CallProcessingWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named

private const val UNIQUE_WORK_NAME_CALL_PROCESSING = "CallProcessing"
private const val UNIQUE_WORK_NAME_CALL_PROCESSING_CHECKS = "CallProcessingChecks"

@Module
@InstallIn(SingletonComponent::class)
class CallProcessingWorkerModule {

    @Provides
    @Named("CallProcessingUniqueWorkName")
    fun provideCallProcessingUniqueWorkName(): String {
        return UNIQUE_WORK_NAME_CALL_PROCESSING
    }

    @Provides
    @Named("CallProcessing")
    fun provideStartCallProcessingWorkRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<CallProcessingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(CONNECTED).build())
            .build()
    }

    @Provides
    @Named("CallProcessingChecksUniqueWorkName")
    fun provideCallProcessingChecksUniqueWorkName(): String {
        return UNIQUE_WORK_NAME_CALL_PROCESSING_CHECKS
    }

    @Provides
    @Named("CallProcessingChecks")
    fun provideCallProcessingChecksWorkRequest(): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<CallProcessingChecksWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(CONNECTED).build())
            .build()
    }
}

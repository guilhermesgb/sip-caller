package com.xibasdev.sipcaller.app.model.di

import androidx.work.Constraints
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import com.xibasdev.sipcaller.app.model.dto.processing.CallProcessing
import com.xibasdev.sipcaller.app.model.dto.processing.CallProcessingStopped
import com.xibasdev.sipcaller.app.model.worker.CallProcessingChecksWorker
import com.xibasdev.sipcaller.app.model.worker.CallProcessingWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Named
import javax.inject.Singleton

private const val UNIQUE_WORK_NAME_CALL_PROCESSING = "CallProcessing"
private const val UNIQUE_WORK_NAME_CALL_PROCESSING_CHECKS = "CallProcessingChecks"

@Module
@InstallIn(SingletonComponent::class)
class CallProcessingWorkerModule {

    @Provides
    @Singleton
    @Named("CallProcessingUpdates")
    fun provideCallProcessingUpdatesSubject(): BehaviorSubject<CallProcessing> {
        return BehaviorSubject.createDefault(CallProcessingStopped)
    }

    @Provides
    @Singleton
    @Named("CallProcessingUniqueWorkName")
    fun provideCallProcessingUniqueWorkName(): String {
        return UNIQUE_WORK_NAME_CALL_PROCESSING
    }

    @Provides
    @Singleton
    @Named("CallProcessing")
    fun provideStartCallProcessingWorkRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<CallProcessingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(CONNECTED).build())
            .build()
    }

    @Provides
    @Singleton
    @Named("CallProcessingChecksUniqueWorkName")
    fun provideCallProcessingChecksUniqueWorkName(): String {
        return UNIQUE_WORK_NAME_CALL_PROCESSING_CHECKS
    }

    @Provides
    @Singleton
    @Named("CallProcessingChecks")
    fun provideCallProcessingChecksWorkRequest(): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<CallProcessingChecksWorker>(15, MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(CONNECTED).build())
            .build()
    }
}

package com.xibasdev.sipcaller.sip.linphone

import android.content.Context
import android.util.Log
import com.xibasdev.sipcaller.BuildConfig
import com.xibasdev.sipcaller.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import javax.inject.Named
import javax.inject.Singleton
import org.linphone.core.AVPFMode
import org.linphone.core.Core
import org.linphone.core.Factory

private const val TAG = "LinphoneModule"

typealias LinphoneCore = Core

@Module
@InstallIn(SingletonComponent::class)
class LinphoneModule {

    @Provides
    @Named("LinphoneRxScheduler")
    fun provideLinphoneDedicatedRxScheduler(): Scheduler {
        return Schedulers.newThread()
    }

    @Provides
    @Named("LinphoneDefaultConfig")
    fun provideLinphoneDefaultConfigFileName(@ApplicationContext context: Context): String {
        return initializeFileFromRawResourceForLinphone(
            context,
            context.filesDir.absolutePath + "/linphone_default.config",
            R.raw.linphone_default
        )
    }

    @Provides
    @Named("LinphoneFactoryConfig")
    fun provideLinphoneFactoryConfigFileName(@ApplicationContext context: Context): String {
        return initializeFileFromRawResourceForLinphone(
            context,
            context.filesDir.absolutePath + "/linphone_factory.config",
            R.raw.linphone_factory
        )
    }

    @Provides
    @Named("LinphoneRootCertificate")
    fun provideLinphoneRootCertificateFileName(@ApplicationContext context: Context): String {
        return initializeFileFromRawResourceForLinphone(
            context,
            context.filesDir.absolutePath + "/linphone_rootca.pem",
            R.raw.linphone_rootca
        )
    }

    @Provides
    @Named("LinphoneRingTone")
    fun provideLinphoneRingToneFileName(@ApplicationContext context: Context): String {
        return initializeFileFromRawResourceForLinphone(
            context,
            context.filesDir.absolutePath + "/linphone_sound_ring.wav",
            R.raw.linphone_sound_ring
        )
    }

    @Provides
    @Named("LinphoneOnHoldTone")
    fun provideLinphoneOnHoldToneFileName(@ApplicationContext context: Context): String {
        return initializeFileFromRawResourceForLinphone(
            context,
            context.filesDir.absolutePath + "/linphone_sound_on_hold.wav",
            R.raw.linphone_sound_on_hold
        )
    }

    @Provides
    @Singleton
    fun provideLinphoneCore(
        @ApplicationContext context: Context,
        @Named("LinphoneRxScheduler") scheduler: Scheduler,
        @Named("LinphoneDefaultConfig") defaultConfigFileName: String,
        @Named("LinphoneFactoryConfig") factoryConfigFileName: String,
        @Named("LinphoneRootCertificate") rootCertificateFileName: String,
        @Named("LinphoneRingTone") ringToneFileName: String,
        @Named("LinphoneOnHoldTone") onHoldToneFileName: String
    ): LinphoneCore {

        return Single
            .fromCallable {
                val factory = Factory.instance()

                factory.createCore(defaultConfigFileName, factoryConfigFileName, context).apply {

                    isIpv6Enabled = false
                    isNetworkReachable = true
                    setUserAgent("SIP Caller", BuildConfig.VERSION_NAME)
                    primaryContact = "sip:unknown-host"

                    clearAllAuthInfo()
                    clearProxyConfig()

                    initializeFileFromRawResourceForLinphone(
                        context,
                        context.filesDir.absolutePath + "/share/images/nowebcamCIF.jpg",
                        R.raw.linphone_placeholder_no_remote_video
                    )

                    rootCa = rootCertificateFileName

                    verifyServerCertificates(false)
                    verifyServerCn(false)

                    playFile = onHoldToneFileName
                    ring = ringToneFileName
                    ringDuringIncomingEarlyMedia = true

                    videoActivationPolicy = factory.createVideoActivationPolicy().also { policy ->
                        policy.automaticallyInitiate = true
                        policy.automaticallyAccept = true
                    }
                    isVideoCaptureEnabled = true
                    isVideoDisplayEnabled = true
                    isVideoPreviewEnabled = true
                    videoDisplayFilter = "MSAndroidTextureDisplay"
                    setVideoSourceReuseEnabled(true)
                    preferredFramerate = 30f

                    adaptiveRateAlgorithm = "advanced"
                    isAdaptiveRateControlEnabled = true
                    avpfMode = AVPFMode.Enabled
                    avpfRrInterval = 1

                    audioJittcomp = 120
                    isAudioAdaptiveJittcompEnabled = true

                    nortpTimeout = 30

                    isAutoIterateEnabled = false
                    isNativeRingingEnabled = false

                    isEchoLimiterEnabled = false
                }
            }
            .doOnError { error ->

                Log.e(TAG, "Failed to create Linphone core!", error)
            }
            .subscribeOn(scheduler)
            .blockingGet()
    }

    private fun initializeFileFromRawResourceForLinphone(
        context: Context,
        fileName: String,
        rawResourceId: Int
    ): String {

        val file = File(fileName)

        if (file.exists()) {
            return fileName
        }

        context.resources.openRawResource(rawResourceId).use { inputStream ->

            FileOutputStream(file).use { outputStream ->

                inputStream.copyTo(outputStream)
            }
        }

        return initializeFileFromRawResourceForLinphone(context, fileName, rawResourceId)
    }
}

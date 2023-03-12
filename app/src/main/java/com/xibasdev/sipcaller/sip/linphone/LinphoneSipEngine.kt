package com.xibasdev.sipcaller.sip.linphone

import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.sip.history.CallHistoryObserverApi
import com.xibasdev.sipcaller.sip.identity.IdentityResolverApi
import com.xibasdev.sipcaller.sip.linphone.history.LinphoneCallHistoryObserver
import com.xibasdev.sipcaller.sip.linphone.identity.LinphoneIdentityResolver
import com.xibasdev.sipcaller.sip.linphone.processing.LinphoneProcessingEngine
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineApi
import com.xibasdev.sipcaller.sip.registering.AccountRegistryApi
import javax.inject.Inject

class LinphoneSipEngine @Inject constructor(
    private val engineProcessor: LinphoneProcessingEngine,
    private val callHistoryObserver: LinphoneCallHistoryObserver,
    private val accountRegistry: LinphoneAccountRegistry,
    private val identityResolver: LinphoneIdentityResolver
) : SipEngineApi,
    ProcessingEngineApi by engineProcessor,
    CallHistoryObserverApi by callHistoryObserver,
    AccountRegistryApi by accountRegistry,
    IdentityResolverApi by identityResolver

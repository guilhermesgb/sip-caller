package com.xibasdev.sipcaller.sip.linphone

import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.sip.calling.details.CallDetailsObserverApi
import com.xibasdev.sipcaller.sip.calling.features.CallFeaturesManagerApi
import com.xibasdev.sipcaller.sip.calling.state.CallStateManagerApi
import com.xibasdev.sipcaller.sip.history.CallHistoryObserverApi
import com.xibasdev.sipcaller.sip.identity.IdentityResolverApi
import com.xibasdev.sipcaller.sip.linphone.calling.details.LinphoneCallDetailsObserver
import com.xibasdev.sipcaller.sip.linphone.calling.features.LinphoneCallFeaturesManager
import com.xibasdev.sipcaller.sip.linphone.calling.state.LinphoneCallStateManager
import com.xibasdev.sipcaller.sip.linphone.history.LinphoneCallHistoryObserver
import com.xibasdev.sipcaller.sip.linphone.identity.LinphoneIdentityResolver
import com.xibasdev.sipcaller.sip.linphone.processing.LinphoneProcessingEngine
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineApi
import com.xibasdev.sipcaller.sip.registering.AccountRegistryApi
import javax.inject.Inject

class LinphoneSipEngine @Inject constructor(
    private val engineProcessor: LinphoneProcessingEngine,
    private val accountRegistry: LinphoneAccountRegistry,
    private val identityResolver: LinphoneIdentityResolver,
    private val callHistoryObserver: LinphoneCallHistoryObserver,
    private val callDetailsObserver: LinphoneCallDetailsObserver,
    private val callStateManager: LinphoneCallStateManager,
    private val callFeaturesManager: LinphoneCallFeaturesManager
) : SipEngineApi,
    ProcessingEngineApi by engineProcessor,
    AccountRegistryApi by accountRegistry,
    IdentityResolverApi by identityResolver,
    CallHistoryObserverApi by callHistoryObserver,
    CallDetailsObserverApi by callDetailsObserver,
    CallStateManagerApi by callStateManager,
    CallFeaturesManagerApi by callFeaturesManager

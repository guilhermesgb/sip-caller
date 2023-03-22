package com.xibasdev.sipcaller.sip.linphone.context

import org.linphone.core.GlobalState

open class FakeCoreListener {

    open fun onGlobalStateChange(globalState: GlobalState, errorReason: String) {}

    open fun onCallStateChange(callStateChange: LinphoneCallStateChange) {}

    open fun onCallStatsChange(callStatsChange: LinphoneCallStatsChange) {}

    open fun onAccountRegistrationStateChange(
        accountRegistrationStateChange: LinphoneAccountRegistrationStateChange
    ) {}

    open fun onNetworkReachabilityChange(isNetworkReachable: Boolean) {}
}

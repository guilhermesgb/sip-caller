package com.xibasdev.sipcaller.sip.linphone.context

import android.util.SparseArray
import android.view.Surface
import androidx.core.util.valueIterator
import com.elvishew.xlog.Logger
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.features.CallFeatures
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStreamDirection.DISABLED
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStreamDirection.ENABLED_RECEIVE_ONLY
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStreamDirection.ENABLED_SEND_ONLY
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStreamDirection.ENABLED_SEND_RECEIVE
import com.xibasdev.sipcaller.sip.linphone.di.LinphoneCore
import com.xibasdev.sipcaller.sip.protocol.DefinedPort
import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.protocol.ProtocolType.TCP
import com.xibasdev.sipcaller.sip.protocol.ProtocolType.UDP
import com.xibasdev.sipcaller.sip.protocol.RandomPort
import com.xibasdev.sipcaller.sip.protocol.SecureProtocolInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import com.xibasdev.sipcaller.sip.registering.account.address.AccountDomainAddress
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIpAddress
import io.reactivex.rxjava3.core.Scheduler
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Named
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Call.Dir.Outgoing
import org.linphone.core.Call.State
import org.linphone.core.Call.State.OutgoingInit
import org.linphone.core.Call.Status.Success
import org.linphone.core.CallStats
import org.linphone.core.Core
import org.linphone.core.CoreListener
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.GlobalState
import org.linphone.core.GlobalState.Off
import org.linphone.core.MediaDirection.RecvOnly
import org.linphone.core.MediaDirection.SendOnly
import org.linphone.core.MediaDirection.SendRecv
import org.linphone.core.Reason.Declined
import org.linphone.core.RegistrationState
import org.linphone.core.RegistrationState.None
import org.linphone.core.RegistrationState.Progress
import org.linphone.core.StreamType.Audio
import org.linphone.core.StreamType.Video

class LinphoneContext @Inject constructor(
    private val linphoneCore: LinphoneCore,
    @Named("LinphoneRxScheduler") private val scheduler: Scheduler,
    @Named("SipEngineLogger") private val logger: Logger
) : LinphoneContextApi(scheduler) {

    private val coreListeners = SparseArray<CoreListener>()

    override fun getCurrentGlobalState(): GlobalState {
        return linphoneCore.globalState
    }

    override fun createGlobalStateChangeListener(
        callback: (globalState: GlobalState, errorReason: String, coreListenerId: Int) -> Unit
    ): Int {

        val coreListener = object : CoreListenerStub() {
            override fun onGlobalStateChanged(
                core: Core,
                globalState: GlobalState?,
                errorReason: String
            ) {

                callback(globalState ?: Off, errorReason, this.hashCode())
            }
        }

        val coreListenerId = coreListener.hashCode()
        coreListeners.put(coreListenerId, coreListener)
        return coreListenerId
    }

    override fun createAccountRegistrationStateChangeListener(
        callback: (
            callStateChange: LinphoneAccountRegistrationStateChange,
            coreListenerId: Int
        ) -> Unit
    ): Int {

        val coreListener = object : CoreListenerStub() {
            override fun onAccountRegistrationStateChanged(
                core: Core,
                account: Account,
                state: RegistrationState?,
                message: String
            ) {

                account.params.idkey?.let { registerId ->

                    callback(
                        LinphoneAccountRegistrationStateChange(
                            idKey = registerId,
                            state = state ?: None,
                            errorReason = message
                        ),
                        this.hashCode()
                    )
                }
            }
        }

        val coreListenerId = coreListener.hashCode()
        coreListeners.put(coreListenerId, coreListener)
        return coreListenerId
    }

    override fun createNetworkReachableListener(
        callback: (isNetworkReachable: Boolean, coreListenerId: Int) -> Unit
    ): Int {

        val coreListener = object : CoreListenerStub() {
            override fun onNetworkReachable(core: Core, isNetworkReachable: Boolean) {

                callback(isNetworkReachable, this.hashCode())
            }
        }

        val coreListenerId = coreListener.hashCode()
        coreListeners.put(coreListenerId, coreListener)
        return coreListenerId
    }

    override fun createCallStateChangeListener(
        callback: (callStateChange: LinphoneCallStateChange, coreListenerId: Int) -> Unit
    ): Int {

        val coreListener = object : CoreListenerStub() {
            override fun onCallStateChanged(
                core: Core,
                call: Call,
                callState: State?,
                errorReason: String
            ) {

                callback(
                    LinphoneCallStateChange(
                        call = call,
                        callId = call.callLog.callId.orEmpty(),
                        direction = call.dir ?: Outgoing,
                        state = callState ?: OutgoingInit,
                        status = call.callLog.status ?: Success,
                        remoteAccountAddress = resolveRemoteAccountAddress(call),
                        audioStream = resolveAudioStream(call),
                        videoStream = resolveVideoStream(call),
                        errorReason = errorReason
                    ),
                    this.hashCode()
                )
            }
        }

        val coreListenerId = coreListener.hashCode()
        coreListeners.put(coreListenerId, coreListener)
        return coreListenerId
    }

    override fun createCallStatsChangeListener(
        callback: (
            callStatsChange: LinphoneCallStatsChange,
            coreListenerId: Int
        ) -> Unit
    ): Int {

        val coreListener = object : CoreListenerStub() {
            override fun onCallStatsUpdated(
                core: Core,
                call: Call,
                callStats: CallStats
            ) {

                callback(
                    LinphoneCallStatsChange(
                        callId = call.callLog.callId.orEmpty(),
                        audioStats = if (callStats.type == Audio) {
                            CallStatsUpdated(
                                stream = resolveAudioStream(call),
                                dataDownloadBandwidthKbps = callStats.downloadBandwidth,
                                dataUploadBandwidthKbps = callStats.uploadBandwidth,
                                controlDownloadBandwidthKbps = callStats.rtcpDownloadBandwidth,
                                controlUploadBandwidthKbps = callStats.rtcpUploadBandwidth
                            )

                        } else {
                            CallStatsNotUpdated
                        },
                        videoStats = if (callStats.type == Video) {
                            CallStatsUpdated(
                                stream = resolveVideoStream(call),
                                dataDownloadBandwidthKbps = callStats.downloadBandwidth,
                                dataUploadBandwidthKbps = callStats.uploadBandwidth,
                                controlDownloadBandwidthKbps = callStats.rtcpDownloadBandwidth,
                                controlUploadBandwidthKbps = callStats.rtcpUploadBandwidth
                            )

                        } else {
                            CallStatsNotUpdated
                        }
                    ),
                    this.hashCode()
                )
            }
        }

        val coreListenerId = coreListener.hashCode()
        coreListeners.put(coreListenerId, coreListener)
        return coreListenerId
    }

    override fun enableCoreListener(coreListenerId: Int) {
        if (coreListeners.indexOfKey(coreListenerId) < 0) {
            throw IllegalStateException("Core listener of ID '$$coreListenerId' not found!")
        }

        linphoneCore.addListener(coreListeners.get(coreListenerId))
    }

    override fun disableCoreListener(coreListenerId: Int) {
        if (coreListeners.indexOfKey(coreListenerId) < 0) {
            throw IllegalStateException("Core listener of ID '$$coreListenerId' not found!")
        }

        linphoneCore.removeListener(coreListeners.get(coreListenerId))
    }

    override fun startLinphoneCore(): Int {
        return linphoneCore.start()
    }

    override fun iterateLinphoneCore() {
        linphoneCore.iterate()
    }

    override fun resolveNetworkCurrentlyReachable(): Boolean {
        return linphoneCore.isNetworkReachable
    }

    override fun resolvePrimaryContactIpAddress(): String? {
        return linphoneCore.createPrimaryContactParsed()?.domain
    }

    override fun getPrimaryContactProtocolInfo(): ProtocolInfo? {
        return if (linphoneCore.transportsUsed.tcpPort != 0) {
            ProtocolInfo(
                type = TCP,
                port = DefinedPort(linphoneCore.transportsUsed.tcpPort),
                sips = SecureProtocolInfo(
                    enabled = linphoneCore.transportsUsed.tlsPort != 0,
                    port = DefinedPort(linphoneCore.transportsUsed.tlsPort)
                ),
                srtp = SecureProtocolInfo(
                    enabled = linphoneCore.transportsUsed.dtlsPort != 0,
                    port = DefinedPort(linphoneCore.transportsUsed.dtlsPort)
                )
            )

        } else if (linphoneCore.transportsUsed.udpPort != 0) {
            ProtocolInfo(
                type = UDP,
                port = DefinedPort(linphoneCore.transportsUsed.udpPort),
                sips = SecureProtocolInfo(enabled = false, port = RandomPort),
                srtp = SecureProtocolInfo(enabled = false, port = RandomPort)
            )

        } else null
    }

    override fun setPrimaryContactProtocolInfo(protocolInfo: ProtocolInfo): Boolean {
        with (protocolInfo) {
            val transportProtocolsSuccessfullySet = linphoneCore.setTransports(
                Factory.instance().createTransports().apply {
                    when (type) {
                        TCP -> {
                            tcpPort = port.value
                            tlsPort = if (sips.enabled) { sips.port.value } else 0
                            dtlsPort = if (srtp.enabled) { srtp.port.value } else 0
                            udpPort = 0
                        }
                        UDP -> {
                            tcpPort = 0
                            tlsPort = 0
                            dtlsPort = 0
                            udpPort = port.value
                        }
                    }
                }
            )
            return transportProtocolsSuccessfullySet == 0
        }
    }

    override fun createAccount(
        idKey: String,
        accountInfo: AccountInfo,
        password: AccountPassword,
        expirationMs: Int
    ): Boolean {

        with (accountInfo) {
            val factory = Factory.instance()

            val accountAddress = when (address) {
                is AccountDomainAddress -> {
                    "${address.domain.value}:${address.protocol.port.value}"
                }
                is AccountIpAddress -> {
                    "${address.ip.value}:${address.protocol.port.value}"
                }
            }

            val authInfo = factory.createAuthInfo(
                username.value,
                null,
                password.value,
                null,
                null,
                accountAddress
            )
            linphoneCore.addAuthInfo(authInfo)

            val accountParams = linphoneCore.createAccountParams()

            val identityRawAddress = "sip:${username.value}@$accountAddress"
            val identityAddress = linphoneCore.interpretUrl(identityRawAddress)
            if (identityAddress != null) {
                if (displayName.value.isEmpty()) {
                    identityAddress.displayName = null

                } else {
                    identityAddress.displayName = displayName.value
                }
            }
            accountParams.identityAddress = identityAddress

            val transportProtocol = address.protocol.type.name.lowercase()
            val serverAddress = "sip:$accountAddress;transport=$transportProtocol"
            accountParams.serverAddress = linphoneCore.interpretUrl(serverAddress)

            accountParams.expires = expirationMs
            accountParams.publishExpires = expirationMs
            accountParams.idkey = idKey
            accountParams.isRegisterEnabled = true

            val account = linphoneCore.createAccount(accountParams)
            val accountAdded = linphoneCore.addAccount(account) == 0
            linphoneCore.defaultAccount = account

            return accountAdded && linphoneCore.getAccountByIdkey(idKey) != null
        }
    }

    override fun deactivateAccount(idKey: String): Boolean {
        val account = linphoneCore.getAccountByIdkey(idKey)

        return if (account == null) {
            false

        } else {
            val updatedAccountParams = account.params.clone()
            updatedAccountParams.isRegisterEnabled = false
            account.params = updatedAccountParams

            scheduler.scheduleDirect({
                coreListeners.valueIterator().forEach { coreListener ->

                    coreListener.onAccountRegistrationStateChanged(
                        linphoneCore, account, Progress, "Unregistering"
                    )
                }
            }, 50, MILLISECONDS)

            true
        }
    }

    override fun destroyAccount(
        idKey: String,
        accountInfo: AccountInfo,
        password: AccountPassword
    ): Boolean {

        val account = linphoneCore.getAccountByIdkey(idKey)

        return if (account == null) {
            false

        } else {
            linphoneCore.removeAccount(account)

            with (accountInfo) {
                val factory = Factory.instance()

                val accountAddress = when (address) {
                    is AccountDomainAddress -> {
                        "${address.domain.value}:${address.protocol.port}"
                    }
                    is AccountIpAddress -> {
                        "${address.ip.value}:${address.protocol.port}"
                    }
                }
                val authInfo = factory.createAuthInfo(
                    username.value,
                    null,
                    password.value,
                    null,
                    null,
                    accountAddress
                )

                linphoneCore.removeAuthInfo(authInfo)
            }

            return linphoneCore.getAccountByIdkey(idKey) == null
        }
    }

    override fun sendCallInvitation(account: AccountInfo): Boolean {
        logger.d("About to send a call invitation to $account. Parsing its address...")

        val rawUsername = account.username.value
        val rawRemoteAddressDomain = when (account.address) {
            is AccountDomainAddress -> account.address.domain.value
            is AccountIpAddress -> account.address.ip.value
        }
        val rawRemoteAddressPort = account.address.protocol.port.value

        val rawAddress = if (rawUsername.isEmpty()) {
            "sip:$rawRemoteAddressDomain:$rawRemoteAddressPort"
        } else {
            "sip:$rawUsername@$rawRemoteAddressDomain:$rawRemoteAddressPort"
        }

        logger.d("Raw address of the destination account to send invite: $rawAddress.")

        val callParameters = linphoneCore.createCallParams(null)

        if (callParameters == null) {
            logger.e("Linphone failed to create Call Parameters for outgoing invitation that" +
                    " was about to be sent to $rawAddress.")
            return false
        }

        callParameters.apply {
            isEarlyMediaSendingEnabled = true
            isAudioEnabled = true
            audioDirection = SendRecv
            isVideoEnabled = true
            videoDirection = SendRecv
        }

        val address = linphoneCore.createAddress(rawAddress)

        if (address == null) {
            logger.e("Linphone failed to create Address representing $rawAddress.")
            return false
        }

        val rawDisplayName = account.displayName.value
        if (rawDisplayName.isEmpty()) {
            address.displayName = null

        } else {
            address.displayName = rawDisplayName
        }

        val call = linphoneCore.inviteAddressWithParams(address, callParameters)

        if (call == null) {
            logger.e("Linphone failed to send outgoing call invitation to $rawAddress" +
                    " using Call Parameters and Address that were just created.")
            return false
        }

        val callId = call.callLog.callId

        if (callId == null || callId.isEmpty()) {
            logger.e("Linphone sent an outgoing call invitation to $rawAddress " +
                    "with null call identifier.")
            return false
        }

        return true
    }

    override fun cancelCallInvitation(callId: CallId): Boolean {
        return doForCallWithCallId(callId) { call ->

            setCallFinishedByLocalParty(callId)

            call.terminate() == 0
        }
    }

    override fun acceptCallInvitation(callId: CallId): Boolean {
        return doForCallWithCallId(callId) { call ->

            val callParameters = linphoneCore.createCallParams(call)
                ?: return@doForCallWithCallId false

            callParameters.apply {
                isEarlyMediaSendingEnabled = true
                isAudioEnabled = true
                audioDirection = SendRecv
                isVideoEnabled = true
                videoDirection = SendRecv
            }

            call.acceptWithParams(callParameters) == 0
        }
    }

    override fun declineCallInvitation(callId: CallId): Boolean {
        return doForCallWithCallId(callId) { call ->

            call.decline(Declined) == 0
        }
    }

    override fun terminateCallSession(callId: CallId): Boolean {
        return doForCallWithCallId(callId) { call ->

            setCallFinishedByLocalParty(callId)

            call.terminate() == 0
        }
    }

    override fun isCurrentlyHandlingCall(): Boolean {
        return linphoneCore.currentCall != null
    }

    override fun enableOrDisableCallFeatures(call: Call, features: CallFeatures): Boolean {
        call.microphoneMuted = !features.microphone.enabled
        call.speakerMuted = !features.speaker.enabled
        call.isCameraEnabled = features.camera.enabled

        return call.microphoneMuted == !features.microphone.enabled &&
                call.speakerMuted == !features.speaker.enabled &&
                call.isCameraEnabled == features.camera.enabled
    }

    override fun setLocalSurface(surface: Surface): Boolean {
        linphoneCore.nativePreviewWindowId = surface
        return linphoneCore.nativePreviewWindowId == surface
    }

    override fun unsetLocalSurface(): Boolean {
        linphoneCore.nativePreviewWindowId = null
        return linphoneCore.nativePreviewWindowId == null
    }

    override fun setRemoteSurface(surface: Surface): Boolean {
        linphoneCore.nativeVideoWindowId = surface
        return linphoneCore.nativeVideoWindowId == surface
    }

    override fun unsetRemoteSurface(): Boolean {
        linphoneCore.nativeVideoWindowId = null
        return linphoneCore.nativeVideoWindowId == null
    }

    private fun resolveRemoteAccountAddress(call: Call): LinphoneAccountAddress {
        val remoteAddress = call.remoteAddress

        return LinphoneAccountAddress(
            displayName = remoteAddress.displayName.orEmpty(),
            username = remoteAddress.username.orEmpty(),
            domain = remoteAddress.domain.orEmpty(),
            port = remoteAddress.port
        )
    }

    private fun resolveAudioStream(call: Call): LinphoneCallStream {
        val localParameters = call.currentParams
        val remoteParameters = call.remoteParams

        val currentCodec = localParameters.usedAudioPayloadType
        val codecName = currentCodec?.mimeType.orEmpty()
        val isCodecInUse = codecName.isNotEmpty()

        val audioDirection = localParameters.audioDirection

        val outgoingAudioEnabled = localParameters.isAudioEnabled && isCodecInUse &&
                audioDirection in listOf(SendOnly, SendRecv) && !call.microphoneMuted

        val incomingAudioEnabled = remoteParameters?.isAudioEnabled ?: false && isCodecInUse &&
                audioDirection in listOf(RecvOnly, SendRecv) && !call.speakerMuted

        val direction = when {
            outgoingAudioEnabled && incomingAudioEnabled -> ENABLED_SEND_RECEIVE
            outgoingAudioEnabled && !incomingAudioEnabled -> ENABLED_SEND_ONLY
            !outgoingAudioEnabled && incomingAudioEnabled -> ENABLED_RECEIVE_ONLY
            else -> DISABLED
        }

        return LinphoneCallStream(
            codecName = codecName,
            clockRateHz = currentCodec?.clockRate ?: -1,
            channelsNumber = currentCodec?.channels ?: -1,
            direction = direction
        )
    }

    private fun resolveVideoStream(call: Call): LinphoneCallStream {
        val localParameters = call.currentParams
        val remoteParameters = call.remoteParams

        val currentCodec = localParameters.usedVideoPayloadType
        val codecName = currentCodec?.mimeType.orEmpty()
        val isCodecInUse = codecName.isNotEmpty()

        val videoDirection = localParameters.videoDirection

        val outgoingVideoEnabled = localParameters.isVideoEnabled && isCodecInUse &&
                videoDirection in listOf(SendOnly, SendRecv) &&
                call.isCameraEnabled && localParameters.sentFramerate > 0

        val incomingVideoEnabled = remoteParameters?.isVideoEnabled ?: false && isCodecInUse &&
                videoDirection in listOf(RecvOnly, SendRecv) &&
                localParameters.receivedFramerate > 0

        val direction = when {
            outgoingVideoEnabled && incomingVideoEnabled -> ENABLED_SEND_RECEIVE
            outgoingVideoEnabled && !incomingVideoEnabled -> ENABLED_SEND_ONLY
            !outgoingVideoEnabled && incomingVideoEnabled -> ENABLED_RECEIVE_ONLY
            else -> DISABLED
        }

        return LinphoneCallStream(
            codecName = codecName,
            clockRateHz = currentCodec?.clockRate ?: -1,
            channelsNumber = currentCodec?.channels ?: -1,
            direction = direction
        )
    }

    private fun doForCallWithCallId(callId: CallId, operation: (Call) -> Boolean): Boolean {
        linphoneCore.calls.forEach { call ->

            if (call.callLog.callId == callId.value) {
                return operation(call)
            }
        }

        return false
    }
}

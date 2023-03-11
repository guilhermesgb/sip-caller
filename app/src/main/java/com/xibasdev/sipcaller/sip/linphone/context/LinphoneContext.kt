package com.xibasdev.sipcaller.sip.linphone.context

import android.util.SparseArray
import com.xibasdev.sipcaller.sip.linphone.di.LinphoneCore
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import com.xibasdev.sipcaller.sip.registering.account.address.AccountDomainAddress
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIpAddress
import javax.inject.Inject
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Call.Dir.Outgoing
import org.linphone.core.Call.State
import org.linphone.core.Call.State.OutgoingInit
import org.linphone.core.Call.Status.Success
import org.linphone.core.Core
import org.linphone.core.CoreListener
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.GlobalState
import org.linphone.core.GlobalState.Off
import org.linphone.core.RegistrationState
import org.linphone.core.RegistrationState.None

class LinphoneContext @Inject constructor(
    private val linphoneCore: LinphoneCore
) : LinphoneContextApi() {

    private val coreListeners = SparseArray<CoreListener>()

    override fun getCurrentGlobalState(): GlobalState {
        return linphoneCore.globalState
    }

    override fun createGlobalStateChangeListener(
        callback: (globalState: GlobalState, coreListenerId: Int, errorReason: String) -> Unit
    ): Int {

        val coreListener = object : CoreListenerStub() {
            override fun onGlobalStateChanged(
                core: Core,
                globalState: GlobalState?,
                errorReason: String
            ) {

                callback(globalState ?: Off, this.hashCode(), errorReason)
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
                        callId = call.callLog.callId.orEmpty(),
                        direction = call.dir ?: Outgoing,
                        state = callState ?: OutgoingInit,
                        status = call.callLog.status ?: Success,
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

    override fun startLinphoneCore(): Int {
        return linphoneCore.start()
    }

    override fun iterateLinphoneCore() {
        linphoneCore.iterate()
    }
}

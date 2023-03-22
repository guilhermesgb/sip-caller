package com.xibasdev.sipcaller.sip.linphone.identity

import com.elvishew.xlog.Logger
import com.xibasdev.sipcaller.sip.identity.IdentityResolverApi
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import com.xibasdev.sipcaller.sip.identity.LocalIdentity
import com.xibasdev.sipcaller.sip.identity.RemoteIdentity
import com.xibasdev.sipcaller.sip.identity.UnreachableIdentity
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneContextApi
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry
import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.registering.RegisteredAccount
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIp
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIpAddress
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LinphoneIdentityResolver @Inject constructor(
    @Named("LinphoneRxScheduler") private val scheduler: Scheduler,
    private val linphoneContext: LinphoneContextApi,
    private val accountRegistry: LinphoneAccountRegistry,
    @Named("SipEngineLogger") private val logger: Logger
) : IdentityResolverApi {

    private val networkReachableListenerId = linphoneContext
        .createNetworkReachableListener { isNetworkReachable, _ ->

            latestNetworkReachabilityUpdate.onNext(isNetworkReachable)
        }

    private val latestNetworkReachabilityUpdate = PublishSubject.create<Boolean>()

    private val exposedNetworkReachabilityUpdate = BehaviorSubject.create<Boolean>().apply {
        processNetworkReachabilityUpdates(this)
    }

    override fun observeIdentity(): Observable<IdentityUpdate> {
        return Observable
            .combineLatest(
                exposedNetworkReachabilityUpdate,
                accountRegistry.observeRegistrations()
            ) { isNetworkReachable, latestRegistrationUpdate ->

                if (latestRegistrationUpdate is RegisteredAccount) {
                    RemoteIdentity(account = latestRegistrationUpdate.account)

                } else {
                    if (isNetworkReachable) {
                        val ipAddress = linphoneContext.resolvePrimaryContactIpAddress()
                        val protocolInfo = linphoneContext.getPrimaryContactProtocolInfo()

                        if (ipAddress != null && protocolInfo != null) {
                            return@combineLatest LocalIdentity(address = AccountIpAddress(
                                protocol = protocolInfo,
                                ip = AccountIp(ipAddress)
                            ))
                        }
                    }

                    UnreachableIdentity
                }
            }
            .distinctUntilChanged()
            .subscribeOn(scheduler)
    }

    override fun setLocalIdentityProtocolInfo(protocolInfo: ProtocolInfo): Completable {
        return Completable
            .create { emitter ->

                if (linphoneContext.setPrimaryContactProtocolInfo(protocolInfo)) {
                    emitter.onComplete()

                } else {
                    val error = IllegalStateException(
                        "Linphone failed to set transport protocol: $protocolInfo."
                    )
                    emitter.onError(error)
                }
            }
            .subscribeOn(scheduler)
    }

    private fun processNetworkReachabilityUpdates(subject: BehaviorSubject<Boolean>) {
        with (linphoneContext) {
            doWhenLinphoneCoreStartsOrStops(subject) { isLinphoneCoreStarted ->

                logger.d("Identity resolver observer detected Linphone core " +
                        (if (isLinphoneCoreStarted) "start!" else "stop!"))

                if (isLinphoneCoreStarted) {
                    enableCoreListener(networkReachableListenerId)

                    latestNetworkReachabilityUpdate
                        .startWith(Single.just(linphoneContext.resolveNetworkCurrentlyReachable()))

                } else {
                    disableCoreListener(networkReachableListenerId)

                    Observable.just(false)
                }
            }
        }
    }
}

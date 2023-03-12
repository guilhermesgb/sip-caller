package com.xibasdev.sipcaller.sip.identity

import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface IdentityResolverApi {

    fun observeIdentity(): Observable<IdentityUpdate>

    fun setLocalIdentityProtocolInfo(protocolInfo: ProtocolInfo): Completable
}

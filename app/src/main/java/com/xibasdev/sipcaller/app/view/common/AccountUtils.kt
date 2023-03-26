package com.xibasdev.sipcaller.app.view.common

import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.address.AccountDomainAddress
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIpAddress

fun AccountInfo.toRawString(includeDisplayName: Boolean = true): String {
    var displayName = displayName.value
    if (displayName.isNotEmpty()) {
        displayName = "<$displayName> "
    }
    var username = username.value
    if (username.isNotEmpty()) {
        username = "$username@"
    }

    val domainOrIp = when (val address = address) {
        is AccountDomainAddress -> address.domain.value
        is AccountIpAddress -> address.ip.value
    }
    val port = address.protocol.port.value

    return if (includeDisplayName) {
        "$displayName$username$domainOrIp:$port"

    } else {
        "$username$domainOrIp:$port"
    }
}

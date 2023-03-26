package com.xibasdev.sipcaller.app.utils

import androidx.core.text.isDigitsOnly
import com.xibasdev.sipcaller.sip.protocol.DefinedPort
import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.protocol.ProtocolType
import com.xibasdev.sipcaller.sip.registering.account.AccountDisplayName
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import com.xibasdev.sipcaller.sip.registering.account.AccountUsername
import com.xibasdev.sipcaller.sip.registering.account.address.AccountAddress
import com.xibasdev.sipcaller.sip.registering.account.address.AccountDomain
import com.xibasdev.sipcaller.sip.registering.account.address.AccountDomainAddress


private const val DEFAULT_PROXY_PORT = 5060

fun parseDisplayName(rawAccountRegistration: String): AccountDisplayName {
    return with(rawAccountRegistration) {
        AccountDisplayName(
            value = substringAfter("<")
                .substringBefore(">")
                .trim()
        )
    }
}

fun parseUsername(rawAccountRegistration: String): AccountUsername {
    return with(rawAccountRegistration) {
        AccountUsername(
            value = substringAfter(">")
                .substringBefore("@")
                .substringBefore(":")
                .trim()
        )
    }
}

fun parsePassword(rawAccountRegistration: String): AccountPassword {
    return with (rawAccountRegistration) {
        AccountPassword(
            value = substringAfter(":")
                .substringBefore("@")
                .trim()
        )
    }
}

fun parseAddress(rawAccountRegistration: String): AccountAddress {
    return with (rawAccountRegistration) {
        AccountDomainAddress(
            protocol = ProtocolInfo(
                type = ProtocolType.TCP,
                port = DefinedPort(
                    value = with (
                        substringAfter("@")
                            .substringAfter(":")
                    ) {
                        if (isDigitsOnly()) {
                            toInt()

                        } else {
                            DEFAULT_PROXY_PORT
                        }
                    }
                )
            ),
            domain = AccountDomain(
                value = substringAfter("@")
                    .substringBefore(":")
                    .trim()
            )
        )
    }
}

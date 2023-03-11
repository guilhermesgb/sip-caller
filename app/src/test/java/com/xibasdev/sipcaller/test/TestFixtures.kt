package com.xibasdev.sipcaller.test

import com.xibasdev.sipcaller.sip.registering.account.AccountDisplayName
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountPassword
import com.xibasdev.sipcaller.sip.registering.account.AccountUsername
import com.xibasdev.sipcaller.sip.registering.account.address.AccountDomain
import com.xibasdev.sipcaller.sip.registering.account.address.AccountDomainAddress
import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolInfo
import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolPort
import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolType.TCP
import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolType.UDP

object AccountRegistryFixtures {

    val DISPLAY_NAME_1 = AccountDisplayName("Display Name 1")
    val DISPLAY_NAME_2 = AccountDisplayName("Display Name 2")

    val USERNAME_1 = AccountUsername("username_1")
    val USERNAME_2 = AccountUsername("username_2")

    val PASSWORD_1 = AccountPassword("******** (1)")
    val PASSWORD_2 = AccountPassword("******** (2)")

    val DOMAIN_1 = AccountDomain("domain.1")
    val DOMAIN_2 = AccountDomain("domain.2")

    val PORT_1 = AccountProtocolPort(5060)
    val PORT_2 = AccountProtocolPort(5061)

    val PROTOCOL_1 = TCP
    val PROTOCOL_2 = UDP

    const val EXPIRATION_MS_1 = 1000 * 60 * 60
    const val EXPIRATION_MS_2 = 1000 * 60 * 60 * 2

    val ACCOUNT_1 = AccountInfo(
        displayName = DISPLAY_NAME_1,
        username = USERNAME_1,
        address = AccountDomainAddress(
            protocol = AccountProtocolInfo(
                type = PROTOCOL_1,
                port = PORT_1
            ),
            domain = DOMAIN_1
        )
    )
    val ACCOUNT_2 = AccountInfo(
        displayName = DISPLAY_NAME_2,
        username = USERNAME_2,
        address = AccountDomainAddress(
            protocol = AccountProtocolInfo(
                type = PROTOCOL_2,
                port = PORT_2
            ),
            domain = DOMAIN_2
        )
    )
}

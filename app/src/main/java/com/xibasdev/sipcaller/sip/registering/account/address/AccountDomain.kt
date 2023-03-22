package com.xibasdev.sipcaller.sip.registering.account.address

/**
 * This value encodes the network domain address of a given call party. Used in remote calls, where
 *   parties reside in different networks.
 */
@JvmInline
value class AccountDomain(val value: String)

val LOCALHOST_DOMAIN = AccountDomain("localhost")

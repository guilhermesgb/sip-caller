package com.xibasdev.sipcaller.dto.parties.address

/**
 * This value encodes the network domain address of a given call party. Used in remote calls, where
 *   parties reside in different networks.
 */
@JvmInline
value class NetworkDomain(val value: String)

val LOCALHOST_DOMAIN = NetworkDomain("localhost")

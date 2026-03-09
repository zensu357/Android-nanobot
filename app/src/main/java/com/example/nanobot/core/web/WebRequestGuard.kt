package com.example.nanobot.core.web

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class WebRequestGuard(
    private val delegateDns: Dns = Dns.SYSTEM,
    private val hostOverrides: Map<String, List<InetAddress>> = emptyMap(),
    private val allowPrivateHostsForTesting: Set<String> = emptySet()
) {
    fun validateUrl(rawUrl: String): HttpUrl {
        val normalized = rawUrl.trim()
        val httpUrl = normalized.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Only http:// and https:// URLs are allowed.")

        if (httpUrl.scheme != "http" && httpUrl.scheme != "https") {
            throw IllegalArgumentException("Only http:// and https:// URLs are allowed.")
        }

        validateHost(httpUrl.host)
        validateResolvedAddresses(httpUrl.host, resolveAddresses(httpUrl.host))
        return httpUrl
    }

    fun validateRedirectTarget(currentUrl: HttpUrl, locationHeader: String): HttpUrl {
        val redirectUrl = currentUrl.resolve(locationHeader)
            ?: throw IllegalArgumentException("Invalid redirect target.")

        validateHost(redirectUrl.host)
        validateResolvedAddresses(redirectUrl.host, resolveAddresses(redirectUrl.host))
        return redirectUrl
    }

    fun validateResolvedAddresses(host: String, addresses: List<InetAddress>) {
        if (host.lowercase() in allowPrivateHostsForTesting) {
            return
        }
        if (addresses.isEmpty()) {
            throw IllegalArgumentException("Unable to resolve host for web request.")
        }
        if (addresses.any { isBlockedAddress(it) }) {
            throw IllegalArgumentException("Private, loopback, link-local, and reserved network targets are not allowed.")
        }
    }

    fun lookupAndValidate(hostname: String): List<InetAddress> {
        val addresses = resolveAddresses(hostname)
        validateResolvedAddresses(hostname, addresses)
        return addresses
    }

    private fun resolveAddresses(hostname: String): List<InetAddress> {
        val key = hostname.lowercase()
        return hostOverrides[key] ?: delegateDns.lookup(hostname)
    }

    private fun validateHost(host: String) {
        val normalized = host.lowercase()
        if (normalized in allowPrivateHostsForTesting) {
            return
        }
        if (normalized == "localhost" || normalized.endsWith(".local")) {
            throw IllegalArgumentException("Localhost and local network hosts are not allowed.")
        }
    }

    private fun isBlockedAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isSiteLocalAddress ||
            address.isLinkLocalAddress ||
            address.isMulticastAddress
        ) {
            return true
        }

        if (address is Inet4Address) {
            val bytes = address.address.map { it.toInt() and 0xFF }
            val first = bytes[0]
            val second = bytes[1]
            return (first == 100 && second in 64..127) ||
                (first == 198 && second in 18..19) ||
                (first == 192 && second == 0)
        }

        if (address is Inet6Address) {
            val firstByte = address.address.firstOrNull()?.toInt()?.and(0xFF) ?: return false
            if ((firstByte and 0xFE) == 0xFC) {
                return true
            }
        }

        return false
    }
}

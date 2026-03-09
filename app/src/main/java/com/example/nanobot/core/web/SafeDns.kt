package com.example.nanobot.core.web

import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Dns

@Singleton
class SafeDns @Inject constructor(
    private val webRequestGuard: WebRequestGuard
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return webRequestGuard.lookupAndValidate(hostname)
    }
}

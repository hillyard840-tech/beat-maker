package com.example.util

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/**
 * Guards the "fetch any URL" tool. The URL can come from the model, which in turn can be steered
 * by text on a previously fetched web page (prompt injection). Without this check a malicious page
 * could make the phone request addresses on the user's own network (router admin pages, NAS,
 * cloud metadata endpoints, etc.).
 */
object UrlSafety {

    /** Returns null if the URL may be fetched, otherwise a short reason. */
    fun rejectReason(rawUrl: String, resolve: (String) -> List<InetAddress> = ::resolveAll): String? {
        val uri = try {
            URI(rawUrl.trim())
        } catch (_: Exception) {
            return "Malformed URL"
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return "Only http and https URLs are allowed"
        if (uri.userInfo != null) return "URLs with embedded credentials are not allowed"

        val host = uri.host?.lowercase()?.trimEnd('.') ?: return "URL has no host"
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") ||
            host.endsWith(".internal") || host.endsWith(".lan") || host.endsWith(".home.arpa")
        ) {
            return "Local network addresses are not allowed"
        }

        val addresses = try {
            resolve(host)
        } catch (_: Exception) {
            return "Could not resolve host"
        }
        if (addresses.isEmpty()) return "Could not resolve host"
        if (addresses.any { isPrivateAddress(it) }) return "Local network addresses are not allowed"
        return null
    }

    internal fun isPrivateAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
        ) {
            return true
        }
        val bytes = address.address
        return when (address) {
            is Inet4Address -> {
                val b0 = bytes[0].toInt() and 0xFF
                val b1 = bytes[1].toInt() and 0xFF
                b0 == 0 || // "this" network
                    (b0 == 100 && b1 in 64..127) || // carrier-grade NAT
                    (b0 == 192 && b1 == 0 && (bytes[2].toInt() and 0xFF) == 0) || // IETF protocol assignments
                    (b0 == 198 && (b1 == 18 || b1 == 19)) || // benchmarking
                    b0 >= 240 // reserved / broadcast
            }
            is Inet6Address -> {
                val b0 = bytes[0].toInt() and 0xFF
                // fc00::/7 unique local; IPv4-mapped addresses are checked as IPv4
                (b0 and 0xFE) == 0xFC || isMappedPrivateIpv4(bytes)
            }
            else -> true
        }
    }

    private fun isMappedPrivateIpv4(bytes: ByteArray): Boolean {
        val isMapped = (0 until 10).all { bytes[it].toInt() == 0 } &&
            (bytes[10].toInt() and 0xFF) == 0xFF && (bytes[11].toInt() and 0xFF) == 0xFF
        if (!isMapped) return false
        return isPrivateAddress(InetAddress.getByAddress(bytes.copyOfRange(12, 16)))
    }

    private fun resolveAll(host: String): List<InetAddress> = InetAddress.getAllByName(host).toList()
}

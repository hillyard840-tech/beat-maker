package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class LiveSearchResult(
    val title: String,
    val snippet: String,
    val url: String
)

data class LiveWebpage(
    val url: String,
    val title: String,
    val content: String,
    val metaDescription: String = ""
)

object LiveInternetService {
    private const val MAX_REDIRECTS = 5
    private const val MAX_BODY_BYTES = 2 * 1024 * 1024
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    /**
     * Performs a direct live internet search and returns organic search results.
     */
    suspend fun searchLiveInternet(query: String, maxResults: Int = 6): List<LiveSearchResult> =
        withContext(Dispatchers.IO) {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) return@withContext emptyList()

            val results = mutableListOf<LiveSearchResult>()

            // 1. Query DuckDuckGo HTML for real-time organic web search results
            try {
                val doc = Jsoup.connect("https://html.duckduckgo.com/html/")
                    .data("q", cleanQuery)
                    .userAgent(USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(8000)
                    .post()

                val elements = doc.select(".result")
                for (elem in elements) {
                    if (results.size >= maxResults) break
                    val titleElem = elem.selectFirst(".result__title a") ?: continue
                    val snippetElem = elem.selectFirst(".result__snippet") ?: continue

                    val rawTitle = titleElem.text().trim()
                    val rawSnippet = snippetElem.text().trim()
                    val rawHref = titleElem.attr("href")

                    // DuckDuckGo wraps target URLs in /l/?kh=-1&uddg=<encoded_target_url>
                    var targetUrl = rawHref
                    if (rawHref.contains("uddg=")) {
                        val encodedPart = rawHref.substringAfter("uddg=").substringBefore("&")
                        try {
                            targetUrl = URLDecoder.decode(encodedPart, StandardCharsets.UTF_8.name())
                        } catch (_: Exception) {}
                    }

                    // Skip sponsor / internal navigation redirects
                    if (targetUrl.startsWith("http") && !targetUrl.contains("duckduckgo.com/y.js")) {
                        results.add(
                            LiveSearchResult(
                                title = rawTitle,
                                snippet = rawSnippet,
                                url = targetUrl
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. If primary web search yielded nothing, fallback to Wikipedia OpenSearch API
            if (results.isEmpty()) {
                try {
                    val encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.name())
                    val wikiUrl = "https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=4&namespace=0&format=json"
                    val resp = Jsoup.connect(wikiUrl)
                        .userAgent("KoraAi/1.0")
                        .ignoreContentType(true)
                        .timeout(6000)
                        .execute()
                        .body()

                    // Parse JSON array format: ["query", ["Title1", ...], ["Desc1", ...], ["Url1", ...]]
                    val jsonArray = org.json.JSONArray(resp)
                    if (jsonArray.length() >= 4) {
                        val titles = jsonArray.getJSONArray(1)
                        val descs = jsonArray.getJSONArray(2)
                        val urls = jsonArray.getJSONArray(3)
                        for (i in 0 until titles.length()) {
                            val t = titles.optString(i)
                            val d = descs.optString(i)
                            val u = urls.optString(i)
                            if (t.isNotEmpty() && u.isNotEmpty()) {
                                results.add(
                                    LiveSearchResult(
                                        title = t,
                                        snippet = if (d.isNotEmpty()) d else "Live encyclopedic entry on $t",
                                        url = u
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            results
        }

    /**
     * Connects directly to any live URL and extracts readable content in real-time.
     */
    suspend fun fetchLiveWebpage(rawUrl: String, maxChars: Int = 5000): LiveWebpage =
        withContext(Dispatchers.IO) {
            val url = when {
                rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                else -> "https://$rawUrl"
            }

            try {
                // Follow redirects by hand so every hop is re-checked; a public URL must not be
                // able to bounce the request onto the local network.
                var currentUrl = url
                var response: org.jsoup.Connection.Response? = null
                for (hop in 0..MAX_REDIRECTS) {
                    UrlSafety.rejectReason(currentUrl)?.let { reason ->
                        return@withContext LiveWebpage(
                            url = currentUrl,
                            title = "Blocked URL",
                            content = "This URL was not fetched: $reason.",
                            metaDescription = ""
                        )
                    }
                    val res = Jsoup.connect(currentUrl)
                        .userAgent(USER_AGENT)
                        .timeout(10000)
                        .maxBodySize(MAX_BODY_BYTES)
                        .followRedirects(false)
                        .ignoreHttpErrors(true)
                        .execute()
                    val location = res.header("Location")
                    if (res.statusCode() in 300..399 && !location.isNullOrBlank()) {
                        currentUrl = java.net.URL(java.net.URL(currentUrl), location).toString()
                        continue
                    }
                    response = res
                    break
                }
                val finalResponse = response ?: throw java.io.IOException("Too many redirects")
                if (finalResponse.statusCode() >= 400) {
                    throw java.io.IOException("HTTP ${finalResponse.statusCode()}")
                }
                val doc = finalResponse.parse()

                val title = doc.title().ifBlank { url }
                val metaDesc = doc.select("meta[name=description]").attr("content").trim()

                // Remove unwanted clutter
                doc.select("script, style, nav, footer, header, iframe, noscript, svg, .ad, .banner").remove()

                val bodyText = doc.body().text()
                val trimmedText = if (bodyText.length > maxChars) {
                    bodyText.take(maxChars) + "... [Content truncated for display]"
                } else {
                    bodyText
                }

                LiveWebpage(
                    url = currentUrl,
                    title = title,
                    content = trimmedText.ifBlank { "No readable text content found on this webpage." },
                    metaDescription = metaDesc
                )
            } catch (e: Exception) {
                LiveWebpage(
                    url = url,
                    title = "Error loading $url",
                    content = "Failed to connect to live URL: ${e.message}",
                    metaDescription = ""
                )
            }
        }
}

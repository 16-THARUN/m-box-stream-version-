package com.mbox.source.youtube

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response as NPResponse
import java.io.IOException
import java.util.concurrent.TimeUnit

class YouTubeDownloader private constructor(
    private val client: OkHttpClient
) : Downloader() {

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        fun create(): YouTubeDownloader {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            return YouTubeDownloader(okHttpClient)
        }
    }

    @Throws(IOException::class)
    override fun execute(request: NPRequest): NPResponse {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val builder = Request.Builder().url(url)

        // Anonymous requests only: no cookies, no user sign-in headers
        builder.header("User-Agent", USER_AGENT)
        builder.header("Accept-Language", "en-US,en;q=0.9")

        for ((headerName, headerValues) in headers) {
            if (headerName.equals("Cookie", ignoreCase = true) || headerName.equals("Authorization", ignoreCase = true)) {
                continue
            }
            for (value in headerValues) {
                builder.addHeader(headerName, value)
            }
        }

        if (httpMethod.equals("POST", ignoreCase = true)) {
            val body = dataToSend?.toRequestBody() ?: "".toRequestBody()
            builder.post(body)
        } else if (httpMethod.equals("GET", ignoreCase = true)) {
            builder.get()
        } else if (httpMethod.equals("HEAD", ignoreCase = true)) {
            builder.head()
        }

        val okHttpRequest = builder.build()
        val response = client.newCall(okHttpRequest).execute()

        val responseCode = response.code
        val responseMessage = response.message
        val responseBody = response.body?.string() ?: ""
        val responseHeaders = mutableMapOf<String, List<String>>()

        for (name in response.headers.names()) {
            responseHeaders[name] = response.headers.values(name)
        }

        val latestUrl = response.request.url.toString()

        return NPResponse(responseCode, responseMessage, responseHeaders, responseBody, latestUrl)
    }
}

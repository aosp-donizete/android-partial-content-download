package com.doni.sample.splitter

import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
object OkHttpHandler : HttpHandler {
    private val client = OkHttpClient()

    override suspend fun head(url: String): Map<String, String> {
        val request = Request.Builder()
            .url(url)
            .head()
            .build()

        val response = client.newCall(request).executeAsync()
        return response.headers.toMap()
    }

    override suspend fun stream(url: String, start: Long, end: Long): InputStream {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-$end")
            .get()
            .build()
        val response = client.newCall(request).executeAsync()

        if (response.code != 206) {
            throw IllegalStateException("Server side does not support partial request")
        }

        return response.body.byteStream()
    }
}
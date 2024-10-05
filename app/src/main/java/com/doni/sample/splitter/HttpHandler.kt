package com.doni.sample.splitter

import java.io.InputStream

interface HttpHandler {
    suspend fun head(url: String): Map<String, String>
    suspend fun stream(url: String, start: Long, end: Long): InputStream
}
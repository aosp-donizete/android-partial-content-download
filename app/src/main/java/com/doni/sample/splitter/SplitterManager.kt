package com.doni.sample.splitter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.moveTo
import kotlin.io.path.outputStream
import kotlin.time.measureTimedValue

object SplitterManager {

    @Throws(IllegalStateException::class)
    suspend fun get(
        url: String,
        destiny: String,
        parts: Long,
        retry: Int,
        httpHandler: HttpHandler = OkHttpHandler
    ) {
        val head = httpHandler.head(url)
        val totalSize = (head["Content-Length"] ?: head["content-length"])?.toLong() ?: 0L
        if (totalSize <= 0) {
            throw IllegalStateException("File initial size cannot be recovered")
        }
        val ranges = SplitterUtils.generateRanges(
            totalSize,
            parts
        )
        val (partialFiles, time) = measureTimedValue {
            withContext(Dispatchers.IO) {
                ranges.map { async { getPartialFile(it, url, httpHandler) } }
            }.awaitAll()
        }

        println("Time to request ${partialFiles.size} items: ${time.inWholeSeconds}")

        val file = kotlin.io.path.createTempFile()

        file.outputStream().use { output ->
            partialFiles.forEach { partialFile ->
                partialFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }

        file.moveTo(Path(destiny), overwrite = true)

        partialFiles.forEach { it.deleteIfExists() }
    }

    private suspend fun getPartialFile(
        range: LongRange,
        url: String,
        httpHandler: HttpHandler
    ) = httpHandler.stream(
        url = url,
        start = range.first,
        end = range.last,
    ).use { input ->
        kotlin.io.path.createTempFile().also { path ->
            path.outputStream().use(input::copyTo)
        }
    }
}
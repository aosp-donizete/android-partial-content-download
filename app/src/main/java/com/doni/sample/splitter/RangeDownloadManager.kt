package com.doni.sample.splitter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class RangeDownloadManager(
    private val httpHandler: HttpHandler,
    private val parts: Long,
    private val retry: Long,
    private val retryDelay: Long = 1000,
    private val parallelism: Int = 2
) {
    @Throws(IllegalStateException::class)
    suspend fun download(url: String): Path {
        val ranges = generateRanges(getContentLength(url))

        val responses = Channel<RangeResponse>(ranges.size)
        val requests = Channel<RangeRequest>(ranges.size).apply {
            ranges.forEachIndexed { index, range -> trySend(RangeRequest(index, range, url)) }
        }

        val rangeResponses: List<RangeResponse> = withContext(Dispatchers.IO) {
            repeat(parallelism) { launch { handleRequests(requests, responses) } }
            buildList {
                var remaining = ranges.size
                responses.consumeEach {
                    add(it)
                    if (--remaining <= 0) {
                        requests.close()
                        responses.close()
                    }
                }
            }
        }.sortedBy { it.id }

        return mergeRangeResponses(rangeResponses)
    }

    @Throws(IllegalStateException::class)
    suspend fun download(
        rangeRequest: RangeRequest
    ): Path {
        for (i in 0 until retry) {
            val tmp = kotlin.io.path.createTempFile()
            return runCatching {
                httpHandler.stream(
                    url = rangeRequest.url,
                    start = rangeRequest.range.first,
                    end = rangeRequest.range.last,
                ).use { input ->
                    tmp.also { it.outputStream().use(input::copyTo) }
                }
            }.onFailure {
                tmp.deleteIfExists()
                delay(retryDelay)
            }.getOrNull() ?: continue
        }

        throw IllegalStateException("Max retry number reached for range request: $rangeRequest")
    }

    @Throws(IllegalStateException::class)
    private suspend fun getContentLength(url: String): Long {
        val head = httpHandler.head(url)

        val totalSize = (head["Content-Length"] ?: head["content-length"])?.toLong() ?: 0L
        if (totalSize <= 0) {
            throw IllegalStateException("File initial size cannot be recovered")
        }

        return totalSize
    }

    private fun generateRanges(
        contentLength: Long
    ) = (0 until parts).map {
        val start = it / 1F / parts
        val end = (it + 1) / 1F / parts
        val rs = (start * contentLength).toLong()
        val re = (end * contentLength).toLong()
        rs until re
    }.filterNot(LongRange::isEmpty)

    private suspend fun handleRequests(
        requests: ReceiveChannel<RangeRequest>,
        responses: SendChannel<RangeResponse>
    ) {
        for (request in requests) {
            val path = download(request)
            responses.trySend(RangeResponse(request.id, path))
        }
    }

    private fun mergeRangeResponses(
        rangeResponses: List<RangeResponse>
    ) = kotlin.io.path.createTempFile().also { path ->
        path.outputStream().use { output ->
            rangeResponses.forEach { response ->
                response.path.inputStream().use { input ->
                    input.copyTo(output)
                }
                response.path.deleteExisting()
            }
        }
    }

    data class RangeRequest(
        val id: Int,
        val range: LongRange,
        val url: String
    )

    data class RangeResponse(
        val id: Int,
        val path: Path
    )
}
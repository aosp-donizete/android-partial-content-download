package com.doni.sample.splitter

object SplitterUtils {
    fun generateRanges(
        size: Long,
        count: Long
    ) = buildList<LongRange> {
        val slice = size / count
        var control = 0L
        while (control + 1 <= size) {
            add(control until (control + slice))
            control += slice
        }
    }
}
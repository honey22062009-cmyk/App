package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ephemeral health counters; never stores addresses, queries, pages or field text. */
object BrowserGuardStatus {
    internal val connected = MutableStateFlow(false)
    val isConnected = connected.asStateFlow()
    private val inspectedCount = MutableStateFlow(0L)
    val inspections = inspectedCount.asStateFlow()
    private val blockCount = MutableStateFlow(0L)
    val blocks = blockCount.asStateFlow()
    internal fun inspected() { inspectedCount.value++ }
    internal fun blocked() { blockCount.value++ }
}

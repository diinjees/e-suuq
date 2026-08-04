package com.example.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ApiMonitor {
    private val _totalRequests = MutableStateFlow(0)
    val totalRequests: StateFlow<Int> = _totalRequests.asStateFlow()

    private val _recentRequests = MutableStateFlow<List<String>>(emptyList())
    val recentRequests: StateFlow<List<String>> = _recentRequests.asStateFlow()

    fun logRequest(method: String, url: String) {
        _totalRequests.value = _totalRequests.value + 1
        // Simplify url for display
        val simpleUrl = if (url.contains("api/collections/")) {
            url.substringAfter("api/collections/")
        } else {
            url.substringAfter(".io/")
        }
        val entry = "[$method] $simpleUrl"
        _recentRequests.value = (_recentRequests.value + entry).takeLast(10)
    }

    fun reset() {
        _totalRequests.value = 0
        _recentRequests.value = emptyList()
    }
}

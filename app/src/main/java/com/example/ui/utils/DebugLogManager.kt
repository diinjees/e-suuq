package com.example.ui.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogManager {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(tag: String, message: String, isError: Boolean = false) {
        val timestamp = dateFormat.format(Date())
        val prefix = if (isError) "❌" else "ℹ️"
        val formattedMsg = "$prefix [$timestamp] [$tag] $message"
        
        if (isError) {
            Log.e(tag, message)
        } else {
            Log.d(tag, message)
        }
        
        val currentList = _logs.value.toMutableList()
        currentList.add(0, formattedMsg) // Newest first
        if (currentList.size > 300) { // Limit buffer size
            currentList.removeAt(currentList.lastIndex)
        }
        _logs.value = currentList
    }

    fun clear() {
        _logs.value = emptyList()
        log("DebugLogManager", "🧹 Log history cleared.")
    }
}

package com.example.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.BufferOverflow
import com.example.BuildConfig
import com.example.ui.utils.DebugLogManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class PocketBaseRealtime(
    private val baseUrl: String,
    private var authToken: String? = null
) {
    private val TAG = "PocketBaseRealtime"

    private fun log(message: String, isError: Boolean = false) {
        DebugLogManager.log(TAG, message, isError)
    }

    // ✅ Track active subscriptions
    private val _activeTopics = MutableStateFlow<Set<String>>(emptySet())
    val activeTopics: StateFlow<Set<String>> = _activeTopics.asStateFlow()

    // ✅ Connection state ("DISCONNECTED", "CONNECTING", "CONNECTED", "ERROR")
    private val _connectionState = MutableStateFlow("DISCONNECTED")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    // ✅ Events
    private val _events = MutableSharedFlow<RealtimeEvent>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sseJob: Job? = null
    private var reconnectJob: Job? = null
    private var activeCall: Call? = null

    @Volatile
    private var clientId: String? = null

    @Volatile
    private var isExplicitlyDisconnected = false

    // OkHttpClient with infinite read timeout for long-lived SSE stream
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite read timeout for SSE stream
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS 
                    else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    // OkHttpClient with short timeouts for subscription POST requests
    private val postClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun updateAuthToken(token: String?) {
        this.authToken = token
    }

    // ✅ Connect via Server-Sent Events (SSE)
    @Synchronized
    fun connect() {
        if (_activeTopics.value.isEmpty()) {
            log("Not connecting SSE stream: no active topics subscribed")
            return
        }

        if (_connectionState.value == "CONNECTING" || _connectionState.value == "CONNECTED") {
            log("Already connected or connecting to SSE stream")
            return
        }

        isExplicitlyDisconnected = false

        reconnectJob?.cancel()
        sseJob?.cancel()
        activeCall?.cancel()

        _connectionState.value = "CONNECTING"

        sseJob = scope.launch {
            try {
                val cleanBaseUrl = baseUrl.trimEnd('/')
                val sseUrl = "$cleanBaseUrl/api/realtime"

                val requestBuilder = Request.Builder()
                    .url(sseUrl)
                    .header("Accept", "text/event-stream")
                    .header("Cache-Control", "no-cache")

                val token = authToken
                if (!token.isNullOrBlank()) {
                    val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
                    requestBuilder.header("Authorization", authHeader)
                }

                val request = requestBuilder.build()
                val call = client.newCall(request)
                activeCall = call

                log("Connecting SSE stream to $sseUrl...")
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        log("SSE connection failed with code: ${response.code}", isError = true)
                        _connectionState.value = "ERROR"
                        attemptReconnect()
                        return@use
                    }

                    val source = response.body?.source()
                    if (source == null) {
                        log("SSE response body is null", isError = true)
                        _connectionState.value = "ERROR"
                        attemptReconnect()
                        return@use
                    }

                    log("SSE Stream connected successfully. Waiting for events...")

                    var currentEvent = ""
                    val currentData = StringBuilder()

                    while (coroutineContext.isActive && !source.exhausted()) {
                        val line = source.readUtf8Line() ?: break

                        if (line.isEmpty()) {
                            if (currentEvent.isNotEmpty() || currentData.isNotEmpty()) {
                                handleSseMessage(currentEvent, currentData.toString())
                                currentEvent = ""
                                currentData.clear()
                            }
                            continue
                        }

                        if (line.startsWith(":")) {
                            // SSE comment / ping keepalive
                            continue
                        }

                        val colonPos = line.indexOf(':')
                        if (colonPos != -1) {
                            val fieldName = line.substring(0, colonPos).trim()
                            val fieldValue = line.substring(colonPos + 1).trimStart()

                            when (fieldName) {
                                "event" -> currentEvent = fieldValue
                                "data" -> {
                                    if (currentData.isNotEmpty()) currentData.append("\n")
                                    currentData.append(fieldValue)
                                }
                            }
                        }
                    }

                    log("SSE stream disconnected")
                    _connectionState.value = "DISCONNECTED"
                    attemptReconnect()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    log("SSE Exception: ${e.message}", isError = true)
                    _connectionState.value = "ERROR"
                    attemptReconnect()
                } else {
                    _connectionState.value = "DISCONNECTED"
                }
            }
        }
    }

    // ✅ Handle incoming SSE event message
    private fun handleSseMessage(event: String, data: String) {
        log("Received SSE Event [$event]: ${data.take(120)}...")
        try {
            if (event == "PB_CONNECT") {
                val json = JSONObject(data)
                clientId = json.optString("clientId")
                log("PocketBase SSE connected with clientId: $clientId")
                _connectionState.value = "CONNECTED"
                // Sync active subscriptions with server
                syncSubscriptions()
                return
            }

            if (data.isBlank()) return

            val json = JSONObject(data)
            val action = json.optString("action", "update")
            val recordObj = json.optJSONObject("record")
            val recordStr = recordObj?.toString() ?: json.optString("record", "")

            var collection = json.optString("collection")
            if (collection.isBlank() && recordObj != null) {
                collection = recordObj.optString("collectionName")
            }
            if (collection.isBlank()) {
                collection = event.substringBefore('/').substringBefore('?')
            }

            val realtimeEvent = RealtimeEvent.CollectionChange(
                collection = collection,
                action = action,
                record = recordStr
            )
            scope.launch {
                _events.emit(realtimeEvent)
            }
        } catch (e: Exception) {
            log("Error parsing SSE event=$event data=$data: ${e.message}", isError = true)
        }
    }

    // ✅ Sync subscriptions via POST /api/realtime
    private fun syncSubscriptions() {
        val cId = clientId
        if (cId.isNullOrBlank() || isExplicitlyDisconnected || _connectionState.value == "DISCONNECTED") {
            log("Cannot sync subscriptions: clientId is null or stream is disconnected")
            return
        }

        val topics = _activeTopics.value.toList()
        if (topics.isEmpty()) {
            log("No active topics remaining. Disconnecting SSE stream.")
            disconnect()
            return
        }

        scope.launch {
            try {
                val jsonPayload = JSONObject().apply {
                    put("clientId", cId)
                    put("subscriptions", JSONArray(topics))
                }.toString()

                val cleanBaseUrl = baseUrl.trimEnd('/')
                val postUrl = "$cleanBaseUrl/api/realtime"
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonPayload.toRequestBody(mediaType)

                val requestBuilder = Request.Builder()
                    .url(postUrl)
                    .post(requestBody)

                val token = authToken
                if (!token.isNullOrBlank()) {
                    val authHeader = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
                    requestBuilder.header("Authorization", authHeader)
                }

                val request = requestBuilder.build()
                postClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!isExplicitlyDisconnected && clientId == cId && _connectionState.value != "DISCONNECTED") {
                            log("Failed to send subscriptions POST to PocketBase: ${e.message}", isError = false)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (response.isSuccessful) {
                                log("Successfully synced ${topics.size} subscriptions: $topics")
                            } else {
                                if (isExplicitlyDisconnected || clientId != cId || _connectionState.value == "DISCONNECTED" || _activeTopics.value.isEmpty()) {
                                    log("Subscription POST returned ${response.code} (client disconnected or resetting)")
                                } else {
                                    log("Subscription POST status ${response.code}")
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                log("Exception in syncSubscriptions: ${e.message}", isError = false)
            }
        }
    }

    // ✅ Explicitly disconnect SSE connection
    @Synchronized
    fun disconnect() {
        isExplicitlyDisconnected = true
        reconnectJob?.cancel()
        sseJob?.cancel()
        activeCall?.cancel()
        clientId = null
        _connectionState.value = "DISCONNECTED"
        log("Explicitly disconnected PocketBase realtime SSE stream")
    }

    // ✅ Subscribe to topic
    fun subscribe(topic: String) {
        val current = _activeTopics.value
        val isNew = !current.contains(topic)
        if (isNew) {
            _activeTopics.value = current + topic
            log("Subscribing to realtime topic: $topic")
        }
        isExplicitlyDisconnected = false
        if (_connectionState.value != "CONNECTED" && _connectionState.value != "CONNECTING") {
            connect()
        } else if (isNew) {
            syncSubscriptions()
        }
    }

    // ✅ Unsubscribe from topic
    fun unsubscribe(topic: String) {
        val current = _activeTopics.value
        if (current.contains(topic)) {
            val newTopics = current - topic
            _activeTopics.value = newTopics
            log("Unsubscribing from realtime topic: $topic")
            if (newTopics.isEmpty()) {
                log("No active topics remaining. Disconnecting SSE stream.")
                disconnect()
            } else {
                syncSubscriptions()
            }
        }
    }

    // ✅ Subscribe to orders
    fun subscribeToOrders(userId: String, role: String) {
        val topic = when (role) {
            "SELLER" -> {
                val filter = "sellerId='$userId'"
                "orders?filter=$filter"
            }
            "BUYER" -> {
                val filter = "buyerId='$userId'"
                "orders?filter=$filter"
            }
            "DELIVERY" -> {
                val filter = "status='READY_FOR_PICKUP' && delivery_pickup=true && deliveryId=null"
                "orders?filter=$filter"
            }
            else -> "orders"
        }
        subscribe(topic)
    }

    // ✅ Unsubscribe from orders
    fun unsubscribeFromOrders(userId: String, role: String) {
        val topic = when (role) {
            "SELLER" -> {
                val filter = "sellerId='$userId'"
                "orders?filter=$filter"
            }
            "BUYER" -> {
                val filter = "buyerId='$userId'"
                "orders?filter=$filter"
            }
            "DELIVERY" -> {
                val filter = "status='READY_FOR_PICKUP' && delivery_pickup=true && deliveryId=null"
                "orders?filter=$filter"
            }
            else -> "orders"
        }
        unsubscribe(topic)
    }

    // ✅ Subscribe to order items (for detailed views)
    fun subscribeToOrderItems(orderId: String) {
        val topic = "order_items?filter=orderId='$orderId'"
        subscribe(topic)
    }

    // ✅ Unsubscribe from order items
    fun unsubscribeFromOrderItems(orderId: String) {
        val topic = "order_items?filter=orderId='$orderId'"
        unsubscribe(topic)
    }

    // ✅ Subscribe to products updates (for sellers)
    fun subscribeToProducts(sellerId: String) {
        val topic = "products?filter=sellerId='$sellerId'"
        subscribe(topic)
    }

    // ✅ Unsubscribe from products updates (for sellers)
    fun unsubscribeFromProducts(sellerId: String) {
        val topic = "products?filter=sellerId='$sellerId'"
        unsubscribe(topic)
    }

    // ✅ Subscribe to all order status changes for a specific order
    fun subscribeToOrderStatus(orderId: String) {
        val topic = "orders?filter=id='$orderId'"
        subscribe(topic)
    }

    // ✅ Reconnection logic
    private fun attemptReconnect() {
        if (isExplicitlyDisconnected) {
            log("Skipping reconnect: explicitly disconnected")
            return
        }
        if (_activeTopics.value.isEmpty()) {
            log("Skipping reconnect: no active topics subscribed")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5000)
            if (coroutineContext.isActive && !isExplicitlyDisconnected && _activeTopics.value.isNotEmpty()) {
                log("Attempting SSE reconnection...")
                connect()
            } else {
                log("Reconnect aborted: explicitly disconnected or no active topics")
            }
        }
    }

    // ✅ Cleanup resources
    fun cleanup() {
        isExplicitlyDisconnected = true
        reconnectJob?.cancel()
        sseJob?.cancel()
        activeCall?.cancel()
        clientId = null
        _connectionState.value = "DISCONNECTED"
        log("Cleaned up PocketBase realtime connection")
    }
}

sealed class RealtimeEvent {
    data class CollectionChange(
        val collection: String,
        val action: String,
        val record: String
    ) : RealtimeEvent()
}

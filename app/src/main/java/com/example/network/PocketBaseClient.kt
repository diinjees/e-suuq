package com.example.network

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.BuildConfig
import com.example.data.SessionManager
import java.util.concurrent.TimeUnit

class PocketBaseClient(private val context: Context) {
    
    companion object {
        // 🔄 CHANGE THIS TO YOUR POCKETBASE URL
        private const val BASE_URL = "https://metube.pockethost.io/" // Cloudflare Tunnel URL
        
        // Or if testing locally: "http://192.168.1.100:8090/"
        private const val TIMEOUT = 30L
    }

    // ✅ JSON serializer
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    // ✅ Auth token management
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    // ✅ OkHttp client with interceptors
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            ApiMonitor.logRequest(request.method, request.url.toString())
            chain.proceed(request)
        }
        .addInterceptor(AuthInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY 
                    else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    // ✅ API service
    private val apiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(
            com.google.gson.GsonBuilder()
                .registerTypeAdapter(Double::class.javaPrimitiveType, SafeDoubleAdapter())
                .registerTypeAdapter(Double::class.javaObjectType, SafeNullableDoubleAdapter())
                .registerTypeAdapter(Int::class.javaPrimitiveType, SafeIntAdapter())
                .registerTypeAdapter(Int::class.javaObjectType, SafeNullableIntAdapter())
                .registerTypeAdapter(Long::class.javaPrimitiveType, SafeLongAdapter())
                .registerTypeAdapter(Long::class.javaObjectType, SafeNullableLongAdapter())
                .registerTypeAdapter(BusinessPayoutInfo::class.java, BusinessPayoutInfoDeserializer())
                .registerTypeAdapter(BusinessDiscountCodeInfo::class.java, BusinessDiscountCodeInfoDeserializer())
                .create()
        ))
        .build()
        .create(PocketBaseApi::class.java)

    // ✅ Auth interceptor
    inner class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var token = _authToken.value
            
            // Fallback to SessionManager if current value in flow is null
            if (token == null) {
                val sessionManager = SessionManager(context)
                token = sessionManager.getAuthToken()
                val uid = sessionManager.getUserId()
                if (token != null && uid != null) {
                    _authToken.value = token
                    _userId.value = uid
                }
            }
            
            val urlStr = request.url.toString()
            val isTargetHost = urlStr.contains("metube.pockethost.io") || !urlStr.startsWith("http")
            
            val newRequest = if (token != null && isTargetHost) {
                request.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                request
            }
            return chain.proceed(newRequest)
        }
    }

    // ✅ Set auth token
    fun setAuthToken(token: String, userId: String) {
        _authToken.value = token
        _userId.value = userId
    }

    // ✅ Clear auth token
    fun clearAuth() {
        _authToken.value = null
        _userId.value = null
    }

    // ✅ Get API service
    fun getApi(): PocketBaseApi = apiService

    // ✅ Get JSON serializer
    fun getJson(): Json = json
}

class SafeDoubleAdapter : com.google.gson.TypeAdapter<Double>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: Double?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: com.google.gson.stream.JsonReader): Double {
        if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
            reader.nextNull()
            return 0.0
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.STRING) {
            val str = reader.nextString()
            if (str.isEmpty()) return 0.0
            return str.toDoubleOrNull() ?: 0.0
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.NUMBER) {
            return reader.nextDouble()
        }
        reader.skipValue()
        return 0.0
    }
}

class SafeNullableDoubleAdapter : com.google.gson.TypeAdapter<Double?>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: Double?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: com.google.gson.stream.JsonReader): Double? {
        if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.STRING) {
            val str = reader.nextString()
            if (str.isEmpty()) return null
            return str.toDoubleOrNull()
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.NUMBER) {
            return reader.nextDouble()
        }
        reader.skipValue()
        return null
    }
}

class SafeIntAdapter : com.google.gson.TypeAdapter<Int>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: Int?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: com.google.gson.stream.JsonReader): Int {
        if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
            reader.nextNull()
            return 0
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.STRING) {
            val str = reader.nextString()
            if (str.isEmpty()) return 0
            return str.toIntOrNull() ?: 0
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.NUMBER) {
            return reader.nextInt()
        }
        reader.skipValue()
        return 0
    }
}

class SafeNullableIntAdapter : com.google.gson.TypeAdapter<Int?>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: Int?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: com.google.gson.stream.JsonReader): Int? {
        if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.STRING) {
            val str = reader.nextString()
            if (str.isEmpty()) return null
            return str.toIntOrNull()
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.NUMBER) {
            return reader.nextInt()
        }
        reader.skipValue()
        return null
    }
}

class SafeLongAdapter : com.google.gson.TypeAdapter<Long>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: Long?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: com.google.gson.stream.JsonReader): Long {
        if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
            reader.nextNull()
            return 0L
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.STRING) {
            val str = reader.nextString()
            if (str.isEmpty()) return 0L
            return str.toLongOrNull() ?: 0L
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.NUMBER) {
            return reader.nextLong()
        }
        reader.skipValue()
        return 0L
    }
}

class SafeNullableLongAdapter : com.google.gson.TypeAdapter<Long?>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: Long?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: com.google.gson.stream.JsonReader): Long? {
        if (reader.peek() == com.google.gson.stream.JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.STRING) {
            val str = reader.nextString()
            if (str.isEmpty()) return null
            return str.toLongOrNull()
        }
        if (reader.peek() == com.google.gson.stream.JsonToken.NUMBER) {
            return reader.nextLong()
        }
        reader.skipValue()
        return null
    }
}
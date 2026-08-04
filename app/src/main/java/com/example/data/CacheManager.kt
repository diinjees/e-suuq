package com.example.data

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class CacheManager {
    private val cacheMap = ConcurrentHashMap<String, CacheEntry<*>>()
    
    // Performance monitoring metrics
    private val hits = AtomicInteger(0)
    private val misses = AtomicInteger(0)

    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val ttlMillis: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttlMillis
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cacheMap[key] as? CacheEntry<T>
        return if (entry != null && !entry.isExpired()) {
            hits.incrementAndGet()
            entry.data
        } else {
            misses.incrementAndGet()
            if (entry != null) {
                cacheMap.remove(key) // Evict expired entry
            }
            null
        }
    }

    fun <T> put(key: String, data: T, ttlMillis: Long) {
        cacheMap[key] = CacheEntry(data, System.currentTimeMillis(), ttlMillis)
    }

    fun remove(key: String) {
        cacheMap.remove(key)
    }

    fun clear() {
        cacheMap.clear()
        hits.set(0)
        misses.set(0)
    }

    fun getStats(): CacheStats {
        val h = hits.get()
        val m = misses.get()
        val total = h + m
        val rate = if (total > 0) (h.toFloat() / total.toFloat()) * 100f else 0f
        return CacheStats(
            hits = h,
            misses = m,
            hitRatePercent = rate,
            totalRequests = total
        )
    }

    companion object {
        private var instance: CacheManager? = null
        
        fun getInstance(): CacheManager {
            return instance ?: synchronized(this) {
                val current = instance ?: CacheManager()
                instance = current
                current
            }
        }
    }
}

data class CacheStats(
    val hits: Int,
    val misses: Int,
    val hitRatePercent: Float,
    val totalRequests: Int
)

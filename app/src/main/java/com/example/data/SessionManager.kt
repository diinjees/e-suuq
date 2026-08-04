package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "E_Suuq_Session_Preferences")

class SessionManager(private val context: Context) {
    
    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_PHONE = stringPreferencesKey("user_phone")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_LAST_LOGIN = longPreferencesKey("last_login")
        private val KEY_TOKEN_EXPIRY = longPreferencesKey("token_expiry")
        private val KEY_IS_FIRST_OPEN = booleanPreferencesKey("is_first_open")
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val KEY_LAST_SYNC_PRODUCTS = longPreferencesKey("last_sync_products")
        private val KEY_LAST_SYNC_SELLER_PRODUCTS = longPreferencesKey("last_sync_seller_products")
        private val KEY_LAST_SYNC_SELLER_ORDERS = longPreferencesKey("last_sync_seller_orders")
        private val KEY_CURRENT_LOGIN_HISTORY_ID = stringPreferencesKey("current_login_history_id")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    private fun <T> readPref(key: Preferences.Key<T>, default: T): T {
        return try {
            runBlocking {
                val prefs = context.sessionDataStore.data.firstOrNull()
                prefs?.get(key) ?: default
            }
        } catch (e: Exception) {
            default
        }
    }

    private fun <T> readPrefNullable(key: Preferences.Key<T>): T? {
        return try {
            runBlocking {
                val prefs = context.sessionDataStore.data.firstOrNull()
                prefs?.get(key)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun <T> writePref(key: Preferences.Key<T>, value: T?) {
        try {
            runBlocking {
                context.sessionDataStore.edit { prefs ->
                    if (value == null) {
                        prefs.remove(key)
                    } else {
                        prefs[key] = value
                    }
                }
            }
        } catch (e: Exception) {
            // handle error
        }
    }

    fun getCurrentLoginHistoryId(): String? = readPrefNullable(KEY_CURRENT_LOGIN_HISTORY_ID)
    fun setCurrentLoginHistoryId(id: String?) = writePref(KEY_CURRENT_LOGIN_HISTORY_ID, id)

    // ✅ Get last sync time for products
    fun getLastSyncProducts(): Long = readPref(KEY_LAST_SYNC_PRODUCTS, 0L)
    fun setLastSyncProducts(time: Long) = writePref(KEY_LAST_SYNC_PRODUCTS, time)

    // ✅ Get last sync time for seller products
    fun getLastSyncSellerProducts(): Long = readPref(KEY_LAST_SYNC_SELLER_PRODUCTS, 0L)
    fun setLastSyncSellerProducts(time: Long) = writePref(KEY_LAST_SYNC_SELLER_PRODUCTS, time)

    // ✅ Get last sync time for seller orders
    fun getLastSyncSellerOrders(): Long = readPref(KEY_LAST_SYNC_SELLER_ORDERS, 0L)
    fun setLastSyncSellerOrders(time: Long) = writePref(KEY_LAST_SYNC_SELLER_ORDERS, time)
    
    // Save user session after successful login
    fun saveUserSession(
        userId: String,
        phone: String,
        name: String,
        email: String,
        token: String,
        role: String = "BUYER"
    ) {
        try {
            runBlocking {
                context.sessionDataStore.edit { prefs ->
                    prefs[KEY_USER_ID] = userId
                    prefs[KEY_USER_PHONE] = phone
                    prefs[KEY_USER_NAME] = name
                    prefs[KEY_USER_EMAIL] = email
                    prefs[KEY_AUTH_TOKEN] = token
                    prefs[KEY_USER_ROLE] = role
                    prefs[KEY_LAST_LOGIN] = System.currentTimeMillis()
                    prefs[KEY_TOKEN_EXPIRY] = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
                }
            }
        } catch (e: Exception) {
            // handle error
        }
    }
    
    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return getAuthToken() != null && getUserId() != null
    }
    
    // Get user details
    fun getUserId(): String? = readPrefNullable(KEY_USER_ID)
    fun getUserPhone(): String? = readPrefNullable(KEY_USER_PHONE)
    fun getUserName(): String? = readPrefNullable(KEY_USER_NAME)
    fun getUserEmail(): String? = readPrefNullable(KEY_USER_EMAIL)
    fun getAuthToken(): String? = readPrefNullable(KEY_AUTH_TOKEN)
    fun getUserRole(): String? = readPrefNullable(KEY_USER_ROLE)
    fun getLastLogin(): Long = readPref(KEY_LAST_LOGIN, 0L)
    fun getTokenExpiry(): Long = readPref(KEY_TOKEN_EXPIRY, 0L)
    
    // Check if session is expired (7 days)
    fun isSessionExpired(): Boolean {
        val tokenExpiry = getTokenExpiry()
        return System.currentTimeMillis() > tokenExpiry
    }
    
    // Check if token needs refresh (within 1 day of expiry)
    fun needsTokenRefresh(): Boolean {
        val tokenExpiry = getTokenExpiry()
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() > (tokenExpiry - oneDayInMillis)
    }
    
    // Clear session (logout)
    fun clearSession() {
        try {
            runBlocking {
                context.sessionDataStore.edit { prefs ->
                    prefs.clear()
                }
            }
        } catch (e: Exception) {
            // handle error
        }
    }
    
    // First open check
    fun isFirstOpen(): Boolean = readPref(KEY_IS_FIRST_OPEN, true)
    fun setFirstOpen(firstOpen: Boolean) = writePref(KEY_IS_FIRST_OPEN, firstOpen)
    
    // Remember me option
    fun setRememberMe(remember: Boolean) = writePref(KEY_REMEMBER_ME, remember)
    fun getRememberMe(): Boolean = readPref(KEY_REMEMBER_ME, false)
    
    // Update token
    fun updateToken(newToken: String) {
        try {
            runBlocking {
                context.sessionDataStore.edit { prefs ->
                    prefs[KEY_AUTH_TOKEN] = newToken
                    prefs[KEY_TOKEN_EXPIRY] = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
                }
            }
        } catch (e: Exception) {
            // handle error
        }
    }
    
    // Get session data
    fun getSessionData(): SessionData? {
        val token = getAuthToken()
        val userId = getUserId()
        val role = getUserRole()
        
        return if (token != null && userId != null) {
            SessionData(
                token = token,
                userId = userId,
                role = role ?: "BUYER",
                name = getUserName(),
                phone = getUserPhone(),
                email = getUserEmail()
            )
        } else {
            null
        }
    }

    // Check if valid session exists
    fun hasValidSession(): Boolean {
        val token = getAuthToken()
        val userId = getUserId()
        return token != null && userId != null
    }

    // Biometric lock check
    fun setBiometricEnabled(enabled: Boolean) = writePref(KEY_BIOMETRIC_ENABLED, enabled)
    fun isBiometricEnabled(): Boolean = readPref(KEY_BIOMETRIC_ENABLED, false)
}

// Session data class
data class SessionData(
    val token: String,
    val userId: String,
    val role: String,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null
)

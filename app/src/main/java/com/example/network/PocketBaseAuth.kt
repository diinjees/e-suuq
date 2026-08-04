package com.example.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import retrofit2.HttpException
import com.example.data.SessionData
import com.example.data.SessionManager
import com.example.data.LoginHistoryEntity
import com.example.data.AppRepository
import com.example.ui.utils.DebugLogManager

private const val TAG = "PocketBaseAuth"

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: Int = -1) : AuthResult<Nothing>()
    data class MfaRequired(val mfaId: String) : AuthResult<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    val dataOrNull: T? get() = if (this is Success) data else null
}

class PocketBaseAuth(private val context: Context) {
    
    val client = PocketBaseClient(context)
    
    private val sessionManager = SessionManager(context)

    init {
        restoreSession()
    }

    // ==========================================
    // AUTH STATE
    // ==========================================

    // ✅ RESTORE SESSION ON APP START
    private fun restoreSession() {
        val token = sessionManager.getAuthToken()
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole()
        
        if (token != null && userId != null) {
            client.setAuthToken(token, userId)
            DebugLogManager.log(TAG, "✅ Session restored for user: $userId (role: $role)")
        } else {
            DebugLogManager.log(TAG, "No cached session found in SessionManager")
        }
    }

    // ✅ CHECK IF SESSION EXISTS
    fun hasValidSession(): Boolean {
        return sessionManager.hasValidSession()
    }

    // ✅ GET SESSION DATA
    fun getSessionData(): SessionData? {
        return sessionManager.getSessionData()
    }

    // ✅ CHECK IF TOKEN IS EXPIRED (Optional)
    fun isTokenExpired(): Boolean {
        return sessionManager.isSessionExpired()
    }

    // ✅ UPDATE TOKEN
    fun updateToken(newToken: String) {
        client.setAuthToken(newToken, getUserId() ?: "")
        sessionManager.updateToken(newToken)
    }

    // ✅ IS USER LOGGED IN
    fun isAuthenticated(): Boolean {
        return hasValidSession() && client.authToken.value != null
    }

    fun getUserId(): String? {
        return client.userId.value
    }

    fun getToken(): String? {
        return client.authToken.value
    }

    fun getUserRole(): String? {
        return sessionManager.getUserRole()
    }

    // ==========================================
    // LOGIN
    // ==========================================

    suspend fun login(identity: String, password: String): AuthResult<PbAuthResponse> = 
        withContext(Dispatchers.IO) {
            DebugLogManager.log(TAG, "🔑 Login initiated for identity: $identity")
            try {
                val response = client.getApi().login(LoginRequest(identity, password))
                DebugLogManager.log(TAG, "✅ Login API response received successfully! User ID: ${response.record.id}, Role: ${response.record.role ?: "BUYER"}")
                saveAuth(response.token, response.record.id, response.record.role ?: "BUYER")
                DebugLogManager.log(TAG, "🎉 Login complete and session successfully initialized!")
                AuthResult.Success(response)
            } catch (e: Exception) {
                DebugLogManager.log(TAG, "❌ Login failed or MFA required for identity: $identity. Error: ${e.message}", isError = true)
                if (e is HttpException) {
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        if (errorBody != null) {
                            val regex = """"mfaId"\s*:\s*"([^"]+)"""".toRegex()
                            val match = regex.find(errorBody)
                            if (match != null) {
                                val mfaId = match.groupValues[1]
                                DebugLogManager.log(TAG, "🔐 Real PocketBase MFA requested successfully! mfaId: $mfaId")
                                return@withContext AuthResult.MfaRequired(mfaId)
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Failed to parse MFA error body", ex)
                    }
                }
                AuthResult.Error(e.message ?: "Unknown login error")
            }
        }

    // ==========================================
    // REGISTER AS BUYER (Only option)
    // ==========================================

    suspend fun registerAsBuyer(
        email: String,
        phone: String,
        fullName: String,
        password: String,
        address: String? = null,
        addressRegion: String? = null,
        addressZone: String? = null,
        addressCity: String? = null,
        addressKebele: String? = null,
        birthDate: String? = null,
        idDocumentFront: File? = null,
        idDocumentBack: File? = null
    ): AuthResult<PbUserRecord> = withContext(Dispatchers.IO) {
        try {
            val fields = mutableMapOf<String, RequestBody>()
            
            // Required fields
            fields["email"] = email.toRequestBody("text/plain".toMediaType())
            fields["phone"] = phone.toRequestBody("text/plain".toMediaType())
            fields["fullName"] = fullName.toRequestBody("text/plain".toMediaType())
            fields["password"] = password.toRequestBody("text/plain".toMediaType())
            fields["passwordConfirm"] = password.toRequestBody("text/plain".toMediaType())
            fields["role"] = "BUYER".toRequestBody("text/plain".toMediaType())
            fields["emailVisibility"] = true.toString().toRequestBody("text/plain".toMediaType())
            fields["status"] = false.toString().toRequestBody("text/plain".toMediaType())
            
            // Optional fields
            addressRegion?.let { fields["addressRegion"] = it.toRequestBody("text/plain".toMediaType()) }
            addressZone?.let { fields["addressZone"] = it.toRequestBody("text/plain".toMediaType()) }
            addressCity?.let { fields["addressCity"] = it.toRequestBody("text/plain".toMediaType()) }
            addressKebele?.let { fields["addressKebele"] = it.toRequestBody("text/plain".toMediaType()) }
            birthDate?.let { fields["birthDate"] = it.toRequestBody("text/plain".toMediaType()) }

            val fileParts = mutableListOf<MultipartBody.Part>()
            
            // ID Document Front
            idDocumentFront?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaType())
                fileParts.add(
                    MultipartBody.Part.createFormData(
                        "idDocumentInfront",
                        file.name,
                        requestFile
                    )
                )
            }
            
            // ID Document Back
            idDocumentBack?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaType())
                fileParts.add(
                    MultipartBody.Part.createFormData(
                        "idDocumentBackfront",
                        file.name,
                        requestFile
                    )
                )
            }

            val response = client.getApi().registerWithFiles(fields, fileParts)
            AuthResult.Success(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "Registration error", e)
            AuthResult.Error(e.message ?: "Unknown registration error")
        }
    }

    // ==========================================
    // APPLY AS SELLER
    // ==========================================

    suspend fun applyAsSeller(
        userId: String,
        businessName: String,
        businessPhone: String,
        region: String,
        zone: String,
        city: String,
        kebele: String,
        tinNumber: String,
        businessDocumentFront: File? = null,
        businessDocumentBack: File? = null
    ): AuthResult<SellerProfile> = withContext(Dispatchers.IO) {
        try {
            val fields = mutableMapOf<String, RequestBody>()
            
            // Required fields
            fields["user"] = userId.toRequestBody("text/plain".toMediaType())
            fields["businessName"] = businessName.toRequestBody("text/plain".toMediaType())
            fields["businessPhone"] = businessPhone.toRequestBody("text/plain".toMediaType())
            fields["region"] = region.toRequestBody("text/plain".toMediaType())
            fields["zone"] = zone.toRequestBody("text/plain".toMediaType())
            fields["city"] = city.toRequestBody("text/plain".toMediaType())
            fields["kebele"] = kebele.toRequestBody("text/plain".toMediaType())
            fields["tinNumber"] = tinNumber.toRequestBody("text/plain".toMediaType())
            fields["sellerVerification"] = "pending".toRequestBody("text/plain".toMediaType())
            fields["businessGradeTitel"] = "Bronze".toRequestBody("text/plain".toMediaType())
            fields["businessGrade"] = "0.0".toRequestBody("text/plain".toMediaType())
            fields["businessRating"] = "0.0".toRequestBody("text/plain".toMediaType())

            val fileParts = mutableListOf<MultipartBody.Part>()
            
            // Business Document Front
            businessDocumentFront?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaType())
                fileParts.add(
                    MultipartBody.Part.createFormData(
                        "businessDocumentFront",
                        file.name,
                        requestFile
                    )
                )
            }
            
            // Business Document Back
            businessDocumentBack?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaType())
                fileParts.add(
                    MultipartBody.Part.createFormData(
                        "businessDocumentBack",
                        file.name,
                        requestFile
                    )
                )
            }

            val response = client.getApi().createSellerProfileWithFiles(fields, fileParts)
            
            val updateFields = mapOf(
            "seller_profile" to response.id
            )

            val updatedUser = client.getApi().updateUser(userId, updateFields)
            
            AuthResult.Success(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "Seller application error", e)
            AuthResult.Error(e.message ?: "Unknown seller application error")
        }
    }

    // ==========================================
    // APPLY AS DELIVERY
    // ==========================================

    suspend fun applyAsDelivery(
        userId: String,
        plateNumber: String,
        phoneNumber: String,
        vehicleType: String,
        driverLicenseFront: File? = null,
        driverLicenseBack: File? = null,
        vehicleDocumentFront: File? = null,
        vehicleDocumentBack: File? = null
    ): AuthResult<DeliveryProfile> = withContext(Dispatchers.IO) {
        try {
            val fields = mutableMapOf<String, RequestBody>()
            
            // Required fields
            fields["user"] = userId.toRequestBody("text/plain".toMediaType())
            fields["plateNumber"] = plateNumber.toRequestBody("text/plain".toMediaType())
            fields["phoneNumber"] = phoneNumber.toRequestBody("text/plain".toMediaType())
            fields["vehicleType"] = vehicleType.toRequestBody("text/plain".toMediaType())
            fields["deliveryVerification"] = "pending".toRequestBody("text/plain".toMediaType())
            fields["deliveryGrade"] = "Novice".toRequestBody("text/plain".toMediaType())
            fields["xpTotal"] = "0".toRequestBody("text/plain".toMediaType())
            fields["ratingTotal"] = "0.0".toRequestBody("text/plain".toMediaType())

            val fileParts = mutableListOf<MultipartBody.Part>()
            
            // Driver License Front
            driverLicenseFront?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaType())
                fileParts.add(
                    MultipartBody.Part.createFormData(
                        "driverLicenseFront",
                        file.name,
                        requestFile
                    )
                )
            }
            
            // Driver License Back
            driverLicenseBack?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaType())
                fileParts.add(
                    MultipartBody.Part.createFormData(
                        "driverLicenseBack",
                        file.name,
                        requestFile
                    )
                )
            }
            
            // Vehicle Document Front
            vehicleDocumentFront?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaType())
                fileParts.add(
                    MultipartBody.Part.createFormData(
                        "vehicleDocumentFront",
                        file.name,
                        requestFile
                    )
                )
            }
            
            // Vehicle Document Back
            vehicleDocumentBack?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaType())
                fileParts.add(
                    MultipartBody.Part.createFormData(
                        "vehicleDocumentBack",
                        file.name,
                        requestFile
                    )
                )
            }

            val response = client.getApi().createDeliveryProfileWithFiles(fields, fileParts)
            
            val updateFields = mapOf(
            "delivery_profile" to response.id
            )

            val updatedUser = client.getApi().updateUser(userId, updateFields)
            
            AuthResult.Success(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "Delivery application error", e)
            AuthResult.Error(e.message ?: "Unknown delivery application error")
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    suspend fun getSellerByUserId(userId: String): SellerProfile? = withContext(Dispatchers.IO) {
        try {
            val response = client.getApi().getSellerProfiles(filter = "user='$userId'")
            response.items.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateSellerProfile(profileId: String, fields: Map<String, @JvmSuppressWildcards Any>): AuthResult<SellerProfile> = withContext(Dispatchers.IO) {
        try {
            val response = client.getApi().updateSellerProfile(profileId, fields)
            AuthResult.Success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update seller profile", e)
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun getDeliveryByUserId(userId: String): DeliveryProfile? = withContext(Dispatchers.IO) {
        try {
            val response = client.getApi().getDeliveryProfiles(filter = "user='$userId'")
            response.items.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // ✅ SAVE AUTH WITH EXPIRY
    private fun saveAuth(token: String, userId: String, role: String) {
        DebugLogManager.log(TAG, "💾 Saving auth session: token length = ${token.length}, userId = $userId, role = $role")
        client.setAuthToken(token, userId)
        sessionManager.saveUserSession(
            userId = userId,
            phone = "",
            name = "",
            email = "",
            token = token,
            role = role
        )
        DebugLogManager.log(TAG, "💾 SessionManager updated successfully")
    }

    suspend fun refreshToken(): AuthResult<PbAuthResponse> = withContext(Dispatchers.IO) {
        val currentToken = client.authToken.value
        if (currentToken == null) {
            AuthResult.Error("No active token")
        } else {
            try {
                val response = client.getApi().refreshAuth("Bearer $currentToken")
                saveAuth(response.token, response.record.id, response.record.role ?: "BUYER")
                AuthResult.Success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh error", e)
                AuthResult.Error(e.message ?: "Unknown token refresh error")
            }
        }
    }

    fun logout() {
        client.clearAuth()
        sessionManager.clearSession()
    }

    // ==========================================
    // LOGIN HISTORY & DEVICES (MOVED FROM REPOSITORY)
    // ==========================================

    suspend fun isLoginSessionRemoved(userId: String): Boolean = withContext(Dispatchers.IO) {
        val loginId = sessionManager.getCurrentLoginHistoryId()
        if (loginId.isNullOrEmpty()) {
            recordDeviceLogin(userId)
            return@withContext false
        }
        try {
            val response = client.getApi().getLoginHistory(filter = "id='$loginId'")
            val exists = response.items.isNotEmpty()
            DebugLogManager.log(TAG, "🔍 Checking login history id: $loginId. Exists on PocketBase? $exists")
            return@withContext !exists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login history on PocketBase", e)
            return@withContext false
        }
    }

    suspend fun recordDeviceLogin(userId: String) = withContext(Dispatchers.IO) {
        try {
            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            val brand = android.os.Build.BRAND
            val model = android.os.Build.MODEL
            val osVersion = android.os.Build.VERSION.RELEASE
            val sdkVersion = android.os.Build.VERSION.SDK_INT.toString()
            val fingerprint = android.os.Build.FINGERPRINT ?: ""
            
            // Detect emulator
            val isEmulator = (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                    || android.os.Build.FINGERPRINT.startsWith("generic")
                    || android.os.Build.FINGERPRINT.startsWith("unknown")
                    || android.os.Build.HARDWARE.contains("goldfish")
                    || android.os.Build.HARDWARE.contains("ranchu")
                    || android.os.Build.MODEL.contains("google_sdk")
                    || android.os.Build.MODEL.contains("Emulator")
                    || android.os.Build.MODEL.contains("Android SDK built for x86")
                    || android.os.Build.MANUFACTURER.contains("Genymotion")
                    || android.os.Build.PRODUCT.contains("sdk_google")
                    || android.os.Build.PRODUCT.contains("google_sdk")
                    || android.os.Build.PRODUCT.contains("sdk")
                    || android.os.Build.PRODUCT.contains("sdk_x86")
                    || android.os.Build.PRODUCT.contains("vbox86p")
                    || android.os.Build.PRODUCT.contains("emulator")
                    || android.os.Build.PRODUCT.contains("simulator")

            // Determine device type
            val isTablet = (context.resources.configuration.screenLayout and 
                            android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) >= 
                           android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
            val deviceType = when {
                isEmulator -> "EMULATOR"
                isTablet -> "TABLET"
                else -> "MOBILE"
            }

            // Get Serial number or fallback to Android ID
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            @Suppress("DEPRECATION")
            val serialNumber = if (android.os.Build.SERIAL != android.os.Build.UNKNOWN && android.os.Build.SERIAL.isNotEmpty()) {
                android.os.Build.SERIAL
            } else {
                androidId
            }

            // Get App Name and Version
            var appName = "Essuq App"
            var appVersion = "1.0.0"
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                appVersion = packageInfo.versionName ?: "1.0.0"
                
                val applicationInfo = context.applicationInfo
                val stringId = applicationInfo.labelRes
                appName = if (stringId == 0) applicationInfo.nonLocalizedLabel?.toString() ?: "Essuq" else context.getString(stringId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting app name/version", e)
            }

            // Retrieve Public IP and Location dynamically
            var ipAddress = "127.0.0.1"
            var location = "Ethiopia"
            try {
                val request = okhttp3.Request.Builder()
                    .url("https://ipapi.co/json/")
                    .build()
                client.okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrEmpty()) {
                            val jsonObject = org.json.JSONObject(bodyString)
                            ipAddress = jsonObject.optString("ip", "127.0.0.1")
                            val city = jsonObject.optString("city", "")
                            val country = jsonObject.optString("country_name", "Ethiopia")
                            location = if (city.isNotEmpty()) "$city, $country" else country
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching public IP/location, using local defaults", e)
            }

            val item = PbLoginHistoryItem(
                user = userId,
                device_name = deviceName,
                device_type = deviceType,
                model = model,
                operatingSystem = "Android",
                osVersion = osVersion,
                sdk_version = sdkVersion,
                browser = appName,
                ip_address = ipAddress,
                location = location,
                brand = brand,
                is_active = true,
                login_status = "success", // success, failed, expired
                browser_version = appVersion,
                serial_number = serialNumber,
                device_fingerprint = fingerprint,
                is_emulator = isEmulator
            )
            
            var existingRecordId: String? = null
            
            val currentLoginId = sessionManager.getCurrentLoginHistoryId()
            if (!currentLoginId.isNullOrEmpty()) {
                try {
                    val resp = client.getApi().getLoginHistory(filter = "id='$currentLoginId'")
                    if (resp.items.isNotEmpty()) {
                        existingRecordId = resp.items.first().id
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking login history by ID", e)
                }
            }

            if (existingRecordId == null) {
                try {
                    val filterStr = if (serialNumber.isNotEmpty() && serialNumber != "unknown") {
                        "user='$userId' && serial_number='$serialNumber'"
                    } else if (fingerprint.isNotEmpty()) {
                        "user='$userId' && device_fingerprint='$fingerprint'"
                    } else {
                        "user='$userId' && device_name='$deviceName'"
                    }
                    val resp = client.getApi().getLoginHistory(filter = filterStr)
                    if (resp.items.isNotEmpty()) {
                        existingRecordId = resp.items.first().id
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking login history by device criteria", e)
                }
            }

            if (existingRecordId != null) {
                client.getApi().updateLoginHistory(existingRecordId, item)
                DebugLogManager.log(TAG, "🔄 Updated existing login_history on PocketBase! ID: $existingRecordId")
                sessionManager.setCurrentLoginHistoryId(existingRecordId)
            } else {
                val createdItem = client.getApi().createLoginHistory(item)
                DebugLogManager.log(TAG, "✅ Saved new login_history on PocketBase! ID: ${createdItem.id}")
                sessionManager.setCurrentLoginHistoryId(createdItem.id)
            }
            syncLoginHistory()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving login history on PocketBase", e)
        }
    }

    suspend fun removeLoginDevice(deviceId: String) = withContext(Dispatchers.IO) {
        try {
            client.getApi().deleteLoginHistory(deviceId)
            DebugLogManager.log(TAG, "🗑️ Deleted login history item with ID: $deviceId on PocketBase")
            syncLoginHistory()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting login history from PocketBase", e)
        }
    }

    suspend fun syncLoginHistory() = withContext(Dispatchers.IO) {
        try {
            val userId = sessionManager.getUserId() ?: return@withContext
            val response = client.getApi().getLoginHistory(filter = "user='$userId'")
            val entities = response.items.map { item ->
                LoginHistoryEntity(
                    id = item.id ?: "",
                    userId = item.user,
                    timestamp = System.currentTimeMillis(),
                    deviceName = item.device_name,
                    deviceType = item.device_type,
                    operatingSystem = item.operatingSystem,
                    osVersion = item.osVersion,
                    ipAddress = item.ip_address,
                    location = item.location,
                    status = item.login_status,
                    created = item.created,
                    updated = item.updated,
                    pocketBaseId = item.id,
                    brand = item.brand,
                    browser = item.browser,
                    isActive = item.is_active,
                    loginStatus = item.login_status,
                    browserVersion = item.browser_version,
                    serialNumber = item.serial_number,
                    model = item.model,
                    sdkVersion = item.sdk_version,
                    deviceFingerprint = item.device_fingerprint,
                    isEmulator = item.is_emulator
                )
            }
            AppRepository.getInstance(context).updateLoginHistoryList(entities)
            DebugLogManager.log(TAG, "🔄 Synced ${entities.size} login history records from PocketBase")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing login history from PocketBase", e)
        }
    }

    // ==========================================
    // GET USER WITH PROFILES
    // ==========================================

    suspend fun getUserWithProfiles(userId: String): AuthResult<PocketBaseUserRecord> = 
        withContext(Dispatchers.IO) {
            try {
                val response = client.getApi().getUser(userId, expand = "seller_profile,delivery_profile")
                AuthResult.Success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Get user error", e)
                AuthResult.Error(e.message ?: "Failed to get user")
            }
        }

    // ==========================================
    // CHECK APPLICATION STATUS
    // ==========================================

    suspend fun getSellerApplicationStatus(userId: String): String? {
        return try {
            val response = client.getApi().getSellerProfiles(filter = "user='$userId'")
            response.items.firstOrNull()?.sellerVerification
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getDeliveryApplicationStatus(userId: String): String? {
        return try {
            val response = client.getApi().getDeliveryProfiles(filter = "user='$userId'")
            response.items.firstOrNull()?.deliveryVerification
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // OTP / MFA METHODS
    // ==========================================

    suspend fun requestOtp(email: String): AuthResult<RequestOtpResponse> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "📧 Requesting OTP from PocketBase for email: $email")
            val response = client.getApi().requestOtp(RequestOtpRequest(email))
            DebugLogManager.log(TAG, "✅ OTP request successful. otpId: ${response.otpId}")
            if (!response.code.isNullOrBlank()) {
                DebugLogManager.log(TAG, "🔑 [DEVELOPER] OTP code received from PocketBase: ${response.code}")
            }
            AuthResult.Success(response)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ OTP request failed: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to request OTP from PocketBase")
        }
    }

    suspend fun authWithOtp(otpId: String, code: String): AuthResult<PocketBaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "🔑 Verifying OTP with PocketBase. otpId: $otpId, code: $code")
            val response = client.getApi().authWithOtp(AuthWithOtpRequest(otpId, code))
            DebugLogManager.log(TAG, "✅ OTP verification successful. Token length: ${response.token.length}")
            saveAuth(response.token, response.record.id, response.record.role ?: "BUYER")
            AuthResult.Success(response)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ OTP verification failed: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to verify OTP with PocketBase")
        }
    }

    suspend fun authWithMfa(mfaId: String, otpId: String, code: String): AuthResult<PocketBaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "🔑 Verifying MFA with PocketBase. mfaId: $mfaId, otpId: $otpId, code: $code")
            val response = client.getApi().authWithOtp(AuthWithOtpRequest(otpId, code, mfaId))
            DebugLogManager.log(TAG, "✅ MFA verification successful. Token length: ${response.token.length}")
            saveAuth(response.token, response.record.id, response.record.role ?: "BUYER")
            AuthResult.Success(response)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ MFA verification failed: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to verify MFA with PocketBase")
        }
    }

    suspend fun updateTwoFactorEnabled(userId: String, enabled: Boolean): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "🔄 Updating isTwoFactorEnabled on PocketBase for user: $userId to $enabled")
            val updateFields = mapOf("isTwoFactorEnabled" to enabled)
            client.getApi().updateUser(userId, updateFields)
            DebugLogManager.log(TAG, "✅ Two-Factor status successfully updated on PocketBase to: $enabled")
            AuthResult.Success(enabled)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Failed to update isTwoFactorEnabled on PocketBase: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to update Two-Factor status on PocketBase")
        }
    }

    suspend fun findUserByIdentity(identity: String): PocketBaseUserRecord? = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "🔍 Querying users for identity: $identity")
            val filter = "phone='$identity' || email='$identity'"
            val response = client.getApi().getUsers(filter)
            response.items.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error finding user by identity: $identity", e)
            null
        }
    }

    suspend fun updatePassword(oldPassword: String, newPassword: String): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uId = client.userId.value ?: return@withContext AuthResult.Error("No user logged in")
            DebugLogManager.log(TAG, "🔄 Updating password on PocketBase for user: $uId")
            val fields = mapOf(
                "oldPassword" to oldPassword,
                "password" to newPassword,
                "passwordConfirm" to newPassword
            )
            client.getApi().updateUser(uId, fields)
            DebugLogManager.log(TAG, "✅ Password successfully updated on PocketBase")
            AuthResult.Success(true)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Failed to change password on PocketBase: ${e.message}", isError = true)
            val errMsg = if (e is HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (!errorBody.isNullOrBlank()) {
                        if (errorBody.contains("oldPassword")) {
                            "The current password you entered is incorrect."
                        } else if (errorBody.contains("password")) {
                            "The new password is invalid or does not meet requirements."
                        } else {
                            "Error changing password: $errorBody"
                        }
                    } else {
                        e.message() ?: "Failed to change password"
                    }
                } catch (ex: Exception) {
                    e.message() ?: "Failed to change password"
                }
            } else {
                e.message ?: "Failed to change password"
            }
            AuthResult.Error(errMsg)
        }
    }

    // ==========================================
    // EMAIL CHANGE METHODS
    // ==========================================

    suspend fun requestEmailChange(newEmail: String, userId: String? = null): AuthResult<RequestEmailChangeResponse> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "📧 Requesting email change from PocketBase to: $newEmail")
            val response = client.getApi().requestEmailChange(RequestEmailChangeRequest(newEmail, userId))
            DebugLogManager.log(TAG, "✅ Email change requested. otpId: ${response.otpId}")
            AuthResult.Success(response)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Email change request failed: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to request email change")
        }
    }

    suspend fun confirmEmailChange(otpId: String, code: String, newEmail: String? = null): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "🔑 Confirming email change with OTP. otpId: $otpId, code: $code")
            val response = client.getApi().confirmEmailChange(ConfirmEmailChangeRequest(otpId, code, newEmail))
            val isSuccess = response.isSuccessful
            DebugLogManager.log(TAG, "✅ Email change confirmation status: $isSuccess")
            if (isSuccess) {
                AuthResult.Success(true)
            } else {
                AuthResult.Error("Confirmation failed with status code: ${response.code()}")
            }
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Email change confirmation failed: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to confirm email change")
        }
    }

    suspend fun resendEmailChangeOtp(otpId: String, email: String? = null): AuthResult<RequestEmailChangeResponse> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "🔄 Resending email change OTP for otpId: $otpId")
            val response = client.getApi().resendEmailChangeOtp(ResendEmailChangeOtpRequest(otpId, email))
            DebugLogManager.log(TAG, "✅ Resent OTP successfully. New otpId: ${response.otpId}")
            AuthResult.Success(response)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Resending email change OTP failed: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to resend email change OTP")
        }
    }

    suspend fun cancelEmailChange(otpId: String): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "❌ Cancelling email change for otpId: $otpId")
            val response = client.getApi().cancelEmailChange(CancelEmailChangeRequest(otpId))
            val isSuccess = response.isSuccessful
            DebugLogManager.log(TAG, "✅ Email change cancellation status: $isSuccess")
            if (isSuccess) {
                AuthResult.Success(true)
            } else {
                AuthResult.Error("Cancellation failed with status code: ${response.code()}")
            }
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Cancelling email change failed: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to cancel email change")
        }
    }

    suspend fun uploadProfileImage(userId: String, imageFile: File): AuthResult<PocketBaseUserRecord> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "📸 Uploading custom profile image to PocketBase for user: $userId")
            val fields = mapOf<String, RequestBody>()
            val requestFile = imageFile.asRequestBody("image/*".toMediaType())
            val filePart = MultipartBody.Part.createFormData(
                "profile_image",
                imageFile.name,
                requestFile
            )
            val updatedUser = client.getApi().updateUserWithFiles(userId, fields, listOf(filePart))
            DebugLogManager.log(TAG, "✅ Profile image successfully uploaded. Filename: ${updatedUser.profile_image}")
            AuthResult.Success(updatedUser)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Failed to upload profile image to PocketBase: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to upload profile image")
        }
    }

    suspend fun updateProfileImagePreset(userId: String, presetId: String): AuthResult<PocketBaseUserRecord> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "📸 Updating profile image preset on PocketBase for user: $userId to $presetId")
            val updateFields = mapOf("profile_image" to presetId)
            val updatedUser = client.getApi().updateUser(userId, updateFields)
            DebugLogManager.log(TAG, "✅ Profile image preset successfully updated. Result: ${updatedUser.profile_image}")
            AuthResult.Success(updatedUser)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Failed to update profile image preset on PocketBase: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to update profile image preset")
        }
    }

    suspend fun uploadSellerProfileImage(profileId: String, imageFile: File): AuthResult<SellerProfile> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "📸 Uploading seller profile image to PocketBase for profile: $profileId")
            val fields = mapOf<String, RequestBody>()
            val requestFile = imageFile.asRequestBody("image/*".toMediaType())
            val filePart = MultipartBody.Part.createFormData(
                "seller_profile_image",
                imageFile.name,
                requestFile
            )
            val updatedProfile = client.getApi().updateSellerProfileWithFiles(profileId, fields, listOf(filePart))
            DebugLogManager.log(TAG, "✅ Seller profile image successfully uploaded. Filename: ${updatedProfile.seller_profile_image}")
            AuthResult.Success(updatedProfile)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Failed to upload seller profile image: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to upload seller profile image")
        }
    }

    suspend fun uploadSellerProfileCover(profileId: String, imageFile: File): AuthResult<SellerProfile> = withContext(Dispatchers.IO) {
        try {
            DebugLogManager.log(TAG, "📸 Uploading seller profile cover to PocketBase for profile: $profileId")
            val fields = mapOf<String, RequestBody>()
            val requestFile = imageFile.asRequestBody("image/*".toMediaType())
            val filePart = MultipartBody.Part.createFormData(
                "seller_profile_cover",
                imageFile.name,
                requestFile
            )
            val updatedProfile = client.getApi().updateSellerProfileWithFiles(profileId, fields, listOf(filePart))
            DebugLogManager.log(TAG, "✅ Seller profile cover successfully uploaded. Filename: ${updatedProfile.seller_profile_cover}")
            AuthResult.Success(updatedProfile)
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "❌ Failed to upload seller profile cover: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to upload seller profile cover")
        }
    }
}

// ==========================================
// EXTENSION FUNCTION FOR FILE TO REQUEST BODY
// ==========================================

fun File.asRequestBody(contentType: String): RequestBody {
    return RequestBody.create(contentType.toMediaType(), this)
}

fun String.toRequestBody(contentType: String): RequestBody {
    return this.toRequestBody(contentType.toMediaType())
}
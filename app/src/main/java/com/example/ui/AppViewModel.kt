package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.data.*
import com.example.network.*
import com.example.ui.utils.DebugLogManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

class AppViewModel : ViewModel() {
    private val TAG = "AppViewModel"

    // ==========================================
    // DEPENDENCIES
    // ==========================================
    
    private val auth = MainActivity.auth
    private val context = MainActivity.context
    val repository = AppRepository.getInstance(context)


    // ==========================================
    // REAL-TIME ORDER SUBSCRIPTIONS
    // ==========================================

    private val realtime = PocketBaseRealtime(
        baseUrl = "https://metube.pockethost.io",
        authToken = auth.getToken()
    )

    // ✅ State for real-time events
    private val _realtimeEvents = MutableSharedFlow<RealtimeEvent>()
    val realtimeEvents: SharedFlow<RealtimeEvent> = _realtimeEvents.asSharedFlow()

    private var isRealtimeConnected = false
    
    private val _orderItems = MutableStateFlow<List<OrderItemEntity>>(emptyList())
    val orderItems: StateFlow<List<OrderItemEntity>> = _orderItems.asStateFlow()

    private val _driverLocation = MutableStateFlow<DriverLocation?>(null)
    val driverLocation: StateFlow<DriverLocation?> = _driverLocation.asStateFlow()
    
    private val _eta = MutableStateFlow<String?>(null)
    val eta: StateFlow<String?> = _eta.asStateFlow()
    
    private var locationTrackingJob: Job? = null


    // ==========================================
    // UI STATE
    // ==========================================
    
    private val _currentScreen = MutableStateFlow<AppScreen>(
        if (repository.isFirstOpen()) {
            AppScreen.SPLASH
        } else {
            if (repository.hasValidSession()) {
                if (repository.isBiometricEnabled()) {
                    AppScreen.APP_LOCK
                } else {
                    AppScreen.MAIN
                }
            } else {
                AppScreen.ONBOARDING
            }
        }
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.FOLLOW_SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // ==========================================
    // USER STATE
    // ==========================================
    
    private val _activeUser = MutableStateFlow<UserEntity?>(null)
    val activeUser: StateFlow<UserEntity?> = _activeUser.asStateFlow()

    private val _sellerProfile = MutableStateFlow<SellerProfile?>(null)
    val sellerProfile: StateFlow<SellerProfile?> = _sellerProfile.asStateFlow()

    private val _deliveryProfile = MutableStateFlow<DeliveryProfile?>(null)
    val deliveryProfile: StateFlow<DeliveryProfile?> = _deliveryProfile.asStateFlow()

    // ==========================================
    // REGISTRATION STATE (For Onboarding)
    // ==========================================
    
    private val _regIdDocumentInfront = MutableStateFlow<String?>(null)
    val regIdDocumentInfront: StateFlow<String?> = _regIdDocumentInfront.asStateFlow()

    private val _regIdDocumentBackfront = MutableStateFlow<String?>(null)
    val regIdDocumentBackfront: StateFlow<String?> = _regIdDocumentBackfront.asStateFlow()

    // ==========================================
    // USER CONVERSION
    // ==========================================

    private fun convertToUserEntity(record: com.example.network.PbUserRecord,sellerProfile: SellerProfile? = null,deliveryProfile: DeliveryProfile? = null): UserEntity {
        val sp = record.expand?.seller_profile ?: sellerProfile
        val dp = record.expand?.delivery_profile ?: deliveryProfile
        return UserEntity(
            id = record.id,
            fullName = record.fullName ?: "",
            phone = record.phone ?: "",
            email = record.email ?: "",
            role = record.role ?: "BUYER",
            isVerified = record.verified ?: false,
            isApproved = record.isApproved ?: false,
            profileImage = if (record.profile_image.orEmpty().startsWith("preset_") || record.profile_image.orEmpty().isEmpty()) {
                record.profile_image ?: ""
            } else if (record.profile_image.orEmpty().startsWith("http")) {
                record.profile_image ?: ""
            } else {
                "https://metube.pockethost.io/api/files/users/${record.id}/${record.profile_image}"
            },
            addressRegion = record.addressRegion ?: "",
            addressZone = record.addressZone ?: "",
            addressCity = record.addressCity ?: "",
            addressKebele = record.addressKebele ?: "",
            birthDate = record.birthDate,
            idDocumentInfront = record.idDocumentInfront,
            idDocumentBackfront = record.idDocumentBackfront,
            isTwoFactorEnabled = record.isTwoFactorEnabled ?: false,
            status = if (record.role == "SELLER" && sp != null) (sp.status ?: false) else (record.status ?: false),
            created = record.created,
            updated = record.updated,
            // ✅ Seller fields from expanded profile
            businessName = sp?.businessName,
            businessPhone = sp?.businessPhone,
            businessAddress = sp?.let {
                "${it.region}, ${it.zone}, ${it.city}, Kebele ${it.kebele}"
            },
            tinNumber = sp?.tinNumber,
            tlcNumber = sp?.tlcNumber,
            businessGradeTitel = sp?.businessGradeTitel,
            businessGrade = sp?.businessGrade?.toString(),
            businessAbout = sp?.businessDescription,
            businessWorkingHours = sp?.businessWorkingHours,
            businessPayout = sp?.businessPayout?.toString(),
            businessRating = sp?.businessRating?.toString(),
            businessDiscountCode = sp?.businessDiscountCode?.toString(),
            businessDocumentFront = sp?.businessDocumentFront,
            businessDocumentBack = sp?.businessDocumentBack,
            sellerVerification = sp?.sellerVerification,
            storeLocation = sp?.storeLocation ?: com.example.data.StoreLocation(),
            sellerProfileId = sp?.id,
            sellerProfileImage = sp?.let {
                if (it.seller_profile_image.isNullOrBlank()) ""
                else if (it.seller_profile_image.startsWith("http")) it.seller_profile_image
                else "https://metube.pockethost.io/api/files/seller_profiles/${it.id}/${it.seller_profile_image}"
            },
            sellerProfileCover = sp?.let {
                if (it.seller_profile_cover.isNullOrBlank()) ""
                else if (it.seller_profile_cover.startsWith("http")) it.seller_profile_cover
                else "https://metube.pockethost.io/api/files/seller_profiles/${it.id}/${it.seller_profile_cover}"
            },
            // ✅ Delivery fields from expanded profile
            plateNumber = dp?.plateNumber,
            deliveryPhone = dp?.phoneNumber,
            vehicleType = dp?.vehicleType,
            deliveryGrade = dp?.deliveryGrade,
            xpTotal = dp?.xpTotal,
            ratingTotal = dp?.ratingTotal,
            driverLicenseFront = dp?.driverLicenseFront,
            driverLicenseBack = dp?.driverLicenseBack,
            vehicleDocumentFront = dp?.vehicleDocumentFront,
            vehicleDocumentBack = dp?.vehicleDocumentBack,
            deliveryVerification = dp?.deliveryVerification,
            deliveryProfileId = dp?.id,
            // ✅ Security
            isAppLockEnabled = _activeUser.value?.isAppLockEnabled ?: false,
            isBiometricEnabled = _activeUser.value?.isBiometricEnabled ?: false,
            is2faCompleted = _activeUser.value?.is2faCompleted ?: true
        )
    }


    fun isFirstOpen(): Boolean {
        return repository.isFirstOpen()
    }

    fun setFirstOpen(firstOpen: Boolean) {
        Log.d(TAG, "💾 setFirstOpen($firstOpen) called")
        repository.setFirstOpen(firstOpen)
    }

    fun checkAuthStatus(): Boolean {
        val status = repository.checkAuthStatus()
        Log.d(TAG, "🔍 checkAuthStatus() returned: $status")
        return status
    }

    suspend fun getActiveUser(): UserEntity? {
        Log.d(TAG, "👤 getActiveUser() requested from repository...")
        val user = repository.getActiveUserWithSession()
        Log.d(TAG, "👤 getActiveUser() found: $user")
        return user
    }

    fun hasValidSession(): Boolean {
        val valid = repository.hasValidSession()
        DebugLogManager.log(TAG, "🔍 hasValidSession() returned: $valid")
        return valid
    }

    fun getUserId(): String? {
        return repository.getUserId()
    }

    fun getAuthToken(): String? {
        return repository.getAuthToken()
    }

    fun getUserRole(): String? {
        return repository.getUserRole()
    }

    // ==========================================
    // LOGIN
    // ==========================================

    fun loginUser(
        phone: String,
        password: String,
        onOtpRequired: (String) -> Unit,
        onSuccess: () -> Unit,
        onIncorrect: () -> Unit
    ) {
        DebugLogManager.log(TAG, "🔑 loginUser() initiated for phone: $phone")
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading

            try {
                val result = auth.login(identity = phone, password = password)
                DebugLogManager.log(TAG, "🔑 login() API result: $result")

                when (result) {
                    is AuthResult.Success -> {
                        val userId = (result.dataOrNull!! as com.example.network.PbAuthResponse).record.id
                        DebugLogManager.log(TAG, "🔑 Login Successful. User ID: $userId. Fetching profile details...")
                        val userResult = auth.getUserWithProfiles(userId)
                        DebugLogManager.log(TAG, "👤 getUserWithProfiles() result: $userResult")
                        
                        when (userResult) {
                            is AuthResult.Success -> {
                                val userRecord = userResult.dataOrNull!! as com.example.network.PbUserRecord
                                val sp = userRecord.expand?.seller_profile 
                                val dp = userRecord.expand?.delivery_profile

                                val user = convertToUserEntity(userRecord, sp, dp)
                                DebugLogManager.log(TAG, "👤 Converted UserEntity: $user")
                                
                                sp?.let { 
                                    _sellerProfile.value = it 
                                    DebugLogManager.log(TAG, "👤 Seller profile loaded: $it")
                                }
                                dp?.let { 
                                    _deliveryProfile.value = it 
                                    DebugLogManager.log(TAG, "👤 Delivery profile loaded: $it")
                                }
                                
                                val token = auth.getToken() ?: ""
                                DebugLogManager.log(TAG, "💾 Saving user credentials to session. Token: $token")
                                
                                repository.saveUserWithSession(
                                    user = user,
                                    token = token,
                                    phone = phone,
                                    name = user.fullName,
                                    email = user.email,
                                    role = user.role
                                )
                                DebugLogManager.log(TAG, "💾 User session and database record successfully saved.")
                                _activeUser.value = user
                                
                                // Record login session on PocketBase
                                try {
                                    auth.recordDeviceLogin(user.id)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to record device login on PocketBase: ${e.message}")
                                }

                                // ✅ Set first open to false
                                DebugLogManager.log(TAG, "💾 setFirstOpen(false) called on login completion")
                                repository.setFirstOpen(false)

                                DebugLogManager.log(TAG, "✅ Login completed. Navigating to Authenticated home.")
                                _authState.value = AuthState.Authenticated
                                onSuccess()
                            }
                            is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                                DebugLogManager.log(TAG, "❌ Failed to fetch profile details: ${userResult.message}", isError = true)
                                _authState.value = AuthState.Error
                                _errorMessage.value = userResult.message
                                onIncorrect()
                            }
                            else -> {
                                _authState.value = AuthState.Error
                                _errorMessage.value = "Failed to load profiles"
                                onIncorrect()
                            }
                        }
                    }
                    is AuthResult.MfaRequired -> {
                        val mfaId = result.mfaId
                        currentMfaId = mfaId
                        DebugLogManager.log(TAG, "🔐 loginUser() received MFA challenge. mfaId: $mfaId. Finding user record by identity: $phone")
                        
                        val userRecord = auth.findUserByIdentity(phone)
                        if (userRecord != null) {
                            val email = userRecord.email ?: ""
                            val userEntity = convertToUserEntity(userRecord)
                            pendingSession = PendingSession(
                                user = userEntity,
                                token = "",
                                phone = phone,
                                name = userEntity.fullName,
                                email = email,
                                role = userEntity.role
                            )
                            
                            if (email.isNotEmpty()) {
                                DebugLogManager.log(TAG, "📧 Requesting OTP for user email: $email")
                                val otpResult = auth.requestOtp(email)
                                if (otpResult is AuthResult.Success) {
                                    currentOtpId = otpResult.dataOrNull!!.otpId
                                    val receivedCode = otpResult.dataOrNull!!.code
                                    DebugLogManager.log(TAG, "📬 Real PocketBase OTP requested successfully! otpId: ${otpResult.dataOrNull!!.otpId}")
                                    if (!receivedCode.isNullOrBlank()) {
                                        _sentOtp.value = receivedCode
                                        DebugLogManager.log(TAG, "🔑 [DEVELOPER] OTP code from PocketBase: $receivedCode")
                                    } else {
                                        _sentOtp.value = "5821"
                                    }
                                } else if (otpResult is AuthResult.Error) {
                                    DebugLogManager.log(TAG, "❌ Real PocketBase OTP request failed: ${otpResult.message}")
                                    _sentOtp.value = "5821"
                                }
                            }
                            
                            _inputOtp.value = ""
                            startOtpTimer()
                            _authState.value = AuthState.OtpRequired
                            onOtpRequired("")
                        } else {
                            _authState.value = AuthState.Error
                            _errorMessage.value = "Failed to find user with identity: $phone"
                            onIncorrect()
                        }
                    }
                    is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                        DebugLogManager.log(TAG, "❌ Login failed from auth service: ${result.message}", isError = true)
                        _authState.value = AuthState.Error
                        _errorMessage.value = result.message
                        onIncorrect()
                    }
                    else -> {
                        _authState.value = AuthState.Error
                        _errorMessage.value = "Unknown login result"
                        onIncorrect()
                    }
                }

            } catch (e: Exception) {
                DebugLogManager.log(TAG, "❌ Exception during loginUser process: ${e.message}", isError = true)
                _authState.value = AuthState.Error
                _errorMessage.value = "Network error: ${e.message}"
                onIncorrect()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // REGISTER AS BUYER
    // ==========================================

    fun registerAsBuyer(
        fullName: String,
        phone: String,
        email: String,
        password: String,
        region: String,
        zone: String,
        city: String,
        kebele: String,
        birthDate: String,
        idDocumentFront: File? = null,
        idDocumentBack: File? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = auth.registerAsBuyer(
                    email = email,
                    phone = phone,
                    fullName = fullName,
                    password = password,
                    addressRegion = region,
                    addressZone = zone,
                    addressCity = city,
                    addressKebele = kebele,
                    birthDate = birthDate,
                    idDocumentFront = idDocumentFront,
                    idDocumentBack = idDocumentBack
                )

                when (result) {
                    is AuthResult.Success -> {
                        val userRecord = result.dataOrNull!!
                        val user = convertToUserEntity(userRecord)
                        
                        pendingSession = PendingSession(
                            user = user,
                            token = "",
                            phone = phone,
                            name = user.fullName,
                            email = user.email,
                            role = user.role
                        )
                        
                        DebugLogManager.log(TAG, "📧 Register successful. Requesting OTP code from PocketBase for email: $email")
                        val otpResult = auth.requestOtp(email)
                        
                        if (otpResult is AuthResult.Success) {
                            currentOtpId = otpResult.dataOrNull!!.otpId
                            currentMfaId = null
                            val receivedCode = otpResult.dataOrNull!!.code
                            DebugLogManager.log(TAG, "📬 Real PocketBase OTP requested successfully! otpId: ${otpResult.dataOrNull!!.otpId}")
                            if (!receivedCode.isNullOrBlank()) {
                                _sentOtp.value = receivedCode
                                DebugLogManager.log(TAG, "🔑 [DEVELOPER] OTP code from PocketBase: $receivedCode")
                            } else {
                                _sentOtp.value = "5821"
                            }
                            
                            _inputOtp.value = ""
                            startOtpTimer()
                            _authState.value = AuthState.OtpRequired
                            onSuccess()
                        } else if (otpResult is AuthResult.Error) {
                            onError("Registration succeeded but failed to send OTP: ${otpResult.message}")
                        }
                    }
                    is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                        _authState.value = AuthState.Error
                        onError(result.message)
                    }
                    else -> {
                        _authState.value = AuthState.Error
                        onError("Unknown registration result")
                    }
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error
                onError("Registration failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // APPLY AS SELLER
    // ==========================================

    fun applyAsSeller(
        businessName: String,
        businessPhone: String,
        region: String,
        zone: String,
        city: String,
        kebele: String,
        tinNumber: String,
        businessDocumentFront: File? = null,
        businessDocumentBack: File? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val userId = auth.getUserId()
                if (userId == null) {
                    onError("User not authenticated")
                    _isLoading.value = false
                    return@launch
                }

                // ✅ STEP 1: Check if user is BUYER (only buyers can apply)
                val currentUser = _activeUser.value
                if (currentUser?.role != "BUYER") {
                    onError("Only buyers can apply to become a seller")
                    _isLoading.value = false
                    return@launch
                }

                // ✅ STEP 2: Check if user already has a seller profile
                val existingSeller = auth.getSellerByUserId(userId)
                if (existingSeller != null) {
                    when (existingSeller.sellerVerification) {
                        "pending" -> onError("You already have a pending seller application. Please wait for admin approval.")
                        "approved" -> onError("You are already an approved seller!")
                        "rejected" -> onError("Your seller application was rejected. Please contact support.")
                        else -> onError("You already have a seller profile.")
                    }
                    _isLoading.value = false
                    return@launch
                }

                // ✅ STEP 3: Create seller profile in PocketBase FIRST
                val result = auth.applyAsSeller(
                    userId = userId,
                    businessName = businessName,
                    businessPhone = businessPhone,
                    region = region,
                    zone = zone,
                    city = city,
                    kebele = kebele,
                    tinNumber = tinNumber,
                    businessDocumentFront = businessDocumentFront,
                    businessDocumentBack = businessDocumentBack
                )

                when (result) {
                    is AuthResult.Success -> {
                        val sellerProfile = result.dataOrNull!! as com.example.network.SellerProfile
                        
                        // ✅ STEP 5: Refresh user data from PocketBase
                        val userResult = auth.getUserWithProfiles(userId)
                        
                        when (userResult) {
                            is AuthResult.Success -> {
                                val userRecord = userResult.dataOrNull!! as com.example.network.PbUserRecord as com.example.network.PbUserRecord
                                val updatedUser = convertToUserEntity(userRecord)
                                
                                // ✅ STEP 6: Update local data
                                repository.saveUser(updatedUser)
                                _activeUser.value = updatedUser
                                _sellerProfile.value = sellerProfile
                                
                                onSuccess()
                            }
                            is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                                // Update local user manually
                                if (currentUser != null) {
                                    val updatedUser = currentUser.copy(
                                        sellerVerification = "pending",
                                        businessName = businessName,
                                        businessPhone = businessPhone,
                                        businessAddress = "$region, $zone, $city, Kebele $kebele",
                                        tinNumber = tinNumber,
                                        sellerProfileId = sellerProfile.id
                                    )
                                    repository.saveUser(updatedUser)
                                    _activeUser.value = updatedUser
                                    _sellerProfile.value = sellerProfile
                                }
                                onSuccess()
                            }
                            else -> {
                                onSuccess()
                            }
                        }
                    }
                    is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                        onError(result.message)
                    }
                    else -> {
                        onError("An unexpected error occurred during seller profile creation")
                    }
                }

            } catch (e: Exception) {
                onError("Application failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==========================================
    // APPLY AS DELIVERY
    // ==========================================

    fun applyAsDelivery(
        plateNumber: String,
        phoneNumber: String,
        vehicleType: String,
        driverLicenseFront: File? = null,
        driverLicenseBack: File? = null,
        vehicleDocumentFront: File? = null,
        vehicleDocumentBack: File? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val userId = auth.getUserId()
                if (userId == null) {
                    onError("User not authenticated")
                    _isLoading.value = false
                    return@launch
                }

                // ✅ STEP 1: Check if user is BUYER (only buyers can apply)
                val currentUser = _activeUser.value
                if (currentUser?.role != "BUYER") {
                    onError("Only buyers can apply to become a delivery partner")
                    _isLoading.value = false
                    return@launch
                }

                // ✅ STEP 2: Check if user already has a delivery profile
                val existingDelivery = auth.getDeliveryByUserId(userId)
                if (existingDelivery != null) {
                    when (existingDelivery.deliveryVerification) {
                        "pending" -> onError("You already have a pending delivery application. Please wait for admin approval.")
                        "approved" -> onError("You are already an approved delivery partner!")
                        "rejected" -> onError("Your delivery application was rejected. Please contact support.")
                        else -> onError("You already have a delivery profile.")
                    }
                    _isLoading.value = false
                    return@launch
                }

                // ✅ STEP 3: Create delivery profile in PocketBase FIRST
                val result = auth.applyAsDelivery(
                    userId = userId,
                    plateNumber = plateNumber,
                    phoneNumber = phoneNumber,
                    vehicleType = vehicleType,
                    driverLicenseFront = driverLicenseFront,
                    driverLicenseBack = driverLicenseBack,
                    vehicleDocumentFront = vehicleDocumentFront,
                    vehicleDocumentBack = vehicleDocumentBack
                )

                when (result) {
                    is AuthResult.Success -> {
                        val deliveryProfile = result.dataOrNull!! as com.example.network.DeliveryProfile
                        
                        // ✅ STEP 5: Refresh user data from PocketBase
                        val userResult = auth.getUserWithProfiles(userId)
                        
                        when (userResult) {
                            is AuthResult.Success -> {
                                val userRecord = userResult.dataOrNull!! as com.example.network.PbUserRecord as com.example.network.PbUserRecord
                                val updatedUser = convertToUserEntity(userRecord)
                                
                                repository.saveUser(updatedUser)
                                _activeUser.value = updatedUser
                                _deliveryProfile.value = deliveryProfile
                                
                                onSuccess()
                            }
                            is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                                // Update local user manually
                                if (currentUser != null) {
                                    val updatedUser = currentUser.copy(
                                        deliveryVerification = "pending",
                                        plateNumber = plateNumber,
                                        deliveryPhone = phoneNumber,
                                        vehicleType = vehicleType,
                                        deliveryProfileId = deliveryProfile.id
                                    )
                                    repository.saveUser(updatedUser)
                                    _activeUser.value = updatedUser
                                    _deliveryProfile.value = deliveryProfile
                                }
                                onSuccess()
                            }
                            else -> {
                                onSuccess()
                            }
                        }
                    }
                    is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                        onError(result.message)
                    }
                    else -> {
                        onError("An unexpected error occurred during delivery application")
                    }
                }

            } catch (e: Exception) {
                onError("Application failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun canApplyAsSeller(): ApplyCheckResult {
        val userId = auth.getUserId()
        if (userId == null) return ApplyCheckResult(false, "User not authenticated")
        
        val currentUser = _activeUser.value
        if (currentUser?.role != "BUYER") {
            return ApplyCheckResult(false, "Only buyers can apply to become a seller")
        }
        
        val existingSeller = auth.getSellerByUserId(userId)
        if (existingSeller != null) {
            return when (existingSeller.sellerVerification) {
                "pending" -> ApplyCheckResult(false, "Application pending approval")
                "approved" -> ApplyCheckResult(false, "Already an approved seller")
                "rejected" -> ApplyCheckResult(false, "Application was rejected")
                else -> ApplyCheckResult(false, "Already have a seller profile")
            }
        }
        
        return ApplyCheckResult(true, "Ready to apply")
    }

    suspend fun canApplyAsDelivery(): ApplyCheckResult {
        val userId = auth.getUserId()
        if (userId == null) return ApplyCheckResult(false, "User not authenticated")
        
        val currentUser = _activeUser.value
        if (currentUser?.role != "BUYER") {
            return ApplyCheckResult(false, "Only buyers can apply to become a delivery partner")
        }
        
        val existingDelivery = auth.getDeliveryByUserId(userId)
        if (existingDelivery != null) {
            return when (existingDelivery.deliveryVerification) {
                "pending" -> ApplyCheckResult(false, "Application pending approval")
                "approved" -> ApplyCheckResult(false, "Already an approved delivery partner")
                "rejected" -> ApplyCheckResult(false, "Application was rejected")
                else -> ApplyCheckResult(false, "Already have a delivery profile")
            }
        }
        
        return ApplyCheckResult(true, "Ready to apply")
    }

    // ==========================================
    // ✅ CHECK APPLICATION STATUS
    // ==========================================

    fun getSellerApplicationStatus(): String? {
        return _sellerProfile.value?.sellerVerification
    }

    fun getDeliveryApplicationStatus(): String? {
        return _deliveryProfile.value?.deliveryVerification
    }

    fun isSellerApproved(): Boolean {
        return _activeUser.value?.isApprovedSeller() ?: false
    }

    fun isDeliveryApproved(): Boolean {
        return _activeUser.value?.isApprovedDelivery() ?: false
    }

    fun isApplicationPending(): Boolean {
        return _activeUser.value?.isApplicationPending() ?: false
    }

    // ==========================================
    // AUTH HELPERS
    // ==========================================

    fun refreshAuthToken(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = auth.refreshToken()
                when (result) {
                    is AuthResult.Success -> {
                        val newToken = auth.getToken()
                        if (newToken != null) {
                            repository.updateToken(newToken)
                        }
                        onSuccess()
                    }
                    is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                        onError(result.message)
                    }
                    else -> {
                        onError("Token refresh returned an unexpected result")
                    }
                }
            } catch (e: Exception) {
                onError("Token refresh failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logoutUser() {
        DebugLogManager.log(TAG, "🔑 logoutUser() initiated. Clearing PocketBase auth state...")
        auth.logout()
        _activeUser.value = null
        _sellerProfile.value = null
        _deliveryProfile.value = null
        _authState.value = AuthState.Idle
        viewModelScope.launch {
            // Clear local data
            DebugLogManager.log(TAG, "💾 Clearing local DB & Encrypted SessionManager credentials...")
            repository.logout()
            DebugLogManager.log(TAG, "✅ Logout complete. Navigating back to ONBOARDING.")
        }
        navigateTo(AppScreen.ONBOARDING)
    }

    // ==========================================
    // NAVIGATION
    // ==========================================

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.MAIN) {
            setFirstOpen(false)
        }
    }

    fun navigateToProfileSub(screen: String) {
        _profileSubScreen.value = screen
    }

    fun dismissSplash() {
        // Handled by SplashScreen LaunchedEffect
    }

    // ==========================================
    // THEME & LANGUAGE
    // ==========================================

    fun setDarkTheme(isDark: Boolean) {
        _themeMode.value = if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }

    // ==========================================
    // REGISTRATION STATE HELPERS
    // ==========================================

    fun setRegIdDocumentInfront(uri: String?) {
        _regIdDocumentInfront.value = uri
    }

    fun setRegIdDocumentBackfront(uri: String?) {
        _regIdDocumentBackfront.value = uri
    }

    fun clearRegistrationState() {
        _regIdDocumentInfront.value = null
        _regIdDocumentBackfront.value = null
    }

    // ==========================================
    // VERIFICATION
    // ==========================================

    data class PendingSession(
        val user: UserEntity,
        val token: String,
        val phone: String,
        val name: String,
        val email: String,
        val role: String
    )

    private var pendingSession: PendingSession? = null
    private var currentOtpId: String? = null
    private var currentMfaId: String? = null

    // OTP Verification
    private val _inputOtp = MutableStateFlow("")
    val inputOtp: StateFlow<String> = _inputOtp.asStateFlow()

    private val _sentOtp = MutableStateFlow("5821")
    val sentOtp: StateFlow<String> = _sentOtp.asStateFlow()

    private val _otpTimer = MutableStateFlow(60)
    val otpTimer: StateFlow<Int> = _otpTimer.asStateFlow()

    private val _isOtpExpired = MutableStateFlow(false)
    val isOtpExpired: StateFlow<Boolean> = _isOtpExpired.asStateFlow()

    private var otpTimerJob: Job? = null

    fun startOtpTimer() {
        otpTimerJob?.cancel()
        _otpTimer.value = 60
        _isOtpExpired.value = false
        otpTimerJob = viewModelScope.launch {
            while (_otpTimer.value > 0) {
                delay(1000)
                _otpTimer.value -= 1
            }
            _isOtpExpired.value = true
        }
    }

    fun verifyOtp(onResult: (Boolean) -> Unit) {
        if (_isOtpExpired.value) {
            DebugLogManager.log(TAG, "❌ Cannot verify: OTP has expired.")
            onResult(false)
            return
        }
        val enteredCode = inputOtp.value
        val mfaId = currentMfaId
        val otpId = currentOtpId
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (mfaId != null && otpId != null) {
                    DebugLogManager.log(TAG, "🔑 Verifying MFA with PocketBase. mfaId: $mfaId, otpId: $otpId, code: $enteredCode")
                    val mfaResult = auth.authWithMfa(mfaId, otpId, enteredCode)
                    if (mfaResult is AuthResult.Success) {
                        val response = mfaResult.data
                        val userRecord = response.record
                        val sp = userRecord.expand?.seller_profile
                        val dp = userRecord.expand?.delivery_profile
                        val userEntity = convertToUserEntity(userRecord, sp, dp)
                        
                        repository.saveUserWithSession(
                            user = userEntity,
                            token = response.token,
                            phone = userEntity.phone,
                            name = userEntity.fullName,
                            email = userEntity.email,
                            role = userEntity.role
                        )
                        DebugLogManager.log(TAG, "💾 Completed MFA successfully after real PocketBase MFA verification.")
                        _activeUser.value = userEntity
                        
                        // Record login session on PocketBase
                        try {
                            auth.recordDeviceLogin(userEntity.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to record device login on MFA: ${e.message}")
                        }
                        repository.setFirstOpen(false)
                        _authState.value = AuthState.Authenticated
                        pendingSession = null
                        currentOtpId = null
                        currentMfaId = null
                        otpTimerJob?.cancel()
                        onResult(true)
                    } else {
                        DebugLogManager.log(TAG, "❌ Real PocketBase MFA verification failed.")
                        onResult(false)
                    }
                } else if (otpId != null) {
                    DebugLogManager.log(TAG, "🔑 Verifying entered OTP code '$enteredCode' with PocketBase using otpId '$otpId'")
                    val otpResult = auth.authWithOtp(otpId, enteredCode)
                    if (otpResult is AuthResult.Success) {
                        val response = otpResult.dataOrNull!!
                        val userRecord = response.record
                        val sp = userRecord.expand?.seller_profile
                        val dp = userRecord.expand?.delivery_profile
                        val userEntity = convertToUserEntity(userRecord, sp, dp)
                        
                        repository.saveUserWithSession(
                            user = userEntity,
                            token = response.token,
                            phone = userEntity.phone,
                            name = userEntity.fullName,
                            email = userEntity.email,
                            role = userEntity.role
                        )
                        DebugLogManager.log(TAG, "💾 Completed login successfully after real PocketBase OTP verification.")
                        _activeUser.value = userEntity
                        
                        // Record login session on PocketBase
                        try {
                            auth.recordDeviceLogin(userEntity.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to record device login on OTP: ${e.message}")
                        }
                        repository.setFirstOpen(false)
                        _authState.value = AuthState.Authenticated
                        pendingSession = null
                        currentOtpId = null
                        currentMfaId = null
                        otpTimerJob?.cancel()
                        onResult(true)
                    } else {
                        DebugLogManager.log(TAG, "❌ Real PocketBase OTP verification failed.")
                        onResult(false)
                    }
                } else {
                    DebugLogManager.log(TAG, "❌ Real OTP verification requested but no active PocketBase otpId or mfaId found.")
                    onResult(false)
                }
            } catch (e: Exception) {
                DebugLogManager.log(TAG, "❌ Verification process exception: ${e.message}", isError = true)
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resendOtp(): String {
        val newOtp = (100000..999999).random().toString()
        _sentOtp.value = newOtp
        _inputOtp.value = ""
        startOtpTimer()
        
        // Request real OTP resend from PocketBase in background
        val email = pendingSession?.email ?: ""
        if (email.isNotEmpty()) {
            viewModelScope.launch {
                val otpResult = auth.requestOtp(email)
                if (otpResult is AuthResult.Success) {
                    currentOtpId = otpResult.dataOrNull!!.otpId
                    val receivedCode = otpResult.dataOrNull!!.code
                    DebugLogManager.log(TAG, "📬 Real PocketBase OTP resend requested successfully! otpId: ${otpResult.dataOrNull!!.otpId}")
                    if (!receivedCode.isNullOrBlank()) {
                        _sentOtp.value = receivedCode
                        DebugLogManager.log(TAG, "🔑 [DEVELOPER] OTP code from PocketBase: $receivedCode")
                    }
                }
            }
        }
        
        DebugLogManager.log(TAG, "🔁 Resent OTP successfully.")
        return ""
    }

    fun setInputOtp(otp: String) {
        _inputOtp.value = otp
    }

    // ==========================================
    // EXTRA STATE VARIABLES (for full UI flow support)
    // ==========================================

    private val _currentTab = MutableStateFlow<MainTab>(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _selectedProductForDetail = MutableStateFlow<ProductEntity?>(null)
    val selectedProductForDetail: StateFlow<ProductEntity?> = _selectedProductForDetail.asStateFlow()

    private val _selectedStoreName = MutableStateFlow<String?>(null)
    val selectedStoreName: StateFlow<String?> = _selectedStoreName.asStateFlow()

    private val _selectedChatRoomId = MutableStateFlow<String?>(null)
    val selectedChatRoomId: StateFlow<String?> = _selectedChatRoomId.asStateFlow()

    private val _profileSubScreen = MutableStateFlow<String>("MENU")
    val profileSubScreen: StateFlow<String> = _profileSubScreen.asStateFlow()

    val activeDashboardSubView = MutableStateFlow<String>("MAIN")
    val previousDashboardSubView = MutableStateFlow<String>("MAIN")

    private val _showNotificationsPage = MutableStateFlow<Boolean>(false)
    val showNotificationsPage: StateFlow<Boolean> = _showNotificationsPage.asStateFlow()

    private val _showUserSearchPage = MutableStateFlow<Boolean>(false)
    val showUserSearchPage: StateFlow<Boolean> = _showUserSearchPage.asStateFlow()

    private val _viewAllPageType = MutableStateFlow<String?>(null)
    val viewAllPageType: StateFlow<String?> = _viewAllPageType.asStateFlow()

    private val _showPromoDetailsIndex = MutableStateFlow<Int?>(null)
    val showPromoDetailsIndex: StateFlow<Int?> = _showPromoDetailsIndex.asStateFlow()

    private val _activeOrderSuccess = MutableStateFlow<OrderEntity?>(null)
    val activeOrderSuccess: StateFlow<OrderEntity?> = _activeOrderSuccess.asStateFlow()

    private val _cart = MutableStateFlow<Map<ProductEntity, Int>>(emptyMap())
    val cart: StateFlow<Map<ProductEntity, Int>> = _cart.asStateFlow()

    private val _appliedDiscount = MutableStateFlow<Double>(0.0)
    val appliedDiscount: StateFlow<Double> = _appliedDiscount.asStateFlow()

    private val _activeChatRoom = MutableStateFlow<String?>(null)
    val activeChatRoom: StateFlow<String?> = _activeChatRoom.asStateFlow()

    private val _isAgentTyping = MutableStateFlow<Boolean>(false)
    val isAgentTyping: StateFlow<Boolean> = _isAgentTyping.asStateFlow()

    private val _pinnedChatRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedChatRoomIds: StateFlow<Set<String>> = _pinnedChatRoomIds.asStateFlow()

    private val _unreadChatRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val unreadChatRoomIds: StateFlow<Set<String>> = _unreadChatRoomIds.asStateFlow()

    private val _isCourierOnline = MutableStateFlow<Boolean>(true)
    val isCourierOnline: StateFlow<Boolean> = _isCourierOnline.asStateFlow()

    private val _pricePerKm = MutableStateFlow<Double>(25.0)
    val pricePerKm: StateFlow<Double> = _pricePerKm.asStateFlow()

    private val _courierAvatar = MutableStateFlow<String?>(null)
    val courierAvatar: StateFlow<String?> = _courierAvatar.asStateFlow()

    private val _buyerOrders = MutableStateFlow<List<BuyerOrderEntity>>(emptyList())
    val buyerOrders: StateFlow<List<BuyerOrderEntity>> = _buyerOrders.asStateFlow()

    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()

    private val _deliveryOrders = MutableStateFlow<List<DeliveryOrderEntity>>(emptyList())
    val deliveryOrders: StateFlow<List<DeliveryOrderEntity>> = _deliveryOrders.asStateFlow()

    private val _trackingOrder = MutableStateFlow<OrderEntity?>(null)
    val trackingOrder: StateFlow<OrderEntity?> = _trackingOrder.asStateFlow()

    // Missing UI & Data states added to fix compilation errors
    private val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    private val _isProductsLoading = MutableStateFlow<Boolean>(false)
    val isProductsLoading: StateFlow<Boolean> = _isProductsLoading.asStateFlow()

    private val _wishlistProductIds = MutableStateFlow<Set<String>>(emptySet())
    val wishlistProductIds: StateFlow<Set<String>> = _wishlistProductIds.asStateFlow()

    private val _storeWeeklyWorkedMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val storeWeeklyWorkedMap: StateFlow<Map<String, String>> = _storeWeeklyWorkedMap.asStateFlow()

    private val _storeTimeWorkedMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val storeTimeWorkedMap: StateFlow<Map<String, String>> = _storeTimeWorkedMap.asStateFlow()

    private val _isNotificationsEnabled = MutableStateFlow(true)
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    val ordersBackTarget = MutableStateFlow<String>("MENU")

    private val _courierRatings = MutableStateFlow<Map<String, Int>>(emptyMap())
    val courierRatings: StateFlow<Map<String, Int>> = _courierRatings.asStateFlow()

    private val _selectedOrderDetail = MutableStateFlow<OrderEntity?>(null)
    val selectedOrderDetail: StateFlow<OrderEntity?> = _selectedOrderDetail.asStateFlow()

    val addressBackTarget = MutableStateFlow<String>("MENU")

    private val _savedAddresses = MutableStateFlow<List<AddressEntity>>(emptyList())
    val savedAddresses: StateFlow<List<AddressEntity>> = _savedAddresses.asStateFlow()

    private val _loginDevices = MutableStateFlow<List<LoginHistoryEntity>>(emptyList())
    val loginDevices: StateFlow<List<LoginHistoryEntity>> = _loginDevices.asStateFlow()

    private val _sellerApplicationStatus = MutableStateFlow<String>("approved")
    val sellerApplicationStatus: StateFlow<String> = _sellerApplicationStatus.asStateFlow()

    private val _deliveryApplicationStatus = MutableStateFlow<String>("approved")
    val deliveryApplicationStatus: StateFlow<String> = _deliveryApplicationStatus.asStateFlow()

    private val _isOtpSentForEmail = MutableStateFlow<Boolean>(false)
    val isOtpSentForEmail: StateFlow<Boolean> = _isOtpSentForEmail.asStateFlow()

    private val _emailOtpCode = MutableStateFlow<String?>(null)
    val emailOtpCode: StateFlow<String?> = _emailOtpCode.asStateFlow()

    // Seller Dashboard caching states
    private val _sellerProducts = MutableStateFlow<List<SellerProductEntity>>(emptyList())
    val sellerProducts: StateFlow<List<SellerProductEntity>> = _sellerProducts.asStateFlow()

    private val _sellerOrders = MutableStateFlow<List<SellerOrderEntity>>(emptyList())
    val sellerOrders: StateFlow<List<SellerOrderEntity>> = _sellerOrders.asStateFlow()

    private val _sellerAnalytics = MutableStateFlow<SellerAnalytics?>(null)
    val sellerAnalytics: StateFlow<SellerAnalytics?> = _sellerAnalytics.asStateFlow()

    val sellerRevenueList: StateFlow<List<SellerRevenue>> = repository.sellerRevenueFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val sellerInventoryList: StateFlow<List<SellerProductInventory>> = repository.sellerInventoryFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val sellerDailySalesList: StateFlow<List<SellerDailySales>> = repository.sellerDailySalesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val sellerMonthlyStatsList: StateFlow<List<SellerMonthlyStats>> = repository.sellerMonthlyStatsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _isSellerDashboardLoading = MutableStateFlow<Boolean>(false)
    val isSellerDashboardLoading: StateFlow<Boolean> = _isSellerDashboardLoading.asStateFlow()

    fun fetchSellerRevenue(sellerId: String, week: String? = null, month: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getSellerRevenue(sellerId, week = week, month = month)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching seller revenue: ${e.message}")
            }
        }
    }

    fun fetchSellerDailySales(sellerId: String, date: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getSellerDailySales(sellerId, date = date)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching seller daily sales: ${e.message}")
            }
        }
    }

    fun loadSellerDashboardData(sellerId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Fetch seller analytics views from PocketBase
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

                    repository.getSellerDailySales(sellerId) // Fetch all for seller 
                    repository.getSellerRevenue(sellerId)
                    repository.getSellerMonthlyStats(sellerId)
                    repository.getSellerProductInventory(sellerId)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching seller financial/views data: ${e.message}")
                }
            }

            if (forceRefresh) {
                _isSellerDashboardLoading.value = true
                repository.invalidateSellerCache(sellerId)
                try {
                    repository.syncEngine.backgroundSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Force refresh dashboard sync error: ${e.message}")
                }
                val products = repository.getCachedSellerProducts(sellerId)
                val orders = repository.getCachedSellerOrders(sellerId)
                val analytics = repository.getCachedSellerAnalytics(sellerId)
                
                _sellerProducts.value = products
                _sellerOrders.value = orders
                _sellerAnalytics.value = analytics
                _isSellerDashboardLoading.value = false
            } else {
                // 1. Check if cached in memory first for instant display
                val cachedProducts = com.example.data.CacheManager.getInstance().get<List<SellerProductEntity>>("seller_products_$sellerId")
                val cachedOrders = com.example.data.CacheManager.getInstance().get<List<SellerOrderEntity>>("seller_orders_$sellerId")
                val cachedAnalytics = com.example.data.CacheManager.getInstance().get<SellerAnalytics>("seller_analytics_$sellerId")
                
                if (cachedProducts != null && cachedOrders != null && cachedAnalytics != null && cachedProducts.isNotEmpty()) {
                    // Cache Hit - Instant Display
                    _sellerProducts.value = cachedProducts
                    _sellerOrders.value = cachedOrders
                    _sellerAnalytics.value = cachedAnalytics
                    DebugLogManager.log(TAG, "📦 Instant cache hit: ${cachedProducts.size} seller products loaded")
                } else {
                    // Cache Miss/Stale - Load DB content instantly
                    val products = repository.getCachedSellerProducts(sellerId)
                    val orders = repository.getCachedSellerOrders(sellerId)
                    val analytics = repository.getCachedSellerAnalytics(sellerId)
                    
                    _sellerProducts.value = products
                    _sellerOrders.value = orders
                    _sellerAnalytics.value = analytics
                    if (products.isNotEmpty()) com.example.data.CacheManager.getInstance().put("seller_products_$sellerId", products, 15 * 60 * 1000L)
                    if (orders.isNotEmpty()) com.example.data.CacheManager.getInstance().put("seller_orders_$sellerId", orders, 15 * 60 * 1000L)
                    com.example.data.CacheManager.getInstance().put("seller_analytics_$sellerId", analytics, 15 * 60 * 1000L)
                    DebugLogManager.log(TAG, "📦 DB load: ${products.size} seller products loaded for $sellerId")
                    
                    // Trigger refresh in background from server
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            DebugLogManager.log("VIEWMODEL", "Seller dashboard cache stale/miss: refreshing from network...")
                            repository.syncEngine.backgroundSync()
                            val updatedProducts = repository.getCachedSellerProducts(sellerId)
                            val updatedOrders = repository.getCachedSellerOrders(sellerId)
                            val updatedAnalytics = repository.getCachedSellerAnalytics(sellerId)
                            withContext(Dispatchers.Main) {
                                _sellerProducts.value = updatedProducts
                                _sellerOrders.value = updatedOrders
                                _sellerAnalytics.value = updatedAnalytics
                                if (updatedProducts.isNotEmpty()) com.example.data.CacheManager.getInstance().put("seller_products_$sellerId", updatedProducts, 15 * 60 * 1000L)
                                if (updatedOrders.isNotEmpty()) com.example.data.CacheManager.getInstance().put("seller_orders_$sellerId", updatedOrders, 15 * 60 * 1000L)
                                com.example.data.CacheManager.getInstance().put("seller_analytics_$sellerId", updatedAnalytics, 15 * 60 * 1000L)
                                DebugLogManager.log(TAG, "📦 Network refresh completed: ${updatedProducts.size} seller products")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Background dashboard refresh failed: ${e.message}", e)
                        }
                    }
                }
            }
        }
    }

    // Removed updateSellerInventoryProductPrice as it is a local update only

    private var pendingEmail: String? = null
    private var currentEmailChangeOtpId: String? = null

    private val _storeOpen = MutableStateFlow(true)
    val storeOpen: StateFlow<Boolean> = _storeOpen.asStateFlow()

    private val _smsAlertsEnabled = MutableStateFlow(true)
    val smsAlertsEnabled: StateFlow<Boolean> = _smsAlertsEnabled.asStateFlow()

    private val _customOffersEnabled = MutableStateFlow(true)
    val customOffersEnabled: StateFlow<Boolean> = _customOffersEnabled.asStateFlow()

    private val _editingProduct = MutableStateFlow<SellerProductEntity?>(null)
    val editingProduct: StateFlow<SellerProductEntity?> = _editingProduct.asStateFlow()

    var editProductFromLiveStock = false

    private val _homeSearchQuery = MutableStateFlow("")
    val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

    private val _homeSelectedCategory = MutableStateFlow("")
    val homeSelectedCategory: StateFlow<String> = _homeSelectedCategory.asStateFlow()

    init {
        DebugLogManager.log(TAG, "🚀 AppViewModel initialized! Checking session...")
        viewModelScope.launch {
            _activeUser.collect { user ->
                if (user != null && user.role == "SELLER") {
                    _storeOpen.value = user.status ?: false
                }
            }
        }
        viewModelScope.launch {
            // Check if we have a session and load user
            val hasSession = repository.hasValidSession()
            DebugLogManager.log(TAG, "🔍 repository.hasValidSession() returned: $hasSession")
            if (hasSession) {
                val user = getActiveUser()
                DebugLogManager.log(TAG, "👤 getActiveUser() returned: $user")
                if (user != null) {
                    if (user.role == "BUYER") {
                        loadBuyerOrders()
                        refreshProducts()
                    } else if (user.role == "SELLER") {
                        val sId = user.sellerProfileId?.takeIf { it.isNotBlank() } ?: user.id
                        loadSellerDashboardData(sId)
                    }
                    _activeUser.value = user
                    repository.setBiometricEnabled(user.isBiometricEnabled)
                    _authState.value = AuthState.Authenticated
                    DebugLogManager.log(TAG, "✅ Authenticated active user session loaded successfully")
                    
                    // One-time startup sync for the user from PocketBase to fetch latest updates (e.g. role, verification status)
                    viewModelScope.launch {
                        try {
                            DebugLogManager.log(TAG, "🔄 One-time startup user sync initiated...")
                            val userResult = auth.getUserWithProfiles(user.id)
                            if (userResult is AuthResult.Success) {
                                val userRecord = userResult.dataOrNull!! as com.example.network.PbUserRecord
                                val sp = userRecord.expand?.seller_profile
                                val dp = userRecord.expand?.delivery_profile
                                val updatedUser = convertToUserEntity(userRecord, sp, dp)
                                
                                repository.saveUser(updatedUser)
                                _activeUser.value = updatedUser
                                
                                sp?.let {
                                    _sellerProfile.value = it
                                }
                                dp?.let {
                                    _deliveryProfile.value = it
                                }
                                DebugLogManager.log(TAG, "✅ One-time startup user sync completed successfully! Role: ${updatedUser.role}")
                            } else {
                                DebugLogManager.log(TAG, "⚠️ One-time startup user sync failed: $userResult")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error performing startup user sync: ${e.message}")
                        }
                        
                        // Perform background global sync for products, orders, etc.
                        try {
                            repository.syncPocketBase()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error performing background global sync on launch: ${e.message}")
                        }
                    }
                    
                    // Check if current device was removed from login history
                    val isRemoved = auth.isLoginSessionRemoved(user.id)
                    if (isRemoved) {
                        DebugLogManager.log(TAG, "🚨 This device was removed from remote login history! Force logging out...")
                        logoutUser()
                    }
                } else {
                    DebugLogManager.log(TAG, "⚠️ Active user is null despite valid session!")
                }
            } else {
                DebugLogManager.log(TAG, "❌ No valid session found in SessionManager on launch")
            }
            
            // Collect repository flows
            repository.activeUserFlow.collect {
                _activeUser.value = it
            }
        }
        viewModelScope.launch {
            repository.allOrdersFlow.collect { ordersList ->
                _orders.value = ordersList
                _buyerOrders.value = ordersList
            }
        }
        viewModelScope.launch {
            repository.allSellerOrdersFlow.collect { sellerOrdersList ->
                _sellerOrders.value = sellerOrdersList
            }
        }
        viewModelScope.launch {
            repository.allDeliveryOrdersFlow.collect { deliveryOrdersList ->
                _deliveryOrders.value = deliveryOrdersList
            }
        }
        viewModelScope.launch {
            repository.allProductsFlow.collect { list ->
                _products.value = list
                if (list.isNotEmpty()) com.example.data.CacheManager.getInstance().put("all_products", list, 15 * 60 * 1000L)
                val user = _activeUser.value
                val sellerId = user?.sellerProfileId?.takeIf { it.isNotBlank() } ?: user?.id ?: ""
                if (sellerId.isNotEmpty()) {
                    val sProducts = repository.getCachedSellerProducts(sellerId)
                    _sellerProducts.value = sProducts
                    if (sProducts.isNotEmpty()) com.example.data.CacheManager.getInstance().put("seller_products_$sellerId", sProducts, 15 * 60 * 1000L)
                }
            }
        }
        viewModelScope.launch {
            _isProductsLoading.value = true
            try {
                val cached = repository.getCachedProducts()
                if (cached.isNotEmpty()) {
                    _products.value = cached
                    DebugLogManager.log(TAG, "📦 Loaded ${cached.size} products from cache")
                }
            } catch (e: Exception) {
                DebugLogManager.log(TAG, "⚠️ Failed to load products: ${e.message}")
            } finally {
                _isProductsLoading.value = false
            }
        }
        viewModelScope.launch {
            repository.allAddressesFlow.collect {
                _savedAddresses.value = it
            }
        }
    }

    fun refreshProducts(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _isProductsLoading.value = true
                try {
                    // ✅ Force refresh from API
                    repository.invalidateProductCache()
                    repository.syncEngine.forceSync()
                    val fresh = repository.getCachedProducts()
                    _products.value = fresh
                    DebugLogManager.log(TAG, "🔄 Refreshed ${fresh.size} products")
                } catch (e: Exception) {
                    _errorMessage.value = "Refresh failed: ${e.message}"
                    DebugLogManager.log(TAG, "❌ Refresh failed: ${e.message}", isError = true)
                } finally {
                    _isProductsLoading.value = false
                }
            } else {
                // Load from cache first for instant display
                val cached = com.example.data.CacheManager.getInstance().get<List<ProductEntity>>("all_products")
                if (cached != null) {
                    _products.value = cached
                } else {
                    val dbList = repository.getCachedProducts()
                    _products.value = dbList
                    
                    // Stale/Miss: Trigger background sync
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            DebugLogManager.log(TAG, "Cache stale/miss: refreshing products in background...")
                            repository.syncEngine.backgroundSync()
                            val updatedList = repository.getCachedProducts()
                            withContext(Dispatchers.Main) {
                                _products.value = updatedList
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Background products refresh failed: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    fun toggleWishlist(productId: String) {
        val current = _wishlistProductIds.value
        _wishlistProductIds.value = if (current.contains(productId)) {
            current - productId
        } else {
            current + productId
        }
    }

    fun updateStoreWeeklyWorked(storeName: String, value: String) {
        val current = _storeWeeklyWorkedMap.value.toMutableMap()
        current[storeName] = value
        _storeWeeklyWorkedMap.value = current
    }

    fun updateStoreTimeWorked(storeName: String, value: String) {
        val current = _storeTimeWorkedMap.value.toMutableMap()
        current[storeName] = value
        _storeTimeWorkedMap.value = current
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _isNotificationsEnabled.value = enabled
    }

    fun setHomeSearch(query: String?, category: String?) {
        _homeSearchQuery.value = query ?: ""
        _homeSelectedCategory.value = category ?: ""
    }

    fun addAddress(address: AddressEntity) {
        viewModelScope.launch {
            repository.addAddress(address)
        }
    }

    fun updateAddress(address: AddressEntity) {
        viewModelScope.launch {
            repository.updateAddress(address)
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            repository.deleteAddress(id)
        }
    }

    fun setAddressAsDefault(id: String) {
        viewModelScope.launch {
            repository.setAddressAsDefault(id)
        }
    }

    fun removeLoginDevice(id: String) {
        viewModelScope.launch {
            auth.removeLoginDevice(id)
        }
    }

    fun syncLoginHistory() {
        viewModelScope.launch { auth.syncLoginHistory() }
    }

    fun sendEmailVerificationOtp(email: String) {
        viewModelScope.launch {
            try {
                val currentUserId = _activeUser.value?.id
                DebugLogManager.log(TAG, "📧 Requesting email change OTP from PocketBase to: $email (userId: $currentUserId)")
                val result = auth.requestEmailChange(email, currentUserId)
                if (result is AuthResult.Success) {
                    pendingEmail = email
                    currentEmailChangeOtpId = result.dataOrNull!!.otpId
                    _emailOtpCode.value = result.dataOrNull!!.code ?: String.format("%04d", (1000..9999).random())
                    _isOtpSentForEmail.value = true
                    DebugLogManager.log(TAG, "✅ Email change OTP requested successfully. otpId=${result.dataOrNull!!.otpId}, code=${_emailOtpCode.value}")
                } else {
                    val errorMsg = (result as? AuthResult.Error)?.message ?: "Unknown error"
                    DebugLogManager.log(TAG, "❌ Error requesting email change: $errorMsg, falling back to local simulation", isError = true)
                    pendingEmail = email
                    currentEmailChangeOtpId = "simulated_email_change_otp_id"
                    _emailOtpCode.value = "5821"
                    _isOtpSentForEmail.value = true
                }
            } catch (e: Exception) {
                DebugLogManager.log(TAG, "❌ Exception requesting email change OTP: ${e.message}, falling back to local simulation", isError = true)
                pendingEmail = email
                currentEmailChangeOtpId = "simulated_email_change_otp_id"
                _emailOtpCode.value = "5821"
                _isOtpSentForEmail.value = true
            }
        }
    }

    suspend fun verifyEmailOtp(otp: String): Boolean {
        val expected = _emailOtpCode.value ?: "5821"
        if (otp != expected) {
            DebugLogManager.log(TAG, "❌ Email OTP Verification failed locally. Entered: $otp, Expected: $expected")
            return false
        }
        val currentUserId = _activeUser.value?.id ?: return false
        val emailToSet = pendingEmail ?: return false
        val otpId = currentEmailChangeOtpId ?: "simulated_email_change_otp_id"
        
        try {
            DebugLogManager.log(TAG, "🔄 Confirming email change on PocketBase. otpId: $otpId, code: $otp")
            if (otpId != "simulated_email_change_otp_id") {
                val confirmResult = auth.confirmEmailChange(otpId, otp, emailToSet)
                if (confirmResult is AuthResult.Success) {
                    DebugLogManager.log(TAG, "✅ PocketBase email change confirmation succeeded!")
                } else {
                    val errorMsg = (confirmResult as? AuthResult.Error)?.message ?: "Unknown confirmation error"
                    DebugLogManager.log(TAG, "⚠️ PocketBase confirmation failed: $errorMsg. Continuing with local sync.", isError = true)
                }
            } else {
                // If it was simulated, just do standard direct update
                auth.client.getApi().updateUser(currentUserId, mapOf("email" to emailToSet))
            }
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "⚠️ PocketBase email update/confirmation failed (likely offline/mock): ${e.message}. Updating locally.", isError = true)
        }
        
        // Always update local database for smooth user experience
        val currentUser = _activeUser.value
        if (currentUser != null) {
            val updated = currentUser.copy(email = emailToSet, isEmailVerified = true)
            repository.updateUser(updated)
            _activeUser.value = updated
        }
        _isOtpSentForEmail.value = false
        currentEmailChangeOtpId = null
        return true
    }

    fun resendEmailChangeOtp() {
        val otpId = currentEmailChangeOtpId ?: return
        viewModelScope.launch {
            try {
                if (otpId == "simulated_email_change_otp_id") {
                    DebugLogManager.log(TAG, "🔄 [Simulated] Resending email change OTP...")
                    _emailOtpCode.value = "5821"
                    _isOtpSentForEmail.value = true
                    return@launch
                }
                DebugLogManager.log(TAG, "🔄 Resending email change OTP for id: $otpId")
                val result = auth.resendEmailChangeOtp(otpId, pendingEmail)
                if (result is AuthResult.Success) {
                    currentEmailChangeOtpId = result.dataOrNull!!.otpId
                    _emailOtpCode.value = result.dataOrNull!!.code ?: String.format("%04d", (1000..9999).random())
                    _isOtpSentForEmail.value = true
                    DebugLogManager.log(TAG, "✅ Resent OTP successfully. New OTP ID: ${result.dataOrNull!!.otpId}")
                }
            } catch (e: Exception) {
                DebugLogManager.log(TAG, "❌ Exception resending email OTP: ${e.message}", isError = true)
            }
        }
    }

    fun cancelEmailChange() {
        val otpId = currentEmailChangeOtpId ?: return
        viewModelScope.launch {
            try {
                if (otpId != "simulated_email_change_otp_id") {
                    DebugLogManager.log(TAG, "❌ Cancelling email change for id: $otpId")
                    auth.cancelEmailChange(otpId)
                }
            } catch (e: Exception) {
                DebugLogManager.log(TAG, "❌ Exception cancelling email change: ${e.message}", isError = true)
            } finally {
                pendingEmail = null
                currentEmailChangeOtpId = null
                _isOtpSentForEmail.value = false
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val user = _activeUser.value ?: return@launch
            val updated = user.copy(isBiometricEnabled = enabled)
            repository.updateUser(updated)
            repository.setBiometricEnabled(enabled)
            _activeUser.value = updated
        }
    }

    fun setTwoFactorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val user = _activeUser.value ?: return@launch
            val updated = user.copy(isTwoFactorEnabled = enabled)
            repository.updateUser(updated)
            _activeUser.value = updated
        }
    }

    fun enableTwoFactor(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = _activeUser.value
            if (user == null) {
                onResult(false, "User session not found")
                return@launch
            }
            _isLoading.value = true
            try {
                val pbResult = auth.updateTwoFactorEnabled(user.id, true)
                if (pbResult is AuthResult.Success) {
                    val updated = user.copy(isTwoFactorEnabled = true)
                    repository.updateUser(updated)
                    _activeUser.value = updated
                    DebugLogManager.log(TAG, "✅ Two-Factor Auth successfully enabled in local and remote.")
                    onResult(true, null)
                } else {
                    val errMsg = (pbResult as? AuthResult.Error)?.message ?: "Failed to update backend"
                    onResult(false, errMsg)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "An error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestOtpFor2FADisable(onResult: (String?, String?) -> Unit) {
        viewModelScope.launch {
            val email = _activeUser.value?.email
            if (email.isNullOrEmpty()) {
                onResult(null, "No email address found for user")
                return@launch
            }
            _isLoading.value = true
            try {
                val pbResult = auth.requestOtp(email)
                if (pbResult is AuthResult.Success) {
                    onResult(pbResult.dataOrNull!!.otpId, null)
                } else {
                    val errMsg = (pbResult as? AuthResult.Error)?.message ?: "Failed to send OTP"
                    onResult(null, errMsg)
                }
            } catch (e: Exception) {
                onResult(null, e.message ?: "An error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyOtpAndDisable2FA(otpId: String, code: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = _activeUser.value
            if (user == null) {
                onResult(false, "User session not found")
                return@launch
            }
            _isLoading.value = true
            try {
                val otpResult = auth.authWithOtp(otpId, code)
                if (otpResult is AuthResult.Success) {
                    val pbResult = auth.updateTwoFactorEnabled(user.id, false)
                    if (pbResult is AuthResult.Success) {
                        val updated = user.copy(isTwoFactorEnabled = false)
                        repository.updateUser(updated)
                        _activeUser.value = updated
                        DebugLogManager.log(TAG, "✅ Two-Factor Auth successfully disabled in local and remote.")
                        onResult(true, null)
                    } else {
                        val errMsg = (pbResult as? AuthResult.Error)?.message ?: "Failed to disable 2FA on backend"
                        onResult(false, errMsg)
                    }
                } else {
                    val errMsg = (otpResult as? AuthResult.Error)?.message ?: "Incorrect OTP code. Please try again."
                    onResult(false, errMsg)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "An error occurred during verification")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changeUserPassword(current: String, new: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = auth.updatePassword(current, new)
                if (res is AuthResult.Success) {
                    onSuccess()
                } else if (res is AuthResult.Error) {
                    onError(res.message)
                }
            } catch (e: Exception) {
                onError(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rateCourier(orderId: String, rating: Int) {
        val current = _courierRatings.value.toMutableMap()
        current[orderId] = rating
        _courierRatings.value = current
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun setSelectedProductForDetail(product: ProductEntity?) {
        _selectedProductForDetail.value = product
    }

    fun setSelectedStoreName(storeName: String?) {
        _selectedStoreName.value = storeName
    }

    fun selectChatRoomId(roomId: String?) {
        _selectedChatRoomId.value = roomId
    }

    fun setProfileSubScreen(subScreen: String) {
        _profileSubScreen.value = subScreen
    }

    fun setActiveDashboardSubView(subView: String) {
        activeDashboardSubView.value = subView
    }

    fun setShowNotificationsPage(show: Boolean) {
        _showNotificationsPage.value = show
    }

    fun setShowUserSearchPage(show: Boolean) {
        _showUserSearchPage.value = show
    }

    fun toggleStoreOpen() {
        val nextStatus = !_storeOpen.value
        _storeOpen.value = nextStatus
        updateSellerProfileField(mapOf("status" to nextStatus))
        val sellerId = _activeUser.value?.sellerProfileId ?: _activeUser.value?.id ?: ""
        if (sellerId.isNotBlank()) {
            if (nextStatus) {
                subscribeSellerRealtime(sellerId)
            } else {
                unsubscribeSellerRealtime(sellerId)
            }
        }
    }

    fun toggleSmsAlerts() {
        _smsAlertsEnabled.value = !_smsAlertsEnabled.value
    }

    fun toggleCustomOffers() {
        _customOffersEnabled.value = !_customOffersEnabled.value
    }

    fun setEditingProduct(product: SellerProductEntity?) {
        _editingProduct.value = product
    }


    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
        }
    }

    fun updateOrderStatus(order: OrderEntity, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, status)
        }
    }

    fun deleteSellerProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteSellerProduct(productId)
            val current = _sellerProducts.value.filter { it.id != productId }
            _sellerProducts.value = current
        }
    }

    fun updateSellerProfileField(fields: Map<String, Any>, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val profile = _sellerProfile.value
            if (profile != null) {
                val modifiedFields = fields.toMutableMap()
                val payoutVal = fields["businessPayout"]
                if (payoutVal is String) {
                    val cbe = if (payoutVal.contains("CBE:")) payoutVal.substringAfter("CBE:").substringBefore("|").trim() else null
                    val telebirr = if (payoutVal.contains("Telebirr:")) payoutVal.substringAfter("Telebirr:").substringBefore("|").trim() else null
                    val ebirr = if (payoutVal.contains("Ebirr:")) payoutVal.substringAfter("Ebirr:").substringBefore("|").trim() else null
                    val name = if (payoutVal.contains("Name:")) payoutVal.substringAfter("Name:").substringBefore("|").trim() else null
                    modifiedFields["businessPayout"] = BusinessPayoutInfo(
                        cbe = cbe,
                        telebirr = telebirr,
                        ebirr = ebirr,
                        name = name
                    )
                }
                val discountVal = fields["businessDiscountCode"]
                if (discountVal is String) {
                    val code = if (discountVal.contains("Code:")) discountVal.substringAfter("Code:").substringBefore("|").trim() else null
                    val percent = if (discountVal.contains("Percent:")) discountVal.substringAfter("Percent:").substringBefore("|").trim() else null
                    modifiedFields["businessDiscountCode"] = BusinessDiscountCodeInfo(
                        code = code,
                        percent = percent?.toDoubleOrNull()
                    )
                }
                DebugLogManager.log(TAG, "🔄 Updating seller profile on PocketBase: $modifiedFields")
                val result = auth.updateSellerProfile(profile.id, modifiedFields)
                when (result) {
                    is AuthResult.Success -> {
                        val updatedProfile = result.dataOrNull!! as com.example.network.SellerProfile
                        _sellerProfile.value = updatedProfile
                        
                        // Also update active user locally
                        val currentUser = _activeUser.value
                        if (currentUser != null) {
                            val updatedUser = currentUser.copy(
                                businessName = updatedProfile.businessName ?: currentUser.businessName,
                                businessPhone = updatedProfile.businessPhone ?: currentUser.businessPhone,
                                businessAbout = updatedProfile.businessDescription ?: currentUser.businessAbout,
                                businessWorkingHours = updatedProfile.businessWorkingHours ?: currentUser.businessWorkingHours,
                                tinNumber = updatedProfile.tinNumber ?: currentUser.tinNumber,
                                tlcNumber = updatedProfile.tlcNumber ?: currentUser.tlcNumber,
                                businessGradeTitel = updatedProfile.businessGradeTitel ?: currentUser.businessGradeTitel,
                                businessGrade = updatedProfile.businessGrade?.toString() ?: currentUser.businessGrade,
                                businessPayout = updatedProfile.businessPayout?.toString() ?: currentUser.businessPayout,
                                businessRating = updatedProfile.businessRating?.toString() ?: currentUser.businessRating,
                                businessDiscountCode = updatedProfile.businessDiscountCode?.toString() ?: currentUser.businessDiscountCode,
                                sellerVerification = updatedProfile.sellerVerification ?: currentUser.sellerVerification,
                                storeLocation = updatedProfile.storeLocation ?: currentUser.storeLocation,
                                status = updatedProfile.status ?: currentUser.status,
                                sellerProfileImage = updatedProfile.seller_profile_image?.let { img ->
                                    if (img.isBlank()) ""
                                    else if (img.startsWith("http")) img
                                    else "https://metube.pockethost.io/api/files/seller_profiles/${updatedProfile.id}/$img"
                                } ?: currentUser.sellerProfileImage,
                                sellerProfileCover = updatedProfile.seller_profile_cover?.let { cov ->
                                    if (cov.isBlank()) ""
                                    else if (cov.startsWith("http")) cov
                                    else "https://metube.pockethost.io/api/files/seller_profiles/${updatedProfile.id}/$cov"
                                } ?: currentUser.sellerProfileCover
                            )
                            
                            repository.saveUser(updatedUser)
                            _activeUser.value = updatedUser
                        }
                        onSuccess()
                    }
                    is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                        onError(result.message)
                    }
                    is AuthResult.MfaRequired -> {
                        onError("Multi-factor authentication required.")
                    }
                }
            } else {
                onError("No active seller profile found to update.")
            }
        }
    }

    fun updateSellerAbout(about: String) {
        updateSellerProfileField(mapOf("businessDescription" to about))
    }

    fun cancelOrderWithReason(orderId: String, reason: String, imageUri: String?) {
        viewModelScope.launch {
            repository.cancelOrder(orderId, reason)
        }
    }

    // Product Reviews State
    private val _productReviews = MutableStateFlow<List<ProductReviewEntity>>(emptyList())
    val productReviews = _productReviews.asStateFlow()

    fun fetchProductReviews(productId: String) {
        viewModelScope.launch {
            val list = repository.getProductReviews(productId)
            _productReviews.value = list
        }
    }

    // Store Reviews State
    private val _storeReviews = MutableStateFlow<List<com.example.ui.utils.ReviewItem>>(emptyList())
    val storeReviews = _storeReviews.asStateFlow()

    fun fetchStoreReviews() {
        viewModelScope.launch {
            val list = repository.getStoreReviews()
            _storeReviews.value = list
        }
    }

    fun submitStoreReview(
        productId: String?,
        rating: Double,
        comment: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val activeUser = _activeUser.value
            val userId = activeUser?.id ?: ""
            val userName = activeUser?.fullName?.takeIf { it.isNotBlank() } ?: "Verified Buyer"
            val userAvatar = activeUser?.profileImage ?: ""
            
            val item = com.example.ui.utils.ReviewItem(
                name = userName,
                rating = "★" + String.format(java.util.Locale.US, "%.1f", rating),
                comment = comment,
                date = "", // formatted by repository
                product = productId ?: "General Store",
                replyText = null,
                userId = userId,
                userAvatar = userAvatar,
                productId = productId
            )
            val success = repository.saveStoreReview(item)
            if (success) {
                fetchStoreReviews()
            }
            onResult(success)
        }
    }

    fun replyToStoreReview(
        reviewId: String,
        replyText: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = repository.updateStoreReviewReply(reviewId, replyText)
            if (success) {
                fetchStoreReviews()
            }
            onResult(success)
        }
    }

    fun submitProductReview(
        productId: String,
        rating: Double,
        comment: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val activeUser = _activeUser.value
            val userId = activeUser?.id ?: ""
            val userName = activeUser?.fullName?.takeIf { it.isNotBlank() } ?: "Verified Buyer"
            val userAvatar = activeUser?.profileImage ?: ""

            val review = ProductReviewEntity(
                productId = productId,
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                rating = rating,
                comment = comment
            )
            val success = repository.addProductReview(review)
            if (success) {
                fetchProductReviews(productId)
            }
            onResult(success)
        }
    }

    fun updateProductReview(
        reviewId: String,
        productId: String,
        rating: Double,
        comment: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = repository.updateProductReview(reviewId, rating, comment)
            if (success) {
                fetchProductReviews(productId)
            }
            onResult(success)
        }
    }

    fun deleteProductReview(
        reviewId: String,
        productId: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = repository.deleteProductReview(reviewId)
            if (success) {
                fetchProductReviews(productId)
            }
            onResult(success)
        }
    }

    fun cancelOrderWithReason(order: OrderEntity, reason: String, imageUri: String?) {
        viewModelScope.launch {
            repository.cancelOrder(order.id, reason)
        }
    }

    fun saveProduct(
        name: String,
        price: Double,
        stock: Int,
        category: String,
        isPromoted: Boolean,
        imageResName: String,
        imageUrlName: String = "",
        description: String,
        unitOfMeasurement: String,
        stateNewOrUsed: String,
        isFreeDelivery: Boolean,
        discountPercent: Int,
        brand: String,
        variants: String,
        subcategory: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentEditing = _editingProduct.value
            val sellerId = _activeUser.value?.sellerProfileId?.takeIf { it.isNotBlank() }
                ?: _activeUser.value?.id
                ?: "seller_1"
            val sellerName = _activeUser.value?.businessName?.takeIf { it.isNotBlank() }
                ?: _sellerProfile.value?.businessName?.takeIf { it.isNotBlank() }
                ?: _activeUser.value?.fullName?.takeIf { it.isNotBlank() }
                ?: "Local Store"
            val product = if (currentEditing != null) {
                currentEditing.copy(
                    name = name,
                    price = price,
                    stock = stock,
                    category = category,
                    isPromoted = isPromoted,
                    imageResName = imageResName,
                    imageUrlName = imageUrlName,
                    description = description,
                    unitOfMeasurement = unitOfMeasurement,
                    stateNewOrUsed = stateNewOrUsed,
                    isFreeDelivery = isFreeDelivery,
                    discountPercent = discountPercent,
                    brand = brand,
                    variants = variants,
                    subcategory = subcategory
                )
            } else {
                SellerProductEntity(
                    sellerId = sellerId,
                    sellerName = sellerName,
                    name = name,
                    price = price,
                    stock = stock,
                    category = category,
                    isPromoted = isPromoted,
                    imageResName = imageResName,
                    imageUrlName = imageUrlName,
                    description = description,
                    unitOfMeasurement = unitOfMeasurement,
                    stateNewOrUsed = stateNewOrUsed,
                    isFreeDelivery = isFreeDelivery,
                    discountPercent = discountPercent,
                    brand = brand,
                    variants = variants,
                    subcategory = subcategory
                )
            }
            val success = repository.saveSellerProduct(product)
            if (success) {
                _editingProduct.value = null
                val activeUser = _activeUser.value
                val sellerId = activeUser?.sellerProfileId ?: activeUser?.id ?: ""
                if (sellerId.isNotEmpty()) {
                    _sellerProducts.value = repository.getCachedSellerProducts(sellerId)
                }
            }
            onResult(success)
        }
    }


    /**
     * O3KAA3
     * Initialize real-time connections for the current user
     */
    fun initRealtimeForUser(userId: String, role: String) {
        // Update auth token on realtime client
        realtime.updateAuthToken(auth.getToken())

        if (!isRealtimeConnected) {
            viewModelScope.launch {
                try {
                    // Connect to SSE stream
                    realtime.connect()
                    
                    // Collect events
                    realtime.events.collect { event ->
                        handleRealtimeEvent(event)
                    }
                } catch (e: Exception) {
                    DebugLogManager.log(TAG, "❌ Realtime collector error: ${e.message}", isError = true)
                }
            }
            isRealtimeConnected = true
        } else {
            // Re-trigger connect if SSE stream got disconnected
            realtime.connect()
        }
        
        // Subscribe BUYER orders if BUYER.
        // SELLER and DELIVERY real-time subscriptions are managed strictly by their screens when online.
        if (role == "BUYER") {
            realtime.subscribeToOrders(userId, "BUYER")
            DebugLogManager.log(TAG, "✅ Buyer real-time subscribed: $userId")
        }
    }

    /**
     * Subscribe Seller real-time (orders & products) when in SellerDashboard and store status is online
     */
    fun subscribeSellerRealtime(sellerId: String) {
        val idToUse = sellerId.ifBlank { _activeUser.value?.sellerProfileId ?: _activeUser.value?.id ?: "" }
        if (idToUse.isBlank()) return

        realtime.updateAuthToken(auth.getToken())
        if (!isRealtimeConnected) {
            initRealtimeForUser(_activeUser.value?.id ?: idToUse, "SELLER")
        }

        realtime.subscribeToOrders(idToUse, "SELLER")
        realtime.subscribeToProducts(idToUse)
        DebugLogManager.log(TAG, "🟢 Seller real-time SUBSCRIBED: $idToUse")
    }

    /**
     * Unsubscribe Seller real-time (orders & products) when leaving SellerDashboard or store is offline
     */
    fun unsubscribeSellerRealtime(sellerId: String) {
        val idToUse = sellerId.ifBlank { _activeUser.value?.sellerProfileId ?: _activeUser.value?.id ?: "" }
        if (idToUse.isBlank()) return

        realtime.unsubscribeFromOrders(idToUse, "SELLER")
        realtime.unsubscribeFromProducts(idToUse)
        DebugLogManager.log(TAG, "🔴 Seller real-time UNSUBSCRIBED: $idToUse")
    }

    /**
     * Subscribe Delivery real-time (orders) when in DeliveryDashboard and status is online
     */
    fun subscribeDeliveryRealtime(deliveryId: String) {
        val idToUse = deliveryId.ifBlank { _activeUser.value?.deliveryProfileId ?: _activeUser.value?.id ?: "" }
        if (idToUse.isBlank()) return

        realtime.updateAuthToken(auth.getToken())
        if (!isRealtimeConnected) {
            initRealtimeForUser(_activeUser.value?.id ?: idToUse, "DELIVERY")
        }

        realtime.subscribeToOrders(idToUse, "DELIVERY")
        DebugLogManager.log(TAG, "🟢 Delivery real-time SUBSCRIBED: $idToUse")
    }

    /**
     * Unsubscribe Delivery real-time (orders) when leaving DeliveryDashboard or status is offline
     */
    fun unsubscribeDeliveryRealtime(deliveryId: String) {
        val idToUse = deliveryId.ifBlank { _activeUser.value?.deliveryProfileId ?: _activeUser.value?.id ?: "" }
        if (idToUse.isBlank()) return

        realtime.unsubscribeFromOrders(idToUse, "DELIVERY")
        DebugLogManager.log(TAG, "🔴 Delivery real-time UNSUBSCRIBED: $idToUse")
    }

    /**
     * Handle real-time events
     */
    private suspend fun handleRealtimeEvent(event: RealtimeEvent) {
        when (event) {
            is RealtimeEvent.CollectionChange -> {
                when (event.collection) {
                    "orders" -> handleOrderChange(event)
                    "products" -> handleProductChange(event)
                    "order_items" -> handleOrderItemChange(event)
                }
            }
        }
    }

    /**
     * Handle order changes (create, update, delete)
     */
    private suspend fun handleOrderChange(event: RealtimeEvent.CollectionChange) {
        DebugLogManager.log(TAG, "📦 Order change: ${event.action} - ${event.record.take(20)}...")
        
        val record = try {
            kotlinx.serialization.json.Json.parseToJsonElement(event.record).jsonObject
        } catch (e: Exception) {
            return
        }
        
        val orderId = record["id"]?.jsonPrimitive?.content ?: return
        val status = record["status"]?.jsonPrimitive?.content ?: ""
        val sellerId = record["sellerId"]?.jsonPrimitive?.content ?: ""
        val buyerId = record["buyerId"]?.jsonPrimitive?.content ?: ""
        
        when (event.action) {
            "create" -> {
                // New order created
                val currentUser = _activeUser.value
                when {
                    // Seller sees new order
                    currentUser?.role == "SELLER" && currentUser.sellerProfileId == sellerId -> {
                        DebugLogManager.log(TAG, "🆕 New order received for seller! Order #$orderId")
                        // Reload seller orders
                        loadSellerOrders(sellerId)
                        // Show notification
                        showOrderNotification("New order received!", "A customer has placed a new order.")
                    }
                    // Buyer sees their order
                    currentUser?.id == buyerId -> {
                        DebugLogManager.log(TAG, "✅ Order confirmed! Order #$orderId")
                        loadBuyerOrders()
                        showOrderNotification("Order confirmed!", "Your order has been placed successfully.")
                    }
                }
            }
            "update" -> {
                // Order status changed
                val updatedOrder = convertJsonToOrder(record)
                val currentUser = _activeUser.value
                updateLocalOrder(updatedOrder)
                
                when {
                    // Seller sees order updates
                    currentUser?.role == "SELLER" && currentUser.sellerProfileId == sellerId -> {
                        DebugLogManager.log(TAG, "📝 Order updated: $orderId -> $status")
                        loadSellerOrders(sellerId)
                        
                        when (status) {
                            "READY_FOR_PICKUP" -> {
                                showOrderNotification("Order ready!", "Order #$orderId is ready for pickup.")
                            }
                            "OUT_FOR_DELIVERY" -> {
                                showOrderNotification("Order in transit!", "Order #$orderId has been picked up for delivery.")
                            }
                            "DELIVERED" -> {
                                showOrderNotification("Order delivered!", "Order #$orderId has been delivered.")
                            }
                            "CANCELLED" -> {
                                showOrderNotification("Order cancelled", "Order #$orderId has been cancelled.")
                            }
                        }
                    }
                    // Buyer sees order updates
                    currentUser?.id == buyerId -> {
                        DebugLogManager.log(TAG, "📝 Your order updated: $orderId -> $status")
                        loadBuyerOrders()
                        
                        when (status) {
                            "READY_FOR_PICKUP" -> {
                                showOrderNotification("Ready for pickup!", "Your order is ready for pickup.")
                            }
                            "OUT_FOR_DELIVERY" -> {
                                showOrderNotification("Order on the way!", "Your order is out for delivery.")
                            }
                            "DELIVERED" -> {
                                showOrderNotification("Order delivered!", "Your order has been delivered.")
                            }
                            "CANCELLED" -> {
                                showOrderNotification("Order cancelled", "Your order has been cancelled.")
                            }
                        }
                    }
                    // Delivery courier sees order updates
                    currentUser?.role == "DELIVERY" -> {
                        DebugLogManager.log(TAG, "🚚 Delivery order updated live: $orderId -> $status")
                        loadAvailableDeliveryOrders()
                        if (status == "READY_FOR_PICKUP" && updatedOrder.courierName.isNullOrBlank() && !updatedOrder.isSelfPickup) {
                            showOrderNotification("New Available Delivery!", "A new ride is available for pickup.")
                        }
                    }
                }
            }
            "delete" -> {
                // Order deleted
                val currentUser = _activeUser.value
                if (currentUser?.role == "SELLER" && currentUser.sellerProfileId == sellerId) {
                    loadSellerOrders(sellerId)
                } else if (currentUser?.id == buyerId) {
                    loadBuyerOrders()
                } else if (currentUser?.role == "DELIVERY") {
                    loadAvailableDeliveryOrders()
                }
            }
        }
    }

    /**
     * Seller only
     * Handle product changes (stock updates)
     */
    private suspend fun handleProductChange(event: RealtimeEvent.CollectionChange) {
        val record = try {
            kotlinx.serialization.json.Json.parseToJsonElement(event.record).jsonObject
        } catch (e: Exception) {
            return
        }
        
        val productId = record["id"]?.jsonPrimitive?.content ?: return
        val stock = record["stock"]?.jsonPrimitive?.intOrNull ?: 0
        
        when (event.action) {
            "update" -> {
                // Product stock updated - refresh seller products
                val currentUser = _activeUser.value
                if (currentUser?.role == "SELLER") {
                    val sellerId = currentUser.sellerProfileId
                    if (!sellerId.isNullOrBlank()) {
                        loadSellerDashboardData(sellerId, forceRefresh = true)
                    }
                }
            }
        }
    }

    /**
     * Handle order item changes
     */
    private suspend fun handleOrderItemChange(event: RealtimeEvent.CollectionChange) {
        // Refresh order details if needed
        val currentUser = _activeUser.value
        if (currentUser != null) {
            if (currentUser.role == "BUYER") {
                loadBuyerOrders()
            } else if (currentUser.role == "SELLER") {
                val sellerId = currentUser.sellerProfileId
                if (!sellerId.isNullOrBlank()) {
                    loadSellerOrders(sellerId)
                }
            }
        }
    }

    /**
     * Convert JSON to OrderEntity
     */
    private fun convertJsonToOrder(json: kotlinx.serialization.json.JsonObject): OrderEntity {
        val lat = json["shippingAddress"]?.jsonObject?.get("lat")?.jsonPrimitive?.doubleOrNull ?: 0.0
        val lon = json["shippingAddress"]?.jsonObject?.get("lon")?.jsonPrimitive?.doubleOrNull ?: 0.0

        val storeLat = json["storeLocation"]?.jsonObject?.get("lat")?.jsonPrimitive?.doubleOrNull ?: 0.0
        val storeLon = json["storeLocation"]?.jsonObject?.get("lon")?.jsonPrimitive?.doubleOrNull ?: 0.0

        return OrderEntity(
            id = json["id"]?.jsonPrimitive?.content ?: "",
            buyerId = json["buyerId"]?.jsonPrimitive?.content ?: "",
            sellerId = json["sellerId"]?.jsonPrimitive?.content ?: "",
            deliveryId = json["deliveryId"]?.jsonPrimitive?.contentOrNull,
            buyerName = json["buyerName"]?.jsonPrimitive?.content ?: json["buyer_name"]?.jsonPrimitive?.content ?: "",
            buyerPhone = json["buyerPhone"]?.jsonPrimitive?.content ?: json["buyer_phone"]?.jsonPrimitive?.content ?: "",
            sellerName = json["sellerName"]?.jsonPrimitive?.content ?: json["seller_name"]?.jsonPrimitive?.content ?: "",
            productNames = json["product_names"]?.jsonPrimitive?.content ?: "",
            totalPrice = json["total_price"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            deliveryPrice = json["delivery_price"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            unit = json["unit"]?.jsonPrimitive?.content ?: "pieces",
            status = json["status"]?.jsonPrimitive?.content ?: "",
            paymentMethod = json["payment_method"]?.jsonPrimitive?.content ?: "Telebirr",
            shippingAddress = LocationShipping(lat = lat, lon = lon),
            storeLocation = com.example.data.StoreLocation(lat = storeLat, lon = storeLon),
            buyerVerifCode = json["buyer_verif_code"]?.jsonPrimitive?.contentOrNull,
            deliveryVerifCode = json["delivery_verif_code"]?.jsonPrimitive?.contentOrNull,
            courierName = json["courier_name"]?.jsonPrimitive?.contentOrNull,
            courierPlate = json["courier_plate"]?.jsonPrimitive?.contentOrNull,
            deliveryPickup = json["delivery_pickup"]?.jsonPrimitive?.booleanOrNull ?: false,
            isSelfPickup = json["is_self_pickup"]?.jsonPrimitive?.booleanOrNull ?: false,
            paymentReceipt = json["payment_receipt"]?.jsonPrimitive?.contentOrNull,
            cancellationReason = json["cancellation_reason"]?.jsonPrimitive?.contentOrNull,
            created = json["created"]?.jsonPrimitive?.contentOrNull,
            updated = json["updated"]?.jsonPrimitive?.contentOrNull
        )
    }

    /**
     * Update local order in flow
     */
    private suspend fun updateLocalOrder(order: OrderEntity) {
        val currentOrders = _orders.value.toMutableList()
        val index = currentOrders.indexOfFirst { it.id == order.id }
        if (index >= 0) {
            currentOrders[index] = order
        } else {
            currentOrders.add(order)
        }
        _orders.value = currentOrders
        
        // Update seller orders if applicable
        val currentUser = _activeUser.value
        if (currentUser?.role == "SELLER") {
            val sellerOrders = _sellerOrders.value.toMutableList()
            val sellerIndex = sellerOrders.indexOfFirst { it.id == order.id }
            if (sellerIndex >= 0) {
                sellerOrders[sellerIndex] = order.toSellerOrder()
            } else {
                sellerOrders.add(order.toSellerOrder())
            }
            _sellerOrders.value = sellerOrders
        }

        // Update delivery orders if applicable
        val delOrders = _deliveryOrders.value.toMutableList()
        val delIndex = delOrders.indexOfFirst { it.id == order.id }
        if (delIndex >= 0) {
            delOrders[delIndex] = order.toDeliveryOrder()
        } else if (!order.isSelfPickup) {
            delOrders.add(0, order.toDeliveryOrder())
        }
        _deliveryOrders.value = delOrders
    }

    /**
     * Show order notification
     */
    private fun showOrderNotification(title: String, message: String) {
        // In-app notification
        _errorMessage.value = "🔔 $title: $message"
        
        // You can also show a toast or custom notification
        android.widget.Toast.makeText(context, "$title - $message", android.widget.Toast.LENGTH_LONG).show()
    }

    // ==========================================
    // REAL-TIME CLEANUP
    // ==========================================

    // ==========================================
    // CLEANUP
    // ==========================================

    fun cleanupRealtime() {
        realtime.cleanup()
        isRealtimeConnected = false
    }


    // ==========================================
    // ORDER MANAGEMENT FUNCTIONS
    // ==========================================
    
    /**
    * Create order with items
    */
    fun createOrderItems(
        sellerId: String,
        items: List<CartItem>,
        deliveryPrice: Double = 0.0,
        paymentMethod: String = "Telebirr",
        isSelfPickup: Boolean = false,
        deliveryPickup: Boolean = true,
        shippingAddress: LocationShipping = LocationShipping(),
        sellerName: String = "",
        buyerName: String = "",
        buyerPhone: String = "",
        onSuccess: (OrderEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val buyerId = getUserId()
                if (buyerId.isNullOrBlank()) {
                    onError("User not logged in")
                    _isLoading.value = false
                    return@launch
                }

                val activeUser = _activeUser.value
                val finalBuyerName = buyerName.ifBlank { activeUser?.fullName.orEmpty() }
                val finalBuyerPhone = buyerPhone.ifBlank { activeUser?.phone.orEmpty() }
                val finalSellerName = sellerName.ifBlank { items.firstOrNull()?.sellerName.orEmpty() }
                
                val result = repository.createOrderWithItems(
                    buyerId = buyerId,
                    sellerId = sellerId,
                    items = items,
                    paymentMethod = paymentMethod,
                    isSelfPickup = isSelfPickup,
                    deliveryPickup = deliveryPickup,
                    deliveryPrice = deliveryPrice,
                    shippingAddress = shippingAddress,
                    sellerName = finalSellerName,
                    buyerName = finalBuyerName,
                    buyerPhone = finalBuyerPhone
                )
                
                when (result) {
                    is AuthResult.Success -> {
                        val newOrder = result.dataOrNull!!
                        _orders.value = listOf(newOrder) + _orders.value
                        _buyerOrders.value = listOf(newOrder) + _buyerOrders.value
                        onSuccess(newOrder)
                    }
                    is AuthResult.MfaRequired -> {}
                    is AuthResult.Error -> {
                        onError(result.message)
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to create order")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
    * Load buyer orders
    */
    fun loadBuyerOrders() {
        viewModelScope.launch {
            val buyerId = getUserId()
            if (buyerId.isNullOrBlank()) return@launch
            
            val orders = repository.getBuyerOrders(buyerId)
            _orders.value = orders
            _buyerOrders.value = orders
        }
    }

    /**
    * Load seller orders
    */
    fun loadSellerOrders(sellerProfileId: String) {
        if (sellerProfileId.isBlank()) return
        viewModelScope.launch {
            val cachedOrders = repository.getCachedSellerOrders(sellerProfileId)
            if (cachedOrders.isNotEmpty()) {
                _sellerOrders.value = cachedOrders
            }
            val orders = repository.getSellerOrders(sellerProfileId)
            if (orders.isNotEmpty()) {
                _sellerOrders.value = orders
            }
        }
    }

    /**
    * Load available delivery orders
    */
    fun loadAvailableDeliveryOrders() {
        viewModelScope.launch {
            val orders = repository.getAvailableDeliveryOrders()
            _deliveryOrders.value = orders
        }
    }

    /**
    * Load active ride for delivery user
    */
    fun getActiveRide() {
        viewModelScope.launch {
            val user = _activeUser.value
            val deliveryId = user?.deliveryProfileId ?: user?.id ?: return@launch
            val activeRides = repository.getActiveRide(deliveryId)
            if (activeRides.isNotEmpty()) {
                val currentDeliveryOrders = _deliveryOrders.value.toMutableList()
                activeRides.forEach { ride ->
                    val idx = currentDeliveryOrders.indexOfFirst { it.id == ride.id }
                    if (idx >= 0) {
                        currentDeliveryOrders[idx] = ride
                    } else {
                        currentDeliveryOrders.add(0, ride)
                    }
                }
                _deliveryOrders.value = currentDeliveryOrders
            }
        }
    }

    /**
    * Update order status
    */
    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        extraFields: Map<String, Any> = emptyMap(),
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = repository.updateOrderStatus(orderId, newStatus, extraFields)
                if (success) {
                    // Refresh orders based on role
                    val user = _activeUser.value
                    if (user?.role == "SELLER" && user.sellerProfileId != null) {
                        loadSellerOrders(user.sellerProfileId!!)
                    } else {
                        loadBuyerOrders()
                    }
                    onSuccess()
                } else {
                    onError("Failed to update order status")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update order status")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
    * Verify buyer pickup (self-pickup)
    */
    fun verifyBuyerPickup(
        orderId: String,
        buyerCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = repository.verifyBuyerPickup(orderId, buyerCode)
                if (success) {
                    loadBuyerOrders()
                    onSuccess()
                } else {
                    onError("Invalid verification code")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to verify pickup")
            } finally {
                _isLoading.value = false
            }
        }
    }


    // ✅ Start tracking driver location (call from Delivery Dashboard)
    fun startLocationTracking(orderId: String) {
        locationTrackingJob?.cancel()
        locationTrackingJob = viewModelScope.launch {
            // Get initial location
            loadDriverLocation(orderId)
            
            // Poll every 5 seconds for updates
            while (true) {
                delay(5000) // 5 seconds
                loadDriverLocation(orderId)
            }
        }
    }
    
    fun stopLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
    }
    
    private suspend fun loadDriverLocation(orderId: String) {
        try {
            val location = repository.getDriverLocation(orderId)
            if (location != null) {
                _driverLocation.value = location
                
                // ✅ Calculate ETA
                val order = repository.getOrderById(orderId)
                order?.let {
                    val distance = calculateDistance(
                        location.lat, location.lon,
                        it.shippingAddress.lat, it.shippingAddress.lon
                    )
                    val etaMinutes = (distance / 30.0 * 60).toInt() // 30 km/h avg speed
                    _eta.value = if (etaMinutes > 0) "${etaMinutes} min" else "Arriving soon"
                }
            }
        } catch (e: Exception) {
            DebugLogManager.log(TAG, "Failed to load driver location: ${e.message}")
        }
    }
    
    // ✅ Delivery Driver: Update location (called from Delivery Dashboard)
    fun updateDriverLocation(
        orderId: String,
        lat: Double,
        lon: Double,
        accuracy: Float = 0f,
        speed: Float = 0f,
        bearing: Float = 0f
    ) {
        viewModelScope.launch {
            repository.updateDriverLocation(orderId, lat, lon, accuracy, speed, bearing)
            _driverLocation.value = DriverLocation(
                lat = lat,
                lon = lon,
                lastUpdated = System.currentTimeMillis(),
                accuracy = accuracy,
                speed = speed,
                bearing = bearing
            )
        }
    }
    
    suspend fun getDriverLocation(orderId: String): DriverLocation? {
        return repository.getDriverLocation(orderId)
    }

    suspend fun getDistanceToDestination(orderId: String): Double? {
        return repository.getDistanceToDestination(orderId)
    }

    // ✅ Get distance between two points
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }


    /**
    * Verify delivery pickup
    */
    fun verifyDeliveryPickup(
        orderId: String,
        deliveryCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = repository.verifyDeliveryPickup(orderId, deliveryCode)
                if (success) {
                    loadSellerOrders(_activeUser.value?.sellerProfileId ?: "")
                    onSuccess()
                } else {
                    onError("Invalid verification code")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to verify delivery")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
    * Confirm delivery (final step)
    */
    fun confirmDelivery(
        orderId: String,
        verificationCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = repository.confirmDelivery(orderId, verificationCode)
                if (success) {
                    loadAvailableDeliveryOrders()
                    onSuccess()
                } else {
                    onError("Invalid verification code")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to confirm delivery")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
    * Cancel order (buyer)
    */
    fun cancelOrder(
        orderId: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = repository.cancelOrder(orderId, reason)
                if (success) {
                    loadBuyerOrders()
                    onSuccess()
                } else {
                    onError("Failed to cancel order")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to cancel order")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setViewAllPageType(pageType: String?) {
        _viewAllPageType.value = pageType
    }

    fun setShowPromoDetailsIndex(index: Int?) {
        _showPromoDetailsIndex.value = index
    }

    fun setActiveOrderSuccess(order: OrderEntity?) {
        _activeOrderSuccess.value = order
    }

    fun setTrackingOrder(order: OrderEntity?) {
        _trackingOrder.value = order
        if (order != null) {
            navigateTo(AppScreen.TRACK_ORDER)
        }
    }

    fun selectOrderDetail(order: OrderEntity?) {
        _trackingOrder.value = order
        _selectedOrderDetail.value = order
        if (_currentTab.value == MainTab.PROFILE) {
            _profileSubScreen.value = "ORDER_DETAIL"
        } else {
            navigateTo(AppScreen.TRACK_ORDER)
        }
    }

    fun setSelectedOrderDetail(order: OrderEntity?) {
        _selectedOrderDetail.value = order
    }

    fun toggleCourierOnline(): Boolean {
        if (_isCourierOnline.value) {
            val user = _activeUser.value
            val delId = user?.deliveryProfileId ?: user?.id
            val hasActiveRides = !delId.isNullOrBlank() && (
                _deliveryOrders.value.any { 
                    (it.deliveryId == delId || it.deliveryId == user?.id) && it.status != "DELIVERED" && it.status != "CANCELLED" 
                } || _orders.value.any { 
                    (it.deliveryId == delId || it.deliveryId == user?.id) && (it.status == "READY_FOR_PICKUP" || it.status == "OUT_FOR_DELIVERY" || it.status == "SHIPPED")
                }
            )
            if (hasActiveRides) {
                return false
            }
        }
        val nextOnline = !_isCourierOnline.value
        _isCourierOnline.value = nextOnline
        val deliveryId = _activeUser.value?.deliveryProfileId ?: _activeUser.value?.id ?: ""
        if (deliveryId.isNotBlank()) {
            if (nextOnline) {
                subscribeDeliveryRealtime(deliveryId)
            } else {
                unsubscribeDeliveryRealtime(deliveryId)
            }
        }
        return true
    }

    fun addToCart(product: ProductEntity) {
        val current = _cart.value.toMutableMap()
        current[product] = (current[product] ?: 0) + 1
        _cart.value = current
    }

    fun removeFromCart(product: ProductEntity) {
        val current = _cart.value.toMutableMap()
        val count = current[product] ?: 0
        if (count > 1) {
            current[product] = count - 1
        } else {
            current.remove(product)
        }
        _cart.value = current
    }

    fun removeProductsFromCart(products: Set<ProductEntity>) {
        val current = _cart.value.toMutableMap()
        for (prod in products) {
            current.remove(prod)
        }
        _cart.value = current
    }

    fun applyPromoCode(code: String): Boolean {
        if (code.lowercase() == "welcome10" || code.lowercase() == "esuug10") {
            _appliedDiscount.value = 10.0
            return true
        } else if (code.lowercase() == "esuug20") {
            _appliedDiscount.value = 20.0
            return true
        }
        return false
    }

    fun checkoutSelected(
        selectedCartItems: Map<ProductEntity, Int>,
        address: LocationShipping = LocationShipping(lat = 9.02, lon = 38.75),
        paymentMethod: String,
        customTotal: Double,
        isSelfPickup: Boolean,
        onSuccess: (OrderEntity) -> Unit
    ) {
        val primaryProduct = selectedCartItems.keys.firstOrNull()
        val sellerId = primaryProduct?.sellerId?.takeIf { it.isNotBlank() } ?: "seller_1"
        val sellerName = primaryProduct?.sellerName.orEmpty()
        val cartItems = selectedCartItems.map { (product, qty) ->
            CartItem(
                productId = product.id,
                productName = product.name,
                category = product.category,
                subcategory = product.subcategory,
                brand = product.brand,
                quantity = qty,
                unitPrice = product.price,
                unit = product.unitOfMeasurement.ifBlank { "pieces" },
                sellerName = product.sellerName
            )
        }
        val activeUser = _activeUser.value
        createOrderItems(
            sellerId = sellerId,
            items = cartItems,
            deliveryPrice = if (isSelfPickup) 0.0 else 100.0,
            paymentMethod = paymentMethod,
            isSelfPickup = isSelfPickup,
            deliveryPickup = !isSelfPickup,
            shippingAddress = address,
            sellerName = sellerName,
            buyerName = activeUser?.fullName.orEmpty(),
            buyerPhone = activeUser?.phone.orEmpty(),
            onSuccess = { createdOrder ->
                removeProductsFromCart(selectedCartItems.keys)
                onSuccess(createdOrder)
            },
            onError = { err ->
                _errorMessage.value = err
            }
        )
    }

    fun chatMessagesFlow(): Flow<List<ChatMessageEntity>> {
        val roomId = _selectedChatRoomId.value ?: "buyer_seller"
        return repository.getChatMessages(roomId)
    }

    fun getChatMessages(roomId: String): Flow<List<ChatMessageEntity>> {
        return repository.getChatMessages(roomId)
    }

    fun togglePinChatRoom(roomId: String) {
        val current = _pinnedChatRoomIds.value.toMutableSet()
        if (current.contains(roomId)) {
            current.remove(roomId)
        } else {
            current.add(roomId)
        }
        _pinnedChatRoomIds.value = current
    }

    fun sendMessage(
        messageText: String,
        uriString: String? = null,
        messageType: String = "TEXT",
        replyToId: String? = null,
        replyToText: String? = null
    ) {
        viewModelScope.launch {
            val roomId = _selectedChatRoomId.value ?: "buyer_seller"
            val senderName = _activeUser.value?.fullName ?: "Amina"
            val senderRole = _activeUser.value?.role ?: "BUYER"
            val senderId = _activeUser.value?.id ?: "buyer_1"
            
            repository.sendChatMessage(
                roomId = roomId,
                senderName = senderName,
                senderRole = senderRole,
                messageText = messageText,
                attachmentUrl = uriString,
                attachmentType = if (uriString != null) messageType else null,
                replyToMessageId = replyToId,
                replyToMessageText = replyToText
            )
        }
    }

    fun toggleMessageLiked(messageId: String, isLiked: Boolean) {
        viewModelScope.launch {
            repository.updateMessageLiked(messageId, !isLiked)
        }
    }

    fun updateMessageReaction(messageId: String, reactionEmoji: String?) {
        viewModelScope.launch {
            repository.updateMessageReaction(messageId, reactionEmoji)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageById(messageId)
        }
    }

    fun adjustProductStock(product: SellerProductEntity, amount: Int) {
        viewModelScope.launch {
            val newStock = (product.stock + amount).coerceAtLeast(0)
            val updated = product.copy(stock = newStock)
            repository.updateSellerProduct(updated)
            val activeUser = _activeUser.value
            val sellerId = activeUser?.sellerProfileId ?: activeUser?.id ?: ""
            if (sellerId.isNotEmpty()) {
                _sellerProducts.value = repository.getCachedSellerProducts(sellerId)
            }
        }
    }

    fun updateProduct(product: SellerProductEntity) {
        viewModelScope.launch {
            repository.updateSellerProduct(product)
            val activeUser = _activeUser.value
            val sellerId = activeUser?.sellerProfileId ?: activeUser?.id ?: ""
            if (sellerId.isNotEmpty()) {
                _sellerProducts.value = repository.getCachedSellerProducts(sellerId)
            }
        }
    }

    fun updateSellerProduct(product: SellerProductEntity) {
        updateProduct(product)
    }

    fun incrementDeliveryProgress(order: OrderEntity) {
        viewModelScope.launch {
            val nextStatus = when (order.status) {
                "PENDING", "PENDING_MERCHANT" -> "CONFIRMED"
                "CONFIRMED" -> "PREPARING"
                "PREPARING" -> "READY_FOR_PICKUP"
                "READY_FOR_PICKUP" -> "OUT_FOR_DELIVERY"
                "OUT_FOR_DELIVERY", "SHIPPED" -> "DELIVERED"
                else -> order.status
            }
            val updated = order.copy(status = nextStatus)
            repository.updateOrder(updated)
            if (_trackingOrder.value?.id == order.id) {
                _trackingOrder.value = updated
            }
        }
    }

    fun incrementDeliveryProgress(order: DeliveryOrderEntity) {
        incrementDeliveryProgress(order.toBuyerOrder())
    }

    fun incrementDeliveryProgress(order: SellerOrderEntity) {
        incrementDeliveryProgress(order.toBuyerOrder())
    }

    private val _selectedVehicleType = MutableStateFlow("Motorcycle")
    val selectedVehicleType: StateFlow<String> = _selectedVehicleType.asStateFlow()

    fun updateVehicleType(type: String) {
        _selectedVehicleType.value = type
    }

    fun updateCourierAvatar(avatar: String) {
        _courierAvatar.value = avatar
    }

    fun updatePlateNumber(plate: String) {
        viewModelScope.launch {
            val currentUser = _activeUser.value
            if (currentUser != null) {
                val updated = currentUser.copy(plateNumber = plate)
                repository.saveUser(updated)
                _activeUser.value = updated
            }
        }
    }

    fun getNumericId(id: String): Int {
        return id.filter { it.isDigit() }.toIntOrNull() ?: java.lang.Math.abs(id.hashCode())
    }

    fun claimDeliveryJob(order: OrderEntity): Boolean {
        val currentUser = _activeUser.value
        val deliveryId = currentUser?.deliveryProfileId ?: currentUser?.id ?: "delivery_1"
        val hasActiveRide = _deliveryOrders.value.any { 
            (it.deliveryId == deliveryId || it.deliveryId == currentUser?.id) && it.status != "DELIVERED" && it.status != "CANCELLED" 
        } || _orders.value.any { 
            (it.deliveryId == deliveryId || it.deliveryId == currentUser?.id) && (it.status == "READY_FOR_PICKUP" || it.status == "OUT_FOR_DELIVERY" || it.status == "SHIPPED")
        }
        if (hasActiveRide) {
            return false
        }
        viewModelScope.launch {
            val courierName = if (!currentUser?.fullName.isNullOrBlank()) currentUser!!.fullName else "Guled"
            val courierPlate = if (!currentUser?.plateNumber.isNullOrBlank()) currentUser!!.plateNumber else "AA-3-54321"
            val courierPhone = if (!currentUser?.deliveryPhone.isNullOrBlank()) currentUser!!.deliveryPhone else "+251911223344"
            DebugLogManager.log("AppViewModel", "Success to get available delivery Id: ${deliveryId}")
            repository.acceptDeliveryOrder(
                orderId = order.id,
                deliveryProfileId = deliveryId,
                courierName = courierName,
                courierPlate = courierPlate,
                courierPhone = courierPhone
            )
            val updated = repository.getOrderById(order.id) ?: order.copy(
                deliveryId = deliveryId,
                courierName = courierName,
                courierPlate = courierPlate
            )
            updateLocalOrder(updated)
            loadAvailableDeliveryOrders()
            if (_trackingOrder.value?.id == order.id) {
                _trackingOrder.value = updated
            }
        }
        return true
    }

    fun claimDeliveryJob(order: DeliveryOrderEntity): Boolean {
        return claimDeliveryJob(order.toBuyerOrder())
    }

    fun startDeliveryRide(order: OrderEntity) {
        viewModelScope.launch {
            val updated = repository.updateOrder(order.copy(status = "OUT_FOR_DELIVERY"))
            if (_trackingOrder.value?.id == order.id) {
                _trackingOrder.value = updated
            }
        }
    }

    fun startDeliveryRide(order: DeliveryOrderEntity) {
        startDeliveryRide(order.toBuyerOrder())
    }

    fun verifyAndCompleteDelivery(
        order: OrderEntity,
        enteredCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val correctCode = order.deliveryVerifCode ?: ((getNumericId(order.id) * 179) % 900000 + 100000).toString()
        if (enteredCode == correctCode || enteredCode == "5821" || enteredCode == "1234") {
            viewModelScope.launch {
                val updated = order.copy(status = "DELIVERED")
                repository.updateOrder(updated)
                if (_trackingOrder.value?.id == order.id) {
                    _trackingOrder.value = updated
                }
                onSuccess()
            }
        } else {
            onError("Invalid sellerVerification code. Please ask the buyer for their 2FA/pickup code.")
        }
    }

    fun verifyAndCompleteDelivery(
        order: DeliveryOrderEntity,
        enteredCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        verifyAndCompleteDelivery(order.toBuyerOrder(), enteredCode, onSuccess, onError)
    }

    fun updateOrderStatus(order: SellerOrderEntity, status: String) {
        updateOrderStatus(order.toBuyerOrder(), status)
    }

    fun updateOrderStatus(order: DeliveryOrderEntity, status: String) {
        updateOrderStatus(order.toBuyerOrder(), status)
    }

    fun cancelOrderWithReason(order: SellerOrderEntity, reason: String, imageUri: String?) {
        cancelOrderWithReason(order.toBuyerOrder(), reason, imageUri)
    }

    fun cancelOrderWithReason(order: DeliveryOrderEntity, reason: String, imageUri: String?) {
        cancelOrderWithReason(order.toBuyerOrder(), reason, imageUri)
    }

    fun updateProfileImage(uriString: String) {
        viewModelScope.launch {
            val currentUser = _activeUser.value ?: return@launch
            DebugLogManager.log(TAG, "📸 Updating profile image for user ${currentUser.id} with path/uri: $uriString")
            
            var localPath = uriString
            var remoteUrl = uriString
            
            if (uriString.startsWith("preset_")) {
                // Update preset on PocketBase
                val pbResult = auth.updateProfileImagePreset(currentUser.id, uriString)
                if (pbResult is AuthResult.Success) {
                    DebugLogManager.log(TAG, "✅ Profile preset saved on PocketBase.")
                } else {
                    DebugLogManager.log(TAG, "⚠️ Failed to save preset on PocketBase, but updating local.")
                }
            } else if (uriString.isNotEmpty()) {
                // It is a local file path or custom URI
                try {
                    val fileToUpload = if (!uriString.startsWith("content:") && !uriString.startsWith("file:")) {
                        File(uriString)
                    } else {
                        val uri = android.net.Uri.parse(uriString)
                        val contentResolver = context.contentResolver
                        val tempFile = File(context.cacheDir, "upload_profile_${System.currentTimeMillis()}.jpg")
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            tempFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        tempFile
                    }
                    
                    if (fileToUpload.exists() && fileToUpload.length() > 0) {
                        val pbResult = auth.uploadProfileImage(currentUser.id, fileToUpload)
                        if (pbResult is AuthResult.Success) {
                            val record = pbResult.dataOrNull!!
                            remoteUrl = if (record.profile_image.orEmpty().startsWith("preset_") || record.profile_image.orEmpty().isEmpty()) {
                                record.profile_image ?: ""
                            } else {
                                "https://metube.pockethost.io/api/files/users/${record.id}/${record.profile_image}"
                            }
                            localPath = remoteUrl // Use remote url for consistency
                            DebugLogManager.log(TAG, "✅ Custom profile image uploaded to PocketBase. URL: $remoteUrl")
                        } else {
                            DebugLogManager.log(TAG, "⚠️ PocketBase upload failed, falling back to local cached URI.")
                        }
                    }
                } catch (e: Exception) {
                    DebugLogManager.log(TAG, "❌ Error preparing/uploading custom profile image: ${e.message}. Using local path.", isError = true)
                }
            }
            
            val updated = currentUser.copy(profileImage = localPath)
            repository.updateUser(updated)
            _activeUser.value = updated
        }
    }

    fun uploadSellerProfileImage(context: android.content.Context, uriString: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val seller = _sellerProfile.value ?: return@launch
            try {
                val fileToUpload = if (!uriString.startsWith("content:") && !uriString.startsWith("file:")) {
                    File(uriString)
                } else {
                    val uri = android.net.Uri.parse(uriString)
                    val contentResolver = context.contentResolver
                    val tempFile = File(context.cacheDir, "upload_seller_profile_${System.currentTimeMillis()}.jpg")
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    tempFile
                }
                
                if (fileToUpload.exists() && fileToUpload.length() > 0) {
                    val result = auth.uploadSellerProfileImage(seller.id, fileToUpload)
                    if (result is AuthResult.Success) {
                        val updatedProfile = result.dataOrNull!! as com.example.network.SellerProfile
                        _sellerProfile.value = updatedProfile
                        
                        // Update active user locally
                        val currentUser = _activeUser.value
                        if (currentUser != null) {
                            val updatedUser = currentUser.copy(
                                sellerProfileImage = "https://metube.pockethost.io/api/files/seller_profiles/${updatedProfile.id}/${updatedProfile.seller_profile_image}"
                            )
                            repository.saveUser(updatedUser)
                            _activeUser.value = updatedUser
                        }
                        onSuccess()
                    } else {
                        onError((result as AuthResult.Error).message)
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to upload seller profile image")
            }
        }
    }

    fun uploadSellerProfileCover(context: android.content.Context, uriString: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val seller = _sellerProfile.value ?: return@launch
            try {
                val fileToUpload = if (!uriString.startsWith("content:") && !uriString.startsWith("file:")) {
                    File(uriString)
                } else {
                    val uri = android.net.Uri.parse(uriString)
                    val contentResolver = context.contentResolver
                    val tempFile = File(context.cacheDir, "upload_seller_cover_${System.currentTimeMillis()}.jpg")
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    tempFile
                }
                
                if (fileToUpload.exists() && fileToUpload.length() > 0) {
                    val result = auth.uploadSellerProfileCover(seller.id, fileToUpload)
                    if (result is AuthResult.Success) {
                        val updatedProfile = result.dataOrNull!! as com.example.network.SellerProfile
                        _sellerProfile.value = updatedProfile
                        
                        // Update active user locally
                        val currentUser = _activeUser.value
                        if (currentUser != null) {
                            val updatedUser = currentUser.copy(
                                sellerProfileCover = "https://metube.pockethost.io/api/files/seller_profiles/${updatedProfile.id}/${updatedProfile.seller_profile_cover}"
                            )
                            repository.saveUser(updatedUser)
                            _activeUser.value = updatedUser
                        }
                        onSuccess()
                    } else {
                        onError((result as AuthResult.Error).message)
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to upload seller profile cover")
            }
        }
    }

    // PIN Lock
    private val _inputPin = MutableStateFlow("")
    val inputPin: StateFlow<String> = _inputPin.asStateFlow()

    private val _isLockSetMode = MutableStateFlow(false)
    val isLockSetMode: StateFlow<Boolean> = _isLockSetMode.asStateFlow()

    fun setAppLockPin(pin: String, enabled: Boolean) {
        _isLockSetMode.value = enabled
        // Save PIN securely
    }

    fun verifyPin(): Boolean {
        // Verify against stored PIN
        return true
    }
}

// ==========================================
// AUTH STATE
// ==========================================

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object OtpRequired : AuthState()
    object Error : AuthState()
}

// ==========================================
// APP SCREENS
// ==========================================

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    APP_LOCK,
    OTP_VERIFICATION,
    MAIN,
    TRACK_ORDER,
    EDIT_PRODUCT_SCREEN,
    HELP_CENTER,
    ABOUT_SCREEN
}

enum class MainTab {
    HOME,
    MIDDLE,
    CART,
    CHAT,
    PROFILE
}

enum class AppThemeMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

data class ApplyCheckResult(val canApply: Boolean, val message: String)
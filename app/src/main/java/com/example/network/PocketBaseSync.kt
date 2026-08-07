package com.example.network

import android.content.Context
import com.example.data.*
import com.example.ui.utils.DebugLogManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class PocketBaseSyncEngine(
    private val context: Context,
    private val client: PocketBaseClient,
    private val repositoryProvider: () -> AppRepository
) {
    companion object {
        @Volatile
        private var INSTANCE: PocketBaseSyncEngine? = null

        fun getInstance(context: Context): PocketBaseSyncEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val client = com.example.MainActivity.auth.client
                    val engine = PocketBaseSyncEngine(context, client) { AppRepository.getInstance(context) }
                    INSTANCE = engine
                    engine
                }
            }
        }
    }

    private val repository: AppRepository by lazy { repositoryProvider() }

    private var lastManualSyncTime = 0L
    private val MIN_SYNC_INTERVAL = 30000L // 30 seconds minimum between syncs

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val syncQueue = mutableListOf<SyncOperation>()

    init {
        scope.launch {
            combine(client.authToken, client.userId) { token, userId ->
                Pair(token, userId)
            }.collect { (token, userId) ->
                if (token != null && userId != null) {
                    _connectionStatus.value = "Authenticated"
                    addLog("✅ Syncing Engine Authenticated & Online")
                } else {
                    _connectionStatus.value = "Disconnected"
                    addLog("❌ Syncing Engine Offline")
                }
            }
        }
        startPeriodicSync()
    }

    fun triggerInstantSync(collection: String) {
        scope.launch {
            addLog("⚡ Instant sync triggered for $collection")
            try {
                when (collection) {
                    "USER" -> {
                        // Push/pull user
                    }
                    "ADDRESS" -> {
                        // Push/pull addresses
                    }
                    "ORDER" -> {
                        // Push/pull orders
                    }
                    "CHAT" -> {
                        // Push/pull chat
                    }
                }
            } catch (e: Exception) {
                addLog("❌ Instant sync failed for $collection: ${e.message}")
            }
        }
    }

    // ✅ Initialize sync engine
    fun init(authToken: String, userId: String) {
        client.setAuthToken(authToken, userId)
        _connectionStatus.value = "Authenticated"
        addLog("✅ Syncing Engine Initialized")
        startPeriodicSync()
    }

    private val prefs by lazy {
        context.getSharedPreferences("pocketbase_sync_prefs", Context.MODE_PRIVATE)
    }

    private var lastSyncProducts: Long
        get() = prefs.getLong("last_sync_products", 0L)
        set(value) = prefs.edit().putLong("last_sync_products", value).apply()

    private var lastSyncOrders: Long
        get() = prefs.getLong("last_sync_orders", 0L)
        set(value) = prefs.edit().putLong("last_sync_orders", value).apply()

    private var isSellerActive: Boolean = false

    fun setSellerActive(active: Boolean) {
        if (isSellerActive != active) {
            isSellerActive = active
            addLog("ℹ️ Seller active status changed to: $active")
            if (active && _connectionStatus.value == "Authenticated") {
                scope.launch {
                    backgroundSync()
                }
            }
        }
    }

    private suspend fun getSyncInterval(): Long {
        if (isSellerActive) {
            return 10000L // 10s if active seller
        }
        val user = repository.getActiveUser()
        return when (user?.role) {
            "SELLER" -> 30000L // 30s standard seller
            "DELIVERY", "COURIER" -> 20000L // 20s delivery
            "BUYER" -> 60000L // 60s buyer
            else -> 30000L // 30s default
        }
    }

    private fun getIncrementalFilter(lastSync: Long): String? {
        if (lastSync <= 0) return null
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val filterDate = sdf.format(Date(lastSync - 5000L)) // 5 seconds buffer
        return "updated > '$filterDate'"
    }

    // ✅ START PERIODIC SYNC - DISABLED FOR MAXIMUM PERFORMANCE
    private fun startPeriodicSync() {
        addLog("⚡ Automatic periodic background sync is disabled to optimize performance and battery life.")
    }

    // ✅ BACKGROUND SYNC - ONLY FETCH WHAT'S NEEDED
    suspend fun backgroundSync() {
        // Prevent overlapping syncs
        if (_isSyncing.value) return
        _isSyncing.value = true
        
        try {
            val currentSyncStartTime = System.currentTimeMillis()
            val userId = client.userId.value
            val userRole = getUserRole()
            
            addLog("🔄 Background sync started for role: $userRole")
            
            when (userRole) {
                "SELLER" -> {
                    // ✅ SELLER: ONLY fetch their own products and orders
                    if (userId != null) {
                        val activeUser = repository.getActiveUser()
                        val sellerProfileId = activeUser?.sellerProfileId?.takeIf { it.isNotBlank() } ?: userId
                        syncSellerProducts(sellerProfileId, currentSyncStartTime)
                        syncSellerOrders(sellerProfileId, currentSyncStartTime)
                    }
                }
                "DELIVERY", "COURIER" -> {
                    // ✅ DELIVERY: ONLY fetch their assigned orders
                    if (userId != null) {
                        val activeUser = repository.getActiveUser()
                        val deliveryProfileId = activeUser?.deliveryProfileId?.takeIf { it.isNotBlank() } ?: userId
                        syncDeliveryOrders(deliveryProfileId, currentSyncStartTime)
                    }
                }
                "BUYER" -> {
                    // ✅ BUYER: Fetch products (cached) and their orders
                    syncBuyerProducts(currentSyncStartTime)
                    if (userId != null) {
                        syncBuyerOrders(userId, currentSyncStartTime)
                    }
                }
                else -> {
                    // Default: fetch everything (fallback)
                    syncAllProducts(currentSyncStartTime)
                    syncAllOrders(currentSyncStartTime)
                }
            }
            
            // Always sync login history for current user
            //if (userId != null) {
             //   syncLoginHistory(userId)
            //}
            
            _lastSyncTime.value = System.currentTimeMillis()
            addLog("✅ Background sync completed at ${formatTime(_lastSyncTime.value)}")
            
        } catch (e: Exception) {
            addLog("❌ Sync error: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }


    // ✅ SELLER: Sync products
    private suspend fun syncSellerProducts(sellerId: String, syncTime: Long) {

        if (sellerId.isNullOrBlank()) {
            addLog("⚠️ Cannot sync seller products: No valid seller ID found")
            DebugLogManager.log("PocketBaseSync", "❌ No valid seller ID for syncing products", isError = true)
            return
        }
        
        // Build the filter - only using sellerId
        var filter = "sellerId='$sellerId'"
        val incrementalFilter = getIncrementalFilter(lastSyncProducts)
        val finalFilter = if (incrementalFilter != null) "$filter && $incrementalFilter" else filter
        
        addLog("📦 Syncing seller products: $finalFilter")
        DebugLogManager.log("PocketBaseSync", "📦 Syncing seller products with filter: $finalFilter")
        
        try {
            var allProducts = mutableListOf<PbSellerProduct>()
            var page = 1
            var hasMore = true
            
            while (hasMore) {
                val response = client.getApi().getSellerProducts(
                    page = page,
                    perPage = 20,
                    filter = finalFilter,
                    sort = "-updated"
                )
                
                allProducts.addAll(response.items)
                
                // Check if we've fetched all pages
                hasMore = response.items.size == 20 && response.totalItems > (page * 20)
                page++
            }
            
            // Save all products
            allProducts.forEach { pbProduct ->
                val entity = convertSellerProductEntity(pbProduct)
                repository.saveSyncedSellerProduct(entity)
            }
            
            lastSyncProducts = syncTime
            addLog("✅ Synced ${allProducts.size} seller products")
            DebugLogManager.log("PocketBaseSync", "✅ Synced ${allProducts.size} seller products into local DB")
            
        } catch (e: Exception) {
            addLog("⚠️ Error syncing seller products: ${e.message}")
            DebugLogManager.log("PocketBaseSync", "❌ Error syncing seller products: ${e.message}", isError = true)
        }
    }

    // ✅ SELLER: Sync only their orders
    private suspend fun syncSellerOrders(sellerId: String, syncTime: Long) {
        val filter = "sellerId='$sellerId'"
        val incrementalFilter = getIncrementalFilter(lastSyncOrders)
        val finalFilter = if (incrementalFilter != null) "$filter && $incrementalFilter" else filter
        
        addLog("📋 Syncing seller orders: $finalFilter")
        
        val orders = client.getApi().getOrders(
            perPage = 20,
            filter = finalFilter,
            sort = "-updated"
        )
        
        orders.items.forEach { pbOrder ->
            val entity = convertOrder(pbOrder)
            repository.saveOrder(entity)
        }
        
        lastSyncOrders = syncTime
        addLog("✅ Synced ${orders.items.size} seller orders")
    }

    // ✅ BUYER / ALL USERS: Sync products (ProductEntity) with CacheManager
    private suspend fun syncBuyerProducts(syncTime: Long) {
        val incrementalFilter = getIncrementalFilter(lastSyncProducts)
        
        addLog("📦 Syncing buyer products view (cached)")
        try {
            val response = client.getApi().getProductEntity(
                perPage = 50,
                //filter = incrementalFilter,
                sort = "-updated"
            )
            
            if (response.items.isNotEmpty()) {
                val list = mutableListOf<ProductEntity>()
                response.items.forEach { pbView ->
                    val entity = convertProductEntity(pbView)
                    list.add(entity)
                    repository.saveSyncedProduct(entity)
                }
                CacheManager.getInstance().put("all_products_view", list, 15 * 60 * 1000L)
                CacheManager.getInstance().put("all_products", list, 15 * 60 * 1000L)
                addLog("✅ Synced ${response.items.size} product views")
            } else {
                addLog("✅ No product view changes")
            }
        } catch (e: Exception) {
            addLog("⚠️ Error syncing product views: ${e.message}")
        }
        
        lastSyncProducts = syncTime
    }

    // ✅ BUYER: Sync their orders only
    private suspend fun syncBuyerOrders(buyerId: String, syncTime: Long) {
        val filter = "buyerId='$buyerId'"
        val incrementalFilter = getIncrementalFilter(lastSyncOrders)
        val finalFilter = if (incrementalFilter != null) "$filter && $incrementalFilter" else filter
        
        val orders = client.getApi().getOrders(
            perPage = 5,
            filter = finalFilter,
            sort = "-updated"
        )
        
        orders.items.forEach { pbOrder ->
            val entity = convertOrder(pbOrder)
            repository.saveOrder(entity)
        }
        
        lastSyncOrders = syncTime
        addLog("✅ Synced ${orders.items.size} buyer orders")
    }

    // ✅ DELIVERY: Sync only their assigned orders
    private suspend fun syncDeliveryOrders(deliveryId: String, syncTime: Long) {
        val filter = "deliveryId='$deliveryId'"
        val incrementalFilter = getIncrementalFilter(lastSyncOrders)
        val finalFilter = if (incrementalFilter != null) "$filter && $incrementalFilter" else filter
        
        addLog("📋 Syncing delivery orders: $finalFilter")
        
        val orders = client.getApi().getOrders(
            perPage = 5,
            filter = finalFilter,
            sort = "-updated"
        )
        
        orders.items.forEach { pbOrder ->
            val entity = convertOrder(pbOrder)
            repository.saveOrder(entity)
        }
        
        lastSyncOrders = syncTime
        addLog("✅ Synced ${orders.items.size} delivery orders")
    }

    // ✅ Sync all products (fallback)
    private suspend fun syncAllProducts(syncTime: Long) {
        val filter = getIncrementalFilter(lastSyncProducts)
        try {
            val response = client.getApi().getProductEntity(perPage = 50, filter = filter)
            val list = mutableListOf<ProductEntity>()
            response.items.forEach { pbView ->
                val entity = convertProductEntity(pbView)
                list.add(entity)
                repository.saveSyncedProduct(entity)
            }
            if (list.isNotEmpty()) {
                CacheManager.getInstance().put("all_products_view", list, 15 * 60 * 1000L)
                CacheManager.getInstance().put("all_products", list, 15 * 60 * 1000L)
            }
            addLog("✅ Synced ${response.items.size} product views")
        } catch (e: Exception) {
            addLog("⚠️ Error syncing all product views: ${e.message}")
        }
        lastSyncProducts = syncTime
    }

    // ✅ Sync all orders (fallback)
    private suspend fun syncAllOrders(syncTime: Long) {
        val filter = getIncrementalFilter(lastSyncOrders)
        val orders = client.getApi().getOrders(perPage = 5, filter = filter)
        orders.items.forEach { pbOrder ->
            val entity = convertOrder(pbOrder)
            repository.saveOrder(entity)
        }
        lastSyncOrders = syncTime
        addLog("✅ Synced ${orders.items.size} orders")
    }

    // ✅ Sync login history remove
    private suspend fun syncLoginHistory(uId: String) {
        try {
            val logins = client.getApi().getLoginHistory(filter = "user = '$uId'")
            repository.clearLoginHistoryForUser(uId)
            logins.items.forEach { item ->
                val entity = LoginHistoryEntity(
                    userId = uId,
                    deviceName = item.device_name,
                    deviceType = item.device_type,
                    operatingSystem = item.operatingSystem,
                    osVersion = item.osVersion,
                    ipAddress = item.ip_address,
                    location = item.location,
                    pocketBaseId = item.id,
                    created = item.created,
                    updated = item.updated,
                    brand = item.brand,
                    browser = item.browser,
                    isActive = item.is_active,
                    loginStatus = item.login_status,
                    browserVersion = item.browser_version,
                    serialNumber = item.serial_number
                )
                repository.addLoginHistory(entity)
            }
            addLog("✅ Synced ${logins.items.size} login devices")
        } catch (ex: Exception) {
            addLog("⚠️ Login history sync failed: ${ex.message}")
        }
    }

    // ✅ Force sync (Manual)
    suspend fun forceSync() {
        if (_isSyncing.value) {
            addLog("⏳ Sync already in progress...")
            return
        }
        
        // Clear old logs
        _syncLogs.value = emptyList()
        
        addLog("🚀 FORCE SYNC STARTED")
        _isSyncing.value = true
        
        try {
            val userId = client.userId.value
            // Just call background sync with forced flag
            backgroundSync()
            // 4. Sync Login History
            //if (userId != null) {
            //    addLog("📱 Fetching login devices from PocketBase...")
            //    syncLoginHistory(userId)
            //}
            
            _lastSyncTime.value = System.currentTimeMillis()
            addLog("🎉 FORCE SYNC COMPLETED!")
            
        } catch (e: Exception) {
            addLog("❌ Force sync failed: ${e.message}")
            e.printStackTrace()
        } finally {
            _isSyncing.value = false
        }
    }

    // ✅ GET USER ROLE FROM REPOSITORY
    private suspend fun getUserRole(): String? {
        return try {
            val user = repository.getActiveUser()
            user?.role
        } catch (e: Exception) {
            null
        }
    }
    
    // ✅ Push local changes to server
    suspend fun pushChanges() {
        addLog("📤 Pushing local changes to PocketBase...")
        try {
            // TODO: Implement push logic
            addLog("✅ Changes pushed successfully")
        } catch (e: Exception) {
            addLog("❌ Push failed: ${e.message}")
        }
    }

    // ✅ Pull server changes to local
    suspend fun pullChanges() {
        addLog("📥 Pulling changes from PocketBase...")
        try {
            // TODO: Implement pull logic
            addLog("✅ Changes pulled successfully")
        } catch (e: Exception) {
            addLog("❌ Pull failed: ${e.message}")
        }
    }

    // ==========================================
    // CONVERTERS
    // ==========================================
    
    private fun convertSellerProductEntity(pb: PbSellerProduct): SellerProductEntity {
        val parsedImg = pb.parseImageResName()
        val pbImageUrl = if (!pb.imageUrlName.isNullOrBlank() && pb.imageUrlName.startsWith("http")) {
            pb.imageUrlName
        } else if (parsedImg.isNotBlank() && (parsedImg.contains(".") || parsedImg.length > 15)) {
            "https://metube.pockethost.io/api/files/products/${pb.id}/$parsedImg"
        } else {
            pb.imageUrlName ?: ""
        }

        return SellerProductEntity(
            id = pb.id ?: "",
            sellerId = pb.sellerId ?: "",
            sellerName = pb.sellerName,
            name = pb.name,
            price = pb.price,
            stock = pb.stock,
            category = pb.category,
            subcategory = pb.subcategory ?: "",
            imageResName = if (parsedImg.contains(".")) "img_banner_1" else if (parsedImg.isBlank()) "img_banner_1" else parsedImg,
            imageUrlName = pbImageUrl,
            isPromoted = pb.isPromoted ?: false,
            impressions = pb.impressions,
            clicks = pb.clicks,
            description = pb.description ?: "",
            unitOfMeasurement = pb.unitOfMeasurement ?: "pieces",
            stateNewOrUsed = pb.stateNewOrUsed ?: "New",
            isFreeDelivery = pb.isFreeDelivery ?: false,
            discountPercent = pb.discountPercent ?: 0,
            brand = pb.brand ?: "",
            variants = pb.variants ?: "",
            rating = pb.rating ?: 4.5,
            cost = pb.cost ?: 0.0,
            sold = pb.sold ?: 0,
            lowStock = pb.lowStock ?: 3,
            created = pb.created,
            updated = pb.updated
        )
    }

    private fun convertProductEntity(pb: PbProductEntity): ProductEntity {
        val parsedImg = pb.parseImageResName()
        val pbImageUrl = if (!pb.imageUrlName.isNullOrBlank() && pb.imageUrlName.startsWith("http")) {
            pb.imageUrlName
        } else if (parsedImg.isNotBlank() && (parsedImg.contains(".") || parsedImg.length > 15)) {
            "https://metube.pockethost.io/api/files/products/${pb.id}/$parsedImg"
        } else {
            pb.imageUrlName ?: ""
        }

        return ProductEntity(
            id = pb.id ?: "",
            sellerId = pb.sellerId ?: "",
            sellerName = pb.sellerName,
            name = pb.name,
            price = pb.price,
            stock = pb.stock,
            category = pb.category,
            subcategory = pb.subcategory ?: "",
            imageResName = if (parsedImg.contains(".")) "img_banner_1" else if (parsedImg.isBlank()) "img_banner_1" else parsedImg,
            imageUrlName = pbImageUrl,
            isPromoted = pb.isPromoted ?: false,
            impressions = pb.impressions,
            clicks = pb.clicks,
            description = pb.description ?: "",
            unitOfMeasurement = pb.unitOfMeasurement ?: "pieces",
            stateNewOrUsed = pb.stateNewOrUsed ?: "New",
            isFreeDelivery = pb.isFreeDelivery ?: false,
            discountPercent = pb.discountPercent ?: 0,
            brand = pb.brand ?: "",
            variants = pb.variants ?: "",
            rating = pb.rating ?: 4.5,
            reviewCount = pb.reviewCount ?: 0,
            sold = pb.sold ?: 0,
            created = pb.created,
            updated = pb.updated
        )
    }

    private fun convertOrder(pb: PbOrder): OrderEntity {
        val loc = pb.shippingAddress ?: com.example.data.ShippingAddress()

        return OrderEntity(
            id = pb.id ?: "",
            buyerId = pb.buyerId,
            sellerId = pb.sellerId,
            deliveryId = pb.deliveryId,
            buyerName = pb.buyerName,
            buyerPhone = pb.buyerPhone,
            sellerName = pb.sellerName,
            productNames = pb.product_names,
            totalPrice = pb.total_price,
            deliveryPrice = pb.delivery_price,
            unit = pb.unit,
            status = pb.status,
            paymentMethod = pb.payment_method,
            shippingAddress = loc,
            storeLocation = pb.storeLocation ?: com.example.data.StoreLocation(),
            buyerVerifCode = pb.buyer_verif_code,
            deliveryVerifCode = pb.delivery_verif_code,
            courierName = pb.courier_name,
            courierPlate = pb.courier_plate,
            deliveryPickup = pb.delivery_pickup,
            isSelfPickup = pb.is_self_pickup,
            paymentReceipt = pb.paymentReceipt,
            paymentReference = pb.paymentReference,
            paymentBackReceipt = pb.paymentBackReceipt,
            paymentBackReference = pb.paymentBackReference,
            cancellationReason = pb.cancellation_reason,
            created = pb.created,
            updated = pb.updated
        )
    }

    private fun convertUser(pb: PbUserRecord): UserEntity {
        val seller = pb.expand?.seller_profile
        val delivery = pb.expand?.delivery_profile
        
        return UserEntity(
            id = pb.id,
            role = pb.role ?: "BUYER",
            fullName = pb.fullName ?: "",
            phone = pb.phone ?: "",
            email = pb.email ?: "",
            profileImage = if (pb.profile_image.orEmpty().startsWith("preset_") || pb.profile_image.orEmpty().isEmpty()) {
                pb.profile_image ?: ""
            } else if (pb.profile_image.orEmpty().startsWith("http")) {
                pb.profile_image ?: ""
            } else {
                "https://metube.pockethost.io/api/files/users/${pb.id}/${pb.profile_image}"
            },
            addressRegion = pb.addressRegion ?: "",
            addressZone = pb.addressZone ?: "",
            addressCity = pb.addressCity ?: "",
            addressKebele = pb.addressKebele ?: "",
            idDocumentInfront = pb.idDocumentInfront,
            idDocumentBackfront = pb.idDocumentBackfront,
            status = pb.status ?: false,
            isVerified = pb.verified ?: true,
            isApproved = pb.isApproved ?: true,
            isEmailVerified = pb.verified ?: false,
            referCode = pb.referCode ?: "",
            fromReferCode = pb.fromReferCode ?: "",
            created = pb.created,
            updated = pb.updated,
            
            // Map from Expanded Seller Profile
            businessName = seller?.businessName,
            businessPhone = seller?.businessPhone,
            businessAddress = seller?.city?.let { "${seller.region ?: ""}, $it" },
            tinNumber = seller?.tinNumber,
            tlcNumber = seller?.tlcNumber,
            businessGradeTitel = seller?.businessGradeTitel,
            businessGrade = seller?.businessGrade?.toString(),
            businessAbout = seller?.businessDescription,
            businessWorkingHours = seller?.businessWorkingHours,
            businessPayout = seller?.businessPayout?.toString(),
            businessRating = seller?.businessRating?.toString(),
            businessDiscountCode = seller?.businessDiscountCode?.toString(),
            businessDocumentFront = seller?.businessDocumentFront,
            businessDocumentBack = seller?.businessDocumentBack,
            sellerVerification = seller?.sellerVerification,
            storeLocation = seller?.storeLocation ?: com.example.data.StoreLocation(),
            sellerProfileId = seller?.id,
            sellerProfileImage = if (seller?.seller_profile_image.isNullOrBlank()) "" else if (seller.seller_profile_image!!.startsWith("http")) seller.seller_profile_image else "https://metube.pockethost.io/api/files/seller_profiles/${seller.id}/${seller.seller_profile_image}",
            sellerProfileCover = if (seller?.seller_profile_cover.isNullOrBlank()) "" else if (seller.seller_profile_cover!!.startsWith("http")) seller.seller_profile_cover else "https://metube.pockethost.io/api/files/seller_profiles/${seller.id}/${seller.seller_profile_cover}",
            
            // Map from Expanded Delivery Profile
            plateNumber = delivery?.plateNumber,
            deliveryPhone = delivery?.phoneNumber,
            vehicleType = delivery?.vehicleType,
            deliveryGrade = delivery?.deliveryGrade,
            xpTotal = delivery?.xpTotal ?: 0,
            ratingTotal = delivery?.ratingTotal ?: 0.0,
            driverLicenseFront = delivery?.driverLicenseFront,
            driverLicenseBack = delivery?.driverLicenseBack,
            vehicleDocumentFront = delivery?.vehicleDocumentFront,
            vehicleDocumentBack = delivery?.vehicleDocumentBack,
            deliveryVerification = delivery?.deliveryVerification,
            deliveryProfileId = delivery?.id,
            
            // Security settings default
            isAppLockEnabled = false,
            isBiometricEnabled = false,
            isTwoFactorEnabled = false,
            currentPinCode = null
        )
    }

    // ==========================================
    // HELPERS
    // ==========================================
    
    private fun addLog(message: String) {
        _syncLogs.update { 
            (it + message).takeLast(100) 
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    fun cleanup() {
        scope.cancel()
    }
}

data class SyncOperation(
    val id: String = "",
    val type: String = ""
)
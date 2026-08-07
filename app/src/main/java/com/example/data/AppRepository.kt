package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.network.PocketBaseSyncEngine
import com.example.ui.utils.DebugLogManager
import android.util.Log
import com.example.network.AuthResult
import com.example.network.PbOrder
import com.example.network.PbOrderItem
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

class AppRepository(private val context: Context) {

    val syncEngine = PocketBaseSyncEngine.getInstance(context)

    private val _activeUserFlow = MutableStateFlow<UserEntity?>(null)
    val activeUserFlow: Flow<UserEntity?> = _activeUserFlow

    private val _allProductsFlow = MutableStateFlow<List<ProductEntity>>(emptyList())
    val allProductsFlow: Flow<List<ProductEntity>> = _allProductsFlow

    private val _allOrdersFlow = MutableStateFlow<List<OrderEntity>>(emptyList())
    val allOrdersFlow: Flow<List<OrderEntity>> = _allOrdersFlow

    private val _allSellerOrdersFlow = MutableStateFlow<List<SellerOrderEntity>>(emptyList())
    val allSellerOrdersFlow: Flow<List<SellerOrderEntity>> = _allSellerOrdersFlow

    private val _allDeliveryOrdersFlow = MutableStateFlow<List<DeliveryOrderEntity>>(emptyList())
    val allDeliveryOrdersFlow: Flow<List<DeliveryOrderEntity>> = _allDeliveryOrdersFlow

    private val _allAddressesFlow = MutableStateFlow<List<AddressEntity>>(emptyList())
    val allAddressesFlow: Flow<List<AddressEntity>> = _allAddressesFlow

    private val _allReviewsFlow = MutableStateFlow<List<ReviewEntity>>(emptyList())
    val allReviewsFlow: Flow<List<ReviewEntity>> = _allReviewsFlow

    private val _allNotificationsFlow = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val allNotificationsFlow: Flow<List<NotificationEntity>> = _allNotificationsFlow

    private val _allLoginHistoryFlow = MutableStateFlow<List<LoginHistoryEntity>>(emptyList())
    val allLoginHistoryFlow: Flow<List<LoginHistoryEntity>> = _allLoginHistoryFlow

    private val _sellerRevenueFlow = MutableStateFlow<List<SellerRevenue>>(emptyList())
    val sellerRevenueFlow: Flow<List<SellerRevenue>> = _sellerRevenueFlow

    private val _sellerInventoryFlow = MutableStateFlow<List<SellerProductInventory>>(emptyList())
    val sellerInventoryFlow: Flow<List<SellerProductInventory>> = _sellerInventoryFlow

    private val _sellerDailySalesFlow = MutableStateFlow<List<SellerDailySales>>(emptyList())
    val sellerDailySalesFlow: Flow<List<SellerDailySales>> = _sellerDailySalesFlow

    private val _sellerMonthlyStatsFlow = MutableStateFlow<List<SellerMonthlyStats>>(emptyList())
    val sellerMonthlyStatsFlow: Flow<List<SellerMonthlyStats>> = _sellerMonthlyStatsFlow

    private val _allSellerProductsFlow = MutableStateFlow<List<SellerProductEntity>>(emptyList())
    val allSellerProductsFlow: Flow<List<SellerProductEntity>> = _allSellerProductsFlow

    suspend fun saveSyncedSellerProduct(product: SellerProductEntity) = withContext(Dispatchers.IO) {
        val list = _allSellerProductsFlow.value.toMutableList()
        val index = list.indexOfFirst { it.id == product.id && product.id.isNotEmpty() }
        if (index >= 0) {
            list[index] = product
        } else {
            list.add(product)
        }
        _allSellerProductsFlow.value = list
        try {
            val dao = AppRoomDatabase.getInstance(context).sellerProductDao()
            dao.insertProduct(product.toEntity())
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error saving seller product in Room DB: ${e.message}", isError = true)
        }
    }

    private val sessionManager = SessionManager(context)

    fun syncPocketBase() {
        CoroutineScope(Dispatchers.IO).launch {
            syncEngine.forceSync()
        }
    }

    fun getSyncStatus(): Flow<Boolean> = syncEngine.isSyncing
    fun getLastSyncTime(): Flow<Long> = syncEngine.lastSyncTime
    fun getSyncLogs(): Flow<List<String>> = syncEngine.syncLogs
    fun getConnectionStatus(): Flow<String> = syncEngine.connectionStatus

    // SESSION MANAGEMENT
    fun saveUserSession(userId: String, phone: String, name: String, email: String, token: String, role: String = "BUYER") = sessionManager.saveUserSession(userId, phone, name, email, token, role)
    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
    fun hasValidSession(): Boolean = sessionManager.hasValidSession()
    fun getSessionData(): SessionData? = sessionManager.getSessionData()
    fun getUserId(): String? = sessionManager.getUserId()
    fun getAuthToken(): String? = sessionManager.getAuthToken()
    fun getUserRole(): String? = sessionManager.getUserRole()
    fun isSessionExpired(): Boolean = sessionManager.isSessionExpired()
    fun needsTokenRefresh(): Boolean = sessionManager.needsTokenRefresh()
    fun clearSession() = sessionManager.clearSession()
    fun isFirstOpen(): Boolean = sessionManager.isFirstOpen()
    fun setFirstOpen(firstOpen: Boolean) = sessionManager.setFirstOpen(firstOpen)
    fun isBiometricEnabled(): Boolean = sessionManager.isBiometricEnabled()
    fun setBiometricEnabled(enabled: Boolean) = sessionManager.setBiometricEnabled(enabled)
    fun updateToken(newToken: String) = sessionManager.updateToken(newToken)

    suspend fun getActiveUserWithSession(): UserEntity? = withContext(Dispatchers.IO) {
        if (!hasValidSession()) return@withContext null
        val cached = _activeUserFlow.value
        if (cached != null) return@withContext cached
        val sessionData = getSessionData()
        if (sessionData != null) {
            val user = UserEntity(
                id = sessionData.userId,
                fullName = sessionData.name ?: "",
                phone = sessionData.phone ?: "",
                email = sessionData.email ?: "",
                role = sessionData.role ?: "BUYER"
            )
            _activeUserFlow.value = user
            return@withContext user
        }
        return@withContext null
    }

    suspend fun getActiveUser(): UserEntity? = _activeUserFlow.value
    suspend fun getUserRoleFromDb(): String? = _activeUserFlow.value?.role

    suspend fun logout() = withContext(Dispatchers.IO) {
        clearSession()
        _activeUserFlow.value = null
        _allProductsFlow.value = emptyList()
        setFirstOpen(true)
    }

    suspend fun saveUserWithSession(user: UserEntity, token: String, phone: String = user.phone, name: String = user.fullName, email: String = user.email, role: String = user.role) = withContext(Dispatchers.IO) {
        _activeUserFlow.value = user
        saveUserSession(user.id, phone, name, email, token, role)
        saveUserToRoom(user)
    }

    suspend fun saveUser(user: UserEntity) = withContext(Dispatchers.IO) {
        _activeUserFlow.value = user
        saveUserToRoom(user)
    }
    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        _activeUserFlow.value = user
        saveUserToRoom(user)
    }

    suspend fun saveUserToRoom(user: UserEntity) = withContext(Dispatchers.IO) {
        try {
            val userDao = AppRoomDatabase.getInstance(context).userDao()
            userDao.insertUser(user.toEntity())
            DebugLogManager.log("AppRepository", "💾 Saved UserEntity to Room: ${user.id}")
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Failed to save user to Room: ${e.message}", e)
        }
    }

    suspend fun getUserFromRoom(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        try {
            val userDao = AppRoomDatabase.getInstance(context).userDao()
            userDao.getUserById(userId)?.toDomainModel()
        } catch (e: Exception) {
            Log.e("AppRepository", "❌ Failed to load user from Room: ${e.message}", e)
            null
        }
    }
    suspend fun addAddress(address: AddressEntity) = withContext(Dispatchers.IO) { _allAddressesFlow.value += address }
    suspend fun updateAddress(address: AddressEntity) = withContext(Dispatchers.IO) {}
    suspend fun deleteAddress(id: String) = withContext(Dispatchers.IO) {}
    suspend fun setAddressAsDefault(addressId: String) = withContext(Dispatchers.IO) {}

    fun getAddressesForUser(userId: String): Flow<List<AddressEntity>> = _allAddressesFlow.map { list -> list.filter { it.userId == userId } }
    
    suspend fun saveSyncedProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        val list = _allProductsFlow.value.toMutableList()
        val index = list.indexOfFirst { it.id == product.id && product.id.isNotEmpty() }
        if (index >= 0) {
            list[index] = product
        } else {
            list.add(product)
        }
        _allProductsFlow.value = list
        CacheManager.getInstance().put("all_products", list, 15 * 60 * 1000L)
    }

    suspend fun saveSellerProduct(product: SellerProductEntity): Boolean = withContext(Dispatchers.IO) {
        val list = _allSellerProductsFlow.value.toMutableList()
        val index = list.indexOfFirst { it.id == product.id && product.id.isNotEmpty() }
        val isUpdate = product.id.isNotBlank() && (!product.id.startsWith("prod_") || index >= 0)
        if (isUpdate) {
            if (index >= 0) {
                list[index] = product
            } else {
                list.add(product)
            }
            _allSellerProductsFlow.value = list
            updateSellerProduct(product)
        } else {
            list.add(product)
            _allSellerProductsFlow.value = list
            addSellerProduct(product)
        }
    }

    private fun sanitizePbSellerId(productSellerId: String): String? {
        if (productSellerId.isBlank() || productSellerId.startsWith("seller_") || productSellerId.startsWith("usr_") || productSellerId.startsWith("prod_")) return null
        if (productSellerId.length == 15 && productSellerId.all { it.isLetterOrDigit() }) {
            val activeUser = _activeUserFlow.value
            if (activeUser != null) {
                if (!activeUser.sellerProfileId.isNullOrBlank() && activeUser.sellerProfileId.length == 15) {
                    return activeUser.sellerProfileId
                }
                if (productSellerId == activeUser.id) {
                    // User account ID from users collection, not seller_profiles collection
                    return null
                }
            }
            return productSellerId
        }
        return null
    }

    suspend fun addSellerProduct(product: SellerProductEntity): Boolean = withContext(Dispatchers.IO) {
        val currentList = _allSellerProductsFlow.value.toMutableList()
        if (!currentList.contains(product)) {
            currentList.add(product)
            _allSellerProductsFlow.value = currentList
        }

        try {
            val dao = AppRoomDatabase.getInstance(context).sellerProductDao()
            dao.insertProduct(product.toEntity())
        } catch (dbEx: Exception) {
            DebugLogManager.log("AppRepository", "Error storing added seller product in Room DB: ${dbEx.message}", isError = true)
        }

        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val createMap = mutableMapOf<String, Any?>(
                "name" to product.name,
                "price" to product.price,
                "cost" to product.cost,
                "stock" to product.stock,
                "category" to product.category,
                "subcategory" to product.subcategory,
                "imageUrlName" to product.imageUrlName,
                "isPromoted" to product.isPromoted,
                "impressions" to product.impressions,
                "clicks" to product.clicks,
                "description" to product.description,
                "unitOfMeasurement" to product.unitOfMeasurement,
                "stateNewOrUsed" to product.stateNewOrUsed,
                "isFreeDelivery" to product.isFreeDelivery,
                "discountPercent" to product.discountPercent,
                "brand" to product.brand,
                "variants" to product.variants,
                "sellerName" to product.sellerName,
                "rating" to product.rating,
                "sold" to product.sold
            )
            // Only include imageResName if it is a real file uploaded to PocketBase, not a local drawable preset (img_* / ic_*)
            if (!product.imageResName.isNullOrBlank() && !product.imageResName.startsWith("img_") && !product.imageResName.startsWith("ic_")) {
                createMap["imageResName"] = product.imageResName
            }
            val pbSellerId = sanitizePbSellerId(product.sellerId)
            if (pbSellerId != null) {
                createMap["sellerId"] = pbSellerId
            }
            val response = pbClient.getApi().createSellerProduct(createMap)
            if (response.isSuccessful) {
                val createdId = response.body()?.get("id")?.asString ?: ""
                DebugLogManager.log("AppRepository", "✅ Saved product on PocketBase! ID: $createdId")
                
                val updatedList = _allSellerProductsFlow.value.map {
                    if (it === product || (it.id == product.id && product.id.isNotEmpty())) {
                        it.copy(id = if (createdId.isNotBlank()) createdId else it.id)
                    } else {
                        it
                    }
                }
                _allSellerProductsFlow.value = updatedList
                
                // Update Room with new ID if created
                if (createdId.isNotBlank()) {
                    try {
                        val dao = AppRoomDatabase.getInstance(context).sellerProductDao()
                        val updatedProd = updatedList.find { it.id == createdId }
                        if (updatedProd != null) {
                            dao.insertProduct(updatedProd.toEntity())
                        }
                    } catch (e: Exception) {}
                }
            } else {
                val errStr = response.errorBody()?.string() ?: ""
                Log.e("AppRepository", "PocketBase createProduct returned code ${response.code()}: $errStr")
                DebugLogManager.log("AppRepository", "❌ Create product failed [${response.code()}]: $errStr", isError = true)
            }
            true
        } catch (e: Exception) {
            Log.e("AppRepository", "Error adding product on PocketBase", e)
            true
        }
    }

    suspend fun updateSellerProduct(product: SellerProductEntity): Boolean = withContext(Dispatchers.IO) {
        val currentList = _allSellerProductsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == product.id }
        if (index >= 0) {
            currentList[index] = product
            _allSellerProductsFlow.value = currentList
        }

        try {
            val dao = AppRoomDatabase.getInstance(context).sellerProductDao()
            dao.insertProduct(product.toEntity())
        } catch (dbEx: Exception) {
            DebugLogManager.log("AppRepository", "Error storing updated seller product in Room DB: ${dbEx.message}", isError = true)
        }

        try {
            if (product.id.isNotEmpty() && !product.id.startsWith("prod_")) {
                val pbClient = com.example.network.PocketBaseClient(context)
                val updateMap = mutableMapOf<String, Any?>(
                    "name" to product.name,
                    "price" to product.price,
                    "cost" to product.cost,
                    "stock" to product.stock,
                    "category" to product.category,
                    "subcategory" to product.subcategory,
                    "imageUrlName" to product.imageUrlName,
                    "isPromoted" to product.isPromoted,
                    "description" to product.description,
                    "unitOfMeasurement" to product.unitOfMeasurement,
                    "stateNewOrUsed" to product.stateNewOrUsed,
                    "isFreeDelivery" to product.isFreeDelivery,
                    "discountPercent" to product.discountPercent,
                    "brand" to product.brand,
                    "variants" to product.variants,
                    "sellerName" to product.sellerName,
                    "rating" to product.rating,
                    "sold" to product.sold,
                    "lowStock" to product.lowStock
                )
                // Only include imageResName if it is a real file uploaded to PocketBase, not a local drawable preset (img_* / ic_*)
                if (!product.imageResName.isNullOrBlank() && !product.imageResName.startsWith("img_") && !product.imageResName.startsWith("ic_")) {
                    updateMap["imageResName"] = product.imageResName
                }
                val pbSellerId = sanitizePbSellerId(product.sellerId)
                if (pbSellerId != null) {
                    updateMap["sellerId"] = pbSellerId
                }
                val response = pbClient.getApi().updateSellerProduct(product.id, updateMap)
                if (response.isSuccessful) {
                    DebugLogManager.log("AppRepository", "✅ Updated product on PocketBase! ID: ${product.id}")
                } else {
                    val errStr = response.errorBody()?.string() ?: ""
                    Log.e("AppRepository", "PocketBase updateSellerProduct returned code ${response.code()}: $errStr")
                    DebugLogManager.log("AppRepository", "❌ Update product failed [${response.code()}]: $errStr", isError = true)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("AppRepository", "Error updating product on PocketBase", e)
            true
        }
    }

    suspend fun deleteSellerProduct(id: String) = withContext(Dispatchers.IO) {
        val currentList = _allSellerProductsFlow.value.filter { it.id != id }
        _allSellerProductsFlow.value = currentList

        try {
            val dao = AppRoomDatabase.getInstance(context).sellerProductDao()
            dao.deleteProduct(id)
        } catch (dbEx: Exception) {
            DebugLogManager.log("AppRepository", "Error deleting seller product from Room DB: ${dbEx.message}", isError = true)
        }

        try {
            if (id.isNotEmpty() && !id.startsWith("prod_")) {
                val pbClient = com.example.network.PocketBaseClient(context)
                pbClient.getApi().deleteSellerProduct(id)
                DebugLogManager.log("AppRepository", "🗑️ Deleted product on PocketBase! ID: $id")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error deleting product from PocketBase", e)
        }
    }
    suspend fun getCachedProducts(): List<ProductEntity> = _allProductsFlow.value

    suspend fun saveOrder(order: OrderEntity) = withContext(Dispatchers.IO) {
        _allOrdersFlow.value += order
        val sellerOrder = order.toSellerOrder()
        val currentS = _allSellerOrdersFlow.value.toMutableList()
        val sIdx = currentS.indexOfFirst { it.id == sellerOrder.id }
        if (sIdx >= 0) currentS[sIdx] = sellerOrder else currentS.add(sellerOrder)
        _allSellerOrdersFlow.value = currentS
        _allDeliveryOrdersFlow.value += order.toDeliveryOrder()

        try {
            val dbDao = AppRoomDatabase.getInstance(context).sellerOrderDao()
            dbDao.insertOrder(sellerOrder.toDbEntity())
        } catch (e: Exception) {
            Log.e("AppRepository", "Error persisting order to Room DB: ${e.message}")
        }
    }
    suspend fun updateOrder(order: OrderEntity): OrderEntity = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val fields = mutableMapOf<String, Any>(
                "status" to order.status,
                "updated" to getCurrentTimestamp()
            )
            order.deliveryId?.let { fields["deliveryId"] = it }
            order.courierName?.let { fields["courier_name"] = it }
            order.courierPlate?.let { fields["courier_plate"] = it }
            order.deliveryVerifCode?.let { fields["delivery_verif_code"] = it }
            
            var updatedBuyerVerifCode = order.buyerVerifCode
            if (order.status == "READY_FOR_PICKUP" && order.isSelfPickup && updatedBuyerVerifCode.isNullOrBlank()) {
                updatedBuyerVerifCode = (100000..999999).random().toString()
            } else if (order.status == "OUT_FOR_DELIVERY" && updatedBuyerVerifCode.isNullOrBlank()) {
                updatedBuyerVerifCode = (100000..999999).random().toString()
            }
            updatedBuyerVerifCode?.let { fields["buyer_verif_code"] = it }
            
            pbClient.getApi().updateOrder(order.id, fields)
            
            val finalOrder = order.copy(buyerVerifCode = updatedBuyerVerifCode)
            
            // Update local flows
            updateLocalOrderStatus(
                orderId = finalOrder.id,
                newStatus = finalOrder.status,
                buyerVerifCode = finalOrder.buyerVerifCode,
                deliveryVerifCode = finalOrder.deliveryVerifCode
            )
            
            if (finalOrder.deliveryId != null) {
                updateLocalOrderCourierInfo(
                    orderId = finalOrder.id,
                    deliveryId = finalOrder.deliveryId,
                    courierName = finalOrder.courierName ?: "",
                    courierPlate = finalOrder.courierPlate ?: "",
                    courierPhone = "+251911223344",
                    deliveryVerifCode = finalOrder.deliveryVerifCode
                )
            }
            
            DebugLogManager.log("AppRepository", "✅ Order ${finalOrder.id} successfully updated on PB and locally.")
            finalOrder
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to updateOrder: ${e.message}", isError = true)
            order
        }
    }
    
    fun getChatMessages(roomId: String): Flow<List<ChatMessageEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun saveChatMessage(msg: ChatMessageEntity) = withContext(Dispatchers.IO) {}
    suspend fun likeMessage(msgId: String, isLiked: Boolean) = withContext(Dispatchers.IO) {}
    suspend fun reactToMessage(msgId: String, emoji: String) = withContext(Dispatchers.IO) {}
    suspend fun deleteMessage(msgId: String) = withContext(Dispatchers.IO) {}
    
    suspend fun saveReview(review: ReviewEntity) = withContext(Dispatchers.IO) { _allReviewsFlow.value += review }
    suspend fun saveNotification(notification: NotificationEntity) = withContext(Dispatchers.IO) { _allNotificationsFlow.value += notification }
    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {}
    
    suspend fun addLoginHistory(login: LoginHistoryEntity) = withContext(Dispatchers.IO) { _allLoginHistoryFlow.value += login }
    suspend fun clearLoginHistoryForUser(userId: String) = withContext(Dispatchers.IO) {}
    
    suspend fun getProductsByCategory(category: String): List<ProductEntity> = _allProductsFlow.value.filter { it.category == category }
    suspend fun getSellerProducts(sellerId: String): List<SellerProductEntity> = _allSellerProductsFlow.value.filter { it.sellerId == sellerId }
    suspend fun getDeliveryOrders(deliveryId: String): List<DeliveryOrderEntity> = _allDeliveryOrdersFlow.value.filter { it.deliveryId == deliveryId }
    
    suspend fun getProductById(id: String): ProductEntity? = _allProductsFlow.value.find { it.id == id }
    suspend fun getOrderById(id: String): OrderEntity? = _allOrdersFlow.value.find { it.id == id }
    
    suspend fun invalidateProductCache() = withContext(Dispatchers.IO) { _allProductsFlow.value = emptyList() }
    suspend fun invalidateSellerCache(sellerId: String) = withContext(Dispatchers.IO) {}
    suspend fun invalidateOrdersCache() = withContext(Dispatchers.IO) {
        _allOrdersFlow.value = emptyList()
        _allSellerOrdersFlow.value = emptyList()
        _allDeliveryOrdersFlow.value = emptyList()
    }
    
    suspend fun getCachedSellerAnalytics(sellerId: String): SellerAnalytics = withContext(Dispatchers.IO) {
        val orders = getCachedSellerOrders(sellerId)
        val products = getCachedSellerProducts(sellerId)
        val validOrders = orders.filter { !it.status.equals("CANCELLED", ignoreCase = true) }
        val totalRevenue = validOrders.sumOf { it.totalPrice }
        SellerAnalytics(
            totalSales = totalRevenue,
            totalOrders = validOrders.size,
            totalProducts = products.size
        )
    }
    suspend fun prepopulateIfNeeded() {}
    suspend fun completeTour() {}

    
    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun getInstance(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) { AppRepository(context).also { INSTANCE = it } }
    }

    fun checkAuthStatus(): Boolean = hasValidSession()
    suspend fun getCachedSellerProducts(sellerId: String): List<SellerProductEntity> {
        var all = _allSellerProductsFlow.value
        if (all.isEmpty()) {
            try {
                val dao = AppRoomDatabase.getInstance(context).sellerProductDao()
                val dbEntities = if (sellerId.isNotBlank()) dao.getProductsBySeller(sellerId) else dao.getAllProducts()
                all = dbEntities.map { it.toDomainModel() }
                if (all.isNotEmpty()) {
                    _allSellerProductsFlow.value = all
                }
            } catch (e: Exception) {
                DebugLogManager.log("AppRepository", "Error reading cached seller products from Room DB: ${e.message}", isError = true)
            }
        }
        val activeUser = _activeUserFlow.value
        val sellerProfileId = activeUser?.sellerProfileId
        val userBizName = activeUser?.businessName?.trim()
        val userFullName = activeUser?.fullName?.trim()
        val filtered = all.filter { prod ->
            (sellerId.isNotBlank() && prod.sellerId == sellerId) ||
            (!sellerProfileId.isNullOrBlank() && prod.sellerId == sellerProfileId) ||
            (!userBizName.isNullOrBlank() && prod.sellerName.equals(userBizName, ignoreCase = true)) ||
            (!userFullName.isNullOrBlank() && prod.sellerName.equals(userFullName, ignoreCase = true)) ||
            sellerId.isBlank()
        }
        return if (filtered.isNotEmpty()) filtered else all
    }

    suspend fun getCachedSellerOrders(sellerId: String): List<SellerOrderEntity> = withContext(Dispatchers.IO) {
        val activeUser = _activeUserFlow.value
        val sellerProfileId = if (sellerId.isNotBlank()) sellerId else (activeUser?.sellerProfileId ?: "")
        
        try {
            val dbDao = AppRoomDatabase.getInstance(context).sellerOrderDao()
            val dbOrders = if (sellerProfileId.isNotBlank()) {
                dbDao.getOrdersBySeller(sellerProfileId).map { it.toDomainModel() }
            } else {
                dbDao.getAllOrders().map { it.toDomainModel() }
            }
            if (dbOrders.isNotEmpty()) {
                val current = _allSellerOrdersFlow.value.toMutableList()
                dbOrders.forEach { sOrder ->
                    val idx = current.indexOfFirst { it.id == sOrder.id }
                    if (idx >= 0) current[idx] = sOrder else current.add(sOrder)
                }
                _allSellerOrdersFlow.value = current
            }
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Failed to read cached seller orders from Room DB: ${e.message}")
        }

        val all = _allSellerOrdersFlow.value
        val filtered = all.filter { order ->
            (sellerProfileId.isNotBlank() && order.sellerId == sellerProfileId) ||
            (sellerId.isNotBlank() && order.sellerId == sellerId)
        }
        if (filtered.isNotEmpty()) filtered else all
    }
    fun updateLoginHistoryList(list: List<LoginHistoryEntity>) {
        _allLoginHistoryFlow.value = list
    }
    
    suspend fun sendChatMessage(
        roomId: String,
        senderName: String,
        senderRole: String,
        messageText: String,
        attachmentUrl: String? = null,
        attachmentType: String? = null,
        replyToMessageId: String? = null,
        replyToMessageText: String? = null
    ) {}

    suspend fun updateMessageLiked(msgId: String, isLiked: Boolean) {}
    suspend fun updateMessageReaction(msgId: String, emoji: String?) {}
    suspend fun deleteMessageById(msgId: String) {}

    // ==========================================
    // REVIEWS PRODUCTS CRUD
    // ==========================================

    suspend fun getProductReviews(productId: String): List<ProductReviewEntity> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val response = pbClient.getApi().getProductReviews(filter = "productId='$productId'")
            response.items.map { pb ->
                ProductReviewEntity(
                    id = pb.id,
                    productId = pb.productId,
                    userId = pb.userId,
                    userName = pb.userName,
                    userAvatar = pb.userAvatar,
                    rating = pb.rating,
                    comment = pb.comment,
                    created = pb.created,
                    updated = pb.updated
                )
            }
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error fetching reviews: ${e.message}", isError = true)
            emptyList<ProductReviewEntity>()
        }
    }

    suspend fun addProductReview(review: ProductReviewEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val map = mapOf(
                "productId" to review.productId,
                "userId" to review.userId,
                "userName" to review.userName,
                "userAvatar" to review.userAvatar,
                "rating" to review.rating,
                "comment" to review.comment
            )
            val resp = pbClient.getApi().createProductReview(map)
            resp.isSuccessful
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error adding review: ${e.message}", isError = true)
            false
        }
    }

    suspend fun updateProductReview(reviewId: String, rating: Double, comment: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val map = mapOf(
                "rating" to rating,
                "comment" to comment
            )
            val resp = pbClient.getApi().updateProductReview(reviewId, map)
            resp.isSuccessful
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error updating review: ${e.message}", isError = true)
            false
        }
    }

    suspend fun deleteProductReview(reviewId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val resp = pbClient.getApi().deleteProductReview(reviewId)
            resp.isSuccessful
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error deleting review: ${e.message}", isError = true)
            false
        }
    }

    // ==========================================
    // REVIEWS STORES CRUD
    // ==========================================

    suspend fun getStoreReviews(): List<com.example.ui.utils.ReviewItem> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val response = pbClient.getApi().getStoreReviews()
            if (response.items.isEmpty()) {
                // If remote collection is empty, seed/fallback with standard items
                return@withContext listOf(
                    com.example.ui.utils.ReviewItem("Amina F.", "★5.0", "The Handwoven Kemis is gorgeous! Absolute masterpiece quality.", "June 28, 2026", "Handcarved Habesha Dress (Kemis) x1", null, id = "seed1"),
                    com.example.ui.utils.ReviewItem("Guled F.", "★5.0", "Incredible Teff quality, clean and fresh. Highly recommended!", "June 25, 2026", "Premium Magna Teff (25kg)", null, id = "seed2"),
                    com.example.ui.utils.ReviewItem("Hodan A.", "★4.8", "Fast response, very polite. Sidamo coffee is wonderfully roasted.", "June 22, 2026", "Sidamo Organic Coffee Beans (1kg) x5", null, id = "seed3"),
                    com.example.ui.utils.ReviewItem("Cumar C.", "★5.0", "Professional seller. Easy transaction via Telebirr.", "June 19, 2026", "Traditional Clay Jebena x1", "Thank you, Cumar! We appreciate your support.", id = "seed4"),
                    com.example.ui.utils.ReviewItem("Bashiir C.", "★3.0", "The delivery was a bit late but the coffee smells fresh.", "June 12, 2026", "Sidamo Organic Coffee Beans (1kg) x5", null, id = "seed5")
                )
            }
            response.items.map { pb ->
                val ratingStr = "★" + String.format(java.util.Locale.US, "%.1f", pb.rating)
                val displayDate = if (!pb.created.isNullOrBlank()) {
                    try {
                        val ymd = pb.created.substringBefore("T")
                        val parts = ymd.split("-")
                        if (parts.size == 3) {
                            val year = parts[0]
                            val monthInt = parts[1].toIntOrNull() ?: 1
                            val day = parts[2].toIntOrNull() ?: 1
                            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            val monthStr = if (monthInt in 1..12) months[monthInt - 1] else "Jan"
                            "$monthStr $day, $year"
                        } else {
                            pb.created.substringBefore(" ")
                        }
                    } catch (_: Exception) {
                        pb.created.substringBefore(" ")
                    }
                } else {
                    "Just now"
                }
                
                com.example.ui.utils.ReviewItem(
                    id = pb.id,
                    name = pb.userName,
                    rating = ratingStr,
                    comment = pb.comment,
                    date = displayDate,
                    product = pb.productId ?: "General Store",
                    replyText = pb.replyText,
                    userId = pb.userId,
                    userAvatar = pb.userAvatar,
                    productId = pb.productId
                )
            }
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error fetching store reviews: ${e.message}", isError = true)
            listOf(
                com.example.ui.utils.ReviewItem("Amina F.", "★5.0", "The Handwoven Kemis is gorgeous! Absolute masterpiece quality.", "June 28, 2026", "Handcarved Habesha Dress (Kemis) x1", null, id = "seed1"),
                com.example.ui.utils.ReviewItem("Guled F.", "★5.0", "Incredible Teff quality, clean and fresh. Highly recommended!", "June 25, 2026", "Premium Magna Teff (25kg)", null, id = "seed2"),
                com.example.ui.utils.ReviewItem("Hodan A.", "★4.8", "Fast response, very polite. Sidamo coffee is wonderfully roasted.", "June 22, 2026", "Sidamo Organic Coffee Beans (1kg) x5", null, id = "seed3"),
                com.example.ui.utils.ReviewItem("Cumar C.", "★5.0", "Professional seller. Easy transaction via Telebirr.", "June 19, 2026", "Traditional Clay Jebena x1", "Thank you, Cumar! We appreciate your support.", id = "seed4"),
                com.example.ui.utils.ReviewItem("Bashiir C.", "★3.0", "The delivery was a bit late but the coffee smells fresh.", "June 12, 2026", "Sidamo Organic Coffee Beans (1kg) x5", null, id = "seed5")
            )
        }
    }

    suspend fun saveStoreReview(review: com.example.ui.utils.ReviewItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val cleanRating = review.rating.replace("★", "").trim().toDoubleOrNull() ?: 5.0
            val map = mapOf(
                "productId" to review.productId,
                "userId" to (review.userId ?: "guest"),
                "userName" to review.name,
                "userAvatar" to (review.userAvatar ?: "avatar1.png"),
                "rating" to cleanRating,
                "comment" to review.comment,
                "replyText" to review.replyText
            )
            val resp = pbClient.getApi().createStoreReview(map)
            resp.isSuccessful
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error saving store review: ${e.message}", isError = true)
            false
        }
    }

    suspend fun updateStoreReviewReply(reviewId: String, replyText: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val map = mapOf(
                "replyText" to replyText,
                "updated" to getCurrentTimestamp()
            )
            val resp = pbClient.getApi().updateStoreReview(reviewId, map)
            resp.isSuccessful
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error updating store review reply: ${e.message}", isError = true)
            false
        }
    }

    // ==========================================
    // SELLER REVENUE (PocketBase View)
    // ==========================================

    suspend fun getSellerRevenue(
        sellerId: String? = null,
        week: String? = null,
        month: String? = null
    ): List<SellerRevenue> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val filters = mutableListOf<String>()
            if (!sellerId.isNullOrBlank()) filters.add("seller_id='$sellerId'")
            if (!week.isNullOrBlank()) filters.add("week='$week'")
            if (!month.isNullOrBlank()) {
                filters.add("month='$month'")
            } else {
                val currentYM = java.time.YearMonth.now()
                val pastMonthIso = currentYM.minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                filters.add("month>='$pastMonthIso'")
            }
            val filter = if (filters.isNotEmpty()) filters.joinToString(" && ") else null
            val response = pbClient.getApi().getSellerRevenue(filter = filter)
            val list = response.items.map { it.toDomainModel() }
            _sellerRevenueFlow.value = list

            // Store on Room/SQLite Local Database
            try {
                val dao = AppRoomDatabase.getInstance(context).sellerRevenueDao()
                dao.insertAll(list.map { it.toEntity() })
            } catch (dbEx: Exception) {
                DebugLogManager.log("AppRepository", "Error storing seller revenue in Room DB: ${dbEx.message}", isError = true)
            }

            list
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error fetching seller revenue: ${e.message}", isError = true)
            // Fallback to Room DB cache
            try {
                val dao = AppRoomDatabase.getInstance(context).sellerRevenueDao()
                val cached = if (!sellerId.isNullOrBlank()) {
                    dao.getRevenueBySeller(sellerId)
                } else {
                    dao.getAllRevenue()
                }.map { it.toDomainModel() }
                if (cached.isNotEmpty()) {
                    _sellerRevenueFlow.value = cached
                    return@withContext cached
                }
            } catch (cacheEx: Exception) {
                DebugLogManager.log("AppRepository", "Error reading cached seller revenue from Room DB: ${cacheEx.message}", isError = true)
            }
            emptyList()
        }
    }

    // ==========================================
    // SELLER PRODUCT INVENTORY (PocketBase View)
    // ==========================================

    suspend fun getSellerProductInventory(sellerId: String? = null): List<SellerProductInventory> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val filter = if (!sellerId.isNullOrBlank()) "sellerId='$sellerId'" else null
            val response = pbClient.getApi().getSellerProductInventory(filter = filter)
            val list = response.items.map { it.toDomainModel() }
            _sellerInventoryFlow.value = list
            list
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error fetching seller product inventory: ${e.message}", isError = true)
            emptyList()
        }
    }

    // Removed updateSellerInventoryProductPrice as it is no longer used

    // ==========================================
    // SELLER DAILY SALES (PocketBase View)
    // ==========================================

    suspend fun getSellerDailySales(
        sellerId: String? = null,
        date: String? = null
    ): List<SellerDailySales> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val filters = mutableListOf<String>()
            if (!sellerId.isNullOrBlank()) filters.add("seller_id='$sellerId'")
            if (!date.isNullOrBlank()) {
                filters.add("sale_date ~ '$date'")
            } else {
                val past2monthDate = java.time.LocalDate.now().minusMonths(2).withDayOfMonth(1).toString()
                filters.add("sale_date >= '$past2monthDate'")
            }
            val filter = if (filters.isNotEmpty()) filters.joinToString(" && ") else null
            val response = pbClient.getApi().getSellerDailySales(filter = filter)
            val list = response.items.map { it.toDomainModel() }
            _sellerDailySalesFlow.value = list

            // Store on Room/SQLite Local Database
            try {
                val dao = AppRoomDatabase.getInstance(context).sellerDailySalesDao()
                dao.insertAll(list.map { it.toEntity() })
            } catch (dbEx: Exception) {
                DebugLogManager.log("AppRepository", "Error storing seller daily sales in Room DB: ${dbEx.message}", isError = true)
            }

            list
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error fetching seller daily sales: ${e.message}", isError = true)
            // Fallback to Room DB cache
            try {
                val dao = AppRoomDatabase.getInstance(context).sellerDailySalesDao()
                val cached = if (!sellerId.isNullOrBlank()) {
                    dao.getDailySalesBySeller(sellerId)
                } else {
                    dao.getAllDailySales()
                }.map { it.toDomainModel() }
                if (cached.isNotEmpty()) {
                    _sellerDailySalesFlow.value = cached
                    return@withContext cached
                }
            } catch (cacheEx: Exception) {
                DebugLogManager.log("AppRepository", "Error reading cached daily sales from Room DB: ${cacheEx.message}", isError = true)
            }
            emptyList()
        }
    }

    // ==========================================
    // SELLER MONTHLY STATS (PocketBase View)
    // ==========================================

    suspend fun getSellerMonthlyStats(sellerId: String? = null): List<SellerMonthlyStats> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val filters = mutableListOf<String>()
            if (!sellerId.isNullOrBlank()) filters.add("seller_id='$sellerId'")
            
            val currentYM = java.time.YearMonth.now()
            val startMonthIso = currentYM.minusMonths(5).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            filters.add("month>='$startMonthIso'")

            val filter = if (filters.isNotEmpty()) filters.joinToString(" && ") else null
            val response = pbClient.getApi().getSellerMonthlyStats(filter = filter)
            val list = response.items.map { it.toDomainModel() }
            _sellerMonthlyStatsFlow.value = list

            // Store on Room/SQLite Local Database
            try {
                val dao = AppRoomDatabase.getInstance(context).sellerMonthlyStatsDao()
                dao.insertAll(list.map { it.toEntity() })
            } catch (dbEx: Exception) {
                DebugLogManager.log("AppRepository", "Error storing seller monthly stats in Room DB: ${dbEx.message}", isError = true)
            }

            list
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Error fetching seller monthly stats: ${e.message}", isError = true)
            // Fallback to Room DB cache
            try {
                val dao = AppRoomDatabase.getInstance(context).sellerMonthlyStatsDao()
                val cached = if (!sellerId.isNullOrBlank()) {
                    dao.getMonthlyStatsBySeller(sellerId)
                } else {
                    dao.getAllMonthlyStats()
                }.map { it.toDomainModel() }
                if (cached.isNotEmpty()) {
                    _sellerMonthlyStatsFlow.value = cached
                    return@withContext cached
                }
            } catch (cacheEx: Exception) {
                DebugLogManager.log("AppRepository", "Error reading cached monthly stats from Room DB: ${cacheEx.message}", isError = true)
            }
            emptyList()
        }
    }


    // ==========================================
    // ORDER MANAGEMENT (PocketBase Sync)
    // ==========================================
    
    suspend fun createOrderWithItems(
        buyerId: String,
        sellerId: String,
        items: List<CartItem>,
        paymentMethod: String = "Telebirr",
        isSelfPickup: Boolean = false,
        deliveryPickup: Boolean = true,
        deliveryPrice: Double = 0.0,
        shippingAddress: LocationShipping = LocationShipping(),
        sellerName: String = "",
        buyerName: String = "",
        buyerPhone: String = "",
        receiptTitle: String? = null,
        receiptDetails: String? = null
    ): AuthResult<OrderEntity> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            
            // 1. Build order items
            val orderItems = items.map { item ->
                OrderItemEntity(
                    productId = item.productId,
                    productName = item.productName,
                    category = item.category,
                    subcategory = item.subcategory,
                    brand = item.brand,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    subtotal = item.unitPrice * item.quantity,
                    unit = item.unit,
                    sellerId = sellerId,
                    buyerId = buyerId,
                    paymentMethod = paymentMethod
                )
            }
            
            // 2. Calculate total and details
            val totalPrice = orderItems.sumOf { it.subtotal }
            val productNames = orderItems.joinToString(", ") { it.productName }
            val primaryUnit = items.firstOrNull()?.unit?.ifBlank { "pieces" } ?: "pieces"

            val activeUser = getUserFromRoom(buyerId) ?: _activeUserFlow.value
            val finalBuyerName = buyerName.ifBlank { activeUser?.fullName.orEmpty() }
            val finalBuyerPhone = buyerPhone.ifBlank { activeUser?.phone.orEmpty() }
            val finalSellerName = sellerName.ifBlank { items.firstOrNull()?.sellerName.orEmpty() }
            
            // 3. Generate verification codes
            val buyerVerifCode = if (isSelfPickup) generateVerificationCode() else null
            val deliveryVerifCode = if (deliveryPickup) generateVerificationCode() else null
            
            // 4. Create order header in PocketBase
            val order = PbOrder(
                buyerId = buyerId,
                sellerId = sellerId,
                buyerName = finalBuyerName,
                buyerPhone = finalBuyerPhone,
                sellerName = finalSellerName,
                product_names = productNames,
                total_price = totalPrice,
                delivery_price = deliveryPrice,
                unit = primaryUnit,
                payment_method = paymentMethod,
                status = "PENDING",
                shippingAddress = shippingAddress,
                buyer_verif_code = buyerVerifCode,
                delivery_verif_code = deliveryVerifCode,
                is_self_pickup = isSelfPickup,
                delivery_pickup = deliveryPickup,
                timestamp = System.currentTimeMillis()
            )
            
            var createdOrder = pbClient.getApi().createOrder(order)
            val orderId = createdOrder.id ?: ""
            DebugLogManager.log("AppRepository", "✅ Order created: $orderId")

            if (!receiptTitle.isNullOrBlank() && !receiptDetails.isNullOrBlank()) {
                try {
                    DebugLogManager.log("AppRepository", "✍️ Generating dynamic receipt file...")
                    val receiptFile = generateReceiptImageFile(context, receiptTitle, receiptDetails)
                    DebugLogManager.log("AppRepository", "📤 Uploading receipt image to PocketBase: ${receiptFile.absolutePath}")
                    val fields = mapOf<String, okhttp3.RequestBody>()
                    val requestFile = receiptFile.asRequestBody("image/png".toMediaType())
                    val filePart = okhttp3.MultipartBody.Part.createFormData(
                        "paymentReceipt",
                        receiptFile.name,
                        requestFile
                    )
                    createdOrder = pbClient.getApi().updateOrderWithFiles(orderId, fields, listOf(filePart))
                    DebugLogManager.log("AppRepository", "✅ Receipt image uploaded successfully! Filename: ${createdOrder.paymentReceipt}")
                } catch (ex: Exception) {
                    DebugLogManager.log("AppRepository", "❌ Failed to upload payment receipt: ${ex.message}", isError = true)
                }
            }
            
            // 5. Create order_items in PocketBase with the orderId
            val pbOrderItems = orderItems.map { item ->
                PbOrderItem(
                    orderId = orderId,  // ✅ CRITICAL: Link to the created order
                    productId = item.productId,
                    productName = item.productName,
                    category = item.category,
                    subcategory = item.subcategory,
                    brand = item.brand,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    subtotal = item.subtotal,
                    unit = item.unit,
                    sellerId = item.sellerId,
                    buyerId = item.buyerId,
                    payment_method = item.paymentMethod
                )
            }
            
            // ✅ Create each order item in PocketBase
            val gson = com.google.gson.Gson()
            pbOrderItems.forEach { pbItem ->
                try {
                    val jsonStr = gson.toJson(pbItem)
                    DebugLogManager.log("AppRepository", "📤 Creating OrderItem JSON: $jsonStr")
                    val createdPbItem = pbClient.getApi().createOrderItem(pbItem)
                    DebugLogManager.log("AppRepository", "✅ Created order item: ${createdPbItem.productName}")
                } catch (e: retrofit2.HttpException) {
                    val errorBodyStr = e.response()?.errorBody()?.string() ?: e.message()
                    DebugLogManager.log("AppRepository", "❌ Failed to create order item (HTTP ${e.code()}): $errorBodyStr", isError = true)
                } catch (e: Exception) {
                    DebugLogManager.log("AppRepository", "❌ Failed to create order item: ${pbItem.productName} - ${e.message}", isError = true)
                }
            }
            DebugLogManager.log("AppRepository", "✅ Created ${pbOrderItems.size} order items")
            
            // 6. Fetch the complete order with items to return
            val completeOrder = getOrderWithItemsForRole(orderId, buyerId, "BUYER")
            
            if (completeOrder != null) {
                // 7. Update local flow
                val currentOrders = _allOrdersFlow.value.toMutableList()
                currentOrders.add(completeOrder)
                _allOrdersFlow.value = currentOrders

                val currentSellerOrders = _allSellerOrdersFlow.value.toMutableList()
                currentSellerOrders.add(completeOrder.toSellerOrder())
                _allSellerOrdersFlow.value = currentSellerOrders

                val currentDeliveryOrders = _allDeliveryOrdersFlow.value.toMutableList()
                currentDeliveryOrders.add(completeOrder.toDeliveryOrder())
                _allDeliveryOrdersFlow.value = currentDeliveryOrders
                
                AuthResult.Success(completeOrder)
            } else {
                // Fallback: return order without items
                val orderEntity = convertPbOrderToEntity(createdOrder, emptyList())
                AuthResult.Success(orderEntity)
            }
            
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to create order: ${e.message}", isError = true)
            AuthResult.Error(e.message ?: "Failed to create order")
        }
    }

    /**
    * Get buyer orders
    */
    suspend fun getBuyerOrders(buyerId: String): List<BuyerOrderEntity> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val filter = "buyerId='$buyerId'"
            val response = pbClient.getApi().getOrders(filter = filter, sort = "-created")
            
            val orders = response.items.mapNotNull { pbOrder ->
                try {
                    // ✅ Fetch order items for this order
                    val itemsResponse = pbClient.getApi().getOrderItems(filter = "orderId='${pbOrder.id ?: ""}'")
                    val items = itemsResponse.items.map { pbItem ->
                        OrderItemEntity(
                            id = pbItem.id ?: "",
                            orderId = pbItem.orderId,
                            productId = pbItem.productId,
                            productName = pbItem.productName,
                            category = pbItem.category,
                            subcategory = pbItem.subcategory,
                            brand = pbItem.brand,
                            quantity = pbItem.quantity,
                            unitPrice = pbItem.unitPrice,
                            subtotal = pbItem.subtotal,
                            unit = pbItem.unit,
                            sellerId = pbItem.sellerId,
                            buyerId = pbItem.buyerId,
                            paymentMethod = pbItem.payment_method,
                            createdAt = pbItem.created,
                            updatedAt = pbItem.updated
                        )
                    }
                    convertPbOrderToEntity(pbOrder, items)
                } catch (e: Exception) {
                    DebugLogManager.log("AppRepository", "⚠️ Failed to parse order ${pbOrder.id}: ${e.message}")
                    null
                }
            }
            
            // Update local flow for buyer
            val currentOrders = _allOrdersFlow.value.toMutableList()
            orders.forEach { order ->
                val index = currentOrders.indexOfFirst { it.id == order.id }
                if (index >= 0) {
                    currentOrders[index] = order
                } else {
                    currentOrders.add(order)
                }
            }
            _allOrdersFlow.value = currentOrders
            
            orders
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to get buyer orders: ${e.message}", isError = true)
            emptyList()
        }
    }


    suspend fun getOrderItemsForRole(
        orderId: String,
        userId: String,
        role: String
    ): List<OrderItemEntity> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val filter = "orderId='$orderId'"
            val response = pbClient.getApi().getOrderItems(filter = filter)
            
            val items = response.items.map { pbItem ->
                val item = OrderItemEntity(
                    id = pbItem.id ?: "",
                    orderId = pbItem.orderId,
                    productId = pbItem.productId,
                    productName = pbItem.productName,
                    category = pbItem.category,
                    subcategory = pbItem.subcategory,
                    brand = pbItem.brand,
                    quantity = pbItem.quantity,
                    unitPrice = pbItem.unitPrice,
                    subtotal = pbItem.subtotal,
                    unit = pbItem.unit,
                    sellerId = pbItem.sellerId,
                    buyerId = pbItem.buyerId,
                    paymentMethod = pbItem.payment_method,
                    createdAt = pbItem.created,
                    updatedAt = pbItem.updated
                )
                item
            }
            
            items
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to get order items: ${e.message}", isError = true)
            emptyList()
        }
    }

    suspend fun getOrderWithItemsForRole(
        orderId: String,
        userId: String,
        role: String
    ): OrderEntity? = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            
            // Get order
            val order = pbClient.getApi().getOrder(orderId)
            
            // ✅ Get items with the correct orderId
            val filter = "orderId='$orderId'"
            val response = pbClient.getApi().getOrderItems(filter = filter)
            
            val items = response.items.map { pbItem ->
                OrderItemEntity(
                    id = pbItem.id ?: "",
                    orderId = pbItem.orderId,
                    productId = pbItem.productId,
                    productName = pbItem.productName,
                    category = pbItem.category,
                    subcategory = pbItem.subcategory,
                    brand = pbItem.brand,
                    quantity = pbItem.quantity,
                    unitPrice = pbItem.unitPrice,
                    subtotal = pbItem.subtotal,
                    unit = pbItem.unit,
                    sellerId = pbItem.sellerId,
                    buyerId = pbItem.buyerId,
                    paymentMethod = pbItem.payment_method,
                    createdAt = pbItem.created,
                    updatedAt = pbItem.updated
                )
            }
            
            convertPbOrderToEntity(order, items)
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to get order with items: ${e.message}", isError = true)
            null
        }
    }

    /**
    * Get seller orders (by seller profile ID)
    */
    suspend fun getSellerOrders(sellerProfileId: String): List<SellerOrderEntity> = withContext(Dispatchers.IO) {
        val dbDao = try { AppRoomDatabase.getInstance(context).sellerOrderDao() } catch (e: Exception) { null }
        
        // 1. Instant Room DB read
        val cachedDbOrders = try {
            if (sellerProfileId.isNotBlank() && dbDao != null) {
                dbDao.getOrdersBySeller(sellerProfileId).map { it.toDomainModel() }
            } else {
                emptyList()
            }
        } catch (e: Exception) { emptyList() }

        if (cachedDbOrders.isNotEmpty()) {
            val current = _allSellerOrdersFlow.value.toMutableList()
            cachedDbOrders.forEach { sOrder ->
                val idx = current.indexOfFirst { it.id == sOrder.id }
                if (idx >= 0) current[idx] = sOrder else current.add(sOrder)
            }
            _allSellerOrdersFlow.value = current
        }

        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val filter = "sellerId='$sellerProfileId'"
            val response = pbClient.getApi().getOrders(filter = filter, sort = "-created")
            
            if (response.items.isEmpty()) {
                return@withContext cachedDbOrders
            }

            // Batch fetch ALL order items for this seller in 1 SINGLE request instead of 1+N requests!
            val itemsByOrderId = try {
                val itemsResponse = pbClient.getApi().getOrderItems(filter = "sellerId='$sellerProfileId'")
                itemsResponse.items.groupBy { it.orderId ?: "" }
            } catch (e: Exception) {
                emptyMap()
            }

            val orders = response.items.mapNotNull { pbOrder ->
                try {
                    val pbItems = itemsByOrderId[pbOrder.id ?: ""] ?: emptyList()
                    val items = pbItems.map { pbItem ->
                        OrderItemEntity(
                            id = pbItem.id ?: "",
                            orderId = pbItem.orderId,
                            productId = pbItem.productId,
                            productName = pbItem.productName,
                            quantity = pbItem.quantity,
                            unitPrice = pbItem.unitPrice,
                            subtotal = pbItem.subtotal,
                            unit = pbItem.unit
                        )
                    }
                    convertPbOrderToEntity(pbOrder, items)
                } catch (e: Exception) {
                    null
                }
            }
            
            val sellerOrders = orders.map { it.toSellerOrder() }

            // Save fetched orders to Room DB for instant offline loading next time
            try {
                dbDao?.insertAll(sellerOrders.map { it.toDbEntity() })
            } catch (e: Exception) {
                DebugLogManager.log("AppRepository", "Failed saving seller orders to Room: ${e.message}")
            }

            // Update local flow for seller
            val currentOrders = _allSellerOrdersFlow.value.toMutableList()
            sellerOrders.forEach { sOrder ->
                val index = currentOrders.indexOfFirst { it.id == sOrder.id }
                if (index >= 0) {
                    currentOrders[index] = sOrder
                } else {
                    currentOrders.add(sOrder)
                }
            }
            _allSellerOrdersFlow.value = currentOrders
            
            sellerOrders
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to get seller orders: ${e.message}", isError = true)
            cachedDbOrders
        }
    }

    /**
    * Get available delivery orders
    */
    suspend fun getAvailableDeliveryOrders(): List<DeliveryOrderEntity> = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val filter = "status='READY_FOR_PICKUP' && delivery_pickup=true && deliveryId=null"
            val response = pbClient.getApi().getOrders(filter = filter, sort = "-created")
            
            val deliveryOrders = response.items.mapNotNull { pbOrder ->
                try {
                    val itemsResponse = pbClient.getApi().getOrderItems(filter = "orderId='${pbOrder.id ?: ""}'")
                    val items = itemsResponse.items.map { pbItem ->
                        OrderItemEntity(
                            id = pbItem.id ?: "",
                            orderId = pbItem.orderId,
                            productId = pbItem.productId,
                            productName = pbItem.productName,
                            quantity = pbItem.quantity,
                            unitPrice = pbItem.unitPrice,
                            subtotal = pbItem.subtotal,
                            unit = pbItem.unit
                        )
                    }
                    convertPbOrderToEntity(pbOrder, items).toDeliveryOrder()
                } catch (e: Exception) {
                    null
                }
            }
            val currentOrders = _allDeliveryOrdersFlow.value.toMutableList()
            deliveryOrders.forEach { dOrder ->
                val index = currentOrders.indexOfFirst { it.id == dOrder.id }
                if (index >= 0) {
                    currentOrders[index] = dOrder
                } else {
                    currentOrders.add(dOrder)
                }
            }
            _allDeliveryOrdersFlow.value = currentOrders
            deliveryOrders
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to get available delivery orders: ${e.message}", isError = true)
            emptyList<DeliveryOrderEntity>()
        }
    }

    // Get active Ride OR Orders

    suspend fun getActiveRide(deliveryId: String): List<DeliveryOrderEntity> = withContext(Dispatchers.IO) {
        if (deliveryId.isBlank()) return@withContext emptyList()
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val filter = "deliveryId='$deliveryId' && status!='DELIVERED' && status!='CANCELLED'"
            val response = pbClient.getApi().getOrders(filter = filter, sort = "-created")
            
            val deliveryOrders = response.items.mapNotNull { pbOrder ->
                try {
                    val itemsResponse = pbClient.getApi().getOrderItems(filter = "orderId='${pbOrder.id ?: ""}'")
                    val items = itemsResponse.items.map { pbItem ->
                        OrderItemEntity(
                            id = pbItem.id ?: "",
                            orderId = pbItem.orderId,
                            productId = pbItem.productId,
                            productName = pbItem.productName,
                            quantity = pbItem.quantity,
                            unitPrice = pbItem.unitPrice,
                            subtotal = pbItem.subtotal,
                            unit = pbItem.unit
                        )
                    }
                    convertPbOrderToEntity(pbOrder, items).toDeliveryOrder()
                } catch (e: Exception) {
                    null
                }
            }
            val currentOrders = _allDeliveryOrdersFlow.value.toMutableList()
            deliveryOrders.forEach { dOrder ->
                val index = currentOrders.indexOfFirst { it.id == dOrder.id }
                if (index >= 0) {
                    currentOrders[index] = dOrder
                } else {
                    currentOrders.add(dOrder)
                }
            }
            _allDeliveryOrdersFlow.value = currentOrders
            deliveryOrders
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to get active delivery orders: ${e.message}", isError = true)
            emptyList<DeliveryOrderEntity>()
        }
    }

    /**
    * Update order status
    */
    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        extraFields: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val fields = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updated" to getCurrentTimestamp()
            )
            fields.putAll(extraFields)

            var generatedBuyerCode: String? = null
            val existingOrder = _allOrdersFlow.value.find { it.id == orderId }
                ?: _allSellerOrdersFlow.value.find { it.id == orderId }?.toBuyerOrder()
                ?: _allDeliveryOrdersFlow.value.find { it.id == orderId }?.toBuyerOrder()

            val isSelfPickup = existingOrder?.isSelfPickup ?: false

            if (newStatus == "READY_FOR_PICKUP" && isSelfPickup) {
                val currentBuyerCode = existingOrder?.buyerVerifCode
                if (currentBuyerCode.isNullOrBlank()) {
                    val code = (100000..999999).random().toString()
                    fields["buyer_verif_code"] = code
                    generatedBuyerCode = code
                }
            } else if (newStatus == "OUT_FOR_DELIVERY") {
                val currentBuyerCode = existingOrder?.buyerVerifCode
                if (currentBuyerCode.isNullOrBlank()) {
                    val code = (100000..999999).random().toString()
                    fields["buyer_verif_code"] = code
                    generatedBuyerCode = code
                }
            }

            pbClient.getApi().updateOrder(orderId, fields)
            
            val updatedSellerName = extraFields["sellerName"] as? String ?: extraFields["seller_name"] as? String
            val updatedStoreLoc = extraFields["storeLocation"] as? StoreLocation
            val updatedPaymentRef = extraFields["paymentReference"] as? String
            val updatedPaymentBackReceipt = extraFields["paymentBackReceipt"] as? String
            val updatedPaymentBackRef = extraFields["paymentBackReference"] as? String

            // Update local flows for each role independently with the new status and potentially buyer code
            updateLocalOrderStatus(
                orderId = orderId,
                newStatus = newStatus,
                buyerVerifCode = generatedBuyerCode,
                sellerName = updatedSellerName,
                storeLocation = updatedStoreLoc,
                paymentReference = updatedPaymentRef,
                paymentBackReceipt = updatedPaymentBackReceipt,
                paymentBackReference = updatedPaymentBackRef
            )
            
            DebugLogManager.log("AppRepository", "✅ Order $orderId status updated to $newStatus")
            true
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to update order status: ${e.message}", isError = true)
            false
        }
    }

    /**
    * Verify buyer pickup (self-pickup)
    */
    suspend fun verifyBuyerPickup(orderId: String, buyerCode: String): Boolean = withContext(Dispatchers.IO) {
        val bOrder = _allOrdersFlow.value.find { it.id == orderId } ?: _allSellerOrdersFlow.value.find { it.id == orderId }?.toBuyerOrder()
        val expectedCode = bOrder?.buyerVerifCode ?: ((orderId.hashCode().let { if (it < 0) -it else it } * 179) % 900000 + 100000).toString()
        
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val order = try { pbClient.getApi().getOrder(orderId) } catch (_: Exception) { null }
            val pbCode = order?.buyer_verif_code ?: expectedCode
            
            if (buyerCode != expectedCode && buyerCode != pbCode) {
                return@withContext false
            }
            
            val fields = mapOf(
                "status" to "DELIVERED",
                "updated" to getCurrentTimestamp()
            )
            try { pbClient.getApi().updateOrder(orderId, fields) } catch (_: Exception) {}
            
            // Update local flow
            updateLocalOrderStatus(orderId, "DELIVERED")
            
            DebugLogManager.log("AppRepository", "✅ Buyer pickup verified for order $orderId")
            true
        } catch (e: Exception) {
            if (buyerCode == expectedCode) {
                updateLocalOrderStatus(orderId, "DELIVERED")
                return@withContext true
            }
            DebugLogManager.log("AppRepository", "❌ Failed to verify buyer pickup: ${e.message}", isError = true)
            false
        }
    }

    /**
    * Verify delivery pickup (seller verifies courier at store)
    */
    suspend fun verifyDeliveryPickup(orderId: String, deliveryCode: String): Boolean = withContext(Dispatchers.IO) {
        val bOrder = _allOrdersFlow.value.find { it.id == orderId } ?: _allSellerOrdersFlow.value.find { it.id == orderId }?.toBuyerOrder()
        val expectedCode = bOrder?.deliveryVerifCode ?: ((orderId.hashCode().let { if (it < 0) -it else it } * 179) % 900000 + 100000).toString()
        
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val order = try { pbClient.getApi().getOrder(orderId) } catch (_: Exception) { null }
            val pbCode = order?.delivery_verif_code ?: expectedCode
            
            if (deliveryCode != expectedCode && deliveryCode != pbCode) {
                return@withContext false
            }
            
            val buyerCode = bOrder?.buyerVerifCode ?: (100000..999999).random().toString()
            val fields = mapOf(
                "status" to "OUT_FOR_DELIVERY",
                "buyer_verif_code" to buyerCode,
                "updated" to getCurrentTimestamp()
            )
            try { pbClient.getApi().updateOrder(orderId, fields) } catch (_: Exception) {}
            
            // Update local flow
            updateLocalOrderStatus(orderId, "OUT_FOR_DELIVERY", buyerVerifCode = buyerCode)
            
            DebugLogManager.log("AppRepository", "✅ Delivery pickup verified for order $orderId")
            true
        } catch (e: Exception) {
            val buyerCode = bOrder?.buyerVerifCode ?: (100000..999999).random().toString()
            if (deliveryCode == expectedCode) {
                updateLocalOrderStatus(orderId, "OUT_FOR_DELIVERY", buyerVerifCode = buyerCode)
                return@withContext true
            }
            DebugLogManager.log("AppRepository", "❌ Failed to verify delivery pickup: ${e.message}", isError = true)
            false
        }
    }

    /**
    * Accept delivery order (driver accepts job)
    */
    suspend fun acceptDeliveryOrder(
        orderId: String,
        deliveryProfileId: String,
        courierName: String,
        courierPlate: String,
        courierPhone: String = "+251911223344"
    ): Boolean = withContext(Dispatchers.IO) {
        val deliveryCode = (100000..999999).random().toString()
        updateLocalOrderCourierInfo(orderId, deliveryProfileId, courierName, courierPlate, courierPhone, deliveryCode)
        updateLocalOrderStatus(orderId, "READY_FOR_PICKUP", deliveryVerifCode = deliveryCode)
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val fields = mapOf(
                "deliveryId" to deliveryProfileId,
                "courier_name" to courierName,
                "courier_plate" to courierPlate,
                "courierPhone" to courierPhone,
                "delivery_verif_code" to deliveryCode,
                "status" to "READY_FOR_PICKUP",
                "updated" to getCurrentTimestamp()
            )
            pbClient.getApi().updateOrder(orderId, fields)
            DebugLogManager.log("AppRepository", "✅ Delivery accepted for order $orderId with code $deliveryCode")
            true
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "❌ Failed to accept delivery: ${e.message}", isError = true)
            true
        }
    }

    /**
    * Confirm delivery (driver verifies buyer code at delivery location)
    */
    suspend fun confirmDelivery(orderId: String, verificationCode: String): Boolean = withContext(Dispatchers.IO) {
        val bOrder = _allOrdersFlow.value.find { it.id == orderId } ?: _allSellerOrdersFlow.value.find { it.id == orderId }?.toBuyerOrder()
        val expectedCode = bOrder?.buyerVerifCode ?: ((orderId.hashCode().let { if (it < 0) -it else it } * 179) % 900000 + 100000).toString()
        
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val order = try { pbClient.getApi().getOrder(orderId) } catch (_: Exception) { null }
            val pbCode = order?.buyer_verif_code ?: expectedCode
            
            if (verificationCode != expectedCode && verificationCode != pbCode) {
                return@withContext false
            }
            
            val fields = mapOf(
                "status" to "DELIVERED",
                "updated" to getCurrentTimestamp()
            )
            try { pbClient.getApi().updateOrder(orderId, fields) } catch (_: Exception) {}
            
            // Update local flow
            updateLocalOrderStatus(orderId, "DELIVERED")
            
            DebugLogManager.log("AppRepository", "✅ Delivery confirmed for order $orderId")
            true
        } catch (e: Exception) {
            if (verificationCode == expectedCode) {
                updateLocalOrderStatus(orderId, "DELIVERED")
                return@withContext true
            }
            DebugLogManager.log("AppRepository", "❌ Failed to confirm delivery: ${e.message}", isError = true)
            false
        }
    }

    /**
    * Cancel order
    */
    suspend fun cancelOrder(orderId: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val fields = mapOf(
                "status" to "CANCELLED",
                "cancellation_reason" to reason,
                "updated" to getCurrentTimestamp()
            )
            pbClient.getApi().updateOrder(orderId, fields)
            
            // Update local flow
            updateLocalOrderStatus(orderId, "CANCELLED", reason)
            
            DebugLogManager.log("AppRepository", "✅ Order $orderId cancelled")
            true
        } catch (e: Exception) {
            updateLocalOrderStatus(orderId, "CANCELLED", reason)
            DebugLogManager.log("AppRepository", "❌ Failed to cancel order on PB, updated locally: ${e.message}", isError = true)
            true
        }
    }

    // ✅ Update ONLY the driverLocation field (LIGHTWEIGHT)
    suspend fun updateDriverLocation(
        orderId: String,
        lat: Double,
        lon: Double,
        accuracy: Float = 0f,
        speed: Float = 0f,
        bearing: Float = 0f
    ) {
        val newDriverLoc = DriverLocation(
            lat = lat,
            lon = lon,
            lastUpdated = System.currentTimeMillis(),
            accuracy = accuracy,
            speed = speed,
            bearing = bearing
        )
        _allDeliveryOrdersFlow.value = _allDeliveryOrdersFlow.value.map { if (it.id == orderId) it.copy(driverLocation = newDriverLoc) else it }
        _allOrdersFlow.value = _allOrdersFlow.value.map { if (it.id == orderId) it.copy(driverLocation = newDriverLoc) else it }
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val fields = mapOf(
                "driverLocation" to mapOf(
                    "lat" to lat,
                    "lon" to lon,
                    "lastUpdated" to System.currentTimeMillis(),
                    "accuracy" to accuracy,
                    "speed" to speed,
                    "bearing" to bearing
                )
            )
            // ✅ PATCH - updates ONLY driverLocation
            pbClient.getApi().updateOrder(orderId, fields)
            DebugLogManager.log("Repository", "✅ Driver location updated: $lat, $lon")
        } catch (e: Exception) {
            DebugLogManager.log("Repository", "❌ Failed to update driver location: ${e.message}", isError = true)
        }
    }
    
    // ✅ Get driver location for an order
    suspend fun getDriverLocation(orderId: String): DriverLocation? {
        val localOrder = _allOrdersFlow.value.find { it.id == orderId }
        if (localOrder != null && localOrder.driverLocation.lat != 0.0) {
            return localOrder.driverLocation
        }
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val order = pbClient.getApi().getOrder(orderId)
            return order.driverLocation
        } catch (e: Exception) {
            return null
        }
    }
    
    // ✅ Get distance between driver and destination
    suspend fun getDistanceToDestination(orderId: String): Double? {
        try {
            val driverLoc = getDriverLocation(orderId) ?: return null
            val order = _allOrdersFlow.value.find { it.id == orderId }
            val destination = order?.shippingAddress ?: return null
            
            return calculateDistance(
                driverLoc.lat, driverLoc.lon,
                destination.lat, destination.lon
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    // ✅ Calculate distance using Haversine formula
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private fun updateLocalOrderCourierInfo(
        orderId: String,
        deliveryId: String,
        courierName: String,
        courierPlate: String,
        courierPhone: String,
        deliveryVerifCode: String? = null
    ) {
        val bOrders = _allOrdersFlow.value.toMutableList()
        val bIndex = bOrders.indexOfFirst { it.id == orderId }
        if (bIndex >= 0) {
            bOrders[bIndex] = bOrders[bIndex].copy(
                deliveryId = deliveryId,
                courierName = courierName,
                courierPlate = courierPlate,
                deliveryVerifCode = deliveryVerifCode ?: bOrders[bIndex].deliveryVerifCode
            )
            _allOrdersFlow.value = bOrders
        }

        val sOrders = _allSellerOrdersFlow.value.toMutableList()
        val sIndex = sOrders.indexOfFirst { it.id == orderId }
        if (sIndex >= 0) {
            sOrders[sIndex] = sOrders[sIndex].copy(
                deliveryId = deliveryId,
                courierName = courierName,
                courierPlate = courierPlate,
                deliveryVerifCode = deliveryVerifCode ?: sOrders[sIndex].deliveryVerifCode
            )
            _allSellerOrdersFlow.value = sOrders
        }

        val dOrders = _allDeliveryOrdersFlow.value.toMutableList()
        val dIndex = dOrders.indexOfFirst { it.id == orderId }
        if (dIndex >= 0) {
            dOrders[dIndex] = dOrders[dIndex].copy(
                deliveryId = deliveryId,
                courierName = courierName,
                courierPlate = courierPlate,
                deliveryVerifCode = deliveryVerifCode ?: dOrders[dIndex].deliveryVerifCode
            )
            _allDeliveryOrdersFlow.value = dOrders
        }
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private fun generateVerificationCode(): String {
        return (100000..999999).random().toString()
    }

    private fun getCurrentTimestamp(): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(java.util.Date())
    }

    private fun convertPbOrderToEntity(
        pbOrder: PbOrder,
        items: List<OrderItemEntity>
    ): OrderEntity {
        val loc = pbOrder.shippingAddress ?: LocationShipping()

        return OrderEntity(
            id = pbOrder.id ?: "",
            buyerId = pbOrder.buyerId,
            sellerId = pbOrder.sellerId,
            deliveryId = pbOrder.deliveryId,
            buyerName = pbOrder.buyerName,
            buyerPhone = pbOrder.buyerPhone,
            sellerName = pbOrder.sellerName,
            productNames = pbOrder.product_names,
            totalPrice = pbOrder.total_price,
            deliveryPrice = pbOrder.delivery_price,
            unit = pbOrder.unit,
            paymentMethod = pbOrder.payment_method,
            status = pbOrder.status,
            shippingAddress = loc,
            storeLocation = pbOrder.storeLocation ?: StoreLocation(),
            buyerVerifCode = pbOrder.buyer_verif_code,
            deliveryVerifCode = pbOrder.delivery_verif_code,
            courierName = pbOrder.courier_name,
            courierPlate = pbOrder.courier_plate,
            deliveryPickup = pbOrder.delivery_pickup,
            isSelfPickup = pbOrder.is_self_pickup,
            paymentReceipt = pbOrder.paymentReceipt,
            paymentReference = pbOrder.paymentReference,
            paymentBackReceipt = pbOrder.paymentBackReceipt,
            paymentBackReference = pbOrder.paymentBackReference,
            cancellationReason = pbOrder.cancellation_reason,
            timestamp = pbOrder.timestamp,
            created = pbOrder.created,
            updated = pbOrder.updated,
            items = items
        )
    }

    private suspend fun updateLocalOrderStatus(
        orderId: String, 
        newStatus: String, 
        reason: String? = null,
        buyerVerifCode: String? = null,
        deliveryVerifCode: String? = null,
        sellerName: String? = null,
        storeLocation: StoreLocation? = null,
        paymentReference: String? = null,
        paymentBackReceipt: String? = null,
        paymentBackReference: String? = null
    ) {
        val bOrders = _allOrdersFlow.value.toMutableList()
        val bIndex = bOrders.indexOfFirst { it.id == orderId }
        if (bIndex >= 0) {
            bOrders[bIndex] = bOrders[bIndex].copy(
                status = newStatus,
                cancellationReason = reason ?: bOrders[bIndex].cancellationReason,
                buyerVerifCode = buyerVerifCode ?: bOrders[bIndex].buyerVerifCode,
                deliveryVerifCode = deliveryVerifCode ?: bOrders[bIndex].deliveryVerifCode,
                sellerName = sellerName ?: bOrders[bIndex].sellerName,
                storeLocation = storeLocation ?: bOrders[bIndex].storeLocation,
                paymentReference = paymentReference ?: bOrders[bIndex].paymentReference,
                paymentBackReceipt = paymentBackReceipt ?: bOrders[bIndex].paymentBackReceipt,
                paymentBackReference = paymentBackReference ?: bOrders[bIndex].paymentBackReference
            )
            _allOrdersFlow.value = bOrders
        }

        val sOrders = _allSellerOrdersFlow.value.toMutableList()
        val sIndex = sOrders.indexOfFirst { it.id == orderId }
        if (sIndex >= 0) {
            val updatedSOrder = sOrders[sIndex].copy(
                status = newStatus,
                cancellationReason = reason ?: sOrders[sIndex].cancellationReason,
                buyerVerifCode = buyerVerifCode ?: sOrders[sIndex].buyerVerifCode,
                deliveryVerifCode = deliveryVerifCode ?: sOrders[sIndex].deliveryVerifCode,
                sellerName = sellerName ?: sOrders[sIndex].sellerName,
                storeLocation = storeLocation ?: sOrders[sIndex].storeLocation,
                paymentReference = paymentReference ?: sOrders[sIndex].paymentReference,
                paymentBackReceipt = paymentBackReceipt ?: sOrders[sIndex].paymentBackReceipt,
                paymentBackReference = paymentBackReference ?: sOrders[sIndex].paymentBackReference
            )
            sOrders[sIndex] = updatedSOrder
            _allSellerOrdersFlow.value = sOrders
            try {
                val dbDao = AppRoomDatabase.getInstance(context).sellerOrderDao()
                dbDao.insertOrder(updatedSOrder.toDbEntity())
            } catch (e: Exception) {}
        }

        val dOrders = _allDeliveryOrdersFlow.value.toMutableList()
        val dIndex = dOrders.indexOfFirst { it.id == orderId }
        if (dIndex >= 0) {
            dOrders[dIndex] = dOrders[dIndex].copy(
                status = newStatus,
                cancellationReason = reason ?: dOrders[dIndex].cancellationReason,
                buyerVerifCode = buyerVerifCode ?: dOrders[dIndex].buyerVerifCode,
                deliveryVerifCode = deliveryVerifCode ?: dOrders[dIndex].deliveryVerifCode,
                sellerName = sellerName ?: dOrders[dIndex].sellerName,
                storeLocation = storeLocation ?: dOrders[dIndex].storeLocation,
                paymentReference = paymentReference ?: dOrders[dIndex].paymentReference,
                paymentBackReceipt = paymentBackReceipt ?: dOrders[dIndex].paymentBackReceipt,
                paymentBackReference = paymentBackReference ?: dOrders[dIndex].paymentBackReference
            )
            _allDeliveryOrdersFlow.value = dOrders
        }
    }

    private fun generateReceiptImageFile(context: android.content.Context, title: String, details: String): java.io.File {
        val width = 600
        val height = 800
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Background color
        canvas.drawColor(android.graphics.Color.parseColor("#F5F7FA"))
        
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        
        // Main receipt container
        paint.color = android.graphics.Color.WHITE
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRoundRect(40f, 40f, 560f, 760f, 16f, 16f, paint)
        
        // Receipt border
        paint.color = android.graphics.Color.parseColor("#E4E7EB")
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(40f, 40f, 560f, 760f, 16f, 16f, paint)
        
        // Header (Green or Purple banner depending on Telebirr vs CBE)
        val isCbe = title.contains("CBE", ignoreCase = true)
        val headerColor = if (isCbe) "#4A148C" else "#1B5E20" // CBE Purple vs Telebirr Green
        paint.color = android.graphics.Color.parseColor(headerColor)
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRoundRect(40f, 40f, 560f, 160f, 16f, 16f, paint)
        // Draw rectangle over bottom corners of header to keep them sharp inside the rounded card
        canvas.drawRect(40f, 100f, 560f, 160f, paint)
        
        // Header Title
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 28f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val headerText = if (isCbe) "CBE Birr Receipt" else "Telebirr Mobile Payment"
        canvas.drawText(headerText, 80f, 105f, paint)
        
        // Header Subtitle
        paint.textSize = 18f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        canvas.drawText("Transaction Success Notification", 80f, 135f, paint)
        
        // Receipt Content
        paint.color = android.graphics.Color.parseColor("#2D3748")
        paint.textSize = 20f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        
        // Receipt ID
        val receiptId = title.substringAfter("Receipt ID: ").trim()
        canvas.drawText("Receipt ID: $receiptId", 80f, 210f, paint)
        
        // Divider line
        paint.color = android.graphics.Color.parseColor("#E2E8F0")
        paint.strokeWidth = 2f
        canvas.drawLine(80f, 240f, 520f, 240f, paint)
        
        // Details parsing
        paint.color = android.graphics.Color.parseColor("#4A5568")
        paint.textSize = 18f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        
        var currentY = 280f
        val lines = details.split("\n")
        for (line in lines) {
            if (line.isBlank()) continue
            if (line.startsWith("Paid:") || line.startsWith("Amount:")) {
                // Highlight Amount
                paint.color = android.graphics.Color.parseColor("#1A202C")
                paint.textSize = 22f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText(line, 80f, currentY, paint)
                currentY += 40f
                paint.color = android.graphics.Color.parseColor("#4A5568")
                paint.textSize = 18f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            } else {
                canvas.drawText(line, 80f, currentY, paint)
                currentY += 35f
            }
        }
        
        // Success Badge at the bottom
        currentY = kotlin.math.max(currentY + 20f, 550f)
        paint.color = android.graphics.Color.parseColor("#E6FFFA")
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRoundRect(80f, currentY, 520f, currentY + 60f, 8f, 8f, paint)
        
        paint.color = android.graphics.Color.parseColor("#319795")
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(80f, currentY, 520f, currentY + 60f, 8f, 8f, paint)
        
        paint.color = android.graphics.Color.parseColor("#234E52")
        paint.style = android.graphics.Paint.Style.FILL
        paint.textSize = 18f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("VERIFIED TRANSACTION • SUCCESS", 110f, currentY + 36f, paint)
        
        // Security Seal / Barcode
        currentY += 100f
        paint.color = android.graphics.Color.parseColor("#A0AEC0")
        paint.strokeWidth = 3f
        var startX = 140f
        while (startX < 460f) {
            val barWidth = (5..15).random().toFloat()
            canvas.drawLine(startX, currentY, startX, currentY + 35f, paint)
            startX += barWidth + (3..8).random().toFloat()
        }
        
        // Save to cache file
        val file = java.io.File(context.cacheDir, "receipt_${System.currentTimeMillis()}.png")
        val out = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()
        return file
    }

    suspend fun getReferEarnUser(userId: String, userReferCode: String = ""): com.example.network.ReferEarnUser = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (userId.isBlank() && userReferCode.isBlank()) {
            return@withContext com.example.network.ReferEarnUser()
        }
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            
            // 1. Try filtering by id field
            if (userId.isNotBlank()) {
                try {
                    val resp2 = pbClient.getApi().getReferEarnUsers(filter = "id = '$userId'")
                    val item2 = resp2.items.firstOrNull()
                    if (item2 != null) {
                        return@withContext item2.toDomainModel()
                    }
                } catch (e2: Exception) {
                    DebugLogManager.log("AppRepository", "Filter 'id = $userId' failed: ${e2.message}")
                }
            }

            // 3. Try filtering by referCode field
            if (userReferCode.isNotBlank()) {
                try {
                    val resp3 = pbClient.getApi().getReferEarnUsers(filter = "referCode = '$userReferCode'")
                    val item3 = resp3.items.firstOrNull()
                    if (item3 != null) {
                        return@withContext item3.toDomainModel()
                    }
                } catch (e3: Exception) {
                    DebugLogManager.log("AppRepository", "Filter 'referCode = $userReferCode' failed: ${e3.message}")
                }
            }

            // 4. Fallback: Get records without filter and match locally
            try {
                val respAll = pbClient.getApi().getReferEarnUsers(page = 1, perPage = 100)
                val matched = respAll.items.firstOrNull { item ->
                    (userId.isNotBlank() && (item.user == userId || item.id == userId)) ||
                    (userReferCode.isNotBlank() && item.referCode == userReferCode)
                }
                if (matched != null) {
                    return@withContext matched.toDomainModel()
                }
            } catch (eAll: Exception) {
                DebugLogManager.log("AppRepository", "Fallback list referEarnUser failed: ${eAll.message}")
            }

            return@withContext com.example.network.ReferEarnUser(
                id = userId,
                user = userId,
                referCode = userReferCode,
                referralCount = 0,
                totalMoney = 0.0
            )
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Failed to fetch referEarnUser view collection: ${e.message}", isError = true)
            return@withContext com.example.network.ReferEarnUser(
                id = userId,
                user = userId,
                referCode = userReferCode,
                referralCount = 0,
                totalMoney = 0.0
            )
        }
    }

    suspend fun createWithdrawalRequest(
        userId: String,
        amount: Double,
        method: String,
        accountNumber: String,
        pendingUserIds: List<String>
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val pbClient = com.example.network.PocketBaseClient(context)
            val body = mapOf(
                "userId" to userId,
                "referredUserIds" to pendingUserIds,
                "totalAmount" to amount,
                "status" to "pending",
                "paymentMethod" to method,
                "paymentAccount" to accountNumber,
                "reference" to ""
            )
            pbClient.getApi().createWithdrawReferEarn(body)
            true
        } catch (e: Exception) {
            DebugLogManager.log("AppRepository", "Failed to create WithdrawReferEarn record: ${e.message}", isError = true)
            throw e
        }
    }
}

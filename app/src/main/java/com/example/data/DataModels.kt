package com.example.data

import kotlinx.coroutines.flow.Flow

// ==========================================
// USER ENTITY - Complete for PocketBase Sync
// ==========================================


data class UserEntity(
    
    val id: String = "1", // Changed to String to match PocketBase ID
    
    // ✅ Core fields from PocketBase users collection
    val role: String = "BUYER", // BUYER, SELLER, DELIVERY
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val profileImage: String = "",
    val addressRegion: String = "",
    val addressZone: String = "",
    val addressCity: String = "",
    val addressKebele: String? = null,
    val birthDate: String? = null,
    
    // ✅ ID Documents (only during registration)
    val idDocumentInfront: String? = null,
    val idDocumentBackfront: String? = null,
    
    // ✅ Status fields
    val isTwoFactorEnabled: Boolean? = false,
    val status: Boolean? = false,
    val isVerified: Boolean = true,
    val isApproved: Boolean = true,
    val isEmailVerified: Boolean = false,
    
    // ✅ Timestamps
    val created: String? = null,
    val updated: String? = null,
    
    // ✅ Seller fields (from seller_profiles collection)
    val businessName: String? = null,
    val businessPhone: String? = null,
    val businessAddress: String? = null,
    val tinNumber: String? = null,
    val tlcNumber: String? = null,
    val businessGradeTitel: String? = null,
    val businessGrade: String? = null,
    val businessAbout: String? = null,
    val businessWorkingHours: String? = null,
    val businessPayout: String? = null,
    val businessRating: String? = null,
    val businessDiscountCode: String? = null,
    val businessDocumentFront: String? = null,
    val businessDocumentBack: String? = null,
    val sellerVerification: String? = null,  // "pending", "approved", "rejected"
    val storeLocation: StoreLocation = StoreLocation(),
    val sellerProfileId: String? = null, // Reference to seller_profiles.id
    val sellerProfileImage: String? = null,
    val sellerProfileCover: String? = null,
    
    // ✅ Delivery fields (from delivery_profiles collection)
    val plateNumber: String? = null,
    val deliveryPhone: String? = null,
    val vehicleType: String? = null,
    val deliveryGrade: String? = null,
    val xpTotal: Int? = 0,
    val ratingTotal: Double? = 0.0,
    val driverLicenseFront: String? = null,
    val driverLicenseBack: String? = null,
    val vehicleDocumentFront: String? = null,
    val vehicleDocumentBack: String? = null,
    val deliveryVerification: String? = null,  // "pending", "approved", "rejected"
    val deliveryProfileId: String? = null, // Reference to delivery_profiles.id
    
    // ✅ Security & Referral fields
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val currentPinCode: String? = null,
    val is2faCompleted: Boolean = true,
    val referCode: String = "",
    val fromReferCode: String = ""
) {
    // ✅ Helper to check if user is a seller with approved status
    fun isApprovedSeller(): Boolean {
        return role == "SELLER" && status == true && isApproved
    }
    
    // ✅ Helper to check if user is a delivery with approved status
    fun isApprovedDelivery(): Boolean {
        return role == "DELIVERY" && status == true && isApproved
    }
    
    // ✅ Helper to check if application is pending
    fun isApplicationPending(): Boolean {
        return if (role == "SELLER") {
            sellerVerification == "pending"
        } else if (role == "DELIVERY") {
            deliveryVerification == "pending"
        } else {
            false
        }
    }

    val address: String
        get() = if (addressCity.isNotBlank()) "$addressRegion, $addressZone, $addressCity, Kebele $addressKebele" else ""
}

// ==========================================
// SELLER PRODUCT ENTITY
// ==========================================

data class SellerProductEntity(
    val id: String = "",
    
    val sellerId: String = "", // Changed to String to match PocketBase
    val sellerName: String = "",
    val name: String,
    val price: Double,
    val stock: Int,
    val category: String,
    val subcategory: String = "",
    val imageResName: String = "",
    val imageUrlName: String = "",
    val isPromoted: Boolean = false,
    val impressions: Int = 0,
    val clicks: Int = 0,
    val description: String = "Premium authentic regional product from local reliable sources in Ethiopia.",
    val unitOfMeasurement: String = "pieces",
    val stateNewOrUsed: String = "New",
    val isFreeDelivery: Boolean = false,
    val discountPercent: Int = 0,
    val brand: String = "",
    val variants: String = "",
    val rating: Double = 4.5,
    val cost: Double = 0.0,
    val sold: Int = 0,
    val lowStock: Int = 3,
    val created: String? = null,
    val updated: String? = null
)

data class ProductReviewEntity(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val created: String? = null,
    val updated: String? = null
)

// ==========================================
// FOR ALL USER PRODUCTS VIEW
// ==========================================

data class ProductEntity(
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val category: String = "",
    val subcategory: String = "",
    val imageResName: String = "",
    val imageUrlName: String = "",
    val isPromoted: Boolean = false,
    val impressions: Int = 0,
    val clicks: Int = 0,
    val description: String = "",
    val unitOfMeasurement: String = "pieces",
    val stateNewOrUsed: String = "New",
    val isFreeDelivery: Boolean = false,
    val discountPercent: Int = 0,
    val brand: String = "",
    val variants: String = "",
    val rating: Double = 4.5,
    val reviewCount: Int = 0,
    val sold: Int = 0,
    val created: String? = null,
    val updated: String? = null
)



// ==========================================
// ORDER ENTITIES (Buyer, Seller, Delivery)
// ==========================================

// --- BUYER ENTITIES ---
data class OrderItemEntity(
    val id: String = "",
    val orderId: String = "",
    val productId: String = "",
    val productName: String = "",
    val category: String? = null,
    val subcategory: String? = null,
    val brand: String? = null,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0,
    val unit: String = "pieces",
    val sellerId: String = "",
    val buyerId: String = "",
    val paymentMethod: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class OrderEntity(
    val id: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val deliveryId: String? = null,
    val buyerName: String = "",
    val buyerPhone: String = "",
    val sellerName: String = "",
    val productNames: String = "",           // Summary text
    val totalPrice: Double = 0.0,
    val deliveryPrice: Double = 0.0,
    val unit: String = "pieces",
    val paymentMethod: String = "Telebirr",
    val status: String = "",
    val shippingAddress: ShippingAddress = ShippingAddress(),
    val storeLocation: StoreLocation = StoreLocation(),
    val buyerVerifCode: String? = null,
    val deliveryVerifCode: String? = null,
    val courierName: String? = null,
    val courierPlate: String? = null,
    val driverLocation: DriverLocation = DriverLocation(),
    val deliveryPickup: Boolean = false,
    val isSelfPickup: Boolean = false,
    val paymentReceipt: String? = null,
    val paymentReference: String? = null,
    val paymentBackReceipt: String? = null,
    val paymentBackReference: String? = null,
    val cancellationReason: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val created: String? = null,
    val updated: String? = null,
    val items: List<OrderItemEntity> = emptyList()
)

typealias BuyerOrderEntity = OrderEntity
typealias BuyerOrderItemEntity = OrderItemEntity

// --- SELLER ENTITIES ---
data class SellerOrderItemEntity(
    val id: String = "",
    val orderId: String = "",
    val productId: String = "",
    val productName: String = "",
    val category: String? = null,
    val subcategory: String? = null,
    val brand: String? = null,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0,
    val unit: String = "pieces",
    val sellerId: String = "",
    val buyerId: String = "",
    val paymentMethod: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class SellerOrderEntity(
    val id: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val deliveryId: String? = null,
    val buyerName: String = "",
    val buyerPhone: String = "",
    val sellerName: String = "",
    val productNames: String = "",
    val totalPrice: Double = 0.0,
    val deliveryPrice: Double = 0.0,
    val unit: String = "pieces",
    val paymentMethod: String = "Telebirr",
    val status: String = "",
    val shippingAddress: ShippingAddress = ShippingAddress(),
    val storeLocation: StoreLocation = StoreLocation(),
    val buyerVerifCode: String? = null,
    val deliveryVerifCode: String? = null,
    val courierName: String? = null,
    val courierPlate: String? = null,
    val driverLocation: DriverLocation = DriverLocation(),
    val deliveryPickup: Boolean = false,
    val isSelfPickup: Boolean = false,
    val paymentReceipt: String? = null,
    val paymentReference: String? = null,
    val paymentBackReceipt: String? = null,
    val paymentBackReference: String? = null,
    val cancellationReason: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val created: String? = null,
    val updated: String? = null,
    val items: List<SellerOrderItemEntity> = emptyList()
)

// --- DELIVERY ENTITIES ---
data class DeliveryOrderItemEntity(
    val id: String = "",
    val orderId: String = "",
    val productId: String = "",
    val productName: String = "",
    val category: String? = null,
    val subcategory: String? = null,
    val brand: String? = null,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0,
    val unit: String = "pieces",
    val sellerId: String = "",
    val buyerId: String = "",
    val paymentMethod: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class DeliveryOrderEntity(
    val id: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val deliveryId: String? = null,
    val buyerName: String = "",
    val buyerPhone: String = "",
    val sellerName: String = "",
    val productNames: String = "",
    val totalPrice: Double = 0.0,
    val deliveryPrice: Double = 0.0,
    val unit: String = "pieces",
    val paymentMethod: String = "Telebirr",
    val status: String = "",
    val shippingAddress: ShippingAddress = ShippingAddress(),
    val storeLocation: StoreLocation = StoreLocation(),
    val buyerVerifCode: String? = null,
    val deliveryVerifCode: String? = null,
    val courierName: String? = null,
    val courierPlate: String? = null,
    val driverLocation: DriverLocation = DriverLocation(),
    val deliveryPickup: Boolean = false,
    val isSelfPickup: Boolean = false,
    val paymentReceipt: String? = null,
    val paymentReference: String? = null,
    val paymentBackReceipt: String? = null,
    val paymentBackReference: String? = null,
    val cancellationReason: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val created: String? = null,
    val updated: String? = null,
    val items: List<DeliveryOrderItemEntity> = emptyList()
)

data class DriverLocation(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val bearing: Float = 0f
)

data class StoreLocation(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

// --- CONVERSION EXTENSIONS ---
fun OrderItemEntity.toSellerItem(): SellerOrderItemEntity = SellerOrderItemEntity(
    id = id, orderId = orderId, productId = productId, productName = productName,
    category = category, subcategory = subcategory, brand = brand, quantity = quantity,
    unitPrice = unitPrice, subtotal = subtotal, unit = unit, sellerId = sellerId,
    buyerId = buyerId, paymentMethod = paymentMethod, createdAt = createdAt, updatedAt = updatedAt
)

fun OrderItemEntity.toDeliveryItem(): DeliveryOrderItemEntity = DeliveryOrderItemEntity(
    id = id, orderId = orderId, productId = productId, productName = productName,
    category = category, subcategory = subcategory, brand = brand, quantity = quantity,
    unitPrice = unitPrice, subtotal = subtotal, unit = unit, sellerId = sellerId,
    buyerId = buyerId, paymentMethod = paymentMethod, createdAt = createdAt, updatedAt = updatedAt
)

fun SellerOrderItemEntity.toBuyerItem(): OrderItemEntity = OrderItemEntity(
    id = id, orderId = orderId, productId = productId, productName = productName,
    category = category, subcategory = subcategory, brand = brand, quantity = quantity,
    unitPrice = unitPrice, subtotal = subtotal, unit = unit, sellerId = sellerId,
    buyerId = buyerId, paymentMethod = paymentMethod, createdAt = createdAt, updatedAt = updatedAt
)

fun SellerOrderItemEntity.toDeliveryItem(): DeliveryOrderItemEntity = DeliveryOrderItemEntity(
    id = id, orderId = orderId, productId = productId, productName = productName,
    category = category, subcategory = subcategory, brand = brand, quantity = quantity,
    unitPrice = unitPrice, subtotal = subtotal, unit = unit, sellerId = sellerId,
    buyerId = buyerId, paymentMethod = paymentMethod, createdAt = createdAt, updatedAt = updatedAt
)

fun DeliveryOrderItemEntity.toBuyerItem(): OrderItemEntity = OrderItemEntity(
    id = id, orderId = orderId, productId = productId, productName = productName,
    category = category, subcategory = subcategory, brand = brand, quantity = quantity,
    unitPrice = unitPrice, subtotal = subtotal, unit = unit, sellerId = sellerId,
    buyerId = buyerId, paymentMethod = paymentMethod, createdAt = createdAt, updatedAt = updatedAt
)

fun DeliveryOrderItemEntity.toSellerItem(): SellerOrderItemEntity = SellerOrderItemEntity(
    id = id, orderId = orderId, productId = productId, productName = productName,
    category = category, subcategory = subcategory, brand = brand, quantity = quantity,
    unitPrice = unitPrice, subtotal = subtotal, unit = unit, sellerId = sellerId,
    buyerId = buyerId, paymentMethod = paymentMethod, createdAt = createdAt, updatedAt = updatedAt
)

fun OrderEntity.toSellerOrder(): SellerOrderEntity = SellerOrderEntity(
    id = id, buyerId = buyerId, sellerId = sellerId, deliveryId = deliveryId,
    buyerName = buyerName, buyerPhone = buyerPhone, sellerName = sellerName,
    productNames = productNames, totalPrice = totalPrice, deliveryPrice = deliveryPrice,
    unit = unit, paymentMethod = paymentMethod, status = status,
    shippingAddress = shippingAddress, storeLocation = storeLocation,
    buyerVerifCode = buyerVerifCode, deliveryVerifCode = deliveryVerifCode,
    courierName = courierName, courierPlate = courierPlate, driverLocation = driverLocation,
    deliveryPickup = deliveryPickup, isSelfPickup = isSelfPickup, paymentReceipt = paymentReceipt,
    paymentReference = paymentReference, paymentBackReceipt = paymentBackReceipt, paymentBackReference = paymentBackReference,
    cancellationReason = cancellationReason, timestamp = timestamp, created = created, updated = updated,
    items = items.map { it.toSellerItem() }
)

fun OrderEntity.toDeliveryOrder(): DeliveryOrderEntity = DeliveryOrderEntity(
    id = id, buyerId = buyerId, sellerId = sellerId, deliveryId = deliveryId,
    buyerName = buyerName, buyerPhone = buyerPhone, sellerName = sellerName,
    productNames = productNames, totalPrice = totalPrice, deliveryPrice = deliveryPrice,
    unit = unit, paymentMethod = paymentMethod, status = status,
    shippingAddress = shippingAddress, storeLocation = storeLocation,
    buyerVerifCode = buyerVerifCode, deliveryVerifCode = deliveryVerifCode,
    courierName = courierName, courierPlate = courierPlate, driverLocation = driverLocation,
    deliveryPickup = deliveryPickup, isSelfPickup = isSelfPickup, paymentReceipt = paymentReceipt,
    cancellationReason = cancellationReason, timestamp = timestamp, created = created, updated = updated,
    items = items.map { it.toDeliveryItem() }
)

fun SellerOrderEntity.toBuyerOrder(): OrderEntity = OrderEntity(
    id = id, buyerId = buyerId, sellerId = sellerId, deliveryId = deliveryId,
    buyerName = buyerName, buyerPhone = buyerPhone, sellerName = sellerName,
    productNames = productNames, totalPrice = totalPrice, deliveryPrice = deliveryPrice,
    unit = unit, paymentMethod = paymentMethod, status = status,
    shippingAddress = shippingAddress, storeLocation = storeLocation,
    buyerVerifCode = buyerVerifCode, deliveryVerifCode = deliveryVerifCode,
    courierName = courierName, courierPlate = courierPlate, driverLocation = driverLocation,
    deliveryPickup = deliveryPickup, isSelfPickup = isSelfPickup, paymentReceipt = paymentReceipt,
    cancellationReason = cancellationReason, timestamp = timestamp, created = created, updated = updated,
    items = items.map { it.toBuyerItem() }
)

fun SellerOrderEntity.toDeliveryOrder(): DeliveryOrderEntity = DeliveryOrderEntity(
    id = id, buyerId = buyerId, sellerId = sellerId, deliveryId = deliveryId,
    buyerName = buyerName, buyerPhone = buyerPhone, sellerName = sellerName,
    productNames = productNames, totalPrice = totalPrice, deliveryPrice = deliveryPrice,
    unit = unit, paymentMethod = paymentMethod, status = status,
    shippingAddress = shippingAddress, storeLocation = storeLocation,
    buyerVerifCode = buyerVerifCode, deliveryVerifCode = deliveryVerifCode,
    courierName = courierName, courierPlate = courierPlate, driverLocation = driverLocation,
    deliveryPickup = deliveryPickup, isSelfPickup = isSelfPickup, paymentReceipt = paymentReceipt,
    cancellationReason = cancellationReason, timestamp = timestamp, created = created, updated = updated,
    items = items.map { it.toDeliveryItem() }
)

fun DeliveryOrderEntity.toBuyerOrder(): OrderEntity = OrderEntity(
    id = id, buyerId = buyerId, sellerId = sellerId, deliveryId = deliveryId,
    buyerName = buyerName, buyerPhone = buyerPhone, sellerName = sellerName,
    productNames = productNames, totalPrice = totalPrice, deliveryPrice = deliveryPrice,
    unit = unit, paymentMethod = paymentMethod, status = status,
    shippingAddress = shippingAddress, storeLocation = storeLocation,
    buyerVerifCode = buyerVerifCode, deliveryVerifCode = deliveryVerifCode,
    courierName = courierName, courierPlate = courierPlate, driverLocation = driverLocation,
    deliveryPickup = deliveryPickup, isSelfPickup = isSelfPickup, paymentReceipt = paymentReceipt,
    cancellationReason = cancellationReason, timestamp = timestamp, created = created, updated = updated,
    items = items.map { it.toBuyerItem() }
)

fun DeliveryOrderEntity.toSellerOrder(): SellerOrderEntity = SellerOrderEntity(
    id = id, buyerId = buyerId, sellerId = sellerId, deliveryId = deliveryId,
    buyerName = buyerName, buyerPhone = buyerPhone, sellerName = sellerName,
    productNames = productNames, totalPrice = totalPrice, deliveryPrice = deliveryPrice,
    unit = unit, paymentMethod = paymentMethod, status = status,
    shippingAddress = shippingAddress, storeLocation = storeLocation,
    buyerVerifCode = buyerVerifCode, deliveryVerifCode = deliveryVerifCode,
    courierName = courierName, courierPlate = courierPlate, driverLocation = driverLocation,
    deliveryPickup = deliveryPickup, isSelfPickup = isSelfPickup, paymentReceipt = paymentReceipt,
    cancellationReason = cancellationReason, timestamp = timestamp, created = created, updated = updated,
    items = items.map { it.toSellerItem() }
)

typealias LocationShipping = ShippingAddress

@kotlinx.serialization.Serializable
data class ShippingAddress(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

// Cart Item (for checkout)
data class CartItem(
    val productId: String,
    val productName: String,
    val category: String? = null,
    val subcategory: String? = null,
    val brand: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val unit: String = "pieces",
    val sellerName: String? = null
)


// Order View Models
data class BuyerOrderView(
    val id: String,
    val orderId: String,
    val productName: String,
    val category: String? = null,
    val brand: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val unit: String = "pieces",
    val createdAt: String? = null
)

// ✅ Seller sees: ALL fields including cost
data class SellerOrderItemView(
    val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val category: String? = null,
    val subcategory: String? = null,
    val brand: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val cost: Double,  // ✅ Seller can see cost
    val unit: String = "pieces",
    val buyerId: String,
    val paymentMethod: String? = null,
    val createdAt: String? = null
)

// ✅ Delivery sees: productName, quantity, unitPrice (no cost)
data class DeliveryOrderItemView(
    val id: String,
    val orderId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val unit: String = "pieces",
    val createdAt: String? = null
)

data class SellerOrderView(
    val id: String,
    val orderNumber: String = "",
    val buyerId: String,
    val buyerName: String,
    val buyerPhone: String,
    val buyerAddress: String? = null,
    val productNames: String,
    val totalPrice: Double,
    val deliveryPrice: Double,
    val status: String,
    val deliveryId: String? = null,
    val courierName: String? = null,
    val courierPlate: String? = null,
    val isSelfPickup: Boolean,
    val deliveryPickup: Boolean,
    val buyerVerifCode: String? = null,
    val deliveryVerifCode: String? = null,
    val items: List<OrderItemEntity>,
    val createdAt: String
)

data class DeliveryOrderView(
    val id: String,
    val orderNumber: String = "",
    val sellerId: String,
    val sellerName: String,
    val sellerPhone: String,
    val sellerAddress: String,
    val buyerId: String,
    val buyerName: String,
    val buyerPhone: String,
    val buyerAddress: String,
    val productNames: String,
    val totalPrice: Double,
    val deliveryPrice: Double,
    val status: String,
    val deliveryVerifCode: String,
    val isSelfPickup: Boolean,
    val items: List<OrderItemEntity>,
    val createdAt: String
)

// ==========================================
// CHAT MESSAGE ENTITY
// ==========================================


data class ChatMessageEntity(
    val id: String = "",
    
    val chatRoomId: String, // "buyer_seller", "buyer_delivery", "support"
    val senderId: String = "",
    val senderName: String,
    val senderRole: String, // "BUYER", "SELLER", "DELIVERY", "SUPPORT"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentUrl: String? = null,
    val attachmentType: String? = null, // "IMAGE", "VIDEO", "FILE"
    val isLiked: Boolean = false,
    val isDeleted: Boolean = false,
    val status: String = "READ", // "SENT", "READ"
    val reactionEmoji: String? = null,
    val replyToMessageId: Int? = null,
    val replyToMessageText: String? = null,
    val created: String? = null
)

// ==========================================
// ADDRESS ENTITY
// ==========================================


data class AddressEntity(
    val id: String = "",
    
    val userId: String = "",
    val label: String, // "Home", "Work", "Other" or custom
    val region: String,
    val zone: String,
    val city: String,
    val kebele: String,
    val streetHouseNumber: String,
    val isDefault: Boolean = false,
    val latitude: Double = 9.02,
    val longitude: Double = 38.75,
    val created: String? = null
)

// ==========================================
// REVIEW ENTITY
// ==========================================


data class ReviewEntity(
    val id: String = "",
    
    val orderId: String = "",
    val productId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis(),
    val images: String = "",
    val created: String? = null
)

// ==========================================
// NOTIFICATION ENTITY
// ==========================================


data class NotificationEntity(
    val id: String = "",
    
    val userId: String = "",
    val title: String,
    val message: String,
    val type: String = "general", // "order", "promo", "alert", "general"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val deepLink: String? = null,
    val data: String? = null, // JSON data
    val created: String? = null
)

// ==========================================
// LOGIN HISTORY ENTITY
// ==========================================


data class LoginHistoryEntity(
    val id: String = "",
    
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deviceName: String = "Android Device",
    val deviceType: String = "Mobile",
    val operatingSystem: String = "Android",
    val osVersion: String = "",
    val ipAddress: String = "192.168.1.1",
    val location: String = "Addis Ababa, Ethiopia",
    val status: String = "SUCCESS", // "SUCCESS", "FAILED"
    val created: String? = null,
    val updated: String? = null,
    val pocketBaseId: String? = null,
    val brand: String = "Unknown",
    val browser: String = "App",
    val isActive: Boolean = true,
    val loginStatus: String = "success",
    val browserVersion: String = "1.0",
    val serialNumber: String = "",
    val model: String? = "",
    val sdkVersion: String? = "",
    val deviceFingerprint: String? = "",
    val isEmulator: Boolean? = false
)

// ==========================================
// SYNC QUEUE ENTITY (For Offline Sync)
// ==========================================


data class SyncQueueEntity(
    val id: String = "",
    
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val collection: String, // "products", "orders", "chat_messages"
    val recordId: String,
    val data: String, // JSON data
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val isSynced: Boolean = false,
    val errorMessage: String? = null
)

// ==========================================
// APP DATABASE
// ==========================================


typealias LoginHistoryItem = LoginHistoryEntity

data class SellerAnalytics(
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val totalProducts: Int = 0
)


// ==========================================
// seller financail 
// ==========================================


data class SellerRevenue(
    val id: String,
    val seller_id: String,
    val week: String,
    val month: String,
    val total_orders: Int = 0,
    val total_revenue: Double = 0.0,

    // ✅ Combined columns from SQL
    val top_categories_data: String? = null,   // "Electronics:5000.0:150|Clothing:3000.0:200|..."
    val top_products_data: String? = null      // "Coffee:3000.0:50|Tea:2000.0:30|..."
) {
    // ==========================================
    // TOP 10 CATEGORIES (name, revenue, units)
    // ==========================================

    val top10Categories: List<Triple<String, Double, Int>>
        get() = top_categories_data?.split("|")?.mapNotNull { part ->
            val parts = part.split(":")
            if (parts.size == 3) {
                val name = parts[0]
                val revenue = parts[1].toDoubleOrNull() ?: 0.0
                val units = parts[2].toIntOrNull() ?: 0
                Triple(name, revenue, units)
            } else null
        } ?: emptyList()

    // ==========================================
    // TOP 10 PRODUCTS (name, revenue, units)
    // ==========================================

    val top10Products: List<Triple<String, Double, Int>>
        get() = top_products_data?.split("|")?.mapNotNull { part ->
            val parts = part.split(":")
            if (parts.size == 3) {
                val name = parts[0]
                val revenue = parts[1].toDoubleOrNull() ?: 0.0
                val units = parts[2].toIntOrNull() ?: 0
                Triple(name, revenue, units)
            } else null
        } ?: emptyList()

    // ==========================================
    // CONVENIENCE: TOP 5 LISTS
    // ==========================================

    val top5Categories: List<Triple<String, Double, Int>>
        get() = top10Categories.take(5)

    val top5Products: List<Triple<String, Double, Int>>
        get() = top10Products.take(5)

    // ==========================================
    // FORMATTING HELPERS
    // ==========================================

    fun formatCurrency(amount: Double): String {
        return String.format("ETB %, .2f", amount)
    }

    fun formatNumber(number: Int): String {
        return String.format("%,d", number)
    }
}

data class SellerProductInventory(
    val id: String,
    val sellerId: String,
    val product_id: String,
    val product_name: String,
    val category: String,
    val subcategory: String? = null,
    val brand: String? = null,
    val stock: Int = 0,
    val price: Double = 0.0,
    val product_created: String,
    val days_old: Double = 0.0,
    val created_date: String,
    val total_sold: Int = 0,
    val total_revenue: Double = 0.0,
    val last_sold_date: String? = null
) {
   // ✅ Get today's Julian Day
    private fun getCurrentJulianDay(): Double {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return julianDay(year, month, day)
    }
    
    private fun julianDay(year: Int, month: Int, day: Int): Double {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045.0
    }
    
    // ✅ Calculate actual days old
    val actualDaysOld: Int
        get() = (getCurrentJulianDay() - days_old).toInt()
    
    
    val ageCategory: String
        get() = when {
            actualDaysOld <= 15 -> "Active"
            actualDaysOld <= 30 -> "Normal"
            actualDaysOld <= 60 -> "Slow"
            else -> "Dead"
        }
    
    val isDeadStock: Boolean
        get() = days_old > 60
    
    val isSlowStock: Boolean
        get() = days_old >= 31.0 && days_old <= 60.0
}


data class SellerDailySales(
    val id: String,
    val seller_id: String,
    val sale_date: String,
    val day_week: String,
    val total_orders: Int = 0,
    val total_sales: Double = 0.0,
    val product_cost: Double = 0.0,
    val net_profit: Double = 0.0,
    val completed_orders: Int = 0,
    val completed_sales: Double = 0.0,
    val canceled_orders: Int = 0,
    val canceled_sales: Double = 0.0,
    val unique_customers: Int = 0,

    // ✅ Combined columns from SQL
    val peak_hours_data: String? = null,           // "17:45|18:38|16:32|..."
    val top_categories_data: String? = null,       // "Electronics:5000.0:150|Clothing:3000.0:200|..."
    val top_brands_data: String? = null            // "Samsung:4000.0:120|Nike:2500.0:80|..."
) {
    // ==========================================
    // PEAK HOURS
    // ==========================================

    val peakHours: List<Pair<String, Int>>
        get() = peak_hours_data?.split("|")
            ?.mapNotNull { part ->
                val parts = part.split(":")
                if (parts.size == 2) {
                    parts[0] to (parts[1].toIntOrNull() ?: 0)
                } else null
            } ?: emptyList()

    val topPeakHour: Pair<String, Int>?
        get() = peakHours.firstOrNull()

    val topPeakHourDisplay: String
        get() = topPeakHour?.let {
            "${formatHour(it.first)} (${it.second} orders)"
        } ?: "No data"

    fun formatHour(hour: String): String {
        val h = hour.toIntOrNull() ?: 0
        val ampm = if (h >= 12) "PM" else "AM"
        val hour12 = when (h) {
            0 -> 12
            in 13..23 -> h - 12
            else -> h
        }
        return "$hour12:00 $ampm"
    }

    // ==========================================
    // TOP 10 CATEGORIES (name, revenue, units)
    // ==========================================

    val top10Categories: List<Triple<String, Double, Int>>
        get() = top_categories_data?.split("|")
            ?.mapNotNull { part ->
                val parts = part.split(":")
                if (parts.size == 3) {
                    val name = parts[0]
                    val revenue = parts[1].toDoubleOrNull() ?: 0.0
                    val units = parts[2].toIntOrNull() ?: 0
                    Triple(name, revenue, units)
                } else null
            } ?: emptyList()

    // ==========================================
    // TOP 10 BRANDS (name, revenue, units)
    // ==========================================

    val top10Brands: List<Triple<String, Double, Int>>
        get() = top_brands_data?.split("|")
            ?.mapNotNull { part ->
                val parts = part.split(":")
                if (parts.size == 3) {
                    val name = parts[0]
                    val revenue = parts[1].toDoubleOrNull() ?: 0.0
                    val units = parts[2].toIntOrNull() ?: 0
                    Triple(name, revenue, units)
                } else null
            } ?: emptyList()

    // ==========================================
    // DAY HELPERS
    // ==========================================

    val dayName: String
        get() = when (day_week) {
            "0" -> "Sunday"
            "1" -> "Monday"
            "2" -> "Tuesday"
            "3" -> "Wednesday"
            "4" -> "Thursday"
            "5" -> "Friday"
            "6" -> "Saturday"
            else -> "Unknown"
        }

    val dayShort: String
        get() = when (day_week) {
            "0" -> "Sun"
            "1" -> "Mon"
            "2" -> "Tue"
            "3" -> "Wed"
            "4" -> "Thu"
            "5" -> "Fri"
            "6" -> "Sat"
            else -> "??"
        }
}


// ==========================================
// SELLER MONTHLY STATS VIEW
// ==========================================

data class SellerMonthlyStats(
    val id: String,
    val seller_id: String,
    val month: String,          // "2025-01" to "2025-12"
    val year: String,           // "2024", "2025", etc.
    val gross_sales: Double = 0.0,
    val product_cost: Double = 0.0,
    val logistics_cost: Double = 0.0,
    val net_profit: Double = 0.0,
    val total_orders: Int = 0,
    val delivered_orders: Int = 0,
    val cancelled_orders: Int = 0,
    val refund_loss: Double = 0.0,
    val unique_customers: Int = 0,
    val sold_stocks: Int = 0,
    val unsold_stocks: Int = 0,
    
    // ✅ Combined columns from SQL
    val top_categories_data: String? = null,           // "Electronics:5000.0:150|Clothing:3000.0:200|..."
    val category_subcategory_data: String? = null,     // "Toys:Dolls:10,Action Figures:5|Electronics:Mobile Phone:20"
    
    // Payment Method Balances
    val telebirr_balance: Double = 0.0,
    val cbe_balance: Double = 0.0,
    val ebirr_balance: Double = 0.0,
) {
    // ==========================================
    // MONTH HELPERS
    // ==========================================
    
    val monthName: String
        get() {
            val cleanMonth = if (month.contains("-")) month.substringAfter("-") else month
            return when (cleanMonth) {
                "01", "1" -> "January"
                "02", "2" -> "February"
                "03", "3" -> "March"
                "04", "4" -> "April"
                "05", "5" -> "May"
                "06", "6" -> "June"
                "07", "7" -> "July"
                "08", "8" -> "August"
                "09", "9" -> "September"
                "10" -> "October"
                "11" -> "November"
                "12" -> "December"
                else -> month
            }
        }
    
    val monthYear: String
        get() = "$monthName $year"
    
    val monthShort: String
        get() {
            val cleanMonth = if (month.contains("-")) month.substringAfter("-") else month
            return when (cleanMonth) {
                "01", "1" -> "Jan"
                "02", "2" -> "Feb"
                "03", "3" -> "Mar"
                "04", "4" -> "Apr"
                "05", "5" -> "May"
                "06", "6" -> "Jun"
                "07", "7" -> "Jul"
                "08", "8" -> "Aug"
                "09", "9" -> "Sep"
                "10" -> "Oct"
                "11" -> "Nov"
                "12" -> "Dec"
                else -> month.take(3)
            }
        }
    
    // ==========================================
    // PERFORMANCE HELPERS
    // ==========================================
    
    val successRate: Double
        get() = if (total_orders > 0) {
            (delivered_orders.toDouble() / total_orders) * 100
        } else 0.0
    
    val profitMargin: Double
        get() = if (gross_sales > 0) {
            (net_profit / gross_sales) * 100
        } else 0.0
    
    val profitMarginFormatted: String
        get() = String.format("%.1f%%", profitMargin)
    
    val successRateFormatted: String
        get() = String.format("%.1f%%", successRate)
    
    // ==========================================
    // STOCK HELPERS
    // ==========================================
    
    val totalInventory: Int
        get() = sold_stocks + unsold_stocks
    
    val stockTurnoverRate: Double
        get() = if (totalInventory > 0) {
            sold_stocks.toDouble() / totalInventory
        } else 0.0
    
    val isStockHealthy: Boolean
        get() = unsold_stocks <= sold_stocks
    
    // ==========================================
    // CATEGORY HELPERS
    // ==========================================
    
    /**
     * Parse top categories as Triples: (categoryName, revenue, units)
     * Format: "Electronics:5000.0:150|Clothing:3000.0:200|..."
     */
    val topCategoriesWithRevenue: List<Triple<String, Double, Int>>
        get() = top_categories_data?.split("|")?.mapNotNull { part ->
            val parts = part.split(":")
            if (parts.size == 3) {
                val name = parts[0]
                val revenue = parts[1].toDoubleOrNull() ?: 0.0
                val units = parts[2].toIntOrNull() ?: 0
                Triple(name, revenue, units)
            } else null
        } ?: emptyList()
    
    /**
     * Parse top categories as Pairs: (categoryName, totalUnits)
     * Convenience for UI when revenue per category isn't needed
     */
    val topCategoriesList: List<Pair<String, Int>>
        get() = topCategoriesWithRevenue.map { it.first to it.third }
    
    // ==========================================
    // SUBCATEGORY HELPERS
    // ==========================================
    
    /**
     * Parse category → subcategory mapping
     * Format: "Toys:Dolls:10,Action Figures:5|Electronics:Mobile Phone:20"
     * Returns: Map<CategoryName, List<SubcategoryName to Units>>
     */
    val categorySubcategoryMap: Map<String, List<Pair<String, Int>>>
        get() = category_subcategory_data?.split("|")?.mapNotNull { part ->
            val segments = part.split(":")
            if (segments.size < 2) return@mapNotNull null
            val category = segments[0]
            // Rejoin remaining parts (in case subcategory names contain colons)
            val subPairs = segments.drop(1).joinToString(":").split(",")
                .mapNotNull { pair ->
                    val subParts = pair.split(":")
                    if (subParts.size == 2) {
                        subParts[0] to (subParts[1].toIntOrNull() ?: 0)
                    } else null
                }
            category to subPairs
        }?.toMap() ?: emptyMap()
    
    // ==========================================
    // BUILD CATEGORY SALE INFO
    // ==========================================
    
    val categorySales: List<CategorySaleInfo>
        get() = topCategoriesList.map { (catName, totalUnits) ->
            val subItems = categorySubcategoryMap[catName] ?: emptyList()
            CategorySaleInfo(catName, totalUnits, subItems)
        }
    
    // ==========================================
    // PAYMENT HELPERS
    // ==========================================
    
    val paymentBreakdown: Map<String, Double>
        get() = mapOf(
            "Telebirr" to telebirr_balance,
            "CBE" to cbe_balance,
            "Ebirr" to ebirr_balance
        ).filter { it.value > 0 }
    
    val totalPaymentCollected: Double
        get() = paymentBreakdown.values.sum()
    
    val topPaymentMethod: Pair<String, Double>?
        get() = paymentBreakdown.maxByOrNull { it.value }?.toPair()
    
    val topPaymentMethodName: String
        get() = topPaymentMethod?.first ?: "None"
    
    val topPaymentMethodAmount: Double
        get() = topPaymentMethod?.second ?: 0.0
    
    // ==========================================
    // FORMATTING HELPERS
    // ==========================================
    
    fun formatCurrency(amount: Double): String {
        return String.format("ETB %, .2f", amount)
    }
    
    fun formatNumber(number: Int): String {
        return String.format("%,d", number)
    }
}

// ==========================================
// DATA CLASS FOR CATEGORY SALE INFO
// ==========================================

data class CategorySaleInfo(
    val name: String,
    val soldCount: Int,
    val items: List<Pair<String, Int>>  // subcategory name → units sold
)
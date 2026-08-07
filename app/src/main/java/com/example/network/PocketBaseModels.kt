package com.example.network

import kotlinx.serialization.Serializable
import com.google.gson.JsonDeserializer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import java.lang.reflect.Type
import com.example.data.SellerRevenue
import com.example.data.SellerProductInventory
import com.example.data.SellerDailySales
import com.example.data.SellerMonthlyStats

typealias PbUserRecord = PocketBaseUserRecord
typealias PbAuthResponse = PocketBaseAuthResponse

// MAIN USER COLLECTION
@Serializable
data class PocketBaseUserRecord(
    val id: String,
    val email: String? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val profile_image: String? = null,
    val addressRegion: String? = null,
    val addressZone: String? = null,
    val addressCity: String? = null,
    val addressKebele: String? = null,
    val birthDate: String? = null,
    val idDocumentInfront: String? = null,
    val idDocumentBackfront: String? = null,
    val role: String? = "BUYER",
    val verified: Boolean? = true,
    val emailVisibility: Boolean? = true,
    val isApproved: Boolean? = true,
    val isTwoFactorEnabled: Boolean? = false,
    val status: Boolean? = false,
    val referCode: String? = null,
    val fromReferCode: String? = null,
    val created: String? = null,
    val updated: String? = null,
    val expand: ExpandedData? = null
)

@Serializable
data class ExpandedData(
    val seller_profile: SellerProfile? = null,
    val delivery_profile: DeliveryProfile? = null
)

typealias StoreLocation = com.example.data.StoreLocation

@Serializable
data class BusinessPayoutInfo(
    val example: Double? = null,
    val cbe: String? = null,
    val telebirr: String? = null,
    val ebirr: String? = null,
    val name: String? = null
) {
    override fun toString(): String {
        val parts = mutableListOf<String>()
        cbe?.let { parts.add("CBE: $it") }
        telebirr?.let { parts.add("Telebirr: $it") }
        ebirr?.let { parts.add("Ebirr: $it") }
        name?.let { parts.add("Name: $it") }
        if (parts.isEmpty()) {
            return example?.let { "example: $it" } ?: ""
        }
        return parts.joinToString(" | ")
    }
}

class BusinessPayoutInfoDeserializer : JsonDeserializer<BusinessPayoutInfo> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BusinessPayoutInfo {
        try {
            if (json.isJsonPrimitive) {
                val str = json.asString
                if (str.isEmpty()) return BusinessPayoutInfo()
                val cbe = if (str.contains("CBE:")) str.substringAfter("CBE:").substringBefore("|").trim() else null
                val telebirr = if (str.contains("Telebirr:")) str.substringAfter("Telebirr:").substringBefore("|").trim() else null
                val ebirr = if (str.contains("Ebirr:")) str.substringAfter("Ebirr:").substringBefore("|").trim() else null
                val name = if (str.contains("Name:")) str.substringAfter("Name:").substringBefore("|").trim() else null
                return BusinessPayoutInfo(cbe = cbe, telebirr = telebirr, ebirr = ebirr, name = name)
            } else if (json.isJsonObject) {
                val obj = json.asJsonObject
                val example = obj.get("example")?.let { if (it.isJsonPrimitive) it.asDouble else null }
                val cbe = obj.get("cbe")?.let { if (it.isJsonPrimitive) it.asString else null }
                val telebirr = obj.get("telebirr")?.let { if (it.isJsonPrimitive) it.asString else null }
                val ebirr = obj.get("ebirr")?.let { if (it.isJsonPrimitive) it.asString else null }
                val name = obj.get("name")?.let { if (it.isJsonPrimitive) it.asString else null }
                return BusinessPayoutInfo(example = example, cbe = cbe, telebirr = telebirr, ebirr = ebirr, name = name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return BusinessPayoutInfo()
    }
}

@Serializable
data class BusinessDiscountCodeInfo(
    val example: Double? = null,
    val code: String? = null,
    val percent: Double? = null
) {
    override fun toString(): String {
        val parts = mutableListOf<String>()
        code?.let { parts.add("Code: $it") }
        percent?.let { parts.add("Percent: $it") }
        if (parts.isEmpty()) {
            return example?.let { "example: $it" } ?: ""
        }
        return parts.joinToString(" | ")
    }
}

class BusinessDiscountCodeInfoDeserializer : JsonDeserializer<BusinessDiscountCodeInfo> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BusinessDiscountCodeInfo {
        try {
            if (json.isJsonPrimitive) {
                val str = json.asString
                if (str.isEmpty()) return BusinessDiscountCodeInfo()
                val code = if (str.contains("Code:")) str.substringAfter("Code:").substringBefore("|").trim() else null
                val percent = if (str.contains("Percent:")) str.substringAfter("Percent:").substringBefore("|").trim().toDoubleOrNull() else null
                return BusinessDiscountCodeInfo(code = code, percent = percent)
            } else if (json.isJsonObject) {
                val obj = json.asJsonObject
                val example = obj.get("example")?.let { if (it.isJsonPrimitive) it.asDouble else null }
                val code = obj.get("code")?.let { if (it.isJsonPrimitive) it.asString else null }
                val percent = obj.get("percent")?.let { if (it.isJsonPrimitive) it.asDouble else null }
                return BusinessDiscountCodeInfo(example = example, code = code, percent = percent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return BusinessDiscountCodeInfo()
    }
}

// SELLER PROFILE COLLECTION

@Serializable
data class SellerProfile(
    val id: String,
    val user: String,  // relation to users.id
    val seller_profile_image: String? = null,
    val seller_profile_cover: String? = null,
    val businessName: String? = null,
    val businessPhone: String? = null,
    val region: String? = null,
    val zone: String? = null,
    val city: String? = null,
    val kebele: String? = null,
    val tinNumber: String? = null,
    val tlcNumber: String? = null,  // Trade License Certificate
    val businessDocumentFront: String? = null,
    val businessDocumentBack: String? = null,
    val businessGradeTitel: String? = null,
    val businessGrade: Double? = null,
    val businessDescription: String? = null,
    val businessWorkingHours: String? = null,
    val businessPayout: BusinessPayoutInfo? = null,
    val businessRating: Double? = null,
    val businessDiscountCode: BusinessDiscountCodeInfo? = null,
    val sellerVerification: String? = "pending",  // "pending", "approved", "rejected"
    val status: Boolean? = false,
    val storeLocation: StoreLocation? = null,
    val created: String? = null,
    val updated: String? = null
)

// DELIVERY PROFILE COLLECTION

@Serializable
data class DeliveryProfile(
    val id: String,
    val user: String,  // relation to users.id
    val plateNumber: String? = null,
    val phoneNumber: String? = null,
    val vehicleType: String? = null,
    val driverLicenseFront: String? = null,
    val driverLicenseBack: String? = null,
    val vehicleDocumentFront: String? = null,
    val vehicleDocumentBack: String? = null,
    val deliveryGrade: String? = null,
    val xpTotal: Int? = 0,
    val ratingTotal: Double? = 0.0,
    val deliveryVerification: String? = "pending",  // "pending", "approved", "rejected"
    val status: Boolean? = false,
    val created: String? = null,
    val updated: String? = null
)

@Serializable
data class PocketBaseAuthResponse(
    val token: String,
    val record: PocketBaseUserRecord
)

@Serializable
data class PbListResponse<T>(
    val page: Int,
    val perPage: Int,
    val totalItems: Int,
    val totalPages: Int,
    val items: List<T>
)

@Serializable
data class PbSellerProduct(
    val id: String? = null,
    val sellerId: String? = null,
    val sellerName: String = "",
    val name: String,
    val price: Double,
    val stock: Int,
    val category: String,
    val subcategory: String? = "",
    val imageResName: com.google.gson.JsonElement? = null,
    val imageUrlName: String? = "",
    val isPromoted: Boolean? = false,
    val impressions: Int = 0,
    val clicks: Int = 0,
    val description: String? = "Premium product",
    val unitOfMeasurement: String? = "pieces",
    val stateNewOrUsed: String? = "New",
    val isFreeDelivery: Boolean? = false,
    val discountPercent: Int? = 0,
    val brand: String? = "",
    val variants: String? = "",
    val rating: Double? = 4.5,
    val cost: Double? = 0.0,
    val sold: Int? = 0,
    val lowStock: Int? = 3,
    val created: String? = null,
    val updated: String? = null
) {
    fun parseImageResName(): String {
        val elem = imageResName ?: return ""
        return try {
            when {
                elem.isJsonArray -> {
                    val arr = elem.asJsonArray
                    if (arr.size() > 0 && !arr[0].isJsonNull) arr[0].asString else ""
                }
                elem.isJsonPrimitive -> elem.asString
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}

// ==========================================
// FOR ALL USER PRODUCTS VIEW
// ==========================================

@Serializable
data class PbProductEntity(
    val id: String? = null,
    val sellerId: String? = null,
    val sellerName: String = "",
    val name: String,
    val price: Double,
    val stock: Int,
    val sold: Int? = 0,
    val category: String,
    val subcategory: String? = "",
    val brand: String? = "",
    val description: String? = "",
    val imageResName: com.google.gson.JsonElement? = null,
    val imageUrlName: String? = "",
    val isPromoted: Boolean? = false,
    val discountPercent: Int? = 0,
    val isFreeDelivery: Boolean? = false,
    val variants: String? = "",
    val stateNewOrUsed: String? = "New",
    val unitOfMeasurement: String? = "pieces",
    val rating: Double? = 4.5,
    val reviewCount: Int? = 0,
    val impressions: Int = 0,
    val clicks: Int = 0,
    val created: String? = null,
    val updated: String? = null
) {
    fun parseImageResName(): String {
        val elem = imageResName ?: return ""
        return try {
            when {
                elem.isJsonArray -> {
                    val arr = elem.asJsonArray
                    if (arr.size() > 0 && !arr[0].isJsonNull) arr[0].asString else ""
                }
                elem.isJsonPrimitive -> elem.asString
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}

@Serializable
data class PbOrderItem(
    val id: String? = null,
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
    val payment_method: String? = null,
    val created: String? = null,
    val updated: String? = null
)

@Serializable
data class PbOrder(
    val id: String? = null,
    val buyerId: String = "",
    val sellerId: String = "",
    val deliveryId: String? = null,
    val product_names: String = "",
    // ❌ REMOVED: productId
    val total_price: Double = 0.0,
    val delivery_price: Double = 0.0,
    val unit: String = "pieces",
    val payment_method: String = "Telebirr",
    val status: String = "PENDING",
    val shippingAddress: com.example.data.ShippingAddress? = null,
    val storeLocation: StoreLocation? = null,
    val driverLocation: com.example.data.DriverLocation? = null,
    val buyer_verif_code: String? = null,
    val delivery_verif_code: String? = null,
    val courier_name: String? = null,
    val courier_plate: String? = null,
    val delivery_pickup: Boolean = false,
    val is_self_pickup: Boolean = false,
    val paymentReceipt: String? = null,
    val paymentReference: String? = null,
    val paymentBackReceipt: String? = null,
    val paymentBackReference: String? = null,
    val cancellation_reason: String? = null,
    val buyerName: String = "",
    val buyerPhone: String = "",
    val sellerName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val created: String? = null,
    val updated: String? = null
)

@Serializable
data class DeliveryLocation(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

typealias LocationStatusPb = DeliveryLocation

@Serializable
data class PbChatMessage(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val messageText: String,
    val attachmentType: String? = null,
    val attachmentUrl: String? = null,
    val replyToId: String? = null,
    val replyToMessageText: String? = null,
    val isLiked: Boolean? = false,
    val reactionEmoji: String? = null,
    val status: String? = "SENT",
    val isDeleted: Boolean? = false,
    val timestamp: Long
)

@Serializable
data class PbAddress(
    val id: String,
    val userId: String,
    val label: String,
    val region: String,
    val zone: String,
    val city: String,
    val kebele: String,
    val streetHouseNumber: String,
    val isDefault: Boolean? = false,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0
)

@Serializable
data class PbLoginHistoryItem(
    val id: String? = null,
    val user: String,
    val device_name: String,
    val device_type: String,
    val operatingSystem: String,
    val osVersion: String,
    val browser: String,
    val ip_address: String,
    val location: String,
    val brand: String,
    val is_active: Boolean,
    val login_status: String,
    val browser_version: String,
    val serial_number: String,
    val created: String? = null,
    val updated: String? = null,
    val model: String? = "",
    val sdk_version: String? = "",
    val device_fingerprint: String? = "",
    val is_emulator: Boolean? = false
)

data class PbReviewProduct(
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

@Serializable
data class PbReviewStore(
    val id: String = "",
    val productId: String? = null,
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val replyText: String? = null,
    val created: String? = null,
    val updated: String? = null
)

// SELLER FINANCIAL / ANALYTICS POCKETBASE MODELS

@Serializable
data class PbSellerRevenue(
    val id: String = "",
    val seller_id: String = "",
    val week: String = "",
    val month: String = "",
    val total_orders: Int? = 0,
    val total_revenue: Double? = 0.0,
    // ✅ Combined columns
    val top_categories_data: String? = null,   // "Electronics:5000.0:150|Clothing:3000.0:200|..."
    val top_products_data: String? = null,      // "Coffee:3000.0:50|Tea:2000.0:30|..."
    val created: String? = null,
    val updated: String? = null
) {
    fun toDomainModel(): SellerRevenue = SellerRevenue(
        id = id,
        seller_id = seller_id,
        week = week,
        month = month,
        total_orders = total_orders ?: 0,
        total_revenue = total_revenue ?: 0.0,
        top_categories_data = top_categories_data,
        top_products_data = top_products_data
    )
}

@Serializable
data class PbSellerProductInventory(
    val id: String = "",
    val sellerId: String = "",
    val product_id: String = "",
    val product_name: String = "",
    val category: String = "",
    val subcategory: String? = null,
    val brand: String? = null,
    val stock: Int? = 0,
    val price: Double? = 0.0,
    val product_created: String = "",
    val days_old: Double? = 0.0,
    val created_date: String = "",
    val total_sold: Int? = 0,
    val total_revenue: Double? = 0.0,
    val last_sold_date: String? = null,
    val created: String? = null,
    val updated: String? = null
) {
    fun toDomainModel(): SellerProductInventory = SellerProductInventory(
        id = id,
        sellerId = sellerId,
        product_id = product_id,
        product_name = product_name,
        category = category,
        subcategory = subcategory,
        brand = brand,
        stock = stock ?: 0,
        price = price ?: 0.0,
        product_created = product_created,
        days_old = days_old ?: 0.0,
        created_date = created_date,
        total_sold = total_sold ?: 0,
        total_revenue = total_revenue ?: 0.0,
        last_sold_date = last_sold_date
    )
}

@Serializable
data class PbSellerDailySales(
    val id: String = "",
    val seller_id: String = "",
    val sale_date: String = "",
    val day_week: String = "",
    // ✅ Combined columns
    val peak_hours_data: String? = null,          // "17:45|18:38|..."
    val top_categories_data: String? = null,      // "Electronics:5000.0:150|..."
    val top_brands_data: String? = null,          // "Samsung:4000.0:120|..."
    // Numeric fields (use Double for decimals)
    val total_orders: Int? = 0,
    val total_sales: Double? = 0.0,
    val product_cost: Double? = 0.0,
    val net_profit: Double? = 0.0,
    val completed_orders: Int? = 0,
    val completed_sales: Double? = 0.0,
    val canceled_orders: Int? = 0,
    val canceled_sales: Double? = 0.0,
    val unique_customers: Int? = 0,
    val created: String? = null,
    val updated: String? = null
) {
    fun toDomainModel(): SellerDailySales = SellerDailySales(
        id = id,
        seller_id = seller_id,
        sale_date = sale_date,
        day_week = day_week,
        peak_hours_data = peak_hours_data,
        top_categories_data = top_categories_data,
        top_brands_data = top_brands_data,
        total_orders = total_orders ?: 0,
        total_sales = total_sales ?: 0.0,
        product_cost = product_cost ?: 0.0,
        net_profit = net_profit ?: 0.0,
        completed_orders = completed_orders ?: 0,
        completed_sales = completed_sales ?: 0.0,
        canceled_orders = canceled_orders ?: 0,
        canceled_sales = canceled_sales ?: 0.0,
        unique_customers = unique_customers ?: 0
    )
}

@Serializable
data class PbSellerMonthlyStats(
    val id: String = "",
    val seller_id: String = "",
    val month: String = "",
    val year: String = "",
    // Financials (use Double for decimals)
    val gross_sales: Double? = 0.0,
    val product_cost: Double? = 0.0,
    val logistics_cost: Double? = 0.0,
    val net_profit: Double? = 0.0,
    val total_orders: Int? = 0,
    val delivered_orders: Int? = 0,
    val cancelled_orders: Int? = 0,
    val refund_loss: Double? = 0.0,
    val unique_customers: Int? = 0,
    val sold_stocks: Int? = 0,
    val unsold_stocks: Int? = 0,
    // Combined columns
    val top_categories_data: String? = null,          // "Electronics:5000.0:150|..."
    val category_subcategory_data: String? = null,    // "Toys:Dolls:10,Action Figures:5|..."
    // Payment balances
    val telebirr_balance: Double? = 0.0,
    val cbe_balance: Double? = 0.0,
    val ebirr_balance: Double? = 0.0,
    val created: String? = null,
    val updated: String? = null
) {
    fun toDomainModel(): SellerMonthlyStats = SellerMonthlyStats(
        id = id,
        seller_id = seller_id,
        month = month,
        year = year,
        gross_sales = gross_sales ?: 0.0,
        product_cost = product_cost ?: 0.0,
        logistics_cost = logistics_cost ?: 0.0,
        net_profit = net_profit ?: 0.0,
        total_orders = total_orders ?: 0,
        delivered_orders = delivered_orders ?: 0,
        cancelled_orders = cancelled_orders ?: 0,
        refund_loss = refund_loss ?: 0.0,
        unique_customers = unique_customers ?: 0,
        sold_stocks = sold_stocks ?: 0,
        unsold_stocks = unsold_stocks ?: 0,
        top_categories_data = top_categories_data,
        category_subcategory_data = category_subcategory_data,
        telebirr_balance = telebirr_balance ?: 0.0,
        cbe_balance = cbe_balance ?: 0.0,
        ebirr_balance = ebirr_balance ?: 0.0
    )
}

@Serializable
data class WithdrawalHistoryItem(
    val id: String = "",
    val amount: Double = 0.0,
    val status: String = "pending",
    val paymentMethod: String = "",
    val paymentAccount: String = "",
    val reference: String = "",
    val created: String = "",
    val updated: String = ""
)

@Serializable
data class PbReferEarnUser(
    val id: String = "",
    val user: String? = "",
    val referCode: String? = "",
    val referralCount: Int? = 0,
    val totalMoney: Double? = 0.0,
    val pendingMoney: Double? = 0.0,
    val pendingUserIds: com.google.gson.JsonElement? = null,
    val withdrawnMoney: Double? = 0.0,
    val withdrawalHistory: com.google.gson.JsonElement? = null,
    val created: String? = null,
    val updated: String? = null
) {
    fun toDomainModel(): ReferEarnUser {
        val parsedPendingUserIds = mutableListOf<String>()
        if (pendingUserIds != null) {
            try {
                if (pendingUserIds.isJsonArray) {
                    pendingUserIds.asJsonArray.forEach { elem ->
                        if (elem.isJsonPrimitive) parsedPendingUserIds.add(elem.asString)
                    }
                } else if (pendingUserIds.isJsonPrimitive) {
                    val str = pendingUserIds.asString
                    if (str.isNotBlank()) parsedPendingUserIds.add(str)
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        val parsedHistory = mutableListOf<WithdrawalHistoryItem>()
        if (withdrawalHistory != null) {
            try {
                val array = if (withdrawalHistory.isJsonArray) {
                    withdrawalHistory.asJsonArray
                } else if (withdrawalHistory.isJsonPrimitive) {
                    com.google.gson.JsonParser.parseString(withdrawalHistory.asString).asJsonArray
                } else null

                array?.forEach { elem ->
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        parsedHistory.add(
                            WithdrawalHistoryItem(
                                id = obj.get("id")?.asString ?: "",
                                amount = obj.get("amount")?.asDouble ?: 0.0,
                                status = obj.get("status")?.asString ?: "pending",
                                paymentMethod = obj.get("paymentMethod")?.asString ?: "",
                                paymentAccount = obj.get("paymentAccount")?.asString ?: "",
                                reference = obj.get("reference")?.asString ?: "",
                                created = obj.get("created")?.asString ?: "",
                                updated = obj.get("updated")?.asString ?: ""
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        val effectivePendingMoney = if (pendingMoney != null && pendingMoney > 0.0) {
            pendingMoney
        } else if (totalMoney != null && totalMoney > 0.0) {
            totalMoney
        } else {
            0.0
        }

        return ReferEarnUser(
            id = id,
            user = user ?: "",
            referCode = referCode ?: "",
            referralCount = referralCount ?: 0,
            totalMoney = totalMoney ?: 0.0,
            pendingMoney = effectivePendingMoney,
            pendingUserIds = parsedPendingUserIds,
            withdrawnMoney = withdrawnMoney ?: 0.0,
            withdrawalHistory = parsedHistory
        )
    }
}

@Serializable
data class ReferEarnUser(
    val id: String = "",
    val user: String = "",
    val referCode: String = "",
    val referralCount: Int = 0,
    val totalMoney: Double = 0.0,
    val pendingMoney: Double = 0.0,
    val pendingUserIds: List<String> = emptyList(),
    val withdrawnMoney: Double = 0.0,
    val withdrawalHistory: List<WithdrawalHistoryItem> = emptyList()
)
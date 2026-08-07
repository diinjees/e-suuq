package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "seller_daily_sales")
data class SellerDailySalesEntity(
    @PrimaryKey val id: String,
    val seller_id: String,
    val sale_date: String,
    val day_week: String,
    val peak_hours_data: String? = null,
    val product_cost: Double = 0.0,
    val net_profit: Double = 0.0,
    val total_orders: Int = 0,
    val total_sales: Double = 0.0,
    val completed_orders: Int = 0,
    val completed_sales: Double = 0.0,
    val canceled_orders: Int = 0,
    val canceled_sales: Double = 0.0,
    val unique_customers: Int = 0,
    val top_categories_data: String? = null,
    val top_brands_data: String? = null
)

fun SellerDailySales.toEntity(): SellerDailySalesEntity {
    return SellerDailySalesEntity(
        id = id,
        seller_id = seller_id,
        sale_date = sale_date,
        day_week = day_week,
        peak_hours_data = peak_hours_data,
        product_cost = product_cost,
        net_profit = net_profit,
        total_orders = total_orders,
        total_sales = total_sales,
        completed_orders = completed_orders,
        completed_sales = completed_sales,
        canceled_orders = canceled_orders,
        canceled_sales = canceled_sales,
        unique_customers = unique_customers,
        top_categories_data = top_categories_data,
        top_brands_data = top_brands_data
    )
}

fun SellerDailySalesEntity.toDomainModel(): SellerDailySales {
    return SellerDailySales(
        id = id,
        seller_id = seller_id,
        sale_date = sale_date,
        day_week = day_week,
        peak_hours_data = peak_hours_data,
        product_cost = product_cost,
        net_profit = net_profit,
        total_orders = total_orders,
        total_sales = total_sales,
        completed_orders = completed_orders,
        completed_sales = completed_sales,
        canceled_orders = canceled_orders,
        canceled_sales = canceled_sales,
        unique_customers = unique_customers,
        top_categories_data = top_categories_data,
        top_brands_data = top_brands_data
    )
}

@Entity(tableName = "seller_revenue")
data class SellerRevenueEntity(
    @PrimaryKey val id: String,
    val seller_id: String,
    val week: String,
    val month: String,
    val total_orders: Int = 0,
    val total_revenue: Double = 0.0,
    val top_categories_data: String? = null,
    val top_products_data: String? = null
)

fun SellerRevenue.toEntity(): SellerRevenueEntity {
    return SellerRevenueEntity(
        id = id,
        seller_id = seller_id,
        week = week,
        month = month,
        total_orders = total_orders,
        total_revenue = total_revenue,
        top_categories_data = top_categories_data,
        top_products_data = top_products_data
    )
}

fun SellerRevenueEntity.toDomainModel(): SellerRevenue {
    return SellerRevenue(
        id = id,
        seller_id = seller_id,
        week = week,
        month = month,
        total_orders = total_orders,
        total_revenue = total_revenue,
        top_categories_data = top_categories_data,
        top_products_data = top_products_data
    )
}

@Entity(tableName = "seller_monthly_stats")
data class SellerMonthlyStatsEntity(
    @PrimaryKey val id: String,
    val seller_id: String,
    val month: String,
    val year: String,
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
    val top_categories_data: String? = null,
    val category_subcategory_data: String? = null,
    val telebirr_balance: Double = 0.0,
    val cbe_balance: Double = 0.0,
    val ebirr_balance: Double = 0.0
)

fun SellerMonthlyStats.toEntity(): SellerMonthlyStatsEntity {
    return SellerMonthlyStatsEntity(
        id = id,
        seller_id = seller_id,
        month = month,
        year = year,
        gross_sales = gross_sales,
        product_cost = product_cost,
        logistics_cost = logistics_cost,
        net_profit = net_profit,
        total_orders = total_orders,
        delivered_orders = delivered_orders,
        cancelled_orders = cancelled_orders,
        refund_loss = refund_loss,
        unique_customers = unique_customers,
        sold_stocks = sold_stocks,
        unsold_stocks = unsold_stocks,
        top_categories_data = top_categories_data,
        category_subcategory_data = category_subcategory_data,
        telebirr_balance = telebirr_balance,
        cbe_balance = cbe_balance,
        ebirr_balance = ebirr_balance
    )
}

fun SellerMonthlyStatsEntity.toDomainModel(): SellerMonthlyStats {
    return SellerMonthlyStats(
        id = id,
        seller_id = seller_id,
        month = month,
        year = year,
        gross_sales = gross_sales,
        product_cost = product_cost,
        logistics_cost = logistics_cost,
        net_profit = net_profit,
        total_orders = total_orders,
        delivered_orders = delivered_orders,
        cancelled_orders = cancelled_orders,
        refund_loss = refund_loss,
        unique_customers = unique_customers,
        sold_stocks = sold_stocks,
        unsold_stocks = unsold_stocks,
        top_categories_data = top_categories_data,
        category_subcategory_data = category_subcategory_data,
        telebirr_balance = telebirr_balance,
        cbe_balance = cbe_balance,
        ebirr_balance = ebirr_balance
    )
}

@Dao
interface SellerDailySalesDao {
    @Query("SELECT * FROM seller_daily_sales WHERE seller_id = :sellerId ORDER BY sale_date DESC")
    suspend fun getDailySalesBySeller(sellerId: String): List<SellerDailySalesEntity>

    @Query("SELECT * FROM seller_daily_sales ORDER BY sale_date DESC")
    suspend fun getAllDailySales(): List<SellerDailySalesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SellerDailySalesEntity>)

    @Query("DELETE FROM seller_daily_sales WHERE seller_id = :sellerId")
    suspend fun deleteBySeller(sellerId: String)
}

@Dao
interface SellerRevenueDao {
    @Query("SELECT * FROM seller_revenue WHERE seller_id = :sellerId ORDER BY month DESC, week DESC")
    suspend fun getRevenueBySeller(sellerId: String): List<SellerRevenueEntity>

    @Query("SELECT * FROM seller_revenue ORDER BY month DESC, week DESC")
    suspend fun getAllRevenue(): List<SellerRevenueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SellerRevenueEntity>)

    @Query("DELETE FROM seller_revenue WHERE seller_id = :sellerId")
    suspend fun deleteBySeller(sellerId: String)
}

@Dao
interface SellerMonthlyStatsDao {
    @Query("SELECT * FROM seller_monthly_stats WHERE seller_id = :sellerId ORDER BY year DESC, month DESC")
    suspend fun getMonthlyStatsBySeller(sellerId: String): List<SellerMonthlyStatsEntity>

    @Query("SELECT * FROM seller_monthly_stats ORDER BY year DESC, month DESC")
    suspend fun getAllMonthlyStats(): List<SellerMonthlyStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SellerMonthlyStatsEntity>)

    @Query("DELETE FROM seller_monthly_stats WHERE seller_id = :sellerId")
    suspend fun deleteBySeller(sellerId: String)
}

@Entity(tableName = "seller_products")
data class SellerProductDbEntity(
    @PrimaryKey val id: String,
    val sellerId: String,
    val sellerName: String,
    val name: String,
    val price: Double,
    val cost: Double,
    val stock: Int,
    val category: String,
    val subcategory: String,
    val imageResName: String,
    val imageUrlName: String,
    val isPromoted: Boolean,
    val impressions: Int,
    val clicks: Int,
    val description: String,
    val unitOfMeasurement: String,
    val stateNewOrUsed: String,
    val isFreeDelivery: Boolean,
    val discountPercent: Int,
    val brand: String,
    val variants: String,
    val rating: Double,
    val sold: Int,
    val lowStock: Int = 3,
    val created: String?,
    val updated: String?
)

fun SellerProductEntity.toEntity(): SellerProductDbEntity {
    return SellerProductDbEntity(
        id = id,
        sellerId = sellerId,
        sellerName = sellerName,
        name = name,
        price = price,
        cost = cost,
        stock = stock,
        category = category,
        subcategory = subcategory,
        imageResName = imageResName,
        imageUrlName = imageUrlName,
        isPromoted = isPromoted,
        impressions = impressions,
        clicks = clicks,
        description = description,
        unitOfMeasurement = unitOfMeasurement,
        stateNewOrUsed = stateNewOrUsed,
        isFreeDelivery = isFreeDelivery,
        discountPercent = discountPercent,
        brand = brand,
        variants = variants,
        rating = rating,
        sold = sold,
        lowStock = lowStock,
        created = created,
        updated = updated
    )
}

fun SellerProductDbEntity.toDomainModel(): SellerProductEntity {
    return SellerProductEntity(
        id = id,
        sellerId = sellerId,
        sellerName = sellerName,
        name = name,
        price = price,
        cost = cost,
        stock = stock,
        category = category,
        subcategory = subcategory,
        imageResName = imageResName,
        imageUrlName = imageUrlName,
        isPromoted = isPromoted,
        impressions = impressions,
        clicks = clicks,
        description = description,
        unitOfMeasurement = unitOfMeasurement,
        stateNewOrUsed = stateNewOrUsed,
        isFreeDelivery = isFreeDelivery,
        discountPercent = discountPercent,
        brand = brand,
        variants = variants,
        rating = rating,
        sold = sold,
        lowStock = lowStock,
        created = created,
        updated = updated
    )
}

@Dao
interface SellerProductDao {
    @Query("SELECT * FROM seller_products")
    suspend fun getAllProducts(): List<SellerProductDbEntity>

    @Query("SELECT * FROM seller_products WHERE sellerId = :sellerId")
    suspend fun getProductsBySeller(sellerId: String): List<SellerProductDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SellerProductDbEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: SellerProductDbEntity)

    @Query("DELETE FROM seller_products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("DELETE FROM seller_products WHERE sellerId = :sellerId")
    suspend fun deleteBySeller(sellerId: String)
}

@Entity(tableName = "seller_orders")
data class SellerOrderDbEntity(
    @PrimaryKey val id: String,
    val buyerId: String,
    val sellerId: String,
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
    val shippingAddressJson: String = "",
    val storeLocationJson: String = "",
    val buyerVerifCode: String? = null,
    val deliveryVerifCode: String? = null,
    val courierName: String? = null,
    val courierPlate: String? = null,
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
    val itemsJson: String = "[]"
)

fun SellerOrderEntity.toDbEntity(): SellerOrderDbEntity {
    val itemsArray = org.json.JSONArray()
    items.forEach { item ->
        val obj = org.json.JSONObject().apply {
            put("id", item.id)
            put("orderId", item.orderId)
            put("productId", item.productId)
            put("productName", item.productName)
            put("category", item.category ?: "")
            put("subcategory", item.subcategory ?: "")
            put("brand", item.brand ?: "")
            put("quantity", item.quantity)
            put("unitPrice", item.unitPrice)
            put("subtotal", item.subtotal)
            put("unit", item.unit)
            put("sellerId", item.sellerId)
            put("buyerId", item.buyerId)
        }
        itemsArray.put(obj)
    }

    val addrObj = org.json.JSONObject().apply {
        put("lat", shippingAddress.lat)
        put("lon", shippingAddress.lon)
    }

    val storeObj = org.json.JSONObject().apply {
        put("lat", storeLocation.lat)
        put("lon", storeLocation.lon)
    }

    return SellerOrderDbEntity(
        id = id,
        buyerId = buyerId,
        sellerId = sellerId,
        deliveryId = deliveryId,
        buyerName = buyerName,
        buyerPhone = buyerPhone,
        sellerName = sellerName,
        productNames = productNames,
        totalPrice = totalPrice,
        deliveryPrice = deliveryPrice,
        unit = unit,
        paymentMethod = paymentMethod,
        status = status,
        shippingAddressJson = addrObj.toString(),
        storeLocationJson = storeObj.toString(),
        buyerVerifCode = buyerVerifCode,
        deliveryVerifCode = deliveryVerifCode,
        courierName = courierName,
        courierPlate = courierPlate,
        deliveryPickup = deliveryPickup,
        isSelfPickup = isSelfPickup,
        paymentReceipt = paymentReceipt,
        paymentReference = paymentReference,
        paymentBackReceipt = paymentBackReceipt,
        paymentBackReference = paymentBackReference,
        cancellationReason = cancellationReason,
        timestamp = timestamp,
        created = created,
        updated = updated,
        itemsJson = itemsArray.toString()
    )
}

fun SellerOrderDbEntity.toDomainModel(): SellerOrderEntity {
    val itemsList = mutableListOf<SellerOrderItemEntity>()
    try {
        if (itemsJson.isNotBlank()) {
            val array = org.json.JSONArray(itemsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                itemsList.add(
                    SellerOrderItemEntity(
                        id = obj.optString("id", ""),
                        orderId = obj.optString("orderId", ""),
                        productId = obj.optString("productId", ""),
                        productName = obj.optString("productName", ""),
                        category = obj.optString("category", null),
                        subcategory = obj.optString("subcategory", null),
                        brand = obj.optString("brand", null),
                        quantity = obj.optInt("quantity", 1),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        subtotal = obj.optDouble("subtotal", 0.0),
                        unit = obj.optString("unit", "pieces"),
                        sellerId = obj.optString("sellerId", ""),
                        buyerId = obj.optString("buyerId", "")
                    )
                )
            }
        }
    } catch (e: Exception) {}

    var shipAddr = ShippingAddress()
    try {
        if (shippingAddressJson.isNotBlank()) {
            val obj = org.json.JSONObject(shippingAddressJson)
            shipAddr = ShippingAddress(
                lat = obj.optDouble("lat", 0.0),
                lon = obj.optDouble("lon", 0.0)
            )
        }
    } catch (e: Exception) {}

    var storeLoc = StoreLocation()
    try {
        if (storeLocationJson.isNotBlank()) {
            val obj = org.json.JSONObject(storeLocationJson)
            storeLoc = StoreLocation(
                lat = obj.optDouble("lat", 0.0),
                lon = obj.optDouble("lon", 0.0)
            )
        }
    } catch (e: Exception) {}

    return SellerOrderEntity(
        id = id,
        buyerId = buyerId,
        sellerId = sellerId,
        deliveryId = deliveryId,
        buyerName = buyerName,
        buyerPhone = buyerPhone,
        sellerName = sellerName,
        productNames = productNames,
        totalPrice = totalPrice,
        deliveryPrice = deliveryPrice,
        unit = unit,
        paymentMethod = paymentMethod,
        status = status,
        shippingAddress = shipAddr,
        storeLocation = storeLoc,
        buyerVerifCode = buyerVerifCode,
        deliveryVerifCode = deliveryVerifCode,
        courierName = courierName,
        courierPlate = courierPlate,
        deliveryPickup = deliveryPickup,
        isSelfPickup = isSelfPickup,
        paymentReceipt = paymentReceipt,
        paymentReference = paymentReference,
        paymentBackReceipt = paymentBackReceipt,
        paymentBackReference = paymentBackReference,
        cancellationReason = cancellationReason,
        timestamp = timestamp,
        created = created,
        updated = updated,
        items = itemsList
    )
}

@Dao
interface SellerOrderDao {
    @Query("SELECT * FROM seller_orders WHERE sellerId = :sellerId ORDER BY timestamp DESC")
    suspend fun getOrdersBySeller(sellerId: String): List<SellerOrderDbEntity>

    @Query("SELECT * FROM seller_orders ORDER BY timestamp DESC")
    suspend fun getAllOrders(): List<SellerOrderDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<SellerOrderDbEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: SellerOrderDbEntity)

    @Query("DELETE FROM seller_orders WHERE id = :id")
    suspend fun deleteOrder(id: String)

    @Query("DELETE FROM seller_orders WHERE sellerId = :sellerId")
    suspend fun deleteBySeller(sellerId: String)
}

@Database(entities = [SellerDailySalesEntity::class, SellerRevenueEntity::class, SellerMonthlyStatsEntity::class, UserDbEntity::class, SellerProductDbEntity::class, SellerOrderDbEntity::class], version = 6, exportSchema = false)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun sellerDailySalesDao(): SellerDailySalesDao
    abstract fun sellerRevenueDao(): SellerRevenueDao
    abstract fun sellerMonthlyStatsDao(): SellerMonthlyStatsDao
    abstract fun userDao(): UserDao
    abstract fun sellerProductDao(): SellerProductDao
    abstract fun sellerOrderDao(): SellerOrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        fun getInstance(context: Context): AppRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    "esuuq_app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Entity(tableName = "users")
data class UserDbEntity(
    @PrimaryKey val id: String,
    val role: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val profileImage: String,
    val addressRegion: String,
    val addressZone: String,
    val addressCity: String,
    val addressKebele: String? = null,
    val birthDate: String? = null,
    val isTwoFactorEnabled: Boolean? = false,
    val status: Boolean? = false,
    val isVerified: Boolean = true,
    val isApproved: Boolean = true,
    val isEmailVerified: Boolean = false,
    val created: String? = null,
    val updated: String? = null,

    // Seller fields
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
    val sellerVerification: String? = null,
    @Embedded(prefix = "store_loc_") val storeLocation: StoreLocation = StoreLocation(),
    val sellerProfileId: String? = null,
    val sellerProfileImage: String? = null,
    val sellerProfileCover: String? = null,

    // Delivery fields
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
    val deliveryVerification: String? = null,
    val deliveryProfileId: String? = null,

    // Security & Referral fields
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val currentPinCode: String? = null,
    val is2faCompleted: Boolean = true,
    val referCode: String? = null,
    val fromReferCode: String? = null
)

fun UserEntity.toEntity(): UserDbEntity {
    return UserDbEntity(
        id = id,
        role = role,
        fullName = fullName,
        phone = phone,
        email = email,
        profileImage = profileImage,
        addressRegion = addressRegion,
        addressZone = addressZone,
        addressCity = addressCity,
        addressKebele = addressKebele,
        birthDate = birthDate,
        isTwoFactorEnabled = isTwoFactorEnabled,
        status = status,
        isVerified = isVerified,
        isApproved = isApproved,
        isEmailVerified = isEmailVerified,
        created = created,
        updated = updated,

        // Seller fields
        businessName = businessName,
        businessPhone = businessPhone,
        businessAddress = businessAddress,
        tinNumber = tinNumber,
        tlcNumber = tlcNumber,
        businessGradeTitel = businessGradeTitel,
        businessGrade = businessGrade,
        businessAbout = businessAbout,
        businessWorkingHours = businessWorkingHours,
        businessPayout = businessPayout,
        businessRating = businessRating,
        businessDiscountCode = businessDiscountCode,
        businessDocumentFront = businessDocumentFront,
        businessDocumentBack = businessDocumentBack,
        sellerVerification = sellerVerification,
        storeLocation = storeLocation,
        sellerProfileId = sellerProfileId,
        sellerProfileImage = sellerProfileImage,
        sellerProfileCover = sellerProfileCover,

        // Delivery fields
        plateNumber = plateNumber,
        deliveryPhone = deliveryPhone,
        vehicleType = vehicleType,
        deliveryGrade = deliveryGrade,
        xpTotal = xpTotal,
        ratingTotal = ratingTotal,
        driverLicenseFront = driverLicenseFront,
        driverLicenseBack = driverLicenseBack,
        vehicleDocumentFront = vehicleDocumentFront,
        vehicleDocumentBack = vehicleDocumentBack,
        deliveryVerification = deliveryVerification,
        deliveryProfileId = deliveryProfileId,

        // Security & Referral fields
        isAppLockEnabled = isAppLockEnabled,
        isBiometricEnabled = isBiometricEnabled,
        currentPinCode = currentPinCode,
        is2faCompleted = is2faCompleted,
        referCode = referCode,
        fromReferCode = fromReferCode
    )
}

fun UserDbEntity.toDomainModel(): UserEntity {
    return UserEntity(
        id = id,
        role = role,
        fullName = fullName,
        phone = phone,
        email = email,
        profileImage = profileImage,
        addressRegion = addressRegion,
        addressZone = addressZone,
        addressCity = addressCity,
        addressKebele = addressKebele,
        birthDate = birthDate,
        isTwoFactorEnabled = isTwoFactorEnabled,
        status = status,
        isVerified = isVerified,
        isApproved = isApproved,
        isEmailVerified = isEmailVerified,
        created = created,
        updated = updated,

        // Seller fields
        businessName = businessName,
        businessPhone = businessPhone,
        businessAddress = businessAddress,
        tinNumber = tinNumber,
        tlcNumber = tlcNumber,
        businessGradeTitel = businessGradeTitel,
        businessGrade = businessGrade,
        businessAbout = businessAbout,
        businessWorkingHours = businessWorkingHours,
        businessPayout = businessPayout,
        businessRating = businessRating,
        businessDiscountCode = businessDiscountCode,
        businessDocumentFront = businessDocumentFront,
        businessDocumentBack = businessDocumentBack,
        sellerVerification = sellerVerification,
        storeLocation = storeLocation,
        sellerProfileId = sellerProfileId,
        sellerProfileImage = sellerProfileImage,
        sellerProfileCover = sellerProfileCover,

        // Delivery fields
        plateNumber = plateNumber,
        deliveryPhone = deliveryPhone,
        vehicleType = vehicleType,
        deliveryGrade = deliveryGrade,
        xpTotal = xpTotal,
        ratingTotal = ratingTotal,
        driverLicenseFront = driverLicenseFront,
        driverLicenseBack = driverLicenseBack,
        vehicleDocumentFront = vehicleDocumentFront,
        vehicleDocumentBack = vehicleDocumentBack,
        deliveryVerification = deliveryVerification,
        deliveryProfileId = deliveryProfileId,

        // Security & Referral fields
        isAppLockEnabled = isAppLockEnabled,
        isBiometricEnabled = isBiometricEnabled,
        currentPinCode = currentPinCode,
        is2faCompleted = is2faCompleted,
        referCode = referCode ?: "",
        fromReferCode = fromReferCode ?: ""
    )
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserDbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserDbEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)
}


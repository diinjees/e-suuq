package com.example.network

import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody
import okhttp3.MultipartBody

interface PocketBaseApi {
    
    // ==========================================
    // AUTHENTICATION
    // ==========================================
    
    @POST("api/collections/users/auth-with-password")
    suspend fun login(
        @Body request: LoginRequest
    ): PocketBaseAuthResponse

    @POST("api/collections/users/auth-refresh")
    suspend fun refreshAuth(
        @Header("Authorization") token: String
    ): PocketBaseAuthResponse

    @Multipart
    @POST("api/collections/users/records")
    suspend fun registerWithFiles(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): PocketBaseUserRecord

    // ==========================================
    // USERS
    // ==========================================
    
    @GET("api/collections/users/records/{id}")
    suspend fun getUser(
        @Path("id") id: String,
        @Query("expand") expand: String? = "seller_profile,delivery_profile"
    ): PocketBaseUserRecord

    @PATCH("api/collections/users/records/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body user: Map<String, @JvmSuppressWildcards Any>
    ): PocketBaseUserRecord

    @Multipart
    @PATCH("api/collections/users/records/{id}")
    suspend fun updateUserWithFiles(
        @Path("id") id: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): PocketBaseUserRecord

    // ==========================================
    // SELLER PROFILES
    // ==========================================
    
    @Multipart
    @POST("api/collections/seller_profiles/records")
    suspend fun createSellerProfileWithFiles(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): SellerProfile

    @GET("api/collections/seller_profiles/records/{id}")
    suspend fun getSellerProfile(
        @Path("id") id: String
    ): SellerProfile

    @GET("api/collections/seller_profiles/records")
    suspend fun getSellerProfiles(
        @Query("filter") filter: String? = null
    ): PbListResponse<SellerProfile>

    @PATCH("api/collections/seller_profiles/records/{id}")
    suspend fun updateSellerProfile(
        @Path("id") id: String,
        @Body profile: Map<String, @JvmSuppressWildcards Any>
    ): SellerProfile

    @Multipart
    @PATCH("api/collections/seller_profiles/records/{id}")
    suspend fun updateSellerProfileWithFiles(
        @Path("id") id: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): SellerProfile

    // ==========================================
    // DELIVERY PROFILES
    // ==========================================
    
    @Multipart
    @POST("api/collections/delivery_profiles/records")
    suspend fun createDeliveryProfileWithFiles(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): DeliveryProfile

    @GET("api/collections/delivery_profiles/records/{id}")
    suspend fun getDeliveryProfile(
        @Path("id") id: String
    ): DeliveryProfile

    @GET("api/collections/delivery_profiles/records")
    suspend fun getDeliveryProfiles(
        @Query("filter") filter: String? = null
    ): PbListResponse<DeliveryProfile>

    @PATCH("api/collections/delivery_profiles/records/{id}")
    suspend fun updateDeliveryProfile(
        @Path("id") id: String,
        @Body profile: Map<String, @JvmSuppressWildcards Any>
    ): DeliveryProfile

    // ==========================================
    // SELLER PRODUCTS
    // ==========================================
    
    @GET("api/collections/products/records")
    suspend fun getSellerProducts(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50,
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "-created"
    ): PbListResponse<PbSellerProduct>

    @POST("api/collections/products/records")
    suspend fun createSellerProduct(
        @Body product: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<com.google.gson.JsonObject>

    @PATCH("api/collections/products/records/{id}")
    suspend fun updateSellerProduct(
        @Path("id") id: String,
        @Body product: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<com.google.gson.JsonObject>

    @DELETE("api/collections/products/records/{id}")
    suspend fun deleteSellerProduct(
        @Path("id") id: String
    ): retrofit2.Response<Unit>


    // ==========================================
    // FOR ALL USER PRODUCTS VIEW
    // ==========================================

    @GET("api/collections/products_view/records")
    suspend fun getProductEntity(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "-created",
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 20
    ): PbListResponse<PbProductEntity>

    // ==========================================
    // ORDER ITEMS
    // ==========================================

    @GET("api/collections/order_items/records")
    suspend fun getOrderItems(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "-created"
    ): PbListResponse<PbOrderItem>
    
    @GET("api/collections/order_items/records/{id}")
    suspend fun getOrderItem(
        @Path("id") id: String
    ): PbOrderItem
    
    @POST("api/collections/order_items/records")
    suspend fun createOrderItem(
        @Body item: PbOrderItem
    ): PbOrderItem
    
    @POST("api/collections/order_items/records")
    suspend fun createOrderItems(
        @Body items: List<PbOrderItem>
    ): PbListResponse<PbOrderItem>
    
    @PATCH("api/collections/order_items/records/{id}")
    suspend fun updateOrderItem(
        @Path("id") id: String,
        @Body item: Map<String, Any>
    ): PbOrderItem
    
    @DELETE("api/collections/order_items/records/{id}")
    suspend fun deleteOrderItem(
        @Path("id") id: String
    ): retrofit2.Response<Unit>


    // ==========================================
    
    @GET("api/collections/orders/records/{id}")
    suspend fun getOrder(@Path("id") id: String): PbOrder

    @GET("api/collections/orders/records")
    suspend fun getOrders(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50,
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "-created"
    ): PbListResponse<PbOrder>

    @POST("api/collections/orders/records")
    suspend fun createOrder(
        @Body order: PbOrder
    ): PbOrder

    @PATCH("api/collections/orders/records/{id}")
    suspend fun updateOrder(
        @Path("id") id: String,
        @Body order: Map<String, @JvmSuppressWildcards Any>
    ): PbOrder

    // ==========================================
    // CHAT MESSAGES
    // ==========================================
    
    @GET("api/collections/chat_messages/records")
    suspend fun getMessages(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "created"
    ): PbListResponse<PbChatMessage>

    @POST("api/collections/chat_messages/records")
    suspend fun sendMessage(
        @Body message: PbChatMessage
    ): PbChatMessage

    // ==========================================
    // ADDRESSES
    // ==========================================
    
    @GET("api/collections/addresses/records")
    suspend fun getAddresses(
        @Query("filter") filter: String? = null
    ): PbListResponse<PbAddress>

    @POST("api/collections/addresses/records")
    suspend fun createAddress(
        @Body address: PbAddress
    ): PbAddress

    @POST("api/collections/users/request-otp")
    suspend fun requestOtp(
        @Body request: RequestOtpRequest
    ): RequestOtpResponse

    @POST("api/collections/users/request-email-change")
    suspend fun requestEmailChange(
        @Body request: RequestEmailChangeRequest
    ): RequestEmailChangeResponse

    @POST("api/collections/users/confirm-email-change")
    suspend fun confirmEmailChange(
        @Body request: ConfirmEmailChangeRequest
    ): retrofit2.Response<Unit>

    @POST("api/collections/users/resend-email-change-otp")
    suspend fun resendEmailChangeOtp(
        @Body request: ResendEmailChangeOtpRequest
    ): RequestEmailChangeResponse

    @POST("api/collections/users/cancel-email-change")
    suspend fun cancelEmailChange(
        @Body request: CancelEmailChangeRequest
    ): retrofit2.Response<Unit>

    @POST("api/collections/users/auth-with-otp")
    suspend fun authWithOtp(
        @Body request: AuthWithOtpRequest
    ): PocketBaseAuthResponse

    @POST("api/collections/users/auth-with-mfa")
    suspend fun authWithMfa(
        @Body request: AuthWithMfaRequest
    ): PocketBaseAuthResponse

    @GET("api/collections/users/records")
    suspend fun getUsers(
        @Query("filter") filter: String? = null
    ): PbListResponse<PocketBaseUserRecord>

    @PATCH("api/collections/addresses/records/{id}")
    suspend fun updateAddress(
        @Path("id") id: String,
        @Body address: Map<String, @JvmSuppressWildcards Any>
    ): PbAddress

    @DELETE("api/collections/addresses/records/{id}")
    suspend fun deleteAddress(
        @Path("id") id: String
    ): Call<Unit>

    // ==========================================
    // LOGIN HISTORY
    // ==========================================

    @GET("api/collections/login_history/records")
    suspend fun getLoginHistory(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "-created"
    ): PbListResponse<PbLoginHistoryItem>

    @POST("api/collections/login_history/records")
    suspend fun createLoginHistory(
        @Body item: PbLoginHistoryItem
    ): PbLoginHistoryItem

    @PATCH("api/collections/login_history/records/{id}")
    suspend fun updateLoginHistory(
        @Path("id") id: String,
        @Body item: PbLoginHistoryItem
    ): PbLoginHistoryItem

    @DELETE("api/collections/login_history/records/{id}")
    suspend fun deleteLoginHistory(
        @Path("id") id: String
    ): retrofit2.Response<Unit>

    // ==========================================
    // REVIEWS PRODUCTS
    // ==========================================

    @GET("api/collections/reviews_products/records")
    suspend fun getProductReviews(
        @Query("filter") filter: String? = null,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50,
        @Query("sort") sort: String? = "-created"
    ): PbListResponse<PbReviewProduct>

    @POST("api/collections/reviews_products/records")
    suspend fun createProductReview(
        @Body review: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<PbReviewProduct>

    @PATCH("api/collections/reviews_products/records/{id}")
    suspend fun updateProductReview(
        @Path("id") id: String,
        @Body review: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<PbReviewProduct>

    @DELETE("api/collections/reviews_products/records/{id}")
    suspend fun deleteProductReview(
        @Path("id") id: String
    ): retrofit2.Response<Unit>

    // ==========================================
    // REVIEWS STORES
    // ==========================================

    @GET("api/collections/reviews_stores/records")
    suspend fun getStoreReviews(
        @Query("filter") filter: String? = null,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50,
        @Query("sort") sort: String? = "-created"
    ): PbListResponse<PbReviewStore>

    @POST("api/collections/reviews_stores/records")
    suspend fun createStoreReview(
        @Body review: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<PbReviewStore>

    @PATCH("api/collections/reviews_stores/records/{id}")
    suspend fun updateStoreReview(
        @Path("id") id: String,
        @Body review: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<PbReviewStore>

    @DELETE("api/collections/reviews_stores/records/{id}")
    suspend fun deleteStoreReview(
        @Path("id") id: String
    ): retrofit2.Response<Unit>

    // ==========================================
    // SELLER REVENUE (PocketBase View)
    // ==========================================

    @GET("api/collections/seller_revenue/records")
    suspend fun getSellerRevenue(
        @Query("filter") filter: String? = null,
        //@Query("sort") sort: String? = "-created",
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50
    ): PbListResponse<PbSellerRevenue>

    // ==========================================
    // SELLER PRODUCT INVENTORY (PocketBase View)
    // ==========================================

    @GET("api/collections/seller_product_inventory/records")
    suspend fun getSellerProductInventory(
        @Query("filter") filter: String? = null,
        //@Query("sort") sort: String? = "-created",
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50
    ): PbListResponse<PbSellerProductInventory>

    // ==========================================
    // SELLER DAILY SALES (PocketBase View)
    // ==========================================

    @GET("api/collections/seller_daily_sales/records")
    suspend fun getSellerDailySales(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "-sale_date",
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50
    ): PbListResponse<PbSellerDailySales>

    // ==========================================
    // SELLER MONTHLY STATS (PocketBase View)
    // ==========================================

    @GET("api/collections/seller_monthly_stats/records")
    suspend fun getSellerMonthlyStats(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "-year,-month",
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 50
    ): PbListResponse<PbSellerMonthlyStats>
}

// ==========================================
// REQUEST MODELS
// ==========================================

data class AuthWithMfaRequest(
    val mfaId: String,
    val otpId: String,
    val password: String
)

data class RequestOtpRequest(
    val email: String
)

data class RequestOtpResponse(
    val otpId: String,
    val code: String? = null
)

data class AuthWithOtpRequest(
    val otpId: String,
    val password: String,
    val mfaId: String? = null
)

data class LoginRequest(
    val identity: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val phone: String,
    val fullName: String,
    val password: String,
    val passwordConfirm: String,
    val role: String = "BUYER",
    val addressRegion: String? = null,
    val addressZone: String? = null,
    val addressCity: String? = null,
    val addressKebele: String? = null
)

data class RequestEmailChangeRequest(
    val newEmail: String,
    val userId: String? = null
)

data class RequestEmailChangeResponse(
    val otpId: String,
    val code: String? = null
)

data class ConfirmEmailChangeRequest(
    val otpId: String,
    val code: String,
    val newEmail: String? = null
)

data class ResendEmailChangeOtpRequest(
    val otpId: String,
    val email: String? = null
)

data class CancelEmailChangeRequest(
    val otpId: String
)
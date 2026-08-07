package com.example.ui.screens

import com.example.data.*

import android.widget.Toast

import coil.request.ImageRequest

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import coil.compose.AsyncImage

import androidx.compose.animation.*

import androidx.compose.animation.core.*

import androidx.compose.foundation.*

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.automirrored.filled.Chat

import androidx.compose.material.icons.automirrored.filled.Send

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.outlined.*

import androidx.compose.material3.*

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.draw.alpha

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.PathEffect

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.graphics.nativeCanvas

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.testTag

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.input.PasswordVisualTransformation

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.ui.components.UnifiedCalendarDialog

import com.example.ui.components.formatDate

import com.example.ui.components.toDateString

import com.example.ui.components.toCalendar

import com.example.ui.components.isSameDay

import androidx.compose.runtime.derivedStateOf

import java.util.Calendar

import com.example.data.AddressEntity

import com.example.data.ChatMessageEntity

import com.example.data.SellerOrderEntity

import com.example.data.SellerProductEntity

import com.example.data.UserEntity
import com.example.data.CategorySaleInfo

import com.example.ui.theme.BrandGoldSecondary

import com.example.ui.theme.BrandGreenPrimary

import com.example.ui.theme.BrandRedTertiary

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch


import com.example.ui.*

import com.example.ui.screens.*

import com.example.ui.components.*

import com.example.ui.utils.*

data class BusinessGradeInfo(
    val title: String,
    val nextTitle: String,
    val minRequirement: Int,
    val maxRequirement: Int
)

fun getBusinessGradeInfo(productsSold: Int): BusinessGradeInfo {
    return when {
        productsSold >= 3500 -> BusinessGradeInfo("Sovereign Emperor", "MAXIMUM", 3500, 3500)
        productsSold >= 2000 -> BusinessGradeInfo("Platinum Apex", "Sovereign Emperor", 2000, 3500)
        productsSold >= 1000 -> BusinessGradeInfo("Platinum Exemplar", "Platinum Apex", 1000, 2000)
        productsSold >= 600 -> BusinessGradeInfo("Gold Legend", "Platinum Exemplar", 600, 1000)
        productsSold >= 400 -> BusinessGradeInfo("Gold Grandmaster", "Gold Legend", 400, 600)
        productsSold >= 250 -> BusinessGradeInfo("Gold Champion", "Gold Grandmaster", 250, 400)
        productsSold >= 150 -> BusinessGradeInfo("Silver Master", "Gold Champion", 150, 250)
        productsSold >= 100 -> BusinessGradeInfo("Silver Elite", "Silver Master", 100, 150)
        productsSold >= 50 -> BusinessGradeInfo("Silver Vanguard", "Silver Elite", 50, 100)
        productsSold >= 25 -> BusinessGradeInfo("Bronze Rising", "Silver Vanguard", 25, 50)
        productsSold >= 10 -> BusinessGradeInfo("Bronze Rookie", "Bronze Rising", 10, 25)
        else -> BusinessGradeInfo("Bronze Novice", "Bronze Rookie", 0, 10)
    }
}

@Composable
fun SellerDashboardScreen(viewModel: AppViewModel) {
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val sellerId = activeUser?.sellerProfileId ?: activeUser?.id ?: ""
    val storeOpen by viewModel.storeOpen.collectAsStateWithLifecycle()

    val allProducts by viewModel.products.collectAsStateWithLifecycle()
    val sellerProducts by viewModel.sellerProducts.collectAsStateWithLifecycle()
    val sellerOrders by viewModel.sellerOrders.collectAsStateWithLifecycle()
    val sellerAnalytics by viewModel.sellerAnalytics.collectAsStateWithLifecycle()
    val sellerRevenueList by viewModel.sellerRevenueList.collectAsStateWithLifecycle()
    val sellerInventoryList by viewModel.sellerInventoryList.collectAsStateWithLifecycle()
    val sellerDailySalesList by viewModel.sellerDailySalesList.collectAsStateWithLifecycle()
    val sellerMonthlyStatsList by viewModel.sellerMonthlyStatsList.collectAsStateWithLifecycle()
    val isSellerDashboardLoading by viewModel.isSellerDashboardLoading.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val sellerProfileId = activeUser?.sellerProfileId ?: ""
    val products = sellerProducts
    val orders = sellerOrders

    var isAppResumed by remember { mutableStateOf(true) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // ✅ Subscribe to real-time updates ONLY when inside SellerDashboard AND store status is online
    DisposableEffect(storeOpen, sellerId) {
        viewModel.repository.syncEngine.setSellerActive(true)
        viewModel.loadSellerDashboardData(sellerId)
        
        if (storeOpen && sellerId.isNotBlank()) {
            viewModel.subscribeSellerRealtime(sellerId)
        } else {
            viewModel.unsubscribeSellerRealtime(sellerId)
        }
        
        onDispose {
            viewModel.repository.syncEngine.setSellerActive(false)
            if (sellerId.isNotBlank()) {
                viewModel.unsubscribeSellerRealtime(sellerId)
            }
        }
    }

    /* ✅ Real-time order updates in the background l
    androidx.compose.runtime.LaunchedEffect(sellerId) {
        if (sellerId.isNotEmpty()) {
            // Initial load
            viewModel.loadSellerDashboardData(sellerId)
            
            // Auto-refresh loop (acts as fallback if real-time fails)
            while (true) {
                kotlinx.coroutines.delay(120_000L) // 2 minutes
                if (isAppResumed) {
                    viewModel.loadSellerDashboardData(sellerId)
                }
            }
        }
    }
    */


    var merchantProfileImageUri by remember(activeUser) { mutableStateOf(activeUser?.sellerProfileImage?.takeIf { it.isNotBlank() } ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300") }
    var merchantProfileCoverUri by remember(activeUser) { mutableStateOf(activeUser?.sellerProfileCover?.takeIf { it.isNotBlank() } ?: "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1000") }

    val productsSoldForEffect = activeUser?.businessGrade?.toDoubleOrNull()?.toInt() ?: 0
    androidx.compose.runtime.LaunchedEffect(productsSoldForEffect) {
        val computedGrade = getBusinessGradeInfo(productsSoldForEffect).title
        if (activeUser?.businessGradeTitel != computedGrade) {
            viewModel.updateSellerProfileField(mapOf("businessGradeTitel" to computedGrade))
        }
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val sharedPrefs = remember { context.getSharedPreferences("inventory_aging_prefs", android.content.Context.MODE_PRIVATE) }
    var ageActiveLimit by remember { mutableStateOf(sharedPrefs.getInt("age_active_limit", 15)) }
    var ageNormalLimit by remember { mutableStateOf(sharedPrefs.getInt("age_normal_limit", 30)) }
    var ageSlowLimit by remember { mutableStateOf(sharedPrefs.getInt("age_slow_limit", 60)) }

    // Helper to calculate delivery price and responsibility (free delivery vs buyer pays)
    val getOrderDeliveryDetails = { order: SellerOrderEntity ->
        val names = order.productNames.split(",").map { 
            it.substringBefore(" x").trim() 
        }
        val orderProducts = products.filter { prod -> 
            names.any { name -> name.equals(prod.name, ignoreCase = true) || prod.name.contains(name, ignoreCase = true) || name.contains(prod.name, ignoreCase = true) }
        }
        val isFree = if (orderProducts.isNotEmpty()) {
            orderProducts.all { it.isFreeDelivery }
        } else {
            order.productNames.contains("Habesha Kemis", ignoreCase = true) || order.id.hashCode() % 2 == 0
        }
        val address = "Buyer Address"
        val simulatedDistance = if (address.isBlank()) {
            3.5
        } else {
            val normalized = address.trim()
            val len = normalized.length
            ((len * 0.38) % 9.5 + 1.5).coerceIn(1.5, 11.2)
        }
        val rawFee = 50.0 + (simulatedDistance * 25.0)
        val fee = Math.round(rawFee * 10.0) / 10.0
        Pair(fee, isFree)
    }

    if (activeUser?.isApproved == false) {
        // Strict pre-approval screen
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LockClock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = BrandGoldSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Application Under Review",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loc("pending_approval"),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )  
                }
            }
        }
    } else {
        // Full Dashboard with sub-navigation, charts, crude operations, order status change
        val storeOpen by viewModel.storeOpen.collectAsStateWithLifecycle()
        val smsAlertsEnabled by viewModel.smsAlertsEnabled.collectAsStateWithLifecycle()
        val customOffersEnabled by viewModel.customOffersEnabled.collectAsStateWithLifecycle()

        val sellerProfileImageLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                viewModel.uploadSellerProfileImage(context, uri.toString(), onSuccess = {
                    val updatedVal = viewModel.activeUser.value?.sellerProfileImage
                    if (updatedVal != null) {
                        merchantProfileImageUri = updatedVal
                    }
                    android.widget.Toast.makeText(context, "Seller Profile Image updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                }, onError = { msg ->
                    android.widget.Toast.makeText(context, "Upload failed: $msg", android.widget.Toast.LENGTH_LONG).show()
                })
            }
        }

        val sellerProfileCoverLauncher = rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                viewModel.uploadSellerProfileCover(context, uri.toString(), onSuccess = {
                    val updatedVal = viewModel.activeUser.value?.sellerProfileCover
                    if (updatedVal != null) {
                        merchantProfileCoverUri = updatedVal
                    }
                    android.widget.Toast.makeText(context, "Seller Profile Cover updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                }, onError = { msg ->
                    android.widget.Toast.makeText(context, "Upload failed: $msg", android.widget.Toast.LENGTH_LONG).show()
                })
            }
        }

        val activeSubViewCollected by viewModel.activeDashboardSubView.collectAsStateWithLifecycle()
        val previousSubViewCollected by viewModel.previousDashboardSubView.collectAsStateWithLifecycle()

        val activeDashboardSubViewDelegate = remember(activeSubViewCollected) {
            object : kotlin.properties.ReadWriteProperty<Any?, String> {
                override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): String = activeSubViewCollected
                override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: String) {
                    viewModel.activeDashboardSubView.value = value
                }
            }
        }
        var activeDashboardSubView by activeDashboardSubViewDelegate

        val previousDashboardSubViewDelegate = remember(previousSubViewCollected) {
            object : kotlin.properties.ReadWriteProperty<Any?, String> {
                override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): String = previousSubViewCollected
                override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: String) {
                    viewModel.previousDashboardSubView.value = value
                }
            }
        }
        var previousDashboardSubView by previousDashboardSubViewDelegate

        var selectedCategoryFilter by remember { mutableStateOf("All") }

        var orderTabFilter by remember { mutableStateOf("ACTIVE") } // "ACTIVE", "COMPLETED", "CANCELLED"
        var orderSearchQuery by remember { mutableStateOf("") }
        var orderSortOption by remember { mutableStateOf("DATE_DESC") } // "DATE_DESC", "DATE_ASC", "PRICE_DESC", "PRICE_ASC"
        var orderDateFilter by remember { mutableStateOf("ALL") } // "All Time", "Today", "Past 7 Days", "Past 30 Days"
        var showSellerDateDialog by remember { mutableStateOf(false) }
        var sellerStartDate by remember { mutableStateOf<java.util.Calendar?>(null) }
        var sellerEndDate by remember { mutableStateOf<java.util.Calendar?>(null) }
        var selectedOrderToCancel by remember { mutableStateOf<SellerOrderEntity?>(null) }
        var cancelReasonInput by remember { mutableStateOf("") }
        var cancelRefundReceiptState by remember { mutableStateOf<String?>(null) }
        var showCancelDialog by remember { mutableStateOf(false) }

        var orderToConfirmPayment by remember { mutableStateOf<SellerOrderEntity?>(null) }
        var showPaymentConfirmDialog by remember { mutableStateOf(false) }

        var orderToMarkReady by remember { mutableStateOf<SellerOrderEntity?>(null) }
        var showReadyConfirmDialog by remember { mutableStateOf(false) }
        var sellerSelectedOrderDetail by remember { mutableStateOf<SellerOrderEntity?>(null) }
        var orderForCourierVerification by remember { mutableStateOf<SellerOrderEntity?>(null) }
        var showCourierVerificationDialog by remember { mutableStateOf(false) }
        var courierVerificationCodeInput by remember { mutableStateOf("") }
        var orderForBuyerVerification by remember { mutableStateOf<SellerOrderEntity?>(null) }
        var showBuyerVerificationDialog by remember { mutableStateOf(false) }
        var buyerVerificationCodeInput by remember { mutableStateOf("") }
        val defaultPayoutStr = activeUser?.businessPayout ?: ""
        val initialCbe = if (defaultPayoutStr.contains("CBE:")) defaultPayoutStr.substringAfter("CBE:").substringBefore("|").trim() else "1000123456789"
        val initialTelebirr = if (defaultPayoutStr.contains("Telebirr:")) defaultPayoutStr.substringAfter("Telebirr:").substringBefore("|").trim() else "982001"
        val initialEbirr = if (defaultPayoutStr.contains("Ebirr:")) defaultPayoutStr.substringAfter("Ebirr:").substringBefore("|").trim() else "EB-72910"
        val initialPayoutName = if (defaultPayoutStr.contains("Name:")) defaultPayoutStr.substringAfter("Name:").substringBefore("|").trim() else (activeUser?.businessName ?: "Bole Boutique PLC")

        var merchantPhone by remember(activeUser) { mutableStateOf(activeUser?.businessPhone ?: activeUser?.phone ?: "+251 912 345678") }
        var merchantPaymentModel by remember(activeUser) { mutableStateOf("Flat Subscription (499 ETB/mo)") }
        var merchantCbeAccount by remember(activeUser) { mutableStateOf(initialCbe) }
        var merchantTelebirrMerchantId by remember(activeUser) { mutableStateOf(initialTelebirr) }
        var merchantStoreName by remember(activeUser) { mutableStateOf(activeUser?.businessName ?: "Bole Traditional Boutique") }
        var merchantStoreImageUri by remember(activeUser) { mutableStateOf("https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=500") }

        var merchantTinNumber by remember(activeUser) { mutableStateOf(activeUser?.tinNumber ?: "TIN-84930211") }
        var merchantTradeLicense by remember(activeUser) { mutableStateOf(activeUser?.tlcNumber ?: "TL-983021-ETH") }
        var merchantEbirrId by remember(activeUser) { mutableStateOf(initialEbirr) }
        var merchantPayoutName by remember(activeUser) { mutableStateOf(initialPayoutName) }
        var merchantDiscountPercent by remember { mutableStateOf(5) }
        var merchantSalesCountForGrade by remember { mutableStateOf(37500.0) }

        val hasEmptySettings = merchantEbirrId.isBlank() || 
                               merchantTelebirrMerchantId.isBlank() || 
                               (activeUser?.businessWorkingHours ?: "").isBlank() || 
                               (activeUser?.businessAbout ?: "").isBlank()

        val currentRealMonthName = remember {
            val cal = java.util.Calendar.getInstance()
            val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            "${monthNames[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.YEAR)}"
        }
        var selectedPerformanceMonth by remember { mutableStateOf<String?>(currentRealMonthName) }
        var selectedPayoutChannel by remember { mutableStateOf("CBE") }
        var selectedPayoutMonth by remember { mutableStateOf(currentRealMonthName) }
        var selectedHeatmapDay by remember { mutableStateOf<String?>("Fri") }
        var selectedCalendarDay by remember { mutableStateOf(19) }
        val currentYM = remember { java.time.YearMonth.now() }
        val real3YearMonths = remember(currentYM) {
            listOf(
                currentYM.minusMonths(2), // 2 months ago (index 0)
                currentYM.minusMonths(1), // 1 month ago (index 1)
                currentYM                // Current month (index 2)
            )
        }
        val restockingMonths = remember(real3YearMonths) {
            real3YearMonths.map { ym ->
                val mName = ym.month.name.lowercase().replaceFirstChar { it.uppercase() }
                "$mName ${ym.year}"
            }
        }
        var restockingMonthIndex by remember { mutableStateOf(2) } // Default to current month (index 2)

        // State variables for Aging Inventory (Dead Stock) promotions and simulator
        var deadStockPercentState by remember { mutableStateOf(55f) } // Initialized to 55% to show warning icon when >= 50%
        var showSqueezePromoDialog by remember { mutableStateOf(false) }
        var squeezePromoProductId by remember { mutableStateOf("") }
        var squeezePromoItemName by remember { mutableStateOf("") }
        var squeezePromoItemAge by remember { mutableStateOf("") }
        var squeezePromoItemOriginalPrice by remember { mutableStateOf(1500.0) }
        var squeezePromoItemType by remember { mutableStateOf("Slow") } // "Slow" or "Dead"
        var squeezePromoInputPrice by remember { mutableStateOf("1200") }
        var squeezePromoFreeShippingActive by remember { mutableStateOf(true) }

        // State variables for Customer Reviews Manager
        val reviewsListState by viewModel.storeReviews.collectAsStateWithLifecycle(initialValue = emptyList())

        androidx.compose.runtime.LaunchedEffect(Unit) {
            viewModel.fetchStoreReviews()
        }

        val configuration = LocalConfiguration.current
        val isWideScreen = configuration.screenWidthDp >= 600

        @Composable
        fun HeaderBlock() {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isOnline) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Offline Mode",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline Mode — Viewing saved Room DB data",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = loc("store_analytics"),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BrandGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Platinum Verified Store",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = BrandGreenPrimary
                            )
                        }
                    }
                    Button(
                        onClick = { 
                            viewModel.editProductFromLiveStock = false
                            viewModel.setEditingProduct(null) 
                            viewModel.navigateTo(AppScreen.EDIT_PRODUCT_SCREEN)
                        },
                        modifier = Modifier.testTag("add_product_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(loc("add_product"))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Store Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (storeOpen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, if (storeOpen) BrandGreenPrimary.copy(alpha = 0.3f) else BrandRedTertiary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(if (storeOpen) BrandGreenPrimary else BrandRedTertiary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (storeOpen) "Store Status: Online & Open" else "Store Status: Offline / On Holiday",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (storeOpen) "Customers can browse and place direct orders" else "Orders are paused temporarily",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Switch(
                                checked = storeOpen,
                                onCheckedChange = { viewModel.toggleStoreOpen() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BrandGreenPrimary,
                                    checkedTrackColor = BrandGreenPrimary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("store_status_switch")
                            )
                            Box(contentAlignment = Alignment.TopEnd) {
                                IconButton(
                                    onClick = {
                                        previousDashboardSubView = activeDashboardSubView
                                        activeDashboardSubView = "MERCHANT_SETTINGS"
                                    },
                                    modifier = Modifier.testTag("merchant_settings_icon_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Merchant Settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (hasEmptySettings) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp, end = 4.dp)
                                            .size(10.dp)
                                            .background(Color.Red, shape = CircleShape)
                                            .testTag("settings_empty_alert_dot")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun StatsBlock(showSecondaryGrid: Boolean = true) {
            val earnings = orders.filter { it.status == "DELIVERED" }.sumOf { it.totalPrice }
            val netEarnings = earnings // E-Suuq is 100% free! No platform commission
            
            val currentDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val todayDailySale = sellerDailySalesList.firstOrNull { it.sale_date.startsWith(currentDateStr) }

            val startOfToday = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayOrders = orders.filter { 0L >= startOfToday }

            val displayNetProfit = todayDailySale?.net_profit?.toDouble()
                ?: todayOrders.filter { it.status == "DELIVERED" }.sumOf { it.totalPrice }
            val displayProductCost = todayDailySale?.product_cost?.toDouble() ?: 0.0

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Earnings Breakdown Card (Clickable to show financials)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeDashboardSubView = "FINANCIALS" }
                        .testTag("net_income_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Net Payout Income", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format("%.2f ETB", displayNetProfit),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrandGreenPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "View details",
                                tint = BrandGreenPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Products Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = String.format("%.2f ETB", displayProductCost), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Column {
                                Text(text = "Payout Channel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Telebirr / CBE", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = BrandGoldSecondary)
                            }
                        }
                    }
                }
                
                if (showSecondaryGrid) {
                    // Secondary Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeDashboardSubView = "INVENTORY" }
                                .testTag("inventory_count_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Inventory Count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${products.size} Products",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    val hasLowStock = products.any { it.stock <= it.lowStock }
                                    if (hasLowStock) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Low Stock Alert",
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandGreenPrimary)
                                }
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Today's Orders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                val startOfToday = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val todayOrders = orders.filter { 0L >= startOfToday }
                                val completedCount = todayDailySale?.completed_orders ?: todayOrders.count { it.status == "DELIVERED" }
                                val cancelledCount = todayDailySale?.canceled_orders ?: todayOrders.count { it.status == "CANCELLED" }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Completed:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = "$completedCount", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Cancelled:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = "$cancelledCount", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        data class MonthlyDetails(
            val month: String,
            val sales: String,
            val soldCount: Int,
            val unsoldText: String,
            val payouts: String
        )

        data class PayoutTransactionHistoryItem(
            val id: String,
            val date: String,
            val amount: String,
            val month: String,
            val buyerName: String,
            val status: String,
            val channel: String
        )

        @Composable
        fun PerformanceChartBlock() {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Financial Performance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sales performance compared against a standard 50,000 ETB monthly milestone target",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val monthsList = getRealCalendarMonthsList()
                    val chartPointsData = monthsList.map { fullMonthStr ->
                        val parts = fullMonthStr.split(" ")
                        val mName = parts.getOrNull(0) ?: ""
                        val mYear = parts.getOrNull(1) ?: ""
                        val matchingStat = sellerMonthlyStatsList.firstOrNull { stat ->
                            (stat.monthName.equals(mName, ignoreCase = true) || stat.month.equals(mName, ignoreCase = true) || stat.month.endsWith(mName, ignoreCase = true)) &&
                            (stat.year.isBlank() || stat.year == mYear)
                        }
                        val grossSales = (matchingStat?.gross_sales ?: 0).toDouble()
                        val targetRatio = (grossSales / 50000.0) * 100.0
                        val pctStr = String.format(java.util.Locale.US, "%.0f%%", targetRatio)
                        val shortMonth = fullMonthStr.substringBefore(" ").take(3)
                        Triple(shortMonth, grossSales, pctStr)
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val strokeWidth = 3.dp.toPx()
                            val w = size.width
                            val h = size.height

                            // ✅ Top margin for labels (20% of height reserved for labels)
                            val topMargin = 0.2f

                            // ✅ Calculate points with top margin
                            val points = monthsList.mapIndexed { index, m ->
                                val grossSales = chartPointsData[index].second
                                val ratio = (grossSales / 50000.0) * 100.0
                                val clampedRatio = ratio.coerceAtMost(100.0)
                                // Map 0% → bottom, 100% → (1 - topMargin) from bottom
                                val normalized = 1f - (clampedRatio.toFloat() / 100f)
                                val y = h * (topMargin + (1f - topMargin) * normalized)
                                Offset(w * (0.05f + 0.18f * index), y)
                            }

                            // ✅ Draw Grid Lines at 25%, 50%, 75%, 100%
                            val gridPercentages = listOf(25f, 50f, 75f, 100f)
                            gridPercentages.forEach { pct ->
                                val normalized = 1f - (pct / 100f)
                                val y = h * (topMargin + (1f - topMargin) * normalized)
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.35f),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }

                            // ✅ Draw Graph Line (connect the dots)
                            for (j in 0 until points.size - 1) {
                                drawLine(
                                    color = BrandGreenPrimary,
                                    start = points[j],
                                    end = points[j + 1],
                                    strokeWidth = strokeWidth
                                )
                            }

                            // ✅ Draw dots and percentage labels above them
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#EAB308")
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            }

                            points.forEachIndexed { index, p ->
                                val isSelected = selectedPerformanceMonth == monthsList[index]
                                // Draw dot
                                drawCircle(
                                    color = if (isSelected) BrandGreenPrimary else BrandGoldSecondary,
                                    radius = (if (isSelected) 8.dp else 6.dp).toPx(),
                                    center = p
                                )
                                if (isSelected) {
                                    drawCircle(
                                        color = BrandGoldSecondary,
                                        radius = 12.dp.toPx(),
                                        center = p,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                                // Draw label above dot (always above)
                                val labelOffset = 12.dp.toPx()
                                val labelY = (p.y - labelOffset).coerceAtLeast(0f) // Clamp to top
                                drawContext.canvas.nativeCanvas.drawText(
                                    chartPointsData[index].third,
                                    p.x,
                                    labelY,
                                    paint
                                )
                            }
                        }

                        // ✅ Clickable overlay
                        Row(modifier = Modifier.fillMaxSize()) {
                            monthsList.forEach { month ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            selectedPerformanceMonth = month
                                            activeDashboardSubView = "MONTHLY_PROFITABILITY_DETAIL"
                                        }
                                        .testTag("perf_point_tap_${month.replace(" ", "_")}")
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ✅ Month labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        monthsList.forEachIndexed { index, fullMonth ->
                            val monthLabel = chartPointsData[index].first
                            val isSelected = selectedPerformanceMonth == fullMonth
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { 
                                        selectedPerformanceMonth = fullMonth 
                                        activeDashboardSubView = "MONTHLY_PROFITABILITY_DETAIL"
                                    }
                                    .background(
                                        if (isSelected) BrandGreenPrimary.copy(alpha = 0.12f) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = monthLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                    ),
                                    fontSize = 12.sp,
                                    color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                val yearLabel = fullMonth.substringAfter(" ", "")
                                if (yearLabel.isNotEmpty()) {
                                    Text(
                                        text = yearLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 9.sp,
                                        color = if (isSelected) BrandGreenPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }



        @Composable
        fun SellerReviewsBlock() {
            val avgRating = remember(reviewsListState) {
                val parsed = reviewsListState.mapNotNull { it.rating.removePrefix("★").trim().toDoubleOrNull() }
                if (parsed.isNotEmpty()) parsed.average() else 5.0
            }
            val formattedAvg = String.format(java.util.Locale.US, "%.1f", avgRating)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        previousDashboardSubView = activeDashboardSubView
                        activeDashboardSubView = "CUSTOMER_FEEDBACK"
                    }
                    .testTag("recent_feedback_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Recent Customer Feedback",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BrandGoldSecondary.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = formattedAvg,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandGoldSecondary)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Based on ${reviewsListState.size} user reviews",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "View All (${reviewsListState.size}) →",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = BrandGreenPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(reviewsListState) { item ->
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable {
                                        previousDashboardSubView = activeDashboardSubView
                                        activeDashboardSubView = "CUSTOMER_FEEDBACK"
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                        Text(text = item.rating, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = BrandGoldSecondary)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.comment,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (!item.replyText.isNullOrBlank()) {
                                        Text(
                                            text = "✓ Responded",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = BrandGreenPrimary
                                        )
                                    } else {
                                        Text(
                                            text = "+ Reply to buyer",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun OrderItemView(order: SellerOrderEntity) {
            val isDelivery = order.deliveryPickup
            val refCode = if (order.paymentMethod.equals("Telebirr", ignoreCase = true)) {
                "MP-8290${order.id}"
            } else {
                "TXN-4920${order.id}"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        sellerSelectedOrderDetail = order
                        previousDashboardSubView = activeDashboardSubView
                        activeDashboardSubView = "ORDER_DETAIL"
                    }
                    .testTag("order_item_card_${order.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = when (order.status) {
                        "PENDING" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        "CONFIRMED" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        "PREPARING" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        "READY_FOR_PICKUP" -> BrandGoldSecondary.copy(alpha = 0.1f)
                        "OUT_FOR_DELIVERY", "SHIPPED" -> BrandGreenPrimary.copy(alpha = 0.1f)
                        "DELIVERED" -> BrandGreenPrimary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    when (order.status) {
                        "PENDING" -> BrandGoldSecondary.copy(alpha = 0.5f)
                        "CONFIRMED" -> MaterialTheme.colorScheme.secondary
                        "PREPARING" -> MaterialTheme.colorScheme.outlineVariant
                        "READY_FOR_PICKUP" -> BrandGoldSecondary.copy(alpha = 0.4f)
                        "OUT_FOR_DELIVERY", "SHIPPED" -> BrandGreenPrimary.copy(alpha = 0.4f)
                        "DELIVERED" -> BrandGreenPrimary.copy(alpha = 0.5f)
                        else -> BrandRedTertiary.copy(alpha = 0.4f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Order #${order.id}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "Placed via ${order.paymentMethod}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Ref: $refCode",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = BrandGreenPrimary
                            )
                        }
                        
                        // Status Badge
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (order.status) {
                                    "PENDING" -> BrandGoldSecondary.copy(alpha = 0.25f)
                                    "CONFIRMED" -> MaterialTheme.colorScheme.secondaryContainer
                                    "PREPARING" -> MaterialTheme.colorScheme.surfaceVariant
                                    "READY_FOR_PICKUP" -> BrandGoldSecondary.copy(alpha = 0.2f)
                                    "OUT_FOR_DELIVERY", "SHIPPED" -> BrandGreenPrimary.copy(alpha = 0.2f)
                                    "DELIVERED" -> BrandGreenPrimary
                                    else -> BrandRedTertiary
                                }
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (order.status == "SHIPPED") "OUT FOR DELIVERY" else order.status,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (order.status == "DELIVERED" || order.status == "CANCELLED") Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Buyer: ${order.buyerName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Phone: ${order.buyerPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isDelivery) {
                        Text(
                            text = "Destination: ${order.shippingAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val courierDetailText = if (order.isSelfPickup) {
                        "Pickup: Self Pickup (Buyer Pickup)"
                    } else {
                        if (!order.courierName.isNullOrBlank()) {
                            "Pickup: Courier Delivery — ${order.courierName} (${order.courierPlate ?: "N/A"})"
                        } else {
                            "Pickup: Delivery awaiting"
                        }
                    }
                    Text(
                        text = courierDetailText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Items: ${order.productNames}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        
                    )
                    Text(
                        text = "Total Price: ${order.totalPrice} ETB",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandGreenPrimary
                    )

                    if (!order.isSelfPickup) {
                        val (deliveryPrice, isFreeDelivery) = getOrderDeliveryDetails(order)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Delivery Price: $deliveryPrice ETB",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isFreeDelivery) "Seller Pays Delivery (Free Delivery for Buyer) 🚛" else "Buyer Pays Delivery 🚚",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isFreeDelivery) BrandRedTertiary else BrandGreenPrimary
                        )
                    }
                    
                    if (order.status == "CANCELLED" && !order.cancellationReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, BrandRedTertiary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = BrandRedTertiary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cancellation Reason: ${order.cancellationReason}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = BrandRedTertiary
                                    )
                                }
                                if (!"".isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Money-Back Receipt: ${""}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = BrandGreenPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (order.status) {
                                "PENDING" -> {
                                    Button(
                                        onClick = {
                                            orderToConfirmPayment = order
                                            showPaymentConfirmDialog = true
                                        },
                                        modifier = Modifier.weight(1f).testTag("confirm_btn_${order.id}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                    ) {
                                        Text("Confirm Order", fontSize = 11.sp)
                                    }
                                }
                                "CONFIRMED" -> {
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(order, "PREPARING") },
                                        modifier = Modifier.weight(1f).testTag("start_prep_btn_${order.id}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                    ) {
                                        Text("Start Preparing", fontSize = 11.sp)
                                    }
                                }
                                "PREPARING" -> {
                                    Button(
                                        onClick = {
                                            orderToMarkReady = order
                                            showReadyConfirmDialog = true
                                        },
                                        modifier = Modifier.weight(1f).testTag("prep_btn_${order.id}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGoldSecondary)
                                    ) {
                                        Text("Ready for Pickup", fontSize = 11.sp)
                                    }
                                }
                                "READY_FOR_PICKUP" -> {
                                    if (isDelivery) {
                                        if (!order.courierName.isNullOrBlank()) {
                                            Button(
                                                onClick = {
                                                    orderForCourierVerification = order
                                                    courierVerificationCodeInput = ""
                                                    showCourierVerificationDialog = true
                                                },
                                                modifier = Modifier.weight(1f).testTag("handover_btn_${order.id}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                            ) {
                                                Text("Handover Courier", fontSize = 11.sp)
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(BrandGoldSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Delivery awaiting...",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandGoldSecondary)
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                orderForBuyerVerification = order
                                                buyerVerificationCodeInput = ""
                                                showBuyerVerificationDialog = true
                                            },
                                            modifier = Modifier.weight(1f).testTag("handover_buyer_btn_${order.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                        ) {
                                            Text("Handover to Buyer", fontSize = 11.sp)
                                        }
                                    }
                                }
                                "SHIPPED" -> {
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(order, "DELIVERED") },
                                        modifier = Modifier.weight(1f).testTag("complete_btn_${order.id}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                    ) {
                                        Text("Completed", fontSize = 11.sp)
                                    }
                                }
                                "OUT_FOR_DELIVERY" -> {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(BrandGreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(vertical = 8.dp, horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Out for delivery...",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandGreenPrimary)
                                        )
                                    }
                                }
                            }
                            
                            // Cancel button is visible ONLY for "PENDING", "CONFIRMED", and "PREPARING" order status
                            if (order.status == "PENDING" || order.status == "CONFIRMED" || order.status == "PREPARING") {
                                OutlinedButton(
                                    onClick = {
                                        selectedOrderToCancel = order
                                        cancelReasonInput = ""
                                        showCancelDialog = true
                                    },
                                    modifier = Modifier.weight(1f).testTag("cancel_btn_${order.id}"),
                                    border = BorderStroke(1.dp, BrandRedTertiary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRedTertiary)
                                ) {
                                    Text("Cancel Order", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun OrdersBlock() {
            val filteredOrders = orders.filter { order ->
                when (orderTabFilter) {
                    "ACTIVE" -> order.status in listOf("PENDING", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY")
                    "COMPLETED" -> order.status == "SHIPPED" || order.status == "DELIVERED"
                    "CANCELLED" -> order.status == "CANCELLED"
                    else -> true
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Customer Income Orders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "ACTIVE" to "Active (${orders.count { it.status in listOf("PENDING", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY") }})",
                        "COMPLETED" to "Completed (${orders.count { it.status == "SHIPPED" || it.status == "DELIVERED" }})",
                        "CANCELLED" to "Cancelled (${orders.count { it.status == "CANCELLED" }})"
                    )
                    tabs.forEach { (tabId, label) ->
                        val isSelected = orderTabFilter == tabId
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { orderTabFilter = tabId }
                                .testTag("order_tab_$tabId"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                if (filteredOrders.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ListAlt, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No orders in this status category.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    val displayOrders = filteredOrders.take(2)
                    displayOrders.forEach { order ->
                        OrderItemView(order)
                    }
                    if (filteredOrders.size > 2) {
                        Button(
                            onClick = { activeDashboardSubView = "ALL_ORDERS" },
                            modifier = Modifier.fillMaxWidth().testTag("see_more_orders_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("See More Orders (${filteredOrders.size - 2} more)")
                        }
                    }
                }
            }
        }

        @Composable
        fun ProductsInventoryBlock(
            viewModel: AppViewModel,
            categoryFilter: String,
            products: List<SellerProductEntity>
        ) {
            val isSellerDashboardLoading by viewModel.isSellerDashboardLoading.collectAsStateWithLifecycle()
            var searchQuery by remember { mutableStateOf("") }
            var activeCategory by remember(categoryFilter) { mutableStateOf(categoryFilter) }
            val context = LocalContext.current
            var productToDelete by remember { mutableStateOf<SellerProductEntity?>(null) }
            var deleteReason by remember { mutableStateOf("") }
            var customDeleteReason by remember { mutableStateOf("") }
            var filterOnlyLowStock by remember { mutableStateOf(false) }
            var showFilterMenu by remember { mutableStateOf(false) }

            // ✅ Fixed: Using remember with explicit parameter keys so products updates trigger recalculation
            val filteredProducts = remember(products, activeCategory, searchQuery, filterOnlyLowStock) {
                var result = products

                // Category filter
                if (activeCategory != "All") {
                    result = result.filter { it.category.equals(activeCategory, ignoreCase = true) }
                }

                // Search filter
                if (searchQuery.isNotBlank()) {
                    val query = searchQuery.lowercase()
                    result = result.filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        it.brand.contains(query, ignoreCase = true)
                    }
                }

                // Low stock filter
                if (filterOnlyLowStock) {
                    result = result.filter { it.stock <= it.lowStock }
                }

                com.example.ui.utils.DebugLogManager.log("ProductsInventoryBlock", "📦 ProductsInventoryBlock showing ${result.size} of ${products.size} products (category: $activeCategory, search: '$searchQuery', lowStockOnly: $filterOnlyLowStock)")
                result
            }

            // ✅ Fixed: Derived low stock count with remember key
            val lowStockCount = remember(products) {
                products.count { it.stock <= it.lowStock }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Prominent Low Stock Metric Card (Full Width)
                if (lowStockCount > 0) {
                    item(key = "low_stock_alert") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { filterOnlyLowStock = !filterOnlyLowStock },
                            colors = CardDefaults.cardColors(
                                containerColor = if (filterOnlyLowStock) Color(0xFFFF9800).copy(alpha = 0.15f) 
                                    else Color(0xFFFF9800).copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(
                                width = if (filterOnlyLowStock) 2.dp else 1.dp,
                                color = Color(0xFFFF9800).copy(alpha = if (filterOnlyLowStock) 0.8f else 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFF9800).copy(alpha = 0.15f), CircleShape)
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Low Stock Alert",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFF9800)
                                        )
                                        Text(
                                            "Products with low stock",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    "$lowStockCount",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                }

                // 2. Search bar and Categories Filter
                item(key = "search_bar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name or brand", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("inventory_search_bar"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreenPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Box {
                            IconButton(
                                onClick = { showFilterMenu = true },
                                modifier = Modifier
                                    .background(
                                        if (activeCategory != "All") BrandGreenPrimary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (activeCategory != "All") BrandGreenPrimary 
                                            else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter Categories",
                                    tint = if (activeCategory != "All") BrandGreenPrimary 
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                val categories = listOf("All", "Electronics", "Fashion", "Home", "Beauty", "Toys", "Sport", "Book")
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (activeCategory == cat) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = BrandGreenPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.size(16.dp))
                                                }
                                                Text(
                                                    cat,
                                                    fontWeight = if (activeCategory == cat) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        },
                                        onClick = {
                                            activeCategory = cat
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Active category and low stock filter chips
                if (activeCategory != "All" || filterOnlyLowStock) {
                    item(key = "filter_chips") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeCategory != "All") {
                                SuggestionChip(
                                    onClick = { 
                                        activeCategory = "All"
                                    },
                                    label = { Text("Category: $activeCategory") },
                                    icon = { 
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear Filter",
                                            modifier = Modifier.size(14.dp)
                                        ) 
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        labelColor = BrandGreenPrimary,
                                        iconContentColor = BrandGreenPrimary
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = BrandGreenPrimary.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            if (filterOnlyLowStock) {
                                SuggestionChip(
                                    onClick = { filterOnlyLowStock = false },
                                    label = { Text("Filter: Low Stock") },
                                    icon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear Filter",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        labelColor = Color(0xFFFF9800),
                                        iconContentColor = Color(0xFFFF9800)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = Color(0xFFFF9800).copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Render list of products
                if (isSellerDashboardLoading) {
                    items(4) {
                        ProductInventorySkeletonItem()
                    }
                } else if (filteredProducts.isEmpty()) {
                    item(key = "empty_state") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No products found matching your filter.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.editProductFromLiveStock = false
                                            viewModel.setEditingProduct(null)
                                            viewModel.navigateTo(AppScreen.EDIT_PRODUCT_SCREEN)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(loc("add_product"))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ✅ OPTIMIZED: Using items with keys for better performance
                    items(
                        items = filteredProducts,
                        key = { prod -> if (prod.id.isNotBlank()) prod.id else "prod_${prod.name}_${prod.hashCode()}" }
                    ) { prod ->
                        ProductInventoryItem(
                            product = prod,
                            viewModel = viewModel,
                            onEdit = {
                                viewModel.editProductFromLiveStock = true
                                viewModel.setEditingProduct(prod)
                                viewModel.navigateTo(AppScreen.EDIT_PRODUCT_SCREEN)
                            },
                            onDelete = { productToDelete = prod }
                        )
                    }
                }
            }

            // Delete Confirmation Dialog
            if (productToDelete != null) {
                ProductDeleteDialog(
                    product = productToDelete!!,
                    onDismiss = {
                        productToDelete = null
                        deleteReason = ""
                        customDeleteReason = ""
                    },
                    onConfirmDelete = { reason ->
                        val finalReason = if (reason == "Other (Write details)") customDeleteReason else reason
                        if (finalReason.isNotBlank()) {
                            viewModel.deleteSellerProduct(productToDelete!!.id)
                            Toast.makeText(context, "Deleted: ${productToDelete!!.name}", Toast.LENGTH_LONG).show()
                        }
                        productToDelete = null
                        deleteReason = ""
                        customDeleteReason = ""
                    },
                    deleteReason = deleteReason,
                    customDeleteReason = customDeleteReason,
                    onDeleteReasonChange = { deleteReason = it },
                    onCustomDeleteReasonChange = { customDeleteReason = it }
                )
            }
        }


        if (activeDashboardSubView != "MAIN") {
            androidx.activity.compose.BackHandler {
                if (activeDashboardSubView == "ORDER_DETAIL" || activeDashboardSubView == "DAY_SALES_DETAIL" || activeDashboardSubView == "CUSTOMER_FEEDBACK") {
                    activeDashboardSubView = previousDashboardSubView
                } else if (activeDashboardSubView == "MONTHLY_REPORT_PREVIEW" || activeDashboardSubView == "PAYOUT_TRANSACTIONS_DETAIL") {
                    activeDashboardSubView = "MONTHLY_PROFITABILITY_DETAIL"
                } else if (activeDashboardSubView == "AGING_INVENTORY_DETAIL" || activeDashboardSubView == "REVENUE_DRIVERS_DETAIL" || activeDashboardSubView == "DAYS_TO_SELL_HEATMAP_DETAIL" || activeDashboardSubView == "MONTHLY_PROFITABILITY_DETAIL" || activeDashboardSubView == "PAYOUT_HISTORY_DETAIL") {
                    activeDashboardSubView = "FINANCIALS"
                } else {
                    activeDashboardSubView = "MAIN"
                }
            }
        }

        // Parent layout
        val isParentScrollable = activeDashboardSubView != "ALL_ORDERS" && 
                                activeDashboardSubView != "INVENTORY" && 
                                activeDashboardSubView != "ORDER_DETAIL" && 
                                activeDashboardSubView != "MERCHANT_SETTINGS" &&
                                activeDashboardSubView != "AGING_INVENTORY_DETAIL" &&
                                activeDashboardSubView != "REVENUE_DRIVERS_DETAIL" &&
                                activeDashboardSubView != "DAYS_TO_SELL_HEATMAP_DETAIL" &&
                                activeDashboardSubView != "MONTHLY_PROFITABILITY_DETAIL" &&
                                activeDashboardSubView != "MONTHLY_REPORT_PREVIEW" &&
                                activeDashboardSubView != "PAYOUT_TRANSACTIONS_DETAIL" &&
                                activeDashboardSubView != "DAY_SALES_DETAIL" && 
                                activeDashboardSubView != "PAYOUT_HISTORY_DETAIL" &&
                                activeDashboardSubView != "CUSTOMER_FEEDBACK"
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isParentScrollable) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                )
                .padding(if (activeDashboardSubView == "MERCHANT_SETTINGS") 0.dp else 16.dp),
            verticalArrangement = if (activeDashboardSubView == "MERCHANT_SETTINGS") Arrangement.Top else Arrangement.spacedBy(16.dp)
        ) {
            when (activeDashboardSubView) {
                "INVENTORY" -> {
                    // Header row with Back Button
                    Row(
                        modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { activeDashboardSubView = "MAIN" },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .testTag("inventory_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Dashboard")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Live Stock Controls",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Update product inventory and manage listings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Show Inventory stock controls
                    ProductsInventoryBlock(viewModel, selectedCategoryFilter, products)
                }
                "FINANCIALS" -> {
                    // Header row with Back Button and Recent Payout History Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = { activeDashboardSubView = "MAIN" },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .testTag("financials_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Dashboard")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Financial Analytics",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Deep dive into your sales and disbursements",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = { 
                                previousDashboardSubView = activeDashboardSubView
                                activeDashboardSubView = "PAYOUT_HISTORY_DETAIL" 
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("recent_payout_history_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Payout History & Search",
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // NEW: Aging of Inventory (Dead Stock) Clickable Preview Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeDashboardSubView = "AGING_INVENTORY_DETAIL" }
                            .testTag("aging_inventory_card_clickable"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Aging of Inventory (Dead Stock) ➔",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap to open complete warehouse inventory aging list, slow-moving items & clearance advice",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (deadStockPercentState >= 50f) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Dead Stock Alert",
                                        tint = BrandRedTertiary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Visual representation bar/breakdown (Dynamic based on simulator state)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val invTotalCount = sellerInventoryList.size
                                val invActiveCount = sellerInventoryList.count { it.actualDaysOld <= ageActiveLimit }
                                val invNormalCount = sellerInventoryList.count { it.actualDaysOld > ageActiveLimit && it.actualDaysOld <= ageNormalLimit }
                                val invDeadSlowCount = sellerInventoryList.count { it.actualDaysOld > ageNormalLimit }

                                val activePctStr = if (invTotalCount > 0) String.format(java.util.Locale.US, "%.0f%%", (invActiveCount.toFloat() / invTotalCount) * 100f) else "0%"
                                val normalPctStr = if (invTotalCount > 0) String.format(java.util.Locale.US, "%.0f%%", (invNormalCount.toFloat() / invTotalCount) * 100f) else "0%"
                                val deadPctStr = if (invTotalCount > 0) String.format(java.util.Locale.US, "%.0f%%", (invDeadSlowCount.toFloat() / invTotalCount) * 100f) else "0%"

                                val breakdownList = listOf(
                                    Pair("0-$ageActiveLimit Days", activePctStr) to BrandGreenPrimary,
                                    Pair("${ageActiveLimit + 1}-$ageNormalLimit Days", normalPctStr) to BrandGoldSecondary,
                                    Pair(">$ageNormalLimit Days", deadPctStr) to BrandRedTertiary
                                )
                                breakdownList.forEach { (pair, color) ->
                                    val (days, pct) = pair
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(color.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = days, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color, fontSize = 9.sp)
                                        Text(text = pct, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Days-to-Sell Restocking Heatmap Block (with current day highlight and click page)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Days-to-Sell Heatmap",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "View Calendar ➔",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BrandGreenPrimary),
                                    modifier = Modifier
                                        .clickable { activeDashboardSubView = "DAYS_TO_SELL_HEATMAP_DETAIL" }
                                        .testTag("view_calendar_heatmap_btn")
                                )
                            }
                            Text(
                                text = "Visualizing which days of the week sell best. Tap on any day to see detailed insights.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Calculate current day name
                            val currentDayIndex = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                            val currentDayName = when (currentDayIndex) {
                                java.util.Calendar.MONDAY -> "Mon"
                                java.util.Calendar.TUESDAY -> "Tue"
                                java.util.Calendar.WEDNESDAY -> "Wed"
                                java.util.Calendar.THURSDAY -> "Thu"
                                java.util.Calendar.FRIDAY -> "Fri"
                                java.util.Calendar.SATURDAY -> "Sat"
                                java.util.Calendar.SUNDAY -> "Sun"
                                else -> ""
                            }

                            // Days Heatmap Squares
                            val defaultDaysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val daysOfWeek = defaultDaysList.map { day ->
                                val matched = sellerDailySalesList.firstOrNull { item ->
                                    item.day_week.startsWith(day, ignoreCase = true) ||
                                    item.dayShort.equals(day, ignoreCase = true) ||
                                    item.dayName.startsWith(day, ignoreCase = true) ||
                                    item.sale_date == day ||
                                    (item.sale_date.isNotBlank() && try {
                                        val dt = java.time.LocalDate.parse(item.sale_date.take(10))
                                        dt.dayOfWeek.name.take(3).equals(day, ignoreCase = true)
                                    } catch (e: Exception) { false })
                                }
                                val count = matched?.total_orders ?: 0
                                val color = when {
                                    count >= 100 -> BrandGreenPrimary
                                    count >= 60 -> BrandGreenPrimary.copy(alpha = 0.75f)
                                    count >= 40 -> BrandGreenPrimary.copy(alpha = 0.5f)
                                    count >= 5 -> BrandGreenPrimary.copy(alpha = 0.25f)
                                    count > 0 -> BrandGreenPrimary.copy(alpha = 0.15f)
                                    else -> BrandGreenPrimary.copy(alpha = 0.08f)
                                }
                                Triple(day, count, color)
                            }

                            val max7DayOrdersCount = daysOfWeek.maxOfOrNull { it.second } ?: 0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                daysOfWeek.forEach { (day, count, color) ->
                                    val isToday = day == currentDayName
                                    val isHighestOrderDay = max7DayOrdersCount > 0 && count == max7DayOrdersCount
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedHeatmapDay = day
                                                previousDashboardSubView = activeDashboardSubView
                                                activeDashboardSubView = "DAY_SALES_DETAIL"
                                            }
                                            .testTag("heatmap_day_tap_$day"),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .background(color, RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (isToday || isHighestOrderDay) 2.dp else 1.dp,
                                                    color = if (isToday) BrandGreenPrimary else if (isHighestOrderDay) BrandGoldSecondary else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (color == BrandGreenPrimary) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isToday) "$day (Today)" else day,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold
                                            ),
                                            color = if (isToday) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Color-coded legend for Heatmap
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(BrandGreenPrimary.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Low (<30)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(BrandGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Medium (30-60)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(BrandGreenPrimary, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "High (>60)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Restocking advice block
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BrandGoldSecondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Restock Advisory", tint = BrandGoldSecondary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "💡 Restocking Tip: Friday & Saturday are peak sales days in Addis Ababa. Schedule and complete your wholesale inventory restocks by Wednesday afternoon to prevent stockouts!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Revenue Category Share & Best Category Spotlight (clickable for detailed drivers view)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeDashboardSubView = "REVENUE_DRIVERS_DETAIL" }
                            .testTag("revenue_drivers_card_clickable"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Revenue Drivers & Top Category ➔",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap to open complete performance analytics, top categories & best items",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandGreenPrimary)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Product Type Progress bars (Pre-visual overview using _sellerRevenueFlow / sellerRevenueList)
                            val latestRevenueItem = sellerRevenueList.firstOrNull()
                            val flowTopCats = latestRevenueItem?.top10Categories ?: emptyList()
                            val flowTotalCatRev = flowTopCats.sumOf { it.second }.coerceAtLeast(1.0)

                            val revenueShares = if (flowTopCats.isNotEmpty()) {
                                flowTopCats.take(3).map { (catName, rev, _) ->
                                    val shareFloat = (rev / flowTotalCatRev).toFloat()
                                    val sharePctStr = "${(shareFloat * 100).toInt()}%"
                                    Triple(catName, shareFloat, sharePctStr)
                                }
                            } else {
                                listOf(
                                    Triple("Agricultural Goods (Teff, Grains)", 0.62f, "62%"),
                                    Triple("Spices, Herbs & Organic Foods", 0.24f, "24%"),
                                    Triple("Traditional Textiles & Handicrafts", 0.14f, "14%")
                                )
                            }

                            revenueShares.forEach { (type, share, label) ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { share },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = if (share > 0.5f) BrandGreenPrimary else BrandGoldSecondary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Spotlight on the best category sold from _sellerRevenueFlow / sellerRevenueList
                            val bestCatSpotlight = flowTopCats.firstOrNull()
                            val bestCatName = bestCatSpotlight?.first ?: "Yirgacheffe Coffee (Premium beans)"
                            val bestCatRev = bestCatSpotlight?.second ?: 24800.0
                            val bestCatUnits = bestCatSpotlight?.third ?: 124
                            val bestCatMonthLabel = latestRevenueItem?.month ?: "Current Period"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BrandGreenPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .border(1.dp, BrandGreenPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(BrandGreenPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Top seller spotlight", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Top Sold Category ($bestCatMonthLabel)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BrandGreenPrimary
                                    )
                                    Text(
                                        text = bestCatName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$bestCatUnits units sold • Generated ${String.format(java.util.Locale.US, "%,.2f", bestCatRev)} ETB revenue",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Monthly Financial Performance (Moved here as the 4th block)
                    PerformanceChartBlock()
                }
                "REVENUE_DRIVERS_DETAIL" -> {
                    SellerDashboardRevenueDriversDetail(
                        sellerId = sellerId,
                        sellerRevenueList = sellerRevenueList,
                        viewModel = viewModel,
                        onBack = { activeDashboardSubView = "FINANCIALS" }
                    )
                }
                "AGING_INVENTORY_DETAIL" -> {
                    var selectedAgeCategory by remember { mutableStateOf("Overview") } // "Overview", "Active", "Normal", "Slow", "Dead"

                    // Header row with Back Button (Fixed)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeDashboardSubView = "FINANCIALS" },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("aging_inventory_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Financial Analytics")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Aging of Inventory",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Identify products sitting in warehouse unsold to run clearance promotions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Age categories navigation row (Fixed)
                    val ageTabs = listOf("Overview", "Active", "Normal", "Slow", "Dead")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(ageTabs) { tab ->
                            val isSelected = selectedAgeCategory == tab
                            val tabColor = when (tab) {
                                "Active" -> BrandGreenPrimary
                                "Normal" -> BrandGoldSecondary
                                "Slow" -> Color(0xFF9C27B0)
                                "Dead" -> BrandRedTertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Card(
                                modifier = Modifier
                                    .clickable { selectedAgeCategory = tab }
                                    .testTag("age_tab_$tab"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) tabColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = when (tab) {
                                        "Overview" -> "📊 Overview"
                                        "Active" -> "🟢 Active (<${ageActiveLimit}d)"
                                        "Normal" -> "🟡 Normal (${ageActiveLimit + 1}-${ageNormalLimit}d)"
                                        "Slow" -> "🟣 Slow (${ageNormalLimit + 1}-${ageSlowLimit}d)"
                                        "Dead" -> "🔴 Dead (>${ageSlowLimit}d)"
                                        else -> tab
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    
                    if (selectedAgeCategory == "Overview") {
                        // Scrollable content area
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Card 1: Inventory Age Breakdown
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Inventory Age Breakdown",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap on any row below to view full stock lists in that category",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    val invTotalCount = sellerInventoryList.size
                                    val invActiveCount = sellerInventoryList.count { it.actualDaysOld <= ageActiveLimit }
                                    val invNormalCount = sellerInventoryList.count { it.actualDaysOld > ageActiveLimit && it.actualDaysOld <= ageNormalLimit }
                                    val invSlowCount = sellerInventoryList.count { it.actualDaysOld > ageNormalLimit && it.actualDaysOld <= ageSlowLimit }
                                    val invDeadCount = sellerInventoryList.count { it.actualDaysOld > ageSlowLimit }

                                    val activePctStr = if (invTotalCount > 0) String.format(java.util.Locale.US, "%.0f%%", (invActiveCount.toFloat() / invTotalCount) * 100f) else "0%"
                                    val normalPctStr = if (invTotalCount > 0) String.format(java.util.Locale.US, "%.0f%%", (invNormalCount.toFloat() / invTotalCount) * 100f) else "0%"
                                    val slowPctStr = if (invTotalCount > 0) String.format(java.util.Locale.US, "%.0f%%", (invSlowCount.toFloat() / invTotalCount) * 100f) else "0%"
                                    val deadPctStr = if (invTotalCount > 0) String.format(java.util.Locale.US, "%.0f%%", (invDeadCount.toFloat() / invTotalCount) * 100f) else "0%"

                                    val breakdownList = listOf(
                                        Triple("Active", "Active • Regular selling frequency (0-${ageActiveLimit} days)", activePctStr) to BrandGreenPrimary,
                                        Triple("Normal", "Normal • Stable sales rotation (${ageActiveLimit + 1}-${ageNormalLimit} days)", normalPctStr) to BrandGoldSecondary,
                                        Triple("Slow", "Slow • Slower turn than average (${ageNormalLimit + 1}-${ageSlowLimit} days)", slowPctStr) to Color(0xFF9C27B0),
                                        Triple("Dead", "Dead • Unsold over two months (>${ageSlowLimit} days)", deadPctStr) to BrandRedTertiary
                                    )
                                    breakdownList.forEach { (triple, color) ->
                                        val (days, desc, pct) = triple
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedAgeCategory = days }
                                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = days, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = color)
                                                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = pct, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = color)
                                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val pctFloat = try { pct.replace("%", "").toFloat() / 100f } catch(e: Exception) { 0.15f }
                                            LinearProgressIndicator(
                                                progress = { pctFloat },
                                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                                color = color,
                                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Card 3: Actionable Advice
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = BrandGreenPrimary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, BrandGreenPrimary.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = "Tip", tint = BrandGoldSecondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Warehouse Optimization Guide",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "1. Liquidate slow-moving stock by running clearance store discounts of 20-30%.\n" +
                                                   "2. Sourcing smaller batch sizes for traditional heavy textiles ensures your capital is not tied up in low-turn items.\n" +
                                                   "3. Relocate older items to the front of your store storage to follow First-In-First-Out (FIFO) retail guidelines.",
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Category stocks list page
                        val categoryColor = when (selectedAgeCategory) {
                            "Active" -> BrandGreenPrimary
                            "Normal" -> BrandGoldSecondary
                            "Slow" -> Color(0xFF9C27B0)
                            "Dead" -> BrandRedTertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Category summary header card
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = categoryColor.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (selectedAgeCategory) {
                                                "Active" -> "🟢"
                                                "Normal" -> "🟡"
                                                "Slow" -> "🟣"
                                                "Dead" -> "🔴"
                                                else -> "📦"
                                            },
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "$selectedAgeCategory Stocks",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = categoryColor
                                        )
                                        Text(
                                            text = when (selectedAgeCategory) {
                                                "Active" -> "Items selling extremely well. Keep inventory levels topped up."
                                                "Normal" -> "Healthy rotation rate. Review monthly for restocking."
                                                "Slow" -> "Turnover is dropping. Sourcing small batches recommended."
                                                "Dead" -> "Immediate attention required! Highly recommend running 20-30% clearance promos."
                                                else -> ""
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // Filter sellerInventoryList for the selected age category
                            val categoryProducts = sellerInventoryList.filter { item ->
                                when (selectedAgeCategory) {
                                    "Active" -> item.actualDaysOld <= ageActiveLimit
                                    "Normal" -> item.actualDaysOld > ageActiveLimit && item.actualDaysOld <= ageNormalLimit
                                    "Slow" -> item.actualDaysOld > ageNormalLimit && item.actualDaysOld <= ageSlowLimit
                                    "Dead" -> item.actualDaysOld > ageSlowLimit
                                    else -> true
                                }
                            }
                            
                            if (categoryProducts.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No products in $selectedAgeCategory category right now.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(categoryProducts) { item ->
                                        val ageDays = "${item.actualDaysOld} Days"
                                        val catInfo = "${item.category}${if (!item.subcategory.isNullOrBlank()) " • " + item.subcategory else ""} • $ageDays"
                                        val qtyAndVal = "${item.stock} items • Value: ${String.format(java.util.Locale.US, "%,.0f", item.stock * item.price)} ETB"
                                        
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = item.product_name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                    Text(text = catInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = qtyAndVal, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = categoryColor.copy(alpha = 0.15f)),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = ageDays,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = categoryColor),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    if (selectedAgeCategory == "Dead" || selectedAgeCategory == "Slow") {
                                                        Text(
                                                            text = "🏷️ Squeeze Promo",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandGreenPrimary),
                                                            modifier = Modifier
                                                                .clickable {
                                                                    squeezePromoProductId = item.product_id
                                                                    squeezePromoItemName = item.product_name
                                                                    squeezePromoItemAge = ageDays
                                                                    squeezePromoItemType = selectedAgeCategory
                                                                    squeezePromoItemOriginalPrice = item.price
                                                                    squeezePromoInputPrice = (item.price * 0.80).toInt().toString()
                                                                    squeezePromoFreeShippingActive = true
                                                                    showSqueezePromoDialog = true
                                                                }
                                                                .background(BrandGreenPrimary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "✓ Sourcing Normal",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandGreenPrimary),
                                                            modifier = Modifier
                                                                .background(BrandGreenPrimary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showSqueezePromoDialog) {
                        AlertDialog(
                            onDismissRequest = { showSqueezePromoDialog = false },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showSqueezePromoDialog = false
                                        merchantDiscountPercent = try {
                                            val entered = squeezePromoInputPrice.toDoubleOrNull() ?: squeezePromoItemOriginalPrice
                                            val discAmt = squeezePromoItemOriginalPrice - entered
                                            ((discAmt / squeezePromoItemOriginalPrice) * 100).toInt().coerceIn(0, 100)
                                        } catch(e: Exception) { 20 }
                                        val shippingText = if (squeezePromoFreeShippingActive) "and FREE Delivery active" else "with standard delivery"
                                        val finalPriceVal = squeezePromoInputPrice.toDoubleOrNull() ?: (squeezePromoItemOriginalPrice * 0.8)
                                        if (squeezePromoProductId.isNotBlank()) {
                                            val matchingProd = viewModel.sellerProducts.value.find { it.id == squeezePromoProductId }
                                             if (matchingProd != null) {
                                                 val updatedProd = matchingProd.copy(
                                                     isPromoted = true,
                                                     isFreeDelivery = squeezePromoFreeShippingActive,
                                                     discountPercent = merchantDiscountPercent
                                                 )
                                                 viewModel.updateSellerProduct(updatedProd)
                                             }
                                        }
                                        android.widget.Toast.makeText(context, "⚡ Campaign Launched! Clearance Promo (${String.format("%.0f", finalPriceVal)} ETB) $shippingText for $squeezePromoItemName!", android.widget.Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                ) {
                                    Text("Launch Campaign ⚡", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSqueezePromoDialog = false }) {
                                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                                }
                            },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = BrandGreenPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Clearance Squeeze Promo",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Configure high-velocity promotion for slow/dead stock item to free up warehouse capital.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    // Item Info Card
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(squeezePromoItemName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Age: $squeezePromoItemAge", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = if (squeezePromoItemType == "Dead") BrandRedTertiary.copy(alpha = 0.15f) else Color(0xFF9C27B0).copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = squeezePromoItemType.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (squeezePromoItemType == "Dead") BrandRedTertiary else Color(0xFF9C27B0)),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Price Input Section
                                    Text(
                                        text = "🏷️ SET PROMOTIONAL PRICE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    OutlinedTextField(
                                        value = squeezePromoInputPrice,
                                        onValueChange = { newValue ->
                                            if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                                                squeezePromoInputPrice = newValue
                                            }
                                        },
                                        label = { Text("Promotional Price (ETB)") },
                                        placeholder = { Text("e.g. 1200") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = {
                                            Text("ETB", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                        }
                                    )

                                    // Toggle Free Shipping
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = if (squeezePromoFreeShippingActive) BrandGreenPrimary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Enable FREE Shipping",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (squeezePromoFreeShippingActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Switch(
                                            checked = squeezePromoFreeShippingActive,
                                            onCheckedChange = { squeezePromoFreeShippingActive = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = BrandGreenPrimary,
                                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                    
                                    // Calculation details dynamically based on input
                                    val finalPrice = squeezePromoInputPrice.toDoubleOrNull() ?: (squeezePromoItemOriginalPrice * 0.8)
                                    val discountAmount = maxOf(0.0, squeezePromoItemOriginalPrice - finalPrice)
                                    val discountPercentComputed = if (squeezePromoItemOriginalPrice > 0) ((discountAmount / squeezePromoItemOriginalPrice) * 100).toInt() else 0
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Original Est. Unit Price:", style = MaterialTheme.typography.bodySmall)
                                            Text(String.format("%.2f ETB", squeezePromoItemOriginalPrice), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Discount Value ($discountPercentComputed%):", style = MaterialTheme.typography.bodySmall, color = BrandRedTertiary)
                                            Text(String.format("-%.2f ETB", discountAmount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Final Promotional Price:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(String.format("%.2f ETB", finalPrice), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BrandGreenPrimary))
                                        }
                                    }
                                    
                                    // Free Shipping status card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (squeezePromoFreeShippingActive) BrandGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (squeezePromoFreeShippingActive) BrandGreenPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = if (squeezePromoFreeShippingActive) BrandGreenPrimary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (squeezePromoFreeShippingActive) "FREE Shipping Enabled" else "Standard Shipping Selected",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (squeezePromoFreeShippingActive) BrandGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Card(
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (squeezePromoFreeShippingActive) BrandGreenPrimary else MaterialTheme.colorScheme.outline
                                                        ),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (squeezePromoFreeShippingActive) "Active" else "Off",
                                                            fontSize = 8.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (squeezePromoFreeShippingActive) {
                                                        "To accelerate clearance of slow/dead stock, E-Suuq applies 100% Free Shipping for buyers! Subsidized completely from warehouse storage savings, boosting sales speed by 4.2x."
                                                    } else {
                                                        "Buyers will pay standard shipping fees. Recommended for medium-high demand products. For dead stock, enabling Free Shipping is highly recommended to expedite clearance."
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        )
                    }
                }
                "DAY_SALES_DETAIL" -> {
                    val selectedDaySale = sellerDailySalesList.firstOrNull { item ->
                        item.day_week.startsWith(selectedHeatmapDay ?: "", ignoreCase = true) ||
                        item.dayShort.equals(selectedHeatmapDay ?: "", ignoreCase = true) ||
                        item.dayName.startsWith(selectedHeatmapDay ?: "", ignoreCase = true) ||
                        item.sale_date == selectedHeatmapDay ||
                        (item.sale_date.isNotBlank() && try {
                            val dt = java.time.LocalDate.parse(item.sale_date.take(10))
                            dt.dayOfWeek.name.take(3).equals(selectedHeatmapDay, ignoreCase = true)
                        } catch (e: Exception) { false })
                    }

                    val displayCompleted = selectedDaySale?.completed_orders?.toString() ?: "0"
                    val displaySalesValue = selectedDaySale?.let { String.format("%,.2f ETB", it.total_sales) } ?: "0.00 ETB"
                    val displayCancelled = selectedDaySale?.canceled_orders?.toString() ?: "0"

                    val categoriesList = if (selectedDaySale != null && selectedDaySale.top10Categories.isNotEmpty()) {
                        selectedDaySale.top10Categories.map { (catName, rev, units) ->
                            Triple(catName, String.format("%,.2f ETB (%d units)", rev, units), "🏷️")
                        }
                    } else {
                        emptyList()
                    }

                    val brandsList = if (selectedDaySale != null && selectedDaySale.top10Brands.isNotEmpty()) {
                        selectedDaySale.top10Brands.map { (bName, rev, units) ->
                            Triple(bName, String.format("%,.2f ETB (%d units)", rev, units), "✨")
                        }
                    } else {
                        emptyList()
                    }

                    // Header row with Back Button and AppBar Share button
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { activeDashboardSubView = previousDashboardSubView },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .testTag("day_sales_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Sales Detail: $selectedHeatmapDay",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Weekly purchasing trends and peak transaction analysis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                val topCategoryInfo = categoriesList.joinToString("\n") { "• ${it.third} ${it.first} (${it.second})" }
                                val shareText = "🚀 E-Suuq platform campaign launched!\n📊 Day: $selectedHeatmapDay\n📈 Orders: $displayCompleted\n⭐ Top Categories:\n$topCategoryInfo\n📢 Shared to social media and E-Suuq buyers platform!"
                                android.widget.Toast.makeText(context, shareText, android.widget.Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("appbar_share_campaign_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share campaign to E-Suuq or social media")
                        }
                    }
                    
                    // Detailed day sales breakdown (Scrollable)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card 1: Sales Summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Performance & Demand",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = BrandGreenPrimary.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            text = "HIGH DEMAND",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandGreenPrimary),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$displayCompleted Orders", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text("Sales Value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(displaySalesValue, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Cancelled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$displayCancelled Orders", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                    }
                                }
                            }
                        }
                        
                        // Card 2: Peak Hours
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "⏰ Peak Buying Hours on ${selectedHeatmapDay}s",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val peakHoursData = if (selectedDaySale != null && selectedDaySale.peakHours.isNotEmpty()) {
                                    val maxOrders = selectedDaySale.peakHours.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                                    selectedDaySale.peakHours.map { (hourRaw, count) ->
                                        val formattedHour = selectedDaySale.formatHour(hourRaw)
                                        val ratio = (count.toFloat() / maxOrders.toFloat()).coerceIn(0.1f, 1.0f)
                                        val desc = "$count orders"
                                        Triple(formattedHour, ratio, desc)
                                    }
                                } else {
                                    listOf(
                                        Triple("10:00 AM - 12:00 PM", 0.35f, "Normal traffic"),
                                        Triple("2:00 PM - 5:00 PM", 0.65f, "Moderately busy"),
                                        Triple("5:00 PM - 8:00 PM", 0.95f, "Peak Rush Hours")
                                    )
                                }
                                
                                peakHoursData.forEach { (time, ratio, desc) ->
                                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = time, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { ratio },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = if (ratio > 0.8f) BrandGreenPrimary else BrandGoldSecondary,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Most Bought Categories Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "🏷️ Most Bought Categories",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                categoriesList.forEach { (category, salesQty, iconEmoji) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(BrandGreenPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = iconEmoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(category, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(salesQty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Most Bought Brands Card (if present)
                        if (brandsList != null && brandsList.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "✨ Most Bought Brands",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    brandsList.forEach { (brandName, salesQty, iconEmoji) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(BrandGoldSecondary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = iconEmoji, fontSize = 24.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(brandName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                Text(salesQty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "DAYS_TO_SELL_HEATMAP_DETAIL" -> {
                    // Header row with Back Button (Fixed)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeDashboardSubView = "FINANCIALS" },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("days_heatmap_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Financial Analytics")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Monthly Restocking Heatmap",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Calendar grid analyzing product sales velocities and optimal stocking days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Scrollable Calendar and Details Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card 1: Calendar View
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val monthOrdersList = real3YearMonths.map { ym ->
                                    val ymIso = ym.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                                    val totalOrders = sellerDailySalesList.filter { item ->
                                        item.sale_date.startsWith(ymIso)
                                    }.sumOf { it.total_orders }
                                    ymIso to totalOrders
                                }
                                val maxOrdersAcross3Months = monthOrdersList.maxOfOrNull { it.second } ?: 0
                                val highestOrderMonthIndex = if (maxOrdersAcross3Months > 0) {
                                    monthOrdersList.indexOfFirst { it.second == maxOrdersAcross3Months }
                                } else -1

                                // 3 Month Selector Chips (2 past months + 1 current month)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    real3YearMonths.forEachIndexed { idx, ym ->
                                        val isSelectedMonth = (idx == restockingMonthIndex)
                                        val isHighestMonth = (idx == highestOrderMonthIndex)
                                        val label = restockingMonths.getOrElse(idx) { "" }
                                        val chipBorderColor = if (isSelectedMonth) BrandGreenPrimary else if (isHighestMonth) BrandGoldSecondary else MaterialTheme.colorScheme.outlineVariant
                                        val chipContainerColor = if (isSelectedMonth) BrandGreenPrimary.copy(alpha = 0.12f) else if (isHighestMonth) BrandGoldSecondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface

                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    restockingMonthIndex = idx
                                                    selectedCalendarDay = 1
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = chipContainerColor),
                                            border = BorderStroke(if (isSelectedMonth || isHighestMonth) 2.dp else 1.dp, chipBorderColor)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSelectedMonth) BrandGreenPrimary else if (isHighestMonth) BrandGoldSecondary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isHighestMonth) {
                                                    Text(
                                                        text = "⭐ Peak Month",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                        color = BrandGoldSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Header text for currently selected month
                                val currentSelectedYM = real3YearMonths.getOrElse(restockingMonthIndex) { currentYM }
                                val currentMonthIso = currentSelectedYM.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                                val totalDays = currentSelectedYM.lengthOfMonth()
                                val startOffset = (currentSelectedYM.atDay(1).dayOfWeek.value - 1) % 7 // Mon = 0
                                
                                // Calendar Days Names Headers (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    val daysHeader = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    daysHeader.forEach { dayName ->
                                        Text(
                                            text = dayName,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                var dayCounter = 1
                                
                                for (row in 0..4) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        for (col in 0..6) {
                                            val cellIndex = row * 7 + col
                                            if (cellIndex >= startOffset && dayCounter <= totalDays) {
                                                val currentDay = dayCounter
                                                val currentDayFormatted = String.format(java.util.Locale.US, "%02d", currentDay)
                                                val currentDayIso = "$currentMonthIso-$currentDayFormatted"

                                                val maxMonthOrdersValue = (1..totalDays).maxOfOrNull { dayNum ->
                                                    val dFormatted = String.format(java.util.Locale.US, "%02d", dayNum)
                                                    val dIso = "$currentMonthIso-$dFormatted"
                                                    val match = sellerDailySalesList.firstOrNull { item ->
                                                        item.sale_date.startsWith(dIso) ||
                                                        item.sale_date.endsWith("-$dFormatted") ||
                                                        item.sale_date.endsWith("-$dayNum")
                                                    }
                                                    match?.total_orders ?: 0
                                                } ?: 0
                                                val matchedCalendarSale = sellerDailySalesList.firstOrNull { item ->
                                                    item.sale_date.startsWith(currentDayIso) ||
                                                    item.sale_date.endsWith("-$currentDayFormatted") ||
                                                    item.sale_date.endsWith("-$currentDay")
                                                }
                                                val countValue = matchedCalendarSale?.total_orders ?: 0
                                                
                                                val cellColor = when {
                                                    countValue >= 90 -> BrandGreenPrimary
                                                    countValue >= 60 -> BrandGreenPrimary.copy(alpha = 0.75f)
                                                    countValue >= 40 -> BrandGreenPrimary.copy(alpha = 0.5f)
                                                    countValue >= 5 -> BrandGreenPrimary.copy(alpha = 0.25f)
                                                    countValue > 0 -> BrandGreenPrimary.copy(alpha = 0.15f)
                                                    else -> BrandGreenPrimary.copy(alpha = 0.08f)
                                                }
                                                
                                                val realTodayLocalDate = remember { java.time.LocalDate.now() }
                                                val isRealTodayDate = (restockingMonthIndex == 2) && (currentDay == realTodayLocalDate.dayOfMonth)
                                                val isSelected = selectedCalendarDay == currentDay
                                                val isHighestOrderCalendarDay = maxMonthOrdersValue > 0 && countValue == maxMonthOrdersValue
                                                
                                                val cellBorderColor = when {
                                                    isRealTodayDate -> BrandGreenPrimary
                                                    isHighestOrderCalendarDay -> BrandGoldSecondary
                                                    isSelected -> BrandGreenPrimary.copy(alpha = 0.5f)
                                                    else -> Color.Transparent
                                                }
                                                val cellBorderWidth = if (isRealTodayDate || isHighestOrderCalendarDay || isSelected) 2.dp else 1.dp

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .background(cellColor, RoundedCornerShape(8.dp))
                                                        .border(
                                                            width = cellBorderWidth,
                                                            color = cellBorderColor,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            selectedCalendarDay = currentDay
                                                            val dayOfWeek = (currentDay - 1 + startOffset) % 7
                                                            selectedHeatmapDay = when (dayOfWeek) {
                                                                0 -> "Mon"
                                                                1 -> "Tue"
                                                                2 -> "Wed"
                                                                3 -> "Thu"
                                                                4 -> "Fri"
                                                                5 -> "Sat"
                                                                6 -> "Sun"
                                                                else -> "Fri"
                                                            }
                                                            previousDashboardSubView = activeDashboardSubView
                                                            activeDashboardSubView = "DAY_SALES_DETAIL"
                                                        }
                                                        .testTag("calendar_day_cell_month_${restockingMonthIndex}_day_$currentDay"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = "$currentDay",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                                            color = if (countValue >= 60) Color.White else MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 11.sp
                                                        )
                                                        Text(
                                                            text = "$countValue",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = if (countValue >= 60) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline,
                                                            fontSize = 8.sp
                                                        )
                                                    }
                                                }
                                                dayCounter++
                                            } else {
                                                // Blank placeholders for start or end of month
                                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Legend Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val legendItems = listOf(
                                        Pair("Low (<20)", BrandGreenPrimary.copy(alpha = 0.1f)),
                                        Pair("Med (20-40)", BrandGreenPrimary.copy(alpha = 0.35f)),
                                        Pair("High (40-90)", BrandGreenPrimary.copy(alpha = 0.65f)),
                                        Pair("Peak (90+)", BrandGreenPrimary)
                                    )
                                    legendItems.forEach { (label, color) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "PAYOUT_HISTORY_DETAIL" -> {
                    var startDateStr by remember { mutableStateOf("2026-06-01") }
                    var endDateStr by remember { mutableStateOf("2026-06-30") }
                    var tempStartDate by remember { mutableStateOf("2026-06-01") }
                    var tempEndDate by remember { mutableStateOf("2026-06-30") }
                    var showCalendarRangePickerDialog by remember { mutableStateOf(false) }
                    var currentCalendarMonthIndex by remember { mutableStateOf(2) } // default to June 2026
                    var searchQuery by remember { mutableStateOf("") }
                    
                    fun formatReadableDateString(dateStr: String): String {
                        val parts = dateStr.split("-")
                        if (parts.size != 3) return dateStr
                        val monthName = when (parts[1]) {
                            "04" -> "Apr"
                            "05" -> "May"
                            "06" -> "Jun"
                            else -> parts[1]
                        }
                        val dayVal = parts[2].toIntOrNull() ?: parts[2]
                        return "$monthName $dayVal, ${parts[0]}"
                    }

                    // Calendar Range Selection Dialog
                    if (showCalendarRangePickerDialog) {
                        UnifiedCalendarDialog(
                            initialStartDate = tempStartDate.toCalendar(),
                            initialEndDate = tempEndDate.toCalendar(),
                            onConfirm = { start, end, label ->
                                startDateStr = start?.toDateString() ?: ""
                                endDateStr = end?.toDateString() ?: ""
                                tempStartDate = start?.toDateString() ?: ""
                                tempEndDate = end?.toDateString() ?: ""
                                showCalendarRangePickerDialog = false
                            },
                            onDismiss = { showCalendarRangePickerDialog = false },
                            title = "Select Date Range",
                            subtitle = "Filter payout transactions by date",
                            showPresets = true
                        )
                    }

                    // Header row with Back Button
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeDashboardSubView = "FINANCIALS" },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("payout_history_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Financial Analytics")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Payout History",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Searchable disbursements ledger of previous & current month payouts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .testTag("payout_search_input"),
                        placeholder = { Text("Search by ID, Date, or Buyer Name...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreenPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Interactive Calendar Picker Trigger Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable {
                                tempStartDate = startDateStr
                                tempEndDate = endDateStr
                                showCalendarRangePickerDialog = true
                            }
                            .testTag("payout_calendar_picker_trigger"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, BrandGreenPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(BrandGreenPrimary.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BrandGreenPrimary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Filter by Date Range",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${formatReadableDateString(startDateStr)} ➔ ${formatReadableDateString(endDateStr)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = {
                                    tempStartDate = startDateStr
                                    tempEndDate = endDateStr
                                    showCalendarRangePickerDialog = true
                                },
                                modifier = Modifier
                                    .background(BrandGreenPrimary.copy(alpha = 0.08f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Change range", tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Mock data items
                    val payoutItems = listOf(
                        PayoutTransactionHistoryItem("TXN-4920239", "2026-06-25", "2,450.00", "June 2026", "Abdirahman Haji", "Completed", "CBE"),
                        PayoutTransactionHistoryItem("TXN-8290138", "2026-06-18", "1,890.50", "June 2026", "Fartun Mohamed", "Completed", "Telebirr"),
                        PayoutTransactionHistoryItem("TXN-3810234", "2026-06-11", "3,120.00", "June 2026", "Cumar Cilmi", "Completed", "CBE"),
                        PayoutTransactionHistoryItem("TXN-5012391", "2026-06-04", "1,540.00", "June 2026", "Mustafe Yusuf", "Completed", "Telebirr"),
                        PayoutTransactionHistoryItem("TXN-7192031", "2026-05-28", "4,500.00", "May 2026", "Fatuma Mohamed", "Completed", "CBE"),
                        PayoutTransactionHistoryItem("TXN-6182938", "2026-05-21", "2,100.00", "May 2026", "Samatar Bashiir", "Completed", "Telebirr"),
                        PayoutTransactionHistoryItem("TXN-5120392", "2026-05-14", "3,850.00", "May 2026", "Hodan Aden", "Completed", "CBE"),
                        PayoutTransactionHistoryItem("TXN-4192038", "2026-05-07", "1,650.00", "May 2026", "Kaafi Mustafe", "Completed", "E-Birr"),
                        PayoutTransactionHistoryItem("TXN-3098234", "2026-04-28", "2,900.00", "April 2026", "Amina Farah", "Completed", "CBE"),
                        PayoutTransactionHistoryItem("TXN-2983412", "2026-04-15", "1,750.00", "April 2026", "Guled Farah", "Completed", "Telebirr")
                    )

                    // Filter list by selected date range & search query
                    val filteredPayouts = payoutItems.filter { item ->
                        val matchesDate = item.date >= startDateStr && item.date <= endDateStr
                        val matchesSearch = searchQuery.isEmpty() ||
                                item.id.contains(searchQuery, ignoreCase = true) ||
                                item.date.contains(searchQuery, ignoreCase = true) ||
                                item.buyerName.contains(searchQuery, ignoreCase = true) ||
                                item.channel.contains(searchQuery, ignoreCase = true)
                        matchesDate && matchesSearch
                    }

                    // Scrollable List of Payout History Ledger
                    if (filteredPayouts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No matching payouts found",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Refine your search term or select a different month tab.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredPayouts) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("payout_item_${item.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        if (item.channel == "CBE") BrandGreenPrimary.copy(alpha = 0.12f)
                                                        else BrandGoldSecondary.copy(alpha = 0.12f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ReceiptLong,
                                                    contentDescription = null,
                                                    tint = if (item.channel == "CBE") BrandGreenPrimary else BrandGoldSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.id,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "• ${item.channel}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (item.channel == "CBE") BrandGreenPrimary else BrandGoldSecondary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Buyer: ${item.buyerName}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Disbursed Date: ${item.date}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${item.amount} ETB",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = BrandGreenPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = BrandGreenPrimary.copy(alpha = 0.12f)),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = item.status.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = BrandGreenPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "MONTHLY_PROFITABILITY_DETAIL" -> {
                    val activeMonth = selectedPerformanceMonth ?: currentRealMonthName
                    
                    // Header row with Back Button (Fixed)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeDashboardSubView = "FINANCIALS" },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("profitability_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Financial Analytics")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Monthly Profitability Analytics",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "In-depth gross margins, logistics expenses, and payout transfers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Horizontal Month Tabs Row derived from sellerMonthlyStatsList
                    val availableStatMonths = sellerMonthlyStatsList.map { stat ->
                        if (stat.year.isNotBlank() && !stat.monthName.contains(stat.year)) {
                            "${stat.monthName} ${stat.year}"
                        } else {
                            stat.monthName
                        }
                    }.distinct()

                    val monthsTabs = if (availableStatMonths.isNotEmpty()) availableStatMonths else getRealCalendarMonthsList()
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(monthsTabs) { month ->
                            val isSelected = activeMonth == month
                            Card(
                                modifier = Modifier
                                    .clickable { selectedPerformanceMonth = month }
                                    .testTag("profit_month_tab_${month.substringBefore(" ")}"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Scrollable Profitability Content (Only this month's stats)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Dynamically scale numbers based on selected month for hyper-realistic simulation
                        val parts = activeMonth.split(" ")
                        val mName = parts.getOrNull(0) ?: ""
                        val mYear = parts.getOrNull(1) ?: ""
                        val matchingMonthlyStat = sellerMonthlyStatsList.firstOrNull { stat ->
                            (stat.monthName.equals(mName, ignoreCase = true) || stat.month.equals(mName, ignoreCase = true) || stat.month.endsWith(mName, ignoreCase = true)) &&
                            (stat.year.isBlank() || stat.year == mYear)
                        }

                        val grossSalesVal = (matchingMonthlyStat?.gross_sales ?: 0).toDouble()
                        val soldCount = matchingMonthlyStat?.sold_stocks ?: 0
                        val unsoldRemaining = matchingMonthlyStat?.unsold_stocks ?: 0
                        
                        val rawProductCost = (matchingMonthlyStat?.product_cost ?: 0).toDouble()
                        val deliveryLogisticsCost = (matchingMonthlyStat?.logistics_cost ?: 0).toDouble()
                        val netProfitVal = (matchingMonthlyStat?.net_profit ?: 0).toDouble()
                        
                        // Card 1: Gross Margins and Monthly Info Overview
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "$activeMonth Store Performance Ledger",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Complete monthly sales volume, product costs & margins",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedPayoutMonth = activeMonth
                                            activeDashboardSubView = "MONTHLY_REPORT_PREVIEW"
                                        },
                                        modifier = Modifier
                                            .background(BrandGreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .size(40.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Report", tint = BrandGreenPrimary)
                                    }
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                
                                // 2x2 Grid for metrics
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Selling Product Value (Revenue)
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Selling Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(String.format("%,.2f ETB", grossSalesVal), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        
                                        // Cost of Goods Sold
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Product Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(String.format("%,.2f ETB", rawProductCost), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = BrandGoldSecondary)
                                            }
                                        }
                                    }
                                    
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Stocks Sold (Selles)
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Stocks Sold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("$soldCount Units", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                            }
                                        }
                                        
                                        // Stocks Unsold (Remaining)
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Inventory, contentDescription = null, tint = BrandRedTertiary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Unsold Stocks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("$unsoldRemaining Units", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Cancelled Orders Metric Card
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = BrandRedTertiary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Cancelled Orders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val cancelledCountMonth = matchingMonthlyStat?.cancelled_orders ?: 0
                                                Text("$cancelledCountMonth Orders", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                            }
                                        }

                                        // Estimated Cancellation Loss Card
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Est. Refund Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val cancellationLoss = matchingMonthlyStat?.refund_loss?.toDouble() ?: 0.0
                                                Text(String.format("%,.2f ETB", cancellationLoss), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BrandGreenPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .border(1.dp, BrandGreenPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Net Shop Profit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(String.format("%,.2f ETB", netProfitVal), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    val marginPct = (netProfitVal / grossSalesVal) * 100
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = BrandGreenPrimary),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = String.format("%.1f%% Margin", marginPct),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        selectedPayoutMonth = activeMonth
                                        activeDashboardSubView = "MONTHLY_REPORT_PREVIEW"
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View & Download PDF Report", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                        
                        // Card 2: Cost Breakdown Progress Bars (Platform Fee & commission removed!)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Monthly Operating Expenses Breakdown",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val breakdownItems = listOf(
                                    Triple("Wholesale Product Sourcing", 0.32f, rawProductCost),
                                    Triple("Local Logistics & Delivery Courier", 0.08f, deliveryLogisticsCost)
                                )
                                
                                breakdownItems.forEach { (name, ratio, value) ->
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                            Text(text = String.format("%,.2f ETB", value), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { ratio * 2f }, // Scale for better visual display
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = if (ratio > 0.25f) BrandGoldSecondary else BrandRedTertiary,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Card 3: Payout Account Allocation
                        val telebirrPayout = if (matchingMonthlyStat != null && matchingMonthlyStat.telebirr_balance > 0) matchingMonthlyStat.telebirr_balance.toDouble() else netProfitVal * 0.50
                        val cbePayout = if (matchingMonthlyStat != null && matchingMonthlyStat.cbe_balance > 0) matchingMonthlyStat.cbe_balance.toDouble() else netProfitVal * 0.30
                        val ebirrPayout = if (matchingMonthlyStat != null && matchingMonthlyStat.ebirr_balance > 0) matchingMonthlyStat.ebirr_balance.toDouble() else netProfitVal * 0.20
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "💰 Merchant Payout Accounts Allocation",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap on any account to view its detailed ledger and disbursement history for $activeMonth.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BrandGreenPrimary.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedPayoutChannel = "Telebirr"
                                            selectedPayoutMonth = activeMonth
                                            activeDashboardSubView = "PAYOUT_TRANSACTIONS_DETAIL"
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Telebirr Instant Wallet", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Merchant ID: $merchantTelebirrMerchantId", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(String.format("%,.2f ETB", telebirrPayout), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = BrandGreenPrimary)
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BrandGreenPrimary.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedPayoutChannel = "CBE"
                                            selectedPayoutMonth = activeMonth
                                            activeDashboardSubView = "PAYOUT_TRANSACTIONS_DETAIL"
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Commercial Bank of Ethiopia", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Account Transfer (Bole Branch)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(String.format("%,.2f ETB", cbePayout), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF57F17).copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedPayoutChannel = "Ebirr"
                                            selectedPayoutMonth = activeMonth
                                            activeDashboardSubView = "PAYOUT_TRANSACTIONS_DETAIL"
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Ebirr Mobile Wallet", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Ebirr Id: $merchantEbirrId", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(String.format("%,.2f ETB", ebirrPayout), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFFF57F17))
                                }
                            }
                        }

                        // Card 4: Best-Selling Categories & Items
                        val topCategoriesData = matchingMonthlyStat?.top_categories_data ?: ""
                        val categorySubcategoryData = matchingMonthlyStat?.category_subcategory_data ?: ""

                        // Helper function to parse subcategories for a given category
                        fun parseSubcategoriesForCategory(data: String, category: String): List<Pair<String, Int>> {
                            // data format: "Toys:Dolls:10,Action Figures:5|Electronics:Mobile Phone:20,Tablets:15"
                            return data.split("|")
                                .find { it.startsWith("$category:") }
                                ?.removePrefix("$category:")
                                ?.split(",")
                                ?.mapNotNull { subPart ->
                                    val subParts = subPart.split(":")
                                    if (subParts.size == 2) subParts[0] to (subParts[1].toIntOrNull() ?: 0) else null
                                } ?: emptyList()
                        }

                        // ✅ Parse top categories: "Electronics:150|Clothing:200|..."
                        val categorySales = topCategoriesData.split("|")
                            .filter { it.isNotBlank() }
                            .mapNotNull { part ->
                                val parts = part.split(":")
                                if (parts.size == 2) {
                                    val name = parts[0]
                                    val units = parts[1].toIntOrNull() ?: 0
                                    // ✅ Parse subcategories for this category
                                    val subItems = parseSubcategoriesForCategory(categorySubcategoryData, name)
                                    CategorySaleInfo(name, units, subItems)
                                } else null
                            }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "📦 Best-Selling Categories & Items",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Volume of products sold sorted by merchandise department for $activeMonth.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (categorySales.isEmpty()) {
                                    Text(
                                        text = "No category data available.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    categorySales.forEach { category ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Category Header: Icon + Name + Total Units
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = when (category.name.lowercase()) {
                                                            "electronics" -> Icons.Default.Devices
                                                            "clothing" -> Icons.Default.Checkroom
                                                            "food" -> Icons.Default.Restaurant
                                                            "books" -> Icons.Default.Book
                                                            "beauty" -> Icons.Default.Palette
                                                            "toys" -> Icons.Default.SportsEsports
                                                            "sport" -> Icons.Default.SportsSoccer
                                                            "home" -> Icons.Default.Home
                                                            else -> Icons.Default.Category
                                                        },
                                                        contentDescription = null,
                                                        tint = BrandGreenPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = category.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = BrandGreenPrimary.copy(alpha = 0.12f)),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        text = "${category.soldCount} Units",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandGreenPrimary),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            // Show subcategories
                                            if (category.items.isNotEmpty()) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                                category.items.forEach { (itemName, count) ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "• $itemName",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "$count sold",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = "No subcategories available",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.padding(start = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "MONTHLY_REPORT_PREVIEW" -> {
                    val activeMonth = selectedPayoutMonth.ifBlank { selectedPerformanceMonth ?: currentRealMonthName }
                    val parts = activeMonth.split(" ")
                    val mName = parts.getOrNull(0) ?: ""
                    val mYear = parts.getOrNull(1) ?: ""
                    val matchingMonthlyStat = sellerMonthlyStatsList.firstOrNull { stat ->
                        (stat.monthName.equals(mName, ignoreCase = true) || stat.month.equals(mName, ignoreCase = true) || stat.month.endsWith(mName, ignoreCase = true)) &&
                        (stat.year.isBlank() || mYear.isBlank() || stat.year == mYear)
                    }

                    val stats = getStatsForMonth(activeMonth)
                    val grossSalesVal = matchingMonthlyStat?.gross_sales?.toDouble() ?: stats.grossSales
                    val soldCount = matchingMonthlyStat?.sold_stocks ?: stats.soldCount
                    val unsoldRemaining = matchingMonthlyStat?.unsold_stocks ?: stats.unsoldRemaining
                    val cancelledCount = matchingMonthlyStat?.cancelled_orders ?: stats.cancelledCount
                    val refundLoss = matchingMonthlyStat?.refund_loss?.toDouble() ?: stats.refundLoss
                    val rawProductCost = matchingMonthlyStat?.product_cost?.toDouble() ?: stats.productCost
                    val deliveryLogisticsCost = matchingMonthlyStat?.logistics_cost?.toDouble() ?: stats.logisticsCost
                    val netProfitVal = matchingMonthlyStat?.net_profit?.toDouble() ?: stats.netProfit
                    val uniqueCustomersCount = matchingMonthlyStat?.unique_customers ?: 18
                    val telebirrPayout = if (matchingMonthlyStat != null && matchingMonthlyStat.telebirr_balance > 0) matchingMonthlyStat.telebirr_balance.toDouble() else stats.telebirrPayout
                    val cbePayout = if (matchingMonthlyStat != null && matchingMonthlyStat.cbe_balance > 0) matchingMonthlyStat.cbe_balance.toDouble() else stats.cbePayout
                    val ebirrPayout = matchingMonthlyStat?.ebirr_balance?.toDouble() ?: 0.0

                    val topCategoriesData = matchingMonthlyStat?.top_categories_data ?: ""
                    val categorySubcategoryData = matchingMonthlyStat?.category_subcategory_data ?: ""

                    fun parseSubcategoriesForCategory(data: String, category: String): List<Pair<String, Int>> {
                        return data.split("|")
                            .find { it.startsWith("$category:") }
                            ?.removePrefix("$category:")
                            ?.split(",")
                            ?.mapNotNull { subPart ->
                                val subParts = subPart.split(":")
                                if (subParts.size == 2) subParts[0] to (subParts[1].toIntOrNull() ?: 0) else null
                            } ?: emptyList()
                    }

                    val categorySales = topCategoriesData.split("|")
                        .filter { it.isNotBlank() }
                        .mapNotNull { part ->
                            val parts = part.split(":")
                            if (parts.size == 2) {
                                val name = parts[0]
                                val units = parts[1].toIntOrNull() ?: 0
                                val subItems = parseSubcategoriesForCategory(categorySubcategoryData, name)
                                CategorySaleInfo(name, units, subItems)
                            } else null
                        }

                    val context = LocalContext.current

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { activeDashboardSubView = "MONTHLY_PROFITABILITY_DETAIL" },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Monthly Printable Statement",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Full document layout ready for storage and bookkeeping",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Preview Paper Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Header of the sheet
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "ESUUQ STORE SYSTEM",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = BrandGreenPrimary
                                        )
                                        Text(
                                            text = "Official Monthly Statement",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = BrandGreenPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Information details
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Store Identity:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(merchantStoreName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("TIN: $merchantTinNumber", style = MaterialTheme.typography.bodySmall)
                                        Text("License: $merchantTradeLicense", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Statement Period:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(activeMonth, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                        val currentFormattedDate = remember {
                                             try {
                                                 java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US).format(java.util.Date())
                                             } catch (e: Exception) {
                                                 "June 30, 2026"
                                             }
                                         }
                                         Text("Issued: $currentFormattedDate", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                Text(
                                    text = "Ledger Audit Trails",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Gross Sales:", style = MaterialTheme.typography.bodyMedium)
                                        Text(String.format("%,.2f ETB", grossSalesVal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cost of Product:", style = MaterialTheme.typography.bodyMedium)
                                        Text(String.format("-%,.2f ETB", rawProductCost), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Delivery Logistics Expenses:", style = MaterialTheme.typography.bodyMedium)
                                        Text(String.format("-%,.2f ETB", deliveryLogisticsCost), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Net Shop Profit Margin:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(String.format("%,.2f ETB", netProfitVal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                    }
                                }

                                Text(
                                    text = "Physical Inventory Volume Logs",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Sold Items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$soldCount units", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Unsold Stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$unsoldRemaining units", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Unique Clients", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$uniqueCustomersCount buyers", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                Text(
                                    text = "Order Cancellations & Refund Losses",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Cancelled Orders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$cancelledCount orders", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Est. Refund Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(String.format("%,.2f ETB", refundLoss), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                                        }
                                    }
                                }

                                Text(
                                    text = "Bank Disbursements Allocation",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Telebirr Merchant Payout:", style = MaterialTheme.typography.bodySmall)
                                        Text(String.format("%,.2f ETB", telebirrPayout), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("CBE Bank Transfer:", style = MaterialTheme.typography.bodySmall)
                                        Text(String.format("%,.2f ETB", cbePayout), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Ebirr Wallet Allocation:", style = MaterialTheme.typography.bodySmall)
                                        Text(String.format("%,.2f ETB", ebirrPayout), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandGoldSecondary)
                                    }
                                }

                                Text(
                                    text = "Best-Selling Categories & Subcategories",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (categorySales.isNotEmpty()) {
                                        categorySales.forEach { category ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(category.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                Text("${category.soldCount} units", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                            }
                                            if (category.items.isNotEmpty()) {
                                                category.items.forEach { (subName, units) ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("• $subName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("$units units", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val fallbackSales = getMonthlyCategorySales(activeMonth)
                                        fallbackSales.forEach { category ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("${category.name}:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                Text("${category.soldCount} units", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "This is a digital copy of your official store record. Tap download below to save this ledger as an industry-standard PDF document.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Action Bar at bottom
                        Button(
                            onClick = {
                                val finalCategorySales = if (categorySales.isNotEmpty()) {
                                    categorySales
                                } else {
                                    getMonthlyCategorySales(activeMonth).map { soldCat ->
                                        CategorySaleInfo(soldCat.name, soldCat.soldCount, soldCat.items)
                                    }
                                }
                                generateMonthlyReportPdf(
                                    context = context,
                                    month = activeMonth,
                                    revenue = grossSalesVal,
                                    netProfit = netProfitVal,
                                    productCost = rawProductCost,
                                    logisticsCost = deliveryLogisticsCost,
                                    soldCount = soldCount,
                                    unsoldCount = unsoldRemaining,
                                    cancelledCount = cancelledCount,
                                    refundLoss = refundLoss,
                                    telebirrPayout = telebirrPayout,
                                    cbePayout = cbePayout,
                                    ebirrPayout = ebirrPayout,
                                    storeName = merchantStoreName,
                                    tin = merchantTinNumber,
                                    license = merchantTradeLicense,
                                    categorySales = finalCategorySales,
                                    uniqueCustomersCount = uniqueCustomersCount
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Statement PDF", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                }
                "PAYOUT_TRANSACTIONS_DETAIL" -> {
                    val activeMonth = selectedPayoutMonth
                    val netProfitVal = when (activeMonth) {
                        "January 2026" -> 32400.0 * 0.60
                        "February 2026" -> 41250.0 * 0.60
                        "March 2026" -> 44800.0 * 0.60
                        "April 2026" -> 36150.0 * 0.60
                        "May 2026" -> 49800.0 * 0.60
                        "June 2026" -> 52000.0 * 0.60
                        else -> 45000.0 * 0.60
                    }
                    val totalPayout = when (selectedPayoutChannel) {
                        "CBE" -> netProfitVal * 0.30
                        "Ebirr" -> netProfitVal * 0.20
                        else -> netProfitVal * 0.50
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { activeDashboardSubView = "MONTHLY_PROFITABILITY_DETAIL" },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "$selectedPayoutChannel Disbursements History",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Complete transactional audits for $activeMonth",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Account summary card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (selectedPayoutChannel) {
                                    "CBE" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    "Ebirr" -> Color(0xFFF57F17).copy(alpha = 0.08f)
                                    else -> BrandGreenPrimary.copy(alpha = 0.08f)
                                }
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Month Disbursed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(String.format("%,.2f ETB", totalPayout), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                when (selectedPayoutChannel) {
                                                    "CBE" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    "Ebirr" -> Color(0xFFF57F17).copy(alpha = 0.2f)
                                                    else -> BrandGreenPrimary.copy(alpha = 0.2f)
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (selectedPayoutChannel) {
                                                "CBE" -> Icons.Default.AccountBalance
                                                "Ebirr" -> Icons.Default.Wallet
                                                else -> Icons.Default.Wallet
                                            },
                                            contentDescription = null,
                                            tint = when (selectedPayoutChannel) {
                                                "CBE" -> MaterialTheme.colorScheme.primary
                                                "Ebirr" -> Color(0xFFF57F17)
                                                else -> BrandGreenPrimary
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Financial Account Instance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = when (selectedPayoutChannel) {
                                                "CBE" -> "Commercial Bank of Ethiopia"
                                                "Ebirr" -> "Ebirr Mobile Wallet"
                                                else -> "Telebirr Instant Wallet"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Target Destination", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = when (selectedPayoutChannel) {
                                                "CBE" -> merchantCbeAccount
                                                "Ebirr" -> merchantEbirrId
                                                else -> merchantTelebirrMerchantId
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Itemized Settlement Transactions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Lazy column of transactions for the month
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            data class DisbursedTx(
                                val name: String,
                                val amount: Double,
                                val txId: String,
                                val status: String,
                                val timestamp: String
                            )

                            val transactionsList = if (selectedPayoutChannel == "CBE") {
                                listOf(
                                    DisbursedTx("Hodan Aden", totalPayout * 0.25, "CBE-TXN-${activeMonth.substringBefore(" ").uppercase()}-84910", "Settled & Completed", "05th of Month - 10:14 AM"),
                                    DisbursedTx("Mustafe Yusuf", totalPayout * 0.30, "CBE-TXN-${activeMonth.substringBefore(" ").uppercase()}-93019", "Settled & Completed", "12th of Month - 03:45 PM"),
                                    DisbursedTx("Cumar Cilmi", totalPayout * 0.20, "CBE-TXN-${activeMonth.substringBefore(" ").uppercase()}-22849", "Settled & Completed", "18th of Month - 11:20 AM"),
                                    DisbursedTx("Guled Farah", totalPayout * 0.25, "CBE-TXN-${activeMonth.substringBefore(" ").uppercase()}-57211", "Settled & Completed", "25th of Month - 09:15 AM"),
                                    DisbursedTx("Fartun Mohamed", 0.0, "CBE-TXN-${activeMonth.substringBefore(" ").uppercase()}-10420", "Cancelled Order", "28th of Month - 02:30 PM")
                                )
                            } else {
                                listOf(
                                    DisbursedTx("Amina Farah", totalPayout * 0.25, "TB-TXN-${activeMonth.substringBefore(" ").uppercase()}-41029", "Settled & Completed", "03rd of Month - 08:12 AM"),
                                    DisbursedTx("Guled Farah", totalPayout * 0.35, "TB-TXN-${activeMonth.substringBefore(" ").uppercase()}-88392", "Settled & Completed", "08th of Month - 01:14 PM"),
                                    DisbursedTx("Mustafe Yusuf", totalPayout * 0.15, "TB-TXN-${activeMonth.substringBefore(" ").uppercase()}-10492", "Settled & Completed", "14th of Month - 04:50 PM"),
                                    DisbursedTx("Hodan Aden", totalPayout * 0.25, "TB-TXN-${activeMonth.substringBefore(" ").uppercase()}-30129", "Settled & Completed", "22nd of Month - 06:18 PM"),
                                    DisbursedTx("Cumar Cilmi", 0.0, "TB-TXN-${activeMonth.substringBefore(" ").uppercase()}-99421", "Cancelled Order", "27th of Month - 11:45 AM")
                                )
                            }

                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(transactionsList) { txn ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = txn.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "ID: ${txn.txId}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(txn.txId))
                                                            android.widget.Toast.makeText(context, "Copied ID: ${txn.txId}", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(16.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "Copy Transaction ID",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Status: ${txn.status}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (txn.status == "Cancelled Order") BrandRedTertiary else BrandGreenPrimary
                                                )
                                                Text(
                                                    text = txn.timestamp,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = if (txn.status == "Cancelled Order") "0.00 ETB" else String.format("+%,.2f ETB", txn.amount),
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        textDecoration = if (txn.status == "Cancelled Order") androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                    ),
                                                    color = if (txn.status == "Cancelled Order") MaterialTheme.colorScheme.outline else BrandGreenPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "ALL_ORDERS" -> {
                    // Header row with Back Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeDashboardSubView = "MAIN" },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .testTag("all_orders_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Dashboard")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "All Customer Orders",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Detailed view of all order transactions and fulfillment status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Search Bar and  Date range filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = orderSearchQuery,
                            onValueChange = { orderSearchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("order_search_input"),
                            placeholder = { Text("Search phone, orderID, name", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                            trailingIcon = {
                                if (orderSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { orderSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        IconButton(
                            onClick = { showSellerDateDialog = true },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .testTag("seller_calendar_picker_btn"),
                        ) {
                           Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick Date Range",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }


                    if (sellerStartDate != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val rangeText = if (sellerEndDate != null) {
                                    "${formatCalendarDate(sellerStartDate!!)} - ${formatCalendarDate(sellerEndDate!!)}"
                                } else {
                                    "Starting from ${formatCalendarDate(sellerStartDate!!)}"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Range: $rangeText",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        sellerStartDate = null
                                        sellerEndDate = null
                                    },
                                    modifier = Modifier.size(20.dp).testTag("clear_seller_calendar_range")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear calendar filter", tint = BrandRedTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    

                    // Tabs (Active, Completed, Cancelled)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            "ACTIVE" to "Active (${orders.count { it.status in listOf("PENDING", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY") }})",
                            "COMPLETED" to "Completed (${orders.count { it.status == "SHIPPED" || it.status == "DELIVERED" }})",
                            "CANCELLED" to "Cancelled (${orders.count { it.status == "CANCELLED" }})"
                        )
                        tabs.forEach { (tabId, label) ->
                            val isSelected = orderTabFilter == tabId
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { orderTabFilter = tabId }
                                    .testTag("all_orders_tab_$tabId"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Render all matching orders in a scrollable list (stationary app bar/controls)
                    val now = System.currentTimeMillis()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val filteredOrders = orders.filter { order ->
                            val matchStatus = when (orderTabFilter) {
                                "ACTIVE" -> order.status in listOf("PENDING", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY")
                                "COMPLETED" -> order.status == "SHIPPED" || order.status == "DELIVERED"
                                "CANCELLED" -> order.status == "CANCELLED"
                                else -> true
                            }
                            
                            val matchSearch = if (orderSearchQuery.isBlank()) true else {
                                order.id.toString().contains(orderSearchQuery) ||
                                order.buyerName.contains(orderSearchQuery, ignoreCase = true) ||
                                order.buyerPhone.contains(orderSearchQuery, ignoreCase = true)
                            }
    
                            val matchDate = if (sellerStartDate != null && sellerEndDate != null) {
                                val startMs = clearCalendarTime(sellerStartDate!!).timeInMillis
                                val endCal = java.util.Calendar.getInstance().apply {
                                    timeInMillis = sellerEndDate!!.timeInMillis
                                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                                    set(java.util.Calendar.MINUTE, 59)
                                    set(java.util.Calendar.SECOND, 59)
                                    set(java.util.Calendar.MILLISECOND, 999)
                                }
                                val endMs = endCal.timeInMillis
                                0L in startMs..endMs
                            } else if (sellerStartDate != null) {
                                val startMs = clearCalendarTime(sellerStartDate!!).timeInMillis
                                0L >= startMs
                            } else {
                                true
                            }
    
                            matchStatus && matchSearch && matchDate
                        }.let { list ->
                            list.sortedByDescending { 0L }
                        }
    
                        if (filteredOrders.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ListAlt, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No orders match the selected search, status, or date range filters.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        } else {
                            filteredOrders.forEach { order ->
                                OrderItemView(order)
                            }
                        }
                    }
                }
                "ORDER_DETAIL" -> {
                    sellerSelectedOrderDetail?.let { orderSnapshot ->
                        // Dynamically resolve the latest order state from the database/ViewModel flow
                        val order = orders.find { it.id == orderSnapshot.id } ?: orderSnapshot

                        val context = androidx.compose.ui.platform.LocalContext.current
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        val isDelivery = !order.isSelfPickup
                        val refCode = if (order.paymentMethod.equals("Telebirr", ignoreCase = true)) {
                            "MP-8290${order.id}"
                        } else {
                            "TXN-4920${order.id}"
                        }

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Header with Back Button (STATIONARY / NO SCROLL)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { activeDashboardSubView = previousDashboardSubView },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .testTag("order_detail_back_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Order Details",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Fulfillment & Transaction details",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Scrollable Content
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Cancel Reason Banner
                                if (order.status == "CANCELLED") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Cancel,
                                                    contentDescription = "Cancelled",
                                                    tint = BrandRedTertiary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Order Cancelled",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = BrandRedTertiary
                                                )
                                            }
                                            Text(
                                                text = "Cancellation Reason: ${order.cancellationReason ?: "No specified reason."}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            if (!"".isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Attached Refund Receipt: ${""}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = BrandGreenPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Card with order details
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Status Badge Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Order Status:",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = when (order.status) {
                                                        "PREPARING" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                        "READY_FOR_PICKUP" -> BrandGoldSecondary.copy(alpha = 0.15f)
                                                        "SHIPPED" -> BrandGreenPrimary.copy(alpha = 0.15f)
                                                        "DELIVERED" -> BrandGreenPrimary.copy(alpha = 0.2f)
                                                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                                    }
                                                )
                                            ) {
                                                Text(
                                                    text = if (order.status == "SHIPPED") "COMPLETE" else order.status,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = when (order.status) {
                                                        "PREPARING" -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        "READY_FOR_PICKUP" -> BrandGoldSecondary
                                                        "SHIPPED", "DELIVERED" -> BrandGreenPrimary
                                                        else -> BrandRedTertiary
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))

                                        // Explicit Order ID Row
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Fingerprint, contentDescription = "Order ID", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Order ID",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "#${order.id}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // Date & Time Row
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccessTime, contentDescription = "Order Date/Time", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Placed On",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = formatPurchaseDate(0L),
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                        }

                                        // Reference with copy button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.Receipt, contentDescription = "Reference", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Payment Ref (${order.paymentMethod})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = refCode,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = BrandGreenPrimary
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(refCode))
                                                    android.widget.Toast.makeText(context, "Copied Reference: $refCode", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("copy_ref_btn_${order.id}")
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy reference", tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))

                                        // Buyer Customer Name
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = "Buyer", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Buyer Customer",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = order.buyerName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                        }

                                        // Buyer Phone (WITH CALL INITIATION)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.Phone, contentDescription = "Phone", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Phone Number",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = order.buyerPhone,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                    )
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                        data = android.net.Uri.parse("tel:${order.buyerName}")
                                                    }
                                                    try {
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Cannot initiate call", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.testTag("call_buyer_btn_${order.id}")
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Call", fontSize = 12.sp)
                                            }
                                        }

                                        // Destination Address (ONLY SHOW WHEN IS DELIVERY)
                                        if (isDelivery) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocationOn, contentDescription = "Destination", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Destination Address",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = order.shippingAddress?.let { "Lat: ${it.lat}, Lon: ${it.lon}" } ?: "Addis Ababa, Ethiopia",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Fulfillment Method
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = "Fulfillment", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Fulfillment Type",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                val courierDetailText = if (order.isSelfPickup) {
                                                    "Self Pickup (Buyer Pickup)"
                                                } else {
                                                    if (!order.courierName.isNullOrBlank()) {
                                                        "Courier Delivery - ${order.courierName} (Plate: ${order.courierPlate ?: "N/A"})"
                                                    } else {
                                                        "Delivery awaiting"
                                                    }
                                                }
                                                Text(
                                                    text = courierDetailText,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))

                                        // Products Ordered
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ShoppingCart, contentDescription = "Items", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Products Ordered",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val itemsList = order.items
                                            if (itemsList.isNotEmpty()) {
                                                itemsList.forEach { item ->
                                                    val subtotalVal = if (item.subtotal > 0.0) item.subtotal else (item.unitPrice * item.quantity)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(text = item.productName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                            Text(
                                                                text = "Qty: ${item.quantity}  •  Unit Price: ${item.unitPrice} ETB",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Text(
                                                            text = "Subtotal: ${String.format("%.1f", subtotalVal)} ETB",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = BrandGreenPrimary
                                                        )
                                                    }
                                                }
                                            } else {
                                                val names = order.productNames.split(",")
                                                names.forEach { rawName ->
                                                    val cleanName = rawName.trim()
                                                    if (cleanName.isNotEmpty()) {
                                                        val itemPrice = if (names.size == 1) order.totalPrice else (order.totalPrice / names.size)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(text = cleanName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                                Text(
                                                                    text = "Qty: 1  •  Unit Price: ${String.format("%.1f", itemPrice)} ETB",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            Text(
                                                                text = "Subtotal: ${String.format("%.1f", itemPrice)} ETB",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = BrandGreenPrimary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Price
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.MonetizationOn, contentDescription = "Total", tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Total Price",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${order.totalPrice} ETB",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = BrandGreenPrimary
                                                )
                                            }
                                        }

                                        // Delivery Price & Responsibility - ONLY show if deliveryPickup is true / NOT self pickup
                                        if (!order.isSelfPickup) {
                                            val (deliveryPrice, isFreeDelivery) = getOrderDeliveryDetails(order)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocalShipping, contentDescription = "Delivery Fee", tint = if (isFreeDelivery) BrandRedTertiary else BrandGreenPrimary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Delivery Price & Responsibility",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "$deliveryPrice ETB",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Text(
                                                        text = if (isFreeDelivery) "Paid by Seller (Product has Free Delivery) 🚛" else "Paid by Buyer 🚚",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isFreeDelivery) BrandRedTertiary else BrandGreenPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Interactive Status Update Buttons
                                if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "Manage Order Actions:",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )

                                            // Display Payment Receipt Image in Manage Order Actions
                                            val rawReceipt = order.paymentReceipt
                                            val receiptUrl = if (!rawReceipt.isNullOrBlank()) {
                                                if (rawReceipt.startsWith("http")) rawReceipt else "https://metube.pockethost.io/api/files/orders/${order.id}/$rawReceipt"
                                            } else null

                                            if (!receiptUrl.isNullOrBlank()) {
                                                var showFullReceipt by remember { mutableStateOf(false) }
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(
                                                        text = "Payment Receipt Image (Tap to expand):",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp)
                                                            .clickable { showFullReceipt = true }
                                                    ) {
                                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                            AsyncImage(
                                                                model = receiptUrl,
                                                                contentDescription = "Payment Receipt Image",
                                                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                                            )
                                                            Surface(
                                                                color = Color.Black.copy(alpha = 0.6f),
                                                                shape = RoundedCornerShape(12.dp),
                                                                modifier = Modifier
                                                                    .align(Alignment.BottomCenter)
                                                                    .padding(8.dp)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.ZoomIn,
                                                                        contentDescription = null,
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                    Text(
                                                                        text = "Tap to view full receipt",
                                                                        color = Color.White,
                                                                        style = MaterialTheme.typography.labelSmall
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                if (showFullReceipt) {
                                                    androidx.compose.ui.window.Dialog(
                                                        onDismissRequest = { showFullReceipt = false },
                                                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color.Black)
                                                                .clickable { showFullReceipt = false }
                                                        ) {
                                                            AsyncImage(
                                                                model = receiptUrl,
                                                                contentDescription = "Full Payment Receipt",
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .padding(16.dp),
                                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                                            )
                                                            IconButton(
                                                                onClick = { showFullReceipt = false },
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(24.dp)
                                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Close",
                                                                    tint = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                when (order.status) {
                                                    "PENDING" -> {
                                                        Button(
                                                            onClick = {
                                                                orderToConfirmPayment = order
                                                                showPaymentConfirmDialog = true
                                                            },
                                                            modifier = Modifier.weight(1f).testTag("page_confirm_btn_${order.id}"),
                                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Confirm Order", fontSize = 12.sp)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                selectedOrderToCancel = order
                                                                cancelReasonInput = ""
                                                                showCancelDialog = true
                                                            },
                                                            modifier = Modifier.weight(1f).testTag("page_cancel_btn_${order.id}"),
                                                            border = BorderStroke(1.dp, BrandRedTertiary),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRedTertiary)
                                                        ) {
                                                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Cancel Order", fontSize = 12.sp)
                                                        }
                                                    }
                                                    "CONFIRMED" -> {
                                                        Button(
                                                            onClick = { viewModel.updateOrderStatus(order, "PREPARING") },
                                                            modifier = Modifier.weight(1f).testTag("page_start_prep_btn_${order.id}"),
                                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Start Preparing", fontSize = 12.sp)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                selectedOrderToCancel = order
                                                                cancelReasonInput = ""
                                                                showCancelDialog = true
                                                            },
                                                            modifier = Modifier.weight(1f).testTag("page_cancel_btn_${order.id}"),
                                                            border = BorderStroke(1.dp, BrandRedTertiary),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRedTertiary)
                                                        ) {
                                                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Cancel Order", fontSize = 12.sp)
                                                        }
                                                    }
                                                    "PREPARING" -> {
                                                        Button(
                                                            onClick = {
                                                                orderToMarkReady = order
                                                                showReadyConfirmDialog = true
                                                            },
                                                            modifier = Modifier.weight(1f).testTag("page_ready_btn_${order.id}"),
                                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGoldSecondary)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Ready for Pickup", fontSize = 12.sp)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                selectedOrderToCancel = order
                                                                cancelReasonInput = ""
                                                                showCancelDialog = true
                                                            },
                                                            modifier = Modifier.weight(1f).testTag("page_cancel_btn_${order.id}"),
                                                            border = BorderStroke(1.dp, BrandRedTertiary),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRedTertiary)
                                                        ) {
                                                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Cancel Order", fontSize = 12.sp)
                                                        }
                                                    }
                                                    "READY_FOR_PICKUP" -> {
                                                        if (isDelivery) {
                                                            if (!order.courierName.isNullOrBlank()) {
                                                                Button(
                                                                    onClick = {
                                                                        orderForCourierVerification = order
                                                                        courierVerificationCodeInput = ""
                                                                        showCourierVerificationDialog = true
                                                                    },
                                                                    modifier = Modifier.fillMaxWidth().testTag("page_handover_btn_${order.id}"),
                                                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                                                ) {
                                                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text("Verify Courier & Handover")
                                                                }
                                                            } else {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .background(BrandGoldSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                                        .border(1.dp, BrandGoldSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                                        .padding(12.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Row(
                                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(16.dp))
                                                                        Text(
                                                                            text = "Delivery awaiting...",
                                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = BrandGoldSecondary)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = {
                                                                    orderForBuyerVerification = order
                                                                    buyerVerificationCodeInput = ""
                                                                    showBuyerVerificationDialog = true
                                                                },
                                                                modifier = Modifier.fillMaxWidth().testTag("page_handover_buyer_btn_${order.id}"),
                                                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                                            ) {
                                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text("Verify & Handover to Buyer")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "CUSTOMER_FEEDBACK" -> {
                    CustomerFeedbackScreen(
                        onBack = { activeDashboardSubView = "MAIN" },
                        reviews = reviewsListState,
                        onReviewsUpdate = { updatedList ->
                            val originalList = reviewsListState
                            updatedList.forEach { updatedItem ->
                                val origItem = originalList.find { it.id == updatedItem.id || (it.id == null && it.name == updatedItem.name) }
                                if (origItem != null && origItem.replyText != updatedItem.replyText) {
                                    val reviewId = updatedItem.id ?: ""
                                    val replyText = updatedItem.replyText ?: ""
                                    viewModel.replyToStoreReview(reviewId, replyText)
                                }
                            }
                        }
                    )
                }
                "MERCHANT_SETTINGS" -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var tempPhone by remember { mutableStateOf(merchantPhone) }
                    var tempPaymentModel by remember { mutableStateOf(merchantPaymentModel) }
                    var tempCbeAccount by remember { mutableStateOf(merchantCbeAccount) }
                    var tempTelebirrId by remember { mutableStateOf(merchantTelebirrMerchantId) }
                    var tempStoreName by remember { mutableStateOf(merchantStoreName) }
                    var tempProfileImageUri by remember(merchantProfileImageUri) { mutableStateOf(merchantProfileImageUri) }
                    var tempProfileCoverUri by remember(merchantProfileCoverUri) { mutableStateOf(merchantProfileCoverUri) }
                    var tempTinNumber by remember { mutableStateOf(merchantTinNumber) }
                    var tempTradeLicense by remember { mutableStateOf(merchantTradeLicense) }
                    var tempEbirrId by remember { mutableStateOf(merchantEbirrId) }
                    var tempPayoutName by remember { mutableStateOf(merchantPayoutName) }
                    var showEditPayoutDialog by remember { mutableStateOf(false) }
                    var showCoverImageChoiceDialog = false
                    var payoutNameEditInput by remember { mutableStateOf(tempPayoutName) }
                    var cbeAccountEditInput by remember { mutableStateOf(tempCbeAccount) }
                    var telebirrEditInput by remember { mutableStateOf(tempTelebirrId) }
                    var ebirrEditInput by remember { mutableStateOf(tempEbirrId) }
                    var tempDiscountPercent by remember { mutableStateOf(merchantDiscountPercent) }
                    var selectedSettingsTab by remember { mutableStateOf("Profile") } // "Profile", "Payouts", "Operations"

                    val defaultWorkingHoursStr = activeUser?.businessWorkingHours ?: ""
                    val initialWeeklyWorked = if (defaultWorkingHoursStr.contains("Days:")) defaultWorkingHoursStr.substringAfter("Days:").substringBefore("|").trim() else "Monday - Saturday"
                    val initialTimeWorked = if (defaultWorkingHoursStr.contains("Hours:")) defaultWorkingHoursStr.substringAfter("Hours:").substringBefore("|").trim() else "09:00 AM - 08:30 PM"

                    var tempWeeklyWorked by remember(activeUser) {
                        mutableStateOf(initialWeeklyWorked)
                    }
                    var tempTimeWorked by remember(activeUser) {
                        mutableStateOf(initialTimeWorked)
                    }

                    val isProfileTabEmpty = (activeUser?.businessAbout ?: "").isBlank() || tempTimeWorked.isBlank()
                    val isPayoutsTabEmpty = tempEbirrId.isBlank() || tempTelebirrId.isBlank()

                    // Inline editing triggers for Payout Info
                    var isEditingPayouts by remember { mutableStateOf(false) }

                    var editPayoutNameVal by remember(tempPayoutName) { mutableStateOf(tempPayoutName) }
                    var editCbeAccountVal by remember(tempCbeAccount) { mutableStateOf(tempCbeAccount) }
                    var editTelebirrIdVal by remember(tempTelebirrId) { mutableStateOf(tempTelebirrId) }
                    var editEbirrIdVal by remember(tempEbirrId) { mutableStateOf(tempEbirrId) }

                    // Store Operations states
                    var deadStockThresholdDays by remember { mutableStateOf(90f) }
                    var deadStockUnsoldPercent by remember { mutableStateOf(50f) }
                    var deadStockAlertsEnabled by remember { mutableStateOf(true) }
                    var smartPricingEnabled by remember { mutableStateOf(true) }
                    var pdfAutoPrintEnabled by remember { mutableStateOf(false) }
                    var vatFilingSyncEnabled by remember { mutableStateOf(false) }

                    var isEditingAging by remember { mutableStateOf(false) }
                    var editAgeActiveLimit by remember(ageActiveLimit) { mutableStateOf(ageActiveLimit.toString()) }
                    var editAgeNormalLimit by remember(ageNormalLimit) { mutableStateOf(ageNormalLimit.toString()) }
                    var editAgeSlowLimit by remember(ageSlowLimit) { mutableStateOf(ageSlowLimit.toString()) }

                    // Gold Promo codes
                    var goldPromoCodes by remember(activeUser) {
                        val discStr = activeUser?.businessDiscountCode ?: ""
                        val code = if (discStr.contains("Code:")) discStr.substringAfter("Code:").substringBefore("|").trim() else "GOLDEN20"
                        val pct = if (discStr.contains("Percent:")) discStr.substringAfter("Percent:").substringBefore("|").trim().toIntOrNull() ?: 20 else 20
                        mutableStateOf(
                            if (discStr.isBlank()) emptyList()
                            else listOf(Pair(code, pct))
                        )
                    }
                    var goldCodeInput by remember { mutableStateOf("") }
                    var goldPercentInput by remember { mutableStateOf(25f) }
                    var editingGoldCodeIndex by remember { mutableStateOf<Int?>(null) }
                    var editingGoldPercentVal by remember { mutableStateOf(25f) }
                    var sharedPromoCodes by remember { mutableStateOf(setOf<String>()) }

                    if (showEditPayoutDialog) {
                        AlertDialog(
                            onDismissRequest = { showEditPayoutDialog = false },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payment, contentDescription = null, tint = BrandGreenPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit Payout Accounts")
                                }
                            },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Update your payout recipient details and bank accounts in one unified action:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = payoutNameEditInput,
                                        onValueChange = { payoutNameEditInput = it },
                                        label = { Text("Payout Beneficiary Name") },
                                        modifier = Modifier.fillMaxWidth().testTag("payout_edit_name_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = cbeAccountEditInput,
                                        onValueChange = { cbeAccountEditInput = it },
                                        label = { Text("Commercial Bank of Ethiopia (CBE) Account") },
                                        modifier = Modifier.fillMaxWidth().testTag("payout_edit_cbe_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = telebirrEditInput,
                                        onValueChange = { telebirrEditInput = it },
                                        label = { Text("Telebirr Merchant Account ID") },
                                        modifier = Modifier.fillMaxWidth().testTag("payout_edit_telebirr_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = ebirrEditInput,
                                        onValueChange = { ebirrEditInput = it },
                                        label = { Text("Ebirr Merchant Payout Account ID") },
                                        modifier = Modifier.fillMaxWidth().testTag("payout_edit_ebirr_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showEditPayoutDialog = false
                                        val newPayoutStr = "Name: $payoutNameEditInput | CBE: $cbeAccountEditInput | Telebirr: $telebirrEditInput | Ebirr: $ebirrEditInput"
                                        viewModel.updateSellerProfileField(mapOf("businessPayout" to newPayoutStr))
                                        
                                        tempPayoutName = payoutNameEditInput
                                        merchantPayoutName = payoutNameEditInput
                                        
                                        tempCbeAccount = cbeAccountEditInput
                                        merchantCbeAccount = cbeAccountEditInput
                                        
                                        tempTelebirrId = telebirrEditInput
                                        merchantTelebirrMerchantId = telebirrEditInput
                                        
                                        tempEbirrId = ebirrEditInput
                                        merchantEbirrId = ebirrEditInput
                                        
                                        android.widget.Toast.makeText(context, "Payout accounts saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                ) {
                                    Text("Save Changes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditPayoutDialog = false }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        )
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Stationary/Fixed AppBar ("do not move appbar")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { activeDashboardSubView = "MAIN" },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .testTag("merchant_settings_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Merchant Settings",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Manage configurations, payouts and store compliance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Elegant Navigation Tabs
                        val settingsTabs = listOf(
                            Triple("Profile", Icons.Default.Store, "Profile"),
                            Triple("Payouts", Icons.Default.Payment, "Payout Info"),
                            Triple("Operations", Icons.Default.Settings, "Operations")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            settingsTabs.forEach { (tab, icon, label) ->
                                val isSelected = selectedSettingsTab == tab
                                val hasError = when (tab) {
                                    "Profile" -> isProfileTabEmpty
                                    "Payouts" -> isPayoutsTabEmpty
                                    else -> false
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedSettingsTab = tab }
                                        .background(
                                            if (isSelected) BrandGreenPrimary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 2.dp)
                                        .testTag("settings_tab_$tab"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            if (hasError) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = 6.dp, y = (-2).dp)
                                                        .size(8.dp)
                                                        .background(Color.Red, shape = CircleShape)
                                                        .testTag("tab_empty_alert_dot_$tab")
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Scrollable Content Column
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (selectedSettingsTab) {
                                "Profile" -> {
                                    // Card 1: Visual Identity & Store Branding (Cover photo, avatar, name, phone, tin, tlc, business grade) - ALL READ ONLY ON FIRST CARD
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column {
                                            // Visual Store Banner with Avatar
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                            ) {
                                                // Store Banner Image
                                                AsyncImage(
                                                    model = tempProfileCoverUri,
                                                    contentDescription = "Store Banner Image",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize().clickable { sellerProfileCoverLauncher.launch("image/*") }
                                                )

                                                // Edit Cover Button overlay
                                                IconButton(
                                                    onClick = { sellerProfileCoverLauncher.launch("image/*") },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(10.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                        .size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Cover", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }

                                                // Store Circular Avatar (Overlapping bottom-start)
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(start = 16.dp, bottom = 8.dp)
                                                        .size(76.dp)
                                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                                        .border(3.dp, Color.White, CircleShape)
                                                        .padding(2.dp)
                                                        .clickable { sellerProfileImageLauncher.launch("image/*") }
                                                ) {
                                                    AsyncImage(
                                                        model = tempProfileImageUri,
                                                        contentDescription = "Profile Photo",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                    )
                                                    
                                                    // Little edit pen bubble on avatar
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .size(22.dp)
                                                            .background(BrandGreenPrimary, CircleShape)
                                                            .border(1.5.dp, Color.White, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White, modifier = Modifier.size(11.dp))
                                                    }
                                                }
                                            }

                                            // Content Info Rows
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "Official Profile",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (activeUser?.sellerVerification?.equals("approved", ignoreCase = true) == true) {
                                                        Surface(
                                                            color = Color(0xFFE8F5E9),
                                                            contentColor = Color(0xFF2E7D32),
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("VERIFIED", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                                            }
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = "Your store name, phone number, compliance registry and business grade are managed under trade registry verification and cannot be modified directly.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                Spacer(modifier = Modifier.height(2.dp))

                                                // Name row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Store, contentDescription = null, tint = BrandGreenPrimary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Store Name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Text(tempStoreName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                }

                                                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                                // Phone row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Phone, contentDescription = null, tint = BrandGreenPrimary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Store Contact Phone", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Text(tempPhone, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                }

                                                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                                // TIN Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandGreenPrimary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("TIN Number", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Text(tempTinNumber, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                }

                                                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                                // TLC Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandGreenPrimary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Trade License ID (TLC)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Text(tempTradeLicense, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                }

                                                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                                // Business Grade row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Star, contentDescription = null, tint = BrandGreenPrimary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Store Business Grade", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    
                                                     // Beautiful Dynamic Business Grade Badge (12 Tiers)
                                                     val productsSoldBadge = activeUser?.businessGrade?.toDoubleOrNull()?.toInt() ?: 0
                                                     val gradeInfo = getBusinessGradeInfo(productsSoldBadge)
                                                     val gradeText = gradeInfo.title.uppercase()
                                                     val isGold = gradeText.contains("GOLD")
                                                     val isBronze = gradeText.contains("BRONZE")
                                                     val isPlatinum = gradeText.contains("PLATINUM")
                                                     val isSovereign = gradeText.contains("SOVEREIGN") || gradeText.contains("EMPEROR") || gradeText.contains("APEX")
                                                     val badgeBg = when {
                                                         isSovereign -> Color(0xFFE0F7FA)
                                                         isPlatinum -> Color(0xFFF3E5F5)
                                                         isGold -> Color(0xFFFFF9C4)
                                                         isBronze -> Color(0xFFD7CCC8)
                                                         else -> Color(0xFFECEFF1)
                                                     }
                                                     val badgeContentColor = when {
                                                         isSovereign -> Color(0xFF006064)
                                                         isPlatinum -> Color(0xFF4A148C)
                                                         isGold -> Color(0xFFF57F17)
                                                         isBronze -> Color(0xFF5D4037)
                                                         else -> Color(0xFF455A64)
                                                     }
                                                     val badgeBorderColor = when {
                                                         isSovereign -> Color(0xFF00ACC1)
                                                         isPlatinum -> Color(0xFFAB47BC)
                                                         isGold -> Color(0xFFFBC02D)
                                                         isBronze -> Color(0xFF8D6E63)
                                                         else -> Color(0xFFCFD8DC)
                                                     }

                                                     Surface(
                                                         color = badgeBg,
                                                         contentColor = badgeContentColor,
                                                         shape = RoundedCornerShape(8.dp),
                                                         border = BorderStroke(1.dp, badgeBorderColor)
                                                     ) {
                                                         Row(
                                                             modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                             verticalAlignment = Alignment.CenterVertically
                                                         ) {
                                                             Icon(Icons.Default.Star, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(14.dp))
                                                             Spacer(modifier = Modifier.width(4.dp))
                                                             Text(gradeText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                                         }
                                                     }
                                                }
                                            }
                                        }
                                    }

                                    // Card 2: Business Grade Growth Progress (Products Sold count based on businessGrade)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = BrandGreenPrimary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Business Grade Sales Count",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            // Parse businessGrade as products sold count (12 Tiers)
                                             val productsSold = activeUser?.businessGrade?.toDoubleOrNull()?.toInt() ?: 0
                                             val gradeInfo = getBusinessGradeInfo(productsSold)
                                             val currentGradeText = gradeInfo.title.uppercase()
                                             val nextGradeText = gradeInfo.nextTitle.uppercase()
                                             val nextGradeRequirement = gradeInfo.maxRequirement
                                             val previousRequirement = gradeInfo.minRequirement

                                             val progressFloat = if (nextGradeText == "MAXIMUM" || productsSold >= 3500) {
                                                 1f
                                             } else {
                                                 val denominator = (nextGradeRequirement - previousRequirement).toFloat()
                                                 if (denominator > 0) {
                                                     ((productsSold - previousRequirement).toFloat() / denominator).coerceIn(0f, 1f)
                                                 } else {
                                                     0f
                                                 }
                                             }

                                             val progressPercentage = (progressFloat * 100).toInt()

                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Column {
                                                     Text(
                                                         text = "$currentGradeText Rank Progress",
                                                         style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                     )
                                                     Text(
                                                         text = "Products Sold: $productsSold",
                                                         style = MaterialTheme.typography.bodySmall,
                                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                                     )
                                                 }

                                                 Surface(
                                                     color = BrandGreenPrimary.copy(alpha = 0.1f),
                                                     contentColor = BrandGreenPrimary,
                                                     shape = RoundedCornerShape(12.dp)
                                                 ) {
                                                     Text(
                                                         text = if (productsSold >= 3500) "Max Tier reached!" else "Progress: $progressPercentage%",
                                                         modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                         style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                     )
                                                 }
                                             }

                                             Spacer(modifier = Modifier.height(2.dp))

                                             // Growth target visualization
                                             Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.SpaceBetween
                                                 ) {
                                                     Text(
                                                         text = if (productsSold >= 3500) "Elite Seller Status Achieved!" else "Sales to reach $nextGradeText Rank:",
                                                         style = MaterialTheme.typography.bodySmall,
                                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                                     )
                                                     Text(
                                                         text = if (productsSold >= 3500) "$productsSold sold" else "$productsSold / $nextGradeRequirement sold",
                                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                     )
                                                 }

                                                 LinearProgressIndicator(
                                                     progress = progressFloat.coerceIn(0f, 1f),
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .height(8.dp)
                                                         .clip(RoundedCornerShape(4.dp)),
                                                     color = BrandGreenPrimary,
                                                     trackColor = BrandGreenPrimary.copy(alpha = 0.15f)
                                                 )
                                        Text(
                                                    text = "💡 Business Grade Level: Based on your total product sales count (currently $productsSold sold). Keep completing orders to climb the merchant tiers and unlock premium benefits like lower commission rates, faster payouts, and priority support!",
                                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    // Combined Card 3: About Your Business & Operations Settings
                                    var isEditingAboutAndHours by remember { mutableStateOf(false) }
                                    var editAboutText by remember(activeUser?.businessAbout) {
                                        mutableStateOf(activeUser?.businessAbout ?: "")
                                    }
                                    var editWeeklyWorkedVal by remember { mutableStateOf(tempWeeklyWorked) }
                                    var editTimeWorkedVal by remember { mutableStateOf(tempTimeWorked) }

                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("merchant_about_card"),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Info, contentDescription = null, tint = BrandGreenPrimary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "About Your Business & Operations",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                if (!isEditingAboutAndHours) {
                                                    IconButton(onClick = { 
                                                        editAboutText = activeUser?.businessAbout ?: ""
                                                        editWeeklyWorkedVal = tempWeeklyWorked
                                                        editTimeWorkedVal = tempTimeWorked
                                                        isEditingAboutAndHours = true 
                                                    }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Business Info", tint = BrandGreenPrimary)
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Configure your boutique story, organic sourcing standards, or traditional craftsmanship along with weekly operating days and daily hours. These will display on your Store Detail Page.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (isEditingAboutAndHours) {
                                                OutlinedTextField(
                                                    value = editAboutText,
                                                    onValueChange = { editAboutText = it },
                                                    label = { Text("Describe your business story & focus") },
                                                    placeholder = { Text("Describe your business story, origins, and focus...") },
                                                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("merchant_about_input"),
                                                    textStyle = MaterialTheme.typography.bodyMedium
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                OutlinedTextField(
                                                    value = editWeeklyWorkedVal,
                                                    onValueChange = { editWeeklyWorkedVal = it },
                                                    label = { Text("Days of Operation") },
                                                    modifier = Modifier.fillMaxWidth().testTag("merchant_weekly_worked_input"),
                                                    singleLine = true
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                OutlinedTextField(
                                                    value = editTimeWorkedVal,
                                                    onValueChange = { editTimeWorkedVal = it },
                                                    label = { Text("Daily Working Hours") },
                                                    modifier = Modifier.fillMaxWidth().testTag("merchant_time_worked_input"),
                                                    singleLine = true
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(onClick = { isEditingAboutAndHours = false }) {
                                                        Text("Cancel")
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            tempWeeklyWorked = editWeeklyWorkedVal
                                                            tempTimeWorked = editTimeWorkedVal
                                                            viewModel.updateStoreWeeklyWorked("Bole Traditional Boutique", editWeeklyWorkedVal)
                                                            viewModel.updateStoreWeeklyWorked("Bole Boutique", editWeeklyWorkedVal)
                                                            viewModel.updateStoreTimeWorked("Bole Traditional Boutique", editTimeWorkedVal)
                                                            viewModel.updateStoreTimeWorked("Bole Boutique", editTimeWorkedVal)
                                                            val newHoursStr = "Days: $editWeeklyWorkedVal | Hours: $editTimeWorkedVal"
                                                            viewModel.updateSellerProfileField(
                                                                mapOf(
                                                                    "businessDescription" to editAboutText,
                                                                    "businessWorkingHours" to newHoursStr
                                                                )
                                                            )
                                                            isEditingAboutAndHours = false
                                                            android.widget.Toast.makeText(context, "Business info and operating times updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.testTag("merchant_working_times_save_btn")
                                                    ) {
                                                        Text("Save Changes")
                                                    }
                                                }
                                            } else {
                                                // Read Only Display
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    // Bio/Description
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                    ) {
                                                        Text(
                                                            text = editAboutText.ifEmpty { "No about story specified yet. Click Edit to add one!" },
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.padding(12.dp),
                                                            color = if (editAboutText.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }

                                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                                    // Weekly days
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(Icons.Default.DateRange, null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                                        Column {
                                                            Text("Days of Operation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(tempWeeklyWorked.ifEmpty {""}, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                        }
                                                    }

                                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                                    // Daily hours
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(Icons.Default.Schedule, null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                                        Column {
                                                            Text("Working Hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(tempTimeWorked.ifEmpty {""}, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                "Payouts" -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("payout_accounts_card"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Payment, contentDescription = null, tint = BrandGreenPrimary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Payout Recipient & Accounts",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                if (!isEditingPayouts) {
                                                    IconButton(onClick = {
                                                        editPayoutNameVal = tempPayoutName
                                                        editCbeAccountVal = tempCbeAccount
                                                        editTelebirrIdVal = tempTelebirrId
                                                        editEbirrIdVal = tempEbirrId
                                                        isEditingPayouts = true
                                                    }, modifier = Modifier.testTag("payout_edit_toggle_btn")) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Payout Info", tint = BrandGreenPrimary)
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Configure your official bank accounts, mobile wallets, and beneficiary names for secure business payouts. These are processed in one unified action.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (isEditingPayouts) {
                                                OutlinedTextField(
                                                    value = editPayoutNameVal,
                                                    onValueChange = { editPayoutNameVal = it },
                                                    label = { Text("Payout Account Beneficiary Name") },
                                                    modifier = Modifier.fillMaxWidth().testTag("payout_edit_name_field"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                OutlinedTextField(
                                                    value = editCbeAccountVal,
                                                    onValueChange = { editCbeAccountVal = it },
                                                    label = { Text("Commercial Bank of Ethiopia (CBE) Account") },
                                                    modifier = Modifier.fillMaxWidth().testTag("payout_edit_cbe_field"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                OutlinedTextField(
                                                    value = editTelebirrIdVal,
                                                    onValueChange = { editTelebirrIdVal = it },
                                                    label = { Text("Telebirr Merchant Account ID") },
                                                    modifier = Modifier.fillMaxWidth().testTag("payout_edit_telebirr_field"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                OutlinedTextField(
                                                    value = editEbirrIdVal,
                                                    onValueChange = { editEbirrIdVal = it },
                                                    label = { Text("Ebirr Merchant Payout Account ID") },
                                                    modifier = Modifier.fillMaxWidth().testTag("payout_edit_ebirr_field"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(onClick = { isEditingPayouts = false }) {
                                                        Text("Cancel")
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            tempPayoutName = editPayoutNameVal
                                                            merchantPayoutName = editPayoutNameVal
                                                            
                                                            tempCbeAccount = editCbeAccountVal
                                                            merchantCbeAccount = editCbeAccountVal
                                                            
                                                            tempTelebirrId = editTelebirrIdVal
                                                            merchantTelebirrMerchantId = editTelebirrIdVal
                                                            
                                                            tempEbirrId = editEbirrIdVal
                                                            merchantEbirrId = editEbirrIdVal
                                                            
                                                            val newPayoutStr = "Name: $editPayoutNameVal | CBE: $editCbeAccountVal | Telebirr: $editTelebirrIdVal | Ebirr: $editEbirrIdVal"
                                                            viewModel.updateSellerProfileField(mapOf("businessPayout" to newPayoutStr))
                                                            isEditingPayouts = false
                                                            android.widget.Toast.makeText(context, "Payout details updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.testTag("payout_save_btn")
                                                    ) {
                                                        Text("Save Changes")
                                                    }
                                                }
                                            } else {
                                                // Read Only Display
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    // 1. Beneficiary Name
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(24.dp))
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Payout Beneficiary Name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(tempPayoutName.ifBlank { "Not Configured" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = if (tempPayoutName.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                                                        }
                                                    }

                                                    // 2. CBE Account
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(24.dp))
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Commercial Bank of Ethiopia (CBE) Account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(tempCbeAccount.ifBlank { "Not Configured" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = if (tempCbeAccount.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                                                        }
                                                    }

                                                    // 3. Telebirr
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(24.dp))
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Telebirr Merchant Account ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(tempTelebirrId.ifBlank { "Not Configured" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = if (tempTelebirrId.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                                                        }
                                                    }

                                                    // 4. Ebirr
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(24.dp))
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Ebirr Merchant Payout Account ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(tempEbirrId.ifBlank { "Not Configured" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = if (tempEbirrId.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "Operations" -> {
                                    // 1. Operational Status Control
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Settings, contentDescription = null, tint = BrandGreenPrimary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Store Operational Status",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .background(if (storeOpen) Color(0xFF2E7D32) else Color(0xFFC62828), CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = if (storeOpen) "Accepting Customer Orders" else "Temporarily Offline",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                        )
                                                    }
                                                    Text(
                                                        text = if (storeOpen) "Customers can browse products and checkout on your storefront." else "Pause storefront checkouts without removing your products.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(start = 16.dp)
                                                    )
                                                }
                                                Switch(
                                                    checked = storeOpen,
                                                    onCheckedChange = { viewModel.toggleStoreOpen() },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = BrandGreenPrimary,
                                                        checkedTrackColor = BrandGreenPrimary.copy(alpha = 0.3f)
                                                    ),
                                                    modifier = Modifier.testTag("settings_store_status_switch")
                                                )
                                            }
                                        }
                                    }

                                    // 2. Aging of Inventory Control Card (Renamed/Repurposed from Dead Stock Alert Control Card)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = BrandGreenPrimary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Aging of Inventory Control",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                if (!isEditingAging) {
                                                    IconButton(onClick = {
                                                        editAgeActiveLimit = ageActiveLimit.toString()
                                                        editAgeNormalLimit = ageNormalLimit.toString()
                                                        editAgeSlowLimit = ageSlowLimit.toString()
                                                        isEditingAging = true
                                                    }, modifier = Modifier.testTag("aging_edit_toggle_btn")) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Aging Thresholds", tint = BrandGreenPrimary)
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Configure parameters based on inventory aging thresholds (in days) to automatically classify your products as Active, Normal, Slow, or Dead stock.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (isEditingAging) {
                                                OutlinedTextField(
                                                    value = editAgeActiveLimit,
                                                    onValueChange = { editAgeActiveLimit = it },
                                                    label = { Text("Active Stock Limit (days)") },
                                                    modifier = Modifier.fillMaxWidth().testTag("aging_edit_active_field"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                OutlinedTextField(
                                                    value = editAgeNormalLimit,
                                                    onValueChange = { editAgeNormalLimit = it },
                                                    label = { Text("Normal Stock Limit (days)") },
                                                    modifier = Modifier.fillMaxWidth().testTag("aging_edit_normal_field"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                OutlinedTextField(
                                                    value = editAgeSlowLimit,
                                                    onValueChange = { editAgeSlowLimit = it },
                                                    label = { Text("Slow Stock Limit (days)") },
                                                    modifier = Modifier.fillMaxWidth().testTag("aging_edit_slow_field"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(onClick = { isEditingAging = false }) {
                                                        Text("Cancel")
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            val activeVal = editAgeActiveLimit.toIntOrNull() ?: 15
                                                            val normalVal = editAgeNormalLimit.toIntOrNull() ?: 30
                                                            val slowVal = editAgeSlowLimit.toIntOrNull() ?: 60
                                                            
                                                            if (activeVal < 1 || normalVal < 1 || slowVal < 1) {
                                                                android.widget.Toast.makeText(context, "Threshold limits must be greater than 0!", android.widget.Toast.LENGTH_LONG).show()
                                                            } else if (activeVal >= normalVal || normalVal >= slowVal) {
                                                                android.widget.Toast.makeText(context, "Limits must be strictly ordered: Active < Normal < Slow", android.widget.Toast.LENGTH_LONG).show()
                                                            } else {
                                                                ageActiveLimit = activeVal
                                                                ageNormalLimit = normalVal
                                                                ageSlowLimit = slowVal
                                                                
                                                                sharedPrefs.edit().apply {
                                                                    putInt("age_active_limit", activeVal)
                                                                    putInt("age_normal_limit", normalVal)
                                                                    putInt("age_slow_limit", slowVal)
                                                                    commit()
                                                                }
                                                                
                                                                isEditingAging = false
                                                                android.widget.Toast.makeText(context, "Aging thresholds updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.testTag("aging_save_btn")
                                                    ) {
                                                        Text("Save Changes")
                                                    }
                                                }
                                            } else {
                                                // Read Only Display
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    // 1. Active Stock
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .background(BrandGreenPrimary.copy(alpha = 0.15f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("🟢", fontSize = 12.sp)
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Active Stock Threshold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("Up to $ageActiveLimit Days", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                                        }
                                                    }

                                                    // 2. Normal Stock
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .background(BrandGoldSecondary.copy(alpha = 0.15f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("🟡", fontSize = 12.sp)
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Normal Stock Threshold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("${ageActiveLimit + 1} to $ageNormalLimit Days", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                                        }
                                                    }

                                                    // 3. Slow Stock
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .background(Color(0xFF9C27B0).copy(alpha = 0.15f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("🟣", fontSize = 12.sp)
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Slow Stock Threshold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("${ageNormalLimit + 1} to $ageSlowLimit Days", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                                        }
                                                    }

                                                    // 4. Dead Stock
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .background(BrandRedTertiary.copy(alpha = 0.15f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("🔴", fontSize = 12.sp)
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Dead Stock Threshold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("Older than $ageSlowLimit Days", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 3. Gold Customer Discount Codes
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandGoldSecondary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Gold Customer Discount Codes",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Text(
                                                text = "Create and manage exclusive discount codes for your highest-spending Gold VIP customers. Gold promo codes can be edited or removed at any time.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                            // Form to Add New Code
                                            Text(text = "Add Custom Gold Discount", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = goldCodeInput,
                                                    onValueChange = { if (it.length <= 10) goldCodeInput = it },
                                                    placeholder = { Text("e.g. GOLDEN35") },
                                                    label = { Text("Promo Code") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1.3f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )

                                                Column(modifier = Modifier.weight(1.2f)) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("Discount:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${goldPercentInput.toInt()}% Off", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandGreenPrimary)
                                                    }
                                                    Slider(
                                                        value = goldPercentInput,
                                                        onValueChange = { goldPercentInput = it },
                                                        valueRange = 5f..50f,
                                                        colors = SliderDefaults.colors(
                                                            activeTrackColor = BrandGreenPrimary,
                                                            thumbColor = BrandGreenPrimary
                                                        )
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val trimmed = goldCodeInput.trim().uppercase()
                                                        if (trimmed.isBlank()) {
                                                            android.widget.Toast.makeText(context, "Code cannot be empty!", android.widget.Toast.LENGTH_SHORT).show()
                                                        } else if (trimmed.length < 6 || trimmed.length > 10) {
                                                            android.widget.Toast.makeText(context, "Code must be between 6 and 10 characters!", android.widget.Toast.LENGTH_SHORT).show()
                                                        } else if (goldPromoCodes.any { it.first == trimmed }) {
                                                            android.widget.Toast.makeText(context, "Code already exists!", android.widget.Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            val pct = goldPercentInput.toInt()
                                                            val discStr = "Code: $trimmed | Percent: $pct"
                                                            viewModel.updateSellerProfileField(mapOf("businessDiscountCode" to discStr))
                                                            goldPromoCodes = listOf(Pair(trimmed, pct))
                                                            goldCodeInput = ""
                                                            android.widget.Toast.makeText(context, "Gold Code $trimmed added!", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .background(BrandGreenPrimary, RoundedCornerShape(8.dp))
                                                        .size(44.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                                                }
                                            }

                                            Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                            Text(text = "Active Gold Promo Codes", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                                            if (goldPromoCodes.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("No active Gold discount codes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                }
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    goldPromoCodes.forEachIndexed { index, item ->
                                                        val isEditing = editingGoldCodeIndex == index
                                                        Card(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (isEditing) BrandGreenPrimary.copy(alpha = 0.05f) 
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                            ),
                                                            shape = RoundedCornerShape(12.dp),
                                                            border = BorderStroke(1.dp, if (isEditing) BrandGreenPrimary else Color.Transparent)
                                                        ) {
                                                            Column(modifier = Modifier.padding(12.dp)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(Icons.Default.Star, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(16.dp))
                                                                        Spacer(modifier = Modifier.width(6.dp))
                                                                        Text(
                                                                            text = item.first,
                                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                            color = MaterialTheme.colorScheme.onSurface
                                                                        )
                                                                    }

                                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                        Text(
                                                                            text = "${item.second}% OFF",
                                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                            color = BrandRedTertiary
                                                                        )
                                                                        
                                                                        IconButton(
                                                                            onClick = {
                                                                                if (isEditing) {
                                                                                    editingGoldCodeIndex = null
                                                                                } else {
                                                                                    editingGoldCodeIndex = index
                                                                                    editingGoldPercentVal = item.second.toFloat()
                                                                                }
                                                                            },
                                                                            modifier = Modifier.size(32.dp)
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                                                                contentDescription = "Edit Discount",
                                                                                tint = BrandGreenPrimary,
                                                                                modifier = Modifier.size(18.dp)
                                                                            )
                                                                        }

                                                                        IconButton(
                                                                            onClick = {
                                                                                val removedCode = item.first
                                                                                goldPromoCodes = emptyList()
                                                                                viewModel.updateSellerProfileField(mapOf("businessDiscountCode" to ""))
                                                                                if (editingGoldCodeIndex == index) {
                                                                                    editingGoldCodeIndex = null
                                                                                }
                                                                                android.widget.Toast.makeText(context, "Code $removedCode deleted!", android.widget.Toast.LENGTH_SHORT).show()
                                                                            },
                                                                            modifier = Modifier.size(32.dp)
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Default.Delete,
                                                                                contentDescription = "Delete Discount",
                                                                                tint = BrandRedTertiary,
                                                                                modifier = Modifier.size(18.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }

                                                                if (isEditing) {
                                                                    Spacer(modifier = Modifier.height(8.dp))
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                                    ) {
                                                                        Slider(
                                                                            value = editingGoldPercentVal,
                                                                            onValueChange = { editingGoldPercentVal = it },
                                                                            valueRange = 5f..50f,
                                                                            modifier = Modifier.weight(1f),
                                                                            colors = SliderDefaults.colors(
                                                                                activeTrackColor = BrandGreenPrimary,
                                                                                thumbColor = BrandGreenPrimary
                                                                            )
                                                                        )
                                                                        Text(
                                                                            text = "${editingGoldPercentVal.toInt()}%",
                                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                                        )
                                                                        Button(
                                                                            onClick = {
                                                                                val updatedList = goldPromoCodes.toMutableList()
                                                                                val newPct = editingGoldPercentVal.toInt()
                                                                                updatedList[index] = Pair(item.first, newPct)
                                                                                val discStr = "Code: ${item.first} | Percent: $newPct"
                                                                                viewModel.updateSellerProfileField(mapOf("businessDiscountCode" to discStr))
                                                                                goldPromoCodes = updatedList
                                                                                editingGoldCodeIndex = null
                                                                                android.widget.Toast.makeText(context, "Discount percent updated and saved!", android.widget.Toast.LENGTH_SHORT).show()


                                                                                goldPromoCodes = updatedList
                                                                                editingGoldCodeIndex = null
                                                                                android.widget.Toast.makeText(context, "Discount percent updated!", android.widget.Toast.LENGTH_SHORT).show()
                                                                            },
                                                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                            shape = RoundedCornerShape(6.dp),
                                                                            modifier = Modifier.height(28.dp)
                                                                        ) {
                                                                            Text("Apply", fontSize = 11.sp, color = Color.White)
                                                                        }
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.height(6.dp))
                                                                val isShared = sharedPromoCodes.contains(item.first)
                                                                androidx.compose.material3.AssistChip(
                                                                    onClick = {
                                                                        if (isShared) {
                                                                            sharedPromoCodes = sharedPromoCodes - item.first
                                                                            android.widget.Toast.makeText(context, "Removed ${item.first} from Featured Promotion", android.widget.Toast.LENGTH_SHORT).show()
                                                                        } else {
                                                                            sharedPromoCodes = sharedPromoCodes + item.first
                                                                            android.widget.Toast.makeText(context, "Shared ${item.first} to Featured Promotion!", android.widget.Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    },
                                                                    label = {
                                                                        Text(
                                                                            text = if (isShared) "Shared to Featured Promotion" else "Share to Featured Promotion",
                                                                            style = MaterialTheme.typography.labelSmall
                                                                        )
                                                                    },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            imageVector = if (isShared) Icons.Default.CheckCircle else Icons.Default.Share,
                                                                            contentDescription = null,
                                                                            tint = if (isShared) BrandGreenPrimary else MaterialTheme.colorScheme.outline,
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                    },
                                                                    colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                                                        containerColor = if (isShared) BrandGreenPrimary.copy(alpha = 0.08f) else Color.Transparent,
                                                                        labelColor = if (isShared) BrandGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                                    ),
                                                                    border = BorderStroke(1.dp, if (isShared) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                else -> {
                    // MAIN View (Clean dashboard with clickable stats)
                    if (isSellerDashboardLoading) {
                        FullSellerDashboardSkeleton()
                    } else {
                        HeaderBlock()
                        
                        if (isWideScreen) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    StatsBlock()
                                    SellerReviewsBlock()
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    OrdersBlock()
                                }
                            }
                        } else {
                            StatsBlock()
                            OrdersBlock()
                            SellerReviewsBlock()
                        }
                    }
                }
            }
        }

        // Payment Confirmation Dialog for Incoming Pending Orders
        if (showPaymentConfirmDialog && orderToConfirmPayment != null) {
            PaymentConfirmationDialog(
                order = orderToConfirmPayment!!,
                viewModel = viewModel,
                activeUser = activeUser,
                merchantStoreName = merchantStoreName,
                onDismissRequest = {
                    showPaymentConfirmDialog = false
                    orderToConfirmPayment = null
                }
            )
        }

        // Cancellation reason dialog
        if (showCancelDialog && selectedOrderToCancel != null) {
            CancelOrderDialog(
                order = selectedOrderToCancel!!,
                viewModel = viewModel,
                onDismissRequest = {
                    showCancelDialog = false
                    selectedOrderToCancel = null
                }
            )
        }

        // Ready for Pickup confirmation dialog
        if (showReadyConfirmDialog && orderToMarkReady != null) {
            ReadyConfirmDialog(
                order = orderToMarkReady!!,
                viewModel = viewModel,
                onDismissRequest = {
                    showReadyConfirmDialog = false
                    orderToMarkReady = null
                }
            )
        }

        // Courier verification dialog
        if (showCourierVerificationDialog && orderForCourierVerification != null) {
            CourierVerificationDialog(
                order = orderForCourierVerification!!,
                viewModel = viewModel,
                onDismissRequest = {
                    showCourierVerificationDialog = false
                    orderForCourierVerification = null
                }
            )
        }

        // Buyer verification dialog for self-collect (buyer pickup)
        if (showBuyerVerificationDialog && orderForBuyerVerification != null) {
            BuyerVerificationDialog(
                order = orderForBuyerVerification!!,
                viewModel = viewModel,
                onDismissRequest = {
                    showBuyerVerificationDialog = false
                    orderForBuyerVerification = null
                }
            )
        }

        // Seller Custom Date Dialog with visual Calendar Widget
        if (showSellerDateDialog) {
            UnifiedCalendarDialog(
                initialStartDate = sellerStartDate,
                initialEndDate = sellerEndDate,
                onConfirm = { start, end, label ->
                    sellerStartDate = start
                    sellerEndDate = end
                    orderDateFilter = label
                    showSellerDateDialog = false
                },
                onDismiss = { showSellerDateDialog = false },
                title = "Filter Customer Orders",
                subtitle = "Select a date range to view orders",
                showPresets = true
            )
        }
    }
}


@Composable
fun EditProductScreen(viewModel: AppViewModel) {
    val editingProduct by viewModel.editingProduct.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var name by remember { mutableStateOf(editingProduct?.name ?: "") }
    var priceStr by remember { mutableStateOf(editingProduct?.price?.toString() ?: "") }
    val initiallyHasVariantPrices = remember(editingProduct) {
        editingProduct?.let { parseStructuredVariants(it.variants).any { v -> v.price != null } } ?: false
    }
    var calculatePriceByVariant by remember { mutableStateOf(initiallyHasVariantPrices) }
    var varPriceInput by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf(editingProduct?.stock?.toString() ?: "") }
    val categories = listOf(
        "Electronics", "Fashion", "Home", "Beauty", "Toys", "Sport", "Book"
    )
    
    val categoryToSubcategories = mapOf(
        "Electronics" to listOf("Mobile Phones", "Tablets", "Laptops", "Cameras", "Accessories"),
        "Fashion" to listOf("Men's Clothing", "Women's Clothing", "Shoes", "Bags & Accessories"),
        "Home" to listOf("Furniture", "Kitchenware", "Bedding", "Lighting"),
        "Beauty" to listOf("Skincare", "Makeup", "Fragrance", "Haircare"),
        "Toys" to listOf("Action Figures", "Board Games", "Dolls", "Puzzles"),
        "Sport" to listOf("Fitness Equipment", "Outdoor Gear", "Athletic Wear"),
        "Book" to listOf("Fiction", "Non-Fiction", "Textbooks", "Children's Books")
    )

    var category by remember { mutableStateOf(editingProduct?.category ?: "") }
    var subcategory by remember { mutableStateOf(editingProduct?.subcategory ?: "") }
    var isPromoted by remember { mutableStateOf(editingProduct?.isPromoted ?: false) }
    
    // New fields
    var unitOfMeasurement by remember { mutableStateOf(editingProduct?.unitOfMeasurement ?: "pieces") }
    var description by remember { mutableStateOf(editingProduct?.description ?: "") }
    var stateNewOrUsed by remember { mutableStateOf(editingProduct?.stateNewOrUsed ?: "New") }
    var isFreeDelivery by remember { mutableStateOf(editingProduct?.isFreeDelivery ?: false) }
    var hasDiscount by remember { mutableStateOf((editingProduct?.discountPercent ?: 0) > 0) }
    var discountPercent by remember { mutableStateOf((editingProduct?.discountPercent ?: 0).toFloat()) }
    var lowStockThreshold by remember(editingProduct) { mutableStateOf(editingProduct?.lowStock?.toString() ?: "3") }
    var lowStockError by remember { mutableStateOf<String?>(null) }
    var brand by remember { mutableStateOf(editingProduct?.brand ?: "") }
    var variants by remember { mutableStateOf(editingProduct?.variants ?: "") }

    // Search and Custom Inputs for Selector Dialogs
    var categorySearchQuery by remember { mutableStateOf("") }
    var customCategoryInput by remember { mutableStateOf("") }

    var subcategorySearchQuery by remember { mutableStateOf("") }
    var customSubcategoryInput by remember { mutableStateOf("") }

    var brandSearchQuery by remember { mutableStateOf("") }
    var customBrandInput by remember { mutableStateOf("") }

    // Structured variants state
    var structuredVariants by remember {
        mutableStateOf(
            editingProduct?.let { parseStructuredVariants(it.variants) } ?: emptyList()
        )
    }

    // Dynamic labels and placeholders for Variant Fields based on Category/Subcategory
    val label1 = when (category) {
        "Electronics" -> "Color"
        "Fashion" -> "Color"
        "Home" -> "Material/Color"
        "Beauty" -> "Shade/Type"
        "Toys" -> "Color/Style"
        "Sport" -> "Color/Type"
        "Book" -> "Cover Type"
        else -> "Color/Type"
    }
    val placeholder1 = when (category) {
        "Electronics" -> "e.g. Midnight Black"
        "Fashion" -> "e.g. Red"
        "Home" -> "e.g. Oak Wood"
        "Beauty" -> "e.g. Matte Pink"
        "Toys" -> "e.g. Red / Sci-Fi"
        "Sport" -> "e.g. Blue"
        "Book" -> "e.g. Hardcover"
        else -> "e.g. Standard"
    }

    val label2 = when (category) {
        "Electronics" -> if (subcategory == "Laptops" || subcategory == "Mobile Phones" || subcategory == "Tablets") "Storage" else "Size"
        "Fashion" -> "Clothing/Shoe Size"
        "Home" -> "Dimensions"
        "Beauty" -> "Volume/Size"
        "Toys" -> "Size"
        "Sport" -> "Size"
        "Book" -> "Language"
        else -> "Size/Storage"
    }
    val placeholder2 = when (category) {
        "Electronics" -> if (subcategory == "Laptops" || subcategory == "Mobile Phones" || subcategory == "Tablets") "e.g. 256GB" else "e.g. Medium"
        "Fashion" -> "e.g. XL or 42"
        "Home" -> "e.g. 120x80cm"
        "Beauty" -> "e.g. 100ml"
        "Toys" -> "e.g. Large"
        "Sport" -> "e.g. Large"
        "Book" -> "e.g. Amharic / English"
        else -> "e.g. Medium"
    }

    val label3 = when (category) {
        "Electronics" -> "RAM/Specs"
        "Fashion" -> "Material"
        "Home" -> "Style/Finish"
        "Beauty" -> "Skin Type/SPF"
        "Toys" -> "Age Group"
        "Sport" -> "Material/Specs"
        "Book" -> "Edition"
        else -> "Specs/Features"
    }
    val placeholder3 = when (category) {
        "Electronics" -> "e.g. 8GB RAM"
        "Fashion" -> "e.g. 100% Cotton"
        "Home" -> "e.g. Glossy Finish"
        "Beauty" -> "e.g. All Skin Types"
        "Toys" -> "e.g. Age 6+"
        "Sport" -> "e.g. Carbon Fiber"
        "Book" -> "e.g. 1st Edition"
        else -> "e.g. Standard Pack"
    }

    // Dialog flags
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showSubcategoryDialog by remember { mutableStateOf(false) }
    var showBrandDialog by remember { mutableStateOf(false) }

    // Dynamic Multiple Image URL or Preset State
    val initialImages = remember(editingProduct) {
        val list = mutableListOf<String>()
        val prod = editingProduct
        if (prod != null) {
            if (prod.imageUrlName.isNotBlank()) {
                prod.imageUrlName.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { list.add(it) }
            }
            if (prod.imageResName.isNotBlank()) {
                prod.imageResName.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { list.add(it) }
            }
        }
        if (list.isEmpty()) {
            list.add("img_banner_1")
        }
        list.distinct()
    }
    var selectedImagesList by remember { mutableStateOf<List<String>>(initialImages) }
    var customImageUrlInput by remember { mutableStateOf("") }
    var showCustomUrlField by remember { mutableStateOf(false) }

    // Simulated Barcode Setting
    var barcodeSku by remember { mutableStateOf("") }

    // Focus Manager & Saving state
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var isSaving by remember { mutableStateOf(false) }

    // Validation States
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var stockError by remember { mutableStateOf<String?>(null) }

    val units = listOf("pieces", "kilograms (kg)", "packets", "liters (L)", "meters (m)", "cartons")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            val newUrls = uris.map { it.toString() }
            selectedImagesList = (selectedImagesList + newUrls).distinct()
            android.widget.Toast.makeText(context, "📸 ${uris.size} photo(s) added!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Unsplash Preset list
    val imagePresets = listOf(
        Triple("Electronics", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600", "Electronics"),
        Triple("Fashion", "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600", "Fashion"),
        Triple("Home", "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=600", "Home Furniture"),
        Triple("Beauty", "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=600", "Beauty Skincare"),
        Triple("Toys", "https://images.unsplash.com/photo-1539627831859-a911cf04b3cd?w=600", "Toys & Games"),
        Triple("Sport", "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600", "Sport & Fitness"),
        Triple("Book", "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600", "Books"),
        Triple("Traditional Preset", "img_banner_1", "Traditional Graphic"),
        Triple("Modern Preset", "img_banner_2", "Modern Graphic")
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {viewModel.navigateTo(AppScreen.MAIN)},
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editingProduct == null) "Product Details" else "Edit Product Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (editingProduct == null) "Add your new inventory information" else "Keep your inventory up-to-date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )   
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (viewModel.editProductFromLiveStock) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("locked_fields_banner"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Standard identifiers (Name, Category, Subcategory, Brand, Product State, Unit) are locked when editing from Live Stock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Live Product Preview Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Dynamic preview images list for sliding preview
                    val previewImages = remember(selectedImagesList, customImageUrlInput) {
                        val list = mutableListOf<String>()
                        if (customImageUrlInput.isNotBlank() && !list.contains(customImageUrlInput)) {
                            list.add(customImageUrlInput)
                        }
                        selectedImagesList.forEach { img ->
                            if (img.isNotBlank() && !list.contains(img)) {
                                list.add(img)
                            }
                        }
                        if (list.isEmpty()) {
                            list.add("img_banner_1")
                        }
                        list
                    }
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { previewImages.size })

                    // Header Image Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIndex ->
                            val currentImg = previewImages.getOrElse(pageIndex) { "img_banner_1" }
                            Image(
                                painter = getBannerPainter(currentImg),
                                contentDescription = "Dynamic Live Preview ${pageIndex + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Sliding page indicators
                        if (previewImages.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 32.dp, end = 12.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                previewImages.indices.forEach { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (pagerState.currentPage == index) 8.dp else 5.dp)
                                            .clip(CircleShape)
                                            .background(if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                        
                        // Promotional / Status Badges on top of preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left badges: State & Free Delivery
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (stateNewOrUsed == "New") BrandGreenPrimary else Color(0xFFE65100))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = stateNewOrUsed.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                if (isFreeDelivery) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "FREE DELIVERY",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Right badge: Discount Percent
                            if (hasDiscount && discountPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandRedTertiary)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${discountPercent.toInt()}% OFF",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Bottom preview banner
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Live Storefront Preview of: ${name.ifBlank { "Product Name" }}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    // Metadata preview row
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name.ifBlank { "Product Title" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            val finalPrice = priceStr.toDoubleOrNull() ?: 0.0
                            val discountedPrice = if (hasDiscount) finalPrice * (1.0 - (discountPercent / 100.0)) else finalPrice
                            
                            Column(horizontalAlignment = Alignment.End) {
                                if (hasDiscount && finalPrice > 0) {
                                    Text(
                                        text = "ETB ${String.format("%.2f", finalPrice)}",
                                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "ETB ${String.format("%.2f", discountedPrice)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = BrandGreenPrimary
                                    )
                                } else {
                                    Text(
                                        text = "ETB ${String.format("%.2f", finalPrice)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = BrandGreenPrimary
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Category: ${category.ifBlank { "None" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Stock: ${stockStr.ifBlank { "0" }} $unitOfMeasurement",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if ((stockStr.toIntOrNull() ?: 0) <= 3) BrandRedTertiary else BrandGreenPrimary
                            )
                        }
                    }
                }
            }

            // Image Upload Selector Card
            Text(
                text = "Product Display Image",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Upload custom product photos or provide external image URLs:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Display multi-image thumbnail list
                    if (selectedImagesList.isNotEmpty()) {
                        Text(
                            text = "Selected Images (${selectedImagesList.size}):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(selectedImagesList.size) { index ->
                                val imgUrl = selectedImagesList[index]
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                ) {
                                    Image(
                                        painter = getBannerPainter(imgUrl),
                                        contentDescription = "Product Image $index",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedImagesList = selectedImagesList.toMutableList().apply { removeAt(index) }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(22.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .padding(2.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete image", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select Photos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showCustomUrlField = !showCustomUrlField
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Image URL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showCustomUrlField) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customImageUrlInput,
                                onValueChange = { customImageUrlInput = it },
                                label = { Text("Paste Image HTTP URL") },
                                placeholder = { Text("https://images.unsplash.com/...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Button(
                                onClick = {
                                    if (customImageUrlInput.isNotBlank()) {
                                        val url = customImageUrlInput.trim()
                                        if (!selectedImagesList.contains(url)) {
                                            selectedImagesList = selectedImagesList + url
                                        }
                                        customImageUrlInput = ""
                                        showCustomUrlField = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }

            // Core Product Information Card
            Text(
                text = "Product Information",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    
                    // 1. Product Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = if (it.isBlank()) "Product name is required" else null
                        },
                        enabled = !viewModel.editProductFromLiveStock,
                        label = { Text("Product Title *") },
                        leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = BrandGreenPrimary) },
                        isError = nameError != null,
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    nameError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                    // 2. Price and Stocks
                    OutlinedTextField(
                        value = if (calculatePriceByVariant) {
                            val minPrice = structuredVariants.mapNotNull { it.price }.minOrNull()
                            if (minPrice != null) {
                                priceStr = minPrice.toString()
                            }
                            priceStr
                        } else {
                            priceStr
                        },
                        onValueChange = {
                            priceStr = it
                            val value = it.toDoubleOrNull()
                            priceError = if (value == null || value <= 0) "Must be > 0" else null
                        },
                        enabled = !calculatePriceByVariant,
                        label = { Text(if (calculatePriceByVariant) "Price (ETB) - Calculated by Variant" else "Price (ETB) *") },
                        isError = priceError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth().testTag("product_price_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    priceError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                    // Auto Checkbox for Variant Price
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { calculatePriceByVariant = !calculatePriceByVariant }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = calculatePriceByVariant,
                            onCheckedChange = { calculatePriceByVariant = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Calculate price by variant",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enable custom pricing for each color, size, or spec",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            OutlinedTextField(
                                value = stockStr,
                                onValueChange = {
                                    stockStr = it
                                    val value = it.toIntOrNull()
                                    stockError = if (value == null || value < 0) "Must be >= 0" else null
                                },
                                enabled = (editingProduct == null),
                                label = { Text("Stock Quantity *") },
                                isError = stockError != null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                                modifier = Modifier.fillMaxWidth().testTag("product_stock_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            stockError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            if (editingProduct != null) {
                                Text("Stock is managed via inventory adjustments.", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Column(modifier = Modifier.weight(0.8f)) {
                            OutlinedTextField(
                                value = lowStockThreshold,
                                onValueChange = { input ->
                                    lowStockThreshold = input.filter { char -> char.isDigit() }
                                    val limit = lowStockThreshold.toIntOrNull()
                                    val availStock = stockStr.toIntOrNull() ?: 0
                                    lowStockError = when {
                                        limit == null || limit < 1 -> "Must be ≥ 1"
                                        availStock > 1 && limit >= availStock -> "Must be < stock ($availStock)"
                                        else -> null
                                    }
                                },
                                label = { Text("Low Stock Limit *") },
                                isError = lowStockError != null,
                                supportingText = {
                                    if (lowStockError != null) {
                                        Text(lowStockError!!, color = BrandRedTertiary, style = MaterialTheme.typography.labelSmall)
                                    } else {
                                        Text("Alert limit (≥ 1 & < stock)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                                modifier = Modifier.fillMaxWidth().testTag("product_low_stock_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // 3. Category & Subcategory Selectors (Side-by-Side Row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Category *", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !viewModel.editProductFromLiveStock) { showCategoryDialog = true }
                                    .testTag("select_category_trigger"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val catIcon = when (category) {
                                            "Electronics" -> Icons.Default.Devices
                                            "Fashion" -> Icons.Default.Checkroom
                                            "Home" -> Icons.Default.Home
                                            "Beauty" -> Icons.Default.Palette
                                            "Toys" -> Icons.Default.SportsEsports
                                            "Sport" -> Icons.Default.SportsSoccer
                                            "Book" -> Icons.Default.Book
                                            else -> Icons.Default.Category
                                        }
                                        Icon(catIcon, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                        Text(category.ifBlank { "None" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Category", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Subcategory *", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !viewModel.editProductFromLiveStock) { showSubcategoryDialog = true }
                                    .testTag("select_subcategory_trigger"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Layers, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(18.dp))
                                        Text(subcategory.ifBlank { "None" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Subcategory", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // 3b. Brand & Product State (Side-by-Side Row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Brand *", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !viewModel.editProductFromLiveStock) { showBrandDialog = true }
                                    .testTag("select_brand_trigger"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Label, contentDescription = null, tint = BrandRedTertiary, modifier = Modifier.size(18.dp))
                                        Text(brand.ifBlank { "None" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Brand", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Product State", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("New", "Used").forEach { stateOption ->
                                    val isSelected = stateNewOrUsed == stateOption
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .background(if (isSelected) BrandGreenPrimary.copy(alpha = 0.08f) else Color.Transparent)
                                            .clickable(enabled = !viewModel.editProductFromLiveStock) { stateNewOrUsed = stateOption }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stateOption,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3c. Description input placed directly under Brand + State
                    Text("Product Description *", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { input ->
                            val lineCount = input.count { it == '\n' } + 1
                            if (input.length <= 500 && lineCount <= 5) {
                                description = input
                            }
                        },
                        label = { Text("Product Description / Details (${description.length}/500)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 3d. Structured Product Variants Section
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Product Variants & Inventory Configuration",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "If this product has multiple variants (e.g. different colors, storage sizes, RAM, etc.), configure them below to manage their stock counts individually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Seed suggestion lists based on category
                    val suggestColors = when (category) {
                        "Electronics" -> listOf("Black", "Silver", "Midnight Blue", "Gold", "Titanium Gray", "Green", "Red")
                        "Fashion" -> listOf("Black", "White", "Navy Blue", "Crimson Red", "Olive Green", "Charcoal Gray", "Pink")
                        "Home" -> listOf("Oak Wood", "Walnut Wood", "White Gloss", "Matte Black", "Gray Fabric", "Beige")
                        else -> listOf("Red", "Blue", "Green", "Black", "White", "Silver", "Gold")
                    }

                    val suggestSizes = when (category) {
                        "Electronics" -> listOf("128GB", "256GB", "512GB", "1TB", "64GB")
                        "Fashion" -> listOf("S", "M", "L", "XL", "XXL", "38", "40", "42")
                        "Beauty" -> listOf("30ml", "50ml", "100ml", "150ml", "200ml")
                        "Book" -> listOf("Paperback", "Hardcover", "Ebook", "Audiobook")
                        else -> listOf("Standard", "Compact", "Deluxe", "Regular")
                    }

                    val suggestRAM = when (category) {
                        "Electronics" -> listOf("4GB RAM", "6GB RAM", "8GB RAM", "12GB RAM", "16GB RAM")
                        "Fashion" -> listOf("100% Cotton", "Polyester Blend", "Premium Denim", "Wool", "Silk")
                        "Beauty" -> listOf("All Skin Types", "Dry Skin", "Oily Skin", "SPF 30", "SPF 50+")
                        else -> listOf("N/A", "Standard Pack", "With Warranty", "No Accessories")
                    }

                    // Variant Entry Fields State (local temporary states)
                    var varColor by remember { mutableStateOf("") }
                    var varStorageSize by remember { mutableStateOf("") }
                    var varRamAttr by remember { mutableStateOf("") }
                    var varStockInput by remember { mutableStateOf("1") }

                    // Display config form
                    if (editingProduct == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add Variant Combo", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = varColor,
                                    onValueChange = { varColor = it },
                                    label = { Text(label1) },
                                    placeholder = { Text(placeholder1) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = varStorageSize,
                                    onValueChange = { varStorageSize = it },
                                    label = { Text(label2) },
                                    placeholder = { Text(placeholder2) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = varRamAttr,
                                    onValueChange = { varRamAttr = it },
                                    label = { Text(label3) },
                                    placeholder = { Text(placeholder3) },
                                    singleLine = true,
                                    modifier = Modifier.weight(if (calculatePriceByVariant) 1.0f else 1.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = varStockInput,
                                    onValueChange = { varStockInput = it },
                                    label = { Text("Stock") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(if (calculatePriceByVariant) 0.8f else 0.8f),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                if (calculatePriceByVariant) {
                                    OutlinedTextField(
                                        value = varPriceInput,
                                        onValueChange = { varPriceInput = it },
                                        label = { Text("Price (ETB)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            // Suggestions rows for variant entry
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Suggested ${label1}s:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                ) {
                                    suggestColors.forEach { col ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { varColor = col }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(col, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                
                                Text("Suggested ${label2}s:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                ) {
                                    suggestSizes.forEach { sz ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { varStorageSize = sz }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(sz, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Text("Suggested ${label3}s:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                ) {
                                    suggestRAM.forEach { ram ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { varRamAttr = ram }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(ram, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            val isVariantComboValid = varColor.isNotBlank() && varStorageSize.isNotBlank() && varRamAttr.isNotBlank() && varStockInput.isNotBlank() && (varStockInput.toIntOrNull() ?: 0) > 0 && (!calculatePriceByVariant || (varPriceInput.isNotBlank() && (varPriceInput.toDoubleOrNull() ?: 0.0) > 0.0))

                            if (!isVariantComboValid) {
                                Text(
                                    text = if (calculatePriceByVariant) "⚠️ All fields (${label1}, ${label2}, ${label3}, Stock > 0, and Price > 0) are required." else "⚠️ All fields (${label1}, ${label2}, ${label3}, and Stock > 0) are required to add a variant combination.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandRedTertiary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (isVariantComboValid) {
                                        val stockVal = varStockInput.toIntOrNull() ?: 0
                                        val priceVal = if (calculatePriceByVariant) varPriceInput.toDoubleOrNull() else null
                                        val newCombo = StructuredVariant(varColor.trim(), varStorageSize.trim(), varRamAttr.trim(), stockVal, priceVal)
                                        structuredVariants = structuredVariants + newCombo
                                        
                                        // Reset input values
                                        varColor = ""
                                        varStorageSize = ""
                                        varRamAttr = ""
                                        varStockInput = "1"
                                        varPriceInput = ""
                                    }
                                },
                                enabled = isVariantComboValid,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandGreenPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Variant Combination", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    }

                    // Configured variants list
                    if (structuredVariants.isNotEmpty()) {
                        Text("Configured Variant Combos:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            structuredVariants.forEachIndexed { idx, item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "🎨 ${label1}: ${item.color}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "💾 ${label2}: ${item.storage} | ⚙️ ${label3}: ${item.ram}" + (if (item.price != null) " | 💰 Price: ${item.price} ETB" else ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(BrandGreenPrimary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Stock: ${item.stock} units",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = BrandGreenPrimary
                                                )
                                            }
                                        }

                                        if (editingProduct == null) {
                                            IconButton(
                                                onClick = {
                                                    structuredVariants = structuredVariants.toMutableList().apply { removeAt(idx) }
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Variant", tint = BrandRedTertiary)
                                            }
                                        }
                                    }
                                }
                            }

                            // Dynamic inventory total display
                            val computedTotalStock = structuredVariants.sumOf { it.stock }
                            androidx.compose.runtime.LaunchedEffect(computedTotalStock) {
                                stockStr = computedTotalStock.toString()
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandGreenPrimary.copy(alpha = 0.1f))
                                    .border(1.dp, BrandGreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Inventory, contentDescription = null, tint = BrandGreenPrimary)
                                    Text(
                                        text = "Variant Inventory Total Stock: $computedTotalStock units (Automatically applied)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }
                    }

                    // 4. Unit Selector
                    Text("Measurement Unit *", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        units.forEach { ut ->
                            val isSelected = unitOfMeasurement == ut
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = !viewModel.editProductFromLiveStock) { unitOfMeasurement = ut }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ut,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Custom Unit Input Textfield if they want to specify a custom unit!
                    OutlinedTextField(
                        value = if (units.contains(unitOfMeasurement)) "" else unitOfMeasurement,
                        onValueChange = { if (it.isNotBlank()) unitOfMeasurement = it },
                        enabled = !viewModel.editProductFromLiveStock,
                        label = { Text("Or write your own measurement unit") },
                        placeholder = { Text("e.g. bottles, boxes, pairs") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth().testTag("product_custom_unit_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Promotional & Delivery Settings Card
            Text(
                text = "Promotions & Logistics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    
                    // Free Delivery Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Offer Free Delivery?", fontWeight = FontWeight.SemiBold)
                            }
                            Text(text = "We waive regional courier fees to incentivize quicker checkouts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isFreeDelivery,
                            onCheckedChange = { isFreeDelivery = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandGreenPrimary, checkedTrackColor = BrandGreenPrimary.copy(alpha = 0.3f))
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Discount Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Apply Promotional Discount?", fontWeight = FontWeight.SemiBold)
                            }
                            Text(text = "Deduct a percentage off the price displayed on customer shelves.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = hasDiscount,
                            onCheckedChange = { hasDiscount = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandGreenPrimary, checkedTrackColor = BrandGreenPrimary.copy(alpha = 0.3f))
                        )
                    }

                    if (hasDiscount) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discount Percent:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${discountPercent.toInt()}% OFF", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandRedTertiary)
                            }
                            Slider(
                                value = discountPercent,
                                onValueChange = { discountPercent = it },
                                valueRange = 5f..75f,
                                steps = 13, // 5%, 10%, 15% ... 75%
                                colors = SliderDefaults.colors(
                                    activeTrackColor = BrandRedTertiary,
                                    thumbColor = BrandRedTertiary
                                )
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Home Slider Spotlight Promo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Spotlight Home Promotion?", fontWeight = FontWeight.SemiBold)
                            }
                            Text(text = "Highlight this product on the main dashboard sliding banner.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isPromoted,
                            onCheckedChange = { isPromoted = it },
                            modifier = Modifier.testTag("promo_switch"),
                            colors = SwitchDefaults.colors(checkedThumbColor = BrandGreenPrimary, checkedTrackColor = BrandGreenPrimary.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Product Button
            Button(
                enabled = !isSaving,
                onClick = {
                    val cleanedVariants = if (calculatePriceByVariant) {
                        structuredVariants
                    } else {
                        structuredVariants.map { it.copy(price = null) }
                    }
                    val finalPrice = if (calculatePriceByVariant && cleanedVariants.isNotEmpty()) {
                        cleanedVariants.mapNotNull { it.price }.minOrNull() ?: priceStr.toDoubleOrNull()
                    } else {
                        priceStr.toDoubleOrNull()
                    }
                    val finalStock = if (cleanedVariants.isNotEmpty()) cleanedVariants.sumOf { it.stock } else (stockStr.toIntOrNull() ?: 0)
                    val finalVariantsStr = if (cleanedVariants.isNotEmpty()) serializeStructuredVariants(cleanedVariants) else variants
                    val finalLowStock = lowStockThreshold.toIntOrNull() ?: 3

                    val isNewProduct = (editingProduct == null)
                    if (name.isBlank()) {
                        nameError = "Product name is required"
                        android.widget.Toast.makeText(context, "Please correct errors before saving", android.widget.Toast.LENGTH_SHORT).show()
                    } else if (finalPrice == null || finalPrice <= 0) {
                        priceError = "Must be a positive number"
                        android.widget.Toast.makeText(context, "Please enter a valid positive price", android.widget.Toast.LENGTH_SHORT).show()
                    } else if (finalStock < 0) {
                        stockError = "Must be 0 or more"
                        android.widget.Toast.makeText(context, "Please enter a valid stock count", android.widget.Toast.LENGTH_SHORT).show()
                    } else if (finalLowStock < 1) {
                        lowStockError = "Must be ≥ 1"
                        android.widget.Toast.makeText(context, "Low stock limit must be at least 1.", android.widget.Toast.LENGTH_SHORT).show()
                    } else if (finalStock > 1 && finalLowStock >= finalStock) {
                        lowStockError = "Must be < stock ($finalStock)"
                        android.widget.Toast.makeText(context, "Low stock limit ($finalLowStock) must be less than available stock ($finalStock).", android.widget.Toast.LENGTH_SHORT).show()
                    } else if (isNewProduct && (category.isBlank() || category == "None" || subcategory.isBlank() || subcategory == "None" || brand.isBlank() || brand == "None")) {
                        android.widget.Toast.makeText(context, "Please complete all fields (Category, Subcategory, Brand) in the Product Information Card.", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        val urls = selectedImagesList.filter { it.startsWith("http") || it.startsWith("content://") || it.startsWith("file://") }
                        val presets = selectedImagesList.filter { !it.startsWith("http") && !it.startsWith("content://") && !it.startsWith("file://") }

                        val finalImageUrlName = urls.joinToString(",")
                        val finalImageResName = if (presets.isNotEmpty()) presets.joinToString(",") else if (urls.isEmpty()) "img_banner_1" else ""

                        isSaving = true
                        viewModel.saveProduct(
                            name = name,
                            price = finalPrice,
                            stock = finalStock,
                            category = category,
                            isPromoted = isPromoted,
                            imageResName = finalImageResName,
                            imageUrlName = finalImageUrlName,
                            description = description,
                            unitOfMeasurement = unitOfMeasurement,
                            stateNewOrUsed = stateNewOrUsed,
                            isFreeDelivery = isFreeDelivery,
                            discountPercent = if (hasDiscount) discountPercent.toInt() else 0,
                            brand = brand,
                            variants = finalVariantsStr,
                            subcategory = subcategory,
                            lowStock = finalLowStock,
                            onResult = { success ->
                                isSaving = false
                                if (success) {
                                    viewModel.fetchSellerProducts()
                                    android.widget.Toast.makeText(context, "Product saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    viewModel.navigateTo(AppScreen.MAIN)
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to save product on PocketBase. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_product_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving to PocketBase...", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish & Save Product", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    // -------------------------------------------------------------
    // SELECTOR DIALOGS FOR CATEGORY, SUBCATEGORY, AND BRAND
    // -------------------------------------------------------------
    val categoryToBrands = mapOf(
        "Electronics" to listOf("Apple", "Samsung", "Oppo", "Xiaomi", "Vivo", "Realme", "OnePlus", "Sony", "Dell", "HP", "Lenovo", "Asus"),
        "Fashion" to listOf("Nike", "Adidas", "Puma", "Zara", "H&M", "Uniqlo", "Levi's", "Gucci", "Prada", "Calvin Klein"),
        "Home" to listOf("IKEA", "Ashley Furniture", "Steelcase", "Herman Miller", "Wayfair", "La-Z-Boy"),
        "Beauty" to listOf("The Ordinary", "CeraVe", "La Roche-Posay", "Neutrogena", "Sephora", "L'Oreal", "MAC", "Estee Lauder"),
        "Toys" to listOf("Hasbro", "Mattel", "Ravensburger", "Asmodee", "LEGO", "Fisher-Price"),
        "Sport" to listOf("Peloton", "Bowflex", "Rogue Fitness", "Lululemon", "Nike Sport", "Adidas Performance", "Under Armour"),
        "Book" to listOf("Penguin Books", "HarperCollins", "Simon & Schuster", "Macmillan Publishers", "Vintage Books", "Scholastic")
    )

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Category", fontWeight = FontWeight.Bold, color = BrandGreenPrimary) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Search Bar
                    OutlinedTextField(
                        value = categorySearchQuery,
                        onValueChange = { categorySearchQuery = it },
                        label = { Text("Search category...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 2. Custom Option Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customCategoryInput,
                            onValueChange = { customCategoryInput = it },
                            label = { Text("Custom category") },
                            placeholder = { Text("e.g. Garden") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (customCategoryInput.isNotBlank()) {
                                    category = customCategoryInput.trim()
                                    subcategory = ""
                                    brand = ""
                                    showCategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Use", fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3. Category options list
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    val filteredCategories = categories.filter {
                        it.contains(categorySearchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredCategories.isEmpty()) {
                            Text("No categories found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            filteredCategories.forEach { cat ->
                                val isSelected = category == cat
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BrandGreenPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            category = cat
                                            showCategoryDialog = false
                                            
                                            // Auto-recommend preset image matching category
                                            val match = imagePresets.firstOrNull { it.first == cat }
                                            if (match != null && !selectedImagesList.contains(match.second)) {
                                                selectedImagesList = selectedImagesList + match.second
                                            }
                                            
                                            if (editingProduct != null) {
                                                // Auto-recommend subcategory & reset brand
                                                val subcats = categoryToSubcategories[cat] ?: emptyList()
                                                if (subcats.isNotEmpty()) {
                                                    subcategory = subcats.first()
                                                } else {
                                                    subcategory = ""
                                                }
                                                
                                                val brandsList = categoryToBrands[cat] ?: emptyList()
                                                if (brandsList.isNotEmpty()) {
                                                    brand = brandsList.first()
                                                } else {
                                                    brand = ""
                                                }
                                            } else {
                                                subcategory = ""
                                                brand = ""
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                        color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = BrandGreenPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Close", color = BrandGreenPrimary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showSubcategoryDialog) {
        val subcats = categoryToSubcategories[category] ?: emptyList()
        AlertDialog(
            onDismissRequest = { showSubcategoryDialog = false },
            title = { Text("Select Subcategory", fontWeight = FontWeight.Bold, color = BrandGreenPrimary) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Search Bar
                    OutlinedTextField(
                        value = subcategorySearchQuery,
                        onValueChange = { subcategorySearchQuery = it },
                        label = { Text("Search subcategory...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 2. Custom Option Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customSubcategoryInput,
                            onValueChange = { customSubcategoryInput = it },
                            label = { Text("Custom subcategory") },
                            placeholder = { Text("e.g. Smart Watch") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (customSubcategoryInput.isNotBlank()) {
                                    subcategory = customSubcategoryInput.trim()
                                    showSubcategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Use", fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3. Option list
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    val filteredSubcats = subcats.filter {
                        it.contains(subcategorySearchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredSubcats.isEmpty()) {
                            Text("No subcategories found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            filteredSubcats.forEach { sub ->
                                val isSelected = subcategory == sub
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BrandGreenPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            subcategory = sub
                                            showSubcategoryDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                        color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = BrandGreenPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubcategoryDialog = false }) {
                    Text("Close", color = BrandGreenPrimary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showBrandDialog) {
        val brandsList = categoryToBrands[category] ?: listOf("Generic", "Samsung", "Apple", "Huawei", "Nike", "Adidas")
        AlertDialog(
            onDismissRequest = { showBrandDialog = false },
            title = { Text("Select Brand", fontWeight = FontWeight.Bold, color = BrandGreenPrimary) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Search Bar
                    OutlinedTextField(
                        value = brandSearchQuery,
                        onValueChange = { brandSearchQuery = it },
                        label = { Text("Search brand...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 2. Custom Option Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customBrandInput,
                            onValueChange = { customBrandInput = it },
                            label = { Text("Custom brand") },
                            placeholder = { Text("e.g. MyBrand") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (customBrandInput.isNotBlank()) {
                                    brand = customBrandInput.trim()
                                    showBrandDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Use", fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3. Option list
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    val filteredBrands = brandsList.filter {
                        it.contains(brandSearchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredBrands.isEmpty()) {
                            Text("No brands found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            filteredBrands.forEach { b ->
                                val isSelected = brand == b
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BrandGreenPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            brand = b
                                            showBrandDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = b,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                        color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = BrandGreenPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBrandDialog = false }) {
                    Text("Close", color = BrandGreenPrimary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun PaymentConfirmationDialog(
    order: SellerOrderEntity,
    viewModel: AppViewModel,
    activeUser: com.example.data.UserEntity?,
    merchantStoreName: String,
    onDismissRequest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var paymentReferenceInput by remember { mutableStateOf(order.paymentReference ?: "") }

    val refCode = if (order.paymentMethod.equals("Telebirr", ignoreCase = true)) {
        "MP-8290${order.id}"
    } else {
        "TXN-4920${order.id}"
    }

    val currentBizName = activeUser?.businessName?.takeIf { it.isNotBlank() }
        ?: merchantStoreName.takeIf { it.isNotBlank() }
        ?: order.sellerName.ifBlank { "Bole Traditional Boutique" }

    val userStoreLoc = activeUser?.storeLocation?.takeIf { it.lat != 0.0 || it.lon != 0.0 }
    val orderStoreLoc = order.storeLocation.takeIf { it.lat != 0.0 || it.lon != 0.0 }
    val currentStoreLoc: com.example.data.StoreLocation = userStoreLoc
        ?: orderStoreLoc
        ?: com.example.data.StoreLocation(lat = 8.9806, lon = 38.7578)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BrandGreenPrimary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = BrandGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Confirm Payment",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Info Banner
                Surface(
                    color = BrandGreenPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BrandGreenPrimary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BrandGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Please verify that you have received payment in your account before confirming this order.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Payment Details Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Order ID Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Order ID",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "#${order.id}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Total Amount Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Amount",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${order.totalPrice} ETB",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = BrandGreenPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))

                        // Expected Payment Ref Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expected Payment Ref (${order.paymentMethod})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = refCode,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandGreenPrimary
                            )
                        }

                        if (order.buyerName.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Customer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = order.buyerName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }

                // Required Payment Reference Input Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Payment Reference / Txn ID *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedTextField(
                        value = paymentReferenceInput,
                        onValueChange = { paymentReferenceInput = it },
                        placeholder = { Text("e.g. TXN98234710 or MP203491") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_reference_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreenPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val extraFields: Map<String, Any> = mapOf(
                            "sellerName" to currentBizName,
                            "seller_name" to currentBizName,
                            "storeLocation" to currentStoreLoc,
                            "paymentReference" to paymentReferenceInput.trim()
                        )
                        viewModel.updateOrderStatus(order, "CONFIRMED", extraFields)
                        android.widget.Toast.makeText(
                            context,
                            "Payment confirmed! Order #${order.id} status updated to CONFIRMED.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        onDismissRequest()
                    },
                    enabled = paymentReferenceInput.trim().equals(refCode, ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_payment_modal_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm Payment")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun CancelOrderDialog(
    order: SellerOrderEntity,
    viewModel: AppViewModel,
    onDismissRequest: () -> Unit
) {
    var cancelReasonInput by remember { mutableStateOf("") }
    var cancelRefundReceiptState by remember { mutableStateOf<String?>(null) }
    var cancelRefundReferenceInput by remember { mutableStateOf(order.paymentBackReference ?: "") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Cancel Order #${order.id}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Cancellation of orders requires an official money-back refund receipt upload and a valid reason.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                
                Text("1. Reason for Cancellation *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = cancelReasonInput,
                    onValueChange = { cancelReasonInput = it },
                    placeholder = { Text("e.g. Out of stock, price changes...") },
                    modifier = Modifier.fillMaxWidth().testTag("cancel_reason_input"),
                    singleLine = true
                )
                
                Text("2. Money-Back Receipt Upload *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                
                if (cancelRefundReceiptState == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "No Refund Receipt Uploaded",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Choose a simulated transfer confirmation below:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Telebirr Txn" to "telebirr_refund_923.png",
                                    "CBE Transfer" to "cbe_refund_812.png",
                                    "Cash Return" to "cash_refund_slip.png"
                                ).forEach { (label, file) ->
                                    Button(
                                        onClick = { cancelRefundReceiptState = file },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BrandGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = BrandGreenPrimary.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = BrandGreenPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Receipt Attached",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = BrandGreenPrimary
                                    )
                                    Text(
                                        cancelRefundReceiptState ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { cancelRefundReceiptState = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove receipt",
                                    tint = BrandRedTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Text("3. Money-Back Payment Reference", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = cancelRefundReferenceInput,
                    onValueChange = { cancelRefundReferenceInput = it },
                    placeholder = { Text("e.g. TXN9812408 or REF82910") },
                    modifier = Modifier.fillMaxWidth().testTag("cancel_refund_ref_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val isFormValid = cancelReasonInput.isNotBlank() && cancelRefundReceiptState != null
            Button(
                onClick = {
                    if (isFormValid) {
                        viewModel.cancelOrderWithReason(order, cancelReasonInput, cancelRefundReceiptState, cancelRefundReferenceInput.takeIf { it.isNotBlank() })
                        onDismissRequest()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) BrandRedTertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                enabled = isFormValid,
                modifier = Modifier.testTag("confirm_cancel_btn")
            ) {
                Text("Confirm Cancel", color = if (isFormValid) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Keep Order")
            }
        }
    )
}

@Composable
fun ReadyConfirmDialog(
    order: SellerOrderEntity,
    viewModel: AppViewModel,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Fulfillment Confirmation") },
        text = { Text("Are you sure Order #${order.id} is ready for pickup?") },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateOrderStatus(order, "READY_FOR_PICKUP")
                    onDismissRequest()
                },
                modifier = Modifier.testTag("ready_confirm_yes")
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("ready_confirm_no")
            ) {
                Text("No")
            }
        }
    )
}

@Composable
fun CourierVerificationDialog(
    order: SellerOrderEntity,
    viewModel: AppViewModel,
    onDismissRequest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var courierVerificationCodeInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Verify Courier Identity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Order #${order.id} is assigned to a delivery partner. Please enter the 6-digit verification code provided by the courier to verify they accepted this job.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = courierVerificationCodeInput,
                    onValueChange = { courierVerificationCodeInput = it.take(6) },
                    label = { Text("6-Digit Courier Code") },
                    placeholder = { Text("e.g. 548291") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("courier_verification_input"),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.verifyDeliveryPickup(
                        orderId = order.id,
                        deliveryCode = courierVerificationCodeInput,
                        onSuccess = {
                            onDismissRequest()
                            android.widget.Toast.makeText(context, "Courier Identity Verified! Order handed over for delivery.", android.widget.Toast.LENGTH_LONG).show()
                        },
                        onError = { errorMsg ->
                            android.widget.Toast.makeText(context, "Verification Failed! $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.testTag("courier_verify_btn")
            ) {
                Text("Verify & Handover")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BuyerVerificationDialog(
    order: SellerOrderEntity,
    viewModel: AppViewModel,
    onDismissRequest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var buyerVerificationCodeInput by remember { mutableStateOf("") }
    val expectedCode = order.buyerVerifCode ?: ((order.id.hashCode() * 179) % 900000 + 100000).toString()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Verify Buyer Collection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Order #${order.id} is a Self-Collect (Buyer Pickup) order. Please ask the buyer for their 6-digit verification code to confirm handover.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = buyerVerificationCodeInput,
                    onValueChange = { buyerVerificationCodeInput = it.take(6) },
                    label = { Text("6-Digit Buyer Code") },
                    placeholder = { Text("e.g. 123456") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("buyer_verification_input"),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (buyerVerificationCodeInput == expectedCode) {
                        viewModel.updateOrderStatus(order, "DELIVERED")
                        onDismissRequest()
                        android.widget.Toast.makeText(context, "Buyer Code Verified! Order handed over successfully.", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(context, "Verification Failed! Code is incorrect.", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.testTag("buyer_verify_btn")
            ) {
                Text("Verify & Handover")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}



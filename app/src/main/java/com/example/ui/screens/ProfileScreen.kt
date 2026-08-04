package com.example.ui.screens


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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions

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

import com.example.data.OrderEntity

import com.example.data.ProductEntity

import com.example.data.UserEntity
import com.example.data.LoginHistoryEntity

import com.example.ui.theme.BrandGoldSecondary

import com.example.ui.theme.BrandGreenPrimary

import com.example.ui.theme.BrandRedTertiary

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch


import com.example.BuildConfig

import com.example.ui.*

import com.example.ui.screens.*

import com.example.ui.components.*

import com.example.ui.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val sellerProfile by viewModel.sellerProfile.collectAsStateWithLifecycle()
    val orders by viewModel.buyerOrders.collectAsStateWithLifecycle()
    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDark = when (themeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()

    val activeSubScreen by viewModel.profileSubScreen.collectAsStateWithLifecycle()
    val ordersBackTarget by viewModel.ordersBackTarget.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Navigation and sub-screen routing container
    Box(modifier = Modifier.fillMaxSize()) {
        when (activeSubScreen) {
            "MENU" -> {
                ProfileMenuMain(
                    viewModel = viewModel,
                    activeUser = activeUser,
                    onNavigateToSub = { viewModel.navigateToProfileSub(it) },
                    onShowLogout = { showLogoutDialog = true }
                )
            }
            "MY_ORDERS" -> {
                MyOrdersSubPage(
                    viewModel = viewModel,
                    orders = orders,
                    onBack = { 
                        if (ordersBackTarget == "HOME") {
                            viewModel.selectTab(MainTab.HOME)
                            viewModel.navigateToProfileSub("MENU")
                            viewModel.ordersBackTarget.value = "MENU"
                        } else {
                            viewModel.navigateToProfileSub("MENU")
                        }
                    }
                )
            }
            "ORDER_DETAIL" -> {
                val selectedOrder by viewModel.selectedOrderDetail.collectAsStateWithLifecycle()
                selectedOrder?.let { ord ->
                    OrderDetailSubPage(
                        viewModel = viewModel,
                        order = ord,
                        onBack = { viewModel.navigateToProfileSub("MY_ORDERS") }
                    )
                } ?: viewModel.navigateToProfileSub("MY_ORDERS")
            }
            "ADDRESS_MANAGEMENT" -> {
                AddressManagementSubPage(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateToProfileSub(viewModel.addressBackTarget.value) }
                )
            }
            "ACCOUNT" -> {
                AccountSubPage(
                    viewModel = viewModel,
                    activeUser = activeUser,
                    onBack = { viewModel.navigateToProfileSub("MENU") }
                )
            }
            "SETTINGS" -> {
                SettingsSubPage(
                    viewModel = viewModel,
                    activeUser = activeUser,
                    currentLang = currentLang,
                    isDark = isDark,
                    isNotificationsEnabled = isNotificationsEnabled,
                    onNavigateToSub = { viewModel.navigateToProfileSub(it) },
                    onBack = { viewModel.navigateToProfileSub("MENU") }
                )
            }
            "CHANGE_PASSWORD" -> {
                ChangePasswordSubPage(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateToProfileSub("SETTINGS") }
                )
            }
            "LOGIN_HISTORY" -> {
                LoginHistorySubPage(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateToProfileSub("SETTINGS") }
                )
            }
            
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(text = "Confirm Logout", fontWeight = FontWeight.Bold) },
                text = { Text(text = "Are you sure you want to log out of E-Suuq? You will need to sign in again to access your account.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logoutUser()
                            viewModel.navigateTo(AppScreen.ONBOARDING)
                            
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRedTertiary)
                    ) {
                        Text(text = "Log Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(text = "Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}


@Composable
fun MyOrdersSubPage(
    viewModel: AppViewModel,
    orders: List<OrderEntity>,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Processing") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedSortOrder by remember { mutableStateOf("Newest First") }
    var selectedDateFilter by remember { mutableStateOf("All Time") }

    var selectedStartDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedEndDate by remember { mutableStateOf<Calendar?>(null) }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }

    // Sync selectedTab and selectedStatusFilter
    LaunchedEffect(selectedTab) {
        selectedStatusFilter = when (selectedTab) {
            "Delivered" -> "Delivered"
            "Cancelled" -> "Cancelled"
            else -> "Processing"
        }
    }

    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()

    // ✅ Initialize real-time connections when profile screen is active
    DisposableEffect(activeUser?.id) {
        val uid = activeUser?.id
        val role = activeUser?.role ?: "BUYER"
        if (!uid.isNullOrBlank()) {
            viewModel.initRealtimeForUser(uid, role)
        }
        onDispose { }
    }

    // ✅ OPTIMIZED: Using derivedStateOf for comprehensive filtering
    // This only recalculates when dependencies change (orders, searchQuery, filters, etc.)
    val filteredOrders by derivedStateOf {
        var result = orders

        // 1. Status Filter
        if (selectedStatusFilter != "All") {
            val matchingStatus = when (selectedStatusFilter) {
                "Delivered" -> listOf("DELIVERED")
                "Cancelled" -> listOf("CANCELLED")
                else -> listOf("PENDING", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY", "SHIPPED")
            }
            result = result.filter { it.status in matchingStatus }
        }

        // 2. Comprehensive Search Filter
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            result = result.filter {
                it.id.toString().contains(query, ignoreCase = true) ||
                "ES-2026-0${it.id}".contains(query, ignoreCase = true) ||
                it.productNames.contains(query, ignoreCase = true) ||
                "Seller Name".contains(query, ignoreCase = true) ||
                "Buyer Name".contains(query, ignoreCase = true) ||
                "Buyer Phone".contains(query, ignoreCase = true)
            }
        }

        // 3. Date Range Filter
        if (selectedStartDate != null && selectedEndDate != null) {
            val startMs = clearCalendarTime(selectedStartDate!!).timeInMillis
            val endCal = Calendar.getInstance().apply {
                timeInMillis = selectedEndDate!!.timeInMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endMs = endCal.timeInMillis
            result = result.filter { 0L in startMs..endMs }
        } else if (selectedStartDate != null) {
            val startMs = clearCalendarTime(selectedStartDate!!).timeInMillis
            result = result.filter { 0L >= startMs }
        } else {
            val now = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L
            result = when (selectedDateFilter) {
                "Today" -> result.filter { now - 0L <= oneDayMs }
                "Past 7 Days" -> result.filter { now - 0L <= 7 * oneDayMs }
                "Past 30 Days" -> result.filter { now - 0L <= 30 * oneDayMs }
                else -> result
            }
        }

        // 4. Sorting Engine
        result = when (selectedSortOrder) {
            "Oldest First" -> result.sortedBy { 0L }
            "Price: Low to High" -> result.sortedBy { it.totalPrice }
            "Price: High to Low" -> result.sortedByDescending { it.totalPrice }
            else -> result.sortedByDescending { 0L }
        }

        result
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header with Address Icon on the RIGHT side of the App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .testTag("orders_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My Orders",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track progress & purchase history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    viewModel.addressBackTarget.value = "MY_ORDERS"
                    viewModel.navigateToProfileSub("ADDRESS_MANAGEMENT")
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .testTag("orders_address_btn")
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Manage Addresses", tint = BrandGreenPrimary)
            }
        }

        // Search & Filter Trigger Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("orders_search_field"),
                placeholder = { Text("Search by Order ID...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .testTag("orders_filter_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter Orders",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Date Picker quick filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val dateLabelText = if (selectedStartDate != null && selectedEndDate != null) {
                "${formatCalendarDate(selectedStartDate!!)} - ${formatCalendarDate(selectedEndDate!!)}"
            } else if (selectedStartDate != null) {
                "From ${formatCalendarDate(selectedStartDate!!)}"
            } else {
                selectedDateFilter
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showDateDialog = true }
                        .testTag("orders_date_filter_btn")
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Date Range", modifier = Modifier.size(16.dp), tint = BrandGreenPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Date: $dateLabelText",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedStartDate != null || selectedDateFilter != "All Time") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Date Filter",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                selectedStartDate = null
                                selectedEndDate = null
                                selectedDateFilter = "All Time"
                            }
                            .testTag("orders_clear_date_btn"),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Text(
                text = "Sort: $selectedSortOrder",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tabs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Processing", "Delivered", "Cancelled").forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedTab = tab }
                        .background(if (isSelected) BrandGreenPrimary else Color.Transparent)
                        .padding(vertical = 10.dp)
                        .testTag("orders_tab_pill_$tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Orders List - ✅ OPTIMIZED with keys
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No orders found matching criteria",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ✅ OPTIMIZED: Added key for better recomposition
                items(
                    items = filteredOrders,
                    key = { order -> order.id }
                ) { order ->
                    OrderCard(
                        order = order,
                        isProcessing = order.status in listOf("PENDING", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY", "SHIPPED"),
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Filter Dialog (Status, Sort)
    if (showFilterSheet) {
        AlertDialog(
            onDismissRequest = { showFilterSheet = false },
            title = { Text("Filter & Sort Orders", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Sort Options
                    Text("Sort By", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                    listOf("Newest First", "Oldest First", "Price: Low to High", "Price: High to Low").forEach { sortOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSortOrder = sortOption }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSortOrder == sortOption,
                                onClick = { selectedSortOrder = sortOption },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandGreenPrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = sortOption, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Status Options
                    Text("Filter by Status", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                    listOf("All", "Delivered", "Processing", "Cancelled").forEach { statusOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStatusFilter = statusOption }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStatusFilter == statusOption,
                                onClick = { selectedStatusFilter = statusOption },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandGreenPrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = statusOption, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFilterSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                ) {
                    Text("Apply Filters")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ✅ OPTIMIZED: Using UnifiedCalendarDialog
    if (showDateDialog) {
        UnifiedCalendarDialog(
            initialStartDate = selectedStartDate,
            initialEndDate = selectedEndDate,
            onConfirm = { start, end, label ->
                selectedStartDate = start
                selectedEndDate = end
                selectedDateFilter = label
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false },
            title = "Select Date Range",
            subtitle = "Filter orders by date",
            showPresets = true
        )
    }
}


@Composable
fun OrderDetailSubPage(
    viewModel: AppViewModel,
    order: OrderEntity,
    onBack: () -> Unit
) {
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    var showBuyerCancelDialog by remember { mutableStateOf(false) }

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
                    onClick = onBack,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =  "Order Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text =  "view your order information",
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card with status
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
                            text = "Order ID: #ES-2026-0${order.id}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val badgeColor = when (order.status) {
                            "DELIVERED" -> Color(0xFFD4EDDA)
                            "CANCELLED" -> Color(0xFFF8D7DA)
                            else -> Color(0xFFFFF3CD)
                        }
                        val badgeTextColor = when (order.status) {
                            "DELIVERED" -> Color(0xFF155724)
                            "CANCELLED" -> Color(0xFF721C24)
                            else -> Color(0xFF856404)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (order.status == "SHIPPED") "OUT FOR DELIVERY" else order.status,
                                color = badgeTextColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Placed on: ${formatPurchaseDate(0L)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fulfillment Type Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Fulfillment Type",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    val isSelf = order.isSelfPickup
                    val fulfillmentText = if (isSelf) {
                        "Self Pickup (Buyer Pickup)"
                    } else {
                        "Courier Delivery"
                    }
                    Text(
                        text = fulfillmentText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (!isSelf) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Courier: ${order.courierName ?: "Unassigned"}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Plate: ${order.courierPlate ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Store Info & Items Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Seller / Merchant",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandGreenPrimary
                    )
                    Text(
                        text = if (order.sellerName.isNotBlank()) order.sellerName else "Sheger Electronics",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Items Ordered",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (order.items.isNotEmpty()) {
                        order.items.forEachIndexed { index, item ->
                            val subtotalVal = if (item.subtotal > 0.0) item.subtotal else (item.unitPrice * item.quantity)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = BrandGoldSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.productName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Qty: ${item.quantity}  •  Unit Price: ${String.format("%.1f", item.unitPrice)} ETB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Subtotal: ${String.format("%.1f ETB", subtotalVal)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BrandGreenPrimary
                                )
                            }
                            if (index < order.items.size - 1) {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    } else {
                        val items = order.productNames.split(",")
                        items.forEachIndexed { index, itemName ->
                            val cleanName = itemName.trim()
                            if (cleanName.isNotEmpty()) {
                                val itemPrice = if (items.size == 1) order.totalPrice else (order.totalPrice / items.size)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = BrandGoldSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cleanName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Qty: 1  •  Unit Price: ${String.format("%.1f", itemPrice)} ETB",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Subtotal: ${String.format("%.1f ETB", itemPrice)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = BrandGreenPrimary
                                    )
                                }
                                if (index < items.size - 1) {
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }

            // Summary Card (Total & Payment)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Payment Method", style = MaterialTheme.typography.bodyMedium)
                        Text(text = order.paymentMethod, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    
                    // ONLY show Delivery Price & Responsibility if NOT self pickup
                    if (!order.isSelfPickup) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        val isFreeDelivery = order.deliveryPrice == 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Delivery Price & Responsibility", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = String.format("%.1f ETB", order.deliveryPrice), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Delivery Paid By", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isFreeDelivery) "Paid by Seller (Product has Free Delivery) 🚛" else "Paid by Buyer 🚚",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isFreeDelivery) BrandRedTertiary else BrandGreenPrimary
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total Paid", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = String.format("%.1f ETB", order.totalPrice),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = BrandGreenPrimary
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(
                            text = if (order.isSelfPickup) "Self-Pickup Store Address" else "Delivery Address",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (order.isSelfPickup) "Store - ${order.storeLocation}" else "Home - ${order.shippingAddress.let { "Lat: ${it.lat}, Lon: ${it.lon}" }}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (order.isSelfPickup) {
                Button(
                    onClick = {
                        viewModel.setTrackingOrder(order)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("track_store_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Track Store / Go to Store",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            if (order.status != "CANCELLED") {
                if (order.status in listOf("SHIPPED", "DELIVERED")) {
                    val courierName = order.courierName ?: "Guled Farah"
                    val courierPlate = order.courierPlate ?: "A.A-B48204"
                    val initials = if (courierName.contains(" ")) {
                        val parts = courierName.split(" ")
                        "${parts[0].firstOrNull() ?: 'G'}${parts[1].firstOrNull() ?: 'F'}"
                    } else {
                        courierName.take(2).uppercase()
                    }

                    // Delivery Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Delivery Information",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val courierRatings by viewModel.courierRatings.collectAsStateWithLifecycle()
                            val currentRating = courierRatings[order.id] ?: 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Courier avatar
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF2E7D32), Color(0xFF4CAF50))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(courierName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Vehicle • Plate: $courierPlate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    if (order.status == "DELIVERED") {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val isCourierUser = activeUser?.role == "DELIVERY"
                                        val subtitleText = if (isCourierUser) {
                                            if (currentRating > 0) "Level 4 (Elite Courier) • ${1200 + currentRating} XP (+$currentRating XP! 🌟)" else "Level 4 (Elite Courier) • 1,200 XP"
                                        } else {
                                            if (currentRating > 0) "Level 4 (Elite Courier) • +$currentRating XP given! 🌟" else "Level 4 (Elite Courier)"
                                        }
                                        Text(
                                            text = subtitleText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = BrandGoldSecondary
                                        )
                                    }
                                }
                                IconButton(onClick = { /* Simulated dial action */ }) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call Courier", tint = BrandGreenPrimary)
                                }
                            }

                            if (order.status == "SHIPPED") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = BrandGoldSecondary.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, BrandGoldSecondary.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Your Delivery Handover Code",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = order.buyerVerifCode ?: "123456",
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
                                            color = BrandGreenPrimary
                                        )
                                        Text(
                                            text = "Give this secure 6-digit code to the courier to confirm handover of your order.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            val isDelivery = !order.isSelfPickup
                            if (order.status == "READY_FOR_PICKUP" && !isDelivery) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("buyer_pickup_code_card"),
                                    colors = CardDefaults.cardColors(containerColor = BrandGoldSecondary.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, BrandGoldSecondary.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Your Store Pickup Verification Code",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = order.buyerVerifCode ?: "123456",
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
                                            color = BrandGreenPrimary
                                        )
                                        Text(
                                            text = "Present this secure 6-digit code to the merchant to verify and pick up your order.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            if (order.status == "DELIVERED") {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(10.dp))

                                val isHimself = activeUser?.role == "DELIVERY" && activeUser?.fullName?.contains("Desta", ignoreCase = true) == true

                                // Star Rating Action
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val rateTitleText = when {
                                        isHimself -> "As the assigned courier, you cannot rate yourself."
                                        currentRating > 0 -> "Thank you for rating $courierName!"
                                        else -> "Rate $courierName to grow his courier experience:"
                                    }
                                    Text(
                                        text = rateTitleText,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isHimself) BrandRedTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    if (currentRating == 0) {
                                        // Show interactive star buttons for rating one time
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (star in 1..5) {
                                                IconButton(
                                                    onClick = {
                                                        if (!isHimself) {
                                                            viewModel.rateCourier(order.id, star)
                                                        }
                                                    },
                                                    enabled = !isHimself,
                                                    modifier = Modifier.size(36.dp).testTag("rate_courier_star_$star")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Rate $star Stars",
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isHimself) 0.08f else 0.2f),
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Once rated, stars are removed and replaced with a rated confirmation badge
                                        Box(
                                            modifier = Modifier
                                                .background(BrandGreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                .border(1.dp, BrandGreenPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "Your Rating: $currentRating / 5 Stars",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Estimated Arrival", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "25 - 35 mins", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            if (order.status == "SHIPPED") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = "Current Courier Location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Near Kirkos Sub-City, Addis Ababa", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }

                    // Track Button
                    if (order.status == "SHIPPED") {
                        Button(
                            onClick = { viewModel.setTrackingOrder(order) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("detail_track_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Live Track Order", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                } else {
                    // PREPARING or READY_FOR_PICKUP
                        val isDelivery = !order.isSelfPickup
                        if (order.status == "READY_FOR_PICKUP" && !isDelivery) {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("buyer_pickup_code_card_${order.id}"),
                                colors = CardDefaults.cardColors(containerColor = BrandGoldSecondary.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, BrandGoldSecondary.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Your Store Pickup Verification Code",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = order.buyerVerifCode ?: "123456",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
                                        color = BrandGreenPrimary
                                    )
                                    Text(
                                        text = "Present this secure 6-digit code to the merchant to verify and pick up your order.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (order.status == "PENDING") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, BrandGoldSecondary.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Pending Merchant Confirmation",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Your order has been placed and is waiting for the seller to confirm. You can cancel before it is confirmed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = { showBuyerCancelDialog = true },
                                        modifier = Modifier.fillMaxWidth().testTag("buyer_cancel_order_btn"),
                                        border = BorderStroke(1.dp, BrandRedTertiary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRedTertiary)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cancel Order", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "Your order is being prepared and packed by the merchant. Delivery details and live tracking will become available as soon as the package is shipped.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, BrandRedTertiary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = BrandRedTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Order Cancelled",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandRedTertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reason for Cancellation:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = order.cancellationReason ?: "User requested cancellation / out of stock.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (showBuyerCancelDialog) {
            AlertDialog(
                onDismissRequest = { showBuyerCancelDialog = false },
                title = { Text("Cancel Order", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to cancel order #${order.id}? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showBuyerCancelDialog = false
                            viewModel.cancelOrder(
                                orderId = order.id,
                                reason = "Cancelled by buyer",
                                onSuccess = { onBack() },
                                onError = { }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRedTertiary)
                    ) {
                        Text("Yes, Cancel Order")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBuyerCancelDialog = false }) {
                        Text("Keep Order")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}


@Composable
fun AddressManagementSubPage(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val addresses by viewModel.savedAddresses.collectAsStateWithLifecycle()
    var isFormOpen by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<AddressEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<AddressEntity?>(null) }

    if (isFormOpen) {
        AddressFormScreen(
            viewModel = viewModel,
            addressToEdit = editingAddress,
            onDismiss = {
                isFormOpen = false
                editingAddress = null
            }
        )
    } else {
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
                    onClick = onBack,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =  "Address Book",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text =  "manage your delivery eaddress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingAddress = null
                        isFormOpen = true
                    },
                    containerColor = BrandGreenPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_address_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Address")
                }
            }
        ) { padding ->
            if (addresses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Addresses Saved", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Tap + to add your first delivery address", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(addresses) { address ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val icon = when (address.label) {
                                            "Home" -> Icons.Default.Home
                                            "Work" -> Icons.Default.Work
                                            else -> Icons.Default.LocationOn
                                        }
                                        Icon(icon, contentDescription = address.label, tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = address.label,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    if (address.isDefault) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BrandGreenPrimary.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Primary",
                                                color = BrandGreenPrimary,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "${address.region}, ${address.zone} Zone, ${address.city}, Kebele ${address.kebele}, ${address.streetHouseNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (address.latitude != 0.0 && address.longitude != 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Geotagged: ${String.format("%.4f", address.latitude)}, ${String.format("%.4f", address.longitude)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!address.isDefault) {
                                        TextButton(
                                            onClick = { viewModel.setAddressAsDefault(address.id) },
                                            modifier = Modifier.testTag("set_default_btn_${address.id}")
                                        ) {
                                            Text("Set as Primary", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingAddress = address
                                                isFormOpen = true
                                            },
                                            modifier = Modifier.testTag("edit_address_${address.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Address", tint = MaterialTheme.colorScheme.outline)
                                        }

                                        IconButton(
                                            onClick = { showDeleteConfirm = address },
                                            modifier = Modifier.testTag("delete_address_${address.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Address", tint = Color.Red)
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

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Saved Address", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${showDeleteConfirm?.label}' address from your profile?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm?.let { viewModel.deleteAddress(it.id) }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = BrandGreenPrimary)
                }
            }
        )
    }
}


@Composable
fun AddressFormScreen(
    viewModel: AppViewModel,
    addressToEdit: AddressEntity?,
    onDismiss: () -> Unit
) {
    var labelChoice by remember { mutableStateOf(if (addressToEdit?.label in listOf("Home", "Work")) addressToEdit?.label ?: "Home" else "Other") }
    var customLabel by remember { mutableStateOf(if (addressToEdit?.label !in listOf("Home", "Work")) addressToEdit?.label ?: "" else "") }

    var region by remember { mutableStateOf(addressToEdit?.region ?: "Addis Ababa") }
    var zone by remember { mutableStateOf(addressToEdit?.zone ?: "Bole") }
    var city by remember { mutableStateOf(addressToEdit?.city ?: "Addis Ababa") }
    var kebele by remember { mutableStateOf(addressToEdit?.kebele ?: "") }
    var streetHouseNumber by remember { mutableStateOf(addressToEdit?.streetHouseNumber ?: "") }

    var pinLat by remember { mutableStateOf(addressToEdit?.latitude ?: 9.02) }
    var pinLon by remember { mutableStateOf(addressToEdit?.longitude ?: 38.75) }
    var showMapPicker by remember { mutableStateOf(false) }

    val regions = listOf("Addis Ababa", "Amhara", "Oromia", "Tigray", "SNNPR", "Sidama")
    val zones = listOf("Bole", "Kirkos", "Yeka", "Nifas Silk", "Gondar", "Adama", "Mekelle")
    val cities = listOf("Addis Ababa", "Gondar", "Adama", "Mekelle", "Bahir Dar", "Hawassa")

    var expandedRegion by remember { mutableStateOf(false) }
    var expandedZone by remember { mutableStateOf(false) }
    var expandedCity by remember { mutableStateOf(false) }

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
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =  if (addressToEdit == null) "Add New Address" else "Edit Address",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text =  if (addressToEdit == null) "add new address for delivery" else "edit your delivery eaddress",
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Label choices
            Text("Address Label", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Home", "Work", "Other").forEach { label ->
                    val isSelected = labelChoice == label
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { labelChoice = label }
                            .background(if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(vertical = 10.dp)
                            .testTag("address_label_choice_$label"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            if (labelChoice == "Other") {
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Custom Label Name") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_label_input"),
                    placeholder = { Text("e.g. Gym, School, Cabin") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // Region Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = region,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Region") },
                    trailingIcon = { IconButton(onClick = { expandedRegion = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                    modifier = Modifier.fillMaxWidth().clickable { expandedRegion = true }.testTag("region_dropdown_field"),
                    shape = RoundedCornerShape(10.dp)
                )
                DropdownMenu(expanded = expandedRegion, onDismissRequest = { expandedRegion = false }) {
                    regions.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r) },
                            onClick = {
                                region = r
                                expandedRegion = false
                            }
                        )
                    }
                }
            }

            // Zone Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = zone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("State / Zone") },
                    trailingIcon = { IconButton(onClick = { expandedZone = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                    modifier = Modifier.fillMaxWidth().clickable { expandedZone = true }.testTag("zone_dropdown_field"),
                    shape = RoundedCornerShape(10.dp)
                )
                DropdownMenu(expanded = expandedZone, onDismissRequest = { expandedZone = false }) {
                    zones.forEach { z ->
                        DropdownMenuItem(
                            text = { Text(z) },
                            onClick = {
                                zone = z
                                expandedZone = false
                            }
                        )
                    }
                }
            }

            // City Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = city,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("City") },
                    trailingIcon = { IconButton(onClick = { expandedCity = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                    modifier = Modifier.fillMaxWidth().clickable { expandedCity = true }.testTag("city_dropdown_field"),
                    shape = RoundedCornerShape(10.dp)
                )
                DropdownMenu(expanded = expandedCity, onDismissRequest = { expandedCity = false }) {
                    cities.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c) },
                            onClick = {
                                city = c
                                expandedCity = false
                            }
                        )
                    }
                }
            }

            // Kebele Input
            OutlinedTextField(
                value = kebele,
                onValueChange = { kebele = it },
                label = { Text("Kebele") },
                modifier = Modifier.fillMaxWidth().testTag("address_kebele_input"),
                placeholder = { Text("e.g. 03 or 14") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Street & House Number
            OutlinedTextField(
                value = streetHouseNumber,
                onValueChange = { streetHouseNumber = it },
                label = { Text("Street Address / House Number") },
                modifier = Modifier.fillMaxWidth().testTag("address_street_input"),
                placeholder = { Text("e.g. Bole Road, H.No 120") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Map Coordinates Section (OpenStreetMap)
            Text("OpenStreetMap Location Dropper 🗺️", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text("Pinpoint delivery coordinates on OpenStreetMap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Button(
                onClick = { showMapPicker = !showMapPicker },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("use_map_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = "OpenStreetMap")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showMapPicker) "Close OpenStreetMap" else "Open OpenStreetMap to Drop Pin 📍", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            }

            if (showMapPicker) {
                MapPickerCanvas(
                    latitude = pinLat,
                    longitude = pinLon,
                    onSelectCoordinates = { lat, lon ->
                        pinLat = lat
                        pinLon = lon
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("OpenStreetMap Location:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                        Text("Lat: ${String.format(java.util.Locale.US, "%.4f", pinLat)} | Lon: ${String.format(java.util.Locale.US, "%.4f", pinLon)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("🗺️ OSM", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save Button
            Button(
                onClick = {
                    val finalLabel = if (labelChoice == "Other") customLabel.ifBlank { "Other" } else labelChoice
                    if (kebele.isNotBlank() && streetHouseNumber.isNotBlank()) {
                        val addressData = AddressEntity(
                            id = addressToEdit?.id ?: "",
                            label = finalLabel,
                            region = region,
                            zone = zone,
                            city = city,
                            kebele = kebele,
                            streetHouseNumber = streetHouseNumber,
                            isDefault = addressToEdit?.isDefault ?: (viewModel.savedAddresses.value.isEmpty()),
                            latitude = pinLat,
                            longitude = pinLon
                        )
                        if (addressToEdit == null) {
                            viewModel.addAddress(addressData)
                        } else {
                            viewModel.updateAddress(addressData)
                        }
                        onDismiss()
                    }
                },
                enabled = kebele.isNotBlank() && streetHouseNumber.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_address_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Address", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSubPage(
    viewModel: AppViewModel,
    activeUser: UserEntity?,
    onBack: () -> Unit
) {
    // Flows from ViewModel
    val sellerProfile by viewModel.sellerProfile.collectAsStateWithLifecycle()
    val deliveryProfile by viewModel.deliveryProfile.collectAsStateWithLifecycle()
    val status by viewModel.sellerApplicationStatus.collectAsStateWithLifecycle()
    val deliveryAppStatus by viewModel.deliveryApplicationStatus.collectAsStateWithLifecycle()

    // Email secure flow states
    var isChangingEmail by remember { mutableStateOf(false) }
    var newEmailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var pendingNewEmail by remember { mutableStateOf<String?>(null) }
    var showOtpDialogForEmail by remember { mutableStateOf(false) }
    
    val isEmailOtpSent by viewModel.isOtpSentForEmail.collectAsStateWithLifecycle()
    var enteredEmailOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    val emailOtpCode by viewModel.emailOtpCode.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Role onboarding form states
    var activeOnboardingFlow by remember { mutableStateOf<String?>(null) } // null, "SELLER", "DELIVERY"
    
    // Seller form inputs
    var sellerTin by remember { mutableStateOf("") }
    var sellerBusName by remember { mutableStateOf("") }
    var sellerBusPhone by remember { mutableStateOf("") }
    var sellerGeoLocation by remember { mutableStateOf("") }
    var sellerDocFrontUri by remember { mutableStateOf<String?>(null) }
    var sellerDocBackUri by remember { mutableStateOf<String?>(null) }
    var sellerRegion by remember { mutableStateOf("") }
    var sellerZone by remember { mutableStateOf("") }
    var sellerCity by remember { mutableStateOf("") }
    var sellerKebele by remember { mutableStateOf("") }
    var sellerOwnerIdType by remember { mutableStateOf("National ID") }

    // Delivery form inputs
    var deliveryPlate by remember { mutableStateOf("") }
    var deliveryIdType by remember { mutableStateOf("Driver's License") }
    var deliveryVehType by remember { mutableStateOf("Motorcycle") }
    var deliveryContactPhone by remember { mutableStateOf("") }
    var deliveryLicenseFrontUri by remember { mutableStateOf<String?>(null) }
    var deliveryLicenseBackUri by remember { mutableStateOf<String?>(null) }
    var deliveryVehDocFrontUri by remember { mutableStateOf<String?>(null) }
    var deliveryVehDocBackUri by remember { mutableStateOf<String?>(null) }

    val launcherSellerFront = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> if (uri != null) sellerDocFrontUri = uri.toString() }
    val launcherSellerBack = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> if (uri != null) sellerDocBackUri = uri.toString() }
    val launcherDelLicFront = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> if (uri != null) deliveryLicenseFrontUri = uri.toString() }
    val launcherDelLicBack = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> if (uri != null) deliveryLicenseBackUri = uri.toString() }
    val launcherDelVehFront = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> if (uri != null) deliveryVehDocFrontUri = uri.toString() }
    val launcherDelVehBack = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> if (uri != null) deliveryVehDocBackUri = uri.toString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .testTag("account_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "My Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Role-specific credentials & secure settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // COMMON SECTION (All Users)
            item {
                Text(
                    text = "Common Information",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AccountValueRow(label = "Full Name", value = activeUser?.fullName ?: "Amina Farah")
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        // Email address + Verification Badge + Verification flow
                        Column {
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val isVerifiedVal = activeUser?.isEmailVerified ?: false
                            val currentOfficialEmail = activeUser?.email ?: "abebe.kebede@gmail.com"
                            
                            if (pendingNewEmail == null) {
                                // State 1 & State 3: Verified
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentOfficialEmail,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!isChangingEmail) {
                                        TextButton(
                                            onClick = { 
                                                newEmailInput = ""
                                                emailError = null
                                                isChangingEmail = true 
                                            },
                                            modifier = Modifier.testTag("verify_email_btn")
                                        ) {
                                            Text(
                                                text = "Change",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = BrandGreenPrimary
                                            )
                                        }
                                    }
                                }
                                
                                // Input field to type new email when Change is clicked
                                if (isChangingEmail) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Enter New Email Address", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        OutlinedTextField(
                                            value = newEmailInput,
                                            onValueChange = { newEmailInput = it },
                                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("new_email_input"),
                                            placeholder = { Text("new.email@gmail.com") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        if (emailError != null) {
                                            Text(emailError!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { isChangingEmail = false }) { Text("Cancel") }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmailInput).matches()) {
                                                        emailError = "Please enter a valid email address."
                                                    } else {
                                                        emailError = null
                                                        viewModel.sendEmailVerificationOtp(newEmailInput)
                                                        pendingNewEmail = newEmailInput // Transition to State 2
                                                        isChangingEmail = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                            ) {
                                                Text("Send OTP")
                                            }
                                        }
                                    }
                                }
                            } else {
                                // State 2: Not Verified (Pending Change)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pendingNewEmail!!,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    TextButton(
                                        onClick = { 
                                            showOtpDialogForEmail = true 
                                            enteredEmailOtp = ""
                                            otpError = null
                                        },
                                        modifier = Modifier.testTag("not_verified_btn")
                                    ) {
                                        Text(
                                            text = "Not Verified",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = BrandRedTertiary
                                        )
                                    }
                                }
                                
                                // Verification block shown inline or via trigger when clicking "Not Verified"
                                if (showOtpDialogForEmail) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Enter 4-digit OTP sent to $pendingNewEmail", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text("Simulated Code: ${emailOtpCode ?: "5821"}", style = MaterialTheme.typography.bodySmall, color = BrandGoldSecondary)
                                        OutlinedTextField(
                                            value = enteredEmailOtp,
                                            onValueChange = { if (it.length <= 4) enteredEmailOtp = it },
                                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("email_otp_input"),
                                            placeholder = { Text("5821") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        if (otpError != null) {
                                            Text(otpError!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            TextButton(
                                                onClick = { 
                                                    viewModel.resendEmailChangeOtp()
                                                }
                                            ) { 
                                                Text("Resend OTP", color = BrandGoldSecondary) 
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            TextButton(
                                                onClick = { 
                                                    viewModel.cancelEmailChange()
                                                    pendingNewEmail = null 
                                                    showOtpDialogForEmail = false
                                                }
                                            ) { 
                                                Text("Cancel Change") 
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val success = viewModel.verifyEmailOtp(enteredEmailOtp)
                                                        if (success) {
                                                            pendingNewEmail = null
                                                            showOtpDialogForEmail = false
                                                            otpError = null
                                                        } else {
                                                            otpError = "Incorrect OTP. Please enter ${emailOtpCode ?: "5821"}."
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                            ) {
                                                Text("Verify OTP")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        AccountValueRow(label = "Phone Number", value = activeUser?.phone ?: "+251 915 234567")
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        AccountValueRow(label = "Birth Date YYYY-MM-DD", value = activeUser?.birthDate ?: "2000-5-7")
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
 
                        // Registered Address - Non-Editable
                        Column {
                            Text(
                                text = "Registered Address",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val fullAddress = remember(activeUser) {
                                if (activeUser != null) {
                                    val region = activeUser.addressRegion
                                    val zone = activeUser.addressZone
                                    val city = activeUser.addressCity
                                    val kebele = activeUser.addressKebele
                                    if (region.isNotEmpty() || zone.isNotEmpty() || city.isNotEmpty() || kebele?.isNotEmpty() == true) {
                                        "$region, $zone, $city, $kebele"
                                    } else {
                                        activeUser.address
                                    }
                                } else {
                                    "Addis Ababa, Bole, Addis Ababa, 03"
                                }
                            }
                            Text(
                                text = fullAddress,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
 
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
 
                        // Delivery Addresses (Redirection to editable Address Book with custom target tracker)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Delivery Addresses",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = BrandGreenPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                TextButton(
                                    onClick = {
                                        viewModel.addressBackTarget.value = "ACCOUNT"
                                        viewModel.navigateToProfileSub("ADDRESS_MANAGEMENT")
                                    },
                                    modifier = Modifier.testTag("manage_addresses_btn")
                                ) {
                                    Text("Manage Addresses 📍", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add, edit, delete, or select your preferred locations for quick checkout.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
 
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
 
                        // Date Created Account Section
                        AccountValueRow(label = "Date Created Account", value = activeUser?.created?.substringBefore(" ") ?: "" )
                    }
                }
            }

            // GROW WITH E-SUUQ (Only visible to Buyer accounts)
            if (activeUser?.role == "BUYER") {
                item {
                    Text(
                        text = "Grow with E-Suuq",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Earn extra income by opening a digital store front or delivering orders with our logistics network.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (activeOnboardingFlow == null) {
                                if (sellerProfile?.sellerVerification.equals("PENDING", ignoreCase = true) || sellerProfile?.sellerVerification.equals("rejected", ignoreCase = true)) {
                                    val pd = "You already have a pending/approved application for Seller. You cannot apply for another role simultaneously."
                                    val rg = "❌ Your Seller application has been rejected. Please contact support for more information."
                                    val message = when {
                                        sellerProfile?.sellerVerification.equals("PENDING", ignoreCase = true) -> pd
                                        sellerProfile?.sellerVerification.equals("rejected", ignoreCase = true) -> rg
                                        else -> pd
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFF3CD))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = message ,
                                            color = Color(0xFF856404),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                } else if (deliveryProfile?.deliveryVerification.equals("PENDING", ignoreCase = true) || deliveryProfile?.deliveryVerification.equals("rejected", ignoreCase = true)) {
                                    val pd = "You already have a pending/approved application for Delivery. You cannot apply for another role simultaneously."
                                    val rg = "❌ Your delivery application has been rejected. Please contact support for more information."
                                    val message = when {
                                        deliveryProfile?.deliveryVerification.equals("PENDING", ignoreCase = true) -> pd
                                        deliveryProfile?.deliveryVerification.equals("rejected", ignoreCase = true) -> rg
                                        else -> pd
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFF3CD))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = message,
                                            color = Color(0xFF856404),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Become a Seller
                                        Button(
                                            onClick = { activeOnboardingFlow = "SELLER" },
                                            modifier = Modifier.weight(1f).height(44.dp).testTag("become_seller_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                        ) {
                                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Become Seller", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }

                                        // Become a Delivery
                                        Button(
                                            onClick = { activeOnboardingFlow = "DELIVERY" },
                                            modifier = Modifier.weight(1f).height(44.dp).testTag("become_delivery_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGoldSecondary)
                                        ) {
                                            Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Join Delivery", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }

                            // Seller onboarding Form
                            if (activeOnboardingFlow == "SELLER") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Seller Requirements", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                        IconButton(onClick = { activeOnboardingFlow = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close")
                                        }
                                    }

                                    // Requirements checklist
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        RequirementCheckRow(text = "Business License Document (Front & Back)")
                                        RequirementCheckRow(text = "Business Owner Valid ID (National ID/Passport)")
                                        RequirementCheckRow(text = "TIN Number (Tax Registration)")
                                        RequirementCheckRow(text = "Verified Store Business Address (Region, Zone, City, Kebele)")
                                        RequirementCheckRow(text = "Map Geolocation coords")
                                    }

                                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                    if (sellerProfile?.sellerVerification.equals("PENDING", ignoreCase = true)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFF3CD))
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = "Application Submitted! Our admin team is reviewing your business credentials. We'll notify you in 24 hours.",
                                                color = Color(0xFF856404),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    } else {
                                        // Inputs
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            OutlinedTextField(
                                                value = sellerBusName,
                                                onValueChange = { sellerBusName = it },
                                                label = { Text("Business Name * (Required)") },
                                                isError = sellerBusName.isEmpty(),
                                                modifier = Modifier.fillMaxWidth().testTag("seller_bus_name_input"),
                                                placeholder = { Text("e.g. Al-Suuq Electronics") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            OutlinedTextField(
                                                value = sellerGeoLocation.ifEmpty { "Pick Map Point" },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Map Point * (Required)") },
                                                isError = sellerGeoLocation.isEmpty(),
                                                trailingIcon = {
                                                    IconButton(onClick = { sellerGeoLocation = "9.0222, 38.7468" }) {
                                                        Icon(Icons.Default.LocationOn, contentDescription = "Pick Map Point", tint = if (sellerGeoLocation.isEmpty()) MaterialTheme.colorScheme.error else BrandGreenPrimary)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("seller_geolocation_input"),
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            OutlinedTextField(
                                                value = sellerTin,
                                                onValueChange = { sellerTin = it },
                                                label = { Text("TIN Tax Registration Number * (Required)") },
                                                isError = sellerTin.isEmpty(),
                                                modifier = Modifier.fillMaxWidth().testTag("seller_tin_input"),
                                                placeholder = { Text("e.g. TIN-987654321") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            Text("Store Address Details * (Required)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = sellerRegion,
                                                    onValueChange = { sellerRegion = it },
                                                    label = { Text("Region *") },
                                                    isError = sellerRegion.isEmpty(),
                                                    modifier = Modifier.weight(1f).testTag("seller_region_input"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                OutlinedTextField(
                                                    value = sellerZone,
                                                    onValueChange = { sellerZone = it },
                                                    label = { Text("Zone *") },
                                                    isError = sellerZone.isEmpty(),
                                                    modifier = Modifier.weight(1f).testTag("seller_zone_input"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = sellerCity,
                                                    onValueChange = { sellerCity = it },
                                                    label = { Text("City *") },
                                                    isError = sellerCity.isEmpty(),
                                                    modifier = Modifier.weight(1f).testTag("seller_city_input"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                OutlinedTextField(
                                                    value = sellerKebele,
                                                    onValueChange = { sellerKebele = it },
                                                    label = { Text("Kebele *") },
                                                    isError = sellerKebele.isEmpty(),
                                                    modifier = Modifier.weight(1f).testTag("seller_kebele_input"),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }

                                            Text("Business License Documents", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { launcherSellerFront.launch("image/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (sellerDocFrontUri != null) BrandGreenPrimary else Color.Gray),
                                                    modifier = Modifier.weight(1f).height(36.dp).testTag("seller_doc_front_btn")
                                                ) {
                                                    Text(if (sellerDocFrontUri != null) "Front ✓" else "Upload Front", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = { launcherSellerBack.launch("image/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (sellerDocBackUri != null) BrandGreenPrimary else Color.Gray),
                                                    modifier = Modifier.weight(1f).height(36.dp).testTag("seller_doc_back_btn")
                                                ) {
                                                    Text(if (sellerDocBackUri != null) "Back ✓" else "Upload Back", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }

                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            Button(
                                                onClick = {
                                                    if (sellerBusName.isNotEmpty() && sellerTin.isNotEmpty() &&
                                                        sellerRegion.isNotEmpty() && sellerZone.isNotEmpty() && sellerCity.isNotEmpty() && sellerKebele.isNotEmpty() &&
                                                        sellerGeoLocation.isNotEmpty() &&
                                                        sellerDocFrontUri != null && sellerDocBackUri != null) {
                                                        
                                                        val fileFront = uriToFile(context, sellerDocFrontUri)
                                                        val fileBack = uriToFile(context, sellerDocBackUri)
                                                        
                                                        viewModel.applyAsSeller(
                                                            businessName = sellerBusName,
                                                            businessPhone = activeUser?.phone ?: "",
                                                            region = sellerRegion,
                                                            zone = sellerZone,
                                                            city = sellerCity,
                                                            kebele = sellerKebele,
                                                            tinNumber = sellerTin,
                                                            businessDocumentFront = fileFront,
                                                            businessDocumentBack = fileBack,
                                                            onSuccess = { activeOnboardingFlow = null },
                                                            onError = { /* display error */ }
                                                        )
                                                    }
                                                },
                                                enabled = sellerBusName.isNotEmpty() && sellerTin.isNotEmpty() &&
                                                        sellerRegion.isNotEmpty() && sellerZone.isNotEmpty() && sellerCity.isNotEmpty() && sellerKebele.isNotEmpty() &&
                                                        sellerGeoLocation.isNotEmpty() &&
                                                        sellerDocFrontUri != null && sellerDocBackUri != null,
                                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("seller_apply_btn"),
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                            ) {
                                                Text("Submit Application", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }
                            }

                            // Delivery onboarding Form
                            if (activeOnboardingFlow == "DELIVERY") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Courier Requirements", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandGoldSecondary)
                                        IconButton(onClick = { activeOnboardingFlow = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close")
                                        }
                                    }

                                    // Requirements checklist
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        RequirementCheckRow(text = "Valid Driver's License (Front & Back)")
                                        RequirementCheckRow(text = "Vehicle Registration Document / Libre (Front & Back)")
                                        RequirementCheckRow(text = "Active License Plate Number & Vehicle Type")
                                        RequirementCheckRow(text = "Active Contact Phone Number")
                                    }

                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    if (deliveryProfile?.deliveryVerification.equals("PENDING", ignoreCase = true)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFF3CD))
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = "Application Submitted! Our safety and verification team is reviewing your vehicle credentials. We'll notify you in 24 hours.",
                                                color = Color(0xFF856404),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    } else {
                                        // Inputs
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            var vehExpanded by remember { mutableStateOf(false) }
                                            val vehicleOptions = listOf("MOTORCYCLE", "BICYCLE", "BAJAJA", "CAR")
                                            
                                            ExposedDropdownMenuBox(
                                                expanded = vehExpanded,
                                                onExpandedChange = { vehExpanded = !vehExpanded },
                                            ) {
                                                OutlinedTextField(
                                                    value = deliveryVehType,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Vehicle Type * (Required)") },
                                                    isError = deliveryVehType.isEmpty(),
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehExpanded) },
                                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("delivery_veh_type_input")
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = vehExpanded,
                                                    onDismissRequest = { vehExpanded = false }
                                                ) {
                                                    vehicleOptions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option) },
                                                            onClick = {
                                                                deliveryVehType = option
                                                                vehExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            OutlinedTextField(
                                                value = deliveryPlate,
                                                onValueChange = { deliveryPlate = it },
                                                label = { Text("Vehicle License Plate Number * (Required)") },
                                                isError = deliveryPlate.isEmpty(),
                                                modifier = Modifier.fillMaxWidth().testTag("delivery_plate_input"),
                                                placeholder = { Text("e.g. A.A-A76543") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            Text("Driver's License (Front & Back)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { launcherDelLicFront.launch("image/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (deliveryLicenseFrontUri != null) BrandGreenPrimary else Color.Gray),
                                                    modifier = Modifier.weight(1f).height(36.dp).testTag("delivery_license_front_btn")
                                                ) {
                                                    Text(if (deliveryLicenseFrontUri != null) "Front ✓" else "Upload Front", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = { launcherDelLicBack.launch("image/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (deliveryLicenseBackUri != null) BrandGreenPrimary else Color.Gray),
                                                    modifier = Modifier.weight(1f).height(36.dp).testTag("delivery_license_back_btn")
                                                ) {
                                                    Text(if (deliveryLicenseBackUri != null) "Back ✓" else "Upload Back", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }

                                            Text("Vehicle Document / Libre (Front & Back)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { launcherDelVehFront.launch("image/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (deliveryVehDocFrontUri != null) BrandGreenPrimary else Color.Gray),
                                                    modifier = Modifier.weight(1f).height(36.dp).testTag("delivery_veh_doc_front_btn")
                                                ) {
                                                    Text(if (deliveryVehDocFrontUri != null) "Front ✓" else "Upload Front", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = { launcherDelVehBack.launch("image/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (deliveryVehDocBackUri != null) BrandGreenPrimary else Color.Gray),
                                                    modifier = Modifier.weight(1f).height(36.dp).testTag("delivery_veh_doc_back_btn")
                                                ) {
                                                    Text(if (deliveryVehDocBackUri != null) "Back ✓" else "Upload Back", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }

                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            Button(
                                                onClick = {
                                                    if (deliveryPlate.isNotEmpty() && deliveryVehType.isNotEmpty() &&
                                                        deliveryLicenseFrontUri != null && deliveryLicenseBackUri != null &&
                                                        deliveryVehDocFrontUri != null && deliveryVehDocBackUri != null) {
                                                        
                                                        val licFront = uriToFile(context, deliveryLicenseFrontUri)
                                                        val licBack = uriToFile(context, deliveryLicenseBackUri)
                                                        val vehFront = uriToFile(context, deliveryVehDocFrontUri)
                                                        val vehBack = uriToFile(context, deliveryVehDocBackUri)
                                                        
                                                        viewModel.applyAsDelivery(
                                                            plateNumber = deliveryPlate,
                                                            phoneNumber = activeUser?.phone ?: "",
                                                            vehicleType = deliveryVehType,
                                                            driverLicenseFront = licFront,
                                                            driverLicenseBack = licBack,
                                                            vehicleDocumentFront = vehFront,
                                                            vehicleDocumentBack = vehBack,
                                                            onSuccess = { activeOnboardingFlow = null },
                                                            onError = { /* display error */ }
                                                        )
                                                    }
                                                },
                                                enabled = deliveryPlate.isNotEmpty() && deliveryVehType.isNotEmpty() &&
                                                        deliveryLicenseFrontUri != null && deliveryLicenseBackUri != null &&
                                                        deliveryVehDocFrontUri != null && deliveryVehDocBackUri != null,
                                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("delivery_apply_btn"),
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                            ) {
                                                Text("Submit Application", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SELLER SPECIFIC INFO
            if (activeUser?.role == "SELLER") {
                item {
                    Text(
                        text = "Seller Business Credentials",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            AccountValueRow(label = "Business Name", value = sellerProfile?.businessName ?: "Bole Traditional Boutique")
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            AccountValueRow(label = "Business Address", value = activeUser?.businessAddress ?: "Bole Road, Addis Ababa")
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            val productsSoldProfile = sellerProfile?.businessGrade?.toInt() ?: 0
                            val gradeLabelProfile = sellerProfile?.businessGradeTitel ?: when {
                                productsSoldProfile >= 500 -> "Platinum"
                                productsSoldProfile >= 150 -> "Gold"
                                productsSoldProfile >= 50 -> "Silver"
                                else -> "Bronze"
                            }
                            AccountValueRow(label = "Business Grade", value = gradeLabelProfile)
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            AccountValueRow(label = "TIN Registration", value = sellerProfile?.tinNumber ?: "TIN-987654321")
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            AccountValueRow(label = "TLC Registration", value = "TCL-${sellerProfile?.tlcNumber ?: "ABCD1234"}")
                        }
                    }
                }
            }

            // COURIER SPECIFIC INFO
            if (activeUser?.role == "DELIVERY") {
                item {
                    Text(
                        text = "Delivery Personnel Credentials",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandGreenPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            AccountValueRow(label = "License Plate Number", value = deliveryProfile?.plateNumber ?: "A.A-B23456")
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            AccountValueRow(label = "Delivery Grade/Rating", value = deliveryProfile?.deliveryGrade ?: "4.9 Stars (Expert)")
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            AccountValueRow(label = "Vehicle Type", value = deliveryProfile?.vehicleType ?: "Bajaj")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}


@Composable
fun SettingsSubPage(
    viewModel: AppViewModel,
    activeUser: UserEntity?,
    currentLang: AppLanguage,
    isDark: Boolean,
    isNotificationsEnabled: Boolean,
    onNavigateToSub: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showSetupSecurityDialog by remember { mutableStateOf(false) }
    val isLockEnabled = activeUser?.isAppLockEnabled ?: false
    val isBioEnabled = activeUser?.isBiometricEnabled ?: false
    val is2faEnabled = activeUser?.isTwoFactorEnabled ?: false
    val isBiometricHardwareAvailable = remember(context) { BiometricHelper.isBiometricAvailable(context) }

    var showDisable2faOtpDialog by remember { mutableStateOf(false) }
    var disable2faOtpId by remember { mutableStateOf<String?>(null) }
    var enteredDisable2faOtp by remember { mutableStateOf("") }
    var disable2faError by remember { mutableStateOf("") }
    var isVerifyingDisableOtp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .testTag("settings_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Central hub for security and app preferences",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Settings Section
            item {
                Text(
                    text = "Security Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Biometric Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(text = "Biometric Lock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Unlock app using your fingerprint or face recognition",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isBioEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (BiometricHelper.hasDeviceSecurity(context)) {
                                            BiometricHelper.showBiometricAndCredentialPrompt(
                                                context = context,
                                                title = "Enable Biometric Lock",
                                                subtitle = "Authenticate using your phone security to enable biometric lock",
                                                onSuccess = {
                                                    viewModel.setBiometricEnabled(true)
                                                    Toast.makeText(context, "Biometric lock enabled", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, "Authentication failed: $err", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            showSetupSecurityDialog = true
                                        }
                                    } else {
                                        if (BiometricHelper.hasDeviceSecurity(context)) {
                                            BiometricHelper.showBiometricAndCredentialPrompt(
                                                context = context,
                                                title = "Disable Biometric Lock",
                                                subtitle = "Authenticate using your phone security to disable biometric lock",
                                                onSuccess = {
                                                    viewModel.setBiometricEnabled(false)
                                                    Toast.makeText(context, "Biometric lock disabled", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, "Authentication failed: $err", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            // If somehow the device security is gone, just let them disable it
                                            viewModel.setBiometricEnabled(false)
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("biometric_lock_toggle")
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Two-Factor Verification
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(text = "Two-Factor Auth", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Receive OTP for login verification",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = is2faEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (BiometricHelper.hasDeviceSecurity(context)) {
                                            BiometricHelper.showBiometricAndCredentialPrompt(
                                                context = context,
                                                title = "Enable Two-Factor Auth",
                                                subtitle = "Authenticate using your phone security to enable Two-Factor Authentication",
                                                onSuccess = {
                                                    viewModel.enableTwoFactor { success, error ->
                                                        if (success) {
                                                            Toast.makeText(context, "Two-Factor Authentication enabled successfully!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Failed to enable 2FA: ${error ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, "Authentication failed: $err", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            Toast.makeText(context, "Please configure a device lock screen / PIN / Biometric security first.", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        viewModel.requestOtpFor2FADisable { otpId, error ->
                                            if (otpId != null) {
                                                disable2faOtpId = otpId
                                                enteredDisable2faOtp = ""
                                                disable2faError = ""
                                                showDisable2faOtpDialog = true
                                            } else {
                                                Toast.makeText(context, "Failed to send verification OTP: ${error ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("2fa_toggle")
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Change Password
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSub("CHANGE_PASSWORD") }
                                .padding(vertical = 4.dp)
                                .testTag("change_password_navigator"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Change Password", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Update or reset your secure account password",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("→", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = BrandGreenPrimary)
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Manage Login Devices
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSub("LOGIN_HISTORY") }
                                .padding(vertical = 4.dp)
                                .testTag("login_history_navigator"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Manage Login Devices", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "View and manage active login sessions and devices",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("→", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = BrandGreenPrimary)
                        }
                    }
                }
            }

            // PREFERENCES SECTION
            item {
                Text(
                    text = "App Preferences",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Language Dropdown Selector
                        Column {
                            Text(text = "App Language Selection", fontWeight = FontWeight.Bold)
                            Text(text = "Select your preferred Ethiopian localized dialect", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AppLanguage.values().forEach { lang ->
                                    val isSelected = currentLang == lang
                                    OutlinedButton(
                                        onClick = { viewModel.setLanguage(lang) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("lang_switch_settings_${lang.code}"),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) BrandGreenPrimary else Color.Transparent
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = if (lang == AppLanguage.AMHARIC) "አማርኛ" else if (lang == AppLanguage.SOMALI) "Somali" else "English",
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Push Notification Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(text = "Push Notifications", fontWeight = FontWeight.Bold)
                                Text(text = "Receive alert tones on order status & messaging updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isNotificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                modifier = Modifier.testTag("push_notifications_toggle")
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Theme switch (The primary app color remains green in both modes)
                        Column {
                            Text(text = "Theme Preference (Primary color remains Green)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.setDarkTheme(false) },
                                    modifier = Modifier.weight(1f).testTag("theme_light_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (!isDark) BrandGreenPrimary else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (!isDark) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Icon(
                                        Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = if (!isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Light",
                                        color = if (!isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.setDarkTheme(true) },
                                    modifier = Modifier.weight(1f).testTag("theme_dark_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isDark) BrandGreenPrimary else Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, if (isDark) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Icon(
                                        Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Dark",
                                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSetupSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showSetupSecurityDialog = false },
            title = {
                Text(
                    text = "Device Security Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Your phone does not have any screen lock or biometric security enabled (such as PIN, Pattern, Password, Fingerprint, or Face). Please set it up in your device settings to enable Biometric Lock.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSetupSecurityDialog = false
                        BiometricHelper.openSecuritySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSetupSecurityDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }

    if (showDisable2faOtpDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isVerifyingDisableOtp) {
                    showDisable2faOtpDialog = false 
                }
            },
            icon = {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = BrandGreenPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Verify OTP to Disable 2FA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "A 6-digit verification code has been sent to ${activeUser?.email ?: "your email"}. Please enter it below to confirm disabling Two-Factor Authentication.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = enteredDisable2faOtp,
                        onValueChange = { newValue ->
                            if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                enteredDisable2faOtp = newValue
                            }
                        },
                        label = { Text("6-Digit OTP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("disable_2fa_otp_input"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                // optional done action
                            }
                        )
                    )
                    
                    if (disable2faError.isNotEmpty()) {
                        Text(
                            text = disable2faError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val otpId = disable2faOtpId
                        if (otpId == null) {
                            disable2faError = "Error: OTP session not found. Please try again."
                            return@Button
                        }
                        if (enteredDisable2faOtp.length != 6) {
                            disable2faError = "Please enter a 6-digit verification code."
                            return@Button
                        }
                        
                        isVerifyingDisableOtp = true
                        viewModel.verifyOtpAndDisable2FA(otpId, enteredDisable2faOtp) { success, error ->
                            isVerifyingDisableOtp = false
                            if (success) {
                                showDisable2faOtpDialog = false
                                Toast.makeText(context, "Two-Factor Authentication disabled successfully.", Toast.LENGTH_SHORT).show()
                            } else {
                                disable2faError = error ?: "Incorrect OTP. Verification failed."
                            }
                        }
                    },
                    enabled = !isVerifyingDisableOtp && enteredDisable2faOtp.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    modifier = Modifier.testTag("disable_2fa_confirm_btn")
                ) {
                    if (isVerifyingDisableOtp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Verify & Disable")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisable2faOtpDialog = false },
                    enabled = !isVerifyingDisableOtp
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}


@Composable
fun ChangePasswordSubPage(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(false) }

    var currentPassVisible by remember { mutableStateOf(false) }
    var newPassVisible by remember { mutableStateOf(false) }
    var confirmPassVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .testTag("change_password_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Update your secure account password",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Change Account Password",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = BrandGreenPrimary
            )

            if (feedbackMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSuccessStatus) Color(0xFFD4EDDA) else Color(0xFFF8D7DA))
                        .padding(10.dp)
                ) {
                    Text(
                        text = feedbackMessage ?: "",
                        color = if (isSuccessStatus) Color(0xFF155724) else Color(0xFF721C24),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Current Password Input
            OutlinedTextField(
                value = currentPass,
                onValueChange = { input -> 
                    if (input.all { it.isDigit() } && input.length <= 6) {
                        currentPass = input
                    }
                },
                label = { Text("Current 6-Digit Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (currentPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { currentPassVisible = !currentPassVisible }) {
                        Icon(
                            imageVector = if (currentPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("current_pin_input"),
                singleLine = true,
                enabled = !isLoading
            )

            // New Password Input
            OutlinedTextField(
                value = newPass,
                onValueChange = { input -> 
                    if (input.all { it.isDigit() } && input.length <= 6) {
                        newPass = input
                    }
                },
                label = { Text("New 6-Digit Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (newPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { newPassVisible = !newPassVisible }) {
                        Icon(
                            imageVector = if (newPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("new_pin_input"),
                singleLine = true,
                enabled = !isLoading
            )

            // Confirm New Password Input
            OutlinedTextField(
                value = confirmPass,
                onValueChange = { input -> 
                    if (input.all { it.isDigit() } && input.length <= 6) {
                        confirmPass = input
                    }
                },
                label = { Text("Confirm New Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (confirmPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                        Icon(
                            imageVector = if (confirmPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input"),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (currentPass.length != 6) {
                        feedbackMessage = "Error: Current password must be exactly 6 digits."
                        isSuccessStatus = false
                    } else if (newPass.length != 6) {
                        feedbackMessage = "Error: New password must be exactly 6 digits."
                        isSuccessStatus = false
                    } else if (newPass != confirmPass) {
                        feedbackMessage = "Error: New password and Confirmation do not match."
                        isSuccessStatus = false
                    } else {
                        viewModel.changeUserPassword(
                            current = currentPass,
                            new = newPass,
                            onSuccess = {
                                feedbackMessage = "Success! Your account password has been updated."
                                isSuccessStatus = true
                                currentPass = ""
                                newPass = ""
                                confirmPass = ""
                            },
                            onError = { error ->
                                feedbackMessage = error
                                isSuccessStatus = false
                            }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_change_password"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Update Password", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun String?.toDateTime(): String = 
    this?.let { 
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault())
                .format(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                    .parse(it) ?: java.util.Date())
        } catch (e: Exception) { 
            it.substringBefore(" ") 
        }
    } ?: "Just Now"
    
@Composable
fun LoginHistorySubPage(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val logins by viewModel.loginDevices.collectAsStateWithLifecycle()
    var deviceToDelete by remember { mutableStateOf<LoginHistoryEntity?>(null) }
    var deviceDetailsToShow by remember { mutableStateOf<LoginHistoryEntity?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentDeviceId = remember(context) {
        android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: ""
    }

    val sortedLogins = remember(logins, currentDeviceId) {
        logins.sortedWith(compareByDescending { it.serialNumber == currentDeviceId })
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.syncLoginHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .testTag("login_history_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Login Devices",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage and secure your active login sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(sortedLogins) { login ->
                val isThisDevice = login.serialNumber == currentDeviceId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deviceDetailsToShow = login }
                        .testTag("device_card_${login.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isThisDevice) BrandGreenPrimary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (login.deviceType) {
                                    "Desktop" -> Icons.Default.Computer
                                    "Tablet" -> Icons.Default.TabletAndroid
                                    else -> Icons.Default.PhoneAndroid
                                },
                                contentDescription = login.deviceType,
                                tint = if (isThisDevice) BrandGreenPrimary else MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = login.deviceName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isThisDevice) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BrandGreenPrimary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "This Device",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${login.location} • OS: ${login.operatingSystem}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val osLower = login.operatingSystem.lowercase(java.util.Locale.ROOT)
                            if (osLower != "android" && osLower != "ios" && login.browser.isNotBlank()) {
                                Text(
                                    text = "Browser: ${login.browser}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = login.updated.toDateTime(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isThisDevice) BrandGreenPrimary else MaterialTheme.colorScheme.outline
                            )
                        }

                        // Remove/logout button available only for other devices
                        if (!isThisDevice) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { deviceToDelete = login },
                                modifier = Modifier.testTag("remove_device_${login.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove Device",
                                    tint = BrandRedTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // CONFIRMATION DIALOG
    if (deviceToDelete != null) {
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("Log Out Device?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this device? You will be logged out on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeLoginDevice(deviceToDelete!!.id)
                        deviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedTertiary),
                    modifier = Modifier.testTag("confirm_remove_device_btn")
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // VIEW DETAILS DIALOG
    val currentDetailsToShow = deviceDetailsToShow
    if (currentDetailsToShow != null) {
        val device = currentDetailsToShow
        val isThisDevice = device.serialNumber == currentDeviceId
        val osLower = device.operatingSystem.lowercase(java.util.Locale.ROOT)
        val isMobileOs = osLower == "android" || osLower == "ios"
        
        // Obfuscate IP for privacy
        val partialIp = if (device.ipAddress.contains(".")) {
            val parts = device.ipAddress.split(".")
            if (parts.size >= 4) "${parts[0]}.${parts[1]}.xxx.xxx" else device.ipAddress
        } else {
            device.ipAddress
        }

        AlertDialog(
            onDismissRequest = { deviceDetailsToShow = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isThisDevice) BrandGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (device.deviceType) {
                                "Desktop" -> Icons.Default.Computer
                                "Tablet" -> Icons.Default.TabletAndroid
                                else -> Icons.Default.PhoneAndroid
                            },
                            contentDescription = device.deviceType,
                            tint = if (isThisDevice) BrandGreenPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = device.deviceName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isThisDevice) "This Current Device" else "Stored Active Session",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isThisDevice) BrandGreenPrimary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Category: Device Details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Device Information",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DetailRow(label = "Brand", value = device.brand)
                        DetailRow(label = "Device Type", value = device.deviceType)
                        DetailRow(label = "Operating System", value = device.operatingSystem)
                        DetailRow(label = "OS Version", value = device.osVersion)
                        
                        if (!isMobileOs) {
                            DetailRow(label = "Browser", value = device.browser)
                            DetailRow(label = "Browser Version", value = device.browserVersion)
                        }
                    }

                    // Category: Connection & Status Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Session & Security",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        // Status Row with dynamic color pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Session Status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (device.isActive) BrandGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (device.isActive) "ACTIVE" else "INACTIVE",
                                    color = if (device.isActive) BrandGreenPrimary else MaterialTheme.colorScheme.outline,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        DetailRow(label = "Login Status", value = device.loginStatus.uppercase())
                        DetailRow(label = "IP Address (Obfuscated)", value = partialIp)
                        DetailRow(label = "Estimated Location", value = device.location)
                        DetailRow(
                            label = "Last Active",
                            value = device.updated.toDateTime()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { deviceDetailsToShow = null },
                    modifier = Modifier.testTag("close_device_details_btn")
                ) {
                    Text("Close", color = BrandGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}


@Composable
fun SyncStrategyRow(strategy: String, details: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = strategy,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = BrandGreenPrimary,
            modifier = Modifier.width(110.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = details,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}




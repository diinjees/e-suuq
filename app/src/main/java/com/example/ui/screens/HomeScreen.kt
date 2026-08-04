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

import com.example.ui.theme.BrandGoldSecondary

import com.example.ui.theme.BrandGreenPrimary

import com.example.ui.theme.BrandRedTertiary

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch


import com.example.ui.*

import com.example.ui.screens.*

import com.example.ui.components.*

import com.example.ui.utils.*

@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val showNotificationsPage by viewModel.showNotificationsPage.collectAsStateWithLifecycle()
    val isProductsLoading by viewModel.isProductsLoading.collectAsStateWithLifecycle()
    val sellerProfile by viewModel.sellerProfile.collectAsStateWithLifecycle()

    val dynamicStores = remember(sellerProfile, activeUser, products) {
        val list = STORES_LIST.toMutableList()
        val bizName = activeUser?.businessName?.takeIf { it.isNotBlank() }
            ?: sellerProfile?.businessName?.takeIf { it.isNotBlank() }
        if (!bizName.isNullOrBlank() && list.none { it.name.equals(bizName, ignoreCase = true) }) {
            list.add(
                0,
                StoreData(
                    name = bizName,
                    emoji = "🏪",
                    color = BrandGreenPrimary,
                    businessGrade = "4.9 ★ Verified Seller",
                    description = sellerProfile?.businessDescription?.takeIf { it.isNotBlank() } ?: "Official seller store with live stock",
                    bannerImageRes = "img_banner_1"
                )
            )
        }
        products.mapNotNull { it.sellerName.takeIf { s -> s.isNotBlank() } }.distinct().forEach { sellerName ->
            if (list.none { it.name.equals(sellerName, ignoreCase = true) }) {
                list.add(
                    StoreData(
                        name = sellerName,
                        emoji = "🛍️",
                        color = BrandGreenPrimary,
                        businessGrade = "4.8 ★ Verified Seller",
                        description = "Trusted local market vendor",
                        bannerImageRes = "img_banner_2"
                    )
                )
            }
        }
        list
    }

    val currentRole = activeUser?.role ?: "BUYER"

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val showUserSearchPage by viewModel.showUserSearchPage.collectAsStateWithLifecycle()
    var previousScrollValue by remember { mutableStateOf(0) }
    var isSearchBarVisible by remember { mutableStateOf(true) }
    val viewAllPageType by viewModel.viewAllPageType.collectAsStateWithLifecycle()
    val showPromoDetailsIndex by viewModel.showPromoDetailsIndex.collectAsStateWithLifecycle()

    var lastRefreshTime by remember { mutableStateOf(0L) }
    var showRefreshIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.value) {
        val currentScroll = scrollState.value
        if (currentScroll > previousScrollValue && currentScroll > 50) {
            isSearchBarVisible = false
        } else if (currentScroll < previousScrollValue) {
            isSearchBarVisible = true
            // Rate-limited scroll-up trigger (refresh if close to top and at least 15s since last refresh)
            val currentTime = System.currentTimeMillis()
            if (currentScroll < 200 && (currentTime - lastRefreshTime > 15000L)) {
                lastRefreshTime = currentTime
                showRefreshIndicator = true
                scope.launch {
                    viewModel.refreshProducts(forceRefresh = true)
                    delay(1500)
                    showRefreshIndicator = false
                }
            }
        }
        previousScrollValue = currentScroll
    }

    // Primary Slide Carousel State
    var carouselIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            carouselIndex = (carouselIndex + 1) % 2
        }
    }

    // Secondary Stores sliding carousel state
    var storeCarouselIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            storeCarouselIndex = (storeCarouselIndex + 1) % 2
        }
    }

    // Ticking Countdown Timer for Flash Sale
    var timeLeftSeconds by remember { mutableStateOf(10400) } // ~3 hours
    LaunchedEffect(Unit) {
        while (timeLeftSeconds > 0) {
            delay(1000)
            timeLeftSeconds--
        }
    }
    val hours = timeLeftSeconds / 3600
    val minutes = (timeLeftSeconds % 3600) / 60
    val seconds = timeLeftSeconds % 60

    if (showNotificationsPage) {
        NotificationPage(viewModel)
    } else if (showUserSearchPage) {
        SellersAndDeliverersSearchPage(onBack = { viewModel.setShowUserSearchPage(false) }, viewModel = viewModel)
    } else if (viewAllPageType != null) {
        ViewAllPage(pageType = viewAllPageType!!, onBack = { viewModel.setViewAllPageType(null) }, viewModel = viewModel)
    } else if (showPromoDetailsIndex != null) {
        PromoDetailsPage(promoIndex = showPromoDetailsIndex!!, onBack = { viewModel.setShowPromoDetailsIndex(null) }, viewModel = viewModel)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // FIXED App Bar Top (Does NOT move/scroll with content)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .shadow(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar simulation
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeUser?.fullName?.firstOrNull()?.toString() ?: "U",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "welcome_back",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = activeUser?.fullName ?: "Guest",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Refresh Products button
                    IconButton(onClick = { viewModel.refreshProducts(forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Products", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    // Notification Icon with badge count (triggers notification page)
                    IconButton(
                        onClick = { viewModel.setShowNotificationsPage(true) },
                        modifier = Modifier.testTag("notification_bell_icon")
                    ) {
                        Box {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface)
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(BrandRedTertiary)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "3",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    // Jump to Orders button
                    IconButton(onClick = { 
                        viewModel.ordersBackTarget.value = "HOME"
                        viewModel.navigateToProfileSub("MY_ORDERS")
                        viewModel.selectTab(MainTab.PROFILE)
                    }) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "My Orders", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Row 2: Search Bar moved to App Bar with scroll-controlled visibility
                val currentRole = activeUser?.role ?: "BUYER"
                AnimatedVisibility(
                    visible = isSearchBarVisible && (currentRole == "SELLER" || currentRole == "DELIVERY"),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val placeholderText = if (currentRole == "SELLER" || currentRole == "DELIVERY") {
                        "Search sellers and delivery agents..."
                    } else {
                        "Search phones, coffee, traditional clothes..."
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(23.dp))
                            .clickable {
                                if (currentRole == "SELLER" || currentRole == "DELIVERY") {
                                    viewModel.setShowUserSearchPage(true)
                                } else {
                                    viewModel.setHomeSearch("", null)
                                    viewModel.selectTab(MainTab.MIDDLE)
                                }
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = placeholderText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showRefreshIndicator,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandGreenPrimary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.8.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Refreshing community products...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Scrollable Content Feed
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Promo Banner Carousel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(180.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = getBannerPainter(if (carouselIndex == 0) "img_banner_1" else "img_banner_2"),
                        contentDescription = "Promo Carousel",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BrandGoldSecondary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (carouselIndex == 0) "NEW UPDATE" else "LIMITED TIME",
                                    color = Color.Black,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (carouselIndex == 0) "E-Suuq Super-App" else "E-Suuq Big Discounts",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            )
                            Text(
                                text = if (carouselIndex == 0) "Sellers, Buyers, Delivery in Ethiopia" else "Use code ETHIO30 for 30% off local Teff and Coffee",
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Dot Carousel Indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (carouselIndex == index) 16.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (carouselIndex == index) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant)
                                .animateContentSize()
                        )
                    }
                }

                // ==========================================
                // SECTION 1: Categories List (Horizontal)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setViewAllPageType("categories") }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = loc("categories"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.sp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "See All",
                            tint = BrandGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val categories = listOf("Electronics", "Fashion", "Home", "Beauty", "Toys", "Sport", "Book")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(items = categories,key = { cat -> cat }) { cat ->
                        val (icon, color) = when (cat) {
                            "Electronics" -> Pair(Icons.Default.Devices, BrandGreenPrimary)
                            "Fashion" -> Pair(Icons.Default.Checkroom, BrandGoldSecondary)
                            "Home" -> Pair(Icons.Default.Home, Color(0xFF3F51B5))
                            "Beauty" -> Pair(Icons.Default.Palette, BrandRedTertiary)
                            "Toys" -> Pair(Icons.Default.SportsEsports, Color(0xFF9C27B0))
                            "Sport" -> Pair(Icons.Default.SportsSoccer, Color(0xFFE91E63))
                            "Book" -> Pair(Icons.Default.Book, Color(0xFF009688))
                            else -> Pair(Icons.Default.ColorLens, MaterialTheme.colorScheme.primary)
                        }
                        Card(
                            onClick = {
                                viewModel.setHomeSearch("", cat)
                                viewModel.selectTab(MainTab.MIDDLE)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
                            modifier = Modifier.testTag("category_card_$cat")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = cat, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION 2: Big Save (Flash Sale with Countdown)
                // ==========================================
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = "Flash Sale", tint = BrandRedTertiary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = loc("big_save"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    // Real ticking timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(hours, minutes, seconds).forEachIndexed { idx, value ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BrandRedTertiary.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, BrandRedTertiary.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = String.format("%02d", value),
                                    color = BrandRedTertiary,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            if (idx < 2) {
                                Text(":", color = BrandRedTertiary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isProductsLoading) {
                        items(3) {
                            ProductSkeletonCard(modifier = Modifier.width(160.dp))
                        }
                    } else {
                        items(products.filter { it.isPromoted },key = { prod -> prod.id }) { prod ->
                            ProductCardSmall(prod, onClick = { viewModel.setSelectedProductForDetail(prod) }, modifier = Modifier.width(160.dp))
                        }
                    }
                }

                // ==========================================
                // SECTION 3: Top Stores Showcase
                // ==========================================
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setViewAllPageType("stores") }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = loc("top_stores"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.sp)
                        )
                        Text(
                            text = "Directly support trusted local Ethiopian sellers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "See All",
                            tint = BrandGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(items = dynamicStores, key = { store -> store.name }) { store ->
                        Column(
                            modifier = Modifier
                                .width(90.dp)
                                .clickable {
                                    viewModel.setSelectedStoreName(store.name)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(store.color.copy(alpha = 0.18f), store.color.copy(alpha = 0.03f))
                                        )
                                    )
                                    .border(2.dp, store.color.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = store.emoji, fontSize = 32.sp)
                            }
                            Text(
                                text = store.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(10.dp))
                                Text(
                                    text = store.businessGrade.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION 4: Top New Products
                // ==========================================
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrandGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = loc("top_new"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    Text(
                        text = "LATEST ARRIVALS",
                        color = BrandGreenPrimary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isProductsLoading) {
                        items(3) {
                            ProductSkeletonCard(modifier = Modifier.width(160.dp))
                        }
                    } else {
                        val newArrivals = products.sortedByDescending { it.id }
                        items(newArrivals) { prod ->
                            ProductCardSmall(prod, onClick = { viewModel.setSelectedProductForDetail(prod) }, modifier = Modifier.width(160.dp))
                        }
                    }
                }

                // ==========================================
                // SECTION 5: Premium Brands Showcase
                // ==========================================
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setViewAllPageType("brands") }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = loc("premium_brands"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.sp)
                        )
                        Text(
                            text = "Browse world-class authentic global brands",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "See All",
                            tint = BrandGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val premiumBrandsList = listOf(
                    Triple("Apple", "🍎", "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=120&auto=format&fit=crop&q=60"),
                    Triple("Samsung", "📱", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=120&auto=format&fit=crop&q=60"),
                    Triple("Nike", "👟", "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=120&auto=format&fit=crop&q=60"),
                    Triple("Adidas", "🏃‍♂️", "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=120&auto=format&fit=crop&q=60"),
                    Triple("Sony", "🎧", "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=120&auto=format&fit=crop&q=60"),
                    Triple("LG", "📺", "https://images.unsplash.com/photo-1593305841991-05c297ba4575?w=120&auto=format&fit=crop&q=60"),
                    Triple("HP", "💻", "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=120&auto=format&fit=crop&q=60")
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(items = premiumBrandsList,key = { (brandName, _, _) -> brandName }) { (brandName, emoji, imageUrl) ->
                        Card(
                            onClick = {
                                viewModel.setHomeSearch(brandName, null)
                                viewModel.selectTab(MainTab.MIDDLE)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .width(130.dp)
                                .shadow(2.dp, RoundedCornerShape(16.dp))
                                .testTag("brand_card_$brandName")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Image(
                                        painter = getBannerPainter(imageUrl),
                                        contentDescription = brandName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text(
                                    text = brandName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION 6: Sliding Banner Carousel for Stores
                // ==========================================
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Featured Stores Promotion",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(140.dp)
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            viewModel.setShowPromoDetailsIndex(storeCarouselIndex)
                        }
                ) {
                    Image(
                        painter = getBannerPainter(if (storeCarouselIndex == 0) "img_banner_1" else "img_banner_2"),
                        contentDescription = "Store Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BrandGreenPrimary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (storeCarouselIndex == 0) "VERIFIED STORE" else "LOCAL COOPERATIVE",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (storeCarouselIndex == 0) "Bole Traditional Boutique" else "Amanuel Weaving Cooperative",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (storeCarouselIndex == 0) "Handmade Habesha traditional clothes direct" else "Authentic hand-woven fabrics & local art support",
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Dot Indicators for Store Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (storeCarouselIndex == index) 16.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (storeCarouselIndex == index) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant)
                                .animateContentSize()
                        )
                    }
                }

                // ==========================================
                // SECTION 7: Popular Trending (Visual Grid Layout)
                // ==========================================
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectTab(MainTab.MIDDLE) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = loc("popular"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Ethiopia's highest-velocity trending items today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "See All",
                            tint = BrandGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Build a fluid, responsive Grid of ProductCards safely within vertical scroll
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val columns = when {
                    screenWidth >= 800 -> 5
                    screenWidth >= 600 -> 4
                    screenWidth >= 420 -> 3
                    else -> 2
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isProductsLoading) {
                        repeat(2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                repeat(columns) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProductSkeletonCard()
                                    }
                                }
                            }
                        }
                    } else {
                        val chunkedProducts = products.chunked(columns)
                        chunkedProducts.forEach { rowGroup ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowGroup.forEach { prod ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProductCardSmall(prod, onClick = { viewModel.setSelectedProductForDetail(prod) })
                                    }
                                }
                                if (rowGroup.size < columns) {
                                    repeat(columns - rowGroup.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


@Composable
fun NotificationPage(viewModel: AppViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .shadow(1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setShowNotificationsPage(false) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to home",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TextButton(onClick = { /* Simulated Clear action */ }) {
                Text("Mark all as read", color = BrandGreenPrimary, fontWeight = FontWeight.Bold)
            }
        }

        // List of realistic Ethiopian contextual notifications
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            val notificationsList = listOf(
                Triple(
                    "Order Delivered Successfully 📦",
                    "Your order #1024 has been successfully picked up by the courier. Delivery agent Guled Farah received the payment via Telebirr.",
                    "2 mins ago"
                ),
                Triple(
                    "Super Weekend Save Live! 🔥",
                    "Flash Sale Alert! Local Sidama Organic Coffee and fresh Teff are 30% off for the next 2 hours. Tap to browse discounts.",
                    "1 hour ago"
                ),
                Triple(
                    "New Partner Store Live! 🦁",
                    "Anbessa Shoe Cooperative is now live on E-Suuq! Shop direct with 0% middleman markup fees.",
                    "5 hours ago"
                ),
                Triple(
                    "Welcome to E-Suuq! ✨",
                    "Explore direct connections with Ethiopian local farmers, weavers, and verified couriers in Addis Ababa and beyond.",
                    "Yesterday"
                )
            )

            items(notificationsList) { (title, message, time) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when {
                                title.contains("Order", ignoreCase = true) -> {
                                    val targetOrder = orders.firstOrNull { it.status == "DELIVERED" } ?: orders.firstOrNull()
                                    if (targetOrder != null) {
                                        viewModel.selectOrderDetail(targetOrder)
                                        viewModel.setShowNotificationsPage(false)
                                        viewModel.selectTab(MainTab.PROFILE)
                                    }
                                }
                                title.contains("Weekend", ignoreCase = true) -> {
                                    val targetProduct = products.find { it.name.contains("Coffee", ignoreCase = true) || it.name.contains("Teff", ignoreCase = true) }
                                    if (targetProduct != null) {
                                        viewModel.setSelectedProductForDetail(targetProduct)
                                        viewModel.setShowNotificationsPage(false)
                                        viewModel.selectTab(MainTab.HOME)
                                    } else {
                                        viewModel.setShowNotificationsPage(false)
                                        viewModel.selectTab(MainTab.HOME)
                                    }
                                }
                                title.contains("Partner", ignoreCase = true) -> {
                                    viewModel.setSelectedStoreName("Anbessa Shoe")
                                    viewModel.setShowNotificationsPage(false)
                                    viewModel.selectTab(MainTab.HOME)
                                }
                                else -> {
                                    viewModel.setShowNotificationsPage(false)
                                    viewModel.selectTab(MainTab.HOME)
                                }
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = time,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SellersAndDeliverersSearchPage(onBack: () -> Unit, viewModel: AppViewModel) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SearchScreen(viewModel = viewModel, onBack = onBack)
        }
    }
}


@Composable
fun ViewAllPage(
    pageType: String,
    onBack: () -> Unit,
    viewModel: AppViewModel
) {
    val products by viewModel.products.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when(pageType) {
                                "categories" -> "All Categories"
                                "stores" -> "Top Ethiopian Stores"
                                "brands" -> "Premium Brands"
                                "trending" -> "Trending Marketplace"
                                else -> "All Collections"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when(pageType) {
                                "categories" -> "Browse by product categories"
                                "stores" -> "Direct from authentic local sellers"
                                "brands" -> "World-class verified authentic products"
                                "trending" -> "Fastest moving products in Ethiopia"
                                else -> "Explore all items"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        var searchQuery by remember { mutableStateOf("") }
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val gridColumns = when {
            screenWidth >= 1200 -> 5
            screenWidth >= 900 -> 4
            screenWidth >= 600 -> 3
            else -> 2
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Live Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("view_all_search_input"),
                placeholder = { 
                    Text(
                        text = when(pageType) {
                            "categories" -> "Search categories..."
                            "stores" -> "Search stores..."
                            "brands" -> "Search brands..."
                            "trending" -> "Search trending products..."
                            else -> "Search..."
                        }
                    ) 
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandGreenPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGreenPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            when(pageType) {
                "categories" -> {
                    val cats = listOf(
                        Pair("Electronics", "📱"),
                        Pair("Fashion", "👗"),
                        Pair("Home", "🏠"),
                        Pair("Beauty", "💄"),
                        Pair("Toys", "🧸"),
                        Pair("Sport", "⚽"),
                        Pair("Book", "📚")
                    )
                    val filteredCats = cats.filter {
                        it.first.contains(searchQuery, ignoreCase = true)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredCats) { (c, emoji) ->
                            Card(
                                onClick = {
                                    viewModel.setHomeSearch("", c)
                                    viewModel.selectTab(MainTab.MIDDLE)
                                    onBack()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(BrandGreenPrimary.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 28.sp)
                                    }
                                    Text(
                                        text = c,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        text = "Explore traditional & modern ${c.lowercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                "stores" -> {
                    val filteredStores = STORES_LIST.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.businessGrade.contains(searchQuery, ignoreCase = true)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredStores) { store ->
                            Card(
                                onClick = {
                                    viewModel.setSelectedStoreName(store.name)
                                    onBack()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, store.color.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(store.color.copy(alpha = 0.12f))
                                            .border(1.5.dp, store.color.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(store.emoji, fontSize = 32.sp)
                                    }
                                    Text(
                                        text = store.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BrandGreenPrimary.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = store.businessGrade,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = BrandGreenPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "brands" -> {
                    val premiumBrandsList = listOf(
                        Triple("Apple", "🍎", "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=120&auto=format&fit=crop&q=60"),
                        Triple("Samsung", "📱", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=120&auto=format&fit=crop&q=60"),
                        Triple("Nike", "👟", "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=120&auto=format&fit=crop&q=60"),
                        Triple("Adidas", "🏃‍♂️", "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=120&auto=format&fit=crop&q=60"),
                        Triple("Sony", "🎧", "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=120&auto=format&fit=crop&q=60"),
                        Triple("LG", "📺", "https://images.unsplash.com/photo-1593305841991-05c297ba4575?w=120&auto=format&fit=crop&q=60"),
                        Triple("HP", "💻", "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=120&auto=format&fit=crop&q=60")
                    )
                    val filteredBrands = premiumBrandsList.filter {
                        it.first.contains(searchQuery, ignoreCase = true)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredBrands) { (brandName, emoji, imageUrl) ->
                            Card(
                                onClick = {
                                    viewModel.setHomeSearch(brandName, null)
                                    viewModel.selectTab(MainTab.MIDDLE)
                                    onBack()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Image(
                                            painter = getBannerPainter(imageUrl),
                                            contentDescription = brandName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Text(
                                        text = brandName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        text = "Official Distributor",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandGreenPrimary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                "trending" -> {
                    val trending = products.take(10).filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.brand.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(trending) { prod ->
                            ProductCardSmall(prod, onClick = {
                                viewModel.setSelectedProductForDetail(prod)
                                onBack()
                            })
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PromoDetailsPage(
    promoIndex: Int,
    onBack: () -> Unit,
    viewModel: AppViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val title = if (promoIndex == 0) {
        "Sellers Direct Promotion: 0% Middleman Fees!"
    } else {
        "Exclusive Handwoven Habesha & Coffee Discounts"
    }

    val desc = if (promoIndex == 0) {
        "E-Suuq is introducing a brand new transaction channel! In collaboration with local agricultural unions, you can now purchase Sidamo Organic Arabica Teff grains directly from verified farming families. By bypassing middlemen completely, farmers receive 100% of their asking price and buyers save up to 22% compared to standard regional marketplace rates.\n\nBrowse verified cooperatives using our Users Directory or search for agricultural specialties on the central marketplace today."
    } else {
        "In celebration of the regional weaving cooperative season, purchase traditional hand-woven Habesha Kemis festive garments at up to 40% off. Additionally, pair your attire with freshly slow-roasted Sidamo premium coffee beans direct from central Gulele Roasters.\n\nAll items are fully Telebirr certified with guaranteed 1-day local motorcycle delivery to your doorstep. Supporting community craftsmen has never been simpler."
    }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Promotional Offer Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Image(
                    painter = getBannerPainter(if (promoIndex == 0) "img_banner_1" else "img_banner_2"),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandGoldSecondary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (promoIndex == 0) "DIRECT SALE" else "SEASON SALE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.Black
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrandGreenPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Verified 100% Genuine",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreenPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Telebirr Accepted",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "About This Offer",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        android.widget.Toast.makeText(context, "Coupon claimed successfully! Your discount will be auto-applied at checkout.", android.widget.Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Claim Discount Coupon",
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.selectTab(MainTab.MIDDLE)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.2.dp, BrandGreenPrimary),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreenPrimary)
                ) {
                    Text(
                        text = "Browse Eligible Products",
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}



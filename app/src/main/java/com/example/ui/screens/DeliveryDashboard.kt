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

import com.example.data.DeliveryOrderEntity

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
fun DeliveryDashboardScreen(viewModel: AppViewModel) {
    val dashboardContext = androidx.compose.ui.platform.LocalContext.current
    val orders by viewModel.deliveryOrders.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val isOnline by viewModel.isCourierOnline.collectAsStateWithLifecycle()
    val pricePerKm by viewModel.pricePerKm.collectAsStateWithLifecycle()
    val courierAvatar by viewModel.courierAvatar.collectAsStateWithLifecycle()


    var activeTab by remember { mutableStateOf(0) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }
    var shiftMinutes by remember { mutableStateOf(272) }
    var isShiftActive by remember { mutableStateOf(true) }
    
    var showSosDialog by remember { mutableStateOf(false) }
    var activeSosReason by remember { mutableStateOf<String?>(null) }
    
    var showPlateEditDialog by remember { mutableStateOf(false) }
    var plateEditInput by remember { mutableStateOf("") }
    
    var rangeStartDay by remember { mutableStateOf<Int?>(1) }
    var rangeEndDay by remember { mutableStateOf<Int?>(31) }
    var showPerformanceCalendarDialog by remember { mutableStateOf(false) }
    
    var showHistoriesSubPage by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = showHistoriesSubPage) {
        showHistoriesSubPage = false
    }
    var showFinalDeliveryDialogForOrder by remember { mutableStateOf<DeliveryOrderEntity?>(null) }
    var enteredDeliveryCode by remember { mutableStateOf("") }
    var deliveryCodeError by remember { mutableStateOf<String?>(null) }

    val deliveryId = activeUser?.deliveryProfileId ?: activeUser?.id ?: ""

    // ✅ Subscribe to real-time delivery orders ONLY when inside DeliveryDashboard AND status is online
    DisposableEffect(isOnline, deliveryId) {
        if (isOnline && deliveryId.isNotBlank()) {
            viewModel.subscribeDeliveryRealtime(deliveryId)
        } else {
            viewModel.unsubscribeDeliveryRealtime(deliveryId)
        }

        onDispose {
            if (deliveryId.isNotBlank()) {
                viewModel.unsubscribeDeliveryRealtime(deliveryId)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAvailableDeliveryOrders()
        viewModel.getActiveRide()
    }

    LaunchedEffect(activeUser?.id) {
        if (activeUser != null) {
            plateEditInput = activeUser?.plateNumber ?: ""
            viewModel.getActiveRide()
        }
    }

    LaunchedEffect(isShiftActive) {
        while (isShiftActive) {
            kotlinx.coroutines.delay(60000L)
            shiftMinutes += 1
        }
    }


    val unassignedJobs = orders.filter { !it.isSelfPickup && (it.courierName.isNullOrBlank() && it.deliveryId.isNullOrBlank()) && it.status == "READY_FOR_PICKUP" }
    val myActiveDeliveries = orders.filter { !it.isSelfPickup && it.deliveryId == activeUser?.deliveryProfileId && it.status != "DELIVERED" && it.status != "CANCELLED" }
    
    val activeDeliveryIds = remember(myActiveDeliveries) { myActiveDeliveries.map { it.id }.toSet() }
    val currentActiveDeliveries by rememberUpdatedState(myActiveDeliveries)

    // 30-second driverLocation update loop for active claimed rides
    LaunchedEffect(activeDeliveryIds) {
        if (activeDeliveryIds.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(60000L)
                currentActiveDeliveries.forEach { activeOrder ->
                    val startLat = if (activeOrder.driverLocation.lat != 0.0) activeOrder.driverLocation.lat else activeOrder.storeLocation.lat
                    val startLon = if (activeOrder.driverLocation.lon != 0.0) activeOrder.driverLocation.lon else activeOrder.storeLocation.lon
                    val destLat = if (activeOrder.shippingAddress.lat != 0.0) activeOrder.shippingAddress.lat else 9.0210
                    val destLon = if (activeOrder.shippingAddress.lon != 0.0) activeOrder.shippingAddress.lon else 38.7650
                    
                    val liveLoc = getUserLiveLocation(dashboardContext)
                    val nextLat = liveLoc?.first ?: (startLat + (destLat - startLat) * 0.02)
                    val nextLon = liveLoc?.second ?: (startLon + (destLon - startLon) * 0.02)
                    
                    viewModel.updateDriverLocation(activeOrder.id, nextLat, nextLon)
                }
            }
        }
    }

    val completedRides = orders.count { it?.deliveryId != null && it.status == "DELIVERED" } + 28
    
    val currentLevel = when {
        completedRides >= 200 -> 10
        completedRides >= 160 -> 9
        completedRides >= 130 -> 8
        completedRides >= 101 -> 7
        completedRides >= 76 -> 6
        completedRides >= 51 -> 5
        completedRides >= 31 -> 4
        completedRides >= 16 -> 3
        completedRides >= 6 -> 2
        else -> 1
    }

    val levelNames = listOf("", "Novice Rider", "Bronze Wheeler", "Silver Swift", "Gold Runner", "Platinum Navigator", "Diamond Elite", "Master Courier", "Grandmaster Deliverer", "Legend Velocity", "Apex Transporter")
    val currentLevelName = levelNames.getOrElse(currentLevel) { "Apex Transporter" }
    val nextLevelTarget = listOf(0, 6, 16, 31, 51, 76, 101, 130, 160, 200, 200)[currentLevel.coerceIn(1, 10)]
    val prevLevelTarget = listOf(0, 0, 6, 16, 31, 51, 75, 100, 130, 160, 200)[currentLevel.coerceIn(1, 10)]
    val progressRatio = if (currentLevel == 10) 1.0f else ((completedRides - prevLevelTarget).toFloat() / (nextLevelTarget - prevLevelTarget)).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        if (showHistoriesSubPage) {
            HistoriesProductsDeliveredSubPage(
                viewModel = viewModel,
                onBack = { showHistoriesSubPage = false }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (activeSosReason != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("active_sos_banner"),
                        colors = CardDefaults.cardColors(containerColor = BrandRedTertiary.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, BrandRedTertiary)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = BrandRedTertiary, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "EMERGENCY SOS ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrandRedTertiary
                                )
                                Text(
                                    text = "Reason: ${activeSosReason}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Support has been notified. Stay safe.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    activeSosReason = null
                                    android.widget.Toast.makeText(dashboardContext, "SOS Emergency resolved.", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRedTertiary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("resolve_sos_btn")
                            ) {
                                Text("Resolve", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                
                // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val avatarColor = when (courierAvatar) {
                            "ic_avatar_blue" -> Color(0xFF1D4ED8)
                            "ic_avatar_yellow" -> Color(0xFFD97706)
                            "ic_avatar_orange" -> Color(0xFFEA580C)
                            else -> BrandGreenPrimary
                        }
                        Box(
                            modifier = Modifier.size(44.dp).background(avatarColor, CircleShape).clickable { showAvatarPickerDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(activeUser?.fullName ?: "Guled Farah", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Bicycle Rider • Level $currentLevel ($currentLevelName)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Plate: ${activeUser?.plateNumber ?: "Not set"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (isOnline) "ONLINE" else "OFFLINE", style = MaterialTheme.typography.labelSmall, color = if (isOnline) BrandGreenPrimary else BrandRedTertiary)
                        Switch(
                            checked = isOnline,
                            onCheckedChange = {
                                val success = viewModel.toggleCourierOnline()
                                if (!success) {
                                    android.widget.Toast.makeText(dashboardContext, "Cannot go offline while you have active rides!", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.testTag("courier_online_switch_tab")
                        )
                    }
                }
                TabRow(selectedTabIndex = activeTab, containerColor = Color.Transparent) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Live Jobs", fontSize = 11.sp, fontWeight = FontWeight.Bold) }, icon = { Icon(Icons.Default.DirectionsBike, null, modifier = Modifier.size(16.dp)) })
                    Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("My Level", fontSize = 11.sp, fontWeight = FontWeight.Bold) }, icon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp)) })
                    Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Stats & History", fontSize = 11.sp, fontWeight = FontWeight.Bold) }, icon = { Icon(Icons.Default.BarChart, null, modifier = Modifier.size(16.dp)) })
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                0 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (isOnline) BrandGreenPrimary.copy(alpha = 0.05f) else BrandRedTertiary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, if (isOnline) BrandGreenPrimary else BrandRedTertiary)
                            ) {
                                Text(
                                    text = if (isOnline) "🟢 Active & Ready to Accept Rides" else "⚫ You are currently offline. Switch to ONLINE to receive jobs.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        item { Text("My Active Rides", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                        if (myActiveDeliveries.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),border = BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
                                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("No active deliveries. Accept a job from the finder below!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        } else {
                            items(myActiveDeliveries.size) { idx ->
                                val order = myActiveDeliveries[idx]
                                val pickupCode = order.deliveryVerifCode ?: ((viewModel.getNumericId(order.id) * 179) % 900000 + 100000).toString()
                                val distText = String.format("%.1f km", (viewModel.getNumericId(order.id) * 1.3 % 3.5) + 1.2)
                                val baseFare = ((viewModel.getNumericId(order.id) * 1.3 % 3.5) + 1.2) * pricePerKm

                                Card(modifier = Modifier.fillMaxWidth().testTag("active_ride_card_${order.id}"),colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Order #${order.id}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(if (order.status == "READY_FOR_PICKUP") "En Route to Store" else "Out for Delivery", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (order.status == "READY_FOR_PICKUP") BrandGoldSecondary else BrandGreenPrimary)
                                        }
                                        Divider()
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Box(modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.ShoppingBag, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(28.dp))
                                            }
                                            Column {
                                                Text("Items: ${order.productNames}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                Text("Store: ${if (order.sellerName.isNotBlank()) order.sellerName else "Store #" + order.sellerId}", style = MaterialTheme.typography.bodySmall, color = BrandGoldSecondary)
                                                Text("To: ${if (order.buyerName.isNotBlank()) order.buyerName else "Customer"} (${if (order.shippingAddress.lat != 0.0) "${String.format("%.4f", order.shippingAddress.lat)}, ${String.format("%.4f", order.shippingAddress.lon)}" else "Addis Ababa"})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("Dist: $distText • Est: ${String.format("%,.0f ETB", baseFare)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                            }
                                        }

                                        // OpenStreetMap Real-Time Delivery Tracking Card
                                        OpenStreetMapOrderTracker(
                                            sellerLat = order.storeLocation.lat,
                                            sellerLon = order.storeLocation.lon,
                                            buyerLat = if (order.shippingAddress.lat != 0.0) order.shippingAddress.lat else 9.0210,
                                            buyerLon = if (order.shippingAddress.lon != 0.0) order.shippingAddress.lon else 38.7650,
                                            driverLat = order.driverLocation.lat.takeIf { it != 0.0 },
                                            driverLon = order.driverLocation.lon.takeIf { it != 0.0 },
                                            progress = if (order.status == "READY_FOR_PICKUP") 0.25f else 0.70f,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            onLocationUpdate = { newLat, newLon ->
                                                viewModel.updateDriverLocation(order.id, newLat, newLon)
                                            },
                                            orderStatus = order.status,
                                            isDeliverySide = true
                                        )

                                        if (order.status == "READY_FOR_PICKUP") {
                                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Pickup Verification Code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(pickupCode, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp), color = BrandGreenPrimary)
                                                    Text("Provide this code to the seller to verify and hand over the items.", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                                                }
                                            }
                                        } else {
                                            val destLat = if (order.shippingAddress.lat != 0.0) order.shippingAddress.lat else 9.0210
                                            val destLon = if (order.shippingAddress.lon != 0.0) order.shippingAddress.lon else 38.7650
                                            val driverLat = order.driverLocation.lat
                                            val driverLon = order.driverLocation.lon
                                            val hasDriverLoc = driverLat != 0.0 && driverLon != 0.0

                                            val distResults = remember(driverLat, driverLon, destLat, destLon) {
                                                if (hasDriverLoc) {
                                                    val results = FloatArray(1)
                                                    android.location.Location.distanceBetween(driverLat, driverLon, destLat, destLon, results)
                                                    results[0]
                                                } else Float.MAX_VALUE
                                            }

                                            val isArrivedAtDestination = hasDriverLoc && distResults <= 10f
                                            val distKm = if (hasDriverLoc) distResults / 1000f else 2.5f
                                            val etaMins = Math.max(1, (distKm * 2.5f).toInt())

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Navigation, null, tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("OSRM Navigation Active", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                                        Text(
                                                            if (isArrivedAtDestination) "Arrived at customer shipping address!"
                                                            else String.format(java.util.Locale.US, "En route • %.2f km remaining (ETA: ~%d mins)", distKm, etaMins),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }

                                            if (isArrivedAtDestination) {
                                                Button(
                                                    onClick = {
                                                        showFinalDeliveryDialogForOrder = order
                                                        enteredDeliveryCode = ""
                                                        deliveryCodeError = null
                                                    },
                                                    modifier = Modifier.fillMaxWidth().testTag("courier_progress_btn_${order.id}"),
                                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Complete Delivery")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Text("Job Finder", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                        if (!isOnline) {
                            item { Card(modifier = Modifier.fillMaxWidth(),colors = CardDefaults.cardColors(containerColor = BrandRedTertiary.copy(alpha = 0.08f)),border = BorderStroke(1.dp,BrandRedTertiary)) { Text("Job Finder disabled while OFFLINE.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.outline) } }
                        } else if (unassignedJobs.isEmpty()) {
                            item { Card(modifier = Modifier.fillMaxWidth(),colors = CardDefaults.cardColors(containerColor = BrandGreenPrimary.copy(alpha = 0.08f)),border = BorderStroke(1.dp,BrandGreenPrimary)) { Text("No available rides nearby.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.outline) } }
                        } else {
                            items(unassignedJobs.size) { idx ->
                                val job = unassignedJobs[idx]
                                val dist = String.format("%.1f km", (viewModel.getNumericId(job.id) * 1.3 % 3.5) + 1.2)
                                val fee = ((viewModel.getNumericId(job.id) * 1.3 % 3.5) + 1.2) * pricePerKm
                                Card(modifier = Modifier.fillMaxWidth().testTag("job_finder_card_${job.id}"),colors = CardDefaults.cardColors(containerColor = BrandGreenPrimary.copy(alpha = 0.08f)),border = BorderStroke(1.dp,BrandGreenPrimary)) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Items: ${job.productNames}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(String.format("%.0f ETB", fee), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("Store: ${if (job.sellerName.isNotBlank()) job.sellerName else "Store #" + job.sellerId} • To: ${if (job.buyerName.isNotBlank()) job.buyerName else "Customer"} (${if (job.shippingAddress.lat != 0.0) "${String.format("%.4f", job.shippingAddress.lat)}, ${String.format("%.4f", job.shippingAddress.lon)}" else "Addis Ababa"})", style = MaterialTheme.typography.bodySmall)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Dist: $dist", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Button(
                                                onClick = {
                                                    if (myActiveDeliveries.isNotEmpty()) {
                                                        android.widget.Toast.makeText(dashboardContext, "You can only have 1 active ride! Please complete your current delivery first.", android.widget.Toast.LENGTH_LONG).show()
                                                    } else {
                                                        val success = viewModel.claimDeliveryJob(job)
                                                        if (!success) {
                                                            android.widget.Toast.makeText(dashboardContext, "You can only have 1 active ride! Please complete your current delivery first.", android.widget.Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.testTag("claim_job_${job.id}")
                                            ) { Text("Accept Job", fontSize = 11.sp) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Level Status & Progress", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    
                                    // Total XP & Rating badges on top of progress info
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Total XP Badge
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(BrandGreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .border(1.dp, BrandGreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(vertical = 8.dp, horizontal = 12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                                                Column {
                                                    Text("TOTAL XP", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                                                    Text("${completedRides * 50} XP", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = BrandGreenPrimary)
                                                }
                                            }
                                        }

                                        // Rating Badge
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(BrandGoldSecondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .border(1.dp, BrandGoldSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(vertical = 8.dp, horizontal = 12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(16.dp))
                                                Column {
                                                    Text("RATING", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                                                    Text("4.9 ★", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = BrandGoldSecondary)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text("Current level: $currentLevel ($currentLevelName) • Complete ${nextLevelTarget - completedRides} more to rank up.", style = MaterialTheme.typography.bodySmall)
                                    LinearProgressIndicator(progress = progressRatio, color = BrandGoldSecondary, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("$completedRides Completed", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                        Text("$nextLevelTarget target", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        item { Text("Level Progression (Levels 1 - 10)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                        val perks = listOf(
                            "Standard Base Tariffs",
                            "+2% surge payout bonus",
                            "Priority dispatch assignment",
                            "+5% bonus, priority support",
                            "Free Hub coffee, +7% bonus",
                            "Dedicated routing lock, +8% bonus",
                            "5% commission discount",
                            "Direct hotline support",
                            "+12% earnings bonus & VIP badge",
                            "0% platform commission, VIP events"
                        )
                        items(10) { idx ->
                            val lvl = idx + 1
                            val isCurrent = currentLevel == lvl
                            val isDone = completedRides >= listOf(0, 0, 6, 16, 31, 51, 76, 101, 131, 161, 201)[lvl]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = if (isCurrent) BorderStroke(2.dp, BrandGreenPrimary) else null,
                                colors = CardDefaults.cardColors(containerColor = if (isCurrent) BrandGreenPrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(32.dp).background(if (isDone) BrandGreenPrimary else MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(lvl.toString(), color = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(levelNames[lvl], style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(perks[idx], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    if (isDone) Icon(Icons.Default.CheckCircle, null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                2 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("today_status_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.size(8.dp).background(if (isOnline) BrandGreenPrimary else MaterialTheme.colorScheme.outline, CircleShape))
                                            Text("Today's Active Status", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(if (isOnline) BrandGreenPrimary.copy(alpha = 0.15f) else BrandRedTertiary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isOnline) "ONLINE" else "OFFLINE",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isOnline) BrandGreenPrimary else BrandRedTertiary
                                            )
                                        }
                                    }

                                    val todayRides = orders.count { it?.deliveryId != null && it.status == "DELIVERED" } + 3
                                    val todayDistance = todayRides * 4.2
                                    val todayEarnings = todayDistance * pricePerKm

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Today Rides", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("$todayRides", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Today Dist", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(String.format("%.1f km", todayDistance), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                        Card(modifier = Modifier.weight(1.1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Today Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(String.format("%.0f ETB", todayEarnings), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Emergency Support Desk",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "Instant assistance & incident reports",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Button(
                                            onClick = { showSosDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandRedTertiary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("emergency_sos_btn")
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("EMERGENCY SOS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val totalDaysInMonth = 31
                                    val selectedDaysCount = if (rangeStartDay != null && rangeEndDay != null) {
                                        (rangeEndDay!! - rangeStartDay!! + 1).coerceAtLeast(1)
                                    } else if (rangeStartDay != null) {
                                        1
                                    } else {
                                        totalDaysInMonth
                                    }

                                    val fraction = selectedDaysCount.toDouble() / totalDaysInMonth.toDouble()
                                    val ridesVal = (completedRides * fraction).toInt().coerceAtLeast(if (selectedDaysCount > 0) 1 else 0)
                                    val distanceVal = ridesVal * 3.8
                                    val earningsVal = distanceVal * pricePerKm

                                    val periodLabel = if (rangeStartDay == 1 && rangeEndDay == 31) {
                                        "July 2026 (All Month)"
                                    } else if (rangeStartDay != null && rangeEndDay != null) {
                                        "July $rangeStartDay to July $rangeEndDay, 2026"
                                    } else if (rangeStartDay != null) {
                                        "July $rangeStartDay, 2026"
                                    } else {
                                        "July 2026"
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Performance Reports", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                text = "Filtered: $periodLabel",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // All Month shortcut button
                                            Button(
                                                onClick = {
                                                    rangeStartDay = 1
                                                    rangeEndDay = 31
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (rangeStartDay == 1 && rangeEndDay == 31) BrandGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (rangeStartDay == 1 && rangeEndDay == 31) BrandGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp).testTag("perf_all_month_btn")
                                            ) {
                                                Text("All Month", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            }

                                            // Calendar Icon Button to open selector
                                            IconButton(
                                                onClick = { showPerformanceCalendarDialog = true },
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                                    .size(32.dp)
                                                    .testTag("choose_perf_calendar_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = "Select Date Range",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = BrandGreenPrimary
                                                )
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Rides", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text("$ridesVal", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text(String.format("%.1f km", distanceVal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                        Card(modifier = Modifier.weight(1.2f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Earnings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text(String.format("%.0f ETB", earningsVal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { generateCourierReportPdf(dashboardContext, activeUser?.fullName ?: "Guled Farah", "Eco Bicycle", activeUser?.deliveryGrade ?: "4.9 Stars", ridesVal, distanceVal, earningsVal, periodLabel) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("pdf_report_btn")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generate PDF Report", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("By-Day Weekly Analytics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    listOf(
                                        Triple("Monday", 4, 15.2),
                                        Triple("Tuesday", 6, 22.8),
                                        Triple("Wednesday", 3, 11.4),
                                        Triple("Thursday", 5, 19.0),
                                        Triple("Friday", 7, 26.6),
                                        Triple("Saturday", 9, 34.2),
                                        Triple("Sunday", 5, 19.0)
                                    ).forEach { (day, rides, distance) ->
                                        val income = distance * pricePerKm
                                        val ratio = (distance / 34.2).toFloat().coerceIn(0.1f, 1.0f)
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(day, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(75.dp))
                                            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                                LinearProgressIndicator(progress = ratio, color = BrandGreenPrimary, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
                                                Text("$rides rides • ${String.format("%.1f km", distance)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Text(String.format("%.0f ETB", income), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("History Products Delivered", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                TextButton(
                                    onClick = { showHistoriesSubPage = true },
                                    modifier = Modifier.testTag("histories_more_btn")
                                ) {
                                    Text("More >", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                }
                            }
                        }
                        
                        val dbDelivered = orders.filter { o -> o?.deliveryId != null && o.status == "DELIVERED" }
                        val staticList = listOf(
                            "Habesha Kemis Outfit" to "Bole Traditional Wear",
                            "Organic Teff Flour (5kg)" to "Mercato Market",
                            "Sidama Specialty Coffee" to "Abyssinia Coffee"
                        )
                        
                        val displayList = mutableListOf<Triple<String, String, Boolean>>()
                        dbDelivered.forEach { displayList.add(Triple(it.productNames, "Seller Name", true)) }
                        staticList.forEach { displayList.add(Triple(it.first, it.second, false)) }
                        
                        val firstThree = displayList.take(3)
                        
                        items(firstThree.size) { idx ->
                            val item = firstThree[idx]
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShoppingBag, null, tint = if (item.third) BrandGreenPrimary else MaterialTheme.colorScheme.outline)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.first, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text("Past Order • Store: ${item.second}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Box(modifier = Modifier.background(BrandGreenPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("VERIFIED", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAvatarPickerDialog) {
            AlertDialog(
                onDismissRequest = { showAvatarPickerDialog = false },
                title = { Text("Select Rider Identity Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("ic_avatar_default" to "Emerald Helmet", "ic_avatar_blue" to "Blue Lightning", "ic_avatar_yellow" to "Amber Falcon", "ic_avatar_orange" to "Sunset Retro").forEach { (id, label) ->
                            val isChosen = courierAvatar == id
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.updateCourierAvatar(id) },
                                colors = CardDefaults.cardColors(containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val indicatorColor = when (id) {
                                        "ic_avatar_blue" -> Color(0xFF1D4ED8)
                                        "ic_avatar_yellow" -> Color(0xFFD97706)
                                        "ic_avatar_orange" -> Color(0xFFEA580C)
                                        else -> BrandGreenPrimary
                                    }
                                    Box(modifier = Modifier.size(14.dp).background(indicatorColor, CircleShape))
                                    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                },
                confirmButton = { Button(onClick = { showAvatarPickerDialog = false }) { Text("Done") } }
            )
        }

        if (showPlateEditDialog) {
            AlertDialog(
                onDismissRequest = { showPlateEditDialog = false },
                title = { Text("Edit Vehicle License Plate") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter your updated vehicle license plate number below:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = plateEditInput,
                            onValueChange = { plateEditInput = it },
                            placeholder = { Text("e.g. A.A-A76543") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("dash_plate_edit_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updatePlateNumber(plateEditInput)
                            showPlateEditDialog = false
                        },
                        modifier = Modifier.testTag("dash_plate_edit_save_btn")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPlateEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showPerformanceCalendarDialog) {
            UnifiedCalendarDialog(
                initialStartDate = rangeStartDay?.let { 
                    Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, it)
                        set(Calendar.MONTH, Calendar.JULY)
                        set(Calendar.YEAR, 2026)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                },
                initialEndDate = rangeEndDay?.let {
                    Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, it)
                        set(Calendar.MONTH, Calendar.JULY)
                        set(Calendar.YEAR, 2026)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                },
                onConfirm = { start, end, label ->
                    rangeStartDay = start?.get(Calendar.DAY_OF_MONTH)
                    rangeEndDay = end?.get(Calendar.DAY_OF_MONTH)
                    showPerformanceCalendarDialog = false
                },
                onDismiss = { showPerformanceCalendarDialog = false },
                title = "Select Date Range for Performance",
                subtitle = "Filter your delivery statistics by date",
                showPresets = false
            )
        }

        if (showFinalDeliveryDialogForOrder != null) {
            val order = showFinalDeliveryDialogForOrder!!
            AlertDialog(
                onDismissRequest = { showFinalDeliveryDialogForOrder = null },
                title = { Text("Complete Final Delivery", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "To complete the delivery, please call the buyer and ask for the secure 6-digit verification code.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        // Buyer Contact Info (Phone Number)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Buyer Phone Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("Buyer Phone" ?: "+251 911 123456", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            IconButton(
                                onClick = { 
                                    // Simulated Call 
                                    android.widget.Toast.makeText(dashboardContext, "Calling ${"Buyer Phone"}...", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.background(BrandGreenPrimary.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call Buyer", tint = BrandGreenPrimary)
                            }
                        }

                        // Simulation helper to let testing/user see the expected code in the developer dialog
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BrandGoldSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Developer Tip (Buyer's Code): ${order.deliveryVerifCode}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF856404)
                            )
                        }

                        // Code input field
                        OutlinedTextField(
                            value = enteredDeliveryCode,
                            onValueChange = {
                                if (it.length <= 6) {
                                    enteredDeliveryCode = it
                                    deliveryCodeError = null
                                }
                            },
                            label = { Text("6-Digit Handover Code") },
                            placeholder = { Text("123456") },
                            isError = deliveryCodeError != null,
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("delivery_code_input")
                        )

                        if (deliveryCodeError != null) {
                            Text(
                                text = deliveryCodeError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (enteredDeliveryCode.length != 6) {
                                deliveryCodeError = "Code must be exactly 6 digits."
                            } else {
                                viewModel.verifyAndCompleteDelivery(
                                    order = order,
                                    enteredCode = enteredDeliveryCode,
                                    onSuccess = {
                                        android.widget.Toast.makeText(dashboardContext, "Order Delivered Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                        showFinalDeliveryDialogForOrder = null
                                    },
                                    onError = { errorMsg ->
                                        deliveryCodeError = errorMsg
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        modifier = Modifier.testTag("verify_delivery_btn")
                    ) {
                        Text("Verify & Deliver")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinalDeliveryDialogForOrder = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showSosDialog) {
            var selectedReason by remember { mutableStateOf("Accident / Crash") }
            val reasons = listOf(
                "Accident / Crash",
                "Broke Vehicle / Flat Tire",
                "Health Emergency / Feeling Unwell",
                "Severe Traffic / Road Blockage",
                "Other Urgent Issue"
            )

            AlertDialog(
                onDismissRequest = { showSosDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = BrandRedTertiary, modifier = Modifier.size(24.dp))
                        Text(
                            text = "Report Emergency SOS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandRedTertiary
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "This will notify dispatch and the customer immediately. Please select the reason for this emergency:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        reasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selectedReason == reason) BrandRedTertiary.copy(alpha = 0.08f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedReason = reason }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = (selectedReason == reason),
                                    onClick = { selectedReason = reason },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandRedTertiary)
                                )
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selectedReason == reason) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (selectedReason == reason) BrandRedTertiary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            activeSosReason = selectedReason
                            showSosDialog = false
                            android.widget.Toast.makeText(dashboardContext, "SOS Sent: $selectedReason. Emergency support is contacting you!", android.widget.Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRedTertiary),
                        modifier = Modifier.testTag("confirm_sos_btn")
                    ) {
                        Text("Trigger SOS")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSosDialog = false },
                        modifier = Modifier.testTag("dismiss_sos_btn")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
}
}


@Composable
fun HistoriesProductsDeliveredSubPage(viewModel: AppViewModel, onBack: () -> Unit) {
    val dashboardContext = androidx.compose.ui.platform.LocalContext.current
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val pricePerKm by viewModel.pricePerKm.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedPickupDate by remember { mutableStateOf<String?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val dbDelivered = orders.filter { o -> o?.deliveryId != null && o.status == "DELIVERED" }
    
    val allItems = remember(dbDelivered) {
        val list = mutableListOf<Triple<String, String, String>>()
        dbDelivered.forEachIndexed { i, o ->
            val date = when (i % 3) {
                0 -> "2026-07-03"
                1 -> "2026-07-12"
                else -> "2026-07-24"
            }
            list.add(Triple(o.productNames, "Seller Name", date))
        }
        list.add(Triple("Habesha Kemis Outfit", "Bole Traditional Wear", "2026-07-03"))
        list.add(Triple("Organic Teff Flour (5kg)", "Mercato Market", "2026-07-12"))
        list.add(Triple("Sidama Specialty Coffee", "Abyssinia Coffee", "2026-07-24"))
        list
    }

    val filteredItems = allItems.filter { item ->
        val matchesSearch = item.first.contains(searchQuery, ignoreCase = true) || item.second.contains(searchQuery, ignoreCase = true)
        val matchesDate = selectedPickupDate == null || item.third == selectedPickupDate
        matchesSearch && matchesDate
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    .testTag("histories_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Delivered Products History",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${filteredItems.size} verified deliveries in records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search products or stores...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("histories_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = { showDatePickerDialog = true },
                    modifier = Modifier
                        .background(if (selectedPickupDate != null) BrandGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .size(48.dp)
                        .border(1.dp, if (selectedPickupDate != null) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .testTag("histories_calendar_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Filter Date",
                        tint = if (selectedPickupDate != null) BrandGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedPickupDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(BrandGreenPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(1.dp, BrandGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(12.dp), tint = BrandGreenPrimary)
                        Text("Pickup Date: $selectedPickupDate", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = BrandGreenPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { selectedPickupDate = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = BrandRedTertiary)
                    ) {
                        Text("Clear Filter", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No history records match filters", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(filteredItems.size) { idx ->
                    val item = filteredItems[idx]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(BrandGreenPrimary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = BrandGreenPrimary, modifier = Modifier.size(24.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Storefront, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text(item.second, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text("Picked up: ${item.third}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .background(BrandGreenPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("VERIFIED", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        UnifiedCalendarDialog(
            initialStartDate = selectedPickupDate?.toCalendar(),
            initialEndDate = selectedPickupDate?.toCalendar(),
            onConfirm = { start, end, label ->
                selectedPickupDate = start?.toDateString()
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false },
            title = "Select Pickup Date",
            subtitle = "Choose a specific date to filter delivery history",
            showPresets = false
        )
    }
}


@Composable
fun Disabled_DeliveryDashboardScreen(viewModel: AppViewModel) {
    val dashboardContext = androidx.compose.ui.platform.LocalContext.current
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val isOnline by viewModel.isCourierOnline.collectAsStateWithLifecycle()
    val pricePerKm by viewModel.pricePerKm.collectAsStateWithLifecycle()
    val selectedVehicleType by viewModel.selectedVehicleType.collectAsStateWithLifecycle()
    val courierAvatar by viewModel.courierAvatar.collectAsStateWithLifecycle()

    var isViewingFullProfilePage by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }

    val unassignedJobs = orders.filter { !it.isSelfPickup && (it.courierName.isNullOrBlank() && it.deliveryId.isNullOrBlank()) && it.status == "READY_FOR_PICKUP" }
    val myActiveDeliveries = orders.filter { !it.isSelfPickup && (!it.courierName.isNullOrBlank() || !it.deliveryId.isNullOrBlank()) && it.status != "DELIVERED" && it.status != "CANCELLED" }

    if (isViewingFullProfilePage) {
        val context = LocalContext.current
        var shiftMinutes by remember { mutableStateOf(272) } // 4h 32m
        var isShiftActive by remember { mutableStateOf(true) }
        
        LaunchedEffect(isShiftActive) {
            while (isShiftActive) {
                kotlinx.coroutines.delay(60000L)
                shiftMinutes += 1
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { isViewingFullProfilePage = false },
                    modifier = Modifier.testTag("profile_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Dashboard"
                    )
                }
                Text(
                    text = "Delivery Partner Center",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Profile Image section (upload image & change avatar)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val avatarBgColor = when (courierAvatar) {
                                "ic_avatar_blue" -> Color(0xFF1D4ED8)
                                "ic_avatar_yellow" -> Color(0xFFD97706)
                                "ic_avatar_orange" -> Color(0xFFEA580C)
                                "ic_avatar_custom" -> Color(0xFF7C3AED)
                                else -> BrandGreenPrimary
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(avatarBgColor.copy(alpha = 0.15f), CircleShape)
                                    .border(3.dp, avatarBgColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (courierAvatar == "ic_avatar_custom") Icons.Default.CloudUpload else Icons.Default.Person,
                                    contentDescription = "Rider Avatar",
                                    tint = avatarBgColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            
                            Text(
                                text = activeUser?.fullName ?: "Guled Farah",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            Text(
                                text = "Active Mode: $selectedVehicleType • Grade: ${activeUser?.deliveryGrade ?: "4.9 Stars (Expert)"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAvatarPickerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                    modifier = Modifier.testTag("edit_avatar_btn")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Select Avatar")
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.updateCourierAvatar("ic_avatar_custom")
                                        android.widget.Toast.makeText(dashboardContext, "Simulated profile image uploaded successfully from camera gallery!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                    modifier = Modifier.testTag("upload_image_btn")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload Photo")
                                }
                            }
                        }
                    }
                }
                
                // 2. Select Type of Vehicle Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Select Active Vehicle Type",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Choose your transport to calculate correct dispatch routes and auto-apply base fares.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            listOf(
                                Triple("Motorbike", "Highly versatile, standard base fare rate.", Icons.Default.TwoWheeler),
                                Triple("Bajaj", "Three-wheeler courier, spacious capacity.", Icons.Default.LocalShipping),
                                Triple("Eco Car / Van", "All-weather secure bulk container.", Icons.Default.DirectionsCar)
                            ).forEach { (type, description, icon) ->
                                val isSelected = selectedVehicleType == type
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updateVehicleType(type) }
                                        .testTag("vehicle_option_${type.replace(" ", "_")}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(type, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.updateVehicleType(type) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 3. Grade Progress by completed orders
                item {
                    val completedRides = 48
                    val targetRides = 60
                    val progressRatio = completedRides.toFloat() / targetRides
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Grade Status & Progress",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(BrandGoldSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Gold Courier",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BrandGoldSecondary
                                    )
                                }
                            }
                            
                            Text(
                                text = "Current rating: 4.91 Stars. You are a highly-rated Expert partner! Complete just ${targetRides - completedRides} more deliveries to reach the Diamond Elite tier.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            LinearProgressIndicator(
                                progress = progressRatio,
                                color = BrandGoldSecondary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$completedRides Completed",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$targetRides rides target",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
                
                // 4. Previous Monthly Analytics
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                Text(
                                    text = "Previous Monthly Analytics",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Button(
                                    onClick = {
                                        generateCourierReportPdf(
                                            context = dashboardContext,
                                            name = activeUser?.fullName ?: "Guled Farah",
                                            vehicleType = selectedVehicleType,
                                            rating = activeUser?.deliveryGrade ?: "4.9 Stars (Expert)",
                                            completedCount = 48,
                                            distance = 192.0,
                                            income = 192.0 * pricePerKm
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("download_courier_pdf_btn")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download PDF", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            
                            val historicalMonths = listOf(
                                Triple("May 2026", "22 rides", "88.0 km"),
                                Triple("April 2026", "18 rides", "72.0 km"),
                                Triple("March 2026", "15 rides", "60.0 km"),
                                Triple("February 2026", "12 rides", "48.0 km"),
                                Triple("January 2026", "10 rides", "40.0 km")
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Month", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1.5f))
                                    Text("Rides", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
                                    Text("Distance", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1.2f))
                                    Text("Income", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                
                                historicalMonths.forEach { (monthName, count, kmText) ->
                                    val kmVal = kmText.replace(" km", "").toDoubleOrNull() ?: 40.0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(monthName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.5f))
                                        Text(count, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        Text(kmText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f))
                                        Text(
                                            text = String.format("%,.0f ETB", kmVal * pricePerKm),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = BrandGreenPrimary,
                                            modifier = Modifier.weight(1.2f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 5. Standard fare list / Reading list per kilometer
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Standard City-Wide Courier Tariffs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Reference list of dispatcher benchmark rates in Addis Ababa based on vehicle class:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            listOf(
                                Triple("🚲 Bicycle / Eco Bike", "8.0 - 10.0 ETB / km", "Best for ultra-short deliveries in local neighborhoods."),
                                Triple("🏍️ Standard Motorbike", "12.0 - 15.0 ETB / km", "Recommended standard; balances speed and heavy city traffic."),
                                Triple("🛺 Bajaj (Rickshaw)", "16.0 - 20.0 ETB / km", "Optimized for bulky items, short neighborhood runs, weatherproof."),
                                Triple("🚗 Eco Compact Car", "22.0 - 30.0 ETB / km", "High-capacity bulk deliveries; full weather protection.")
                            ).forEach { (vName, rates, desc) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.8f)) {
                                        Text(vName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text(
                                        text = rates,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = BrandGoldSecondary,
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
                
                // 6. Future Features (Duty Shift toggle stopwatch, network ping, SOS emergency)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Rider Command Center",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Duty Shift Timer", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    val hours = shiftMinutes / 60
                                    val minutes = shiftMinutes % 60
                                    Text(
                                        text = String.format("%02dh %02dm active today", hours, minutes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isShiftActive) BrandGreenPrimary else MaterialTheme.colorScheme.outline
                                    )
                                }
                                Button(
                                    onClick = { isShiftActive = !isShiftActive },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isShiftActive) BrandRedTertiary else BrandGreenPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isShiftActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isShiftActive) "Pause Shift" else "Resume Shift", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("GPS Broadcasting", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text("Broadcasting live telemetry (Ping: 42ms)", style = MaterialTheme.typography.labelSmall, color = BrandGreenPrimary)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(BrandGreenPrimary, CircleShape)
                                )
                            }
                            
                            Button(
                                onClick = {
                                    android.widget.Toast.makeText(dashboardContext, "🚨 SOS Emergency Signal broadcast to near dispatcher centers!", android.widget.Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRedTertiary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Emergency SOS Signal Dispatch", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
        
        // Avatar picker Dialog
        if (showAvatarPickerDialog) {
            AlertDialog(
                onDismissRequest = { showAvatarPickerDialog = false },
                title = { Text("Select Driver Identity Profile") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text("Choose from colored identity presets below:", style = MaterialTheme.typography.bodySmall)
                        
                        listOf(
                            "ic_avatar_default" to "Emerald Helmet Rider",
                            "ic_avatar_blue" to "Blue Lightning Swift",
                            "ic_avatar_yellow" to "Amber Falcon Express",
                            "ic_avatar_orange" to "Sunset Retro Bajaj"
                        ).forEach { (id, label) ->
                            val isChosen = courierAvatar == id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateCourierAvatar(id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val indicatorColor = when (id) {
                                        "ic_avatar_blue" -> Color(0xFF1D4ED8)
                                        "ic_avatar_yellow" -> Color(0xFFD97706)
                                        "ic_avatar_orange" -> Color(0xFFEA580C)
                                        else -> BrandGreenPrimary
                                    }
                                    Box(modifier = Modifier.size(16.dp).background(indicatorColor, CircleShape))
                                    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showAvatarPickerDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Analytics & Title Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Delivery Partner Center",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Track, manage, and configure your rides",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    // Online/Offline Status toggle switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isOnline) BrandGreenPrimary else MaterialTheme.colorScheme.outline
                        )
                        Switch(
                            checked = isOnline,
                            onCheckedChange = {
                                val success = viewModel.toggleCourierOnline()
                                if (!success) {
                                    android.widget.Toast.makeText(dashboardContext, "Cannot go offline while you have active rides!", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.testTag("courier_online_switch")
                        )
                    }
                }
            }

            // Status Banner Badge
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOnline) BrandGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isOnline) BrandGreenPrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (isOnline) BrandGreenPrimary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (isOnline) "Active & Ready to Accept Rides" else "Duty Status: Resting / Offline",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isOnline) BrandGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (isOnline) "Receiving Jobs" else "No Jobs Broadcast",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Simple Courier Profile & Grade Card (with click capability to navigate to the page of all info)
            item {
                val avatarBgColor = when (courierAvatar) {
                    "ic_avatar_blue" -> Color(0xFF1D4ED8)
                    "ic_avatar_yellow" -> Color(0xFFD97706)
                    "ic_avatar_orange" -> Color(0xFFEA580C)
                    "ic_avatar_custom" -> Color(0xFF7C3AED)
                    else -> BrandGreenPrimary
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isViewingFullProfilePage = true }
                        .testTag("courier_profile_card"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(avatarBgColor.copy(alpha = 0.15f), CircleShape)
                                        .border(2.dp, avatarBgColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (courierAvatar == "ic_avatar_custom") Icons.Default.CloudUpload else Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        tint = avatarBgColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = activeUser?.fullName ?: "Guled Farah",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Verified Courier",
                                            tint = BrandGreenPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Active Vehicle: $selectedVehicleType • ${activeUser?.plateNumber ?: "AA-3-B98234"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Rating Grade badge
                            Box(
                                modifier = Modifier
                                    .background(BrandGoldSecondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = BrandGoldSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Gold Grade",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Click to configure vehicle & view full grade analytics",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        // Active Deliveries (Ride Management)
        item {
            Text(
                text = "My Accepted Rides",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (myActiveDeliveries.isEmpty()) {
            item {
                Text(
                    text = "No active rides. Claim some delivery jobs below!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            items(myActiveDeliveries) { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Deliver to: ${"Buyer Name"}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Address: ${"Buyer Address"}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Items: ${order.productNames}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.startDeliveryRide(order) },
                                modifier = Modifier.fillMaxWidth().testTag("ride_start_${order.id}")
                            ) {
                                Text(loc("start_delivery"))
                            }
                        }
                    }
                }
            }
        }

        // Job Finder
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = loc("job_finder"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // If Offline, display warning, otherwise display available unassigned jobs
        if (!isOnline) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = "Offline indicator",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "You are currently Offline",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Toggle your status to Online at the top of the screen to start viewing, accepting, and receiving local delivery rides.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            if (unassignedJobs.isEmpty()) {
                item {
                    Text(
                        text = "No jobs nearby ready. Refresh later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                items(unassignedJobs) { job ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Store: ${"Seller Name"}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(text = "To Bole Kebele 03 (3.2 km)", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = String.format("Est. Fee: %,.1f0 ETB", 3.2 * pricePerKm), // Estimated fee calculated by ride distance * price per km
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrandGreenPrimary
                                )
                            }
                            Button(
                                onClick = {
                                    if (myActiveDeliveries.isNotEmpty()) {
                                        android.widget.Toast.makeText(dashboardContext, "You can only have 1 active ride! Please complete your current delivery first.", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        val success = viewModel.claimDeliveryJob(job)
                                        if (!success) {
                                            android.widget.Toast.makeText(dashboardContext, "You can only have 1 active ride! Please complete your current delivery first.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("claim_job_${job.id}")
                            ) {
                                Text(loc("accept_job"))
                            }
                        }
                    }
                }
            }
        }
    }
}
}



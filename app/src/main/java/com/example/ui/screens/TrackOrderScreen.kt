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
fun TrackOrderScreen(viewModel: AppViewModel) {
    val order by viewModel.trackingOrder.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            OptInHeader(
                title = loc("order_tracking"),
                onBack = {
                    viewModel.selectOrderDetail(order)
                    viewModel.navigateTo(AppScreen.MAIN)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            order?.let { ord ->
                if (ord.isSelfPickup) {
                    // Self-Pickup Store Header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏬 Self-Pickup Store Route",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = BrandGreenPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BrandGoldSecondary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Code: ${ord.buyerVerifCode ?: "482019"}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Text(
                                text = "Item: ${ord.productNames}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (ord.sellerName.isNotBlank()) "${ord.sellerName} Store" else "Store Location",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // OpenStreetMap Store Route & Direct To Store Action Component
                    OpenStreetMapStoreTracker(
                        storeLat = ord.storeLocation.lat,
                        storeLon = ord.storeLocation.lon,
                        storeName = if (ord.sellerName.isNotBlank()) "${ord.sellerName} Store" else "E-Suuq Store",
                        storeAddress = if (ord.sellerName.isNotBlank()) "${ord.sellerName} Store, Addis Ababa" else "Addis Ababa, Bole Road",
                        userLat = if (ord.shippingAddress.lat != 0.0) ord.shippingAddress.lat else 9.0210,
                        userLon = if (ord.shippingAddress.lon != 0.0) ord.shippingAddress.lon else 38.7650,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    )
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = loc("route_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Item: ${ord.productNames}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Payment: ${ord.paymentMethod} • Status: ${ord.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandGoldSecondary
                            )
                        }
                    }

                    // Real OpenStreetMap Delivery Tracking Canvas
                    val progress = when (ord.status) {
                        "PENDING_MERCHANT" -> 0.1f
                        "PREPARING" -> 0.25f
                        "READY_FOR_PICKUP" -> 0.45f
                        "OUT_FOR_DELIVERY" -> 0.8f
                        "DELIVERED" -> 1.0f
                        else -> 0.5f
                    }

                    OpenStreetMapOrderTracker(
                        sellerLat = ord.storeLocation.lat,
                        sellerLon = ord.storeLocation.lon,
                        buyerLat = if (ord.shippingAddress.lat != 0.0) ord.shippingAddress.lat else 9.0210,
                        buyerLon = if (ord.shippingAddress.lon != 0.0) ord.shippingAddress.lon else 38.7650,
                        driverLat = ord.driverLocation.lat.takeIf { it != 0.0 },
                        driverLon = ord.driverLocation.lon.takeIf { it != 0.0 },
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        orderStatus = ord.status
                    )

                    // Courier driver simulator controls
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Courier Simulator Controller",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.incrementDeliveryProgress(ord) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("driver_move_sim_btn")
                                ) {
                                    Text("Move Courier +15%")
                                }
                            }
                        }
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tracking order active.")
                }
            }
        }
    }
}



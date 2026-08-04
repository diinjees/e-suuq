package com.example.ui.components


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
fun OrderCard(
    order: OrderEntity,
    isProcessing: Boolean,
    viewModel: AppViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clickable { viewModel.selectOrderDetail(order) }
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order E-${order.id}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Beautiful Status Badge
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
                        text = order.status,
                        color = badgeTextColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Row: Icon & Product name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Order Package",
                        tint = BrandGreenPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.productNames,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total: ${order.totalPrice} ETB",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandGoldSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Date & Optional Tracking Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPurchaseDate(0L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (order.isSelfPickup) {
                    Button(
                        onClick = { viewModel.setTrackingOrder(order) },
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("track_store_btn_${order.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = "Store Icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Track Store / Go to Store", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                } else if (order.status in listOf("SHIPPED", "OUT_FOR_DELIVERY", "READY_FOR_PICKUP", "PREPARING")) {
                    Button(
                        onClick = { viewModel.setTrackingOrder(order) },
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("track_btn_${order.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Map Icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Track Order", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}



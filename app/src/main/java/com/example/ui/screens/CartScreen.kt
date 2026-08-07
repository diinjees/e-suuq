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
fun CartScreen(viewModel: AppViewModel) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val discount by viewModel.appliedDiscount.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()

    var promoInput by remember { mutableStateOf("") }
    var promoResult by remember { mutableStateOf<Boolean?>(null) }

    val primaryAddress = remember(activeUser) { activeUser?.address ?: "Addis Ababa, Bole Sub-City, Kebele 03" }
    var paymentMethod by remember { mutableStateOf("") } // Telebirr, CBE, COD
    var isSelfPickup by remember { mutableStateOf(false) }

    // Track selected items in the cart (Default: select all on cart updates)
    var selectedProducts by remember(cart) { mutableStateOf<Set<ProductEntity>>(cart.keys.toSet()) }
    val selectedCartItems: Map<ProductEntity, Int> = remember(cart, selectedProducts) {
        cart.filterKeys { selectedProducts.contains(it) }
    }

    // Receipt uploader simulation states
    var selectedReceiptIndex by remember { mutableStateOf<Int?>(null) }
    var showReceiptPicker by remember { mutableStateOf(false) }
    var isExtractingReceipt by remember { mutableStateOf(false) }
    var extractionLogs by remember { mutableStateOf("") }
    var receiptVerificationError by remember { mutableStateOf<String?>(null) }

    // Order Success page state collected from ViewModel
    val orderSuccessEntity by viewModel.activeOrderSuccess.collectAsStateWithLifecycle()

    val subtotal = selectedCartItems.entries.sumOf { it.key.price * it.value }
    val discountAmt = subtotal * (discount / 100.0)

    // Calculate delivery details dynamically based on selected items
    val allFreeDelivery = selectedCartItems.isNotEmpty() && selectedCartItems.keys.all { it.isFreeDelivery }
    
    // Deterministic yet interactive distance calculation based on primaryAddress!
    val simulatedDistance = remember(primaryAddress) {
        if (primaryAddress.isBlank()) {
            3.5
        } else {
            val normalized = primaryAddress.trim()
            val len = normalized.length
            // Keep it realistic between 1.5 km and 11.2 km
            ((len * 0.38) % 9.5 + 1.5).coerceIn(1.5, 11.2)
        }
    }

    val deliveryFee = if (selectedCartItems.isEmpty()) {
        0.0
    } else if (isSelfPickup) {
        15.0 // Special self-pickup handling/processing fee
    } else if (allFreeDelivery) {
        0.0
    } else {
        // Base rate 50 ETB + 25 ETB per km
        val rawFee = 50.0 + (simulatedDistance * 25.0)
        // Round to 1 decimal place
        Math.round(rawFee * 10.0) / 10.0
    }

    val total = (subtotal - discountAmt + deliveryFee).coerceAtLeast(0.0)

    // Ethiopian Telebirr / CBE Birr mock receipts to simulate uploader verification (with timestamp checking)
    val mockReceipts = listOf(
        Triple("Telebirr Receipt ID: TXN90281442", "Paid: ${Math.round(total)} ETB\nDate: Today (Recent)\nStatus: SUCCESS\nMerchant: E-Suuq Verified", true),
        Triple("CBE Birr Receipt ID: FT261902801", "Paid: ${Math.round(total)} ETB\nDate: Today (Recent)\nAccount: 1000344558292\nStatus: SUCCESS", true),
        Triple("Telebirr Receipt ID: TXN_EXPIRED_291", "Paid: ${Math.round(total)} ETB\nDate: 2 Years Ago (Expired Timestamp)\nRecipient: Forged\nStatus: SUCCESS", false),
        Triple("CBE Birr Receipt ID: FT_FAKE_99018", "Paid: 10 ETB\nDate: Suspicious/Altered Time (Fake)\nAccount: Unknown\nStatus: SUCCESS", false)
    )

    // Render Order Success Screen
    if (orderSuccessEntity != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    
                    // Success animated/visual pulse circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(BrandGreenPrimary.copy(alpha = 0.12f))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(BrandGreenPrimary.copy(alpha = 0.22f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success Icon",
                                tint = BrandGreenPrimary,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Order Successful! 🎉",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = BrandGreenPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Barka! Your payment receipt has been successfully verified. Your order is now being prepared for shipping.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "ORDER DETAIL SUMMARY",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Order Reference", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("#ESQ-${orderSuccessEntity!!.id}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payment Method", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(orderSuccessEntity!!.paymentMethod, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Products Purchased", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = orderSuccessEntity!!.productNames, 
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), 
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 180.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Delivery Distance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (allFreeDelivery) "Free Shipping" else "${String.format(java.util.Locale.US, "%.1f", simulatedDistance)} km", 
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                                    color = if (allFreeDelivery) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Delivery Destination", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "N/A", 
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp)
                                )
                            }

                            HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Amount Paid", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                Text("${orderSuccessEntity!!.totalPrice} ETB", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = BrandGreenPrimary)
                            }
                        }
                    }
                }

                // Action Buttons
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val savedOrder = orderSuccessEntity
                            viewModel.setActiveOrderSuccess(null)
                            viewModel.ordersBackTarget.value = "HOME"
                            viewModel.navigateToProfileSub("MY_ORDERS")
                            viewModel.selectTab(MainTab.PROFILE)
                            if (savedOrder != null) {
                                viewModel.setSelectedOrderDetail(savedOrder)
                                viewModel.navigateToProfileSub("ORDER_DETAIL")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("track_order_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                    ) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Track Order Details",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            viewModel.setActiveOrderSuccess(null)
                            viewModel.selectTab(MainTab.HOME)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("continue_home_btn"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue Shopping Home",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    } else {
        // Main Cart Layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (cart.isNotEmpty()) {
                                Checkbox(
                                    checked = selectedProducts.size == cart.size,
                                    onCheckedChange = { checked ->
                                        selectedProducts = if (checked) cart.keys.toSet() else emptySet()
                                    },
                                    modifier = Modifier.testTag("select_all_cart_cb")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Column {
                                Text(
                                    text = "Shopping Cart",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.2.sp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (cart.isNotEmpty()) {
                                    Text(
                                        text = "${selectedProducts.size} of ${cart.size} items selected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                        if (selectedProducts.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    viewModel.removeProductsFromCart(selectedProducts)
                                    selectedProducts = emptySet()
                                },
                                modifier = Modifier.testTag("delete_selected_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = BrandRedTertiary)
                            }
                        }
                    }
                }

                if (cart.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Your E-Suuq Cart is empty",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Browse high-quality local products & place order.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Button(
                                    onClick = { viewModel.selectTab(MainTab.HOME) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Explore Products")
                                }
                            }
                        }
                    }
                } else {
                    // Cart list of items
                    items(cart.entries.toList()) { entry ->
                        val prod = entry.key
                        val qty = entry.value
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedProducts.contains(prod),
                                    onCheckedChange = { checked ->
                                        selectedProducts = if (checked) {
                                            selectedProducts + prod
                                        } else {
                                            selectedProducts - prod
                                        }
                                    },
                                    modifier = Modifier.testTag("select_item_cb_${prod.id}")
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                // Product Image fallback badge
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BrandGreenPrimary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (prod.category == "Foods") "🍲" else if (prod.category == "Fashion") "👗" else "📦",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(14.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = prod.brand.ifBlank { "E-Suuq Verified Suuq" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${prod.price} ETB",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = BrandGreenPrimary
                                        )
                                        if (prod.isFreeDelivery) {
                                            Box(
                                                modifier = Modifier
                                                    .background(BrandGreenPrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Free Delivery",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = BrandGreenPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quantifier
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.removeFromCart(prod) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("remove_qty_${prod.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Reduce",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = qty.toString(),
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.addToCart(prod) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("add_qty_${prod.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Promo Code Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Have a Promo Code?",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = promoInput,
                                        onValueChange = { promoInput = it },
                                        placeholder = { Text("Enter WELCOME or ETHIO30") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("promo_input_field"),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val ok = viewModel.applyPromoCode(promoInput)
                                            promoResult = ok
                                        },
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("promo_apply_btn"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(loc("apply"))
                                    }
                                }
                                if (promoResult == true) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Code applied! 30% discount added.", color = BrandGreenPrimary, style = MaterialTheme.typography.bodySmall)
                                    }
                                } else if (promoResult == false) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Clear, contentDescription = null, tint = BrandRedTertiary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Invalid promo code. Try ETHIO30.", color = BrandRedTertiary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // Delivery / Self-Pickup details & selector
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Pickup Self or Delivery Toggle Button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = { isSelfPickup = false },
                                        modifier = Modifier.weight(1f).testTag("select_delivery_toggle"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isSelfPickup) BrandGreenPrimary else Color.Transparent,
                                            contentColor = if (!isSelfPickup) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                        elevation = null
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Delivery", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Button(
                                        onClick = { isSelfPickup = true },
                                        modifier = Modifier.weight(1f).testTag("select_self_pickup_toggle"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelfPickup) BrandGreenPrimary else Color.Transparent,
                                            contentColor = if (isSelfPickup) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                        elevation = null
                                    ) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Self-Pickup", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelfPickup) Icons.Default.Storefront else Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = BrandGreenPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isSelfPickup) "E-Suuq Store Location (Self-Pickup)" else "Primary Delivery Address Booking for ${activeUser?.fullName ?: "Amina Farah"}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = BrandGreenPrimary
                                        )
                                        Text(
                                            text = if (isSelfPickup) "Addis Ababa, Bole Road, Sheger Building, Ground Floor" else primaryAddress,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Real-time distance dynamic infographic calculator based on the primary address booking
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isSelfPickup) {
                                            Text(
                                                text = "SELF-PICKUP DETAILS & FEES",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                                color = BrandGreenPrimary
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Your Location", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 8.dp)
                                                        .height(2.dp)
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(BrandGreenPrimary, BrandGoldSecondary)
                                                            )
                                                        )
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Store", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Estimated Distance:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("1.2 km", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Pickup Fee Breakdown:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("Flat 15.0 ETB Handling", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        } else if (allFreeDelivery) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "FREE SHIPPING APPLIED!",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = BrandGreenPrimary
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "DELIVERY DISTANCE CALCULATION",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Merchant", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                }
                                                
                                                // Route Visual Path Line
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 8.dp)
                                                        .height(2.dp)
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(BrandGreenPrimary, BrandGoldSecondary)
                                                            )
                                                        )
                                                )
                                                
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Standard Path",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Calculated Distance:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${String.format(java.util.Locale.US, "%.1f", simulatedDistance)} km",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = BrandGreenPrimary
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Delivery Rate Breakdown:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Base 50 ETB + (25 ETB / km)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Payment details and verified receipt uploader
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Payment Option & Payout Number",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Telebirr Option
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { paymentMethod = "Telebirr" },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (paymentMethod == "Telebirr") BrandGreenPrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(1.dp, if (paymentMethod == "Telebirr") BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = paymentMethod == "Telebirr",
                                                onClick = { paymentMethod = "Telebirr" },
                                                modifier = Modifier.testTag("pay_telebirr")
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Telebirr Merchant Payout", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (paymentMethod == "Telebirr") {
                                                    Text("Merchant Number Code: 883391", style = MaterialTheme.typography.bodySmall, color = BrandGreenPrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // CBE Birr Option
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { paymentMethod = "CBE" },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (paymentMethod == "CBE") BrandGreenPrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(1.dp, if (paymentMethod == "CBE") BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = paymentMethod == "CBE",
                                                onClick = { paymentMethod = "CBE" },
                                                modifier = Modifier.testTag("pay_cbe")
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("CBE Birr Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (paymentMethod == "CBE") {
                                                    Text("CBE Account Number: 100034499231", style = MaterialTheme.typography.bodySmall, color = BrandGreenPrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Verified Reception image uploader section (Required for Telebirr / CBE)
                                if (paymentMethod == "Telebirr" || paymentMethod == "CBE") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "VERIFY PAYMENT RECEPTION SCREENSHOT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                        color = BrandRedTertiary
                                    )
                                    
                                    if (isExtractingReceipt) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BrandGreenPrimary.copy(alpha = 0.08f))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    color = BrandGreenPrimary,
                                                    modifier = Modifier.size(28.dp),
                                                    strokeWidth = 3.dp
                                                )
                                                Text(
                                                    text = extractionLogs,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = BrandGreenPrimary,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (selectedReceiptIndex != null) BrandGreenPrimary.copy(alpha = 0.05f)
                                                    else if (receiptVerificationError != null) BrandRedTertiary.copy(alpha = 0.05f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                                .clickable { showReceiptPicker = true }
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selectedReceiptIndex == null) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (receiptVerificationError != null) Icons.Default.Warning else Icons.Default.CloudUpload,
                                                        contentDescription = "Upload",
                                                        tint = if (receiptVerificationError != null) BrandRedTertiary else MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                    Text(
                                                        text = if (receiptVerificationError != null) receiptVerificationError!! else "Upload Payment Receipt Screen to Verify",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = if (receiptVerificationError != null) BrandRedTertiary else MaterialTheme.colorScheme.outline,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "(Tap to select simulated payment reception)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                                    )
                                                }
                                            } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.ReceiptLong,
                                                        contentDescription = null,
                                                        tint = BrandGreenPrimary,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = mockReceipts[selectedReceiptIndex!!].first,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                        Text(
                                                            text = "Verified Payment Reception Screen ✅",
                                                            color = BrandGreenPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                                
                                                IconButton(onClick = { selectedReceiptIndex = null }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = BrandRedTertiary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }

                    // Price Summary card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Order Invoice",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Basket Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                    Text("$subtotal ETB", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                
                                if (discount > 0) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Discount Applied (${discount}%)", color = BrandGreenPrimary, style = MaterialTheme.typography.bodyMedium)
                                        Text("-$discountAmt ETB", color = BrandGreenPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                                
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (isSelfPickup) "Self-Pickup Fee" else "Shipping Delivery Fee",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (isSelfPickup) "$deliveryFee ETB" else if (allFreeDelivery) "0.0 ETB (FREE)" else "$deliveryFee ETB",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (!isSelfPickup && allFreeDelivery) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Final Amount Due", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                    Text("$total ETB", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = BrandGreenPrimary)
                                }
                            }
                        }
                    }

                    // Place Order Button
                    item {
                        val isUploaderRequired = paymentMethod == "Telebirr" || paymentMethod == "CBE"
                        val canPlaceOrder = selectedCartItems.isNotEmpty() && paymentMethod.isNotEmpty() && (if (isUploaderRequired) selectedReceiptIndex != null else true)
                        
                        val selectedReceipt = if (selectedReceiptIndex != null) mockReceipts[selectedReceiptIndex!!] else null
                        Button(
                            onClick = {
                                viewModel.checkoutSelected(
                                    selectedCartItems = selectedCartItems,
                                    address = com.example.data.ShippingAddress(lat = 9.02, lon = 38.75),
                                    paymentMethod = paymentMethod,
                                    customTotal = total,
                                    isSelfPickup = isSelfPickup,
                                    receiptTitle = selectedReceipt?.first,
                                    receiptDetails = selectedReceipt?.second
                                ) { latestOrder ->
                                    viewModel.setActiveOrderSuccess(latestOrder)
                                }
                            },
                            enabled = canPlaceOrder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("place_order_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreenPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedCartItems.isEmpty()) "Select Items to Order"
                                       else if (paymentMethod.isEmpty()) "Select Payment Method"
                                       else if (isUploaderRequired && selectedReceiptIndex == null) "Upload Receipt to Order" 
                                       else "Pay & Place Order ($total ETB)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Beautiful Custom Dialog receipt picker for simulation
    if (showReceiptPicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showReceiptPicker = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select Simulated Screenshot Receipt",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Select a receipt screenshot to verify your digital payment:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                     mockReceipts.forEachIndexed { idx, receiptTriple ->
                         val title = receiptTriple.first
                         val desc = receiptTriple.second
                         Card(
                             onClick = {
                                 showReceiptPicker = false
                                 scope.launch {
                                     isExtractingReceipt = true
                                     receiptVerificationError = null
                                     extractionLogs = "Scanning payment screenshot and extracting text (OCR)..."
                                     kotlinx.coroutines.delay(1200)
                                     val receiptId = title.substringAfter("Receipt ID: ")
                                     extractionLogs = "Found Receipt ID: $receiptId\nChecking timestamp & validity..."
                                     kotlinx.coroutines.delay(1200)
                                     if (receiptTriple.third) {
                                         extractionLogs = "Receipt timestamp verified success! ✅"
                                         kotlinx.coroutines.delay(800)
                                         isExtractingReceipt = false
                                         selectedReceiptIndex = idx
                                         android.widget.Toast.makeText(context, "verified reception success", android.widget.Toast.LENGTH_LONG).show()
                                     } else {
                                         isExtractingReceipt = false
                                         selectedReceiptIndex = null
                                         receiptVerificationError = "Fake/Expired transaction: Receipt ID $receiptId is invalid."
                                         android.widget.Toast.makeText(context, "Verification failed! Fake or expired screenshot.", android.widget.Toast.LENGTH_LONG).show()
                                     }
                                 }
                             },
                             modifier = Modifier.fillMaxWidth(),
                             border = BorderStroke(
                                 width = 1.5.dp,
                                 color = if (selectedReceiptIndex == idx) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant
                             ),
                             colors = CardDefaults.cardColors(
                                 containerColor = if (selectedReceiptIndex == idx) BrandGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                             )
                         ) {
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(12.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Icon(
                                     imageVector = Icons.Default.ReceiptLong,
                                     contentDescription = null,
                                     tint = if (selectedReceiptIndex == idx) BrandGreenPrimary else MaterialTheme.colorScheme.outline,
                                     modifier = Modifier.size(28.dp)
                                 )
                                 Spacer(modifier = Modifier.width(12.dp))
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text(
                                         text = title, 
                                         fontWeight = FontWeight.Bold, 
                                         style = MaterialTheme.typography.bodyMedium,
                                         color = MaterialTheme.colorScheme.onSurface
                                     )
                                     Text(
                                         text = desc, 
                                         style = MaterialTheme.typography.bodySmall, 
                                         color = MaterialTheme.colorScheme.outline
                                     )
                                 }
                             }
                         }
                     }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showReceiptPicker = false }) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}



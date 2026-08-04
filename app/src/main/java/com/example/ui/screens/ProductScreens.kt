package com.example.ui.screens

import com.example.data.ProductReviewEntity


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
fun ProductDetailPage(
    product: ProductEntity,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val rating = remember(product.id) { String.format(java.util.Locale.US, "%.1f", 4.0 + (product.id.hashCode() % 10) * 0.1) }
    val soldCount = remember(product.id) { (product.id.hashCode() * 37) % 300 + 45 }
    val reviewsCount = remember(product.id) { (product.id.hashCode() * 13) % 120 + 8 }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var quantity by remember(product.id) { mutableStateOf(1) }

    val products by viewModel.products.collectAsStateWithLifecycle()
    val wishlistProductIds by viewModel.wishlistProductIds.collectAsStateWithLifecycle()
    val isWishlisted = wishlistProductIds.contains(product.id)
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val accountName = activeUser?.fullName ?: "Amina Farah"

    // Dynamic store matching
    val store = remember(product) {
        STORES_LIST.firstOrNull { s ->
            product.name.contains(s.name, ignoreCase = true) ||
            product.description.contains(s.name, ignoreCase = true) ||
            product.brand.contains(s.name, ignoreCase = true)
        } ?: when (product.category) {
            "Beauty" -> STORES_LIST.firstOrNull { it.name == "Chaka Coffee" }
            "Fashion" -> STORES_LIST.firstOrNull { it.name == "Bole Boutique" } ?: STORES_LIST.firstOrNull { it.name == "Anbessa Shoe" }
            "Electronics" -> STORES_LIST.firstOrNull { it.name == "Bole Boutique" }
            else -> STORES_LIST.firstOrNull { it.name == "Chaka Coffee" }
        } ?: STORES_LIST.first()
    }

    // Interactive custom reviews list
    var customReviews by remember(product.id) {
        mutableStateOf(
            listOf(
                Triple("Abdi H.", 5, "Excellent quality! Exceeded my expectations. Delivery was incredibly fast in Mogadishu."),
                Triple("Hodan A.", 4, "Very nice and genuine product. Highly recommend this store!"),
                Triple("Mustafe Y.", 5, "100% authentic. The build quality is top notch. Best purchase this year.")
            )
        )
    }

    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewRatingInput by remember { mutableStateOf(5) }
    var reviewTextInput by remember { mutableStateOf("") }
    var reviewerNameInput by remember { mutableStateOf("") }

    var showAllReviewsPage by remember { mutableStateOf(false) }
    var showSimilarProductsPage by remember { mutableStateOf(false) }

    // Similar Products
    val similarProducts = remember(products, product) {
        products.filter { it.category == product.category && it.id != product.id }.take(5)
    }

    // Custom variants state
    val allOptions = remember(product) { getVariantOptions(product) }
    val availableColors = remember(allOptions) { allOptions.map { it.color }.distinct() }
    var selectedColor by remember(product.id) { mutableStateOf<String?>(null) }
    var showErrorVariantSelection by remember(product.id) { mutableStateOf(false) }
    var showErrorSpecSelection by remember(product.id) { mutableStateOf(false) }

    val availableSpecsForColor = remember(allOptions, selectedColor) {
        if (selectedColor == null) emptyList() else allOptions.filter { it.color == selectedColor }
    }
    var selectedSpec by remember(product.id, selectedColor, availableSpecsForColor) {
        mutableStateOf("")
    }

    val selectedVariantPrice = remember(selectedColor, selectedSpec, allOptions) {
        allOptions.firstOrNull { it.color == selectedColor && it.spec == selectedSpec }?.price
    }
    val displayedPrice = selectedVariantPrice ?: product.price

    // Scroll opacity logic
    val scrollOffset = scrollState.value
    val headerAlpha = (scrollOffset / 350f).coerceIn(0f, 1f)

    if (showAllReviewsPage) {
        ProductReviewsPage(
            product = product,
            viewModel = viewModel,
            onBack = { showAllReviewsPage = false }
        )
    } else if (showSimilarProductsPage) {
        SimilarProductsPage(
            originalProduct = product,
            similarProducts = similarProducts,
            viewModel = viewModel,
            onBack = { showSimilarProductsPage = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    tonalElevation = 12.dp,
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Quantity Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { quantity++ },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Add to Cart flowing button with calculated price
                        Button(
                            onClick = {
                                val hasSpecsToSelect = selectedColor != null && availableSpecsForColor.isNotEmpty() && (availableSpecsForColor.size > 1 || (availableSpecsForColor.first().spec != "Default" && availableSpecsForColor.first().spec.isNotEmpty()))
                                if (availableColors.isNotEmpty() && selectedColor == null) {
                                    showErrorVariantSelection = true
                                    android.widget.Toast.makeText(context, "Please select a variant/color option first!", android.widget.Toast.LENGTH_LONG).show()
                                    scope.launch {
                                        scrollState.animateScrollTo(800)
                                    }
                                    return@Button
                                }
                                showErrorVariantSelection = false

                                if (hasSpecsToSelect && selectedSpec.isEmpty()) {
                                    showErrorSpecSelection = true
                                    android.widget.Toast.makeText(context, "Please select a variant specification!", android.widget.Toast.LENGTH_LONG).show()
                                    scope.launch {
                                        scrollState.animateScrollTo(900)
                                    }
                                    return@Button
                                }
                                showErrorSpecSelection = false

                                val finalProductForCart = if (selectedColor != null || selectedSpec.isNotEmpty()) {
                                    val variantSuffix = listOfNotNull(selectedColor, selectedSpec.takeIf { it != "Default" && it.isNotEmpty() }).joinToString(" / ")
                                    product.copy(
                                        price = displayedPrice,
                                        name = "${product.name} ($variantSuffix)"
                                    )
                                } else {
                                    product
                                }
                                repeat(quantity) {
                                    viewModel.addToCart(finalProductForCart)
                                }
                                android.widget.Toast.makeText(context, "Added $quantity x ${finalProductForCart.name} to cart!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                                .height(48.dp)
                                .testTag("add_to_cart_detail_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            shape = RoundedCornerShape(24.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Add to Cart", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add to Cart • ${(displayedPrice * quantity).toInt()} ETB",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Main Scrollable Area
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // 1. Image Carousel (now at the absolute top of main scroll)
                    var activeImageIndex by remember { mutableStateOf(0) }
                    val productImages = remember(product) {
                        val list = mutableListOf<String>()
                        if (product.imageUrlName.isNotBlank()) {
                            product.imageUrlName.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { list.add(it) }
                        }
                        if (product.imageResName.isNotBlank()) {
                            product.imageResName.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { list.add(it) }
                        }
                        if (list.isEmpty()) {
                            list.add("img_banner_1")
                        }
                        list.distinct()
                    }
                    var offsetX by remember { mutableStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(productImages) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (offsetX > 100) {
                                            activeImageIndex = if (activeImageIndex == 0) productImages.size - 1 else activeImageIndex - 1
                                        } else if (offsetX < -100) {
                                            activeImageIndex = (activeImageIndex + 1) % productImages.size
                                        }
                                        offsetX = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount
                                    }
                                )
                            }
                    ) {
                        Image(
                            painter = getBannerPainter(productImages[activeImageIndex]),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                // Page indicator dots
                if (productImages.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        productImages.forEachIndexed { index, _ ->
                            val isSelected = index == activeImageIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BrandGreenPrimary else Color.White.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }

            // 2. Product Details Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Name & Brand Header
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreenPrimary
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        
                        // Glowing badge for high-visibility condition
                        Box(
                            modifier = Modifier
                                .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp), ambientColor = BrandGreenPrimary, spotColor = BrandGreenPrimary)
                                .background(BrandGreenPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, BrandGreenPrimary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Condition: ${product.stateNewOrUsed}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandGreenPrimary
                            )
                        }

                        if (product.brand.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Text(
                                text = "Brand: ${product.brand}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Dynamic Promo Badges
                    val showBadges = product.discountPercent > 0 || product.isFreeDelivery || product.isPromoted
                    if (showBadges) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Discount badge
                            if (product.discountPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFEBEE)) // Light red
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${product.discountPercent}% OFF",
                                        color = Color(0xFFC62828),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Free Delivery badge
                            if (product.isFreeDelivery) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE8F5E9)) // Light green
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Free Delivery",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Best Seller badge
                            if (product.isPromoted) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFF3E0)) // Light orange
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Whatshot,
                                            contentDescription = null,
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Best Seller",
                                            color = Color(0xFFE65100),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // REMOVED CARD FOR PRICE: Clean direct row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (product.discountPercent > 0) {
                            val discountedPrice = displayedPrice * (1.0 - (product.discountPercent / 100.0))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%.0f ETB", discountedPrice),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = BrandGreenPrimary
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.0f ETB", displayedPrice),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            Text(
                                text = "$displayedPrice ${loc("etb")}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = BrandGreenPrimary
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(20.dp))
                            Text(
                                text = rating,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${reviewsCount + customReviews.size - 3} reviews",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // REMOVED CARD FOR STORE INFO: Clean direct block
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Store Info & Merchant Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSelectedStoreName(store.name)
                                viewModel.setSelectedProductForDetail(null)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Circular Store Avatar
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(store.color.copy(alpha = 0.15f))
                                    .border(2.dp, store.color.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = store.emoji, fontSize = 24.sp)
                            }

                            // Store text details
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = store.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Store",
                                        tint = BrandGreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = store.businessGrade,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Visit Store arrow
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Visit Store",
                            tint = store.color
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // INTERACTIVE VARIANTS SECTION
                if (availableColors.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Select Color",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (showErrorVariantSelection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (showErrorVariantSelection) {
                                Text(
                                    text = "Required",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        ) {
                            availableColors.forEach { col ->
                                val isSelected = selectedColor == col
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) BrandGreenPrimary.copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { 
                                            selectedColor = col 
                                            selectedSpec = ""
                                            showErrorVariantSelection = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Dynamic color dot indicator
                                        val dotColor = when (col.lowercase()) {
                                            "midnight black", "black", "matte black" -> Color.Black
                                            "platinum silver", "silver" -> Color.LightGray
                                            "deep blue", "blue", "indigo blue", "midnight indigo" -> Color(0xFF1E3A8A)
                                            "natural titanium" -> Color(0xFF8E8E93)
                                            "crimson red", "red" -> Color(0xFFB91C1C)
                                            else -> BrandGreenPrimary
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Text(
                                            text = col,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Specifications subcategories matching chosen color (only when selectedColor is not null!)
                        if (selectedColor != null && availableSpecsForColor.isNotEmpty() && (availableSpecsForColor.size > 1 || availableSpecsForColor.first().spec != "Default" && availableSpecsForColor.first().spec.isNotEmpty())) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Select Specification",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (showErrorSpecSelection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                if (showErrorSpecSelection) {
                                    Text(
                                        text = "Required",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            ) {
                                availableSpecsForColor.forEach { option ->
                                    val isSelected = selectedSpec == option.spec
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) BrandGreenPrimary.copy(alpha = 0.1f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { 
                                                selectedSpec = option.spec 
                                                showErrorSpecSelection = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = option.spec,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (option.stock > 0) {
                                                Text(
                                                    text = "${option.stock} units available",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) BrandGreenPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Description
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                }

                // Escrow & delivery specs
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Buying Safeguards & Shipping",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Safe Escrow Protection: Funds held securely until arrival verification",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (product.isFreeDelivery) "Free Shipping: 100% complimentary home delivery" else "Next-Day Delivery: Dispatched within 24 hours across Ethiopia",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. RATINGS & REVIEWS SECTION: Play Store style!
                val p5 = remember(product.id) { (0.75f + (product.id.hashCode() % 3) * 0.06f).coerceIn(0f, 1f) }
                val p4 = remember(product.id) { (0.55f + (product.id.hashCode() % 4) * 0.05f).coerceIn(0f, 1f) }
                val p3 = remember(product.id) { (0.35f + (product.id.hashCode() % 5) * 0.05f).coerceIn(0f, 1f) }
                val p2 = remember(product.id) { (0.18f + (product.id.hashCode() % 3) * 0.03f).coerceIn(0f, 1f) }
                val p1 = remember(product.id) { (0.07f + (product.id.hashCode() % 2) * 0.02f).coerceIn(0f, 1f) }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ratings and reviews",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showAllReviewsPage = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "See All Reviews",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Play Store Split layout row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Column: Giant text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp)
                        ) {
                            Text(
                                text = rating,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 58.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { index ->
                                    val starRating = rating.toFloatOrNull() ?: 4.0f
                                    val fillIcon = index < starRating.toInt()
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (fillIcon) BrandGoldSecondary else Color.LightGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${reviewsCount + customReviews.size - 3} reviews",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Right Column: Progress Bars
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val progressList = listOf(p5, p4, p3, p2, p1)
                            val starLabels = listOf("5", "4", "3", "2", "1")
                            
                            starLabels.forEachIndexed { idx, label ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(10.dp)
                                    )
                                    
                                    // Custom rounded Orange/Amber Play Store style progress bar
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progressList[idx])
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFF57C00)) // Google Play Orange
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Individual Custom Reviews list (Play Store style layout!) - ONLY SHOW 1 REVIEW
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val firstReview = customReviews.firstOrNull()
                        if (firstReview != null) {
                            val (reviewer, ratingValue, comment) = firstReview
                            val reviewDate = "12 Jan 2026"
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        ReviewUserAvatar(reviewer)
                                        Text(
                                            text = reviewer,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        repeat(5) { starIndex ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (starIndex < ratingValue) BrandGoldSecondary else Color.LightGray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = reviewDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                
                                Text(
                                    text = comment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Similar Products
                if (similarProducts.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = BrandGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Similar Products You May Like",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("similar_products_lazy_row")
                        ) {
                            items(similarProducts) { prod ->
                                Box(modifier = Modifier.width(160.dp)) {
                                    ProductCardSmall(
                                        product = prod,
                                        onClick = {
                                            viewModel.setSelectedProductForDetail(prod)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(84.dp)) // padding for floating footer
            }
        }

        // 4. OVERLAID COLLAPSING TOP HEADER BAR (Scroll sensitive!)
        // Transitions from transparent to solid colored as scroll progress increases.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.surface.copy(alpha = headerAlpha),
            tonalElevation = if (headerAlpha > 0.8f) 4.dp else 0.dp,
            shadowElevation = if (headerAlpha > 0.8f) 4.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Back button + Name fading in
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val backBtnBg = androidx.compose.ui.graphics.lerp(
                        Color.Black.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        headerAlpha
                    )
                    val backBtnTint = androidx.compose.ui.graphics.lerp(
                        Color.White,
                        MaterialTheme.colorScheme.onSurface,
                        headerAlpha
                    )

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(backBtnBg, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = backBtnTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (headerAlpha > 0.1f) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = headerAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Right side: Wishlist + Share buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rightBtnBg = androidx.compose.ui.graphics.lerp(
                        Color.Black.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        headerAlpha
                    )
                    val wishlistTint = if (isWishlisted) {
                        Color.Red
                    } else {
                        androidx.compose.ui.graphics.lerp(
                            Color.White,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            headerAlpha
                        )
                    }
                    val shareTint = androidx.compose.ui.graphics.lerp(
                        Color.White,
                        BrandGreenPrimary,
                        headerAlpha
                    )

                    // Wishlist Heart Icon
                    IconButton(
                        onClick = {
                            viewModel.toggleWishlist(product.id)
                            val msg = if (!isWishlisted) "${product.name} added to wishlist!" else "${product.name} removed from wishlist"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(rightBtnBg, CircleShape)
                            .testTag("wishlist_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Add to Wishlist",
                            tint = wishlistTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Share Icon
                    IconButton(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "Check out this amazing product on E-Suuq: ${product.name} for only ${product.price} ETB from ${store.name}! Powered by secure local escrow."
                                )
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share product via"))
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(rightBtnBg, CircleShape)
                            .testTag("share_product_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Product",
                            tint = shareTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Write Review Interactive Dialog
        if (showReviewDialog) {
            AlertDialog(
                onDismissRequest = { showReviewDialog = false },
                title = { Text("Write a Product Review", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = reviewerNameInput,
                            onValueChange = { reviewerNameInput = it },
                            label = { Text("Your Name") },
                            placeholder = { Text("e.g. Amina F.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text("Rating", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(5) { starIndex ->
                                val starRating = starIndex + 1
                                IconButton(
                                    onClick = { reviewRatingInput = starRating },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (starRating <= reviewRatingInput) BrandGoldSecondary else Color.LightGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = reviewTextInput,
                            onValueChange = { reviewTextInput = it },
                            label = { Text("Your Review Comment") },
                            placeholder = { Text("How was the product experience?") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (reviewerNameInput.isNotEmpty() && reviewTextInput.isNotEmpty()) {
                                customReviews = customReviews + Triple(reviewerNameInput, reviewRatingInput, reviewTextInput)
                                reviewerNameInput = ""
                                reviewTextInput = ""
                                showReviewDialog = false
                                android.widget.Toast.makeText(context, "Review submitted successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Please fill in all fields.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                    ) {
                        Text("Submit Review")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReviewDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
        } // Closes Box
    } // Closes Scaffold innerPadding lambda
    } // Closes else block of if (showAllReviewsPage)
} // Closes fun ProductDetailPage


@Composable
fun ProductReviewsPage(
    product: ProductEntity,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val reviews by viewModel.productReviews.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val accountName = activeUser?.fullName?.takeIf { it.isNotBlank() } ?: "Verified Buyer"

    LaunchedEffect(product.id) {
        viewModel.fetchProductReviews(product.id)
    }

    var reviewRatingInput by remember { mutableStateOf(5) }
    var reviewTextInput by remember { mutableStateOf("") }
    var showWriteForm by remember { mutableStateOf(false) }
    var selectedRatingFilter by remember { mutableStateOf(0) } // 0 means All, 1-5 mean that rating

    // Edit Review Dialog state
    var editingReview by remember { mutableStateOf<ProductReviewEntity?>(null) }
    var editRatingInput by remember { mutableStateOf(5) }
    var editCommentInput by remember { mutableStateOf("") }

    val displayRating = remember(reviews, product.rating) {
        if (reviews.isNotEmpty()) {
            String.format(java.util.Locale.US, "%.1f", reviews.map { it.rating }.average())
        } else {
            String.format(java.util.Locale.US, "%.1f", if (product.rating > 0) product.rating else 4.8)
        }
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ratings & Reviews",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Giant Play Store Split layout row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Giant text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(100.dp)
                ) {
                    Text(
                        text = displayRating,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 58.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            val starRating = displayRating.toFloatOrNull() ?: 4.8f
                            val fillIcon = index < starRating.toInt()
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (fillIcon) BrandGoldSecondary else Color.LightGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${reviews.size} reviews",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Right Column: Progress Bars
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val starLabels = listOf("5", "4", "3", "2", "1")
                    val total = if (reviews.isEmpty()) 1 else reviews.size
                    val progressList = (5 downTo 1).map { star ->
                        val count = reviews.count { it.rating.toInt() == star }
                        (count.toFloat() / total).coerceIn(0.1f, 1f)
                    }
                    
                    starLabels.forEachIndexed { idx, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(10.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressList[idx])
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF57C00)) // Google Play Orange
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Inline Write Review Card
            if (!showWriteForm) {
                OutlinedButton(
                    onClick = { showWriteForm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.RateReview, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Write a Review")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Add Your Rating and Review",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Posting publicly as: $accountName",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = BrandGreenPrimary
                        )
                        
                        Text("Rating", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(5) { starIndex ->
                                val starRating = starIndex + 1
                                IconButton(
                                    onClick = { reviewRatingInput = starRating },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (starRating <= reviewRatingInput) BrandGoldSecondary else Color.LightGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = reviewTextInput,
                            onValueChange = { reviewTextInput = it },
                            label = { Text("Your Review Comment") },
                            placeholder = { Text("Write your honest feedback...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showWriteForm = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (reviewTextInput.isNotBlank()) {
                                        viewModel.submitProductReview(product.id, reviewRatingInput.toDouble(), reviewTextInput) { success ->
                                            if (success) {
                                                android.widget.Toast.makeText(context, "Review posted to PocketBase!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Review submitted locally!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        reviewTextInput = ""
                                        reviewRatingInput = 5
                                        showWriteForm = false
                                    } else {
                                        android.widget.Toast.makeText(context, "Please write a review comment.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                            ) {
                                Text("Submit")
                            }
                        }
                    }
                }
            }

            // Filter Pills
            val filteredReviews = remember(reviews, selectedRatingFilter) {
                if (selectedRatingFilter == 0) {
                    reviews
                } else {
                    reviews.filter { it.rating.toInt() == selectedRatingFilter }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 5, 4, 3, 2, 1).forEach { ratingVal ->
                            val isSelected = selectedRatingFilter == ratingVal
                            val label = if (ratingVal == 0) "All" else "$ratingVal ★"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, if (isSelected) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                    .clickable { selectedRatingFilter = ratingVal }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "User Reviews (${filteredReviews.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (filteredReviews.isEmpty()) {
                    Text(
                        text = "No reviews match this filter. Be the first to leave a review!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    filteredReviews.forEach { item ->
                        val isMyReview = activeUser != null && (item.userId == activeUser?.id || item.userId.isBlank())
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ReviewUserAvatar(item.userName.ifBlank { "Verified Buyer" })
                                    Column {
                                        Text(
                                            text = item.userName.ifBlank { "Verified Buyer" },
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isMyReview) {
                                            Text(
                                                text = "Your Review",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BrandGreenPrimary
                                            )
                                        }
                                    }
                                }

                                if (isMyReview) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingReview = item
                                                editRatingInput = item.rating.toInt()
                                                editCommentInput = item.comment
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit review", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteProductReview(item.id, product.id) {
                                                    android.widget.Toast.makeText(context, "Review deleted!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete review", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(5) { starIndex ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (starIndex < item.rating.toInt()) BrandGoldSecondary else Color.LightGray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = item.created?.take(10) ?: "Recent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            
                            Text(
                                text = item.comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }

    // Edit Review Dialog
    val currentReview = editingReview
    if (currentReview != null) {
        AlertDialog(
            onDismissRequest = { editingReview = null },
            title = { Text("Edit Review", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Rating", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { starIndex ->
                            val starRating = starIndex + 1
                            IconButton(
                                onClick = { editRatingInput = starRating },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (starRating <= editRatingInput) BrandGoldSecondary else Color.LightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editCommentInput,
                        onValueChange = { editCommentInput = it },
                        label = { Text("Review Comment") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProductReview(currentReview.id, product.id, editRatingInput.toDouble(), editCommentInput) {
                            android.widget.Toast.makeText(context, "Review updated!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        editingReview = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingReview = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}


@Composable
fun SimilarProductsPage(
    originalProduct: ProductEntity,
    similarProducts: List<ProductEntity>,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Similar Products",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Based on: ${originalProduct.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (similarProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No similar products found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(similarProducts) { prod ->
                        ProductCardSmall(
                            product = prod,
                            onClick = {
                                viewModel.setSelectedProductForDetail(prod)
                                onBack() // Close Similar Products Page to show details of the selected product
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun StoreDetailPage(
    storeName: String,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val store = remember(storeName) {
        STORES_LIST.firstOrNull { it.name == storeName } ?: StoreData(
            name = storeName,
            emoji = "🏪",
            color = BrandGreenPrimary,
            businessGrade = "4.8 ★",
            description = "Welcome to our store! We offer high-quality authentic Ethiopian products directly to your doorstep."
        )
    }
    
    val storeProducts = remember(products, storeName) {
        products.filter { 
            it.name.contains(storeName, ignoreCase = true) || 
            it.description.contains(storeName, ignoreCase = true) ||
            it.brand.contains(storeName, ignoreCase = true) ||
            false
        }.ifEmpty {
            if (storeName == "Chaka Coffee") {
                products.filter { it.category == "Beauty" || it.name.contains("Coffee", ignoreCase = true) }
            } else if (storeName == "Anbessa Shoe" || storeName == "Gulele Leather") {
                products.filter { it.category == "Fashion" || it.name.contains("Leather", ignoreCase = true) }
            } else if (storeName == "Bole Boutique") {
                products.filter { it.category == "Fashion" }
            } else {
                products.take(4)
            }
        }
    }

    var selectedTab by remember { mutableStateOf("Products") } // "Products", "Reviews", "Info"
    var selectedRatingFilter by remember { mutableStateOf<Int?>(null) } // null means "All"

    val storeReviewsFromDb by viewModel.storeReviews.collectAsStateWithLifecycle()
    
    androidx.compose.runtime.LaunchedEffect(storeName) {
        viewModel.fetchStoreReviews()
    }

    val storeReviews = remember(storeReviewsFromDb) {
        if (storeReviewsFromDb.isEmpty()) {
            listOf(
                Triple("Guled Farah", 5, "Adeeg aad u fiican iyo alaab tayo sare leh! Aad baan ugu qancay."),
                Triple("Hodan Aden", 4, "Aad baa u mahadsantihiin, dhalmada waa ay degdegsantahay."),
                Triple("Mustafe Yusuf", 5, "Kani waa dukaanka ugu fiican. Wax kasta waa kuwo tayo leh!")
            )
        } else {
            storeReviewsFromDb.map { item ->
                val ratingValue = item.rating.replace("★", "").trim().toDoubleOrNull()?.toInt() ?: 5
                Triple(item.name, ratingValue, item.comment)
            }
        }
    }
    
    var showStoreReviewDialog by remember { mutableStateOf(false) }
    var storeReviewRatingInput by remember { mutableStateOf(5) }
    var storeReviewTextInput by remember { mutableStateOf("") }
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()

    val isOpen = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        hour in 8..21
    }
    val statusText = if (isOpen) "Online" else "Offline"
    val statusColor = if (isOpen) BrandGreenPrimary else Color.Red

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Store Hero Banner with elegant visual height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Image(
                    painter = getBannerPainter(store.bannerImageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Linear gradient overlay for high contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
                
                // Overlay text inside banner (overlapping)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile avatar badge (overlapping look)
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(3.dp, store.color, CircleShape)
                            .shadow(2.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(store.emoji, fontSize = 34.sp)
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = store.name,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(Icons.Default.Verified, null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(14.dp))
                            Text(
                                text = store.businessGrade,
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Beautiful Statistics / Fast Info Row (Glassmorphic vibe)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Products", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${storeProducts.size} Active", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = statusColor
                            )
                        }
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Store Hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("08:00 AM - 09:00 PM", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Beautiful Material 3 Tabs
            StoreTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content Rendering
            if (selectedTab == "Products") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (storeProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No products found for this store.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        // Display products in clean responsive 2-column grid
                        val chunked = storeProducts.chunked(2)
                        chunked.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { prod ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProductCardSmall(prod, onClick = { viewModel.setSelectedProductForDetail(prod) })
                                    }
                                }
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            } else if (selectedTab == "Reviews") {
                // Store Reviews Tab Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Rating Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Store Rating",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "4.8",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Column {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(5) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = BrandGoldSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Based on ${storeReviews.size} reviews",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { showStoreReviewDialog = !showStoreReviewDialog },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                            ) {
                                Text(if (showStoreReviewDialog) "Close Form" else "Write Review")
                            }
                        }
                    }

                    // Expandable Write Review Form
                    if (showStoreReviewDialog) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Write a Store Review",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Your Rating:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row {
                                        repeat(5) { index ->
                                            val starRating = index + 1
                                            IconButton(
                                                onClick = { storeReviewRatingInput = starRating },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (starRating <= storeReviewRatingInput) BrandGoldSecondary else Color.LightGray,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = storeReviewTextInput,
                                    onValueChange = { storeReviewTextInput = it },
                                    label = { Text("Your Review Comment") },
                                    placeholder = { Text("How was your shopping experience with this merchant?") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showStoreReviewDialog = false }) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (storeReviewTextInput.isNotBlank()) {
                                                viewModel.submitStoreReview(
                                                    productId = "Store: $storeName",
                                                    rating = storeReviewRatingInput.toDouble(),
                                                    comment = storeReviewTextInput
                                                ) { success ->
                                                    if (success) {
                                                        android.widget.Toast.makeText(context, "Thank you for your feedback!", android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Feedback saved locally.", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                storeReviewTextInput = ""
                                                storeReviewRatingInput = 5
                                                showStoreReviewDialog = false
                                            } else {
                                                android.widget.Toast.makeText(context, "Please write a comment.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                    ) {
                                        Text("Submit")
                                    }
                                }
                            }
                        }
                    }

                    // Rating Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filter Rate:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedRatingFilter == null,
                                    onClick = { selectedRatingFilter = null },
                                    label = { Text("All") }
                                )
                            }
                            (5 downTo 1).forEach { stars ->
                                item {
                                    FilterChip(
                                        selected = selectedRatingFilter == stars,
                                        onClick = { selectedRatingFilter = stars },
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text("$stars")
                                                Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = BrandGoldSecondary)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Reviews List
                    val filteredReviews = remember(storeReviews, selectedRatingFilter) {
                        if (selectedRatingFilter == null) {
                            storeReviews
                        } else {
                            storeReviews.filter { it.second == selectedRatingFilter }
                        }
                    }

                    if (filteredReviews.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reviews found for this rating filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        filteredReviews.forEach { (reviewer, ratingValue, comment) ->
                            Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ReviewUserAvatar(reviewer)
                                        Column {
                                            Text(
                                                text = reviewer,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                repeat(5) { starIdx ->
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (starIdx < ratingValue) BrandGoldSecondary else Color.LightGray,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Verified Buyer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandGreenPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = comment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                    }
                }
            } else {
                // Info Tab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // About Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "About Our Brand",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = store.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 21.sp
                            )
                        }
                    }

                    // Credentials and Badges section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Compliance & Verification",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                Text("Registered Trade Merchant", style = MaterialTheme.typography.bodyMedium)
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                Text("Premium Quality Sourced In Ethiopia", style = MaterialTheme.typography.bodyMedium)
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.LocalShipping, null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                Text("Fast Express Home Delivery Support", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Business Days & Working Hours Card
                    val storeWeeklyMap by viewModel.storeWeeklyWorkedMap.collectAsStateWithLifecycle()
                    val storeTimeMap by viewModel.storeTimeWorkedMap.collectAsStateWithLifecycle()
                    val weeklyWorked = storeWeeklyMap[store.name] ?: when (store.name) {
                        "Chaka Coffee" -> "Monday - Friday"
                        "Anbessa Shoe" -> "Monday - Saturday"
                        "Bole Boutique", "Bole Traditional Boutique" -> "Monday - Saturday"
                        "Selam Teff" -> "Monday - Sunday"
                        "Gulele Leather" -> "Monday - Saturday"
                        "Highland Tea" -> "Monday - Friday"
                        else -> "Monday - Saturday"
                    }
                    val timeWorked = storeTimeMap[store.name] ?: when (store.name) {
                        "Chaka Coffee" -> "07:00 AM - 08:00 PM"
                        "Anbessa Shoe" -> "08:30 AM - 07:30 PM"
                        "Bole Boutique", "Bole Traditional Boutique" -> "09:00 AM - 08:30 PM"
                        "Selam Teff" -> "06:00 AM - 09:00 PM"
                        "Gulele Leather" -> "08:00 AM - 07:00 PM"
                        "Highland Tea" -> "08:00 AM - 06:00 PM"
                        else -> "08:00 AM - 09:00 PM"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("store_hours_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Business Days & Working Hours",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.DateRange, null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Days of Operation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(weeklyWorked, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Schedule, null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Working Hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(timeWorked, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Overlaid Collapsing Top Header Bar (Scroll sensitive!)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val scrollDp = with(density) { scrollState.value.toDp() }
        val areTabsSticky = scrollDp > 310.dp
        val headerAlpha = (scrollState.value / 200f).coerceIn(0f, 1f)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (areTabsSticky) 1f else headerAlpha),
            tonalElevation = if (areTabsSticky || headerAlpha > 0.8f) 4.dp else 0.dp,
            shadowElevation = if (areTabsSticky || headerAlpha > 0.8f) 4.dp else 0.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val buttonBg = androidx.compose.ui.graphics.lerp(
                        Color.Black.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        if (areTabsSticky) 1f else headerAlpha
                    )
                    val buttonTint = androidx.compose.ui.graphics.lerp(
                        Color.White,
                        MaterialTheme.colorScheme.onSurface,
                        if (areTabsSticky) 1f else headerAlpha
                    )

                    // Left: Back button + fading title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .background(buttonBg, CircleShape)
                                .testTag("store_detail_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = buttonTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (areTabsSticky || headerAlpha > 0.1f) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = store.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Official Verified Merchant",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandGreenPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Right: Chat icon on appbar
                    IconButton(
                        onClick = {
                            val roomId = when (store.name) {
                                "Chaka Coffee" -> "chaka_coffee_chat"
                                "Anbessa Shoe" -> "anbessa_shoe_chat"
                                "Selam Teff" -> "selam_teff_chat"
                                "Gulele Leather" -> "gulele_leather_chat"
                                "Highland Tea" -> "highland_tea_chat"
                                "Bole Boutique", "Bole Traditional Boutique" -> "buyer_seller"
                                else -> "buyer_seller"
                            }
                            viewModel.selectChatRoomId(roomId)
                            viewModel.selectTab(MainTab.CHAT)
                            onBack()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(buttonBg, CircleShape)
                            .testTag("store_chat_appbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat with Merchant",
                            tint = buttonTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Sticky Tab Bar
                if (areTabsSticky) {
                    StoreTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}



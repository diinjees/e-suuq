package com.example.ui.utils


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

val STORES_LIST = listOf(
    StoreData("Chaka Coffee", "☕", BrandGreenPrimary, "4.9 ★ Excellent", "Direct trade organic arabica coffee from Sidamo and Harar highlands.", "img_banner_1"),
    StoreData("Anbessa Shoe", "🦁", BrandGoldSecondary, "4.8 ★ Trusted", "Iconic Ethiopian genuine leather footwear crafted since 1935.", "img_banner_2"),
    StoreData("Bole Boutique", "👗", BrandRedTertiary, "4.7 ★ Highly Rated", "Handmade traditional Habesha Kemis clothing & festive attire.", "img_banner_1"),
    StoreData("Selam Teff", "🌾", Color(0xFF009688), "4.9 ★ Premium", "Fine organic teff grain and freshly baked injera delivered daily.", "img_banner_2"),
    StoreData("Gulele Leather", "💼", Color(0xFF3F51B5), "4.6 ★ Verified", "Premium artisanal leather goods, jackets, and corporate bags.", "img_banner_1"),
    StoreData("Highland Tea", "🍃", Color(0xFF9C27B0), "4.5 ★ Local Choice", "Organic black and herbal teas harvested from organic estates.", "img_banner_2")
)


data class StoreData(
    val name: String,
    val emoji: String,
    val color: Color,
    val businessGrade: String,
    val description: String,
    val bannerImageRes: String = "img_banner_1"
)


data class ReviewItem(
    val name: String,
    val rating: String,
    val comment: String,
    val date: String,
    val product: String,
    val replyText: String? = null,
    val id: String? = null,
    val userId: String? = null,
    val userAvatar: String? = null,
    val productId: String? = null
)


data class ColorVariantOption(
    val color: String,
    val spec: String,
    val stock: Int = 10,
    val price: Double? = null
)


fun getVariantOptions(product: ProductEntity): List<ColorVariantOption> {
    val structured = parseStructuredVariants(product.variants)
    if (structured.isNotEmpty()) {
        return structured.map { ColorVariantOption(it.color, "${it.storage}/${it.ram}", it.stock, it.price) }
    }
    
    val simpleList = product.variants.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    
    if (product.category == "Electronics" || product.name.contains("iPhone") || product.name.contains("Galaxy") || product.name.contains("S23") || product.name.contains("Laptop") || product.name.contains("Phone")) {
        val colors = listOf("Midnight Black", "Platinum Silver", "Deep Blue")
        val specs = listOf("128GB / 4GB RAM", "256GB / 8GB RAM", "512GB / 12GB RAM")
        val list = mutableListOf<ColorVariantOption>()
        colors.forEach { col ->
            specs.forEachIndexed { idx, spec ->
                list.add(ColorVariantOption(col, spec, 5 + idx * 3))
            }
        }
        return list
    } else if (product.category == "Fashion" || product.name.contains("Shoe") || product.name.contains("Hoodie") || product.name.contains("Jacket") || product.name.contains("Shirt")) {
        val colors = if (simpleList.any { it.contains("Black") || it.contains("Indigo") || it.contains("Blue") || it.contains("Gray") }) {
            simpleList.filter { !it.contains("M") && !it.contains("L") && !it.contains("XL") }
        } else {
            listOf("Midnight Indigo", "Crimson Red", "Classic Black")
        }
        val sizes = listOf("Size M", "Size L", "Size XL")
        val list = mutableListOf<ColorVariantOption>()
        colors.forEach { col ->
            sizes.forEachIndexed { idx, sz ->
                list.add(ColorVariantOption(col, sz, 12 - idx))
            }
        }
        return list
    } else {
        if (simpleList.size >= 2) {
            val colors = listOf("Standard Color", "Premium Shade")
            val list = mutableListOf<ColorVariantOption>()
            colors.forEach { col ->
                simpleList.forEach { spec ->
                    list.add(ColorVariantOption(col, spec, 15))
                }
            }
            return list
        } else {
            val item = simpleList.firstOrNull() ?: "Standard Edition"
            return listOf(ColorVariantOption("Default", item, 10))
        }
    }
}


data class StructuredVariant(
    val color: String,
    val storage: String,
    val ram: String,
    val stock: Int,
    val price: Double? = null
)


fun parseStructuredVariants(variantsStr: String): List<StructuredVariant> {
    if (variantsStr.isBlank()) return emptyList()
    val list = mutableListOf<StructuredVariant>()
    val items = variantsStr.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    for (item in items) {
        val clean = item.replace("(", "").replace(")", "").trim()
        val parts = clean.split(",").map { it.trim() }
        if (parts.size >= 4) {
            val color = parts[0]
            val storage = parts[1]
            val ram = parts[2]
            val stockStr = parts[3].replace("mobile", "").replace("units", "").replace("pieces", "").replace("piece", "").trim()
            val stockVal = stockStr.toIntOrNull() ?: 0
            val priceVal = if (parts.size >= 5) parts[4].toDoubleOrNull() else null
            list.add(StructuredVariant(color, storage, ram, stockVal, priceVal))
        }
    }
    return list
}


fun serializeStructuredVariants(variantsList: List<StructuredVariant>): String {
    return variantsList.joinToString("; ") {
        if (it.price != null) {
            "(${it.color}, ${it.storage}, ${it.ram}, ${it.stock} mobile, ${it.price})"
        } else {
            "(${it.color}, ${it.storage}, ${it.ram}, ${it.stock} mobile)"
        }
    }
}


data class SoldCategory(val name: String, val soldCount: Int, val items: List<Pair<String, Int>>)

fun getMonthlyCategorySales(month: String): List<SoldCategory> {
    return when (month) {
        "January 2026" -> listOf(
            SoldCategory("Electronics", 70, listOf("Power Bank" to 40, "USB-C Cable" to 30)),
            SoldCategory("Clothing", 50, listOf("Cotton T-Shirt" to 30, "Denim Jeans" to 20)),
            SoldCategory("Home & Kitchen", 40, listOf("Water Bottle" to 25, "Lunch Box" to 15))
        )
        "February 2026" -> listOf(
            SoldCategory("Electronics", 90, listOf("Wireless Mouse" to 50, "Keyboard" to 40)),
            SoldCategory("Clothing", 60, listOf("Hoodie" to 35, "Socks Pack" to 25)),
            SoldCategory("Cosmetics", 50, listOf("Face Wash" to 30, "Lip Balm" to 20))
        )
        "March 2026" -> listOf(
            SoldCategory("Electronics", 100, listOf("Earbuds" to 60, "Phone Case" to 40)),
            SoldCategory("Clothing", 70, listOf("Jacket" to 40, "Sneakers" to 30)),
            SoldCategory("Accessories", 50, listOf("Sunglasses" to 30, "Leather Wallet" to 20))
        )
        "April 2026" -> listOf(
            SoldCategory("Electronics", 80, listOf("Smart Watch" to 50, "Charger Adapter" to 30)),
            SoldCategory("Clothing", 60, listOf("Polo Shirt" to 35, "Shorts" to 25)),
            SoldCategory("Stationery", 40, listOf("Notebook" to 25, "Gel Pen Pack" to 15))
        )
        "May 2026" -> listOf(
            SoldCategory("Electronics", 110, listOf("Bluetooth Speaker" to 60, "MicroSD Card" to 50)),
            SoldCategory("Clothing", 80, listOf("Summer Dress" to 45, "Sandals" to 35)),
            SoldCategory("Outdoors", 50, listOf("Backpack" to 30, "Travel Pillow" to 20))
        )
        "June 2026" -> listOf(
            SoldCategory("Electronics", 120, listOf("Wireless Earphones" to 70, "Ring Light" to 50)),
            SoldCategory("Clothing", 90, listOf("T-Shirt Set" to 50, "Canvas Shoes" to 40)),
            SoldCategory("Home", 50, listOf("LED Desk Lamp" to 30, "Organizer Box" to 20))
        )
        else -> listOf(
            SoldCategory("Electronics", 100, listOf("Earbuds" to 60, "Phone Case" to 40)),
            SoldCategory("Clothing", 80, listOf("Jacket" to 45, "Sneakers" to 35)),
            SoldCategory("Accessories", 40, listOf("Sunglasses" to 25, "Leather Wallet" to 15))
        )
    }
}


data class MonthlyStats(
    val grossSales: Double,
    val soldCount: Int,
    val unsoldRemaining: Int,
    val cancelledCount: Int,
    val refundLoss: Double,
    val productCost: Double,
    val logisticsCost: Double,
    val netProfit: Double,
    val telebirrPayout: Double,
    val cbePayout: Double,
    val percentageOfTarget: String
)

fun getRealCalendarMonthsList(): List<String> {
    val calendar = java.util.Calendar.getInstance()
    val months = mutableListOf<String>()
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) // 0 to 11
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    var currentYear = year
    var currentMonthIdx = month
    for (i in 0 until 6) {
        months.add(0, "${monthNames[currentMonthIdx]} $currentYear")
        currentMonthIdx--
        if (currentMonthIdx < 0) {
            currentMonthIdx = 11
            currentYear--
        }
    }
    return months
}


data class HelpGuide(
    val title: String,
    val content: String,
    val tags: List<String>
)



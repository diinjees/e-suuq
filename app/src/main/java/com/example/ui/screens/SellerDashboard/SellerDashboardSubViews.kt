package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.AppViewModel
import com.example.ui.theme.*

@Composable
fun SellerDashboardRevenueDriversDetail(
    sellerId: String,
    sellerRevenueList: List<SellerRevenue>,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var isTopProductsExpanded by remember { mutableStateOf(false) }
    var selectedRevenueDriverTab by remember { mutableStateOf("Monthly") } // "Monthly" or "Weekly"
    var selectedWeeklyPeriodIndex by remember { mutableStateOf(1) } // Default to index 1 (Previous Week if available)

    // ISO Date Calculations
    val today = remember { java.time.LocalDate.now() }
    val isoWeekFields = remember { java.time.temporal.WeekFields.ISO }

    val currentMonthIso = remember(today) { today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")) }
    val previousMonthIso = remember(today) { today.minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")) }

    fun formatWeekIso(date: java.time.LocalDate): String {
        val y = date.get(isoWeekFields.weekBasedYear())
        val w = date.get(isoWeekFields.weekOfWeekBasedYear())
        return String.format(java.util.Locale.US, "%04d-%02d", y, w)
    }

    val currentWeekIso = remember(today) { formatWeekIso(today) }
    val previousWeekIso = remember(today) { formatWeekIso(today.minusWeeks(1)) }
    val weekMinus2Iso = remember(today) { formatWeekIso(today.minusWeeks(2)) }
    val weekMinus3Iso = remember(today) { formatWeekIso(today.minusWeeks(3)) }

    val weeklyPeriods = remember(today) {
        listOf(
            "$currentWeekIso (Current Week)" to currentWeekIso,
            "$previousWeekIso (Previous Week)" to previousWeekIso,
            "$weekMinus2Iso (Week ${today.minusWeeks(2).get(isoWeekFields.weekOfWeekBasedYear())})" to weekMinus2Iso,
            "$weekMinus3Iso (Week ${today.minusWeeks(3).get(isoWeekFields.weekOfWeekBasedYear())})" to weekMinus3Iso
        )
    }

    // Availability checks
    val hasPreviousMonthData = sellerRevenueList.any { it.month == previousMonthIso }
    val hasCurrentMonthData = sellerRevenueList.any { it.month == currentMonthIso }
    val activeMonthIso = if (hasPreviousMonthData) previousMonthIso else if (hasCurrentMonthData) currentMonthIso else previousMonthIso

    val hasPreviousWeekData = sellerRevenueList.any { it.week == previousWeekIso }
    val selectedWeekIso = weeklyPeriods.getOrNull(selectedWeeklyPeriodIndex.coerceIn(0, weeklyPeriods.lastIndex))?.second ?: currentWeekIso

    // Adjust selected weekly index on load if previous week is available vs current week
    LaunchedEffect(hasPreviousWeekData) {
        if (!hasPreviousWeekData && selectedWeeklyPeriodIndex == 1) {
            selectedWeeklyPeriodIndex = 0 // Fallback to current week if previous week not available
        }
    }

    val activeWeekIso = if (sellerRevenueList.any { it.week == selectedWeekIso }) {
        selectedWeekIso
    } else if (hasPreviousWeekData) {
        previousWeekIso
    } else {
        selectedWeekIso
    }

    val weekRevenueItem = sellerRevenueList.find { it.week == selectedWeekIso }
    val hasSelectedWeekData = weekRevenueItem != null || sellerRevenueList.any { it.week == selectedWeekIso }

    val currentRevenue = if (selectedRevenueDriverTab == "Monthly") {
        sellerRevenueList.find { it.month == activeMonthIso } ?: sellerRevenueList.firstOrNull()
    } else {
        weekRevenueItem
    }

    // Header row with Back Button
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .testTag("revenue_drivers_back_btn")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Financial Analytics")
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Revenue Drivers Analysis",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Product categories and top performing items generating the most value",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Pinned Tab Selectors to switch between Monthly and Weekly
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { selectedRevenueDriverTab = "Monthly" },
            modifier = Modifier.weight(1f).testTag("revenue_driver_tab_monthly"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedRevenueDriverTab == "Monthly") BrandGreenPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = if (selectedRevenueDriverTab == "Monthly") Color.White else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "📅 Monthly ($activeMonthIso)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        Button(
            onClick = { selectedRevenueDriverTab = "Weekly" },
            modifier = Modifier.weight(1f).testTag("revenue_driver_tab_weekly"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedRevenueDriverTab == "Weekly") BrandGreenPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = if (selectedRevenueDriverTab == "Weekly") Color.White else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "🗓️ Weekly ($activeWeekIso)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }

    if (selectedRevenueDriverTab == "Weekly") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                if (selectedWeeklyPeriodIndex < weeklyPeriods.lastIndex) {
                    selectedWeeklyPeriodIndex++
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Week")
            }
            Text(
                text = weeklyPeriods.getOrNull(selectedWeeklyPeriodIndex)?.first ?: activeWeekIso,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = {
                if (selectedWeeklyPeriodIndex > 0) {
                    selectedWeeklyPeriodIndex--
                }
            }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Week")
            }
        }
    }
    
    // Detailed breakdown screen (Scrollable)
    if (selectedRevenueDriverTab == "Weekly" && !hasSelectedWeekData) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No data on this week",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "There are no recorded revenue drivers or transactions for week $selectedWeekIso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Card 1: Revenue Share Category breakdown (Top 3)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (selectedRevenueDriverTab == "Monthly") "Revenue Share by Product Category (Month: $activeMonthIso)" else "Revenue Share by Product Category (Week: $activeWeekIso)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val categoriesBreakdown = if (currentRevenue != null && currentRevenue.top10Categories.isNotEmpty()) {
                    val top3Cat = currentRevenue.top10Categories.take(3)
                    val maxRev = top3Cat.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
                    top3Cat.map { (catName, rev, units) ->
                        val shareRatio = (rev / maxRev).toFloat().coerceIn(0.1f, 1.0f)
                        Triple(catName, shareRatio, String.format("%,.2f ETB (%d units)", rev, units))
                    }
                } else {
                    val defaultList = if (selectedRevenueDriverTab == "Monthly") {
                        listOf(
                            Triple("Agricultural Goods (Teff, Grains)", 0.62f, "32,240.00 ETB (62%)"),
                            Triple("Spices, Herbs & Organic Foods", 0.24f, "12,480.00 ETB (24%)"),
                            Triple("Traditional Textiles & Apparel", 0.14f, "7,280.00 ETB (14%)")
                        )
                    } else {
                        when (selectedWeeklyPeriodIndex) {
                            0 -> listOf(
                                Triple("Agricultural Goods (Teff, Grains)", 0.58f, "7,830.00 ETB (58%)"),
                                Triple("Spices, Herbs & Organic Foods", 0.25f, "3,375.00 ETB (25%)"),
                                Triple("Traditional Textiles & Apparel", 0.17f, "2,295.00 ETB (17%)")
                            )
                            1 -> listOf(
                                Triple("Agricultural Goods (Teff, Grains)", 0.64f, "8,960.00 ETB (64%)"),
                                Triple("Spices, Herbs & Organic Foods", 0.22f, "3,080.00 ETB (22%)"),
                                Triple("Traditional Textiles & Apparel", 0.14f, "1,960.00 ETB (14%)")
                            )
                            2 -> listOf(
                                Triple("Agricultural Goods (Teff, Grains)", 0.60f, "7,200.00 ETB (60%)"),
                                Triple("Spices, Herbs & Organic Foods", 0.26f, "3,120.00 ETB (26%)"),
                                Triple("Traditional Textiles & Apparel", 0.14f, "1,680.00 ETB (14%)")
                            )
                            else -> listOf(
                                Triple("Agricultural Goods (Teff, Grains)", 0.65f, "7,800.00 ETB (65%)"),
                                Triple("Spices, Herbs & Organic Foods", 0.23f, "2,760.00 ETB (23%)"),
                                Triple("Traditional Textiles & Apparel", 0.12f, "1,440.00 ETB (12%)")
                            )
                        }
                    }
                    defaultList.take(3)
                }
                
                categoriesBreakdown.forEach { (cat, share, value) ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = cat, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { share },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = if (share > 0.5f) BrandGreenPrimary else BrandGoldSecondary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
        
        // Card 2: Top Products Detail (Top 10)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (selectedRevenueDriverTab == "Monthly") "🏆 Top Performing Products (Month: $activeMonthIso)" else "🏆 Top Performing Products (Week: $activeWeekIso)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val topProducts = if (currentRevenue != null && currentRevenue.top10Products.isNotEmpty()) {
                    currentRevenue.top10Products.take(10).map { (pName, rev, units) ->
                        Triple(pName, "$units units sold", String.format("%,.2f ETB", rev))
                    }
                } else {
                    val defaultList = if (selectedRevenueDriverTab == "Monthly") {
                        listOf(
                            Triple("Yirgacheffe Coffee (Premium beans)", "124 bags sold", "24,800.00 ETB"),
                            Triple("Magna Teff (First Grade)", "85 quintals sold", "7,450.00 ETB"),
                            Triple("Traditional Handwoven Kemis (Dress)", "12 dresses sold", "5,200.00 ETB"),
                            Triple("Organic Cardamom (Korarima)", "45 packs sold", "2,150.00 ETB"),
                            Triple("Premium Sidamo Honey (White)", "38 jars sold", "1,900.00 ETB"),
                            Triple("Berbere Spice Mix (Spicy Blend)", "95 packs sold", "1,850.00 ETB"),
                            Triple("Handmade Clay Coffee Pot (Jebena)", "22 items sold", "1,430.00 ETB"),
                            Triple("Traditional Men's Gabi (Heavy Cotton)", "8 items sold", "1,200.00 ETB"),
                            Triple("Black Cumin Seeds (Tikur Azmud)", "52 packs sold", "1,040.00 ETB"),
                            Triple("Ethiopian Butter (Kibe with spices)", "15 jars sold", "900.00 ETB")
                        )
                    } else {
                        when (selectedWeeklyPeriodIndex) {
                            0 -> listOf(
                                Triple("Yirgacheffe Coffee (Premium beans)", "28 bags sold", "5,600.00 ETB"),
                                Triple("Traditional Handwoven Kemis (Dress)", "4 dresses sold", "1,750.00 ETB"),
                                Triple("Magna Teff (First Grade)", "18 quintals sold", "1,580.00 ETB"),
                                Triple("Organic Cardamom (Korarima)", "12 packs sold", "580.00 ETB"),
                                Triple("Berbere Spice Mix (Spicy Blend)", "22 packs sold", "430.00 ETB"),
                                Triple("Premium Sidamo Honey (White)", "8 jars sold", "400.00 ETB"),
                                Triple("Handmade Clay Coffee Pot (Jebena)", "5 items sold", "325.00 ETB"),
                                Triple("Black Cumin Seeds (Tikur Azmud)", "12 packs sold", "240.00 ETB"),
                                Triple("Traditional Men's Gabi (Heavy Cotton)", "1 item sold", "150.00 ETB"),
                                Triple("Ethiopian Butter (Kibe with spices)", "2 jars sold", "120.00 ETB")
                            )
                            1 -> listOf(
                                Triple("Yirgacheffe Coffee (Premium beans)", "32 bags sold", "6,400.00 ETB"),
                                Triple("Magna Teff (First Grade)", "22 quintals sold", "1,930.00 ETB"),
                                Triple("Traditional Handwoven Kemis (Dress)", "3 dresses sold", "1,300.00 ETB"),
                                Triple("Organic Cardamom (Korarima)", "10 packs sold", "480.00 ETB"),
                                Triple("Berbere Spice Mix (Spicy Blend)", "24 packs sold", "470.00 ETB"),
                                Triple("Premium Sidamo Honey (White)", "9 jars sold", "450.00 ETB"),
                                Triple("Handmade Clay Coffee Pot (Jebena)", "6 items sold", "390.00 ETB"),
                                Triple("Black Cumin Seeds (Tikur Azmud)", "15 packs sold", "300.00 ETB"),
                                Triple("Traditional Men's Gabi (Heavy Cotton)", "2 items sold", "300.00 ETB"),
                                Triple("Ethiopian Butter (Kibe with spices)", "4 jars sold", "240.00 ETB")
                            )
                            2 -> listOf(
                                Triple("Yirgacheffe Coffee (Premium beans)", "26 bags sold", "5,200.00 ETB"),
                                Triple("Magna Teff (First Grade)", "15 quintals sold", "1,320.00 ETB"),
                                Triple("Traditional Handwoven Kemis (Dress)", "2 dresses sold", "850.00 ETB"),
                                Triple("Organic Cardamom (Korarima)", "11 packs sold", "530.00 ETB"),
                                Triple("Berbere Spice Mix (Spicy Blend)", "20 packs sold", "390.00 ETB"),
                                Triple("Premium Sidamo Honey (White)", "7 jars sold", "350.00 ETB"),
                                Triple("Handmade Clay Coffee Pot (Jebena)", "4 items sold", "260.00 ETB"),
                                Triple("Black Cumin Seeds (Tikur Azmud)", "10 packs sold", "200.00 ETB"),
                                Triple("Traditional Men's Gabi (Heavy Cotton)", "1 item sold", "150.00 ETB"),
                                Triple("Ethiopian Butter (Kibe with spices)", "3 jars sold", "180.00 ETB")
                            )
                            else -> listOf(
                                Triple("Yirgacheffe Coffee (Premium beans)", "28 bags sold", "5,600.00 ETB"),
                                Triple("Traditional Handwoven Kemis (Dress)", "2 dresses sold", "850.00 ETB"),
                                Triple("Magna Teff (First Grade)", "20 quintals sold", "1,750.00 ETB"),
                                Triple("Organic Cardamom (Korarima)", "8 packs sold", "380.00 ETB"),
                                Triple("Berbere Spice Mix (Spicy Blend)", "19 packs sold", "370.00 ETB"),
                                Triple("Premium Sidamo Honey (White)", "6 jars sold", "300.00 ETB"),
                                Triple("Handmade Clay Coffee Pot (Jebena)", "5 items sold", "325.00 ETB"),
                                Triple("Black Cumin Seeds (Tikur Azmud)", "8 packs sold", "160.00 ETB"),
                                Triple("Traditional Men's Gabi (Heavy Cotton)", "1 item sold", "150.00 ETB"),
                                Triple("Ethiopian Butter (Kibe with spices)", "2 jars sold", "120.00 ETB")
                            )
                        }
                    }
                    defaultList.take(10)
                }
                
                val visibleProducts = if (isTopProductsExpanded) topProducts else topProducts.take(3)
                
                visibleProducts.forEachIndexed { index, (name, sales, rev) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                if (index == 0) BrandGreenPrimary.copy(alpha = 0.05f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (index == 0) BrandGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (index == 0) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = sales, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = rev, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { isTopProductsExpanded = !isTopProductsExpanded },
                    modifier = Modifier.fillMaxWidth().testTag("toggle_top_products_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = if (isTopProductsExpanded) "Show Less ▴" else "Show More (up to 10) ▾",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        
        // Card 3: Business Recommendation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandGoldSecondary.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, BrandGoldSecondary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Actionable Revenue Insights",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedRevenueDriverTab == "Monthly") {
                        "1. Yirgacheffe Coffee dominates your revenue, representing 47% of total sales. Sourcing organic certification will allow a premium price bump of 15%.\n" +
                        "2. Traditional Textiles is a high margin but slow-moving segment. Bundle textiles with spices to capture premium gifting occasions during Ethiopian holidays."
                    } else {
                        "1. Weekly trends show peak sales for Spices on Wednesday and Thursday (Berbere preparation times). Position spice bundles mid-week to maximize conversions.\n" +
                        "2. Agricultural Grains (Teff) remain highly consistent week-over-week. Ensure restocking runs smoothly every Monday morning to prevent stockouts."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
}

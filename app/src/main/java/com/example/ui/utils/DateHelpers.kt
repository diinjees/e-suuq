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

fun formatPurchaseDate(timestamp: Long): String {
    return try {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.ENGLISH)
        sdf.format(java.util.Date(timestamp))
    } catch (e: Exception) {
        "June 26, 2026"
    }
}


fun isCalendarSameDay(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH) &&
           cal1.get(java.util.Calendar.DAY_OF_MONTH) == cal2.get(java.util.Calendar.DAY_OF_MONTH)
}


fun isCalendarBetween(date: java.util.Calendar, start: java.util.Calendar, end: java.util.Calendar): Boolean {
    val dateMs = date.timeInMillis
    val startMs = start.timeInMillis
    val endMs = end.timeInMillis
    return dateMs in startMs..endMs || isCalendarSameDay(date, start) || isCalendarSameDay(date, end)
}


fun clearCalendarTime(cal: java.util.Calendar): java.util.Calendar {
    val res = java.util.Calendar.getInstance()
    res.timeInMillis = cal.timeInMillis
    res.set(java.util.Calendar.HOUR_OF_DAY, 0)
    res.set(java.util.Calendar.MINUTE, 0)
    res.set(java.util.Calendar.SECOND, 0)
    res.set(java.util.Calendar.MILLISECOND, 0)
    return res
}


fun formatCalendarDate(cal: java.util.Calendar): String {
    val monthNamesShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val month = monthNamesShort[cal.get(java.util.Calendar.MONTH)]
    val year = cal.get(java.util.Calendar.YEAR)
    return "$month $day, $year"
}


fun getStatsForMonth(month: String): MonthlyStats {
    val parts = month.split(" ")
    val monthName = parts.getOrNull(0) ?: "January"
    val yearNum = parts.getOrNull(1)?.toIntOrNull() ?: 2026
    
    val baseSales = when (monthName) {
        "January" -> 32400.0
        "February" -> 41250.0
        "March" -> 44800.0
        "April" -> 36150.0
        "May" -> 49800.0
        "June" -> 52000.0
        "July" -> 54500.0
        "August" -> 43000.0
        "September" -> 46000.0
        "October" -> 48500.0
        "November" -> 51000.0
        "December" -> 58000.0
        else -> 45000.0
    }
    
    val yearFactor = if (yearNum > 2026) 1.1 else if (yearNum < 2026) 0.9 else 1.0
    val grossSales = baseSales * yearFactor
    
    val soldCount = (grossSales / 200).toInt()
    val unsoldRemaining = when (monthName) {
        "January" -> 50
        "February" -> 45
        "March" -> 30
        "April" -> 65
        "May" -> 35
        "June" -> 40
        else -> 42
    }
    
    val cancelledCount = when (monthName) {
        "January" -> 8
        "February" -> 12
        "March" -> 6
        "April" -> 15
        "May" -> 5
        "June" -> 9
        "July" -> 11
        else -> 7
    }
    val refundLoss = cancelledCount * 185.0
    
    val productCost = grossSales * 0.32
    val logisticsCost = grossSales * 0.08
    val netProfit = grossSales - productCost - logisticsCost
    val telebirrPayout = netProfit * 0.70
    val cbePayout = netProfit * 0.30
    
    val targetRatio = (grossSales / 50000.0) * 100
    val percentageOfTarget = String.format("%.0f%%", targetRatio)
    
    return MonthlyStats(
        grossSales = grossSales,
        soldCount = soldCount,
        unsoldRemaining = unsoldRemaining,
        cancelledCount = cancelledCount,
        refundLoss = refundLoss,
        productCost = productCost,
        logisticsCost = logisticsCost,
        netProfit = netProfit,
        telebirrPayout = telebirrPayout,
        cbePayout = cbePayout,
        percentageOfTarget = percentageOfTarget
    )
}



// CalendarComponents.kt
package com.example.ui.components


import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.testTag

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.compose.ui.window.Dialog

import androidx.compose.ui.window.DialogProperties

import java.text.SimpleDateFormat

import java.util.*

// ==========================================
// DATA CLASSES
// ==========================================

data class CalendarDateState(
    val day: Int,
    val calendar: Calendar,
    val isStart: Boolean = false,
    val isEnd: Boolean = false,
    val isInRange: Boolean = false,
    val isSelected: Boolean = false,
    val isToday: Boolean = false
)

data class CalendarSelection(
    val startDate: Calendar? = null,
    val endDate: Calendar? = null,
    val label: String = "All Time"
)

// ==========================================
// MAIN CALENDAR COMPONENT
// ==========================================

@Composable
fun UnifiedCalendarDialog(
    initialStartDate: Calendar? = null,
    initialEndDate: Calendar? = null,
    onConfirm: (startDate: Calendar?, endDate: Calendar?, label: String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Date Range",
    subtitle: String = "Tap to select a start and end date",
    showPresets: Boolean = true,
    maxYear: Int = 2026,
    minYear: Int = 2022
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }
    
    var tempStartDate by remember { mutableStateOf(initialStartDate) }
    var tempEndDate by remember { mutableStateOf(initialEndDate) }
    var tempLabel by remember { mutableStateOf("All Time") }
    var tempPreset by remember { mutableStateOf<String?>(null) }
    
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    // Update tempLabel when dates change
    LaunchedEffect(tempStartDate, tempEndDate) {
        tempLabel = when {
            tempStartDate != null && tempEndDate != null -> {
                "Custom: ${formatDate(tempStartDate!!)} - ${formatDate(tempEndDate!!)}"
            }
            tempStartDate != null -> {
                "From ${formatDate(tempStartDate!!)}"
            }
            else -> "All Time"
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title Section
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Current Selection Display
                SelectionDisplay(
                    startDate = tempStartDate,
                    endDate = tempEndDate,
                    onClear = {
                        tempStartDate = null
                        tempEndDate = null
                        tempPreset = null
                        tempLabel = "All Time"
                    }
                )
                
                // Presets (Optional)
                if (showPresets) {
                    PresetsRow(
                        onPresetSelected = { label, start, end ->
                            tempPreset = label
                            tempStartDate = start
                            tempEndDate = end
                            tempLabel = label
                        },
                        selectedPreset = tempPreset
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                
                // Calendar Navigation
                CalendarNavigation(
                    currentMonth = currentMonth,
                    currentYear = currentYear,
                    monthNames = monthNames,
                    showMonthDropdown = showMonthDropdown,
                    showYearDropdown = showYearDropdown,
                    onMonthChange = { currentMonth = it },
                    onYearChange = { currentYear = it },
                    onToggleMonthDropdown = { showMonthDropdown = it },
                    onToggleYearDropdown = { showYearDropdown = it },
                    onPrevMonth = {
                        if (currentMonth == 0) {
                            currentMonth = 11
                            currentYear--
                        } else {
                            currentMonth--
                        }
                    },
                    onNextMonth = {
                        if (currentMonth == 11) {
                            currentMonth = 0
                            currentYear++
                        } else {
                            currentMonth++
                        }
                    },
                    minYear = minYear,
                    maxYear = maxYear
                )
                
                // Calendar Grid
                CalendarGrid(
                    year = currentYear,
                    month = currentMonth,
                    startDate = tempStartDate,
                    endDate = tempEndDate,
                    onDateSelected = { date ->
                        tempPreset = null
                        when {
                            tempStartDate == null -> {
                                tempStartDate = date
                                tempEndDate = null
                            }
                            tempEndDate == null -> {
                                if (date.timeInMillis < tempStartDate!!.timeInMillis) {
                                    // Swap if end date is before start date
                                    tempStartDate = date
                                } else {
                                    tempEndDate = date
                                }
                            }
                            else -> {
                                // Reset and start fresh
                                tempStartDate = date
                                tempEndDate = null
                            }
                        }
                    }
                )
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            tempStartDate = null
                            tempEndDate = null
                            tempPreset = null
                            tempLabel = "All Time"
                        },
                        modifier = Modifier.testTag("calendar_clear_btn")
                    ) {
                        Text(
                            text = "Clear",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("calendar_cancel_btn")
                        ) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        Button(
                            onClick = {
                                onConfirm(tempStartDate, tempEndDate, tempLabel)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("calendar_apply_btn")
                        ) {
                            Text(
                                text = "Apply",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
private fun SelectionDisplay(
    startDate: Calendar?,
    endDate: Calendar?,
    onClear: () -> Unit
) {
    val displayText = when {
        startDate != null && endDate != null -> {
            "📅 ${formatDate(startDate)} → ${formatDate(endDate)}"
        }
        startDate != null -> {
            "📅 From ${formatDate(startDate)} (Select end date)"
        }
        else -> "📅 No range selected"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (startDate != null) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (startDate != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear selection",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PresetsRow(
    onPresetSelected: (label: String, start: Calendar, end: Calendar) -> Unit,
    selectedPreset: String?
) {
    val presets = listOf(
        "Today" to { getTodayRange() },
        "This Week" to { getWeekRange() },
        "This Month" to { getMonthRange() },
        "Last 30 Days" to { getLast30DaysRange() }
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        presets.forEach { (label, rangeProvider) ->
            val isSelected = selectedPreset == label
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val (start, end) = rangeProvider()
                        onPresetSelected(label, start, end)
                    }
                    .testTag("preset_${label.replace(" ", "_")}"),
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun CalendarNavigation(
    currentMonth: Int,
    currentYear: Int,
    monthNames: List<String>,
    showMonthDropdown: Boolean,
    showYearDropdown: Boolean,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    onToggleMonthDropdown: (Boolean) -> Unit,
    onToggleYearDropdown: (Boolean) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    minYear: Int,
    maxYear: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevMonth,
            modifier = Modifier.size(36.dp),
            enabled = currentYear > minYear || currentMonth > 0
        ) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous Month",
                modifier = Modifier.size(24.dp)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Month Dropdown
            Box {
                Text(
                    text = monthNames[currentMonth],
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onToggleMonthDropdown(!showMonthDropdown) }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("month_select_trigger")
                )
                
                DropdownMenu(
                    expanded = showMonthDropdown,
                    onDismissRequest = { onToggleMonthDropdown(false) },
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    monthNames.forEachIndexed { idx, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onMonthChange(idx)
                                onToggleMonthDropdown(false)
                            }
                        )
                    }
                }
            }
            
            // Year Dropdown
            Box {
                Text(
                    text = currentYear.toString(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onToggleYearDropdown(!showYearDropdown) }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("year_select_trigger")
                )
                
                DropdownMenu(
                    expanded = showYearDropdown,
                    onDismissRequest = { onToggleYearDropdown(false) },
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    (minYear..maxYear).forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                onYearChange(year)
                                onToggleYearDropdown(false)
                            }
                        )
                    }
                }
            }
        }
        
        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(36.dp),
            enabled = currentYear < maxYear || currentMonth < 11
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Next Month",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    startDate: Calendar?,
    endDate: Calendar?,
    onDateSelected: (Calendar) -> Unit
) {
    val calendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()
    
    // Weekday headers
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
    
    // Calendar days grid
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val totalCells = daysInMonth + firstDayOfWeek - 1
        val rows = (totalCells + 6) / 7
        
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val dayIndex = row * 7 + col
                    val dayNumber = dayIndex - firstDayOfWeek + 2
                    
                    if (dayIndex < firstDayOfWeek - 1 || dayNumber > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dateCal = Calendar.getInstance().apply {
                            set(year, month, dayNumber, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        
                        val isStart = startDate?.let { isSameDay(it, dateCal) } ?: false
                        val isEnd = endDate?.let { isSameDay(it, dateCal) } ?: false
                        val isInRange = startDate != null && endDate != null && 
                            dateCal.timeInMillis in startDate.timeInMillis..endDate.timeInMillis &&
                            !isStart && !isEnd
                        val isToday = isSameDay(today, dateCal)
                        
                        CalendarDayCell(
                            dayNumber = dayNumber,
                            isStart = isStart,
                            isEnd = isEnd,
                            isInRange = isInRange,
                            isToday = isToday,
                            onClick = { onDateSelected(dateCal) },
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isStart: Boolean,
    isEnd: Boolean,
    isInRange: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isStart || isEnd -> MaterialTheme.colorScheme.primary
        isInRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    
    val textColor = when {
        isStart || isEnd -> Color.White
        isInRange || isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val shape = when {
        isStart && isEnd -> RoundedCornerShape(50.dp)
        isStart -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
        isEnd -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
        isInRange -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(50.dp)
    }
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .border(
                width = if (isToday && !isStart && !isEnd) 1.5.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isStart || isEnd || isToday) FontWeight.Bold 
                            else FontWeight.Medium
            ),
            color = textColor
        )
    }
}

// ==========================================
// HELPER FUNCTIONS
// ==========================================

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
           cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}

fun formatDate(calendar: Calendar): String {
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${monthNames[calendar.get(Calendar.MONTH)]} ${calendar.get(Calendar.DAY_OF_MONTH)}, ${calendar.get(Calendar.YEAR)}"
}

fun formatDateShort(calendar: Calendar): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(calendar.time)
}

fun formatDateForDisplay(calendar: Calendar): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(calendar.time)
}

fun getTodayRange(): Pair<Calendar, Calendar> {
    val today = Calendar.getInstance()
    val start = today.clone() as Calendar
    val end = today.clone() as Calendar
    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)
    end.set(Calendar.HOUR_OF_DAY, 23)
    end.set(Calendar.MINUTE, 59)
    end.set(Calendar.SECOND, 59)
    end.set(Calendar.MILLISECOND, 999)
    return start to end
}

fun getWeekRange(): Pair<Calendar, Calendar> {
    val today = Calendar.getInstance()
    val start = today.clone() as Calendar
    val end = today.clone() as Calendar
    
    // Start of week (Monday)
    val dayOfWeek = start.get(Calendar.DAY_OF_WEEK)
    val diff = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    start.add(Calendar.DAY_OF_MONTH, -diff)
    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)
    
    // End of week (Sunday)
    end.timeInMillis = start.timeInMillis
    end.add(Calendar.DAY_OF_MONTH, 6)
    end.set(Calendar.HOUR_OF_DAY, 23)
    end.set(Calendar.MINUTE, 59)
    end.set(Calendar.SECOND, 59)
    end.set(Calendar.MILLISECOND, 999)
    
    return start to end
}

fun getMonthRange(): Pair<Calendar, Calendar> {
    val today = Calendar.getInstance()
    val start = today.clone() as Calendar
    val end = today.clone() as Calendar
    
    start.set(Calendar.DAY_OF_MONTH, 1)
    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)
    
    end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
    end.set(Calendar.HOUR_OF_DAY, 23)
    end.set(Calendar.MINUTE, 59)
    end.set(Calendar.SECOND, 59)
    end.set(Calendar.MILLISECOND, 999)
    
    return start to end
}

fun getLast30DaysRange(): Pair<Calendar, Calendar> {
    val today = Calendar.getInstance()
    val start = today.clone() as Calendar
    val end = today.clone() as Calendar
    
    start.add(Calendar.DAY_OF_MONTH, -29)
    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)
    
    end.set(Calendar.HOUR_OF_DAY, 23)
    end.set(Calendar.MINUTE, 59)
    end.set(Calendar.SECOND, 59)
    end.set(Calendar.MILLISECOND, 999)
    
    return start to end
}

// ==========================================
// EXTENSION FUNCTIONS FOR EASY INTEGRATION
// ==========================================

fun Calendar.toDateString(): String {
    return String.format(
        "%d-%02d-%02d",
        this.get(Calendar.YEAR),
        this.get(Calendar.MONTH) + 1,
        this.get(Calendar.DAY_OF_MONTH)
    )
}

fun String.toCalendar(): Calendar? {
    return try {
        val parts = this.split("-")
        if (parts.size == 3) {
            Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } else null
    } catch (e: Exception) {
        null
    }
}

fun String.toCalendarEnd(): Calendar? {
    return try {
        val parts = this.split("-")
        if (parts.size == 3) {
            Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        } else null
    } catch (e: Exception) {
        null
    }
}
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
fun StoreCard(
    storeName: String,
    emoji: String,
    color: Color,
    grade: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .width(160.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .testTag("store_card_$storeName")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile image circular avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.05f))
                        )
                    )
                    .border(2.dp, color.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 28.sp)
            }
            
            // Store Name
            Text(
                text = storeName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Business Grade Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrandGoldSecondary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = BrandGoldSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = grade,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
fun StoreTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    TabRow(
        selectedTabIndex = if (selectedTab == "Products") 0 else if (selectedTab == "Reviews") 1 else 2,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[if (selectedTab == "Products") 0 else if (selectedTab == "Reviews") 1 else 2]),
                color = BrandGreenPrimary
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Tab(
            selected = selectedTab == "Products",
            onClick = { onTabSelected("Products") },
            modifier = Modifier.testTag("store_tab_products")
        ) {
            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Store, null, modifier = Modifier.size(16.dp), tint = if (selectedTab == "Products") BrandGreenPrimary else MaterialTheme.colorScheme.outline)
                    Text("Shop Products", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (selectedTab == "Products") BrandGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Tab(
            selected = selectedTab == "Reviews",
            onClick = { onTabSelected("Reviews") },
            modifier = Modifier.testTag("store_tab_reviews")
        ) {
            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = if (selectedTab == "Reviews") BrandGreenPrimary else MaterialTheme.colorScheme.outline)
                    Text("Reviews", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (selectedTab == "Reviews") BrandGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Tab(
            selected = selectedTab == "Info",
            onClick = { onTabSelected("Info") },
            modifier = Modifier.testTag("store_tab_info")
        ) {
            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = if (selectedTab == "Info") BrandGreenPrimary else MaterialTheme.colorScheme.outline)
                    Text("Store Details", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (selectedTab == "Info") BrandGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}



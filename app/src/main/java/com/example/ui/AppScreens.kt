package com.example.ui


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
fun ESuuqApp(viewModel: AppViewModel) {
    val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDark = when (themeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()

    val localization = remember(language) { Localization(language) }

    CompositionLocalProvider(LocalLocalization provides localization) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetScreen ->
                    when (targetScreen) {
                        AppScreen.SPLASH -> SplashScreen(viewModel)
                        AppScreen.ONBOARDING -> OnboardingScreen(viewModel)
                        AppScreen.APP_LOCK -> AppLockScreen(viewModel)
                        AppScreen.OTP_VERIFICATION -> OtpScreen(viewModel)
                        AppScreen.MAIN -> MainDashboardScreen(viewModel)
                        AppScreen.TRACK_ORDER -> TrackOrderScreen(viewModel)
                        AppScreen.EDIT_PRODUCT_SCREEN -> EditProductScreen(viewModel)
                        AppScreen.HELP_CENTER -> HelpCenterScreen(viewModel)
                        AppScreen.ABOUT_SCREEN -> AboutScreen(viewModel)
                    }
                }
                
                // Live in-app debug panel overlay
                DebugOverlay(viewModel = viewModel)
            }
        }
    }
}


@Composable
fun MainDashboardScreen(viewModel: AppViewModel) {
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedProductForDetail by viewModel.selectedProductForDetail.collectAsStateWithLifecycle()
    val selectedStoreName by viewModel.selectedStoreName.collectAsStateWithLifecycle()
    val selectedChatRoomId by viewModel.selectedChatRoomId.collectAsStateWithLifecycle()
    val profileSubScreen by viewModel.profileSubScreen.collectAsStateWithLifecycle()
    val activeDashboardSubView by viewModel.activeDashboardSubView.collectAsStateWithLifecycle()
    val showNotificationsPage by viewModel.showNotificationsPage.collectAsStateWithLifecycle()
    val showUserSearchPage by viewModel.showUserSearchPage.collectAsStateWithLifecycle()
    val viewAllPageType by viewModel.viewAllPageType.collectAsStateWithLifecycle()
    val showPromoDetailsIndex by viewModel.showPromoDetailsIndex.collectAsStateWithLifecycle()
    val activeOrderSuccess by viewModel.activeOrderSuccess.collectAsStateWithLifecycle()

    val currentRole = activeUser?.role ?: "BUYER"

    if (selectedProductForDetail != null) {
        ProductDetailPage(
            product = selectedProductForDetail!!,
            viewModel = viewModel,
            onBack = { viewModel.setSelectedProductForDetail(null) }
        )
    } else if (selectedStoreName != null) {
        StoreDetailPage(
            storeName = selectedStoreName!!,
            viewModel = viewModel,
            onBack = { viewModel.setSelectedStoreName(null) }
        )
    } else {
        Scaffold(
            bottomBar = {
                val showBottomMenu = (currentTab != MainTab.CHAT || selectedChatRoomId == null) &&
                        !(currentTab == MainTab.PROFILE && profileSubScreen != "MENU") &&
                        !(currentTab == MainTab.MIDDLE && activeDashboardSubView != "MAIN") &&
                        !(currentTab == MainTab.MIDDLE && currentRole == "BUYER") &&
                        !showNotificationsPage &&
                        !showUserSearchPage &&
                        (viewAllPageType == null) &&
                        (showPromoDetailsIndex == null) &&
                        (activeOrderSuccess == null)
                if (showBottomMenu) {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                Column {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home Tab
                        val homeSelected = currentTab == MainTab.HOME
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.selectTab(MainTab.HOME) }
                                .testTag("tab_home"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (homeSelected) Color(0xFFD8E7CB) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = if (homeSelected) Color(0xFF131F0D) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = loc("home"),
                                fontSize = 10.sp,
                                fontWeight = if (homeSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (homeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Middle Floating Button
                        val middleSelected = currentTab == MainTab.MIDDLE
                        val (middleIcon, middleLabel, middleTag) = when (currentRole) {
                            "SELLER" -> Triple(Icons.Default.Storefront, loc("manage_store"), "tab_middle_seller")
                            "DELIVERY" -> Triple(Icons.Default.DeliveryDining, loc("manage_delivery"), "tab_middle_delivery")
                            else -> Triple(Icons.Default.Search, loc("search"), "tab_middle_buyer")
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .offset(y = (-12).dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.selectTab(MainTab.MIDDLE) }
                                .testTag(middleTag),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(6.dp, RoundedCornerShape(16.dp))
                                    .background(
                                        color = if (middleSelected) MaterialTheme.colorScheme.secondary else BrandGreenPrimary,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(4.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = middleIcon,
                                    contentDescription = middleLabel,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = middleLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (middleSelected) MaterialTheme.colorScheme.secondary else BrandGreenPrimary
                            )
                        }

                        // Cart Tab
                        val cartSelected = currentTab == MainTab.CART
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.selectTab(MainTab.CART) }
                                .testTag("tab_cart"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (cartSelected) Color(0xFFD8E7CB) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = if (cartSelected) Color(0xFF131F0D) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    val cartSize = viewModel.cart.collectAsStateWithLifecycle().value.values.sum()
                                    if (cartSize > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(BrandRedTertiary)
                                                .align(Alignment.TopEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cartSize.toString(),
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = loc("cart"),
                                fontSize = 10.sp,
                                fontWeight = if (cartSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (cartSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Chat Tab
                        val chatSelected = currentTab == MainTab.CHAT
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.selectTab(MainTab.CHAT) }
                                .testTag("tab_chat"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (chatSelected) Color(0xFFD8E7CB) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Chat",
                                    tint = if (chatSelected) Color(0xFF131F0D) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = loc("chat"),
                                fontSize = 10.sp,
                                fontWeight = if (chatSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (chatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Profile Tab
                        val profileSelected = currentTab == MainTab.PROFILE
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.selectTab(MainTab.PROFILE) }
                                .testTag("tab_profile"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (profileSelected) Color(0xFFD8E7CB) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    tint = if (profileSelected) Color(0xFF131F0D) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = loc("profile"),
                                fontSize = 10.sp,
                                fontWeight = if (profileSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (profileSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                MainTab.HOME -> HomeScreen(viewModel)
                MainTab.MIDDLE -> {
                    when (currentRole) {
                        "SELLER" -> SellerDashboardScreen(viewModel)
                        "DELIVERY" -> DeliveryDashboardScreen(viewModel)
                        else -> SearchScreen(viewModel, onBack = { viewModel.selectTab(MainTab.HOME) })
                    }
                }
                MainTab.CART -> CartScreen(viewModel)
                MainTab.CHAT -> ChatScreenComponent(viewModel)
                MainTab.PROFILE -> ProfileScreen(viewModel)
            }
        }
    }
}
}

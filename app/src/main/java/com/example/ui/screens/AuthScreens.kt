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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.ui.theme.LightBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.R
import com.example.ui.*
import com.example.ui.screens.*
import com.example.ui.components.*
import com.example.ui.utils.*

// ==========================================
// SPLASH SCREEN
// ==========================================

@Composable
fun SplashScreen(viewModel: AppViewModel) {
    var currentSlide by remember { mutableStateOf(0) }
    val totalSlides = 3
    var dragAmountAccumulator by remember { mutableStateOf(0f) }

    val slideTitles = listOf(
        "Ethiopia's Direct Suuq",
        "Direct Farmer & Weaver Trade",
        "Fast Community Couriers"
    )

    val slideDescriptions = listOf(
        "Welcome to E-Suuq! We connect you directly with local farmers, weavers, and trusted couriers. Support direct trade with zero middleman costs.",
        "Buy organic crops, Sidama coffee, Amhara honey, and beautiful hand-woven garments directly from the producers who make them.",
        "Get direct contact with community-approved Bajaj and motorcycle couriers. Quick, secure delivery with live tracking and zero broker fees."
    )

    val slideIcons = listOf(
        Icons.Default.Storefront,
        Icons.Default.ShoppingCart,
        Icons.Default.LocalShipping
    )

    // ✅ Check auth status on splash
    LaunchedEffect(Unit) {
        val isFirst = viewModel.isFirstOpen()
        val hasSession = viewModel.hasValidSession()
        
        if (!isFirst) {
            // ✅ Not first open - bypass splash screen
            if (hasSession) {
                val activeUser = viewModel.getActiveUser()
                if (activeUser?.isBiometricEnabled == true) {
                    viewModel.navigateTo(AppScreen.APP_LOCK)
                } else {
                    viewModel.navigateTo(AppScreen.MAIN)
                }
            } else {
                viewModel.navigateTo(AppScreen.ONBOARDING)
            }
        } else {
            // ✅ First open - show splash/onboarding slides, but handle session if somehow logged in
            if (hasSession) {
                val activeUser = viewModel.getActiveUser()
                if (activeUser?.isBiometricEnabled == true) {
                    viewModel.navigateTo(AppScreen.APP_LOCK)
                } else {
                    viewModel.navigateTo(AppScreen.MAIN)
                }
            }
        }
    }
    
    fun completeOnboarding() {
        viewModel.setFirstOpen(false)
        viewModel.navigateTo(AppScreen.ONBOARDING)
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BrandGreenPrimary,
                            Color(0xFF14472B)
                        )
                    )
                )
                .padding(padding)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragAmountAccumulator > 120f) {
                                // Swipe right -> Previous slide
                                if (currentSlide > 0) {
                                    currentSlide--
                                }
                            } else if (dragAmountAccumulator < -120f) {
                                // Swipe left -> Next slide
                                if (currentSlide < totalSlides - 1) {
                                    currentSlide++
                                } else {
                                    completeOnboarding()
                                }
                            }
                            dragAmountAccumulator = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragAmountAccumulator += dragAmount
                        }
                    )
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Top Skip bar with logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: App Logo and Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_app_1784199175789),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(4.dp)
                        )
                        Text(
                            text = "E-Suuq",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                    }

                    if (currentSlide < totalSlides - 1) {
                        TextButton(
                            onClick = { completeOnboarding() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                        ) {
                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }

                // Center Content: Graphic & Text info
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        if (targetState > initialState) {
                            // Slide from right to left
                            (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
                            )
                        } else {
                            // Slide from left to right
                            (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300))
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "whole_slide_anim"
                ) { slideIdx ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Illustration Frame
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.1f)
                                .shadow(12.dp, RoundedCornerShape(28.dp))
                                .clip(RoundedCornerShape(28.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_onboarding_hero_1784142712436),
                                    contentDescription = "E-Suuq Market Illustration",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                // App brand small tag
                                Box(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.TopStart)
                                        .background(BrandGreenPrimary, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = BrandGoldSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "100% Direct",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Icon indicator
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = slideIcons[slideIdx],
                                contentDescription = null,
                                tint = BrandGoldSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = slideTitles[slideIdx],
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = slideDescriptions[slideIdx],
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 24.sp
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // Bottom Indicator and Button Panel
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Slide dots indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until totalSlides) {
                            val isActive = i == currentSlide
                            val width by animateDpAsState(
                                targetValue = if (isActive) 24.dp else 8.dp,
                                animationSpec = tween(300)
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) BrandGoldSecondary else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }

                    // Slide controls button
                    Button(
                        onClick = {
                            if (currentSlide < totalSlides - 1) {
                                currentSlide++
                            } else {
                                completeOnboarding()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BrandGreenPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("splash_start_button"),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (currentSlide == totalSlides - 1) "Get Started" else "Next",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Icon(
                                imageVector = if (currentSlide == totalSlides - 1) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// HELPER: GET FILE NAME FROM URI
// ==========================================

fun getFileNameFromUri(context: android.content.Context, uriString: String?): String? {
    if (uriString == null) return null
    val uri = android.net.Uri.parse(uriString)
    var name: String? = null
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
    } catch (e: Exception) {
        // Fallback
    }
    if (name == null) {
        name = uri.lastPathSegment
    }
    return name ?: "document.png"
}

// ==========================================
// HELPER: CONVERT URI TO FILE
// ==========================================

fun uriToFile(context: android.content.Context, uriString: String?): java.io.File? {
    if (uriString == null) return null
    try {
        val uri = android.net.Uri.parse(uriString)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = java.io.File(context.cacheDir, "temp_id_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return file
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// ==========================================
// ONBOARDING SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: AppViewModel) {
    var isLoginMode by remember { mutableStateOf(true) }
    var registrationStep by remember { mutableStateOf(1) }

    // Loading & Dialog State
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    // Focus Requesters
    val regNameFocusRequester = remember { FocusRequester() }
    val regPhoneFocusRequester = remember { FocusRequester() }
    val regEmailFocusRequester = remember { FocusRequester() }
    val regBirthDateFocusRequester = remember { FocusRequester() }
    val loginPasswordFocusRequester = remember { FocusRequester() }

    // Login Form State
    var loginPhone by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginErrorText by remember { mutableStateOf<String?>(null) }

    // Registration Form State
    var regName by remember { mutableStateOf("") }
    var regPhoneInput by remember { mutableStateOf("") }
    var regEmailInput by remember { mutableStateOf("") }
    var regBirthDate by remember { mutableStateOf("") }
    var regPasswordInput by remember { mutableStateOf("") }
    var regFromReferCodeInput by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val idDocInfront by viewModel.regIdDocumentInfront.collectAsStateWithLifecycle()
    val idDocBackfront by viewModel.regIdDocumentBackfront.collectAsStateWithLifecycle()

    // ✅ Launchers for ID Document Images
    val launcherInfront = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setRegIdDocumentInfront(uri.toString())
            Toast.makeText(context, "ID Front uploaded successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    val launcherBackfront = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setRegIdDocumentBackfront(uri.toString())
            Toast.makeText(context, "ID Back uploaded successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Address Picker state variables
    var selectedRegion by remember { mutableStateOf("Addis Ababa") }
    var selectedZone by remember { mutableStateOf("Bole Sub-City") }
    var selectedCity by remember { mutableStateOf("Addis Ababa") }
    var selectedKebele by remember { mutableStateOf("03") }

    var regionExpanded by remember { mutableStateOf(false) }
    var zoneExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var kebeleExpanded by remember { mutableStateOf(false) }

    val regions = listOf("Addis Ababa", "Oromia", "Amhara", "Tigray", "Somali", "Sidama", "SNNPR", "Afar")
    val zones = listOf("Bole Sub-City", "Kirkos Sub-City", "Yeka Sub-City", "Arada Sub-City", "Nifas Silk Sub-City", "Adama Zone", "Hawassa Zone")
    val cities = listOf("Addis Ababa", "Adama", "Bahir Dar", "Mekelle", "Jijiga", "Hawassa", "Semera")
    val kebeles = listOf("01", "02", "03", "04", "05", "06", "10", "12", "15")

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    if (isLoading) {
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .size(150.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                border = BorderStroke(1.dp, if (isSystemInDarkTheme()) Color(0xFF2C4035) else Color(0xFFE2EFE7))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = BrandGreenPrimary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Please wait...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding() // Automatically adjusts when keyboard is visible
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Decorative Brand Badge with custom App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .border(1.5.dp, BrandGreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_app_1784199175789),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Header Titles
            Text(
                text = "E-Suuq",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                ),
                color = BrandGreenPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ethiopia's Direct Community Marketplace",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Elegant Pill-shaped Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(if (isSystemInDarkTheme()) Color(0xFF1E2F25) else Color(0xFFE2EFE7), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sign In Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLoginMode) (if (isSystemInDarkTheme()) Color(0xFF2C4035) else Color.White) else Color.Transparent)
                        .clickable { 
                            isLoginMode = true 
                            loginErrorText = null
                            registrationStep = 1
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = if (isLoginMode) BrandGreenPrimary else (if (isSystemInDarkTheme()) Color.LightGray else Color.Gray),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                // Create Account Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isLoginMode) (if (isSystemInDarkTheme()) Color(0xFF2C4035) else Color.White) else Color.Transparent)
                        .clickable { 
                            isLoginMode = false 
                            loginErrorText = null
                            registrationStep = 1
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create Account",
                        color = if (!isLoginMode) BrandGreenPrimary else (if (isSystemInDarkTheme()) Color.LightGray else Color.Gray),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Dynamic Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isSystemInDarkTheme()) Color(0xFF2C4035) else Color(0xFFE2EFE7))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoginMode) {
                        // ==========================================
                        // LOGIN FORM
                        // ==========================================
                        Text(
                            text = "Selan, Welcome Back! 👋",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = BrandGreenPrimary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Text(
                            text = "Log in to trade directly with sellers and local couriers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 20.dp)
                        )

                        val triggerLoginAction = {
                            if (loginPhone.isBlank() || loginPassword.length != 6) {
                                loginErrorText = "Please enter phone and a valid 6-digit numeric password."
                            } else {
                                viewModel.loginUser(
                                    phone = loginPhone,
                                    password = loginPassword,
                                    onOtpRequired = { otp ->
                                        viewModel.navigateTo(AppScreen.OTP_VERIFICATION)
                                    },
                                    onSuccess = {
                                        viewModel.navigateTo(AppScreen.MAIN)
                                    },
                                    onIncorrect = {
                                        val err = viewModel.errorMessage.value
                                        loginErrorText = if (!err.isNullOrBlank()) {
                                            "Error: $err"
                                        } else {
                                            "Incorrect phone or password. Register if you don't have an account."
                                        }
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = loginPhone,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 9) {
                                    if (input.isEmpty() || input.startsWith("9") || input.startsWith("7")) {
                                        loginPhone = input
                                    }
                                }
                            },
                            label = { Text("Phone Number") },
                            prefix = { Text("+251 ", fontWeight = FontWeight.Bold, color = BrandGreenPrimary) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { loginPasswordFocusRequester.requestFocus() }
                            ),
                            placeholder = { Text("9xxxxxxxx") },
                            modifier = Modifier.fillMaxWidth().testTag("login_phone_input"),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BrandGreenPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 6) {
                                    loginPassword = input
                                }
                            },
                            label = { Text("6-Digit Password") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { triggerLoginAction() }
                            ),
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("******") },
                            modifier = Modifier.fillMaxWidth().focusRequester(loginPasswordFocusRequester).testTag("login_password_input"),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandGreenPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (loginErrorText != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BrandRedTertiary.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Error, null, tint = BrandRedTertiary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = loginErrorText!!,
                                        color = BrandRedTertiary,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Button
                        Button(
                            onClick = { triggerLoginAction() },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("login_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Login",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }

                    } else {
                        // ==========================================
                        // REGISTER MULTI-STEP WIZARD
                        // ==========================================
                        
                        // Stepper Progress Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        ) {
                            // Middle Connecting Line (spans 2/3 of the total width, centered)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.66f)
                                    .height(2.dp)
                                    .background(if (isSystemInDarkTheme()) Color(0xFF2C4035) else Color(0xFFD0E6DA))
                                    .align(Alignment.TopCenter)
                                    .offset(y = 13.dp) // centered vertically relative to the 28.dp circles
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("Profile", "Location", "ID & Security").forEachIndexed { index, label ->
                                    val stepNum = index + 1
                                    val isActive = registrationStep == stepNum
                                    val isCompleted = registrationStep > stepNum
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isActive) BrandGreenPrimary 
                                                    else if (isCompleted) BrandGoldSecondary 
                                                    else if (isSystemInDarkTheme()) Color(0xFF1E2F25)
                                                    else Color(0xFFE2EFE7)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCompleted) {
                                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            } else {
                                                Text(
                                                    text = stepNum.toString(),
                                                    color = if (isActive) Color.White else if (isSystemInDarkTheme()) Color.LightGray else Color.Gray,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isActive) BrandGreenPrimary else if (isSystemInDarkTheme()) Color.LightGray else Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = if (isSystemInDarkTheme()) Color(0xFF2C4035) else Color(0xFFE2EFE7), modifier = Modifier.padding(bottom = 20.dp))

                        // Render active step
                        when (registrationStep) {
                            1 -> {
                                // STEP 1: Personal Details
                                val triggerStep1Next = {
                                    val isPhoneValid = regPhoneInput.length == 9 && (regPhoneInput.startsWith("9") || regPhoneInput.startsWith("7"))
                                    val dateParts = regBirthDate.split("-")
                                    val isDateValid = if (dateParts.size == 3) {
                                        val y = dateParts[0].toIntOrNull()
                                        val m = dateParts[1].toIntOrNull()
                                        val d = dateParts[2].toIntOrNull()
                                        y != null && m != null && d != null && m in 1..12 && d in 1..31
                                    } else false

                                    if (regName.isBlank() || regPhoneInput.isBlank() || regEmailInput.isBlank() || regBirthDate.isBlank()) {
                                        loginErrorText = "Please complete all fields to proceed."
                                    } else if (!isPhoneValid) {
                                        loginErrorText = "Phone number must be exactly 9 digits and start with 9 or 7."
                                    } else if (!isDateValid || regBirthDate.length != 10) {
                                        loginErrorText = "Birth Date must be a valid date in YYYY-MM-DD format (Month 01-12, Day 01-31)."
                                    } else {
                                        loginErrorText = null
                                        registrationStep = 2
                                    }
                                }

                                Text(
                                    text = "Personal Profile Details",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = BrandGreenPrimary,
                                    modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "Provide your basic profile information to get started.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                                )

                                OutlinedTextField(
                                    value = regName,
                                    onValueChange = { regName = it },
                                    label = { Text("Full Name") },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { regPhoneFocusRequester.requestFocus() }
                                    ),
                                    modifier = Modifier.fillMaxWidth().focusRequester(regNameFocusRequester).testTag("reg_name_input"),
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandGreenPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = regPhoneInput,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 9) {
                                            if (input.isEmpty() || input.startsWith("9") || input.startsWith("7")) {
                                                regPhoneInput = input
                                            }
                                        }
                                    },
                                    label = { Text("Phone Number") },
                                    prefix = { Text("+251 ", fontWeight = FontWeight.Bold, color = BrandGreenPrimary) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { regEmailFocusRequester.requestFocus() }
                                    ),
                                    placeholder = { Text("9xxxxxxxx") },
                                    modifier = Modifier.fillMaxWidth().focusRequester(regPhoneFocusRequester).testTag("reg_phone_input"),
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BrandGreenPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = regEmailInput,
                                    onValueChange = { regEmailInput = it },
                                    label = { Text("Email Address") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { regBirthDateFocusRequester.requestFocus() }
                                    ),
                                    modifier = Modifier.fillMaxWidth().focusRequester(regEmailFocusRequester).testTag("reg_email_input"),
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandGreenPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = regBirthDate,
                                    onValueChange = { input ->
                                        // Filter digits and format to yyyy-mm-dd automatically
                                        val clean = input.filter { it.isDigit() }
                                        if (clean.length <= 8) {
                                            var valid = true
                                            if (clean.length >= 6) {
                                                val mm = clean.substring(4, 6).toIntOrNull()
                                                if (mm == null || mm < 1 || mm > 12) valid = false
                                            } else if (clean.length == 5) {
                                                val m = clean.substring(4, 5).toIntOrNull()
                                                if (m != null && m > 1) valid = false
                                            }
                                            if (clean.length >= 8) {
                                                val dd = clean.substring(6, 8).toIntOrNull()
                                                if (dd == null || dd < 1 || dd > 31) valid = false
                                            } else if (clean.length == 7) {
                                                val d = clean.substring(6, 7).toIntOrNull()
                                                if (d != null && d > 3) valid = false
                                            }
                                            if (valid) {
                                                val formatted = buildString {
                                                    for (i in clean.indices) {
                                                        append(clean[i])
                                                        if ((i == 3 && clean.length > 4) || (i == 5 && clean.length > 6)) {
                                                            append("-")
                                                        }
                                                    }
                                                }
                                                regBirthDate = formatted
                                            }
                                        }
                                    },
                                    label = { Text("Birth Date (YYYY-MM-DD)") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { triggerStep1Next() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(regBirthDateFocusRequester)
                                        .testTag("reg_birth_date_input"),
                                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = BrandGreenPrimary) },
                                    singleLine = true,
                                    placeholder = { Text("YYYY-MM-DD") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = regFromReferCodeInput,
                                    onValueChange = { regFromReferCodeInput = it },
                                    label = { Text("Referral Code (Optional)") },
                                    placeholder = { Text("e.g. ES23U") },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { triggerStep1Next() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reg_refer_code_input"),
                                    leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = BrandGreenPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                if (loginErrorText != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = loginErrorText!!,
                                        color = BrandRedTertiary,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { triggerStep1Next() },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Next: Location", fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            2 -> {
                                // STEP 2: Location selections
                                Text(
                                    text = "Your Delivery Location",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = BrandGreenPrimary,
                                    modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "We use your regional and kebele location to filter nearby merchants and optimize delivery Bajaj/bikes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color(0xFF132A1F) else Color(0xFFF0F7F3)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, null, tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "Accurate address entries reduce shopping delivery friction. Make sure to specify the closest Kebele.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrandGreenPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = selectedRegion,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Region") },
                                            trailingIcon = { IconButton(onClick = { regionExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                            modifier = Modifier.fillMaxWidth().clickable { regionExpanded = true }
                                        )
                                        DropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                                            regions.forEach { r ->
                                                DropdownMenuItem(
                                                    text = { Text(r) },
                                                    onClick = {
                                                        selectedRegion = r
                                                        regionExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = selectedZone,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Zone/Sub-City") },
                                            trailingIcon = { IconButton(onClick = { zoneExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                            modifier = Modifier.fillMaxWidth().clickable { zoneExpanded = true }
                                        )
                                        DropdownMenu(expanded = zoneExpanded, onDismissRequest = { zoneExpanded = false }) {
                                            zones.forEach { z ->
                                                DropdownMenuItem(
                                                    text = { Text(z) },
                                                    onClick = {
                                                        selectedZone = z
                                                        zoneExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = selectedCity,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("City") },
                                            trailingIcon = { IconButton(onClick = { cityExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                            modifier = Modifier.fillMaxWidth().clickable { cityExpanded = true }
                                        )
                                        DropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                                            cities.forEach { c ->
                                                DropdownMenuItem(
                                                    text = { Text(c) },
                                                    onClick = {
                                                        selectedCity = c
                                                        cityExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = selectedKebele,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Kebele") },
                                            trailingIcon = { IconButton(onClick = { kebeleExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                                            modifier = Modifier.fillMaxWidth().clickable { kebeleExpanded = true }
                                        )
                                        DropdownMenu(expanded = kebeleExpanded, onDismissRequest = { kebeleExpanded = false }) {
                                            kebeles.forEach { k ->
                                                DropdownMenuItem(
                                                    text = { Text(k) },
                                                    onClick = {
                                                        selectedKebele = k
                                                        kebeleExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { registrationStep = 1 },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.5.dp, BrandGreenPrimary)
                                    ) {
                                        Text("Back", color = BrandGreenPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Button(
                                        onClick = { registrationStep = 3 },
                                        modifier = Modifier.weight(1.5f).height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Next: Security", fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // STEP 3: Security & ID Document Upload
                                Text(
                                    text = "Security & ID Verification",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = BrandGreenPrimary,
                                    modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "Establish a strong password and upload your identification documents.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                                )

                                val triggerRegisterAction = {
                                    val isPhoneValid = regPhoneInput.length == 9 && (regPhoneInput.startsWith("9") || regPhoneInput.startsWith("7"))
                                    if (regName.isBlank() || regPhoneInput.isBlank() || regEmailInput.isBlank() || regBirthDate.isBlank() || regPasswordInput.length != 6 || idDocInfront == null || idDocBackfront == null) {
                                        loginErrorText = "Please complete all fields, including the 6-number password and both ID uploads."
                                    } else if (!isPhoneValid) {
                                        loginErrorText = "Phone number must be exactly 9 digits and start with 9 or 7."
                                    } else {
                                        val frontFile = uriToFile(context, idDocInfront)
                                        val backFile = uriToFile(context, idDocBackfront)
                                        
                                        viewModel.registerAsBuyer(
                                            fullName = regName,
                                            phone = regPhoneInput,
                                            email = regEmailInput,
                                            password = regPasswordInput,
                                            region = selectedRegion,
                                            zone = selectedZone,
                                            city = selectedCity,
                                            kebele = selectedKebele,
                                            birthDate = regBirthDate,
                                            idDocumentFront = frontFile,
                                            idDocumentBack = backFile,
                                            fromReferCode = regFromReferCodeInput.ifBlank { null },
                                            onSuccess = {
                                                Toast.makeText(context, "Account created successfully! Real OTP code sent to $regEmailInput.", Toast.LENGTH_LONG).show()
                                                viewModel.navigateTo(AppScreen.OTP_VERIFICATION)
                                            },
                                            onError = { error ->
                                                loginErrorText = error
                                            }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = regPasswordInput,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 6) {
                                            regPasswordInput = input
                                        }
                                    },
                                    label = { Text("Required 6-Number Password") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { triggerRegisterAction() }
                                    ),
                                    placeholder = { Text("******") },
                                    modifier = Modifier.fillMaxWidth().testTag("reg_password_input"),
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandGreenPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // ✅ ID Document Upload (Front)
                                DocumentUploadWidget(
                                    title = "Upload ID Document Front (Required)",
                                    isUploaded = idDocInfront != null,
                                    onUploadSimulate = { launcherInfront.launch("image/*") },
                                    testTag = "upload_id_front_btn",
                                    fileName = getFileNameFromUri(context, idDocInfront)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // ✅ ID Document Upload (Back)
                                DocumentUploadWidget(
                                    title = "Upload ID Document Back (Required)",
                                    isUploaded = idDocBackfront != null,
                                    onUploadSimulate = { launcherBackfront.launch("image/*") },
                                    testTag = "upload_id_back_btn",
                                    fileName = getFileNameFromUri(context, idDocBackfront)
                                )

                                if (loginErrorText != null) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = BrandRedTertiary.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Error, null, tint = BrandRedTertiary, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = loginErrorText!!,
                                                color = BrandRedTertiary,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { registrationStep = 2 },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.5.dp, BrandGreenPrimary)
                                    ) {
                                        Text("Back", color = BrandGreenPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Button(
                                        onClick = { triggerRegisterAction() },
                                        modifier = Modifier.weight(1.5f).height(50.dp).testTag("register_submit_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Register", fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==========================================
// OTP SCREEN
// ==========================================

@Composable
fun OtpScreen(viewModel: AppViewModel) {
    val inputOtp by viewModel.inputOtp.collectAsStateWithLifecycle()
    val sentOtp by viewModel.sentOtp.collectAsStateWithLifecycle()
    val otpTimer by viewModel.otpTimer.collectAsStateWithLifecycle()
    val isOtpExpired by viewModel.isOtpExpired.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var otpError by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = loc("setup_2fa"),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "A 6-digit verification code has been sent to your email. Please enter it below to complete your login.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // OTP inputs display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0..5) {
                    val digit = inputOtp.getOrNull(i)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 2.dp,
                                color = if (otpError.isNotEmpty()) BrandRedTertiary
                                else if (inputOtp.length == i) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Expiry countdown / Resend trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                if (isOtpExpired) {
                    Text(
                        text = "OTP has expired. ",
                        color = BrandRedTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Resend OTP",
                        color = BrandGreenPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.clickable {
                            viewModel.resendOtp()
                            otpError = ""
                            android.widget.Toast.makeText(context, "A new OTP code has been sent to your email.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer Icon",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Resend available in ${otpTimer}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (otpError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = otpError,
                    color = BrandRedTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2FA Keypad
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Clear", "0", "Verify")
            )
            for (row in keys) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (key in row) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.8f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    otpError = ""
                                    when (key) {
                                        "Clear" -> {
                                            if (inputOtp.isNotEmpty()) {
                                                viewModel.setInputOtp(inputOtp.dropLast(1))
                                            }
                                        }
                                        "Verify" -> {
                                            if (isOtpExpired) {
                                                otpError = "This OTP has expired. Please resend."
                                            } else if (inputOtp.length == 6) {
                                                viewModel.verifyOtp { success ->
                                                    if (success) {
                                                        viewModel.navigateTo(AppScreen.MAIN)
                                                    } else {
                                                        otpError = "Incorrect verification OTP. Check the test code above."
                                                    }
                                                }
                                            } else {
                                                otpError = "Please enter a 6-digit OTP."
                                            }
                                        }
                                        else -> {
                                            if (inputOtp.length < 6) {
                                                viewModel.setInputOtp(inputOtp + key)
                                            }
                                        }
                                    }
                                }
                                .testTag("otp_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (key == "Verify") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
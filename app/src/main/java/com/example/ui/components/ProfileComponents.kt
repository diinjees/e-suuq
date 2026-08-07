package com.example.ui.components


import android.widget.Toast
import androidx.compose.ui.window.Dialog

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
fun ProfileMenuMain(
    viewModel: AppViewModel,
    activeUser: UserEntity?,
    onNavigateToSub: (String) -> Unit,
    onShowLogout: () -> Unit
) {
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.updateProfileImage(uri.toString())
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            val path = saveBitmapToCache(context, bitmap)
            if (path != null) {
                viewModel.updateProfileImage(path)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to take a profile picture.", Toast.LENGTH_SHORT).show()
        }
    }

    val checkAndLaunchCamera = {
        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Section: Displays profile picture, full name, email, and phone number
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dynamic Profile Picture with change trigger
                    ProfileImageOrAvatar(
                        profileImage = activeUser?.profileImage ?: "",
                        fullName = activeUser?.fullName ?: "Amina Farah",
                        size = 72.dp,
                        showEditBadge = true,
                        onClick = { showViewDialog = true }
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeUser?.fullName ?: "Amina Farah",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeUser?.email ?: "almaz.bekele@telecom.et",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (activeUser?.isEmailVerified == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Email",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = activeUser?.phone ?: "+251 911 234567",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        // Role Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandGreenPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = activeUser?.role ?: "BUYER",
                                color = BrandGreenPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }



        // Section Title: Account & Settings Info
        item {
            Text(
                text = "Preferences & Management",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Menu Items vertical list
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                // 1. My Order
                ProfileMenuItem(
                    icon = Icons.Default.ShoppingBag,
                    title = "My Order",
                    subtitle = "Complete order history & tracking status",
                    testTag = "menu_my_order",
                    onClick = { onNavigateToSub("MY_ORDERS") }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 2. Account
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = "Account",
                    subtitle = "Personal identity & role details",
                    testTag = "menu_account",
                    onClick = { onNavigateToSub("ACCOUNT") }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 3. Settings
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "Preferences, theme, language & lock security",
                    testTag = "menu_settings",
                    onClick = { onNavigateToSub("SETTINGS") }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 4. Refer & Earn
                ProfileMenuItem(
                    icon = Icons.Default.CardGiftcard,
                    title = "Refer & Earn",
                    subtitle = "Invite friends, earn points & referral cash",
                    testTag = "menu_refer_and_earn",
                    onClick = { onNavigateToSub("REFER_AND_EARN") }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 4. Help Center
                ProfileMenuItem(
                    icon = Icons.Default.SupportAgent,
                    title = "Help Center",
                    subtitle = "24/7 customer support, guides & resources",
                    testTag = "menu_help_center",
                    onClick = { viewModel.navigateTo(AppScreen.HELP_CENTER) }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 5. About
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Terms, conditions, licensing & privacy policy",
                    testTag = "menu_about",
                    onClick = { viewModel.navigateTo(AppScreen.ABOUT_SCREEN) }
                )
            }
        }

        // 6. Log Out Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onShowLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedTertiary.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Log Out")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Log Out", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog to view Profile Picture in high quality
    if (showViewDialog) {
        Dialog(onDismissRequest = { showViewDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View Profile Picture",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showViewDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Large Profile Picture preview
                    ProfileImageOrAvatar(
                        profileImage = activeUser?.profileImage ?: "",
                        fullName = activeUser?.fullName ?: "Amina Farah",
                        size = 200.dp,
                        showEditBadge = false
                    )

                    Text(
                        text = activeUser?.fullName ?: "Amina Farah",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showViewDialog = false
                                showAvatarDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, BrandGreenPrimary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = BrandGreenPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change", color = BrandGreenPrimary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showViewDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                        ) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Custom Avatar Selection Dialog
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Choose Profile Avatar", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Select a preset avatar or simulate a custom photo capture to update your identity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 2 rows of 3 columns each
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            listOf("preset_1" to "Default", "preset_2" to "Delivery", "preset_3" to "Merchant"),
                            listOf("preset_4" to "Support", "preset_5" to "Global", "preset_6" to "VIP")
                        ).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPresets.forEach { (presetId, label) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (activeUser?.profileImage == presetId) BrandGreenPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(
                                                width = if (activeUser?.profileImage == presetId) 2.dp else 1.dp,
                                                color = if (activeUser?.profileImage == presetId) BrandGreenPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.updateProfileImage(presetId)
                                            }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            ProfileImageOrAvatar(
                                                profileImage = presetId,
                                                fullName = activeUser?.fullName ?: "",
                                                size = 40.dp
                                            )
                                            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    
                    // Custom Camera Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                            .clickable {
                                checkAndLaunchCamera()
                                showAvatarDialog = false
                            }
                            .padding(12.dp)
                            .testTag("open_camera_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = BrandGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Open Camera", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = BrandGreenPrimary)
                            Text("Take a fresh portrait picture with your camera", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Real Upload Image File Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandGreenPrimary.copy(alpha = 0.12f))
                            .clickable {
                                galleryLauncher.launch("image/*")
                                showAvatarDialog = false
                            }
                            .padding(12.dp)
                            .testTag("upload_profile_image_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Upload Photo",
                            tint = BrandGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Upload Image File", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = BrandGreenPrimary)
                            Text("Choose a real photo or file from your device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAvatarDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                ) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("avatar_selection_dialog")
        )
    }
}


@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BrandGreenPrimary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.outline
        )
    }
}


@Composable
fun ProfileImageOrAvatar(
    profileImage: String,
    fullName: String,
    size: androidx.compose.ui.unit.Dp = 72.dp,
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val initials = fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase().ifEmpty { "AB" }

    // Define colors for each preset or generic fallback
    val (backgroundColor, icon) = when (profileImage) {
        "preset_1" -> Color(0xFF90A4AE) to Icons.Default.Person
        "preset_2" -> Color(0xFFFFB74D) to Icons.Default.LocalShipping
        "preset_3" -> Color(0xFF4DB6AC) to Icons.Default.Storefront
        "preset_4" -> Color(0xFF64B5F6) to Icons.Default.SupportAgent
        "preset_5" -> Color(0xFFBA68C8) to Icons.Default.Public
        "preset_6" -> Color(0xFFFFD54F) to Icons.Default.Stars
        else -> {
            // Stable background color based on name hash code
            val hash = fullName.hashCode()
            val colors = listOf(
                Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
                Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DB6AC),
                Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFFD54F), Color(0xFFFFB74D)
            )
            val colorIndex = Math.abs(hash % colors.size)
            colors[colorIndex] to null
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (profileImage.startsWith("preset_") && icon != null) {
            // Preset avatar with beautiful colored background & icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = fullName,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        } else if (profileImage.isNotBlank()) {
            // Remote image or file URI image using Coil's AsyncImage with caching enabled
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(profileImage)
                    .crossfade(true)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = fullName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(id = android.R.drawable.ic_menu_gallery), // default fallback if error
                fallback = painterResource(id = android.R.drawable.ic_menu_gallery)
            )
        } else {
            // Plain initials with beautifully colored background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = (size.value * 0.38f).sp
                    )
                )
            }
        }

        // Edit Badge (small camera or edit overlay if showEditBadge is true)
        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Edit Profile Picture",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.35f)
                )
            }
        }
    }
}

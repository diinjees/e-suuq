@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
fun ChatScreenComponent(viewModel: AppViewModel) {
    val activeRoom by viewModel.activeChatRoom.collectAsStateWithLifecycle()
    val chatMessages: List<ChatMessageEntity> by viewModel.chatMessagesFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val isAgentTyping by viewModel.isAgentTyping.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val fileName = "Photo_" + (System.currentTimeMillis() % 10000) + ".jpg"
            viewModel.sendMessage(fileName, it.toString(), "IMAGE")
        }
    }

    val videoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val fileName = "Video_" + (System.currentTimeMillis() % 10000) + ".mp4"
            viewModel.sendMessage(fileName, it.toString(), "VIDEO")
        }
    }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val fileName = "Doc_" + (System.currentTimeMillis() % 10000) + ".pdf"
            viewModel.sendMessage(fileName, it.toString(), "FILE")
        }
    }

    // Scroll to bottom of chat automatically
    LaunchedEffect(chatMessages.size, isAgentTyping, scrollState.maxValue) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    var showAttachmentMenu by remember { mutableStateOf(false) }

    // Chat Screen Master State: selectedChatRoomId determines if we are in list or conversation
    val selectedChatRoomId by viewModel.selectedChatRoomId.collectAsStateWithLifecycle()
    var chatFilter by remember { mutableStateOf("Stores") } // "Stores" or "Contacts"
    var searchQuery by remember { mutableStateOf("") }
    var messageToDelete by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var isDeleteModeActive by remember { mutableStateOf(false) }
    var selectedChatsToDelete by remember { mutableStateOf(emptySet<String>()) }
    var showConfirmDeleteChatsDialog by remember { mutableStateOf(false) }
    var replyingToMessage by remember(selectedChatRoomId) { mutableStateOf<ChatMessageEntity?>(null) }
    var deletedChatIds by remember { mutableStateOf(emptySet<String>()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(selectedChatRoomId) {
        showAttachmentMenu = false
    }

    // Dynamic Live Messages for List View to ensure real-time sync with database
    val sellerMessages by viewModel.getChatMessages("buyer_seller").collectAsStateWithLifecycle(initialValue = emptyList())
    val deliveryMessages by viewModel.getChatMessages("buyer_delivery").collectAsStateWithLifecycle(initialValue = emptyList())
    val supportMessages by viewModel.getChatMessages("support").collectAsStateWithLifecycle(initialValue = emptyList())
    val securityMessages by viewModel.getChatMessages("esuug_security").collectAsStateWithLifecycle(initialValue = emptyList())

    val lastSellerMessage = sellerMessages.lastOrNull()?.messageText ?: "Ok, See you in the market..."
    val lastSellerTime = sellerMessages.lastOrNull()?.let {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(it.timestamp))
    } ?: "Yesterday"

    val lastDeliveryMessage = deliveryMessages.lastOrNull()?.let {
        "${if (it.senderRole == "BUYER") "You" else "Guled"}: ${it.messageText}"
    } ?: "Guled: Have a good day, Amina!"
    val lastDeliveryTime = deliveryMessages.lastOrNull()?.let {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(it.timestamp))
    } ?: "7:32 AM"

    val lastSupportMessage = supportMessages.lastOrNull()?.messageText ?: "Welcome to E-Suuq Support. How can we help you?"
    val lastSupportTime = supportMessages.lastOrNull()?.let {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(it.timestamp))
    } ?: "Last week"

    val lastSecurityMessage = securityMessages.lastOrNull()?.messageText ?: "Your transactions are protected by E-Suuq Escrow."
    val lastSecurityTime = securityMessages.lastOrNull()?.let {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(it.timestamp))
    } ?: "10:15 AM"

    val newsMessages by viewModel.getChatMessages("esuug_news").collectAsStateWithLifecycle(initialValue = emptyList())
    val lastNewsMessage = newsMessages.lastOrNull()?.messageText ?: "Welcome to E-Suuq News channel!"
    val lastNewsTime = newsMessages.lastOrNull()?.let {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(it.timestamp))
    } ?: "12:00 PM"

    // Master Chat list matching the desired layout and picture
    data class ChatListItem(
        val id: String,
        val name: String,
        val avatarInitial: String = "",
        val defaultLastMessage: String,
        val defaultTime: String,
        val isGroup: Boolean = false,
        val hasPin: Boolean = false,
        val hasStar: Boolean = false,
        val isMuted: Boolean = false,
        val isStore: Boolean = false
    )

    val initialChats = remember(lastDeliveryMessage, lastDeliveryTime, lastSellerMessage, lastSellerTime, lastSupportMessage, lastSupportTime, lastNewsMessage, lastNewsTime, lastSecurityMessage, lastSecurityTime) {
        listOf(
            ChatListItem(
                id = "esuug_news",
                name = "E-Suuq News",
                avatarInitial = "EN",
                defaultLastMessage = lastNewsMessage,
                defaultTime = lastNewsTime,
                isGroup = false,
                hasPin = true,
                isStore = false
            ),
            ChatListItem(
                id = "esuug_security",
                name = "E-Suuq Security",
                avatarInitial = "ES",
                defaultLastMessage = lastSecurityMessage,
                defaultTime = lastSecurityTime,
                isGroup = false,
                hasPin = true,
                isStore = false
            ),
            ChatListItem(
                id = "buyer_delivery",
                name = "Garowe Delivery Team",
                avatarInitial = "GD",
                defaultLastMessage = lastDeliveryMessage,
                defaultTime = lastDeliveryTime,
                isGroup = true,
                hasPin = true,
                isStore = false
            ),
            ChatListItem(
                id = "mumina_suleiman",
                name = "Mumina Suleiman",
                avatarInitial = "MS",
                defaultLastMessage = "I look forward to hearing from you and requesting access to the...",
                defaultTime = "10/22/24",
                hasStar = true,
                isStore = false
            ),
            ChatListItem(
                id = "idil_rooble",
                name = "Idil Rooble",
                avatarInitial = "IR",
                defaultLastMessage = "You reacted 😄 to 'There was something in the air today 😇'",
                defaultTime = "12:40 PM",
                isStore = false
            ),
            ChatListItem(
                id = "buyer_seller",
                name = "Muqdisho Traders",
                avatarInitial = "MT",
                defaultLastMessage = "You: $lastSellerMessage",
                defaultTime = lastSellerTime,
                isGroup = true,
                isStore = true
            ),
            ChatListItem(
                id = "chaka_coffee_chat",
                name = "Chaka Coffee",
                avatarInitial = "CC",
                defaultLastMessage = "Welcome to Chaka Coffee! Tap to view our Sidamo Arabica specialty beans.",
                defaultTime = "Wednesday",
                isStore = true
            ),
            ChatListItem(
                id = "anbessa_shoe_chat",
                name = "Anbessa Shoe",
                avatarInitial = "AS",
                defaultLastMessage = "Check out our premium leather footwear collection.",
                defaultTime = "Monday",
                isStore = true
            ),
            ChatListItem(
                id = "shukri_daahir",
                name = "Shukri Daahir",
                avatarInitial = "SD",
                defaultLastMessage = "👈 no problem, we will settle it",
                defaultTime = "Saturday",
                isStore = false
            ),
            ChatListItem(
                id = "amina_farah",
                name = "Amina Farah",
                avatarInitial = "AF",
                defaultLastMessage = "What's man?!",
                defaultTime = "Saturday",
                isStore = false
            ),
            ChatListItem(
                id = "nasteexo_cilmi",
                name = "Nasteexo Cilmi",
                avatarInitial = "NC",
                defaultLastMessage = "Have a good day, Amina! 🥳",
                defaultTime = "10/22/24",
                isStore = false
            ),
            ChatListItem(
                id = "Aswaan_yusuf",
                name = "Aswaan Yusuf",
                avatarInitial = "AY",
                defaultLastMessage = "Have a good day, Amina!",
                defaultTime = "10/21/24",
                isStore = false
            ),
            ChatListItem(
                id = "support",
                name = "E-Suuq Support Care",
                avatarInitial = "SC",
                defaultLastMessage = lastSupportMessage,
                defaultTime = lastSupportTime,
                isStore = false
            )
        )
    }

    val dynamicChatsList = remember { androidx.compose.runtime.mutableStateListOf<ChatListItem>() }
    LaunchedEffect(initialChats) {
        if (dynamicChatsList.isEmpty()) {
            dynamicChatsList.addAll(initialChats)
        }
    }

    if (selectedChatRoomId == null) {
        // ==========================================
        // 1. BEAUTIFUL REDESIGNED CHAT LIST VIEW
        // ==========================================
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isDeleteModeActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        isDeleteModeActive = false
                                        selectedChatsToDelete = emptySet()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel Delete Mode", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text(
                                        text = "Select to Delete (${selectedChatsToDelete.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (selectedChatsToDelete.isNotEmpty()) {
                                            showConfirmDeleteChatsDialog = true
                                        }
                                    },
                                    enabled = selectedChatsToDelete.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Confirm Delete",
                                        tint = if (selectedChatsToDelete.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "E-Suuq Messages",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Search Bar Capsule
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search Chats",style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            val pinnedChatRoomIds by viewModel.pinnedChatRoomIds.collectAsStateWithLifecycle()
            val unreadChatRoomIds by viewModel.unreadChatRoomIds.collectAsStateWithLifecycle()

            // Filter list dynamically based on search query, and pins (showing all active/populated chats)
            val filteredChats = remember(dynamicChatsList.size, searchQuery, deletedChatIds, pinnedChatRoomIds, lastSellerMessage, lastDeliveryMessage, lastSupportMessage, lastNewsMessage, lastSecurityMessage, unreadChatRoomIds) {
                dynamicChatsList.map { chat ->
                    val lastMsg = when (chat.id) {
                        "buyer_seller" -> lastSellerMessage
                        "buyer_delivery" -> lastDeliveryMessage
                        "support" -> lastSupportMessage
                        "esuug_news" -> lastNewsMessage
                        "esuug_security" -> lastSecurityMessage
                        else -> chat.defaultLastMessage
                    }
                    val lastTime = when (chat.id) {
                        "buyer_seller" -> lastSellerTime
                        "buyer_delivery" -> lastDeliveryTime
                        "support" -> lastSupportTime
                        "esuug_news" -> lastNewsTime
                        "esuug_security" -> lastSecurityTime
                        else -> chat.defaultTime
                    }
                    chat.copy(
                        hasPin = pinnedChatRoomIds.contains(chat.id),
                        defaultLastMessage = lastMsg,
                        defaultTime = lastTime
                    )
                }.filter { chat ->
                    if (chat.id in deletedChatIds) return@filter false
                    val matchesSearch = chat.name.contains(searchQuery, ignoreCase = true) ||
                            chat.defaultLastMessage.contains(searchQuery, ignoreCase = true)
                    matchesSearch
                }.sortedWith(
                    compareByDescending<ChatListItem> { it.hasPin }
                    .thenByDescending { unreadChatRoomIds.contains(it.id) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (filteredChats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No conversations found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(filteredChats, key = { it.id }) { chat ->
                        val avatarBgColor = remember(chat.name) {
                            val colors = listOf(
                                Color(0xFFE3F2FD), // soft blue
                                Color(0xFFE8F5E9), // soft green
                                Color(0xFFFFF3E0), // soft orange
                                Color(0xFFFCE4EC), // soft pink
                                Color(0xFFF3E5F5), // soft purple
                                Color(0xFFFFFDE7), // soft yellow
                                Color(0xFFE0F7FA)  // soft cyan
                            )
                            colors[chat.name.hashCode().coerceAtLeast(0) % colors.size]
                        }
                        val avatarTextColor = remember(chat.name) {
                            val colors = listOf(
                                Color(0xFF1565C0),
                                Color(0xFF2E7D32),
                                Color(0xFFE65100),
                                Color(0xFFC2185B),
                                Color(0xFF7B1FA2),
                                Color(0xFFF57F17),
                                Color(0xFF006064)
                            )
                            colors[chat.name.hashCode().coerceAtLeast(0) % colors.size]
                        }

                        val isSelected = selectedChatsToDelete.contains(chat.id)
                        val isUnread = unreadChatRoomIds.contains(chat.id)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background)
                                .combinedClickable(
                                    onClick = {
                                        if (isDeleteModeActive) {
                                            if (chat.id == "esuug_news" || chat.id == "esuug_security") {
                                                android.widget.Toast.makeText(context, "${chat.name} cannot be deleted", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                selectedChatsToDelete = if (isSelected) {
                                                    selectedChatsToDelete - chat.id
                                                } else {
                                                    selectedChatsToDelete + chat.id
                                                }
                                            }
                                        } else {
                                            viewModel.selectChatRoomId(chat.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isDeleteModeActive) {
                                            if (chat.id == "esuug_news" || chat.id == "esuug_security") {
                                                android.widget.Toast.makeText(context, "${chat.name} cannot be deleted", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                isDeleteModeActive = true
                                                selectedChatsToDelete = setOf(chat.id)
                                            }
                                        }
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isDeleteModeActive) {
                                    if (chat.id == "esuug_news" || chat.id == "esuug_security") {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Cannot delete channel",
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedChatsToDelete = if (checked == true) {
                                                    selectedChatsToDelete + chat.id
                                                } else {
                                                    selectedChatsToDelete - chat.id
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }

                                // Avatar Circle
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(avatarBgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (chat.isGroup) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = "Group",
                                            tint = avatarTextColor,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    } else {
                                        Text(
                                            text = chat.avatarInitial,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = avatarTextColor
                                        )
                                    }
                                }

                                // Text contents
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = chat.name,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isUnread) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "NEW",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = chat.defaultTime,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (chat.hasPin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chat.defaultLastMessage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (chat.hasPin) {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = "Pinned",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            if (chat.hasStar) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Starred",
                                                    tint = BrandGoldSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 66.dp)
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }

            if (showAddContactDialog) {
                var newContactName by remember { mutableStateOf("") }
                var newContactMsg by remember { mutableStateOf("") }
                var inputError by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showAddContactDialog = false },
                    title = { Text("Add New Contact Conversation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Enter contact details from your phone to start a new chat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            
                            OutlinedTextField(
                                value = newContactName,
                                onValueChange = { 
                                    newContactName = it
                                    inputError = false
                                },
                                label = { Text("Contact Name") },
                                isError = inputError,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = newContactMsg,
                                onValueChange = { newContactMsg = it },
                                label = { Text("Initial Message") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newContactName.isNotBlank()) {
                                    val initials = newContactName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(2).uppercase()
                                    val newId = "custom_contact_" + System.currentTimeMillis()
                                    val newChat = ChatListItem(
                                        id = newId,
                                        name = newContactName,
                                        avatarInitial = if (initials.isNotEmpty()) initials else "C",
                                        defaultLastMessage = if (newContactMsg.isNotBlank()) newContactMsg else "Say hello!",
                                        defaultTime = "Just now",
                                        isStore = false
                                    )
                                    dynamicChatsList.add(0, newChat)
                                    showAddContactDialog = false
                                } else {
                                    inputError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                        ) {
                            Text("Add Contact")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddContactDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    } else {
        // ==========================================
        // 2. MODERN ACTIVE CONVERSATION SCREEN VIEW
        // ==========================================
        val activeChatName = remember(selectedChatRoomId, dynamicChatsList.size) {
            dynamicChatsList.firstOrNull { it.id == selectedChatRoomId }?.name ?: "Chat Room"
        }
        val activeChatInitials = remember(selectedChatRoomId, dynamicChatsList.size) {
            dynamicChatsList.firstOrNull { it.id == selectedChatRoomId }?.avatarInitial ?: "CR"
        }
        val isGroupChat = remember(selectedChatRoomId, dynamicChatsList.size) {
            dynamicChatsList.firstOrNull { it.id == selectedChatRoomId }?.isGroup ?: false
        }
        val activeChatIsStore = remember(selectedChatRoomId, dynamicChatsList.size) {
            dynamicChatsList.firstOrNull { it.id == selectedChatRoomId }?.isStore ?: false
        }

        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.selectChatRoomId(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Mini Avatar
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .then(
                                    if (activeChatIsStore) {
                                        Modifier.clickable {
                                            viewModel.setSelectedStoreName(activeChatName)
                                        }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGroupChat) {
                                Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = activeChatInitials,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (activeChatIsStore) {
                                        Modifier.clickable {
                                            viewModel.setSelectedStoreName(activeChatName)
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Text(
                                text = activeChatName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenPrimary)
                                )
                                Text(
                                    text = "Online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandGreenPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        val pinnedChatRoomIds by viewModel.pinnedChatRoomIds.collectAsStateWithLifecycle()
                        val isPinned = selectedChatRoomId?.let { pinnedChatRoomIds.contains(it) } ?: false

                        IconButton(onClick = {
                            selectedChatRoomId?.let { viewModel.togglePinChatRoom(it) }
                        }) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (isPinned) "Unpin Chat" else "Pin Chat",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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
                    .imePadding()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Conversation message stream
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No messages yet. Send a message to start bargaining!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        val visibleMessages = chatMessages.filter { !it.isDeleted }
                        if (visibleMessages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No messages yet. Send a message to start bargaining!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            var selectedMsgIdActions by remember { mutableStateOf<String?>(null) }
                            visibleMessages.forEach { msg ->
                                val isMe = msg.senderRole == "BUYER"
                                
                                var swipeReplyOffset by remember { mutableStateOf(0f) }
                                val animatedSwipeOffset by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = swipeReplyOffset,
                                    label = "MessageSwipe"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .offset { androidx.compose.ui.unit.IntOffset(animatedSwipeOffset.toInt(), 0) }
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    if (swipeReplyOffset > 100f) {
                                                        replyingToMessage = msg
                                                    }
                                                    swipeReplyOffset = 0f
                                                },
                                                onDragCancel = {
                                                    swipeReplyOffset = 0f
                                                },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val newOffset = swipeReplyOffset + dragAmount
                                                    swipeReplyOffset = newOffset.coerceIn(0f, 180f)
                                                }
                                            )
                                        },
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (animatedSwipeOffset > 10f) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .alpha((animatedSwipeOffset / 100f).coerceIn(0f, 1f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Reply,
                                                contentDescription = "Reply",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isMe) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 0.dp,
                                                bottomEnd = if (isMe) 0.dp else 16.dp
                                            ),
                                            modifier = Modifier
                                                .widthIn(max = 280.dp)
                                                .combinedClickable(
                                                    onClick = {
                                                        if (selectedMsgIdActions == msg.id) {
                                                            selectedMsgIdActions = null
                                                        }
                                                    },
                                                    onLongClick = {
                                                        selectedMsgIdActions = if (selectedMsgIdActions == msg.id) null else msg.id
                                                    }
                                                )
                                        ) {
                                            Column {
                                                if (!isMe) {
                                                    Text(
                                                        text = msg.senderName,
                                                        color = BrandGoldSecondary,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)
                                                    )
                                                }
                                                
                                                if (msg.replyToMessageText != null) {
                                                    Surface(
                                                        color = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(3.dp)
                                                                    .height(24.dp)
                                                                    .background(if (isMe) Color.White else MaterialTheme.colorScheme.primary)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column {
                                                                Text(
                                                                    text = "Reply",
                                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                                    fontSize = 10.sp,
                                                                    color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                                                )
                                                                Text(
                                                                    text = msg.replyToMessageText,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontSize = 12.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    color = (if (isMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.7f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                ChatBubbleContent(msg = msg, isMe = isMe)
                                                
                                                // Footer for timestamp, ticks and like status
                                                Row(
                                                    modifier = Modifier
                                                        .padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 6.dp)
                                                        .align(Alignment.End),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Liked Heart Indicator
                                                    if (msg.isLiked) {
                                                        Icon(
                                                            imageVector = Icons.Default.Favorite,
                                                            contentDescription = "Liked",
                                                            tint = Color(0xFFE91E63),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    
                                                    // Time
                                                    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                                                    val timeStr = sdf.format(java.util.Date(msg.timestamp))
                                                    Text(
                                                        text = timeStr,
                                                        fontSize = 9.sp,
                                                        color = (if (isMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.6f)
                                                    )
                                                    
                                                    // Ticks for messages sent by BUYER (isMe)
                                                    if (isMe) {
                                                        if (msg.status == "SENT") {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Sent",
                                                                tint = Color.White.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                        } else { // READ (twotik)
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Read",
                                                                    tint = Color(0xFF81C784), // Light green for read ticks
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Read",
                                                                    tint = Color(0xFF81C784),
                                                                    modifier = Modifier.size(11.dp).offset(x = (-5).dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (msg.reactionEmoji != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(y = 8.dp, x = if (isMe) (-12).dp else 12.dp)
                                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = msg.reactionEmoji, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                        
                                        // Action buttons (Like, Delete) below the card if clicked
                                        if (selectedMsgIdActions == msg.id) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(top = 4.dp, bottom = 8.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Like Action
                                                IconButton(
                                                    onClick = {
                                                        viewModel.toggleMessageLiked(msg.id, msg.isLiked)
                                                        selectedMsgIdActions = null
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (msg.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "Like Message",
                                                        tint = if (msg.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // Emojis: Sad, Happy, Angry
                                                listOf("😢", "😄", "😡").forEach { emoji ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .clickable {
                                                                viewModel.updateMessageReaction(msg.id, if (msg.reactionEmoji == emoji) null else emoji)
                                                                selectedMsgIdActions = null
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = emoji, fontSize = 16.sp)
                                                    }
                                                }

                                                // Delete Action
                                                IconButton(
                                                    onClick = {
                                                        messageToDelete = msg
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Message",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isAgentTyping) {
                        val typingName = when (selectedChatRoomId) {
                            "buyer_seller" -> "Muqdisho Traders"
                            "buyer_delivery" -> "Garowe Delivery Team"
                            else -> "Merchant Agent"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 16.dp
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$typingName is typing",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                                }
                            }
                        }
                    }
                }

                // Attachments row drawer
                if (showAttachmentMenu) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Send Image
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        imagePickerLauncher.launch("image/*")
                                        showAttachmentMenu = false
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = "Send Image", tint = Color(0xFF2E7D32))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Photo", style = MaterialTheme.typography.labelSmall)
                            }

                            // Send Video
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        videoPickerLauncher.launch("video/*")
                                        showAttachmentMenu = false
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE3F2FD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = "Send Video", tint = Color(0xFF1565C0))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Video", style = MaterialTheme.typography.labelSmall)
                            }

                            // Send File/PDF
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        filePickerLauncher.launch("*/*")
                                        showAttachmentMenu = false
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFF3E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = "Send PDF", tint = Color(0xFFE65100))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Document", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Redesigned Chat Input message typing Row (Pill design)
                if (selectedChatRoomId == "esuug_news") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "This is a read-only channel for E-Suuq official updates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(
                            onClick = { showAttachmentMenu = !showAttachmentMenu },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .testTag("chat_attach_btn")
                        ) {
                            Icon(
                                imageVector = if (showAttachmentMenu) Icons.Default.Close else Icons.Default.AddCircleOutline,
                                contentDescription = "Attach Options",
                                tint = BrandGreenPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            if (replyingToMessage != null) {
                                val replyMsg = replyingToMessage!!
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(28.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Replying to ${replyMsg.senderName}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = replyMsg.messageText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { replyingToMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel Reply",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Divider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            }

                            TextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_input_field"),
                                placeholder = { Text("Type a message...", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))) },
                                singleLine = true,
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                        Text(
                                            "GIF",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .clickable {
                                                    textInput = "[GIF Sent]"
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                android.widget.Toast.makeText(context, "Microphone integration (mock)", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Mic,
                                                contentDescription = "Voice Message",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))
                                        IconButton(
                                            onClick = {
                                                viewModel.sendMessage("Photo_Receipt.jpg", "mock_image_uri", "IMAGE")
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PhotoCamera,
                                                contentDescription = "Take Photo",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(
                                        messageText = textInput,
                                        replyToId = replyingToMessage?.id,
                                        replyToText = replyingToMessage?.messageText
                                    )
                                    textInput = ""
                                    replyingToMessage = null
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .testTag("chat_send_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (messageToDelete != null) {
        val msg = messageToDelete!!
        val isMe = msg.senderRole == "BUYER"
        val isSeen = msg.status == "READ"

        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Delete Message", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = {
                if (!isMe) {
                    Text("Do you want to delete this message?")
                } else if (isSeen) {
                    Text("This message was already seen by the other user. Do you want to delete it for you or other and you?")
                } else {
                    Text("Do you want to delete this message for everyone or just for you?")
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isMe) {
                        Button(
                            onClick = {
                                viewModel.deleteMessage(msg.id)
                                messageToDelete = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete for Everyone")
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.deleteMessage(msg.id)
                            messageToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Delete for me")
                    }
                    OutlinedButton(
                        onClick = { messageToDelete = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showConfirmDeleteChatsDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteChatsDialog = false },
            title = { Text("Delete Conversations", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = { Text("Are you sure you want to delete the selected (${selectedChatsToDelete.size}) conversations? This action cannot be undone.") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // Clear selected chats
                            deletedChatIds = deletedChatIds + selectedChatsToDelete
                            isDeleteModeActive = false
                            selectedChatsToDelete = emptySet()
                            showConfirmDeleteChatsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete selected")
                    }
                    OutlinedButton(
                        onClick = { showConfirmDeleteChatsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}



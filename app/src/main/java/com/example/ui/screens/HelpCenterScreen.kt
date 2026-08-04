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
fun CustomerFeedbackScreen(
    onBack: () -> Unit,
    reviews: List<ReviewItem>,
    onReviewsUpdate: (List<ReviewItem>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var feedbackRatingFilter by remember { mutableStateOf("All") }
    var feedbackSearchQuery by remember { mutableStateOf("") }
    
    // Reply state map to keep track of inline draft texts for each buyer name
    var replyDrafts by remember { mutableStateOf(mapOf<String, String>()) }
    var activeReplyTarget by remember { mutableStateOf<String?>(null) } // Name of buyer currently being replied to

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .testTag("feedback_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Customer Reviews & Ratings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "View ratings and manage customer service responses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reputation Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(
                            text = "4.9",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(5) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = BrandGoldSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "32 Total Ratings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BrandGreenPrimary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = "TOP RATED STORE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandGreenPrimary),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(1.dp).height(100.dp).background(MaterialTheme.colorScheme.outlineVariant))

                    Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "5 Star" to 0.92f,
                            "4 Star" to 0.06f,
                            "3 Star" to 0.02f,
                            "2 Star" to 0.0f,
                            "1 Star" to 0.0f
                        ).forEach { (label, pct) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.width(42.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                                    color = BrandGoldSecondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "${(pct * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.width(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Filter Bar & Search Input
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = feedbackSearchQuery,
                    onValueChange = { feedbackSearchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("feedback_search_input"),
                    placeholder = { Text("Search by customer..",style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (feedbackSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { feedbackSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filterOptions = listOf("All", "5 ★", "4 ★", "3 ★", "Critical (<= 3★)")
                    items(filterOptions) { filter ->
                        val isSelected = feedbackRatingFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { feedbackRatingFilter = filter },
                            label = { Text(filter) },
                            modifier = Modifier.testTag("feedback_filter_$filter")
                        )
                    }
                }
            }

            // Feedback List Content
            val filteredReviews = reviews.filter { item ->
                // Filter rating
                val ratingValue = item.rating.removePrefix("★").toDoubleOrNull() ?: 5.0
                val matchesRating = when (feedbackRatingFilter) {
                    "5 ★" -> ratingValue >= 5.0
                    "4 ★" -> ratingValue >= 4.0 && ratingValue < 5.0
                    "3 ★" -> ratingValue >= 3.0 && ratingValue < 4.0
                    "Critical (<= 3★)" -> ratingValue <= 3.0
                    else -> true
                }
                
                val matchesSearch = feedbackSearchQuery.isEmpty() ||
                        item.name.contains(feedbackSearchQuery, ignoreCase = true) ||
                        item.comment.contains(feedbackSearchQuery, ignoreCase = true) ||
                        item.product.contains(feedbackSearchQuery, ignoreCase = true)

                matchesRating && matchesSearch
            }

            if (filteredReviews.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching reviews found", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text("Try adjusting your rating filter or search term.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                filteredReviews.forEach { item ->
                    val avatarLetter = item.name.firstOrNull()?.toString() ?: "?"
                    val themeColor = remember(item.name) {
                        val hash = item.name.hashCode()
                        listOf(BrandGreenPrimary, BrandGoldSecondary, Color(0xFF1E3A8A), Color(0xFF6B21A8), Color(0xFF0369A1))[kotlin.math.abs(hash) % 5]
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("review_card_${item.name.replace(" ", "_")}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(themeColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = avatarLetter,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = themeColor
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = item.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BrandGoldSecondary.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = BrandGoldSecondary, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = item.rating.removePrefix("★"),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = BrandGoldSecondary)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Purchased: ${item.product}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = item.comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Response Box (existing reply)
                            if (item.replyText != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Store, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Your Response",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = BrandGreenPrimary
                                            )
                                        }
                                        Text(
                                            text = "Sent",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.replyText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                // No reply yet, show Reply draft trigger
                                if (activeReplyTarget != item.name) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { 
                                                activeReplyTarget = item.name 
                                                replyDrafts = replyDrafts + (item.name to "")
                                            },
                                            modifier = Modifier.testTag("reply_trigger_${item.name.replace(" ", "_")}"),
                                            colors = ButtonDefaults.textButtonColors(contentColor = BrandGreenPrimary)
                                        ) {
                                            Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reply to Buyer", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                } else {
                                    // Display the interactive text input to reply
                                    val draftText = replyDrafts[item.name] ?: ""
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Write a reply to ${item.name}:",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        OutlinedTextField(
                                            value = draftText,
                                            onValueChange = { newText ->
                                                replyDrafts = replyDrafts + (item.name to newText)
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("reply_input_${item.name.replace(" ", "_")}"),
                                            placeholder = { Text("e.g. Thank you for your review! We are delighted that you enjoyed...") },
                                            maxLines = 4,
                                            singleLine = false,
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BrandGreenPrimary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End)
                                        ) {
                                            TextButton(
                                                onClick = { activeReplyTarget = null }
                                            ) {
                                                Text("Cancel", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Button(
                                                onClick = {
                                                    if (draftText.isNotBlank()) {
                                                        onReviewsUpdate(reviews.map { r ->
                                                            if (r.name == item.name) {
                                                                r.copy(replyText = draftText)
                                                            } else r
                                                        })
                                                        activeReplyTarget = null
                                                        android.widget.Toast.makeText(context, "Reply submitted successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Please enter your reply text", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.testTag("reply_submit_${item.name.replace(" ", "_")}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Submit Response", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}


@Composable
fun HelpCenterScreen(viewModel: AppViewModel) {
    var activeHelpTab by remember { mutableStateOf("TOPICS") } // Default is Help Topics
    var searchQuery by remember { mutableStateOf("") }
    var expandedGuideTitles by remember { mutableStateOf(emptySet<String>()) }
    var textInput by remember { mutableStateOf("") }
    var showSupportAttachmentMenu by remember { mutableStateOf(false) }

    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val messages by viewModel.getChatMessages("support").collectAsStateWithLifecycle(initialValue = emptyList())
    val isAgentTyping by viewModel.isAgentTyping.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val helpGuides = remember {
        listOf(
            HelpGuide(
                title = "How to change my password",
                content = "To change your passcode:\n\n1. Go to Profile Menu -> Settings.\n2. Tap 'Change Security Passcode'.\n3. Enter your current 4-digit PIN, then enter your new 4-digit PIN and confirm.\n4. Tap 'Update Passcode' to save your new PIN.",
                tags = listOf("password", "passcode", "pin", "security")
            ),
            HelpGuide(
                title = "How to become a seller",
                content = "To become an approved seller on E-Suuq:\n\n1. Register an account and choose 'Seller' as your role.\n2. Upload your Trade License, National ID, and TIN number.\n3. The E-Suuq admin team will review your credentials within 24 hours.\n4. Once approved, you can access 'My Store' to upload products.",
                tags = listOf("seller", "merchant", "business", "onboarding", "license")
            ),
            HelpGuide(
                title = "How to track my order",
                content = "Tracking active orders is simple:\n\n1. Open Profile -> My Order.\n2. In the 'Processing' tab, find the active order you wish to trace.\n3. Tap the 'Track' button.\n4. A live delivery route visualization map will open, showing real-time courier movement.",
                tags = listOf("track", "order", "delivery", "map", "courier")
            ),
            HelpGuide(
                title = "Return and refund policy",
                content = "Our 100% Secure Buyer Protection Policy covers you:\n\n1. You can request a refund if the item is not as described or has damage.\n2. Contact support via Live Chat or raise a dispute within 48 hours of delivery.\n3. Refunds are credited instantly via CBE Birr or Telebirr upon product return confirmation.",
                tags = listOf("return", "refund", "policy", "payment", "dispute")
            ),
            HelpGuide(
                title = "Supported Payment Methods",
                content = "E-Suuq supports the most secure payment methods in Ethiopia:\n\n1. Telebirr: Fast mobile wallet payments with zero transaction fees.\n2. CBE Birr: Direct bank transfer from Commercial Bank of Ethiopia accounts.\n3. Cash on Delivery (COD): Pay our verified couriers with cash upon receiving your order safely.",
                tags = listOf("payment", "telebirr", "cbe", "cbe birr", "cash", "cod", "price")
            ),
            HelpGuide(
                title = "How to become a Delivery Partner",
                content = "To join our elite logistics network as a Delivery Partner:\n\n1. Go to Profile Menu -> Account details -> Grow with E-Suuq.\n2. Tap 'Become a Courier' and fill out your transport type (Motorcycle, Bicycle, etc.) and Plate Number.\n3. Upload your Driver's License and Vehicle Registration certificate.\n4. Admin will verify your documents within 24 hours. Once verified, you will immediately unlock the Courier Dashboard!",
                tags = listOf("delivery", "courier", "driver", "earn", "jobs")
            ),
            HelpGuide(
                title = "E-Suuq Buyer Protection Guarantee",
                content = "We prioritize buyer trust and secure commerce:\n\n1. Secure Hold: Payments are held safely until you confirm receipt of your ordered products.\n2. Discrepancy Dispute: If you receive the incorrect size, color, or condition, notify us immediately via Live Chat.\n3. Verified Stores: All E-Suuq sellers have undergone rigorous background verification, including valid trade license checks.",
                tags = listOf("protection", "dispute", "trust", "safety", "guarantee")
            ),
            HelpGuide(
                title = "Delivery Locations & Coverage",
                content = "We deliver across major sub-cities and commercial districts in Addis Ababa, including:\n\n1. Bole, Kirkos, Lideta, Yeka, and Arada sub-cities.\n2. We are currently expanding services to other major urban centers across Ethiopia (Adama, Hawassa, Bahir Dar) very soon!\n3. Delivery charges are calculated dynamically based on distance to ensure fair rates for our riders.",
                tags = listOf("deliver", "locations", "addis ababa", "ethiopia", "coverage")
            )
        )
    }

    val filteredGuides = remember(searchQuery) {
        helpGuides.filter { guide ->
            searchQuery.isBlank() ||
                guide.title.contains(searchQuery, ignoreCase = true) ||
                guide.tags.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(messages.size, activeHelpTab, scrollState.maxValue) {
        if (activeHelpTab == "CHAT") {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (activeHelpTab == "CHAT") {
                            activeHelpTab = "TOPICS"
                        } else {
                            viewModel.navigateTo(AppScreen.MAIN)
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .testTag("help_center_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeHelpTab == "CHAT") "Live Chat Support" else "Help Center",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (activeHelpTab == "CHAT") "24/7 Live Support Chat Specialist" else "Central hub for help topics & guides",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Direct Live Chat Access Icon (Headset) on right appbar
                if (activeHelpTab != "CHAT") {
                    IconButton(
                        onClick = { activeHelpTab = "CHAT" },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .testTag("direct_live_chat_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = "Direct Live Chat Support Access",
                            tint = BrandGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeHelpTab == "CHAT") {
                // REAL-TIME SUPPORT CHAT INTERFACE
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat header with Support Agent details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Agent Avatar with status dot
                        Box(modifier = Modifier.size(48.dp)) {
                            // Circle avatar with initials
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(BrandGreenPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "MS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenPrimary
                                    )
                                )
                            }
                            // Green online status dot
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32)) // Green
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chatting with: Meron S.",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Online Support Specialist",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Live agent status banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandGreenPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(BrandGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Customer Support Agent (Meron S.) is online.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandGreenPrimary
                        )
                    }

                    // Chat messages list
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        messages.forEach { msg ->
                            val isMe = msg.senderRole == "BUYER"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) BrandGreenPrimary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isMe) 12.dp else 2.dp,
                                        bottomEnd = if (isMe) 2.dp else 12.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column {
                                        ChatBubbleContent(msg = msg, isMe = isMe)
                                    }
                                }
                            }
                        }
                        if (isAgentTyping) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = 2.dp,
                                        bottomEnd = 12.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Meron S. is typing",
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

                    // Support Attachment Options Row
                    if (showSupportAttachmentMenu) {
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
                                            viewModel.sendMessage("Screenshot_Payment_Telebirr.jpg", "mock_image_uri", "IMAGE")
                                            showSupportAttachmentMenu = false
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
                                            viewModel.sendMessage("Courier_Delivery_Update.mp4", "mock_video_uri", "VIDEO")
                                            showSupportAttachmentMenu = false
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
                                            viewModel.sendMessage("CBE_Birr_Receipt_804.pdf", "mock_file_uri", "FILE")
                                            showSupportAttachmentMenu = false
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

                    // Inputs bar (Pill design)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showSupportAttachmentMenu = !showSupportAttachmentMenu },
                            modifier = Modifier.testTag("support_attach_btn")
                        ) {
                            Icon(
                                imageVector = if (showSupportAttachmentMenu) Icons.Default.Close else Icons.Default.AddCircleOutline,
                                contentDescription = "Attach Options",
                                tint = BrandGreenPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("support_input_field"),
                            placeholder = { Text("Type a message...", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))) },
                            singleLine = true,
                            trailingIcon = {
                                val ctx = androidx.compose.ui.platform.LocalContext.current
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
                                            android.widget.Toast.makeText(ctx, "Microphone integration (mock)", android.widget.Toast.LENGTH_SHORT).show()
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
                                            viewModel.sendMessage("Screenshot_Payment_Telebirr.jpg", "mock_image_uri", "IMAGE")
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(BrandGreenPrimary, CircleShape)
                                .testTag("support_send_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // HELP RESOURCES / TOPICS DASHBOARD (Accordion list)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search topics (e.g., payment, password, refund)...",style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("help_search_bar")
                        )
                    }

                    // Help topics list with expansion
                    if (filteredGuides.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No matching guides found.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredGuides) { guide ->
                            val isExpanded = expandedGuideTitles.contains(guide.title)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedGuideTitles = if (isExpanded) {
                                            expandedGuideTitles - guide.title
                                        } else {
                                            expandedGuideTitles + guide.title
                                        }
                                    }
                                    .testTag("guide_${guide.title.replace(" ", "_")}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = BrandGoldSecondary
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = guide.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = guide.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



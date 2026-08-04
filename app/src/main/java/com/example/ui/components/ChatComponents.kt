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
fun ChatBubbleContent(msg: ChatMessageEntity, isMe: Boolean) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (msg.attachmentType != null) {
            val contentColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
            val cardBg = if (isMe) BrandGreenPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(cardBg)
                    .padding(8.dp)
            ) {
                when (msg.attachmentType) {
                    "IMAGE" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Image",
                                    tint = if (isMe) Color.White else BrandGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Image Attachment",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor
                                )
                            }
                            // Styled Mock Photo Frame
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Photo,
                                        contentDescription = null,
                                        tint = BrandGreenPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.messageText.ifBlank { "E-Suuq_Upload.jpg" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandGreenPrimary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    "VIDEO" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play Video",
                                tint = if (isMe) Color.White else BrandGreenPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Column {
                                Text(
                                    text = msg.messageText.ifBlank { "Video_Upload.mp4" },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor
                                )
                                Text(
                                    text = "Video • 1.8 MB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    else -> { // FILE / DOCUMENT
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = "File",
                                tint = if (isMe) Color.White else BrandGreenPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = msg.messageText.ifBlank { "Document_Receipt.pdf" },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor
                                )
                                Text(
                                    text = "PDF Document • 420 KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Download File",
                                tint = contentColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = msg.messageText,
                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}



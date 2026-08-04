package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.MainActivity
import com.example.ui.AppViewModel
import com.example.ui.utils.DebugLogManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DebugOverlay(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    var isExpanded by remember { mutableStateOf(false) }
    val logs by DebugLogManager.logs.collectAsState()
    
    // Auth statuses
    val pbAuth = MainActivity.auth
    val token = pbAuth.client.authToken.value ?: "null"
    val userId = pbAuth.getUserId() ?: "null"
    val role = pbAuth.getUserRole() ?: "null"
    val isPbSessionValid = pbAuth.hasValidSession()
    
    val sessionManagerValid = viewModel.hasValidSession()
    val activeUser by viewModel.activeUser.collectAsState()

    // Floating Button Position State
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(500f) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Floating Draggable DEBUG Button
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFE91E63)) // Dynamic Pink for debug
                .clickable { isExpanded = true }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Debug Panel",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "DEBUG",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        // Fullscreen/Dialog expanded debug console
        if (isExpanded) {
            Dialog(
                onDismissRequest = { isExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E1E1E) // Dark developer theme
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Title Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D2D2D))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = "Debug",
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "In-App Debug Console",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "PocketBase + SessionManager Diagnostics",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(onClick = { isExpanded = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }

                        // Auth & Diagnostics Panel
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "🔴 LIVE SESSION DIAGNOSTICS",
                                    color = Color(0xFFE91E63),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                
                                Divider(color = Color.DarkGray, thickness = 0.5.dp)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Auth Status:", color = Color.LightGray, fontSize = 11.sp)
                                    val authText = when {
                                        isPbSessionValid && sessionManagerValid -> "✅ AUTHENTICATED & ACTIVE"
                                        isPbSessionValid -> "✅ PB AUTH ONLY"
                                        sessionManagerValid -> "✅ SESSION ONLY"
                                        else -> "❌ UNAUTHENTICATED"
                                    }
                                    val authColor = if (isPbSessionValid || sessionManagerValid) Color.Green else Color.Red
                                    Text(
                                        text = authText,
                                        color = authColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("User Role:", color = Color.LightGray, fontSize = 11.sp)
                                    Text(role, color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Active User:", color = Color.LightGray, fontSize = 11.sp)
                                    Text(
                                        text = if (activeUser != null) "👤 ${activeUser?.fullName} (${activeUser?.phone})" else "None",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (token != "null") {
                                                clipboardManager.setText(AnnotatedString(token))
                                                Toast.makeText(context, "Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Token (Click to Copy):", color = Color.LightGray, fontSize = 11.sp)
                                    Text(
                                        text = if (token != "null") "${token.take(15)}... [Len: ${token.length}]" else "null",
                                        color = Color(0xFF03A9F4),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // API & Cache Optimization Monitor Card
                        val totalRequests by com.example.network.ApiMonitor.totalRequests.collectAsState()
                        val recentRequests by com.example.network.ApiMonitor.recentRequests.collectAsState()
                        val cacheStats = remember(totalRequests) { com.example.data.CacheManager.getInstance().getStats() }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF222831)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "⚡ API & CACHE OPTIMIZATION MONITOR",
                                    color = Color(0xFF00ADB5),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                
                                Divider(color = Color.DarkGray, thickness = 0.5.dp)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Session API Requests:", color = Color.LightGray, fontSize = 12.sp)
                                    Text(
                                        text = "$totalRequests",
                                        color = Color(0xFF00ADB5),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cache Hits / Misses:", color = Color.LightGray, fontSize = 12.sp)
                                    Text(
                                        text = "${cacheStats.hits} / ${cacheStats.misses}",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cache Hit Rate:", color = Color.LightGray, fontSize = 12.sp)
                                    Text(
                                        text = String.format("%.1f%%", cacheStats.hitRatePercent),
                                        color = if (cacheStats.hitRatePercent > 50f) Color.Green else Color.Yellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                if (recentRequests.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Recent Requests:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        recentRequests.takeLast(3).reversed().forEach { req ->
                                            Text(
                                                text = req,
                                                color = Color(0xFFEEEEEE),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Toolbar & Logs Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📝 CONSOLE LOGS (${logs.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        DebugLogManager.log("DEBUG", "Initiated manual token refresh request...")
                                        scope.launch {
                                            val res = pbAuth.refreshToken()
                                            DebugLogManager.log("DEBUG", "Refresh response: $res")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refresh", color = Color.White, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        if (logs.isNotEmpty()) {
                                            val allLogs = logs.joinToString("\n")
                                            clipboardManager.setText(AnnotatedString(allLogs))
                                            Toast.makeText(context, "Copied all logs (${logs.size} lines)!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No logs to copy!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy All", color = Color.White, fontSize = 10.sp)
                                }
                                
                                Button(
                                    onClick = { DebugLogManager.clear() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }

                        // Live logs terminal View
                        val scrollState = rememberLazyListState()
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(12.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    text = "Terminal is idle. Trigger actions (login, navigation, refresh) to see real-time output.",
                                    color = Color.DarkGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn(
                                    state = scrollState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(logs) { logMsg ->
                                        val isErr = logMsg.contains("❌")
                                        val isSuccess = logMsg.contains("✅") || logMsg.contains("🎉")
                                        val textColor = when {
                                            isErr -> Color(0xFFFF5252)
                                            isSuccess -> Color(0xFF69F0AE)
                                            logMsg.contains("🔑") -> Color(0xFFFFD740)
                                            logMsg.contains("💾") -> Color(0xFF40C4FF)
                                            else -> Color.LightGray
                                        }
                                        Text(
                                            text = logMsg,
                                            color = textColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(logMsg))
                                                    Toast.makeText(context, "Copied log line to clipboard!", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 2.dp)
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

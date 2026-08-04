package com.example.ui.components


import android.widget.Toast
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.Marker


import com.example.ui.*

import com.example.ui.screens.*

import com.example.ui.components.*

import com.example.ui.utils.*

@Composable
fun OptInHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}


@Composable
fun ReviewUserAvatar(name: String) {
    val initials = remember(name) { 
        name.split(" ")
            .mapNotNull { it.firstOrNull() }
            .take(2)
            .joinToString("")
            .uppercase() 
    }
    val avatarBg = remember(name) {
        val colors = listOf(
            Color(0xFF3F51B5), Color(0xFFE91E63), Color(0xFF009688),
            Color(0xFFFF9800), Color(0xFF673AB7), Color(0xFF4CAF50)
        )
        colors[java.lang.Math.abs(name.hashCode()) % colors.size]
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(avatarBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}


fun isEmulator(): Boolean {
    val brand = android.os.Build.BRAND ?: ""
    val device = android.os.Build.DEVICE ?: ""
    val fingerprint = android.os.Build.FINGERPRINT ?: ""
    val hardware = android.os.Build.HARDWARE ?: ""
    val model = android.os.Build.MODEL ?: ""
    val manufacturer = android.os.Build.MANUFACTURER ?: ""
    val product = android.os.Build.PRODUCT ?: ""

    return (brand.startsWith("generic") && device.startsWith("generic"))
            || fingerprint.startsWith("generic")
            || fingerprint.startsWith("unknown")
            || hardware.contains("goldfish")
            || hardware.contains("ranchu")
            || model.contains("google_sdk")
            || model.contains("Emulator")
            || model.contains("Android SDK built for x86")
            || manufacturer.contains("Genymotion")
            || product.contains("sdk_google")
            || product.contains("google_sdk")
            || product.contains("sdk")
            || product.contains("sdk_x86")
            || product.contains("vbox86p")
            || product.contains("emulator")
            || product.contains("simulator")
}


fun getUserLiveLocation(context: Context): Pair<Double, Double>? {
    val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    if (!hasFine && !hasCoarse) {
        return Pair(9.0105, 38.7612) // Default safe coordinates
    }

    return try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        if (lm != null) {
            val isGps = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            val isNet = lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            var loc: android.location.Location? = null
            if (isGps && hasFine) {
                loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            }
            if (loc == null && isNet) {
                loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            }
            if (loc != null) {
                Pair(loc.latitude, loc.longitude)
            } else {
                Pair(9.0105, 38.7612) // Addis Ababa Bole/Meskel Square Default Live GPS Position
            }
        } else {
            Pair(9.0105, 38.7612)
        }
    } catch (e: SecurityException) {
        Pair(9.0105, 38.7612)
    } catch (e: Exception) {
        Pair(9.0105, 38.7612)
    }
}

data class OsmSearchResult(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

@Composable
fun MapPickerCanvas(
    latitude: Double,
    longitude: Double,
    onSelectCoordinates: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<OsmSearchResult>>(emptyList()) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            val liveLoc = getUserLiveLocation(context) ?: Pair(9.0105, 38.7612)
            onSelectCoordinates(liveLoc.first, liveLoc.second)
            mapViewRef?.controller?.animateTo(GeoPoint(liveLoc.first, liveLoc.second))
            Toast.makeText(context, "Location active! Map centered on your position 📍", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission required to pinpoint live location.", Toast.LENGTH_SHORT).show()
        }
    }

    val handleTurnOnLocation = {
        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasPerm) {
            val liveLoc = getUserLiveLocation(context) ?: Pair(9.0105, 38.7612)
            onSelectCoordinates(liveLoc.first, liveLoc.second)
            mapViewRef?.controller?.animateTo(GeoPoint(liveLoc.first, liveLoc.second))
            Toast.makeText(context, "Live GPS active! Centered at ${String.format(java.util.Locale.US, "%.4f, %.4f", liveLoc.first, liveLoc.second)} 📍", Toast.LENGTH_SHORT).show()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
        onDispose {}
    }

    val osmPresets = remember {
        listOf(
            "Bole, Addis" to Pair(9.0012, 38.7831),
            "Piazza" to Pair(9.0345, 38.7521),
            "Merkato" to Pair(9.0301, 38.7392),
            "Sarbet" to Pair(9.0023, 38.7389),
            "Gondar" to Pair(12.6000, 37.4667),
            "Mekelle" to Pair(13.4967, 39.4753),
            "Adama" to Pair(8.5400, 39.2700),
            "Hawassa" to Pair(7.0600, 38.4700)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // OpenStreetMap Real Search Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it 
                    if (it.isEmpty()) searchResults = emptyList()
                },
                placeholder = { Text("Search location on OpenStreetMap...", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("osm_search_input"),
                shape = RoundedCornerShape(8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search OSM", modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = "" 
                            searchResults = emptyList()
                            searchError = null
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )

            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        isSearching = true
                        searchError = null
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val encoded = URLEncoder.encode(searchQuery.trim(), "UTF-8")
                                val urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=5&addressdetails=1"
                                val conn = URL(urlStr).openConnection() as java.net.HttpURLConnection
                                conn.setRequestProperty("User-Agent", "${context.packageName}/1.0 (Android App)")
                                conn.connectTimeout = 6000
                                conn.readTimeout = 6000
                                val response = conn.inputStream.bufferedReader().use { it.readText() }
                                val jsonArray = JSONArray(response)
                                val list = mutableListOf<OsmSearchResult>()
                                for (i in 0 until jsonArray.length()) {
                                    val item = jsonArray.getJSONObject(i)
                                    val dispName = item.optString("display_name", "Location")
                                    val latVal = item.optDouble("lat", 0.0)
                                    val lonVal = item.optDouble("lon", 0.0)
                                    if (latVal != 0.0 || lonVal != 0.0) {
                                        list.add(OsmSearchResult(dispName, latVal, lonVal))
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    searchResults = list
                                    if (list.isNotEmpty()) {
                                        val top = list.first()
                                        onSelectCoordinates(top.lat, top.lon)
                                        mapViewRef?.controller?.animateTo(GeoPoint(top.lat, top.lon))
                                    } else {
                                        searchError = "No matching location found on OpenStreetMap"
                                    }
                                    isSearching = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    searchError = "OSM Search error: ${e.localizedMessage ?: "Network error"}"
                                    isSearching = false
                                }
                            }
                        }
                    }
                },
                enabled = searchQuery.isNotBlank() && !isSearching,
                modifier = Modifier.height(48.dp).testTag("osm_search_btn"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary)
            ) {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Search", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        if (searchError != null) {
            Text(
                text = searchError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Search Results Pick List
        if (searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Matching Location:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
                    searchResults.take(3).forEach { res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onSelectCoordinates(res.lat, res.lon)
                                    mapViewRef?.controller?.animateTo(GeoPoint(res.lat, res.lon))
                                    searchResults = emptyList()
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(14.dp))
                            Text(
                                text = res.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Quick Presets Horizontal Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(osmPresets) { (name, coords) ->
                AssistChip(
                    onClick = {
                        onSelectCoordinates(coords.first, coords.second)
                        mapViewRef?.controller?.animateTo(GeoPoint(coords.first, coords.second))
                    },
                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrandGreenPrimary) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Native OpenStreetMap Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(latitude, longitude))

                        setOnTouchListener { v, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false
                        }
                        mapViewRef = this
                    }
                },
                update = { view ->
                    mapViewRef = view
                    view.controller.setCenter(GeoPoint(latitude, longitude))
                    view.overlays.clear()

                    val mapEventsReceiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            onSelectCoordinates(p.latitude, p.longitude)
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint): Boolean {
                            onSelectCoordinates(p.latitude, p.longitude)
                            return true
                        }
                    }
                    view.overlays.add(MapEventsOverlay(mapEventsReceiver))

                    val marker = Marker(view).apply {
                        position = GeoPoint(latitude, longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "📍 Delivery Pin"
                        snippet = "Lat: ${String.format(java.util.Locale.US, "%.4f", latitude)}, Lon: ${String.format(java.util.Locale.US, "%.4f", longitude)}"
                        isDraggable = true
                        setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                            override fun onMarkerDrag(m: Marker?) {}
                            override fun onMarkerDragEnd(m: Marker?) {
                                m?.position?.let { pt ->
                                    onSelectCoordinates(pt.latitude, pt.longitude)
                                }
                            }
                            override fun onMarkerDragStart(m: Marker?) {}
                        })
                    }
                    view.overlays.add(marker)
                    view.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // OpenStreetMap Badge Tag
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF102A1C),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🗺️ OpenStreetMap Native", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF22C55E))
                }
            }

            // Quick Turn On / Center My Live Location Floating Action Button
            SmallFloatingActionButton(
                onClick = { handleTurnOnLocation() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .testTag("turn_on_live_location_fab"),
                containerColor = BrandGreenPrimary,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Turn On Location", modifier = Modifier.size(16.dp))
                    Text("My Live Location", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Coordinate Status Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = "Lat: ${String.format(java.util.Locale.US, "%.4f", latitude)}, Lon: ${String.format(java.util.Locale.US, "%.4f", longitude)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { handleTurnOnLocation() },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandGreenPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Turn On Location", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = BrandGreenPrimary)
            }
        }
    }
}

private fun createMapMarkerDrawable(context: android.content.Context, colorInt: Int): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (28 * density).toInt()
    val heightPx = (36 * density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(widthPx, heightPx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val shadowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    val outerPath = android.graphics.Path().apply {
        val radius = widthPx / 2f
        addCircle(radius, radius, radius - 2f, android.graphics.Path.Direction.CW)
        moveTo(radius - (radius * 0.6f), radius + (radius * 0.4f))
        lineTo(radius, heightPx.toFloat() - 2f)
        lineTo(radius + (radius * 0.6f), radius + (radius * 0.4f))
        close()
    }
    canvas.drawPath(outerPath, shadowPaint)

    val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = android.graphics.Paint.Style.FILL
    }
    val innerPath = android.graphics.Path().apply {
        val radius = (widthPx / 2f) - (3 * density)
        addCircle(widthPx / 2f, widthPx / 2f, radius, android.graphics.Path.Direction.CW)
        moveTo(widthPx / 2f - (radius * 0.6f), widthPx / 2f + (radius * 0.4f))
        lineTo(widthPx / 2f, heightPx.toFloat() - (5 * density))
        lineTo(widthPx / 2f + (radius * 0.6f), widthPx / 2f + (radius * 0.4f))
        close()
    }
    canvas.drawPath(innerPath, fillPaint)

    val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(widthPx / 2f, widthPx / 2f, 4 * density, dotPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

@Composable
fun OpenStreetMapOrderTracker(
    sellerLat: Double? = null,
    sellerLon: Double? = null,
    buyerLat: Double? = null,
    buyerLon: Double? = null,
    driverLat: Double? = null,
    driverLon: Double? = null,
    progress: Float = 0.5f,
    modifier: Modifier = Modifier,
    onLocationUpdate: ((Double, Double) -> Unit)? = null,
    orderStatus: String = "",
    isDeliverySide: Boolean = false
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var osmrRoad by remember { mutableStateOf<Road?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    val sLat = sellerLat ?: 9.0012
    val sLon = sellerLon ?: 38.7831
    val bLat = buyerLat ?: 9.0210
    val bLon = buyerLon ?: 38.7650
    val dLat = driverLat ?: 0.0
    val dLon = driverLon ?: 0.0

    val isOutForDelivery = orderStatus == "OUT_FOR_DELIVERY" || orderStatus == "SHIPPED"

    val activeDriverLat = if (isDeliverySide) {
        val live = getUserLiveLocation(context)
        if (dLat != 0.0) dLat else (live?.first ?: 9.0210)
    } else {
        dLat
    }
    val activeDriverLon = if (isDeliverySide) {
        val live = getUserLiveLocation(context)
        if (dLon != 0.0) dLon else (live?.second ?: 38.7650)
    } else {
        dLon
    }

    val originLat = if (isOutForDelivery) {
        if (activeDriverLat != 0.0) activeDriverLat else sLat
    } else {
        if (activeDriverLat != 0.0) activeDriverLat else 9.0210
    }
    val originLon = if (isOutForDelivery) {
        if (activeDriverLon != 0.0) activeDriverLon else sLon
    } else {
        if (activeDriverLon != 0.0) activeDriverLon else 38.7650
    }

    val destLat = if (isOutForDelivery) bLat else sLat
    val destLon = if (isOutForDelivery) bLon else sLon

    LaunchedEffect(originLat, originLon, destLat, destLon) {
        isCalculatingRoute = true
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val roadManager = OSRMRoadManager(context, Configuration.getInstance().userAgentValue)
                val waypoints = ArrayList<GeoPoint>()
                waypoints.add(GeoPoint(originLat, originLon))
                waypoints.add(GeoPoint(destLat, destLon))
                val road = roadManager.getRoad(waypoints)
                if (road != null && road.mStatus == Road.STATUS_OK) {
                    osmrRoad = road
                }
            }
        } catch (e: Exception) {
            // Fallback gracefully to interpolated point route
        } finally {
            isCalculatingRoute = false
        }
    }

    // Calculate live courier position along OSRM road route or linear path or actual driver coordinates
    val routePoints = osmrRoad?.mRouteHigh ?: emptyList()
    val courierPoint = remember(routePoints, originLat, originLon, destLat, destLon, activeDriverLat, activeDriverLon, progress, isOutForDelivery) {
        if (activeDriverLat != 0.0 && activeDriverLon != 0.0) {
            GeoPoint(activeDriverLat, activeDriverLon)
        } else if (routePoints.isNotEmpty()) {
            val targetIdx = ((routePoints.size - 1) * progress.coerceIn(0f, 1f)).toInt()
            routePoints[targetIdx]
        } else {
            GeoPoint(
                originLat + (destLat - originLat) * progress.toDouble(),
                originLon + (destLon - originLon) * progress.toDouble()
            )
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            val liveLoc = getUserLiveLocation(context) ?: Pair(courierPoint.latitude, courierPoint.longitude)
            mapViewRef?.controller?.animateTo(GeoPoint(liveLoc.first, liveLoc.second))
            Toast.makeText(context, "GPS Location active! Centered map 📍", Toast.LENGTH_SHORT).show()
            onLocationUpdate?.invoke(liveLoc.first, liveLoc.second)
        } else {
            Toast.makeText(context, "Location permission required to get live GPS.", Toast.LENGTH_SHORT).show()
        }
    }

    val handleTurnOnLocation = {
        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasPerm) {
            val liveLoc = getUserLiveLocation(context) ?: Pair(courierPoint.latitude, courierPoint.longitude)
            mapViewRef?.controller?.animateTo(GeoPoint(liveLoc.first, liveLoc.second))
            Toast.makeText(context, "GPS active! Centered on live position 📍", Toast.LENGTH_SHORT).show()
            onLocationUpdate?.invoke(liveLoc.first, liveLoc.second)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        handleTurnOnLocation()
    }

    DisposableEffect(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
        onDispose {}
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(14.5)
                    controller.setCenter(courierPoint)

                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }
                    mapViewRef = this
                }
            },
            update = { view ->
                mapViewRef = view
                view.overlays.clear()

                if (osmrRoad != null) {
                    val roadOverlay = RoadManager.buildRoadOverlay(osmrRoad).apply {
                        outlinePaint.color = android.graphics.Color.parseColor("#22C55E")
                        outlinePaint.strokeWidth = 9f
                    }
                    view.overlays.add(roadOverlay)
                } else {
                    // Fallback Polyline Route
                    val routeLine = org.osmdroid.views.overlay.Polyline(view).apply {
                        addPoint(GeoPoint(originLat, originLon))
                        addPoint(courierPoint)
                        addPoint(GeoPoint(destLat, destLon))
                        outlinePaint.color = android.graphics.Color.parseColor("#22C55E")
                        outlinePaint.strokeWidth = 8f
                    }
                    view.overlays.add(routeLine)
                }

                // Store / Seller Marker (RED)
                val sellerMarker = Marker(view).apply {
                    position = GeoPoint(sLat, sLon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "☕ Merchant Store"
                    snippet = "Order Pickup Point"
                    icon = createMapMarkerDrawable(context, android.graphics.Color.RED)
                }
                view.overlays.add(sellerMarker)

                // Buyer Destination Marker (Only if OUT_FOR_DELIVERY)
                if (isOutForDelivery) {
                    val buyerMarker = Marker(view).apply {
                        position = GeoPoint(bLat, bLon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "🏠 Buyer Destination"
                        snippet = "Delivery Dropoff Location"
                        icon = createMapMarkerDrawable(context, android.graphics.Color.parseColor("#22C55E")) // GREEN
                    }
                    view.overlays.add(buyerMarker)
                }

                // Live Courier / Driver Location Moving Marker (BLUE) - ONLY ON BUYER SIDE
                if (!isDeliverySide) {
                    val courierMarker = Marker(view).apply {
                        position = courierPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "🚴 Driver Location"
                        snippet = "Progress: ${(progress * 100).toInt()}% • Active En Route"
                        icon = createMapMarkerDrawable(context, android.graphics.Color.parseColor("#2563EB")) // BLUE
                    }
                    view.overlays.add(courierMarker)
                } else {
                    val hasFinePerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasCoarsePerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if ((hasFinePerm || hasCoarsePerm) && !isEmulator()) {
                        try {
                            val myLocationOverlay = org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(
                                org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider(context),
                                view
                            ).apply {
                                enableMyLocation()
                            }
                            view.overlays.add(myLocationOverlay)
                        } catch (e: Exception) {
                            try {
                                val myLocationOverlay = SimpleLocationOverlay(context).apply {
                                    val liveLoc = getUserLiveLocation(context) ?: Pair(9.0105, 38.7612)
                                    setLocation(org.osmdroid.util.GeoPoint(liveLoc.first, liveLoc.second))
                                }
                                view.overlays.add(myLocationOverlay)
                            } catch (e2: Exception) {
                                val fallbackMarker = org.osmdroid.views.overlay.Marker(view).apply {
                                    val liveLoc = getUserLiveLocation(context) ?: Pair(9.0105, 38.7612)
                                    position = org.osmdroid.util.GeoPoint(liveLoc.first, liveLoc.second)
                                    setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                    title = "My Location"
                                    icon = createMapMarkerDrawable(context, android.graphics.Color.parseColor("#10B981"))
                                }
                                view.overlays.add(fallbackMarker)
                            }
                        }
                    } else if (hasFinePerm || hasCoarsePerm) {
                        try {
                            val myLocationOverlay = SimpleLocationOverlay(context).apply {
                                val liveLoc = getUserLiveLocation(context) ?: Pair(9.0105, 38.7612)
                                setLocation(org.osmdroid.util.GeoPoint(liveLoc.first, liveLoc.second))
                            }
                            view.overlays.add(myLocationOverlay)
                        } catch (e: Exception) {
                            val fallbackMarker = org.osmdroid.views.overlay.Marker(view).apply {
                                val liveLoc = getUserLiveLocation(context) ?: Pair(9.0105, 38.7612)
                                position = org.osmdroid.util.GeoPoint(liveLoc.first, liveLoc.second)
                                setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                title = "My Location"
                                icon = createMapMarkerDrawable(context, android.graphics.Color.parseColor("#10B981"))
                            }
                            view.overlays.add(fallbackMarker)
                        }
                    }
                }

                view.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Stats & Control Bar Overlays
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF102A1C),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isCalculatingRoute) "🔄 Calculating Route..." else if (osmrRoad != null) "🛣️ OSRM Road Route" else "🗺️ OpenStreetMap Live",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF22C55E)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallFloatingActionButton(
                        onClick = { mapViewRef?.controller?.animateTo(courierPoint) },
                        containerColor = BrandGoldSecondary,
                        contentColor = Color.Black,
                        modifier = Modifier.testTag("osm_center_courier_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.DirectionsBike, contentDescription = "Center Courier", modifier = Modifier.size(16.dp))
                            Text("Courier", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    SmallFloatingActionButton(
                        onClick = { handleTurnOnLocation() },
                        containerColor = BrandGreenPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("osm_turn_on_location_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Turn On Location", modifier = Modifier.size(16.dp))
                            Text("GPS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

        
        }
    }
}

fun calculateDeliveryEta(
    road: Road?,
    startLat: Double,
    startLon: Double,
    destLat: Double,
    destLon: Double,
    progress: Float = 0f
): Pair<String, String> {
    val totalSeconds = if (road != null && road.mStatus == Road.STATUS_OK && road.mDuration > 0) {
        (road.mDuration * (1.0 - progress.coerceIn(0f, 1f))).toInt()
    } else {
        val remainingDistKm = calculateDistanceKm(startLat, startLon, destLat, destLon) * (1.0 - progress.coerceIn(0f, 1f))
        (remainingDistKm * 2.4 * 60).toInt().coerceAtLeast(180)
    }

    val minutes = (totalSeconds / 60).coerceAtLeast(1)
    val durationStr = if (minutes >= 60) {
        val hrs = minutes / 60
        val mins = minutes % 60
        if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
    } else {
        "$minutes mins"
    }

    val arrivalCalendar = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.SECOND, totalSeconds)
    }
    val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
    val clockTimeStr = timeFormat.format(arrivalCalendar.time)

    return Pair(durationStr, clockTimeStr)
}

fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@Composable
fun OpenStreetMapStoreTracker(
    storeLat: Double = 9.0012,
    storeLon: Double = 38.7831,
    storeName: String = "E-Suuq Partner Store",
    storeAddress: String = "Addis Ababa, Bole Road, Sheger Building, Ground Floor",
    userLat: Double = 9.0210,
    userLon: Double = 38.7650,
    modifier: Modifier = Modifier,
    onUserLocationChange: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentLat by remember { mutableStateOf(userLat) }
    var currentLon by remember { mutableStateOf(userLon) }

    val distanceKm = remember(currentLat, currentLon, storeLat, storeLon) {
        calculateDistanceKm(currentLat, currentLon, storeLat, storeLon)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            val liveLoc = getUserLiveLocation(context) ?: Pair(currentLat, currentLon)
            currentLat = liveLoc.first
            currentLon = liveLoc.second
            onUserLocationChange?.invoke(liveLoc.first, liveLoc.second)
            mapViewRef?.controller?.animateTo(GeoPoint(liveLoc.first, liveLoc.second))
            Toast.makeText(context, "Location updated! Distance to store: ${String.format(java.util.Locale.US, "%.2f", distanceKm)} km", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission required.", Toast.LENGTH_SHORT).show()
        }
    }

    val handleTurnOnLocation = {
        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            val liveLoc = getUserLiveLocation(context) ?: Pair(currentLat, currentLon)
            currentLat = liveLoc.first
            currentLon = liveLoc.second
            onUserLocationChange?.invoke(liveLoc.first, liveLoc.second)
            mapViewRef?.controller?.animateTo(GeoPoint(liveLoc.first, liveLoc.second))
            Toast.makeText(context, "GPS active! Distance to store: ${String.format(java.util.Locale.US, "%.2f", distanceKm)} km", Toast.LENGTH_SHORT).show()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val openDirectionsMap = {
        try {
            val uri = android.net.Uri.parse("geo:$storeLat,$storeLon?q=$storeLat,$storeLon(" + java.net.URLEncoder.encode(storeName, "UTF-8") + ")")
            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Navigating to $storeAddress...", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
        onDispose {}
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(14.0)
                        controller.setCenter(GeoPoint((currentLat + storeLat) / 2, (currentLon + storeLon) / 2))

                        setOnTouchListener { v, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false
                        }
                        mapViewRef = this
                    }
                },
                update = { view ->
                    mapViewRef = view
                    view.overlays.clear()

                    // Polyline route between User and Store
                    val routeLine = org.osmdroid.views.overlay.Polyline(view).apply {
                        addPoint(GeoPoint(currentLat, currentLon))
                        addPoint(GeoPoint(storeLat, storeLon))
                        outlinePaint.color = android.graphics.Color.parseColor("#22C55E")
                        outlinePaint.strokeWidth = 7f
                    }
                    view.overlays.add(routeLine)

                    // Store Marker
                    val storeMarker = Marker(view).apply {
                        position = GeoPoint(storeLat, storeLon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "🏬 $storeName"
                        snippet = storeAddress
                    }
                    view.overlays.add(storeMarker)

                    // User Marker
                    val userMarker = Marker(view).apply {
                        position = GeoPoint(currentLat, currentLon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "📍 Your Position"
                        snippet = "Lat: ${String.format(java.util.Locale.US, "%.4f", currentLat)}, Lon: ${String.format(java.util.Locale.US, "%.4f", currentLon)}"
                    }
                    view.overlays.add(userMarker)

                    view.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Header Badge
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF102A1C),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🏬 Store Location Map", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF22C55E))
                }
            }

            // Turn On GPS Floating Action Button
            SmallFloatingActionButton(
                onClick = { handleTurnOnLocation() },
                containerColor = BrandGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .testTag("turn_on_store_location_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Turn On Location", modifier = Modifier.size(16.dp))
                    Text("Turn On GPS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Distance & Direct To Store Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandGreenPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(storeName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text(storeAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandGreenPrimary.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("DISTANCE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.2f", distanceKm)} km",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = BrandGreenPrimary
                            )
                        }
                    }
                }

                Button(
                    onClick = { openDirectionsMap() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("direct_to_store_location_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = "Direct to Store", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Direct to Store Location 🗺️", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}


@Composable
fun RequirementCheckRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = BrandGreenPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun AccountValueRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface))
    }
}


class SimpleLocationOverlay(context: android.content.Context) : org.osmdroid.views.overlay.Overlay() {
    private var mLocation: org.osmdroid.util.GeoPoint? = null
    private val mPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#3B82F6") // BLUE
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = true
    }
    private val mAccuracyPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#303B82F6") // Translucent BLUE
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = true
    }
    private val mWhiteBorderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    fun setLocation(mp: org.osmdroid.util.GeoPoint) {
        mLocation = mp
    }

    override fun draw(canvas: android.graphics.Canvas, mapView: org.osmdroid.views.MapView, shadow: Boolean) {
        if (shadow) return
        val loc = mLocation ?: return
        
        val pj = mapView.projection
        val screenCoords = android.graphics.Point()
        pj.toPixels(loc, screenCoords)

        // Draw accuracy circle and the location dot
        canvas.drawCircle(screenCoords.x.toFloat(), screenCoords.y.toFloat(), 40f, mAccuracyPaint)
        canvas.drawCircle(screenCoords.x.toFloat(), screenCoords.y.toFloat(), 18f, mPaint)
        canvas.drawCircle(screenCoords.x.toFloat(), screenCoords.y.toFloat(), 18f, mWhiteBorderPaint)
    }
}




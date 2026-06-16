package com.geomemoir.presentation.map

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.geomemoir.R
import com.geomemoir.domain.entity.PlaceWithCategory
import com.geomemoir.presentation.navigation.Screen
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import com.geomemoir.data.map.MapConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val places by viewModel.places.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeCategoryId by viewModel.activeCategoryId.collectAsState()
    val pendingLocation by viewModel.pendingLocation.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fineGranted || coarseGranted) {
                viewModel.fetchCurrentLocation()
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Location permission denied. Drop pins manually by long-pressing.")
                }
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Current Location")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map Rendering view
            MapLibreView(
                modifier = Modifier.fillMaxSize(),
                places = places,
                cameraEvents = viewModel.cameraEvents,
                onMarkerTap = { placeId ->
                    navController.navigate(Screen.PlaceDetail.createRoute(placeId))
                },
                onMapLongPress = { latLng ->
                    viewModel.onMapLongPress(latLng)
                }
            )

            // Category Chips at Top
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC0F0F13)) // semi transparent obsidian
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeCategoryId == null,
                        onClick = { viewModel.filterByCategory(null) },
                        label = { Text("All Places") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = activeCategoryId == category.id,
                        onClick = { viewModel.filterByCategory(category.id) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            color = Color(android.graphics.Color.parseColor(category.colorHex)),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(category.name)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Bottom popup when a pin is dropped via long press
            if (pendingLocation != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pin Dropped",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lat: ${"%.6f".format(pendingLocation?.latitude)}, Lng: ${"%.6f".format(pendingLocation?.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Button(
                                onClick = {
                                    val loc = pendingLocation
                                    if (loc != null) {
                                        navController.navigate(Screen.AddEditPlace.createRoute(loc.latitude, loc.longitude))
                                        viewModel.clearPendingLocation()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Visited Place")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.clearPendingLocation() },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.Gray
                                )
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapLibreView(
    modifier: Modifier = Modifier,
    places: List<PlaceWithCategory>,
    cameraEvents: kotlinx.coroutines.flow.SharedFlow<LatLng>,
    onMarkerTap: (Long) -> Unit,
    onMapLongPress: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    // Manage MapView Lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = MapLifecycleObserver(mapView)
        lifecycleOwner.lifecycle.addObserver(observer)
        // mapView.onCreate(null) // Not strictly required if handled by observer, but good practice
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )

    // Initialize Map Instance and Style
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            mapInstance = map
            map.setStyle(Style.Builder().fromUri(MapConfig.STYLE_URL)) { style ->
                android.util.Log.d("MapScreen", "Style loaded. Initial setup.")
                setupPlacesLayer(style, places)
                
                // Initial world view if not moved
                if (map.cameraPosition.zoom < 2.0) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.0, 0.0), 1.5))
                }
            }
            
            map.addOnMapLongClickListener { latLng ->
                onMapLongPress(latLng)
                true
            }
            map.addOnMapClickListener { latLng ->
                val screenPoint = map.projection.toScreenLocation(latLng)
                val features = map.queryRenderedFeatures(screenPoint, MapConfig.PLACES_LAYER_ID)
                if (features.isNotEmpty()) {
                    features.firstOrNull()?.getStringProperty("id")?.toLongOrNull()?.let {
                        onMarkerTap(it)
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    LaunchedEffect(places, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        map.getStyle { style ->
            val center = map.cameraPosition.target
            android.util.Log.d("MapScreen", "Syncing ${places.size} places. Center: ${center?.latitude}, ${center?.longitude}")
            setupPlacesLayer(style, places)
            updatePlacesSource(style, places)
        }
    }

    // Handle Camera Events
    LaunchedEffect(cameraEvents) {
        cameraEvents.collect { latLng ->
            mapInstance?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, MapConfig.DEFAULT_ZOOM))
        }
    }
}

class MapLifecycleObserver(private val mapView: MapView) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> {}
        }
    }
}

fun setupPlacesLayer(style: Style, places: List<PlaceWithCategory>) {
    if (style.getSource(MapConfig.PLACES_SOURCE_ID) != null) {
        updatePlacesSource(style, places)
        return
    }

    val source = GeoJsonSource(MapConfig.PLACES_SOURCE_ID)
    style.addSource(source)

    // Colored Badge Layer (Just Circles, no text)
    val circleLayer = CircleLayer(MapConfig.PLACES_LAYER_ID, MapConfig.PLACES_SOURCE_ID).withProperties(
        PropertyFactory.circleRadius(10f),
        PropertyFactory.circleColor(Expression.get("color")),
        PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
        PropertyFactory.circleStrokeWidth(2f),
        PropertyFactory.circleOpacity(1.0f),
        PropertyFactory.circleStrokeOpacity(1.0f)
    )
    style.addLayer(circleLayer)
    
    updatePlacesSource(style, places)
}

private fun buildFeatureCollection(places: List<PlaceWithCategory>): FeatureCollection {
    val features = places.map { item ->
        val feature = Feature.fromGeometry(Point.fromLngLat(item.place.longitude, item.place.latitude))
        feature.addStringProperty("id", item.place.id.toString())
        feature.addStringProperty("name", item.place.name)
        feature.addNumberProperty("rating", item.place.rating ?: 0)
        // Set color: Category color if exists, else Default Grey (#9E9E9E)
        val color = item.category?.colorHex ?: "#9E9E9E"
        feature.addStringProperty("color", color)
        feature
    }
    return FeatureCollection.fromFeatures(features)
}

fun updatePlacesSource(style: Style, places: List<PlaceWithCategory>) {
    val collection = buildFeatureCollection(places)
    android.util.Log.d("MapScreen", "Updating GeoJSON source. Size: ${places.size}")
    (style.getSource(MapConfig.PLACES_SOURCE_ID) as? GeoJsonSource)
        ?.setGeoJson(collection)
}


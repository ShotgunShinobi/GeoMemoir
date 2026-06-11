package com.geomemoir.presentation.offline_maps

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.geomemoir.data.map.MapConfig
import com.geomemoir.presentation.map.MapLifecycleObserver
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectRegionScreen(
    navController: NavController,
    viewModel: OfflineMapsViewModel = hiltViewModel()
) {
    var regionName by remember { mutableStateOf("") }
    var maxZoom by remember { mutableFloatStateOf(13f) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var tileEstimate by remember { mutableStateOf(0L) }
    var isLimitExceeded by remember { mutableStateOf(false) }

    val updateEstimation = {
        val map = mapInstance
        if (map != null) {
            val bounds = map.projection.visibleRegion.latLngBounds
            val est = estimateTileCount(
                minLat = bounds.latitudeSouth,
                maxLat = bounds.latitudeNorth,
                minLng = bounds.longitudeWest,
                maxLng = bounds.longitudeEast,
                maxZoom = maxZoom.toInt()
            )
            tileEstimate = est
            isLimitExceeded = est > 6000
        }
    }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapInstance = map
                map.setStyle(Style.Builder().fromUri(MapConfig.STYLE_URL)) {
                    updateEstimation()
                }
                map.addOnCameraIdleListener {
                    updateEstimation()
                }
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        val observer = MapLifecycleObserver(mapView)
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Download Area") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Outline Box overlay in center showing downloaded bounds
            Box(
                modifier = Modifier
                    .fillMaxSize(0.7f)
                    .align(Alignment.Center)
                    .border(3.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
            )

            // Settings card at bottom
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = regionName,
                        onValueChange = { regionName = it },
                        placeholder = { Text("Region Name (e.g. Rome)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Max Zoom: ${maxZoom.toInt()}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        Slider(
                            value = maxZoom,
                            onValueChange = {
                                maxZoom = it
                                updateEstimation()
                            },
                            valueRange = 1f..15f,
                            steps = 14,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Estimated Tiles: $tileEstimate",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLimitExceeded) Color.Red else Color.LightGray,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLimitExceeded) {
                            Text(
                                text = "Limit Exceeded (>6000)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val map = mapInstance
                            if (map != null && regionName.isNotBlank() && !isLimitExceeded) {
                                val bounds = map.projection.visibleRegion.latLngBounds
                                viewModel.createAndDownloadRegion(
                                    name = regionName.trim(),
                                    minLat = bounds.latitudeSouth,
                                    maxLat = bounds.latitudeNorth,
                                    minLng = bounds.longitudeWest,
                                    maxLng = bounds.longitudeEast,
                                    maxZoom = maxZoom.toInt()
                                )
                                navController.popBackStack()
                            }
                        },
                        enabled = regionName.isNotBlank() && !isLimitExceeded,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download Region")
                    }
                }
            }
        }
    }
}

fun estimateTileCount(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double, maxZoom: Int): Long {
    var totalTiles = 0L
    for (z in 1..maxZoom) {
        val latTiles = Math.max(1.0, (maxLat - minLat) * Math.pow(2.0, z.toDouble()) / 170.1022)
        val lngTiles = Math.max(1.0, (maxLng - minLng) * Math.pow(2.0, z.toDouble()) / 360.0)
        totalTiles += (latTiles * lngTiles).toLong()
    }
    return totalTiles
}

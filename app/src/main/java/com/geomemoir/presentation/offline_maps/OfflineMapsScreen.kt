package com.geomemoir.presentation.offline_maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.geomemoir.domain.entity.DownloadProgress
import com.geomemoir.domain.entity.DownloadStatus
import com.geomemoir.domain.entity.OfflineRegion
import com.geomemoir.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapsScreen(
    navController: NavController,
    viewModel: OfflineMapsViewModel = hiltViewModel()
) {
    val regions by viewModel.regions.collectAsState()
    val progresses by viewModel.downloadProgresses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Offline Map Regions") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.SelectRegion.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Region")
            }
        }
    ) { paddingValues ->
        if (regions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No offline maps saved yet.\nTap + to download a region.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(regions) { region ->
                    val progress = progresses[region.id]
                    OfflineRegionItem(
                        region = region,
                        progress = progress,
                        onDelete = { viewModel.deleteRegion(region.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun OfflineRegionItem(
    region: OfflineRegion,
    progress: DownloadProgress?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = region.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Zoom level range: 1 - ${region.maxZoom}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (region.status == DownloadStatus.DOWNLOADING || progress != null) {
                val percentage = progress?.percentage ?: 0
                val progressVal = (progress?.completedTiles?.toFloat() ?: 0f) / (progress?.totalTiles?.toFloat() ?: 1f)
                
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Downloading Tiles...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressVal },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when (region.status) {
                        DownloadStatus.COMPLETE -> Color(0xFF26A69A)
                        DownloadStatus.ERROR -> Color.Red
                        DownloadStatus.PAUSED -> Color.Yellow
                        else -> Color.Gray
                    }
                    Text(
                        text = region.status.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    region.sizeBytes?.let { bytes ->
                        val mbSize = bytes.toFloat() / (1024f * 1024f)
                        Text(
                            text = "Size: ${"%.2f".format(mbSize)} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

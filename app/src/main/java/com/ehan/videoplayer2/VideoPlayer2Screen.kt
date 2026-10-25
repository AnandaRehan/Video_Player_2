package com.ehan.videoplayer2

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class
)
@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Determine which permission to request based on API level
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(permission = permission) { granted ->
        viewModel.setPermissionGranted(granted)
    }

    // SAF launcher for picking a video file (doesn't require permission)
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setVideoUri(it) }
    }

    // Initialize ExoPlayer once
    LaunchedEffect(Unit) {
        viewModel.initializePlayer()
        viewModel.setPermissionGranted(permissionState.status.isGranted)
    }

    // Refresh permission state when returning to screen
    LaunchedEffect(permissionState.status.isGranted) {
        viewModel.setPermissionGranted(permissionState.status.isGranted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Player") },
                actions = {
                    IconButton(
                        onClick = {
                            pickVideoLauncher.launch(arrayOf("video/*"))
                        }
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Pick video")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // ----------------------------------------------------------
            //  Permission gate
            // ----------------------------------------------------------
            if (!uiState.isPermissionGranted) {
                PermissionRationale(
                    permission = permission,
                    onRequestPermission = { permissionState.launchPermissionRequest() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // ------------------------------------------------------
                //  Video surface + controls
                // ------------------------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    VideoSurface(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PlayerControls(
                    uiState = uiState,
                    onPlayPause = viewModel::playPause,
                    onSeek = viewModel::seekTo,
                    onSpeedChange = viewModel::setPlaybackSpeed,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------
//  Video Surface using PlayerView in AndroidView
// ------------------------------------------------------------------
@Composable
private fun VideoSurface(
    viewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // We need to access the ExoPlayer instance from the ViewModel.
    // In a real app, expose the player via a read-only property or use Hilt.
    // For this demo, we re-create the player reference through a callback.
    // (See the ViewModel change below for a cleaner approach.)
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false // we use custom Compose controls
                // The player is set via the ViewModel's helper
                viewModel.getPlayer()?.let { player = it }
            }
        },
        update = { playerView ->
            viewModel.getPlayer()?.let { playerView.player = it }
        },
        modifier = modifier
    )
}

// ------------------------------------------------------------------
//  Custom Compose Controls
// ------------------------------------------------------------------
@Composable
private fun PlayerControls(
    uiState: VideoPlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Progress Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formatTime(uiState.currentPosition),
                style = MaterialTheme.typography.labelSmall
            )
            Slider(
                value = if (uiState.duration > 0) {
                    uiState.currentPosition.toFloat() / uiState.duration
                } else 0f,
                onValueChange = { fraction ->
                    onSeek((fraction * uiState.duration).toLong())
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = formatTime(uiState.duration),
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Play / Pause + Speed selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }

            PlaybackSpeedSelector(
                currentSpeed = uiState.playbackSpeed,
                onSpeedChange = onSpeedChange
            )
        }

        // Error message
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ------------------------------------------------------------------
//  Speed Selector (Dropdown)
// ------------------------------------------------------------------
@Composable
private fun PlaybackSpeedSelector(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("${currentSpeed}x")
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Speed")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            speeds.forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}x") },
                    onClick = {
                        onSpeedChange(speed)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------------
//  Permission Rationale
// ------------------------------------------------------------------
@Composable
private fun PermissionRationale(
    permission: String,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Permission required to read video files",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This app needs access to your media to play local videos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

// ------------------------------------------------------------------
//  Helper – format milliseconds to mm:ss
// ------------------------------------------------------------------
private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    return "%02d:%02d".format(minutes, seconds)
}
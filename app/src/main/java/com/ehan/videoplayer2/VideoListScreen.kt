
package com.ehan.videoplater2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun VideoListScreen(
    onVideoClick: (LocalVideo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permission =
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var videos by remember {
        mutableStateOf<List<LocalVideo>>(emptyList())
    }

    var loading by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            loading = true
            videos = getLocalVideos(context)
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "My Videos (${videos.size})",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        when {
            !hasPermission -> {
                Text(
                    "Allow video access to see videos on your phone.",
                    modifier = Modifier.padding(16.dp)
                )

                Button(
                    onClick = {
                        permissionLauncher.launch(permission)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Grant permission")
                }
            }

            loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }

            videos.isEmpty() -> {
                Text(
                    "No videos found.",
                    modifier = Modifier.padding(16.dp)
                )
            }

            else -> {
                LazyColumn {
                    items(
                        items = videos,
                        key = { it.id }
                    ) { video ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onVideoClick(video)
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = video.name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = formatDuration(video.durationMs) +
                                    " • " +
                                    formatSize(video.sizeBytes),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun formatSize(bytes: Long): String {
    return if (bytes >= 1024L * 1024L * 1024L) {
        "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    } else {
        "%.1f MB".format(bytes / (1024.0 * 1024))
    }
}
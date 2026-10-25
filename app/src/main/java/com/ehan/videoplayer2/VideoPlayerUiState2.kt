package com.ehan.videoplayer2

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class VideoPlayerUiState(
    val videoUri: Uri? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isPermissionGranted: Boolean = false
)
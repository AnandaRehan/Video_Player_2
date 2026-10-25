package com.ehan.videoplayer2

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    // ------------------------------------------------------------------
    //  Player setup
    // ------------------------------------------------------------------
    fun initializePlayer() {
        if (exoPlayer != null) return
        exoPlayer = ExoPlayer.Builder(getApplication()).build().also { player ->
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val duration = player.duration.coerceAtLeast(0L)
                        _uiState.update { it.copy(duration = duration, isLoading = false) }
                    }
                    if (playbackState == Player.STATE_BUFFERING) {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        _uiState.update { it.copy(isPlaying = false) }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _uiState.update {
                        it.copy(
                            error = error.localizedMessage ?: "Playback error",
                            isLoading = false
                        )
                    }
                }
            })
        }
    }

    // ------------------------------------------------------------------
    //  Media loading
    // ------------------------------------------------------------------
    fun setVideoUri(uri: Uri) {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            _uiState.update {
                it.copy(
                    videoUri = uri,
                    error = null,
                    isLoading = true
                )
            }
            startProgressUpdates()
        }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    // ------------------------------------------------------------------
    //  Playback controls
    // ------------------------------------------------------------------
    fun playPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    // ------------------------------------------------------------------
    //  Progress updates
    // ------------------------------------------------------------------
    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _uiState.update {
                            it.copy(currentPosition = player.currentPosition)
                        }
                    }
                }
                delay(200L)
            }
        }
    }

    // ------------------------------------------------------------------
    //  Permission state
    // ------------------------------------------------------------------
    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isPermissionGranted = granted) }
    }

    // ------------------------------------------------------------------
    //  Lifecycle
    // ------------------------------------------------------------------
    override fun onCleared() {
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        super.onCleared()
    }
}

data class LocalVideo(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long
)
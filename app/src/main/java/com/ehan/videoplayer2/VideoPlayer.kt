
package com.ehan.videoplayer2

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.material3.Player

@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(player, videoUrl) {
        player.setMediaItem(MediaItem.fromUri(videoUrl))
        player.prepare()
        player.playWhenReady = false
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Player(
        player = player,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    )
}
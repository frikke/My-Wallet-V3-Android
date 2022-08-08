package com.blockchain.componentlib.media

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView

@Composable
fun VideoPlayerItem(
    modifier: Modifier,
    sourceUrl: String
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(sourceUrl))
            // repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
            // play()
        }
    }
    DisposableEffect(
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            factory = {
                StyledPlayerView(context).apply {
                    player = exoPlayer
                    setControllerVisibilityListener(
                        StyledPlayerView.ControllerVisibilityListener { visibility ->
                            if (visibility == View.VISIBLE) {
                                // useController = false
                            }
                        }
                    )
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        )
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}

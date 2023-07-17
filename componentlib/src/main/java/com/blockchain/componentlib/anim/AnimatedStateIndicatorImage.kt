package com.blockchain.componentlib.anim

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppTheme

enum class TargetState {
    DEFAULT,
    SUCCESS,
    FAILURE
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedStateIndicatorImage(imageResource: ImageResource, state: TargetState) {
    Box(
        modifier = Modifier.size(
            width = 100.dp,
            height = 100.dp
        )
    ) {
        Box(
            modifier = Modifier
                .background(Color.White, CircleShape)
                .size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(58.dp),
                imageResource = imageResource
            )
        }

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                scaleIn(
                    animationSpec = tween(durationMillis = 300)
                ) with fadeOut(
                    animationSpec = tween(durationMillis = 300)
                )
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        ) { targetState ->
            when (targetState) {
                TargetState.SUCCESS -> {
                    Image(
                        imageResource = Icons.Filled.Check.withTint(AppTheme.colors.success),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(
                                width = 6.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
                TargetState.FAILURE -> {
                    Image(
                        imageResource = Icons.Filled.Alert.withTint(AppTheme.colors.error),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(
                                width = 6.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.size(50.dp))
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewAnimatedStateIndicatorImage() {
    AppTheme {

        var state by remember { mutableStateOf(TargetState.DEFAULT) }
        LaunchedEffect(Unit) {
            // change the state every 1300ms
            while (true) {
                state = when (state) {
                    TargetState.DEFAULT -> TargetState.SUCCESS
                    TargetState.SUCCESS -> TargetState.FAILURE
                    TargetState.FAILURE -> TargetState.DEFAULT
                }
                kotlinx.coroutines.delay(1300)
            }
        }

        AnimatedStateIndicatorImage(
            imageResource = ImageResource.Local(R.drawable.ic_blockchain),
            state = state
        )
    }
}

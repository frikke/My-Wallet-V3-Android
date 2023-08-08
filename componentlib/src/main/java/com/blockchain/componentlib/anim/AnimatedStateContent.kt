package com.blockchain.componentlib.anim

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedStateContent(
    state: TargetState,
    defaultContent: @Composable ColumnScope.() -> Unit,
    successContent: @Composable ColumnScope.() -> Unit,
    failureContent: @Composable ColumnScope.() -> Unit
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            scaleIn(
                animationSpec = tween(
                    durationMillis = 300
                ),
                initialScale = 0.8f
            ) with fadeOut(
                animationSpec = tween(durationMillis = 100)
            )
        }
    ) { targetState ->
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (targetState) {
                TargetState.DEFAULT -> {
                    defaultContent()
                }
                TargetState.SUCCESS -> {
                    successContent()
                }
                TargetState.FAILURE -> {
                    failureContent()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAnimatedStateContent() {
    AppTheme {
        Column {
            var state by remember { mutableStateOf(TargetState.DEFAULT) }

            AnimatedStateContent(
                state = state,
                defaultContent = {
                    SimpleText(
                        text = "Default",
                        style = ComposeTypographies.Title1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre
                    )
                },
                successContent = {
                    SimpleText(
                        text = "Success",
                        style = ComposeTypographies.Title1,
                        color = ComposeColors.Success,
                        gravity = ComposeGravities.Centre
                    )
                },
                failureContent = {
                    SimpleText(
                        text = "Failure",
                        style = ComposeTypographies.Title1,
                        color = ComposeColors.Error,
                        gravity = ComposeGravities.Centre
                    )
                }
            )

            PrimaryButton(
                text = "change state", onClick = {
                    state = when (state) {
                        TargetState.DEFAULT -> {
                            TargetState.SUCCESS
                        }

                        TargetState.SUCCESS -> {
                            TargetState.FAILURE
                        }

                        TargetState.FAILURE -> {
                            TargetState.DEFAULT
                        }
                    }
                }
            )
        }
    }
}

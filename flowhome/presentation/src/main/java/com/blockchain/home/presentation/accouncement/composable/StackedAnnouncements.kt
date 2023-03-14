package com.blockchain.home.presentation.accouncement.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.swipeable.rememberSwipeableState
import com.blockchain.componentlib.swipeable.swipeable
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.announcements.Announcement
import com.blockchain.home.presentation.R
import kotlinx.coroutines.launch

@Composable
fun StackedAnnouncements(
    announcements: List<Announcement>,
    hideConfirmation: Boolean,
    onSwiped: (Announcement) -> Unit
) {
    val localDensity = LocalDensity.current

    val confirmationCardScale = 0.5F
    val backCardScale = 0.9F
    val frontCardScale = 1F
    val animatableScale = remember { Animatable(backCardScale) }

    val backCardsTranslation = with(localDensity) {
        AppTheme.dimensions.tinySpacing.toPx()
    }.unaryMinus()
    val frontCardTranslation = 0F
    val animatableTranslation = remember { Animatable(backCardsTranslation) }

    val frontCardFocusedAlpha = 1F
    val frontCardUnfocusedAlpha = 0.5F
    val animatableAlpha = remember { Animatable(frontCardFocusedAlpha) }

    var fullHeight by remember { mutableStateOf(0.dp) }

    Box {
        val scope = rememberCoroutineScope()
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .onGloballyPositioned {
                    if (fullHeight == 0.dp) {
                        with(localDensity) {
                            fullHeight = it.size.height.toDp()
                        }
                    }
                }
        ) {
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center),
                visible = !hideConfirmation,
                exit = shrinkOut(tween(durationMillis = 200, delayMillis = 200)) + fadeOut(tween(durationMillis = 200))
            ) {
                Box(
                    modifier = Modifier.height(fullHeight)
                ) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val scale = if (announcements.isNotEmpty()) {
                                    animatableScale.value
                                } else {
                                    frontCardScale
                                }
                                scaleY = scale
                                scaleX = scale
                            },
                        text = stringResource(R.string.announcements_all_done),
                        style = AppTheme.typography.title3,
                        color = AppTheme.colors.title
                    )
                }
            }

            announcements.map {
                it to rememberSwipeableState()
            }.forEachIndexed { index, (announcement, state) ->
                state.isSwipeEnabled = index == announcements.lastIndex

                AnnouncementCard(
                    modifier = Modifier
                        .padding(AppTheme.dimensions.smallSpacing)
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = when (index) {
                                // front is always 1F
                                announcements.lastIndex -> frontCardScale
                                // second should animate
                                announcements.lastIndex - 1 -> animatableScale.value
                                // all others are 0.9F
                                else -> backCardScale
                            }
                            scaleY = scale
                            scaleX = scale

                            translationY = when (index) {
                                // front is always 0F
                                announcements.lastIndex -> frontCardTranslation
                                // second should animate
                                announcements.lastIndex - 1 -> animatableTranslation.value
                                // all others are AppTheme.dimensions.tinySpacing
                                else -> backCardsTranslation
                            }
                        }
                        .swipeable(
                            state = state,
                            onDrag = { hasReachedDismissThreshold ->
                                // when dragging, if dismiss threshold is reached
                                // animate the next card to the front to hint that current can be dismissed
                                // or reverse it otherwise

                                val scaleTarget: Float
                                val translationTarget: Float
                                val alphaTarget: Float
                                if (hasReachedDismissThreshold) {
                                    scaleTarget = frontCardScale
                                    translationTarget = frontCardTranslation
                                    alphaTarget = frontCardUnfocusedAlpha
                                } else {
                                    scaleTarget = if (announcements.size == 1) confirmationCardScale else backCardScale
                                    translationTarget = backCardsTranslation
                                    alphaTarget = frontCardFocusedAlpha
                                }

                                scope.launch {
                                    animatableScale.animateTo(scaleTarget)
                                }
                                scope.launch {
                                    animatableTranslation.animateTo(translationTarget)
                                }
                                scope.launch {
                                    animatableAlpha.animateTo(alphaTarget)
                                }
                            },
                            onSwipe = {
                                // when starting dismiss animation
                                // animate the next card to the front
                                scope.launch {
                                    animatableScale.animateTo(frontCardScale)
                                }
                                scope.launch {
                                    animatableTranslation.animateTo(frontCardTranslation)
                                }
                            },
                            onSwipeComplete = {
                                // reset animators
                                scope.launch {
                                    animatableScale.snapTo(backCardScale)
                                }
                                scope.launch {
                                    animatableTranslation.snapTo(backCardsTranslation)
                                }
                                scope.launch {
                                    animatableAlpha.snapTo(frontCardFocusedAlpha)
                                }

                                onSwiped(announcement)
                            }
                        ),
                    title = announcement.title,
                    subtitle = announcement.description,
                    icon = announcement.imageUrl?.let {
                        StackedIcon.SingleIcon(ImageResource.Remote(it, size = 40.dp))
                    } ?: StackedIcon.None,
                    elevation = when (index) {
                        announcements.lastIndex,
                        announcements.lastIndex - 1 -> AppTheme.dimensions.mediumElevation
                        else -> 0.dp
                    },
                    contentAlphaProvider = {
                        when (index) {
                            // front is animatableAlpha
                            announcements.lastIndex -> animatableAlpha.value
                            // all others are 1F
                            else -> 1F
                        }
                    },
                    onClick = {}
                )
            }
        }
    }
}

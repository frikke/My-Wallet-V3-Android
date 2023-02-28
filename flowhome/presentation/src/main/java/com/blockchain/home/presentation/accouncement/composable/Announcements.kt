package com.blockchain.home.presentation.accouncement.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Unlock
import com.blockchain.componentlib.swipeable.rememberSwipeableState
import com.blockchain.componentlib.swipeable.swipeable
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Pink600
import kotlinx.coroutines.launch

data class AnnouncementTbd(
    val id: Int,
    val title: String
)

@Composable
fun Announcements(
    announcements: List<AnnouncementTbd>,
    onSwiped: (Int) -> Unit
) {
    val backCardScale = 0.9F
    val frontCardScale = 1F
    val animatableScale = remember { Animatable(backCardScale) }

    val backCardsTranslation = with(LocalDensity.current) {
        AppTheme.dimensions.tinySpacing.toPx()
    }.unaryMinus()
    val frontCardTranslation = 0F
    val animatableTranslation = remember { Animatable(backCardsTranslation) }

    Box {
        val scope = rememberCoroutineScope()
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
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
                                if (hasReachedDismissThreshold) {
                                    scaleTarget = frontCardScale
                                    translationTarget = frontCardTranslation
                                } else {
                                    scaleTarget = backCardScale
                                    translationTarget = backCardsTranslation
                                }

                                scope.launch {
                                    animatableScale.animateTo(scaleTarget)
                                }
                                scope.launch {
                                    animatableTranslation.animateTo(translationTarget)
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
                                onSwiped(announcement.id)
                            }
                        ),
                    title = announcement.title + announcement.id.toString(),
                    subtitle = "announcement.subtitle" + announcement.id.toString(),
                    icon = StackedIcon.SingleIcon(Icons.Filled.Unlock.withTint(Pink600).withSize(40.dp)),
                    elevation = when (index) {
                        announcements.lastIndex,
                        announcements.lastIndex - 1 -> AppTheme.dimensions.mediumElevation
                        else -> 0.dp
                    },
                    onClick = {}
                )
            }
        }
    }
}

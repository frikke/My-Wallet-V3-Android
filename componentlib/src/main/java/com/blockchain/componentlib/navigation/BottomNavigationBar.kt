package com.blockchain.componentlib.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark400
import com.blockchain.componentlib.theme.Dark700
import com.blockchain.componentlib.theme.Dark900
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey600

@Composable
fun BottomNavigationBar(
    navigationItems: List<NavigationItem> = listOf(
        NavigationItem.Home,
        NavigationItem.Prices,
        NavigationItem.BuyAndSell,
        NavigationItem.Activity
    ),
    onNavigationItemClick: (NavigationItem) -> Unit = {},
    hasMiddleButton: Boolean,
    onMiddleButtonClick: () -> Unit = {},
    selectedNavigationItem: NavigationItem? = null,
    bottomNavigationState: BottomNavigationState = BottomNavigationState.Add,
    isPulseAnimationEnabled: Boolean = false
) {
    val animationDuration = 300
    var isPressed by remember { mutableStateOf(false) }

    val scaling by animateFloatAsState(
        targetValue = when (isPressed) {
            true -> 0.75f
            false -> 1f
        },
        animationSpec = tween(durationMillis = animationDuration, easing = LinearEasing)
    )

    val backgroundColor: Color = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Dark900
    }

    val unselectedContentColor: Color = Grey400
    val selectedContentColor: Color = AppTheme.colors.primary

    val middleIndex = if (hasMiddleButton) navigationItems.size / 2 else -1

    val textColor = if (!isSystemInDarkTheme()) {
        Dark400
    } else {
        Grey600
    }

    val dividerColor = if (!isSystemInDarkTheme()) {
        Grey000
    } else {
        Dark700
    }

    BottomNavigation(
        backgroundColor = backgroundColor
    ) {
        Column {
            Divider(thickness = 1.dp, color = dividerColor, modifier = Modifier.fillMaxWidth())
            Box {
                if (isPulseAnimationEnabled) {
                    PulseLoading(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .wrapContentSize(unbounded = true)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    navigationItems.forEachIndexed { index, item ->
                        if (index == middleIndex) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true
                                                try {
                                                    awaitRelease()
                                                } finally {
                                                    onMiddleButtonClick()
                                                    isPressed = false
                                                }
                                            }
                                        )
                                    }
                                    .scale(scaling)
                                    .semantics(mergeDescendants = true) {},
                                contentAlignment = Alignment.Center
                            ) {
                                Crossfade(targetState = isPressed) { state ->
                                    when (state) {
                                        false -> Image(
                                            painter = painterResource(R.drawable.ic_bottom_nav_add),
                                            contentDescription = null
                                        )
                                        true -> Image(
                                            painter = painterResource(R.drawable.ic_bottom_nav_cancel),
                                            contentDescription = null
                                        )
                                    }
                                }
                                Image(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.Center),
                                    painter = painterResource(R.drawable.ic_bottom_nav_plus),
                                    contentDescription = stringResource(R.string.accessibility_action_menu),
                                    colorFilter = ColorFilter.tint(backgroundColor)
                                )
                            }
                        }

                        val selected = item == selectedNavigationItem
                        BottomNavigationItemWithIndicator(selected = selected, modifier = Modifier.weight(1f)) {
                            BottomNavigationItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(id = item.icon),
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        text = stringResource(item.title),
                                        fontSize = 10.sp,
                                        color = if (selected) {
                                            AppTheme.colors.primary
                                        } else {
                                            textColor
                                        }
                                    )
                                },
                                selectedContentColor = selectedContentColor,
                                unselectedContentColor = unselectedContentColor,
                                alwaysShowLabel = true,
                                selected = selected,
                                onClick = {
                                    onNavigationItemClick(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationItemWithIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
    BottomNavigationItem: @Composable () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (selected) {
            Divider(
                modifier = Modifier
                    .clipToBounds()
                    .absoluteOffset(y = (-2).dp)
                    .fillMaxWidth(0.6f)
                    .clip(AppTheme.shapes.small),
                thickness = dimensionResource(R.dimen.smallest_margin),
                color = AppTheme.colors.primary
            )
        }
        BottomNavigationItem()
    }
}

enum class BottomNavigationState {
    Add, Cancel
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    AppTheme {
        BottomNavigationBar(selectedNavigationItem = NavigationItem.Home, hasMiddleButton = false)
    }
}

sealed class NavigationItem(val route: String, val icon: Int, val title: Int) {
    object Home : NavigationItem("home", R.drawable.ic_bottom_nav_home, R.string.bottom_nav_home)
    object Prices : NavigationItem("prices", R.drawable.ic_bottom_nav_prices, R.string.bottom_nav_prices)
    object BuyAndSell : NavigationItem("buy_and_sell", R.drawable.ic_bottom_nav_buy, R.string.bottom_nav_buy_and_sell)
    object Activity : NavigationItem("activity", R.drawable.ic_bottom_nav_activity, R.string.bottom_nav_activity)
    object Nfts : NavigationItem("nfts", R.drawable.ic_bottom_nav_nfts, R.string.bottom_nav_nfts)
}

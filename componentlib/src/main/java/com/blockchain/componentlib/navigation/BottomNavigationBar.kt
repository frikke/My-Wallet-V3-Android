package com.blockchain.componentlib.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    onMiddleButtonClick: () -> Unit = {},
    selectedNavigationItem: NavigationItem? = null,
    bottomNavigationState: BottomNavigationState = BottomNavigationState.Add
) {
    val rotation by animateFloatAsState(
        targetValue = when (bottomNavigationState) {
            BottomNavigationState.Add -> 0f
            BottomNavigationState.Cancel -> 135f
        },
        animationSpec = tween(durationMillis = 300)
    )

    val backgroundColor: Color = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Dark900
    }

    val unselectedContentColor: Color = Grey400
    val selectedContentColor: Color = AppTheme.colors.primary

    val middleIndex = navigationItems.size / 2

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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                navigationItems.forEachIndexed { index, item ->
                    if (index == middleIndex) {
                        Box(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onMiddleButtonClick.invoke()
                            }
                        ) {
                            Crossfade(targetState = bottomNavigationState) { state ->
                                when (state) {
                                    BottomNavigationState.Add -> Image(
                                        painter = painterResource(R.drawable.ic_bottom_nav_add),
                                        contentDescription = null
                                    )
                                    BottomNavigationState.Cancel -> Image(
                                        painter = painterResource(R.drawable.ic_bottom_nav_cancel),
                                        contentDescription = null
                                    )
                                }
                            }
                            Image(
                                modifier = Modifier
                                    .height(56.dp)
                                    .width(56.dp)
                                    .padding(AppTheme.dimensions.paddingMedium)
                                    .rotate(rotation),
                                painter = painterResource(R.drawable.ic_bottom_nav_plus),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(backgroundColor)
                            )
                        }
                    }

                    val selected = item == selectedNavigationItem
                    BottomNavigationItemWithIndicator(selected = selected) {
                        BottomNavigationItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = item.icon),
                                    contentDescription = stringResource(item.title)
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

@Composable
fun BottomNavigationItemWithIndicator(
    selected: Boolean,
    BottomNavigationItem: @Composable () -> Unit
) {
    Column(modifier = Modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
        if (selected) {
            Divider(
                modifier = Modifier
                    .clipToBounds()
                    .absoluteOffset(y = (-2).dp)
                    .fillMaxWidth(0.8f)
                    .clip(AppTheme.shapes.small),
                thickness = 4.dp,
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
        BottomNavigationBar(selectedNavigationItem = NavigationItem.Home)
    }
}

sealed class NavigationItem(var route: String, var icon: Int, var title: Int) {
    object Home : NavigationItem("home", R.drawable.ic_bottom_nav_home, R.string.bottom_nav_home)
    object Prices : NavigationItem("prices", R.drawable.ic_bottom_nav_prices, R.string.bottom_nav_prices)
    object BuyAndSell : NavigationItem("buy_and_sell", R.drawable.ic_bottom_nav_buy, R.string.bottom_nav_buy_and_sell)
    object Activity : NavigationItem("activity", R.drawable.ic_bottom_nav_activity, R.string.bottom_nav_activity)
}

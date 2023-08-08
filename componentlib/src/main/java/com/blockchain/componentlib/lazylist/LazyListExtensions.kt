package com.blockchain.componentlib.lazylist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.conditional

fun <T> LazyListScope.roundedCornersItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    dividerColor: @Composable (() -> Color)? = { AppTheme.colors.background },
    animateItemPlacement: Boolean = false,
    content: @Composable (T) -> Unit
) {
    paddedRoundedCornersItems(
        items = items,
        key = key,
        dividerColor = dividerColor,
        paddingValues = { PaddingValues() },
        animateItemPlacement = animateItemPlacement,
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun <T> LazyListScope.paddedRoundedCornersItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    dividerColor: @Composable (() -> Color)? = { AppTheme.colors.background },
    paddingValues: @Composable () -> PaddingValues,
    animateItemPlacement: Boolean = false,
    content: @Composable (T) -> Unit
) {
    items(
        items = items,
        key = key
    ) {
        Box(
            modifier = Modifier
                .padding(
                    start = paddingValues().calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues().calculateEndPadding(LayoutDirection.Ltr),
                    top = if (it == items.first()) paddingValues().calculateTopPadding() else 0.dp,
                    bottom = if (it == items.last()) paddingValues().calculateBottomPadding() else 0.dp
                )
                .conditional(animateItemPlacement) {
                    animateItemPlacement()
                }
        ) {
            when {
                items.size == 1 -> {
                    Surface(
                        color = AppTheme.colors.backgroundSecondary,
                        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing)
                    ) {
                        content(it)
                    }
                }

                it == items.first() -> {
                    Surface(
                        color = AppTheme.colors.backgroundSecondary,
                        shape = RoundedCornerShape(
                            topStart = AppTheme.dimensions.mediumSpacing,
                            topEnd = AppTheme.dimensions.mediumSpacing
                        )
                    ) {
                        Column {
                            content(it)
                            dividerColor?.let {
                                Divider(color = dividerColor())
                            }
                        }
                    }
                }

                it == items.last() -> {
                    Surface(
                        color = AppTheme.colors.backgroundSecondary,
                        shape = RoundedCornerShape(
                            bottomEnd = AppTheme.dimensions.mediumSpacing,
                            bottomStart = AppTheme.dimensions.mediumSpacing
                        )
                    ) {
                        content(it)
                    }
                }

                else -> {
                    Surface(
                        color = AppTheme.colors.backgroundSecondary,
                    ) {
                        Column {
                            content(it)
                            dividerColor?.let {
                                Divider(color = dividerColor())
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.paddedItem(
    key: Any? = null,
    paddingValues: @Composable () -> PaddingValues,
    animateItemPlacement: Boolean = false,
    content: @Composable () -> Unit
) {
    item(
        key = key
    ) {
        Column(
            modifier = Modifier
                .padding(paddingValues())
                .then(
                    if (animateItemPlacement) {
                        Modifier.animateItemPlacement()
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
    }
}

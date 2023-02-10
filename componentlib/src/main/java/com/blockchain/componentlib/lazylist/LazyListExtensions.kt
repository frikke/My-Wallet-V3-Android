package com.blockchain.componentlib.lazylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted

fun <T> LazyListScope.roundedCornersItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    dividerColor: Color? = BackgroundMuted,
    content: @Composable (T) -> Unit,
) {
    paddedRoundedCornersItems(
        items = items,
        key = key,
        dividerColor = dividerColor,
        paddingValues = PaddingValues(),
        content = content
    )
}

fun <T> LazyListScope.paddedRoundedCornersItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    dividerColor: Color? = BackgroundMuted,
    paddingValues: PaddingValues,
    content: @Composable (T) -> Unit,
) {
    items(
        items = items,
        key = key,
    ) {
        Box(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            when {
                items.size == 1 -> Card(
                    backgroundColor = AppTheme.colors.background,
                    shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                    elevation = 0.dp
                ) {
                    content(it)
                }
                it == items.first() -> Card(
                    backgroundColor = AppTheme.colors.background,
                    shape = RoundedCornerShape(
                        topStart = AppTheme.dimensions.mediumSpacing,
                        topEnd = AppTheme.dimensions.mediumSpacing
                    ),
                    elevation = 0.dp
                ) {
                    content(it)
                    dividerColor?.let {
                        Divider(color = it)
                    }
                }
                it == items.last() -> Card(
                    modifier = Modifier.padding(top = 1.dp),
                    backgroundColor = AppTheme.colors.background,
                    shape = RoundedCornerShape(
                        bottomEnd = AppTheme.dimensions.mediumSpacing,
                        bottomStart = AppTheme.dimensions.mediumSpacing
                    ),
                    elevation = 0.dp
                ) {
                    content(it)
                }
                else -> {
                    content(it)
                    dividerColor?.let {
                        Divider(color = it)
                    }
                }
            }
        }
    }
}

fun LazyListScope.paddedItem(
    key: Any? = null,
    paddingValues: PaddingValues,
    content: @Composable () -> Unit,
) {
    item(
        key = key,
    ) {
        Column(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}

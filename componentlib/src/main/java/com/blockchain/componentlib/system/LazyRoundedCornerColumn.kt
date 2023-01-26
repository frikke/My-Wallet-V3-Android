package com.blockchain.componentlib.system

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun <T> LazyRoundedCornersColumn(items: List<T>, rowContent: @Composable (item: T) -> Unit) {
    LazyRoundedCornersColumnIndexed(
        items = items,
        rowContent = { item, _ ->
            rowContent(item)
        }
    )
}

@Composable
fun <T> LazyRoundedCornersColumnIndexed(items: List<T>, rowContent: @Composable (item: T, index: Int) -> Unit) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState
    ) {
        itemsIndexed(
            items = items,
            itemContent = { index, item ->
                val top = if (index == 0) {
                    AppTheme.dimensions.smallSpacing
                } else {
                    AppTheme.dimensions.noSpacing
                }
                val bottom = if (index == items.lastIndex) {
                    AppTheme.dimensions.smallSpacing
                } else {
                    AppTheme.dimensions.noSpacing
                }

                Surface(
                    shape = RoundedCornerShape(
                        topStart = top,
                        topEnd = top,
                        bottomEnd = bottom,
                        bottomStart = bottom
                    ),
                    color = Color.Transparent
                ) {
                    rowContent(item, index)
                }
            }
        )
    }
}

@Preview
@Composable
fun ColumnExample() {
    AppTheme {
        AppSurface {
            LazyRoundedCornersColumn(
                items = listOf("A", "B", "C"),
                rowContent = {
                    Text(it)
                }
            )
        }
    }
}

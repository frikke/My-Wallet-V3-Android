package com.blockchain.componentlib.lazylist

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ScrollState
data class ScrollState(
    val isTop: Boolean,
    val isBottom: Boolean,
    val shouldGetNextPage: Boolean,
)

// LazyList
val LazyListState.isLastItemVisible: Boolean
    get() = layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1

val LazyListState.isFirstItemVisible: Boolean
    get() = firstVisibleItemIndex == 0

fun LazyListState.isTriggerNextPageItemVisible(offset: Int): Boolean =
    layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - offset - 1

@Composable
fun rememberScrollState(listState: LazyListState, offset: Int = 0): ScrollState {
    val scrollState by remember {
        derivedStateOf {
            ScrollState(
                isTop = listState.isFirstItemVisible,
                isBottom = listState.isLastItemVisible,
                shouldGetNextPage = listState.isTriggerNextPageItemVisible(offset)
            )
        }
    }
    return scrollState
}

// LazyGrid
val LazyGridState.isLastItemVisible: Boolean
    get() = layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1

val LazyGridState.isFirstItemVisible: Boolean
    get() = firstVisibleItemIndex == 0

fun LazyGridState.isTriggerNextPageItemVisible(offset: Int): Boolean =
    (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= layoutInfo.totalItemsCount - offset - 1

@Composable
fun rememberScrollState(gridState: LazyGridState, offset: Int = 0): ScrollState {
    val scrollState by remember {
        derivedStateOf {
            ScrollState(
                isTop = gridState.isFirstItemVisible,
                isBottom = gridState.isLastItemVisible,
                shouldGetNextPage = gridState.isTriggerNextPageItemVisible(offset)
            )
        }
    }
    return scrollState
}

/**
 * The vertically scrolling list that only composes and lays out the currently visible items, while facilitating
 * pagination.
 * The [content] block defines a DSL which allows you to emit items of different types. For
 * example you can use [LazyListScope.item] to add a single item and [LazyListScope.items] to add
 * a list of items.
 *
 * @sample androidx.compose.foundation.samples.LazyColumnSample
 *
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the list's state.
 * @param contentPadding a padding around the whole content. This will add padding for the.
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first item or after the last one. If you want to add a spacing
 * between each item use [verticalArrangement].
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items are
 * laid out in the reverse order and [LazyListState.firstVisibleItemIndex] == 0 means
 * that column is scrolled to the bottom. Note that [reverseLayout] does not change the behavior of
 * [verticalArrangement],
 * e.g. with [Arrangement.Top] (top) 123### (bottom) becomes (top) 321### (bottom).
 * @param verticalArrangement The vertical arrangement of the layout's children. This allows
 * to add a spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param flingBehavior logic describing fling behavior.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param loadNextPageItemOffset offset (from the bottom) of the item that should trigger [onGetNextPage]
 * @param onGetNextPage lambda that will be invoked when the last item becomes visible.
 * Use this fetch the next page and update the observed item list
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items.
 */
@Composable
fun PaginatedLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    loadNextPageItemOffset: Int = 0,
    onGetNextPage: () -> Unit,
    content: LazyListScope.() -> Unit
) {

    val lazyListState = rememberLazyListState()
    val scrollState = rememberScrollState(listState = lazyListState, offset = loadNextPageItemOffset)

    if (scrollState.shouldGetNextPage) {
        onGetNextPage()
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

/**
 * A lazy vertical grid layout that facilitates pagination. It composes only visible rows of the grid.
 *
 * Sample:
 * @sample androidx.compose.foundation.samples.LazyVerticalGridSample
 *
 * Sample with custom item spans:
 * @sample androidx.compose.foundation.samples.LazyVerticalGridSpanSample
 *
 * @param columns describes the count and the size of the grid's columns,
 * see [GridCells] doc for more information
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding specify a padding around the whole content
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items will be
 * laid out in the reverse order  and [LazyGridState.firstVisibleItemIndex] == 0 means
 * that grid is scrolled to the bottom. Note that [reverseLayout] does not change the behavior of
 * [verticalArrangement],
 * e.g. with [Arrangement.Top] (top) 123### (bottom) becomes (top) 321### (bottom).
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param flingBehavior logic describing fling behavior
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param loadNextPageItemOffset offset (from the bottom) of the item that should trigger [onGetNextPage]
 * @param onGetNextPage lambda that will be run when the last item becomes visible.
 * Use this fetch the next page and update the observed item list
 * @param content the [LazyGridScope] which describes the content
 */
@Composable
fun PaginatedLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    loadNextPageItemOffset: Int = 0,
    onGetNextPage: () -> Unit,
    content: LazyGridScope.() -> Unit
) {

    val lazyGridState = rememberLazyGridState()
    val scrollState = rememberScrollState(gridState = lazyGridState, offset = loadNextPageItemOffset)

    if (scrollState.shouldGetNextPage) {
        onGetNextPage()
    }

    LazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        state = lazyGridState,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

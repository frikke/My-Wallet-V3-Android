package piuk.blockchain.android.ui.multiapp.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.SwipeRefreshState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeRefreshWithoutOverscroll(
    state: SwipeRefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    swipeEnabled: Boolean = true,
    refreshTriggerDistance: Dp = 80.dp,
    indicatorAlignment: Alignment = Alignment.TopCenter,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    indicator: @Composable (state: SwipeRefreshState, refreshTrigger: Dp) -> Unit = { s, trigger ->
        SwipeRefreshIndicator(s, trigger)
    },
    clipIndicatorToPadding: Boolean = true,
    content: @Composable () -> Unit,
) {
    SwipeRefresh(
        state = state,
        onRefresh = onRefresh,
        modifier = modifier,
        swipeEnabled = swipeEnabled,
        refreshTriggerDistance = refreshTriggerDistance,
        indicatorAlignment = indicatorAlignment,
        indicatorPadding = indicatorPadding,
        indicator = indicator,
        clipIndicatorToPadding = clipIndicatorToPadding,
    ) {
        val overscrollConfiguration = if (state.isSwipeInProgress) {
            null
        } else {
            LocalOverscrollConfiguration.current
        }
        CompositionLocalProvider(LocalOverscrollConfiguration provides overscrollConfiguration) {
            content()
        }
    }
}
package com.blockchain.componentlib.control

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.utils.clickableNoEffect
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun TabSwitcher(tabs: ImmutableList<String>, initialTabIndex: Int, onTabChanged: (Int) -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    var tabLayoutWidthPx by remember { mutableStateOf(0) }
    var currentTabIndex by remember { mutableStateOf(initialTabIndex) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.xHugeSpacing)
            .background(AppTheme.colors.medium, RoundedCornerShape(AppTheme.dimensions.largeSpacing))
            .onSizeChanged { tabLayoutWidthPx = it.width },
        contentAlignment = Alignment.CenterStart
    ) {
        if (tabLayoutWidthPx > 0) {
            val indicatorWidth = tabLayoutWidthPx / tabs.size
            val indicatorWithDp = with(LocalDensity.current) {
                indicatorWidth.toDp()
            }

            val indicatorPosition = remember(indicatorWidth) {
                Animatable(
                    (currentTabIndex * (tabLayoutWidthPx / tabs.size)).toFloat()
                )
            }

            Card(
                modifier = Modifier
                    .width(width = indicatorWithDp)
                    .fillMaxHeight()
                    .graphicsLayer { translationX = indicatorPosition.value }
                    .padding(AppTheme.dimensions.composeSmallestSpacing),
                shape = RoundedCornerShape(AppTheme.dimensions.largeSpacing),
                backgroundColor = AppColors.backgroundSecondary,
                elevation = 2.dp
            ) {
                /* This is empty since we are just using it to have a white composable that we can animate
                to display the indicator*/
            }

            Row(Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, tab ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickableNoEffect {
                                coroutineScope.launch {
                                    currentTabIndex = index
                                    indicatorPosition.animateTo(index * indicatorWidth.toFloat())
                                }
                                onTabChanged(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SimpleText(
                            text = tab,
                            style = ComposeTypographies.Paragraph2,
                            color = if (currentTabIndex == index) {
                                ComposeColors.Title
                            } else {
                                ComposeColors.Body
                            },
                            gravity = ComposeGravities.Centre
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Preview
@Composable
fun TabSwitcherLayoutPreview() {
    // Start in Interactive Mode to see it in the preview window

    val pagerState = rememberPagerState()
    LaunchedEffect(Unit) {
        pagerState.scrollToPage(0)
    }
    TabSwitcher(
        initialTabIndex = 0,
        tabs = persistentListOf("Tab 1", "Tab 2")
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TabSwitcherLayoutPreviewDark() {
    TabSwitcherLayoutPreview()
}

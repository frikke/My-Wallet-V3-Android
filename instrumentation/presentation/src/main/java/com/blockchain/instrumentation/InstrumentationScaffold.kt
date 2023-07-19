@file:OptIn(ExperimentalMaterialApi::class)

package com.blockchain.instrumentation

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import java.util.UUID

@Composable
fun InstrumentationScaffold(content: @Composable () -> Unit) {
    Box {
        val items by InstrumentationQueue.queue.collectAsStateLifecycleAware()
        var isPanelOpen by remember { mutableStateOf(false) }

        content()

        if (items.isNotEmpty()) {
            if (isPanelOpen) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .clickable {
                            isPanelOpen = false
                        }
                )
            }

            Panel(
                isPanelOpen = isPanelOpen,
                items = items,
                panelClicked = { isPanelOpen = !isPanelOpen },
                skipClicked = { requestId ->
                    InstrumentationQueue.pickResponse(requestId, null)
                },
                responseClicked = { requestId, pickedResponse ->
                    InstrumentationQueue.pickResponse(requestId, pickedResponse)
                }
            )
        }
    }
}

@Composable
private fun BoxScope.Panel(
    isPanelOpen: Boolean,
    items: List<InstrumentationQueue.Item>,
    panelClicked: () -> Unit,
    skipClicked: (UUID) -> Unit,
    responseClicked: (UUID, InstrumentedResponse) -> Unit
) {
    val configuration = LocalConfiguration.current
    val panelWidth = configuration.screenWidthDp.dp / 2
    val panelHeight = configuration.screenHeightDp * 0.75

    Row(
        modifier = Modifier
            .align(Alignment.CenterEnd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fabColor by rememberInfiniteTransition().animateColor(
            initialValue = AppColors.backgroundSecondary,
            targetValue = AppColors.primary,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        FloatingActionButton(
            modifier = Modifier
                .padding(end = AppTheme.dimensions.smallSpacing)
                .size(40.dp),
            onClick = panelClicked,
            backgroundColor = fabColor
        ) {
            val icon = if (isPanelOpen) com.blockchain.componentlib.R.drawable.ic_close else R.drawable.ic_menu_open
            Icon(painterResource(id = icon), contentDescription = null, tint = Color.Black)
        }

        if (isPanelOpen) {
            Card(
                modifier = Modifier.size(width = panelWidth, height = panelHeight.dp),
                elevation = 3.dp
            ) {
                Column {
                    SimpleText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppTheme.dimensions.standardSpacing),
                        text = "Pending requests:",
                        style = ComposeTypographies.Subheading,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre
                    )

                    LazyColumn {
                        itemsIndexed(items) { index, item ->
                            Request(
                                item = item,
                                index = index,
                                skipClicked = { skipClicked(item.requestId) },
                                responseClicked = { responseClicked(item.requestId, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Request(
    item: InstrumentationQueue.Item,
    index: Int,
    skipClicked: () -> Unit,
    responseClicked: (InstrumentedResponse) -> Unit
) {
    val background = if (index % 2 == 0) AppColors.medium else AppColors.backgroundSecondary
    Column(
        modifier = Modifier
            .background(background)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleText(
                text = item.url,
                style = ComposeTypographies.ParagraphMono,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }

        LazyVerticalGrid(
            modifier = Modifier.heightIn(0.dp, 1000.dp), // workaround because we can't nest lazycolumns
            columns = GridCells.Fixed(2)
        ) {
            if (item.canPassThrough) {
                item {
                    Response("network", AppColors.body, skipClicked)
                }
            }
            items(item.responses) {
                Response(it.key.lowercase()) {
                    responseClicked(it)
                }
            }
        }
    }
}

@Composable
private fun Response(
    label: String,
    backgroundColor: Color = Color(0XFF20242C),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.composeSmallestSpacing)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(all = AppTheme.dimensions.smallestSpacing)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(), text = label, textAlign = TextAlign.Center,
            color = AppColors.backgroundSecondary
        )
    }
}

@Preview(heightDp = 830)
@Composable
fun PreviewScaffold() {
    InstrumentationScaffold {
        Box(
            Modifier
                .fillMaxSize()
                .background(AppColors.backgroundSecondary)
        ) {
            SimpleText(
                text = "Lorem ispum Lorem ispum Lorem ispum Lorem ispum Lorem ispum",
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 830)
@Composable
private fun PreviewPanel() {
    val items = listOf(
        InstrumentationQueue.Item(
            requestId = UUID.randomUUID(),
            url = "nabu-gateway/payments",
            canPassThrough = false,
            responses = listOf(
                InstrumentedResponse.Json("SUCCESS", 200, "{}"),
                InstrumentedResponse.Json("SUCCESS_EMPTY", 200, "{}"),
                InstrumentedResponse.Json("SUCCESS_DATA", 200, "{}"),
                InstrumentedResponse.Json("FAILURE", 200, "{}")
            )
        ),
        InstrumentationQueue.Item(
            requestId = UUID.randomUUID(),
            url = "nabu-gateway/accounts/USD",
            canPassThrough = true,
            responses = listOf(
                InstrumentedResponse.Json("SUCCESS", 200, "{}"),
                InstrumentedResponse.Json("SUCCESS_EMPTY", 200, "{}"),
                InstrumentedResponse.Json("SUCCESS_DATA", 200, "{}"),
                InstrumentedResponse.Json("FAILURE", 200, "{}")
            )
        )
    )
    Box {
        Panel(
            isPanelOpen = true,
            items = items,
            panelClicked = {},
            skipClicked = {},
            responseClicked = { _, _ -> }
        )
    }
}

@Preview
@Composable
private fun PreviewRequest() {
    Request(
        item = InstrumentationQueue.Item(
            requestId = UUID.randomUUID(),
            url = "nabu-gateway/accounts/USD",
            canPassThrough = true,
            responses = listOf(
                InstrumentedResponse.Json("SUCCESS", 200, "{}"),
                InstrumentedResponse.Json("SUCCESS_EMPTY", 200, "{}"),
                InstrumentedResponse.Json("SUCCESS_DATA", 200, "{}"),
                InstrumentedResponse.Json("FAILURE", 200, "{}")
            )
        ),
        index = 0,
        skipClicked = {},
        responseClicked = {}
    )
}

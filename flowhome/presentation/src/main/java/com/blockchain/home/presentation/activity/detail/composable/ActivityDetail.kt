package com.blockchain.home.presentation.activity.detail.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.sheets.SheetFloatingHeader
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponentItem
import com.blockchain.home.presentation.activity.common.ActivitySectionCard
import com.blockchain.home.presentation.activity.common.ClickAction
import com.blockchain.home.presentation.activity.common.toStackedIcon
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.home.presentation.activity.detail.ActivityDetailIntent
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewModel
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewState
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ActivityDetail(
    viewModel: ActivityDetailViewModel,
    onCloseClick: () -> Unit
) {
    val viewState: ActivityDetailViewState? by viewModel.viewState.collectAsStateLifecycleAware(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ActivityDetailIntent.LoadActivityDetail)
        onDispose { }
    }

    ActivityDetailScreen(
        activityDetail = viewState?.activityDetail ?: DataResource.Loading,
        onComponentClick = { clickAction ->
            viewModel.onIntent(ActivityDetailIntent.ComponentClicked(clickAction))
        },
        onCloseClick = onCloseClick
    )
}

@Composable
fun ActivityDetailScreen(
    activityDetail: DataResource<ActivityDetail>,
    onComponentClick: ((ClickAction) -> Unit),
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0XFFF1F2F7))
    ) {
        SheetFloatingHeader(
            icon = if (activityDetail is DataResource.Data) {
                activityDetail.data.icon.toStackedIcon()
            } else {
                StackedIcon.None
            },
            title = if (activityDetail is DataResource.Data) {
                activityDetail.data.title
            } else {
                ""
            },
            onCloseClick = onCloseClick
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            when (activityDetail) {
                is DataResource.Loading -> {
                    ShimmerLoadingCard()
                }
                is DataResource.Error -> {
                    // todo
                }
                is DataResource.Data -> {
                    ActivityDetailData(
                        activityDetail = activityDetail.data,
                        onComponentClick = onComponentClick
                    )
                }
            }
        }
    }
}

/**
 * draw the list of cards (multiple groups) based on [ActivityDetail.detailItems]
 * and then draw the list of actions
 */
@Composable
fun ActivityDetailData(
    activityDetail: ActivityDetail,
    onComponentClick: ((ClickAction) -> Unit)
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        activityDetail.detailItems.forEach { sectionItems ->
            item {
                ActivitySectionCard(
                    components = sectionItems.itemGroup,
                    onClick = onComponentClick
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
            }
        }

        activityDetail.floatingActions.forEach { item ->
            item {
                ActivityComponentItem(component = item, onClick = { })
            }

            item {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
            }
        }
    }
}

@Preview
@Composable
fun PreviewActivityScreen() {
    ActivityDetailScreen(
        activityDetail = DETAIL_DUMMY_DATA,
        onComponentClick = {},
        onCloseClick = {}
    )
}

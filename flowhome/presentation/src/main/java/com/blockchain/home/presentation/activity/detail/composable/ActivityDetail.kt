package com.blockchain.home.presentation.activity.detail.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityComponentItem
import com.blockchain.home.presentation.activity.common.ActivitySectionCard
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.home.presentation.activity.detail.ActivityDetailIntent
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewModel
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewState
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun ActivityDetail(
    viewModel: ActivityDetailViewModel = getViewModel(scope = payloadScope)
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: ActivityDetailViewState? by stateFlowLifecycleAware.collectAsState(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ActivityDetailIntent.LoadActivityDetail)
        onDispose { }
    }

    ActivityDetailScreen(
        activityDetail = viewState?.activityDetailItems ?: DataResource.Loading
    )
}

@Composable
fun ActivityDetailScreen(
    activityDetail: DataResource<ActivityDetail>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0XFFF1F2F7))
    ) {
        NavigationBar(
            title = stringResource(R.string.ma_home_activity_title),
            onBackButtonClick = { },
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
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityDetailData(
    activityDetail: ActivityDetail
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        activityDetail.itemGroups.forEach { sectionItems ->
            item {
                ActivitySectionCard(components = sectionItems)
            }

            item {
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
        activityDetail = DETAIL_DUMMY_DATA
    )
}

package com.blockchain.home.presentation.activity.detail.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.detail.ActivityDetailIntent
import com.blockchain.home.presentation.activity.detail.ActivityDetailItemState
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewModel
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewState
import com.blockchain.home.presentation.activity.detail.ValueStyle
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

    ActivityScreen(
        activity = viewState?.activity ?: DataResource.Loading
    )
}

@Composable
fun ActivityScreen(
    activity: DataResource<List<List<ActivityDetailItemState>>>
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
            when (activity) {
                is DataResource.Loading -> {
                    ShimmerLoadingCard()
                }
                is DataResource.Error -> {
                    // todo
                }
                is DataResource.Data -> {
                    ActivityDetailData(
                        activity = activity.data,
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityDetailData(
    activity: List<List<ActivityDetailItemState>>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        activity.forEach { sectionItems ->
            item {
                ActivityDetailSection(sectionItems)
            }

            if (sectionItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
                }
            }
        }
    }
}

@Composable
fun ActivityDetailSection(
    sectionItems: List<ActivityDetailItemState>
) {
    if (sectionItems.isNotEmpty()) {
        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 0.dp
        ) {
            Column {
                sectionItems.forEachIndexed { index, item ->
                    when(item){
                        is ActivityDetailItemState.Button -> {

                        }
                        is ActivityDetailItemState.KeyValue -> {
                            ActivityDetailKeyValue(data = item)
                        }
                    }

                    if (index < sectionItems.lastIndex) {
                        Divider(color = Color(0XFFF1F2F7))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewActivityScreen() {
    ActivityScreen(
        activity = DataResource.Data(
            listOf(
                listOf(
                    ActivityDetailItemState.KeyValue(
                        "Purchase",
                        "100.00",
                        ValueStyle.Text
                    ),
                    ActivityDetailItemState.KeyValue(
                        "BTC Price",
                        "34,183.91",
                        ValueStyle.Text
                    ),
                    ActivityDetailItemState.KeyValue(
                        "Fees",
                        "Free",
                        ValueStyle.GreenText
                    )
                ),
                listOf(
                    ActivityDetailItemState.KeyValue(
                        "Status",
                        "Complete",
                        ValueStyle.SuccessBadge
                    ),
                    ActivityDetailItemState.KeyValue(
                        "Type",
                        "Easy Bank Transfer",
                        ValueStyle.Text
                    )
                ),
            )
        )
    )
}
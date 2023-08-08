package com.blockchain.home.presentation.activity.detail.composable

import android.content.res.Configuration
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.sheets.SheetFloatingHeader
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.CopyText
import com.blockchain.componentlib.utils.OpenUrl
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.toStackedIcon
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponentItem
import com.blockchain.home.presentation.activity.common.ActivitySectionCard
import com.blockchain.home.presentation.activity.common.ClickAction
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.home.presentation.activity.detail.ActivityDetailIntent
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewState
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailViewModel
import com.blockchain.home.presentation.activity.detail.privatekey.PrivateKeyActivityDetailViewModel
import com.blockchain.koin.payloadScope
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ActivityDetail(
    selectedTxId: String,
    walletMode: WalletMode,
    onCloseClick: () -> Unit
) {
    when (walletMode) {
        WalletMode.CUSTODIAL -> CustodialActivityDetail(
            viewModel = getViewModel(
                scope = payloadScope,
                key = selectedTxId,
                parameters = { parametersOf(selectedTxId) }
            ),
            onCloseClick = onCloseClick
        )
        WalletMode.NON_CUSTODIAL -> PrivateKeyActivityDetail(
            viewModel = getViewModel(
                scope = payloadScope,
                key = selectedTxId,
                parameters = { parametersOf(selectedTxId) }
            ),
            onCloseClick = onCloseClick
        )
        else -> error("unsupported")
    }
}

@Composable
fun CustodialActivityDetail(
    viewModel: CustodialActivityDetailViewModel,
    onCloseClick: () -> Unit
) {
    val viewState: ActivityDetailViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ActivityDetailIntent.LoadActivityDetail())
        onDispose { }
    }

    ActivityDetailScreen(
        activityDetail = viewState.activityDetail,
        onCloseClick = onCloseClick
    )
}

@Composable
fun PrivateKeyActivityDetail(
    viewModel: PrivateKeyActivityDetailViewModel,
    onCloseClick: () -> Unit
) {
    val viewState: ActivityDetailViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(ActivityDetailIntent.LoadActivityDetail())
        onDispose { }
    }

    ActivityDetailScreen(
        activityDetail = viewState.activityDetail,
        onCloseClick = onCloseClick
    )
}

@Composable
fun ActivityDetailScreen(
    activityDetail: DataResource<ActivityDetail>,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppColors.background)
    ) {
        SheetFloatingHeader(
            icon = if (activityDetail is DataResource.Data) {
                activityDetail.data.icon.toStackedIcon()
            } else {
                StackedIcon.None
            },
            title = if (activityDetail is DataResource.Data) {
                activityDetail.data.title.value()
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
                        activityDetail = activityDetail.data
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
    activityDetail: ActivityDetail
) {
    var clickAction: ClickAction by remember {
        mutableStateOf(ClickAction.None)
    }
    ComponentAction(clickAction)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        activityDetail.detailItems.forEach { sectionItems ->
            item {
                ActivitySectionCard(
                    components = sectionItems.itemGroup,
                    onClick = { clickAction = it }
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
            }
        }

        activityDetail.floatingActions.forEach { item ->
            item {
                ActivityComponentItem(
                    component = item,
                    onClick = { clickAction = it }
                )
            }

            item {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))
            }
        }
    }
}

@Composable
fun ComponentAction(clickAction: ClickAction) {
    when (clickAction) {
        is ClickAction.Button -> {
            when (clickAction.action.type) {
                ActivityButtonAction.ActivityButtonActionType.Copy -> CopyText(
                    textToCopy = clickAction.action.data
                )
                ActivityButtonAction.ActivityButtonActionType.OpenUrl -> OpenUrl(
                    url = clickAction.action.data
                )
            }
        }
        is ClickAction.Stack -> {
            // n/a nothing expected for now
        }
        ClickAction.None -> {
            // n/a
        }
    }
}

@Preview
@Composable
fun PreviewActivityScreen() {
    ActivityDetailScreen(
        activityDetail = DETAIL_DUMMY_DATA,
        onCloseClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewActivityScreenDark() {
    PreviewActivityScreen()
}

@Preview
@Composable
fun PreviewActivityScreenLoading() {
    ActivityDetailScreen(
        activityDetail = DataResource.Loading,
        onCloseClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewActivityScreenLoadingDark() {
    PreviewActivityScreenLoading()
}

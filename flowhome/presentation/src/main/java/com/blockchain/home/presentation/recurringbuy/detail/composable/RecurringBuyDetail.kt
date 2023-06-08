package com.blockchain.home.presentation.recurringbuy.detail.composable

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Sync
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.sheets.SheetFloatingHeader
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.recurringbuy.RecurringBuysAnalyticsEvents
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuyDetail
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuyDetailViewState
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuysDetailIntent
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuysDetailNavEvent
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuysDetailViewModel
import com.blockchain.koin.payloadScope
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RecurringBuyDetail(
    recurringBuyId: String,
    viewModel: RecurringBuysDetailViewModel = getViewModel(
        scope = payloadScope,
        key = recurringBuyId,
        parameters = { parametersOf(recurringBuyId) }
    ),
    onCloseClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    LaunchedEffect(key1 = viewModel) {
        navEventsFlowLifecycleAware.collectLatest {
            when (it) {
                RecurringBuysDetailNavEvent.Close -> onCloseClick()
            }
        }
    }

    val viewState: RecurringBuyDetailViewState by viewModel.viewState.collectAsStateLifecycleAware()
    LaunchedEffect(key1 = viewModel) {
        viewModel.onIntent(RecurringBuysDetailIntent.LoadRecurringBuy())
    }

    RecurringBuyDetailScreen(
        recurringBuy = viewState.detail,
        cancelationInProgress = viewState.cancelationInProgress,
        removeOnClick = {
            viewModel.onIntent(RecurringBuysDetailIntent.CancelRecurringBuy)
        },
        closeOnClick = onCloseClick
    )
}

@Composable
private fun RecurringBuyDetailScreen(
    recurringBuy: DataResource<RecurringBuyDetail>,
    cancelationInProgress: Boolean,
    removeOnClick: () -> Unit,
    closeOnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        SheetFloatingHeader(
            icon = if (recurringBuy is DataResource.Data) {
                StackedIcon.SmallTag(
                    main = ImageResource.Remote(recurringBuy.data.iconUrl),
                    tag = Icons.Filled.Sync
                )
            } else {
                StackedIcon.None
            },
            title = stringResource(com.blockchain.stringResources.R.string.coinview_rb_card_title),
            onCloseClick = closeOnClick
        )

        when (recurringBuy) {
            DataResource.Loading -> {
                ShimmerLoadingCard(showEndBlocks = false)
            }

            is DataResource.Error -> {
                closeOnClick()
            }

            is DataResource.Data -> {
                RecurringBuyDetailData(
                    recurringBuy = recurringBuy.data,
                    cancelationInProgress = cancelationInProgress,
                    removeOnClick = removeOnClick
                )
            }
        }
    }
}

@Composable
private fun RecurringBuyDetailData(
    analytics: Analytics = get(),
    recurringBuy: RecurringBuyDetail,
    cancelationInProgress: Boolean,
    removeOnClick: () -> Unit
) {
    LaunchedEffect(null) {
        analytics.logEvent(RecurringBuysAnalyticsEvents.DetailViewed(recurringBuy.assetTicker))
    }

    val data = recurringBuy.build().toList()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1F)) {
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = AppTheme.dimensions.smallSpacing,
                    vertical = AppTheme.dimensions.standardSpacing
                )
            ) {
                roundedCornersItems(
                    items = data,
                    content = { (key, value) ->
                        RecurringBuyDetailItem(
                            key = key,
                            value = value
                        )
                    }
                )
            }
        }

        MinimalButton(
            modifier = Modifier
                .padding(AppTheme.dimensions.standardSpacing)
                .fillMaxWidth(),
            text = stringResource(com.blockchain.stringResources.R.string.common_remove),
            textColor = AppTheme.colors.error,
            onClick = {
                removeOnClick()
                analytics.logEvent(RecurringBuysAnalyticsEvents.CancelClicked(recurringBuy.assetTicker))
            },
            state = if (cancelationInProgress) ButtonState.Loading else ButtonState.Enabled
        )
    }
}

@Composable
private fun RecurringBuyDetailItem(
    key: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundSecondary)
            .padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.body
        )
        Spacer(modifier = Modifier.weight(1F))
        Text(
            text = value,
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.title
        )
    }
}

@Composable
private fun RecurringBuyDetail.build(): Map<String, String> {
    return mutableMapOf(
        stringResource(com.blockchain.stringResources.R.string.amount) to amount,
        stringResource(com.blockchain.stringResources.R.string.common_crypto) to assetName,
        stringResource(com.blockchain.stringResources.R.string.payment_method) to paymentMethod,
        stringResource(com.blockchain.stringResources.R.string.recurring_buy_frequency_label_1) to frequency.value(),
        stringResource(com.blockchain.stringResources.R.string.recurring_buy_info_purchase_label_1) to nextBuy
    )
}

@Preview
@Composable
private fun PreviewRecurringBuyDetailData() {
    RecurringBuyDetailData(
        analytics = previewAnalytics,
        recurringBuy = RecurringBuyDetail(
            iconUrl = "", amount = "10.00", assetName = "Ethereum", assetTicker = "ETH", paymentMethod = "Euro",
            frequency = TextValue.StringValue("daily around 1AP"), nextBuy = "Tomorrow"
        ),
        cancelationInProgress = true,
        removeOnClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRecurringBuyDetailDataDark() {
    PreviewRecurringBuyDetailData()
}

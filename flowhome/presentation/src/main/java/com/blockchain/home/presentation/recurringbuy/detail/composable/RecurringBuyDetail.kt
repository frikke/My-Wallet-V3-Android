package com.blockchain.home.presentation.recurringbuy.detail.composable

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Sync
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.sheets.SheetFloatingHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuyDetail
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuyDetailViewState
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuysDetailIntent
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuysDetailViewModel
import com.blockchain.koin.payloadScope
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
    val viewState: RecurringBuyDetailViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(RecurringBuysDetailIntent.LoadRecurringBuy())
        onDispose { }
    }

    RecurringBuyDetailScreen(
        recurringBuy = viewState.detail,
        onCloseClick = onCloseClick
    )
}

@Composable
private fun RecurringBuyDetailScreen(
    recurringBuy: DataResource<RecurringBuyDetail>,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundMuted)
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
            title = stringResource(R.string.coinview_rb_card_title),
            onCloseClick = onCloseClick
        )

        when (recurringBuy) {
            DataResource.Loading -> {
            }
            is DataResource.Error -> {
            }
            is DataResource.Data -> {
                RecurringBuyDetailData(recurringBuy = recurringBuy.data)
            }
        }
    }
}

@Composable
private fun RecurringBuyDetailData(
    recurringBuy: RecurringBuyDetail
) {
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

        TertiaryButton(
            modifier = Modifier
                .padding(AppTheme.dimensions.standardSpacing)
                .fillMaxWidth(),
            text = stringResource(R.string.common_remove),
            textColor = AppTheme.colors.error,
            onClick = {},
            state = ButtonState.Enabled
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
            .background(AppTheme.colors.background)
            .padding(AppTheme.dimensions.smallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.body,
        )
        Spacer(modifier = Modifier.weight(1F))
        Text(
            text = value,
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.title,
        )
    }
}

@Composable
private fun RecurringBuyDetail.build(): Map<String, String> {
    return mutableMapOf(
        stringResource(R.string.amount) to amount,
        stringResource(R.string.common_crypto) to assetName,
        stringResource(R.string.payment_method) to paymentMethod,
        stringResource(R.string.recurring_buy_frequency_label_1) to frequency.value(),
        stringResource(R.string.recurring_buy_info_purchase_label_1) to nextBuy,
    )
}
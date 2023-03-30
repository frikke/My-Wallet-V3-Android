package com.blockchain.home.presentation.recurringbuy.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.data.toImmutableList
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.recurringbuy.RecurringBuyViewState
import com.blockchain.home.presentation.recurringbuy.RecurringBuysIntent
import com.blockchain.home.presentation.recurringbuy.RecurringBuysViewModel
import com.blockchain.home.presentation.recurringbuy.RecurringBuysViewState
import com.blockchain.koin.payloadScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.getViewModel

@Composable
fun RecurringBuyDashboard(
    viewModel: RecurringBuysViewModel = getViewModel(scope = payloadScope),
    onBackPressed: () -> Unit
) {
    val viewState: RecurringBuysViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(RecurringBuysIntent.LoadRecurringBuys(SectionSize.All))
        onDispose { }
    }

    RecurringBuyDashboardScreen(
        recurringBuys = viewState.recurringBuys.toImmutableList(),
        onBackPressed = onBackPressed
    )
}

@Composable
fun RecurringBuyDashboardScreen(
    recurringBuys: DataResource<ImmutableList<RecurringBuyViewState>>,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundMuted)
    ) {
        NavigationBar(
            title = stringResource(R.string.recurring_buy_toolbar),
            onBackButtonClick = onBackPressed,
        )

        when (recurringBuys) {
            DataResource.Loading -> {
                ShimmerLoadingCard(showEndBlocks = false)
            }
            is DataResource.Error -> {
            }
            is DataResource.Data -> {
                RecurringBuyDashboardData(
                    recurringBuys = recurringBuys.data
                )
            }
        }
    }
}

@Composable
fun RecurringBuyDashboardData(
    recurringBuys: ImmutableList<RecurringBuyViewState>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1F)) {
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = AppTheme.dimensions.smallSpacing,
                    vertical = AppTheme.dimensions.standardSpacing
                )
            ) {
                roundedCornersItems(
                    items = recurringBuys,
                    content = { recurringBuy ->
                        RecurringBuyTableRow(
                            description = recurringBuy.description.value(),
                            status = recurringBuy.status.value(),
                            iconUrl = recurringBuy.iconUrl,
                            onClick = {}
                        )
                    }
                )
            }
        }

        PrimaryButton(
            modifier = Modifier
                .padding(AppTheme.dimensions.standardSpacing)
                .fillMaxWidth(),
            text = stringResource(R.string.recurring_buy_add),
            onClick = { },
            state = ButtonState.Enabled
        )
    }
}

@Composable
fun RecurringBuyTableRow(
    description: String,
    status: String,
    iconUrl: String,
    onClick: () -> Unit
) {
    DefaultTableRow(
        primaryText = description,
        secondaryText = status,
        startImageResource = ImageResource.Remote(iconUrl),
        onClick = onClick
    )
}

@Preview
@Composable
fun PreviewRecurringBuyDashboardData() {
    RecurringBuyDashboardData(
        recurringBuys = persistentListOf(
            RecurringBuyViewState(
                id = "1",
                iconUrl = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png?1547033579",
                description = TextValue.StringValue("20 every Tuesday"),
                status = TextValue.StringValue("Next buy on Tue, March 18"),
            ),
            RecurringBuyViewState(
                id = "2",
                iconUrl = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png?1547033579",
                description = TextValue.StringValue("20 every Tuesday"),
                status = TextValue.StringValue("Next buy on Tue, March 18"),
            ),
            RecurringBuyViewState(
                id = "3",
                iconUrl = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png?1547033579",
                description = TextValue.StringValue("20 every Tuesday"),
                status = TextValue.StringValue("Next buy on Tue, March 18"),
            )
        )
    )
}
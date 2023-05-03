package com.blockchain.home.presentation.recurringbuy.list.composable

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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blockchain.analytics.Analytics
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
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.recurringbuy.RecurringBuysAnalyticsEvents
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuyEligibleState
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuyViewState
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysIntent
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysViewModel
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysViewState
import com.blockchain.koin.payloadScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun RecurringBuyDashboard(
    viewModel: RecurringBuysViewModel = getViewModel(scope = payloadScope),
    assetActionsNavigation: AssetActionsNavigation,
    openRecurringBuyDetail: (id: String) -> Unit,
    onBackPressed: () -> Unit
) {
    val viewState: RecurringBuysViewState by viewModel.viewState.collectAsStateLifecycleAware()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(RecurringBuysIntent.LoadRecurringBuys(SectionSize.All))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    RecurringBuyDashboardScreen(
        recurringBuys = viewState.recurringBuys,
        openRecurringBuyDetail = openRecurringBuyDetail,
        addOnClick = {
            assetActionsNavigation.buyCryptoWithRecurringBuy()
        },
        onBackPressed = onBackPressed
    )
}

@Composable
fun RecurringBuyDashboardScreen(
    recurringBuys: DataResource<RecurringBuyEligibleState>,
    openRecurringBuyDetail: (id: String) -> Unit,
    addOnClick: () -> Unit,
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
                // todo error state
            }
            is DataResource.Data -> {
                (recurringBuys.data as? RecurringBuyEligibleState.Eligible)?.let {
                    RecurringBuyDashboardData(
                        recurringBuys = it.recurringBuys.toImmutableList(),
                        openRecurringBuyDetail = openRecurringBuyDetail,
                        addOnClick = addOnClick
                    )
                } // todo ?: error state
            }
        }
    }
}

@Composable
fun RecurringBuyDashboardData(
    analytics: Analytics = get(),
    recurringBuys: ImmutableList<RecurringBuyViewState>,
    openRecurringBuyDetail: (id: String) -> Unit,
    addOnClick: () -> Unit,
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
                            onClick = {
                                openRecurringBuyDetail(recurringBuy.id)
                                analytics.logEvent(
                                    RecurringBuysAnalyticsEvents.DashboardDetailClicked(recurringBuy.assetTicker)
                                )
                            }
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
            onClick = {
                addOnClick()
                analytics.logEvent(RecurringBuysAnalyticsEvents.DashboardAddClicked)
            },
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
        startImageResource = ImageResource.Remote(url = iconUrl, size = AppTheme.dimensions.standardSpacing),
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
                assetTicker = "",
                iconUrl = "",
                description = TextValue.StringValue("20 every Tuesday"),
                status = TextValue.StringValue("Next buy on Tue, March 18"),
            ),
            RecurringBuyViewState(
                id = "2",
                assetTicker = "",
                iconUrl = "",
                description = TextValue.StringValue("20 every Tuesday"),
                status = TextValue.StringValue("Next buy on Tue, March 18"),
            ),
            RecurringBuyViewState(
                id = "3",
                assetTicker = "",
                iconUrl = "",
                description = TextValue.StringValue("20 every Tuesday"),
                status = TextValue.StringValue("Next buy on Tue, March 18"),
            )
        ),
        openRecurringBuyDetail = {},
        addOnClick = {}
    )
}

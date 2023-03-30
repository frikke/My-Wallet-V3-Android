package com.blockchain.home.presentation.recurringbuy.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.value
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.recurringbuy.RecurringBuyViewState
import com.blockchain.home.presentation.recurringbuy.RecurringBuysIntent
import com.blockchain.home.presentation.recurringbuy.RecurringBuysViewModel
import com.blockchain.home.presentation.recurringbuy.RecurringBuysViewState
import com.blockchain.koin.payloadScope
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
        recurringBuys = viewState.recurringBuys,
        onBackPressed = onBackPressed
    )
}

@Composable
fun RecurringBuyDashboardScreen(
    recurringBuys: DataResource<List<RecurringBuyViewState>>,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0XFFF1F2F7))
    ) {
        NavigationBar(
            title = stringResource(R.string.recurring_buy_toolbar),
            onBackButtonClick = onBackPressed,
        )

        when (recurringBuys) {
            DataResource.Loading -> {
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
    recurringBuys: List<RecurringBuyViewState>
) {
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
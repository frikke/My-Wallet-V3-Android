package piuk.blockchain.android.ui.prices.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.Search
import com.blockchain.componentlib.filter.FilterState
import com.blockchain.componentlib.filter.LabeledFilterState
import com.blockchain.componentlib.filter.LabeledFiltersGroup
import com.blockchain.componentlib.theme.AppTheme
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.prices.presentation.composables.PriceListItem
import piuk.blockchain.android.ui.prices.presentation.composables.PriceScreenLoading
import piuk.blockchain.android.ui.prices.presentation.composables.PricesScreenError

@Composable
fun PricesScreen(
    viewState: PricesViewState,
    retryAction: () -> Unit,
    filterAction: (PricesFilter) -> Unit,
    pricesItemClicked: (AssetInfo) -> Unit,
    filterData: (String) -> Unit,
) {
    with(viewState) {
        when {
            isLoading -> PriceScreenLoading()
            isError -> PricesScreenError(retryAction)
            else -> PricesScreenData(
                pricesItemClicked,
                filterData,
                viewState.selectedFilter,
                viewState.availableFilters,
                filterAction,
                viewState.data
            )
        }
    }
}

@Composable
fun PricesScreenData(
    pricesItemClicked: (AssetInfo) -> Unit,
    filterData: (String) -> Unit,
    selectedFilter: PricesFilter,
    filters: List<PricesFilter>,
    filterAction: (PricesFilter) -> Unit,
    data: List<PriceItemViewState>
) {
    Column {
        Box(
            modifier = Modifier.padding(
                start = AppTheme.dimensions.standardSpacing,
                top = AppTheme.dimensions.tinySpacing,
                end = AppTheme.dimensions.standardSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
        ) {
            Search(
                label = stringResource(R.string.search_coins_hint),
                onValueChange = filterData
            )
        }
        if (filters.isNotEmpty()) {
            LabeledFiltersGroup(
                filters = filters.map { filter ->
                    LabeledFilterState(
                        text = stringResource(id = filter.title()),
                        onSelected = { filterAction(filter) },
                        state = if (selectedFilter == filter) FilterState.SELECTED else FilterState.UNSELECTED
                    )
                },
                modifier = Modifier.padding(
                    horizontal = AppTheme.dimensions.standardSpacing,
                    vertical = AppTheme.dimensions.smallSpacing
                )
            )
        }

        LazyColumn {
            items(
                items = data,
            ) {
                PriceListItem(
                    priceItem = it,
                    onClick = { pricesItemClicked(it.assetInfo) }
                )
            }

            item {
                Spacer(Modifier.size(dimensionResource(R.dimen.standard_spacing)))
            }
        }
    }
}

private fun PricesFilter.title(): Int {
    return when (this) {
        PricesFilter.All -> R.string.all_prices
        PricesFilter.Tradable -> R.string.tradable
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
fun PreviewInterestDashboardScreenLoading() {
    PricesScreen(
        PricesViewState(
            isLoading = true,
            isError = false,
            selectedFilter = PricesFilter.All,
            availableFilters = emptyList(),
            data = listOf()
        ),
        {}, {}, {}, {},
    )
}

@Preview(name = "Error", showBackground = true)
@Composable
fun PreviewInterestDashboardScreenError() {
    PricesScreen(
        PricesViewState(
            isLoading = false,
            isError = true,
            selectedFilter = PricesFilter.All,
            availableFilters = emptyList(),
            data = listOf()
        ),
        {}, {}, {}, {},
    )
}

@Preview(name = "Data", showBackground = true)
@Composable
fun PreviewInterestDashboardScreenDataNoKyc() {
    PricesScreen(
        PricesViewState(
            isLoading = false,
            isError = false,
            selectedFilter = PricesFilter.All,
            availableFilters = emptyList(),
            data = listOf()
        ),
        {}, {}, {}, {},
    )
}

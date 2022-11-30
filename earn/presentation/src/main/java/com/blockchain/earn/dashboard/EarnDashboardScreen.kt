package com.blockchain.earn.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.control.Search
import com.blockchain.componentlib.control.TabLayoutLarge
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.filter.FilterState
import com.blockchain.componentlib.filter.LabeledFilterState
import com.blockchain.componentlib.filter.LabeledFiltersGroup
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.earn.R
import com.blockchain.earn.dashboard.viewmodel.DashboardState
import com.blockchain.earn.dashboard.viewmodel.EarnAsset
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardIntent
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardListFilter
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewModel
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewState
import com.blockchain.earn.dashboard.viewmodel.EarnType

@Composable
fun EarnDashboardScreen(
    viewModel: EarnDashboardViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: EarnDashboardViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        EarnDashboard(state, filterAction = {
            viewModel.onIntent(EarnDashboardIntent.UpdateListFilter(it))
        }, queryFilter = {
            viewModel.onIntent(EarnDashboardIntent.UpdateSearchQuery(it))
        }, onItemClicked = {
            viewModel.onIntent(EarnDashboardIntent.ItemSelected(it))
        })
    }
}

@Composable
fun EarnDashboard(
    state: EarnDashboardViewState,
    filterAction: (EarnDashboardListFilter) -> Unit,
    queryFilter: (String) -> Unit,
    onItemClicked: (EarnAsset) -> Unit
) {
    when (val s = state.dashboardState) {
        DashboardState.Loading -> EarnDashboardLoading()
        is DashboardState.ShowError -> {
            Text("ShowError ${s.error}")
        }
        DashboardState.ShowKyc -> {
            Text("ShowKyc")
        }
        is DashboardState.EarningAndDiscover -> {
            EarningAndDiscover(s, state.filterBy, filterAction, queryFilter, onItemClicked)
        }
        is DashboardState.OnlyDiscover -> {
            DashboardState.OnlyDiscover(s.discover)
        }
    }
}

@Composable
fun EarningAndDiscover(
    state: DashboardState.EarningAndDiscover,
    filterBy: EarnDashboardListFilter,
    filterAction: (EarnDashboardListFilter) -> Unit,
    queryFilter: (String) -> Unit,
    onItemClicked: (EarnAsset) -> Unit
) {
    var selectedTab by remember { mutableStateOf(SelectedTab.Earning) }

    Column {
        TabLayoutLarge(
            items = listOf("Earning", "Discover"),
            selectedItemIndex = selectedTab.index,
            hasBottomShadow = true,
            onItemSelected = {
                selectedTab = SelectedTab.fromInt(it)
            }
        )

        when (selectedTab) {
            SelectedTab.Earning -> {
                EarningScreen(queryFilter, filterAction, filterBy, state, onItemClicked)
            }
            SelectedTab.Discover -> {
                Text("COMING SOON TM")
            }
        }
    }
}

@Composable
private fun EarningScreen(
    queryFilter: (String) -> Unit,
    filterAction: (EarnDashboardListFilter) -> Unit,
    filterBy: EarnDashboardListFilter,
    state: DashboardState.EarningAndDiscover,
    onItemClicked: (EarnAsset) -> Unit
) {
    var searchedText by remember { mutableStateOf("") }

    Column {
        Box(modifier = Modifier.padding(dimensionResource(id = R.dimen.small_spacing))) {
            Search(label = stringResource(R.string.staking_dashboard_search), onValueChange = {
                searchedText = it
                queryFilter(it)
            })
        }

        LabeledFiltersGroup(
            filters = EarnDashboardListFilter.values().map { filter ->
                LabeledFilterState(
                    text = stringResource(id = filter.title()),
                    onSelected = { filterAction(filter) },
                    state = if (filterBy == filter) FilterState.SELECTED else FilterState.UNSELECTED
                )
            },
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.standardSpacing,
                vertical = AppTheme.dimensions.smallSpacing
            )
        )

        if (searchedText.isNotEmpty() && state.earning.isEmpty()) {
            SimpleText(
                text = stringResource(R.string.staking_dashboard_no_results),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        } else {
            LazyColumn {
                items(
                    items = state.earning,
                    itemContent = {
                        Column {
                            BalanceTableRow(
                                titleStart = buildAnnotatedString { append(it.assetName) },
                                titleEnd = buildAnnotatedString { append(it.balanceFiat.toStringWithSymbol()) },
                                startImageResource = ImageResource.Remote(it.iconUrl),
                                bodyStart = buildAnnotatedString {
                                    append(
                                        stringResource(
                                            id = R.string.staking_summary_rate_value, it.rate.toString()
                                        )
                                    )
                                },
                                tags = listOf(
                                    TagViewState(
                                        when (it.type) {
                                            EarnType.Rewards -> "Passive Rewards"
                                            EarnType.Staking -> "Staking Rewards"
                                        },
                                        TagType.Default()
                                    )
                                ),
                                isInlineTags = true,
                                bodyEnd = buildAnnotatedString { append(it.balanceCrypto.toStringWithSymbol()) },
                                onClick = { onItemClicked(it) }
                            )

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.medium
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun EarnDashboardListFilter.title(): Int =
    when (this) {
        EarnDashboardListFilter.All -> R.string.earn_dashboard_filter_all
        EarnDashboardListFilter.Staking -> R.string.earn_dashboard_filter_staking
        EarnDashboardListFilter.Rewards -> R.string.earn_dashboard_filter_rewards
    }

private enum class SelectedTab(val index: Int) {
    Earning(0),
    Discover(1);

    companion object {
        fun fromInt(value: Int) = values().first { it.index == value }
    }
}

@Composable
fun EarnDashboardLoading() {
    Column(modifier = Modifier.padding(dimensionResource(R.dimen.standard_spacing))) {
        ShimmerLoadingTableRow(false)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))

        ShimmerLoadingTableRow(false)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))

        ShimmerLoadingTableRow(true)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))

        ShimmerLoadingTableRow(true)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))

        ShimmerLoadingTableRow(true)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.standard_spacing)))
    }
}

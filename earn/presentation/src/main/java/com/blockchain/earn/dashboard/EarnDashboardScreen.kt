package com.blockchain.earn.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentManager
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
import com.blockchain.componentlib.system.EmbeddedFragment
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
import com.blockchain.earn.dashboard.viewmodel.EarnEligibility
import com.blockchain.earn.dashboard.viewmodel.EarnType
import com.blockchain.presentation.customviews.EmptyStateView
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet

@Composable
fun EarnDashboardScreen(
    viewModel: EarnDashboardViewModel,
    fragmentManager: FragmentManager
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: EarnDashboardViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        EarnDashboard(
            state = state,
            earningTabFilterAction = {
                viewModel.onIntent(EarnDashboardIntent.UpdateEarningTabListFilter(it))
            },
            earningTabQueryFilter = {
                viewModel.onIntent(EarnDashboardIntent.UpdateEarningTabSearchQuery(it))
            },
            onEarningItemClicked = {
                viewModel.onIntent(EarnDashboardIntent.EarningItemSelected(it))
            },
            discoverTabFilterAction = {
                viewModel.onIntent(EarnDashboardIntent.UpdateDiscoverTabListFilter(it))
            },
            discoverTabQueryFilter = {
                viewModel.onIntent(EarnDashboardIntent.UpdateDiscoverTabSearchQuery(it))
            },
            onDiscoverItemClicked = {
                viewModel.onIntent(EarnDashboardIntent.DiscoverItemSelected(it))
            },
            onRefreshData = {
                viewModel.onIntent(EarnDashboardIntent.LoadEarn)
            },
            fragmentManager = fragmentManager,
            earningTabQueryBy = state.earningTabQueryBy,
            discoverTabQueryBy = state.discoverTabQueryBy
        )
    }
}

@Composable
fun EarnDashboard(
    state: EarnDashboardViewState,
    earningTabFilterAction: (EarnDashboardListFilter) -> Unit,
    earningTabQueryFilter: (String) -> Unit,
    onEarningItemClicked: (EarnAsset) -> Unit,
    discoverTabFilterAction: (EarnDashboardListFilter) -> Unit,
    discoverTabQueryFilter: (String) -> Unit,
    onDiscoverItemClicked: (EarnAsset) -> Unit,
    onRefreshData: () -> Unit,
    fragmentManager: FragmentManager,
    earningTabQueryBy: String,
    discoverTabQueryBy: String
) {
    when (val s = state.dashboardState) {
        DashboardState.Loading -> EarnDashboardLoading()
        DashboardState.ShowKyc -> EarnKycRequired(fragmentManager)
        is DashboardState.ShowError -> EarnLoadError(onRefreshData)
        is DashboardState.EarningAndDiscover -> EarningAndDiscover(
            state = s,
            earningTabFilterBy = state.earningTabFilterBy,
            earningTabFilterAction = earningTabFilterAction,
            earningTabQueryFilter = earningTabQueryFilter,
            discoverTabFilterBy = state.discoverTabFilterBy,
            discoverTabFilterAction = discoverTabFilterAction,
            discoverTabQueryFilter = discoverTabQueryFilter,
            onEarningItemClicked = onEarningItemClicked,
            onDiscoverItemClicked = onDiscoverItemClicked,
            earningTabQueryBy = earningTabQueryBy,
            discoverTabQueryBy = discoverTabQueryBy
        )
        is DashboardState.OnlyDiscover -> DiscoverScreen(
            queryFilter = discoverTabQueryFilter,
            filterAction = discoverTabFilterAction,
            filterBy = state.discoverTabFilterBy,
            discoverAssetList = s.discover,
            onItemClicked = onDiscoverItemClicked,
            discoverTabQueryBy = discoverTabQueryBy
        )
    }
}

@Composable
fun EarningAndDiscover(
    state: DashboardState.EarningAndDiscover,
    earningTabFilterBy: EarnDashboardListFilter,
    earningTabFilterAction: (EarnDashboardListFilter) -> Unit,
    earningTabQueryFilter: (String) -> Unit,
    discoverTabFilterBy: EarnDashboardListFilter,
    discoverTabFilterAction: (EarnDashboardListFilter) -> Unit,
    discoverTabQueryFilter: (String) -> Unit,
    onEarningItemClicked: (EarnAsset) -> Unit,
    onDiscoverItemClicked: (EarnAsset) -> Unit,
    earningTabQueryBy: String,
    discoverTabQueryBy: String
) {
    var selectedTab by remember { mutableStateOf(SelectedTab.Earning) }

    Column {
        TabLayoutLarge(
            items = listOf(
                stringResource(id = R.string.earn_dashboard_tab_earning),
                stringResource(id = R.string.earn_dashboard_tab_discover)
            ),
            selectedItemIndex = selectedTab.index,
            hasBottomShadow = true,
            onItemSelected = {
                selectedTab = SelectedTab.fromInt(it)
            }
        )

        when (selectedTab) {
            SelectedTab.Earning -> {
                EarningScreen(
                    queryFilter = earningTabQueryFilter,
                    filterAction = earningTabFilterAction,
                    filterBy = earningTabFilterBy,
                    earningAssetList = state.earning,
                    onItemClicked = onEarningItemClicked,
                    earningTabQueryBy = earningTabQueryBy
                )
            }
            SelectedTab.Discover -> {
                DiscoverScreen(
                    queryFilter = discoverTabQueryFilter,
                    filterAction = discoverTabFilterAction,
                    filterBy = discoverTabFilterBy,
                    discoverAssetList = state.discover,
                    onItemClicked = onDiscoverItemClicked,
                    discoverTabQueryBy = discoverTabQueryBy
                )
            }
        }
    }
}

@Composable
private fun DiscoverScreen(
    queryFilter: (String) -> Unit,
    filterAction: (EarnDashboardListFilter) -> Unit,
    filterBy: EarnDashboardListFilter,
    discoverAssetList: List<EarnAsset>,
    onItemClicked: (EarnAsset) -> Unit,
    discoverTabQueryBy: String
) {
    var searchedText by remember { mutableStateOf("") }

    Column {
        Box(modifier = Modifier.padding(dimensionResource(id = R.dimen.small_spacing))) {
            Search(
                label = stringResource(R.string.staking_dashboard_search),
                prePopulatedText = discoverTabQueryBy,
                onValueChange = {
                    searchedText = it
                    queryFilter(it)
                }
            )
        }

        LabeledFiltersGroup(
            filters = EarnDashboardListFilter.values().map { filter ->
                LabeledFilterState(
                    text = stringResource(id = filter.title()),
                    onSelected = { filterAction(filter) },
                    state = if (filterBy == filter) {
                        FilterState.SELECTED
                    } else {
                        FilterState.UNSELECTED
                    }
                )
            },
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.standardSpacing,
                vertical = AppTheme.dimensions.smallSpacing
            )
        )

        if (searchedText.isNotEmpty() && discoverAssetList.isEmpty()) {
            SimpleText(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(R.string.staking_dashboard_no_results),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        } else {
            LazyColumn {
                items(
                    items = discoverAssetList,
                    itemContent = {
                        Column {
                            Box(
                                modifier = Modifier.alpha(
                                    if (it.eligibility !is EarnEligibility.Eligible) {
                                        0.5f
                                    } else {
                                        1f
                                    }
                                )
                            ) {
                                BalanceTableRow(
                                    titleStart = buildAnnotatedString { append(it.assetName) },
                                    startImageResource = ImageResource.Remote(it.iconUrl),
                                    bodyStart = buildAnnotatedString {
                                        append(
                                            stringResource(id = R.string.staking_summary_rate_value, it.rate.toString())
                                        )
                                    },
                                    tags = listOf(
                                        TagViewState(
                                            when (it.type) {
                                                EarnType.Rewards -> stringResource(
                                                    id = R.string.earn_rewards_label_passive
                                                )
                                                EarnType.Staking -> stringResource(
                                                    id = R.string.earn_rewards_label_staking
                                                )
                                            },
                                            TagType.Default()
                                        )
                                    ),
                                    isInlineTags = true,
                                    endImageResource = ImageResource.Local(R.drawable.ic_chevron_end),
                                    onClick = { onItemClicked(it) },
                                )
                            }

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

@Composable
private fun EarningScreen(
    queryFilter: (String) -> Unit,
    filterAction: (EarnDashboardListFilter) -> Unit,
    filterBy: EarnDashboardListFilter,
    earningAssetList: List<EarnAsset>,
    onItemClicked: (EarnAsset) -> Unit,
    earningTabQueryBy: String
) {
    var searchedText by remember { mutableStateOf("") }

    Column {
        Box(modifier = Modifier.padding(dimensionResource(id = R.dimen.small_spacing))) {
            Search(
                label = stringResource(R.string.staking_dashboard_search),
                prePopulatedText = earningTabQueryBy,
                onValueChange = {
                    searchedText = it
                    queryFilter(it)
                }
            )
        }

        LabeledFiltersGroup(
            filters = EarnDashboardListFilter.values().map { filter ->
                LabeledFilterState(
                    text = stringResource(id = filter.title()),
                    onSelected = { filterAction(filter) },
                    state = if (filterBy == filter) {
                        FilterState.SELECTED
                    } else {
                        FilterState.UNSELECTED
                    }
                )
            },
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.standardSpacing,
                vertical = AppTheme.dimensions.smallSpacing
            )
        )

        if (searchedText.isNotEmpty() && earningAssetList.isEmpty()) {
            SimpleText(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(R.string.staking_dashboard_no_results),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        } else {
            LazyColumn {
                items(
                    items = earningAssetList,
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
                                            EarnType.Rewards -> stringResource(id = R.string.earn_rewards_label_passive)
                                            EarnType.Staking -> stringResource(id = R.string.earn_rewards_label_staking)
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

@Composable
fun EarnLoadError(onRefresh: () -> Unit) {
    AndroidView(
        factory = { context ->
            EmptyStateView(context).apply {
                setDetails(
                    title = R.string.earn_dashboard_error_title,
                    description = R.string.earn_dashboard_error_desc,
                    action = { onRefresh() },
                )
            }
        }
    )
}

@Composable
fun EarnKycRequired(fm: FragmentManager) {
    EmbeddedFragment(
        modifier = Modifier.fillMaxSize(),
        fragment = KycUpgradeNowSheet.newInstance(),
        fragmentManager = fm,
        tag = "KycNow"
    )
}

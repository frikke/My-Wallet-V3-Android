package com.blockchain.earn.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.control.NonCancelableOutlinedSearch
import com.blockchain.componentlib.control.TabLayoutLarge
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.filter.FilterState
import com.blockchain.componentlib.filter.LabeledFilterState
import com.blockchain.componentlib.filter.LabeledFiltersGroup
import com.blockchain.componentlib.system.LazyRoundedCornersColumn
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
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowScreen
import okhttp3.internal.immutableListOf
import okhttp3.internal.toImmutableList

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
        Box(modifier = Modifier.background(AppTheme.colors.light)) {
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
                earningTabQueryBy = state.earningTabQueryBy,
                discoverTabQueryBy = state.discoverTabQueryBy,
                carouselLearnMoreClicked = { url ->
                    viewModel.onIntent(EarnDashboardIntent.CarouselLearnMoreSelected(url))
                },
                startKycClicked = {
                    viewModel.onIntent(EarnDashboardIntent.StartKycClicked)
                }
            )
        }
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
    earningTabQueryBy: String,
    discoverTabQueryBy: String,
    carouselLearnMoreClicked: (String) -> Unit,
    startKycClicked: () -> Unit,
) {
    when (val s = state.dashboardState) {
        DashboardState.Loading -> EarnDashboardLoading()
        DashboardState.ShowKyc -> KycUpgradeNowScreen(startKycClicked = startKycClicked)
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
            discoverTabQueryBy = discoverTabQueryBy,
            carouselLearnMoreClicked = carouselLearnMoreClicked
        )
        is DashboardState.OnlyDiscover -> DiscoverScreen(
            queryFilter = discoverTabQueryFilter,
            filterAction = discoverTabFilterAction,
            filterBy = state.discoverTabFilterBy,
            discoverAssetList = s.discover.toImmutableList(),
            onItemClicked = onDiscoverItemClicked,
            discoverTabQueryBy = discoverTabQueryBy,
            carouselLearnMoreClicked = carouselLearnMoreClicked
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
    discoverTabQueryBy: String,
    carouselLearnMoreClicked: (String) -> Unit
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
                    earningAssetList = state.earning.toImmutableList(),
                    onItemClicked = onEarningItemClicked,
                    earningTabQueryBy = earningTabQueryBy,
                    investNowClicked = {
                        selectedTab = SelectedTab.Discover
                    }
                )
            }
            SelectedTab.Discover -> {
                DiscoverScreen(
                    queryFilter = discoverTabQueryFilter,
                    filterAction = discoverTabFilterAction,
                    filterBy = discoverTabFilterBy,
                    discoverAssetList = state.discover.toImmutableList(),
                    onItemClicked = onDiscoverItemClicked,
                    discoverTabQueryBy = discoverTabQueryBy,
                    carouselLearnMoreClicked = carouselLearnMoreClicked
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverScreen(
    queryFilter: (String) -> Unit,
    filterAction: (EarnDashboardListFilter) -> Unit,
    filterBy: EarnDashboardListFilter,
    discoverAssetList: List<EarnAsset>,
    onItemClicked: (EarnAsset) -> Unit,
    discoverTabQueryBy: String,
    carouselLearnMoreClicked: (String) -> Unit,
) {
    var searchedText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .padding(horizontal = AppTheme.dimensions.smallSpacing)
            .background(AppTheme.colors.light)
            .shadow(elevation = 0.dp)
    ) {
        item {
            LearningCarousel(carouselLearnMoreClicked)
        }

        stickyHeader {
            Column(modifier = Modifier.background(color = AppTheme.colors.light)) {
                Box(
                    modifier = Modifier.padding(
                        top = AppTheme.dimensions.verySmallSpacing,
                        bottom = AppTheme.dimensions.verySmallSpacing
                    )
                ) {
                    NonCancelableOutlinedSearch(
                        placeholder = stringResource(R.string.staking_dashboard_search),
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
                        vertical = AppTheme.dimensions.tinySpacing
                    )
                )
            }
        }

        if (searchedText.isNotEmpty() && discoverAssetList.isEmpty()) {
            item {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.earning_dashboard_no_results),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre
                )
            }
        } else if (searchedText.isEmpty() && discoverAssetList.isEmpty()) {
            item {
                SimpleText(
                    text = stringResource(R.string.earning_dashboard_empty_filter),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre
                )
            }
        } else {
            itemsIndexed(
                items = discoverAssetList,
                itemContent = { index, item ->
                    val top = if (index == 0) {
                        AppTheme.dimensions.smallSpacing
                    } else {
                        AppTheme.dimensions.noSpacing
                    }
                    val bottom = if (index == discoverAssetList.lastIndex) {
                        AppTheme.dimensions.smallSpacing
                    } else {
                        AppTheme.dimensions.noSpacing
                    }

                    Card(
                        shape = RoundedCornerShape(
                            topStart = top,
                            topEnd = top,
                            bottomEnd = bottom,
                            bottomStart = bottom
                        )
                    ) {
                        Column {
                            Box(
                                modifier = Modifier.alpha(
                                    if (item.eligibility !is EarnEligibility.Eligible) {
                                        0.5f
                                    } else {
                                        1f
                                    }
                                )
                            ) {
                                BalanceTableRow(
                                    titleStart = buildAnnotatedString { append(item.assetName) },
                                    startImageResource = ImageResource.Remote(item.iconUrl),
                                    bodyStart = buildAnnotatedString {
                                        append(
                                            stringResource(
                                                id = R.string.staking_summary_rate_value, item.rate.toString()
                                            )
                                        )
                                    },
                                    tags = listOf(
                                        TagViewState(
                                            when (item.type) {
                                                EarnType.Passive -> stringResource(
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
                                    onClick = { onItemClicked(item) },
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.medium
                            )
                        }
                    }
                }
            )
        }
    }
}

private data class DiscoverCarouselItem(
    val title: Int,
    val description: Int,
    val icon: Int,
    val learnMoreUrl: String
)

private const val CAROUSEL_STAKING_LINK = "https://support.blockchain.com/hc/en-us/sections/5954708914460-Staking"
private const val CAROUSEL_REWARDS_LINK = "https://support.blockchain.com/hc/en-us/sections/4416668318740-Rewards"

@Composable
private fun LearningCarousel(onLearnMoreClicked: (String) -> Unit) {
    val listItems = immutableListOf(
        DiscoverCarouselItem(
            R.string.earn_rewards_label_passive,
            R.string.earn_rewards_carousel_passive_desc,
            R.drawable.ic_interest_blue_circle,
            CAROUSEL_REWARDS_LINK
        ),
        DiscoverCarouselItem(
            R.string.earn_rewards_label_staking,
            R.string.earn_rewards_carousel_staking_desc,
            R.drawable.ic_lock,
            CAROUSEL_STAKING_LINK
        )
    )

    LazyRow(modifier = Modifier.shadow(elevation = 0.dp)) {
        items(
            items = listItems,
            itemContent = {
                val position = listItems.indexOf(it)
                Card(
                    modifier = Modifier.padding(
                        top = AppTheme.dimensions.smallSpacing,
                        bottom = AppTheme.dimensions.smallestSpacing,
                        end = if (position != listItems.lastIndex) {
                            AppTheme.dimensions.smallSpacing
                        } else {
                            0.dp
                        }
                    ),
                    shape = AppTheme.shapes.medium,
                    backgroundColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .padding(AppTheme.dimensions.smallSpacing)
                    ) {
                        Row {
                            Image(
                                modifier = Modifier
                                    .size(AppTheme.dimensions.mediumSpacing)
                                    .align(Alignment.CenterVertically),
                                imageResource = ImageResource.Local(it.icon)
                            )

                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = AppTheme.dimensions.tinySpacing),
                                text = stringResource(it.title),
                                style = AppTheme.typography.body1,
                                color = AppTheme.colors.muted
                            )
                        }

                        Text(
                            modifier = Modifier.padding(
                                top = AppTheme.dimensions.tinySpacing,
                                bottom = AppTheme.dimensions.tinySpacing
                            ),
                            text = stringResource(it.description),
                            style = AppTheme.typography.paragraph1,
                            color = AppTheme.colors.title
                        )

                        SmallMinimalButton(
                            text = stringResource(R.string.common_learn_more),
                            onClick = { onLearnMoreClicked(it.learnMoreUrl) },
                            isTransparent = false
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun EarningScreen(
    queryFilter: (String) -> Unit,
    filterAction: (EarnDashboardListFilter) -> Unit,
    filterBy: EarnDashboardListFilter,
    earningAssetList: List<EarnAsset>,
    onItemClicked: (EarnAsset) -> Unit,
    earningTabQueryBy: String,
    investNowClicked: () -> Unit
) {
    var searchedText by remember { mutableStateOf("") }

    Column {
        Box(modifier = Modifier.padding(AppTheme.dimensions.smallSpacing)) {
            NonCancelableOutlinedSearch(
                placeholder = stringResource(R.string.staking_dashboard_search),
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
                horizontal = AppTheme.dimensions.smallSpacing,
                vertical = AppTheme.dimensions.tinySpacing
            )
        )

        if (searchedText.isNotEmpty() && earningAssetList.isEmpty()) {
            SimpleText(
                modifier = Modifier
                    .padding(AppTheme.dimensions.verySmallSpacing)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(R.string.earning_dashboard_no_results),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
        } else if (searchedText.isEmpty() && earningAssetList.isEmpty()) {
            SimpleText(
                modifier = Modifier
                    .padding(AppTheme.dimensions.verySmallSpacing)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(R.string.earning_dashboard_empty_filter),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            PrimaryButton(
                modifier = Modifier
                    .padding(horizontal = AppTheme.dimensions.smallSpacing)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(R.string.earning_dashboard_empty_filter_cta),
                onClick = { investNowClicked() }
            )
        } else {
            Box(modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing)) {
                LazyRoundedCornersColumn(
                    items = earningAssetList,
                    rowContent = {
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
                                            EarnType.Passive -> stringResource(id = R.string.earn_rewards_label_passive)
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
    Column(modifier = Modifier.padding(AppTheme.dimensions.standardSpacing)) {
        ShimmerLoadingTableRow(false)
        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))

        ShimmerLoadingTableRow(false)
        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))

        ShimmerLoadingTableRow(true)
        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))

        ShimmerLoadingTableRow(true)
        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))

        ShimmerLoadingTableRow(true)
        Spacer(modifier = Modifier.height(AppTheme.dimensions.standardSpacing))
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

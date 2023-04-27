package com.blockchain.earn.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.button.SmallSecondaryButton
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.control.NonCancelableOutlinedSearch
import com.blockchain.componentlib.control.TabSwitcher
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.system.LazyRoundedCornersColumnIndexed
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.tag.button.TagButtonRow
import com.blockchain.componentlib.tag.button.TagButtonValue
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.EarnAnalytics
import com.blockchain.earn.R
import com.blockchain.earn.dashboard.viewmodel.DashboardState
import com.blockchain.earn.dashboard.viewmodel.EarnAsset
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardIntent
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardListFilter
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewModel
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewState
import com.blockchain.earn.dashboard.viewmodel.EarnType
import com.blockchain.earn.navigation.EarnNavigation
import com.blockchain.earn.onboarding.EarnOnboardingProductPage
import com.blockchain.earn.onboarding.EarnProductOnboarding
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.customviews.EmptyStateView
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun EarnDashboardScreen(
    viewModel: EarnDashboardViewModel = getViewModel(scope = payloadScope),
    earnNavigation: EarnNavigation,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit
) {
    val viewState: EarnDashboardViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(EarnDashboardIntent.LoadEarn)
        onDispose { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    LaunchedEffect(key1 = viewModel) {
        navEventsFlowLifecycleAware.collectLatest {
            earnNavigation.route(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.light)
    ) {
        EarnDashboard(
            state = viewState,
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
            discoverTabQueryBy = viewState.discoverTabQueryBy,
            onCompareProductsClicked = {
                viewModel.onIntent(EarnDashboardIntent.LaunchProductComparator)
            },
            startKycClicked = {
                viewModel.onIntent(EarnDashboardIntent.StartKycClicked)
            },
            onFinishOnboarding = {
                viewModel.onIntent(EarnDashboardIntent.FinishOnboarding)
            },
            openSettings = openSettings,
            launchQrScanner = launchQrScanner,
        )
    }
}

@Composable
fun EarnDashboard(
    state: EarnDashboardViewState,
    onEarningItemClicked: (EarnAsset) -> Unit,
    discoverTabFilterAction: (EarnDashboardListFilter) -> Unit,
    discoverTabQueryFilter: (String) -> Unit,
    onDiscoverItemClicked: (EarnAsset) -> Unit,
    onRefreshData: () -> Unit,
    discoverTabQueryBy: String,
    onCompareProductsClicked: () -> Unit,
    startKycClicked: () -> Unit,
    onFinishOnboarding: () -> Unit,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.backgroundMuted)
    ) {

        MenuOptionsScreen(
            openSettings = openSettings,
            launchQrScanner = launchQrScanner
        )

        TinyVerticalSpacer()

        Box(modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing)) {
            when (val s = state.dashboardState) {
                DashboardState.Loading -> EarnDashboardLoading()
                DashboardState.ShowKyc -> KycUpgradeNowScreen(startKycClicked = startKycClicked)
                is DashboardState.ShowIntro -> EarnProductOnboarding(
                    onboardingPages = listOf(EarnOnboardingProductPage.Intro) +
                        s.earnProductsAvailable.map { earnType ->
                            when (earnType) {
                                EarnType.Passive -> EarnOnboardingProductPage.Interest
                                EarnType.Staking -> EarnOnboardingProductPage.Staking
                                EarnType.Active -> EarnOnboardingProductPage.ActiveRewards
                            }
                        },
                    onFinishOnboarding = onFinishOnboarding
                )
                is DashboardState.ShowError -> EarnLoadError(onRefreshData)
                is DashboardState.EarningAndDiscover -> EarningAndDiscover(
                    state = s,
                    discoverTabFilterBy = state.discoverTabFilterBy,
                    discoverTabFilterAction = discoverTabFilterAction,
                    discoverTabQueryFilter = discoverTabQueryFilter,
                    onEarningItemClicked = onEarningItemClicked,
                    onDiscoverItemClicked = onDiscoverItemClicked,
                    discoverTabQueryBy = discoverTabQueryBy,
                    onCompareProductsClicked = onCompareProductsClicked
                )
                is DashboardState.OnlyDiscover -> DiscoverScreen(
                    queryFilter = discoverTabQueryFilter,
                    filterAction = discoverTabFilterAction,
                    filterBy = state.discoverTabFilterBy,
                    filtersAvailable = s.filterList,
                    discoverAssetList = s.discover.toImmutableList(),
                    onItemClicked = onDiscoverItemClicked,
                    discoverTabQueryBy = discoverTabQueryBy,
                    onOpenProductComparator = onCompareProductsClicked
                )
            }
        }

        StandardVerticalSpacer()
    }
}

@Composable
fun EarningAndDiscover(
    analytics: Analytics = get(),
    state: DashboardState.EarningAndDiscover,
    discoverTabFilterBy: EarnDashboardListFilter,
    discoverTabFilterAction: (EarnDashboardListFilter) -> Unit,
    discoverTabQueryFilter: (String) -> Unit,
    onEarningItemClicked: (EarnAsset) -> Unit,
    onDiscoverItemClicked: (EarnAsset) -> Unit,
    discoverTabQueryBy: String,
    onCompareProductsClicked: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(SelectedTab.Earning) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TabSwitcher(
            tabs = persistentListOf(
                stringResource(id = R.string.earn_dashboard_tab_earning),
                stringResource(id = R.string.earn_dashboard_tab_discover)
            ),
            initialTabIndex = selectedTab.index,
            onTabChanged = {
                selectedTab = SelectedTab.fromInt(it)

                if (selectedTab == SelectedTab.Discover) {
                    analytics.logEvent(EarnAnalytics.DiscoverClicked)
                }
            }
        )

        StandardVerticalSpacer()

        when (selectedTab) {
            SelectedTab.Earning -> {
                EarningScreen(
                    earningAssetList = state.earning.toImmutableList(),
                    totalEarningBalanceFiat = state.totalEarningBalanceFiat,
                    onItemClicked = onEarningItemClicked,
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
                    filtersAvailable = state.filterList,
                    discoverAssetList = state.discover.toImmutableList(),
                    onItemClicked = onDiscoverItemClicked,
                    discoverTabQueryBy = discoverTabQueryBy,
                    onOpenProductComparator = onCompareProductsClicked
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
    filtersAvailable: List<EarnDashboardListFilter>,
    discoverAssetList: List<EarnAsset>,
    onItemClicked: (EarnAsset) -> Unit,
    discoverTabQueryBy: String,
    onOpenProductComparator: () -> Unit,
) {
    var searchedText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundMuted)
    ) {
        item {
            ProductComparatorCta(onOpenProductComparator = onOpenProductComparator)
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

                TinyVerticalSpacer()

                TagButtonRow(
                    selected = filterBy,
                    values = filtersAvailable.map {
                        TagButtonValue(it, stringResource(id = it.title()))
                    }.toImmutableList(),
                    onClick = { filter -> filterAction(filter) }
                )

                TinyVerticalSpacer()
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

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = top,
                            topEnd = top,
                            bottomEnd = bottom,
                            bottomStart = bottom
                        ),
                        color = Color.Transparent
                    ) {
                        Column {
                            BalanceTableRow(
                                modifier = Modifier.alpha(
                                    if (item.eligibility !is EarnRewardsEligibility.Eligible) {
                                        0.5f
                                    } else {
                                        1f
                                    }
                                ),
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
                                                id = R.string.earn_rewards_label_passive_short
                                            )
                                            EarnType.Staking -> stringResource(
                                                id = R.string.earn_rewards_label_staking_short
                                            )
                                            EarnType.Active -> stringResource(
                                                id = R.string.earn_rewards_label_active_short
                                            )
                                        },
                                        TagType.Default()
                                    )
                                ),
                                isInlineTags = true,
                                endImageResource = ImageResource.Local(R.drawable.ic_chevron_end),
                                onClick = {
                                    onItemClicked(item)
                                },
                            )

                            if (index < discoverAssetList.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.backgroundMuted
                                )
                            } else {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ProductComparatorCta(onOpenProductComparator: () -> Unit) {
    Card(
        backgroundColor = AppTheme.colors.light,
        shape = AppTheme.shapes.large,
        elevation = 0.dp
    ) {
        TableRow(
            contentStart = {
                Image(imageResource = ImageResource.Local(R.drawable.coins_on).withTint(AppTheme.colors.primary))
            },
            content = {
                Column(modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing)) {
                    SimpleText(
                        text = stringResource(id = R.string.earn_product_comparator_title),
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                    SmallestVerticalSpacer()
                    SimpleText(
                        text = stringResource(id = R.string.earn_product_comparator_description),
                        style = ComposeTypographies.Paragraph2,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )
                }
            },
            contentEnd = {
                SmallSecondaryButton(
                    text = stringResource(id = R.string.common_go),
                    onClick = onOpenProductComparator,
                    state = ButtonState.Enabled,
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.End)
                        .padding(start = AppTheme.dimensions.smallSpacing)
                        .wrapContentWidth(align = Alignment.End)
                        .weight(1f)
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductComparatorCtaPreview() {
    AppTheme {
        ProductComparatorCta(onOpenProductComparator = {})
    }
}

private data class DiscoverCarouselItem(
    val type: EarnType,
    val title: Int,
    val description: Int,
    val icon: Int,
    val learnMoreUrl: String
)

private const val CAROUSEL_STAKING_LINK = "https://support.blockchain.com/hc/en-us/sections/5954708914460-Staking"
private const val CAROUSEL_REWARDS_LINK = "https://support.blockchain.com/hc/en-us/sections/4416668318740-Rewards"
private const val CAROUSEL_ACTIVE_LINK =
    "https://support.blockchain.com/hc/en-us/articles/6868491485724-What-is-Active-Rewards-"

@Composable
private fun LearningCarousel(
    analytics: Analytics = get(),
    onLearnMoreClicked: (String) -> Unit
) {
    val listItems = persistentListOf(
        DiscoverCarouselItem(
            type = EarnType.Passive,
            title = R.string.earn_rewards_label_passive,
            description = R.string.earn_rewards_carousel_passive_desc,
            icon = R.drawable.ic_interest_blue_circle,
            learnMoreUrl = CAROUSEL_REWARDS_LINK
        ),
        DiscoverCarouselItem(
            type = EarnType.Staking,
            title = R.string.earn_rewards_label_staking,
            description = R.string.earn_rewards_carousel_staking_desc,
            icon = R.drawable.ic_lock,
            learnMoreUrl = CAROUSEL_STAKING_LINK
        ),
        DiscoverCarouselItem(
            type = EarnType.Active,
            title = R.string.earn_rewards_label_active,
            description = R.string.earn_rewards_carousel_active_desc,
            icon = R.drawable.ic_lock,
            learnMoreUrl = CAROUSEL_ACTIVE_LINK
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
                            onClick = {
                                onLearnMoreClicked(it.learnMoreUrl)
                                analytics.logEvent(EarnAnalytics.LearnMoreClicked(product = it.type))
                            },
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
    earningAssetList: List<EarnAsset>,
    totalEarningBalanceFiat: String,
    onItemClicked: (EarnAsset) -> Unit,
    investNowClicked: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        SimpleText(
            text = totalEarningBalanceFiat,
            style = ComposeTypographies.Title1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SmallestVerticalSpacer()

        SimpleText(
            text = stringResource(id = R.string.common_total_balance),
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        StandardVerticalSpacer()

        if (earningAssetList.isEmpty()) {
            SimpleText(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(R.string.earning_dashboard_empty_filter),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            PrimaryButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(R.string.earning_dashboard_empty_filter_cta),
                onClick = { investNowClicked() }
            )
        } else {
            Box {
                LazyRoundedCornersColumnIndexed(
                    modifier = Modifier.fillMaxSize(),
                    items = earningAssetList,
                    rowContent = { asset, index ->
                        Column {
                            BalanceTableRow(
                                titleStart = buildAnnotatedString { append(asset.assetName) },
                                titleEnd = buildAnnotatedString { append(asset.balanceFiat.toStringWithSymbol()) },
                                startImageResource = ImageResource.Remote(asset.iconUrl),
                                bodyStart = buildAnnotatedString {
                                    append(
                                        stringResource(
                                            id = R.string.staking_summary_rate_value, asset.rate.toString()
                                        )
                                    )
                                },
                                tags = listOf(
                                    TagViewState(
                                        when (asset.type) {
                                            EarnType.Passive -> stringResource(
                                                id = R.string.earn_rewards_label_passive_short
                                            )
                                            EarnType.Staking -> stringResource(
                                                id = R.string.earn_rewards_label_staking_short
                                            )
                                            EarnType.Active -> stringResource(
                                                id = R.string.earn_rewards_label_active_short
                                            )
                                        },
                                        TagType.Default()
                                    )
                                ),
                                isInlineTags = true,
                                bodyEnd = buildAnnotatedString { append(asset.balanceCrypto.toStringWithSymbol()) },
                                onClick = { onItemClicked(asset) }
                            )

                            if (index < earningAssetList.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.backgroundMuted
                                )
                            } else {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
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
        EarnDashboardListFilter.Interest -> R.string.earn_dashboard_filter_interest
        EarnDashboardListFilter.Active -> R.string.earn_dashboard_filter_active
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

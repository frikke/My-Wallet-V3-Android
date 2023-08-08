package piuk.blockchain.android.ui.brokerage.buy.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.blockchain.analytics.Analytics
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.componentlib.alert.CustomEmptyState
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.data.DataResource
import com.blockchain.data.toImmutableList
import com.blockchain.koin.payloadScope
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowScreen
import com.blockchain.prices.prices.PriceItemViewState
import com.blockchain.prices.prices.PricesIntents
import com.blockchain.prices.prices.PricesLoadStrategy
import com.blockchain.prices.prices.PricesOutputGroup
import com.blockchain.prices.prices.PricesViewModel
import com.blockchain.prices.prices.PricesViewState
import com.blockchain.prices.prices.composable.TopMoversScreen
import com.blockchain.prices.prices.percentAndPositionOf
import info.blockchain.balance.AssetInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.ui.brokerage.buy.BuyAnalyticsEvents
import piuk.blockchain.android.ui.brokerage.buy.BuySelectAssetIntent
import piuk.blockchain.android.ui.brokerage.buy.BuySelectAssetViewModel
import piuk.blockchain.android.ui.brokerage.buy.BuySelectAssetViewState
import piuk.blockchain.android.ui.customviews.CustomEmptyStateView
import retrofit2.HttpException

@Composable
fun BuySelectAsset(
    viewModel: BuySelectAssetViewModel = getViewModel(scope = payloadScope),
    onErrorContactSupportClicked: () -> Unit,
    onEmptyStateClicked: (BlockedReason) -> Unit,
    startKycClicked: () -> Unit
) {
    val viewState: BuySelectAssetViewState by viewModel.viewState.collectAsStateLifecycleAware()
    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(BuySelectAssetIntent.LoadEligibility)
        onDispose { }
    }

    BuySelectAssetScreen(
        featureAccess = viewState.featureAccess,
        onErrorRetryClicked = {
            viewModel.onIntent(BuySelectAssetIntent.LoadEligibility)
        },
        onErrorContactSupportClicked = onErrorContactSupportClicked,
        onEmptyStateClicked = onEmptyStateClicked,
        startKycClicked = startKycClicked,
        showTopMovers = viewState.showTopMovers,
        onAssetClick = { asset ->
            viewModel.onIntent(BuySelectAssetIntent.AssetClicked(asset))
        }
    )
}

@Composable
fun BuySelectAssetScreen(
    featureAccess: DataResource<FeatureAccess>,
    onErrorRetryClicked: () -> Unit,
    onErrorContactSupportClicked: () -> Unit,
    onEmptyStateClicked: (BlockedReason) -> Unit,
    startKycClicked: () -> Unit,
    showTopMovers: Boolean,
    onAssetClick: (AssetInfo) -> Unit
) {
    with(featureAccess) {
        when (this) {
            DataResource.Loading -> {
                Loading()
            }

            is DataResource.Error -> {
                Error(
                    error = error,
                    onErrorRetryClicked = onErrorRetryClicked,
                    onErrorContactSupportClicked = onErrorContactSupportClicked
                )
            }

            is DataResource.Data -> {
                when (val reason = (data as? FeatureAccess.Blocked)?.reason) {
                    is BlockedReason.NotEligible -> {
                        BlockedDueToNotEligible(
                            reason = reason,
                            onEmptyStateClicked = onEmptyStateClicked
                        )
                    }

                    is BlockedReason.InsufficientTier -> {
                        KycUpgradeNowScreen(
                            startKycClicked = startKycClicked
                        )
                    }

                    is BlockedReason.Sanctions -> {
                        BlockedDueToSanctions(
                            reason = reason,
                            onEmptyStateClicked = onEmptyStateClicked
                        )
                    }

                    is BlockedReason.TooManyInFlightTransactions,
                    is BlockedReason.ShouldAcknowledgeStakingWithdrawal,
                    BlockedReason.ShouldAcknowledgeActiveRewardsWithdrawalWarning,
                    null -> {
                        Assets(
                            showTopMovers = showTopMovers,
                            onErrorContactSupportClicked = onErrorContactSupportClicked,
                            onAssetClick = onAssetClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Assets(
    viewModel: PricesViewModel = getViewModel(scope = payloadScope),
    showTopMovers: Boolean,
    onErrorContactSupportClicked: () -> Unit,
    onAssetClick: (AssetInfo) -> Unit
) {
    val viewState: PricesViewState by viewModel.viewState.collectAsStateLifecycleAware()
    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(PricesIntents.LoadData(PricesLoadStrategy.TradableOnly))
        onDispose { }
    }

    with(viewState.mostPopularAndOtherAssets) {
        when (this) {
            DataResource.Loading -> {
                Loading()
            }

            is DataResource.Error -> {
                Error(
                    error = error,
                    onErrorRetryClicked = {
                        viewModel.onIntent(PricesIntents.Refresh)
                    },
                    onErrorContactSupportClicked = onErrorContactSupportClicked
                )
            }

            is DataResource.Data -> {
                AssetsData(
                    showTopMovers = showTopMovers,
                    topMovers = viewState.topMovers.toImmutableList(),
                    mostPopular = data[PricesOutputGroup.MostPopular]?.toImmutableList() ?: persistentListOf(),
                    others = data[PricesOutputGroup.Others]?.toImmutableList() ?: persistentListOf(),
                    onSearchValueUpdated = {
                        viewModel.onIntent(PricesIntents.FilterSearch(term = it))
                    },
                    onAssetClick = onAssetClick
                )
            }
        }
    }
}

@Composable
private fun AssetsData(
    analytics: Analytics = get(),
    showTopMovers: Boolean,
    topMovers: DataResource<ImmutableList<PriceItemViewState>>,
    mostPopular: ImmutableList<PriceItemViewState>,
    others: List<PriceItemViewState>,
    onSearchValueUpdated: (String) -> Unit,
    onAssetClick: (AssetInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .background(AppColors.background)
    ) {
        var searchedText by remember { mutableStateOf("") }

        CancelableOutlinedSearch(
            modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing),
            placeholder = stringResource(com.blockchain.stringResources.R.string.search_coins_hint),
            onValueChange = {
                searchedText = it
                onSearchValueUpdated(it)
            }
        )

        SmallVerticalSpacer()

        LazyColumn {
            // top movers
            if (showTopMovers) {
                paddedItem(
                    paddingValues = {
                        PaddingValues(
                            start = AppTheme.dimensions.smallSpacing,
                            end = AppTheme.dimensions.smallSpacing,
                            bottom = AppTheme.dimensions.tinySpacing
                        )
                    }
                ) {
                    TableRowHeader(title = stringResource(com.blockchain.stringResources.R.string.prices_top_movers))
                }

                item {
                    TopMoversScreen(
                        data = topMovers,
                        assetOnClick = { asset ->
                            onAssetClick(asset)

                            topMovers.percentAndPositionOf(asset)?.let { (percentageMove, position) ->
                                analytics.logEvent(
                                    BuyAnalyticsEvents.TopMoverAssetClicked(
                                        ticker = asset.networkTicker,
                                        percentageMove = percentageMove,
                                        position = position
                                    )
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
                }
            }

            if (searchedText.isNotEmpty() && mostPopular.isEmpty() && others.isEmpty()) {
                paddedItem(
                    paddingValues = {
                        PaddingValues(
                            horizontal = AppTheme.dimensions.smallSpacing,
                        )
                    }
                ) {
                    SimpleText(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(com.blockchain.stringResources.R.string.assets_no_result),
                        style = ComposeTypographies.Body1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Centre
                    )
                }
            } else {
                // popular header + list
                if (mostPopular.isNotEmpty()) {
                    paddedItem(
                        paddingValues = {
                            PaddingValues(
                                start = AppTheme.dimensions.smallSpacing,
                                end = AppTheme.dimensions.smallSpacing,
                                bottom = AppTheme.dimensions.tinySpacing
                            )
                        }
                    ) {
                        TableRowHeader(title = stringResource(com.blockchain.stringResources.R.string.most_popular))
                    }

                    paddedRoundedCornersItems(
                        items = mostPopular,
                        key = {
                            it.asset.networkTicker
                        },
                        paddingValues = {
                            PaddingValues(horizontal = AppTheme.dimensions.smallSpacing)
                        }
                    ) { cryptoAsset ->
                        BalanceChangeTableRow(
                            data = cryptoAsset.data,
                            onClick = { onAssetClick(cryptoAsset.asset) }
                        )
                    }

                    if (others.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
                        }
                    }
                }

                // popular header + list
                if (others.isNotEmpty()) {
                    paddedItem(
                        paddingValues = {
                            PaddingValues(
                                start = AppTheme.dimensions.smallSpacing,
                                end = AppTheme.dimensions.smallSpacing,
                                bottom = AppTheme.dimensions.tinySpacing
                            )
                        }
                    ) {
                        TableRowHeader(title = stringResource(com.blockchain.stringResources.R.string.other_tokens))
                    }

                    paddedRoundedCornersItems(
                        items = others,
                        key = {
                            it.asset.networkTicker
                        },
                        paddingValues = {
                            PaddingValues(horizontal = AppTheme.dimensions.smallSpacing)
                        }
                    ) { cryptoAsset ->
                        BalanceChangeTableRow(
                            data = cryptoAsset.data,
                            onClick = { onAssetClick(cryptoAsset.asset) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            }
        }
    }
}

@Composable
fun BlockedDueToNotEligible(
    reason: BlockedReason.NotEligible,
    onEmptyStateClicked: (BlockedReason) -> Unit
) {
    val defaultDescription = stringResource(com.blockchain.stringResources.R.string.feature_not_available)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            CustomEmptyStateView(context).apply {
                title = com.blockchain.stringResources.R.string.account_restricted
                descriptionText = reason.message ?: defaultDescription
                icon = Icons.Filled.User
                ctaText = com.blockchain.stringResources.R.string.contact_support
                ctaAction = { onEmptyStateClicked(reason) }
            }
        }
    )
}

@Composable
fun BlockedDueToSanctions(
    reason: BlockedReason.Sanctions,
    onEmptyStateClicked: (BlockedReason) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            CustomEmptyStateView(context).apply {
                title = com.blockchain.stringResources.R.string.account_restricted
                descriptionText = reason.message
                icon = Icons.Filled.User
                ctaText = com.blockchain.stringResources.R.string.common_learn_more
                ctaAction = { onEmptyStateClicked(reason) }
            }
        }
    )
}

@Composable
fun Loading() {
    Box {
        ShimmerLoadingCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.smallSpacing)
        )
    }
}

@Composable
private fun Error(
    analytics: Analytics = get(),
    error: Exception,
    onErrorRetryClicked: () -> Unit,
    onErrorContactSupportClicked: () -> Unit
) {
    val nabuException: NabuApiException? = (error as? HttpException)?.let { httpException ->
        NabuApiExceptionFactory.fromResponseBody(httpException)
    }

    analytics.logEvent(
        ClientErrorAnalytics.ClientLogError(
            nabuApiException = nabuException,
            errorDescription = error.message,
            error = ClientErrorAnalytics.NABU_ERROR,
            source = if (error is HttpException) {
                ClientErrorAnalytics.Companion.Source.NABU
            } else {
                ClientErrorAnalytics.Companion.Source.CLIENT
            },
            title = ClientErrorAnalytics.OOPS_ERROR,
            action = ClientErrorAnalytics.ACTION_BUY,
            categories = nabuException?.getServerSideErrorInfo()?.categories ?: emptyList()
        )
    )

    CustomEmptyState(
        ctaText = com.blockchain.stringResources.R.string.common_empty_cta,
        ctaAction = onErrorRetryClicked,
        secondaryText = com.blockchain.stringResources.R.string.contact_support,
        secondaryAction = onErrorContactSupportClicked
    )
}

// PREVIEW

@Preview
@Composable
fun PreviewError() {
    Error(
        analytics = previewAnalytics,
        error = Exception(),
        onErrorRetryClicked = {},
        onErrorContactSupportClicked = {}
    )
}

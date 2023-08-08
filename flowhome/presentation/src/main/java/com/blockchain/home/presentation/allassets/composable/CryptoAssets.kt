package com.blockchain.home.presentation.allassets.composable

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.chrome.navigation.AssetActionsNavigation
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.control.CancelableOutlinedSearch
import com.blockchain.componentlib.icons.Filter
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.MaskedBalanceChangeTableRow
import com.blockchain.componentlib.tablerow.MaskedBalanceFiatAndCryptoTableRow
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.home.domain.AssetFilter
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.allassets.AssetsIntent
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.allassets.CustodialAssetState
import com.blockchain.home.presentation.allassets.HomeCryptoAsset
import com.blockchain.home.presentation.allassets.NonCustodialAssetState
import com.blockchain.koin.payloadScope
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun CryptoAssets(
    viewModel: AssetsViewModel = getViewModel(scope = payloadScope),
    assetActionsNavigation: AssetActionsNavigation,
    onBackPressed: () -> Unit
) {
    val walletMode by get<WalletModeService>(scope = payloadScope)
        .walletMode.collectAsStateLifecycleAware(initial = null)

    walletMode?.let { walletMode ->
        val viewState: AssetsViewState by viewModel.viewState.collectAsStateLifecycleAware()

        LaunchedEffect(viewModel) {
            viewModel.onIntent(AssetsIntent.LoadAccounts(walletMode, SectionSize.All))
            viewModel.onIntent(AssetsIntent.LoadFilters)
        }

        CryptoAssetsScreen(
            cryptoAssets = viewState.assets.map { it.filterIsInstance<HomeCryptoAsset>() },
            onSearchTermEntered = { term ->
                viewModel.onIntent(AssetsIntent.FilterSearch(term = term))
            },
            filters = viewState.filters,
            showNoResults = viewState.showNoResults,
            showFilterIcon = viewState.showFilterIcon,
            onFiltersConfirmed = { filters ->
                viewModel.onIntent(AssetsIntent.UpdateFilters(filters = filters))
            },
            onAssetClick = { asset ->
                assetActionsNavigation.coinview(asset)
            },
            onBackPressed = onBackPressed
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CryptoAssetsScreen(
    cryptoAssets: DataResource<List<HomeCryptoAsset>>,
    showNoResults: Boolean,
    showFilterIcon: Boolean,
    onSearchTermEntered: (String) -> Unit,
    filters: List<AssetFilter>,
    onFiltersConfirmed: (List<AssetFilter>) -> Unit,
    onAssetClick: (AssetInfo) -> Unit,
    onBackPressed: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded }
    )
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current

    BackHandler(sheetState.isVisible) {
        coroutineScope.launch { sheetState.hide() }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            CryptoAssetsFilters(
                filters = filters,
                onConfirmClick = { filters ->
                    coroutineScope.launch { sheetState.hide() }
                    onFiltersConfirmed(filters)
                }
            )
        },
        sheetShape = AppTheme.shapes.large.topOnly(),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = AppTheme.colors.background)
        ) {
            NavigationBar(
                title = stringResource(com.blockchain.stringResources.R.string.ma_home_assets_title),
                onBackButtonClick = onBackPressed,
                navigationBarButtons = listOfNotNull(
                    NavigationBarButton.IconResource(
                        Icons.Filter.copy(
                            contentDescription = stringResource(
                                com.blockchain.stringResources.R.string.accessibility_filter
                            )
                        )
                    ) {
                        focusManager.clearFocus(true)
                        coroutineScope.launch { sheetState.show() }
                    }.takeIf { showFilterIcon }
                )
            )

            CryptoAssetsData(
                cryptoAssets = cryptoAssets,
                showNoResults = showNoResults,
                onSearchTermEntered = onSearchTermEntered,
                onAssetClick = onAssetClick
            )
        }
    }
}

@Composable
private fun CryptoAssetsData(
    cryptoAssets: DataResource<List<HomeCryptoAsset>>,
    showNoResults: Boolean,
    onSearchTermEntered: (String) -> Unit,
    onAssetClick: (AssetInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        when (cryptoAssets) {
            is DataResource.Loading -> {
                ShimmerLoadingCard()
            }

            is DataResource.Error -> {
                // todo
            }

            is DataResource.Data -> {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CancelableOutlinedSearch(
                        onValueChange = onSearchTermEntered,
                        placeholder = stringResource(com.blockchain.stringResources.R.string.search)
                    )

                    Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

                    CryptoAssetsList(
                        cryptoAssets = cryptoAssets.data,
                        onAssetClick = onAssetClick,
                        showNoResults = showNoResults
                    )
                }
            }
        }
    }
}

@Composable
fun CryptoAssetsList(
    showNoResults: Boolean,
    cryptoAssets: List<HomeCryptoAsset>,
    onAssetClick: (AssetInfo) -> Unit
) {

    if (cryptoAssets.isNotEmpty()) {
        LazyColumn {
            roundedCornersItems(cryptoAssets) { cryptoAsset ->
                when (cryptoAsset) {
                    is CustodialAssetState -> BalanceWithPriceChange(cryptoAsset, onAssetClick)
                    is NonCustodialAssetState -> BalanceWithFiatAndCryptoBalance(cryptoAsset, onAssetClick)
                }
            }
        }
    } else if (showNoResults) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTheme.colors.backgroundSecondary,
                    shape = AppTheme.shapes.large
                )
                .padding(AppTheme.dimensions.smallSpacing),
            text = stringResource(com.blockchain.stringResources.R.string.assets_no_result),
            style = AppTheme.typography.body2,
            color = AppTheme.colors.title,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BalanceWithFiatAndCryptoBalance(
    cryptoAsset: NonCustodialAssetState,
    onAssetClick: (AssetInfo) -> Unit
) {
    MaskedBalanceFiatAndCryptoTableRow(
        title = cryptoAsset.name,
        tag = cryptoAsset.asset.takeIf { it.isLayer2Token }?.coinNetwork?.shortName ?: "",
        valueCrypto = cryptoAsset.balance.map { it.toStringWithSymbol() }.dataOrElse(""),
        valueFiat = cryptoAsset.fiatBalance.map { it?.toStringWithSymbol().orEmpty() }.dataOrElse(""),
        icon = if (cryptoAsset.icon.size == 2) {
            StackedIcon.SmallTag(
                main = ImageResource.Remote(
                    cryptoAsset.icon[0]
                ),
                tag = ImageResource.Remote(
                    cryptoAsset.icon[1]
                )
            )
        } else {
            StackedIcon.SingleIcon(
                icon = ImageResource.Remote(cryptoAsset.icon[0])
            )
        },
        onClick = { onAssetClick(cryptoAsset.asset) }
    )
}

@Composable
fun BalanceWithPriceChange(
    cryptoAsset: CustodialAssetState,
    onAssetClick: (AssetInfo) -> Unit
) {
    MaskedBalanceChangeTableRow(
        name = cryptoAsset.name,
        value = fiatOrCryptoBalance(cryptoAsset),
        valueChange = cryptoAsset.change,
        icon = if (cryptoAsset.icon.size == 2) {
            StackedIcon.SmallTag(
                main = ImageResource.Remote(
                    cryptoAsset.icon[0]
                ),
                tag = ImageResource.Remote(
                    cryptoAsset.icon[1]
                )
            )
        } else {
            StackedIcon.SingleIcon(
                icon = ImageResource.Remote(cryptoAsset.icon[0])
            )
        },
        onClick = { onAssetClick(cryptoAsset.asset) }
    )
}

/***
 * if fiatbalance is available, return the fiatbalance, otherwise the
 * cryptobalance if has loaded.
 */
private fun fiatOrCryptoBalance(cryptoAsset: CustodialAssetState): DataResource<String> {
    val fiatBalanceData = (cryptoAsset.fiatBalance as? DataResource.Data)
        ?: return cryptoAsset.fiatBalance.map { it?.toStringWithSymbol().orEmpty() }
    val cryptoBalanceData = (cryptoAsset.balance as? DataResource.Data)
        ?: return cryptoAsset.balance.map { it.toStringWithSymbol() }

    return DataResource.Data(
        fiatBalanceData.data?.toStringWithSymbol() ?: cryptoBalanceData.data.toStringWithSymbol()
    )
}

@Preview
@Composable
fun PreviewCryptoAssetsScreen() {
    CryptoAssetsData(
        cryptoAssets = DataResource.Data(
            listOf(
                CustodialAssetState(
                    icon = listOf(""),
                    name = "Ethereum",
                    balance = DataResource.Data(Money.fromMajor(FiatCurrency.Dollars, 306.28.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Up(3.94)),
                    fiatBalance = DataResource.Data(Money.fromMajor(FiatCurrency.Dollars, 306.28.toBigDecimal())),
                    asset = CryptoCurrency.ETHER
                ),
                CustodialAssetState(
                    icon = listOf(""),
                    name = "Bitcoin",
                    balance = DataResource.Loading,
                    change = DataResource.Loading,
                    fiatBalance = DataResource.Loading,
                    asset = CryptoCurrency.ETHER
                ),
                CustodialAssetState(
                    icon = listOf(""),
                    name = "Solana",
                    balance = DataResource.Data(Money.fromMajor(FiatCurrency.Dollars, 306.28.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Down(2.32)),
                    fiatBalance = DataResource.Data(Money.fromMajor(FiatCurrency.Dollars, 306.28.toBigDecimal())),
                    asset = CryptoCurrency.ETHER
                )
            )
        ),
        onSearchTermEntered = {},
        onAssetClick = {},
        showNoResults = false
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCryptoAssetsScreenDark() {
    PreviewCryptoAssetsScreen()
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewCryptoAssetsScreen_Empty() {
    CryptoAssetsData(
        cryptoAssets = DataResource.Data(
            listOf()
        ),
        showNoResults = true,
        onSearchTermEntered = {},
        onAssetClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCryptoAssetsScreen_EmptyDark() {
    PreviewCryptoAssetsScreen_Empty()
}

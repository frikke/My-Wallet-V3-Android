package com.blockchain.home.presentation.allassets.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.control.Search
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.home.model.AssetFilterStatus
import com.blockchain.home.presentation.allassets.SectionSize
import com.blockchain.home.presentation.allassets.CryptoAssetState
import com.blockchain.home.presentation.allassets.AssetsIntent
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.koin.payloadScope
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun CryptoAssets(
    viewModel: AssetsViewModel = getViewModel(scope = payloadScope)
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: AssetsViewState? by stateFlowLifecycleAware.collectAsState(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(AssetsIntent.LoadData(SectionSize.All))
        onDispose { }
    }

    viewState?.let { state ->
        CryptoAssetsScreen(
            cryptoAssets = state.cryptoAssets.map { it.first },
            onSearchTermEntered = { term ->
                viewModel.onIntent(AssetsIntent.FilterSearch(term = term))
            },
            filters = state.filters,
            onFiltersConfirmed = { filters ->
                viewModel.onIntent(AssetsIntent.UpdateFilters(filters = filters))
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CryptoAssetsScreen(
    cryptoAssets: DataResource<List<CryptoAssetState>>,
    onSearchTermEntered: (String) -> Unit,
    filters: List<AssetFilterStatus>,
    onFiltersConfirmed: (List<AssetFilterStatus>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded }
    )
    val coroutineScope = rememberCoroutineScope()

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
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0XFFF1F2F7))
        ) {
            // todo make this a generic screen with composable {contents}

            // todo header stuff

            // content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                when (cryptoAssets) {
                    is DataResource.Loading -> {
                        CryptoAssetsLoading()
                    }
                    is DataResource.Error -> {
                        // todo
                    }
                    is DataResource.Data -> {
                        CryptoAssetsData(
                            cryptoAssets = cryptoAssets.data,
                            onSearchTermEntered = onSearchTermEntered,
                            onAssetClick = { coroutineScope.launch { sheetState.show() } }
                        )
                    }
                }
            }

            // todo footer stuff
        }
    }
}

@Composable
fun CryptoAssetsLoading() {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        Column {
            ShimmerLoadingTableRow()

            Divider(color = Color(0XFFF1F2F7))

            ShimmerLoadingTableRow()
        }
    }
}

@Composable
fun CryptoAssetsData(
    cryptoAssets: List<CryptoAssetState>,
    onSearchTermEntered: (String) -> Unit,
    onAssetClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Search(
            onValueChange = onSearchTermEntered,
            placeholder = "120210210210"
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.standardSpacing))

        CryptoAssetsList(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            cryptoAssets = cryptoAssets,
            onAssetClick = onAssetClick
        )
    }
}

@Composable
fun CryptoAssetsList(
    modifier: Modifier = Modifier,
    cryptoAssets: List<CryptoAssetState>,
    onAssetClick: () -> Unit
) {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        if (cryptoAssets.isNotEmpty()) {
            Column(modifier = modifier) {
                cryptoAssets.forEachIndexed { index, cryptoAsset ->
                    BalanceChangeTableRow(
                        name = cryptoAsset.name,
                        value = cryptoAsset.fiatBalance.map {
                            it.toStringWithSymbol()
                        },
                        valueChange = cryptoAsset.change,
                        icon = ImageResource.Remote(cryptoAsset.icon),
                        onClick = onAssetClick
                    )
                    if (index < cryptoAssets.lastIndex) {
                        Divider(color = Color(0XFFF1F2F7))
                    }
                }
            }
        } else {
            CryptoAssetsNoResults()
        }
    }
}

@Composable
fun CryptoAssetsNoResults() {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing),
        text = "\uD83D\uDE1E No results",
        style = AppTheme.typography.body2,
        color = AppTheme.colors.title,
        textAlign = TextAlign.Center
    )
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewCryptoAssetsScreen() {
    CryptoAssetsScreen(
        cryptoAssets = DataResource.Data(
            listOf(
                CryptoAssetState(
                    icon = "",
                    name = "Ethereum",
                    balance = DataResource.Data(Money.fromMajor(FiatCurrency.Dollars, 306.28.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Up(3.94)),
                    fiatBalance = DataResource.Data(Money.fromMajor(FiatCurrency.Dollars, 306.28.toBigDecimal()))
                ),
                CryptoAssetState(
                    icon = "",
                    name = "Bitcoin",
                    balance = DataResource.Loading,
                    change = DataResource.Loading,
                    fiatBalance = DataResource.Loading
                ),
                CryptoAssetState(
                    icon = "",
                    name = "Solana",
                    balance = DataResource.Data(Money.fromMajor(FiatCurrency.Dollars, 306.28.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Down(2.32)),
                    fiatBalance = DataResource.Data(Money.fromMajor(FiatCurrency.Dollars, 306.28.toBigDecimal()))
                )
            )
        ),
        onSearchTermEntered = {},
        filters = listOf(),
        {}
    )
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewCryptoAssetsScreen_Empty() {
    CryptoAssetsScreen(
        cryptoAssets = DataResource.Data(
            listOf()
        ),
        onSearchTermEntered = {},
        filters = listOf(),
        {}
    )
}

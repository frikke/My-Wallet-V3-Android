package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.home.presentation.allassets.AssetsIntent
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.allassets.CryptoAssetState
import com.blockchain.home.presentation.allassets.FiatAssetState
import com.blockchain.home.presentation.allassets.SectionSize
import com.blockchain.home.presentation.allassets.composable.CryptoAssetsList
import com.blockchain.home.presentation.allassets.composable.CryptoAssetsLoading
import com.blockchain.koin.payloadScope
import info.blockchain.balance.FiatCurrency.Companion.Dollars
import info.blockchain.balance.Money
import org.koin.androidx.compose.getViewModel

@Composable
fun HomeAssets(
    viewModel: AssetsViewModel = getViewModel(scope = payloadScope),
    openAllAssets: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: AssetsViewState? by stateFlowLifecycleAware.collectAsState(null)

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(AssetsIntent.LoadAccounts(SectionSize.Limited()))
        viewModel.onIntent(AssetsIntent.LoadFilters)
        onDispose { }
    }

    viewState?.let { state ->
        HomeAssetsScreen(
            cryptoAssets = state.cryptoAssets.map { it.first },
            showSeeAllCryptoAssets = state.cryptoAssets.map { it.second },
            onSeeAllCryptoAssetsClick = openAllAssets,
            fiatAssets = state.fiatAssets,
        )
    }
}

@Composable
fun HomeAssetsScreen(
    cryptoAssets: DataResource<List<CryptoAssetState>>,
    showSeeAllCryptoAssets: DataResource<Boolean>,
    onSeeAllCryptoAssetsClick: () -> Unit,
    fiatAssets: DataResource<List<FiatAssetState>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.ma_home_assets_title),
                style = AppTheme.typography.body2,
                color = Grey700
            )

            Spacer(modifier = Modifier.weight(1f))

            // todo a trb decision will be made about this
            //            if ((showSeeAllCryptoAssets as? DataResource.Data)?.data == true) {
            Text(
                modifier = Modifier.clickableNoEffect(onSeeAllCryptoAssetsClick),
                text = stringResource(R.string.see_all),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.primary,
            )
            //            }
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        when (cryptoAssets) {
            DataResource.Loading -> {
                CryptoAssetsLoading()
            }
            is DataResource.Error -> {
                // todo
            }
            is DataResource.Data -> {
                if (cryptoAssets.data.isNotEmpty()) {
                    CryptoAssetsList(cryptoAssets = cryptoAssets.data)
                }
            }
        }

        when (fiatAssets) {
            DataResource.Loading -> {
                ShimmerLoadingTableRow()
                ShimmerLoadingTableRow()
            }
            is DataResource.Error -> {
                // todo
            }
            is DataResource.Data -> {
                if (fiatAssets.data.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                    Column(
                        modifier = Modifier.background(
                            color = AppTheme.colors.background,
                            shape = RoundedCornerShape(AppTheme.dimensions.smallSpacing)
                        )
                    ) {
                        fiatAssets.data.forEachIndexed { index, fiatAsset ->
                            BalanceChangeTableRow(
                                name = fiatAsset.name,
                                value = fiatAsset.balance.map {
                                    it.toStringWithSymbol()
                                },
                                icon = ImageResource.Remote(fiatAsset.icon),
                                onClick = {
                                }
                            )

                            if (index < fiatAssets.data.lastIndex) {
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewHomeAccounts() {
    HomeAssetsScreen(
        cryptoAssets = DataResource.Data(
            listOf(
                CryptoAssetState(
                    icon = "",
                    name = "Ethereum",
                    balance = DataResource.Data(Money.fromMajor(Dollars, 128.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Up(3.94)),
                    fiatBalance = DataResource.Data(Money.fromMajor(Dollars, 112328.toBigDecimal()))
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
                    balance = DataResource.Data(Money.fromMajor(Dollars, 555.28.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Down(2.32)),
                    fiatBalance = DataResource.Data(Money.fromMajor(Dollars, 1.28.toBigDecimal()))
                )
            )
        ),
        fiatAssets = DataResource.Data(
            listOf(
                FiatAssetState(
                    icon = "",
                    name = "US Dollar",
                    balance = DataResource.Data(Money.fromMajor(Dollars, 123.28.toBigDecimal())),
                ),
                FiatAssetState(
                    icon = "",
                    name = "Euro",
                    balance = DataResource.Loading,
                )
            )
        ),
        showSeeAllCryptoAssets = DataResource.Data(true),
        onSeeAllCryptoAssetsClick = {},
    )
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewHomeAccounts_Loading() {
    HomeAssetsScreen(
        cryptoAssets = DataResource.Loading,
        fiatAssets = DataResource.Loading,
        showSeeAllCryptoAssets = DataResource.Data(false),
        onSeeAllCryptoAssetsClick = {},
    )
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewHomeAccounts_LoadingFiat() {
    HomeAssetsScreen(
        cryptoAssets = DataResource.Data(
            listOf(
                CryptoAssetState(
                    icon = "",
                    name = "Ethereum",
                    balance = DataResource.Data(Money.fromMajor(Dollars, 306.28.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Up(3.94)),
                    fiatBalance = DataResource.Data(Money.fromMajor(Dollars, 306.28.toBigDecimal()))
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
                    balance = DataResource.Data(Money.fromMajor(Dollars, 306.28.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Down(2.32)),
                    fiatBalance = DataResource.Data(Money.fromMajor(Dollars, 306.28.toBigDecimal()))
                )
            )
        ),
        fiatAssets = DataResource.Loading,
        showSeeAllCryptoAssets = DataResource.Data(true),
        onSeeAllCryptoAssetsClick = {},
    )
}

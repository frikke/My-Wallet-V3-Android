package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.blockchain.home.presentation.dashboard.HomeCryptoAsset
import com.blockchain.home.presentation.dashboard.HomeFiatAsset
import info.blockchain.balance.FiatCurrency.Companion.Dollars
import info.blockchain.balance.Money

@Composable
fun HomeAssets(
    cryptoAssets: DataResource<List<HomeCryptoAsset>>,
    showSeeAllCryptoAssets: DataResource<Boolean>,
    onSeeAllCryptoAssetsClick: () -> Unit,
    fiatAssets: DataResource<List<HomeFiatAsset>>
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

            if ((showSeeAllCryptoAssets as? DataResource.Data)?.data == true) {
                Text(
                    modifier = Modifier.clickableNoEffect(onSeeAllCryptoAssetsClick),
                    text = stringResource(R.string.see_all),
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        when (cryptoAssets) {
            DataResource.Loading -> {
                ShimmerLoadingTableRow()
                ShimmerLoadingTableRow()
            }
            is DataResource.Error -> {
                // todo
            }
            is DataResource.Data -> {
                if (cryptoAssets.data.isNotEmpty()) {
                    Card(
                        backgroundColor = AppTheme.colors.background,
                        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                        elevation = 0.dp
                    ) {
                        Column {
                            cryptoAssets.data.forEachIndexed { index, cryptoAsset ->
                                BalanceChangeTableRow(
                                    name = cryptoAsset.name,
                                    value = cryptoAsset.fiatBalance.map {
                                        it.toStringWithSymbol()
                                    },
                                    valueChange = cryptoAsset.change,
                                    icon = ImageResource.Remote(cryptoAsset.icon),
                                    onClick = {
                                    }
                                )
                                if (index < cryptoAssets.data.lastIndex) {
                                    Divider(color = Color(0XFFF1F2F7))
                                }
                            }
                        }
                    }
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
    HomeAssets(
        cryptoAssets = DataResource.Data(
            listOf(
                HomeCryptoAsset(
                    icon = "",
                    name = "Ethereum",
                    balance = DataResource.Data(Money.fromMajor(Dollars, 128.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Up(3.94)),
                    fiatBalance = DataResource.Data(Money.fromMajor(Dollars, 112328.toBigDecimal()))
                ),
                HomeCryptoAsset(
                    icon = "",
                    name = "Bitcoin",
                    balance = DataResource.Loading,
                    change = DataResource.Loading,
                    fiatBalance = DataResource.Loading
                ),
                HomeCryptoAsset(
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
                HomeFiatAsset(
                    icon = "",
                    name = "US Dollar",
                    balance = DataResource.Data(Money.fromMajor(Dollars, 123.28.toBigDecimal())),
                ),
                HomeFiatAsset(
                    icon = "",
                    name = "Euro",
                    balance = DataResource.Loading,
                )
            )
        ),
        showSeeAllCryptoAssets = DataResource.Data(true),
        onSeeAllCryptoAssetsClick = {}
    )
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewHomeAccounts_Loading() {
    HomeAssets(
        cryptoAssets = DataResource.Loading,
        fiatAssets = DataResource.Loading,
        showSeeAllCryptoAssets = DataResource.Data(false),
        onSeeAllCryptoAssetsClick = {}
    )
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewHomeAccounts_LoadingFiat() {
    HomeAssets(
        cryptoAssets = DataResource.Data(
            listOf(
                HomeCryptoAsset(
                    icon = "",
                    name = "Ethereum",
                    balance = DataResource.Data(Money.fromMajor(Dollars, 306.28.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Up(3.94)),
                    fiatBalance = DataResource.Data(Money.fromMajor(Dollars, 306.28.toBigDecimal()))
                ),
                HomeCryptoAsset(
                    icon = "",
                    name = "Bitcoin",
                    balance = DataResource.Loading,
                    change = DataResource.Loading,
                    fiatBalance = DataResource.Loading
                ),
                HomeCryptoAsset(
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
        onSeeAllCryptoAssetsClick = {}
    )
}

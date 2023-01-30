package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Question
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.tablerow.BalanceChangeTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.data.map
import com.blockchain.home.presentation.allassets.CustodialAssetState
import com.blockchain.home.presentation.allassets.FiatAssetState
import com.blockchain.home.presentation.allassets.HomeAsset
import com.blockchain.home.presentation.allassets.NonCustodialAssetState
import com.blockchain.home.presentation.allassets.composable.BalanceWithFiatAndCryptoBalance
import com.blockchain.home.presentation.allassets.composable.BalanceWithPriceChange
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

@Composable
fun HomeAssetsHeader(openCryptoAssets: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ma_home_assets_title),
            style = AppTheme.typography.body2,
            color = Grey700
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.clickableNoEffect(openCryptoAssets),
            text = stringResource(R.string.see_all),
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.primary,
        )
    }
}

@Composable
fun FundLocksData(
    total: Money,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick),
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.funds_locked_warning_title),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.muted
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

            Image(Icons.Question.withTint(Grey400).withSize(14.dp))

            Spacer(modifier = Modifier.weight(1F))

            Text(
                text = total.toStringWithSymbol(),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.muted
            )
        }
    }
}

internal fun LazyListScope.homeAssets(
    data: List<HomeAsset>,
    assetOnClick: (AssetInfo) -> Unit,
    openFiatActionDetail: (String) -> Unit
) {
    roundedCornersItems(items = data.filterIsInstance<CustodialAssetState>(), key = { state ->
        state.asset.networkTicker
    }) {
        BalanceWithPriceChange(
            cryptoAsset = it,
            onAssetClick = assetOnClick
        )
    }
    roundedCornersItems(items = data.filterIsInstance<NonCustodialAssetState>(), key = { state ->
        state.asset.networkTicker
    }) {
        BalanceWithFiatAndCryptoBalance(
            cryptoAsset = it,
            onAssetClick = assetOnClick
        )
    }

    item {
        val fiatSpacer = if (data.filterIsInstance<CustodialAssetState>().isNotEmpty() &&
            data.filterIsInstance<FiatAssetState>().isNotEmpty()
        ) {
            AppTheme.dimensions.smallSpacing
        } else
            0.dp
        Spacer(modifier = Modifier.size(fiatSpacer))
    }

    roundedCornersItems(items = data.filterIsInstance<FiatAssetState>(), key = { state ->
        state.account.currency.networkTicker
    }) {
        BalanceChangeTableRow(
            name = it.name,
            value = it.balance.map { money ->
                money.toStringWithSymbol()
            },
            contentStart = {
                Image(
                    imageResource = ImageResource.Remote(it.icon[0]),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(dimensionResource(R.dimen.standard_spacing)),
                    defaultShape = CircleShape
                )
            },
            onClick = {
                openFiatActionDetail(it.account.currency.networkTicker)
            }
        )
    }
}
/*
@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewHomeAccounts() {
    HomeAssetsScreen(
        assets = DataResource.Data(
            listOf(
                CustodialAssetState(
                    icon = listOf(""),
                    name = "Ethereum",
                    balance = DataResource.Data(Money.fromMajor(Dollars, 128.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Up(3.94)),
                    fiatBalance = DataResource.Data(Money.fromMajor(Dollars, 112328.toBigDecimal())),
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
                    balance = DataResource.Data(Money.fromMajor(Dollars, 555.28.toBigDecimal())),
                    change = DataResource.Data(ValueChange.Down(2.32)),
                    fiatBalance = DataResource.Data(Money.fromMajor(Dollars, 1.28.toBigDecimal())),
                    asset = CryptoCurrency.ETHER
                )
            ) +

                listOf(
                    FiatAssetState(
                        icon = listOf(""),
                        name = "US Dollar",
                        balance = DataResource.Data(Money.fromMajor(Dollars, 123.28.toBigDecimal())),
                        fiatBalance = DataResource.Data(Money.fromMajor(Dollars, 123.28.toBigDecimal())),
                        account = NullFiatAccount
                    ),
                    FiatAssetState(
                        icon = listOf(""),
                        name = "Euro",
                        balance = DataResource.Loading,
                        fiatBalance = DataResource.Loading,
                        account = NullFiatAccount
                    )
                )
        ),
        fundsLocks = DataResource.Data(
            FundsLocks(
                onHoldTotalAmount = Money.fromMajor(Dollars, 100.28.toBigDecimal()),
                locks = listOf()
            )
        ),
        onSeeAllCryptoAssetsClick = {},
        onFundsLocksClick = {},
        onAssetClick = {},
        openFiatActionDetail = {}
    )
}

@Preview(backgroundColor = 0xFF272727)
@Composable
fun PreviewHomeAccounts_Loading() {
    HomeAssetsScreen(
        assets = DataResource.Loading,
        fundsLocks = DataResource.Loading,
        onSeeAllCryptoAssetsClick = {},
        onFundsLocksClick = {},
        onAssetClick = {},
        openFiatActionDetail = {}
    )
}*/

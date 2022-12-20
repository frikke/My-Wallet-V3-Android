package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.system.ShimmerLoadingBox
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.allassets.WalletBalance
import com.blockchain.koin.payloadScope
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import org.koin.androidx.compose.getViewModel

@Composable
fun Balance(
    viewModel: AssetsViewModel = getViewModel(scope = payloadScope)
) {
    val viewState: AssetsViewState by viewModel.viewState.collectAsStateLifecycleAware()

    BalanceScreen(walletBalance = viewState.balance)
}

@Composable
fun BalanceScreen(
    walletBalance: WalletBalance
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.dimensions.standardSpacing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TotalBalance(balance = walletBalance.balance)

        BalanceDifference(
            balanceDifference = walletBalance.cryptoBalanceDifference24h,
            percentChange = walletBalance.percentChange
        )
    }
}

@Composable
fun ColumnScope.TotalBalance(balance: DataResource<Money>) {
    when (balance) {
        DataResource.Loading -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1F))
                ShimmerLoadingBox(
                    modifier = Modifier
                        .height(AppTheme.dimensions.largeSpacing)
                        .weight(1F)
                )
                Spacer(modifier = Modifier.weight(1F))
            }
        }

        is DataResource.Data -> {
            Text(
                text = balance.data.toStringWithSymbol(),
                style = AppTheme.typography.title1,
                color = AppTheme.colors.title
            )
        }

        is DataResource.Error -> {
            // todo(othman) checking with Ethan
        }
    }
}

@Composable
fun ColumnScope.BalanceDifference(
    balanceDifference: DataResource<Money>,
    percentChange: DataResource<ValueChange>
) {
    val difference = combineDataResources(
        balanceDifference,
        percentChange
    ) { balanceDifferenceData, percentChangeData -> balanceDifferenceData to percentChangeData }

    when (difference) {
        DataResource.Loading -> {
            // n/a
        }

        is DataResource.Data -> {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            difference.data.let { (balanceDifference, percentChange) ->
                Text(
                    text =
                    "${percentChange.indicator} ${balanceDifference.toStringWithSymbol()} (${percentChange.value}%)",
                    style = AppTheme.typography.paragraph2,
                    color = percentChange.color
                )
            }
        }

        is DataResource.Error -> {
            // todo(othman) checking with Ethan
        }
    }
}

@Preview
@Composable
fun PreviewBalanceScreen() {
    BalanceScreen(
        walletBalance =
        WalletBalance(
            balance = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())),
            cryptoBalanceDifference24h = DataResource.Data(Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal())),
        )
    )
}

@Preview
@Composable
fun PreviewBalanceScreenLoading() {
    BalanceScreen(
        walletBalance = WalletBalance(
            balance = DataResource.Loading,
            cryptoBalanceDifference24h = DataResource.Loading,
        )
    )
}

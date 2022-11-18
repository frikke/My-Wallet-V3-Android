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
    walletBalance: DataResource<WalletBalance>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.dimensions.standardSpacing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (walletBalance) {
            DataResource.Loading -> {
                BalanceLoading()
            }

            is DataResource.Data -> {
                BalanceData(walletBalance.data)
            }

            is DataResource.Error -> {
                // todo(othman) checking with Ethan
            }
        }
    }
}

@Composable
fun ColumnScope.BalanceLoading() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1F))
        ShimmerLoadingBox(
            modifier = Modifier
                .height(AppTheme.dimensions.largeSpacing)
                .weight(1F)
        )
        Spacer(modifier = Modifier.weight(1F))
    }

    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1F))
        ShimmerLoadingBox(
            modifier = Modifier
                .height(AppTheme.dimensions.mediumSpacing)
                .weight(0.5F)
        )
        Spacer(modifier = Modifier.weight(1F))
    }
}

@Composable
fun ColumnScope.BalanceData(data: WalletBalance) {
    with(data) {
        Text(
            text = balance.toStringWithSymbol(),
            style = AppTheme.typography.title1,
            color = AppTheme.colors.title
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Text(
            text = "${percentChange.indicator} ${balanceDifference24h.toStringWithSymbol()} (${percentChange.value}%)",
            style = AppTheme.typography.paragraph2,
            color = percentChange.color
        )
    }
}
//

@Preview
@Composable
fun PreviewBalanceScreen() {
    BalanceScreen(
        walletBalance = DataResource.Data(
            WalletBalance(
                balance = Money.fromMajor(CryptoCurrency.ETHER, 1234.toBigDecimal()),
                balanceDifference24h = Money.fromMajor(CryptoCurrency.ETHER, 12.3.toBigDecimal()),
                percentChange = ValueChange.Up((7.18))
            )
        )
    )
}

@Preview
@Composable
fun PreviewBalanceScreenLoading() {
    BalanceScreen(
        walletBalance = DataResource.Loading
    )
}

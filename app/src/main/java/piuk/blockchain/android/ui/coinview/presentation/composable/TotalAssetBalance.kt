package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.sectionheader.BalanceSectionHeader
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewTotalBalance

@Composable
fun TotalBalance(
    data: CoinviewTotalBalance
) {
    when (data) {
        CoinviewTotalBalance.NotSupported -> {
            // don't show any view
        }

        CoinviewTotalBalance.Loading -> {
            TotalBalanceLoading()
        }

        is CoinviewTotalBalance.Data -> {
            TotalBalanceData(
                data = data
            )
        }
    }
}

@Composable
fun TotalBalanceLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ShimmerLoadingTableRow(showIconLoader = false)
    }
}

@Composable
fun TotalBalanceData(
    data: CoinviewTotalBalance.Data
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BalanceSectionHeader(
            labelText = stringResource(R.string.coinview_balance_label, data.assetName),
            primaryText = data.totalFiatBalance,
            secondaryText = data.totalCryptoBalance
        )
    }
}

@Preview
@Composable
fun PreviewTotalBalance_Loading() {
    TotalBalance(CoinviewTotalBalance.Loading)
}

@Preview
@Composable
fun PreviewTotalBalance_Data() {
    TotalBalance(
        CoinviewTotalBalance.Data(
            assetName = "Ethereum",
            totalFiatBalance = "$4,570.27",
            totalCryptoBalance = "969.25 BTC",
        )
    )
}

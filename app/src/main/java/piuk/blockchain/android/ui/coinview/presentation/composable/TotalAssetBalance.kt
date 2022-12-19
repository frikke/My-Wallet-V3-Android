package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.theme.AppTheme
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewTotalBalanceState

@Composable
fun TotalBalance(
    totalBalanceData: CoinviewTotalBalanceState
) {
    when (totalBalanceData) {
        CoinviewTotalBalanceState.NotSupported -> {
            Empty()
        }

        CoinviewTotalBalanceState.Loading -> {
            TotalBalanceLoading()
        }

        CoinviewTotalBalanceState.Error -> {
            Empty()
        }

        is CoinviewTotalBalanceState.Data -> {
            TotalBalanceData(
                totalBalanceData = totalBalanceData
            )
        }
    }
}

@Composable
fun TotalBalanceLoading() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ShimmerLoadingTableRow(showIconLoader = false, showBottomBlock = false)
    }
}

@Composable
fun TotalBalanceData(
    totalBalanceData: CoinviewTotalBalanceState.Data
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = AppTheme.dimensions.tinySpacing,
                horizontal = AppTheme.dimensions.smallSpacing
            )
    ) {
        Text(
            modifier = Modifier.weight(1F),
            text = stringResource(R.string.common_balance),
            style = AppTheme.typography.body2,
            color = AppTheme.colors.muted
        )

        Text(
            text = totalBalanceData.totalFiatBalance,
            style = AppTheme.typography.body2,
            color = AppTheme.colors.title,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTotalBalance_Loading() {
    TotalBalance(CoinviewTotalBalanceState.Loading)
}

@Preview(showBackground = true)
@Composable
fun PreviewTotalBalance_Data() {
    TotalBalance(
        CoinviewTotalBalanceState.Data(
            assetName = "Ethereum",
            totalFiatBalance = "$4,570.27",
            totalCryptoBalance = "969.25 BTC",
        )
    )
}
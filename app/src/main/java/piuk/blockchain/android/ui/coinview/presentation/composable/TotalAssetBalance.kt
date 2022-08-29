package piuk.blockchain.android.ui.coinview.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sectionheader.BalanceSectionHeader
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.coinview.presentation.CoinviewTotalBalanceState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewWatchlistState

@Composable
fun TotalBalance(
    totalBalanceData: CoinviewTotalBalanceState,
    watchlistData: CoinviewWatchlistState,
    onWatchlistClick: () -> Unit
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
                totalBalanceData = totalBalanceData,
                watchlistData = watchlistData,
                onWatchlistClick = onWatchlistClick
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
    totalBalanceData: CoinviewTotalBalanceState.Data,
    watchlistData: CoinviewWatchlistState,
    onWatchlistClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BalanceSectionHeader(
            labelText = stringResource(R.string.coinview_balance_label, totalBalanceData.assetName),
            primaryText = totalBalanceData.totalFiatBalance,
            secondaryText = totalBalanceData.totalCryptoBalance,
            iconResource = ImageResource.Local(
                if (watchlistData is CoinviewWatchlistState.Data && watchlistData.isInWatchlist) {
                    R.drawable.ic_star_filled
                } else {
                    R.drawable.ic_star
                }
            ),
            onIconClick = onWatchlistClick,
            shouldShowIcon = watchlistData is CoinviewWatchlistState.Data
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTotalBalance_Loading() {
    TotalBalance(CoinviewTotalBalanceState.Loading, CoinviewWatchlistState.Loading, {})
}

@Preview(showBackground = true)
@Composable
fun PreviewTotalBalance_Data_Watchlist_Loading() {
    TotalBalance(
        CoinviewTotalBalanceState.Data(
            assetName = "Ethereum",
            totalFiatBalance = "$4,570.27",
            totalCryptoBalance = "969.25 BTC",
        ),
        CoinviewWatchlistState.Loading,
        {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewTotalBalance_Data_Watchlist_True() {
    TotalBalance(
        CoinviewTotalBalanceState.Data(
            assetName = "Ethereum",
            totalFiatBalance = "$4,570.27",
            totalCryptoBalance = "969.25 BTC",
        ),
        CoinviewWatchlistState.Data(true),
        {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewTotalBalance_Data_Watchlist_False() {
    TotalBalance(
        CoinviewTotalBalanceState.Data(
            assetName = "Ethereum",
            totalFiatBalance = "$4,570.27",
            totalCryptoBalance = "969.25 BTC",
        ),
        CoinviewWatchlistState.Data(false),
        {}
    )
}

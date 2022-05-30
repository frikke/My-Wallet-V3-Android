package piuk.blockchain.android.ui.interest.presentation.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.Search
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.interest.domain.model.AssetInterestDetail
import piuk.blockchain.android.ui.interest.domain.model.InterestAsset
import piuk.blockchain.android.ui.interest.presentation.InterestDashboardViewState

@Composable
fun InterestDashboardScreen(
    viewState: InterestDashboardViewState,
    loadDashboard: () -> Unit,
    startKyc: () -> Unit,
    interestItemClicked: (cryptoCurrency: AssetInfo, hasBalance: Boolean) -> Unit,
    filterData: (String) -> Unit
) {
    with(viewState) {
        when {
            isLoading -> {
                InterestDashboardLoading()
            }

            isError -> {
                InterestDashboardError(loadDashboard)
            }

            isLoading.not() && isError.not() -> {
                Column {
                    if (isKycGold.not()) {
                        LazyColumn {
                            item {
                                InterestDashboardVerificationItem(startKyc)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.padding(
                                start = dimensionResource(R.dimen.standard_margin),
                                end = dimensionResource(R.dimen.standard_margin)
                            )
                        ) {
                            Search(
                                label = stringResource(R.string.search_coins_hint),
                                onValueChange = filterData
                            )
                        }

                        LazyColumn {
                            items(
                                items = data,
                            ) {
                                InterestDashboardAssetItem(
                                    assetInfo = it.assetInfo,
                                    assetInterestDetail = it.interestDetail,
                                    isKycGold = isKycGold,
                                    interestItemClicked = interestItemClicked
                                )
                            }

                            item {
                                Spacer(Modifier.size(dimensionResource(R.dimen.standard_margin)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
fun PreviewInterestDashboardScreenLoading() {
    InterestDashboardScreen(
        InterestDashboardViewState(
            isLoading = true,
            isError = false,
            isKycGold = false,
            data = listOf()
        ),
        {}, {}, { _, _ -> }, {}
    )
}

@Preview(name = "Error", showBackground = true)
@Composable
fun PreviewInterestDashboardScreenError() {
    InterestDashboardScreen(
        InterestDashboardViewState(
            isLoading = false,
            isError = true,
            isKycGold = false,
            data = listOf()
        ),
        {}, {}, { _, _ -> }, {}
    )
}

private val list = listOf(
    InterestAsset(
        CryptoCurrency.ETHER,
        AssetInterestDetail(
            totalInterest = Money.fromMajor(CryptoCurrency.ETHER, 100.toBigDecimal()),
            totalBalance = Money.fromMajor(CryptoCurrency.ETHER, 100.toBigDecimal()),
            rate = 2.3,
            eligible = true,
            ineligibilityReason = IneligibilityReason.KYC_TIER,
            totalBalanceFiat = Money.fromMajor(CryptoCurrency.ETHER, 100.toBigDecimal())
        )
    ),
    InterestAsset(
        CryptoCurrency.BTC,
        null
    ),
    InterestAsset(
        CryptoCurrency.XLM,
        AssetInterestDetail(
            totalInterest = Money.fromMajor(CryptoCurrency.XLM, 0.toBigDecimal()),
            totalBalance = Money.fromMajor(CryptoCurrency.XLM, 0.toBigDecimal()),
            rate = 4.3,
            eligible = true,
            ineligibilityReason = IneligibilityReason.KYC_TIER,
            totalBalanceFiat = Money.fromMajor(CryptoCurrency.XLM, 0.toBigDecimal())
        )
    )
)

// could not preview KYC view as Glide raises an error
@Preview(name = "Data without Kyc", showBackground = true)
@Composable
fun PreviewInterestDashboardScreenDataNoKyc() {
    InterestDashboardScreen(
        InterestDashboardViewState(
            isLoading = false,
            isError = false,
            isKycGold = true,
            data = list
        ),
        {}, {}, { _, _ -> }, {}
    )
}

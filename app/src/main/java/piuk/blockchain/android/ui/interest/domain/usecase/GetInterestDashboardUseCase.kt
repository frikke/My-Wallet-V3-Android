package piuk.blockchain.android.ui.interest.domain.usecase

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.store.filterNotLoading
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import piuk.blockchain.android.ui.interest.domain.model.AssetInterestDetail
import piuk.blockchain.android.ui.interest.domain.model.InterestAsset

class GetInterestDashboardUseCase(
    private val interestService: InterestService,
    private val exchangeRatesManager: ExchangeRatesDataManager
) {
    @OptIn(FlowPreview::class)
    operator fun invoke(): Flow<DataResource<List<InterestAsset>>> {
        return interestService.getAvailableAssetsForInterestFlow()
            .flatMapMerge { dataResource ->
                when (dataResource) {
                    is DataResource.Data -> {
                        val interestAssetFlowList = dataResource.data
                            .map { asset ->
                                mapToInterestAsset(asset)
                            }

                        combine(interestAssetFlowList) {
                            DataResource.Data(it.toList().sorted())
                        }
                    }

                    is DataResource.Error -> {
                        flowOf(dataResource)
                    }

                    is DataResource.Loading -> {
                        flowOf(dataResource)
                    }
                }
            }
    }

    private fun mapToInterestAsset(asset: AssetInfo): Flow<InterestAsset> {
        // get data for each asset - data is coming as Flow
        // no need to handle loadings for this
        //
        // -> we only want the base data loading from getAvailableAssetsForInterestFlow
        // loading state for composite data is irrelevant for this use case
        return combine(
            interestService.getBalanceForFlow(asset).filterNotLoading(),
            interestService.getInterestRateFlow(asset).filterNotLoading(),
            interestService.getEligibilityForAssetFlow(asset).filterNotLoading(),
            exchangeRatesManager.exchangeRateToUserFiatFlow(asset).filterNotLoading()
        ) { balance, interestRate, eligibility, exchangeRate ->
            when {
                // if any of the data is an error - interestDetail is null
                listOf(
                    balance, interestRate, eligibility, exchangeRate
                ).any { it is DataResource.Error } -> {
                    InterestAsset(
                        assetInfo = asset,
                        interestDetail = null
                    )
                }

                // otherwise (all are successful - remember no loading state is coming)
                // we finally create InterestAsset
                else -> {
                    balance as DataResource.Data
                    interestRate as DataResource.Data
                    eligibility as DataResource.Data
                    exchangeRate as DataResource.Data

                    InterestAsset(
                        assetInfo = asset,
                        interestDetail = AssetInterestDetail(
                            totalInterest = balance.data.totalInterest,
                            totalBalance = balance.data.totalBalance,
                            rate = interestRate.data,
                            eligibility = eligibility.data,
                            totalBalanceFiat = exchangeRate.data.convert(
                                balance.data.totalBalance
                            )
                        )
                    )
                }
            }
        }
    }

    private fun List<InterestAsset>.sorted() = sortedWith(
        compareByDescending<InterestAsset> { asset ->
            // 1. first sort highest balances
            asset.interestDetail?.totalBalanceFiat?.toBigDecimal()
        }.thenBy(nullsLast()) { asset ->
            // 2. when balances are the same ^ (i.e. when we reach zero balances),
            // sort by priority, ref https://blockchain.atlassian.net/browse/AND-5925
            // using nullsLast() will automatically sort prioritizedCurrencies first
            // leaving the rest to be sorted by name v
            prioritizedCurrencies.find { it.name == asset.assetInfo.networkTicker }?.priority
        }.thenBy { asset ->
            // 3. for the rest, sort alphabetically
            asset.assetInfo.name
        }
    )

    private data class AssetInfoPriority(val name: String, val priority: Int)

    private val prioritizedCurrencies = listOf(
        AssetInfoPriority(name = "BTC", priority = 1),
        AssetInfoPriority(name = "ETH", priority = 2),
        AssetInfoPriority(name = "USDC", priority = 3),
        AssetInfoPriority(name = "USDT", priority = 4),
    )
}

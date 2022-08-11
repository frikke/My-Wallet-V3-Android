package piuk.blockchain.android.ui.interest.domain.usecase

import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.store.filterNotLoading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import piuk.blockchain.android.ui.interest.domain.model.AssetInterestDetail
import piuk.blockchain.android.ui.interest.domain.model.InterestAsset

class GetInterestDashboardUseCase(
    private val interestService: InterestService,
    private val exchangeRatesManager: ExchangeRatesDataManager,
) {
    suspend operator fun invoke(): Flow<DataResource<List<InterestAsset>>> = flow {
        interestService.getAvailableAssetsForInterestFlow()
            .onEach { dataResource ->
                when (dataResource) {
                    is DataResource.Data -> {
                        /**
                         * because every InterestAsset data is coming as flow (see combine below)
                         * (since we always want up to date data)
                         * we need to collect every change, upsert it into the list and then emit the result
                         *
                         * we can't return the flow without collecting and updating the list
                         * otherwise the return type would have to be
                         * Flow<DataResource<List<*Flow*<InterestAsset>>>>
                         */
                        val finalList = mutableListOf<InterestAsset>()

                        dataResource.data
                            .map { asset ->
                                // get data for each asset - data is coming as Flow
                                // no need to handle loadings for this
                                //
                                // -> we only want the base data loading from getAvailableAssetsForInterestFlow
                                // loading state for composite data is irrelevant for this use case
                                combine(
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
                            // the map{} above returns a List<Flow<InterestAsset>>
                            // so we merge them together and collect them as they are emitted
                            .merge()
                            .onEach { interestAsset ->
                                // each InterestAsset that is received is upserted it into the list
                                finalList
                                    .apply {
                                        removeIf { it.assetInfo.networkTicker == interestAsset.assetInfo.networkTicker }
                                        add(interestAsset)
                                    }
                                    .also {
                                        // finally the full list is returned
                                        emit(DataResource.Data(it.sorted()))
                                    }

                                // thoughts:
                                // this way whenever any data is changed and emitted by its flow
                                // for example getBalanceForFlow returned a new value
                                // "combine" is gonna emit a new InterestAsset
                                // will be caught here in the onEach and processed
                            }.collect()
                    }

                    is DataResource.Error -> {
                        emit(dataResource)
                    }

                    is DataResource.Loading -> {
                        emit(dataResource)
                    }
                }
            }.collect()
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

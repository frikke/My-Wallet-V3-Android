package piuk.blockchain.android.ui.interest.tbm.domain.usecase

import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.domain.repository.AssetInterestService

class GetAssetInterestInfoUseCase(private val service: AssetInterestService) {
    suspend operator fun invoke(assets: List<AssetInfo>): Outcome<Throwable, List<AssetInterestInfo>> =
        service.getAssetsInterestInfo(assets).map {
            it.sortedWith(
                compareByDescending<AssetInterestInfo> { asset ->
                    // 1. first sort highest balances
                    asset.assetInterestDetail?.totalBalanceFiat?.toBigDecimal()
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
        }

    private data class AssetInfoPriority(val name: String, val priority: Int)

    private val prioritizedCurrencies = listOf(
        AssetInfoPriority(name = "BTC", priority = 1),
        AssetInfoPriority(name = "ETH", priority = 2),
        AssetInfoPriority(name = "USDC", priority = 3),
        AssetInfoPriority(name = "USDT", priority = 4),
    )
}


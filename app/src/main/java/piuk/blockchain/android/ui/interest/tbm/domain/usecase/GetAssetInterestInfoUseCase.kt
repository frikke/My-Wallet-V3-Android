package piuk.blockchain.android.ui.interest.tbm.domain.usecase

import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.domain.repository.AssetInterestRepository

class GetAssetInterestInfoUseCase(private val repository: AssetInterestRepository) {
    suspend operator fun invoke(assets: List<AssetInfo>): Outcome<Throwable, List<AssetInterestInfo>> =
        repository.getAssetsInterestInfo(assets).map {
            it.sortedWith(
                compareByDescending<AssetInterestInfo> { asset ->
                    asset.assetInterestDetail?.totalBalanceFiat?.toBigDecimal()
                }.thenBy(nullsLast()) { asset ->
                    prioritizedCurrencies.find { it.name == asset.assetInfo.networkTicker }?.priority
                }.thenBy { asset ->
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


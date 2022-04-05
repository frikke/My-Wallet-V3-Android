package piuk.blockchain.android.ui.interest.tbm.domain.usecase

import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.domain.repository.AssetInterestRepository

class GetAssetInterestInfoUseCase(private val repository: AssetInterestRepository) {
    suspend operator fun invoke(assets: List<AssetInfo>): Outcome<Throwable, List<AssetInterestInfo>> =
        repository.getAssetsInterestInfo(assets).map {
            it.sortedByDescending { assetInterestInfo ->
                assetInterestInfo.assetInterestDetail?.totalBalanceFiat?.toBigDecimal()
            }
        }
}
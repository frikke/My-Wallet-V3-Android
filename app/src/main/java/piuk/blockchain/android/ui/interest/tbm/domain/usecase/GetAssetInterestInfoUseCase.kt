package piuk.blockchain.android.ui.interest.tbm.domain.usecase

import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.domain.repository.AssetInterestRepository

class GetAssetInterestInfoUseCase(private val repository: AssetInterestRepository) {
    suspend operator fun invoke(assets: List<AssetInfo>): Result<List<AssetInterestInfo>> =
        repository.getAssetsInterestInfo(assets).map {
            it.sortedByDescending { assetInterestInfo ->
                assetInterestInfo.assetInterestDetail?.totalBalanceFiat?.toBigDecimal()
            }
        }
}
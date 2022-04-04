package piuk.blockchain.android.ui.interest.tbm.domain.repository

import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.InterestDetail
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo

interface AssetInterestRepository {
    suspend fun getInterestDetail(): Result<InterestDetail>
    suspend fun getAssetsInterestInfo(assets: List<AssetInfo>): Result<List<AssetInterestInfo>>
}
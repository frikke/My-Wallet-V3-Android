package piuk.blockchain.android.ui.interest.tbm.domain.repository

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.InterestDetail

interface AssetInterestRepository {
    suspend fun getInterestDetail(): Result<InterestDetail>
    suspend fun getAssetsInterestInfo(assets: List<AssetInfo>): Result<List<AssetInterestInfo>>

    // todo move to coincore
    suspend fun getAccountGroup(cryptoCurrency: AssetInfo, filter: AssetFilter): Result<AccountGroup>
}
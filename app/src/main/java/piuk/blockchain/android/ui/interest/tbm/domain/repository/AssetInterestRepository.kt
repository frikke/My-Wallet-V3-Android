package piuk.blockchain.android.ui.interest.tbm.domain.repository

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.tbm.domain.model.InterestDetail

interface AssetInterestRepository {
    suspend fun getInterestDetail(): Outcome<Throwable, InterestDetail>
    suspend fun getAssetsInterestInfo(cryptoCurrencies: List<AssetInfo>): Outcome<Throwable, List<AssetInterestInfo>>

    // todo move to coincore
    suspend fun getAccountGroup(cryptoCurrency: AssetInfo, filter: AssetFilter): Outcome<Throwable, AccountGroup>
}
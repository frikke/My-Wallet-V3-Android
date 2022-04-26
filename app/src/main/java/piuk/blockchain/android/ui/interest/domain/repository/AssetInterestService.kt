package piuk.blockchain.android.ui.interest.domain.repository

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.interest.domain.model.InterestAsset
import piuk.blockchain.android.ui.interest.domain.model.InterestDashboard

interface AssetInterestService {
    suspend fun getInterestDashboard(): Outcome<Throwable, InterestDashboard>
    suspend fun getAssetsInterest(cryptoCurrencies: List<AssetInfo>): Outcome<Throwable, List<InterestAsset>>
    // todo (othman): move to coincore after refactor
    suspend fun getAccountGroup(cryptoCurrency: AssetInfo, filter: AssetFilter): Outcome<Throwable, AccountGroup>
}

package piuk.blockchain.android.ui.interest.domain.usecase

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService

// todo move to coincore module
class GetAccountGroupUseCase(private val service: AssetInterestService) {
    suspend operator fun invoke(
        cryptoCurrency: AssetInfo,
        filter: AssetFilter = AssetFilter.All
    ): Outcome<Throwable, AccountGroup> = service.getAccountGroup(cryptoCurrency = cryptoCurrency, filter = filter)
}
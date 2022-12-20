package piuk.blockchain.android.ui.interest.domain.usecase

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.outcome.Outcome
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.AssetInfo

class GetAccountGroupUseCase(private val coincore: Coincore) {

    suspend operator fun invoke(
        cryptoCurrency: AssetInfo,
        filter: AssetFilter = AssetFilter.All
    ): Outcome<Throwable, AccountGroup> {
        return coincore[cryptoCurrency].accountGroup(filter)
            .toSingle()
            .awaitOutcome()
    }
}

package piuk.blockchain.android.ui.interest.domain.usecase

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

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

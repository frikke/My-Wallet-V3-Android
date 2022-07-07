package piuk.blockchain.android.ui.kyc.address

import com.blockchain.nabu.EthEligibility
import com.blockchain.nabu.datamanagers.NabuDataUserProvider
import io.reactivex.rxjava3.core.Single

class EligibilityForFreeEthAdapter(
    private val nabuDataUserProvider: NabuDataUserProvider
) : EthEligibility {

    override fun isEligible(): Single<Boolean> {
        return nabuDataUserProvider.getUser()
            .map { nabuUser ->
                val userTier = nabuUser.tiers?.current ?: 0
                return@map userTier == 2 && nabuUser.isPowerPaxTagged.not()
            }
    }
}

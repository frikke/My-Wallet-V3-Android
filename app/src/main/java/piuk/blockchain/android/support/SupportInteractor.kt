package piuk.blockchain.android.support

import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.kotlin.Singles

class SupportInteractor(
    private val userIdentity: UserIdentity
) {
    fun loadUserInformation() =
        Singles.zip(
            userIdentity.getHighestApprovedKycTier(),
            userIdentity.getBasicProfileInformation()
        ).map { (tier, basicInfo) ->
            Pair(tier == Tier.GOLD, basicInfo)
        }
}

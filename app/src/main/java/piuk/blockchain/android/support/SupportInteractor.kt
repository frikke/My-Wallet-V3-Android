package piuk.blockchain.android.support

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles

class SupportInteractor(
    private val userIdentity: UserIdentity,
    private val isIntercomEnabledFlag: FeatureFlag
) {
    fun loadUserInformation(): Single<UserInfo> =
        Singles.zip(
            userIdentity.getHighestApprovedKycTier(),
            userIdentity.getBasicProfileInformation(),
            isIntercomEnabledFlag.enabled
        ).map { (tier, basicInfo, isIntercomEnabled) ->
            UserInfo(tier == Tier.GOLD, basicInfo, isIntercomEnabled)
        }
}

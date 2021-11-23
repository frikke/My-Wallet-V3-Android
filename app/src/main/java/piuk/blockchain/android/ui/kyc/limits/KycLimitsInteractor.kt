package piuk.blockchain.android.ui.kyc.limits

import com.blockchain.core.limits.FeatureWithLimit
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single

class KycLimitsInteractor(
    private val limitsDataManager: LimitsDataManager,
    private val userIdentity: UserIdentity
) {
    fun fetchLimits(): Single<List<FeatureWithLimit>> = limitsDataManager.getFeatureLimits()

    fun fetchHighestApprovedTier(): Single<Tier> = userIdentity.getHighestApprovedKycTier()

    fun fetchIsKycRejected(): Single<Boolean> = userIdentity.isKycRejected()

    fun fetchIsGoldKycPending(): Single<Boolean> = userIdentity.isKycPending(Tier.GOLD)
}

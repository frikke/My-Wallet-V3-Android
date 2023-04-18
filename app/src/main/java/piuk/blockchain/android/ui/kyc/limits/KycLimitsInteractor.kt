package piuk.blockchain.android.ui.kyc.limits

import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.limits.FeatureWithLimit
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.store.asSingle
import io.reactivex.rxjava3.core.Single

class KycLimitsInteractor(
    private val limitsDataManager: LimitsDataManager,
    private val kycService: KycService,
    private val userFeaturePermissionService: UserFeaturePermissionService,
) {
    fun fetchLimits(): Single<List<FeatureWithLimit>> = limitsDataManager.getFeatureLimits()

    fun fetchHighestApprovedTier(): Single<KycTier> = kycService.getHighestApprovedTierLevelLegacy()

    fun fetchIsKycRejected(): Single<Boolean> = kycService.isRejected()

    fun fetchIsEligibleToKyc(): Single<Boolean> = userFeaturePermissionService.isEligibleFor(Feature.Kyc).asSingle()

    fun fetchIsGoldKycPending(): Single<Boolean> = kycService.isPendingFor(KycTier.GOLD)
}

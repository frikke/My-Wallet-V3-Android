package piuk.blockchain.android.ui.kyc.limits

import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.limits.FeatureWithLimit
import com.blockchain.core.limits.LimitsDataManager
import io.reactivex.rxjava3.core.Single

class KycLimitsInteractor(
    private val limitsDataManager: LimitsDataManager,
    private val kycService: KycService
) {
    fun fetchLimits(): Single<List<FeatureWithLimit>> = limitsDataManager.getFeatureLimits()

    fun fetchHighestApprovedTier(): Single<KycTier> = kycService.getHighestApprovedTierLevelLegacy()

    fun fetchIsKycRejected(): Single<Boolean> = kycService.isRejected()

    fun fetchIsGoldKycPending(): Single<Boolean> = kycService.isPendingFor(KycTier.GOLD)
}

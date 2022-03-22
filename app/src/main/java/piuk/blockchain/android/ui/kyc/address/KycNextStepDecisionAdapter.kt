package piuk.blockchain.android.ui.kyc.address

import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.datamanagers.kyc.KycDataManager
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.kyc.additional_info.toMutableNode
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

internal class KycNextStepDecisionAdapter(
    private val nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val kycDataManager: KycDataManager
) : KycNextStepDecision {

    override fun nextStep(): Single<KycNextStepDecision.NextStep> =
        Single.zip(
            nabuToken.fetchNabuToken().flatMap(nabuDataManager::getUser),
            rxSingleOutcome { kycDataManager.getAdditionalInfoForm() }
        ) { user, missingAdditionalInfo ->
            if (missingAdditionalInfo.isNotEmpty()) {
                KycNextStepDecision.NextStep.MissingAdditionalInfo(missingAdditionalInfo.toMutableNode())
            } else if (user.tierInProgressOrCurrentTier == 1) {
                KycNextStepDecision.NextStep.Tier1Complete
            } else {
                val tiers = user.tiers
                if (tiers == null || tiers.next ?: 0 > tiers.selected ?: 0) {
                    // the backend is telling us the user should be put down path for tier2 even though they
                    // selected tier 1, so we need to inform them
                    KycNextStepDecision.NextStep.Tier2ContinueTier1NeedsMoreInfo
                } else {
                    KycNextStepDecision.NextStep.Tier2Continue
                }
            }
        }
}

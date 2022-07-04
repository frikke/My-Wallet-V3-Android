package piuk.blockchain.android.ui.kyc.address

import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.nabu.datamanagers.NabuDataUserProvider
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.extensions.rxMaybeOutcome

class KycHomeAddressNextStepDecision(
    private val nabuDataUserProvider: NabuDataUserProvider,
    private val dataRemediationService: DataRemediationService
) : KycNextStepDecision {

    override fun nextStep(): Single<KycNextStepDecision.NextStep> =
        nabuDataUserProvider.getUser()
            .flatMap { user ->
                val tiers = user.tiers
                if (user.tierInProgressOrCurrentTier == 1) {
                    Single.just(KycNextStepDecision.NextStep.Tier1Complete)
                } else if (tiers == null || (tiers.next ?: 0) > (tiers.selected ?: 0)) {
                    // the backend is telling us the user should be put down path for tier2 even though they
                    // selected tier 1, so we need to inform them
                    Single.just(KycNextStepDecision.NextStep.Tier2ContinueTier1NeedsMoreInfo)
                } else {
                    rxMaybeOutcome {
                        dataRemediationService.getQuestionnaire(QuestionnaireContext.TIER_TWO_VERIFICATION)
                    }.map { questionnaire ->
                        KycNextStepDecision.NextStep.Questionnaire(questionnaire) as KycNextStepDecision.NextStep
                    }.defaultIfEmpty(KycNextStepDecision.NextStep.Veriff)
                }
            }
}

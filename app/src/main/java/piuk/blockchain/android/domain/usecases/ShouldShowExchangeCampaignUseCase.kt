package piuk.blockchain.android.domain.usecases

import com.blockchain.domain.mercuryexperiments.MercuryExperimentsService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.ExchangeCampaignPrefs
import io.reactivex.rxjava3.core.Single

class ShouldShowExchangeCampaignUseCase(
    private val exchangeWAPromptFF: FeatureFlag,
    private val exchangeCampaignPrefs: ExchangeCampaignPrefs,
    private val mercuryExperimentsService: MercuryExperimentsService
) {

    operator fun invoke(): Single<Boolean> = exchangeWAPromptFF
        .enabled
        .flatMap { featureEnabled ->
            if (featureEnabled.not()) {
                Single.just(false)
            } else {
                isInExperiment()
            }
        }
        .map { isInExperiment -> isInExperiment && exchangeCampaignPrefs.shouldShow() }

    private fun isInExperiment(): Single<Boolean> =
        mercuryExperimentsService
            .getMercuryExperiments()
            .map { it.isInExperiment }

    private fun ExchangeCampaignPrefs.shouldShow() = actionTaken.not() && dismissCount < MAX_DISMISS_COUNT

    val dismissCount: Int
        get() = exchangeCampaignPrefs.dismissCount

    fun onDismiss() {
        exchangeCampaignPrefs.dismissCount++
    }

    fun onActionTaken() {
        exchangeCampaignPrefs.actionTaken = true
    }

    companion object {
        private const val MAX_DISMISS_COUNT = 2
    }
}

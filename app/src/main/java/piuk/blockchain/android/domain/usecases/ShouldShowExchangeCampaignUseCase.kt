package piuk.blockchain.android.domain.usecases

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.ExchangeCampaignPrefs
import io.reactivex.rxjava3.core.Single

class ShouldShowExchangeCampaignUseCase(
    private val exchangeWAPromptFF: FeatureFlag,
    private val exchangeCampaignPrefs: ExchangeCampaignPrefs
) {

    operator fun invoke(): Single<Boolean> = exchangeWAPromptFF
        .enabled
        .map { featureEnabled -> featureEnabled && exchangeCampaignPrefs.shouldShow() }

    private fun ExchangeCampaignPrefs.shouldShow() = actionTaken.not() && dismissCount < MAX_DISMISS_COUNT

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

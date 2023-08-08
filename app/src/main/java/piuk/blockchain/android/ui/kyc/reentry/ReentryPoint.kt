package piuk.blockchain.android.ui.kyc.reentry

import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.nabu.models.responses.nabu.KycState

sealed class ReentryPoint(val entryPoint: String) {
    object EmailEntry : ReentryPoint("Email Entry")
    object CountrySelection : ReentryPoint("Country Selection")
    object Profile : ReentryPoint("Profile Entry")
    object Address : ReentryPoint("Address Entry")
    data class Questionnaire(val questionnaire: com.blockchain.domain.dataremediation.model.Questionnaire) :
        ReentryPoint("Extra Info Entry")
    object MobileEntry : ReentryPoint("Mobile Entry")
    object Veriff : ReentryPoint("Veriff Splash")
    data class TierCurrentState(val kycState: KycState) : ReentryPoint("TierCurrentState")
}

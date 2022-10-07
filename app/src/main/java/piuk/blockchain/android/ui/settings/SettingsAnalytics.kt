package piuk.blockchain.android.ui.settings

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import java.io.Serializable

sealed class SettingsAnalytics(override val event: String, override val params: Map<String, Serializable> = mapOf()) :
    AnalyticsEvent {

    object WalletIdCopyClicked : SettingsAnalytics("settings_wallet_id_copy_click")
    object WalletIdCopyCopied : SettingsAnalytics("settings_wallet_id_copied")

    class BiometricsOptionUpdated(isEnabled: Boolean) : SettingsAnalytics(
        AnalyticsNames.BIOMETRICS_OPTION_UPDATED.eventName,
        mapOf(
            "is_enabled" to isEnabled
        )
    )

    object RecoveryPhraseShown : SettingsAnalytics(
        AnalyticsNames.RECOVERY_PHRASE_SHOWN.eventName
    )

    class TwoStepVerificationCodeSubmitted(option: String) : SettingsAnalytics(
        AnalyticsNames.TWO_STEP_VERIFICATION_CODE_SUBMITTED.eventName,
        mapOf(
            TWO_STEP_OPTION to option
        )
    )

    class LinkCardClicked(override val origin: LaunchOrigin) : SettingsAnalytics(
        AnalyticsNames.LINK_CARD_CLICKED.eventName
    )

    class RemoveCardClicked(override val origin: LaunchOrigin) :
        SettingsAnalytics(AnalyticsNames.REMOVE_CARD_CLICKED.eventName)

    class SettingsHyperlinkClicked(private val destination: AnalyticsHyperlinkDestination) :
        SettingsAnalytics(
            AnalyticsNames.SETTINGS_HYPERLINK_DESTINATION.eventName,
            mapOf(
                "destination" to destination.name
            )
        )

    companion object {
        const val TWO_SET_MOBILE_NUMBER_OPTION = "MOBILE_NUMBER"
        private const val TWO_STEP_OPTION = "two_step_option"
    }

    enum class AnalyticsHyperlinkDestination {
        ABOUT, PRIVACY_POLICY, TERMS_OF_SERVICE
    }
}

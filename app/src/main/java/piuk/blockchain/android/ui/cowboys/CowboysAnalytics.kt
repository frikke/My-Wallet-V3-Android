package piuk.blockchain.android.ui.cowboys

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

sealed class CowboysAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    object VerifyEmailAnnouncementClicked : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_VERIFY_EMAIL_ANNOUNCEMENT_CLICKED.eventName
    )

    object WelcomeInterstitialViewed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_WELCOME_INTERSTITIAL_VIEWED.eventName
    )

    object WelcomeInterstitialContinueClicked : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_WELCOME_INTERSTITIAL_CONTINUE_CLICKED.eventName
    )

    object WelcomeInterstitialClosed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_WELCOME_INTERSTITIAL_CLOSED.eventName
    )

    object CompleteSignupAnnouncementClicked : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_COMPLETE_SIGNUP_ANNOUNCEMENT_CLICKED.eventName
    )

    object KycPersonalInfoViewed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_KYC_PERSONAL_INFO_VIEWED.eventName
    )

    object KycAddressViewed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_KYC_ADDRESS_VIEWED.eventName
    )

    object KycPersonalInfoConfirmed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_KYC_PERSONAL_INFO_CONFIRMED.eventName
    )

    object KycAddressConfirmed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_KYC_ADDRESS_CONFIRMED.eventName
    )

    object RaffleInterstitialViewed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_RAFFLE_INTERSTITIAL_VIEWED.eventName
    )

    object RaffleInterstitialClosed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_RAFFLE_INTERSTITIAL_CLOSED.eventName
    )

    object RaffleInterstitialBuyClicked : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_RAFFLE_INTERSTITIAL_BUY_CLICKED.eventName
    )

    object VerifyIdAnnouncementClicked : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_VERIFY_ANNOUNCEMENT_CLICKED.eventName
    )

    object VerifyIdInterstitialViewed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_VERIFY_INTERSTITIAL_VIEWED.eventName
    )

    object VerifyIdInterstitialClosed : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_VERIFY_INTERSTITIAL_CLOSED.eventName
    )

    object VerifyIdInterstitialCtaClicked : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_VERIFY_INTERSTITIAL_CTA_CLICKED.eventName
    )

    object ReferFriendAnnouncementClicked : CowboysAnalytics(
        event = AnalyticsNames.COWBOYS_REFER_FRIEND_ANNOUNCEMENT_CLICKED.eventName
    )
}

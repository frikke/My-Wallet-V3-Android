package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.UserAnalytics
import com.blockchain.analytics.UserAnalytics.Companion.USER_ELIGIBLE_FOR_EXCHANGE_AWARENESS_PROMPT
import com.blockchain.analytics.UserAnalytics.Companion.USER_HAS_SEEN_THE_EXCHANGE_AWARENESS_PROMPT
import com.blockchain.analytics.UserProperty
import com.blockchain.nabu.UserIdentity
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.domain.usecases.ShouldShowExchangeCampaignUseCase
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard
import piuk.blockchain.android.ui.exchangecampaign.ExchangeCampaignAnalytics
import piuk.blockchain.android.urllinks.EXCHANGE_DYNAMIC_LINK

class ExchangeCampaignAnnouncement(
    dismissRecorder: DismissRecorder,
    private val shouldShowExchangeCampaignUseCase: ShouldShowExchangeCampaignUseCase,
    private val userIdentity: UserIdentity,
    private val analytics: Analytics,
    private val userAnalytics: UserAnalytics
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override val name = "exchange_campaign"

    override val associatedWalletModes: List<WalletMode>
        get() = WalletMode.values().toList()

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return shouldShowExchangeCampaignUseCase()
    }

    override fun show(host: AnnouncementHost) {
        setUserAnalyticsProperties()
        logPromptShownEvent()

        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.exchange_campaign_announcement_title,
                bodyText = R.string.exchange_campaign_announcement_description,
                iconImage = R.drawable.ic_exchange_white_black_bg,
                background = R.drawable.bkgd_exchange_campaign,
                buttonColor = R.color.grey_800,
                ctaText = R.string.exchange_campaign_announcement_action,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.openBrowserLink(EXCHANGE_DYNAMIC_LINK)
                    shouldShowExchangeCampaignUseCase.onActionTaken()
                    logPromptClickedEvent()
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                    shouldShowExchangeCampaignUseCase.onDismiss()
                    logPromptDismissedEvent()
                }
            )
        )
    }

    private fun setUserAnalyticsProperties() {
        userAnalytics.logUserProperty(
            UserProperty(
                property = USER_ELIGIBLE_FOR_EXCHANGE_AWARENESS_PROMPT,
                value = USER_PROPERTY_VALUE
            )
        )
        userAnalytics.logUserProperty(
            UserProperty(
                property = USER_HAS_SEEN_THE_EXCHANGE_AWARENESS_PROMPT,
                value = USER_PROPERTY_VALUE
            )
        )
    }

    private fun logPromptShownEvent() = logEvent { isSSO ->
        ExchangeCampaignAnalytics.ExchangeAwarenessPromptShown(
            countOfPrompts = shouldShowExchangeCampaignUseCase.dismissCount + 1,
            isSSO = isSSO
        )
    }

    private fun logPromptClickedEvent() = logEvent { isSSO ->
        ExchangeCampaignAnalytics.ExchangeAwarenessPromptClicked(
            isSSO = isSSO
        )
    }

    private fun logPromptDismissedEvent() = logEvent { isSSO ->
        ExchangeCampaignAnalytics.ExchangeAwarenessPromptDismissed(
            isSSO = isSSO
        )
    }

    @SuppressLint("CheckResult")
    private fun logEvent(createEvent: (Boolean) -> AnalyticsEvent) {
        userIdentity.isSSO().subscribe { isSSO -> analytics.logEvent(createEvent(isSSO)) }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "ExchangeCampaignAnnouncement_DISMISSED"

        private const val USER_PROPERTY_VALUE = "true"
    }
}

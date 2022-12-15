package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.domain.usecases.ShouldShowExchangeCampaignUseCase
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard
import piuk.blockchain.android.urllinks.EXCHANGE_DYNAMIC_LINK

class ExchangeCampaignAnnouncement(
    dismissRecorder: DismissRecorder,
    private val shouldShowExchangeCampaignUseCase: ShouldShowExchangeCampaignUseCase
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
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                    shouldShowExchangeCampaignUseCase.onDismiss()
                }
            )
        )
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "ExchangeCampaignAnnouncement_DISMISSED"
    }
}

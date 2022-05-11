package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

internal class KycIncompleteAnnouncement(
    private val userIdentity: UserIdentity,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey =
        DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }
        return userIdentity.isKycInProgress()
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            StandardAnnouncementCard(
                name = name,
                titleText = R.string.kyc_drop_off_card_title,
                bodyText = R.string.kyc_drop_off_card_description,
                ctaText = R.string.kyc_drop_off_card_button,
                iconImage = R.drawable.ic_announce_kyc,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startKyc(CampaignType.None)
                },
                dismissEntry = dismissEntry,
                dismissRule = DismissRule.CardPeriodic
            )
        )
    }

    override val name = "kyc_incomplete"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "KYC_INCOMPLETE_DISMISSED"
    }
}

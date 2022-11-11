package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard
import piuk.blockchain.android.urllinks.URL_JOIN_BLOCKCHAIN_CARD_WAITLIST

class BlockchainCardWaitlistAnnouncement(
    private val announcementQueries: AnnouncementQueries,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override val name = "card_issuing_waitlist"
    override val associatedWalletModes: List<WalletMode>
        get() = listOf(WalletMode.CUSTODIAL_ONLY)

    override fun shouldShow(): Single<Boolean> =
        if (dismissEntry.isDismissed) {
            Single.just(false)
        } else {
            announcementQueries.isBlockchainCardAvailable()
        }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "BlockchainCardWaitlistAnnouncement_DISMISSED"
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                titleText = R.string.blockchain_card_waitlist_announcement_title,
                bodyText = R.string.blockchain_card_waitlist_announcement_description,
                ctaText = R.string.blockchain_card_waitlist_announcement_action,
                iconImage = R.drawable.card_filled_with_background,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.openBrowserLink(URL_JOIN_BLOCKCHAIN_CARD_WAITLIST)
                }
            )
        )
    }
}

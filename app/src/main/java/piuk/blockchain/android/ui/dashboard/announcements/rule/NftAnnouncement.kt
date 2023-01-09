package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.NftAnnouncementPrefs
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class NftAnnouncement(
    dismissRecorder: DismissRecorder,
    private val nftAnnouncementPrefs: NftAnnouncementPrefs,
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        return Single.just(
            nftAnnouncementPrefs.isJoinNftWaitlistSuccessful.not() &&
                nftAnnouncementPrefs.isNftAnnouncementDismissed.not()
        )
    }

    override val associatedWalletModes: List<WalletMode>
        get() = listOf(WalletMode.NON_CUSTODIAL)

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                titleText = R.string.nft_announcement_title,
                bodyText = R.string.nft_announcement_description,
                ctaText = R.string.nft_announcement_action,
                iconImage = R.drawable.ic_nft,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                    nftAnnouncementPrefs.isNftAnnouncementDismissed = true
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.joinNftWaitlist()
                }
            )
        )
    }

    override val name = "view_nft_waitlist"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "NftAnnouncement_DISMISSED"
    }
}

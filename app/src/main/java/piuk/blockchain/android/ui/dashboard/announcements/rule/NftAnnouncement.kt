package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.featureflag.FeatureFlag
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class NftAnnouncement(
    dismissRecorder: DismissRecorder,
    private val showNftAnnouncementFeatureFlag: FeatureFlag
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        return Single.zip(
            showNftAnnouncementFeatureFlag.enabled,
            Single.just(dismissEntry.isDismissed)
        ) { enabled, dismissed ->
            enabled && dismissed.not()
        }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPersistent,
                dismissEntry = dismissEntry,
                titleText = R.string.nft_announcement_title,
                bodyText = R.string.nft_announcement_description,
                ctaText = R.string.nft_announcement_action,
                iconImage = R.drawable.ic_nft,
                shouldWrapIconWidth = false,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
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

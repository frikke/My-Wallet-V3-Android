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
import piuk.blockchain.android.ui.settings.SettingsActivity.Companion.SettingsDestination

class HideDustAnnouncement(
    dismissRecorder: DismissRecorder,
    val announcementQueries: AnnouncementQueries
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return announcementQueries.hasDustBalances()
    }

    override val associatedWalletModes: List<WalletMode>
        get() = listOf(WalletMode.CUSTODIAL_ONLY)

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                titleText = R.string.hide_dust_announcement_title,
                bodyText = R.string.hide_dust_announcement_description,
                ctaText = R.string.hide_dust_announcement_action,
                iconImage = R.drawable.ic_hide,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startSettings(SettingsDestination.General)
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    override val name = "dust_hiding_available"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "HideDustAnnouncement_DISMISSED"
    }
}

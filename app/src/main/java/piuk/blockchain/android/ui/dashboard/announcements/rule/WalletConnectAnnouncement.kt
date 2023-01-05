package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard
import piuk.blockchain.android.urllinks.URL_WALLET_CONNECT_LEARN_MORE

class WalletConnectAnnouncement(
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> = Single.just(!dismissEntry.isDismissed)

    override val associatedWalletModes: List<WalletMode>
        get() = listOf(WalletMode.NON_CUSTODIAL)

    override fun show(host: AnnouncementHost) {
        try {
            host.showAnnouncementCard(
                card = StandardAnnouncementCard(
                    name = name,
                    dismissRule = DismissRule.CardOneTime,
                    dismissEntry = dismissEntry,
                    titleText = R.string.wallet_connect_announcement_card_title,
                    bodyText = R.string.wallet_connect_announcement_card_subtitle,
                    ctaText = R.string.wallet_connect_announcement_action,
                    iconImage = R.drawable.ic_walletconnect_background,
                    shouldWrapIconWidth = true,
                    dismissFunction = {
                        host.dismissAnnouncementCard()
                    },
                    ctaFunction = {
                        host.dismissAnnouncementCard()
                        host.openBrowserLink(URL_WALLET_CONNECT_LEARN_MORE)
                    }
                )
            )
        } catch (e: Resources.NotFoundException) {
            host.dismissAnnouncementCard()
        }
    }

    override val name = "wallet_connect_available"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "WalletConnectAnnouncement_DISMISSED"
    }
}

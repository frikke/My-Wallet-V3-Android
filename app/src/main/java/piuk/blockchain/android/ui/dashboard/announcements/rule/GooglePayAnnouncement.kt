package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.domain.paymentmethods.model.PaymentMethod.Companion.GOOGLE_PAY_PAYMENT_ID
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class GooglePayAnnouncement(
    private val announcementQueries: AnnouncementQueries,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override val name = "google_pay_available"

    override fun shouldShow(): Single<Boolean> =
        if (dismissEntry.isDismissed) {
            Single.just(false)
        } else {
            announcementQueries.isGooglePayAvailable()
        }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                titleText = R.string.gpay_announcement_title,
                bodyText = R.string.gpay_announcement_description,
                ctaText = R.string.gpay_announcement_action,
                iconImage = R.drawable.google_pay_mark,
                shouldWrapIconWidth = true,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startSimpleBuy(asset = CryptoCurrency.BTC, paymentMethodId = GOOGLE_PAY_PAYMENT_ID)
                }
            )
        )
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "GooglePayAnnouncement_DISMISSED"
    }
}

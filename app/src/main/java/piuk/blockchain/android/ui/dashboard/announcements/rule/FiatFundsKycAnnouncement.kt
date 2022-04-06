package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class FiatFundsKycAnnouncement(
    dismissRecorder: DismissRecorder,
    private val userIdentity: UserIdentity,
    private val paymentsDataManager: PaymentsDataManager
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        // if eligible for simple buy balance then user is KYC gold
        return Singles.zip(
            userIdentity.isEligibleFor(Feature.SimpleBuy),
            paymentsDataManager.getLinkedBanks()
        ) { isEligible, linkedBanks ->
            isEligible && linkedBanks.isEmpty()
        }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                iconImage = R.drawable.ic_transfer_bank_blue_600,
                titleText = R.string.fiat_funds_kyc_announcement_title,
                bodyText = R.string.fiat_funds_kyc_announcement_description,
                ctaText = R.string.fiat_funds_kyc_announcement_action,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.showBankLinking()
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    override val name = "fiat_funds_kyc"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "FiatFundsKycAnnouncement_DISMISSED"
    }
}

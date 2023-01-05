package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class BackupPhraseAnnouncement(
    dismissRecorder: DismissRecorder,
    private val walletStatusPrefs: WalletStatusPrefs
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY
    override val associatedWalletModes: List<WalletMode>
        get() = listOf(WalletMode.NON_CUSTODIAL)

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return Single.just(walletStatusPrefs.isWalletFunded && !walletStatusPrefs.isWalletBackedUp)
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.recovery_card_title_1,
                bodyText = R.string.recovery_card_body_1,
                ctaText = R.string.recovery_card_cta,
                iconImage = R.drawable.ic_announce_backup,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startFundsBackup()
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    override val name = "backup_funds"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "BackupWalletAuthAnnouncement_DISMISSED"
    }
}

package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.text.Spannable
import androidx.annotation.VisibleForTesting
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard
import piuk.blockchain.android.util.StringUtils

class TaxCenterAnnouncement(
    dismissRecorder: DismissRecorder,
    private val userIdentity: UserIdentity
) : AnnouncementRule(dismissRecorder) {
    override val dismissKey: String = DISMISS_KEY
    override val name: String = ANNOUNCEMENT_NAME

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return userIdentity.getUserCountry()
            .defaultIfEmpty("")
            .map { country -> country == "US" }
    }

    override fun show(host: AnnouncementHost) {
        val bodyTextSpannable = host.context?.let {
            StringUtils.getStringWithMappedAnnotations(
                it,
                R.string.tax_center_announcement_description, emptyMap()
            ) {}
        }
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                iconImage = R.drawable.ic_tax_center_indicator,
                titleText = R.string.tax_center_announcement_title,
                bodyTextSpannable = bodyTextSpannable as? Spannable,
                ctaText = R.string.common_ok,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "TaxCenterAnnouncement_DISMISSED"

        const val ANNOUNCEMENT_NAME = "tax_center_available"
    }
}

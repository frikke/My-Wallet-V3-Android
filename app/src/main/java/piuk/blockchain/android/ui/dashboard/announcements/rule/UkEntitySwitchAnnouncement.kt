package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.net.Uri
import android.text.Spannable
import androidx.annotation.VisibleForTesting
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard
import piuk.blockchain.android.urllinks.UK_ENTITY_SWITCH
import piuk.blockchain.android.util.StringUtils

class UkEntitySwitchAnnouncement(
    private val userIdentity: UserIdentity,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder), KoinComponent {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return userIdentity.getUserCountry()
            .defaultIfEmpty("")
            .map { country -> country == "GB" }
    }

    override fun show(host: AnnouncementHost) {
        val linksMap = mapOf("learn_more" to Uri.parse(UK_ENTITY_SWITCH))
        val bodyTextSpannable = host.context?.let {
            StringUtils.getStringWithMappedAnnotations(it, R.string.ukentityswitch_announcement_description, linksMap)
        }
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                titleText = R.string.ukentityswitch_announcement_title,
                bodyText = R.string.ukentityswitch_announcement_description,
                bodyTextSpannable = bodyTextSpannable as? Spannable,
                ctaText = R.string.ukentityswitch_announcement_action,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    override val name = ANNOUNCEMENT_NAME

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "UkBankChangeAnnouncement_DISMISSED"

        const val ANNOUNCEMENT_NAME = "uk_entity_switch_2022"
    }
}

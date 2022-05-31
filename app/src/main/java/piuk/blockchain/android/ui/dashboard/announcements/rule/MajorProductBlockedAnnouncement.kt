package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import com.blockchain.domain.eligibility.model.ProductNotEligibleReason
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicReference
import piuk.blockchain.android.R
import piuk.blockchain.android.maintenance.presentation.openUrl
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard
import piuk.blockchain.android.urllinks.URL_RUSSIA_SANCTIONS_EU5

class MajorProductBlockedAnnouncement(
    dismissRecorder: DismissRecorder,
    private val userIdentity: UserIdentity
) : AnnouncementRule(dismissRecorder) {
    override val dismissKey: String = DISMISS_KEY
    override val name: String = ANNOUNCEMENT_NAME

    private val reason = AtomicReference<ProductNotEligibleReason>()

    override fun shouldShow(): Single<Boolean> {
        return userIdentity.majorProductsNotEligibleReasons().map {
            val first = it.filterKnownReasons().firstOrNull() ?: return@map false
            reason.set(first)
            true
        }
    }

    override fun show(host: AnnouncementHost) {
        val reason = reason.get()

        val title = when (reason) {
            is ProductNotEligibleReason.InsufficientTier -> throw IllegalArgumentException()
            ProductNotEligibleReason.Sanctions.RussiaEU5,
            is ProductNotEligibleReason.Sanctions.Unknown,
            is ProductNotEligibleReason.Unknown -> R.string.account_restricted
        }

        val body = when (reason) {
            is ProductNotEligibleReason.InsufficientTier -> throw IllegalArgumentException()
            ProductNotEligibleReason.Sanctions.RussiaEU5 ->
                StringResource.Id(R.string.russia_sanctions_eu5_sheet_subtitle)
            is ProductNotEligibleReason.Sanctions.Unknown -> StringResource.Value(reason.message)
            is ProductNotEligibleReason.Unknown -> StringResource.Value(reason.message)
        }

        val ctaText = when (reason) {
            is ProductNotEligibleReason.InsufficientTier -> throw IllegalArgumentException()
            ProductNotEligibleReason.Sanctions.RussiaEU5 -> R.string.learn_more
            is ProductNotEligibleReason.Sanctions.Unknown -> R.string.common_ok
            is ProductNotEligibleReason.Unknown -> R.string.common_ok
        }

        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPersistent,
                dismissEntry = dismissEntry,
                iconImage = R.drawable.ic_major_product_not_eligible_indicator,
                titleText = title,
                body = body.valueOrNull(),
                bodyText = body.idOrZero(),
                ctaText = ctaText,
                ctaFunction = {
                    if (reason is ProductNotEligibleReason.Sanctions.RussiaEU5) {
                        host.context?.openUrl(URL_RUSSIA_SANCTIONS_EU5)
                    } else {
                        host.dismissAnnouncementCard()
                    }
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    private fun List<ProductNotEligibleReason>.filterKnownReasons(): List<ProductNotEligibleReason> =
        filter {
            when (it) {
                ProductNotEligibleReason.InsufficientTier.Tier1TradeLimitExceeded,
                ProductNotEligibleReason.InsufficientTier.Tier2Required,
                is ProductNotEligibleReason.InsufficientTier.Unknown -> false
                ProductNotEligibleReason.Sanctions.RussiaEU5,
                is ProductNotEligibleReason.Sanctions.Unknown,
                is ProductNotEligibleReason.Unknown -> true
            }
        }

    private sealed class StringResource {
        class Id(@StringRes val id: Int) : StringResource()
        class Value(val value: String) : StringResource()

        fun idOrZero(): Int = (this as? Id)?.id ?: 0
        fun valueOrNull(): String? = (this as? Value)?.value
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "MajorProductNotEligibleReasonAnnouncement_DISMISSED"

        const val ANNOUNCEMENT_NAME = "major_product_blocked"
    }
}

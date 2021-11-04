package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard
import piuk.blockchain.android.urllinks.URL_BLOCKCHAIN_CEUR_LEARN_MORE

class CeloEurAnnouncement(
    private val announcementQueries: AnnouncementQueries,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override val name = "celo_eur"

    private var newAsset: AssetInfo? = null

    override fun shouldShow(): Single<Boolean> =
        announcementQueries.getAssetFromCatalogueByTicker(TICKER_NAME)?.let { assetInfo ->
            newAsset = assetInfo
            announcementQueries.isTier1Or2Verified()
                .zipWith(announcementQueries.getCountryCode())
                .map { (isVerified, countryCode) ->
                    if (isVerified && !excludedCountries.contains(countryCode)) {
                        !dismissEntry.isDismissed
                    } else {
                        false
                    }
                }
        } ?: Single.just(false)

    override fun show(host: AnnouncementHost) {
        try {
            newAsset?.let {
                host.showAnnouncementCard(
                    card = StandardAnnouncementCard(
                        name = name,
                        dismissRule = DismissRule.CardOneTime,
                        dismissEntry = dismissEntry,
                        titleText = R.string.ceur_announcement_title,
                        bodyText = R.string.ceur_announcement_description,
                        ctaText = R.string.ceur_announcement_action,
                        iconUrl = it.logo,
                        shouldWrapIconWidth = true,
                        dismissFunction = {
                            host.dismissAnnouncementCard()
                        },
                        ctaFunction = {
                            host.dismissAnnouncementCard()
                            host.openBrowserLink(URL_BLOCKCHAIN_CEUR_LEARN_MORE)
                        }
                    )
                )
            }
        } catch (e: Resources.NotFoundException) {
            host.dismissAnnouncementCard()
        }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "CeloEurAnnouncement_DISMISSED"
        const val TICKER_NAME = "CEUR"
        private val excludedCountries = listOf("US", "IT")
    }
}

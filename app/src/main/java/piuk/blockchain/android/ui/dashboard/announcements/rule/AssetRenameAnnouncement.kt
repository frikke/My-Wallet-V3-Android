package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class AssetRenameAnnouncement(
    private val announcementQueries: AnnouncementQueries,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {
    private var oldAssetTicker: String = ""
    private var renamedAsset: AssetInfo? = null

    override val dismissKey: String
        get() = DISMISS_KEY.plus(renamedAsset?.networkTicker.orEmpty())

    override val name: String
        get() = "asset_rename"

    override fun shouldShow(): Single<Boolean> =
            announcementQueries.getRenamedAssetFromCatalogue()
                .doOnSuccess {
                    oldAssetTicker = it.first
                    renamedAsset = it.second
                }
                .toSingle()
                .map {
                    !dismissEntry.isDismissed
                }.onErrorReturn { false }

    override fun show(host: AnnouncementHost) {
        renamedAsset?.let { assetInfo ->
            host.showAnnouncementCard(
                card = StandardAnnouncementCard(
                    name = name,
                    dismissRule = DismissRule.CardOneTime,
                    dismissEntry = dismissEntry,
                    titleText = R.string.rename_asset_card_title,
                    titleFormatParams = arrayOf(oldAssetTicker),
                    bodyText = R.string.rename_asset_card_body,
                    bodyFormatParams = arrayOf(oldAssetTicker, assetInfo.displayTicker),
                    ctaText = R.string.rename_asset_card_cta,
                    ctaFormatParams = arrayOf(assetInfo.displayTicker),
                    iconUrl = assetInfo.logo,
                    dismissFunction = {
                        host.dismissAnnouncementCard()
                    },
                    ctaFunction = {
                        host.dismissAnnouncementCard()
                        host.startSimpleBuy(assetInfo)
                    }
                )
            )
        }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "ASSET_RENAME_ANNOUNCEMENT_DISMISSED"
    }
}
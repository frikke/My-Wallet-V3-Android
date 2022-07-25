package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import kotlin.math.absoluteValue
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.ComponentAnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.asPercentString
import piuk.blockchain.android.ui.dashboard.asString
import piuk.blockchain.android.ui.resources.AssetResources

class NewAssetAnnouncement(
    private val announcementQueries: AnnouncementQueries,
    dismissRecorder: DismissRecorder,
    private val assetResources: AssetResources
) : AnnouncementRule(dismissRecorder) {
    private var newAsset: AssetInfo? = null
    private var newAssetPrice: Prices24HrWithDelta? = null

    override val dismissKey: String
        get() = DISMISS_KEY.plus(newAsset?.networkTicker.orEmpty())

    override val name: String
        get() = "new_asset"

    override fun shouldShow(): Single<Boolean> =
        announcementQueries.getAssetFromCatalogue()
            .doOnSuccess {
                newAsset = it
            }
            .toSingle()
            .flatMap { assetInfo ->
                announcementQueries.getAssetPrice(assetInfo).firstOrError()
            }
            .doOnSuccess { newAssetPrice = it }
            .map {
                !dismissEntry.isDismissed
            }.onErrorReturn { false }

    override fun show(host: AnnouncementHost) {
        newAsset?.let {
            host.showAnnouncementCard(
                card = ComponentAnnouncementCard(
                    name = name,
                    dismissRule = DismissRule.CardOneTime,
                    dismissEntry = dismissEntry,
                    headerText = R.string.new_asset_card_header,
                    headerFormatParams = createHeaderParams(newAssetPrice),
                    subHeaderText = R.string.new_asset_card_subheader,
                    subheaderFormatParams = createSubHeaderParams(newAssetPrice),
                    subHeaderColour = newAssetPrice.toColor(),
                    subHeaderSuffixText = R.string.new_asset_card_subheader_suffix,
                    subHeaderSuffixColour = R.color.paletteBaseTextBody,
                    titleText = R.string.new_asset_card_title,
                    titleFormatParams = arrayOf(it.name, it.displayTicker),
                    bodyText = R.string.new_asset_card_body,
                    bodyFormatParams = arrayOf(it.displayTicker),
                    ctaText = R.string.new_asset_card_cta,
                    ctaFormatParams = arrayOf(it.displayTicker),
                    iconUrl = it.logo,
                    buttonColor = assetResources.assetColor(it),
                    dismissFunction = {
                        host.dismissAnnouncementCard()
                    },
                    ctaFunction = {
                        host.dismissAnnouncementCard()
                        host.startSimpleBuy(it)
                    }
                )
            )
        }
    }

    private fun createHeaderParams(price: Prices24HrWithDelta?) = price?.let {
        arrayOf(price.currentRate.from.displayTicker, price.currentRate.price.toStringWithSymbol())
    } ?: arrayOf()

    private fun createSubHeaderParams(price: Prices24HrWithDelta?) = price?.let {
        arrayOf(getGainWithSymbol(price), price.delta24h.asPercentString())
    } ?: arrayOf()

    private fun getGainWithSymbol(price: Prices24HrWithDelta): String {
        val gain = price.delta24h * price.currentRate.price.toFloat() / 100.0
        val decimalPlaces = if (gain.absoluteValue > 0 && gain.absoluteValue < CLIFF_FOR_EXTRA_DECIMALS) {
            FOUR_DECIMALS
        } else {
            TWO_DECIMALS
        }
        val gainWithSymbol = "${price.currentRate.price.symbol}${gain.absoluteValue.asString(decimalPlaces)}"
        return when {
            price.delta24h > 0 -> "+$gainWithSymbol"
            price.delta24h < 0 -> "-$gainWithSymbol"
            else -> gainWithSymbol
        }
    }

    private fun Prices24HrWithDelta?.toColor() =
        when {
            this == null -> 0
            delta24h < 0 -> R.color.paletteBaseError
            delta24h > 0 -> R.color.paletteBaseSuccess
            else -> R.color.paletteBaseTextBody
        }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "NEW_ASSET_ANNOUNCEMENT_DISMISSED"
        private const val TWO_DECIMALS = 2
        private const val FOUR_DECIMALS = 4
        private const val CLIFF_FOR_EXTRA_DECIMALS = 0.01
    }
}

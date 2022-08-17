package com.blockchain.deeplinking.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Destination : Parcelable {
    @Parcelize
    data class AssetViewDestination(val networkTicker: String, val recurringBuyId: String?) : Destination()

    @Parcelize
    data class AssetBuyDestination(
        val networkTicker: String,
        val amount: String
    ) : Destination()

    @Parcelize
    data class AssetSendDestination(
        val networkTicker: String,
        val amount: String,
        val accountAddress: String
    ) : Destination()

    @Parcelize
    data class ActivityDestination(val filter: String? = null) : Destination()

    @Parcelize
    data class AssetEnterAmountDestination(
        val networkTicker: String
    ) : Destination()

    @Parcelize
    data class AssetEnterAmountLinkCardDestination(
        val networkTicker: String
    ) : Destination()

    @Parcelize
    data class AssetEnterAmountNewMethodDestination(
        val networkTicker: String
    ) : Destination()

    @Parcelize
    object CustomerSupportDestination : Destination()

    @Parcelize
    object StartKycDestination : Destination()

    @Parcelize
    object ReferralDestination : Destination()

    @Parcelize
    class ExternalLinkDestination(
        val url: String
    ) : Destination()

    @Parcelize
    object DashboardDestination : Destination()
}

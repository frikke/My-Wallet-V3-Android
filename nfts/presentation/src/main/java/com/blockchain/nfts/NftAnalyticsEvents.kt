package com.blockchain.nfts

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import java.io.Serializable

sealed class NftAnalyticsEvents(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    object ScreenViewed : NftAnalyticsEvents(
        event = AnalyticsNames.WALLET_NFT_VIEWED.eventName
    )
}

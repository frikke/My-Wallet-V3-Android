package com.blockchain.prices

import com.blockchain.analytics.TraitsService
import com.blockchain.extensions.filterNotNullValues
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.payloadScope
import com.blockchain.koin.topMoversInBuy

class TopMoversTraitsRepository : TraitsService {
    override suspend fun traits(): Map<String, String> {
        val topMoversInBuyFF = payloadScope.getOrNull<FeatureFlag>(topMoversInBuy)
        return mapOf(
            "buy_top_movers_enabled" to (topMoversInBuyFF?.coEnabled() ?: false).toString()
        ).filterNotNullValues()
    }
}

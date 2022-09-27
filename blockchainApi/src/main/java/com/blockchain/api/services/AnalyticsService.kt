@file:UseSerializers(BigDecimalSerializer::class)

package com.blockchain.api.services

import com.blockchain.analytics.AnalyticsContext
import com.blockchain.analytics.NabuAnalyticsEvent
import com.blockchain.api.analytics.AnalyticsApiInterface
import com.blockchain.api.analytics.AnalyticsRequestBody
import com.blockchain.serializers.BigDecimalSerializer
import io.reactivex.rxjava3.core.Completable
import kotlinx.serialization.UseSerializers

class AnalyticsService internal constructor(
    private val api: AnalyticsApiInterface
) {
    fun postEvents(
        events: List<NabuAnalyticsEvent>,
        id: String,
        analyticsContext: AnalyticsContext,
        platform: String,
        device: String,
        authorization: String?
    ): Completable {

        return api.postAnalytics(
            authorization,
            AnalyticsRequestBody(
                id = id,
                device = device,
                platform = platform,
                events = events,
                context = analyticsContext
            )
        )
    }
}

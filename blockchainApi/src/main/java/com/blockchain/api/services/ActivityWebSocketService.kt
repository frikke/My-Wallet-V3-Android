package com.blockchain.api.services

import com.blockchain.api.selfcustody.AuthInfo
import com.blockchain.api.selfcustody.activity.ActivityRequest
import com.blockchain.api.selfcustody.activity.ActivityRequestParams
import com.blockchain.api.selfcustody.activity.ActivityResponse
import com.blockchain.network.websocket.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow

class ActivityWebSocketService(
    private val webSocket: WebSocket<ActivityRequest, ActivityResponse>,
    private val activityCacheService: ActivityCacheService,
    private val credentials: SelfCustodyServiceAuthCredentials,
    private val wsScope: CoroutineScope
) {
    private val authInfo: AuthInfo
        get() = AuthInfo(
            guidHash = credentials.hashedGuid,
            sharedKeyHash = credentials.hashedSharedKey,
        )

    fun open() {
        webSocket.open()
    }

    fun send(fiatCurrency: String, acceptLanguage: String, timeZone: String) {
        webSocket.send(
            ActivityRequest(
                auth = authInfo,
                params = ActivityRequestParams(
                    timezone = timeZone,
                    fiatCurrency = fiatCurrency,
                    acceptLanguage = acceptLanguage // "en-GB;q=1.0, en"
                ),
                action = UNIFIED_ACTIVITY_WS_ACTION,
                channel = UNIFIED_ACTIVITY_WS_CHANNEL
            )
        )
    }

    suspend fun subscribeToActivity() = wsScope.launch {
        webSocket.responses.asFlow()
            .collect {
                activityCacheService.addOrUpdateActivityItems(it)
            }
    }

    fun stop() {
        webSocket.close()
    }

    companion object {
        private const val UNIFIED_ACTIVITY_WS_ACTION = "subscribe"
        private const val UNIFIED_ACTIVITY_WS_CHANNEL = "activity"
    }
}

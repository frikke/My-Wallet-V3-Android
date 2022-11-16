package com.blockchain.unifiedcryptowallet.data.activity.repository

import com.blockchain.api.selfcustody.activity.ActivityViewItemDto
import com.blockchain.api.services.ActivityWebSocketService
import com.blockchain.data.DataResource
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.unifiedcryptowallet.data.activity.datasource.UnifiedActivityCache
import com.blockchain.unifiedcryptowallet.data.activity.repository.mapper.toActivityViewItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityItem
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class UnifiedActivityRepository(
    private val activityWebSocketService: ActivityWebSocketService,
    private val activityCache: UnifiedActivityCache,
    private val json: Json,
    private val currencyPrefs: CurrencyPrefs
) : UnifiedActivityService {

    override suspend fun getAllActivity(
        acceptLanguage: String,
        timeZone: String
    ): Flow<DataResource<List<UnifiedActivityItem>>> {

        return flow {
            emit(DataResource.Loading)

            activityWebSocketService.open()
            activityWebSocketService.send(
                fiatCurrency = currencyPrefs.selectedFiatCurrency.networkTicker,
                acceptLanguage = acceptLanguage,
                timeZone = timeZone
            )

            emitAll(
                activityCache.getActivity()
                    .catch {
                        emit(DataResource.Error(Exception(it)))
                    }.map { activityItems ->
                        val items = activityItems.mapNotNull {
                            json.decodeFromString<ActivityViewItemDto>(it.summary_view)
                                .toActivityViewItem()?.let { summary ->
                                    UnifiedActivityItem(
                                        txId = it.tx_id,
                                        network = it.network,
                                        blockExplorerUrl = it.external_url,
                                        summary = summary,
                                        status = it.status,
                                        date = Calendar.getInstance()
                                            .apply { set(Calendar.MILLISECOND, it.timestamp.toInt()) }
                                    )
                                }
                        }
                        DataResource.Data(items)
                    }
            )
        }
    }
}

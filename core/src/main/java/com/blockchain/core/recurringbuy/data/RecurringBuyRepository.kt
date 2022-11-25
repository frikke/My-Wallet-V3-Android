package com.blockchain.core.recurringbuy.data

import com.blockchain.core.recurringbuy.data.datasources.RecurringBuyWithIdStore
import com.blockchain.core.recurringbuy.domain.RecurringBuy
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.nabu.models.responses.simplebuy.toRecurringBuy
import com.blockchain.store.mapData
import com.blockchain.utils.toException
import info.blockchain.balance.AssetCatalogue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

internal class RecurringBuyRepository(
    private val recurringBuyWithIdStore: RecurringBuyWithIdStore,
    private val assetCatalogue: AssetCatalogue
) : RecurringBuyService {

    override fun getRecurringBuyForId(
        id: String,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<RecurringBuy>> {
        return recurringBuyWithIdStore.stream(freshnessStrategy.withKey(RecurringBuyWithIdStore.Key(id)))
            .mapData {
                it.first().toRecurringBuy(assetCatalogue) ?: error("No recurring buy")
            }
            .catch {
                emit(DataResource.Error(it.toException()))
            }
    }
}
package com.blockchain.core.experiments

import com.blockchain.analytics.TraitsService
import com.blockchain.core.experiments.cache.ExperimentsStore
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.dsl.bind
import org.koin.dsl.module

class ExperimentTraitsRepository(
    val experimentsStore: ExperimentsStore
) : TraitsService {

    override suspend fun traits(overrideWalletMode: WalletMode?): Map<String, String> = getExperiments()

    private suspend fun getExperiments(): Map<String, String> {
        return experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .filter { it !is DataResource.Loading }
            .map { dataResourceMap ->
                when (dataResourceMap) {
                    is DataResource.Data -> {
                        dataResourceMap.data.mapValues { it.value.toString() }
                    }
                    is DataResource.Error -> emptyMap()
                    DataResource.Loading -> {
                        error("experimentsStore illegal argument exception -  we should never reach this point")
                    }
                }
            }
            .firstOrNull().orEmpty()
    }
}

val experimentsTraitsModule = module {
    factory {
        ExperimentTraitsRepository(experimentsStore = get())
    }.bind(TraitsService::class)
}

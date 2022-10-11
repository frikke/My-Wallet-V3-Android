package piuk.blockchain.androidcore.data.settings.datastore

import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.IsCachedMediator
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder
import info.blockchain.wallet.api.data.Settings
import piuk.blockchain.androidcore.data.settings.SettingsService

class SettingsStore(
    private val settingsService: SettingsService,
) : Store<Settings> by InMemoryCacheStoreBuilder().build(
    storeId = STORE_ID,
    fetcher = Fetcher.ofSingle {
        settingsService.getSettingsObservable().singleOrError()
    },
    mediator = IsCachedMediator(),
) {
    companion object {
        private const val STORE_ID = "SettingsStore"
    }
}

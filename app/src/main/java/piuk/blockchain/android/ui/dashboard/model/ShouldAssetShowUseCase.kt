package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.FreshnessStrategy
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.store.getDataOrThrow
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class ShouldAssetShowUseCase(
    private val hideDustFeatureFlag: FeatureFlag,
    private val localSettingsPrefs: LocalSettingsPrefs,
    private val watchlistService: WatchlistService
) {

    operator fun invoke(currency: Currency, account: BlockchainAccount): Flow<Boolean> =
        combine(
            flowOf(hideDustFeatureFlag.enabled.blockingGet()),
            flowOf(localSettingsPrefs.areSmallBalancesEnabled),
            watchlistService.isAssetInWatchlist(
                asset = currency,
                freshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
            ).getDataOrThrow(),
            account.balance
        ) { isFeatureEnabled, isPreferenceEnabled, isInWatchlist, balance ->
            if (isFeatureEnabled && isPreferenceEnabled) {
                if (isInWatchlist) {
                    true
                } else {
                    !balance.totalFiat.isDust()
                }
            } else {
                true
            }
        }
}

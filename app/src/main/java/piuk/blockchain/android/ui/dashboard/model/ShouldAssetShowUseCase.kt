package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AccountBalance
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.FreshnessStrategy
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.store.getDataOrThrow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ShouldAssetShowUseCase(
    private val hideDustFeatureFlag: FeatureFlag,
    private val localSettingsPrefs: LocalSettingsPrefs,
    private val watchlistService: WatchlistService
) {

    operator fun invoke(accountBalance: AccountBalance): Flow<Boolean> =
        flow {
            val isFFenabled = hideDustFeatureFlag.coEnabled()
            emitAll(
                watchlistService.isAssetInWatchlist(
                    asset = accountBalance.total.currency,
                    freshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
                ).getDataOrThrow().map { isInWatchlist ->
                    if (isFFenabled && localSettingsPrefs.hideSmallBalancesEnabled) {
                        if (isInWatchlist) {
                            true
                        } else {
                            !accountBalance.totalFiat.isDust()
                        }
                    } else
                        true
                }
            )
        }.catch {
            emit(true)
        }
}

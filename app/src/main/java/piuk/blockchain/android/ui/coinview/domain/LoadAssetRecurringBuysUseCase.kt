package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.CryptoAsset
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.combineDataResources
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewRecurringBuys

class LoadAssetRecurringBuysUseCase(
    private val recurringBuyService: RecurringBuyService,
    private val custodialWalletManager: CustodialWalletManager
) {
    operator fun invoke(
        asset: CryptoAsset,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<CoinviewRecurringBuys>> {
        return combine(
            recurringBuyService.recurringBuys(asset = asset.currency, freshnessStrategy = freshnessStrategy),
            custodialWalletManager.isCurrencyAvailableForTrading(asset.currency, freshnessStrategy)
        ) { recurringBuys, isAvailableForTrading ->

            combineDataResources(
                recurringBuys,
                isAvailableForTrading
            ) { recurringBuysData, isAvailableForTradingData ->
                CoinviewRecurringBuys(
                    data = recurringBuysData,
                    isAvailableForTrading = isAvailableForTradingData
                )
            }
        }
    }
}

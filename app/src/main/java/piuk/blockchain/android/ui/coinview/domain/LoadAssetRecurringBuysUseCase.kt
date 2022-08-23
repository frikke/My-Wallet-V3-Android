package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.CryptoAsset
import com.blockchain.data.DataResource
import com.blockchain.data.anyError
import com.blockchain.data.anyLoading
import com.blockchain.data.getFirstError
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import piuk.blockchain.android.domain.repositories.TradeDataService
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewRecurringBuys

class LoadAssetRecurringBuysUseCase(
    private val tradeDataService: TradeDataService,
    private val custodialWalletManager: CustodialWalletManager
) {
    operator fun invoke(asset: CryptoAsset): Flow<DataResource<CoinviewRecurringBuys>> {
        return combine(
            tradeDataService.getRecurringBuysForAsset(asset.currency),
            custodialWalletManager.isCurrencyAvailableForTrading(asset.currency)
        ) { recurringBuys, isAvailableForTrading ->
            val results = listOf(recurringBuys, isAvailableForTrading)

            when {
                results.anyLoading() -> {
                    DataResource.Loading
                }

                results.anyError() -> {
                    DataResource.Error(results.getFirstError().error)
                }

                else -> {
                    recurringBuys as DataResource.Data
                    isAvailableForTrading as DataResource.Data

                    DataResource.Data(
                        CoinviewRecurringBuys(
                            data = recurringBuys.data,
                            isAvailableForTrading = isAvailableForTrading.data
                        )
                    )
                }
            }
        }
    }
}

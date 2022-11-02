package piuk.blockchain.android.walletmode

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.combineDataResources
import com.blockchain.store.mapData
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeBalanceService
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Money
import info.blockchain.balance.total
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import piuk.blockchain.android.ui.dashboard.WalletModeBalanceCache

class WalletModeBalanceRepository(
    private var walletModeService: WalletModeService,
    private val balanceStore: WalletModeBalanceCache
) : WalletModeBalanceService {

    override fun balanceFor(walletMode: WalletMode): Flow<DataResource<Money>> {
        return balanceStore
            .getBalance(walletMode, FreshnessStrategy.Cached(forceRefresh = true))
            .mapData { it.total }
    }

    override fun totalBalance(): Flow<DataResource<Money>> {
        val balances = walletModeService.availableModes().map { walletMode ->
            balanceFor(walletMode)
        }

        return combine(balances) { balancesArray ->
            combineDataResources(balancesArray.toList()) { balancesList ->
                balancesList.total()
            }
        }
    }
}

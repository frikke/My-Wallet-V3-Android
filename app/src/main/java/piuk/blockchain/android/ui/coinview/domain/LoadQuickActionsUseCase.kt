package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.CryptoAsset
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.data.DataResource
import com.blockchain.data.anyError
import com.blockchain.data.anyLoading
import com.blockchain.data.getFirstError
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewRecurringBuys

class LoadQuickActionsUseCase(
    private val walletModeService: WalletModeService,
    private val kycService: KycService,
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val custodialWalletManager: CustodialWalletManager,
) {
    // todo(othman) remove accounts/total balance args - and use flow once caching is available
    operator fun invoke(
        asset: CryptoAsset,
        accounts: CoinviewAccounts,
        totalBalance: CoinviewAssetTotalBalance
    ): Flow<DataResource<CoinviewRecurringBuys>> {
        when (walletModeService.enabledWalletMode()) {
            WalletMode.UNIVERSAL, WalletMode.CUSTODIAL_ONLY -> {
                // center: SWAP
                // bottom start: SELL
                // bottom end: BUY

                combine(
                    kycService.getHighestApprovedTierLevel(),
                    userFeaturePermissionService.isEligibleFor(Feature.SimplifiedDueDiligence),
                    userFeaturePermissionService.getAccessForFeature(Feature.Buy),
                    userFeaturePermissionService.getAccessForFeature(Feature.Sell),
                    custodialWalletManager.isCurrencyAvailableForTrading(asset.currency),
                    custodialWalletManager.isCurrencyAvailableForTrading(asset.currency)
                ) { kycTier, sddEligibility, buyAccess, sellAccess, isAvailableForTrading, isSupportedForSwap->

                }
            }
            WalletMode.NON_CUSTODIAL_ONLY -> {
                // center: SWAP
                // bottom start: RECEIVE
                // bottom end: SEND
            }
        }

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

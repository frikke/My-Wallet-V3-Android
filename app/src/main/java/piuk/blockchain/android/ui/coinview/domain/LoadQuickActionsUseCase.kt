package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAsset
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.store.mapData
import com.blockchain.utils.toFlowDataResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickActions

class LoadQuickActionsUseCase(
    private val kycService: KycService,
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val custodialWalletManager: CustodialWalletManager,
) {
    // todo(othman) remove accounts/total balance args - and use flow once caching is available
    operator fun invoke(
        asset: CryptoAsset,
        accounts: CoinviewAccounts,
        totalBalance: CoinviewAssetTotalBalance
    ): Flow<DataResource<CoinviewQuickActions>> {
        return when (accounts) {
            is CoinviewAccounts.Custodial -> {
                // center: SWAP
                // bottom start: SELL
                // bottom end: BUY

                // get target account
                val custodialAccount = accounts.accounts.firstOrNull()?.account

                // return None when no account is found
                if (custodialAccount == null) {
                    flowOf(DataResource.Data(CoinviewQuickActions.none()))
                } else {
                    combine(
                        kycService.getHighestApprovedTierLevel(),
                        userFeaturePermissionService.isEligibleFor(Feature.SimplifiedDueDiligence),
                        userFeaturePermissionService.getAccessForFeatures(Feature.Buy, Feature.Sell),
                        custodialWalletManager.isCurrencyAvailableForTrading(asset.currency),
                        custodialWalletManager.isAssetSupportedForSwap(asset.currency).toFlowDataResource()
                    ) { kycTier, sddEligibility, featuresAccess, isAvailableForTrading, isSupportedForSwap ->
                        combineDataResources(
                            kycTier, sddEligibility, featuresAccess, isAvailableForTrading, isSupportedForSwap
                        ) { kycTierData,
                            sddEligibilityData,
                            featuresAccessData,
                            isAvailableForTradingData,
                            isSupportedForSwapData ->

                            val assetFilters = listOf(AssetFilter.Trading)

                            val hasPositiveFilterBalance = assetFilters.any {
                                totalBalance.totalCryptoBalance[it]?.isPositive ?: false
                            }

                            /**
                             * Sell button will be enabled if
                             * * Sell access is [FeatureAccess.Granted]
                             *
                             * *AND*
                             *
                             * * Is available for trading ([isAvailableForTrading])
                             *
                             * *AND*
                             *
                             * * kyc Gold *OR* eligible for SimplifiedDueDiligence
                             */
                            val sellAccess = featuresAccessData[Feature.Sell]
                            val canSell = sellAccess is FeatureAccess.Granted &&
                                isAvailableForTradingData &&
                                (kycTierData == KycTier.GOLD || sddEligibilityData) &&
                                hasPositiveFilterBalance

                            /**
                             * Buy button will be enabled if
                             * * Is available for trading ([isAvailableForTrading])
                             *
                             * *AND*
                             *
                             * * Buy access is [FeatureAccess.Granted]
                             *
                             * *OR*
                             *
                             * * Access is [FeatureAccess.Blocked] but because of [BlockedReason.InsufficientTier],
                             * when trying to buy with low tier upgrading to gold will be requested
                             */
                            val buyAccess = featuresAccessData[Feature.Buy]
                            val canBuy = isAvailableForTradingData &&
                                (
                                    buyAccess is FeatureAccess.Granted ||
                                        (
                                            buyAccess is FeatureAccess.Blocked &&
                                                buyAccess.reason is BlockedReason.InsufficientTier
                                            )
                                    )

                            /**
                             * Swap button will be enabled if
                             * * Balance is positive
                             */
                            val canSwap = hasPositiveFilterBalance && isSupportedForSwapData

                            /**
                             * Send button will be enabled if
                             * * Balance is positive
                             *
                             * *AND*
                             *
                             * * Is available for trading ([isAvailableForTrading])
                             */
                            val canSend = isAvailableForTradingData && hasPositiveFilterBalance

                            /**
                             * Receive button will be enabled if
                             * * Is available for trading ([isAvailableForTrading])
                             */
                            @Suppress("UnnecessaryVariable")
                            val canReceive = isAvailableForTradingData

                            val centerButtons = listOfNotNull(
                                CoinviewQuickAction.Swap.takeIf { canSwap },
                                CoinviewQuickAction.Receive.takeIf { canReceive },
                                CoinviewQuickAction.Send.takeIf { canSend },
                            )

                            val bottomButtons = listOfNotNull(
                                CoinviewQuickAction.Sell.takeIf { canSell },
                                CoinviewQuickAction.Buy.takeIf { canBuy },
                            )

                            /**
                             * if true, combine both lists on the bottom
                             */
                            val canListsBeCombined = centerButtons.size == 1 && bottomButtons.size == 1

                            CoinviewQuickActions(
                                center = if (canListsBeCombined) listOf() else centerButtons,
                                bottom = if (canListsBeCombined) centerButtons + bottomButtons else bottomButtons
                            )
                        }
                    }
                }
            }

            is CoinviewAccounts.Defi -> {
                // center: SWAP
                // bottom start: RECEIVE
                // bottom end: SEND

                val nonCustodialAccount = accounts.accounts.firstOrNull()

                if (nonCustodialAccount == null) {
                    flowOf(DataResource.Data(CoinviewQuickActions.none()))
                } else {
                    custodialWalletManager.isAssetSupportedForSwap(asset.currency).toFlowDataResource()
                        .mapData { isSwapAvailable ->
                            /**
                             * Send button will be enabled if
                             * * Balance is positive
                             */
                            val canSend = totalBalance.totalCryptoBalance[AssetFilter.NonCustodial]?.isPositive == true

                            /**
                             * Can always receive
                             */
                            val canReceive = true

                            /**
                             * Swap button will be enabled if
                             * * Balance is positive
                             */
                            val canSwap =
                                totalBalance.totalCryptoBalance[AssetFilter.NonCustodial]?.isPositive == true &&
                                    isSwapAvailable

                            val centerButtons = listOfNotNull(
                                CoinviewQuickAction.Send.takeIf { canSend },
                                CoinviewQuickAction.Receive.takeIf { canReceive },
                            )

                            val bottomButtons = listOfNotNull(
                                CoinviewQuickAction.Swap.takeIf { canSwap }
                            )

                            /**
                             * if true, put the centerbottons on the bottom
                             */
                            val shouldSwitch = bottomButtons.isEmpty()

                            CoinviewQuickActions(
                                center = if (shouldSwitch) emptyList() else centerButtons,
                                bottom = if (shouldSwitch) centerButtons else bottomButtons
                            )
                        }
                }
            }
        }
    }
}

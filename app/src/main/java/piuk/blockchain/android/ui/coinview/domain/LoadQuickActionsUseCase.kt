package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAsset
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResourceFlows
import com.blockchain.data.combineDataResources
import com.blockchain.data.mapData
import com.blockchain.data.mapListData
import com.blockchain.domain.swap.SwapOption
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.utils.toFlowDataResource
import com.dex.domain.DexNetworkService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickActions

class LoadQuickActionsUseCase(
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val custodialWalletManager: CustodialWalletManager,
    private val kycService: KycService,
    private val dexNetworkService: DexNetworkService,
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
                        userFeaturePermissionService.getAccessForFeatures(
                            Feature.Buy,
                            Feature.Sell,
                            Feature.DepositCrypto,
                            Feature.Swap
                        ),
                        custodialWalletManager.isCurrencyAvailableForTrading(asset.currency),
                        custodialWalletManager.isAssetSupportedForSwap(asset.currency).toFlowDataResource(),
                        kycService.stateFor(KycTier.GOLD).mapData { it == KycTierState.Rejected }
                    ) { featuresAccess, isAvailableForTrading, isSupportedForSwap, isKycRejected ->
                        combineDataResources(
                            featuresAccess,
                            isAvailableForTrading,
                            isSupportedForSwap,
                            isKycRejected
                        ) { featuresAccessData,
                            isAvailableForTradingData,
                            isSupportedForSwapData,
                            isKycRejectedData ->

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
                             * * Access is [FeatureAccess.Granted]
                             *
                             * *AND*
                             *
                             * * Balance is positive
                             */
                            val swapAccess = featuresAccessData[Feature.Swap]
                            val canSwap = swapAccess is FeatureAccess.Granted &&
                                hasPositiveFilterBalance && isSupportedForSwapData

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
                            val receiveAccess = featuresAccessData[Feature.DepositCrypto]
                            val canReceive = receiveAccess is FeatureAccess.Granted && isAvailableForTradingData

                            val centerButtons = listOfNotNull(
                                CoinviewQuickAction.Swap(
                                    enabled = !isKycRejectedData,
                                    swapOption = SwapOption.BcdcSwap
                                ).takeIf { canSwap },
                                CoinviewQuickAction.Receive(enabled = !isKycRejectedData).takeIf { canReceive },
                                CoinviewQuickAction.Send(enabled = !isKycRejectedData).takeIf { canSend }
                            )

                            val bottomButtons = listOfNotNull(
                                CoinviewQuickAction.Sell(enabled = !isKycRejectedData).takeIf { canSell },
                                CoinviewQuickAction.Buy(enabled = !isKycRejectedData).takeIf { canBuy }
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
                    val userEligibleForDexFlow = userFeaturePermissionService.isEligibleFor(Feature.Dex)

                    val assetAvailableOnDexFlow = flow {
                        emit(DataResource.Data(dexNetworkService.supportedNetworks()))
                    }.mapListData {
                        it.nativeAssetTicker
                    }.mapData {
                        it.contains(asset.currency.coinNetwork?.nativeAssetTicker)
                    }

                    val isAvailableForSwapFlow = custodialWalletManager.isAssetSupportedForSwap(asset.currency)
                        .toFlowDataResource()

                    val isSellSupportedFlow = userFeaturePermissionService.getAccessForFeature(Feature.Sell)
                        .mapData { it is FeatureAccess.Granted }

                    val isAvailableForTradingFlow = custodialWalletManager.isCurrencyAvailableForTrading(asset.currency)

                    val isKycGoldFlow = kycService.stateFor(KycTier.GOLD).mapData { it == KycTierState.Verified }

                    combineDataResourceFlows(
                        userEligibleForDexFlow,
                        assetAvailableOnDexFlow,
                        isAvailableForSwapFlow,
                        isSellSupportedFlow,
                        isAvailableForTradingFlow,
                        isKycGoldFlow
                    ) { userEligibleForDex,
                        availableForDex,
                        isAvailableForSwap,
                        isSellSupported,
                        isAvailableForTrading,
                        isKycGold ->

                        val isSwapSupported = isAvailableForSwap && isKycGold

                        val isDexSupported = userEligibleForDex && availableForDex

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
                         * Get XXX will be available if balance is 0 - could be native swap or dex
                         */
                        val noBalanceSwapOption =
                            if (totalBalance.totalCryptoBalance[AssetFilter.NonCustodial]?.isZero == true) {
                                when {
                                    isSwapSupported -> SwapOption.BcdcSwap
                                    isDexSupported -> SwapOption.Dex
                                    else -> null
                                }
                            } else {
                                null
                            }

                        /**
                         * Swap button will be enabled if
                         * * Balance is positive
                         * * swap option: bcdc swap and/or dex
                         */
                        val positiveBalanceSwapOption =
                            if (totalBalance.totalCryptoBalance[AssetFilter.NonCustodial]?.isPositive == true) {
                                when {
                                    isSwapSupported && isDexSupported -> SwapOption.Multiple
                                    isSwapSupported -> SwapOption.BcdcSwap
                                    isDexSupported -> SwapOption.Dex
                                    else -> null
                                }
                            } else {
                                null
                            }

                        /**
                         * Can sell if
                         * * Sell is supported
                         * * Asset is available for bcdc
                         * * kyc gold
                         */
                        val canSell = isSellSupported && isAvailableForTrading && isKycGold

                        val centerButtons = listOfNotNull(
                            CoinviewQuickAction.Send().takeIf { canSend },
                            CoinviewQuickAction.Receive().takeIf { canReceive }
                        )

                        val bottomButtons = listOfNotNull(
                            noBalanceSwapOption?.let { CoinviewQuickAction.Get(swapOption = it) },
                            positiveBalanceSwapOption?.let { CoinviewQuickAction.Swap(swapOption = it) },
                            CoinviewQuickAction.Sell().takeIf { canSell },
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

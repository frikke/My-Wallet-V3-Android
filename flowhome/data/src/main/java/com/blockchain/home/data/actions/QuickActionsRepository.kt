package com.blockchain.home.data.actions

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.onErrorReturn
import com.blockchain.home.actions.QuickActionsService
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.rx3.asFlow

class QuickActionsRepository(
    private val coincore: Coincore,
    private val userFeaturePermissionService: UserFeaturePermissionService
) : QuickActionsService {

    private var defiActionsCache = listOf<StateAwareAction>()
    private var tradingActionsCache = listOf<StateAwareAction>()

    override fun availableQuickActionsForWalletMode(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy
    ): Flow<List<StateAwareAction>> =
        allQuickActionsForWalletMode(walletMode, freshnessStrategy)

    private fun allQuickActionsForWalletMode(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy
    ): Flow<List<StateAwareAction>> =
        when (walletMode) {
            WalletMode.NON_CUSTODIAL -> {
                allActionsForDefi(freshnessStrategy)
            }

            WalletMode.CUSTODIAL -> {
                allActionsForBrokerage(freshnessStrategy)
            }
        }

    private fun allActionsForBrokerage(freshnessStrategy: FreshnessStrategy): Flow<List<StateAwareAction>> {
        val featuresDataResourceFlow = userFeaturePermissionService.getAccessForFeatures(
            Feature.Sell,
            Feature.Buy,
            Feature.Swap,
            Feature.DepositFiat,
            Feature.WithdrawFiat,
            Feature.DepositCrypto,
            freshnessStrategy = freshnessStrategy
        ).filterNot { it is DataResource.Loading }

        val balanceFlow =
            totalWalletModeBalance(WalletMode.CUSTODIAL, freshnessStrategy)
                .map { it.totalFiat?.isPositive == true }
                .catch { emit(false) }
                .distinctUntilChanged().debounce(200)

        val hasFiatBalance =
            coincore.activeWalletsInModeRx(
                WalletMode.CUSTODIAL,
                freshnessStrategy = freshnessStrategy
            )
                .map { it.accounts.filterIsInstance<FiatAccount>() }
                .distinctUntilChanged { old, new ->
                    old.map { it.currency.networkTicker }.toSet() ==
                        new.map { it.currency.networkTicker }
                }
                .flatMap {
                    if (it.isEmpty()) {
                        Observable.just(false)
                    } else
                        it[0].balanceRx().map { balance ->
                            balance.total.isPositive
                        }.distinctUntilChanged()
                }.onErrorReturn { false }.asFlow()

        return combine(
            balanceFlow,
            featuresDataResourceFlow,
            hasFiatBalance
        ) { balanceIsPositive, featuresDataResource, fiatBalanceIsPositive ->
            val features = (featuresDataResource as? DataResource.Data)?.data ?: emptyMap()
            listOf(
                StateAwareAction(
                    action = AssetAction.Buy,
                    state = features[Feature.Buy].toAvailability()
                ),
                StateAwareAction(
                    action = AssetAction.Sell,
                    state = if (features[Feature.Sell].toAvailability() == ActionState.Available &&
                        balanceIsPositive
                    ) {
                        ActionState.Available
                    } else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.Swap,
                    state = if (features[Feature.Swap].toAvailability() == ActionState.Available &&
                        balanceIsPositive
                    ) {
                        ActionState.Available
                    } else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.Receive,
                    state = features[Feature.DepositCrypto].toAvailability()
                ),
                StateAwareAction(
                    action = AssetAction.Send,
                    state = if (balanceIsPositive) ActionState.Available else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.FiatWithdraw,
                    state = if (features[Feature.WithdrawFiat].toAvailability() == ActionState.Available &&
                        fiatBalanceIsPositive
                    ) {
                        ActionState.Available
                    } else ActionState.Unavailable
                )
            ).sorted(balanceIsPositive)
        }.onEach {
            tradingActionsCache = it
        }.onStart {
            emit(tradingActionsCache)
        }
    }

    private fun AssetAction.canAddFunds() =
        when (this) {
            AssetAction.Buy,
            AssetAction.FiatDeposit,
            AssetAction.Receive -> true

            AssetAction.ViewActivity,
            AssetAction.ViewStatement,
            AssetAction.Send,
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.FiatWithdraw,
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.Sign,
            AssetAction.ActiveRewardsDeposit,
            AssetAction.ActiveRewardsWithdraw,
            AssetAction.StakingDeposit,
            AssetAction.StakingWithdraw -> false
        }

    private fun FeatureAccess?.toAvailability(): ActionState {
        return if (this is FeatureAccess.Granted ||
            (this is FeatureAccess.Blocked && this.reason is BlockedReason.InsufficientTier)
        ) {
            ActionState.Available
        } else ActionState.Unavailable
    }

    private fun allActionsForDefi(freshnessStrategy: FreshnessStrategy): Flow<List<StateAwareAction>> {
        val sellEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                Feature.Sell,
                freshnessStrategy
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn {
                    false
                }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }

        val balanceFlow = totalWalletModeBalance(WalletMode.NON_CUSTODIAL, freshnessStrategy).map {
            it.total.isPositive
        }.catch { emit(false) }

        return combine(sellEnabledFlow, balanceFlow) { sellEligible, balanceIsPositive ->
            listOf(
                StateAwareAction(
                    action = AssetAction.Swap,
                    state = if (balanceIsPositive) ActionState.Available else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.Send,
                    state = if (balanceIsPositive) ActionState.Available else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.Receive,
                    state = ActionState.Available
                ),
                StateAwareAction(
                    action = AssetAction.Sell,
                    state = if (balanceIsPositive && sellEligible) ActionState.Available else ActionState.Unavailable
                )
            ).sorted(balanceIsPositive)
        }.onEach {
            defiActionsCache = it
        }.onStart {
            emit(defiActionsCache)
        }
    }

    private fun totalWalletModeBalance(walletMode: WalletMode, freshnessStrategy: FreshnessStrategy) =
        coincore.activeWalletsInModeRx(
            walletMode = walletMode,
            freshnessStrategy = freshnessStrategy
        ).distinctUntilChanged { t1, t2 ->
            t1.accounts.map { it.currency.networkTicker } ==
                t2.accounts.map { it.currency.networkTicker }
        }.flatMap {
            it.balanceRx()
        }.asFlow()

    private fun List<StateAwareAction>.sorted(balanceIsPositive: Boolean): List<StateAwareAction> {
        return if (balanceIsPositive) {
            this
        } else {
            sortedByDescending { it.action.canAddFunds() && it.state == ActionState.Available }
        }
    }
}

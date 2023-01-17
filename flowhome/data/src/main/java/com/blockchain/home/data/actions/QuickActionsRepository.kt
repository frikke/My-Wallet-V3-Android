package com.blockchain.home.data.actions

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.onErrorReturn
import com.blockchain.home.actions.QuickActionsService
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.asFlow
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.rx3.asFlow

class QuickActionsRepository(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val userFeaturePermissionService: UserFeaturePermissionService
) : QuickActionsService {

    private val defFreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES))

    private var moreActionsCache = listOf(
        StateAwareAction(action = AssetAction.Send, state = ActionState.Unavailable),
        StateAwareAction(action = AssetAction.FiatDeposit, state = ActionState.Unavailable),
        StateAwareAction(action = AssetAction.FiatWithdraw, state = ActionState.Unavailable)
    )

    override fun availableQuickActionsForWalletMode(walletMode: WalletMode): Flow<List<StateAwareAction>> =
        allQuickActionsForWalletMode(walletMode).map { actionList ->
            actionList.filter { action ->
                action.state == ActionState.Available
            }
        }

    private fun unavailableQuickActionsForWalletMode(walletMode: WalletMode): Flow<List<StateAwareAction>> =
        allQuickActionsForWalletMode(walletMode).map { actionList ->
            actionList.filter { action ->
                action.state != ActionState.Available
            }
        }

    private fun allQuickActionsForWalletMode(walletMode: WalletMode): Flow<List<StateAwareAction>> =
        when (walletMode) {
            WalletMode.NON_CUSTODIAL -> {
                allActionsForDefi()
            }
            WalletMode.CUSTODIAL -> {
                allActionsForBrokerage()
            }
        }

    private fun allActionsForBrokerage(): Flow<List<StateAwareAction>> {
        val buyEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                Feature.Buy,
                defFreshnessStrategy
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val sellEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                Feature.DepositFiat,
                defFreshnessStrategy
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val swapEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                Feature.Swap,
                defFreshnessStrategy
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val receiveEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                Feature.DepositCrypto,
                defFreshnessStrategy
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }
        val balanceFlow = totalWalletModeBalance(WalletMode.CUSTODIAL).map { it.totalFiat.isPositive }

        return combine(
            balanceFlow,
            buyEnabledFlow,
            sellEnabledFlow,
            swapEnabledFlow,
            receiveEnabledFlow
        ) { config ->
            val balanceIsPositive = config[0]
            val buyEnabled = config[1]
            val sellEnabled = config[2]
            val swapEnabled = config[3]
            val receiveEnabled = config[4]
            listOf(
                StateAwareAction(
                    action = AssetAction.Buy,
                    state = if (buyEnabled) ActionState.Available else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.Sell,
                    state = if (sellEnabled && balanceIsPositive) ActionState.Available else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.Swap,
                    state = if (swapEnabled && balanceIsPositive) ActionState.Available else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.Receive,
                    state = if (receiveEnabled) ActionState.Available else ActionState.Unavailable
                )
            )
        }
    }

    private fun allActionsForDefi(): Flow<List<StateAwareAction>> {
        val sellEnabledFlow =
            userFeaturePermissionService.isEligibleFor(
                Feature.Sell,
                defFreshnessStrategy
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn {
                    false
                }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }

        val balanceFlow = totalWalletModeBalance(WalletMode.NON_CUSTODIAL).map {
            it.total.isPositive
        }

        return combine(sellEnabledFlow, balanceFlow) { sellEligible, balanceIsPositive ->
            listOf(
                StateAwareAction(
                    action = AssetAction.Swap,
                    state = if (balanceIsPositive) ActionState.Available else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.Receive,
                    state = ActionState.Available
                ),
                StateAwareAction(
                    action = AssetAction.Send,
                    state = if (balanceIsPositive) ActionState.Available else ActionState.Unavailable
                ),
                StateAwareAction(
                    action = AssetAction.Sell,
                    state = if (balanceIsPositive && sellEligible) ActionState.Available else ActionState.Unavailable
                )
            )
        }
    }

    private fun totalWalletModeBalance(walletMode: WalletMode) =
        coincore.activeWalletsInModeRx(
            walletMode = walletMode,
            freshnessStrategy =
            defFreshnessStrategy
        ).flatMap {
            it.balanceRx()
        }.asFlow()

    override fun moreActions(walletMode: WalletMode): Flow<List<StateAwareAction>> {

        val disabledQuickActionsFlow = unavailableQuickActionsForWalletMode(walletMode)

        val hasBalanceFlow =
            coincore.activeWalletsInModeRx(
                walletMode = walletMode,
                freshnessStrategy =
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES))
            ).flatMap {
                it.balanceRx()
            }.map {
                it.total.isPositive
            }.onErrorReturn { false }
                .asFlow()

        val hasFiatBalance =
            coincore.activeWalletsInModeRx(
                WalletMode.CUSTODIAL,
                freshnessStrategy =
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES))
            )
                .map { it.accounts.filterIsInstance<FiatAccount>() }
                .flatMap {
                    if (it.isEmpty()) {
                        Observable.just(false)
                    } else
                        it[0].balanceRx().map { balance ->
                            balance.total.isPositive
                        }
                }.onErrorReturn { false }.asFlow()

        val depositFiatFeature =
            userFeaturePermissionService.isEligibleFor(
                Feature.DepositFiat,
                FreshnessStrategy.Cached(
                    RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
                )
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }

        val withdrawFiatFeature =
            userFeaturePermissionService.isEligibleFor(
                Feature.WithdrawFiat,
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES))
            ).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }

        val stateAwareActions = coincore.allFiats()
            .flatMap { list ->
                list.firstOrNull()?.stateAwareActions ?: Single.just(emptySet())
            }.asFlow()

        val moreActionsFlow = combine(
            hasBalanceFlow,
            depositFiatFeature,
            withdrawFiatFeature,
            hasFiatBalance,
            stateAwareActions
        ) { hasBalance, depositEnabled, withdrawEnabled, hasAnyFiatBalance, actions ->
            if (walletMode == WalletMode.CUSTODIAL) {
                listOf(
                    StateAwareAction(
                        action = AssetAction.Send,
                        state = if (hasBalance) ActionState.Available else ActionState.Unavailable
                    ),
                    StateAwareAction(
                        action = AssetAction.FiatDeposit,
                        state = if (depositEnabled && hasAnyFiatBalance && actions.hasAvailableAction(
                                AssetAction.FiatDeposit
                            )
                        ) ActionState.Available else ActionState.Unavailable
                    ),
                    StateAwareAction(
                        action = AssetAction.FiatWithdraw,
                        state = if (withdrawEnabled && actions.hasAvailableAction(
                                AssetAction.FiatWithdraw
                            )
                        ) ActionState.Available else ActionState.Unavailable
                    ),
                )
            } else {
                emptyList()
            }
        }

        return combine(moreActionsFlow, disabledQuickActionsFlow) { moreActions, disabledActions ->
            moreActions + disabledActions
        }.onStart {
            emit(moreActionsCache)
        }
            .distinctUntilChanged()
            .onEach {
                moreActionsCache = it
            }
    }

    private fun Set<StateAwareAction>.hasAvailableAction(action: AssetAction): Boolean =
        firstOrNull { it.action == action && it.state == ActionState.Available } != null
}

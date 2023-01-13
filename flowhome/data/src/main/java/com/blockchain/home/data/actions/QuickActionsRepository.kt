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
    private val userFeaturePermissionService: UserFeaturePermissionService
) : QuickActionsService {

    private var moreActionsCache = listOf(
        StateAwareAction(action = AssetAction.Send, state = ActionState.Unavailable),
        StateAwareAction(action = AssetAction.FiatDeposit, state = ActionState.Unavailable),
        StateAwareAction(action = AssetAction.FiatWithdraw, state = ActionState.Unavailable)
    )

    override fun moreActions(): Flow<List<StateAwareAction>> {
        val hasCustodialBalance =
            coincore.activeWalletsInModeRx(
                walletMode = WalletMode.CUSTODIAL,
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

        return combine(
            hasCustodialBalance,
            depositFiatFeature,
            withdrawFiatFeature,
            hasFiatBalance,
            stateAwareActions
        ) { hasBalance, depositEnabled, withdrawEnabled, hasAnyFiatBalance, actions ->
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

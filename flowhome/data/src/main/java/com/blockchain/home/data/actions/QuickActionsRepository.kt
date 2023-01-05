package com.blockchain.home.data.actions

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.data.DataResource
import com.blockchain.data.onErrorReturn
import com.blockchain.home.actions.QuickActionsService
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.asFlow
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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

    private var moreActionsCache = listOf(
        StateAwareAction(action = AssetAction.Send, state = ActionState.Unavailable),
        StateAwareAction(action = AssetAction.FiatDeposit, state = ActionState.Unavailable),
        StateAwareAction(action = AssetAction.FiatWithdraw, state = ActionState.Unavailable)
    )

    override fun moreActions(): Flow<List<StateAwareAction>> {
        val custodialBalance = coincore.activeWallets(WalletMode.CUSTODIAL).flatMapObservable {
            it.balanceRx
        }.asFlow().catch { emit(AccountBalance.zero(currencyPrefs.selectedFiatCurrency)) }

        val hasFiatBalance =
            coincore.activeWallets(WalletMode.CUSTODIAL).map { it.accounts.filterIsInstance<FiatAccount>() }
                .flatMapObservable {
                    if (it.isEmpty()) {
                        Observable.just(false)
                    } else
                        it[0].balanceRx.map { balance ->
                            balance.total.isPositive
                        }
                }.asFlow()

        val depositFiatFeature =
            userFeaturePermissionService.isEligibleFor(Feature.DepositFiat).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }

        val withdrawFiatFeature =
            userFeaturePermissionService.isEligibleFor(Feature.WithdrawFiat).filterNot { it is DataResource.Loading }
                .onErrorReturn { false }
                .map {
                    (it as? DataResource.Data<Boolean>)?.data ?: throw IllegalStateException("Data should be returned")
                }

        val stateAwareActions = coincore.allFiats()
            .flatMap { list ->
                list.firstOrNull()?.stateAwareActions ?: Single.just(emptySet())
            }.asFlow()

        return combine(
            custodialBalance,
            depositFiatFeature,
            withdrawFiatFeature,
            hasFiatBalance,
            stateAwareActions
        ) { balance, depositEnabled, withdrawEnabled, hasAnyFiatBalance, actions ->
            listOf(
                StateAwareAction(
                    action = AssetAction.Send,
                    state = if (balance.total.isPositive) ActionState.Available else ActionState.Unavailable
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

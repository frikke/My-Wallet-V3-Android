package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.data.DataResource
import com.blockchain.extensions.minus
import com.blockchain.preferences.DashboardPrefs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitFirst
import kotlinx.coroutines.supervisorScope
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator

data class GetAccountActionsUseCase(
    private val assetActionsComparator: StateAwareActionsComparator,
    private val dashboardPrefs: DashboardPrefs,
    private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(account: CoinviewAccount): DataResource<List<StateAwareAction>> {
        return supervisorScope {
            val actionsDeferred = async(dispatcher) { account.account.stateAwareActions.await() }
            val balanceDeferred = async(dispatcher) { account.account.balanceRx.awaitFirst() }

            try {
                val actions = actionsDeferred.await()
                val balance = balanceDeferred.await()

                assetActionsComparator.initAccount(account.account, balance)
                val sortedActions = when (account) {
                    is InterestAccount -> {
                        if (actions.none { it.action == AssetAction.InterestDeposit }) {
                            actions + StateAwareAction(ActionState.Available, AssetAction.InterestDeposit)
                        } else {
                            actions
                        }
                    }
                    else -> actions.minus { it.action == AssetAction.InterestDeposit }
                }.sortedWith(assetActionsComparator)

                DataResource.Data(sortedActions)
            } catch (e: Exception) {
                DataResource.Error(e)
            }
        }
    }

    /**
     * @return Pair<isIntroSeen: Boolean, markAsSeen: () -> Unit>
     */
    fun getSeenAccountExplainerState(account: CoinviewAccount): Pair<Boolean, () -> Unit> {
        return when (account) {
            is CoinviewAccount.Universal -> {
                when (account.filter) {
                    AssetFilter.Trading -> {
                        Pair(dashboardPrefs.isCustodialIntroSeen) { dashboardPrefs.isCustodialIntroSeen = true }
                    }
                    AssetFilter.Interest -> {
                        Pair(dashboardPrefs.isRewardsIntroSeen) { dashboardPrefs.isRewardsIntroSeen = true }
                    }
                    AssetFilter.NonCustodial -> {
                        Pair(dashboardPrefs.isPrivateKeyIntroSeen) { dashboardPrefs.isPrivateKeyIntroSeen = true }
                    }
                    AssetFilter.Staking -> {
                        Pair(dashboardPrefs.isStakingIntroSeen) { dashboardPrefs.isStakingIntroSeen = true }
                    }
                    else -> error("account type not supported")
                }
            }
            is CoinviewAccount.Custodial.Interest -> {
                Pair(dashboardPrefs.isCustodialIntroSeen) { dashboardPrefs.isCustodialIntroSeen = true }
            }
            is CoinviewAccount.Custodial.Trading -> {
                Pair(dashboardPrefs.isRewardsIntroSeen) { dashboardPrefs.isRewardsIntroSeen = true }
            }
            is CoinviewAccount.Custodial.Staking -> {
                Pair(dashboardPrefs.isStakingIntroSeen) { dashboardPrefs.isStakingIntroSeen = true }
            }
            is CoinviewAccount.PrivateKey -> {
                Pair(dashboardPrefs.isPrivateKeyIntroSeen) { dashboardPrefs.isPrivateKeyIntroSeen = true }
            }
        }
    }
}

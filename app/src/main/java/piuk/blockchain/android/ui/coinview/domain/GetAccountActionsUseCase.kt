package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
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
import piuk.blockchain.android.ui.coinview.domain.model.isActiveRewardsAccount
import piuk.blockchain.android.ui.coinview.domain.model.isInterestAccount
import piuk.blockchain.android.ui.coinview.domain.model.isStakingAccount
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator

data class GetAccountActionsUseCase(
    private val assetActionsComparator: StateAwareActionsComparator,
    private val dashboardPrefs: DashboardPrefs,
    private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(account: CoinviewAccount): DataResource<List<StateAwareAction>> {
        return supervisorScope {
            try {
                val actionsDeferred = async(dispatcher) { account.account.stateAwareActions.await() }
                val balanceDeferred = async(dispatcher) { account.account.balanceRx().awaitFirst() }
                val actions = actionsDeferred.await()
                val balance = balanceDeferred.await()

                assetActionsComparator.initAccount(account.account, balance)

                val sortedActions = when {
                    account.isInterestAccount() -> {
                        if (actions.none { it.action == AssetAction.InterestDeposit }) {
                            actions + StateAwareAction(ActionState.Available, AssetAction.InterestDeposit)
                        } else {
                            actions
                        }
                    }
                    account.isStakingAccount() -> {
                        if (actions.none { it.action == AssetAction.StakingDeposit }) {
                            actions + StateAwareAction(ActionState.Available, AssetAction.StakingDeposit)
                        } else {
                            actions
                        }
                    }
                    account.isActiveRewardsAccount() -> {
                        if (actions.none { it.action == AssetAction.ActiveRewardsDeposit }) {
                            actions + StateAwareAction(ActionState.Available, AssetAction.ActiveRewardsDeposit)
                        } else {
                            actions
                        }
                    }
                    // TODO(EARN): Why are these shennenigans of removing actions and adding on the above cases here?
                    else -> actions.minus {
                        it.action == AssetAction.InterestDeposit ||
                            it.action == AssetAction.StakingDeposit ||
                            it.action == AssetAction.ActiveRewardsDeposit
                    }
                }.minus { it.action == AssetAction.ViewActivity }.sortedWith(assetActionsComparator)

                DataResource.Data(sortedActions)
            } catch (e: Exception) {
                DataResource.Error(e)
            }
        }
    }

    /**
     * @return Pair<isIntroSeen: Boolean, markAsSeen: () -> Unit>
     */
    fun getSeenAccountExplainerState(account: CoinviewAccount): Pair<Boolean, () -> Unit> =
        with(dashboardPrefs) {
            when (account) {
                is CoinviewAccount.Custodial.Interest ->
                    Pair(isCustodialIntroSeen) { isCustodialIntroSeen = true }
                is CoinviewAccount.Custodial.Trading ->
                    Pair(isRewardsIntroSeen) { isRewardsIntroSeen = true }
                is CoinviewAccount.Custodial.Staking ->
                    Pair(isStakingIntroSeen) { isStakingIntroSeen = true }
                is CoinviewAccount.Custodial.ActiveRewards ->
                    Pair(isActiveRewardsIntroSeen) { isActiveRewardsIntroSeen = true }
                is CoinviewAccount.PrivateKey ->
                    Pair(isPrivateKeyIntroSeen) { isPrivateKeyIntroSeen = true }
            }
        }
}

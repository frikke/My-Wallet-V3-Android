package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.data.DataResource
import com.blockchain.extensions.minus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitFirst
import kotlinx.coroutines.supervisorScope
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator

data class GetAccountActionsUseCase(
    private val assetActionsComparator: StateAwareActionsComparator,
    private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(account: BlockchainAccount): DataResource<List<StateAwareAction>> {
        return supervisorScope {
            val actionsDeferred = async(dispatcher) { account.stateAwareActions.await() }
            val balanceDeferred = async(dispatcher) { account.balance.awaitFirst() }

            try {
                val actions = actionsDeferred.await()
                val balance = balanceDeferred.await()

                assetActionsComparator.initAccount(account, balance)
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
}

package com.blockchain.coincore

import io.reactivex.rxjava3.core.Single

@Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY") // For code clarity
fun SingleAccountList.filterByAction(
    action: AssetAction
): Single<SingleAccountList> =
    Single.zip(
        this.map { account -> account.stateAwareActions.map { actions -> Pair(account, actions) } }
    ) { result: Array<Any> ->
        result.filterIsInstance<Pair<SingleAccount, Set<AssetAction>>>()
            .filter { (account, actions) -> actions.contains(action) }
            .map { (account, actions) -> account }
    }

@Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY") // For code clarity
fun SingleAccountList.filterByActionAndState(
    action: AssetAction,
    states: List<ActionState>
): Single<SingleAccountList> =
    Single.zip(
        this.map { account -> account.stateAwareActions.map { actions -> account to actions } }
    ) { result: Array<Any> ->
        result.filterIsInstance<Pair<SingleAccount, Set<StateAwareAction>>>()
            .filter { (account, stateAwareActions) ->
                stateAwareActions.any { stateAwareAction ->
                    stateAwareAction.action == action &&
                        (states.isEmpty() || states.contains(stateAwareAction.state))
                }
            }
            .map { (account, stateAwareActions) -> account }
    }

fun BlockchainAccount?.selectFirstAccount(): CryptoAccount {
    val selectedAccount = when (this) {
        is SingleAccount -> this
        is AccountGroup ->
            this.accounts
                .firstOrNull { a -> a.isDefault }
                ?: this.accounts.firstOrNull()
                ?: throw IllegalStateException("No SingleAccount found")
        else -> throw IllegalStateException("Unknown account base")
    }

    return selectedAccount as CryptoAccount
}

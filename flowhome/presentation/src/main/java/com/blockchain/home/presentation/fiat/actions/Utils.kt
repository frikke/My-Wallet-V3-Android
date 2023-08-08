package com.blockchain.home.presentation.fiat.actions

import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.StateAwareAction

fun Set<StateAwareAction>.hasAvailableAction(action: AssetAction): Boolean =
    firstOrNull { it.action == action && it.state == ActionState.Available } != null

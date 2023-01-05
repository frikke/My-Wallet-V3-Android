package com.blockchain.home.actions

import com.blockchain.coincore.StateAwareAction
import kotlinx.coroutines.flow.Flow

interface QuickActionsService {
    fun moreActions(): Flow<List<StateAwareAction>>
}

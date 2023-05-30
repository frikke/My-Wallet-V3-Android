package com.blockchain.internalnotifications

import kotlinx.coroutines.flow.Flow

interface NotificationReceiver {
    val events: Flow<NotificationEvent>
}

interface NotificationTransmitter {
    fun postEvent(event: NotificationEvent)
    fun postEvents(events: List<NotificationEvent>)
}

enum class NotificationEvent {
    Login,
    Logout,
    NonCustodialTransaction,
    TradingTransaction,
    RewardsTransaction,
    StakingTransaction,
    KycStatusChanged,
    PKWalletCreated,
    KycStarted,
}

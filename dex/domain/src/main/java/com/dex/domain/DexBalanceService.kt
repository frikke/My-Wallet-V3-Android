package com.dex.domain

import info.blockchain.balance.Money

interface DexBalanceService {
    suspend fun networkBalance(account: DexAccount): Money
}
